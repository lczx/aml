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

package io.github.lczx.aml.proto.http.stream;

import io.github.lczx.aml.proto.http.model.HttpHeader;
import io.github.lczx.aml.proto.http.parser.HeaderReader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class ChunkedBodyStream extends AbstractBodyStream {

    // https://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.6.1

    private static final int STATE_START_OF_CHUNK = -1;  // Waiting to read a chunk-size line
    private static final int STATE_END_OF_CHUNK = -2;    // Waiting to read CRLF at end of chunk
    private static final int STATE_READING_TRAILER = -3; // Reading trailing headers
    private static final int STATE_END_OF_STREAM = -4;   // When the chunked stream is ended

    private final HeaderReader headerReader = new HeaderReader();
    private int chunkRemaining = STATE_START_OF_CHUNK; // Also used as a state holder
    private List<HttpHeader.Field> trailingHeaders;

    @Override
    public synchronized void appendPayload(final ByteBuffer payload) {
        if (chunkRemaining == STATE_START_OF_CHUNK) {
            // We are expecting a chunk-size element + optional (ignored) extension
            String line = headerReader.getLineReader().readLine(payload);
            if (line == null) return;

            // Ignore chunk extensions if present
            final int extStart = line.indexOf(';');
            if (extStart != -1) line = line.substring(0, extStart);
            final int chunkSize = Integer.parseInt(line, 16);

            if (chunkSize > 0) {
                // We have another chunk to process, set remaining
                chunkRemaining = chunkSize;
            } else {
                // This is the last chunk (size = 0), start reading trailer
                chunkRemaining = STATE_READING_TRAILER;
                // Notify waiting readers that we reached EOS
                notifyAll();
                return;
            }
        }

        if (chunkRemaining >= 0) {
            chunkRemaining -= putData(chunkRemaining, payload);
            if (chunkRemaining == 0) // End of chunk
                chunkRemaining = STATE_END_OF_CHUNK;
        }

        if (chunkRemaining == STATE_END_OF_CHUNK) {
            // We consumed all the data in this chunk, read line feed at the end
            final String line = headerReader.getLineReader().readLine(payload);
            if (line == null) return;
            if (!line.isEmpty())
                throw new IllegalStateException("Expected CRLF at the end of chunk, got \"" + line + '"');
            chunkRemaining = STATE_START_OF_CHUNK;
        }

        if (chunkRemaining == STATE_READING_TRAILER) {
            try {
                final List<HttpHeader.Field> trailers = headerReader.readHeader(payload);
                if (trailers == null) return;
                this.trailingHeaders = trailers;
                chunkRemaining = STATE_END_OF_STREAM;
                // Notify waiting readers that we have trailers now (and we also reached EOS)
                notifyAll();
            } catch (IOException e) {
                throw new IllegalStateException("Chunked trailing headers section too large");
            }
        }
    }

    public synchronized List<HttpHeader.Field> getTrailingHeaders(final boolean blocking) {
        if (blocking) {
            while (chunkRemaining != STATE_END_OF_STREAM) {
                try {
                    wait();
                } catch (final InterruptedException e) { /* ignore */ }
            }
            return trailingHeaders;
        } else {
            return chunkRemaining == STATE_END_OF_STREAM ? trailingHeaders : null;
        }
    }

    @Override
    protected boolean wantsMoreData() {
        return chunkRemaining != STATE_END_OF_STREAM;
    }

    @Override
    public boolean isStreamClosed() {
        // We can signal EOS even if we are still reading trailers. Data is actually finished before reading
        // the last-chunk line (and not when STATE_READING_TRAILER) but we have no way to tell that.
        return (!requiresMoreData() || chunkRemaining == STATE_READING_TRAILER) &&
                (buffer == null || buffer.available() == 0);
    }

}
