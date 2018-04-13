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

class PacketSamples {

    //Internet Protocol Version 4, Src: 10.0.0.129, Dst: 93.184.216.34
    //    0100 .... = Version: 4
    //    .... 0101 = Header Length: 20 bytes (5)
    //    Differentiated Services Field: 0x00 (DSCP: CS0, ECN: Not-ECT)
    //    Total Length: 52
    //    Identification: 0x3c0a (15370)
    //    Flags: 0x02 (Don't Fragment)
    //    Fragment offset: 0
    //    Time to live: 128
    //    Protocol: TCP (6)
    //    Header checksum: 0x7e5e [validation disabled]
    //    [Header checksum status: Unverified]
    //    Source: 10.0.0.129
    //    Destination: 93.184.216.34
    //    [Source GeoIP: Unknown]
    //    [Destination GeoIP: Unknown]
    //Transmission Control Protocol, Src Port: 6843, Dst Port: 80, Seq: 0, Len: 0
    //    Source Port: 6843
    //    Destination Port: 80
    //    [Stream index: 2]
    //    [TCP Segment Len: 0]
    //    Sequence number: 0    (relative sequence number)
    //    Acknowledgment number: 0
    //    1000 .... = Header Length: 32 bytes (8)
    //    Flags: 0x002 (SYN)
    //    Window size value: 64240
    //    [Calculated window size: 64240]
    //    Checksum: 0xa102 [unverified]
    //    [Checksum Status: Unverified]
    //    Urgent pointer: 0
    //    Options: (12 bytes), Maximum segment size, No-Operation (NOP), Window scale, No-Operation (NOP), No-Operation (NOP), SACK permitted
    //        TCP Option - Maximum segment size: 1460 bytes
    //        TCP Option - No-Operation (NOP)
    //        TCP Option - Window scale: 8 (multiply by 256)
    //        TCP Option - No-Operation (NOP)
    //        TCP Option - No-Operation (NOP)
    //        TCP Option - SACK permitted
    static final byte[] SAMPLE_PACKET_01 = {
            (byte) 0x45, (byte) 0x00, (byte) 0x00, (byte) 0x34, (byte) 0x3C, (byte) 0x0A, (byte) 0x40, (byte) 0x00,
            (byte) 0x80, (byte) 0x06, (byte) 0x7E, (byte) 0x5E, (byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0x81,
            (byte) 0x5D, (byte) 0xB8, (byte) 0xD8, (byte) 0x22, (byte) 0x1A, (byte) 0xBB, (byte) 0x00, (byte) 0x50,
            (byte) 0x1A, (byte) 0xAC, (byte) 0x5D, (byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x80, (byte) 0x02, (byte) 0xFA, (byte) 0xF0, (byte) 0xA1, (byte) 0x02, (byte) 0x00, (byte) 0x00,
            (byte) 0x02, (byte) 0x04, (byte) 0x05, (byte) 0xB4, (byte) 0x01, (byte) 0x03, (byte) 0x03, (byte) 0x08,
            (byte) 0x01, (byte) 0x01, (byte) 0x04, (byte) 0x02,
    };

    //Internet Protocol Version 4, Src: 93.184.216.34, Dst: 10.0.0.129
    //    0100 .... = Version: 4
    //    .... 0101 = Header Length: 20 bytes (5)
    //    Differentiated Services Field: 0x00 (DSCP: CS0, ECN: Not-ECT)
    //    Total Length: 52
    //    Identification: 0x0000 (0)
    //    Flags: 0x02 (Don't Fragment)
    //    Fragment offset: 0
    //    Time to live: 53
    //    Protocol: TCP (6)
    //    Header checksum: 0x0569 [validation disabled]
    //    [Header checksum status: Unverified]
    //    Source: 93.184.216.34
    //    Destination: 10.0.0.129
    //    [Source GeoIP: Unknown]
    //    [Destination GeoIP: Unknown]
    //Transmission Control Protocol, Src Port: 80, Dst Port: 6843, Seq: 0, Ack: 1, Len: 0
    //    Source Port: 80
    //    Destination Port: 6843
    //    [Stream index: 2]
    //    [TCP Segment Len: 0]
    //    Sequence number: 0    (relative sequence number)
    //    Acknowledgment number: 1    (relative ack number)
    //    1000 .... = Header Length: 32 bytes (8)
    //    Flags: 0x012 (SYN, ACK)
    //    Window size value: 65535
    //    [Calculated window size: 65535]
    //    Checksum: 0x624c [unverified]
    //    [Checksum Status: Unverified]
    //    Urgent pointer: 0
    //    Options: (12 bytes), Maximum segment size, No-Operation (NOP), No-Operation (NOP), SACK permitted, No-Operation (NOP), Window scale
    //        TCP Option - Maximum segment size: 1452 bytes
    //        TCP Option - No-Operation (NOP)
    //        TCP Option - No-Operation (NOP)
    //        TCP Option - SACK permitted
    //        TCP Option - No-Operation (NOP)
    //        TCP Option - Window scale: 9 (multiply by 512)
    static final byte[] SAMPLE_PACKET_02 = {
            (byte) 0x45, (byte) 0x00, (byte) 0x00, (byte) 0x34, (byte) 0x00, (byte) 0x00, (byte) 0x40, (byte) 0x00,
            (byte) 0x35, (byte) 0x06, (byte) 0x05, (byte) 0x69, (byte) 0x5D, (byte) 0xB8, (byte) 0xD8, (byte) 0x22,
            (byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0x81, (byte) 0x00, (byte) 0x50, (byte) 0x1A, (byte) 0xBB,
            (byte) 0x65, (byte) 0xC1, (byte) 0xD3, (byte) 0xDB, (byte) 0x1A, (byte) 0xAC, (byte) 0x5D, (byte) 0x0B,
            (byte) 0x80, (byte) 0x12, (byte) 0xFF, (byte) 0xFF, (byte) 0x62, (byte) 0x4C, (byte) 0x00, (byte) 0x00,
            (byte) 0x02, (byte) 0x04, (byte) 0x05, (byte) 0xAC, (byte) 0x01, (byte) 0x01, (byte) 0x04, (byte) 0x02,
            (byte) 0x01, (byte) 0x03, (byte) 0x03, (byte) 0x09,
    };

    //Internet Protocol Version 4, Src: 10.0.0.129, Dst: 93.184.216.34
    //    0100 .... = Version: 4
    //    .... 0101 = Header Length: 20 bytes (5)
    //    Differentiated Services Field: 0x00 (DSCP: CS0, ECN: Not-ECT)
    //    Total Length: 40
    //    Identification: 0x3c0b (15371)
    //    Flags: 0x02 (Don't Fragment)
    //    Fragment offset: 0
    //    Time to live: 128
    //    Protocol: TCP (6)
    //    Header checksum: 0x7e69 [validation disabled]
    //    [Header checksum status: Unverified]
    //    Source: 10.0.0.129
    //    Destination: 93.184.216.34
    //    [Source GeoIP: Unknown]
    //    [Destination GeoIP: Unknown]
    //Transmission Control Protocol, Src Port: 6843, Dst Port: 80, Seq: 1, Ack: 1, Len: 0
    //    Source Port: 6843
    //    Destination Port: 80
    //    [Stream index: 2]
    //    [TCP Segment Len: 0]
    //    Sequence number: 1    (relative sequence number)
    //    Acknowledgment number: 1    (relative ack number)
    //    0101 .... = Header Length: 20 bytes (5)
    //    Flags: 0x010 (ACK)
    //    Window size value: 260
    //    [Calculated window size: 66560]
    //    [Window size scaling factor: 256]
    //    Checksum: 0xa214 [unverified]
    //    [Checksum Status: Unverified]
    //    Urgent pointer: 0
    static final byte[] SAMPLE_PACKET_03 = {
            (byte) 0x45, (byte) 0x00, (byte) 0x00, (byte) 0x28, (byte) 0x3C, (byte) 0x0B, (byte) 0x40, (byte) 0x00,
            (byte) 0x80, (byte) 0x06, (byte) 0x7E, (byte) 0x69, (byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0x81,
            (byte) 0x5D, (byte) 0xB8, (byte) 0xD8, (byte) 0x22, (byte) 0x1A, (byte) 0xBB, (byte) 0x00, (byte) 0x50,
            (byte) 0x1A, (byte) 0xAC, (byte) 0x5D, (byte) 0x0B, (byte) 0x65, (byte) 0xC1, (byte) 0xD3, (byte) 0xDC,
            (byte) 0x50, (byte) 0x10, (byte) 0x01, (byte) 0x04, (byte) 0xA2, (byte) 0x14, (byte) 0x00, (byte) 0x00,
    };

    //Internet Protocol Version 4, Src: 10.0.0.129, Dst: 93.184.216.34
    //    0100 .... = Version: 4
    //    .... 0101 = Header Length: 20 bytes (5)
    //    Differentiated Services Field: 0x00 (DSCP: CS0, ECN: Not-ECT)
    //    Total Length: 115
    //    Identification: 0x3c0c (15372)
    //    Flags: 0x02 (Don't Fragment)
    //    Fragment offset: 0
    //    Time to live: 128
    //    Protocol: TCP (6)
    //    Header checksum: 0x7e1d [validation disabled]
    //    [Header checksum status: Unverified]
    //    Source: 10.0.0.129
    //    Destination: 93.184.216.34
    //    [Source GeoIP: Unknown]
    //    [Destination GeoIP: Unknown]
    //Transmission Control Protocol, Src Port: 6843, Dst Port: 80, Seq: 1, Ack: 1, Len: 75
    //    Source Port: 6843
    //    Destination Port: 80
    //    [Stream index: 2]
    //    [TCP Segment Len: 75]
    //    Sequence number: 1    (relative sequence number)
    //    [Next sequence number: 76    (relative sequence number)]
    //    Acknowledgment number: 1    (relative ack number)
    //    0101 .... = Header Length: 20 bytes (5)
    //    Flags: 0x018 (PSH, ACK)
    //    Window size value: 260
    //    [Calculated window size: 66560]
    //    [Window size scaling factor: 256]
    //    Checksum: 0x00ea [unverified]
    //    [Checksum Status: Unverified]
    //    Urgent pointer: 0
    //    TCP payload (75 bytes)
    //Hypertext Transfer Protocol
    //    GET / HTTP/1.1\r\n
    //    Host: example.com\r\n
    //    User-Agent: curl/7.52.1\r\n
    //    Accept: */*\r\n
    //    \r\n
    static final byte[] SAMPLE_PACKET_04 = {
            (byte) 0x45, (byte) 0x00, (byte) 0x00, (byte) 0x73, (byte) 0x3C, (byte) 0x0C, (byte) 0x40, (byte) 0x00,
            (byte) 0x80, (byte) 0x06, (byte) 0x7E, (byte) 0x1D, (byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0x81,
            (byte) 0x5D, (byte) 0xB8, (byte) 0xD8, (byte) 0x22, (byte) 0x1A, (byte) 0xBB, (byte) 0x00, (byte) 0x50,
            (byte) 0x1A, (byte) 0xAC, (byte) 0x5D, (byte) 0x0B, (byte) 0x65, (byte) 0xC1, (byte) 0xD3, (byte) 0xDC,
            (byte) 0x50, (byte) 0x18, (byte) 0x01, (byte) 0x04, (byte) 0x00, (byte) 0xEA, (byte) 0x00, (byte) 0x00,
            (byte) 0x47, (byte) 0x45, (byte) 0x54, (byte) 0x20, (byte) 0x2F, (byte) 0x20, (byte) 0x48, (byte) 0x54,
            (byte) 0x54, (byte) 0x50, (byte) 0x2F, (byte) 0x31, (byte) 0x2E, (byte) 0x31, (byte) 0x0D, (byte) 0x0A,
            (byte) 0x48, (byte) 0x6F, (byte) 0x73, (byte) 0x74, (byte) 0x3A, (byte) 0x20, (byte) 0x65, (byte) 0x78,
            (byte) 0x61, (byte) 0x6D, (byte) 0x70, (byte) 0x6C, (byte) 0x65, (byte) 0x2E, (byte) 0x63, (byte) 0x6F,
            (byte) 0x6D, (byte) 0x0D, (byte) 0x0A, (byte) 0x55, (byte) 0x73, (byte) 0x65, (byte) 0x72, (byte) 0x2D,
            (byte) 0x41, (byte) 0x67, (byte) 0x65, (byte) 0x6E, (byte) 0x74, (byte) 0x3A, (byte) 0x20, (byte) 0x63,
            (byte) 0x75, (byte) 0x72, (byte) 0x6C, (byte) 0x2F, (byte) 0x37, (byte) 0x2E, (byte) 0x35, (byte) 0x32,
            (byte) 0x2E, (byte) 0x31, (byte) 0x0D, (byte) 0x0A, (byte) 0x41, (byte) 0x63, (byte) 0x63, (byte) 0x65,
            (byte) 0x70, (byte) 0x74, (byte) 0x3A, (byte) 0x20, (byte) 0x2A, (byte) 0x2F, (byte) 0x2A, (byte) 0x0D,
            (byte) 0x0A, (byte) 0x0D, (byte) 0x0A,
    };

    //Internet Protocol Version 4, Src: 93.184.216.34, Dst: 10.0.0.129
    //    0100 .... = Version: 4
    //    .... 0101 = Header Length: 20 bytes (5)
    //    Differentiated Services Field: 0x00 (DSCP: CS0, ECN: Not-ECT)
    //    Total Length: 40
    //    Identification: 0xa116 (41238)
    //    Flags: 0x02 (Don't Fragment)
    //    Fragment offset: 0
    //    Time to live: 53
    //    Protocol: TCP (6)
    //    Header checksum: 0x645e [validation disabled]
    //    [Header checksum status: Unverified]
    //    Source: 93.184.216.34
    //    Destination: 10.0.0.129
    //    [Source GeoIP: Unknown]
    //    [Destination GeoIP: Unknown]
    //Transmission Control Protocol, Src Port: 80, Dst Port: 6843, Seq: 1, Ack: 76, Len: 0
    //    Source Port: 80
    //    Destination Port: 6843
    //    [Stream index: 2]
    //    [TCP Segment Len: 0]
    //    Sequence number: 1    (relative sequence number)
    //    Acknowledgment number: 76    (relative ack number)
    //    0101 .... = Header Length: 20 bytes (5)
    //    Flags: 0x010 (ACK)
    //    Window size value: 286
    //    [Calculated window size: 146432]
    //    [Window size scaling factor: 512]
    //    Checksum: 0xa1af [unverified]
    //    [Checksum Status: Unverified]
    //    Urgent pointer: 0
    static final byte[] SAMPLE_PACKET_05 = {
            (byte) 0x45, (byte) 0x00, (byte) 0x00, (byte) 0x28, (byte) 0xA1, (byte) 0x16, (byte) 0x40, (byte) 0x00,
            (byte) 0x35, (byte) 0x06, (byte) 0x64, (byte) 0x5E, (byte) 0x5D, (byte) 0xB8, (byte) 0xD8, (byte) 0x22,
            (byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0x81, (byte) 0x00, (byte) 0x50, (byte) 0x1A, (byte) 0xBB,
            (byte) 0x65, (byte) 0xC1, (byte) 0xD3, (byte) 0xDC, (byte) 0x1A, (byte) 0xAC, (byte) 0x5D, (byte) 0x56,
            (byte) 0x50, (byte) 0x10, (byte) 0x01, (byte) 0x1E, (byte) 0xA1, (byte) 0xAF, (byte) 0x00, (byte) 0x00,
    //        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    };

    //Frame 49: 1506 bytes on wire (12048 bits), 1506 bytes captured (12048 bits) on interface 0
    //Ethernet II, Src: BelkinIn_32:88:5c (14:91:82:32:88:5c), Dst: Micro-St_42:e0:31 (4c:cc:6a:42:e0:31)
    //Internet Protocol Version 4, Src: 93.184.216.34, Dst: 10.0.0.129
    //    0100 .... = Version: 4
    //    .... 0101 = Header Length: 20 bytes (5)
    //    Differentiated Services Field: 0x00 (DSCP: CS0, ECN: Not-ECT)
    //    Total Length: 1492
    //    Identification: 0xa117 (41239)
    //    Flags: 0x02 (Don't Fragment)
    //    Fragment offset: 0
    //    Time to live: 53
    //    Protocol: TCP (6)
    //    Header checksum: 0x5eb1 [validation disabled]
    //    [Header checksum status: Unverified]
    //    Source: 93.184.216.34
    //    Destination: 10.0.0.129
    //    [Source GeoIP: Unknown]
    //    [Destination GeoIP: Unknown]
    //Transmission Control Protocol, Src Port: 80, Dst Port: 6843, Seq: 1, Ack: 76, Len: 1452
    //    Source Port: 80
    //    Destination Port: 6843
    //    [Stream index: 2]
    //    [TCP Segment Len: 1452]
    //    Sequence number: 1    (relative sequence number)
    //    [Next sequence number: 1453    (relative sequence number)]
    //    Acknowledgment number: 76    (relative ack number)
    //    0101 .... = Header Length: 20 bytes (5)
    //    Flags: 0x010 (ACK)
    //    Window size value: 286
    //    [Calculated window size: 146432]
    //    [Window size scaling factor: 512]
    //    Checksum: 0xfb38 [unverified]
    //    [Checksum Status: Unverified]
    //    Urgent pointer: 0
    //    TCP payload (1452 bytes)
    static final byte[] SAMPLE_PACKET_06 = {
            (byte) 0x45, (byte) 0x00, (byte) 0x05, (byte) 0xD4, (byte) 0xA1, (byte) 0x17, (byte) 0x40, (byte) 0x00,
            (byte) 0x35, (byte) 0x06, (byte) 0x5E, (byte) 0xB1, (byte) 0x5D, (byte) 0xB8, (byte) 0xD8, (byte) 0x22,
            (byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0x81, (byte) 0x00, (byte) 0x50, (byte) 0x1A, (byte) 0xBB,
            (byte) 0x65, (byte) 0xC1, (byte) 0xD3, (byte) 0xDC, (byte) 0x1A, (byte) 0xAC, (byte) 0x5D, (byte) 0x56,
            (byte) 0x50, (byte) 0x10, (byte) 0x01, (byte) 0x1E, (byte) 0xFB, (byte) 0x38, (byte) 0x00, (byte) 0x00,
            (byte) 0x48, (byte) 0x54, (byte) 0x54, (byte) 0x50, (byte) 0x2F, (byte) 0x31, (byte) 0x2E, (byte) 0x31,
            (byte) 0x20, (byte) 0x32, (byte) 0x30, (byte) 0x30, (byte) 0x20, (byte) 0x4F, (byte) 0x4B, (byte) 0x0D,
            (byte) 0x0A, (byte) 0x43, (byte) 0x61, (byte) 0x63, (byte) 0x68, (byte) 0x65, (byte) 0x2D, (byte) 0x43,
            (byte) 0x6F, (byte) 0x6E, (byte) 0x74, (byte) 0x72, (byte) 0x6F, (byte) 0x6C, (byte) 0x3A, (byte) 0x20,
            (byte) 0x6D, (byte) 0x61, (byte) 0x78, (byte) 0x2D, (byte) 0x61, (byte) 0x67, (byte) 0x65, (byte) 0x3D,
            (byte) 0x36, (byte) 0x30, (byte) 0x34, (byte) 0x38, (byte) 0x30, (byte) 0x30, (byte) 0x0D, (byte) 0x0A,
            (byte) 0x43, (byte) 0x6F, (byte) 0x6E, (byte) 0x74, (byte) 0x65, (byte) 0x6E, (byte) 0x74, (byte) 0x2D,
            (byte) 0x54, (byte) 0x79, (byte) 0x70, (byte) 0x65, (byte) 0x3A, (byte) 0x20, (byte) 0x74, (byte) 0x65,
            (byte) 0x78, (byte) 0x74, (byte) 0x2F, (byte) 0x68, (byte) 0x74, (byte) 0x6D, (byte) 0x6C, (byte) 0x0D,
            (byte) 0x0A, (byte) 0x44, (byte) 0x61, (byte) 0x74, (byte) 0x65, (byte) 0x3A, (byte) 0x20, (byte) 0x57,
            (byte) 0x65, (byte) 0x64, (byte) 0x2C, (byte) 0x20, (byte) 0x32, (byte) 0x38, (byte) 0x20, (byte) 0x46,
            (byte) 0x65, (byte) 0x62, (byte) 0x20, (byte) 0x32, (byte) 0x30, (byte) 0x31, (byte) 0x38, (byte) 0x20,
            (byte) 0x31, (byte) 0x30, (byte) 0x3A, (byte) 0x31, (byte) 0x39, (byte) 0x3A, (byte) 0x32, (byte) 0x35,
            (byte) 0x20, (byte) 0x47, (byte) 0x4D, (byte) 0x54, (byte) 0x0D, (byte) 0x0A, (byte) 0x45, (byte) 0x74,
            (byte) 0x61, (byte) 0x67, (byte) 0x3A, (byte) 0x20, (byte) 0x22, (byte) 0x31, (byte) 0x35, (byte) 0x34,
            (byte) 0x31, (byte) 0x30, (byte) 0x32, (byte) 0x35, (byte) 0x36, (byte) 0x36, (byte) 0x33, (byte) 0x2B,
            (byte) 0x67, (byte) 0x7A, (byte) 0x69, (byte) 0x70, (byte) 0x2B, (byte) 0x69, (byte) 0x64, (byte) 0x65,
            (byte) 0x6E, (byte) 0x74, (byte) 0x22, (byte) 0x0D, (byte) 0x0A, (byte) 0x45, (byte) 0x78, (byte) 0x70,
            (byte) 0x69, (byte) 0x72, (byte) 0x65, (byte) 0x73, (byte) 0x3A, (byte) 0x20, (byte) 0x57, (byte) 0x65,
            (byte) 0x64, (byte) 0x2C, (byte) 0x20, (byte) 0x30, (byte) 0x37, (byte) 0x20, (byte) 0x4D, (byte) 0x61,
            (byte) 0x72, (byte) 0x20, (byte) 0x32, (byte) 0x30, (byte) 0x31, (byte) 0x38, (byte) 0x20, (byte) 0x31,
            (byte) 0x30, (byte) 0x3A, (byte) 0x31, (byte) 0x39, (byte) 0x3A, (byte) 0x32, (byte) 0x35, (byte) 0x20,
            (byte) 0x47, (byte) 0x4D, (byte) 0x54, (byte) 0x0D, (byte) 0x0A, (byte) 0x4C, (byte) 0x61, (byte) 0x73,
            (byte) 0x74, (byte) 0x2D, (byte) 0x4D, (byte) 0x6F, (byte) 0x64, (byte) 0x69, (byte) 0x66, (byte) 0x69,
            (byte) 0x65, (byte) 0x64, (byte) 0x3A, (byte) 0x20, (byte) 0x46, (byte) 0x72, (byte) 0x69, (byte) 0x2C,
            (byte) 0x20, (byte) 0x30, (byte) 0x39, (byte) 0x20, (byte) 0x41, (byte) 0x75, (byte) 0x67, (byte) 0x20,
            (byte) 0x32, (byte) 0x30, (byte) 0x31, (byte) 0x33, (byte) 0x20, (byte) 0x32, (byte) 0x33, (byte) 0x3A,
            (byte) 0x35, (byte) 0x34, (byte) 0x3A, (byte) 0x33, (byte) 0x35, (byte) 0x20, (byte) 0x47, (byte) 0x4D,
            (byte) 0x54, (byte) 0x0D, (byte) 0x0A, (byte) 0x53, (byte) 0x65, (byte) 0x72, (byte) 0x76, (byte) 0x65,
            (byte) 0x72, (byte) 0x3A, (byte) 0x20, (byte) 0x45, (byte) 0x43, (byte) 0x53, (byte) 0x20, (byte) 0x28,
            (byte) 0x64, (byte) 0x63, (byte) 0x61, (byte) 0x2F, (byte) 0x32, (byte) 0x34, (byte) 0x39, (byte) 0x46,
            (byte) 0x29, (byte) 0x0D, (byte) 0x0A, (byte) 0x56, (byte) 0x61, (byte) 0x72, (byte) 0x79, (byte) 0x3A,
            (byte) 0x20, (byte) 0x41, (byte) 0x63, (byte) 0x63, (byte) 0x65, (byte) 0x70, (byte) 0x74, (byte) 0x2D,
            (byte) 0x45, (byte) 0x6E, (byte) 0x63, (byte) 0x6F, (byte) 0x64, (byte) 0x69, (byte) 0x6E, (byte) 0x67,
            (byte) 0x0D, (byte) 0x0A, (byte) 0x58, (byte) 0x2D, (byte) 0x43, (byte) 0x61, (byte) 0x63, (byte) 0x68,
            (byte) 0x65, (byte) 0x3A, (byte) 0x20, (byte) 0x48, (byte) 0x49, (byte) 0x54, (byte) 0x0D, (byte) 0x0A,
            (byte) 0x43, (byte) 0x6F, (byte) 0x6E, (byte) 0x74, (byte) 0x65, (byte) 0x6E, (byte) 0x74, (byte) 0x2D,
            (byte) 0x4C, (byte) 0x65, (byte) 0x6E, (byte) 0x67, (byte) 0x74, (byte) 0x68, (byte) 0x3A, (byte) 0x20,
            (byte) 0x31, (byte) 0x32, (byte) 0x37, (byte) 0x30, (byte) 0x0D, (byte) 0x0A, (byte) 0x0D, (byte) 0x0A,
            (byte) 0x3C, (byte) 0x21, (byte) 0x64, (byte) 0x6F, (byte) 0x63, (byte) 0x74, (byte) 0x79, (byte) 0x70,
            (byte) 0x65, (byte) 0x20, (byte) 0x68, (byte) 0x74, (byte) 0x6D, (byte) 0x6C, (byte) 0x3E, (byte) 0x0A,
            (byte) 0x3C, (byte) 0x68, (byte) 0x74, (byte) 0x6D, (byte) 0x6C, (byte) 0x3E, (byte) 0x0A, (byte) 0x3C,
            (byte) 0x68, (byte) 0x65, (byte) 0x61, (byte) 0x64, (byte) 0x3E, (byte) 0x0A, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x3C, (byte) 0x74, (byte) 0x69, (byte) 0x74, (byte) 0x6C, (byte) 0x65,
            (byte) 0x3E, (byte) 0x45, (byte) 0x78, (byte) 0x61, (byte) 0x6D, (byte) 0x70, (byte) 0x6C, (byte) 0x65,
            (byte) 0x20, (byte) 0x44, (byte) 0x6F, (byte) 0x6D, (byte) 0x61, (byte) 0x69, (byte) 0x6E, (byte) 0x3C,
            (byte) 0x2F, (byte) 0x74, (byte) 0x69, (byte) 0x74, (byte) 0x6C, (byte) 0x65, (byte) 0x3E, (byte) 0x0A,
            (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x3C, (byte) 0x6D, (byte) 0x65,
            (byte) 0x74, (byte) 0x61, (byte) 0x20, (byte) 0x63, (byte) 0x68, (byte) 0x61, (byte) 0x72, (byte) 0x73,
            (byte) 0x65, (byte) 0x74, (byte) 0x3D, (byte) 0x22, (byte) 0x75, (byte) 0x74, (byte) 0x66, (byte) 0x2D,
            (byte) 0x38, (byte) 0x22, (byte) 0x20, (byte) 0x2F, (byte) 0x3E, (byte) 0x0A, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x3C, (byte) 0x6D, (byte) 0x65, (byte) 0x74, (byte) 0x61, (byte) 0x20,
            (byte) 0x68, (byte) 0x74, (byte) 0x74, (byte) 0x70, (byte) 0x2D, (byte) 0x65, (byte) 0x71, (byte) 0x75,
            (byte) 0x69, (byte) 0x76, (byte) 0x3D, (byte) 0x22, (byte) 0x43, (byte) 0x6F, (byte) 0x6E, (byte) 0x74,
            (byte) 0x65, (byte) 0x6E, (byte) 0x74, (byte) 0x2D, (byte) 0x74, (byte) 0x79, (byte) 0x70, (byte) 0x65,
            (byte) 0x22, (byte) 0x20, (byte) 0x63, (byte) 0x6F, (byte) 0x6E, (byte) 0x74, (byte) 0x65, (byte) 0x6E,
            (byte) 0x74, (byte) 0x3D, (byte) 0x22, (byte) 0x74, (byte) 0x65, (byte) 0x78, (byte) 0x74, (byte) 0x2F,
            (byte) 0x68, (byte) 0x74, (byte) 0x6D, (byte) 0x6C, (byte) 0x3B, (byte) 0x20, (byte) 0x63, (byte) 0x68,
            (byte) 0x61, (byte) 0x72, (byte) 0x73, (byte) 0x65, (byte) 0x74, (byte) 0x3D, (byte) 0x75, (byte) 0x74,
            (byte) 0x66, (byte) 0x2D, (byte) 0x38, (byte) 0x22, (byte) 0x20, (byte) 0x2F, (byte) 0x3E, (byte) 0x0A,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x3C, (byte) 0x6D, (byte) 0x65, (byte) 0x74,
            (byte) 0x61, (byte) 0x20, (byte) 0x6E, (byte) 0x61, (byte) 0x6D, (byte) 0x65, (byte) 0x3D, (byte) 0x22,
            (byte) 0x76, (byte) 0x69, (byte) 0x65, (byte) 0x77, (byte) 0x70, (byte) 0x6F, (byte) 0x72, (byte) 0x74,
            (byte) 0x22, (byte) 0x20, (byte) 0x63, (byte) 0x6F, (byte) 0x6E, (byte) 0x74, (byte) 0x65, (byte) 0x6E,
            (byte) 0x74, (byte) 0x3D, (byte) 0x22, (byte) 0x77, (byte) 0x69, (byte) 0x64, (byte) 0x74, (byte) 0x68,
            (byte) 0x3D, (byte) 0x64, (byte) 0x65, (byte) 0x76, (byte) 0x69, (byte) 0x63, (byte) 0x65, (byte) 0x2D,
            (byte) 0x77, (byte) 0x69, (byte) 0x64, (byte) 0x74, (byte) 0x68, (byte) 0x2C, (byte) 0x20, (byte) 0x69,
            (byte) 0x6E, (byte) 0x69, (byte) 0x74, (byte) 0x69, (byte) 0x61, (byte) 0x6C, (byte) 0x2D, (byte) 0x73,
            (byte) 0x63, (byte) 0x61, (byte) 0x6C, (byte) 0x65, (byte) 0x3D, (byte) 0x31, (byte) 0x22, (byte) 0x20,
            (byte) 0x2F, (byte) 0x3E, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x3C,
            (byte) 0x73, (byte) 0x74, (byte) 0x79, (byte) 0x6C, (byte) 0x65, (byte) 0x20, (byte) 0x74, (byte) 0x79,
            (byte) 0x70, (byte) 0x65, (byte) 0x3D, (byte) 0x22, (byte) 0x74, (byte) 0x65, (byte) 0x78, (byte) 0x74,
            (byte) 0x2F, (byte) 0x63, (byte) 0x73, (byte) 0x73, (byte) 0x22, (byte) 0x3E, (byte) 0x0A, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x62, (byte) 0x6F, (byte) 0x64, (byte) 0x79, (byte) 0x20,
            (byte) 0x7B, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x62, (byte) 0x61, (byte) 0x63, (byte) 0x6B, (byte) 0x67, (byte) 0x72,
            (byte) 0x6F, (byte) 0x75, (byte) 0x6E, (byte) 0x64, (byte) 0x2D, (byte) 0x63, (byte) 0x6F, (byte) 0x6C,
            (byte) 0x6F, (byte) 0x72, (byte) 0x3A, (byte) 0x20, (byte) 0x23, (byte) 0x66, (byte) 0x30, (byte) 0x66,
            (byte) 0x30, (byte) 0x66, (byte) 0x32, (byte) 0x3B, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x6D, (byte) 0x61, (byte) 0x72,
            (byte) 0x67, (byte) 0x69, (byte) 0x6E, (byte) 0x3A, (byte) 0x20, (byte) 0x30, (byte) 0x3B, (byte) 0x0A,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x70, (byte) 0x61, (byte) 0x64, (byte) 0x64, (byte) 0x69, (byte) 0x6E, (byte) 0x67, (byte) 0x3A,
            (byte) 0x20, (byte) 0x30, (byte) 0x3B, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x66, (byte) 0x6F, (byte) 0x6E, (byte) 0x74,
            (byte) 0x2D, (byte) 0x66, (byte) 0x61, (byte) 0x6D, (byte) 0x69, (byte) 0x6C, (byte) 0x79, (byte) 0x3A,
            (byte) 0x20, (byte) 0x22, (byte) 0x4F, (byte) 0x70, (byte) 0x65, (byte) 0x6E, (byte) 0x20, (byte) 0x53,
            (byte) 0x61, (byte) 0x6E, (byte) 0x73, (byte) 0x22, (byte) 0x2C, (byte) 0x20, (byte) 0x22, (byte) 0x48,
            (byte) 0x65, (byte) 0x6C, (byte) 0x76, (byte) 0x65, (byte) 0x74, (byte) 0x69, (byte) 0x63, (byte) 0x61,
            (byte) 0x20, (byte) 0x4E, (byte) 0x65, (byte) 0x75, (byte) 0x65, (byte) 0x22, (byte) 0x2C, (byte) 0x20,
            (byte) 0x48, (byte) 0x65, (byte) 0x6C, (byte) 0x76, (byte) 0x65, (byte) 0x74, (byte) 0x69, (byte) 0x63,
            (byte) 0x61, (byte) 0x2C, (byte) 0x20, (byte) 0x41, (byte) 0x72, (byte) 0x69, (byte) 0x61, (byte) 0x6C,
            (byte) 0x2C, (byte) 0x20, (byte) 0x73, (byte) 0x61, (byte) 0x6E, (byte) 0x73, (byte) 0x2D, (byte) 0x73,
            (byte) 0x65, (byte) 0x72, (byte) 0x69, (byte) 0x66, (byte) 0x3B, (byte) 0x0A, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x0A, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x7D, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x64, (byte) 0x69, (byte) 0x76, (byte) 0x20, (byte) 0x7B, (byte) 0x0A, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x77,
            (byte) 0x69, (byte) 0x64, (byte) 0x74, (byte) 0x68, (byte) 0x3A, (byte) 0x20, (byte) 0x36, (byte) 0x30,
            (byte) 0x30, (byte) 0x70, (byte) 0x78, (byte) 0x3B, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x6D, (byte) 0x61, (byte) 0x72,
            (byte) 0x67, (byte) 0x69, (byte) 0x6E, (byte) 0x3A, (byte) 0x20, (byte) 0x35, (byte) 0x65, (byte) 0x6D,
            (byte) 0x20, (byte) 0x61, (byte) 0x75, (byte) 0x74, (byte) 0x6F, (byte) 0x3B, (byte) 0x0A, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x70,
            (byte) 0x61, (byte) 0x64, (byte) 0x64, (byte) 0x69, (byte) 0x6E, (byte) 0x67, (byte) 0x3A, (byte) 0x20,
            (byte) 0x35, (byte) 0x30, (byte) 0x70, (byte) 0x78, (byte) 0x3B, (byte) 0x0A, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x62, (byte) 0x61,
            (byte) 0x63, (byte) 0x6B, (byte) 0x67, (byte) 0x72, (byte) 0x6F, (byte) 0x75, (byte) 0x6E, (byte) 0x64,
            (byte) 0x2D, (byte) 0x63, (byte) 0x6F, (byte) 0x6C, (byte) 0x6F, (byte) 0x72, (byte) 0x3A, (byte) 0x20,
            (byte) 0x23, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x3B, (byte) 0x0A, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x62, (byte) 0x6F,
            (byte) 0x72, (byte) 0x64, (byte) 0x65, (byte) 0x72, (byte) 0x2D, (byte) 0x72, (byte) 0x61, (byte) 0x64,
            (byte) 0x69, (byte) 0x75, (byte) 0x73, (byte) 0x3A, (byte) 0x20, (byte) 0x31, (byte) 0x65, (byte) 0x6D,
            (byte) 0x3B, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x7D, (byte) 0x0A,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x61, (byte) 0x3A, (byte) 0x6C, (byte) 0x69,
            (byte) 0x6E, (byte) 0x6B, (byte) 0x2C, (byte) 0x20, (byte) 0x61, (byte) 0x3A, (byte) 0x76, (byte) 0x69,
            (byte) 0x73, (byte) 0x69, (byte) 0x74, (byte) 0x65, (byte) 0x64, (byte) 0x20, (byte) 0x7B, (byte) 0x0A,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x63, (byte) 0x6F, (byte) 0x6C, (byte) 0x6F, (byte) 0x72, (byte) 0x3A, (byte) 0x20, (byte) 0x23,
            (byte) 0x33, (byte) 0x38, (byte) 0x34, (byte) 0x38, (byte) 0x38, (byte) 0x66, (byte) 0x3B, (byte) 0x0A,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x74, (byte) 0x65, (byte) 0x78, (byte) 0x74, (byte) 0x2D, (byte) 0x64, (byte) 0x65, (byte) 0x63,
            (byte) 0x6F, (byte) 0x72, (byte) 0x61, (byte) 0x74, (byte) 0x69, (byte) 0x6F, (byte) 0x6E, (byte) 0x3A,
            (byte) 0x20, (byte) 0x6E, (byte) 0x6F, (byte) 0x6E, (byte) 0x65, (byte) 0x3B, (byte) 0x0A, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x7D, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x40, (byte) 0x6D, (byte) 0x65, (byte) 0x64, (byte) 0x69, (byte) 0x61, (byte) 0x20,
            (byte) 0x28, (byte) 0x6D, (byte) 0x61, (byte) 0x78, (byte) 0x2D, (byte) 0x77, (byte) 0x69, (byte) 0x64,
            (byte) 0x74, (byte) 0x68, (byte) 0x3A, (byte) 0x20, (byte) 0x37, (byte) 0x30, (byte) 0x30, (byte) 0x70,
            (byte) 0x78, (byte) 0x29, (byte) 0x20, (byte) 0x7B, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x62, (byte) 0x6F, (byte) 0x64,
            (byte) 0x79, (byte) 0x20, (byte) 0x7B, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x62, (byte) 0x61, (byte) 0x63, (byte) 0x6B, (byte) 0x67, (byte) 0x72, (byte) 0x6F, (byte) 0x75,
            (byte) 0x6E, (byte) 0x64, (byte) 0x2D, (byte) 0x63, (byte) 0x6F, (byte) 0x6C, (byte) 0x6F, (byte) 0x72,
            (byte) 0x3A, (byte) 0x20, (byte) 0x23, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x3B, (byte) 0x0A,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x7D, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x64, (byte) 0x69, (byte) 0x76, (byte) 0x20, (byte) 0x7B, (byte) 0x0A,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x77, (byte) 0x69, (byte) 0x64, (byte) 0x74,
            (byte) 0x68, (byte) 0x3A, (byte) 0x20, (byte) 0x61, (byte) 0x75, (byte) 0x74, (byte) 0x6F, (byte) 0x3B,
            (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x6D, (byte) 0x61, (byte) 0x72,
            (byte) 0x67, (byte) 0x69, (byte) 0x6E, (byte) 0x3A, (byte) 0x20, (byte) 0x30, (byte) 0x20, (byte) 0x61,
            (byte) 0x75, (byte) 0x74, (byte) 0x6F, (byte) 0x3B, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x62, (byte) 0x6F, (byte) 0x72, (byte) 0x64, (byte) 0x65, (byte) 0x72, (byte) 0x2D,
            (byte) 0x72, (byte) 0x61, (byte) 0x64, (byte) 0x69, (byte) 0x75, (byte) 0x73, (byte) 0x3A, (byte) 0x20,
            (byte) 0x30, (byte) 0x3B, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x70,
            (byte) 0x61, (byte) 0x64, (byte) 0x64, (byte) 0x69, (byte) 0x6E, (byte) 0x67, (byte) 0x3A, (byte) 0x20,
            (byte) 0x31, (byte) 0x65, (byte) 0x6D, (byte) 0x3B, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x7D, (byte) 0x0A, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x7D, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x3C, (byte) 0x2F, (byte) 0x73, (byte) 0x74, (byte) 0x79, (byte) 0x6C, (byte) 0x65,
            (byte) 0x3E, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x0A, (byte) 0x3C, (byte) 0x2F,
            (byte) 0x68, (byte) 0x65, (byte) 0x61, (byte) 0x64, (byte) 0x3E, (byte) 0x0A, (byte) 0x0A, (byte) 0x3C,
            (byte) 0x62, (byte) 0x6F, (byte) 0x64, (byte) 0x79, (byte) 0x3E, (byte) 0x0A, (byte) 0x3C, (byte) 0x64,
            (byte) 0x69, (byte) 0x76, (byte) 0x3E, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
            (byte) 0x3C, (byte) 0x68, (byte) 0x31, (byte) 0x3E, (byte) 0x45, (byte) 0x78, (byte) 0x61, (byte) 0x6D,
            (byte) 0x70, (byte) 0x6C, (byte) 0x65, (byte) 0x20, (byte) 0x44, (byte) 0x6F, (byte) 0x6D, (byte) 0x61,
            (byte) 0x69, (byte) 0x6E, (byte) 0x3C, (byte) 0x2F, (byte) 0x68, (byte) 0x31, (byte) 0x3E, (byte) 0x0A,
            (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x3C, (byte) 0x70, (byte) 0x3E, (byte) 0x54,
            (byte) 0x68, (byte) 0x69, (byte) 0x73, (byte) 0x20, (byte) 0x64, (byte) 0x6F, (byte) 0x6D, (byte) 0x61,
            (byte) 0x69, (byte) 0x6E, (byte) 0x20, (byte) 0x69, (byte) 0x73, (byte) 0x20, (byte) 0x65, (byte) 0x73,
            (byte) 0x74, (byte) 0x61, (byte) 0x62, (byte) 0x6C, (byte) 0x69, (byte) 0x73, (byte) 0x68, (byte) 0x65,
            (byte) 0x64, (byte) 0x20, (byte) 0x74, (byte) 0x6F, (byte) 0x20, (byte) 0x62, (byte) 0x65, (byte) 0x20,
            (byte) 0x75, (byte) 0x73, (byte) 0x65, (byte) 0x64, (byte) 0x20, (byte) 0x66, (byte) 0x6F, (byte) 0x72,
            (byte) 0x20, (byte) 0x69, (byte) 0x6C, (byte) 0x6C, (byte) 0x75, (byte) 0x73, (byte) 0x74, (byte) 0x72,
            (byte) 0x61, (byte) 0x74, (byte) 0x69, (byte) 0x76, (byte) 0x65, (byte) 0x20, (byte) 0x65, (byte) 0x78,
            (byte) 0x61, (byte) 0x6D, (byte) 0x70, (byte) 0x6C, (byte) 0x65, (byte) 0x73, (byte) 0x20, (byte) 0x69,
            (byte) 0x6E, (byte) 0x20, (byte) 0x64, (byte) 0x6F, (byte) 0x63, (byte) 0x75, (byte) 0x6D, (byte) 0x65,
            (byte) 0x6E, (byte) 0x74, (byte) 0x73, (byte) 0x2E, (byte) 0x20, (byte) 0x59, (byte) 0x6F, (byte) 0x75,
            (byte) 0x20, (byte) 0x6D, (byte) 0x61, (byte) 0x79, (byte) 0x20, (byte) 0x75, (byte) 0x73, (byte) 0x65,
            (byte) 0x20, (byte) 0x74, (byte) 0x68, (byte) 0x69, (byte) 0x73, (byte) 0x0A, (byte) 0x20, (byte) 0x20,
            (byte) 0x20, (byte) 0x20, (byte) 0x64, (byte) 0x6F, (byte) 0x6D, (byte) 0x61, (byte) 0x69, (byte) 0x6E,
            (byte) 0x20, (byte) 0x69, (byte) 0x6E, (byte) 0x20, (byte) 0x65, (byte) 0x78, (byte) 0x61, (byte) 0x6D,
            (byte) 0x70, (byte) 0x6C, (byte) 0x65, (byte) 0x73, (byte) 0x20, (byte) 0x77, (byte) 0x69, (byte) 0x74,
            (byte) 0x68, (byte) 0x6F, (byte) 0x75, (byte) 0x74, (byte) 0x20, (byte) 0x70, (byte) 0x72, (byte) 0x69,
            (byte) 0x6F, (byte) 0x72, (byte) 0x20, (byte) 0x63, (byte) 0x6F, (byte) 0x6F, (byte) 0x72, (byte) 0x64,
            (byte) 0x69, (byte) 0x6E, (byte) 0x61, (byte) 0x74, (byte) 0x69, (byte) 0x6F, (byte) 0x6E, (byte) 0x20,
            (byte) 0x6F, (byte) 0x72, (byte) 0x20, (byte) 0x61,
    };

    //Internet Protocol Version 4, Src: 93.184.216.34, Dst: 10.0.0.129
    //    0100 .... = Version: 4
    //    .... 0101 = Header Length: 20 bytes (5)
    //    Differentiated Services Field: 0x00 (DSCP: CS0, ECN: Not-ECT)
    //    Total Length: 170
    //    Identification: 0xa118 (41240)
    //    Flags: 0x02 (Don't Fragment)
    //    Fragment offset: 0
    //    Time to live: 53
    //    Protocol: TCP (6)
    //    Header checksum: 0x63da [validation disabled]
    //    [Header checksum status: Unverified]
    //    Source: 93.184.216.34
    //    Destination: 10.0.0.129
    //    [Source GeoIP: Unknown]
    //    [Destination GeoIP: Unknown]
    //Transmission Control Protocol, Src Port: 80, Dst Port: 6843, Seq: 1453, Ack: 76, Len: 130
    //    Source Port: 80
    //    Destination Port: 6843
    //    [Stream index: 2]
    //    [TCP Segment Len: 130]
    //    Sequence number: 1453    (relative sequence number)
    //    [Next sequence number: 1583    (relative sequence number)]
    //    Acknowledgment number: 76    (relative ack number)
    //    0101 .... = Header Length: 20 bytes (5)
    //    Flags: 0x018 (PSH, ACK)
    //    Window size value: 286
    //    [Calculated window size: 146432]
    //    [Window size scaling factor: 512]
    //    Checksum: 0x43e0 [unverified]
    //    [Checksum Status: Unverified]
    //    Urgent pointer: 0
    //    TCP payload (130 bytes)
    //    TCP segment data (130 bytes)
    //[2 Reassembled TCP Segments (1582 bytes): #49(1452), #50(130)]
    //Hypertext Transfer Protocol
    //    HTTP/1.1 200 OK\r\n
    //    Cache-Control: max-age=604800\r\n
    //    Content-Type: text/html\r\n
    //    Date: Wed, 28 Feb 2018 10:19:25 GMT\r\n
    //    Etag: "1541025663+gzip+ident"\r\n
    //    Expires: Wed, 07 Mar 2018 10:19:25 GMT\r\n
    //    Last-Modified: Fri, 09 Aug 2013 23:54:35 GMT\r\n
    //    Server: ECS (dca/249F)\r\n
    //    Vary: Accept-Encoding\r\n
    //    X-Cache: HIT\r\n
    //    Content-Length: 1270\r\n
    //    \r\n
    //    File Data: 1270 bytes
    static final byte[] SAMPLE_PACKET_07 = {
            (byte) 0x45, (byte) 0x00, (byte) 0x00, (byte) 0xAA, (byte) 0xA1, (byte) 0x18, (byte) 0x40, (byte) 0x00,
            (byte) 0x35, (byte) 0x06, (byte) 0x63, (byte) 0xDA, (byte) 0x5D, (byte) 0xB8, (byte) 0xD8, (byte) 0x22,
            (byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0x81, (byte) 0x00, (byte) 0x50, (byte) 0x1A, (byte) 0xBB,
            (byte) 0x65, (byte) 0xC1, (byte) 0xD9, (byte) 0x88, (byte) 0x1A, (byte) 0xAC, (byte) 0x5D, (byte) 0x56,
            (byte) 0x50, (byte) 0x18, (byte) 0x01, (byte) 0x1E, (byte) 0x43, (byte) 0xE0, (byte) 0x00, (byte) 0x00,
            (byte) 0x73, (byte) 0x6B, (byte) 0x69, (byte) 0x6E, (byte) 0x67, (byte) 0x20, (byte) 0x66, (byte) 0x6F,
            (byte) 0x72, (byte) 0x20, (byte) 0x70, (byte) 0x65, (byte) 0x72, (byte) 0x6D, (byte) 0x69, (byte) 0x73,
            (byte) 0x73, (byte) 0x69, (byte) 0x6F, (byte) 0x6E, (byte) 0x2E, (byte) 0x3C, (byte) 0x2F, (byte) 0x70,
            (byte) 0x3E, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x3C, (byte) 0x70,
            (byte) 0x3E, (byte) 0x3C, (byte) 0x61, (byte) 0x20, (byte) 0x68, (byte) 0x72, (byte) 0x65, (byte) 0x66,
            (byte) 0x3D, (byte) 0x22, (byte) 0x68, (byte) 0x74, (byte) 0x74, (byte) 0x70, (byte) 0x3A, (byte) 0x2F,
            (byte) 0x2F, (byte) 0x77, (byte) 0x77, (byte) 0x77, (byte) 0x2E, (byte) 0x69, (byte) 0x61, (byte) 0x6E,
            (byte) 0x61, (byte) 0x2E, (byte) 0x6F, (byte) 0x72, (byte) 0x67, (byte) 0x2F, (byte) 0x64, (byte) 0x6F,
            (byte) 0x6D, (byte) 0x61, (byte) 0x69, (byte) 0x6E, (byte) 0x73, (byte) 0x2F, (byte) 0x65, (byte) 0x78,
            (byte) 0x61, (byte) 0x6D, (byte) 0x70, (byte) 0x6C, (byte) 0x65, (byte) 0x22, (byte) 0x3E, (byte) 0x4D,
            (byte) 0x6F, (byte) 0x72, (byte) 0x65, (byte) 0x20, (byte) 0x69, (byte) 0x6E, (byte) 0x66, (byte) 0x6F,
            (byte) 0x72, (byte) 0x6D, (byte) 0x61, (byte) 0x74, (byte) 0x69, (byte) 0x6F, (byte) 0x6E, (byte) 0x2E,
            (byte) 0x2E, (byte) 0x2E, (byte) 0x3C, (byte) 0x2F, (byte) 0x61, (byte) 0x3E, (byte) 0x3C, (byte) 0x2F,
            (byte) 0x70, (byte) 0x3E, (byte) 0x0A, (byte) 0x3C, (byte) 0x2F, (byte) 0x64, (byte) 0x69, (byte) 0x76,
            (byte) 0x3E, (byte) 0x0A, (byte) 0x3C, (byte) 0x2F, (byte) 0x62, (byte) 0x6F, (byte) 0x64, (byte) 0x79,
            (byte) 0x3E, (byte) 0x0A, (byte) 0x3C, (byte) 0x2F, (byte) 0x68, (byte) 0x74, (byte) 0x6D, (byte) 0x6C,
            (byte) 0x3E, (byte) 0x0A,
    };

    //Internet Protocol Version 4, Src: 10.0.0.129, Dst: 93.184.216.34
    //    0100 .... = Version: 4
    //    .... 0101 = Header Length: 20 bytes (5)
    //    Differentiated Services Field: 0x00 (DSCP: CS0, ECN: Not-ECT)
    //    Total Length: 40
    //    Identification: 0x3c0d (15373)
    //    Flags: 0x02 (Don't Fragment)
    //    Fragment offset: 0
    //    Time to live: 128
    //    Protocol: TCP (6)
    //    Header checksum: 0x7e67 [validation disabled]
    //    [Header checksum status: Unverified]
    //    Source: 10.0.0.129
    //    Destination: 93.184.216.34
    //    [Source GeoIP: Unknown]
    //    [Destination GeoIP: Unknown]
    //Transmission Control Protocol, Src Port: 6843, Dst Port: 80, Seq: 76, Ack: 1583, Len: 0
    //    Source Port: 6843
    //    Destination Port: 80
    //    [Stream index: 2]
    //    [TCP Segment Len: 0]
    //    Sequence number: 76    (relative sequence number)
    //    Acknowledgment number: 1583    (relative ack number)
    //    0101 .... = Header Length: 20 bytes (5)
    //    Flags: 0x010 (ACK)
    //    Window size value: 260
    //    [Calculated window size: 66560]
    //    [Window size scaling factor: 256]
    //    Checksum: 0x9b9b [unverified]
    //    [Checksum Status: Unverified]
    //    Urgent pointer: 0
    static final byte[] SAMPLE_PACKET_08 = {
            (byte) 0x45, (byte) 0x00, (byte) 0x00, (byte) 0x28, (byte) 0x3C, (byte) 0x0D, (byte) 0x40, (byte) 0x00,
            (byte) 0x80, (byte) 0x06, (byte) 0x7E, (byte) 0x67, (byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0x81,
            (byte) 0x5D, (byte) 0xB8, (byte) 0xD8, (byte) 0x22, (byte) 0x1A, (byte) 0xBB, (byte) 0x00, (byte) 0x50,
            (byte) 0x1A, (byte) 0xAC, (byte) 0x5D, (byte) 0x56, (byte) 0x65, (byte) 0xC1, (byte) 0xDA, (byte) 0x0A,
            (byte) 0x50, (byte) 0x10, (byte) 0x01, (byte) 0x04, (byte) 0x9B, (byte) 0x9B, (byte) 0x00, (byte) 0x00,
    };

    //Internet Protocol Version 4, Src: 10.0.0.129, Dst: 93.184.216.34
    //    0100 .... = Version: 4
    //    .... 0101 = Header Length: 20 bytes (5)
    //    Differentiated Services Field: 0x00 (DSCP: CS0, ECN: Not-ECT)
    //    Total Length: 40
    //    Identification: 0x3c0e (15374)
    //    Flags: 0x02 (Don't Fragment)
    //    Fragment offset: 0
    //    Time to live: 128
    //    Protocol: TCP (6)
    //    Header checksum: 0x7e66 [validation disabled]
    //    [Header checksum status: Unverified]
    //    Source: 10.0.0.129
    //    Destination: 93.184.216.34
    //    [Source GeoIP: Unknown]
    //    [Destination GeoIP: Unknown]
    //Transmission Control Protocol, Src Port: 6843, Dst Port: 80, Seq: 76, Ack: 1583, Len: 0
    //    Source Port: 6843
    //    Destination Port: 80
    //    [Stream index: 2]
    //    [TCP Segment Len: 0]
    //    Sequence number: 76    (relative sequence number)
    //    Acknowledgment number: 1583    (relative ack number)
    //    0101 .... = Header Length: 20 bytes (5)
    //    Flags: 0x011 (FIN, ACK)
    //    Window size value: 260
    //    [Calculated window size: 66560]
    //    [Window size scaling factor: 256]
    //    Checksum: 0x9b9a [unverified]
    //    [Checksum Status: Unverified]
    //    Urgent pointer: 0
    static final byte[] SAMPLE_PACKET_09 = {
            (byte) 0x45, (byte) 0x00, (byte) 0x00, (byte) 0x28, (byte) 0x3C, (byte) 0x0E, (byte) 0x40, (byte) 0x00,
            (byte) 0x80, (byte) 0x06, (byte) 0x7E, (byte) 0x66, (byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0x81,
            (byte) 0x5D, (byte) 0xB8, (byte) 0xD8, (byte) 0x22, (byte) 0x1A, (byte) 0xBB, (byte) 0x00, (byte) 0x50,
            (byte) 0x1A, (byte) 0xAC, (byte) 0x5D, (byte) 0x56, (byte) 0x65, (byte) 0xC1, (byte) 0xDA, (byte) 0x0A,
            (byte) 0x50, (byte) 0x11, (byte) 0x01, (byte) 0x04, (byte) 0x9B, (byte) 0x9A, (byte) 0x00, (byte) 0x00,
    };

    //Internet Protocol Version 4, Src: 93.184.216.34, Dst: 10.0.0.129
    //    0100 .... = Version: 4
    //    .... 0101 = Header Length: 20 bytes (5)
    //    Differentiated Services Field: 0x00 (DSCP: CS0, ECN: Not-ECT)
    //    Total Length: 40
    //    Identification: 0xa119 (41241)
    //    Flags: 0x02 (Don't Fragment)
    //    Fragment offset: 0
    //    Time to live: 53
    //    Protocol: TCP (6)
    //    Header checksum: 0x645b [validation disabled]
    //    [Header checksum status: Unverified]
    //    Source: 93.184.216.34
    //    Destination: 10.0.0.129
    //    [Source GeoIP: Unknown]
    //    [Destination GeoIP: Unknown]
    //Transmission Control Protocol, Src Port: 80, Dst Port: 6843, Seq: 1583, Ack: 77, Len: 0
    //    Source Port: 80
    //    Destination Port: 6843
    //    [Stream index: 2]
    //    [TCP Segment Len: 0]
    //    Sequence number: 1583    (relative sequence number)
    //    Acknowledgment number: 77    (relative ack number)
    //    0101 .... = Header Length: 20 bytes (5)
    //    Flags: 0x011 (FIN, ACK)
    //    Window size value: 286
    //    [Calculated window size: 146432]
    //    [Window size scaling factor: 512]
    //    Checksum: 0x9b7f [unverified]
    //    [Checksum Status: Unverified]
    //    Urgent pointer: 0
    static final byte[] SAMPLE_PACKET_10 = {
            (byte) 0x45, (byte) 0x00, (byte) 0x00, (byte) 0x28, (byte) 0xA1, (byte) 0x19, (byte) 0x40, (byte) 0x00,
            (byte) 0x35, (byte) 0x06, (byte) 0x64, (byte) 0x5B, (byte) 0x5D, (byte) 0xB8, (byte) 0xD8, (byte) 0x22,
            (byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0x81, (byte) 0x00, (byte) 0x50, (byte) 0x1A, (byte) 0xBB,
            (byte) 0x65, (byte) 0xC1, (byte) 0xDA, (byte) 0x0A, (byte) 0x1A, (byte) 0xAC, (byte) 0x5D, (byte) 0x57,
            (byte) 0x50, (byte) 0x11, (byte) 0x01, (byte) 0x1E, (byte) 0x9B, (byte) 0x7F, (byte) 0x00, (byte) 0x00,
    };

    //Internet Protocol Version 4, Src: 10.0.0.129, Dst: 93.184.216.34
    //    0100 .... = Version: 4
    //    .... 0101 = Header Length: 20 bytes (5)
    //    Differentiated Services Field: 0x00 (DSCP: CS0, ECN: Not-ECT)
    //    Total Length: 40
    //    Identification: 0x3c0f (15375)
    //    Flags: 0x02 (Don't Fragment)
    //    Fragment offset: 0
    //    Time to live: 128
    //    Protocol: TCP (6)
    //    Header checksum: 0x7e65 [validation disabled]
    //    [Header checksum status: Unverified]
    //    Source: 10.0.0.129
    //    Destination: 93.184.216.34
    //    [Source GeoIP: Unknown]
    //    [Destination GeoIP: Unknown]
    //Transmission Control Protocol, Src Port: 6843, Dst Port: 80, Seq: 77, Ack: 1584, Len: 0
    //    Source Port: 6843
    //    Destination Port: 80
    //    [Stream index: 2]
    //    [TCP Segment Len: 0]
    //    Sequence number: 77    (relative sequence number)
    //    Acknowledgment number: 1584    (relative ack number)
    //    0101 .... = Header Length: 20 bytes (5)
    //    Flags: 0x010 (ACK)
    //    Window size value: 260
    //    [Calculated window size: 66560]
    //    [Window size scaling factor: 256]
    //    Checksum: 0x9b99 [unverified]
    //    [Checksum Status: Unverified]
    //    Urgent pointer: 0
    static final byte[] SAMPLE_PACKET_11 = {
            (byte) 0x45, (byte) 0x00, (byte) 0x00, (byte) 0x28, (byte) 0x3C, (byte) 0x0F, (byte) 0x40, (byte) 0x00,
            (byte) 0x80, (byte) 0x06, (byte) 0x7E, (byte) 0x65, (byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0x81,
            (byte) 0x5D, (byte) 0xB8, (byte) 0xD8, (byte) 0x22, (byte) 0x1A, (byte) 0xBB, (byte) 0x00, (byte) 0x50,
            (byte) 0x1A, (byte) 0xAC, (byte) 0x5D, (byte) 0x57, (byte) 0x65, (byte) 0xC1, (byte) 0xDA, (byte) 0x0B,
            (byte) 0x50, (byte) 0x10, (byte) 0x01, (byte) 0x04, (byte) 0x9B, (byte) 0x99, (byte) 0x00, (byte) 0x00,
    };

    static final byte[][] ALL_TCP_SAMPLES = {
            SAMPLE_PACKET_01, SAMPLE_PACKET_02, SAMPLE_PACKET_03, SAMPLE_PACKET_04, SAMPLE_PACKET_05, SAMPLE_PACKET_06,
            SAMPLE_PACKET_07, SAMPLE_PACKET_08, SAMPLE_PACKET_09, SAMPLE_PACKET_10, SAMPLE_PACKET_11
    };

}
