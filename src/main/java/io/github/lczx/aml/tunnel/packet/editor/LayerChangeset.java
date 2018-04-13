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
 * Tracks changes to be made by a {@link LayerEditor}.
 *
 * <p> Each edit is identified by an offset and a value object,
 * native types are boxed and their type is used to determine the edit size.
 *
 * <p> This class does not support overlapping edits, order of processing on commit is implementation defined.
 */
public abstract class LayerChangeset implements Iterable<LayerChangeset.Entry>, Cloneable {

    /**
     * Adds an edit to this changeset.
     *
     * @param offset The offset in the buffer to change
     * @param value  The new value to put
     */
    public abstract void putEdit(int offset, Object value);

    /**
     * Returns the edit planned for the given offset, if present.
     *
     * @param offset The offset to check for edits
     * @return The new object assigned to that offset or {@code null} if no edit to that offset is found
     */
    public abstract Object getEdit(int offset);

    /**
     * Merges all the changes in another changeset to this one.
     *
     * @param edits The changeset to append
     */
    public abstract void merge(LayerChangeset edits);

    /**
     * Clones this changeset.
     *
     * @return A copy of this changeset
     */
    @Override
    public abstract LayerChangeset clone();

    /**
     * A change entry defined by offset and value, independent from the actual changeset implementation.
     */
    public static class Entry {
        public final int offset;
        public final Object value;

        public Entry(int offset, Object value) {
            this.offset = offset;
            this.value = value;
        }
    }

}
