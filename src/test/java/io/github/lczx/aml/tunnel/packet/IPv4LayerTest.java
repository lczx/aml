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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

@RunWith(Parameterized.class)
public class IPv4LayerTest {

    private static final Random RANDOM = ThreadLocalRandom.current();

    private final byte[] packetRaw;
    private final Packet packet;
    private final IPv4Layer ip;

    public IPv4LayerTest(byte[] packetRaw) {
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
        checkBufferSlices(packet, packetRaw.length);
    }

    @Test
    public void payloadContentChangeTest() {
        assumeTrue("Payload should be non empty for this test to run", ip.getPayloadSize() != 0);

        final ProtocolLayer<?> oldNextLayer = ip.getNextLayer();
        final short oldChecksum = ip.getHeaderChecksum();

        // Put a small edit at the beginning of the payload
        PayloadEditor payloadEditor = ip.payloadEditor();
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
        checkBufferSlices(packet, packetRaw.length);
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
        checkBufferSlices(packet, newTotalSize);

        // Check backing buffer limit (backing buffer pointers are preserved to the view without need to detach)
        assertEquals(newTotalSize, packet.getBufferView().limit());
    }

    private static byte[] dumpBuffer(ByteBuffer buffer) {
        final byte[] ret = new byte[buffer.remaining()];
        buffer.get(ret);
        return ret;
    }

    private static void checkBufferSlices(final Packet packet, final int totalSize) {
        final IPv4Layer ip = packet.getLayer(IPv4Layer.class);
        final int ipHSiz = ip.getHeaderSize();

        final ByteBuffer view = packet.getBufferView();
        assertEquals(0, view.position());
        assertEquals(totalSize, view.limit());
        assertEquals(ByteBufferPool.BUFFER_SIZE, view.capacity());

        final ByteBuffer layerView = ip.getBufferView();
        assertEquals(0, layerView.position());
        assertEquals(totalSize, layerView.limit());
        assertEquals(totalSize, layerView.capacity());

        final ByteBuffer payloadView = ip.getPayloadBufferView();
        assertEquals(0, payloadView.position());
        assertEquals(totalSize - ipHSiz, payloadView.limit());
        assertEquals(totalSize - ipHSiz, payloadView.limit());
    }

}
