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

import io.github.lczx.aml.tunnel.protocol.DataTransferQueue;
import io.github.lczx.aml.tunnel.protocol.tcp.Connection;

import java.util.HashMap;
import java.util.Map;

public class ProxyConnection {

    private final Connection tcpConnection;
    private final Type proxyType;
    private final DataTransferQueue transmittingQueue = new DataTransferQueue();
    private final DataTransferQueue receivingQueue = new DataTransferQueue();
    private final Map<String, Object> extra = new HashMap<>();

    public ProxyConnection(final Connection tcpConnection, final Type proxyType) {
        this.tcpConnection = tcpConnection;
        this.proxyType = proxyType;
    }

    public Connection getTcpConnection() {
        return tcpConnection;
    }

    public Type getProxyType() {
        return proxyType;
    }

    public DataTransferQueue getTransmittingQueue() {
        return transmittingQueue;
    }

    public DataTransferQueue getReceivingQueue() {
        return receivingQueue;
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
