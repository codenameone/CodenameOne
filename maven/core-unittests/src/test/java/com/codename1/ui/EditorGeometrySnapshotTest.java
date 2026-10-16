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

package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.editor.CodeView;
import com.codename1.ui.editor.EditorView;
import com.codename1.ui.layouts.BorderLayout;
import static org.junit.jupiter.api.Assertions.*;

class EditorGeometrySnapshotTest extends UITestBase {
    private static class CountingFont extends Font {
        int bulkCalls;
        int prefixCalls;
        int measuredUnits;
        @Override public int getHeight() { return 20; }
        @Override public int charWidth(char c) { return 1; }
        @Override public int stringWidth(String s) { return s.length(); }
        @Override public int substringWidth(String s, int start, int length) {
            prefixCalls++;
            measuredUnits += length;
            return length;
        }
        @Override public int[] getTextRunPositions(String text, boolean rtl) {
            bulkCalls++;
            measuredUnits += text.length();
            int[] positions = new int[text.length() + 1];
            for (int i = 0; i < positions.length; i++) positions[i] = rtl ? text.length() - i : i;
            return positions;
        }
        void reset() { bulkCalls = prefixCalls = measuredUnits = 0; }
    }

    @FormTest
    void longEditorAndCodeLinesUseOneBulkMeasurementInsteadOfThousandsOfPrefixes() {
        for (boolean code : new boolean[]{false, true}) {
            final CountingFont font = new CountingFont();
            EditorView editor = code ? new CodeView(null) {
                @Override protected Font getEditorFont() { return font; }
            } : new EditorView(null, false) {
                @Override protected Font getEditorFont() { return font; }
            };
            Form form = new Form(new BorderLayout());
            form.add(BorderLayout.CENTER, editor);
            form.show();
            flushSerialCalls();
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < 4096; i++) text.append('x');
            editor.setText(text.toString());
            editor.setWidth(5000);
            editor.setSelectionRange(2048, 2048);
            font.reset();
            TextInputGeometry geometry = TextInputGeometry.capture(editor);
            assertEquals(1, font.bulkCalls);
            assertTrue(font.prefixCalls <= 3, "only caret/selection endpoints may use scalar measurement");
            assertTrue(font.measuredUnits < 4096 * 4, "work must grow linearly in the line length");
            assertArrayEquals(editor.rectForOffset(2048), geometry.rectForOffset(2048));
            assertEquals(2048, geometry.offsetAtPoint(geometry.rectForOffset(2048)[0],
                    geometry.rectForOffset(2048)[1] + 5));
        }
    }

    @FormTest
    void bulkSnapshotsPreserveBidiAndStyledCaretGeometry() {
        for (EditorView editor : new EditorView[]{new EditorView(null, false),
                new CodeView(null), new com.codename1.ui.editor.RichView(null)}) {
            Form form = new Form(new BorderLayout());
            form.add(BorderLayout.CENTER, editor);
            form.show();
            flushSerialCalls();
            for (String text : new String[]{"abc def", "\u05d0\u05d1\u05d2", "abc \u05d0\u05d1 def"}) {
                editor.setText(text);
                if (editor instanceof com.codename1.ui.editor.RichView) {
                    editor.setSelectionRange(1, Math.min(3, text.length()));
                    ((com.codename1.ui.editor.RichView) editor).toggleBold();
                }
                TextInputGeometry geometry = TextInputGeometry.capture(editor);
                for (int i = 0; i <= text.length(); i++) {
                    assertArrayEquals(editor.rectForOffset(i), geometry.rectForOffset(i),
                            editor.getClass().getName() + " offset " + i);
                }
            }
        }
    }
    @FormTest
    void largeSelectionsOnlyMeasureVisibleLinesInEitherDirection() {
        final int[] measured = new int[1];
        EditorView[] editors = {new EditorView(null, false) {
            @Override protected int[] lineVisualSegments(int line, String text, int a, int b, Font font) {
                measured[0]++;
                return super.lineVisualSegments(line, text, a, b, font);
            }
        }, new com.codename1.ui.editor.RichView(null) {
            @Override protected int[] lineVisualSegments(int line, String text, int a, int b, Font font) {
                measured[0]++;
                return super.lineVisualSegments(line, text, a, b, font);
            }
        }};
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 10000; i++) text.append("abc\n");
        for (EditorView editor : editors) {
            editor.setWidth(300);
            editor.setHeight(100);
            editor.setText(text.toString());
            for (boolean reverse : new boolean[]{false, true}) {
                int end = editor.getTextLength();
                editor.setSelectionRange(reverse ? end : 0, reverse ? 0 : end);
                measured[0] = 0;
                TextInputGeometry geometry = TextInputGeometry.capture(editor);
                int[] rectangles = geometry.selectionRects(0, end);
                assertTrue(measured[0] < 30, "capture must not walk 10,000 selected lines");
                assertTrue(rectangles.length > 0 && rectangles.length < 120);
                for (int r = 0; r < rectangles.length; r += 4) {
                    assertTrue(rectangles[r + 1] + rectangles[r + 3] >= editor.getAbsoluteY());
                    assertTrue(rectangles[r + 1] <= editor.getAbsoluteY() + editor.getHeight());
                }
            }
        }
    }

    private static class ScrolledClient extends Container implements TextInputClient {
        final java.util.List<int[]> samples = new java.util.ArrayList<int[]>();
        void scroll(int x, int y) { setScrollX(x); setScrollY(y); }
        @Override protected com.codename1.ui.geom.Dimension calcScrollSize() {
            return new com.codename1.ui.geom.Dimension(2000, 2000);
        }
        @Override public TextInputState getEditingState() { return new TextInputState("", 600, 600); }
        @Override public int getTextLength() { return 2000; }
        @Override public int offsetAtPoint(int x, int y) { samples.add(new int[]{x, y}); return y; }
        @Override public int[] rectForOffset(int offset) { return new int[]{getAbsoluteX(), offset, 1, 1}; }
        @Override public int[] getCaretRect() { return rectForOffset(600); }
        @Override public int[] selectionRects(int a, int b) { return new int[0]; }
        @Override public TextInputConfig getConfig() { return new TextInputConfig(); }
        @Override public String getTextRange(int a, int b) { return ""; }
        @Override public void commitText(String t) { }
        @Override public void setComposingText(String t, int c) { }
        @Override public void finishComposing() { }
        @Override public void deleteSurroundingText(int a, int b) { }
        @Override public void onKeyCommand(int c, int m) { }
        @Override public void onEditorAction(int a) { }
        @Override public void inputFocusGained() { }
        @Override public void inputFocusLost() { }
        @Override public void replaceRange(int a, int b, String t) { }
        @Override public void setSelectionRange(int a, int b) { }
    }

    @FormTest
    void fallbackSamplesViewportAfterOwnAndAncestorScrolling() {
        ScrolledClient client = new ScrolledClient();
        ScrolledClient ancestor = new ScrolledClient();
        ancestor.setWidth(1000);
        ancestor.setHeight(1000);
        ancestor.setScrollableX(true);
        ancestor.setScrollableY(true);
        ancestor.scroll(10, 15);
        ancestor.setX(20);
        ancestor.setY(30);
        ancestor.add(client);
        client.setX(40);
        client.setY(50);
        client.setWidth(100);
        client.setHeight(80);
        client.setScrollableX(true);
        client.setScrollableY(true);
        client.scroll(70, 400);
        TextInputGeometry.capture(client);
        assertArrayEquals(new int[]{50, 65}, client.samples.get(0));
        assertArrayEquals(new int[]{150, 145}, client.samples.get(3));
    }

    @FormTest
    void fallbackCapsGeometryAroundBackwardSelectionFocus() {
        final int[] range = {-1, -1};
        ScrolledClient client = new ScrolledClient() {
            @Override public TextInputState getEditingState() { return new TextInputState("", 9000, 100); }
            @Override public int getTextLength() { return 10000; }
            @Override public int offsetAtPoint(int x, int y) { return y == 0 ? 0 : 9999; }
            @Override public int[] rectForOffset(int offset) { return new int[]{offset, 0, 1, 20}; }
            @Override public int[] getCaretRect() { return new int[]{-1, -1, 1, 20}; }
            @Override public int[] selectionRects(int a, int b) { range[0] = a; range[1] = b; return new int[0]; }
        };
        client.setWidth(100);
        client.setHeight(100);
        TextInputGeometry geometry = TextInputGeometry.capture(client);
        assertArrayEquals(new int[]{100, 0, 1, 20}, geometry.rectForOffset(100));
        assertArrayEquals(new int[]{-1, -1, 1, 20}, geometry.rectForOffset(9000));
        assertEquals(100, geometry.offsetAtPoint(100, 0));
        assertArrayEquals(new int[]{100, 9000}, range);
    }

}
