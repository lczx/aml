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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class IPv4LayerTest {

    private static final Random RANDOM = ThreadLocalRandom.current();

    private final byte[] packetRaw;
    private final Packet packet;
    private final IPv4Layer ip;

    public IPv4LayerTest(byte[] packetRaw) {
        this.packetRaw = packetRaw;
        this.packet = Packets.wrapBuffer((ByteBuffer) Packets.createBuffer().put(packetRaw).flip());
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

}
