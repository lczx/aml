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

import io.github.lczx.aml.tunnel.network.Connection;
import io.github.lczx.aml.tunnel.network.tcp.TcpConnection;

import java.util.HashMap;
import java.util.Map;

public class ProxyConnection extends Connection {

    public static final String EXTRA_TLS_SERVER_NAME = "tls-sni-server-name";
    public static final String EXTRA_TLS_NEXT_PROTOCOL_NAME = "tls-alpn-next-protocol";

    private final TcpConnection tcpConnection;
    private final Type proxyType;
    private final Map<String, Object> extra = new HashMap<>();

    public ProxyConnection(final TcpConnection tcpConnection, final Type proxyType) {
        this.tcpConnection = tcpConnection;
        this.proxyType = proxyType;
    }

    public TcpConnection getTcpConnection() {
        return tcpConnection;
    }

    public Type getProxyType() {
        return proxyType;
    }

    @SuppressWarnings("unchecked")
    public <T> T getExtra(final String key) {
        return (T) extra.get(key);
    }

    public void putExtra(final String key, final Object value) {
        extra.put(key, value);
    }

    @Override
    public String toString() {
        return "ProxyConnection{" +
                "tcpConnection=" + tcpConnection +
                ", proxyType=" + proxyType +
                ", extra=" + extra +
                '}';
    }

    public enum Type {
        RESERVED, HTTPS
    }

}
