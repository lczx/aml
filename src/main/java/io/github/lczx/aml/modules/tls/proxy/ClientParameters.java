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

import org.spongycastle.crypto.tls.ProtocolVersion;

import java.util.Arrays;
import java.util.Hashtable;

public class ClientParameters {

    public final ProtocolVersion protocolVersion;
    public final int[] cipherSuites;
    public final short[] compressionMethods;
    public final Hashtable extensions;

    public ClientParameters(final ProtocolVersion protocolVersion, final int[] cipherSuites,
                            final short[] compressionMethods, final Hashtable extensions) {
        this.protocolVersion = protocolVersion;
        this.cipherSuites = cipherSuites;
        this.compressionMethods = compressionMethods;
        this.extensions = extensions;
    }

    @Override
    public String toString() {
        return "ClientParameters{" +
                "protocolVersion=" + protocolVersion +
                ", cipherSuites=" + Arrays.toString(cipherSuites) +
                ", compressionMethods=" + Arrays.toString(compressionMethods) +
                ", extensions=" + extensions +
                '}';
    }

}
