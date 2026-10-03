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

/// Expectations of the response headers, from [MockMvcResultMatchers#header].
public final class HeaderResultMatchers {
    HeaderResultMatchers() {
    }

    /// The header's first value is exactly this.
    public ResultMatcher string(String name, String expected) {
        return stringMatcher(name, expected);
    }

    /// The header's first value is this number.
    public ResultMatcher longValue(String name, long expected) {
        return longValueMatcher(name, expected);
    }

    public ResultMatcher exists(String name) {
        return existsMatcher(name);
    }

    public ResultMatcher doesNotExist(String name) {
        return doesNotExistMatcher(name);
    }

    private static ResultMatcher stringMatcher(final String name, final String expected) {
        return new ResultMatcher() {
            @Override
            public void match(MvcResult result) {
                Matchers.equal("Response header '" + name + "'", expected,
                        result.getResponse().getHeader(name));
            }
        };
    }

    private static ResultMatcher longValueMatcher(final String name, final long expected) {
        return new ResultMatcher() {
            @Override
            public void match(MvcResult result) {
                String actual = result.getResponse().getHeader(name);
                Matchers.check(actual != null, "Response header '" + name + "' does not exist");
                Matchers.equal("Response header '" + name + "'", Long.valueOf(expected),
                        Long.valueOf(Long.parseLong(actual.trim())));
            }
        };
    }

    private static ResultMatcher existsMatcher(final String name) {
        return new ResultMatcher() {
            @Override
            public void match(MvcResult result) {
                Matchers.check(result.getResponse().getHeader(name) != null,
                        "Response header '" + name + "' does not exist");
            }
        };
    }

    private static ResultMatcher doesNotExistMatcher(final String name) {
        return new ResultMatcher() {
            @Override
            public void match(MvcResult result) {
                Matchers.check(result.getResponse().getHeader(name) == null,
                        "Response header '" + name + "' exists with value="
                                + result.getResponse().getHeader(name));
            }
        };
    }
}
