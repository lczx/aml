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
import io.github.lczx.aml.tunnel.network.DataTransferQueue;
import io.github.lczx.aml.tunnel.packet.Packet;
import io.github.lczx.aml.tunnel.packet.Packets;
import io.github.lczx.aml.tunnel.packet.TcpLayer;
import io.github.lczx.aml.tunnel.packet.editor.PayloadEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
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
                    final TcpConnection connection = (TcpConnection) key.attachment();
                    if (key.isValid()) {
                        if (key.isConnectable()) {
                            keyIterator.remove();
                            processConnect(connection);
                        } else if (key.isReadable()) {
                            keyIterator.remove();
                            processInput(connection);
                        }
                    }
                }
            }
        } catch (final InterruptedException e) {
            LOG.info("TCP receiver stopping");
        } catch (final IOException e) {
            LOG.error("I/O exception in TCP receiver", e);
        }
    }

    private void processConnect(final TcpConnection connection) {
        // This is the packet attached in TcpTransmitter#initializeConnection() [with TCP SYN options] (not a copy)
        final Packet packet = connection.getPacketAttachment();

        try {
            if (!connection.getUpstreamChannel().finishConnect())
                throw new IOException("finishConnect() returned false when channel was connectable");

            // Make a copy of the packet without options as a template for further I/O
            final Packet templatePacket = Packets.makeCopy(packet);
            templatePacket.getLayer(TcpLayer.class).editor().setOptions(null).commit();
            connection.setPacketAttachment(templatePacket);

            // The channel is now connected, answer local with SYN,ACK
            TcpTransmitter.processChannelConnected(connection, packet);

            connection.getSelectionKey().interestOps(SelectionKey.OP_READ); // todo mmh

        } catch (final IOException e) {
            // Got an error, reset the connection, use packet attachment, strip options
            LOG.error("I/O exception on channel connect: " + connection.getLink(), e);
            TcpUtil.recyclePacketForEmptyResponse(packet, TcpLayer.FLAG_RST,
                    0, connection.getTcb().localAckN, false);
            sessionRegistry.closeConnection(connection);
        }

        packetSink.receive(packet);
    }

    private void processInput(final TcpConnection connection) {
        synchronized (connection) {
            // This is the attached copy without options
            final Packet refPacket = Packets.makeCopy(connection.getPacketAttachment());

            final PayloadEditor editor = refPacket.getLayer(TcpLayer.class).payloadEditor();
            final int readBytes;
            final ByteBuffer payloadBuffer = editor.buffer(true);
            try {
                readBytes = connection.getUpstreamChannel().read(payloadBuffer);
                payloadBuffer.flip();
            } catch (final IOException e) {
                LOG.error("Network read error: " + connection.getLink(), e);
                editor.clearContent();

                TcpUtil.recyclePacketForEmptyResponse(refPacket, TcpLayer.FLAG_RST,
                        0, connection.getTcb().localAckN);
                packetSink.receive(refPacket);
                sessionRegistry.closeConnection(connection);
                return;
            }

            if (!connection.getReceivingQueue().isReceiverSet())
                connection.getReceivingQueue().setDataReceiver(new RxDelayedReceiver(connection));

            if (readBytes == -1) {
                connection.getReceivingQueue().putCommand(new EOSCommand(refPacket));
            } else {
                connection.getReceivingQueue().putData(payloadBuffer, refPacket, editor);
            }
        }
    }

    private class RxDelayedReceiver implements DataTransferQueue.DataReceiver {
        private final TcpConnection connection;

        private RxDelayedReceiver(final TcpConnection connection) {
            this.connection = connection;
        }

        @Override
        public void onDataReady(final ByteBuffer buffer, final Object... attachments) {
            final Packet refPacket;
            final int bufferSize = buffer.remaining();
            if (attachments != null) {
                // The buffer was not retained/copied and is part of a pending edit, committing now
                refPacket = (Packet) attachments[0];
                ((PayloadEditor) attachments[1]).commit();
            } else {
                // The buffer is a copy or generated, we need to create a new packet and copy data into it
                refPacket = Packets.makeCopy(connection.getPacketAttachment());
                final PayloadEditor editor = refPacket.getLayer(TcpLayer.class).payloadEditor();
                editor.buffer(true).put(buffer);
                editor.flipAndCommit();
            }

            // TODO: We should ideally be splitting segments by MTU/MSS, but this seems to work without
            TcpUtil.recyclePacketForResponse(refPacket, TcpLayer.FLAG_PSH | TcpLayer.FLAG_ACK,
                    connection.getTcb().localSeqN, connection.getTcb().localAckN, true, false);
            connection.getTcb().localSeqN += bufferSize; // Next sequence number
            packetSink.receive(refPacket);
        }

        @Override
        public void onBufferRetained(final ByteBuffer buffer, final Object... attachments) {
            // The buffer was retained (and copied), recycle the unused packet
            final Packet packet = (Packet) attachments[0];
            // TODO: Release packet when implemented
        }

        @Override
        public void onTransferCommand(final DataTransferQueue.TransferCommand command) {
            final Packet refPacket = ((EOSCommand) command).refPacket;

            // End of stream, stop waiting until we push more data
            connection.getSelectionKey().interestOps(0);
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
            packetSink.receive(refPacket);
        }
    }

    private static class EOSCommand implements DataTransferQueue.TransferCommand {
        private final Packet refPacket;

        private EOSCommand(final Packet refPacket) {
            this.refPacket = refPacket;
        }
    }

}
