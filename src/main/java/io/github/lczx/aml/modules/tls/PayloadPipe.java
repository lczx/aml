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

import io.github.lczx.aml.tunnel.IOUtils;
import io.github.lczx.aml.tunnel.protocol.DataTransferQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.tls.TlsNoCloseNotifyException;
import org.spongycastle.crypto.tls.TlsProtocol;

import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/* package */ class PayloadPipe implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(PayloadPipe.class);

    private final ReadableByteChannel input;
    private final WritableByteChannel output;
    private final DataTransferQueue transferQueue;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
    private Thread pipeThread;

    /* package */ PayloadPipe(final TlsProtocol inProto, final TlsProtocol outProto,
                              final DataTransferQueue transferQueue) {
        this.input = Channels.newChannel(inProto.getInputStream());
        this.output = Channels.newChannel(outProto.getOutputStream());
        this.transferQueue = transferQueue;
        transferQueue.setDataReceiver(new DelayedWriter());
    }

    @Override
    public void run() {
        pipeThread = Thread.currentThread();
        try {
            while (!Thread.interrupted()) {
                int count;
                try {
                    count = input.read(buffer);
                    buffer.flip();
                } catch (final TlsNoCloseNotifyException e) {
                    LOG.debug("{} got into an EOS-like situation: {}", this, e.getMessage());
                    count = -1;
                } catch (final SocketException e) {
                    LOG.debug("{} input was closed: {}", this, e.getMessage());
                    count = -1;
                }

                if (count != -1) {
                    transferQueue.putData(buffer);
                } else {
                    LOG.debug("{} reached EOS, closing output and quitting", this);
                    transferQueue.putCommand(new EOSCommand());
                    break;
                }
            }
        } catch (final IOException e) {
            LOG.error(this.toString() + " errored while transferring data", e);
            IOUtils.safeClose(input, output);
        }
    }

    @Override
    public String toString() {
        return "PayloadPipe{" + pipeThread.getName() + '}';
    }

    private class DelayedWriter implements DataTransferQueue.DataReceiver {
        @Override
        public void onDataReady(final ByteBuffer buffer, final Object... attachments) {
            try {
                final int count = output.write(buffer);
                buffer.clear(); // <-- We don't care if this is our buffer or a copy
                LOG.trace("{} wrote {} bytes", this, count);
            } catch (final IOException e) {
                LOG.error(this.toString() + " errored while transferring data", e);
                IOUtils.safeClose(input, output);
            }
        }

        @Override
        public void onBufferRetained(final ByteBuffer buffer, final Object... attachments) {
            // Data was retained/copied, clear the buffer for reuse
            PayloadPipe.this.buffer.clear(); // same as "buffer.clear();"
        }

        @Override
        public void onTransferCommand(final DataTransferQueue.TransferCommand command) {
            if (!(command instanceof EOSCommand)) return;

            // Do not use shutdownOutput() / isOutputShutdown() on the socket (it prevents transmission of
            // close_notify); TLS does not support half-close to avoid truncation attacks. Closing output
            // sends close_notify to the remote peer. See: https://tools.ietf.org/html/rfc2246#section-7.2.1
            try {
                output.close();
            } catch (final IOException e) {
                LOG.error(this.toString() + " errored while transferring data", e);
                IOUtils.safeClose(input, output);
            }
        }
    }

    private static class EOSCommand implements DataTransferQueue.TransferCommand { }

}
