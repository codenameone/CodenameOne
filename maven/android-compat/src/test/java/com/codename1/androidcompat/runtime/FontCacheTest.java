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

import android.graphics.Typeface;

import com.codename1.androidcompat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// Android weights onto Codename One's native faces. Medium has no face of its
/// own and falls back to regular, as CSS font matching does for 500.
public class FontCacheTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static String face(int weight, boolean italic) {
        return FontCache.nativeName(Typeface.create(Typeface.DEFAULT, weight, italic));
    }

    @Test
    public void mediumIsRegularAndSemiBoldIsBold() {
        assertEquals("native:MainRegular", face(400, false));
        assertEquals("native:MainRegular", face(500, false));
        assertEquals("native:MainBold", face(600, false));
        assertEquals("native:MainBold", face(700, false));
        assertEquals("native:ItalicRegular", face(500, true));
        assertEquals("native:MainBlack", face(900, false));
        assertEquals("native:MainLight", face(300, false));
    }
}
