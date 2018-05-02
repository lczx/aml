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

import io.github.lczx.aml.proto.http.HttpTestUtils.ContentReader;
import io.github.lczx.aml.proto.http.HttpTestUtils.ContentReaderResult;
import io.github.lczx.aml.proto.http.model.HttpHeader;
import io.github.lczx.aml.proto.http.model.HttpRequestHeader;
import io.github.lczx.aml.proto.http.model.HttpResponse;
import io.github.lczx.aml.proto.http.model.HttpResponseHeader;
import io.github.lczx.aml.proto.http.parser.HttpResponseHeaderReader;
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

import static io.github.lczx.aml.proto.http.HttpMessageSamples.Ans1;
import static io.github.lczx.aml.proto.http.HttpMessageSamples.HTTP_VERSION;
import static org.junit.Assert.*;

public class HttpResponseReaderTest {

    private static final String HTTP_ANS_STREAM = Ans1.VALUE_HEAD + Ans1.VALUE;

    private final ExecutorService exec = Executors.newFixedThreadPool(1);
    private final Map<Future<ContentReaderResult>, String> futures = new HashMap<>();

    @Test
    public void responseReaderTest() throws IOException, InterruptedException, ExecutionException {
        final ReadableByteChannel in = Channels.newChannel(
                new ByteArrayInputStream(HTTP_ANS_STREAM.getBytes(StandardCharsets.UTF_8)));
        final ByteBuffer buffer = ByteBuffer.allocate(16); // Keep small to maximize "incomplete parsing" statuses

        final HttpMessageStreamReader<HttpResponse> reader =
                new HttpMessageStreamReader<>(new HttpResponseHeaderReader());

        int ansIdx = 0;
        int actAnsIdx = 0; // Last message is closing
        System.out.println("REM\tPENDING\tMSG");
        while (in.read(buffer) != -1) {
            buffer.flip();
            while (buffer.hasRemaining()) {
                final HttpResponse ans = reader.readMessage(buffer);
                if (!reader.hasPendingMessage()) actAnsIdx++;
                System.out.printf("%d\t%b\t%s\n", buffer.remaining(), reader.hasPendingMessage(), ans);

                if (ans == null && reader.hasPendingMessage())
                    assertFalse("Buffer should be fully consumed if no result " +
                            "was given and is processing a message", buffer.hasRemaining());

                if (ans != null) handleResponse(ansIdx++, ans);
            }
            buffer.clear();
        }

        reader.close();
        exec.shutdown();
        exec.awaitTermination(0, TimeUnit.SECONDS);

        assertEquals(ansIdx, actAnsIdx);
        assertEquals(ansIdx, futures.size());
        checkResults();
    }

    private void handleResponse(final int index, final HttpResponse ans) {
        final HttpResponseHeader h = ans.getHeader();
        assertEquals(Ans1.STATUS_CODE, h.getStatusCode());
        assertEquals(Ans1.STATUS_MESSAGE, h.getStatusDescription());
        assertEquals(Ans1.FIELD_COUNT, h.getFields().size());
        assertEquals(Ans1.SERVER, h.getField(HttpResponseHeader.FIELD_SERVER));
        assertEquals(Ans1.BODY.length(), Integer.parseInt(h.getField(HttpHeader.FIELD_CONTENT_LENGTH)));

        if (index == 0) {
            // This is the response to HEAD, let it be before attempting to read body
            ans.setRequestHeader(new HttpRequestHeader("HEAD", "/", HTTP_VERSION));
            futures.put(exec.submit(new ContentReader(ans.getBody())), null);

        } else if (index == 1) {
            futures.put(exec.submit(new ContentReader(ans.getBody())), Ans1.BODY);
        }
    }

    private void checkResults() throws ExecutionException, InterruptedException {
        for (final Map.Entry<Future<ContentReaderResult>, String> e : futures.entrySet()) {
            final ContentReaderResult result = e.getKey().get();

            if (e.getValue() == null)
                assertEquals(0, result.data.length);
            else
                assertArrayEquals(e.getValue().getBytes(StandardCharsets.UTF_8), result.data);
        }
    }

}
