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
import org.spongycastle.asn1.x509.Extension;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.X509v3CertificateBuilder;
import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.tls.SignatureAlgorithm;

import java.io.IOException;
import java.util.Objects;

public class ProxyCertificateBuilder implements ProxyCertificateProvider {

    // TODO See https://stackoverflow.com/questions/29852290

    private static final Logger LOG = LoggerFactory.getLogger(ProxyCertificateBuilder.class);

    private final X509CertificateHolder caCert;
    private final AsymmetricKeyParameter caKey;

    public ProxyCertificateBuilder(final X509CertificateHolder caCert, final AsymmetricKeyParameter caKey) {
        Objects.requireNonNull(caCert, "CA certificate must not be null");
        Objects.requireNonNull(caKey, "CA private key must not be null");
        this.caCert = caCert;
        this.caKey = caKey;
    }

    @Override
    public ServerCredentials cloneCertificate(final int signatureAlgorithm,
                                              final X509CertificateHolder originalCertificate) throws IOException {
        final AsymmetricCipherKeyPair keyPair = createKeyPair(signatureAlgorithm);

        final X509v3CertificateBuilder builder = CryptoUtils.createDefaultCertificateBuilder(
                caCert.getSubject(), originalCertificate.getSubject(), keyPair.getPublic());
        builder.copyAndAddExtension(Extension.subjectAlternativeName, false, originalCertificate);
        final X509CertificateHolder newCertificate = builder.build(CryptoUtils.createDefaultCASigner(caKey));

        LOG.debug("Forged certificate type {} for \"{}\"", signatureAlgorithm, originalCertificate.getSubject());

        /* // TO COMPARE ORIGINAL AND NEW ONE
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            System.out.println("---------------------------- OLD ------------------------------");
            System.out.println(cf.generateCertificate(new ByteArrayInputStream(originalCertificate.getEncoded())));
            System.out.println("---------------------------- NEW ------------------------------");
            System.out.println(cf.generateCertificate(new ByteArrayInputStream(newCertificate.getEncoded())));
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        return new ServerCredentials(signatureAlgorithm, newCertificate, keyPair.getPrivate());
    }

    private AsymmetricCipherKeyPair createKeyPair(final int signatureAlgorithm) throws IOException {
        switch (signatureAlgorithm) {
            case SignatureAlgorithm.ecdsa:
                return CryptoUtils.generateECCKeyPair(CryptoUtils.DEFAULT_ECC_PARAMS);
            case SignatureAlgorithm.rsa:
                return CryptoUtils.generateRSAKeyPair(CryptoUtils.DEFAULT_RSA_STRENGTH);
            default:
                throw new UnsupportedOperationException(
                        "Cannot generate key pair, signature algorithm not implemented (" + signatureAlgorithm + ')');
        }
    }

}
