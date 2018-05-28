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

import org.spongycastle.crypto.tls.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;

/* package */ final class TlsProxyUtils {

    private TlsProxyUtils() { }

    /* package */ static byte[] readAll(final InputStream inputStream) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        int nRead;
        byte[] buf = new byte[1024];
        while ((nRead = inputStream.read(buf)) != -1) out.write(buf, 0, nRead);
        out.flush();
        return out.toByteArray();
    }

    /* package */ static String getServerName(final Hashtable clientExtensions) throws IOException {
        final byte[] sni = (byte[]) clientExtensions.get(ExtensionType.server_name);
        if (sni == null) return null;

        try {
            final ServerNameList snl = TlsExtensionsUtils.getServerNameExtension(clientExtensions);
            return ((ServerName) snl.getServerNameList().get(0)).getHostName();
        } catch (final IllegalStateException e) {
            return null;
        }
    }

    /* package */ static String getNextProtocolName(final Hashtable serverExtensions) throws IOException {
        final byte[] alpn = (byte[]) serverExtensions.get(ExtensionType.application_layer_protocol_negotiation);
        if (alpn == null) return null;

        final ByteArrayInputStream in = new ByteArrayInputStream(alpn);
        TlsUtils.readUint16(in); // <-- whole ALPN extension length
        final int alpnStrLen = TlsUtils.readUint8(in);
        return new String(TlsUtils.readFully(alpnStrLen, in), StandardCharsets.UTF_8);
    }

}
