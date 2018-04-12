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

import io.github.lczx.aml.tunnel.packet.editor.LayerChangeset;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link ProtocolLayer} representing the Internet Protocol, version 4.
 */
public class IPv4Layer extends AbstractProtocolLayer<IPv4LayerEditor> implements IPv4Header {

    // IP flag masks
    public static final int FLAG_MF = 0x01;     // MF: More fragments
    public static final int FLAG_DF = 0x02;     // DF: Don't fragment

    // IANA-assigned IP protocol numbers, unsigned
    public static final int PROTOCOL_ICMP = 1;
    public static final int PROTOCOL_TCP = 6;
    public static final int PROTOCOL_UDP = 17;

    // Intra-header field offsets (in bytes)
    static final int IDX_BYTE_VERSION_AND_IHL = 0;              //   0 :   3  (4b), version
    //                                                          //   4 :   7  (4b), internet header length
    static final int IDX_BYTE_DSCP_ECN = 1;                     //   8 :  15  (8b), diffServices & ECN (ex. ToS)
    static final int IDX_WORD_TOTAL_LENGTH = 2;                 //  16 :  31 (16b), total length
    static final int IDX_WORD_IDENTIFICATION = 4;               //  32 :  47 (16b), identification
    static final int IDX_WORD_FLAGS_AND_FRAGMENT_OFFSET = 6;    //  48 :  50  (3b), flags
    //                                                          //  51 :  63 (13b), fragment offset
    static final int IDX_BYTE_TIME_TO_LIVE = 8;                 //  64 :  71  (8b), time to live
    static final int IDX_BYTE_PROTOCOL_ID = 9;                  //  72 :  79  (8b), protocol
    static final int IDX_WORD_CHECKSUM = 10;                    //  80 :  95 (16b), header checksum
    static final int IDX_DWORD_SOURCE_ADDRESS = 12;             //  96 : 127 (32b), source address
    static final int IDX_DWORD_DESTINATION_ADDRESS = 16;        // 128 : 159 (32b), destination address
    static final int IDX_BLOB_OPTIONS = 20;                     // -- up to IHL, also optionless header size --

    public IPv4Layer(final ProtocolLayer<?> parentLayer, final ByteBuffer backingBuffer, final int offset) {
        super(parentLayer, backingBuffer, offset);
    }

    @Override
    public int getHeaderSize() {
        return getIHL() << 2; // IHL is the number of dwords (4B each), we want bytes
    }

    @Override
    public int getTotalSize() {
        return getTotalLength();
    }

    @Override
    public void onEditorCommit(final LayerChangeset changeset, final int sizeDelta) {
        // Invalidate lower layers if the protocol ID has changed
        if (changeset != null && changeset.getEdit(IDX_BYTE_PROTOCOL_ID) != null)
            invalidateChildLayers();

        super.onEditorCommit(changeset, sizeDelta);
    }

    @Override
    public byte getVersion() {
        return (byte) (backingBuffer.get(offset + IDX_BYTE_VERSION_AND_IHL) >> 4);
    }

    @Override
    public byte getIHL() {
        return (byte) (backingBuffer.get(offset + IDX_BYTE_VERSION_AND_IHL) & 0x0F);
    }

    @Override
    public byte getDiffServicesAndECN() {
        return backingBuffer.get(offset + IDX_BYTE_DSCP_ECN);
    }

    @Override
    public int getTotalLength() {
        return NumberUtils.asUnsigned(backingBuffer.getShort(offset + IDX_WORD_TOTAL_LENGTH));
    }

    @Override
    public int getIdentificationField() {
        return NumberUtils.asUnsigned(backingBuffer.getShort(offset + IDX_WORD_IDENTIFICATION));
    }

    @Override
    public byte getFlags() { // Retrieving only lower 2 bits, 3rd is reserved
        return (byte) (backingBuffer.getShort(offset + IDX_WORD_FLAGS_AND_FRAGMENT_OFFSET) >> 13 & 0x03);
    }

    @Override
    public short getFragmentOffset() { // Excluding upper 3 bits (flags)
        return (short) (backingBuffer.getShort(offset + IDX_WORD_FLAGS_AND_FRAGMENT_OFFSET) & 0x1FFF);
    }

    @Override
    public short getTTL() {
        return NumberUtils.asUnsigned(backingBuffer.get(offset + IDX_BYTE_TIME_TO_LIVE));
    }

    @Override
    public short getProtocolId() {
        return NumberUtils.asUnsigned(backingBuffer.get(offset + IDX_BYTE_PROTOCOL_ID));
    }

    @Override
    public short getHeaderChecksum() {
        return backingBuffer.getShort(offset + IDX_WORD_CHECKSUM);
    }

    @Override
    public Inet4Address getSourceAddress() {
        return readIPv4Address(offset + IDX_DWORD_SOURCE_ADDRESS);
    }

    @Override
    public Inet4Address getDestinationAddress() {
        return readIPv4Address(offset + IDX_DWORD_DESTINATION_ADDRESS);
    }

    @Override
    public byte[] getOptions() {
        final int optionsSize = getHeaderSize() - IDX_BLOB_OPTIONS;
        if (optionsSize == 0) return null;

        final byte[] options = new byte[optionsSize];
        ((ByteBuffer) backingBuffer.duplicate().position(offset + IDX_BLOB_OPTIONS)).get(options);
        return options;
    }

    public boolean hasMoreFragmentsFlag() {
        return (getFlags() & FLAG_MF) == FLAG_MF;
    }

    public boolean hasDoNotFragmentFlag() {
        return (getFlags() & FLAG_DF) == FLAG_DF;
    }

    public short calculateChecksum() {
        final ByteBuffer b = backingBuffer.duplicate();
        b.position(offset);
        b.limit(getHeaderSize());
        return InternetChecksum.newInstance().update(b, IDX_WORD_CHECKSUM).compute();
    }

    @Override
    protected ProtocolLayer<?> buildNextLayer(final int nextOffset) {
        return LayerFactory.getFactory(Packets.LAYER_TRANSPORT).detectLayer(this, backingBuffer, nextOffset);
    }

    @Override
    protected IPv4LayerEditor buildEditor(final ByteBuffer bufferView) {
        return new IPv4LayerEditor(this, bufferView);
    }

    @Override
    protected void onPayloadChanged(final int sizeDelta) {
        if (sizeDelta != 0) {
            backingBuffer.putShort(offset + IDX_WORD_TOTAL_LENGTH, (short) (getTotalLength() + sizeDelta));
            backingBuffer.putShort(offset + IDX_WORD_CHECKSUM, calculateChecksum());
        }
    }

    private Inet4Address readIPv4Address(final int index) {
        final byte[] addressBytes = new byte[4];
        ((ByteBuffer) backingBuffer.duplicate().position(index)).get(addressBytes);
        try {
            return (Inet4Address) InetAddress.getByAddress(addressBytes);
        } catch (final UnknownHostException e) {
            // TODO: Should we throw or log here?
            return null;
        }
    }

    @Override
    public String toString() {
        final Set<String> flagsStr = new HashSet<>();
        if (hasMoreFragmentsFlag()) flagsStr.add("MF");
        if (hasDoNotFragmentFlag()) flagsStr.add("DF");

        return "IPv4Layer{" +
                "bufferOffset=" + offset +
                ", version=" + getVersion() +
                ", IHL=" + getIHL() +
                ", DSCP+ECN=" + getDiffServicesAndECN() +
                ", length=(H:" + getHeaderSize() + "+P:" + getPayloadSize() + "=T:" + getTotalSize() + ')' +
                ", identification=" + getIdentificationField() +
                ", flags=" + flagsStr +
                ", fragmentOffset=" + getFragmentOffset() +
                ", TTL=" + getTTL() +
                ", protocolId=" + getProtocolId() +
                ", headerChecksum=" + getHeaderChecksum() +
                ", sourceAddress=" + getSourceAddress().getHostAddress() +
                ", destinationAddress=" + getDestinationAddress().getHostAddress() +
                ", options=" + Arrays.toString(getOptions()) +
                ", nextLayer=" + getNextLayer() +
                '}';
    }

}
