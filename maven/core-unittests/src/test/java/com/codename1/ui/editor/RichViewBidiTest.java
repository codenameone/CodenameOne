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

package com.codename1.ui.editor;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.TextInputClient;
import com.codename1.ui.TextInputConfig;
import com.codename1.ui.TextInputState;
import com.codename1.ui.layouts.BorderLayout;
import static org.junit.jupiter.api.Assertions.*;

/// Bidi geometry tests for the rich text view, which measures per styled run (variable fonts) and must
/// still reorder mixed LTR/RTL content into the correct visual order via its atom layout.
class RichViewBidiTest extends UITestBase {

    private static final String HEB = "\u05D0\u05D1\u05D2\u05D3";

    private static final class NoopHost implements EditorHost {
        public boolean isTextInputSupported() {
            return false;
        }

        public Object startTextInput(TextInputClient client, TextInputConfig config) {
            return null;
        }

        public void updateTextInputState(Object handle, TextInputState state) {
        }

        public void stopTextInput(Object handle) {
        }

        public void editorChanged() {
        }

        public void fireEditorEvent(String type, String value) {
        }
    }

    private RichView show(String text) {
        RichView v = new RichView(new NoopHost());
        Form f = new Form("rv", new BorderLayout());
        f.add(BorderLayout.CENTER, v);
        f.show();
        for (int i = 0; i < 4; i++) {
            flushSerialCalls();
        }
        v.setText(text);
        return v;
    }

    private Font font(RichView v) {
        return v.getUnselectedStyle().getFont();
    }

    @FormTest
    void rtlCaretXDecreasesWithColumn() {
        RichView v = show(HEB);
        Font f = font(v);
        assertTrue(f.stringWidth(HEB) > 0);
        assertTrue(v.measureColumnX(0, HEB, 0, f) > v.measureColumnX(0, HEB, HEB.length(), f));
    }

    @FormTest
    void ltrCaretRoundTrips() {
        RichView v = show("abcd");
        Font f = font(v);
        for (int c = 0; c <= 4; c++) {
            assertEquals(c, v.columnAtX(0, "abcd", v.measureColumnX(0, "abcd", c, f), f));
        }
    }

    @FormTest
    void rtlCaretRoundTrips() {
        RichView v = show(HEB);
        Font f = font(v);
        for (int c = 0; c <= HEB.length(); c++) {
            assertEquals(c, v.columnAtX(0, HEB, v.measureColumnX(0, HEB, c, f), f));
        }
    }

    @FormTest
    void mixedLineHitTestIsVisuallyStable() {
        String s = "ab " + HEB + " cd";
        RichView v = show(s);
        Font f = font(v);
        for (int c = 0; c <= s.length(); c++) {
            int x = v.measureColumnX(0, s, c, f);
            int back = v.columnAtX(0, s, x, f);
            assertEquals(x, v.measureColumnX(0, s, back, f), "col " + c);
        }
    }

    @FormTest
    void selectionSegmentsTileTheLine() {
        String s = "ab " + HEB + " cd";
        RichView v = show(s);
        Font f = font(v);
        int[] segs = v.lineVisualSegments(0, s, 0, s.length(), f);
        int total = 0;
        for (int i = 1; i < segs.length; i += 2) {
            total += segs[i];
        }
        assertEquals(f.substringWidth(s, 0, s.length()), total, 3);
    }
}
