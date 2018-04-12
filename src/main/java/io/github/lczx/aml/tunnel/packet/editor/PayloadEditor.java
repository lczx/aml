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

public class PayloadEditor {

    private static final Logger LOG = LoggerFactory.getLogger(PayloadEditor.class);

    private final ProtocolLayer protocolLayer;
    private final ByteBuffer targetBuffer;

    private int originalPayloadSize;

    public PayloadEditor(ProtocolLayer protocolLayer, ByteBuffer targetBuffer) {
        this.protocolLayer = protocolLayer;
        this.targetBuffer = targetBuffer;
    }

    public ByteBuffer buffer() {
        return buffer(false);
    }

    public ByteBuffer buffer(final boolean writeNow) {
        originalPayloadSize = protocolLayer.getPayloadSize();
        if (!writeNow) targetBuffer.limit(originalPayloadSize);
        return targetBuffer;
    }

    public void commit() {
        final int sizeDelta = targetBuffer.limit() - originalPayloadSize;
        LOG.trace("Payload of {}, change committed: size changed of {} bytes",
                protocolLayer.getClass().getSimpleName(), sizeDelta);
        protocolLayer.onEditorCommit(null, sizeDelta);
    }

    public void flipAndCommit() {
        targetBuffer.flip();
        commit();
    }

    public void clearContent() {
        final int sizeDelta = -protocolLayer.getPayloadSize();
        LOG.trace("Payload of {} cleared, size changed of {} bytes",
                protocolLayer.getClass().getSimpleName(), sizeDelta);
        protocolLayer.onEditorCommit(null, sizeDelta);
    }

}
