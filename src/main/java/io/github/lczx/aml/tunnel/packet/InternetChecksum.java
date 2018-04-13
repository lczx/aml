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

package io.github.lczx.aml.tunnel.packet;

import java.nio.ByteBuffer;

/**
 * Assists in the calculation of checksums required in IP and TCP headers.
 */
public abstract class InternetChecksum implements Cloneable {

    private InternetChecksum() { }

    /**
     * Creates a new {@link InternetChecksum} instance.
     *
     * @return The new instance
     */
    public static InternetChecksum newInstance() {
        return new ChecksumImpl();
    }

    private static boolean arrayContains(int[] a, int v) {
        for (int x : a) if (x == v) return true;
        return false;
    }

    /**
     * Adds a single 2-byte element to this sum.
     *
     * @param v Short-sized data
     * @return This {@link InternetChecksum} instance
     */
    public abstract InternetChecksum update(short v);

    /**
     * Consumes the given buffer to update this digest.
     *
     * @param buffer      The given buffer
     * @param skipIndexes A list of short-sized offsets to skip in the calculation
     * @return This {@link InternetChecksum} instance
     */
    public abstract InternetChecksum update(ByteBuffer buffer, int... skipIndexes);

    /**
     * Resets the calculation of this checksum.
     *
     * @return This {@link InternetChecksum} instance
     */
    public abstract InternetChecksum reset();

    /**
     * Finalizes the calculation of this checksum.
     *
     * @return The computed digest
     */
    public abstract short compute();

    /**
     * @return A clone of this {@link InternetChecksum} instance in its current state
     */
    public abstract InternetChecksum clone();

    private static final class ChecksumImpl extends InternetChecksum {

        private int sum = 0;
        private boolean lastBufferOdd = false;

        @Override
        public InternetChecksum update(short v) {
            sum += NumberUtils.asUnsigned(v);
            return this;
        }

        @Override
        public InternetChecksum update(ByteBuffer buffer, int... skipIndexes) {
            if (lastBufferOdd && buffer.hasRemaining()) { // Complete the last unpaired byte if necessary
                sum += NumberUtils.asUnsigned(buffer.get());
                lastBufferOdd = false;
            }
            while (buffer.remaining() >= 2) {
                if (arrayContains(skipIndexes, buffer.position())) {
                    buffer.position(buffer.position() + 2);
                    continue;
                }
                update(buffer.getShort());
            }
            if (buffer.hasRemaining()) { // To handle odd-sized buffers
                sum += NumberUtils.asUnsigned(buffer.get()) << 8;
                lastBufferOdd = true;
            }
            return this;
        }

        @Override
        public InternetChecksum reset() {
            sum = 0;
            return this;
        }

        @Override
        public short compute() {
            while (sum >> 16 > 0)
                sum = (sum & 0xFFFF) + (sum >> 16);
            return (short) ~sum;
        }

        @Override
        public InternetChecksum clone() {
            ChecksumImpl clone = new ChecksumImpl();
            clone.sum = this.sum;
            return clone;
        }

    }

}
