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

package io.github.lczx.aml.modules.tls;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.security.KeyChain;
import io.github.lczx.aml.modules.tls.cert.AuthorityCredentials;
import io.github.lczx.aml.modules.tls.cert.CACredentialsFactory;
import io.github.lczx.aml.modules.tls.cert.CredentialsStoreUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;

public final class ProxyModuleHelper {

    public static final String DEFAULT_CA_DISTINGUISHED_NAME = "CN=AML Local Root Authority";

    public static final String CA_CERTIFICATE_LOCATION = "aml/proxy/ca.crt";
    public static final String CA_PRIVATE_KEY_LOCATION = "aml/proxy/ca.key";

    private static final Logger LOG = LoggerFactory.getLogger(ProxyModuleHelper.class);

    private static AuthorityCredentials caCredentials;

    private ProxyModuleHelper() { }

    public static boolean hasLoadedCredentials() {
        return caCredentials != null;
    }

    public static boolean hasStoredCredentials(final Context context) {
        return new File(context.getFilesDir(), CA_CERTIFICATE_LOCATION).isFile() &&
                new File(context.getFilesDir(), CA_PRIVATE_KEY_LOCATION).isFile();
    }

    public static void loadCredentials(final Context context) throws IOException {
        final FileInputStream in1 = new FileInputStream(new File(context.getFilesDir(), CA_CERTIFICATE_LOCATION));
        final FileInputStream in2 = new FileInputStream(new File(context.getFilesDir(), CA_PRIVATE_KEY_LOCATION));
        caCredentials = new AuthorityCredentials(
                CredentialsStoreUtils.parseX509CertificateDER(TlsProxyUtils.readAll(in1)),
                CredentialsStoreUtils.parsePKCS8PrivateKeyDER(TlsProxyUtils.readAll(in2)));
        in1.close();
        in2.close();
    }

    public static void storeCredentials(final Context context) throws IOException {
        if (!hasLoadedCredentials()) throw new IllegalStateException("No credentials to store");
        FileOutputStream out;

        final File pvkFile = new File(context.getFilesDir(), CA_PRIVATE_KEY_LOCATION);
        pvkFile.getParentFile().mkdirs();
        pvkFile.createNewFile();
        out = new FileOutputStream(pvkFile);
        out.write(CredentialsStoreUtils.dumpPKCS8PrivateKeyDER(caCredentials.privateKey));
        out.close();

        final File crtFile = new File(context.getFilesDir(), CA_CERTIFICATE_LOCATION);
        crtFile.getParentFile().mkdirs();
        crtFile.createNewFile();
        out = new FileOutputStream(crtFile);
        out.write(CredentialsStoreUtils.dumpX509CertificateDer(caCredentials.certificate));
        out.close();
    }

    public static boolean isCAInstalled() {
        if (!hasLoadedCredentials()) throw new IllegalStateException("No credentials loaded");

        try {
            final KeyStore ks = KeyStore.getInstance("AndroidCAStore");
            ks.load(null, null);

            final Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                final String alias = aliases.nextElement();
                final Certificate installedCert = ks.getCertificate(alias);
                if (installedCert instanceof X509Certificate &&
                        Arrays.equals(((X509Certificate) installedCert).getSignature(),
                                caCredentials.certificate.getSignature()))
                    return true;
            }
        } catch (final IOException | GeneralSecurityException e) {
            LOG.error("Error while attempting to read device keystore", e);
        }
        return false;
    }

    public static void createNewAuthority(final String distinguishedName) throws IOException {
        caCredentials = CACredentialsFactory.buildLocalCACertificate(distinguishedName);
    }

    public static Intent createInstallIntent(final CharSequence alias) throws IOException {
        return KeyChain.createInstallIntent()
                .putExtra(KeyChain.EXTRA_NAME, alias)
                .putExtra(KeyChain.EXTRA_CERTIFICATE, CredentialsStoreUtils.dumpX509CertificateDer(caCredentials.certificate));
    }

    public static Bundle createModuleParameters() {
        if (!hasLoadedCredentials()) throw new IllegalStateException("No credentials loaded");

        try {
            final Bundle params = new Bundle();
            params.putByteArray(TlsProxy.PARAM_CA_CERTIFICATE,
                    CredentialsStoreUtils.dumpX509CertificateDer(caCredentials.certificate));
            params.putByteArray(TlsProxy.PARAM_CA_PRIVATE_KEY,
                    CredentialsStoreUtils.dumpPKCS8PrivateKeyDER(caCredentials.privateKey));
            return params;
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
