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

import com.codename1.compat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/// `normalizeScheme()` lower-cases the scheme and nothing else; it used to
/// return the URI unchanged.
public class UriNormalizeSchemeTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void lowerCasesOnlyTheScheme() {
        Uri u = Uri.parse("HTTP://Example.COM/Path?Q=V#Frag").normalizeScheme();
        assertEquals("http", u.getScheme());
        assertEquals("http://Example.COM/Path?Q=V#Frag", u.toString());
        assertEquals("mailto:Someone@Example.com",
                Uri.parse("MailTo:Someone@Example.com").normalizeScheme().toString());
    }

    @Test
    public void returnsTheSameUriWhenNothingChanges() {
        Uri lower = Uri.parse("http://example.com");
        assertSame(lower, lower.normalizeScheme());
        Uri relative = Uri.parse("Some/Path");
        assertSame(relative, relative.normalizeScheme());
    }
}
