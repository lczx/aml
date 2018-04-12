package io.github.lczx.aml.tunnel.packet.buffer;

import java.nio.ByteBuffer;

public final class ByteBufferPool {

    // TODO: Implement ByteBuffer pool
    // Some examples:
    // - https://github.com/eclipse/jetty.project/blob/jetty-9.4.x/jetty-io/src/main/java/org/eclipse/jetty/io/ArrayByteBufferPool.java
    // - https://github.com/eclipse/jetty.project/blob/jetty-9.4.x/jetty-util/src/main/java/org/eclipse/jetty/util/BufferUtil.java

    public static final int BUFFER_SIZE = 1520; // TODO: Is this ideal? Or 1506
    //private ConcurrentLinkedQueue<ByteBuffer> pool = new ConcurrentLinkedQueue<>();

    private ByteBufferPool() { }

    public static ByteBuffer acquire() {
        //ByteBuffer buffer = pool.poll();
        // if (buffer == null) { ... }
        return ByteBuffer.allocateDirect(BUFFER_SIZE); // Use direct for zero-copy
    }

    public static void release(ByteBuffer buffer) {
        buffer.clear();
        //pool.offer(buffer);
    }

}
