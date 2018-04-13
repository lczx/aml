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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.nio.channels.FileChannel;
import java.util.Objects;

public class TunUplinkReader implements TaskRunner.Task {

    private static final Logger LOG = LoggerFactory.getLogger(TunUplinkReader.class);

    private final FileDescriptor deviceDescriptor;
    private final PacketSink uplinkSink;
    private FileChannel deviceInput;

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
    public boolean loop() throws Exception {
        // TODO: Implement
        return false;
    }

    @Override
    public void terminate() {
        IOUtils.closeResources(deviceInput);
        LOG.debug("Stopped VPN device reader");
    }

}
