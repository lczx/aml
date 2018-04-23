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

import io.github.lczx.aml.tunnel.PacketSink;
import io.github.lczx.aml.tunnel.packet.Packet;
import io.github.lczx.aml.tunnel.packet.Packets;
import io.github.lczx.aml.tunnel.packet.TcpLayer;
import io.github.lczx.aml.tunnel.packet.editor.PayloadEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

class TcpReceiver implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(TcpReceiver.class);

    private final Selector networkSelector;
    private final PacketSink packetSink;
    private final SessionRegistry sessionRegistry;

    TcpReceiver(final Selector networkSelector, final PacketSink packetSink, final SessionRegistry sessionRegistry) {
        this.networkSelector = networkSelector;
        this.packetSink = packetSink;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void run() {
        LOG.info("TCP receiver started");
        try {
            while (!Thread.interrupted()) {
                final int readyChannels = networkSelector.select();

                if (readyChannels == 0) {
                    Thread.sleep(10);
                    continue;
                }

                final Iterator<SelectionKey> keyIterator = networkSelector.selectedKeys().iterator();

                while (keyIterator.hasNext() && !Thread.interrupted()) {
                    final SelectionKey key = keyIterator.next();
                    if (key.isValid()) {
                        if (key.isConnectable())
                            processConnect(key, keyIterator);
                        else if (key.isReadable())
                            processInput(key, keyIterator);
                    }
                }
            }
        } catch (final InterruptedException e) {
            LOG.info("TCP receiver stopping");
        } catch (final IOException e) {
            LOG.error("I/O exception in TCP receiver", e);
        }
    }

    private void processConnect(final SelectionKey key, final Iterator<SelectionKey> keyIterator) {
        final Connection connection = (Connection) key.attachment();

        // This is the packet attached in TcpTransmitter#initializeConnection() [with TCP SYN options] (not a copy)
        final Packet packet = connection.getPacketAttachment();

        try {
            if (!connection.getUpstreamChannel().finishConnect())
                throw new IOException("finishConnect() returned false when channel was connectable");
            keyIterator.remove();

            // Make a copy of the packet without options as a template for further I/O
            final Packet templatePacket = Packets.makeCopy(packet);
            templatePacket.getLayer(TcpLayer.class).editor().setOptions(null).commit();
            connection.setPacketAttachment(templatePacket);

            // The channel is now connected, answer local with SYN,ACK
            TcpTransmitter.processChannelConnected(connection, packet);

            key.interestOps(SelectionKey.OP_READ); // todo mmh

        } catch (final IOException e) {
            // Got an error, reset the connection, use packet attachment, strip options
            LOG.error("I/O exception on channel connect: " + connection.getLink(), e);
            TcpUtil.recyclePacketForEmptyResponse(packet, TcpLayer.FLAG_RST,
                    0, connection.getTcb().localAckN, false);
            sessionRegistry.closeConnection(connection);
        }

        packetSink.receive(packet);
    }

    private void processInput(final SelectionKey key, final Iterator<SelectionKey> keyIterator) {
        keyIterator.remove();
        final Connection connection = (Connection) key.attachment();
        synchronized (connection) {
            final Packet refPacket = Packets.makeCopy(connection.getPacketAttachment()); // This is the attached copy without options
            final SocketChannel inputChannel = (SocketChannel) key.channel();

            final PayloadEditor editor = refPacket.getLayer(TcpLayer.class).payloadEditor();
            final int readBytes;
            try {
                readBytes = inputChannel.read(editor.buffer(true));
            } catch (final IOException e) {
                LOG.error("Network read error: " + connection.getLink(), e);
                editor.clearContent();

                TcpUtil.recyclePacketForEmptyResponse(refPacket, TcpLayer.FLAG_RST,
                        0, connection.getTcb().localAckN);
                packetSink.receive(refPacket);
                sessionRegistry.closeConnection(connection);
                return;
            }

            if (readBytes == -1) {
                // End of stream, stop waiting until we push more data
                key.interestOps(0);
                connection.setWaitingForNetworkData(false);

                if (connection.getTcb().state != TCB.State.CLOSE_WAIT) {
                    /* // TODO: Begin experimental passive close code (is it necessary?)
                    connection.getTcb().state = TCB.State.FIN_WAIT_1;
                    TcpUtil.recyclePacketForEmptyResponse(refPacket, TcpLayer.FLAG_FIN,
                            connection.getTcb().localSeqN, connection.getTcb().localAckN());
                    connection.getTcb().localSeqN++; // FIN counts as a byte
                    packetSink.receive(refPacket);
                    } // TODO: End experimental passive close code */
                    // TODO: Release packet when implemented
                    return;
                }

                connection.getTcb().state = TCB.State.LAST_ACK;
                // Using FIN,ACK instead of FIN makes the client answer us with ACK and close in TX#processACK()
                TcpUtil.recyclePacketForEmptyResponse(refPacket, TcpLayer.FLAG_FIN | TcpLayer.FLAG_ACK,
                        connection.getTcb().localSeqN, connection.getTcb().localAckN);
                connection.getTcb().localSeqN++; // FIN counts as a byte
            } else {
                // TODO: We should ideally be splitting segments by MTU/MSS, but this seems to work without
                editor.flipAndCommit();
                TcpUtil.recyclePacketForResponse(refPacket, TcpLayer.FLAG_PSH | TcpLayer.FLAG_ACK,
                        connection.getTcb().localSeqN, connection.getTcb().localAckN, true, false);
                connection.getTcb().localSeqN += readBytes; // Next sequence number
            }
            packetSink.receive(refPacket);
        }
    }

}
