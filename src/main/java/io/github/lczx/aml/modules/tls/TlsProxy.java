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
import io.github.lczx.aml.modules.tls.cert.CredentialsLoader;
import io.github.lczx.aml.modules.tls.cert.ProxyCertificateBuilder;
import io.github.lczx.aml.modules.tls.cert.ProxyCertificateCache;
import io.github.lczx.aml.modules.tls.cert.ProxyCertificateProvider;
import io.github.lczx.aml.tunnel.SocketProtector;

import java.io.IOException;

public class TlsProxy {

    private final RouteTable proxyRoutes = new RouteTable();
    private final ProxyServerLoop serverRunnable;
    private final Thread serverThread;

    public TlsProxy(final SocketProtector socketProtector, final AssetManager assetManager) {
        final ProxyCertificateProvider certificateProvider;
        try {
            certificateProvider = new ProxyCertificateCache(new ProxyCertificateBuilder(
                    CredentialsLoader.loadCertificateX509(assetManager.open("ca.crt")),
                    CredentialsLoader.loadPrivateKeyDER(assetManager.open("ca.key"))));
        } catch (IOException e) {
            throw new RuntimeException("Cannot load CA credentials from application assets", e);
        }

        serverRunnable = new ProxyServerLoop(proxyRoutes, certificateProvider, socketProtector);
        serverThread = new Thread(serverRunnable);
    }

    public void start() {
        serverThread.start();
    }

    public void stop() {
        serverThread.interrupt();
    }

    public TcpRedirectHook createTcpHook() {
        return new TcpRedirectHook(proxyRoutes, serverRunnable);
    }

}
