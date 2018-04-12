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

import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static io.github.lczx.aml.tunnel.packet.IPv4Layer.*;

public class IPv4LayerEditor extends LayerEditorBase<IPv4LayerEditor> {

    private static final Logger LOG = LoggerFactory.getLogger(IPv4LayerEditor.class);

    private final IPv4Layer protocolLayer;

    public IPv4LayerEditor(IPv4Layer protocolLayer, ByteBuffer targetBuffer) {
        super(targetBuffer);
        this.protocolLayer = protocolLayer;
    }

    @Override
    public void commit() {
        super.commit();
        targetBuffer.putShort(IDX_WORD_CHECKSUM, protocolLayer.calculateChecksum());
        LOG.trace("IPv4 header change committed");
        protocolLayer.onEditorCommit(changeset, 0); // We will use sizeDelta after implementing option edits
    }

    public IPv4LayerEditor setIdentificationField(int identification) {
        if (protocolLayer.getIdentificationField() != identification)
            changeset.putEdit(IDX_WORD_IDENTIFICATION, (short) identification);
        return this;
    }

    public IPv4LayerEditor setTTL(short ttl) {
        if (protocolLayer.getTTL() != ttl)
            changeset.putEdit(IDX_BYTE_TIME_TO_LIVE, (byte) ttl);
        return this;
    }

    public IPv4LayerEditor setProtocolId(int protocolId) {
        if (protocolLayer.getProtocolId() != protocolId)
            changeset.putEdit(IDX_BYTE_PROTOCOL_ID, (byte) protocolId);
        return this;
    }

    public IPv4LayerEditor setSourceAddress(Inet4Address sourceAddress) {
        if (!Arrays.equals(protocolLayer.getSourceAddress().getAddress(), sourceAddress.getAddress()))
            changeset.putEdit(IDX_DWORD_SOURCE_ADDRESS, sourceAddress.getAddress());
        return this;
    }

    public IPv4LayerEditor setDestinationAddress(Inet4Address destinationAddress) {
        if (!Arrays.equals(protocolLayer.getDestinationAddress().getAddress(), destinationAddress.getAddress()))
            changeset.putEdit(IDX_DWORD_DESTINATION_ADDRESS, destinationAddress.getAddress());
        return this;
    }

    public IPv4LayerEditor setOptions(byte[] options) {
        // TODO: Implement
        return this;
    }

}
