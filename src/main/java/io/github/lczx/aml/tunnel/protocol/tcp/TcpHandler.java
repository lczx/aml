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

import io.github.lczx.aml.tunnel.IOUtils;
import io.github.lczx.aml.tunnel.PacketSink;
import io.github.lczx.aml.tunnel.PacketSource;
import io.github.lczx.aml.tunnel.SocketProtector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.Selector;

public class TcpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TcpHandler.class);

    private final SessionRegistry sessionRegistry = new SessionRegistry();

    private Selector networkSelector;
    private Thread txTread;
    private Thread rxThread;

    public void start(final SocketProtector socketProtector,
                      final PacketSource pSrc, final PacketSink pDst) throws IOException {
        networkSelector = Selector.open();
        txTread = new Thread(new TcpTransmitter(networkSelector, pSrc, pDst, socketProtector, sessionRegistry));
        rxThread = new Thread(new TcpReceiver(networkSelector, pDst, sessionRegistry));
        txTread.start();
        rxThread.start();
    }

    public void shutdown() {
        txTread.interrupt();
        rxThread.interrupt();
        IOUtils.safeClose(networkSelector);

        // TODO: Clear session registry?

        txTread = null;
        rxThread = null;
        networkSelector = null;
    }

}
