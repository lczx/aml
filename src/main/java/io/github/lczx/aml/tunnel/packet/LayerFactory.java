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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

final class LayerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(LayerFactory.class);

    private static final LayerDetector networkLayerDetector = new NetworkLayerDetector();
    private static final LayerDetector transportLayerDetector = new TransportLayerDetector();

    private LayerFactory() { }

    static LayerDetector getFactory(int type) {
        switch (type) {
            case Packets.LAYER_NETWORK:
                return networkLayerDetector;
            case Packets.LAYER_TRANSPORT:
                return transportLayerDetector;
            default:
                throw new IllegalArgumentException("No layer factory implementation found for the given type");
        }
    }

    private static class NetworkLayerDetector implements LayerDetector {
        @Override
        public ProtocolLayer detectLayer(ProtocolLayer parent, ByteBuffer buffer, int offset) {
            if ((buffer.get(offset) & 0xF0) == 0x40)
                return new IPv4Layer(parent, buffer, offset);
            throw new LayerDetectException("Cannot determine network layer type");
        }
    }

    private static class TransportLayerDetector implements LayerDetector {
        @Override
        public ProtocolLayer detectLayer(ProtocolLayer parent, ByteBuffer buffer, int offset) {
            final short protoId = ((IPv4Layer) parent).getProtocolId();
            switch (protoId) {
                case IPv4Layer.PROTOCOL_TCP:
                    return new TcpLayer(parent, buffer, offset);
                case IPv4Layer.PROTOCOL_UDP:
                    return new UdpLayer(parent, buffer, offset);
                case IPv4Layer.PROTOCOL_ICMP:
                    // TODO: Consider implementing ICMP
                    LOG.info("Requested construction for an ICMP layer " +
                            "(which is not yet implemented), treating as data");
                    return null;
                default:
                    LOG.warn("Unknown IPv4 protocol ID ({}), treating as data", protoId);
                    return null;
            }
        }
    }

    public interface LayerDetector {
        ProtocolLayer detectLayer(ProtocolLayer parent, ByteBuffer buffer, int offset);
    }

    private static class LayerDetectException extends RuntimeException {
        private LayerDetectException(String message) {
            super(message);
        }
    }

}
