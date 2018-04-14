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

import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static io.github.lczx.aml.tunnel.packet.IPv4Layer.*;

/**
 * Editor for {@link IPv4Layer}.
 */
public class IPv4LayerEditor extends RelocatingLayerEditor<IPv4Layer, IPv4LayerEditor> {

    private static final Logger LOG = LoggerFactory.getLogger(IPv4LayerEditor.class);
    private static final int MAX_HEADER_SIZE = 0x0F << 2; // Max. IHL is 1111b, max header size is 60 bytes

    IPv4LayerEditor(final IPv4Layer protocolLayer, final ByteBuffer targetBuffer) {
        super(protocolLayer, targetBuffer, IDX_BLOB_OPTIONS);
    }

    @Override
    public void commit() {
        final int sizeDelta = processVariableSizeEdits();
        super.commit();
        targetBuffer.putShort(IDX_WORD_CHECKSUM, protocolLayer.calculateChecksum());
        LOG.trace("IPv4 header change committed (size delta: {} bytes)", sizeDelta);
        protocolLayer.onEditorCommit(changeset, sizeDelta);
    }

    /**
     * Sets this packet's <i>identification</i> field (16 bits).
     *
     * @param identification The new value for the field
     * @return This editor
     */
    public IPv4LayerEditor setIdentificationField(final int identification) {
        if (protocolLayer.getIdentificationField() != identification)
            changeset.putEdit(IDX_WORD_IDENTIFICATION, (short) identification);
        return this;
    }

    /**
     * Sets this packet's <i>time to live</i> field (8 bits).
     *
     * @param ttl The new value for the field
     * @return This editor
     */
    public IPv4LayerEditor setTTL(final short ttl) {
        if (protocolLayer.getTTL() != ttl)
            changeset.putEdit(IDX_BYTE_TIME_TO_LIVE, (byte) ttl);
        return this;
    }

    /**
     * Sets this packet's <i>protocol ID</i> field (8 bits).
     *
     * @param protocolId The new value for the field
     * @return This editor
     */
    public IPv4LayerEditor setProtocolId(final int protocolId) {
        if (protocolLayer.getProtocolId() != protocolId)
            changeset.putEdit(IDX_BYTE_PROTOCOL_ID, (byte) protocolId);
        return this;
    }

    /**
     * Sets this packet's <i>source address</i> (32 bits).
     *
     * @param sourceAddress The new value for the field
     * @return This editor
     */
    public IPv4LayerEditor setSourceAddress(final Inet4Address sourceAddress) {
        if (!Arrays.equals(protocolLayer.getSourceAddress().getAddress(), sourceAddress.getAddress()))
            changeset.putEdit(IDX_DWORD_SOURCE_ADDRESS, sourceAddress.getAddress());
        return this;
    }

    /**
     * Sets this packet's <i>destination address</i> (32 bits).
     *
     * @param destinationAddress The new value for the field
     * @return This editor
     */
    public IPv4LayerEditor setDestinationAddress(final Inet4Address destinationAddress) {
        if (!Arrays.equals(protocolLayer.getDestinationAddress().getAddress(), destinationAddress.getAddress()))
            changeset.putEdit(IDX_DWORD_DESTINATION_ADDRESS, destinationAddress.getAddress());
        return this;
    }

    /**
     * Sets this packet's <i>options</i> (variable length, 0-320 bits, divisible by 32).
     *
     * @param options The new value for the field
     * @return This editor
     */
    public IPv4LayerEditor setOptions(byte[] options) {
        if (Arrays.equals(protocolLayer.getOptions(), options)) return this;
        if (options != null) {
            if (options.length % 4 != 0)
                throw new IllegalArgumentException("Options length must be multiple of 4");
            int maxOptSize = MAX_HEADER_SIZE - IDX_BLOB_OPTIONS;
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
        changeset.putEdit(IDX_BYTE_VERSION_AND_IHL,
                (byte) ((protocolLayer.getVersion() << 4) | ((newHeaderSize >> 2) & 0x0F)));
        changeset.putEdit(IDX_WORD_TOTAL_LENGTH, (short) (newHeaderSize + payloadSize));
    }

}
