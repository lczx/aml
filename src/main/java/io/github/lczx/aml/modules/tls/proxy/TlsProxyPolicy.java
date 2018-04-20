/*
 * Copyright 2018 Luca Zanussi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.lczx.aml.modules.tls.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.tls.ExtensionType;
import org.spongycastle.util.Arrays;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

public final class TlsProxyPolicy {

    private static final Logger LOG = LoggerFactory.getLogger(TlsProxyPolicy.class);

    private static final int EPOL_PASSTHROUGH = 0; // Passed unchanged to upstream
    private static final int EPOL_DROP = 1; // stripped before passing upstream because we can't handle it
    private static final int EPOL_USE_LOCAL = 2; // Replaced with local TLS extension

    private static final int ext_draft_channel_id = 0x7550; // Channel ID extension code

    // Blacklisted suites, stripped before passing upstream because we can't handle them if selected
    private static final int[] TLS_BLACKLISTED_CIPHER_SUITES = {
            // New PSK ciphers introduced by TLS 1.3 <https://tlswg.github.io/tls13-spec/#rfc.appendix.A.4>
            0x1301, // "TLS_AES_128_GCM_SHA256"
            0x1302, // "TLS_AES_256_GCM_SHA384"
            0x1303, // "TLS_CHACHA20_POLY1305_SHA256"
            0x1304, // "TLS_AES_128_CCM_SHA256"
            0x1305, // "TLS_AES_128_CCM_8_SHA256"
    };

    private static final int[] TLS_EXTENSION_POLICY = {
            ExtensionType.server_name, EPOL_PASSTHROUGH, // RFC 6066, Server Name Indication
            ExtensionType.status_request, EPOL_DROP, // RFC 6066, OCSP stapling
            ExtensionType.supported_groups, EPOL_USE_LOCAL, // RFC 7919, supported DHE curves
            ExtensionType.ec_point_formats, EPOL_USE_LOCAL, // RFC 4492, EC point format (usually uncompressed)
            ExtensionType.signature_algorithms, EPOL_USE_LOCAL, // RFC 5246, Supported signature algorithms
            ExtensionType.application_layer_protocol_negotiation, EPOL_PASSTHROUGH,
            ExtensionType.status_request_v2, EPOL_DROP, // RFC 6961, Multi OCSP stapling
            ExtensionType.signed_certificate_timestamp, EPOL_DROP, // RFC 6962, Certificate Transparency
            ExtensionType.padding, EPOL_DROP, // RFC 7685, Ext. manipulation may result in wrong size
            ExtensionType.extended_master_secret, EPOL_DROP, // RFC 7627, MITM prevention
            ExtensionType.session_ticket, EPOL_PASSTHROUGH, // RFC 5077, server-stateless session resuming TODO
            ext_draft_channel_id, EPOL_DROP, // DRAFT, self-generated client certificates
            ExtensionType.renegotiation_info, EPOL_PASSTHROUGH, // RFC 5746, Crypto-bind renegotiation to TLS conn. TODO
    };

    // TLS v1.3 extensions <https://tlswg.github.io/tls13-spec/draft-ietf-tls-tls13.html#rfc.section.4.2>
    private static final int[] TLS13_EXTENSIONS = {
            41, // pre_shared_key
            42, // early_data
            43, // supported_versions
            44, // cookie
            45, // psk_key_exchange_modes
            47, // certificate_authorities
            48, // oid_filters
            49, // post_handshake_auth
            50, // signature_algorithms_cert
            51, // key_share
    };

    private TlsProxyPolicy() { }

    /**
     * Filters the cipher suites received downstream before sending them to the network.
     *
     * @param downstreamCiphers The suites to filter, usually {@link ClientParameters#cipherSuites}
     * @return The suites to send upstream
     */
    public static int[] applyUplinkCipherSuitePolicy(final int[] downstreamCiphers) {
        // Strip unsupported cipher suites from the ones offered downstream,
        // we don't strip GREASE suites: if the server selects one, TLSClientProtocol errors out and closes for us
        return stripSuites(downstreamCiphers, TLS_BLACKLISTED_CIPHER_SUITES);
    }

    @SuppressWarnings("unchecked")
    public static Hashtable applyUplinkExtensionPolicy(
            final Hashtable downstreamExtensions, final Hashtable localExtensions) {
        return applyExtPolicy(downstreamExtensions, localExtensions);
    }

    private static Hashtable<Integer, byte[]> applyExtPolicy(
            final Hashtable<Integer, byte[]> downstreamExtensions, final Hashtable<Integer, byte[]> localExtensions) {
        final Iterator<Map.Entry<Integer, byte[]>> i = downstreamExtensions.entrySet().iterator();
        final StringBuilder log = new StringBuilder();
        boolean logWarn = false;

        while (i.hasNext()) {
            final Map.Entry<Integer, byte[]> e = i.next();
            log.append(' ').append(e.getKey()).append(':');
            switch (policyForExtension(e.getKey())) {
                case EPOL_PASSTHROUGH:
                    log.append("pass");
                    break;

                case EPOL_DROP:
                    log.append("drop");
                    i.remove();
                    break;

                case EPOL_USE_LOCAL:
                    final byte[] localData = localExtensions.get(e.getKey());
                    if (localData == null) {
                        log.append("drop(local_absent)");
                        i.remove();
                    } else {
                        log.append("local");
                        e.setValue(localData);
                    }
                    break;

                default:
                    if (isGreaseId(e.getKey())) {
                        log.append("pass(grease)");
                    } else if (Arrays.contains(TLS13_EXTENSIONS, e.getKey())) {
                        log.append("drop(v1.3)");
                        i.remove();
                    } else {
                        log.append("drop(***unknown***)");
                        logWarn = true;
                        i.remove();
                    }
                    break;
            }
        }

        // Add missing USE_LOCAL extensions
        for (final Map.Entry<Integer, byte[]> e : localExtensions.entrySet()) {
            if (policyForExtension(e.getKey()) == EPOL_USE_LOCAL && !downstreamExtensions.containsKey(e.getKey())) {
                log.append(' ').append(e.getKey()).append(":local(orig_absent)");
                downstreamExtensions.put(e.getKey(), e.getValue());
            }
        }

        if (logWarn)
            LOG.warn("Applied extension policy (unknown extensions detected):{}", log.toString());
        else
            LOG.debug("Applied extension policy:{}", log.toString());
        return downstreamExtensions;
    }

    private static int[] stripSuites(final int[] cipherSuites, final int[] toStrip) {
        int removed = 0;
        int[] suitesCopy = null;
        for (int i = 0; i < cipherSuites.length; ++i) {
            if (Arrays.contains(toStrip, cipherSuites[i])) {
                if (suitesCopy == null) suitesCopy = Arrays.copyOf(cipherSuites, cipherSuites.length);
                suitesCopy[i] = -1;
                removed++;
            }
        }

        if (removed == 0) return cipherSuites;

        final int[] ret = new int[cipherSuites.length - removed];
        int j = 0;
        for (final int suite : suitesCopy)
            if (suite != -1) ret[j++] = suite;
        return ret;
    }

    // GREASE random invalid ciphers & extension IDs <https://tools.ietf.org/html/draft-davidben-tls-grease-01>
    // Should be passed upstream and drop the connection if selected by the server
    private static boolean isGreaseId(final int id) {
        return (id & 0x0F0F) == 0x0A0A; // 0x0A0A : 0xFAFA - TLS_GREASE_IS_THE_WORD_*A
    }

    private static int policyForExtension(final int extId) {
        for (int i = 0; i < TLS_EXTENSION_POLICY.length; i += 2)
            if (TLS_EXTENSION_POLICY[i] == extId) return TLS_EXTENSION_POLICY[i + 1];
        return -1;
    }

}
