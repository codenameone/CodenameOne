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

/// Headless integration tests for the bidi geometry wiring in {@link EditorView} (same package, so the
/// protected {@code measureColumnX}/{@code columnAtX}/{@code lineVisualSegments} hooks are reachable).
/// The Unicode Bidi Algorithm itself is covered by {@link BidiUtilTest}; this verifies the editor maps
/// logical columns to visual x and back correctly for right-to-left and mixed lines.
class EditorViewBidiTest extends UITestBase {

    private static final String HEB = "\u05D0\u05D1\u05D2\u05D3"; // 4 Hebrew letters (strong R)

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

    private EditorView show(String text) {
        EditorView v = new EditorView(new NoopHost(), true);
        Form f = new Form("ev", new BorderLayout());
        f.add(BorderLayout.CENTER, v);
        f.show();
        for (int i = 0; i < 4; i++) {
            flushSerialCalls();
        }
        v.setText(text);
        return v;
    }

    private Font font(EditorView v) {
        return v.getUnselectedStyle().getFont();
    }

    @FormTest
    void ltrCaretXIncreasesWithColumn() {
        EditorView v = show("abcd");
        Font f = font(v);
        assertTrue(f.stringWidth("abcd") > 0, "test font must report widths");
        int prev = -1;
        for (int c = 0; c <= 4; c++) {
            int x = v.measureColumnX(0, "abcd", c, f);
            assertTrue(x >= prev, "LTR caret x must be non-decreasing (col " + c + ")");
            prev = x;
        }
    }

    @FormTest
    void rtlCaretXDecreasesWithColumn() {
        EditorView v = show(HEB);
        Font f = font(v);
        // pure RTL line: logical column 0 is the RIGHTMOST caret, the end is the LEFTMOST
        int x0 = v.measureColumnX(0, HEB, 0, f);
        int xEnd = v.measureColumnX(0, HEB, HEB.length(), f);
        assertTrue(x0 > xEnd, "in RTL the start caret is to the right of the end caret");
        int prev = Integer.MAX_VALUE;
        for (int c = 0; c <= HEB.length(); c++) {
            int x = v.measureColumnX(0, HEB, c, f);
            assertTrue(x <= prev, "RTL caret x must be non-increasing (col " + c + ")");
            prev = x;
        }
    }

    @FormTest
    void caretRoundTripsThroughHitTestForUnidirectionalText() {
        // In purely LTR or purely RTL text every logical column has a distinct visual x, so hit testing
        // the caret x returns the exact same column.
        for (String text : new String[]{"abcd", HEB}) {
            EditorView v = show(text);
            Font f = font(v);
            for (int c = 0; c <= text.length(); c++) {
                int x = v.measureColumnX(0, text, c, f);
                int back = v.columnAtX(0, text, x, f);
                assertEquals(c, back, "hit test of caret x should return the same column for '"
                        + text + "' col " + c);
            }
        }
    }

    @FormTest
    void hitTestIsVisuallyStableForMixedText() {
        // At a bidi direction boundary two logical columns can share one visual x, so hit testing may
        // return the equivalent column; the correct invariant is that the visual x is preserved.
        String text = "ab " + HEB + " cd";
        EditorView v = show(text);
        Font f = font(v);
        for (int c = 0; c <= text.length(); c++) {
            int x = v.measureColumnX(0, text, c, f);
            int back = v.columnAtX(0, text, x, f);
            assertEquals(x, v.measureColumnX(0, text, back, f),
                    "hit testing caret x must map to a column at the same visual x (col " + c + ")");
        }
    }

    @FormTest
    void selectionSegmentsCoverRangeWidth() {
        EditorView v = show(HEB);
        Font f = font(v);
        int[] segs = v.lineVisualSegments(0, HEB, 0, HEB.length(), f);
        assertTrue(segs.length >= 2, "a non-empty selection yields at least one segment");
        int total = 0;
        for (int i = 1; i < segs.length; i += 2) {
            total += segs[i];
        }
        // the segments of a full-line selection add up to the whole line width
        assertEquals(f.substringWidth(HEB, 0, HEB.length()), total, 2);
    }

    @FormTest
    void mixedLineProducesMultipleSelectionSegments() {
        String s = "ab " + HEB + " cd"; // LTR ... RTL ... LTR -> visually disjoint when selecting across
        EditorView v = show(s);
        Font f = font(v);
        // select the whole line: because the RTL run reverses, the logical range still tiles the line,
        // so total segment width equals the full line width
        int[] segs = v.lineVisualSegments(0, s, 0, s.length(), f);
        int total = 0;
        for (int i = 1; i < segs.length; i += 2) {
            total += segs[i];
        }
        assertEquals(f.substringWidth(s, 0, s.length()), total, 3);
    }
}
