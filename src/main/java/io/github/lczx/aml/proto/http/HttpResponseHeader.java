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

public class HttpResponseHeader extends HttpHeader {

    public static final String FIELD_TRANSFER_ENCODING = "Transfer-Encoding";

    private final String version;
    private final int statusCode;
    private final String statusDescription;

    public HttpResponseHeader(final String version, final String statusCode, final String statusDescription) {
        this(version, statusCode, statusDescription, new LinkedList<Field>());
    }

    public HttpResponseHeader(final String version, final String statusCode, final String statusDescription,
                              final List<Field> headerFields) {
        super(headerFields);
        this.version = version;
        this.statusCode = Integer.parseInt(statusCode);
        this.statusDescription = statusDescription;
    }

    public String getVersion() {
        return version;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    @Override
    public String toString() {
        return "HttpResponseHeader{" + version + " " + statusCode + " " + statusDescription + ", " +
                fields.toString() + '}';
    }

}
