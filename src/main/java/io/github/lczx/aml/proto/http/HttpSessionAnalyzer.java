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

import io.github.lczx.aml.proto.http.model.HttpRequest;
import io.github.lczx.aml.proto.http.model.HttpRequestHeader;
import io.github.lczx.aml.proto.http.model.HttpResponse;
import io.github.lczx.aml.proto.http.parser.HttpRequestHeaderReader;
import io.github.lczx.aml.proto.http.parser.HttpResponseHeaderReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public class HttpSessionAnalyzer implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(HttpSessionAnalyzer.class);

    private final HttpMessageStreamReader<HttpRequest> requestReader =
            new HttpMessageStreamReader<>(new HttpRequestHeaderReader());
    private final HttpMessageStreamReader<HttpResponse> responseReader =
            new HttpMessageStreamReader<>(new HttpResponseHeaderReader());
    private final MessageCallback callback;

    private boolean receiving = false;
    private HttpRequestHeader lastRequestHeader;

    public HttpSessionAnalyzer(final MessageCallback callback) {
        this.callback = callback;
    }

    public void receiveUplink(final ByteBuffer buffer) throws IOException {
        if (receiving)
            LOG.warn("Got request data while expecting a response");
        while (buffer.hasRemaining()) {
            final HttpRequest req = requestReader.readMessage(buffer);
            if (req != null) {
                lastRequestHeader = req.getHeader();
                callback.onRequest(req);
                // Read body while buffer.hasRemaining()
            }
            if (!requestReader.hasPendingMessage()) {
                receiving = true;
                if (buffer.hasRemaining())
                    LOG.warn("Request ended but buffer has more content");
                break;
            }
        }
    }

    public void receiveDownlink(final ByteBuffer buffer) throws IOException {
        if (!receiving)
            LOG.warn("Got response data while expecting a request");
        while (buffer.hasRemaining()) {
            final HttpResponse ans = responseReader.readMessage(buffer);
            if (ans != null) {
                ans.setRequestHeader(lastRequestHeader);
                lastRequestHeader = null;
                callback.onResponse(ans);
                // Read payload while buffer.hasRemaining()
            }
            if (!responseReader.hasPendingMessage()) {
                receiving = false;
                if (buffer.hasRemaining())
                    LOG.warn("Response ended but buffer has more content");
                break;
            }
        }
    }

    @Override
    public void close() throws IOException {
        responseReader.close();
        requestReader.close();
    }

    public interface MessageCallback {
        void onRequest(HttpRequest request);

        void onResponse(HttpResponse response);
    }

}
