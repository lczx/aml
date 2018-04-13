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

/**
 * An interface implemented by {@link io.github.lczx.aml.tunnel.packet.ProtocolLayer ProtocolLayer} editors.
 *
 * <p> An editor implementation may have setters to edit the layer's backing buffer. These changes are then written to
 * the buffer by calling {@link #commit()}.
 *
 * @param <E> The actual layer editor class (for builder pattern semantics)
 */
public interface LayerEditor<E extends LayerEditor> {

    /**
     * Retrieves a copy of this editor's list of changes.
     *
     * @return A copy of this editor's changeset
     */
    LayerChangeset exportChangeset();

    /**
     * Adds a list of changes to this editor's current changeset.
     *
     * @param changeset A {@link LayerChangeset} to be merged in this editor
     * @return This {@link LayerEditor} instance
     */
    E addChangeset(LayerChangeset changeset);

    /**
     * Commits all the accumulated changes to the layer buffer bound to this editor.
     */
    void commit();

}
