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

package io.github.lczx.aml.tunnel.protocol.tcp;

import io.github.lczx.aml.tunnel.protocol.udp.LruCache;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;

public class SessionRegistry {

    private static final int MAX_CACHE_SIZE = 64; // TODO: Is this ideal?

    private final LruCache<String, Connection> connCache = new LruCache<>(MAX_CACHE_SIZE,
            new LruCache.RemoveCallback<String, Connection>() {
        @Override
        public void onRemove(final Map.Entry<String, Connection> eldest) {
            eldest.getValue().closeUpstreamChannel();
        }
    });

    public Connection getConnection(final String key) {
        synchronized (connCache) {
            return connCache.get(key);
        }
    }

    public void putConnection(final Connection connection) {
        synchronized (connCache) {
            connCache.put(connection.getRegistryKey(), connection);
        }
    }

    public void closeConnection(final Connection connection) {
        // TODO: Place here connection closed hook
        connection.closeUpstreamChannel();
        synchronized (connCache) {
            connCache.remove(connection.getRegistryKey());
        }
    }

    public void closeAll() {
        synchronized (connCache) {
            final Iterator<Map.Entry<String, Connection>> it = connCache.entrySet().iterator();
            while (it.hasNext()) {
                it.next().getValue().closeUpstreamChannel();
                it.remove();
            }
        }
    }

    @Override
    public String toString() {
        return "SessionRegistry{" +
                "connCache=" + connCache +
                '}';
    }

    // TODO: Fix nice code duplication from UDP transmitter
    public static String buildKey(final InetSocketAddress destination, final int sourcePort) {
        return destination.getAddress().getHostAddress() + ':' + destination.getPort() + ':' + sourcePort;
    }

}
