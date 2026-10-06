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

/// A builder taken from an opaque URI becomes hierarchical once an authority,
/// path or query is set, as on Android; the stale opaque part used to win in
/// `build()` and silently discard the new path.
public class UriBuilderOpaqueResetTest {

    @Test
    public void pathReplacesOpaquePart() {
        assertEquals("mailto:/inbox", Uri.parse("mailto:a@b").buildUpon().path("/inbox").build().toString());
    }

    @Test
    public void authorityAndQueryReplaceOpaquePart() {
        Uri u = Uri.parse("mailto:a@b").buildUpon().scheme("https").authority("example.com")
                .appendQueryParameter("q", "1").build();
        assertEquals("https://example.com?q=1", u.toString());
    }

    @Test
    public void opaqueRoundTripIsUnchanged() {
        assertEquals("mailto:a@b#f", Uri.parse("mailto:a@b#f").buildUpon().build().toString());
    }
}
