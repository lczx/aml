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

import io.github.lczx.aml.hook.DraftTcpHook;
import io.github.lczx.aml.AMLContext;
import io.github.lczx.aml.hook.monitoring.BaseMeasureKeys;
import io.github.lczx.aml.hook.monitoring.MeasureHolder;
import io.github.lczx.aml.hook.monitoring.StatusProbe;
import io.github.lczx.aml.tunnel.protocol.udp.LruCache;

import java.net.InetSocketAddress;
import java.util.ArrayList;
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

    private DraftTcpHook __hook;

    /* package */ SessionRegistry(final AMLContext amlContext) {
        amlContext.getStatusMonitor().attachProbe(new TcpSessionProbe());
    }

    public void __setHook(final DraftTcpHook hook) {
        this.__hook = hook;
    }

    /* package */ Connection getConnection(final String key) {
        synchronized (connCache) {
            return connCache.get(key);
        }
    }

    /* package */ void putConnection(final Connection connection) {
        synchronized (connCache) {
            connCache.put(connection.getRegistryKey(), connection);
        }
    }

    /* package */ void closeConnection(final Connection connection) {
        __hook.onClose(connection);
        connection.closeUpstreamChannel();
        synchronized (connCache) {
            connCache.remove(connection.getRegistryKey());
        }
    }

    /* package */ void closeAll() {
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
        synchronized (connCache) {
            return "SessionRegistry{" +
                    "connCache=" + connCache +
                    '}';
        }
    }

    // TODO: Fix nice code duplication from UDP transmitter
    /* package */ static String buildKey(final InetSocketAddress destination, final int sourcePort) {
        return destination.getAddress().getHostAddress() + ':' + destination.getPort() + ':' + sourcePort;
    }

    private class TcpSessionProbe implements StatusProbe {
        @Override
        public void onMeasure(final MeasureHolder m) {
            // Note: this runs on the main thread
            final ArrayList<String> l = new ArrayList<>(connCache.size());
            for (final Map.Entry<String, Connection> i : connCache.entrySet())
                l.add(String.format("%s -> %s", i.getKey(), i.getValue()));

            m.putStringArray(BaseMeasureKeys.TCP_CONN_CACHE_DUMP, l.toArray(new String[0]));
            m.putInt(BaseMeasureKeys.TCP_CONN_CACHE_CAPACITY, connCache.getMaxSize());
        }
    }

}
