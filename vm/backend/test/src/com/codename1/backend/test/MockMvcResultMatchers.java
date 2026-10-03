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

/// The expectations `andExpect(...)` takes, for static import:
///
/// ```java
/// mvc.perform(get("/pets/7"))
///         .andExpect(status().isOk())
///         .andExpect(content().contentType(MediaType.APPLICATION_JSON))
///         .andExpect(jsonPath("$.name").value("Rex"));
/// ```
public final class MockMvcResultMatchers {
    private MockMvcResultMatchers() {
    }

    public static StatusResultMatchers status() {
        return new StatusResultMatchers();
    }

    public static ContentResultMatchers content() {
        return new ContentResultMatchers();
    }

    public static HeaderResultMatchers header() {
        return new HeaderResultMatchers();
    }

    public static CookieResultMatchers cookie() {
        return new CookieResultMatchers();
    }

    /// A JSONPath into the JSON body: `$.name`, `$.items[0].id`, `$.items[*].id`,
    /// `$['odd key']`, `$.items.length()`. Each `%s` in the expression takes the
    /// next of `args`.
    public static JsonPathResultMatchers jsonPath(String expression, Object... args) {
        return new JsonPathResultMatchers(substitute(expression, args));
    }

    /// The response redirects to exactly this location.
    public static ResultMatcher redirectedUrl(final String expected) {
        return new ResultMatcher() {
            @Override
            public void match(MvcResult result) {
                Matchers.equal("Redirected URL", expected, result.getResponse().getRedirectedUrl());
            }
        };
    }

    static String substitute(String expression, Object[] args) {
        if (args == null || args.length == 0) {
            return expression;
        }
        StringBuilder sb = new StringBuilder();
        int next = 0;
        int pos = 0;
        while (pos < expression.length()) {
            int at = expression.indexOf("%s", pos);
            if (at < 0 || next >= args.length) {
                break;
            }
            sb.append(expression, pos, at).append(args[next++]);
            pos = at + 2;
        }
        sb.append(expression.substring(pos));
        return sb.toString();
    }
}
