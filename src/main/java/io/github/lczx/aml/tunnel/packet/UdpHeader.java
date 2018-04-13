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

package io.github.lczx.aml.tunnel.packet;

public interface UdpHeader {

    /**
     * Gets this datagram's <i>source port</i> (16 bits).
     *
     * <p> This field identifies the sender's port when meaningful and should be assumed to be the port
     * to reply to if needed. If not used, then it should be zero. If the source host is the client,
     * the port number is likely to be an ephemeral port number. If the source host is the server,
     * the port number is likely to be a well-known port number.
     *
     * @return The field value
     */
    int getSourcePort();

    /**
     * Gets this datagram's <i>destination port</i> (16 bits).
     *
     * <p> This field identifies the receiver's port and is required. Similar to source port number,
     * if the client is the destination host then the port number will likely be an ephemeral port number
     * and if the destination host is the server then the port number will likely be a well-known port number.
     *
     * @return The field value
     */
    int getDestinationPort();

    /**
     * Gets this datagram's <i></i> field ( bits).
     *
     * <p> A field that specifies the length in bytes of the UDP header and UDP data.
     *
     * @return The field value
     */
    int getTotalLength();

    /**
     * Gets this datagram's <i></i> field ( bits).
     *
     * <p> The checksum field may be used for error-checking of the header and data.
     * This field is optional in IPv4, and mandatory in IPv6. The field carries all-zeros if unused.
     *
     * @return The field value
     */
    short getChecksum();

}
