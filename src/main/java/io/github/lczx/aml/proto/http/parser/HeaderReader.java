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

package io.github.lczx.aml.proto.http.parser;

import io.github.lczx.aml.proto.http.HttpHeader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class HeaderReader {

    public static final int MAX_HEADER_SIZE = 8192;

    private final NonBlockingLineReader lineReader = new NonBlockingLineReader();
    private List<HttpHeader.Field> fields = null;
    private int bytesRead = 0;

    public List<HttpHeader.Field> readHeader(final ByteBuffer buffer) throws IOException {
        // Try to read an header line
        final int rem = buffer.remaining();
        final String line = lineReader.readLine(buffer);
        bytesRead += rem - buffer.remaining();
        if (bytesRead > MAX_HEADER_SIZE)
            throw new IOException("Maximum header size of " + MAX_HEADER_SIZE + " bytes reached");

        // If we have no line the buffer was fully consumed without reaching EOL
        if (line == null) return null;

        // If the line is empty (""), we have reached end of headers, return our map
        if (line.isEmpty()) {
            final List<HttpHeader.Field> ret = fields != null ? fields : new LinkedList<HttpHeader.Field>();
            clear();
            return ret;
        }

        // We have an header line...
        if (fields == null) { // ...we don't have a field map yet, parse the prime header and create new
            fields = new LinkedList<>();
            if (!parseFirstLine(line)) putLine(line);
        } else { // ...we already have some fields, parse the line as an header field
            putLine(line);
        }

        // We have added a line but the header is not yet complete
        return null;
    }

    public NonBlockingLineReader getLineReader() {
        return lineReader;
    }

    protected boolean parseFirstLine(final String line) {
        // To be overridden if special handling of prime header line is needed.
        return false;
    }

    private void putLine(final String line) {
        final String[] field = line.split(": *", 2);
        fields.add(new HttpHeader.Field(field[0], field[1]));
    }

    public void clear() {
        fields = null;
        bytesRead = 0;
    }

}
