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

import java.nio.ByteBuffer;
import java.util.List;

/**
 * A wrapper for raw network data contained in a {@link ByteBuffer}.
 *
 * <p> This class provides a lazily constructed chain of {@link ProtocolLayer ProtocolLayers}
 * to facilitate I/O on the raw data.
 */
public interface Packet {

    /**
     * Returns the top layer type of this packet, as defined in {@link Packets}.
     *
     * @return The type of the first layer
     */
    int getTopLayerType();

    /**
     * Obtains the top level layer detected in the attached buffer.
     *
     * @return The first {@link ProtocolLayer}
     * @throws IllegalStateException If no buffer is attached to this packet
     */
    ProtocolLayer<?> getFirstLayer();

    /**
     * Traverses the layer chain to retrieve a {@link ProtocolLayer} with the given class.
     *
     * @param clazz The class of the layer to obtain
     * @param <T>   The return type (inferred from the {@code clazz} parameter)
     * @return The requested layer or {@code null} if no layer with the given class was not found
     * @throws IllegalStateException If no buffer is attached to this packet
     */
    <T extends ProtocolLayer> T getLayer(Class<T> clazz);

    /**
     * Traverses the layer chain to construct a list of all the layers in this packet (in structure order).
     *
     * @return The list of layers detected in this packet
     * @throws IllegalStateException If no buffer is attached to this packet
     */
    List<ProtocolLayer> getLayers();

    /**
     * Gets a read-only buffer view of this packet.
     *
     * <p> Can be used to read raw data without invalidating the internal layer cache.
     *
     * @return A read-only view of the attached buffer.
     * @throws IllegalStateException If no buffer is attached to this packet
     */
    ByteBuffer getBufferView();

    /**
     * Attaches a {@link ByteBuffer} to this packet.
     *
     * <p> <b>Note:</b> Editing this buffer directly after attachment can cause corruption: this packet and the
     * internal {@link ProtocolLayer} chain do hold caches and cannot be aware of any changes done outside their
     * interface. If you need to perform direct edits, please invoke {@link #detachBuffer()} first.
     *
     * @param topLayerType The type of the layer at the beginning of the buffer
     * @param buffer       The buffer to attach to this packet
     * @return This {@link Packet}
     * @throws IllegalStateException If a buffer is already attached to this packet
     * @see #detachBuffer()
     */
    Packet attachBuffer(int topLayerType, ByteBuffer buffer);

    /**
     * Detaches the buffer backing this packet and clears the internal layer cache.
     *
     * @return The detached buffer
     * @throws IllegalStateException If no buffer is attached to this packet
     * @see #attachBuffer(int, ByteBuffer)
     */
    ByteBuffer detachBuffer();

}
