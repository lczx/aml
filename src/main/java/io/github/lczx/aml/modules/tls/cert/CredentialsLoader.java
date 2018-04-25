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

import io.github.lczx.aml.modules.tls.TlsProxyUtils;
import org.spongycastle.asn1.DERNull;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.asn1.pkcs.RSAPrivateKey;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.util.PrivateKeyFactory;

import java.io.IOException;
import java.io.InputStream;

public final class CredentialsLoader {

    private CredentialsLoader() { }

    public static X509CertificateHolder loadCertificateX509(final InputStream inputStream) throws IOException {
        return new X509CertificateHolder(TlsProxyUtils.readAll(inputStream));
    }

    public static AsymmetricKeyParameter loadPrivateKeyDER(final InputStream inputStream) throws IOException {
        final PrivateKeyInfo p = new PrivateKeyInfo(
                new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE),
                RSAPrivateKey.getInstance(TlsProxyUtils.readAll(inputStream)));
        return PrivateKeyFactory.createKey(p);
    }

}
