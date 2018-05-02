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

import java.util.Arrays;
import java.util.List;

final class HttpMessageSamples {

    static final String HTTP_VERSION = "HTTP/1.1";
    static final String HTTP_HOST = "example.com";

    static final class Req1 {
        static final int IDX = 0;
        static final String METHOD = "POST";
        static final String PATH = "/";
        static final int FIELD_COUNT = 3;
        static final String UA = "Raving Ducks: The Final Cut ";
        static final String BODY = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc at rhoncus " +
                "velit. Ut pharetra tempor nisi a suscipit. Nulla ultrices sagittis ex, ac mollis libero efficitur " +
                "sit amet. Quisque venenatis fermentum ipsum, quis laoreet neque ultricies quis metus.";

        static final String VALUE = METHOD + ' ' + PATH + ' ' + HTTP_VERSION + '\n' +
                "Host: " + HTTP_HOST + '\n' +
                "User-Agent: " + UA + '\n' +
                "Content-Length: " + BODY.length() /* = 256 */ + '\n' +
                '\n' + BODY;

        private Req1() { }
    }

    static final class Req2 {
        static final int IDX = 1;
        static final String METHOD = "GET";
        static final String PATH = "/index.html";
        static final int FIELD_COUNT = 2;
        static final String UA = "aml/0.1";

        static final String VALUE = METHOD + ' ' + PATH + ' ' + HTTP_VERSION + "\r\n" +
                "Host: " + HTTP_HOST + "\r\n" +
                "User-Agent: " + UA + "\r\n" +
                "\r\n";

        private Req2() { }
    }

    static final class Req3 {
        static final int IDX = 2;
        static final String METHOD = "POST";
        static final String PATH = "/kittens";
        static final int FIELD_COUNT = 4;
        static final String UA = "Trolololol/5.0 (TempleOS 5.03, x86_64) Satan/66.0.1234 SoMuchFunWithRandomUserAgents/3.5";
        static final String CUSTOM_KEY = "X-Custom-Field";
        static final String CUSTOM_VALUE = "GIMME FUE GIMME FAI GIMME DABAJABAZA";
        static final String BODY = "name=Belphegor&race=tabby";

        static final String VALUE = METHOD + ' ' + PATH + ' ' + HTTP_VERSION + "\r\n" +
                "Host: " + HTTP_HOST + "\r\n" +
                "User-Agent: " + UA + "\r\n" +
                "Content-Length: " + BODY.length() /* = 25 */ + "\r\n" +
                CUSTOM_KEY + ": " + CUSTOM_VALUE + "\r\n" +
                "\r\n" + BODY;

        private Req3() { }
    }

    static final class Req4 {
        static final int IDX = 3;
        static final String METHOD = "HEAD";
        static final String PATH = "/index.html";
        static final int FIELD_COUNT = 2;
        static final String UA = "aml/0.1";

        static final String VALUE = METHOD + ' ' + PATH + ' ' + HTTP_VERSION + "\r\n" +
                "Host: " + HTTP_HOST + "\r\n" +
                "User-Agent: " + UA + "\r\n" +
                "\r\n";

        private Req4() { }
    }

    static final class Req5 {
        static final int IDX = 4;
        static final String METHOD = "POST";
        static final String PATH = "/lol";
        static final int FIELD_COUNT = 5;
        static final String TE_KEY = "TE";
        static final String TE_VALUE = "trailers";
        static final String TRANSFER_ENCODING = "chunked";
        static final String TRAILER_KEY = "Trailer";
        static final String[] TRAILER_VALUES = new String[]{"X-Custom-Field", "X-Funny-Trailer"};
        static final List<HttpHeader.Field> TRAILING_HEADERS = Arrays.asList(
                new HttpHeader.Field("X-Custom-Field", "Whoa!"),
                new HttpHeader.Field("X-Funny-Trailer", "yes"));
        static final String BODY = "This is a nice chunked body stream made especially for this" +
                " weird test, I have to make this chunk 82 bytes long to blablabla\r\n";
        private static final String BODY_CHUNKED = "0D\r\n" +
                "This is a nic\r\n" +
                "1C\r\n" +
                "e chunked body stream made e\r\n" +
                "52\r\n" +
                "specially for this weird test, I have to make this chunk 82 bytes long to blablabl\r\n" +
                "3\r\n" +
                "a\r\n\r\n" +
                "00\r\n" +
                "X-Custom-Field: Whoa!\r\n" +
                "X-Funny-Trailer: yes\r\n" +
                "\r\n";

        static final String VALUE = METHOD + ' ' + PATH + ' ' + HTTP_VERSION + "\r\n" +
                "Host: " + HTTP_HOST + "\r\n" +
                TE_KEY + ": " + TE_VALUE + "\r\n" +
                TRAILER_KEY + ": " + TRAILER_VALUES[0] + "\r\n" +
                TRAILER_KEY + ": " + TRAILER_VALUES[1] + "\r\n" +
                "Transfer-Encoding: " + TRANSFER_ENCODING + "\r\n" +
                "\r\n" + BODY_CHUNKED;

        private Req5() { }
    }

    static final class Req6 {
        static final int IDX = 5;
        static final String METHOD = "POST";
        static final String PATH = "/ohmy";
        static final int FIELD_COUNT = 2;
        static final String CUSTOM_KEY = "X-Description";
        static final String CUSTOM_VALUE = "Last request w/ no Content-Length, body stream should be closed with connection";
        static final String BODY = "The quick brown fox jumped over the lazy dog\n";

        static final String VALUE = METHOD + ' ' + PATH + ' ' + HTTP_VERSION + "\r\n" +
                "Host: " + HTTP_HOST + "\r\n" +
                CUSTOM_KEY + ": " + CUSTOM_VALUE + "\r\n" +
                "\r\n" + BODY;


        private Req6() { }
    }

    private HttpMessageSamples() { }

}
