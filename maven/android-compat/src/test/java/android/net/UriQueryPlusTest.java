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
package android.net;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// Query parameter decoding follows Android: `getQueryParameter` reads a
/// literal `+` as a space, `getQueryParameters` keeps it, and `%2B` is a
/// plus in both. `getQueryParameters` used to turn the plus into a space.
public class UriQueryPlusTest {

    @Test
    public void pluralKeepsLiteralPlus() {
        Uri u = Uri.parse("https://example.com/p?sig=a+b%2Bc&sig=x+y");
        assertEquals("[a+b+c, x+y]", u.getQueryParameters("sig").toString());
    }

    @Test
    public void singularReadsPlusAsSpace() {
        Uri u = Uri.parse("https://example.com/p?q=a+b%2Bc");
        assertEquals("a b+c", u.getQueryParameter("q"));
    }
}
