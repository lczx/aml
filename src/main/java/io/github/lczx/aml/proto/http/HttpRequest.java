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

import java.util.LinkedList;
import java.util.List;

public class HttpRequest extends HttpHeader {

    public static final String FIELD_HOST = "Host";
    public static final String FIELD_USER_AGENT = "User-Agent";
    public static final String FIELD_CONTENT_LENGTH = "Content-Length";
    public static final String FIELD_TRANSFER_ENCODING = "Transfer-Encoding";

    private final String method;
    private final String path;
    private final String version;

    private HttpBodyStream body;

    public HttpRequest(final String method, final String path, final String version) {
        this(method, path, version, new LinkedList<Field>());
    }

    public HttpRequest(final String method, final String path, final String version, final List<Field> headerFields) {
        super(headerFields);
        this.method = method;
        this.path = path;
        this.version = version;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getVersion() {
        return version;
    }

    public HttpBodyStream getBody() {
        if (body == null) {
            final String contentLength = getField(FIELD_CONTENT_LENGTH);
            if (contentLength != null)
                body = new KnownSizeBodyStream(Long.parseLong(contentLength));
            else if ("chunked".equals(getField(FIELD_TRANSFER_ENCODING)))
                body = new ChunkedBodyStream();
            else
                body = new UndeterminedBodyStream();
        }
        return body;
    }

    @Override
    public String toString() {
        return "HttpRequest{" + method + ' ' + path + ' ' + version + ", " + fields.toString() + '}';
    }

}
