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

package io.github.lczx.aml.tunnel;

import io.github.lczx.aml.tunnel.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;

public class TunDownlinkWriter implements TaskRunner.Task {

    private static final Logger LOG = LoggerFactory.getLogger(TunDownlinkWriter.class);

    private final FileDescriptor deviceDescriptor;
    private final PacketSource downlinkSource;
    private FileChannel deviceOutput;

    public TunDownlinkWriter(final FileDescriptor deviceDescriptor, final PacketSource downlinkSource) {
        Objects.requireNonNull(deviceDescriptor, "Device descriptor must not be null");
        Objects.requireNonNull(downlinkSource, "Packet source must not be null");
        this.deviceDescriptor = deviceDescriptor;
        this.downlinkSource = downlinkSource;
    }

    @Override
    public void initialize() {
        this.deviceOutput = new FileOutputStream(deviceDescriptor).getChannel();
        LOG.debug("Started VPN device writer");
    }

    @Override
    public boolean loop() {
        final Packet packet = downlinkSource.poll();

        // If the queue is empty, return false to signal that no work was done
        if (packet == null) return false;

        try {
            final ByteBuffer bytes = packet.getBufferView();
            while (bytes.hasRemaining()) {
                final int written = deviceOutput.write(bytes);
                LOG.trace("{} bytes written", written);
            }
        } catch (final IOException e) {
            LOG.error("VPN device write exception", e);
        }

        // TODO: Add packet recycling (w/ buffer releasing) here if implemented
        return true;
    }

    @Override
    public void terminate() {
        IOUtils.safeClose(deviceOutput);
        LOG.debug("Stopped VPN device writer");
    }

}
