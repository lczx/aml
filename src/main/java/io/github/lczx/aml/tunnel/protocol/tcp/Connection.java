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
import io.github.lczx.aml.tunnel.protocol.Link;

import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class Connection {

    public static final String EXTRA_DESTINATION_REDIRECT = "redirect-destination";

    private final Link link;
    private final TCB tcb;
    private final SocketChannel upstreamChannel;
    private final Map<String, Object> extra = new HashMap<>();

    private boolean waitingForNetworkData;
    private SelectionKey selectionKey;
    private Packet packetAttachment;

    /* package */ Connection(final Link link, final TCB tcb, final SocketChannel upstreamChannel) {
        this.link = link;
        this.tcb = tcb;
        this.upstreamChannel = upstreamChannel;
    }

    public Link getLink() {
        return link;
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

    @SuppressWarnings("unchecked")
    public <T> T getExtra(final String key) {
        return (T) extra.get(key);
    }

    public void putExtra(final String key, final Object value) {
        extra.put(key, value);
    }

    void closeUpstreamChannel() { // Used only by session registry
        IOUtils.safeClose(upstreamChannel);
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
                ", extra=" + extra +
                '}';
    }

}
