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

package io.github.lczx.aml.tunnel.network.udp;

import io.github.lczx.aml.AMLContext;
import io.github.lczx.aml.hook.monitoring.BaseMeasureKeys;
import io.github.lczx.aml.hook.monitoring.MeasureHolder;
import io.github.lczx.aml.hook.monitoring.StatusProbe;
import io.github.lczx.aml.tunnel.PacketSink;
import io.github.lczx.aml.tunnel.PacketSource;
import io.github.lczx.aml.tunnel.network.ProtocolNetworkInterface;

import java.io.IOException;
import java.nio.channels.Selector;

public class UdpNetworkInterface extends ProtocolNetworkInterface {

    private final AMLContext amlContext;
    private final PacketSource packetSource;
    private final PacketSink packetDestination;

    public UdpNetworkInterface(final AMLContext amlContext, final PacketSource pSrc, final PacketSink pDst) {
        this.amlContext = amlContext;
        this.packetSource = pSrc;
        this.packetDestination = pDst;
    }

    @Override
    public void start() throws IOException {
        super.start();
        amlContext.getStatusMonitor().attachProbe(new UdpProbe());
    }

    @Override
    protected Runnable createTransmitterRunnable(final Selector networkSelector) {
        return new UdpTransmitter(networkSelector, packetSource, amlContext);
    }

    @Override
    protected Runnable createReceiverRunnable(final Selector networkSelector) {
        return new UdpReceiver(networkSelector, packetDestination);
    }

    private class UdpProbe implements StatusProbe {
        @Override
        public void onMeasure(final MeasureHolder m) {
            m.putInt(BaseMeasureKeys.THREAD_STATE_UDP_TX, txThread.getState().ordinal());
            m.putInt(BaseMeasureKeys.THREAD_STATE_UDP_RX, rxThread.getState().ordinal());
        }
    }

}
