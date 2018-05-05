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

package io.github.lczx.aml.tunnel.network.udp;

import io.github.lczx.aml.tunnel.PacketSink;
import io.github.lczx.aml.tunnel.packet.Packet;
import io.github.lczx.aml.tunnel.packet.Packets;
import io.github.lczx.aml.tunnel.packet.UdpLayer;
import io.github.lczx.aml.tunnel.packet.editor.PayloadEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

class UdpReceiver implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(UdpReceiver.class);

    private final Selector networkSelector;
    private final PacketSink packetSink;

    UdpReceiver(final Selector networkSelector, final PacketSink packetSink) {
        this.networkSelector = networkSelector;
        this.packetSink = packetSink;
    }

    @Override
    public void run() {
        LOG.info("UDP receiver started");
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
                    if (!(key.isValid() && key.isReadable())) continue;

                    keyIterator.remove();

                    // Create a new UDP datagram from the network
                    final LinkInfo linkInfo = (LinkInfo) key.attachment();
                    final Packet packet = Packets.newDatagramPacket(linkInfo.remoteSocket, linkInfo.localSocket);

                    // Fill the datagram with data received from the network
                    // TODO: We should handle any IOException here immediately, but that probably won't happen with UDP
                    final PayloadEditor e = packet.getLayer(UdpLayer.class).payloadEditor();
                    final int readBytes = ((DatagramChannel) key.channel()).read(e.buffer(true));
                    LOG.trace("Read {} bytes from the network", readBytes);
                    e.flipAndCommit();

                    // Push the received datagram
                    packetSink.receive(packet);
                }
            }
        } catch (final InterruptedException e) {
            LOG.info("UDP receiver stopping");
        } catch (final IOException e) {
            LOG.error("I/O error on channel selection", e);
        }
    }

}
