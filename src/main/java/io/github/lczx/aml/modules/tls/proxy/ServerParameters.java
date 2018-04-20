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

import org.spongycastle.crypto.tls.Certificate;
import org.spongycastle.crypto.tls.ProtocolVersion;

import java.util.Hashtable;

public class ServerParameters {

    public final ProtocolVersion protocolVersion;
    public final int cipherSuite;
    public final short compressionMethod;
    public final Hashtable extensions;
    public final Certificate originalCertificate;

    public ServerParameters(final ProtocolVersion protocolVersion, final int cipherSuite, final short compressionMethod,
                            final Hashtable extensions, final Certificate originalCertificate) {
        this.protocolVersion = protocolVersion;
        this.cipherSuite = cipherSuite;
        this.compressionMethod = compressionMethod;
        this.extensions = extensions;
        this.originalCertificate = originalCertificate;
    }

    @Override
    public String toString() {
        return "ServerParameters{" +
                "protocolVersion=" + protocolVersion +
                ", cipherSuite=" + cipherSuite +
                ", compressionMethod=" + compressionMethod +
                ", extensions=" + extensions +
                ", originalCertificate=" + originalCertificate +
                '}';
    }

}
