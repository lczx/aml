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
import org.spongycastle.crypto.tls.Certificate;
import org.spongycastle.crypto.tls.ProtocolVersion;
import org.spongycastle.crypto.tls.ServerOnlyTlsAuthentication;
import org.spongycastle.crypto.tls.TlsAuthentication;

import java.io.IOException;
import java.util.Hashtable;

public class ProxyTlsClient extends TlsClientBase {

    private static final Logger LOG = LoggerFactory.getLogger(ProxyTlsClient.class);

    private final ClientParameters clientParameters;
    private Hashtable serverExtensions;
    private Certificate peerCert;

    public ProxyTlsClient(final ClientParameters clientParameters) {
        this.clientParameters = clientParameters;
    }

    public ServerParameters makeServerParameters() {
        return new ServerParameters(context.getServerVersion(),
                selectedCipherSuite, selectedCompressionMethod, serverExtensions, peerCert);
    }

    @Override
    public TlsAuthentication getAuthentication() {
        return new ServerOnlyTlsAuthentication() {
            @Override
            public void notifyServerCertificate(final Certificate serverCertificate) {
                peerCert = serverCertificate;
            }
        };
    }

    @Override
    public ProtocolVersion getClientVersion() {
        return clientParameters.protocolVersion;
    }

    @Override
    public short[] getCompressionMethods() {
        return clientParameters.compressionMethods; // Should always be none
    }

    @Override
    public int[] getCipherSuites() {
        return clientParameters.cipherSuites; // TODO: This may need fixing
    }

    @Override
    public Hashtable getClientExtensions() throws IOException {
        return clientParameters.extensions; // TODO: This will surely crash
    }

    @Override
    public void processServerExtensions(final Hashtable serverExtensions) throws IOException {
        super.processServerExtensions(serverExtensions);
        this.serverExtensions = serverExtensions;
    }

}
