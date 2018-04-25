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

package io.github.lczx.aml.modules.tls;

import io.github.lczx.aml.AMLContext;
import io.github.lczx.aml.hook.monitoring.MeasureHolder;
import io.github.lczx.aml.hook.monitoring.StatusProbe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RouteTable {

    public static final String PROXY_ROUTE_TABLE_DUMP = "proxy_route_table_dump";

    private final Map<Integer, ProxyConnection> routeMap = new HashMap<>();

    /* package */ RouteTable(final AMLContext amlContext) {
        amlContext.getStatusMonitor().attachProbe(new RouteProbe());
    }

    public void addRoute(final int sourcePort, final ProxyConnection proxyConnection) {
        synchronized (routeMap) {
            routeMap.put(sourcePort, proxyConnection);
        }
    }

    public void removeRoute(final int sourcePort) {
        synchronized (routeMap) {
            routeMap.remove(sourcePort);
        }
    }

    public ProxyConnection getRoute(final int sourcePort) {
        synchronized (routeMap) {
            return routeMap.get(sourcePort);
        }
    }

    public void clear() {
        synchronized (routeMap) {
            routeMap.clear();
        }
    }

    private class RouteProbe implements StatusProbe {
        @Override
        public void onMeasure(final MeasureHolder m) {
            // Note: this runs on the main thread
            final ArrayList<String> l = new ArrayList<>(routeMap.size());
            for (final Map.Entry<Integer, ProxyConnection> i : routeMap.entrySet())
                l.add(String.format("%s -> %s", i.getKey(), i.getValue()));

            m.putStringArray(PROXY_ROUTE_TABLE_DUMP, l.toArray(new String[0]));
        }
    }

}
