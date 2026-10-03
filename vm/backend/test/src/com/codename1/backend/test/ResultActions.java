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

/// What to do with a [MockMvc] result: assert on it, act on it, or return it.
public final class ResultActions {
    private final MvcResult result;

    ResultActions(MvcResult result) {
        this.result = result;
    }

    /// Asserts one expectation, throwing AssertionError when it fails.
    public ResultActions andExpect(ResultMatcher matcher) throws Exception {
        matcher.match(result);
        return this;
    }

    /// Asserts every expectation, reporting all the failures together. A matcher
    /// that throws -- a body that is not JSON, a header that is not a number -- is
    /// one of the failures and the rest still run; an `Error` other than an
    /// assertion failure still ends the run.
    public ResultActions andExpectAll(ResultMatcher... matchers) throws Exception {
        StringBuilder failures = null;
        for (ResultMatcher value : matchers) {
            String failure;
            try {
                value.match(result);
                continue;
            } catch (AssertionError failed) {
                failure = failed.getMessage();
            } catch (Exception failed) {
                failure = failed.getClass().getName() + ": " + failed.getMessage();
            }
            if (failures == null) {
                failures = new StringBuilder("Multiple expectations failed:");
            }
            failures.append("\n\t").append(failure);
        }
        if (failures != null) {
            throw new AssertionError(failures.toString());
        }
        return this;
    }

    public ResultActions andDo(ResultHandler handler) throws Exception {
        handler.handle(result);
        return this;
    }

    public MvcResult andReturn() {
        return result;
    }
}
