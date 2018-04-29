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

package io.github.lczx.aml.proto.http;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;

public class HttpRequestReaderTest {

    public static final String HTTP_REQ_1_HEADERS = "POST / HTTP/1.1\n" +
            "Host: example.com\n" +
            "User-Agent: Raving Ducks: The Final Cut \n" +
            "Content-Length: 256\n\n";
    public static final String HTTP_REQ_1_BODY = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
            "Nunc at rhoncus velit. Ut pharetra tempor nisi a suscipit. Nulla ultrices sagittis ex, ac mollis " +
            "libero efficitur sit amet. Quisque venenatis fermentum ipsum, quis laoreet neque ultricies quis metus.";
    public static final String HTTP_REQ_2_HEADERS = "POST /kittens HTTP/1.1\r\n" +
            "Host: example.com\r\n" +
            "User-Agent: Trolololol/5.0 (TempleOS 5.03, x86_64) Satan/66.0.1234 SoMuchFunWithRandomUserAgents/3.5\r\n" +
            "Content-Length: 25\r\n" +
            "X-Custom-Field: GIMME FUE GIMME FAI GIMME DABAJABAZA\r\n\r\n";
    public static final String HTTP_REQ_2_BODY = "name=Belphegor&race=tabby";
    public static final String HTTP_REQ_1 = HTTP_REQ_1_HEADERS + HTTP_REQ_1_BODY + HTTP_REQ_2_HEADERS + HTTP_REQ_2_BODY;

    private final ExecutorService exec = Executors.newFixedThreadPool(2);
    private final Map<Future<String>, String> futures = new HashMap<>();

    @Test
    public void requestReaderTest() throws IOException, ExecutionException, InterruptedException {
        final ReadableByteChannel in = Channels.newChannel(
                new ByteArrayInputStream(HTTP_REQ_1.getBytes(StandardCharsets.UTF_8)));
        final ByteBuffer buffer = ByteBuffer.allocate(16);

        final HttpRequestReader reader = new HttpRequestReader();

        int reqIdx = 0;
        while (in.read(buffer) != -1) {
            buffer.flip();
            while (buffer.hasRemaining()) {
                final HttpRequest req = reader.readRequest(buffer);
                if (req != null) handleRequest(reqIdx++, req);
            }
            buffer.clear();
        }

        exec.shutdown();
        exec.awaitTermination(0, TimeUnit.SECONDS);

        assertEquals(2, futures.size());
        for (Map.Entry<Future<String>, String> e : futures.entrySet()) {
            assertEquals(e.getValue(), e.getKey().get());
        }
    }

    private void handleRequest(final int index, final HttpRequest req) {
        assertEquals("HTTP/1.1", req.getVersion());
        assertEquals("example.com", req.getField(HttpRequest.FIELD_HOST));

        if (index == 0) {
            assertEquals("POST", req.getMethod());
            assertEquals("/", req.getPath());
            assertEquals("Raving Ducks: The Final Cut ", req.getField(HttpRequest.FIELD_USER_AGENT));
            assertEquals(HTTP_REQ_1_BODY.length(), Integer.parseInt(req.getField(HttpRequest.FIELD_CONTENT_LENGTH)));
            assertEquals(3, req.getFields().size());
            futures.put(exec.submit(new ContentReader(req.getBody())), HTTP_REQ_1_BODY);

        } else if (index == 1) {
            assertEquals("POST", req.getMethod());
            assertEquals("/kittens", req.getPath());
            assertEquals("Trolololol/5.0 (TempleOS 5.03, x86_64) Satan/66.0.1234 SoMuchFunWithRandomUserAgents/3.5", req.getField(HttpRequest.FIELD_USER_AGENT));
            assertEquals(HTTP_REQ_2_BODY.length(), Integer.parseInt(req.getField(HttpRequest.FIELD_CONTENT_LENGTH)));
            assertEquals("GIMME FUE GIMME FAI GIMME DABAJABAZA", req.getField("X-Custom-Field"));
            assertEquals(4, req.getFields().size());
            futures.put(exec.submit(new ContentReader(req.getBody())), HTTP_REQ_2_BODY);
        }
    }

    private class ContentReader implements Callable<String> {

        private final HttpBodyStream bodyStream;

        private ContentReader(final HttpBodyStream bodyStream) {
            this.bodyStream = bodyStream;
        }

        @Override
        public String call() throws Exception {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final byte[] buffer = new byte[128];

            int count;
            while ((count = bodyStream.getInputStream().read(buffer)) != -1)
                out.write(buffer, 0, count);

            return out.toString("UTF-8");
        }

    }

}
