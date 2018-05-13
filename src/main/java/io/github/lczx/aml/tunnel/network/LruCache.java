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

package io.github.lczx.aml.tunnel.network;

import java.util.LinkedHashMap;

public class LruCache<K, V> extends LinkedHashMap<K, V> {

    private final int maxSize;
    private final RemoveCallback<K, V> removeCallback;

    public LruCache(final int maxSize, final RemoveCallback<K, V> removeCallback) {
        super(maxSize + 1, 1, true);
        this.maxSize = maxSize;
        this.removeCallback = removeCallback;
    }

    @Override
    protected boolean removeEldestEntry(Entry<K, V> eldest) {
        if (size() <= maxSize) return false;
        if (removeCallback != null) removeCallback.onRemove(eldest);
        return true;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public interface RemoveCallback<K, V> {
        void onRemove(Entry<K, V> eldest);
    }

}
