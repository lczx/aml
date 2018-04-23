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

package io.github.lczx.aml.modules.uid;

import io.github.lczx.aml.AMLContext;
import io.github.lczx.aml.hook.AMLEvent;
import io.github.lczx.aml.hook.AMLEventListener;
import io.github.lczx.aml.hook.AMLModule;
import io.github.lczx.aml.hook.AMLTunnelModule;
import io.github.lczx.aml.tunnel.protocol.Link;
import io.github.lczx.aml.tunnel.protocol.tcp.TcpNewConnectionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@AMLModule(name = "UID Provider", priority = 1000)
public class UidModule implements AMLTunnelModule {

    public static final String CONNECTION_EXTRA_UID = "uid";

    private static final Logger LOG = LoggerFactory.getLogger(UidModule.class);
    private static final String PROCFS_FILE_TCP_CONNECTIONS = "/proc/net/tcp";
    private static final int CONN_TABLE_UID_COLUMN = 7;

    @Override
    public void initialize(final AMLContext amlContext) {
        amlContext.getEventDispatcher().addEventListener(new OnConnectListener(), TcpNewConnectionEvent.class);
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onStop() {

    }

    private static class OnConnectListener implements AMLEventListener {

        @Override
        public void receiveEvent(final AMLEvent event) {
            final TcpNewConnectionEvent connEvt = (TcpNewConnectionEvent) event;
            final Link link = connEvt.getConnection().getLink();

            // Define the pattern to search for in /proc/net/tcp
            final String pattern = String.format(":%04X %08X:%04X", link.sourcePort,
                    getLittleEndianAddress((Inet4Address) link.destination.getAddress()), link.destination.getPort());

            // Search for that and get the UID
            try {
                final BufferedReader br = new BufferedReader(new FileReader(PROCFS_FILE_TCP_CONNECTIONS));

                String line;
                while ((line = br.readLine()) != null)
                    if (line.contains(pattern)) break;
                br.close();

                if (line == null)
                    LOG.warn("No UID found for link {}", link);
                else {
                    final String[] data = line.trim().split(" +");
                    final int uid = Integer.parseInt(data[CONN_TABLE_UID_COLUMN]);
                    LOG.debug("Link {} has been established by UID {}", link, uid);
                    connEvt.getConnection().putExtra(CONNECTION_EXTRA_UID, uid);
                }
            } catch (final IOException e) {
                LOG.error("I/O exception while reading socket status file", e);
            } catch (final NumberFormatException e) {
                LOG.error("Malformed UID column (unexpected socket status file format)", e);
            }
        }

        private int getLittleEndianAddress(final Inet4Address address) {
            // Use a ByteBuffer to flip address endianness (Big -> Little endian)
            ByteBuffer b = ByteBuffer.allocate(4);
            b.putInt(address.hashCode()); // <-- hashCode() is an hack to get raw 32bit address
            b.order(ByteOrder.LITTLE_ENDIAN).flip();
            return b.getInt();
        }

    }

}
