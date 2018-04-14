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

import io.github.lczx.aml.tunnel.packet.editor.PayloadEditor;
import org.junit.Test;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static io.github.lczx.aml.tunnel.packet.PacketTestUtils.dumpBuffer;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PacketFactoryTest {

    private static final byte[] SAMPLE_PAYLOAD = "Hi... world?".getBytes(StandardCharsets.UTF_8);

    @Test
    public void wrapBufferTest() {
        final ByteBuffer b = Packets.createBuffer().put(PacketSamples.SAMPLE_PACKET_01);
        final Packet p = Packets.wrapBuffer(Packets.LAYER_NETWORK, (ByteBuffer) b.flip());
        assertArrayEquals(PacketSamples.SAMPLE_PACKET_01, dumpBuffer(p.getBufferView()));
    }

    @Test
    public void packetCloneTest() {
        final ByteBuffer b = Packets.createBuffer().put(PacketSamples.SAMPLE_PACKET_04);

        final Packet p1 = Packets.wrapBuffer(Packets.LAYER_NETWORK, (ByteBuffer) b.flip());
        final ProtocolLayer<?> ipLayer1 = p1.getFirstLayer();

        final Packet p2 = Packets.makeCopy(p1);
        final ProtocolLayer<?> ipLayer2 = p2.getFirstLayer();

        assertArrayEquals(dumpBuffer(p1.getBufferView()), dumpBuffer(p2.getBufferView()));
        assertNotSame(ipLayer1, ipLayer2);
    }

    @Test
    public void createRawIPPacketTest() throws UnknownHostException {
        final int protoId = 0xFF;
        final Inet4Address srcAddr = (Inet4Address) InetAddress.getByAddress(new byte[]{10, 0, 0, 1});
        final Inet4Address dstAddr = (Inet4Address) InetAddress.getByAddress(new byte[]{(byte) 149, 20, 4, 15});
        final Packet p = Packets.newRawV4Packet(protoId, srcAddr, dstAddr);
        putLayerPayload(p.getFirstLayer(), SAMPLE_PAYLOAD);

        final IPv4Layer ip = p.getLayer(IPv4Layer.class);
        assertEquals(20, ip.getHeaderSize());
        assertEquals(0, ip.getDiffServicesAndECN());
        assertEquals(SAMPLE_PAYLOAD.length, ip.getPayloadSize());
        assertEquals(0, ip.getIdentificationField());
        assertTrue(ip.hasDoNotFragmentFlag());
        assertFalse(ip.hasMoreFragmentsFlag());
        assertEquals(128, ip.getTTL());
        assertEquals(protoId, ip.getProtocolId());
        assertNotEquals(0, ip.getHeaderChecksum());
        assertEquals(ip.getHeaderChecksum(), ip.calculateChecksum());
        assertEquals(srcAddr, ip.getSourceAddress());
        assertEquals(dstAddr, ip.getDestinationAddress());
        assertNull(ip.getOptions());
        assertNull(ip.getNextLayer()); // Cause unknown/reserved protocol ID
        assertArrayEquals(SAMPLE_PAYLOAD, dumpBuffer(ip.getPayloadBufferView()));
    }

    @Test
    public void createTCPSegmentTest() throws UnknownHostException {
        final InetSocketAddress srcSock =
                new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 0, 1}), 30666);
        final InetSocketAddress dstSock =
                new InetSocketAddress(InetAddress.getByAddress(new byte[]{(byte) 149, 20, 4, 15}), 443);
        final int seqNum = 1234567;
        final int ackNum = 7654321;
        final int flags = TcpLayer.FLAG_PSH | TcpLayer.FLAG_ACK;
        final int wndSize = 0xFEFE;
        final Packet p = Packets.newSegmentPacket(srcSock, dstSock, seqNum, ackNum, flags, wndSize);
        final ProtocolLayer[] layers = p.getLayers().toArray(new ProtocolLayer[2]);
        final IPv4Layer ip = p.getLayer(IPv4Layer.class);
        final TcpLayer tcp = p.getLayer(TcpLayer.class);
        assertArrayEquals(layers, new ProtocolLayer[]{ip, tcp});

        putLayerPayload(tcp, SAMPLE_PAYLOAD);
        assertEquals(IPv4Layer.PROTOCOL_TCP, ip.getProtocolId());
        assertEquals(tcp.getHeaderSize() + SAMPLE_PAYLOAD.length, ip.getPayloadSize());
        assertEquals(SAMPLE_PAYLOAD.length, ip.getPayloadSize() - tcp.getHeaderSize());
        assertArrayEquals(SAMPLE_PAYLOAD, dumpBuffer(tcp.getPayloadBufferView()));

        assertEquals(srcSock.getAddress(), ip.getSourceAddress());
        assertEquals(dstSock.getAddress(), ip.getDestinationAddress());

        assertEquals(srcSock.getPort(), tcp.getSourcePort());
        assertEquals(dstSock.getPort(), tcp.getDestinationPort());
        assertEquals(seqNum, tcp.getSequenceNumber());
        assertEquals(ackNum, tcp.getAcknowledgementNumber());
        assertEquals(20, tcp.getHeaderSize());
        assertEquals(flags, tcp.getFlags());
        assertEquals(wndSize, tcp.getWindowSize());
        assertNotEquals(0, tcp.getChecksum());
        assertEquals(tcp.getChecksum(), tcp.calculateChecksum());
        assertEquals(0, tcp.getUrgentPointer());
    }

    @Test
    public void createUDPDatagramTest() throws UnknownHostException {
        final InetSocketAddress srcSock =
                new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 0, 1}), 30666);
        final InetSocketAddress dstSock =
                new InetSocketAddress(InetAddress.getByAddress(new byte[]{(byte) 149, 20, 4, 15}), 443);
        final Packet p = Packets.newDatagramPacket(srcSock, dstSock);
        final ProtocolLayer[] layers = p.getLayers().toArray(new ProtocolLayer[2]);
        final IPv4Layer ip = p.getLayer(IPv4Layer.class);
        final UdpLayer udp = p.getLayer(UdpLayer.class);
        assertArrayEquals(layers, new ProtocolLayer[]{ip, udp});

        putLayerPayload(udp, SAMPLE_PAYLOAD);
        assertEquals(IPv4Layer.PROTOCOL_UDP, ip.getProtocolId());
        assertEquals(udp.getHeaderSize() + SAMPLE_PAYLOAD.length, ip.getPayloadSize());
        assertEquals(udp.getHeaderSize() + SAMPLE_PAYLOAD.length, udp.getTotalLength());
        assertArrayEquals(SAMPLE_PAYLOAD, dumpBuffer(udp.getPayloadBufferView()));

        assertEquals(srcSock.getAddress(), ip.getSourceAddress());
        assertEquals(dstSock.getAddress(), ip.getDestinationAddress());
        assertEquals(srcSock.getPort(), udp.getSourcePort());
        assertEquals(dstSock.getPort(), udp.getDestinationPort());
    }

    private void putLayerPayload(final ProtocolLayer<?> layer, final byte[] payload) {
        final PayloadEditor e = layer.payloadEditor();
        e.buffer(true).put(payload);
        e.flipAndCommit();
    }

}
