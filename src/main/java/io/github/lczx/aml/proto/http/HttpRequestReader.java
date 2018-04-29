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

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public class HttpRequestReader implements Closeable {

    private final HttpRequestHeaderReader headerReader = new HttpRequestHeaderReader();
    private HttpRequest currentRequest;
    private boolean isReadingBody = false;

    public HttpRequest readRequest(final ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            // If we are reading body...
            if (isReadingBody) {
                final HttpBodyStream body = currentRequest.getBody();

                // ...append payload to the last request
                if (!body.endReached()) body.appendPayload(buffer);

                // If we haven't yet reached end of body, return (check if buffer.hasRemaining())
                if (!body.endReached()) continue;

                // If the body has been fully read, prepare to process the next request
                isReadingBody = false;
                currentRequest = null;
            }

            // Try to read an HTTP header/request
            final HttpRequest request = headerReader.readRequest(buffer);
            if (request != null) {
                isReadingBody = true;
                return currentRequest = request;
            }
        }

        // The buffer was fully consumed
        return null;
    }

    @Override
    public void close() {
        if (currentRequest != null) currentRequest.getBody().setEndReached();
    }

}
