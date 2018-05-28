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

import org.spongycastle.asn1.DERNull;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.asn1.pkcs.RSAPrivateKey;
import org.spongycastle.asn1.sec.ECPrivateKey;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x9.X9ObjectIdentifiers;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.params.ECPrivateKeyParameters;
import org.spongycastle.crypto.params.RSAPrivateCrtKeyParameters;

import java.io.IOException;

public final class CredentialsStoreUtils {

    public static X509CertificateHolder parseX509CertificateDER(final byte[] bytes) throws IOException {
        return new X509CertificateHolder(bytes);
    }

    public static byte[] dumpX509CertificateDer(final X509CertificateHolder certificate) throws IOException {
        return certificate.getEncoded();
    }

    public static PrivateKeyInfo parsePKCS8PrivateKeyDER(final byte[] bytes) throws IOException {
        return new PrivateKeyInfo(
                new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE),
                RSAPrivateKey.getInstance(bytes));
    }

    public static PrivateKeyInfo parsePKCS8PrivateECKeyDER(final byte[] bytes) throws IOException {
        return new PrivateKeyInfo(
                new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, X9ObjectIdentifiers.prime256v1),
                ECPrivateKey.getInstance(bytes));
    }

    public static byte[] dumpPKCS8PrivateKeyDER(final PrivateKeyInfo privateKey) throws IOException {
        return privateKey.parsePrivateKey().toASN1Primitive().getEncoded();
    }

    public static PrivateKeyInfo toPrivateKeyInfo(final AsymmetricKeyParameter privateKey) throws IOException {
        try {
            if (privateKey instanceof RSAPrivateCrtKeyParameters) {
                final RSAPrivateCrtKeyParameters pvk = (RSAPrivateCrtKeyParameters) privateKey;
                return new PrivateKeyInfo(
                        new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption),
                        new RSAPrivateKey(pvk.getModulus(), pvk.getPublicExponent(), pvk.getExponent(),
                                pvk.getP(), pvk.getQ(), pvk.getDP(), pvk.getDQ(), pvk.getQInv()));

            } else if (privateKey instanceof ECPrivateKeyParameters) {
                final ECPrivateKeyParameters pvk = (ECPrivateKeyParameters) privateKey;
                return new PrivateKeyInfo(
                        new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey),
                        new ECPrivateKey(pvk.getD().bitLength(), pvk.getD()));

            }
        } catch (final IOException e) {
            throw new IOException("Unexpected error while generating PrivateKeyInfo structure");
        }

        throw new IOException("Unrecognized private key type " + privateKey.getClass().getName());
    }

    private CredentialsStoreUtils() { }

}
