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

package io.github.lczx.aml.tunnel.network;

import io.github.lczx.aml.tunnel.network.udp.UdpNetworkInterface;
import io.github.lczx.aml.tunnel.packet.IPv4Layer;
import io.github.lczx.aml.tunnel.packet.Packet;
import io.github.lczx.aml.tunnel.packet.Packets;
import io.github.lczx.aml.tunnel.packet.UdpLayer;
import io.github.lczx.aml.tunnel.packet.editor.PayloadEditor;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertEquals;

public class UdpProtocolTest {

    static {
        final Properties props = System.getProperties();
        props.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
        props.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        props.setProperty("org.slf4j.simpleLogger.showShortLogName", "true");
    }

    private static final Logger LOG = LoggerFactory.getLogger(UdpProtocolTest.class);

    private static final String QUERIED_HOSTNAME = "example.com";

    private final InetSocketAddress srcSock; // 127.0.0.1:60666 - src addr doesn't matter anyway
    private final InetSocketAddress dstSock; // 8.8.8.8:53 - 53: std. DNS port
    private final UdpNetworkInterface udpNetworkInterface;
    private final ConcurrentLinkedQueue<Packet> networkSink = new ConcurrentLinkedQueue<>();
    private final LinkedBlockingQueue<Packet> networkSource = new LinkedBlockingQueue<>();

    public UdpProtocolTest() throws IOException {
        srcSock = new InetSocketAddress(Inet4Address.getLocalHost(), 60666);
        dstSock = new InetSocketAddress(InetAddress.getByAddress(new byte[]{8, 8, 8, 8}), 53);
        udpNetworkInterface = new UdpNetworkInterface(
                new ProtocolTestUtils.DummyContext(),
                new ProtocolTestUtils.PacketConnector(networkSink),
                new ProtocolTestUtils.PacketConnector(networkSource));
        udpNetworkInterface.start();
    }

    @Test
    public void dnsQueryTest() throws InterruptedException {
        final short transactionId = (short) ThreadLocalRandom.current().nextInt(Short.MAX_VALUE + 1);
        final Packet query = buildDNSQuery(transactionId);
        networkSink.add(query);

        final Packet answer = networkSource.take();
        final IPv4Layer aIp = answer.getLayer(IPv4Layer.class);
        final UdpLayer aUdp = answer.getLayer(UdpLayer.class);

        assertEquals(srcSock.getAddress(), aIp.getDestinationAddress());
        assertEquals(dstSock.getAddress(), aIp.getSourceAddress());
        assertEquals(srcSock.getPort(), aUdp.getDestinationPort());
        assertEquals(dstSock.getPort(), aUdp.getSourcePort());

        final ByteBuffer r = aUdp.getPayloadBufferView();
        assertEquals(transactionId, r.getShort());
        assertEquals((short) 0x8180, r.getShort()); // Standard response flags, no error
        udpNetworkInterface.shutdown();
    }

    private Packet buildDNSQuery(final short transactionId) {
        final Packet packet = Packets.newDatagramPacket(srcSock, dstSock);
        final PayloadEditor e = packet.getLayer(UdpLayer.class).payloadEditor();
        final ByteBuffer p = e.buffer(true);

        p.putShort(transactionId); // Transaction ID
        p.putShort((short) 0x0100); // Standard query flags
        p.putShort((short) 1); // # of questions: 1
        p.putShort((short) 0); // # of answer RRs: 0
        p.putShort((short) 0); // # of authority RRs: 0
        p.putShort((short) 0); // # of additional RRs: 0

        for (final String part : QUERIED_HOSTNAME.split("\\.")) {
            final byte[] partBytes = part.getBytes(StandardCharsets.UTF_8);
            p.put((byte) partBytes.length);
            p.put(partBytes);
        }
        p.put((byte) 0);
        p.putShort((short) 1); // Type A (IPv4 host address)
        p.putShort((short) 1); // Class: IN

        e.flipAndCommit();
        return packet;
    }

}
