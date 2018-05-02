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

import io.github.lczx.aml.proto.http.model.HttpMessage;
import io.github.lczx.aml.proto.http.parser.HttpMessageHeaderReader;
import io.github.lczx.aml.proto.http.stream.HttpBodyStream;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This class extracts HTTP messages from a succession of {@link ByteBuffer byte buffers}.
 *
 * <p> Pass buffers to {@link #readMessage(ByteBuffer)} to read them; when an header has been fully read it is returned
 * from this method and the reader starts to read the body of the message, which can in turn be retrieved from the
 * {@link HttpBodyStream} attached to the message.
 *
 * <p> Once a message (header + body) has been fully read, {@link #hasPendingMessage()} returns {@code false}.
 * A further call to {@link #readMessage(ByteBuffer)} will start reading a new message.
 *
 * @param <T> The type of message that this instance is supposed to read
 */
public class HttpMessageStreamReader<T extends HttpMessage> implements Closeable {

    private final HttpMessageHeaderReader<T> headerReader;
    private T currentMessage;
    private boolean hasPendingMessage = false;

    public HttpMessageStreamReader(final HttpMessageHeaderReader<T> headerReader) {
        this.headerReader = headerReader;
    }

    /**
     * Attempts to read an HTTP  message from the given {@link ByteBuffer}.
     *
     * <p> This method can be called multiple times and accumulates data until a message is read.
     * It returns in the following circumstances:
     * <ul>
     * <li>
     * The input buffer is exhausted (i.e. {@link ByteBuffer#hasRemaining()} returns {@code false});
     * </li>
     * <li>
     * A message header has been fully read (and returned from this method), no further bytes are read
     * from the message body, in this case the buffer may not be fully consumed. The message should be capable to
     * determine its body stream before attempting further calls to this method (see documentation of
     * {@link #hasPendingMessage()} for more info);
     * </li>
     * <li>
     * A message has been read completely, call {@link #hasPendingMessage()} to see if this condition is met,
     * also in this case the buffer may not be fully consumed.
     * </li>
     * </ul>
     *
     * @param buffer The buffer with data to read
     * @return An HTTP message if an header has been read, {@code null} if no result is available yet
     * @throws IOException If an I/O error occurs while reading the header
     */
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
     * <p> If this method is called just after getting a message, make sure that the given message is in the proper
     * condition to determine the type of its body (e.g. pass request header to response in order to check if it has
     * no data in response to HEAD).
     *
     * @return {@code false} if this reader has no partial message in its buffer
     */
    public boolean hasPendingMessage() {
        if (currentMessage != null && !currentMessage.getBody().requiresMoreData()) {
            hasPendingMessage = false;
            currentMessage = null;
        }
        return hasPendingMessage;
    }

    /**
     * Closes this reader, removing references to any message being processed and, if its body is being processed,
     * truncating its stream.
     *
     * <p> This method should be called when the underlying connection is closed. An instance of {@link HttpBodyStream}
     * may rely on this to terminate its stream (e.g. responses with undetermined size)
     */
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
