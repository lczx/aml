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

package io.github.lczx.aml.modules.tls;

import io.github.lczx.aml.hook.AMLEvent;
import io.github.lczx.aml.hook.AMLEventListener;
import io.github.lczx.aml.tunnel.network.tcp.Connection;
import io.github.lczx.aml.tunnel.network.tcp.TcpCloseConnectionEvent;
import io.github.lczx.aml.tunnel.network.tcp.TcpNewConnectionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/* package */ class TcpRedirectHook implements AMLEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(TcpRedirectHook.class);

    private final RouteTable routes;
    private final ProxyServerLoop serverRunnable;

    private InetSocketAddress proxyAddress;

    /* package */ TcpRedirectHook(final RouteTable routes, final ProxyServerLoop serverRunnable) {
        this.routes = routes;
        this.serverRunnable = serverRunnable;
    }

    @Override
    public void receiveEvent(final AMLEvent event) {
        if (event instanceof TcpNewConnectionEvent)
            onConnect(((TcpNewConnectionEvent) event).getConnection());
        else if (event instanceof TcpCloseConnectionEvent)
            onClose(((TcpCloseConnectionEvent) event).getConnection());
    }

    private void onConnect(final Connection connection) {
        final InetSocketAddress destination = connection.getLink().destination;
        final int relayPort = connection.getUpstreamChannel().socket().getLocalPort();

        // Redirect connections to port 443 (HTTPS)
        if (destination.getPort() == 443) {
            LOG.debug("Intercepted connection from TCP relay port ({}) to HTTPS standard port (443), rerouting " +
                    "destination to proxy ({} becomes {})", relayPort, destination, getProxyAddress());
            routes.addRoute(relayPort, new ProxyConnection(connection, ProxyConnection.Type.HTTPS));
            connection.putExtra(Connection.EXTRA_DESTINATION_REDIRECT, getProxyAddress());
        }
    }

    private void onClose(final Connection connection) {
        routes.removeRoute(connection.getUpstreamChannel().socket().getLocalPort());
    }

    private InetSocketAddress getProxyAddress() {
        if (proxyAddress == null)
            proxyAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), serverRunnable.getLocalPort());
        return proxyAddress;
    }

}
