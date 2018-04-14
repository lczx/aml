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

/**
 * Transmission Control Block
 */
public class TCB {

    private final long localSeqNBase;
    private final long localAckNBase;
    private final long remoteSeqNBase;
    private final long remoteAckNBase;

    public long localSeqN;
    public long localAckN;
    public long remoteSeqN;
    public long remoteAckN;
    public State state;

    public TCB(final long localSeqN, final long localAckN, final long remoteSeqN, final long remoteAckN) {
        this.localSeqNBase = this.localSeqN = localSeqN;
        this.localAckNBase = this.localAckN = localAckN;
        this.remoteSeqNBase = this.remoteSeqN = remoteSeqN;
        this.remoteAckNBase = this.remoteAckN = remoteAckN;
    }

    @Override
    public String toString() {
        return "TCB{" + state +
                ", local SEQ:" + (localSeqN - localSeqNBase) + " ACK:" + (localAckN - localAckNBase) +
                ", remote SEQ:" + (remoteSeqN - remoteSeqNBase) + " ACK:" + (remoteAckN - remoteAckNBase) +
                '}';
    }

    // TCP has more states but we need only these for now
    public enum State {
        SYN_SENT,
        SYN_RECEIVED,
        ESTABLISHED,
        CLOSE_WAIT,
        LAST_ACK,

        FIN_WAIT_1,
        FIN_WAIT_2,
        CLOSING
    }

}
