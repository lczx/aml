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

import io.github.lczx.aml.tunnel.packet.editor.LayerChangeset;
import io.github.lczx.aml.tunnel.packet.editor.LayerEditor;
import io.github.lczx.aml.tunnel.packet.editor.PayloadEditor;

import java.nio.ByteBuffer;

public abstract class AbstractProtocolLayer<E extends LayerEditor> implements ProtocolLayer<E> {

    protected final ByteBuffer backingBuffer;
    protected final int offset;
    private final ProtocolLayer<?> parentLayer;
    private ProtocolLayer<?> nextLayer;

    public AbstractProtocolLayer(final ProtocolLayer<?> parentLayer, final ByteBuffer backingBuffer, final int offset) {
        this.parentLayer = parentLayer;
        this.backingBuffer = backingBuffer;
        this.offset = offset;
    }

    @Override
    public ProtocolLayer<?> getParentLayer() {
        return parentLayer;
    }

    @Override
    public ProtocolLayer<?> getNextLayer() {
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
    public E editor() {
        // Let the editor operate on a view without limit of the buffer starting from this layer offset
        return buildEditor(makeBufferView(offset, -1));
    }

    @Override
    public PayloadEditor payloadEditor() {
        // Let the editor operate on a view without limit of the buffer starting from this layer's payload offset
        return new PayloadEditor(this, makeBufferView(offset + getHeaderSize(), -1));
    }

    @Override
    public ByteBuffer getBufferView() {
        return makeBufferView(offset, getTotalSize()).asReadOnlyBuffer();
    }

    @Override
    public ByteBuffer getPayloadBufferView() {
        return makeBufferView(offset + getHeaderSize(), getPayloadSize()).asReadOnlyBuffer();
    }

    @Override
    public void onParentHeaderChanged(final ProtocolLayer<?> layer, final LayerChangeset changeset) {
        getNextLayer(); // Build the next layer to propagate the event
        if (nextLayer != null) nextLayer.onParentHeaderChanged(layer, changeset);
    }

    @Override
    public void onChildLayerChanged(final ProtocolLayer<?> layer, final LayerChangeset changeset, final int sizeDelta) {
        if (layer == nextLayer) onPayloadChanged(sizeDelta);
        if (parentLayer != null) parentLayer.onChildLayerChanged(layer, changeset, sizeDelta);
    }

    @Override
    public void onEditorCommit(final LayerChangeset changeset, final int sizeDelta) {
        // Fix our backing buffer limit in face of size changes
        if (sizeDelta != 0)
            backingBuffer.limit(backingBuffer.limit() + sizeDelta);

        if (changeset == null) {
            invalidateChildLayers();
            onPayloadChanged(sizeDelta);
        }

        if (changeset != null) {
            getNextLayer(); // Rebuild that now
            if (nextLayer != null) nextLayer.onParentHeaderChanged(this, changeset);
        }

        // Let our parent (if we have one) know that we have changed
        if (parentLayer != null) parentLayer.onChildLayerChanged(this, changeset, sizeDelta);
    }

    protected void invalidateChildLayers() {
        nextLayer = null;
    }

    protected abstract ProtocolLayer<?> buildNextLayer(int nextOffset);

    protected abstract E buildEditor(ByteBuffer bufferView);

    protected void onPayloadChanged(final int sizeDelta) { }

    private ByteBuffer makeBufferView(final int offset, final int size) {
        final ByteBuffer view = backingBuffer.duplicate();
        view.position(offset);
        view.limit(size < 0 ? view.capacity() : offset + size);
        return view.slice();
    }

}
