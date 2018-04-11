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

public abstract class AbstractProtocolLayer implements ProtocolLayer {

    protected final ByteBuffer backingBuffer;
    protected final int offset;
    private final ProtocolLayer parentLayer;
    private ProtocolLayer nextLayer;

    public AbstractProtocolLayer(ProtocolLayer parentLayer, ByteBuffer backingBuffer, int offset) {
        this.parentLayer = parentLayer;
        this.backingBuffer = backingBuffer;
        this.offset = offset;
    }

    @Override
    public ProtocolLayer getParentLayer() {
        return parentLayer;
    }

    @Override
    public ProtocolLayer getNextLayer() {
        if (nextLayer == null)
            nextLayer = buildNextLayer(offset + getHeaderSize());
        return nextLayer;
    }

    @Override
    public int getBufferOffset() {
        return offset;
    }

    @Override
    public int getPayloadSize() {
        return getTotalSize() - getHeaderSize();
    }

    @Override
    public ByteBuffer getBufferView() {
        return makeBufferView(offset, getTotalSize()).asReadOnlyBuffer();
    }

    @Override
    public ByteBuffer getPayloadBufferView() {
        return makeBufferView(offset + getHeaderSize(), getPayloadSize()).asReadOnlyBuffer();
    }

    protected abstract ProtocolLayer buildNextLayer(int nextOffset);

    private ByteBuffer makeBufferView(int offset, int size) {
        ByteBuffer view = backingBuffer.duplicate();
        view.position(offset);
        view.limit(offset + size);
        return view.slice();
    }

}
