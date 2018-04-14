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

import static org.junit.Assert.assertNotSame;

@RunWith(Parameterized.class)
public class PacketTest {

    private final byte[] packetRaw;
    private final Packet packet;

    public PacketTest(final byte[] packetRaw) {
        this.packetRaw = packetRaw;
        this.packet = Packets.wrapBuffer(Packets.LAYER_NETWORK,
                (ByteBuffer) Packets.createBuffer().put(packetRaw).flip());
    }

    @Parameterized.Parameters
    public static List<byte[]> input() {
        return Arrays.asList(PacketSamples.ALL_TCP_SAMPLES);
    }

    @Test
    public void packetBufferDetachTest() {
        final IPv4Layer oldIpLayer = packet.getLayer(IPv4Layer.class);
        packet.attachBuffer(Packets.LAYER_NETWORK, packet.detachBuffer());
        assertNotSame("Protocol layers should be rebuilt on packet buffer detach",
                oldIpLayer, packet.getLayer(IPv4Layer.class));
    }

}
