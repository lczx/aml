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

import io.github.lczx.aml.tunnel.SocketProtector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

class ProxyServerLoop implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ProxyServerLoop.class);

    private final SocketProtector socketProtector;
    private ServerSocket serverSocket;

    ProxyServerLoop(final SocketProtector socketProtector) {
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

        while (!Thread.interrupted()) {
            final Socket clientSocket;
            try {
                clientSocket = serverSocket.accept(); // TODO: call serverSock.close() from another thread to interrupt
            } catch (final IOException e) {
                LOG.error("Exception on socket accept", e);
                continue;
            }
            handleNewConnection(clientSocket);
        }
    }

    public int getLocalPort() {
        return serverSocket.getLocalPort();
    }

    private void handleNewConnection(final Socket socket) {
        // TODO: Get original destination info and start a new thread to handle the connection
    }

}
