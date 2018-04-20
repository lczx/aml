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

import io.github.lczx.aml.hook.DraftTcpHook;
import io.github.lczx.aml.tunnel.protocol.tcp.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class TcpRedirectHook implements DraftTcpHook {

    private static final Logger LOG = LoggerFactory.getLogger(TcpRedirectHook.class);

    private final RouteTable routes;
    private final ProxyServerLoop serverRunnable;

    private InetSocketAddress proxyAddress;

    TcpRedirectHook(final RouteTable routes, final ProxyServerLoop serverRunnable) {
        this.routes = routes;
        this.serverRunnable = serverRunnable;
    }

    @Override
    public InetSocketAddress onConnect(final InetSocketAddress destination, final int localPort) {
        // Redirect connections to port 443 (HTTPS)
        if (destination.getPort() == 443) {
            LOG.debug("Intercepted connection from TCP relay port ({}) to HTTPS standard port (443), rerouting " +
                    "destination to proxy ({} becomes {})", localPort, destination, getProxyAddress());
            routes.addRoute(localPort, destination, RouteTable.TYPE_HTTPS);
            return getProxyAddress();
        }

        return destination;
    }

    @Override
    public void onClose(final Connection connection) {
        routes.removeRoute(connection.getUpstreamChannel().socket().getLocalPort());
    }

    private InetSocketAddress getProxyAddress() {
        if (proxyAddress == null)
            proxyAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), serverRunnable.getLocalPort());
        return proxyAddress;
    }

}
