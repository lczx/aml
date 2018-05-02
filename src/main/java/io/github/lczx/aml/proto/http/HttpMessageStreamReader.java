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

import io.github.lczx.aml.proto.http.parser.HttpMessageHeaderReader;
import io.github.lczx.aml.proto.http.stream.HttpBodyStream;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public class HttpMessageStreamReader<T extends HttpMessage> implements Closeable {

    private final HttpMessageHeaderReader<T> headerReader;
    private T currentMessage;
    private boolean hasPendingMessage = false;

    public HttpMessageStreamReader(final HttpMessageHeaderReader<T> headerReader) {
        this.headerReader = headerReader;
    }

    public T readMessage(final ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            // If we have a current message, it means that we are still reading its body
            if (currentMessage != null) {

                final HttpBodyStream body = currentMessage.getBody();

                // ...append payload to the last request
                if (body.requiresMoreData()) body.appendPayload(buffer);

                // If we haven't yet reached end of body, return (check if buffer.hasRemaining())
                if (body.requiresMoreData()) continue;

                // If the body has been fully read, prepare to process the next request
                hasPendingMessage = false;
                currentMessage = null;

                // Return before trying to handle a new message, allowing user to call hasPendingMessage()
                return null;
            }

            // Try to read an HTTP header/request
            final T message = headerReader.readMessage(buffer);
            if (headerReader.getBytesRead() != 0) hasPendingMessage = true;
            if (message != null) return currentMessage = message;
        }

        // The buffer was fully consumed
        return null;
    }

    /**
     * Returns {@code true} if this reader is currently processing a message.
     *
     * @return {@code false} if this reader has no partial message in its buffer
     */
    public boolean hasPendingMessage() {
        return hasPendingMessage;
    }

    @Override
    public void close() {
        if (currentMessage != null) {
            currentMessage.getBody().truncateInput();
            currentMessage = null;
        }
        headerReader.clear();
        hasPendingMessage = false;
    }

}
