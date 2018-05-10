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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public abstract class AbstractBodyStream implements HttpBodyStream {

    public static final int DEFAULT_BUFFER_SIZE = 8192;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractBodyStream.class);

    private InputStream bodyStream;
    private ReadableByteChannel bodyChannel;
    private boolean truncated = false;

    private int bufferSize;
    private boolean startedWriting = false;
    protected RingBuffer buffer;

    public AbstractBodyStream() {
        this(Integer.MAX_VALUE);
    }

    public AbstractBodyStream(final long expectedDataLength) {
        bufferSize = Math.min((int) expectedDataLength, DEFAULT_BUFFER_SIZE);
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

    @Override
    public void resizeBuffer(final int newSize) {
        bufferSize = newSize;
        if (buffer != null) {
            buffer.resize(newSize);
            LOG.debug("Buffer resized (as requested) to {} bytes - reallocated");
        } else {
            LOG.debug("Buffer resized (as requested) to {} bytes - no reallocation needed" +
                    " (still no data or stream dropped)", newSize);
        }
    }

    protected abstract boolean wantsMoreData();

    protected abstract boolean isStreamClosed();

    protected synchronized int putData(final long remaining, final ByteBuffer payload) {
        final int toRead = (int) Math.min(payload.remaining(), remaining);
        if (toRead == 0) return 0;

        if (!startedWriting) {
            buffer = new RingBuffer(bufferSize);
            startedWriting = true;
        }

        // Write data to our temporary buffer if we have it
        if (buffer != null) {
            final int actuallyRead = buffer.put(payload, toRead);
            if (toRead != actuallyRead) {
                // We have no more space for payload in our buffer,
                // at this point it will probably get never requested so we discard it and do not write more data
                LOG.info("Internal buffer size exceeded {} bytes without reading, dropping stream", buffer.capacity());
                payload.position(payload.position() + toRead - actuallyRead);
                buffer = null;
            }
        } else {
            //  If we don't have a buffer, simulate the read
            payload.position(payload.position() + toRead);
        }
        notifyAll();
        return toRead;
    }

    private boolean isStreamDropped() {
        return startedWriting && buffer == null;
    }

    private void checkStreamDropped() throws IOException {
        if (isStreamDropped()) throw new IOException("Stream dropped because cache was filled");
    }

    private int getAvailableDataSize() throws IOException {
        checkStreamDropped();
        if (isStreamClosed()) return -1;

        return startedWriting ? buffer.available() : 0;
    }

    private boolean blockRead() throws IOException {
        checkStreamDropped();
        if (isStreamClosed()) return true;
        waitForData();
        checkStreamDropped();
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
            checkStreamDropped();
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
            if (isStreamDropped()) return false;
            return !isStreamClosed();
        }

        @Override
        public void close() throws IOException { }

    }

}
