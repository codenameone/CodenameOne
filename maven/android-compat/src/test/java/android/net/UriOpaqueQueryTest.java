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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// An opaque URI has no query, as on Android: the `?` in a mailto is part of
/// its scheme-specific part, and a `?` inside a fragment starts nothing.
public class UriOpaqueQueryTest {

    @Test
    public void opaqueUriHasNoQuery() {
        Uri u = Uri.parse("mailto:user@example.com?subject=x");
        assertTrue(u.isOpaque());
        assertNull(u.getEncodedQuery());
        assertNull(u.getQuery());
        assertQueryAccessRejected(u);
        assertEquals("user@example.com?subject=x", u.getSchemeSpecificPart());
        assertEquals("mailto:user@example.com?subject=x", u.buildUpon().build().toString());
    }

    private static void assertQueryAccessRejected(Uri uri) {
        try {
            uri.getQueryParameter("subject");
            fail("opaque URI accepted getQueryParameter");
        } catch (UnsupportedOperationException expected) {
        }
        try {
            uri.getQueryParameters("subject");
            fail("opaque URI accepted getQueryParameters");
        } catch (UnsupportedOperationException expected) {
        }
        try {
            uri.getQueryParameterNames();
            fail("opaque URI accepted getQueryParameterNames");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void questionMarkInFragmentIsNotAQuery() {
        Uri u = Uri.parse("https://example.com/p#frag?x=1");
        assertNull(u.getEncodedQuery());
        assertEquals("frag?x=1", u.getFragment());
    }

    @Test
    public void hierarchicalQueryStillParses() {
        Uri u = Uri.parse("https://example.com/p?a=1#f");
        assertEquals("a=1", u.getEncodedQuery());
        assertEquals("1", u.getQueryParameter("a"));
    }

    @Test
    public void decodedOpaquePartsCannotBecomeHierarchical() {
        Uri fromParts = Uri.fromParts("x", "/path@host:part", null);
        assertTrue(fromParts.isOpaque());
        assertEquals("/path@host:part", fromParts.getSchemeSpecificPart());
        assertEquals("x:%2Fpath%40host%3Apart", fromParts.toString());

        Uri built = new Uri.Builder().scheme("x").opaquePart("/path@host:part").build();
        assertTrue(built.isOpaque());
        assertEquals(fromParts.toString(), built.toString());
    }

    @Test
    public void fromPartsRejectsNullRequiredParts() {
        try {
            Uri.fromParts(null, "part", null);
            fail("null scheme accepted");
        } catch (NullPointerException expected) {
        }
        try {
            Uri.fromParts("x", null, null);
            fail("null scheme-specific part accepted");
        } catch (NullPointerException expected) {
        }
    }
}
