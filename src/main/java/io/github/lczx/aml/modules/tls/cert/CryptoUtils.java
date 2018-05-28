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
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x9.ECNamedCurveTable;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.asn1.x9.X9ObjectIdentifiers;
import org.spongycastle.cert.X509v3CertificateBuilder;
import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.generators.ECKeyPairGenerator;
import org.spongycastle.crypto.generators.RSAKeyPairGenerator;
import org.spongycastle.crypto.params.*;
import org.spongycastle.crypto.prng.ThreadedSeedGenerator;
import org.spongycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.spongycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.bc.BcRSAContentSignerBuilder;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Date;
import java.util.Locale;

public final class CryptoUtils {

    /* package */ static final int DEFAULT_RSA_STRENGTH = 1024;
    /* package */ static final ASN1ObjectIdentifier DEFAULT_ECC_PARAMS = X9ObjectIdentifiers.prime256v1;
            // a.k.a. ANSI x9.62 prime256v1 / SEC secp256r1 / NIST P-256

    private static final Logger LOG = LoggerFactory.getLogger(CryptoUtils.class);
    private static final ThreadedSeedGenerator SEED_GEN = new ThreadedSeedGenerator();
    private static final String CA_SIGNER_ALGORITHM_NAME = "SHA256WithRSAEncryption"; // Since the CA has RSA keys
    private static final long ONE_DAY_MS = 86400000L; // 24 * 3600 * 1000

    // % of certainty that the generated primes are actually prime numbers,
    // use a low value to generate keys faster since we don't need strong ones
    private static final int RSA_CERTAINTY = 12;

    private CryptoUtils() { }

    public static SecureRandom createSecureRandom() {
        // 20 bytes should be enough
        return new SecureRandom(SEED_GEN.generateSeed(20, true));
    }

    /* package */ static AsymmetricCipherKeyPair generateRSAKeyPair(final int length) {
        final RSAKeyPairGenerator kpGen = new RSAKeyPairGenerator();
        kpGen.init(new RSAKeyGenerationParameters(RSAKeyGenParameterSpec.F4,
                createSecureRandom(), length, RSA_CERTAINTY));
        LOG.debug("Generating new RSA key pair, strength: {} bits", length);
        return kpGen.generateKeyPair();
    }

    /* package */ static AsymmetricCipherKeyPair generateECCKeyPair(final ASN1ObjectIdentifier curveOid) {
        final ECKeyPairGenerator kpGen = new ECKeyPairGenerator();
        final X9ECParameters x9 = ECNamedCurveTable.getByOID(curveOid);
        kpGen.init(new ECKeyGenerationParameters(new ECNamedDomainParameters(curveOid,
                x9.getCurve(), x9.getG(), x9.getN(), x9.getH(), x9.getSeed()), createSecureRandom()));
        LOG.debug("Generating new EC key pair, curve: {}", curveOid);
        return kpGen.generateKeyPair();
    }

    /* package */ static X509v3CertificateBuilder createDefaultCertificateBuilder(
            final X500Name issuer, final X500Name subject, final AsymmetricKeyParameter subjPubKey) throws IOException {
        final long now = System.currentTimeMillis();
        return new X509v3CertificateBuilder(
                issuer,
                BigInteger.valueOf(now),
                new Date(now - 30 * ONE_DAY_MS),
                new Date(now + 335 * ONE_DAY_MS),
                Locale.ENGLISH,
                subject,
                SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(subjPubKey));
    }

    /* package */ static ContentSigner createDefaultCASigner(final AsymmetricKeyParameter caPrivateKey) {
        final AlgorithmIdentifier sigAlgId =
                new DefaultSignatureAlgorithmIdentifierFinder().find(CA_SIGNER_ALGORITHM_NAME);
        final AlgorithmIdentifier digAlgId =
                new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);

        try {
            return new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(caPrivateKey);
        } catch (final OperatorCreationException e) {
            throw new RuntimeException("Illegal signer algorithm name: \"" + CA_SIGNER_ALGORITHM_NAME + '"', e);
        }
    }

}
