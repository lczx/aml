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

import org.spongycastle.crypto.tls.ExtensionType;
import org.spongycastle.crypto.tls.TlsUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;

public final class TlsProxyUtils {

    private TlsProxyUtils() { }

    public static byte[] readAll(final InputStream inputStream) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        int nRead;
        byte[] buf = new byte[1024];
        while ((nRead = inputStream.read(buf)) != -1) out.write(buf, 0, nRead);
        out.flush();
        return out.toByteArray();
    }

    public static String getNextProtocolName(final Hashtable serverExtensions) throws IOException {
        byte[] alpn = (byte[]) serverExtensions.get(ExtensionType.application_layer_protocol_negotiation);
        if (alpn == null) return null;

        final ByteArrayInputStream in = new ByteArrayInputStream(alpn);
        /*int alpnLen =*/ TlsUtils.readUint16(in);
        final int alpnStrLen = TlsUtils.readUint8(in);
        return  new String(TlsUtils.readFully(alpnStrLen, in), StandardCharsets.UTF_8);
    }

}
