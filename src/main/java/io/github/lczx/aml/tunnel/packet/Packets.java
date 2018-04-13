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

public final class Packets {

    public static final int LAYER_DATA_LINK = 2;
    public static final int LAYER_NETWORK = 3;
    public static final int LAYER_TRANSPORT = 4;

    private static final byte[] IPV4_SKEL = { // Bare IPv4 header template
            /*ver: 4, IHL: 5*/ 0x45, /*ToS: 0*/ 0x00, /*total len: 20*/ 0x00, 0x14, /*ident: 0*/ 0x00, 0x00,
            /*don't fragment*/ 0x40, 0x00, /*TTL: 128*/ (byte) 0x80, /*proto: 0xFF*/ (byte) 0xff,
            /*checksum: 0*/ 0x00, 0x00, /*src. addr.*/ 0x00, 0x00, 0x00, 0x00, /*dst. addr.*/ 0x00, 0x00, 0x00, 0x00 };

    private static final byte[] TCP_SKEL = { // Bare TCP header template}
            /*src. port*/ 0x00, 0x00, /*dst. port*/ 0x00, 0x00, /*seq. nr.*/ 0x00, 0x00, 0x00, 0x00,
            /*ack. nr.*/ 0x00, 0x00, 0x00, 0x00, /*data offset: 5 (20), no flags*/ 0x50, 0x00,
            /*wnd. size: 65535*/ (byte) 0xFF, (byte) 0xFF, /*checksum: 0*/ 0x00, 0x00, /*urg. ptr.*/ 0x00, 0x00 };

    private static final byte[] UDP_SKEL = { // Bare UDP header template
            /*src. port*/ 0x00, 0x00, /*dst. port*/ 0x00, 0x00, /*length: 8*/ 0x00, 0x08, /*checksum: 0*/ 0x00, 0x00 };

    private Packets() { }

    public static ByteBuffer createBuffer() {
        return ByteBufferPool.acquire();
    }

    public static Packet wrapBuffer(final int topLayerType, final ByteBuffer buffer) {
        return new PacketImpl(topLayerType, buffer);
    }

    public static Packet newRawV4Packet(final int protoId, final Inet4Address srcAddr, final Inet4Address dstAddr) {
        final Packet p = wrapBuffer(LAYER_NETWORK, (ByteBuffer) createBuffer().put(IPV4_SKEL).flip());
        p.getLayer(IPv4Layer.class).editor()
                .setProtocolId(protoId).setSourceAddress(srcAddr).setDestinationAddress(dstAddr).commit();
        return p;
    }

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

    public static Packet newSegmentPacket(final InetSocketAddress srcSock, final InetSocketAddress dstSock,
                                          final long seqNr, final long ackNr, final int flags, final int windowSize) {
        return newSegmentPacket(srcSock, dstSock, seqNr, ackNr, flags, windowSize, null);
    }

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
