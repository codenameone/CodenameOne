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
package com.codename1.wordprocessing;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Floating pictures and text wrap, columns, sections, headers and footers, tab stops and
/// indents. Every test checks the geometry it makes: no two words overlap, nothing leaves its
/// page, and no word sits inside the box of a picture it should wrap around.
class PageFeaturesTest extends UITestBase {
    /// Letters of uneven width, wider when bold, so nothing passes by being uniform.
    static final Measurer UNEVEN = new Measurer() {
        @Override
        public float width(String text, TextStyle style) {
            float w = 0;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                float f = c == 'i' || c == 'l' || c == '.' ? 0.25f : c == 'm' || c == 'w' ? 0.8f
                        : c == ' ' ? 0.3f : 0.5f;
                w += f * style.size;
            }
            return style.bold ? w * 1.1f : w;
        }

        @Override
        public float ascent(TextStyle style) {
            return style.size * 0.8f;
        }

        @Override
        public float descent(TextStyle style) {
            return style.size * 0.2f;
        }
    };

    static final PageSpec PAGE = new PageSpec(300, 400, 20, 20, 20, 20);
    static final String LOREM = "lorem ipsum dolor sit amet consectetur adipiscing elit sed do "
            + "eiusmod tempor incididunt ut labore et dolore magna aliqua ut enim ad minim veniam "
            + "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat";

    static Picture picture(String id, float w, float h) {
        return new Picture(id, new byte[] {1, 2, 3}, "image/png", w, h, null);
    }

    static Paragraph para(String id, String text) {
        return DocumentLayoutTest.para(id, text);
    }

    static Paragraph withFloat(String id, Picture pic, String text) {
        RichText t = new RichText.Builder().append(pic, TextStyle.DEFAULT)
                .append(text, TextStyle.DEFAULT).build();
        return para(id, "").withText(t);
    }

    static DocumentLayout layout(Document d) {
        return new DocumentLayout(d, UNEVEN);
    }

    static Document doc(PageSpec page, Block... blocks) {
        return new Document(new ArrayList<Block>(Arrays.asList(blocks)), page, null, 1000);
    }

    static List<LayoutItem> words(LayoutPage p) {
        List<LayoutItem> out = new ArrayList<LayoutItem>();
        for (LayoutItem i : p.getItems()) {
            if (i.kind == LayoutItem.TEXT && i.text.trim().length() > 0) {
                out.add(i);
            }
        }
        return out;
    }

    static LayoutItem pictureItem(LayoutPage p) {
        for (LayoutItem i : p.getItems()) {
            if (i.kind == LayoutItem.PICTURE) {
                return i;
            }
        }
        return null;
    }

    /// The ink of a word: its trailing spaces are not.
    static float[] box(LayoutItem i) {
        float w = UNEVEN.width(i.text.trim(), i.style);
        float top = i.baseline - UNEVEN.ascent(i.style);
        return new float[] {i.x, top, i.x + w, i.baseline + UNEVEN.descent(i.style)};
    }

    static boolean overlaps(float[] a, float[] b) {
        return a[0] < b[2] - 0.01f && b[0] < a[2] - 0.01f && a[1] < b[3] - 0.01f
                && b[1] < a[3] - 0.01f;
    }

    /// No word overlaps another, and every word is inside its page.
    static void assertSane(DocumentLayout l) {
        for (int p = 0; p < l.getPageCount(); p++) {
            LayoutPage page = l.getPage(p);
            List<LayoutItem> w = words(page);
            for (int a = 0; a < w.size(); a++) {
                float[] ba = box(w.get(a));
                assertTrue(ba[0] >= -0.01f && ba[2] <= page.getWidth() + 0.01f
                        && ba[1] >= -0.01f && ba[3] <= page.getHeight() + 0.01f,
                        "outside page " + p + ": " + w.get(a).text);
                for (int b = a + 1; b < w.size(); b++) {
                    assertTrue(!overlaps(ba, box(w.get(b))), "page " + p + ": '"
                            + w.get(a).text + "' overlaps '" + w.get(b).text + "'");
                }
            }
        }
    }

    static void assertClearOf(LayoutPage page, LayoutItem pic) {
        float[] pb = {pic.x, pic.y, pic.x + pic.width, pic.y + pic.height};
        for (LayoutItem w : words(page)) {
            assertTrue(!overlaps(box(w), pb), "'" + w.text + "' is inside the picture");
        }
    }

    @Test
    void squareFloatOnTheRightWrapsTheTextBesideIt() {
        Picture pic = picture("i1", 100, 80).withWrap(Picture.SQUARE, Picture.RIGHT, 0, 6);
        DocumentLayout l = layout(doc(PAGE, withFloat("p1", pic, LOREM), para("p2", LOREM)));
        assertSane(l);
        LayoutItem pi = pictureItem(l.getPage(0));
        assertEquals(PAGE.width - PAGE.right - 100, pi.x, 0.01);
        assertClearOf(l.getPage(0), pi);
        boolean beside = false;
        boolean below = false;
        for (LayoutItem w : words(l.getPage(0))) {
            float[] b = box(w);
            if (b[3] <= pi.y + pi.height) {
                beside = true;
                assertTrue(b[2] <= pi.x - 6 + 0.01f, "'" + w.text + "' ignores the gap");
            } else if (b[2] > pi.x + 1) {
                below = true;
            }
        }
        assertTrue(beside && below, "text runs beside the picture and then full width");
    }

    @Test
    void squareFloatOnTheLeftMovesTheLinesRight() {
        Picture pic = picture("i1", 60, 60).withWrap(Picture.SQUARE, Picture.LEFT, 30, 4);
        DocumentLayout l = layout(doc(PAGE, withFloat("p1", pic, LOREM)));
        assertSane(l);
        LayoutItem pi = pictureItem(l.getPage(0));
        assertEquals(PAGE.left, pi.x, 0.01);
        assertClearOf(l.getPage(0), pi);
        float firstX = words(l.getPage(0)).get(0).x;
        assertEquals(PAGE.left, firstX, 0.01, "the first line is above the picture");
    }

    @Test
    void topAndBottomKeepsTheSidesEmpty() {
        Picture pic = picture("i1", 80, 50).withWrap(Picture.TOP_BOTTOM, Picture.CENTER, 0, 4);
        DocumentLayout l = layout(doc(PAGE, para("p0", "before"), withFloat("p1", pic, LOREM)));
        assertSane(l);
        LayoutItem pi = pictureItem(l.getPage(0));
        assertEquals((PAGE.width - 80) / 2, pi.x, 0.01);
        for (LayoutItem w : words(l.getPage(0))) {
            float[] b = box(w);
            assertTrue(b[3] <= pi.y + 0.01f || b[1] >= pi.y + pi.height - 0.01f,
                    "'" + w.text + "' sits beside a top-and-bottom picture");
        }
    }

    @Test
    void behindAndInFrontDoNotMoveText() {
        Picture behind = picture("i1", 100, 100).withWrap(Picture.BEHIND, Picture.LEFT, 0, 0);
        DocumentLayout plain = layout(doc(PAGE, para("p1", LOREM)));
        DocumentLayout l = layout(doc(PAGE, withFloat("p1", behind, LOREM)));
        assertEquals(LayoutItem.PICTURE, l.getPage(0).getItems().get(0).kind,
                "a picture behind the text is painted first");
        assertEquals(words(plain.getPage(0)).size(), words(l.getPage(0)).size());
        assertEquals(words(plain.getPage(0)).get(3).x, words(l.getPage(0)).get(3).x, 0.01);
        Picture front = behind.withWrap(Picture.IN_FRONT, Picture.LEFT, 0, 0);
        DocumentLayout f = layout(doc(PAGE, withFloat("p1", front, LOREM)));
        List<LayoutItem> items = f.getPage(0).getItems();
        assertNotEquals(LayoutItem.PICTURE, items.get(0).kind);
    }

    @Test
    void aFloatingBlockPictureWrapsTheFollowingParagraphs() {
        Picture pic = picture("i1", 90, 120).withWrap(Picture.SQUARE, Picture.LEFT, 0, 6);
        DocumentLayout l = layout(doc(PAGE, pic, para("p1", LOREM), para("p2", LOREM)));
        assertSane(l);
        LayoutItem pi = pictureItem(l.getPage(0));
        assertClearOf(l.getPage(0), pi);
        assertTrue(words(l.getPage(0)).get(0).x >= pi.x + pi.width + 6 - 0.01f);
    }

    @Test
    void aTableStartsBelowAFloat() {
        Picture pic = picture("i1", 60, 100).withWrap(Picture.SQUARE, Picture.LEFT, 0, 6);
        DocumentLayout l = layout(doc(PAGE, withFloat("p1", pic, "short"),
                DocumentLayoutTest.table(2, 2, 0)));
        LayoutItem pi = pictureItem(l.getPage(0));
        for (LayoutItem it : l.getPage(0).getItems()) {
            if (it.kind == LayoutItem.LINE) {
                assertTrue(it.y >= pi.y + pi.height - 0.01f, "table border inside the float");
            }
        }
    }

    @Test
    void columnsFillLeftToRightThenTheNextPage() {
        PageSpec two = PAGE.withColumns(2, 20);
        assertEquals(2, two.columns);
        assertEquals((260 - 20) / 2f, two.columnWidth(), 0.01);
        List<Block> b = new ArrayList<Block>();
        for (int i = 0; i < 12; i++) {
            b.add(para("p" + i, LOREM));
        }
        DocumentLayout l = layout(new Document(b, two, null, 1000));
        assertSane(l);
        assertTrue(l.getPageCount() >= 2);
        boolean left = false;
        boolean right = false;
        for (LayoutItem w : words(l.getPage(0))) {
            float[] bx = box(w);
            if (bx[2] <= PAGE.left + two.columnWidth() + 0.01f) {
                left = true;
            } else {
                assertTrue(bx[0] >= PAGE.left + two.columnWidth() + 20 - 0.01f,
                        "'" + w.text + "' is in the gutter");
                right = true;
            }
        }
        assertTrue(left && right);
    }

    @Test
    void hostileColumnCountsAreReduced() {
        PageSpec many = new PageSpec(100, 200, 10, 10, 10, 10).withColumns(50, 200);
        assertTrue(many.columns >= 1 && many.columnWidth() >= 36 || many.columns == 1);
        DocumentLayout l = layout(doc(many, para("p", LOREM)));
        assertSane(l);
    }

    @Test
    void aContinuousSectionBalancesItsColumns() {
        PageSpec two = PAGE.withColumns(2, 20);
        Paragraph head = para("h", "Heading across the page");
        DocumentLayout l = layout(doc(PAGE, head, new SectionBreak("s1", two, false),
                para("a", LOREM), new SectionBreak("s2", PAGE, false), para("z", "after")));
        assertSane(l);
        assertEquals(1, l.getPageCount());
        float leftBottom = 0;
        float rightBottom = 0;
        float zTop = 0;
        for (LayoutItem w : words(l.getPage(0))) {
            float[] bx = box(w);
            if ("z".equals(w.paragraphId)) {
                zTop = bx[1];
            } else if ("a".equals(w.paragraphId)) {
                if (bx[0] < PAGE.left + two.columnWidth()) {
                    leftBottom = Math.max(leftBottom, bx[3]);
                } else {
                    rightBottom = Math.max(rightBottom, bx[3]);
                }
            }
        }
        assertTrue(rightBottom > 0, "the section uses both columns");
        assertTrue(Math.abs(leftBottom - rightBottom) <= 14, "columns are balanced");
        assertTrue(zTop >= Math.max(leftBottom, rightBottom) - 0.01f);
    }

    @Test
    void aNewPageSectionCanTurnThePage() {
        PageSpec wide = PAGE.rotated();
        DocumentLayout l = layout(doc(PAGE, para("a", "portrait"),
                new SectionBreak("s1", wide, true), para("b", "landscape")));
        assertEquals(2, l.getPageCount());
        assertEquals(300, l.getPage(0).getWidth(), 0.01);
        assertEquals(400, l.getPage(1).getWidth(), 0.01);
        assertEquals(300, l.getPage(1).getHeight(), 0.01);
        assertSane(l);
    }

    static Paragraph pageNumber(String id, String prefix) {
        RichText t = new RichText.Builder().append(prefix, TextStyle.DEFAULT)
                .append(new Field("PAGE", "1", false), TextStyle.DEFAULT)
                .append(" of ", TextStyle.DEFAULT)
                .append(new Field("NUMPAGES", "1", false), TextStyle.DEFAULT).build();
        return para(id, "").withText(t);
    }

    @Test
    void headersAndFootersRepeatWithTheirPageNumbers() {
        List<Block> b = new ArrayList<Block>();
        for (int i = 0; i < 24; i++) {
            b.add(para("p" + i, LOREM));
        }
        Document d = new Document(b, PAGE, null, 1000).withHeaders(
                Arrays.asList((Block) para("h", "Report")),
                Arrays.asList((Block) pageNumber("f", "Page ")),
                Arrays.asList((Block) para("h1", "Cover")), null);
        DocumentLayout l = layout(d);
        assertSane(l);
        assertTrue(l.getPageCount() >= 3);
        String count = String.valueOf(l.getPageCount());
        for (int p = 0; p < l.getPageCount(); p++) {
            StringBuilder head = new StringBuilder();
            StringBuilder foot = new StringBuilder();
            float bodyTop = Float.MAX_VALUE;
            float headBottom = 0;
            for (LayoutItem w : words(l.getPage(p))) {
                if (w.paragraphId == null) {
                    if (w.y < 100) {
                        head.append(w.text);
                        headBottom = Math.max(headBottom, box(w)[3]);
                    } else {
                        foot.append(w.text);
                    }
                } else {
                    bodyTop = Math.min(bodyTop, box(w)[1]);
                }
            }
            assertEquals(p == 0 ? "Cover" : "Report", head.toString().trim());
            assertEquals("Page" + (p + 1) + "of" + count, foot.toString().replace(" ", ""));
            assertTrue(bodyTop >= headBottom, "the body starts below the header");
        }
        // the caret never lands in a header
        assertNull(l.positionAt(0, 0, 0) == null ? null
                : l.positionAt(0, 25, 12).paragraphId.equals("h1") ? "header" : null);
    }

    @Test
    void rightTabStopsWithLeadersEndTheTextAtTheStop() {
        Paragraph p = para("p", "Intro\t12").withTabs(Arrays.asList(
                new TabStop(200, TabStop.RIGHT, '.')));
        DocumentLayout l = layout(doc(PAGE, p));
        LayoutItem num = null;
        LayoutItem dots = null;
        for (LayoutItem w : words(l.getPage(0))) {
            if ("12".equals(w.text)) {
                num = w;
            } else if (w.paragraphId == null) {
                dots = w;
            }
        }
        assertEquals(PAGE.left + 200, num.x + UNEVEN.width("12", num.style), 0.5);
        assertTrue(dots != null && dots.text.startsWith("..."));
        assertSane(l);
        Paragraph c = para("c", "a\tmid").withTabs(Arrays.asList(
                new TabStop(100, TabStop.CENTER, (char) 0)));
        LayoutItem mid = words(layout(doc(PAGE, c)).getPage(0)).get(1);
        assertEquals(PAGE.left + 100, mid.x + UNEVEN.width("mid", mid.style) / 2, 0.5);
    }

    @Test
    void hangingIndentAndRightIndent() {
        Paragraph p = para("p", LOREM).withIndents(40, -30, 50);
        DocumentLayout l = layout(doc(PAGE, p));
        List<LayoutItem> w = words(l.getPage(0));
        assertEquals(PAGE.left + 10, w.get(0).x, 0.01);
        float secondLineX = Float.MAX_VALUE;
        for (LayoutItem i : w) {
            if (i.baseline > w.get(0).baseline + 1) {
                secondLineX = Math.min(secondLineX, i.x);
            }
            assertTrue(box(i)[2] <= PAGE.width - PAGE.right - 50 + 0.01f);
        }
        assertEquals(PAGE.left + 40, secondLineX, 0.01);
    }

    @Test
    void headingStylesLookLikeHeadings() {
        Paragraph h = para("h", "Title").withStyle("Heading1");
        LayoutItem w = words(layout(doc(PAGE, h)).getPage(0)).get(0);
        assertEquals(24, w.style.size, 0.01);
        assertTrue(w.style.bold);
        Paragraph direct = para("d", "").withText(RichText.of("Small",
                TextStyle.DEFAULT.withSize(9))).withStyle("Heading1");
        assertEquals(9, words(layout(doc(PAGE, direct)).getPage(0)).get(0).style.size, 0.01);
    }

    @Test
    void hostileFloatsStillTerminate() {
        Picture huge = picture("i1", 5000, 9000).withWrap(Picture.SQUARE, Picture.CENTER, 1e9f,
                1e9f);
        assertEquals(2000, huge.offsetY, 0.01);
        assertEquals(72, huge.distance, 0.01);
        Picture wide = picture("i2", 5000, 50).withWrap(Picture.SQUARE, Picture.LEFT, 0, 0);
        DocumentLayout l = layout(doc(PAGE, withFloat("p1", huge, LOREM),
                withFloat("p2", wide, LOREM), para("p3", LOREM)));
        assertSane(l);
        assertTrue(l.getPageCount() < 20);
    }
}
