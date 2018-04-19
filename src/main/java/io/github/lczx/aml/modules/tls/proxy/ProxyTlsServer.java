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

package io.github.lczx.aml.modules.tls.proxy;

import org.spongycastle.crypto.tls.CipherSuite;
import org.spongycastle.crypto.tls.TlsSignerCredentials;
import org.spongycastle.util.Arrays;

import java.io.IOException;

public class ProxyTlsServer extends TlsServerBase {

    @Override
    protected int[] getCipherSuites() {
        // TODO: Use cipher suites negotiated upstream

        return Arrays.concatenate(new int[]{
                CipherSuite.DRAFT_TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256
        }, super.getCipherSuites());

        /* // If ECC key:
        return new int[] {
                CipherSuite.DRAFT_TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256,
                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384,
                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256};*/
    }

    @Override
    protected TlsSignerCredentials getRSASignerCredentials() throws IOException {
        // TODO: Implement certificate forging with RSA keys
        return null;
    }

    @Override
    protected TlsSignerCredentials getECDSASignerCredentials() throws IOException {
        // TODO: Implement certificate forging with ECC keys
        return null;
    }
}
