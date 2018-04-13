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
 * An encapsulation layer for a protocol inside a packet.
 *
 * @param <E> The class of the editor for this layer
 */
public interface ProtocolLayer<E extends LayerEditor> {

    /**
     * Returns the previous layer in this layer chain.
     *
     * @return The previous layer in the chain or {@code null} if this is the top level layer
     */
    ProtocolLayer<?> getParentLayer();

    /**
     * Returns the next layer in this layer chain.
     *
     * <p> For example an IP layer might return a TCP, UDP or ICMP layer from this method.
     *
     * @return The next layer in the chain or {@code null} if it cannot be determined
     */
    ProtocolLayer<?> getNextLayer();

    /**
     * Obtains the offset of this layer inside the packet's backing buffer.
     *
     * @return The offset of this layer
     */
    int getBufferOffset();

    /**
     * The size of the header for the protocol represented by this layer
     *
     * @return The header size in bytes
     */
    int getHeaderSize();

    /**
     * The payload size as seen from this layer, contains header and payload of the all the layer underneath this one.
     *
     * @return The size of the payload in bytes
     */
    int getPayloadSize();

    /**
     * The total size of this layer,
     * equals the sum of the results of {@link #getHeaderSize()} and {@link #getPayloadSize()}.
     *
     * @return The total size of this layer in bytes
     */
    int getTotalSize();

    /**
     * Returns an editor for the properties of this layer, encapsulated in the header.
     *
     * @return An editor for this layer
     */
    E editor();

    /**
     * Returns an editor for the payload of this layer.
     *
     * @return An editor for this layer's payload
     */
    PayloadEditor payloadEditor();

    /**
     * Provides a read-only view of this layer, consisting of protocol header plus payload.
     *
     * @return A read-only slice of the packet's backing buffer,
     *         from the start of the header of this layer to the end of the packet (not the buffer)
     */
    ByteBuffer getBufferView();

    /**
     * Provides a read-only view of this layer's payload.
     *
     * @return A read-only slice of the packet's backing buffer,
     *         from the start of the payload of this layer to the end of the packet (not the buffer)
     */
    ByteBuffer getPayloadBufferView();

    /**
     * Called by children layers to notify this layer of changes in their content.
     *
     * <p> <i>Use case: An application-level layer can notify the TCP layer to update its checksum.</i>
     *
     * @param layer     The layer that changed
     * @param changeset The change set committed to the layer's header or {@code null} if it was a payload edit
     * @param sizeDelta The difference in size from before the edit
     */
    void onChildLayerChanged(ProtocolLayer<?> layer, LayerChangeset changeset, int sizeDelta);

    /**
     * Called by a layer or payload editor after committing its changes.
     *
     * <p> <b>Important note:</b> Payload editors cannot make changes to the header of the layer; if this call was
     * originated from a payload edit ({@code changeSet} is null) you must change the appropriate size fields manually.
     *
     * @param changeset The change set committed to the layer's header or {@code null} if it was a payload edit
     * @param sizeDelta The difference in size from before the edit (in bytes)
     */
    void onEditorCommit(LayerChangeset changeset, int sizeDelta);

}
