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

public class HttpResponse extends AbstractHttpMessage<HttpResponseHeader> {

    private HttpRequestHeader requestHeader;

    public HttpResponse(final HttpResponseHeader header) {
        super(header);
    }

    public void setRequestHeader(final HttpRequestHeader requestHeader) {
        this.requestHeader = requestHeader;
    }

    public HttpRequestHeader getRequestHeader() {
        return requestHeader;
    }

    @Override
    protected HttpBodyStream createBodyStream() {
        // https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.4

        // Any message with 1xx, 204 and 304 responses plus any response to HEAD should have no body
        if (hasNoBody()) return new NullBodyStream();

        // If Transfer-Encoding is present and has any value other than "identity",
        // the length is defined by the use of the chunked transfer encoding
        final String transferEncoding = header.getField(HttpResponseHeader.FIELD_TRANSFER_ENCODING);
        if (transferEncoding != null && !"identity".equalsIgnoreCase(transferEncoding))
            return new ChunkedBodyStream();

        // If Content-Length is present, use that to determine the stream
        final String contentLength = header.getField(HttpHeader.FIELD_CONTENT_LENGTH);
        if (contentLength != null)
            return new SizedBodyStream(Long.parseLong(contentLength));

        // Otherwise size it is determined by the server closing the connection
        return new UndeterminedBodyStream();
    }

    private boolean hasNoBody() {
        final int responseCode = header.getStatusCode();
        if ((responseCode >= 100 && responseCode < 200) || responseCode == 204 || responseCode == 304)
            return true;

        if (requestHeader != null && "HEAD".equalsIgnoreCase(requestHeader.getMethod()))
            return true;

        return false;
    }

}
