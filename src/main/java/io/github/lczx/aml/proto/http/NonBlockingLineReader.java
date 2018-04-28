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

public class NonBlockingLineReader {

    private final StringBuilder stringBuilder = new StringBuilder(200);

    public String readLine(final ByteBuffer buffer) {
        final int avail = buffer.remaining();
        for (int i = 0; i < avail; i++) {
            final char lastChar = (char) buffer.get();
            stringBuilder.append(lastChar);
            if (lastChar == '\n') return trimLine();
        }
        return null;
    }

    public String getLastLine() {
        return stringBuilder.toString();
    }

    public void clear() {
        stringBuilder.setLength(0);
    }

    private String trimLine() {
        final int len = stringBuilder.length();
        if (len >= 2 && "\r\n".equals(stringBuilder.substring(len - 2)))
            stringBuilder.setLength(len - 2);
        else if (len >= 1 && "\n".equals(stringBuilder.substring(len - 1)))
            stringBuilder.setLength(len - 1);
        final String line = stringBuilder.toString();
        clear();
        return line;
    }

}
