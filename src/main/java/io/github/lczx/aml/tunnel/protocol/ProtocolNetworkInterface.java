package io.github.lczx.aml.tunnel.protocol;

import io.github.lczx.aml.tunnel.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.Selector;

public abstract class ProtocolNetworkInterface {

    private static final Logger LOG = LoggerFactory.getLogger(ProtocolNetworkInterface.class);

    private Selector networkSelector;
    private Thread txThread, rxThread;

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
        IOUtils.closeResources(networkSelector);
    }

    protected abstract Runnable createTransmitterRunnable(Selector networkSelector);

    protected abstract Runnable createReceiverRunnable(Selector networkSelector);

}
