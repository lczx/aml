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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class HttpRequestReaderTest {

    private static final String HTTP_REQ_1_HEADERS = "POST / HTTP/1.1\n" +
            "Host: example.com\n" +
            "User-Agent: Raving Ducks: The Final Cut \n" +
            "Content-Length: 256\n\n";
    private static final String HTTP_REQ_1_BODY = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
            "Nunc at rhoncus velit. Ut pharetra tempor nisi a suscipit. Nulla ultrices sagittis ex, ac mollis " +
            "libero efficitur sit amet. Quisque venenatis fermentum ipsum, quis laoreet neque ultricies quis metus.";

    private static final String HTTP_REQ_2_HEADERS = "POST /kittens HTTP/1.1\r\n" +
            "Host: example.com\r\n" +
            "User-Agent: Trolololol/5.0 (TempleOS 5.03, x86_64) Satan/66.0.1234 SoMuchFunWithRandomUserAgents/3.5\r\n" +
            "Content-Length: 25\r\n" +
            "X-Custom-Field: GIMME FUE GIMME FAI GIMME DABAJABAZA\r\n\r\n";
    private static final String HTTP_REQ_2_BODY = "name=Belphegor&race=tabby";

    private static final String HTTP_REQ_3_HEADERS = "FAKE /lol HTTP/1.1\r\n" +
            "Host: example.com\r\n" +
            "TE: trailers\r\n" +
            "Trailer: X-Custom-Field\r\n" +
            "Trailer: X-Funny-Trailer\r\n" +
            "Transfer-Encoding: chunked\r\n\r\n";
    private static final String HTTP_REQ_3_BODY = "0D\r\n" +
            "This is a nic\r\n" +
            "1C\r\n" +
            "e chunked body stream made e\r\n" +
            "52\r\n" +
            "specially for this weird test, I have to make this chunk 82 bytes long to blablabl\r\n" +
            "3\r\n" +
            "a\r\n\r\n" +
            "00\r\n" +
            "X-Custom-Field: Whoa!\r\n" +
            "X-Funny-Trailer: yes\r\n" +
            "\r\n";
    private static final String HTTP_REQ_3_BODY_CLEAR = "This is a nice chunked body stream made especially for this" +
            " weird test, I have to make this chunk 82 bytes long to blablabla\r\n";

    private static final String HTTP_REQ_STREAM = HTTP_REQ_1_HEADERS + HTTP_REQ_1_BODY +
            HTTP_REQ_2_HEADERS + HTTP_REQ_2_BODY + HTTP_REQ_3_HEADERS + HTTP_REQ_3_BODY;

    private final ExecutorService exec = Executors.newFixedThreadPool(2);
    private final Map<Future<ContentReaderResult>, String> futures = new HashMap<>();

    @Test
    public void requestReaderTest() throws IOException, ExecutionException, InterruptedException {
        final ReadableByteChannel in = Channels.newChannel(
                new ByteArrayInputStream(HTTP_REQ_STREAM.getBytes(StandardCharsets.UTF_8)));
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

        assertEquals(reqIdx, futures.size());
        for (Map.Entry<Future<ContentReaderResult>, String> e : futures.entrySet()) {
            final ContentReaderResult result = e.getKey().get();

            assertArrayEquals(e.getValue().getBytes(StandardCharsets.UTF_8), result.data);
            if (result.trailingHeaders != null) {
                assertEquals(Arrays.asList(
                        new HttpHeader.Field("X-Custom-Field", "Whoa!"),
                        new HttpHeader.Field("X-Funny-Trailer", "yes")), result.trailingHeaders);
            }
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

        } else if (index == 2) {
            assertEquals("FAKE", req.getMethod());
            assertEquals("/lol", req.getPath());
            assertEquals("trailers", req.getField("TE"));
            assertEquals("chunked", req.getField(HttpRequest.FIELD_TRANSFER_ENCODING));
            assertEquals("X-Custom-Field", req.getField("Trailer"));
            assertArrayEquals(new String[]{"X-Custom-Field", "X-Funny-Trailer"}, req.getFields("Trailer"));
            futures.put(exec.submit(new ContentReader(req.getBody())), HTTP_REQ_3_BODY_CLEAR);
        }
    }


    private static class ContentReaderResult {
        public byte[] data;
        public List<HttpHeader.Field> trailingHeaders;

        public ContentReaderResult(final byte[] data, final List<HttpHeader.Field> trailingHeaders) {
            this.data = data;
            this.trailingHeaders = trailingHeaders;
        }
    }


    private class ContentReader implements Callable<ContentReaderResult> {

        private final HttpBodyStream bodyStream;

        private ContentReader(final HttpBodyStream bodyStream) {
            this.bodyStream = bodyStream;
        }

        @Override
        public ContentReaderResult call() throws Exception {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final byte[] buffer = new byte[128];

            int count;
            while ((count = bodyStream.getInputStream().read(buffer)) != -1)
                out.write(buffer, 0, count);

            return new ContentReaderResult(out.toByteArray(), bodyStream instanceof ChunkedBodyStream ?
                    ((ChunkedBodyStream) bodyStream).getTrailingHeaders(true) : null);
        }

    }

}
