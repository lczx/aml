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

package io.github.lczx.aml.tunnel.protocol;

import io.github.lczx.aml.tunnel.packet.IPv4Layer;
import io.github.lczx.aml.tunnel.packet.Packet;
import io.github.lczx.aml.tunnel.packet.Packets;
import io.github.lczx.aml.tunnel.packet.TcpLayer;
import io.github.lczx.aml.tunnel.packet.editor.PayloadEditor;
import io.github.lczx.aml.tunnel.protocol.tcp.TcpNetworkInterface;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class TcpProtocolTest {

    static {
        final Properties props = System.getProperties();
        props.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
        props.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        props.setProperty("org.slf4j.simpleLogger.showShortLogName", "true");
    }

    private static final Logger LOG = LoggerFactory.getLogger(TcpProtocolTest.class);

    private static final int STD_HTTP_PORT = 80;
    private static final String HTTP_HOST = "example.com";
    private static final int WINDOW_SIZE = 0xffff;
    private static final byte[] SYN_TCP_OPTS = // MSS:1460, wnd. scale *256 (8), SACK
            new byte[]{0x02, 0x04, 0x05, (byte) 0xb4, 0x01, 0x03, 0x03, 0x08, 0x01, 0x01, 0x04, 0x02};
    private static final int TIMEOUT_MS = 200;

    private final TcpNetworkInterface tcpNetworkInterface;
    private final ConcurrentLinkedQueue<Packet> networkSink = new ConcurrentLinkedQueue<>();
    private final LinkedBlockingQueue<Packet> networkSource = new LinkedBlockingQueue<>();

    public TcpProtocolTest() throws IOException {
        tcpNetworkInterface = new TcpNetworkInterface(
                new ProtocolTestUtils.DummyContext(),
                new ProtocolTestUtils.PacketConnector(networkSink),
                new ProtocolTestUtils.PacketConnector(networkSource));
        tcpNetworkInterface.start();
    }

    @Test
    public void httpQueryTest() throws UnknownHostException, InterruptedException {
        final InetSocketAddress localSock = new InetSocketAddress(InetAddress.getLocalHost(), 60666);
        final InetSocketAddress remoteSock = new InetSocketAddress(InetAddress.getByName(HTTP_HOST), STD_HTTP_PORT);
        IPv4Layer ip;
        TcpLayer tcp;

        long mSeqN = ThreadLocalRandom.current().nextInt(Short.MAX_VALUE + 1);
        long rSeqN;

        // Send a SYN packet
        final Packet pSyn = Packets.newSegmentPacket(
                localSock, remoteSock, mSeqN, 0, TcpLayer.FLAG_SYN, WINDOW_SIZE, SYN_TCP_OPTS);
        mSeqN++; // SYN counts as a byte
        LOG.info("SYN \t{}", pSyn);
        networkSink.offer(pSyn);

        // Receive and analyze SYN,ACK
        final Packet pSynAck = networkSource.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (pSynAck == null) throw new RuntimeException("Got no answer");
        tcp = pSynAck.getLayer(TcpLayer.class);
        LOG.info("SACK\t{}", pSynAck);
        checkEndpoints(pSynAck, remoteSock, localSock);
        rSeqN = tcp.getSequenceNumber() + 1; // SYN counts as byte
        assertEquals(mSeqN, tcp.getAcknowledgementNumber());
        assertEquals(TcpLayer.FLAG_SYN | TcpLayer.FLAG_ACK, tcp.getFlags());
        assertEquals(WINDOW_SIZE, tcp.getWindowSize());
        assertArrayEquals(SYN_TCP_OPTS, tcp.getOptions());

        // Send ACK to end 3-way handshake
        final Packet pSynAckAck = Packets.newSegmentPacket(
                localSock, remoteSock, mSeqN, rSeqN, TcpLayer.FLAG_ACK, WINDOW_SIZE);
        LOG.info("ACK \t{}", pSynAckAck);
        networkSink.offer(pSynAckAck);

        // Send HTTP request
        final Packet pHttpReq = Packets.newSegmentPacket(
                localSock, remoteSock, mSeqN, rSeqN, TcpLayer.FLAG_PSH | TcpLayer.FLAG_ACK, WINDOW_SIZE);
        final byte[] httpReq = ("GET / HTTP/1.1\r\nHost: " + HTTP_HOST + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        final PayloadEditor payloadEditor = pHttpReq.getLayer(TcpLayer.class).payloadEditor();
        payloadEditor.buffer(true).put(httpReq);
        payloadEditor.flipAndCommit();
        mSeqN += httpReq.length;
        LOG.info("REQ \t{}", pHttpReq);
        networkSink.offer(pHttpReq);

        // Receive and analyze ACK to request
        final Packet pHttpReqAck = networkSource.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (pHttpReqAck == null) throw new RuntimeException("Got no answer");
        tcp = pHttpReqAck.getLayer(TcpLayer.class);
        LOG.info("RACK\t{}", pHttpReqAck);
        checkEndpoints(pHttpReqAck, remoteSock, localSock);
        assertEquals(rSeqN, tcp.getSequenceNumber());
        assertEquals(mSeqN, tcp.getAcknowledgementNumber());
        assertEquals(TcpLayer.FLAG_ACK, tcp.getFlags());
        assertEquals(WINDOW_SIZE, tcp.getWindowSize());
        assertNull(tcp.getOptions());

        // Process incoming packets until done
        int ansCount = 0;
        Packet pHttpAns;
        while ((pHttpAns = networkSource.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS)) != null) {
            ip = (IPv4Layer) pHttpAns.getFirstLayer();
            tcp = (TcpLayer) ip.getNextLayer();

            LOG.info("DATA\t{}", pHttpAns);
            checkEndpoints(pHttpAns, remoteSock, localSock);

            assertEquals(rSeqN, tcp.getSequenceNumber());
            assertEquals(mSeqN, tcp.getAcknowledgementNumber());
            assertEquals(TcpLayer.FLAG_PSH | TcpLayer.FLAG_ACK, tcp.getFlags());
            assertEquals(WINDOW_SIZE, tcp.getWindowSize());
            assertNull(tcp.getOptions());
            rSeqN += tcp.getPayloadSize();
            ansCount++;

            // Can ACK single elements here
        }
        assertNotEquals("No answer to request", 0, ansCount);

        // ACK the received data
        final Packet pHttpAnsAck = Packets.newSegmentPacket(
                localSock, remoteSock, mSeqN, rSeqN, TcpLayer.FLAG_ACK, WINDOW_SIZE);
        LOG.info("DACK\t{}", pHttpAnsAck);
        networkSink.offer(pHttpAnsAck);

        // Send a FIN to start termination of the connection
        final Packet pFin = Packets.newSegmentPacket(
                localSock, remoteSock, mSeqN, rSeqN, TcpLayer.FLAG_FIN | TcpLayer.FLAG_ACK, WINDOW_SIZE);
        LOG.info("FIN \t{}", pFin);
        mSeqN++; // FIN counts as a byte
        networkSink.offer(pFin);

        // Wait for a FIN,ACK
        boolean isFin;
        do {
            // Server answers w/ FIN,ACK the 1st time if we send the "Connection: close" HTTP header,
            // but if we close the connection, it first ACKs our FIN and then closes
            final Packet pFinAck = networkSource.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (pFinAck == null) throw new RuntimeException("Got no answer");
            tcp = pFinAck.getLayer(TcpLayer.class);
            LOG.info("FACK\t{}", pFinAck);
            checkEndpoints(pFinAck, remoteSock, localSock);
            assertEquals(rSeqN, tcp.getSequenceNumber());
            assertEquals(mSeqN, tcp.getAcknowledgementNumber());
            isFin = tcp.isFIN();
            //assertEquals(TCPLayer.FLAG_FIN | TCPLayer.FLAG_ACK, tcp.getFlags());
            assertEquals(WINDOW_SIZE, tcp.getWindowSize());
            assertNull(tcp.getOptions());
            if (isFin) rSeqN++; // FIN counts as a byte
        } while (!isFin);

        // ACK the received FIN,ACK to close
        final Packet pFinAckAck = Packets.newSegmentPacket(
                localSock, remoteSock, mSeqN, rSeqN, TcpLayer.FLAG_ACK, WINDOW_SIZE);
        LOG.info("FAAK\t{}", pFinAckAck);
        networkSink.offer(pFinAckAck);
        Thread.sleep(20);
        tcpNetworkInterface.shutdown();
    }

    private static void checkEndpoints(final Packet packet,
                                       final InetSocketAddress srcSock, final InetSocketAddress dstSock) {
        final IPv4Layer ip = (IPv4Layer) packet.getFirstLayer();
        final TcpLayer tcp = (TcpLayer) ip.getNextLayer();
        assertEquals(dstSock.getAddress(), ip.getDestinationAddress());
        assertEquals(srcSock.getAddress(), ip.getSourceAddress());
        assertEquals(dstSock.getPort(), tcp.getDestinationPort());
        assertEquals(srcSock.getPort(), tcp.getSourcePort());
    }

}
