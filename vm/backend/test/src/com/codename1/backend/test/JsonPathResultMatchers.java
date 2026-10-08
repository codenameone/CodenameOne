/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.backend.test;

import com.codename1.backend.Json;
import java.util.List;
import java.util.Map;

/// Expectations of one JSONPath into the JSON body, from
/// [MockMvcResultMatchers#jsonPath].
public final class JsonPathResultMatchers {
    private final String expression;

    JsonPathResultMatchers(String expression) {
        this.expression = expression;
        // Parsed now, so a malformed path fails where it is written.
        JsonPath.compile(expression);
    }

    /// The value there equals `expected`. Numbers compare by value, so `5`
    /// matches the parser's `5L`; a list compares element by element.
    public ResultMatcher value(final Object expected) {
        return new ResultMatcher() {
            @Override
            public void match(MvcResult result) throws Exception {
                Object actual = read(result);
                Matchers.check(!JsonPath.isMissing(actual), "No value at JSON path \""
                        + expression + "\"");
                Matchers.equal("JSON path \"" + expression + "\"", expected, actual);
            }
        };
    }

    /// A non-null value is there. A JSON `null` does not count, as in Spring:
    /// `{"value":null}` fails `exists()` and passes [#doesNotExist].
    public ResultMatcher exists() {
        return new ResultMatcher() {
            @Override
            public void match(MvcResult result) throws Exception {
                Object actual = read(result);
                Matchers.check(!JsonPath.isMissing(actual) && actual != null,
                        "No value at JSON path \"" + expression + "\"");
            }
        };
    }

    /// No non-null value is there: the path is absent or holds JSON `null`.
    public ResultMatcher doesNotExist() {
        return new ResultMatcher() {
            @Override
            public void match(MvcResult result) throws Exception {
                Object actual = read(result);
                Matchers.check(JsonPath.isMissing(actual) || actual == null,
                        "Expected no value at JSON path \"" + expression + "\" but found: " + actual);
            }
        };
    }

    /// An empty value is there: an empty string, list or map, or JSON `null`. An
    /// absent path fails, as [#isNotEmpty] does -- it is not an empty value.
    public ResultMatcher isEmpty() {
        return new ResultMatcher() {
            @Override
            public void match(MvcResult result) throws Exception {
                Object actual = read(result);
                Matchers.check(!JsonPath.isMissing(actual) && empty(actual), "Expected an empty value at JSON path \""
                        + expression + "\" but found: " + actual);
            }
        };
    }

    public ResultMatcher isNotEmpty() {
        return new ResultMatcher() {
            @Override
            public void match(MvcResult result) throws Exception {
                Object actual = read(result);
                Matchers.check(!JsonPath.isMissing(actual) && !empty(actual),
                        "Expected a non-empty value at JSON path \"" + expression + "\"");
            }
        };
    }

    public ResultMatcher isArray() {
        return kind(List.class, "an array");
    }

    public ResultMatcher isMap() {
        return kind(Map.class, "a map");
    }

    public ResultMatcher isString() {
        return kind(String.class, "a string");
    }

    public ResultMatcher isNumber() {
        return kind(Number.class, "a number");
    }

    public ResultMatcher isBoolean() {
        return kind(Boolean.class, "a boolean");
    }

    private ResultMatcher kind(final Class type, final String what) {
        return new ResultMatcher() {
            @Override
            public void match(MvcResult result) throws Exception {
                Object actual = read(result);
                Matchers.check(!JsonPath.isMissing(actual) && type.isInstance(actual),
                        "Expected " + what + " at JSON path \"" + expression + "\" but found: "
                                + actual);
            }
        };
    }

    private Object read(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return JsonPath.compile(expression).read(Json.parse(body));
    }

    private static boolean empty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).length() == 0;
        }
        if (value instanceof List) {
            return ((List) value).isEmpty();
        }
        if (value instanceof Map) {
            return ((Map) value).isEmpty();
        }
        return false;
    }
}
