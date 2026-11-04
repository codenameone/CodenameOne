/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The bundle hoister writes a member key as a dotted access, `o["name"]` to `o.name`,
/// and must do so only where the two mean the same.
class JavascriptHoistMemberKeyTest {

    private static String hoist(String src) {
        return JavascriptBundleWriter.hoistStringConstants(src, new int[1]);
    }

    @Test
    void memberKeysBecomeDottedAccess() {
        String out = hoist("a[\"field\"] = b()[\"value\"] + c[0][\"other\"];");
        assertTrue(out.contains("a.field = b().value + c[0].other;"), out);
    }

    @Test
    void keywordBeforeBracketIsNotAnOperand() {
        String out = hoist("function f(x){ return[\"name\"]; } g(typeof[\"name\"], \"name\" in[\"name\"]);");
        assertFalse(out.contains("return.name"), out);
        assertFalse(out.contains("typeof.name"), out);
        assertFalse(out.contains("in.name"), out);
    }

    @Test
    void nonIdentifierKeysStayKeyed() {
        String out = hoist("a[\"9lives\"]; a[\"\"]; 1[\"name\"]; a[\"with space\"]; [\"array\"];");
        assertFalse(out.contains("a.9lives"), out);
        assertFalse(out.contains("a."), out);
        assertFalse(out.contains("1.name"), out);
        assertTrue(out.contains("[\"with space\"]"), out);
    }
}
