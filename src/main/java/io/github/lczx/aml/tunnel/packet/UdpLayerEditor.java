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

import io.github.lczx.aml.tunnel.packet.editor.LayerEditorBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static io.github.lczx.aml.tunnel.packet.UdpLayer.*;

/**
 * Editor for {@link UdpLayer}.
 */
public class UdpLayerEditor extends LayerEditorBase<UdpLayerEditor> {

    private static final Logger LOG = LoggerFactory.getLogger(UdpLayerEditor.class);

    private final UdpLayer protocolLayer;

    UdpLayerEditor(final UdpLayer protocolLayer, final ByteBuffer targetBuffer) {
        super(targetBuffer);
        this.protocolLayer = protocolLayer;
    }

    @Override
    public void commit() {
        super.commit();
        targetBuffer.putShort(IDX_WORD_CHECKSUM, protocolLayer.calculateChecksum());
        LOG.trace("UDP header change committed");
        protocolLayer.onEditorCommit(changeset, 0);
    }

    /**
     * Sets this datagram's <i>source port</i> (16 bits).
     *
     * @param sourcePort The new value for the field
     * @return This editor
     */
    public UdpLayerEditor setSourcePort(final int sourcePort) {
        if (protocolLayer.getSourcePort() != sourcePort)
            changeset.putEdit(IDX_WORD_SOURCE_PORT, (short) sourcePort);
        return this;
    }

    /**
     * Sets this datagram's <i>destination port</i> (16 bits).
     *
     * @param destinationPort The new value for the field
     * @return This editor
     */
    public UdpLayerEditor setDestinationPort(final int destinationPort) {
        if (protocolLayer.getDestinationPort() != destinationPort)
            changeset.putEdit(IDX_WORD_DESTINATION_PORT, (short) destinationPort);
        return this;
    }

}
