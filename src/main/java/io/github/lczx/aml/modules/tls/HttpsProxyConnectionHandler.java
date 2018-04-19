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
import io.github.lczx.aml.tunnel.SocketProtector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.tls.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.SecureRandom;

class HttpsProxyConnectionHandler implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(HttpsProxyConnectionHandler.class);

    private final InetSocketAddress destinationSockAddress;
    private final ProxyCertificateProvider certificateProvider;
    private final SocketProtector socketProtector;
    private final Socket downstreamSocket;
    private Socket upstreamSocket;
    private TlsServerProtocol downstreamTunnel;
    private TlsClientProtocol upstreamTunnel;
    private boolean halfClosed = false;

    HttpsProxyConnectionHandler(final InetSocketAddress destinationSockAddress, final Socket acceptedSocket,
                                final ProxyCertificateProvider certProvider, final SocketProtector socketProtector) {
        this.destinationSockAddress = destinationSockAddress;
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
            final Pipe txPipe = new Pipe("TX", downstreamTunnel, upstreamTunnel);
            final Pipe rxPipe = new Pipe("RX", upstreamTunnel, downstreamTunnel);

            new Thread(rxPipe).start();
            txPipe.run();

        } catch (final IOException e) {
            LOG.error("I/O exception while establishing a tunnel from " +
                    downstreamSocket.getRemoteSocketAddress() + " to " + destinationSockAddress, e);
            // TODO: Close socket (should close TCB as well)
        }
    }

    private void onClientHelloReceived(final TlsServer tlsServer,
                                       final ClientParameters clientParameters) throws IOException {
        LOG.debug("Received TLS Client Hello (server: {}, params: {}), connecting upstream to {}",
                downstreamTunnel, clientParameters, destinationSockAddress);
        upstreamSocket = new Socket(destinationSockAddress.getAddress(), destinationSockAddress.getPort());
        socketProtector.protect(upstreamSocket);

        final ProxyTlsClient tlsClient = new ProxyTlsClient(clientParameters);
        upstreamTunnel = new TlsClientProtocol(
                upstreamSocket.getInputStream(), upstreamSocket.getOutputStream(), CryptoUtils.createSecureRandom());
        upstreamTunnel.connect(tlsClient);

        LOG.debug("Upstream connection established");
        ((ProxyTlsServer) tlsServer).setOriginalCertificate(tlsClient.getOriginalCertificate());
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

    private class Pipe implements Runnable {

        private final String name;
        private final TlsProtocol inProto, outProto;

        private Pipe(final String name, final TlsProtocol inProto, final TlsProtocol outProto) {
            this.name = name;
            this.inProto = inProto;
            this.outProto = outProto;
        }

        @Override
        public void run() {
            try {
                final byte[] buf = new byte[8192];
                while (!Thread.interrupted()) {
                    int count;
                    try {
                        count = inProto.getInputStream().read(buf);
                    } catch (TlsNoCloseNotifyException e) {
                        LOG.debug("{} got into an EOS-like situation: {}", this, e.getMessage());
                        count = -1;
                    }

                    if (count != -1) {
                        outProto.getOutputStream().write(buf, 0, count);
                        LOG.trace("{} wrote {} bytes", this, count);
                    } else {
                        LOG.debug("{} reached EOS, closing output and quitting", this);

                        // Do not use shutdownOutput() / isOutputShutdown() otherwise we can't answer to close_notify,
                        // use a boolean to monitor the other thread's status
                        // if (!outSock.isClosed()) outSock.shutdownOutput();
                        if (halfClosed) {
                            LOG.debug("{}, source has output shutdown too, also closing both sockets", this);
                            TlsIOUtils.safeClose(inProto, outProto);
                            halfClosed = false;
                        } else {
                            halfClosed = true;
                        }
                        break;
                    }
                }
            } catch (final IOException e) {
                LOG.error(this.toString() + " errored while transferring data", e);
                TlsIOUtils.safeClose(inProto, outProto);
            }
        }

        @Override
        public String toString() {
            return "Pipe{\"" + name + "\", " + Integer.toHexString(hashCode()) + '}';
        }

    }

}
