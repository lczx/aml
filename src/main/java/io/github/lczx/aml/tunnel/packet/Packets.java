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

import io.github.lczx.aml.tunnel.packet.buffer.ByteBufferPool;
import io.github.lczx.aml.tunnel.packet.editor.PayloadEditor;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * Utility {@link Packet} builder class.
 */
public final class Packets {

    public static final int LAYER_DATA_LINK = 2;
    public static final int LAYER_NETWORK = 3;
    public static final int LAYER_TRANSPORT = 4;

    private static final byte[] IPV4_SKEL = { // Bare IPv4 header template
            /*ver: 4, IHL: 5*/ 0x45, /*ToS: 0*/ 0x00, /*total len: 20*/ 0x00, 0x14, /*ident: 0*/ 0x00, 0x00,
            /*don't fragment*/ 0x40, 0x00, /*TTL: 128*/ (byte) 0x80, /*proto: 0xFF*/ (byte) 0xff,
            /*checksum: 0*/ 0x00, 0x00, /*src. addr.*/ 0x00, 0x00, 0x00, 0x00, /*dst. addr.*/ 0x00, 0x00, 0x00, 0x00};

    private static final byte[] TCP_SKEL = { // Bare TCP header template}
            /*src. port*/ 0x00, 0x00, /*dst. port*/ 0x00, 0x00, /*seq. nr.*/ 0x00, 0x00, 0x00, 0x00,
            /*ack. nr.*/ 0x00, 0x00, 0x00, 0x00, /*data offset: 5 (20), no flags*/ 0x50, 0x00,
            /*wnd. size: 65535*/ (byte) 0xFF, (byte) 0xFF, /*checksum: 0*/ 0x00, 0x00, /*urg. ptr.*/ 0x00, 0x00};

    private static final byte[] UDP_SKEL = { // Bare UDP header template
            /*src. port*/ 0x00, 0x00, /*dst. port*/ 0x00, 0x00, /*length: 8*/ 0x00, 0x08, /*checksum: 0*/ 0x00, 0x00};

    private Packets() { }

    /**
     * Allocates an empty buffer for filling and attachment to a packet through {@link #wrapBuffer(int, ByteBuffer)}.
     *
     * @return A frame-sized buffer
     */
    public static ByteBuffer createBuffer() {
        return ByteBufferPool.acquire();
    }

    /**
     * Wraps a {@link ByteBuffer} inside a {@link Packet}.
     *
     * @param topLayerType The top level protocol in the packet
     * @param buffer       The buffer to wrap
     * @return A packet wrapping the content of the buffer
     */
    public static Packet wrapBuffer(final int topLayerType, final ByteBuffer buffer) {
        return new PacketImpl(topLayerType, buffer);
    }

    /**
     * Clones a packet, allocating a new buffer through {@link #createBuffer()}. No caches are copied.
     *
     * @param toCopy The packet to clone
     * @return A copy of the given packet
     */
    public static Packet makeCopy(final Packet toCopy) {
        return wrapBuffer(toCopy.getTopLayerType(), ((ByteBuffer) createBuffer().put(toCopy.getBufferView()).flip()));
    }

    /**
     * Creates an empty IPv4 packet with <i>don't fragment</i> flag and a TTL value of 128.
     *
     * @param protoId The value of the <i>protocol ID</i> field
     * @param srcAddr The <i>source address</i> of the packet
     * @param dstAddr THe <i>destination address</i> of the packet
     * @return The built packet
     */
    public static Packet newRawV4Packet(final int protoId, final Inet4Address srcAddr, final Inet4Address dstAddr) {
        final Packet p = wrapBuffer(LAYER_NETWORK, (ByteBuffer) createBuffer().put(IPV4_SKEL).flip());
        p.getLayer(IPv4Layer.class).editor()
                .setProtocolId(protoId).setSourceAddress(srcAddr).setDestinationAddress(dstAddr).commit();
        return p;
    }

    /**
     * Creates an empty UDP datagram.
     *
     * @param srcSock The source socket, consisting of IPv4 address + port
     * @param dstSock The destination socket, consisting of IPv4 address + port
     * @return The built packet
     */
    public static Packet newDatagramPacket(final InetSocketAddress srcSock, final InetSocketAddress dstSock) {
        final Packet p = newRawV4Packet(IPv4Layer.PROTOCOL_UDP,
                (Inet4Address) srcSock.getAddress(), (Inet4Address) dstSock.getAddress());

        // We could generate an header via editor but we can't change the length field; we use a template instead
        final PayloadEditor e = p.getLayer(IPv4Layer.class).payloadEditor();
        e.buffer(true).put(UDP_SKEL);
        e.flipAndCommit();
        p.getLayer(UdpLayer.class).editor()
                .setSourcePort(srcSock.getPort()).setDestinationPort(dstSock.getPort()).commit();
        return p;
    }

    /**
     * Creates an empty TCP segment. <i>Urgent pointer</i> is set to zero.
     *
     * @param srcSock    The source socket, consisting of IPv4 address + port
     * @param dstSock    The destination socket, consisting of IPv4 address + port
     * @param seqNr      The <i>sequence number</i> of the segment
     * @param ackNr      The <i>acknowledgement number</i> of the segment
     * @param flags      The TCP <i>flags</i> of the segment
     * @param windowSize The <i>window size</i> field of the segment
     * @return The built packet
     */
    public static Packet newSegmentPacket(final InetSocketAddress srcSock, final InetSocketAddress dstSock,
                                          final long seqNr, final long ackNr, final int flags, final int windowSize) {
        return newSegmentPacket(srcSock, dstSock, seqNr, ackNr, flags, windowSize, null);
    }

    /**
     * Creates an empty TCP segment with options. <i>Urgent pointer</i> is set to zero.
     *
     * @param srcSock    The source socket, consisting of IPv4 address + port
     * @param dstSock    The destination socket, consisting of IPv4 address + port
     * @param seqNr      The <i>sequence number</i> of the segment
     * @param ackNr      The <i>acknowledgement number</i> of the segment
     * @param flags      The TCP <i>flags</i> of the segment
     * @param windowSize The <i>window size</i> field of the segment
     * @param tcpOptions <i>Options</i> to add to the TCP header
     * @return The built packet
     */
    public static Packet newSegmentPacket(final InetSocketAddress srcSock, final InetSocketAddress dstSock,
                                          final long seqNr, final long ackNr, final int flags, final int windowSize,
                                          final byte[] tcpOptions) {
        final Packet p = newRawV4Packet(IPv4Layer.PROTOCOL_TCP,
                (Inet4Address) srcSock.getAddress(), (Inet4Address) dstSock.getAddress());

        final PayloadEditor e = p.getLayer(IPv4Layer.class).payloadEditor();
        e.buffer(true).put(TCP_SKEL);
        e.flipAndCommit();
        p.getLayer(TcpLayer.class).editor()
                .setSourcePort(srcSock.getPort()).setDestinationPort(dstSock.getPort())
                .setSequenceNumber(seqNr).setAcknowledgementNumber(ackNr)
                .setFlags(flags).setWindowSize(windowSize).setOptions(tcpOptions).commit();
        return p;
    }

}
