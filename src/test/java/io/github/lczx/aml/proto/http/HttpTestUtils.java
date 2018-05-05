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

import io.github.lczx.aml.proto.http.model.HttpHeader;
import io.github.lczx.aml.proto.http.stream.ChunkedBodyStream;
import io.github.lczx.aml.proto.http.stream.HttpBodyStream;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.Callable;

final class HttpTestUtils {

    private HttpTestUtils() { }

    static class ContentReader implements Callable<ContentReaderResult> {

        private final HttpBodyStream bodyStream;

        ContentReader(final HttpBodyStream bodyStream) {
            this.bodyStream = bodyStream;
        }

        @Override
        public ContentReaderResult call() throws Exception {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final byte[] buffer = new byte[128];

            int count;
            while ((count = bodyStream.getInputStream().read(buffer)) != -1)
                out.write(buffer, 0, count);

            return new ContentReaderResult(out.toByteArray(), bodyStream instanceof ChunkedBodyStream ?
                    ((ChunkedBodyStream) bodyStream).getTrailingHeaders(true) : null);
        }

    }

    static class ContentReaderResult {
        public byte[] data;
        public List<HttpHeader.Field> trailingHeaders;

        private ContentReaderResult(final byte[] data, final List<HttpHeader.Field> trailingHeaders) {
            this.data = data;
            this.trailingHeaders = trailingHeaders;
        }
    }

}
