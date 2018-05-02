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

import io.github.lczx.aml.proto.http.stream.HttpBodyStream;

public abstract class AbstractHttpMessage<H extends HttpHeader> implements HttpMessage<H> {

    protected final H header;
    protected HttpBodyStream bodyStream;

    public AbstractHttpMessage(final H header) {
        this.header = header;
    }

    @Override
    public H getHeader() {
        return header;
    }

    @Override
    public HttpBodyStream getBody() {
        if (bodyStream == null) bodyStream = createBodyStream();
        return bodyStream;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' +
                "header=" + header +
                ", bodyStream=" + (bodyStream == null ? "not-initialized" : bodyStream) +
                '}';
    }

    protected abstract HttpBodyStream createBodyStream();

}
