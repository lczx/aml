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

import android.content.res.AssetManager;
import io.github.lczx.aml.AMLContext;
import io.github.lczx.aml.hook.AMLModule;
import io.github.lczx.aml.hook.AMLTunnelModule;
import io.github.lczx.aml.modules.tls.cert.CredentialsLoader;
import io.github.lczx.aml.modules.tls.cert.ProxyCertificateBuilder;
import io.github.lczx.aml.modules.tls.cert.ProxyCertificateCache;
import io.github.lczx.aml.modules.tls.cert.ProxyCertificateProvider;
import io.github.lczx.aml.tunnel.AMLTunnelService;
import io.github.lczx.aml.tunnel.protocol.tcp.TcpCloseConnectionEvent;
import io.github.lczx.aml.tunnel.protocol.tcp.TcpNewConnectionEvent;

import java.io.IOException;

@AMLModule(name = "TLS Proxy")
public class TlsProxy implements AMLTunnelModule {

    private ProxyServerLoop serverRunnable;
    private Thread serverThread;

    @Override
    public void initialize(final AMLContext amlContext) {
        final RouteTable proxyRoutes = new RouteTable(amlContext);

        // TODO: HAAAX (Will probably be no longer required once we generate our own CAs)
        final AssetManager assetManager = ((AMLTunnelService) amlContext.getSocketProtector()).getAssets();

        final ProxyCertificateProvider certificateProvider;
        try {
            certificateProvider = new ProxyCertificateCache(new ProxyCertificateBuilder(
                    CredentialsLoader.loadCertificateX509(assetManager.open("ca.crt")),
                    CredentialsLoader.loadPrivateKeyDER(assetManager.open("ca.key"))));
        } catch (final IOException e) {
            throw new RuntimeException("Cannot load CA credentials from application assets", e);
        }

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
