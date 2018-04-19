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
import io.github.lczx.aml.tunnel.packet.Packets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;

public class TunUplinkReader implements TaskRunner.Task {

    private static final Logger LOG = LoggerFactory.getLogger(TunUplinkReader.class);

    private final FileDescriptor deviceDescriptor;
    private final PacketSink uplinkSink;
    private FileChannel deviceInput;
    private ByteBuffer currentBuffer;

    public TunUplinkReader(final FileDescriptor deviceDescriptor, final PacketSink uplinkSink) {
        Objects.requireNonNull(deviceDescriptor, "Device descriptor must not be null");
        Objects.requireNonNull(uplinkSink, "Packet sink must not be null");
        this.deviceDescriptor = deviceDescriptor;
        this.uplinkSink = uplinkSink;
    }

    @Override
    public void initialize() {
        this.deviceInput = new FileInputStream(deviceDescriptor).getChannel();
        LOG.debug("Started VPN device reader");
    }

    @Override
    public boolean loop() {
        // Avoid allocation of a new buffer if it was not used the last time
        if (currentBuffer == null)
            currentBuffer = Packets.createBuffer();
        else
            currentBuffer.clear();

        // TODO: Find a way to block if not connected
        final int readBytes;
        try {
            readBytes = deviceInput.read(currentBuffer);
        } catch (final IOException e) {
            LOG.error("VPN device read exception", e);
            return false;
        }

        // Return false if no data was read
        if (readBytes <= 0) return false;

        // Flip the buffer and wrap it in a packet
        LOG.trace("{} bytes read", readBytes);
        currentBuffer.flip();
        final Packet packet = Packets.wrapBuffer(Packets.LAYER_NETWORK, currentBuffer);

        // Set current buffer to null (next round will allocate a new one) and push out the packet
        currentBuffer = null;
        uplinkSink.receive(packet);
        return true;
    }

    @Override
    public void terminate() {
        IOUtils.safeClose(deviceInput);
        LOG.debug("Stopped VPN device reader");
    }

}
