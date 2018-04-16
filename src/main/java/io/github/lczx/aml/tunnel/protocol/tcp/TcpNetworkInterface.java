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

package io.github.lczx.aml.tunnel.protocol.tcp;

import io.github.lczx.aml.tunnel.PacketSink;
import io.github.lczx.aml.tunnel.PacketSource;
import io.github.lczx.aml.tunnel.SocketProtector;
import io.github.lczx.aml.tunnel.protocol.ProtocolNetworkInterface;

import java.nio.channels.Selector;

public class TcpNetworkInterface extends ProtocolNetworkInterface {

    // TODO: Override shutdown to clear session registry?

    private final SessionRegistry sessionRegistry = new SessionRegistry();
    private final SocketProtector socketProtector;
    private final PacketSource packetSource;
    private final PacketSink packetDestination;

    public TcpNetworkInterface(final SocketProtector socketProtector, final PacketSource pSrc, final PacketSink pDst) {
        this.socketProtector = socketProtector;
        this.packetSource = pSrc;
        this.packetDestination = pDst;
    }

    @Override
    protected Runnable createTransmitterRunnable(final Selector networkSelector) {
        return new TcpTransmitter(networkSelector, packetSource, packetDestination, socketProtector, sessionRegistry);
    }

    @Override
    protected Runnable createReceiverRunnable(final Selector networkSelector) {
        return new TcpReceiver(networkSelector, packetDestination, sessionRegistry);
    }

}
