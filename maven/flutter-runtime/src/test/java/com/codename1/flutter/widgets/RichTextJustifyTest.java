/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.widgets;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless tests of {@link JustifiedRichTextComponent#layout}: the line breaking and the
 * distribution of a justified line's slack over its inter-word gaps.
 */
public class RichTextJustifyTest {

    /** Every character 10 px wide, a space 5 px, run 1 twice as tall as run 0. */
    private static final JustifiedRichTextComponent.Measure M = new JustifiedRichTextComponent.Measure() {
        @Override
        public int wordWidth(int run, String text) {
            return text.length() * 10;
        }

        @Override
        public int spaceWidth(int run) {
            return 5;
        }

        @Override
        public int height(int run) {
            return run == 1 ? 20 : 10;
        }
    };

    private static JustifiedRichTextComponent.Result lay(int avail, boolean stretch, String... runs) {
        return JustifiedRichTextComponent.layout(Arrays.asList(runs), M, avail, 10, 2, 4, stretch);
    }

    private static int right(JustifiedRichTextComponent.Line l) {
        JustifiedRichTextComponent.Frag f = l.frags.get(l.frags.size() - 1);
        return f.x + M.wordWidth(f.run, f.text);
    }

    @Test
    public void nonFinalLinesReachTheRightEdge() {
        // "aa bb cc" = 20+5+20+5+20 = 70 fits 75; "dd" wraps
        JustifiedRichTextComponent.Result r = lay(75, true, "aa bb cc dd");
        assertEquals(2, r.lines.size());
        JustifiedRichTextComponent.Line first = r.lines.get(0);
        assertEquals(75, right(first));
        // slack 5 over 2 gaps: 3 to the first, 2 to the second
        assertEquals(0, first.frags.get(0).x);
        assertEquals(28, first.frags.get(1).x);
        assertEquals(55, first.frags.get(2).x);
        assertTrue(r.lines.get(1).last);
        assertEquals(0, r.lines.get(1).frags.get(0).x);
        assertEquals(75, r.width);
    }

    @Test
    public void lastLineOfEachParagraphIsNotStretched() {
        JustifiedRichTextComponent.Result r = lay(75, true, "aa bb\ncc dd ee ff");
        assertEquals(3, r.lines.size());
        assertTrue(r.lines.get(0).last);
        assertEquals(45, right(r.lines.get(0)));
        assertEquals(75, right(r.lines.get(1)));
        assertEquals(20, right(r.lines.get(2)));
        // y: line 0 at 0, then height 10 + gap 2 + paragraph gap 4
        assertEquals(16, r.lines.get(1).y);
    }

    @Test
    public void wordSpanningRunsMovesAsOneAndHeightIsTheTallestRun() {
        JustifiedRichTextComponent.Result r = lay(60, true, "ab c", "d", "e ff gg");
        JustifiedRichTextComponent.Line first = r.lines.get(0);
        // "ab" | "c"+"d"+"e" (one word over three runs) = 20+5+30 = 55, slack 5 on the one gap
        assertEquals(4, first.frags.size());
        assertEquals(25 + 5, first.frags.get(1).x);
        assertEquals(35 + 5, first.frags.get(2).x);
        assertEquals(45 + 5, first.frags.get(3).x);
        assertEquals(20, first.height);
    }

    @Test
    public void singleWordLineAndNoStretchStayAtStart() {
        List<JustifiedRichTextComponent.Line> ls = lay(30, true, "aaaa bb").lines;
        assertEquals(0, ls.get(0).frags.get(0).x);
        assertEquals(40, ls.get(0).width);
        JustifiedRichTextComponent.Result plain = lay(75, false, "aa bb cc dd");
        assertEquals(70, right(plain.lines.get(0)));
    }
}
