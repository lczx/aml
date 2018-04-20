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
import org.spongycastle.crypto.tls.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
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
            final Pipe txPipe = new Pipe(downstreamTunnel, upstreamTunnel);
            final Pipe rxPipe = new Pipe(upstreamTunnel, downstreamTunnel);

            new Thread(rxPipe, Thread.currentThread().getName() + "-rx").start();
            Thread.currentThread().setName(Thread.currentThread().getName() + "-tx");
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

    private static class Pipe implements Runnable {

        private final TlsProtocol inProto, outProto;
        private Thread pipeThread;

        private Pipe(final TlsProtocol inProto, final TlsProtocol outProto) {
            this.inProto = inProto;
            this.outProto = outProto;
        }

        @Override
        public void run() {
            pipeThread = Thread.currentThread();
            try {
                final byte[] buf = new byte[8192];
                while (!Thread.interrupted()) {
                    int count;
                    try {
                        count = inProto.getInputStream().read(buf);
                    } catch (final TlsNoCloseNotifyException e) {
                        LOG.debug("{} got into an EOS-like situation: {}", this, e.getMessage());
                        count = -1;
                    } catch (final SocketException e) {
                        LOG.debug("{} input was closed: {}", this, e.getMessage());
                        count = -1;
                    }

                    if (count != -1) {
                        outProto.getOutputStream().write(buf, 0, count);
                        LOG.trace("{} wrote {} bytes", this, count);
                    } else {
                        LOG.debug("{} reached EOS, closing output and quitting", this);

                        // Do not use shutdownOutput() / isOutputShutdown() on the socket (it prevents transmission of
                        // close_notify); TLS does not support half-close to avoid truncation attacks. Closing output
                        // sends close_notify to the remote peer. See: https://tools.ietf.org/html/rfc2246#section-7.2.1
                        outProto.close();
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
            return "Pipe{" + pipeThread.getName() + '}';
        }

    }

}
