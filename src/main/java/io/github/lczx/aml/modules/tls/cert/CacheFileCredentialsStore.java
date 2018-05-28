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

import io.github.lczx.aml.AMLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.x9.X9ObjectIdentifiers;
import org.spongycastle.crypto.tls.Certificate;
import org.spongycastle.crypto.tls.SignatureAlgorithm;
import org.spongycastle.crypto.util.PrivateKeyFactory;

import java.io.*;

public class CacheFileCredentialsStore implements PersistentCredentialsStore {

    private static final Logger LOG = LoggerFactory.getLogger(PersistentCredentialsStore.class);

    private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();
    private static final String PROXY_CERTS_DIR = "aml_proxy_certs";
    private static final int IDENTIFIER_LENGTH = 12;

    private final File storeDir;

    public CacheFileCredentialsStore(final AMLContext amlContext) {
        storeDir = new File(amlContext.getServiceContext().getCacheDir(), PROXY_CERTS_DIR);
        if (storeDir.isFile())
            throw new IllegalStateException(storeDir.getPath() + " is a file!");
        if (!storeDir.exists()) storeDir.mkdirs();
    }

    public void storeCredentials(final byte[] identifier, final ServerCredentials forgedCredentials) {
        try {
            final String nameBase = bytesToHexString(identifier, IDENTIFIER_LENGTH);
            final File crtFile = new File(storeDir, nameBase + "_c");
            final File keyFile = new File(storeDir, nameBase + "_k");

            crtFile.createNewFile(); // Ignore if the file already exists
            keyFile.createNewFile();

            final FileOutputStream crtOut = new FileOutputStream(crtFile, false);
            forgedCredentials.getCertificate().encode(crtOut);
            crtOut.close();

            final FileOutputStream keyOut = new FileOutputStream(keyFile, false);
            keyOut.write(CredentialsStoreUtils.dumpPKCS8PrivateKeyDER(
                    CredentialsStoreUtils.toPrivateKeyInfo(forgedCredentials.getPrivateKey())));
            keyOut.close();
        } catch (final IOException e) {
            LOG.error("Failed to write credentials to cache", e);
        }
    }

    public ServerCredentials loadCredentials(final byte[] identifier) {
        try {
            final String nameBase = bytesToHexString(identifier, IDENTIFIER_LENGTH);
            final File crtFile = new File(storeDir, nameBase + "_c");
            final File keyFile = new File(storeDir, nameBase + "_k");
            if (!(crtFile.isFile() && keyFile.isFile())) return null;

            final FileInputStream crtIn = new FileInputStream(crtFile);
            final Certificate cert = Certificate.parse(crtIn);
            crtIn.close();

            final DataInputStream keyIn = new DataInputStream(new FileInputStream(keyFile));
            final byte[] pvkRaw = new byte[keyIn.available()];
            keyIn.readFully(pvkRaw);
            keyIn.close();

            final ASN1ObjectIdentifier keyAlgorithm =
                    cert.getCertificateAt(0).getSubjectPublicKeyInfo().getAlgorithm().getAlgorithm();

            if (keyAlgorithm.equals(PKCSObjectIdentifiers.rsaEncryption)) {
                return new ServerCredentials(SignatureAlgorithm.rsa, cert,
                        PrivateKeyFactory.createKey(CredentialsStoreUtils.parsePKCS8PrivateKeyDER(pvkRaw)));
            } else if (keyAlgorithm.equals(X9ObjectIdentifiers.id_ecPublicKey)) {
                return new ServerCredentials(SignatureAlgorithm.ecdsa, cert,
                        PrivateKeyFactory.createKey(CredentialsStoreUtils.parsePKCS8PrivateECKeyDER(pvkRaw)));
            } else
                throw new IOException("Unknown key algorithm");

        } catch (final IOException e) {
            LOG.error("Failed to read credentials from cache", e);
            return null;
        }
    }

    private String bytesToHexString(final byte[] bytes, final int limit) {
        final int count = Math.min(bytes.length, limit);
        final char[] hex = new char[2 * count];
        for (int i = 0; i < count; ++i) {
            final int v = bytes[i] & 0xFF;
            hex[i * 2] = HEX_CHARS[v >> 4];
            hex[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hex);
    }

}
