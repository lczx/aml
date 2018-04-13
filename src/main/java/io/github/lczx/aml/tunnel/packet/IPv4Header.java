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

import java.net.Inet4Address;

public interface IPv4Header {

    /**
     * Gets this packet's <i>version</i> field (4 bits).
     *
     * <p> This fields is the version of the IP protocol, always equal to 4.
     *
     * @return The field value
     */
    byte getVersion();

    /**
     * Gets this packet's <i>Internet Header Length</i> field (4 bits).
     *
     * <p> This fields is the size of the IP header expressed in number of 32-bit words. For an header without options
     * it is always 5 (20 bytes), however it can change in case of options.
     *
     * @return The field value
     */
    byte getIHL();

    /**
     * Gets this packet's <i>Differentiated Services</i> and <i>Explicit Congestion Notification</i> fields (8 bits).
     *
     * <p> These fields to mark differentiated services (e.g. VoIP data streaming) and end-to-end notification of
     * network congestion without dropping packets (only used when both endpoints support it and are willing to use it).
     *
     * @return The field value
     */
    byte getDiffServicesAndECN();

    /**
     * Gets this packet's <i>total length</i> field (16 bits).
     *
     * <p> This field defines the entire packet size in bytes, including header and data.
     *
     * @return The field value
     */
    int getTotalLength();

    /**
     * Gets this packet's <i>identification</i> field (16 bits).
     *
     * <p> This field is primarily used for uniquely identifying the group of fragments of a single IP datagram.
     *
     * @return The field value
     */
    int getIdentificationField();

    /**
     * Gets this packet's <i>flags</i> field (3 bits).
     *
     * <p> This field allows identification of fragments as "don't fragment" or "more fragments".
     *
     * @return The field value
     */
    byte getFlags();

    /**
     * Gets this packet's <i>fragment offset</i> field (13 bits).
     *
     * <p> This field is measured in units of eight-byte blocks. It specifies the offset of a particular fragment
     * relative to the beginning of the original unfragmented IP datagram.
     *
     * @return The field value
     */
    short getFragmentOffset();

    /**
     * Gets this packet's <i>time to live</i> field (8 bits).
     *
     * <p> This field limits a datagram's lifetime: when the packet arrives at a router, it decrements this field by 1.
     *
     * @return The field value
     */
    short getTTL();

    /**
     * Gets this packet's <i>protocol ID</i> field (8 bits).
     *
     * <p> This field defines the protocol used in the data portion of the IP datagram.
     }
     * @return The field value
     */
    short getProtocolId();

    /**
     * Gets this packet's <i>header checksum</i> field (16 bits).
     *
     * <p> This field is used for error-checking of the header.
     *
     * @return The field value
     */
    short getHeaderChecksum();

    /**
     * Gets this packet's <i>source address</i> (32 bits).
     *
     * <p> This field is the IPv4 address of the sender of the packet. It may be changed in transit by a NAT device.
     *
     * @return The field value
     */
    Inet4Address getSourceAddress();

    /**
     * Gets this packet's <i>destination address</i> (32 bits).
     *
     * <p> This field defines the protocol used in the data portion of the IP datagram.
     *
     * @return The field value
     */
    Inet4Address getDestinationAddress();

    /**
     * Gets this packet's <i>options</i> (variable length, 0-320 bits, divisible by 32).
     *
     * <p> Not often used. Options typically configure a number of behaviors such as for the method to be
     * used during source routing, some control and probing facilities and a number of experimental features.
     *
     * @return The IP header options or {@code null} if this packet has no options
     */
    byte[] getOptions();

}
