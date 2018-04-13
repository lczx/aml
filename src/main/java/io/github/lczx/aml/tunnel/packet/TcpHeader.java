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

public interface TcpHeader {

    /**
     * Gets this packet's <i>source port</i> (16 bits).
     *
     * <p> Identifies the sending port.
     *
     * @return The field value
     */
    int getSourcePort();

    /**
     * Gets this packet's <i>destination port</i> (16 bits).
     *
     * <p> Identifies the receiving port.
     *
     * @return The field value
     */
    int getDestinationPort();

    /**
     * Gets this packet's <i>sequence number</i> field (32 bits).
     *
     * <p> Has a dual role:
     * <ul>
     *     <li>If the {@code SYN} flag is set (1), then this is the initial sequence number.</li>
     *     <li>If the {@code SYN} flag is clear (0), then this is the accumulated sequence number of the first
     *     data byte of this segment for the current session.</li>
     * </ul>
     *
     * @return The field value
     */
    long getSequenceNumber();

    /**
     * Gets this packet's <i>acknowledgement number</i> field (32 bits).
     *
     * <p> If the {@code ACK} flag is set then the value of this field is the next sequence number that
     * the sender of the {@code ACK} is expecting.
     *
     * <p> The first {@code ACK} sent by each end acknowledges the other end's initial sequence number itself, but no data.
     *
     * @return The field value
     */
    long getAcknowledgementNumber();

    /**
     * Gets this packet's <i>data offset</i> field (4 bits).
     *
     * <p> Specifies the size of the TCP header in 32-bit words. The minimum size header is 5 words and the maximum
     * is 15 words thus giving the minimum size of 20 bytes and maximum of 60 bytes, allowing for up to 40 bytes
     * of options in the header.
     *
     * @return The field value
     */
    byte getDataOffset();

    /**
     * Gets this packet's <i>flags</i> field (8 bits).
     *
     * <p> This field contains the segment's control bits, including {@code FIN}, {@code SYN}, {@code RST},
     * {@code PSH} and {@code ACK}.
     *
     * @return The field value
     */
    byte getFlags();

    /**
     * Gets this packet's <i>window size</i> field (16 bits).
     *
     * <p> The size of the receive window, which specifies the number of window size units (by default, bytes)
     * (beyond the segment identified by the sequence number in the acknowledgment field) that the sender of
     * this segment is currently willing to receive.
     *
     * @return The field value
     */
    int getWindowSize();

    /**
     * Gets this packet's <i>checksum</i> field (16 bits).
     *
     * <p> The 16-bit checksum field is used for error-checking of the header, the Payload and a Pseudo-Header.
     *
     * <p> The Pseudo-Header consist of the Source IP Address, the Destination IP Address, the protocol number
     * for the TCP-Protocol (0x0006) and the length of the TCP-Headers including Payload (in Bytes).
     *
     * @return The field value
     */
    short getChecksum();

    /**
     * Gets this packet's <i>urgent pointer</i> field (16 bits).
     *
     * <p> if the {@code URG} flag is set, then this 16-bit field is an offset
     * from the sequence number indicating the last urgent data byte.
     *
     * @return The field value
     */
    int getUrgentPointer();

    /**
     * Gets this segment's <i>options</i> (variable length, 0-320 bits, divisible by 32).
     *
     * <p> Options can define the maximum segment size value, window size units (window scale) and support for
     * selective acknowledgement (SACK).
     *
     * @return The TCP segment options or {@code null} if this segment has no options
     */
    byte[] getOptions();

}
