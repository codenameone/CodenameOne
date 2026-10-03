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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The status classification of the backend test API, which has no tests of its
/// own module; in its package to reach the package-private factory a response
/// uses for a code with no constant.
class HttpStatusTest {
    @Test
    void onlyFourAndFiveHundredsAreErrors() {
        assertFalse(HttpStatus.of(600).isError(), "a 600 is neither 4xx nor 5xx");
        assertFalse(HttpStatus.of(299).isError());
        assertTrue(HttpStatus.of(499).isError(), "a 4xx with no constant");
        assertTrue(HttpStatus.NOT_FOUND.isError());
        assertTrue(HttpStatus.SERVICE_UNAVAILABLE.isError());
        assertFalse(HttpStatus.OK.isError());
    }
}
