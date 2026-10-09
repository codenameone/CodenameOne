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
package android.widget;

import android.text.TextUtils;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/// `android:ellipsize="start"` and `"middle"` on a single line keep the end,
/// or both ends, of a long path or account number. Every mode used to cut the
/// end.
public class TextViewEllipsizeModeTest {

    private static final char ELLIPSIS = (char) 0x2026;
    private static final String TEXT = "/storage/emulated/0/Download/report-final.pdf";

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static String shown(TextUtils.TruncateAt where, int width) {
        TextView t = new TextView(AndroidTestSupport.context());
        t.setSingleLine(true);
        t.setEllipsize(where);
        t.setText(TEXT);
        return t.layoutFor(width).lines.get(0);
    }

    @Test
    public void startAndMiddleCutTheRequestedSide() {
        TextView probe = new TextView(AndroidTestSupport.context());
        int width = Math.round(probe.getPaint().measureText(TEXT) / 2);

        String end = shown(TextUtils.TruncateAt.END, width);
        assertTrue(end, end.startsWith("/storage") && end.charAt(end.length() - 1) == ELLIPSIS);

        String start = shown(TextUtils.TruncateAt.START, width);
        assertEquals("START kept the wrong side: " + start, ELLIPSIS, start.charAt(0));
        assertTrue(start, start.endsWith("report-final.pdf"));

        String middle = shown(TextUtils.TruncateAt.MIDDLE, width);
        assertTrue("MIDDLE lost an end: " + middle, middle.startsWith("/storage") && middle.endsWith(".pdf"));
        assertTrue(middle, middle.indexOf(ELLIPSIS) > 0);

        assertTrue(probe.getPaint().measureText(start) <= width);
        assertTrue(probe.getPaint().measureText(middle) <= width);
    }

    @Test
    public void textUtilsHonoursTheModeToo() {
        TextView probe = new TextView(AndroidTestSupport.context());
        float width = probe.getPaint().measureText(TEXT) / 2;
        String start = TextUtils.ellipsize(TEXT, probe.getPaint(), width, TextUtils.TruncateAt.START).toString();
        assertEquals(start, ELLIPSIS, start.charAt(0));
        assertTrue(start, start.endsWith(".pdf"));
    }
}
