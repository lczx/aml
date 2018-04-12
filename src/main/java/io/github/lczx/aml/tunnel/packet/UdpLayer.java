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

import io.github.lczx.aml.tunnel.packet.editor.LayerEditor;

import java.nio.ByteBuffer;

public class UdpLayer extends AbstractProtocolLayer {

    private static final int HEADER_SIZE = 8;

    // Intra-header field offsets (in bytes)
    static final int IDX_WORD_SOURCE_PORT = 0;          //    0 :  15 (16b), source port
    static final int IDX_WORD_DESTINATION_PORT = 2;     //   16 :  31 (16b), destination port
    static final int IDX_WORD_TOTAL_LENGTH = 4;         //   32 :  47 (16b), length
    static final int IDX_WORD_CHECKSUM = 6;             //   48 :  63 (16b), checksum

    public UdpLayer(final ProtocolLayer<?> parentLayer, final ByteBuffer backingBuffer, final int offset) {
        super(parentLayer, backingBuffer, offset);
    }

    @Override
    public int getHeaderSize() {
        return HEADER_SIZE;
    }

    @Override
    public int getTotalSize() {
        return getTotalLength();
    }

    public int getSourcePort() {
        return NumberUtils.asUnsigned(backingBuffer.getShort(offset + IDX_WORD_SOURCE_PORT));
    }

    public int getDestinationPort() {
        return NumberUtils.asUnsigned(backingBuffer.getShort(offset + IDX_WORD_DESTINATION_PORT));
    }

    public int getTotalLength() {
        return NumberUtils.asUnsigned(backingBuffer.getShort(offset + IDX_WORD_TOTAL_LENGTH));
    }

    public short getChecksum() {
        return backingBuffer.getShort(offset + IDX_WORD_CHECKSUM);
    }

    @Override
    protected ProtocolLayer<?> buildNextLayer(final int nextOffset) {
        return null;
    }

    @Override
    protected LayerEditor buildEditor(final ByteBuffer bufferView) {
        return null; // TODO: Implement
    }

    @Override
    public String toString() {
        return "UdpLayer{" +
                "bufferOffset=" + offset +
                ", sourcePort=" + getSourcePort() +
                ", destinationPort=" + getDestinationPort() +
                ", length=(H:" + getHeaderSize() + "+P:" + getPayloadSize() + "=T:" + getTotalSize() + ')' +
                ", checksum=" + getChecksum() +
                ", nextLayer=" + getNextLayer() +
                '}';
    }

}
