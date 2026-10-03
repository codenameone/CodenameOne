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

/// Expectations of the response body, from [MockMvcResultMatchers#content].
public final class ContentResultMatchers {
    ContentResultMatchers() {
    }

    /// The body is exactly this text.
    public ResultMatcher string(String expected) {
        return stringMatcher(expected);
    }

    /// The body is exactly these bytes.
    public ResultMatcher bytes(byte[] expected) {
        return bytesMatcher(expected);
    }

    /// The Content-Type is this one, parameters included when it names any:
    /// `application/json` matches `application/json;charset=utf-8`, which a
    /// stricter test names in full.
    public ResultMatcher contentType(String expected) {
        return contentTypeMatcher(expected);
    }

    /// The Content-Type's type and subtype are compatible with this one;
    /// `application/*` and `*/*` match anything in their range.
    public ResultMatcher contentTypeCompatibleWith(String expected) {
        return contentTypeCompatibleWithMatcher(expected);
    }

    /// The body is JSON holding everything `expected` does; objects may carry
    /// more fields. [#json(String, boolean)] with `true` asks for exact equality.
    public ResultMatcher json(String expected) {
        return json(expected, false);
    }

    public ResultMatcher json(String expected, boolean strict) {
        return jsonMatcher(expected, strict);
    }

    private static String type(String contentType) {
        int semi = contentType.indexOf(';');
        return (semi < 0 ? contentType : contentType.substring(0, semi)).trim();
    }

    private static ResultMatcher stringMatcher(final String expected) {
        return new ResultMatcher() {
            @Override
            public void match(MvcResult result) {
                Matchers.equal("Response content", expected,
                        result.getResponse().getContentAsString());
            }
        };
    }

    private static ResultMatcher bytesMatcher(final byte[] expected) {
        return new ResultMatcher() {
            @Override
            public void match(MvcResult result) {
                Matchers.check(java.util.Arrays.equals(expected,
                        result.getResponse().getContentAsByteArray()),
                        "Response content does not match the expected bytes");
            }
        };
    }

    private static ResultMatcher contentTypeMatcher(final String expected) {
        return new ResultMatcher() {
            @Override
            public void match(MvcResult result) {
                String actual = result.getResponse().getContentType();
                Matchers.check(actual != null && (actual.equalsIgnoreCase(expected)
                        || (expected.indexOf(';') < 0 && type(actual).equalsIgnoreCase(expected))),
                        "Content type expected:<" + expected + "> but was:<" + actual + ">");
            }
        };
    }

    private static ResultMatcher contentTypeCompatibleWithMatcher(final String expected) {
        return new ResultMatcher() {
            @Override
            public void match(MvcResult result) {
                String actual = result.getResponse().getContentType();
                String e = type(expected);
                String a = actual == null ? "" : type(actual);
                boolean ok = "*/*".equals(e) || e.equalsIgnoreCase(a)
                        || (e.endsWith("/*") && a.regionMatches(true, 0, e, 0, e.length() - 1));
                Matchers.check(ok, "Content type [" + actual + "] is not compatible with ["
                        + expected + "]");
            }
        };
    }

    private static ResultMatcher jsonMatcher(final String expected, final boolean strict) {
        return new ResultMatcher() {
            @Override
            public void match(MvcResult result) throws Exception {
                Object want = Json.parse(expected);
                Object got = Json.parse(result.getResponse().getContentAsString());
                Matchers.check(Matchers.jsonMatches(want, got, strict), "JSON "
                        + (strict ? "" : "leniently ") + "expected:<" + expected + "> but was:<"
                        + result.getResponse().getContentAsString() + ">");
            }
        };
    }
}
