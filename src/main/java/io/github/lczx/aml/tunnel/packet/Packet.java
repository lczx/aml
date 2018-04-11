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

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Packet {

    private final ByteBuffer backingBuffer;
    private ProtocolLayer firstLayer;

    public Packet(ByteBuffer buffer) {
        Objects.requireNonNull(buffer, "Buffer must not be null");
        this.backingBuffer = buffer;
    }

    public ProtocolLayer getFirstLayer() {
        if (firstLayer == null)
            firstLayer = LayerFactory.getIPLayer(backingBuffer);
        return firstLayer;
    }

    @SuppressWarnings("unchecked")
    public <T extends ProtocolLayer> T getLayer(Class<T> clazz) {
        ProtocolLayer nextLayer = getFirstLayer();
        while (nextLayer != null && nextLayer.getClass() != clazz)
            nextLayer = nextLayer.getNextLayer();
        return (T) nextLayer;
    }

    public List<ProtocolLayer> getLayers() {
        List<ProtocolLayer> list = new LinkedList<>();
        ProtocolLayer nextLayer = getFirstLayer();
        list.add(nextLayer);

        while ((nextLayer = nextLayer.getNextLayer()) != null)
            list.add(nextLayer);
        return list;
    }

}
