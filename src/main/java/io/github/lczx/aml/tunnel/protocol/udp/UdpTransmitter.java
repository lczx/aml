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

package io.github.lczx.aml.tunnel.protocol.udp;

import io.github.lczx.aml.AMLContext;
import io.github.lczx.aml.hook.monitoring.BaseMeasureKeys;
import io.github.lczx.aml.hook.monitoring.MeasureHolder;
import io.github.lczx.aml.hook.monitoring.StatusProbe;
import io.github.lczx.aml.tunnel.PacketSource;
import io.github.lczx.aml.tunnel.packet.IPv4Layer;
import io.github.lczx.aml.tunnel.packet.Packet;
import io.github.lczx.aml.tunnel.packet.UdpLayer;
import io.github.lczx.aml.tunnel.protocol.Link;
import io.github.lczx.aml.tunnel.protocol.LruCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

class UdpTransmitter implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(UdpTransmitter.class);
    private static final int MAX_CACHE_SIZE = 64;

    private final Selector networkSelector;
    private final PacketSource packetSource;
    private final AMLContext amlContext;

    private final LruCache<Link, DatagramChannel> channelCache =
            new LruCache<>(MAX_CACHE_SIZE, new LruCache.RemoveCallback<Link, DatagramChannel>() {
                @Override
                public void onRemove(final Map.Entry<Link, DatagramChannel> eldest) {
                    closeChannel(eldest.getValue());
                }
            });

    UdpTransmitter(final Selector networkSelector, final PacketSource packetSource, final AMLContext amlContext) {
        this.networkSelector = networkSelector;
        this.packetSource = packetSource;
        this.amlContext = amlContext;
        amlContext.getStatusMonitor().attachProbe(new UdpTxProbe());
    }

    @Override
    public void run() {
        LOG.info("UDP transmitter started");
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

                // TODO: Use Thread.interrupted() this time? Throw InterruptedException instead?
                if (Thread.currentThread().isInterrupted()) break;

                final IPv4Layer ip = (IPv4Layer) currentPacket.getFirstLayer();
                final UdpLayer udp = (UdpLayer) ip.getNextLayer();

                // Extract destination socket from packet headers
                final InetSocketAddress dstSock = new InetSocketAddress(
                        ip.getDestinationAddress(), udp.getDestinationPort());

                // Use a channel in cache if present
                final Link cacheKey = new Link(udp.getSourcePort(), dstSock);
                DatagramChannel outChannel;
                synchronized (channelCache) {
                    outChannel = channelCache.get(cacheKey);
                }
                if (outChannel == null) {
                    // No channel already in cache, create a new one
                    final InetSocketAddress srcSock = new InetSocketAddress(
                            ip.getSourceAddress(), udp.getSourcePort());
                    outChannel = createAndRegisterChannel(srcSock, dstSock);
                    if (outChannel == null) {
                        LOG.error("Error while establishing UDP connection to {}, dropping packet", dstSock);
                        // TODO: Recycle packet with buffer here when implemented
                        continue;
                    }
                    synchronized (channelCache) {
                        channelCache.put(cacheKey, outChannel);
                    }
                }

                // Send packet payload through channel
                try {
                    final ByteBuffer payload = udp.getPayloadBufferView();
                    while (payload.hasRemaining()) outChannel.write(payload);
                } catch (final IOException e) {
                    LOG.error("UDP write error, destination: {}", dstSock);
                    synchronized (channelCache) {
                        channelCache.remove(cacheKey);
                    }
                    closeChannel(outChannel);
                }
                // TODO: Recycle packet with buffer here when implemented
            }
        } catch (final InterruptedException e) {
            LOG.info("UDP transmitter stopping");
        } finally {
            closeAll();
        }
    }

    private DatagramChannel createAndRegisterChannel(final InetSocketAddress srcSock, final InetSocketAddress dstSock) {
        try {
            final DatagramChannel channel = DatagramChannel.open();
            channel.connect(dstSock);
            channel.configureBlocking(false);
            amlContext.getSocketProtector().protect(channel.socket());

            // Attach link information for response
            networkSelector.wakeup();
            channel.register(networkSelector, SelectionKey.OP_READ, new LinkInfo(srcSock, dstSock));
            return channel;
        } catch (final IOException e) {
            LOG.error("Error while establishing an UDP connection to " + dstSock, e);
            return null;
        }
    }

    private void closeChannel(final DatagramChannel channel) {
        try {
            channel.close();
        } catch (final IOException e) {
            LOG.error("Exception while closing UDP channel " + channel, e);
        }
    }

    private void closeAll() {
        synchronized (channelCache) {
            final Iterator<Map.Entry<Link, DatagramChannel>> it = channelCache.entrySet().iterator();
            while (it.hasNext()) {
                closeChannel(it.next().getValue());
                it.remove();
            }
        }
    }

    private class UdpTxProbe implements StatusProbe {
        @Override
        public void onMeasure(final MeasureHolder m) {
            // Note: this runs on the main thread
            synchronized (channelCache) {
                final ArrayList<String> l = new ArrayList<>(channelCache.size());
                for (final Map.Entry<Link, DatagramChannel> i : channelCache.entrySet())
                    l.add(String.format("%s -> %s", i.getKey(), i.getValue()));

                m.putStringArray(BaseMeasureKeys.UDP_SOCK_CACHE_DUMP, l.toArray(new String[0]));
                m.putInt(BaseMeasureKeys.UDP_SOCK_CACHE_CAPACITY, channelCache.getMaxSize());
            }
        }
    }

}
