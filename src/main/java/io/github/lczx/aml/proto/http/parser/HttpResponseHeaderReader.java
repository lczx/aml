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
import io.github.lczx.aml.proto.http.HttpResponse;
import io.github.lczx.aml.proto.http.HttpResponseHeader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class HttpResponseHeaderReader extends HeaderReader implements HttpMessageHeaderReader<HttpResponse> {

    private String version, statusCode, statusDescription;

    @Override
    public HttpResponse readMessage(final ByteBuffer buffer) throws IOException {
        final List<HttpHeader.Field> fields = readHeader(buffer);
        if (fields == null) return null;

        final HttpResponseHeader header = new HttpResponseHeader(version, statusCode, statusDescription, fields);
        version = null;
        statusCode = null;
        statusDescription = null;
        return new HttpResponse(header);
    }

    @Override
    protected boolean parseFirstLine(final String line) {
        final String[] primeHeader = line.trim().split(" +");
        version = primeHeader[0];
        statusCode = primeHeader[1];
        statusDescription = primeHeader[2];
        return true;
    }

}
