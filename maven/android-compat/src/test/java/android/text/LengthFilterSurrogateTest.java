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
package android.text;

import com.codename1.compat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/// `LengthFilter` never cuts a surrogate pair in half; like Android it keeps
/// one character less instead.
public class LengthFilterSurrogateTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static final String EMOJI = "\uD83D\uDE00";

    @Test
    public void cutBacksOffAHighSurrogate() {
        InputFilter.LengthFilter f = new InputFilter.LengthFilter(4);
        SpannableStringBuilder dest = new SpannableStringBuilder("ab");
        String src = "c" + EMOJI;
        assertEquals("c", f.filter(src, 0, src.length(), dest, 2, 2).toString());
        src = EMOJI + "d";
        assertEquals(EMOJI, f.filter(src, 0, src.length(), dest, 2, 2).toString());
        dest = new SpannableStringBuilder("abc");
        src = EMOJI;
        assertEquals("", f.filter(src, 0, src.length(), dest, 3, 3).toString());
        assertNull(f.filter("x", 0, 1, dest, 3, 3));
    }
}
