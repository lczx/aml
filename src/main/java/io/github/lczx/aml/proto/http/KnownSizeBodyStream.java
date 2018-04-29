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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class KnownSizeBodyStream implements HttpBodyStream {

    public static final int DEFAULT_BUFFER_SIZE = 8192;

    private final InputStream bodyStream = new PayloadInputStream();
    private final ReadableByteChannel bodyChannel = new PayloadChannel();

    private RingBuffer buffer;

    private long remaining;
    private boolean truncated = false;

    public KnownSizeBodyStream(final long size) {
        buffer = new RingBuffer(Math.min((int) size, DEFAULT_BUFFER_SIZE));
        this.remaining = size;
    }

    @Override
    public synchronized void appendPayload(final ByteBuffer payload) {
        final int toRead = (int) Math.min(remaining, payload.remaining());
        remaining -= toRead;

        // Write data to our temporary buffer if we have it
        if (buffer != null) {
            final int actuallyRead = buffer.put(payload, toRead);
            if (toRead != actuallyRead) {
                // We have no more space for payload in our buffer,
                // at this point it will probably get never requested so we discard it and do not write more data
                buffer = null;
            }
            notifyAll(); // Can throw an IOException on waiting threads if the buffer has been dropped
        } else {
            //  If we don't have a buffer, simulate the read
            payload.position(payload.position() + toRead);
        }
    }

    @Override
    public boolean endReached() {
        return truncated || remaining == 0;
    }

    @Override
    public void setEndReached() {
        truncated = true;
    }

    @Override
    public InputStream getInputStream() {
        return bodyStream;
    }

    @Override
    public ReadableByteChannel getChannel() {
        return bodyChannel;
    }

    private int available() throws IOException {
        if (buffer == null) throw new IOException("Stream dropped because cache was filled");
        if (!isOpen()) return -1;

        return buffer.available();
    }

    private boolean isOpen() {
        if (buffer == null) return false;
        return !(endReached() && buffer.available() == 0);
    }

    private boolean blockRead() throws IOException {
        if (buffer == null) throw new IOException("Stream dropped because cache was filled");
        if (!isOpen()) return true;
        waitForData();
        if (buffer == null) throw new IOException("Stream dropped because cache was filled");
        return false;
    }

    private synchronized void waitForData() throws IOException {
        while (available() == 0) {
            try {
                wait();
            } catch (final InterruptedException e) { /* ignore */ }
        }
    }

    private class PayloadInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            if (blockRead()) return -1;
            return buffer.get();
        }

        @Override
        public int read(byte[] b) throws IOException {
            if (blockRead()) return -1;
            return buffer.get(b);
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            if (blockRead()) return -1;
            return buffer.get(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            if (buffer == null) throw new IOException("Stream dropped because cache was filled");
            return buffer.skip((int) n);
        }

        @Override
        public int available() throws IOException {
            return KnownSizeBodyStream.this.available();
        }

    }

    private class PayloadChannel implements ReadableByteChannel {

        @Override
        public int read(final ByteBuffer dst) throws IOException {
            if (blockRead()) return -1;
            return buffer.get(dst);
        }

        @Override
        public boolean isOpen() {
            return KnownSizeBodyStream.this.isOpen();
        }

        @Override
        public void close() throws IOException { }

    }

}
