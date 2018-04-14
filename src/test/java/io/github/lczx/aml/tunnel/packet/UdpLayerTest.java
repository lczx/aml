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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static io.github.lczx.aml.tunnel.packet.PacketTestUtils.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(Parameterized.class)
public class UdpLayerTest {

    private final Packet packet;
    private final int payloadSize;
    private final IPv4Layer ip;
    private final UdpLayer udp;

    public UdpLayerTest(final Packet packet, final int payloadSize) {
        this.packet = packet;
        this.payloadSize = payloadSize;
        ip = packet.getLayer(IPv4Layer.class);
        udp = packet.getLayer(UdpLayer.class);
    }

    @Parameterized.Parameters
    public static List<Object[]> input() throws UnknownHostException {
        final List<Object[]> testParams = new ArrayList<>(10);

        for (int i = 0; i < 10; ++i) {
            final byte[] addrBytes = new byte[4];

            // Create a datagram with random source and destination
            RANDOM.nextBytes(addrBytes);
            final InetSocketAddress dummySockAddr1 = new InetSocketAddress(
                    InetAddress.getByAddress(addrBytes), RANDOM.nextInt(0x10000));
            RANDOM.nextBytes(addrBytes);
            final InetSocketAddress dummySockAddr2 = new InetSocketAddress(
                    InetAddress.getByAddress(addrBytes), RANDOM.nextInt(0x10000));
            final Packet packet = Packets.newDatagramPacket(dummySockAddr1, dummySockAddr2);

            // Add a random payload
            final PayloadEditor e = packet.getLayer(UdpLayer.class).payloadEditor();
            final ByteBuffer b = e.buffer();
            final int rndPayloadSize = RANDOM.nextInt(b.capacity() + 1);
            b.limit(rndPayloadSize);
            e.commit();

            testParams.add(new Object[]{packet, rndPayloadSize});
        }

        return testParams;
    }

    @Test
    public void integrityTest() {
        assertEquals("UDP layer offset should be at end of IP header",
                ip.getHeaderSize(), udp.getBufferOffset());
        assertEquals("IP payload should match UDP total size",
                ip.getPayloadSize(), udp.getTotalSize());
        assertEquals("UDP header should always be 8 bytes",
                UdpLayer.HEADER_SIZE, udp.getHeaderSize());
        assertEquals("UDP total length should match expected",
                UdpLayer.HEADER_SIZE + payloadSize, udp.getTotalLength());

        checkBufferSlices(packet, ip.getHeaderSize() + udp.getHeaderSize() + payloadSize, UdpLayer.class);
    }

    @Test
    public void headerEditChecksumTest() {
        final short ipChecksum = ip.getHeaderChecksum();
        udp.editor()
                .setSourcePort(RANDOM.nextInt(0x10000))
                .setDestinationPort(RANDOM.nextInt(0x10000))
                .commit();

        assertEquals("IP checksum should not change (stored)", ipChecksum, ip.getHeaderChecksum());
        assertEquals("IP checksum should not change (computed)", ipChecksum, ip.calculateChecksum());

        // --> UDP checksum is not implemented, otherwise test it here <--

        checkBufferSlices(packet, ip.getTotalSize(), UdpLayer.class);
    }

    @Test
    public void payloadContentChangeTest() {
        final int udpTotalLength = udp.getTotalLength();
        assertEquals(ip.getTotalSize() - ip.getHeaderSize(), udpTotalLength);
        checkTransportPayloadEdit(packet, UdpLayer.class);
        assertEquals(udpTotalLength, udp.getTotalLength());
    }

    @Test
    public void payloadSizeChangeTest() {
        final int ipHeaderSize = ip.getHeaderSize();
        final int oldUdpPayloadSize = udp.getPayloadSize();
        final short oldIpChecksum = ip.getHeaderChecksum();

        // Randomize the buffer's limit to simulate a random-size edit
        final PayloadEditor payloadEditor = udp.payloadEditor();
        final ByteBuffer payloadBuffer = payloadEditor.buffer();
        final int randomSize = RANDOM.nextInt(payloadBuffer.capacity() + 1);
        payloadBuffer.limit(randomSize);
        final byte[] bufferAfterEdit = dumpBuffer(payloadBuffer);
        payloadEditor.commit();

        // Check sizes changed
        assertEquals(ipHeaderSize, ip.getHeaderSize());
        assertEquals(randomSize, udp.getPayloadSize());
        assertEquals(ip.getTotalSize(), ip.getHeaderSize() + udp.getTotalSize());
        assertEquals(ip.getPayloadSize(), udp.getTotalSize());
        assertEquals(ip.getPayloadSize(), udp.getHeaderSize() + randomSize);

        final int newTotalSize = ipHeaderSize + udp.getHeaderSize() + randomSize;
        assertEquals(newTotalSize, ip.getTotalSize());

        // Test checksum changed
        if (randomSize != oldUdpPayloadSize) {
            assertNotEquals(oldIpChecksum, ip.getHeaderChecksum());
            assertEquals(ip.getHeaderChecksum(), ip.calculateChecksum());
        }

        // --> UDP checksum is not implemented, otherwise test it here <--

        // Check payload consistency
        assertArrayEquals(bufferAfterEdit, dumpBuffer(udp.getPayloadBufferView()));

        // Check views consistency
        checkBufferSlices(packet, newTotalSize, UdpLayer.class);

        // Check backing buffer limit (backing buffer pointers are preserved to the view without need to detach)
        assertEquals(newTotalSize, packet.getBufferView().limit());
    }

    @Test
    public void ipOptionsChangeTest() {
        checkIpOptionsChange(packet, UdpLayer.class);
    }

}
