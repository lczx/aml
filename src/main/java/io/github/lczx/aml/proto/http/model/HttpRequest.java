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

package io.github.lczx.aml.proto.http.model;

import io.github.lczx.aml.proto.http.stream.*;

import java.util.Arrays;
import java.util.List;

public class HttpRequest extends AbstractHttpMessage<HttpRequestHeader> {

    private static final List<String> METHODS_WITH_NO_BODY =
            Arrays.asList("GET", "HEAD", "CONNECT", "OPTIONS", "TRACE");

    public HttpRequest(final HttpRequestHeader header) {
        super(header);
    }

    @Override
    protected HttpBodyStream createBodyStream() {
        if (METHODS_WITH_NO_BODY.contains(header.getMethod()))
            return new NullBodyStream();

        // TODO: This is practically the same as in HttpResponse#createBodyStream()
        final String transferEncoding = header.getField(HttpResponseHeader.FIELD_TRANSFER_ENCODING);
        if (transferEncoding != null && !"identity".equalsIgnoreCase(transferEncoding))
            return new ChunkedBodyStream();

        final String contentLength = header.getField(HttpHeader.FIELD_CONTENT_LENGTH);
        if (contentLength != null)
            return new SizedBodyStream(Long.parseLong(contentLength));

        // This shouldn't happen
        return new UndeterminedBodyStream();
    }

}
