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

import io.github.lczx.aml.modules.tls.cert.CryptoUtils;
import io.github.lczx.aml.modules.tls.cert.ProxyCertificateProvider;
import io.github.lczx.aml.modules.tls.proxy.ClientParameters;
import io.github.lczx.aml.modules.tls.proxy.ProxyTlsClient;
import io.github.lczx.aml.modules.tls.proxy.ProxyTlsServer;
import io.github.lczx.aml.modules.tls.proxy.ServerParameters;
import io.github.lczx.aml.tunnel.SocketProtector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.tls.TlsClientProtocol;
import org.spongycastle.crypto.tls.TlsServer;
import org.spongycastle.crypto.tls.TlsServerProtocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.SecureRandom;

/* package */ class HttpsProxyConnectionHandler implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(HttpsProxyConnectionHandler.class);

    private final ProxyConnection proxyConnection;
    private final ProxyCertificateProvider certificateProvider;
    private final SocketProtector socketProtector;
    private final Socket downstreamSocket;

    private TlsServerProtocol downstreamTunnel;
    private TlsClientProtocol upstreamTunnel;

    /* package */ HttpsProxyConnectionHandler(final ProxyConnection proxyConnection, final Socket acceptedSocket,
                                              final ProxyCertificateProvider certProvider,
                                              final SocketProtector socketProtector) {
        this.proxyConnection = proxyConnection;
        this.downstreamSocket = acceptedSocket;
        this.certificateProvider = certProvider;
        this.socketProtector = socketProtector;
    }

    @Override
    public void run() {
        try {
            downstreamTunnel = new ProxyServerProtocol(
                    downstreamSocket.getInputStream(), downstreamSocket.getOutputStream(),
                    CryptoUtils.createSecureRandom());
            LOG.debug("Starting downstream TLS server side ({}) on socket {}", downstreamTunnel, downstreamSocket);
            downstreamTunnel.accept(new ProxyTlsServer(certificateProvider));

            // Create pipes to transfer data up and down
            LOG.debug("Handshake on socket {} complete, starting I/O pipes", downstreamSocket);
            final PayloadPipe txPipe = new PayloadPipe(downstreamTunnel.getInputStream(),
                    upstreamTunnel.getOutputStream(), proxyConnection.getTransmittingQueue());
            final PayloadPipe rxPipe = new PayloadPipe(upstreamTunnel.getInputStream(),
                    downstreamTunnel.getOutputStream(), proxyConnection.getReceivingQueue());

            new Thread(rxPipe, Thread.currentThread().getName() + "-rx").start();
            Thread.currentThread().setName(Thread.currentThread().getName() + "-tx");
            txPipe.run();

        } catch (final IOException e) {
            LOG.error("I/O exception while establishing a tunnel from " + downstreamSocket.getRemoteSocketAddress() +
                    " to " + proxyConnection.getTcpConnection().getLink().destination, e);
            // TODO: Close socket (should close TCB as well)
        }
    }

    private void onClientHelloReceived(final TlsServer tlsServer,
                                       final ClientParameters clientParameters) throws IOException {
        final InetSocketAddress destinationSockAddress = proxyConnection.getTcpConnection().getLink().destination;

        LOG.debug("Received TLS Client Hello (server: {}, params: {}), connecting upstream to {}",
                downstreamTunnel, clientParameters, destinationSockAddress);
        final Socket upstreamSocket = new Socket(destinationSockAddress.getAddress(), destinationSockAddress.getPort());
        socketProtector.protect(upstreamSocket);

        final ProxyTlsClient tlsClient = new ProxyTlsClient(clientParameters);
        upstreamTunnel = new TlsClientProtocol(
                upstreamSocket.getInputStream(), upstreamSocket.getOutputStream(), CryptoUtils.createSecureRandom());
        upstreamTunnel.connect(tlsClient);

        final ServerParameters serverParams = tlsClient.makeServerParameters();
        LOG.debug("Upstream connection established emulating downstream client parameters, " +
                        "using upstream Server Hello response to answer downstream (dSrv: {}, uSrv: {}, params: {}",
                downstreamTunnel, upstreamTunnel, serverParams);
        ((ProxyTlsServer) tlsServer).setParams(serverParams);
    }

    private class ProxyServerProtocol extends TlsServerProtocol {
        private ProxyServerProtocol(final InputStream input, final OutputStream output,
                                    final SecureRandom secureRandom) {
            super(input, output, secureRandom);
        }

        @Override
        protected void sendServerHelloMessage() throws IOException {
            onClientHelloReceived(tlsServer, new ClientParameters(
                    getContext().getClientVersion(), offeredCipherSuites, offeredCompressionMethods, clientExtensions));
            super.sendServerHelloMessage();
        }
    }

}
