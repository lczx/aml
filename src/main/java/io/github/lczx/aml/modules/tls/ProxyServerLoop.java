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

import io.github.lczx.aml.modules.tls.cert.ProxyCertificateProvider;
import io.github.lczx.aml.tunnel.IOUtils;
import io.github.lczx.aml.tunnel.SocketProtector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/* package */ class ProxyServerLoop implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ProxyServerLoop.class);

    private final RouteTable routeTable;
    private final ProxyCertificateProvider certProvider;
    private final SocketProtector socketProtector;

    private ServerSocket serverSocket;

    /* package */ ProxyServerLoop(final RouteTable routes, final ProxyCertificateProvider certProvider,
                    final SocketProtector socketProtector) {
        this.routeTable = routes;
        this.certProvider = certProvider;
        this.socketProtector = socketProtector;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(0);
            // serverSocket.setReceiveBufferSize(8192); // Necessary?
        } catch (IOException e) {
            LOG.error("Error while opening proxy server socket", e);
            return;
        }
        LOG.info("Proxy server started on {}", serverSocket.getLocalSocketAddress());

        while (true) {
            final Socket clientSocket;
            try {
                clientSocket = serverSocket.accept();
            } catch (final SocketException e) {
                LOG.debug("Socket closed, breaking loop");
                break;
            } catch (final IOException e) {
                LOG.error("Exception on socket accept", e);
                continue;
            }
            handleNewConnection(clientSocket);
        }
    }

    /* package */ int getLocalPort() {
        return serverSocket.getLocalPort();
    }

    /* package */ void closeServerSocket() {
        IOUtils.safeClose(serverSocket); // Called from another thread
    }

    private void handleNewConnection(final Socket socket) {
        final ProxyConnection route = routeTable.getRoute(socket.getPort());
        if (route == null) {
            LOG.warn("Unknown source port ({}), cannot determine proxy params: closing connection", socket.getPort());
            IOUtils.safeClose(socket);
            return;
        }

        final Thread connThread;
        switch (route.getProxyType()) {
            case HTTPS:
                LOG.debug("New connection from port {}, original destination {}, type HTTPS",
                        socket.getPort(), route.getTcpConnection().getLink().destination);
                connThread = new Thread(new HttpsProxyConnectionHandler(route, socket, certProvider, socketProtector),
                        "pxy_conn" + socket.getPort());
                break;

            default:
                LOG.error("Requested proxy type unknown, closing connection");
                IOUtils.safeClose(socket);
                return;
        }
        connThread.start();
    }

}
