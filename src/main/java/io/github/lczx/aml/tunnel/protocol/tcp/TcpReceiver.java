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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.Selector;

class TcpReceiver implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(TcpReceiver.class);

    private final Selector networkSelector;
    private final PacketSink packetSink;
    private final SessionRegistry sessionRegistry;

    TcpReceiver(final Selector networkSelector, final PacketSink packetSink, final SessionRegistry sessionRegistry) {
        this.networkSelector = networkSelector;
        this.packetSink = packetSink;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void run() {

    }

}
