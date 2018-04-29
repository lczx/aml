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

    public static final int MAX_HEADER_SIZE = 8192;

    private final NonBlockingLineReader lineReader = new NonBlockingLineReader();
    private int headerSize = 0;
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
                headerSize = 0;
                currentRequest = null;
            }

            // Try to read an header line
            final int rem = buffer.remaining();
            final String line = lineReader.readLine(buffer);
            headerSize += rem - buffer.remaining();
            if (headerSize >= MAX_HEADER_SIZE)
                throw new IOException("Maximum header size of " + MAX_HEADER_SIZE + " bytes reached");

            // If we have no line the buffer was fully consumed without reaching EOL
            if (line == null) return null;

            // If the line is empty (""), we have reached end of headers, return our current pending request
            if (line.isEmpty()) {
                isReadingBody = true;
                return currentRequest;
            }

            // If we have an header line...
            if (currentRequest == null) { // ...we don't have a pending request, parse the prime header and create new
                final String[] primeHeader = line.trim().split(" +");
                currentRequest = new HttpRequest(primeHeader[0], primeHeader[1], primeHeader[2]);
            } else { // ...we have a pending request, parse the line as an header field
                final String[] field = line.split(": *", 2);
                currentRequest.putField(field[0], field[1]);
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
