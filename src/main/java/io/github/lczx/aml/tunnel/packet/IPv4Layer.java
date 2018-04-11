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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class IPv4Layer extends AbstractProtocolLayer {

    // IANA-assigned IP protocol numbers, unsigned
    public static final int PROTOCOL_ICMP = 1;
    public static final int PROTOCOL_TCP = 6;
    public static final int PROTOCOL_UDP = 17;

    // Intra-header field offsets (in bytes)
    static final int IDX_BYTE_VERSION_AND_IHL = 0;              //   0 :   3  (4b), version
    //                                                          //   4 :   7  (4b), internet header length
    static final int IDX_BYTE_DSCP_ECN = 1;                     //   8 :  15  (8b), diffServ & ECN (ex. ToS)
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

    public IPv4Layer(ProtocolLayer parentLayer, ByteBuffer backingBuffer, int offset) {
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

    public byte getVersion() {
        return (byte) (backingBuffer.get(offset + IDX_BYTE_VERSION_AND_IHL) >> 4);
    }

    public byte getIHL() {
        return (byte) (backingBuffer.get(offset + IDX_BYTE_VERSION_AND_IHL) & 0x0F);
    }

    public byte getDiffServAndECN() {
        return backingBuffer.get(offset + IDX_BYTE_DSCP_ECN);
    }

    public int getTotalLength() {
        return NumberUtils.asUnsigned(backingBuffer.getShort(offset + IDX_WORD_TOTAL_LENGTH));
    }

    public int getIdentificationField() {
        return NumberUtils.asUnsigned(backingBuffer.getShort(offset + IDX_WORD_IDENTIFICATION));
    }

    public byte getFlags() { // Retrieving only lower 2 bits, 3rd is reserved
        return (byte) (backingBuffer.getShort(offset + IDX_WORD_FLAGS_AND_FRAGMENT_OFFSET) >> 13 & 0x03);
    }

    public short getFragmentOffset() { // Excluding upper 3 bits (flags)
        return (short) (backingBuffer.getShort(offset + IDX_WORD_FLAGS_AND_FRAGMENT_OFFSET) & 0x1FFF);
    }

    public short getTTL() {
        return NumberUtils.asUnsigned(backingBuffer.get(offset + IDX_BYTE_TIME_TO_LIVE));
    }

    public short getProtocolId() {
        return NumberUtils.asUnsigned(backingBuffer.get(offset + IDX_BYTE_PROTOCOL_ID));
    }

    public short getHeaderChecksum() {
        return backingBuffer.getShort(offset + IDX_WORD_CHECKSUM);
    }

    public Inet4Address getSourceAddress() {
        return readIPv4Address(offset + IDX_DWORD_SOURCE_ADDRESS);
    }

    public Inet4Address getDestinationAddress() {
        return readIPv4Address(offset + IDX_DWORD_DESTINATION_ADDRESS);
    }

    public byte[] getOptions() {
        final int optionsSize = getHeaderSize() - IDX_BLOB_OPTIONS;
        if (optionsSize == 0) return null;

        final byte[] options = new byte[optionsSize];
        ((ByteBuffer) backingBuffer.duplicate().position(offset + IDX_BLOB_OPTIONS)).get(options);
        return options;
    }

    @Override
    protected ProtocolLayer buildNextLayer(int nextOffset) {
        return LayerFactory.getFactory(LayerFactory.LAYER_TRANSPORT).detectLayer(this, backingBuffer, nextOffset);
    }

    private Inet4Address readIPv4Address(int index) {
        final byte[] addressBytes = new byte[4];
        ((ByteBuffer) backingBuffer.duplicate().position(index)).get(addressBytes);
        try {
            return (Inet4Address) InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            // TODO: Should we throw or log here?
            return null;
        }
    }

}
