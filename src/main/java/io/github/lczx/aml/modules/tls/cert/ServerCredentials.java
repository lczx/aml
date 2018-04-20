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

import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.tls.Certificate;

import java.util.Objects;

public class ServerCredentials {

    private final int signatureAlgorithm;
    private final Certificate certificate;
    private final AsymmetricKeyParameter privateKey;

    public ServerCredentials(final int keyAlgorithm, final X509CertificateHolder certificate,
                             final AsymmetricKeyParameter privateKey) {
        this(keyAlgorithm, certificate.toASN1Structure(), privateKey);
    }

    public ServerCredentials(final int keyAlgorithm, final org.spongycastle.asn1.x509.Certificate certificate,
                             final AsymmetricKeyParameter privateKey) {
        this(keyAlgorithm, new Certificate(new org.spongycastle.asn1.x509.Certificate[]{certificate}), privateKey);
    }

    public ServerCredentials(final int signatureAlgorithm, final Certificate certificate,
                             final AsymmetricKeyParameter privateKey) {
        Objects.requireNonNull(certificate, "Certificate is null");
        Objects.requireNonNull(privateKey, "Private key is null");
        this.signatureAlgorithm = signatureAlgorithm;
        this.certificate = certificate;
        this.privateKey = privateKey;
    }

    public int getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public AsymmetricKeyParameter getPrivateKey() {
        return privateKey;
    }

}
