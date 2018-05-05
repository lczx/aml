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

import io.github.lczx.aml.tunnel.PacketSink;
import io.github.lczx.aml.tunnel.packet.IPv4Layer;
import io.github.lczx.aml.tunnel.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpProtocolDispatcher implements PacketSink {

    private static final Logger LOG = LoggerFactory.getLogger(IpProtocolDispatcher.class);

    private final PacketSink tcpReceiver;
    private final PacketSink udpReceiver;
    private final PacketSink icmpReceiver;

    public IpProtocolDispatcher(final PacketSink tcpReceiver, final PacketSink udpReceiver,
                                final PacketSink icmpReceiver) {
        this.tcpReceiver = tcpReceiver;
        this.udpReceiver = udpReceiver;
        this.icmpReceiver = icmpReceiver;
    }

    @Override
    public void receive(final Packet packet) {
        int protocolId = -1;

        try {
            protocolId = ((IPv4Layer) packet.getFirstLayer()).getProtocolId();
        } catch (final Exception e) {
            LOG.error("Error while reading packet IP header, dropping", e);
        }

        switch (protocolId) {
            case IPv4Layer.PROTOCOL_TCP:
                dispatchPacket("TCP", packet, tcpReceiver);
                break;

            case IPv4Layer.PROTOCOL_UDP:
                dispatchPacket("UDP", packet, udpReceiver);
                break;

            case IPv4Layer.PROTOCOL_ICMP:
                dispatchPacket("ICMP", packet, icmpReceiver);
                break;

            default:
                LOG.warn("Unknown packet transport protocol (id: {}), dropping", protocolId);
                dropPacket(packet);
                break;
        }
    }

    private void dispatchPacket(final String typeName, final Packet packet, final PacketSink sink) {
        if (sink != null) {
            sink.receive(packet);
        } else {
            LOG.warn("Got packet of type {}, dropping because no sink was provided", typeName);
            dropPacket(packet);
        }
    }

    private void dropPacket(final Packet packet) {
        // If it wasn't for hooks, we could return false from receive() to notify
        // the VPN reader that we didn't consume the buffer so that it can be reused

        // TODO: Release packet once recycling implemented
    }

}
