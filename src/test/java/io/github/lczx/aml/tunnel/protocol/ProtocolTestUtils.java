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

package io.github.lczx.aml.tunnel.protocol;

import io.github.lczx.aml.AMLContext;
import io.github.lczx.aml.hook.EventDispatcher;
import io.github.lczx.aml.hook.monitoring.StatusMonitor;
import io.github.lczx.aml.tunnel.PacketSink;
import io.github.lczx.aml.tunnel.PacketSource;
import io.github.lczx.aml.tunnel.SocketProtector;
import io.github.lczx.aml.tunnel.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramSocket;
import java.net.Socket;
import java.util.Queue;

public final class ProtocolTestUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ProtocolTestUtils.class);

    private ProtocolTestUtils() { }

    public static class PacketConnector implements PacketSource, PacketSink {

        private final Queue<Packet> queue;

        public PacketConnector(final Queue<Packet> queue) {
            this.queue = queue;
        }

        @Override
        public void receive(final Packet packet) {
            queue.add(packet);
        }

        @Override
        public Packet poll() {
            return queue.poll();
        }
    }

    public static class DummySocketProtector implements SocketProtector {
        @Override
        public boolean protect(final Socket socket) {
            LOG.info("Got VPN protect request for TCP socket: {} -> {}",
                    socket.getLocalSocketAddress(), socket.getRemoteSocketAddress());
            return true;
        }

        @Override
        public boolean protect(final DatagramSocket datagramSocket) {
            LOG.info("Got VPN protect request for UDP socket: {} -> {}",
                    datagramSocket.getLocalSocketAddress(), datagramSocket.getRemoteSocketAddress());
            return true;
        }
    }

    public static class DummyContext implements AMLContext {

        private final SocketProtector socketProtector = new ProtocolTestUtils.DummySocketProtector();
        private final StatusMonitor statusMonitor = new StatusMonitor();
        private final EventDispatcher eventDispatcher = new EventDispatcher();

        @Override
        public SocketProtector getSocketProtector() {
            return socketProtector;
        }

        @Override
        public StatusMonitor getStatusMonitor() {
            return statusMonitor;
        }

        @Override
        public EventDispatcher getEventDispatcher() {
            return eventDispatcher;
        }

    }

}
