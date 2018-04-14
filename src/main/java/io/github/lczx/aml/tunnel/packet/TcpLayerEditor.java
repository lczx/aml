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

import io.github.lczx.aml.tunnel.packet.editor.RelocatingLayerEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static io.github.lczx.aml.tunnel.packet.TcpLayer.*;

/**
 * Editor for {@link TcpLayer}.
 */
public class TcpLayerEditor extends RelocatingLayerEditor<TcpLayer, TcpLayerEditor> {

    private static final Logger LOG = LoggerFactory.getLogger(TcpLayerEditor.class);
    private static final int MAX_HEADER_SIZE = 0x0F << 2; // Max. data offset is 1111b, max header size is 60 bytes

    TcpLayerEditor(final TcpLayer protocolLayer, final ByteBuffer targetBuffer) {
        super(protocolLayer, targetBuffer, IDX_BLOB_OPTIONS);
    }

    @Override
    public void commit() {
        final int sizeDelta = processVariableSizeEdits();
        super.commit();
        LOG.trace("TCP header change committed (size delta: {} bytes)", sizeDelta);
        protocolLayer.onEditorCommit(changeset, sizeDelta);

        // Update checksum after onEditorCommit: this.getTotalSize() (used for checksum calculation)
        // is calculated from the IP header, which is updated in onEditorCommit(), so let it update its fields first.
        targetBuffer.putShort(IDX_WORD_CHECKSUM, protocolLayer.calculateChecksum());
    }

    /**
     * Sets this packet's <i>source port</i> (16 bits).
     *
     * @param sourcePort The new value for the field
     * @return This editor
     */
    public TcpLayerEditor setSourcePort(final int sourcePort) {
        if (protocolLayer.getSourcePort() != sourcePort)
            changeset.putEdit(IDX_WORD_SOURCE_PORT, (short) sourcePort);
        return this;
    }

    /**
     * Sets this packet's <i>destination port</i> (16 bits).
     *
     * @param destinationPort The new value for the field
     * @return This editor
     */
    public TcpLayerEditor setDestinationPort(final int destinationPort) {
        if (protocolLayer.getDestinationPort() != destinationPort)
            changeset.putEdit(IDX_WORD_DESTINATION_PORT, (short) destinationPort);
        return this;
    }

    /**
     * Sets this packet's <i>sequence number</i> field (32 bits).
     *
     * @param sequenceNumber The new value for the field
     * @return This editor
     */
    public TcpLayerEditor setSequenceNumber(final long sequenceNumber) {
        if (protocolLayer.getSequenceNumber() != sequenceNumber)
            changeset.putEdit(IDX_DWORD_SEQUENCE_NUMBER, (int) sequenceNumber);
        return this;
    }

    /**
     * Sets this packet's <i>acknowledgement number</i> field (32 bits).
     *
     * @param acknowledgementNumber The new value for the field
     * @return This editor
     */
    public TcpLayerEditor setAcknowledgementNumber(final long acknowledgementNumber) {
        if (protocolLayer.getAcknowledgementNumber() != acknowledgementNumber)
            changeset.putEdit(IDX_DWORD_ACKNOWLEDGEMENT_NUMBER, (int) acknowledgementNumber);
        return this;
    }

    /**
     * Sets this packet's <i>flags</i> field (8 bits).
     *
     * @param flags The new value for the field
     * @return This editor
     */
    public TcpLayerEditor setFlags(final int flags) {
        if (protocolLayer.getFlags() != flags)
            changeset.putEdit(IDX_BYTE_FLAGS, (byte) flags);
        return this;
    }

    /**
     * Sets this packet's <i>window size</i> field (16 bits).
     *
     * @param windowSize The new value for the field
     * @return This editor
     */
    public TcpLayerEditor setWindowSize(final int windowSize) {
        if (protocolLayer.getWindowSize() != windowSize)
            changeset.putEdit(IDX_WORD_WINDOW_SIZE, (short) windowSize);
        return this;
    }

    /**
     * Sets this packet's <i>urgent pointer</i> field (16 bits).
     *
     * @param urgentPointer The new value for the field
     * @return This editor
     */
    public TcpLayerEditor setUrgentPointer(final int urgentPointer) {
        if (protocolLayer.getUrgentPointer() != urgentPointer)
            changeset.putEdit(IDX_WORD_URGENT_POINTER, (short) urgentPointer);
        return this;
    }

    /**
     * Sets this segment's <i>options</i> (variable length, 0-320 bits, divisible by 32).
     *
     * @param options The new value for the field
     * @return This editor
     */
    public TcpLayerEditor setOptions(byte[] options) {
        if (Arrays.equals(protocolLayer.getOptions(), options)) return this;
        if (options != null) {
            if (options.length % 4 != 0)
                throw new IllegalArgumentException("Options length must be multiple of 4");
            final int maxOptSize = MAX_HEADER_SIZE - IDX_BLOB_OPTIONS;
            if (options.length > maxOptSize)
                throw new IllegalArgumentException("Options must not be longer than " + maxOptSize + " bytes");
        } else {
            options = new byte[]{};
        }
        changeset.putEdit(IDX_BLOB_OPTIONS, options);
        return this;
    }

    @Override
    protected void onHeaderSizeChanged(final int newHeaderSize, final int payloadSize) {
        final int reservedBits = targetBuffer.get(IDX_BYTE_DATA_OFFSET_AND_RESERVED) & 0x0F;
        changeset.putEdit(IDX_BYTE_DATA_OFFSET_AND_RESERVED,
                (byte) (newHeaderSize >> 2 << 4 | reservedBits));
    }

}
