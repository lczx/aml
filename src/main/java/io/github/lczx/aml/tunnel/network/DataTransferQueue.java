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

package io.github.lczx.aml.tunnel.network;

import java.nio.ByteBuffer;
import java.util.*;

public class DataTransferQueue {

    private static final int INITIAL_CAPACITY = 4;

    private final Deque<TransferCommand> commandQueue = new ArrayDeque<>(INITIAL_CAPACITY);
    private final List<DataListener> dataListeners = new LinkedList<>();

    private DataReceiver dataReceiver;
    private boolean doRetain = false;
    private boolean flushAutomatically = true;

    public void putData(final ByteBuffer buffer, final Object... attachments) {
        if (!isReceiverSet())
            throw new IllegalStateException("Attempted command enqueuing without setting a data receiver first");

        if (!doRetain && flushAutomatically && commandQueue.isEmpty()) {
            // Optimization skipping the queue and the copying entirely if empty & no retaining is needed
            for (final DataListener dl : dataListeners) dl.onNewData(this, buffer);
            dataReceiver.onDataReady(buffer, attachments);
        } else {
            final ByteBuffer bufferCopy = deepCopy(buffer);
            dataReceiver.onBufferRetained(buffer, attachments);
            commandQueue.add(new DataHolder(bufferCopy));
            for (final DataListener dl : dataListeners) dl.onNewData(this, bufferCopy);
            if (flushAutomatically) flush();
        }
    }

    public void putCommand(final TransferCommand command) {
        if (!isReceiverSet())
            throw new IllegalStateException("Attempted command enqueuing without setting a data receiver first");

        if (flushAutomatically && commandQueue.isEmpty()) {
            dataReceiver.onTransferCommand(command);
        } else {
            commandQueue.add(command);
            if (flushAutomatically) flush();
        }
    }

    public ByteBuffer getLastDataElement() {
        final Iterator<TransferCommand> i = commandQueue.descendingIterator();
        while (i.hasNext()) {
            final TransferCommand ret = i.next();
            if (ret instanceof DataHolder) return ((DataHolder) ret).buffer;
        }
        return null;
    }

    public void setDataReceiver(final DataReceiver dataReceiver) {
        this.dataReceiver = dataReceiver;
    }

    public boolean isReceiverSet() {
        return dataReceiver != null;
    }

    public void addDataListener(final DataListener dataListener, final boolean doesRetain) {
        dataListeners.add(dataListener);
        doRetain = doRetain || doesRetain;
    }

    public boolean isFlushAutomatically() {
        return flushAutomatically;
    }

    public void setFlushAutomatically(final boolean flushAutomatically) {
        this.flushAutomatically = flushAutomatically;
    }

    public void flush() {
        while (!commandQueue.isEmpty()) {
            final TransferCommand cmd = commandQueue.remove();
            if (cmd instanceof DataHolder)
                dataReceiver.onDataReady(((DataHolder) cmd).buffer, (Object[]) null);
            else
                dataReceiver.onTransferCommand(cmd);
        }
    }

    private ByteBuffer deepCopy(final ByteBuffer buffer) {
        final ByteBuffer copy = ByteBuffer.allocateDirect(buffer.capacity());
        buffer.rewind();
        copy.put(buffer);
        buffer.rewind();
        return (ByteBuffer) copy.flip();
    }

    public interface DataListener {
        void onNewData(DataTransferQueue transferQueue, ByteBuffer pendingData);
    }

    public interface DataReceiver {
        void onDataReady(ByteBuffer buffer, Object... attachments);

        void onBufferRetained(ByteBuffer buffer, Object... attachments);

        void onTransferCommand(TransferCommand command);
    }

    public interface TransferCommand { }

    private static class DataHolder implements TransferCommand {
        private final ByteBuffer buffer;

        public DataHolder(final ByteBuffer buffer) {
            this.buffer = buffer;
        }
    }

}
