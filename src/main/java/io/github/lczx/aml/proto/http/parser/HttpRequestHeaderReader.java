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
import io.github.lczx.aml.proto.http.HttpRequest;
import io.github.lczx.aml.proto.http.HttpRequestHeader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class HttpRequestHeaderReader extends HeaderReader implements HttpMessageHeaderReader<HttpRequest> {

    private String method, path, version;

    @Override
    public HttpRequest readMessage(final ByteBuffer buffer) throws IOException {
        final List<HttpHeader.Field> fields = readHeader(buffer);
        if (fields == null) return null;

        final HttpRequestHeader header = new HttpRequestHeader(method, path, version, fields);
        method = null;
        path = null;
        version = null;
        return new HttpRequest(header);
    }

    @Override
    protected boolean parseFirstLine(final String line) {
        final String[] primeHeader = line.trim().split(" +");
        method = primeHeader[0];
        path = primeHeader[1];
        version = primeHeader[2];
        return true;
    }

}
