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

/**
 * A {@link ProtocolLayer} abstraction utility, providing a common implementation of redundant size getters, child
 * layer cache management, parent-child relationships, editor commit behavior and view providers.
 *
 * @param <E> The layer editor class for the implementing layer
 */
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
        if (nextLayer == null && canBuildNextLayer())
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

        // In case of header edit with size change we need to invalidate lower layers because the offset changed;
        // however we also invalidate in case of any payload change (changeset == null).
        if (sizeDelta != 0 || changeset == null)
            invalidateChildLayers();

        // Payload changed, we may need to change size or checksum in header
        if (changeset == null)
            onPayloadChanged(sizeDelta);

        // Our child is now invalidated if we edited this layer's payload directly or if our header size changed.
        // If this was a header edit, we need to rebuild our child layers now to give them a chance to maintain
        // integrity, otherwise if it was a payload edit, we don't do nothing because it was a deliberate raw change.
        if (changeset != null) {
            getNextLayer(); // Rebuild that now
            if (nextLayer != null) nextLayer.onParentHeaderChanged(this, changeset);
        }

        // Let our parent (if we have one) know that we have changed
        if (parentLayer != null) parentLayer.onChildLayerChanged(this, changeset, sizeDelta);
    }

    /**
     * Invalidates the following layers in the chain.
     */
    protected void invalidateChildLayers() {
        nextLayer = null;
    }

    /**
     * Builds the next protocol layer of the chain.
     *
     * <p> The result of this method is cached; the value is discarded by a call to {@link #invalidateChildLayers()}.
     *
     * @param nextOffset The buffer offset of the next layer
     * @return The layer detected from the payload of this one
     */
    protected abstract ProtocolLayer<?> buildNextLayer(int nextOffset);

    /**
     * Returns the minimum payload size required to build the next layer.
     *
     * <p> If the current {@link #getPayloadSize()} is less than this value, construction of the next layer will not
     * be allowed and {@link #getNextLayer()} will return {@link null}.
     *
     * <p> Since child layers are automatically generated and notified of changes to this layer
     * (via {@link #onParentHeaderChanged(ProtocolLayer, LayerChangeset)}), this provides a safe way to abort this
     * mechanism if the child would not be able to access its fields.
     *
     * @return The minimum payload size required to call {@link #buildNextLayer(int)}
     */
    protected int buildNextLayerMinimumSize() {
        return 0;
    }

    /**
     * Builds an editor for this layer with the given buffer.
     *
     * @param bufferView The slice of this packet's backing buffer (from the start of
     *                   this layer to the end of the buffer) used to create the editor
     * @return The built editor
     */
    protected abstract E buildEditor(ByteBuffer bufferView);

    /**
     * Called by the {@link #onEditorCommit(LayerChangeset, int)} implementation of {@link AbstractProtocolLayer}
     * when a payload commit is detected.
     *
     * <p> Implementations should update their size and/or checksum in the header when this method is called.
     *
     * @param sizeDelta The change in size of the payload
     * @see #onEditorCommit(LayerChangeset, int)  The note in the documentation of onEditorCommit(LayerChangeset, int)
     */
    protected void onPayloadChanged(final int sizeDelta) { }

    private boolean canBuildNextLayer() {
        // To allow the creation of the next layer, the following conditions must be met:
        // - Our payload is greater than the minimum safe next layer size, as provided by buildNextLayerMinimumSize()
        // - There must be at least as much allocated space in the buffer (by limit) to hold the
        //   declared (as in header) payload of this layer (i.e. limit - offset - headerSz >= payloadSz)
        final int effectiveTotalSize = backingBuffer.limit() - offset;
        return getPayloadSize() >= buildNextLayerMinimumSize() && getTotalSize() <= effectiveTotalSize;
    }

    private ByteBuffer makeBufferView(final int offset, final int size) {
        final ByteBuffer view = backingBuffer.duplicate();
        view.position(offset);
        view.limit(size < 0 ? view.capacity() : offset + size);
        return view.slice();
    }

}
