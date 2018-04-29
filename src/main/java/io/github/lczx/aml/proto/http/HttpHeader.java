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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class HttpHeader {

    protected final List<Field> fields = new LinkedList<>();

    public String getField(final String name) {
        for (final Field f : fields)
            if (name.equalsIgnoreCase(f.name)) return f.value;
        return null;
    }

    public void putField(final String name, final String value) {
        fields.add(new Field(name, value));
    }

    public List<Field> getFields() {
        return Collections.unmodifiableList(fields);
    }

    public static class Field {
        private final String name;
        private final String value;

        public Field(final String name, final String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return name + ": \"" + value + '"';
        }
    }

}
