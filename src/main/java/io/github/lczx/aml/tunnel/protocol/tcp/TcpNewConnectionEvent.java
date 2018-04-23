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

import io.github.lczx.aml.hook.AMLEvent;

public class TcpNewConnectionEvent extends AMLEvent {

    private final Connection connection;
    private final int localPort;

    /* package */ TcpNewConnectionEvent(final Connection connection, final int localPort) {
        this.connection = connection;
        this.localPort = localPort;
    }

    public Connection getConnection() {
        return connection;
    }

    public int getLocalPort() {
        return localPort;
    }

}
