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

package io.github.lczx.aml.hook.monitoring;

public final class BaseMeasureKeys {

    /**
     * List of probes attached at the moment of measurement
     *
     * <p> Type: String
     */
    public static final String PROBE_NAMES = "probe_names";
    /**
     * Status of the VPN I/O thread
     *
     * <p> Type: int (ordinal of Thread.State)
     */
    public static final String THREAD_STATE_VPN = "thread_state_vpn";
    /**
     * Status of the TCP transmitter thread
     *
     * <p> Type: int (ordinal of Thread.State)
     */
    public static final String THREAD_STATE_TCP_TX = "thread_state_tcp_tx";
    /**
     * Status of the TCP receiver thread
     *
     * <p> Type: int (ordinal of Thread.State)
     */
    public static final String THREAD_STATE_TCP_RX = "thread_state_tcp_rx";
    /**
     * Status of the UDP transmitter thread
     *
     * <p> Type: int (ordinal of Thread.State)
     */
    public static final String THREAD_STATE_UDP_TX = "thread_state_udp_tx";
    /**
     * Status of the UDP receiver thread
     *
     * <p> Type: int (ordinal of Thread.State)
     */
    public static final String THREAD_STATE_UDP_RX = "thread_state_udp_rx";
    /**
     * Number of packets waiting to be written to the VPN interface (received from the network)
     *
     * <p> Type: int
     */
    public static final String QUEUE_SIZE_RX = "queue_size_rx";
    /**
     * Number of TCP packets waiting to be sent on the network (read from the VPN interface and marked as TCP)
     *
     * <p> Type: int
     */
    public static final String QUEUE_SIZE_TX_TCP = "queue_size_tx_tcp";
    /**
     * Number of UDP packets waiting to be sent on the network (read from the VPN interface and marked as UDP)
     *
     * <p> Type: int
     */
    public static final String QUEUE_SIZE_TX_UDP = "queue_size_tx_udp";

    /**
     * The maximum number of entries that can be stored in the UDP datagram socket cache
     *
     * <p> Type: int
     */
    public static final String UDP_SOCK_CACHE_CAPACITY = "udp_sock_cache_capacity";

    /**
     * A string array representation of the entries in the UDP datagram socket cache
     *
     * <p> Type: String[]
     */
    public static final String UDP_SOCK_CACHE_DUMP = "udp_sock_cache_dump";

    /**
     * The maximum number of entries that can be stored in the TCP session registry without dropping
     *
     * <p> Type: int
     */
    public static final String TCP_CONN_CACHE_CAPACITY = "tcp_conn_cache_capacity";

    /**
     * A string array representation of the entries in the TCP session registry
     *
     * <p> Type: String[]
     */
    public static final String TCP_CONN_CACHE_DUMP = "tcp_conn_cache_dump";

    private BaseMeasureKeys() { }

}
