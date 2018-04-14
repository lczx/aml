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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.Selector;

class TcpTransmitter implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(TcpTransmitter.class);

    private final Selector networkSelector;
    private final PacketSource packetSource;
    private final PacketSink packetSink;
    private final SocketProtector socketProtector;
    private final SessionRegistry sessionRegistry;

    TcpTransmitter(final Selector networkSelector, final PacketSource packetSource, final PacketSink packetSink,
                          final SocketProtector socketProtector, final SessionRegistry sessionRegistry) {

        this.networkSelector = networkSelector;
        this.packetSource = packetSource;
        this.packetSink = packetSink;
        this.socketProtector = socketProtector;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void run() {

    }

}
