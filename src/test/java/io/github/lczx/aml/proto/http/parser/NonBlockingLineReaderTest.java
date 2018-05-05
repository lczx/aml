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

package io.github.lczx.aml.proto.http.parser;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class NonBlockingLineReaderTest {

    private static final String CRLF = "\r\n";
    private static final String[] L = {
            "POST / HTTP/1.1",
            "Host: example.com",
            "Content-Type: application/x-www-form-urlencoded",
            "Content-Length: 17",
            ""};
    private static final String CNT = "say=Hi&to=Example";

    private static final String LINES = L[0] + CRLF + L[1] + CRLF + L[2] + CRLF + L[3] + CRLF + CRLF + CNT;

    @Test
    public void readLinesTest() throws IOException {
        final ByteArrayInputStream ins = new ByteArrayInputStream(LINES.getBytes(StandardCharsets.UTF_8));
        final ReadableByteChannel in = Channels.newChannel(ins);
        final NonBlockingLineReader lr = new NonBlockingLineReader();
        final ByteBuffer buf = ByteBuffer.allocate(16);

        int i = 0;
        while (in.read(buf) != -1) {
            buf.flip();

            while (buf.hasRemaining()) {
                final String line = lr.readLine(buf);
                if (line != null)
                    assertEquals(L[i++], line);
            }
            buf.clear();
            //buf.compact();
        }

        // EOS reached, get the last unterminated line
        assertEquals(CNT, lr.getLastLine());
    }

}
