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

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeTrue;

final class PacketTestUtils {

    static final Random RANDOM = ThreadLocalRandom.current();

    private PacketTestUtils() { }

    static byte[] dumpBuffer(final ByteBuffer buffer) {
        final byte[] ret = new byte[buffer.remaining()];
        buffer.get(ret);
        return ret;
    }

    static void checkBufferSlices(final Packet packet, final int totalSize,
                                  final Class<? extends ProtocolLayer> expectedTLClass) {
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
        assertEquals(totalSize - ipHSiz, payloadView.capacity());

        final ByteBuffer payloadEdit = ip.payloadEditor().buffer();
        assertEquals(0, payloadEdit.position());
        assertEquals(totalSize - ipHSiz, payloadEdit.limit());
        assertEquals(ByteBufferPool.BUFFER_SIZE - ipHSiz, payloadEdit.capacity());

        final ByteBuffer payloadEdit2 = ip.payloadEditor().buffer(true);
        assertEquals(0, payloadEdit2.position());
        assertEquals(ByteBufferPool.BUFFER_SIZE - ipHSiz, payloadEdit2.limit());
        assertEquals(ByteBufferPool.BUFFER_SIZE - ipHSiz, payloadEdit2.capacity());

        if (expectedTLClass != null) {
            final ProtocolLayer<?> tr = packet.getLayer(expectedTLClass);
            final int trHSiz = tr.getHeaderSize();

            final ByteBuffer transportView = tr.getBufferView();
            assertEquals(0, transportView.position());
            assertEquals(totalSize - ipHSiz, transportView.limit());
            assertEquals(totalSize - ipHSiz, transportView.capacity());

            final ByteBuffer transportPayloadView = tr.getPayloadBufferView();
            assertEquals(0, transportPayloadView.position());
            assertEquals(totalSize - ipHSiz - trHSiz, transportPayloadView.limit());
            assertEquals(totalSize - ipHSiz - trHSiz, transportPayloadView.capacity());

            final ByteBuffer transportPayloadEditView = tr.payloadEditor().buffer();
            assertEquals(0, transportPayloadEditView.position());
            assertEquals(totalSize - ipHSiz - trHSiz, transportPayloadEditView.limit());
            assertEquals(ByteBufferPool.BUFFER_SIZE - ipHSiz - trHSiz, transportPayloadEditView.capacity());

            final ByteBuffer transportPayloadEditView2 = tr.payloadEditor().buffer(true);
            assertEquals(0, transportPayloadEditView2.position());
            assertEquals(ByteBufferPool.BUFFER_SIZE - ipHSiz - trHSiz, transportPayloadEditView2.limit());
            assertEquals(ByteBufferPool.BUFFER_SIZE - ipHSiz - trHSiz, transportPayloadEditView2.capacity());
        }
    }

    static void checkTransportPayloadEdit(final Packet packet, final Class<? extends ProtocolLayer> expectedTLClass) {
        final IPv4Layer ip = packet.getLayer(IPv4Layer.class);
        final ProtocolLayer<?> transportLayer = packet.getLayer(expectedTLClass);
        assumeTrue("Payload should be non empty for this test to run",
                transportLayer.getPayloadSize() != 0);

        final int bufferLimit = packet.getBufferView().limit();
        final int ipHeaderSize = ip.getHeaderSize();
        final int ipTotalSize = ip.getTotalSize();
        final short ipChecksum = ip.getHeaderChecksum();

        // Put a small edit at the beginning of the payload (assuming that it isn't the same content as original)
        final PayloadEditor payloadEditor = transportLayer.payloadEditor();
        payloadEditor.buffer().put((byte) 0x01);
        payloadEditor.commit(); // Without flipping, we don't want to change the limit (and size of the payload)

        // Test IP checksum values
        final short newIpChecksum = ip.getHeaderChecksum();
        assertEquals("Calculated IP checksum must match field value",
                newIpChecksum, ip.calculateChecksum());
        assertEquals("IP checksum should not change after payload edit (without size change)",
                ipChecksum, newIpChecksum);

        // Check sizes
        assertEquals(bufferLimit, packet.getBufferView().limit());
        assertEquals(ipHeaderSize, ip.getHeaderSize());
        assertEquals(ipTotalSize, ip.getTotalSize());

        // Check views consistency
        checkBufferSlices(packet, bufferLimit, transportLayer.getClass());
    }

    static void checkIpOptionsChange(final Packet packet, final Class<? extends ProtocolLayer> tlClass) {
        final IPv4Layer ip = packet.getLayer(IPv4Layer.class);
        ProtocolLayer<?> tp = packet.getLayer(tlClass);

        int prevSrcPort = -1, prevDstPort = -1;
        if (UdpLayer.class == tlClass) {
            prevSrcPort = ((UdpLayer) tp).getSourcePort();
            prevDstPort = ((UdpLayer) tp).getDestinationPort();
        } else if (TcpLayer.class == tlClass) {
            prevSrcPort = ((TcpLayer) tp).getSourcePort();
            prevDstPort = ((TcpLayer) tp).getDestinationPort();
        }
        final int prevSize = tp.getTotalSize();
        final byte[] prevTransportPayload = dumpBuffer(tp.getPayloadBufferView());

        final int optsLen = RANDOM.nextInt(11) * 4;
        try {
            ip.editor().setOptions(new byte[optsLen]).commit();
        } catch (final RelocationException e) {
            assumeNoException("We were lucky enough that our random options + our " +
                    "random original payload overflowed the buffer, skip test", e);
        }

        // Retrieve the transport layer again: it has been rebuilt with the new offset
        tp = packet.getLayer(tlClass);
        assertEquals(ip.getHeaderSize(), IPv4Layer.IDX_BLOB_OPTIONS + optsLen);
        assertEquals(ip.getHeaderSize(), tp.getBufferOffset());

        if (UdpLayer.class == tlClass) {
            assertEquals(prevSrcPort, ((UdpLayer) tp).getSourcePort());
            assertEquals(prevDstPort, ((UdpLayer) tp).getDestinationPort());
        } else if (TcpLayer.class == tlClass) {
            assertEquals(prevSrcPort, ((TcpLayer) tp).getSourcePort());
            assertEquals(prevDstPort, ((TcpLayer) tp).getDestinationPort());
        }
        assertEquals(prevSize, tp.getTotalSize());
        assertArrayEquals(prevTransportPayload, dumpBuffer(tp.getPayloadBufferView()));

        checkBufferSlices(packet, ip.getTotalSize(), tlClass);
    }

}
