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

import java.nio.ByteBuffer;

public final class Packets {

    public static final int LAYER_DATA_LINK = 2;
    public static final int LAYER_NETWORK = 3;
    public static final int LAYER_TRANSPORT = 4;

    private Packets() { }

    public static ByteBuffer createBuffer() {
        return ByteBufferPool.acquire();
    }

    public static Packet wrapBuffer(final int topLayerType, final ByteBuffer buffer) {
        return new PacketImpl(topLayerType, buffer);
    }

}
