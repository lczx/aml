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

package io.github.lczx.aml;

import android.content.Context;
import android.net.VpnService;
import io.github.lczx.aml.hook.EventDispatcher;
import io.github.lczx.aml.hook.monitoring.StatusMonitor;
import io.github.lczx.aml.tunnel.SocketProtector;

import java.net.DatagramSocket;
import java.net.Socket;

public class AMLContextImpl implements AMLContext {

    private final VpnService vpnService;
    private final SocketProtector socketProtector = new SockPr();
    private final StatusMonitor statusMonitor = new StatusMonitor();
    private final EventDispatcher eventDispatcher = new EventDispatcher();

    public AMLContextImpl(final VpnService vpnService) {
        this.vpnService = vpnService;
    }

    @Override
    public SocketProtector getSocketProtector() {
        return socketProtector;
    }

    @Override
    public StatusMonitor getStatusMonitor() {
        return statusMonitor;
    }

    @Override
    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    @Override
    public Context getServiceContext() {
        return vpnService;
    }

    private class SockPr implements SocketProtector {
        @Override
        public boolean protect(final Socket socket) {
            return vpnService.protect(socket);
        }

        @Override
        public boolean protect(final DatagramSocket datagramSocket) {
            return vpnService.protect(datagramSocket);
        }
    }

}
