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

import io.github.lczx.aml.modules.tls.cert.ProxyCertificateProvider;
import io.github.lczx.aml.modules.tls.cert.ServerCredentials;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.crypto.tls.*;

import java.io.IOException;
import java.util.Hashtable;

public class ProxyTlsServer extends TlsServerBase {

    private final ProxyCertificateProvider credentialsProvider;
    private ServerParameters serverParameters;

    public ProxyTlsServer(final ProxyCertificateProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    public void setParams(final ServerParameters serverParameters) {
        this.serverParameters = serverParameters;
    }

    @Override
    protected TlsSignerCredentials getRSASignerCredentials() throws IOException {
        return getForgedCredentials(SignatureAlgorithm.rsa);
    }

    @Override
    protected TlsSignerCredentials getECDSASignerCredentials() throws IOException {
        return getForgedCredentials(SignatureAlgorithm.ecdsa);
    }

    @Override
    public ProtocolVersion getServerVersion() throws IOException {
        return serverParameters.protocolVersion;
    }

    @Override
    public short getSelectedCompressionMethod() throws IOException {
        return serverParameters.compressionMethod;
    }

    @Override
    public int getSelectedCipherSuite() throws IOException {
        return serverParameters.cipherSuite;
    }

    @Override
    public Hashtable getServerExtensions() throws IOException {
        return serverParameters.extensions;
    }

    private TlsSignerCredentials getForgedCredentials(final int signatureAlgorithm) throws IOException {
        final ServerCredentials credentials = credentialsProvider.cloneCertificate(signatureAlgorithm,
                new X509CertificateHolder(serverParameters.originalCertificate.getCertificateAt(0)));
        return new DefaultTlsSignerCredentials(context, credentials.getCertificate(), credentials.getPrivateKey(),
                getSignatureAndHashAlgorithm(credentials.getSignatureAlgorithm()));
    }

    private SignatureAndHashAlgorithm getSignatureAndHashAlgorithm(final int signatureAlgorithm) {
        // TODO This fails to provide a default value for the client supported algorithms if it wasn't sent

        if (supportedSignatureAlgorithms != null) {
            for (final Object alg : supportedSignatureAlgorithms) {
                if (((SignatureAndHashAlgorithm) alg).getSignature() == signatureAlgorithm)
                    return (SignatureAndHashAlgorithm) alg;
            }
        }
        // This will make DefaultSignerCredentials throw if TLS v1.2, that's OK
        return null;
    }

}
