package io.github.lczx.aml.modules.tls.cert;

import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.cert.X509CertificateHolder;

public class AuthorityCredentials {

    public final X509CertificateHolder certificate;
    public final PrivateKeyInfo privateKey;

    public AuthorityCredentials(final X509CertificateHolder certificate, final PrivateKeyInfo privateKey) {
        this.certificate = certificate;
        this.privateKey = privateKey;
    }

}
