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

package io.github.lczx.aml.modules.tls.cert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.cert.X509CertificateHolder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProxyCertificateCache implements ProxyCertificateProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ProxyCertificateCache.class);

    private final Map<X500Name, ServerCredentials> credentialsCache = new HashMap<>();
    private final ProxyCertificateProvider nextProvider;

    public ProxyCertificateCache(final ProxyCertificateProvider nextProvider) {
        this.nextProvider = nextProvider;
    }

    @Override
    public ServerCredentials cloneCertificate(
            final int signatureAlgorithm, final X509CertificateHolder originalCertificate) throws IOException {
        final X500Name subject = originalCertificate.getSubject();
        ServerCredentials ret = credentialsCache.get(subject);

        if (ret == null || ret.getSignatureAlgorithm() != signatureAlgorithm) {
            LOG.debug("Cache miss, generating new credentials, key: {}", subject);
            ret = nextProvider.cloneCertificate(signatureAlgorithm, originalCertificate);
            credentialsCache.put(subject, ret);
        } else {
            LOG.debug("Retrieved credentials from cache, key: {}", subject);
        }
        return ret;
    }

}
