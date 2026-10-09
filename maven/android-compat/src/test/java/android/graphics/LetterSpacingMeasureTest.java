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
package android.graphics;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.HeadlessImplementation;
import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// Text with letter spacing (Material 3's text appearances all have some) is
/// drawn a character at a time, so it must be measured that way. Measured as
/// one run it came out narrower wherever the font kerns -- the browser's does
/// -- and the JavaScript port clipped the end of every Material label.
public class LetterSpacingMeasureTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void spacedTextMeasuresAsItIsDrawn() {
        AndroidTestSupport.context();
        HeadlessImplementation.kerning = true;
        try {
            Paint p = new Paint();
            p.setTextSize(20);
            p.setLetterSpacing(0.1f);
            String label = "Outlined";
            com.codename1.ui.Font f = p.cn1Font();
            float drawn = 0;
            for (int i = 0; i < label.length(); i++) {
                drawn += f.charWidth(label.charAt(i)) + 0.1f * 20;
            }
            assertEquals(drawn, p.measureText(label), 0.01f);
        } finally {
            HeadlessImplementation.kerning = false;
        }
    }
}
