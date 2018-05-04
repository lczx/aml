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

import io.github.lczx.aml.AMLContext;
import io.github.lczx.aml.hook.AMLModule;
import io.github.lczx.aml.hook.AMLTunnelModule;
import io.github.lczx.aml.hook.ModuleParameters;
import io.github.lczx.aml.modules.tls.cert.CredentialsLoader;
import io.github.lczx.aml.modules.tls.cert.ProxyCertificateBuilder;
import io.github.lczx.aml.modules.tls.cert.ProxyCertificateCache;
import io.github.lczx.aml.modules.tls.cert.ProxyCertificateProvider;
import io.github.lczx.aml.tunnel.network.tcp.TcpCloseConnectionEvent;
import io.github.lczx.aml.tunnel.network.tcp.TcpNewConnectionEvent;

import java.io.IOException;
import java.util.Objects;

@AMLModule(name = "TLS Proxy")
public class TlsProxy implements AMLTunnelModule {

    public static final String PARAM_CA_CERTIFICATE = "ca_crt";
    public static final String PARAM_CA_PRIVATE_KEY = "ca_pvk";

    private final ProxyCertificateProvider certificateProvider;

    private ProxyServerLoop serverRunnable;
    private Thread serverThread;

    public TlsProxy(final ModuleParameters parameters) {
        Objects.requireNonNull(parameters, "Module started without params, cannot determine CA credentials");
        final byte[] certBytes = parameters.getParameter(PARAM_CA_CERTIFICATE);
        final byte[] keyBytes = parameters.getParameter(PARAM_CA_PRIVATE_KEY);
        if (certBytes == null || keyBytes == null)
            throw new IllegalArgumentException("Module initialization failed, no CA credentials provided");

        try {
            certificateProvider = new ProxyCertificateCache(new ProxyCertificateBuilder(
                    CredentialsLoader.loadCertificateX509(certBytes),
                    CredentialsLoader.loadPrivateKeyDER(keyBytes)));
        } catch (final IOException e) {
            throw new RuntimeException("Malformed credentials parameter", e);
        }
    }

    @Override
    public void initialize(final AMLContext amlContext) {
        final RouteTable proxyRoutes = new RouteTable(amlContext);
        serverRunnable = new ProxyServerLoop(proxyRoutes, certificateProvider, amlContext.getSocketProtector());
        serverThread = new Thread(serverRunnable, "pxy_server");

        amlContext.getEventDispatcher().addEventListener(new TcpRedirectHook(proxyRoutes, serverRunnable),
                TcpNewConnectionEvent.class, TcpCloseConnectionEvent.class);
    }

    @Override
    public void onStart() {
        serverThread.start();
    }

    @Override
    public void onStop() {
        serverThread.interrupt();
        serverRunnable.closeServerSocket();
    }

}
