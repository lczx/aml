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

import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x509.*;
import org.spongycastle.cert.X509v3CertificateBuilder;
import org.spongycastle.crypto.AsymmetricCipherKeyPair;

import java.io.IOException;

public final class CACredentialsFactory {

    private CACredentialsFactory() { }

    public static AuthorityCredentials buildLocalCACertificate(final String distinguishedName) throws IOException {
        final AsymmetricCipherKeyPair keyPair = CryptoUtils.generateRSAKeyPair(2048);

        final X509v3CertificateBuilder builder = CryptoUtils.createDefaultCertificateBuilder(
                new X500Name(distinguishedName), new X500Name(distinguishedName), keyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(
                KeyUsage.keyCertSign | KeyUsage.keyEncipherment | KeyUsage.digitalSignature));
        builder.addExtension(Extension.extendedKeyUsage, true, new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
        //builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(new GeneralName(1, "test@test.test")));

        // Create self-signed and return
        return new AuthorityCredentials(
                builder.build(CryptoUtils.createDefaultCASigner(keyPair.getPrivate())),
                CredentialsStoreUtils.toPrivateKeyInfo(keyPair.getPrivate()));
    }

}
