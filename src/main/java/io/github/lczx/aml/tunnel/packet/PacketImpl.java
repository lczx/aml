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

class PacketImpl implements Packet {

    private int topLayerType = -1;
    private ByteBuffer backingBuffer;
    private ProtocolLayer<?> firstLayer;

    PacketImpl(final int topLayerType, final ByteBuffer buffer) {
        attachBuffer(topLayerType, buffer);
    }

    @Override
    public int getTopLayerType() {
        return topLayerType;
    }

    @Override
    public ProtocolLayer<?> getFirstLayer() {
        if (backingBuffer == null)
            throw new IllegalStateException("Cannot retrieve packet layers, no buffer attached");
        if (firstLayer == null)
            firstLayer = LayerFactory.getFactory(topLayerType).detectLayer(null, backingBuffer, 0);
        return firstLayer;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ProtocolLayer> T getLayer(Class<T> clazz) {
        ProtocolLayer nextLayer = getFirstLayer();
        while (nextLayer != null && nextLayer.getClass() != clazz)
            nextLayer = nextLayer.getNextLayer();
        return (T) nextLayer;
    }

    @Override
    public List<ProtocolLayer> getLayers() {
        final List<ProtocolLayer> list = new LinkedList<>();
        ProtocolLayer nextLayer = getFirstLayer();
        list.add(nextLayer);

        while ((nextLayer = nextLayer.getNextLayer()) != null)
            list.add(nextLayer);
        return list;
    }

    @Override
    public ByteBuffer getBufferView() {
        if (backingBuffer == null)
            throw new IllegalStateException("No buffer attached to this packet");
        return backingBuffer.asReadOnlyBuffer();
    }

    @Override
    public Packet attachBuffer(final int topLayerType, final ByteBuffer buffer) {
        if (backingBuffer != null)
            throw new IllegalStateException("Packet already attached to another buffer");
        if (buffer != null) {
            this.topLayerType = topLayerType;
            buffer.rewind();
        }
        this.backingBuffer = buffer;
        return this;
    }

    @Override
    public ByteBuffer detachBuffer() {
        if (backingBuffer == null)
            throw new IllegalStateException("No buffer attached to this packet");
        final ByteBuffer ret = backingBuffer;
        topLayerType = -1;
        backingBuffer = null;
        firstLayer = null;
        return ret;
    }

    @Override
    public String toString() {
        return "PacketImpl{" +
                "backingBuffer=" + backingBuffer +
                ", layerChain=" + (backingBuffer != null ? getFirstLayer() : null) +
                '}';
    }

}
