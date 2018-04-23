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

import java.net.InetSocketAddress;
import java.util.Objects;

public class Link {

    public final int sourcePort;
    public final InetSocketAddress destination;

    public Link(final int sourcePort, final InetSocketAddress destination) {
        this.sourcePort = sourcePort;
        this.destination = destination;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Link that = (Link) o;
        return sourcePort == that.sourcePort && Objects.equals(destination, that.destination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourcePort, destination);
    }

    @Override
    public String toString() {
        return destination.getAddress().getHostAddress() + ':' + destination.getPort() + ':' + sourcePort;
    }

}
