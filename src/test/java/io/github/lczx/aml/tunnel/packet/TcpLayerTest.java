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
import io.github.lczx.aml.tunnel.packet.editor.RelocationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static io.github.lczx.aml.tunnel.packet.PacketTestUtils.*;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeTrue;

@RunWith(Parameterized.class)
public class TcpLayerTest {

    private final byte[] packetRaw;
    private final Packet packet;
    private final IPv4Layer ip;
    private final TcpLayer tcp;

    public TcpLayerTest(final byte[] packetRaw) {
        this.packetRaw = packetRaw;
        this.packet = Packets.wrapBuffer(Packets.LAYER_NETWORK,
                (ByteBuffer) Packets.createBuffer().put(packetRaw).flip());
        ip = packet.getLayer(IPv4Layer.class);
        tcp = packet.getLayer(TcpLayer.class);
    }

    @Parameterized.Parameters
    public static List<byte[]> input() {
        return Arrays.asList(PacketSamples.ALL_TCP_SAMPLES);
    }

    @Test
    public void integrityTest() {
        assertTrue("IPv4 header size should be in range [20, 60]",
                ip.getHeaderSize() >= 20 && ip.getHeaderSize() <= 60);
        assertTrue("TCP header size should be in range [20, 60]",
                tcp.getHeaderSize() >= 20 && tcp.getHeaderSize() <= 60);

        final byte oldReservedBits = getTcpReservedBits(tcp);
        final byte oldDataOffset = tcp.getDataOffset();
        final short oldIpChecksum = ip.getHeaderChecksum();
        final short oldTcpChecksum = tcp.getChecksum();

        assertEquals(packetRaw.length, ip.getHeaderSize() + tcp.getHeaderSize() + tcp.getPayloadSize());
        assertEquals(oldIpChecksum, ip.calculateChecksum());
        assertEquals(oldTcpChecksum, tcp.calculateChecksum());

        tcp.editor().setAcknowledgementNumber(666).commit();

        final short newIpChecksum = ip.getHeaderChecksum();
        final short newTcpChecksum = tcp.getChecksum();

        assertEquals(666, tcp.getAcknowledgementNumber());
        assertEquals(oldIpChecksum, newIpChecksum);
        assertEquals(newIpChecksum, ip.calculateChecksum());
        assertNotEquals(oldTcpChecksum, newTcpChecksum);
        assertEquals(newTcpChecksum, tcp.calculateChecksum());
        assertEquals(oldReservedBits, getTcpReservedBits(tcp));
        assertEquals(oldDataOffset, tcp.getDataOffset()); // <-- field adjacent to edit offset

        checkBufferSlices(packet, packetRaw.length, TcpLayer.class);
    }

    @Test
    public void ipHeaderChangeTest() throws UnknownHostException {
        final short tcpChecksum = tcp.getChecksum();
        final Inet4Address srcAddr = ip.getSourceAddress();
        final byte[] tcpDump = dumpBuffer(ip.getPayloadBufferView());

        // Setting a different IP source or destination address should change TCP checksum
        ip.editor().setSourceAddress((Inet4Address) InetAddress.getByAddress(new byte[]{0, 0, 0, 0})).commit();
        final short tcpChecksum2 = tcp.getChecksum();
        assertNotEquals(tcpChecksum, tcpChecksum2);

        // The same for protocol ID: switch to UDP and restore original address (and so original checksum)
        ip.editor().setProtocolId(IPv4Layer.PROTOCOL_UDP).commit();
        ip.editor().setSourceAddress(srcAddr).commit();
        ip.editor().setProtocolId(IPv4Layer.PROTOCOL_TCP).commit();
        assertNotEquals(tcpChecksum2, tcp.getChecksum());
        assertEquals(tcpChecksum, tcp.getChecksum());

        // Change IP options, shouldn't change TCP at all, including checksum (which is based on IP *payload* size)
        ip.editor().setOptions(new byte[]{0, 0, 0, 0}).commit();

        final TcpLayer tcp = (TcpLayer) ip.getNextLayer(); // Shadow class field because offset changed and this gets rebuilt
        assertEquals(tcpChecksum, tcp.getChecksum());
        assertArrayEquals(tcpDump, dumpBuffer(tcp.getBufferView()));

        // Finally, setting a different IP source or destination address should change TCP checksum
        ip.editor().setDestinationAddress((Inet4Address) InetAddress.getByAddress(new byte[]{0, 0, 0, 0})).commit();
        assertNotEquals(tcpChecksum, tcp.getChecksum());
    }

    @Test
    public void optionsArgumentTest() {
        try {
            tcp.editor().setOptions(new byte[44]).commit();
            fail("TCP header should not accept options longer than 40 bytes");
        } catch (final IllegalArgumentException e) { /* ignore */ }
        try {
            tcp.editor().setOptions(new byte[38]).commit();
            fail("TCP header should not accept options not multiple of 4");
        } catch (final IllegalArgumentException e) { /* ignore */ }
    }

    @Test
    public void optionsEditTest() {
        final byte[] oldOpts = tcp.getOptions();
        assumeTrue("TCP options should be present for this test to run", oldOpts != null);

        final int bufferLimit = packet.getBufferView().limit();
        final short oldChecksum = tcp.getChecksum();
        final byte[] payload = dumpBuffer(tcp.getPayloadBufferView());
        final int dataOffset = tcp.getDataOffset();
        final byte reserved = getTcpReservedBits(tcp);
        final int ipHeaderSize = ip.getHeaderSize();
        final int ipTotSize = ip.getTotalSize();
        final short ipChecksum = ip.getHeaderChecksum();

        // Put new options, same size
        final byte[] newOpts = new byte[oldOpts.length];
        assumeFalse("My purpose was to **change** the options...", Arrays.equals(oldOpts, newOpts));
        tcp.editor().setOptions(newOpts).commit();

        assertEquals(bufferLimit, packet.getBufferView().limit());
        assertEquals(ipChecksum, ip.getHeaderChecksum());
        assertEquals(ip.getHeaderChecksum(), ip.calculateChecksum());
        assertNotEquals(oldChecksum, tcp.getChecksum());
        assertEquals(tcp.getChecksum(), tcp.calculateChecksum());

        assertArrayEquals(newOpts, tcp.getOptions());
        assertEquals(dataOffset, tcp.getDataOffset());
        assertEquals(reserved, getTcpReservedBits(tcp));
        assertArrayEquals(payload, dumpBuffer(tcp.getPayloadBufferView()));
        assertEquals(ipHeaderSize, ip.getHeaderSize());
        assertEquals(ipTotSize, ip.getTotalSize());

        checkBufferSlices(packet, bufferLimit, TcpLayer.class);
    }

    @Test
    public void optionsFillTest() {
        checkOptionsTcp(40);
    }

    @Test
    public void optionsRandomAndClearTest() {
        checkOptionsTcp(RANDOM.nextInt(11) * 4);
        checkOptionsTcp(0);
    }

    @Test
    public void ipOptionsChangeTest() {
        checkIpOptionsChange(packet, TcpLayer.class);
    }

    @Test
    public void tcpPayloadEditTest() {
        final byte dataOffset = tcp.getDataOffset();
        final byte reservedBits = getTcpReservedBits(tcp);
        final short checksum = tcp.getChecksum();
        checkTransportPayloadEdit(packet, TcpLayer.class);
        assertEquals(dataOffset, tcp.getDataOffset());
        assertEquals(reservedBits, getTcpReservedBits(tcp));
        assertNotEquals(checksum, tcp.getChecksum()); // <-- Should sum content too
    }

    @Test
    public void tcpPayloadResizeTest() {
        final int ipHeaderSize = ip.getHeaderSize();
        final byte dataOffset = tcp.getDataOffset();
        final byte reserved = getTcpReservedBits(tcp);
        final short ipChecksum = ip.getHeaderChecksum();
        final short tcpChecksum = tcp.getChecksum();

        final int oldTcpPayloadSize = tcp.getPayloadSize();

        // Randomize the buffer's limit to simulate a resizing edit
        final PayloadEditor payloadEditor = tcp.payloadEditor();
        final ByteBuffer payloadBuffer = payloadEditor.buffer();
        final int randomSize = RANDOM.nextInt(payloadBuffer.capacity() + 1);
        payloadBuffer.limit(randomSize);
        final byte[] bufferAfterEdit = dumpBuffer(payloadBuffer);
        payloadEditor.commit();

        final int newTotalSize = ipHeaderSize + tcp.getHeaderSize() + randomSize;

        // Test IP checksum changed
        if (randomSize != oldTcpPayloadSize) {
            assertNotEquals(ipChecksum, ip.getHeaderChecksum());
            assertEquals(ip.getHeaderChecksum(), ip.calculateChecksum());
        }

        // Test TCP checksum changed
        assertEquals(tcp.getChecksum(), tcp.calculateChecksum());
        assertNotEquals(tcpChecksum, tcp.getChecksum()); // WTF: Got a clash here while testing

        // Check sizes changed
        assertEquals(ipHeaderSize, ip.getHeaderSize());
        assertEquals(randomSize, tcp.getPayloadSize());
        assertEquals(newTotalSize, ip.getTotalSize());
        assertEquals(ip.getTotalSize(), ip.getHeaderSize() + tcp.getTotalSize());
        assertEquals(ip.getPayloadSize(), tcp.getTotalSize());
        assertEquals(ip.getPayloadSize(), tcp.getHeaderSize() + randomSize);

        assertEquals(dataOffset, tcp.getDataOffset());
        assertEquals(reserved, getTcpReservedBits(tcp));

        // Check payload consistency
        assertArrayEquals(bufferAfterEdit, dumpBuffer(tcp.getPayloadBufferView()));

        // Check views consistency
        checkBufferSlices(packet, newTotalSize, TcpLayer.class);

        // Check backing buffer limit (backing buffer pointers are preserved to the view without need to detach)
        assertEquals(newTotalSize, packet.getBufferView().limit());
    }

    private byte getTcpReservedBits(final TcpLayer tcp) {
        return (byte) (tcp.getBufferView().get(TcpLayer.IDX_BYTE_DATA_OFFSET_AND_RESERVED) & 0x0F);
    }

    private void checkOptionsTcp(final int newOptsLen) {
        final byte[] tcpOptions = tcp.getOptions();
        final int optsLen = tcpOptions == null ? 0 : tcpOptions.length;
        final byte[] newOpts = new byte[newOptsLen];
        RANDOM.nextBytes(newOpts);
        assumeFalse("A randomized array equals our original options!!! (-_-)", Arrays.equals(tcpOptions, newOpts));

        final short ipChecksum = ip.getHeaderChecksum();
        final short tcpChecksum = tcp.getChecksum();
        final int ipHeaderSize = ip.getHeaderSize();
        final int ipTotalSize = ip.getTotalSize();
        final int tcpHeaderSize = tcp.getHeaderSize(); // <-- directly mapped to "data offset" field
        final int tcpReservedBits = getTcpReservedBits(tcp); // <-- lower part of "data offset" byte, check unchanged
        final byte[] prevTcpPayload = dumpBuffer(tcp.getPayloadBufferView());

        final int bufferLimit = packet.getBufferView().limit();
        if (newOptsLen > ByteBufferPool.BUFFER_SIZE - bufferLimit) {
            try {
                tcp.editor().setOptions(newOpts).commit();
                fail("Commit operation should have thrown: buffer overflow due to options too large");
            } catch (final RelocationException e) {
                // Checksum, size & payload should not have changed
                assertEquals(ipChecksum, ip.getHeaderChecksum());
                assertEquals(tcpChecksum, tcp.getChecksum());
                assertEquals(ipTotalSize, ip.getTotalSize());
                assertArrayEquals(prevTcpPayload, dumpBuffer(tcp.getPayloadBufferView()));
                assumeNoException("Random parameter would give buffer overflow, packet unchanged (as planned)", e);
            }
        }

        tcp.editor().setOptions(newOpts).commit();
        final int sizeDelta = newOptsLen - optsLen;
        final byte[] newOptsRet = newOpts.length != 0 ? newOpts : null; // <-- as returned by getOptions()

        // Check if data offset changed (but not reserved bits)
        assertEquals(ipHeaderSize, ip.getHeaderSize());
        assertEquals(ipTotalSize + sizeDelta, ip.getTotalSize());
        assertEquals(tcpHeaderSize + sizeDelta, tcp.getHeaderSize());
        assertEquals(tcpReservedBits, getTcpReservedBits(tcp));

        // Checksum should have changed (if size actually changed, or if not, the options are not the same)
        if (sizeDelta != 0 || !Arrays.equals(tcpOptions, newOptsRet)) {
            assertEquals(ip.getHeaderChecksum(), ip.calculateChecksum());
            assertEquals(tcp.getChecksum(), tcp.calculateChecksum());
            // WTF: Got a clash here while testing (once TCP, the other IP)
            assertNotEquals(ipChecksum, ip.getHeaderChecksum()); // <-- total size changed in IP
            assertNotEquals(tcpChecksum, tcp.getChecksum()); // <-- options and data offset changed in TCP
        }

        // Options are actually changed
        assertArrayEquals(newOptsRet, tcp.getOptions());

        // Buffer limit should match
        final int newBufferLimit = packet.getBufferView().limit();
        assertEquals(bufferLimit + sizeDelta, newBufferLimit);

        // Check consistency of views
        checkBufferSlices(packet, newBufferLimit, TcpLayer.class);

        // Payload should not have changed
        assertArrayEquals("Payload should not have changed",
                prevTcpPayload, dumpBuffer(tcp.getPayloadBufferView()));

        // Test if any checksum is out of sync
        assertEquals(ip.getHeaderChecksum(), ip.calculateChecksum());
        assertEquals(tcp.getChecksum(), tcp.calculateChecksum());

        // Final comprehensive integrity test
        final byte[] rawOpts = new byte[newOptsLen];
        final byte[] rawPayload = new byte[newBufferLimit - tcp.getHeaderSize() - ip.getHeaderSize()];
        final int tcpOffset = tcp.getBufferOffset();
        final ByteBuffer rawPacket = packet.detachBuffer();
        rawPacket.position(tcpOffset + TcpLayer.IDX_BLOB_OPTIONS);
        rawPacket.get(rawOpts);
        rawPacket.get(rawPayload);
        assertEquals(0, rawPacket.remaining());
        assertArrayEquals(newOpts, rawOpts);
        assertArrayEquals(prevTcpPayload, rawPayload);

        // Reattach the buffer to allow further testing
        rawPacket.rewind();
        packet.attachBuffer(Packets.LAYER_NETWORK, rawPacket);
    }

}
