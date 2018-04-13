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

import java.nio.ByteBuffer;

/**
 * A layer editor that can handle dynamic sized content at the end of the layer header,
 * relocating the payload accordingly.
 *
 * <p> This class is initialized with an offset representing the start of the variable section in the header;
 * any implementor that wishes to check for dynamic content changes should place a call to
 * {@link #processVariableSizeEdits()} before calling <i>{@code super}</i> in {@link #commit()}.
 *
 * <p> If a size-changing edit is detected at the start of the variable section,
 * the payload is relocated and then {@link #onHeaderSizeChanged(int, int)} is called to allow the implementor to
 * append header-changing edits before committing.
 *
 * @param <T> The bound {@link ProtocolLayer} class
 * @param <E> The actual layer editor class (for builder pattern semantics)
 */
public abstract class RelocatingLayerEditor<T extends ProtocolLayer, E extends RelocatingLayerEditor> extends LayerEditorBase<E> {

    protected final T protocolLayer;
    private final int variableSectionStart;

    /**
     * Initializes this layer editor
     *
     * @param protocolLayer        The {@link ProtocolLayer} bound to this editor, used to get size information
     * @param targetBuffer         The buffer slice assigned to this editor
     * @param variableSectionStart The start of the variable section in the header, size changes are triggered by edits
     *                             at this offset not matching the current dynamic content size
     *                             <i>(= header length - this offset)</i>
     */
    protected RelocatingLayerEditor(final T protocolLayer, final ByteBuffer targetBuffer,
                                    final int variableSectionStart) {
        super(targetBuffer);
        this.protocolLayer = protocolLayer;
        this.variableSectionStart = variableSectionStart;
    }

    /**
     * Checks if header size-changing edits are made and if so, relocates the payload and calls
     * {@link #onHeaderSizeChanged(int, int)}
     *
     * @return The difference in size of this layer
     */
    protected int processVariableSizeEdits() {
        final int payloadSize = protocolLayer.getPayloadSize();
        final int oldHeaderSize = protocolLayer.getHeaderSize();

        // Relocate payload if variable section edits may result in a change of header size
        final int newHeaderSize = adjustPayload(oldHeaderSize, payloadSize);

        // Change lengths in header if size changed
        if (newHeaderSize != oldHeaderSize) onHeaderSizeChanged(newHeaderSize, payloadSize);

        return newHeaderSize - oldHeaderSize;
    }

    /**
     * Method called after a header-changing edit was detected and the payload was relocated.
     *
     * <p> Use this to add edits to length-related fields in the header.
     *
     * @param newHeaderSize The new header size
     * @param payloadSize   The size of the payload that was relocated
     */
    protected abstract void onHeaderSizeChanged(int newHeaderSize, int payloadSize);

    private int adjustPayload(final int headerSize, final int payloadSize) {
        // Warn: this only relocates payload, only size changes at the end of the header are supported

        // Check if a variable section edit with a different size than current is made, else we can return now
        final byte[] newVariableSection = (byte[]) changeset.getEdit(variableSectionStart);
        final int currentSectionLen = headerSize - variableSectionStart;
        if (newVariableSection == null || newVariableSection.length == currentSectionLen)
            return headerSize;

        // We need to check if writing the new section will overflow the buffer
        final int newHeaderSize = variableSectionStart + newVariableSection.length;
        if (newHeaderSize + payloadSize > targetBuffer.capacity())
            throw new RelocationException("Packet buffer would overflow, new header too large");

        // Relocate payload
        relocateChunk(headerSize, newVariableSection.length - currentSectionLen, payloadSize);
        return newHeaderSize;
    }

    private void relocateChunk(final int position, final int offset, final int size) {
        if (offset == 0) return;
        if (offset % 4 != 0) throw new IllegalArgumentException("Only offsets multiple of 4 bytes are supported");

        if (offset > 0) {
            final int endPos = position + size;
            final int alignRem = endPos % 4;
            final int endPosAligned = endPos + (alignRem == 0 ? 0 : (4 - alignRem));
            for (int i = endPosAligned - 4; i >= position; i -= 4)
                targetBuffer.putInt(i + offset, targetBuffer.getInt(i));
        } else {
            for (int i = position; i < position + size; i += 4)
                targetBuffer.putInt(i + offset, targetBuffer.getInt(i));
        }
    }

}
