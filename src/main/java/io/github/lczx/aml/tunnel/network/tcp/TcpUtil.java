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

package io.github.lczx.aml.tunnel.network.tcp;

import io.github.lczx.aml.tunnel.packet.IPv4Layer;
import io.github.lczx.aml.tunnel.packet.Packet;
import io.github.lczx.aml.tunnel.packet.TcpLayer;
import io.github.lczx.aml.tunnel.packet.TcpLayerEditor;

final class TcpUtil {

    private TcpUtil() { }

    static void recyclePacketForEmptyResponse(final Packet packet, final int flags, final long seqN, final long ackN) {
        recyclePacketForEmptyResponse(packet, flags, seqN, ackN, true);
    }

    static void recyclePacketForEmptyResponse(final Packet packet, final int flags, final long seqN, final long ackN,
                                              final boolean preserveOptions) {
        recyclePacketForResponse(packet, flags, seqN, ackN, false, preserveOptions);
    }

    static void recyclePacketForResponse(final Packet packet, final int flags, final long seqN, final long ackN,
                                         final boolean preservePayload, final boolean preserveOptions) {
        final IPv4Layer ip = (IPv4Layer) packet.getFirstLayer();
        final TcpLayer tcp = (TcpLayer) ip.getNextLayer();

        // Clear content if requested
        if (!preservePayload) tcp.payloadEditor().clearContent();

        // Swap endpoints and update properties
        ip.editor()
                .setSourceAddress(ip.getDestinationAddress())
                .setDestinationAddress(ip.getSourceAddress())
                .commit();

        final TcpLayerEditor tcpEditor = tcp.editor()
                .setSourcePort(tcp.getDestinationPort())
                .setDestinationPort(tcp.getSourcePort())
                .setFlags(flags)
                .setSequenceNumber(seqN)
                .setAcknowledgementNumber(ackN);

        if (!preserveOptions) tcpEditor.setOptions(null);
        tcpEditor.commit();
    }

}
