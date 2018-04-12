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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TcpLayer extends AbstractProtocolLayer {

    // TCP flag masks
    public static final int FLAG_FIN = 0x01;    // FIN: Last packet from sender
    public static final int FLAG_SYN = 0x02;    // SYN: Synchronize sequence numbers
    public static final int FLAG_RST = 0x04;    // RST: Connection reset
    public static final int FLAG_PSH = 0x08;    // PSH: Push function (push buffered data to app. layer)
    public static final int FLAG_ACK = 0x10;    // ACK: Acknowledgement field is used
    public static final int FLAG_URG = 0x20;    // URG: Urgent pointer field is used
    public static final int FLAG_ECE = 0x40;    // ECE: ECN-echo, if SYN(1) is ECN capable, else congestion experienced
    public static final int FLAG_CWR = 0x80;    // CWR: Congestion window reduced

    // Intra-header field offsets (in bytes)
    static final int IDX_WORD_SOURCE_PORT = 0;                  // RW   0 :  15 (16b), source port
    static final int IDX_WORD_DESTINATION_PORT = 2;             // RW  16 :  31 (16b), destination port
    static final int IDX_DWORD_SEQUENCE_NUMBER = 4;             // RW  32 :  63 (32b), sequence number
    static final int IDX_DWORD_ACKNOWLEDGEMENT_NUMBER = 8;      // RW  64 :  95 (32b), acknowledgement number
    static final int IDX_BYTE_DATA_OFFSET_AND_RESERVED = 12;    // RA  96 :  99  (4b), data offset
    //                                                          // -- 100 : 102  (3b), reserved
    //                                                          // --    103     (1b), NS: ECN-nonce (experimental)
    static final int IDX_BYTE_FLAGS = 13;                       // RW 104 : 111  (8b), flags
    static final int IDX_WORD_WINDOW_SIZE = 14;                 // RW 112 : 127 (16b), window size
    static final int IDX_WORD_CHECKSUM = 16;                    // RA 128 : 143 (16b), checksum
    static final int IDX_WORD_URGENT_POINTER = 18;              // RW 144 : 159 (16b), urgent pointer
    static final int IDX_BLOB_OPTIONS = 20;                     // RW -- up to data offset, optionless header size --

    public TcpLayer(final ProtocolLayer<?> parentLayer, final ByteBuffer backingBuffer, final int offset) {
        super(parentLayer, backingBuffer, offset);
    }

    @Override
    public int getHeaderSize() {
        return getDataOffset() << 2; // dataOffset is number of dwords (4B each), we want bytes
    }

    @Override
    public int getTotalSize() {
        return getParentLayer().getPayloadSize();
    }

    public int getSourcePort() {
        return NumberUtils.asUnsigned(backingBuffer.getShort(offset + IDX_WORD_SOURCE_PORT));
    }

    public int getDestinationPort() {
        return NumberUtils.asUnsigned(backingBuffer.getShort(offset + IDX_WORD_DESTINATION_PORT));
    }

    public long getSequenceNumber() {
        return NumberUtils.asUnsigned(backingBuffer.getInt(offset + IDX_DWORD_SEQUENCE_NUMBER));
    }

    public long getAcknowledgementNumber() {
        return NumberUtils.asUnsigned(backingBuffer.getInt(offset + IDX_DWORD_ACKNOWLEDGEMENT_NUMBER));
    }

    public byte getDataOffset() {
        // Bit mask cause Java (logical shift doesn't work?)... TODO: Is it necessary?
        return (byte) (backingBuffer.get(offset + IDX_BYTE_DATA_OFFSET_AND_RESERVED) >> 4 & 0x0F);
    }

    public byte getFlags() {
        return backingBuffer.get(offset + IDX_BYTE_FLAGS);
    }

    public int getWindowSize() {
        return NumberUtils.asUnsigned(backingBuffer.getShort(offset + IDX_WORD_WINDOW_SIZE));
    }

    public short getChecksum() {
        return backingBuffer.getShort(offset + IDX_WORD_CHECKSUM);
    }

    public int getUrgentPointer() {
        return NumberUtils.asUnsigned(backingBuffer.getShort(offset + IDX_WORD_URGENT_POINTER));
    }

    public byte[] getOptions() {
        final int optionsSize = getHeaderSize() - IDX_BLOB_OPTIONS;
        if (optionsSize == 0) return null;

        final byte[] options = new byte[optionsSize];
        ((ByteBuffer) backingBuffer.duplicate().position(offset + IDX_BLOB_OPTIONS)).get(options);
        return options;
    }

    public boolean isFIN() {
        return (getFlags() & FLAG_FIN) == FLAG_FIN;
    }

    public boolean isSYN() {
        return (getFlags() & FLAG_SYN) == FLAG_SYN;
    }

    public boolean isRST() {
        return (getFlags() & FLAG_RST) == FLAG_RST;
    }

    public boolean isPSH() {
        return (getFlags() & FLAG_PSH) == FLAG_PSH;
    }

    public boolean isACK() {
        return (getFlags() & FLAG_ACK) == FLAG_ACK;
    }

    public boolean isURG() {
        return (getFlags() & FLAG_URG) == FLAG_URG;
    }

    public boolean isECE() {
        return (getFlags() & FLAG_ECE) == FLAG_ECE;
    }

    public boolean isCWR() {
        return (getFlags() & FLAG_CWR) == FLAG_CWR;
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
        final Set<String> flagsStr = new HashSet<>();
        if (isFIN()) flagsStr.add("FIN");
        if (isSYN()) flagsStr.add("SYN");
        if (isRST()) flagsStr.add("RST");
        if (isPSH()) flagsStr.add("PSH");
        if (isACK()) flagsStr.add("ACK");
        if (isURG()) flagsStr.add("URG");
        if (isECE()) flagsStr.add("ECE");
        if (isCWR()) flagsStr.add("CWR");

        return "TcpLayer{" +
                "bufferOffset=" + offset +
                ", sourcePort=" + getSourcePort() +
                ", destinationPort=" + getDestinationPort() +
                ", sequenceNumber=" + getSequenceNumber() +
                ", acknowledgementNumber=" + getAcknowledgementNumber() +
                ", length=(H:" + getHeaderSize() + "+P:" + getPayloadSize() + "=T:" + getTotalSize() + ')' +
                ", dataOffset=" + getDataOffset() +
                ", flags=" + flagsStr +
                ", windowSize=" + getWindowSize() +
                ", checksum=" + getChecksum() +
                ", urgentPointer=" + getUrgentPointer() +
                ", optionsAndPadding=" + Arrays.toString(getOptions()) +
                ", nextLayer=" + getNextLayer() +
                '}';
    }

}
