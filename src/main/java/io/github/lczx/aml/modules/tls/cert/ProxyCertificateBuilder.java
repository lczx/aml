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
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.Extension;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.X509v3CertificateBuilder;
import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.tls.SignatureAlgorithm;
import org.spongycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.spongycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.bc.BcRSAContentSignerBuilder;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class ProxyCertificateBuilder implements ProxyCertificateProvider {

    // TODO See https://stackoverflow.com/questions/29852290

    private static final Logger LOG = LoggerFactory.getLogger(ProxyCertificateBuilder.class);
    private static final String CA_SIGNER_ALGORITHM_NAME = "SHA256WithRSAEncryption"; // Since the CA has RSA keys
    private static final long ONE_DAY_MS = 86400000L; // 24 * 3600 * 1000

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

        long now = System.currentTimeMillis();
        final X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                caCert.getSubject(),
                BigInteger.valueOf(now),
                new Date(now - 30 * ONE_DAY_MS),
                new Date(now + 335 * ONE_DAY_MS),
                Locale.ENGLISH,
                originalCertificate.getSubject(),
                SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(keyPair.getPublic()));

        builder.copyAndAddExtension(Extension.subjectAlternativeName, false, originalCertificate);

        X509CertificateHolder holder = builder.build(createCASigner());
        LOG.debug("Forged certificate from {}, algorithm ID: {}", originalCertificate, signatureAlgorithm);

        /* // TO COMPARE ORIGINAL AND NEW ONE
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            System.out.println("---------------------------- OLD ------------------------------");
            System.out.println(cf.generateCertificate(new ByteArrayInputStream(originalCertificate.getEncoded())));
            System.out.println("---------------------------- NEW ------------------------------");
            System.out.println(cf.generateCertificate(new ByteArrayInputStream(holder.getEncoded())));
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        return new ServerCredentials(signatureAlgorithm, holder, keyPair.getPrivate());
    }

    private AsymmetricCipherKeyPair createKeyPair(final int signatureAlgorithm) {
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

    private ContentSigner createCASigner() {
        final AlgorithmIdentifier sigAlgId =
                new DefaultSignatureAlgorithmIdentifierFinder().find(CA_SIGNER_ALGORITHM_NAME);
        final AlgorithmIdentifier digAlgId =
                new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
        try {
            return new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(caKey);
        } catch (OperatorCreationException e) {
            throw new RuntimeException("Illegal signer algorithm name: \"" + CA_SIGNER_ALGORITHM_NAME + '"', e);
        }
    }

}
