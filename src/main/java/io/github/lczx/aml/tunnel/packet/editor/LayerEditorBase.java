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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * A base to build {@link LayerEditor LayerEditors},
 * provides the facility to apply a non-overlapping changeset to a buffer.
 *
 * <p> This class does not handle any additional constant bias added to the edit offset, so it usually operates on a
 * {@link ByteBuffer#slice()} of the original buffer.
 *
 * @param <E> The actual layer editor class (for builder pattern semantics)
 */
public class LayerEditorBase<E extends LayerEditorBase> implements LayerEditor<E> {

    private static final Logger LOG = LoggerFactory.getLogger(LayerEditorBase.class);

    private final ByteBuffer targetBuffer;
    protected final LayerChangeset changeset = new HashChangeset();

    protected LayerEditorBase(ByteBuffer targetBuffer) {
        this.targetBuffer = targetBuffer;
    }

    @Override
    public LayerChangeset exportChangeset() {
        return changeset.clone();
    }

    @Override
    @SuppressWarnings("unchecked")
    public E addChangeset(LayerChangeset changeset) {
        this.changeset.merge(changeset);
        return (E) this;
    }

    @Override
    public void commit() {
        LOG.trace("Committing changeset: {}", changeset);
        for (LayerChangeset.Entry e : changeset) {
            if (e.value instanceof Byte)
                targetBuffer.put(e.offset, (byte) e.value);
            else if (e.value instanceof Short)
                targetBuffer.putShort(e.offset, (short) e.value);
            else if (e.value instanceof Integer)
                targetBuffer.putInt(e.offset, (int) e.value);
            else if (e.value instanceof Long)
                targetBuffer.putLong(e.offset, (long) e.value);
            else if (e.value instanceof byte[])
                ((ByteBuffer) targetBuffer.duplicate().position(e.offset)).put((byte[]) e.value);
            else
                throw new ClassCastException("Changed value must be of native type or byte array");
        }
    }

}
