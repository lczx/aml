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

import io.github.lczx.aml.tunnel.IOUtils;
import io.github.lczx.aml.tunnel.packet.Packet;

import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class Connection {

    private final String registryKey;
    private final TCB tcb;
    private final SocketChannel upstreamChannel;

    private boolean waitingForNetworkData;
    private SelectionKey selectionKey;
    private Packet packetAttachment;

    public Connection(final String registryKey, final TCB tcb, final SocketChannel upstreamChannel) {
        this.registryKey = registryKey;
        this.tcb = tcb;
        this.upstreamChannel = upstreamChannel;
    }

    public String getRegistryKey() {
        return registryKey;
    }

    public TCB getTcb() {
        return tcb;
    }

    public SocketChannel getUpstreamChannel() {
        return upstreamChannel;
    }

    public boolean isWaitingForNetworkData() {
        return waitingForNetworkData;
    }

    public void setWaitingForNetworkData(final boolean waitingForNetworkData) {
        this.waitingForNetworkData = waitingForNetworkData;
    }

    public SelectionKey getSelectionKey() {
        return selectionKey;
    }

    public void setSelectionKey(final SelectionKey selectionKey) {
        this.selectionKey = selectionKey;
    }

    public Packet getPacketAttachment() {
        return packetAttachment;
    }

    public void setPacketAttachment(final Packet packetAttachment) {
        this.packetAttachment = packetAttachment;
    }

    void closeUpstreamChannel() { // Used only by session registry
        IOUtils.closeResources(upstreamChannel);
    }

    @Override
    public String toString() {
        final String sockStatus = upstreamChannel.isConnected() ? "connected" :
                (upstreamChannel.isConnectionPending() ? "pending" : "unknown");
        final SocketAddress localSockAddr = upstreamChannel.socket().getLocalSocketAddress();
        final SocketAddress remoteSockAddr = upstreamChannel.socket().getRemoteSocketAddress();

        return "Connection{" +
                "tcb=" + tcb +
                ", upstreamChannel=" + sockStatus + ' ' + localSockAddr + ' ' + remoteSockAddr +
                ", wait=" + waitingForNetworkData +
                ", packetAttachment=" + packetAttachment +
                '}';
    }
}
