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

package io.github.lczx.aml.proto.http;

import io.github.lczx.aml.tunnel.network.Connection;
import io.github.lczx.aml.tunnel.network.DataTransferQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Utility class to interface a {@link Connection} to an {@link HttpSessionAnalyzer}.
 *
 * <p> It rewinds all read buffers before sending and catches all unhandled exceptions from the parser to avoid them
 * reaching the tunnel core running in the same thread.
 */
public class HttpConnectionListener implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(HttpConnectionListener.class);

    private final Connection connection;
    private final HttpSessionAnalyzer sessionAnalyzer;

    public HttpConnectionListener(final Connection connection, final HttpSessionAnalyzer sessionAnalyzer) {
        this.connection = connection;
        this.sessionAnalyzer = sessionAnalyzer;

        final TransferListener listener = new TransferListener();
        connection.getReceivingQueue().addDataListener(listener, false);
        connection.getTransmittingQueue().addDataListener(listener, false);
    }

    @Override
    public void close() throws IOException {
        // Unregister?
        sessionAnalyzer.close();
    }

    private class TransferListener implements DataTransferQueue.DataListener {
        @Override
        public void onNewData(final DataTransferQueue transferQueue, final ByteBuffer pendingData) {
            try {
                if (transferQueue == connection.getTransmittingQueue())
                    sessionAnalyzer.receiveUplink(pendingData);
                else if (transferQueue == connection.getReceivingQueue())
                    sessionAnalyzer.receiveDownlink(pendingData);
            } catch (final Exception e) {
                LOG.error("Unhandled exception while reading HTTP stream", e);
            } finally {
                pendingData.rewind();
            }
        }
    }

}
