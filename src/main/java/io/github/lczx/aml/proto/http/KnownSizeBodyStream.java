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

package io.github.lczx.aml.proto.http;

import java.nio.ByteBuffer;

public class KnownSizeBodyStream extends AbstractBodyStream {

    private long remaining;

    public KnownSizeBodyStream(final long size) {
        super(size);
        this.remaining = size;
    }

    @Override
    public synchronized void appendPayload(final ByteBuffer payload) {
        remaining -= putData(remaining, payload);
    }

    @Override
    protected boolean wantsMoreData() {
        return remaining > 0;
    }

    @Override
    protected boolean isStreamClosed() {
        return !requiresMoreData() && (buffer == null || buffer.available() == 0);
    }

}
