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

import io.github.lczx.aml.tunnel.PacketSink;
import io.github.lczx.aml.tunnel.PacketSource;
import io.github.lczx.aml.tunnel.SocketProtector;
import io.github.lczx.aml.tunnel.network.DataTransferQueue;
import io.github.lczx.aml.tunnel.network.Link;
import io.github.lczx.aml.tunnel.packet.IPv4Layer;
import io.github.lczx.aml.tunnel.packet.Packet;
import io.github.lczx.aml.tunnel.packet.Packets;
import io.github.lczx.aml.tunnel.packet.TcpLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

class TcpTransmitter implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(TcpTransmitter.class);

    private final Random random = ThreadLocalRandom.current();
    private final Selector networkSelector;
    private final PacketSource packetSource;
    private final PacketSink packetSink;
    private final SessionRegistry sessionRegistry;
    private final SocketProtector socketProtector;

    TcpTransmitter(final Selector networkSelector, final PacketSource packetSource, final PacketSink packetSink,
                   final SessionRegistry sessionRegistry, final SocketProtector socketProtector) {
        this.networkSelector = networkSelector;
        this.packetSource = packetSource;
        this.packetSink = packetSink;
        this.sessionRegistry = sessionRegistry;
        this.socketProtector = socketProtector;
    }

    @Override
    public void run() {
        LOG.info("TCP transmitter started");
        try {
            while (true) {
                Packet currentPacket;

                // Fetch a packet, wait if no available, break if interrupted
                // TODO: Block when not connected
                do {
                    currentPacket = packetSource.poll();
                    if (currentPacket != null) break;
                    Thread.sleep(10);
                } while (!Thread.currentThread().isInterrupted());
                // TODO: Use Thread.interrupted() this time? Or throw InterruptedException instead?
                if (Thread.currentThread().isInterrupted()) break;

                processPacket(currentPacket);
            }
        } catch (final InterruptedException e) {
            LOG.info("TCP transmitter stopping");
        } catch (final IOException e) {
            LOG.error("I/O exception in TCP transmitter", e);
        } finally {
            sessionRegistry.closeAll();
        }
    }

    private void processPacket(final Packet currentPacket) throws IOException {
        final IPv4Layer ip = (IPv4Layer) currentPacket.getFirstLayer();
        final TcpLayer tcp = (TcpLayer) ip.getNextLayer();

        // Extract destination socket from packet headers
        final InetSocketAddress dstSock = new InetSocketAddress(
                ip.getDestinationAddress(), tcp.getDestinationPort());

        // Check if we have an open connection already
        final Link registryKey = new Link(tcp.getSourcePort(), dstSock);
        final Connection connection = sessionRegistry.getConnection(registryKey);
        if (connection == null) {
            // Not connected, establish new
            initializeConnection(registryKey, currentPacket);
        } else if (tcp.isSYN()) {
            // We got a SYN after the connection was established
            processDuplicateSYN(connection, currentPacket);
        } else if (tcp.isRST()) {
            // Connection reset by client, close and remove it from registry
            closeCleanly(connection, currentPacket);
        } else if (tcp.isFIN()) {
            // Client will not send more data, determine if connection should be closed
            processFIN(connection, currentPacket);
        } else if (tcp.isACK()) {
            // Client sent an ACK
            processACK(connection, currentPacket);
        }
    }

    private void initializeConnection(final Link registryKey, final Packet packet)
            throws IOException {
        final IPv4Layer ip = (IPv4Layer) packet.getFirstLayer();
        final TcpLayer tcp = (TcpLayer) ip.getNextLayer();

        // Every connection starts with a SYN, if this is not the case then answer with RST (reset)
        if (!tcp.isSYN()) {
            TcpUtil.recyclePacketForEmptyResponse(packet, TcpLayer.FLAG_RST, 0, tcp.getSequenceNumber() + 1);
            packetSink.receive(packet);
            return;
        }

        // Create and configure a new channel
        final SocketChannel outChannel = SocketChannel.open();
        outChannel.configureBlocking(false);
        outChannel.socket().bind(null);
        socketProtector.protect(outChannel.socket());

        final Connection connection = new Connection(registryKey, new TCB(random.nextInt(Short.MAX_VALUE + 1),
                tcp.getSequenceNumber(), tcp.getSequenceNumber(), tcp.getAcknowledgementNumber()), outChannel);
        connection.getTcb().localAckN++; // SYN counts as a byte
        connection.getTransmittingQueue().setDataReceiver(new TxDelayedReceiver(connection));
        sessionRegistry.putConnection(connection);

        InetSocketAddress dstSock = connection.getExtra(Connection.EXTRA_DESTINATION_REDIRECT);
        if (dstSock == null) dstSock = registryKey.destination;

        try {
            // Attempt connection
            outChannel.connect(dstSock);

            if (!outChannel.finishConnect()) {
                // The channel is still connecting, register with selector to be notified when it completes & return
                connection.getTcb().state = TCB.State.SYN_SENT;
                networkSelector.wakeup();
                connection.setSelectionKey(outChannel.register(networkSelector, SelectionKey.OP_CONNECT, connection));
                connection.setPacketAttachment(packet); // <-- Packet will be reused once the connection is established
                return;
            }

            // Make a copy of the packet without options as a template for further I/O
            final Packet templatePacket = Packets.makeCopy(packet);
            templatePacket.getLayer(TcpLayer.class).editor().setOptions(null).commit();
            connection.setPacketAttachment(templatePacket);

            // The channel is connected already, answer local with SYN,ACK now
            processChannelConnected(connection, packet);

        } catch (final IOException e) {
            // In case of error, reset the connection (reuse packet but strip options)
            LOG.error("Connection error: " + dstSock, e);
            TcpUtil.recyclePacketForEmptyResponse(packet, TcpLayer.FLAG_RST,
                    0, connection.getTcb().localAckN, false);
            sessionRegistry.closeConnection(connection);
        }

        packetSink.receive(packet);
    }

    private void processDuplicateSYN(final Connection connection, final Packet packet) {
        synchronized (connection) {
            // We received a SYN: We want to increment our ACK in any case
            connection.getTcb().localAckN = packet.getLayer(TcpLayer.class).getSequenceNumber() + 1;

            if (connection.getTcb().state == TCB.State.SYN_SENT) {
                // The client repeated SYN to ask again for a connection, but we haven't got a response yet,
                // we already have the old packet as attachment w/ options, recycle the new one
                // TODO: Recycle packet when implemented
                return;
            }

            // ...otherwise the connection is in an invalid state and must be reset
            sendRSTAndClose(connection, packet);
        }
    }

    private void processFIN(final Connection connection, final Packet packet) {
        synchronized (connection) {
            final TcpLayer tcp = packet.getLayer(TcpLayer.class);
            connection.getTcb().localAckN = tcp.getSequenceNumber() + 1;
            connection.getTcb().remoteAckN = tcp.getAcknowledgementNumber();
            connection.getTransmittingQueue().putCommand(new ShutdownOutputCommand());

            /* // TODO: Begin experimental passive close code (is it necessary?)
            if (connection.getTcb().state == TCB.State.FIN_WAIT_2) {
                TcpUtil.recyclePacketForEmptyResponse(packet, TcpLayer.FLAG_ACK,
                        connection.getTcb().localSeqN, connection.getTcb().localAckN);
                packetSink.receive(packet);
                sessionRegistry.closeConnection(connection); // Should go to TIME_WAIT but nevermind
                return;
            }
            if (connection.getTcb().state == TCB.State.FIN_WAIT_1) {
                if (tcp.isACK()) {
                    TcpUtil.recyclePacketForEmptyResponse(packet, TcpLayer.FLAG_ACK,
                            connection.getTcb().localSeqN, connection.getTcb().localAckN);
                    sessionRegistry.closeConnection(connection); // Should go to TIME_WAIT but nevermind
                } else {
                    connection.getTcb().state = TCB.State.CLOSING;
                    TcpUtil.recyclePacketForEmptyResponse(packet, TcpLayer.FLAG_ACK,
                            connection.getTcb().localSeqN, connection.getTcb().localAckN);
                }
                packetSink.receive(packet);
                return;
            } // TODO: End experimental passive close code */

            if (connection.isWaitingForNetworkData()) {
                // There is still data to be received
                connection.getTcb().state = TCB.State.CLOSE_WAIT;
                TcpUtil.recyclePacketForEmptyResponse(packet, TcpLayer.FLAG_ACK,
                        connection.getTcb().localSeqN, connection.getTcb().localAckN);
            } else {
                // No more data to be received, prepare to close on next ACK from client
                connection.getTcb().state = TCB.State.LAST_ACK;
                TcpUtil.recyclePacketForEmptyResponse(packet, TcpLayer.FLAG_FIN | TcpLayer.FLAG_ACK,
                        connection.getTcb().localSeqN, connection.getTcb().localAckN);
                connection.getTcb().localSeqN++; // FIN counts as a byte
            }
        }
        packetSink.receive(packet);
    }

    private void processACK(final Connection connection, final Packet packet) throws IOException {
        final TcpLayer tcp = packet.getLayer(TcpLayer.class);
        final int payloadSize = tcp.getPayloadSize(); // Must be same as backing buffer limit - end of headers

        synchronized (connection) {
            // If we were waiting for the last ACK, this is it: close the connection
            if (connection.getTcb().state == TCB.State.LAST_ACK) {
                closeCleanly(connection, packet);
                return;
            }

            /* // TODO: Begin experimental passive close code (is it necessary?)
            if (connection.getTcb().state == TCB.State.FIN_WAIT_1) {
                connection.getTcb().state = TCB.State.FIN_WAIT_2;
                // TODO: Recycle packet when implemented
                return;
            }
            if (connection.getTcb().state == TCB.State.CLOSING) {
                // Should go to TIME_WAIT to ensure received but nevermind
                closeCleanly(connection, packet);
                return;
            } // TODO: End experimental passive close code */

            // If this is the response to our SYN,ACK, set the connection as established and register to the selector
            if (connection.getTcb().state == TCB.State.SYN_RECEIVED) {
                connection.getTcb().state = TCB.State.ESTABLISHED;
                networkSelector.wakeup();
                connection.setSelectionKey(connection.getUpstreamChannel().register(
                        networkSelector, SelectionKey.OP_READ, connection));
                connection.setWaitingForNetworkData(true);
            }

            // Ignore empty ACKs
            if (payloadSize == 0) {
                // TODO: Recycle packet when implemented
                return;
            }

            // If we weren't waiting for data, now we are
            if (!connection.isWaitingForNetworkData()) {
                networkSelector.wakeup();
                connection.getSelectionKey().interestOps(SelectionKey.OP_READ);
                connection.setWaitingForNetworkData(true);
            }

            // Put data in our transfer queue and answer with ACK
            connection.getTcb().localAckN = tcp.getSequenceNumber() + payloadSize;
            connection.getTcb().remoteAckN = tcp.getAcknowledgementNumber();

            // Note: The transfer queue will copy the buffer if necessary, we don't need to worry about packet recycling
            connection.getTransmittingQueue().putData(tcp.getPayloadBufferView());

            // TODO: We don't expect out-of-order packets, but verify
            TcpUtil.recyclePacketForEmptyResponse(packet, TcpLayer.FLAG_ACK,
                    connection.getTcb().localSeqN, connection.getTcb().localAckN);
        }
        packetSink.receive(packet);
    }

    private void sendRSTAndClose(final Connection connection, final Packet packet) {
        TcpUtil.recyclePacketForEmptyResponse(packet, TcpLayer.FLAG_RST, 0, connection.getTcb().localAckN);
        packetSink.receive(packet);
        sessionRegistry.closeConnection(connection);
    }

    private void closeCleanly(final Connection connection, final Packet packet) {
        // TODO: Recycle packet & buffer when implemented
        connection.getTransmittingQueue().putCommand(new CloseCommand());
    }

    static void processChannelConnected(final Connection connection, final Packet packet) {
        // The channel is connected, answer with SYN,ACK - keep options
        connection.getTcb().state = TCB.State.SYN_RECEIVED;
        // TODO: Set MSS for receiving larger packets from the device
        TcpUtil.recyclePacketForEmptyResponse(packet, TcpLayer.FLAG_SYN | TcpLayer.FLAG_ACK,
                connection.getTcb().localSeqN, connection.getTcb().localAckN, true);
        connection.getTcb().localSeqN++; // SYN counts as a byte
    }

    private class TxDelayedReceiver implements DataTransferQueue.DataReceiver {
        private final Connection connection;

        private TxDelayedReceiver(final Connection connection) {
            this.connection = connection;
        }

        @Override
        public void onDataReady(final ByteBuffer payload, final Object... attachments) {
            try {
                while (payload.hasRemaining()) connection.getUpstreamChannel().write(payload);
            } catch (final IOException e) {
                LOG.error("Network write error: " + connection.getLink(), e);
                // TODO: Copy may be useless since this is the last packet sent
                sendRSTAndClose(connection, Packets.makeCopy(connection.getPacketAttachment()));
            }
        }

        @Override
        public void onBufferRetained(final ByteBuffer buffer, final Object... attachments) {
            // Do nothing since we do not have any packet lifecycle to manage here
        }

        @Override
        public void onTransferCommand(final DataTransferQueue.TransferCommand command) {
            if (command instanceof CloseCommand) {
                sessionRegistry.closeConnection(connection);
            } else if (command instanceof ShutdownOutputCommand) {
                try {
                    connection.getUpstreamChannel().socket().shutdownOutput(); // TODO: Check if this works
                } catch (IOException e) {
                    LOG.error("Connection error: " + connection.getLink().destination, e);
                    // TODO: Copy may be useless since this is the last packet sent
                    sendRSTAndClose(connection, Packets.makeCopy(connection.getPacketAttachment()));
                }
            }
        }

    }

    private static class ShutdownOutputCommand implements DataTransferQueue.TransferCommand { }

    private static class CloseCommand implements DataTransferQueue.TransferCommand { }

}
