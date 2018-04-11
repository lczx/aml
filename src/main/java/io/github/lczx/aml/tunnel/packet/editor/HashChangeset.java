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

import android.annotation.SuppressLint;
import io.github.lczx.aml.tunnel.packet.NumberUtils;

import java.util.*;

/**
 * A {@link LayerChangeset} implemented using an {@link HashMap}.
 */
public class HashChangeset extends LayerChangeset {

    private final Map<Integer, Object> changeset;

    @SuppressLint("UseSparseArrays") // No thanks, I don't want to mess up my tests to "mock" only this
    public HashChangeset() {
        this.changeset = new HashMap<>();
    }

    private HashChangeset(Map<Integer, Object> changeset) {
        this.changeset = changeset;
    }

    @Override
    public void putEdit(int offset, Object value) {
        Objects.requireNonNull(value, "Attempted to make null edit");
        changeset.put(offset, value);
    }

    @Override
    public Object getEdit(int offset) {
        return changeset.get(offset);
    }

    @Override
    public void merge(LayerChangeset edits) {
        for (Entry e : edits) changeset.put(e.offset, e.value);
    }

    @Override
    @SuppressLint("UseSparseArrays")
    public LayerChangeset clone() {
        return new HashChangeset(new HashMap<>(changeset));
    }

    @Override
    public Iterator<Entry> iterator() {
        return new Itr();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<Integer, Object>> i = changeset.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<Integer, Object> e = i.next();
            sb.append(e.getKey());
            Object v = e.getValue();
            if (v instanceof Byte)
                sb.append("B:").append(NumberUtils.asUnsigned((byte) v));
            else if (v instanceof Short)
                sb.append("W:").append(NumberUtils.asUnsigned((short) v));
            else if (v instanceof Integer)
                sb.append("D:").append(NumberUtils.asUnsigned((int) v));
            else if (v instanceof Long)
                sb.append("Q:").append((long) v);
            else if (v instanceof byte[])
                sb.append("[:").append(Arrays.toString((byte[]) v));
            if (i.hasNext()) sb.append(", ");
        }
        return "HashChangeset{" + sb.toString() + '}';
    }

    private class Itr implements Iterator<Entry> {
        private final Iterator<Map.Entry<Integer, Object>> mapIterator = changeset.entrySet().iterator();

        @Override
        public boolean hasNext() {
            return mapIterator.hasNext();
        }

        @Override
        public Entry next() {
            Map.Entry<Integer, Object> e = mapIterator.next();
            return new Entry(e.getKey(), e.getValue());
        }
    }

}
