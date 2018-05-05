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

import io.github.lczx.aml.proto.http.HttpMessageSamples.Ans1;
import io.github.lczx.aml.proto.http.HttpMessageSamples.Req1;
import io.github.lczx.aml.proto.http.HttpMessageSamples.Req4;
import io.github.lczx.aml.proto.http.model.HttpRequest;
import io.github.lczx.aml.proto.http.model.HttpRequestHeader;
import io.github.lczx.aml.proto.http.model.HttpResponse;
import io.github.lczx.aml.proto.http.model.HttpResponseHeader;
import io.github.lczx.aml.proto.http.stream.NullBodyStream;
import io.github.lczx.aml.proto.http.stream.SizedBodyStream;
import org.junit.Test;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class HttpCoordinationTest {

    private static final int CHUNK_SIZE = 8;

    @Test
    public void httpCoordinationTest() throws IOException {
        final TestLogger testLogger = TestLoggerFactory.getTestLogger(HttpSessionAnalyzer.class);
        TestLoggerFactory.getInstance().setPrintLevel(Level.DEBUG);
        testLogger.setEnabledLevels(Level.WARN);

        final MessageListener messageListener = new MessageListener();
        final HttpSessionAnalyzer analyzer = new HttpSessionAnalyzer(messageListener);

        final ByteBuffer req1 = ByteBuffer.wrap(Req4.VALUE.getBytes(StandardCharsets.UTF_8));      // HEAD
        final ByteBuffer ans1 = ByteBuffer.wrap(Ans1.VALUE_HEAD.getBytes(StandardCharsets.UTF_8)); // 200 OK (no body)
        final ByteBuffer req2 = ByteBuffer.wrap(Req1.VALUE.getBytes(StandardCharsets.UTF_8));      // POST
        final ByteBuffer ans2 = ByteBuffer.wrap(Ans1.VALUE.getBytes(StandardCharsets.UTF_8));      // 200 OK

        final ChunkReceiver uplinkReceiver = new ChunkReceiver() {
            @Override
            public void onChunk(final ByteBuffer chunk) throws IOException {
                analyzer.receiveUplink(chunk);
            }
        };
        final ChunkReceiver downlinkReceiver = new ChunkReceiver() {
            @Override
            public void onChunk(final ByteBuffer chunk) throws IOException {
                analyzer.receiveDownlink(chunk);
            }
        };

        processInChunks(req1, uplinkReceiver);
        processInChunks(ans1, downlinkReceiver);
        processInChunks(req2, uplinkReceiver);
        processInChunks(ans2, downlinkReceiver);
        analyzer.close();

        assertEquals("No warnings should have been thrown", 0, testLogger.getLoggingEvents().size());
        assertEquals(2, messageListener.requestCount);
        assertEquals(2, messageListener.responseCount);
    }

    private void processInChunks(final ByteBuffer reqBuffer, final ChunkReceiver receiver) throws IOException {
        final ByteBuffer buf = ByteBuffer.allocate(CHUNK_SIZE);
        while (reqBuffer.hasRemaining()) {
            final int transferSize = Math.min(buf.remaining(), reqBuffer.remaining());
            buf.put((ByteBuffer) reqBuffer.duplicate().limit(reqBuffer.position() + transferSize));
            reqBuffer.position(reqBuffer.position() + transferSize);
            buf.flip();
            receiver.onChunk(buf);
            buf.clear();
        }
    }

    private interface ChunkReceiver {
        void onChunk(ByteBuffer chunk) throws IOException;
    }

    private class MessageListener implements HttpSessionAnalyzer.MessageCallback {
        private int requestCount = 0;
        private int responseCount = 0;

        @Override
        public void onRequest(final HttpRequest request) {
            final HttpRequestHeader h = request.getHeader();
            if (requestCount == 0) {
                assertEquals(Req4.METHOD, h.getMethod());
                assertEquals(Req4.UA, h.getField(HttpRequestHeader.FIELD_USER_AGENT));
                assertTrue("HEAD request should have no body",request.getBody() instanceof NullBodyStream);
            } else if (requestCount == 1) {
                assertEquals(Req1.METHOD, h.getMethod());
                assertEquals(Req1.UA, h.getField(HttpRequestHeader.FIELD_USER_AGENT));
                assertTrue("POST should have sized body", request.getBody() instanceof SizedBodyStream);
            } else {
                fail("Too many requests");
            }
            requestCount++;
        }

        @Override
        public void onResponse(final HttpResponse response) {
            final HttpResponseHeader h = response.getHeader();
            assertEquals(Ans1.STATUS_CODE, h.getStatusCode());
            assertEquals(Ans1.SERVER, h.getField(HttpResponseHeader.FIELD_SERVER));
            if (responseCount == 0) {
                assertTrue("HEAD response should have no body", response.getBody() instanceof NullBodyStream);
            } else if (responseCount == 1) {
                assertTrue("GET/POST response should be sized", response.getBody() instanceof SizedBodyStream);
            } else {
                fail("Too many responses");
            }
            responseCount++;
        }
    }

}
