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

import java.nio.ByteBuffer;

/**
 * A <i>thread-safe</i> circular byte buffer acting as a queue of raw bytes.
 */
public class RingBuffer {

    private static final int DEFAULT_SIZE = 8192;

    private byte[] buffer;
    private int avail, ri, wi;

    public RingBuffer() {
        this(DEFAULT_SIZE);
    }

    public RingBuffer(final int capacity) {
        buffer = new byte[capacity];
    }

    /**
     * The capacity of this ring buffer.
     *
     * @return The maximum number of bytes that can be put in this buffer
     */
    public int capacity() {
        return buffer.length;
    }

    /**
     * The number of bytes available in this buffer.
     *
     * @return The number of bytes currently stored in this buffer
     */
    public int available() {
        return avail;
    }

    /**
     * The free space in bytes of this buffer.
     *
     * @return The number of bytes that can be currently written into this buffer
     */
    public int free() {
        return buffer.length - avail;
    }

    /**
     * Retrieves a byte from this buffer.
     *
     * @return a byte from this buffer or {@code -1} if no data is available
     */
    public synchronized int get() {
        if (avail == 0) return -1;

        final byte value = buffer[ri];
        ri = (ri + 1) % buffer.length;
        avail--;
        return value & 0xFF;
    }

    /**
     * Attempts to fill the given array with data from this buffer.
     *
     * <p>Equivalent to {@code get(dst, 0, dst.length)}.
     *
     * @param dst The target array
     * @return The number of bytes actually written to the array
     */
    public int get(final byte[] dst) {
        return get(dst, 0, dst.length);
    }

    /**
     * Attempts to fill the given array slice with data from this buffer.
     *
     * @param dst    The target array
     * @param offset The starting offset to where data will be written
     * @param length The number of bytes to write
     * @return The actual number of bytes written
     */
    public synchronized int get(final byte[] dst, final int offset, final int length) {
        if (avail == 0) return 0;

        final int limit = ri < wi ? wi : buffer.length;
        int count = Math.min(limit - ri, length);
        System.arraycopy(buffer, ri, dst, offset, count);
        ri += count;

        if (ri == buffer.length) {
            // Resume from the start of the buffer
            final int count2 = Math.min(length - count, wi);
            if (count2 > 0) {
                System.arraycopy(buffer, 0, dst, offset + count, count2);
                ri = count2;
                count += count2;
            } else {
                ri = 0;
            }
        }
        avail -= count;
        return count;
    }

    /**
     * Attempts to fill the given {@link ByteBuffer} with data from this buffer.
     *
     * <p>Equivalent to {@code get(dst, dst.remaining())}.
     *
     * @param dst The target buffer
     * @return The number of bytes actually written
     */
    public int get(final ByteBuffer dst) {
        return get(dst, dst.remaining());
    }

    /**
     * Attempts to write to the given {@link ByteBuffer} with data from this buffer.
     *
     * @param dst The target buffer
     * @param length The number of bytes to read from this buffer
     * @return The number of bytes actually written
     */
    public synchronized int get(final ByteBuffer dst, final int length) {
        if (avail == 0) return 0;

        final int limit = ri < wi ? wi : buffer.length;
        int count = Math.min(limit - ri, length);
        dst.put(buffer, ri, count);
        ri += count;


        if (ri == buffer.length) {
            // Resume from the start of the buffer
            final int count2 = Math.min(length - count, wi);
            if (count2 > 0) {
                dst.put(buffer, 0, count2);
                ri = count2;
                count += count2;
            } else {
                ri = 0;
            }
        }
        avail -= count;
        return count;
    }

    /**
     * Puts a byte into this buffer.
     *
     * @param value The byte to insert into the buffer
     * @return {@code true} if the operation succeeded or {@code false} if the buffer is full
     */
    public synchronized boolean put(final byte value) {
        if (avail == buffer.length) return false;

        buffer[wi] = value;
        wi = (wi + 1) % buffer.length;
        avail++;
        return true;
    }

    /**
     * Attempts to read all the given array into this buffer.
     *
     * <p>Equivalent to {@code put(src, 0, src.length)}.
     *
     * @param src The source array
     * @return The number of bytes actually read from the array
     */
    public int put(final byte[] src) {
        return put(src, 0, src.length);
    }

    /**
     * Attempts to read the given array slice into this buffer.
     *
     * @param src    The source array
     * @param offset The starting offset from where data will be read
     * @param length The number of bytes to read
     * @return The actual number of bytes read
     */
    public synchronized int put(final byte[] src, final int offset, final int length) {
        if (avail == buffer.length) return 0;

        final int limit = wi < ri ? ri : buffer.length;
        int count = Math.min(limit - wi, length);
        System.arraycopy(src, offset, buffer, wi, count);
        wi += count;

        if (wi == buffer.length) {
            // Wrap around the buffer
            final int count2 = Math.min(length - count, ri);
            if (count2 > 0) {
                System.arraycopy(src, offset + count, buffer, 0, count2);
                wi = count2;
                count += count2;
            } else {
                wi = 0;
            }
        }
        avail += count;
        return count;
    }

    /**
     * Attempts to completely read the given {@link ByteBuffer} into this buffer.
     *
     * <p>Equivalent to {@code put(src, src.remaining())}.
     *
     * @param src The source buffer
     * @return The number of bytes actually read
     */
    public int put(final ByteBuffer src) {
        return put(src, src.remaining());
    }

    /**
     * Attempts to read the given {@link ByteBuffer} into this buffer.
     *
     * @param src    The source buffer
     * @param length The number of bytes to put in this buffer
     * @return The number of bytes actually read
     */
    public synchronized int put(final ByteBuffer src, final int length) {
        if (avail == buffer.length) return 0;

        final int limit = wi < ri ? ri : buffer.length;
        int count = Math.min(limit - wi, length);
        src.get(buffer, wi, count);
        wi += count;

        if (wi == buffer.length) {
            // Wrap around the buffer
            final int count2 = Math.min(length - count, ri);
            if (count2 > 0) {
                src.get(buffer, 0, count2);
                wi = count2;
                count += count2;
            } else {
                wi = 0;
            }
        }
        avail += count;
        return count;
    }

    /**
     * Skips the given amount of bytes.
     *
     * @param count The number of bytes to skip.
     *              If this value is greater than {@link #available()}, that value will be used instead.
     * @return The number of bytes actually skipped.
     */
    public synchronized int skip(int count) {
        if (count > avail) count = avail;
        ri = (ri + count) % buffer.length;
        avail -= count;
        return count;
    }

    /**
     * Clears all the data in this buffer.
     */
    public synchronized void clear() {
        ri = wi = avail = 0;
    }

    /**
     * Resizes this ring buffer.
     *
     * <p> This methods allocates a new byte array with the given size
     * and copies all the data actually stored in the buffer to the new array.
     *
     * @param newSize The new size in bytes
     */
    public synchronized void resize(final int newSize) {
        final byte[] newBuffer = new byte[newSize];
        final int avail = this.avail;
        get(newBuffer);
        ri = wi = 0;
        this.avail = avail;
        buffer = newBuffer;
    }

}
