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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static io.github.lczx.aml.tunnel.packet.PacketTestUtils.RANDOM;
import static io.github.lczx.aml.tunnel.packet.PacketTestUtils.checkBufferSlices;
import static io.github.lczx.aml.tunnel.packet.PacketTestUtils.dumpBuffer;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeTrue;

@RunWith(Parameterized.class)
public class IPv4LayerTest {

    private final byte[] packetRaw;
    private final Packet packet;
    private final IPv4Layer ip;

    public IPv4LayerTest(final byte[] packetRaw) {
        this.packetRaw = packetRaw;
        this.packet = Packets.wrapBuffer(Packets.LAYER_NETWORK,
                (ByteBuffer) Packets.createBuffer().put(packetRaw).flip());
        this.ip = packet.getLayer(IPv4Layer.class);
    }

    @Parameterized.Parameters
    public static List<byte[]> input() {
        return Arrays.asList(PacketSamples.ALL_TCP_SAMPLES);
    }

    @Test
    public void integrityTest() {
        assertEquals(0, ip.getBufferOffset());

        final short origFlags = ip.getFlags();
        final int origLength = ip.getTotalLength();

        // Test checksum
        final short origChecksum = ip.getHeaderChecksum();
        assertEquals("Calculated IP checksum must match field value",
                origChecksum, ip.calculateChecksum());

        // Edit a field and test checksum + adjacent field & size
        ip.editor()
                .setIdentificationField(RANDOM.nextInt(1 + Short.MAX_VALUE))
                .commit();

        final short newChecksum = ip.getHeaderChecksum();
        assertEquals("Calculated IP checksum must match field value",
                newChecksum, ip.calculateChecksum());
        assertNotEquals("Checksum did not change after edit",
                origChecksum, newChecksum);
        assertEquals("Flags should not change (wrong size edit)",
                origFlags, ip.getFlags());
        assertEquals("Size should not change",
                origLength, ip.getTotalLength());
    }

    @Test
    public void protocolChangeTest() {
        final ProtocolLayer<?> oldNextLayer = ip.getNextLayer();
        ip.editor().setProtocolId(IPv4Layer.PROTOCOL_UDP).commit();
        final ProtocolLayer<?> newNextLayer = ip.getNextLayer();

        if (oldNextLayer != null && newNextLayer != null) {
            assertNotSame("Child layer should have changed by changing protocol ID", oldNextLayer, newNextLayer);
            assertTrue("Old child layer should be TCP", oldNextLayer instanceof TcpLayer);
            assertTrue("New child layer should be UDP", newNextLayer instanceof UdpLayer);
        }
    }

    @Test
    public void bufferSliceTest() {
        checkBufferSlices(packet, packetRaw.length, null);
    }

    @Test
    public void optionsArgumentTest() {
        try {
            ip.editor().setOptions(new byte[44]).commit();
            fail("IP header should not accept options longer than 40 bytes");
        } catch (final IllegalArgumentException e) { /* ignore */ }
        try {
            ip.editor().setOptions(new byte[38]).commit();
            fail("IP header should not accept options not multiple of 4");
        } catch (final IllegalArgumentException e) { /* ignore */ }
    }

    @Test
    public void optionsFillTest() {
        checkOptions(40);
    }

    @Test
    public void optionsRandomAndClearTest() {
        //List<Integer> optsLenSequence = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        //Collections.shuffle(optsLenSequence, RANDOM);
        //for (int len : optsLenSequence) checkOptions(4 * len);
        checkOptions(RANDOM.nextInt(11) * 4);
        checkOptions(0);
    }

    @Test
    public void payloadContentChangeTest() {
        assumeTrue("Payload should be non empty for this test to run", ip.getPayloadSize() != 0);

        final ProtocolLayer<?> oldNextLayer = ip.getNextLayer();
        final short oldChecksum = ip.getHeaderChecksum();

        // Put a small edit at the beginning of the payload
        final PayloadEditor payloadEditor = ip.payloadEditor();
        payloadEditor.buffer().put((byte) 0);
        payloadEditor.commit(); // Without flipping, we don't want to change the limit (and so the size of the payload)

        // Check layer invalidation
        final ProtocolLayer<?> newNextLayer = ip.getNextLayer();
        if (oldNextLayer != null && newNextLayer != null) {
            assertNotSame("Child layer object should be a new instance after rewriting payload directly",
                    oldNextLayer, newNextLayer);
        }

        // Test checksum
        final short newChecksum = ip.getHeaderChecksum();
        assertEquals("Calculated IP checksum must match field value",
                newChecksum, ip.calculateChecksum());
        assertEquals("IP checksum should not change after payload edit (without size change)",
                oldChecksum, newChecksum);

        // Check views consistency
        checkBufferSlices(packet, packetRaw.length, null);
    }

    @Test
    public void payloadSizeChangeTest() {
        final ProtocolLayer<?> oldNextLayer = ip.getNextLayer();
        final int prevHeaderSize = ip.getHeaderSize();
        final int prevPayloadSize = ip.getPayloadSize();
        final int prevChecksum = ip.getHeaderChecksum();

        // Randomize the buffer's limit to simulate a resizing edit
        final PayloadEditor payloadEditor = ip.payloadEditor();
        final ByteBuffer payloadBuffer = payloadEditor.buffer();
        final int randomSize = RANDOM.nextInt(payloadBuffer.capacity() + 1);
        payloadBuffer.limit(randomSize);
        final byte[] bufferAfterEdit = dumpBuffer(payloadBuffer);
        payloadEditor.commit();

        // Check layer invalidation
        final ProtocolLayer<?> newNextLayer = ip.getNextLayer();
        if (oldNextLayer != null && newNextLayer != null) {
            assertNotSame("Child layer object should be a new instance after rewriting payload directly",
                    oldNextLayer, newNextLayer);
        }

        // Check sizes changed
        final int newTotalSize = prevHeaderSize + randomSize;
        assertEquals(prevHeaderSize, ip.getHeaderSize());
        assertEquals(randomSize, ip.getPayloadSize());
        assertEquals(newTotalSize, ip.getTotalSize());

        // Test checksum changed
        if (randomSize != prevPayloadSize) {
            assertNotEquals(prevChecksum, ip.getHeaderChecksum());
            assertEquals(ip.getHeaderChecksum(), ip.calculateChecksum());
        }

        // Check payload consistency
        assertArrayEquals(bufferAfterEdit, dumpBuffer(ip.getPayloadBufferView()));

        // Check views consistency
        checkBufferSlices(packet, newTotalSize, null);

        // Check backing buffer limit (backing buffer pointers are preserved to the view without need to detach)
        assertEquals(newTotalSize, packet.getBufferView().limit());
    }

    private void checkOptions(final int newOptsLen) {
        final byte[] options = ip.getOptions();
        final int optsLen = options == null ? 0 : options.length;
        assertEquals("Options length should be multiple of 4 bytes", 0, optsLen % 4);
        final byte[] newOpts = new byte[newOptsLen];
        RANDOM.nextBytes(newOpts);
        assumeFalse("Our dummy options are the same as original (-_-)", Arrays.equals(options, newOpts));

        final ProtocolLayer<?> nextLayer = ip.getNextLayer();
        final int headerSize = ip.getHeaderSize();
        final int payloadSize = ip.getPayloadSize();
        final int totalSize = ip.getTotalSize();
        final short checksum = ip.getHeaderChecksum();
        final byte[] prevPayload = dumpBuffer(ip.getPayloadBufferView());

        final int bufferLimit = packet.getBufferView().limit();
        if (newOptsLen > ByteBufferPool.BUFFER_SIZE - bufferLimit) {
            try {
                ip.editor().setOptions(newOpts).commit();
                fail("Commit operation should have thrown: buffer overflow due to options too large");
            } catch (final RelocationException e) {
                // Checksum, size & payload should not have changed
                assertEquals(checksum, ip.getHeaderChecksum());
                assertEquals(totalSize, ip.getTotalSize());
                assertArrayEquals(prevPayload, dumpBuffer(ip.getPayloadBufferView()));
                assumeNoException("Random parameter would give buffer overflow, packet unchanged (as planned)", e);
            }
        }

        ip.editor().setOptions(newOpts).commit();
        final int sizeDelta = newOptsLen - optsLen;
        final byte[] newOptsRet = newOpts.length != 0 ? newOpts : null; // <-- as returned by getOptions()

        // Next layer equality should fail
        if (sizeDelta != 0 && nextLayer != null)
            assertNotSame("Next layer should be rebuilt after a payload relocation",
                    nextLayer, ip.getNextLayer());

        // Check if total length and IHL changed
        assertEquals(headerSize + sizeDelta, ip.getHeaderSize());
        assertEquals(payloadSize, ip.getPayloadSize());
        assertEquals(totalSize + sizeDelta, ip.getTotalSize());

        // Checksum should have changed (if size actually changed, or if not, the options are not the same)
        if (sizeDelta != 0 || !Arrays.equals(options, newOptsRet))
            assertNotEquals(checksum, ip.getHeaderChecksum());

        // Options are actually changed
        assertArrayEquals(newOptsRet, ip.getOptions());

        // Buffer limit should match
        assertEquals(bufferLimit + sizeDelta, packet.getBufferView().limit());

        // Check consistency of views
        final ByteBuffer layerView = ip.getBufferView();
        assertEquals(ip.getTotalSize(), layerView.limit());
        assertEquals(ip.getTotalSize(), layerView.capacity());
        final ByteBuffer payloadView = ip.getPayloadBufferView();
        assertEquals(ip.getPayloadSize(), payloadView.limit());
        assertEquals(ip.getPayloadSize(), payloadView.capacity());

        // Payload should not have changed
        assertArrayEquals("Payload should not have changed",
                prevPayload, dumpBuffer(ip.getPayloadBufferView()));

        // Final comprehensive integrity test
        assertEquals(ip.getHeaderChecksum(), ip.calculateChecksum());
        final byte[] rawOpts = new byte[newOptsLen];
        final byte[] rawPayload = new byte[payloadSize];
        final ByteBuffer rawPacket = packet.detachBuffer();
        rawPacket.position(IPv4Layer.IDX_BLOB_OPTIONS);
        rawPacket.get(rawOpts);
        rawPacket.get(rawPayload);
        assertEquals(0, rawPacket.remaining());
        assertArrayEquals(newOpts, rawOpts);
        assertArrayEquals(prevPayload, rawPayload);

        // Reattach the buffer to allow further testing
        rawPacket.rewind();
        packet.attachBuffer(Packets.LAYER_NETWORK, rawPacket);
    }

}
