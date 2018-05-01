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

import io.github.lczx.aml.proto.http.util.RingBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public abstract class AbstractBodyStream implements HttpBodyStream {

    public static final int DEFAULT_BUFFER_SIZE = 8192;

    private InputStream bodyStream;
    private ReadableByteChannel bodyChannel;
    private boolean truncated = false;

    protected RingBuffer buffer;

    public AbstractBodyStream() {
        this(Integer.MAX_VALUE);
    }

    public AbstractBodyStream(final long expectedDataLength) {
        buffer = new RingBuffer(Math.min((int) expectedDataLength, DEFAULT_BUFFER_SIZE));
    }

    @Override
    public boolean requiresMoreData() {
        return !truncated && wantsMoreData();
    }

    @Override
    public synchronized void truncateInput() {
        truncated = true;
        notifyAll();
    }

    @Override
    public InputStream getInputStream() {
        if (bodyStream == null) bodyStream = new PayloadInputStream();
        return bodyStream;
    }

    @Override
    public ReadableByteChannel getChannel() {
        if (bodyChannel == null) bodyChannel = new PayloadChannel();
        return bodyChannel;
    }

    protected abstract boolean wantsMoreData();

    protected abstract boolean isStreamClosed();

    protected synchronized int putData(final long remaining, final ByteBuffer payload) {
        final int toRead = (int) Math.min(payload.remaining(), remaining);

        // Write data to our temporary buffer if we have it
        if (buffer != null) {
            final int actuallyRead = buffer.put(payload, toRead);
            if (toRead != actuallyRead) {
                // We have no more space for payload in our buffer,
                // at this point it will probably get never requested so we discard it and do not write more data
                buffer = null;
            }
        } else {
            //  If we don't have a buffer, simulate the read
            payload.position(payload.position() + toRead);
        }
        notifyAll();
        return toRead;
    }

    private int getAvailableDataSize() throws IOException {
        if (buffer == null) throw new IOException("Stream dropped because cache was filled");
        if (isStreamClosed()) return -1;

        return buffer.available();
    }

    private boolean blockRead() throws IOException {
        if (buffer == null) throw new IOException("Stream dropped because cache was filled");
        if (isStreamClosed()) return true;
        waitForData();
        if (buffer == null) throw new IOException("Stream dropped because cache was filled");
        if (isStreamClosed()) return true;
        return false;
    }

    private synchronized void waitForData() throws IOException {
        while (getAvailableDataSize() == 0) {
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
        public int read(final byte[] b) throws IOException {
            if (blockRead()) return -1;
            return buffer.get(b);
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            if (blockRead()) return -1;
            return buffer.get(b, off, len);
        }

        @Override
        public long skip(final long n) throws IOException {
            if (buffer == null) throw new IOException("Stream dropped because cache was filled");
            return buffer.skip((int) n);
        }

        @Override
        public int available() throws IOException {
            return getAvailableDataSize();
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
            if (buffer == null) return false;
            return !isStreamClosed();
        }

        @Override
        public void close() throws IOException { }

    }

}
