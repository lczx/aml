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

package io.github.lczx.aml.tunnel.protocol;

import io.github.lczx.aml.tunnel.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.Selector;

public abstract class ProtocolNetworkInterface {

    private static final Logger LOG = LoggerFactory.getLogger(ProtocolNetworkInterface.class);

    private Selector networkSelector;
    protected Thread txThread, rxThread;

    public void start() throws IOException {
        LOG.info("Starting {} I/O thread pair", this);
        this.networkSelector = Selector.open();
        txThread = new Thread(createTransmitterRunnable(networkSelector));
        rxThread = new Thread(createReceiverRunnable(networkSelector));
        txThread.start();
        rxThread.start();
    }

    public void shutdown() {
        LOG.info("Stopping {} I/O thread pair", this);
        txThread.interrupt();
        rxThread.interrupt();
        IOUtils.safeClose(networkSelector);

        txThread = null;
        rxThread = null;
        networkSelector = null;
    }

    protected abstract Runnable createTransmitterRunnable(Selector networkSelector);

    protected abstract Runnable createReceiverRunnable(Selector networkSelector);

}
