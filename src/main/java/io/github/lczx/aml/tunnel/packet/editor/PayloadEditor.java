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

package io.github.lczx.aml.tunnel.packet.editor;

import io.github.lczx.aml.tunnel.packet.ProtocolLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Raw editor for {@link ProtocolLayer} payloads,
 *
 * <p> This class is initialized with a slice of the packet's backing buffer, ranging from the start of the payload
 * to edit to the capacity of the buffer.
 *
 * <p> A call to {@link #buffer()} sets the limit of this slice to the current payload size and gives back
 * the raw buffer for editing. Calling {@link #commit()} after editing the buffer notifies the packet of the edit
 * (and possible change of limit) so it can update headers and invalidate caches.
 */
public class PayloadEditor {

    private static final Logger LOG = LoggerFactory.getLogger(PayloadEditor.class);

    private final ProtocolLayer protocolLayer;
    private final ByteBuffer targetBuffer;

    private int originalPayloadSize;

    /**
     * Creates a new {@link PayloadEditor} instance.
     *
     * @param protocolLayer The {@link ProtocolLayer} bound to this editor
     * @param targetBuffer  The buffer to edit (usually a {@link ByteBuffer#slice()} of the packet's backing buffer)
     */
    public PayloadEditor(final ProtocolLayer protocolLayer, final ByteBuffer targetBuffer) {
        this.protocolLayer = protocolLayer;
        this.targetBuffer = targetBuffer;
    }

    /**
     * Retrieves the raw buffer to edit. See {@link #buffer(boolean)} for a full description.
     *
     * @return A buffer containing the payload of the {@link ProtocolLayer} for edit
     * @see #buffer(boolean)
     */
    public ByteBuffer buffer() {
        return buffer(false);
    }

    /**
     * Retrieves the raw buffer to edit. The limit is set to the current payload size and the capacity to the remaining
     * capacity in the packet's backing buffer. Call {@link #commit()} once done editing.
     *
     * @param writeNow {@code true} if there is no interest in setting the limit to the previous payload size; spares
     *                 a {@link ByteBuffer#clear()} operation and a cast back to {@link ByteBuffer}. The limit is kept
     *                 to the backing buffer's remaining capacity.
     * @return A buffer containing the payload of the {@link ProtocolLayer} for edit
     * @see  #commit()
     */
    public ByteBuffer buffer(final boolean writeNow) {
        originalPayloadSize = protocolLayer.getPayloadSize();
        if (!writeNow) targetBuffer.limit(originalPayloadSize);
        return targetBuffer;
    }

    /**
     * Notifies the {@link ProtocolLayer} bound to this editor of the changes made to the buffer provided by
     * {@link #buffer()}.
     *
     * <p> <b>Changes to the buffer after calling this method can cause corruption to the packet.</b>
     *
     * @see #buffer()
     */
    public void commit() {
        final int sizeDelta = targetBuffer.limit() - originalPayloadSize;
        LOG.trace("Payload of {}, change committed: size changed of {} bytes",
                protocolLayer.getClass().getSimpleName(), sizeDelta);
        protocolLayer.onEditorCommit(null, sizeDelta);
    }

    /**
     * Utility method to {@link ByteBuffer#flip()} the buffer before {@link #commit()}.
     *
     * @see #commit()
     */
    public void flipAndCommit() {
        targetBuffer.flip();
        commit();
    }

    /**
     * Utility method to empty the payload attached to this editor and {@link #commit()} the changes.
     */
    public void clearContent() {
        final int sizeDelta = -protocolLayer.getPayloadSize();
        LOG.trace("Payload of {} cleared, size changed of {} bytes",
                protocolLayer.getClass().getSimpleName(), sizeDelta);
        protocolLayer.onEditorCommit(null, sizeDelta);
    }

}
