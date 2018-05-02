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

import io.github.lczx.aml.proto.http.parser.HttpRequestHeaderReader;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import static io.github.lczx.aml.proto.http.HttpMessageSamples.*;
import static io.github.lczx.aml.proto.http.HttpTestUtils.ContentReader;
import static io.github.lczx.aml.proto.http.HttpTestUtils.ContentReaderResult;
import static org.junit.Assert.*;

public class HttpRequestReaderTest {

    private static final String HTTP_REQ_STREAM =
            Req1.VALUE + Req2.VALUE + Req3.VALUE + Req4.VALUE + Req5.VALUE + Req6.VALUE;

    private final ExecutorService exec = Executors.newFixedThreadPool(2);
    private final Map<Future<ContentReaderResult>, String> futures = new HashMap<>();

    @Test
    public void requestReaderTest() throws IOException, ExecutionException, InterruptedException {
        final ReadableByteChannel in = Channels.newChannel(
                new ByteArrayInputStream(HTTP_REQ_STREAM.getBytes(StandardCharsets.UTF_8)));
        final ByteBuffer buffer = ByteBuffer.allocate(16); // Keep small to maximize "incomplete parsing" statuses

        final HttpMessageStreamReader<HttpRequest> reader =
                new HttpMessageStreamReader<>(new HttpRequestHeaderReader());

        int reqIdx = 0;
        int actReqIdx = 0;
        System.out.println("REM\tPENDING\tMSG");
        while (in.read(buffer) != -1) {
            buffer.flip();
            while (buffer.hasRemaining()) {
                final HttpRequest req = reader.readMessage(buffer);
                if (!reader.hasPendingMessage()) actReqIdx++;
                System.out.printf("%d\t%b\t%s\n", buffer.remaining(), reader.hasPendingMessage(), req);

                if (req == null && reader.hasPendingMessage())
                    assertFalse("Buffer should be fully consumed if no result " +
                            "was given and is processing a message", buffer.hasRemaining());

                if (req != null) handleRequest(reqIdx++, req);
            }
            buffer.clear();
        }

        reader.close();
        exec.shutdown();
        exec.awaitTermination(0, TimeUnit.SECONDS);

        assertEquals(reqIdx, actReqIdx + 1 /* because last message was interrupted */);
        assertEquals(reqIdx, futures.size());
        checkResults();
    }

    private void handleRequest(final int index, final HttpRequest req) {
        final HttpRequestHeader h = req.getHeader();
        assertEquals(HTTP_VERSION, h.getVersion());
        assertEquals(HTTP_HOST, h.getField(HttpRequestHeader.FIELD_HOST));

        if (index == Req1.IDX) {
            assertEquals(Req1.METHOD, h.getMethod());
            assertEquals(Req1.PATH, h.getPath());
            assertEquals(Req1.FIELD_COUNT, h.getFields().size());

            assertEquals(Req1.UA, h.getField(HttpRequestHeader.FIELD_USER_AGENT));
            assertEquals(Req1.BODY.length(), Integer.parseInt(h.getField(HttpHeader.FIELD_CONTENT_LENGTH)));

            futures.put(exec.submit(new ContentReader(req.getBody())), Req1.BODY);

        } else if (index == Req2.IDX) {
            assertEquals(Req2.METHOD, h.getMethod());
            assertEquals(Req2.PATH, h.getPath());
            assertEquals(Req2.FIELD_COUNT, h.getFields().size());

            assertEquals(Req2.UA, h.getField(HttpRequestHeader.FIELD_USER_AGENT));

            futures.put(exec.submit(new ContentReader(req.getBody())), null);

        } else if (index == Req3.IDX) {
            assertEquals(Req3.METHOD, h.getMethod());
            assertEquals(Req3.PATH, h.getPath());
            assertEquals(Req3.FIELD_COUNT, h.getFields().size());

            assertEquals(Req3.UA, h.getField(HttpRequestHeader.FIELD_USER_AGENT));
            assertEquals(Req3.BODY.length(), Integer.parseInt(h.getField(HttpHeader.FIELD_CONTENT_LENGTH)));
            assertEquals(Req3.CUSTOM_VALUE, h.getField(Req3.CUSTOM_KEY));

            futures.put(exec.submit(new ContentReader(req.getBody())), Req3.BODY);

        } else if (index == Req4.IDX) {
            assertEquals(Req4.METHOD, h.getMethod());
            assertEquals(Req4.PATH, h.getPath());
            assertEquals(Req4.FIELD_COUNT, h.getFields().size());

            assertEquals(Req4.UA, h.getField(HttpRequestHeader.FIELD_USER_AGENT));

            futures.put(exec.submit(new ContentReader(req.getBody())), null);

        } else if (index == Req5.IDX) {
            assertEquals(Req5.METHOD, h.getMethod());
            assertEquals(Req5.PATH, h.getPath());
            assertEquals(Req5.FIELD_COUNT, h.getFields().size());

            assertEquals(Req5.TE_VALUE, h.getField(Req5.TE_KEY));
            assertEquals(Req5.TRANSFER_ENCODING, h.getField(HttpResponseHeader.FIELD_TRANSFER_ENCODING));
            assertEquals(Req5.TRAILER_VALUES[0], h.getField(Req5.TRAILER_KEY));
            assertArrayEquals(Req5.TRAILER_VALUES, h.getFields(Req5.TRAILER_KEY));

            futures.put(exec.submit(new ContentReader(req.getBody())), Req5.BODY);

        } else if (index == Req6.IDX) {
            assertEquals(Req6.METHOD, h.getMethod());
            assertEquals(Req6.PATH, h.getPath());
            assertEquals(Req6.FIELD_COUNT, h.getFields().size());

            assertEquals(Req6.CUSTOM_VALUE, h.getField(Req6.CUSTOM_KEY));
            futures.put(exec.submit(new ContentReader(req.getBody())), Req6.BODY);
        }
    }

    private void checkResults() throws ExecutionException, InterruptedException {
        for (final Map.Entry<Future<ContentReaderResult>, String> e : futures.entrySet()) {
            final ContentReaderResult result = e.getKey().get();

            if (e.getValue() == null)
                assertEquals(0, result.data.length);
            else {
                assertArrayEquals(e.getValue().getBytes(StandardCharsets.UTF_8), result.data);
                if (result.trailingHeaders != null)
                    assertEquals(Req5.TRAILING_HEADERS, result.trailingHeaders);
            }
        }
    }

}
