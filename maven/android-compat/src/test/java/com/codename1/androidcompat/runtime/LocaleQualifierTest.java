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
package com.codename1.androidcompat.runtime;

import com.codename1.compat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

/// Device locale to resource qualifiers: `values-fil` must match a Filipino
/// device, which a fixed two-letter cut turned into `fi`.
public class LocaleQualifierTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void keepsTheWholeLanguage() {
        assertArrayEquals(new String[] {"en", "US"}, ResourceManager.languageAndRegion("en_US"));
        assertArrayEquals(new String[] {"fil", "PH"}, ResourceManager.languageAndRegion("fil_PH"));
        assertArrayEquals(new String[] {"pt", "BR"}, ResourceManager.languageAndRegion("pt-br"));
        assertArrayEquals(new String[] {"haw", ""}, ResourceManager.languageAndRegion("haw"));
        assertNull(ResourceManager.languageAndRegion("x"));
        assertNull(ResourceManager.languageAndRegion(null));
    }

    /// A UN M49 region keeps its three digits, so `values-b+es+419` can
    /// match a Latin-American Spanish device; it was cut to `41`.
    @Test
    public void keepsANumericRegion() {
        assertArrayEquals(new String[] {"es", "419"}, ResourceManager.languageAndRegion("es_419"));
        assertArrayEquals(new String[] {"es", "419"}, ResourceManager.languageAndRegion("es-419"));
        assertArrayEquals(new String[] {"zh", "CN"}, ResourceManager.languageAndRegion("zh_CN_#Hans"));
    }
}
