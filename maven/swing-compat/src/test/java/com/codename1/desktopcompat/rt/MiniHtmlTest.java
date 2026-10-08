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
package com.codename1.desktopcompat.rt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.image.BufferedImage;
import org.junit.Test;

/// The HTML reader: the runs and lines it makes, the sizes it measures,
/// and that nothing it is given makes it throw.
public class MiniHtmlTest extends KernelTestBase {

    private static final Font FONT = new Font(Font.DIALOG, Font.PLAIN, 12);

    @Test
    public void onlyATextStartingWithTheHtmlTagIsHtml() {
        assertTrue(MiniHtml.isHtml("<html>x"));
        assertTrue(MiniHtml.isHtml("<HTML>x"));
        assertTrue(MiniHtml.isHtml("<HtMl>"));
        assertFalse(MiniHtml.isHtml(" <html>x"));
        assertFalse(MiniHtml.isHtml("<b>x</b>"));
        assertFalse(MiniHtml.isHtml("<htm"));
        assertFalse(MiniHtml.isHtml(null));
    }

    @Test
    public void stylesBecomeRuns() {
        MiniHtml.Document d = MiniHtml.parse("<html><b>Bold</b> plain <i>it</i><u>under</u></html>");
        assertEquals(1, d.lineCount());
        MiniHtml.Line l = d.line(0);
        assertEquals(4, l.runCount());
        assertEquals("Bold", l.run(0).text());
        assertTrue(l.run(0).bold());
        assertFalse(l.run(0).italic());
        assertEquals(" plain ", l.run(1).text());
        assertFalse(l.run(1).bold());
        assertEquals("it", l.run(2).text());
        assertTrue(l.run(2).italic());
        assertFalse(l.run(2).underline());
        assertEquals("under", l.run(3).text());
        assertTrue(l.run(3).underline());
        assertFalse(l.run(3).italic());
        assertEquals("Bold plain itunder", l.text());
        assertEquals(MiniHtml.ALIGN_DEFAULT, l.alignment());
    }

    @Test
    public void stylesNest() {
        MiniHtml.Document d = MiniHtml.parse("<html><b>a<i>b</i>c</b>d<strong><em>e</em></strong>");
        MiniHtml.Line l = d.line(0);
        assertEquals(5, l.runCount());
        assertTrue(l.run(0).bold() && !l.run(0).italic());
        assertTrue(l.run(1).bold() && l.run(1).italic());
        assertTrue(l.run(2).bold() && !l.run(2).italic());
        assertFalse(l.run(3).bold());
        assertTrue(l.run(4).bold() && l.run(4).italic());
    }

    @Test
    public void breaksAndParagraphsMakeLines() {
        MiniHtml.Document d = MiniHtml.parse("<html>one<br>two<BR/>\n  three<br><br>five<p>six</p>seven<p>eight");
        assertEquals(8, d.lineCount());
        assertEquals("one", d.line(0).text());
        assertEquals("two", d.line(1).text());
        assertEquals("three", d.line(2).text());
        assertEquals("", d.line(3).text());
        assertEquals(0, d.line(3).runCount());
        assertEquals("five", d.line(4).text());
        assertEquals("six", d.line(5).text());
        assertTrue(d.line(5).gapBefore());
        assertFalse(d.line(4).gapBefore());
        assertEquals("seven", d.line(6).text());
        assertTrue(d.line(6).gapBefore());
        assertEquals("eight", d.line(7).text());
        assertEquals("one\ntwo\nthree\n\nfive\nsix\nseven\neight",
                MiniHtml.plainText("<html>one<br>two<BR/>\n  three<br><br>five<p>six</p>seven<p>eight"));
    }

    @Test
    public void whiteSpaceCollapsesAndEntitiesAreRead() {
        MiniHtml.Document d = MiniHtml.parse("<html>  a \n\t b&amp;c &lt;d&gt; &quot;e&quot;&nbsp;&nbsp;f &#65;&#x42; </html>");
        assertEquals(1, d.lineCount());
        assertEquals("a b&c <d> \"e\"  f AB", d.line(0).text());
        assertEquals("x & y &bogus; &", MiniHtml.plainText("<html>x & y &bogus; &"));
    }

    @Test
    public void fontColorsAreRead() {
        MiniHtml.Document d = MiniHtml.parse("<html><font color=\"red\">r</font><font color=#00ff00>g</font>"
                + "<FONT COLOR='#00F'>b</FONT><span style=\"font-weight: bold; color: rgb(1, 2, 3)\">s</span>n"
                + "<font color=nonsense>x</font>");
        MiniHtml.Line l = d.line(0);
        assertEquals(new Color(255, 0, 0), l.run(0).color());
        assertEquals(new Color(0, 255, 0), l.run(1).color());
        assertEquals(new Color(0, 0, 255), l.run(2).color());
        assertEquals(new Color(1, 2, 3), l.run(3).color());
        assertEquals("nx", l.run(4).text());
        assertNull(l.run(4).color());
        assertNull(MiniHtml.color(null));
        assertNull(MiniHtml.color("#12"));
        assertEquals(new Color(0x80, 0x80, 0x80), MiniHtml.color("Gray"));
    }

    @Test
    public void centerAndAlignSetTheLinesAlignment() {
        MiniHtml.Document d = MiniHtml.parse("<html>left<center>mid<br>dle</center>after<p align=right>r</p>"
                + "<div align=\"CENTER\">c</div>");
        assertEquals(6, d.lineCount());
        assertEquals(MiniHtml.ALIGN_DEFAULT, d.line(0).alignment());
        assertEquals(MiniHtml.ALIGN_CENTER, d.line(1).alignment());
        assertEquals(MiniHtml.ALIGN_CENTER, d.line(2).alignment());
        assertEquals("after", d.line(3).text());
        assertEquals(MiniHtml.ALIGN_DEFAULT, d.line(3).alignment());
        assertEquals(MiniHtml.ALIGN_RIGHT, d.line(4).alignment());
        assertEquals(MiniHtml.ALIGN_CENTER, d.line(5).alignment());
    }

    @Test
    public void sizesScaleTheFont() {
        MiniHtml.Document d = MiniHtml.parse("<html><font size=5>big</font><font size=\"-1\">small</font>"
                + "<h1>head</h1>");
        assertEquals(1.5f, d.line(0).run(0).scale(), 0.001f);
        assertEquals(0.8f, d.line(0).run(1).scale(), 0.001f);
        assertEquals(2f, d.line(1).run(0).scale(), 0.001f);
        assertTrue(d.line(1).run(0).bold());
    }

    @Test
    public void headStyleAndCommentsAreDropped() {
        assertEquals("shown", MiniHtml.plainText("<html><head><title>t</title><style>b { x }</style></head>"
                + "<body><!-- not this -->shown</body></html>"));
        assertEquals("a b", MiniHtml.plainText("<html>a <unknown attr=1>b</unknown>"));
        assertEquals("not html <b>", MiniHtml.plainText("not html <b>"));
        assertEquals("a b", MiniHtml.singleLine("<html>a<br>b"));
    }

    @Test
    public void malformedInputNeverThrows() {
        String[] bad = {
            "<html><b>unclosed <i>x</b> y </zzz> <",
            "<html><",
            "<html></",
            "<html><>",
            "<html>&;&#xZZ;&#;&#99999999999;&unknown; a < b > c <font color=> <font color",
            "<html><!-- never closed",
            "<html><head>never closed",
            "<html><font color=\"unterminated>x",
            "<html></b></i></p></center></font>text",
            "<html><p><p><p></p><br><br><br>",
            "<html><font size=abc>x</font><font size=+>y</font><font size=99>z</font>",
            "<html><span style=\"color\">a</span><span style=\":;:;\">b</span>",
            "<html>&#0;&#x0;&#160;&#xFFFFF;",
            "<html><li>one<li>two</ul></ol>",
            "",
            "<",
            "&"
        };
        BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < bad.length; i++) {
            MiniHtml.Document d = MiniHtml.parse(bad[i]);
            assertNotNull(bad[i], d);
            assertNotNull(MiniHtml.plainText(bad[i]));
            Dimension size = MiniHtml.preferredSize(d, FONT);
            assertTrue(size.width >= 0 && size.height >= 0);
            assertNotNull(MiniHtml.wrap(d, FONT, 10));
            Graphics g = img.createGraphics();
            try {
                MiniHtml.paint(g, d, 0, 0, 20, MiniHtml.ALIGN_CENTER);
            } finally {
                g.dispose();
            }
        }
        assertEquals(0, MiniHtml.parse(null).lineCount());
        assertEquals("unclosed x y <", MiniHtml.plainText(bad[0]));
    }

    @Test
    public void theSizeIsTheWidestLineAndTheSumOfTheHeights() {
        Dimension one = MiniHtml.preferredSize(MiniHtml.parse("<html>abcdef"), FONT);
        Dimension two = MiniHtml.preferredSize(MiniHtml.parse("<html>abcdef<br>abc"), FONT);
        Dimension para = MiniHtml.preferredSize(MiniHtml.parse("<html>abcdef<p>abc"), FONT);
        assertTrue(one.width > 0 && one.height > 0);
        assertEquals(one.width, two.width);
        assertEquals(2 * one.height, two.height);
        assertEquals(2 * one.height + one.height / 2, para.height);
        assertEquals(Fonts.metrics(FONT).stringWidth("abcdef"), one.width);
    }

    @Test
    public void aLineWiderThanTheWidthWrapsAtSpaces() {
        MiniHtml.Document d = MiniHtml.parse("<html>aaaa bbbb <b>cccc dddd</b><br>ee");
        int word = Fonts.metrics(FONT).stringWidth("aaaa bbbb");
        MiniHtml.Document w = MiniHtml.wrap(d, FONT, word + 2);
        assertEquals(3, w.lineCount());
        assertEquals("aaaa bbbb ", w.line(0).text());
        assertEquals("cccc dddd", w.line(1).text());
        assertTrue(w.line(1).run(0).bold());
        assertEquals("ee", w.line(2).text());
        assertSame(d, MiniHtml.wrap(d, FONT, 10000));
        assertSame(d, MiniHtml.wrap(d, FONT, 0));
        // A word wider than the width stays whole on a line of its own.
        MiniHtml.Document narrow = MiniHtml.wrap(d, FONT, 1);
        assertEquals(5, narrow.lineCount());
        assertEquals("aaaa ", narrow.line(0).text());
    }
}
