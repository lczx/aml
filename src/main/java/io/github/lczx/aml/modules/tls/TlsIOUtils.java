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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.tls.TlsProtocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class TlsIOUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TlsIOUtils.class);

    private TlsIOUtils() { }

    public static byte[] readAll(final InputStream inputStream) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        int nRead;
        byte[] buf = new byte[1024];
        while ((nRead = inputStream.read(buf)) != -1) out.write(buf, 0, nRead);
        out.flush();
        return out.toByteArray();
    }

    public static void safeClose(TlsProtocol... protocols) {
        for (final TlsProtocol t : protocols) {
            try {
                t.close();
            } catch (final IOException e) {
                LOG.error("Error while closing " + t, e);
            }
        }
    }

}
