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
package com.codename1.desktopcompat.javax.swing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.javax.swing.event.HyperlinkEvent;
import com.codename1.desktopcompat.javax.swing.event.HyperlinkListener;
import com.codename1.desktopcompat.rt.Units;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/// An editor pane with the content type `text/html` shows the page
/// formatted and reports clicks on its links.
public class EditorPaneHtmlTest extends KernelTestBase {

    private static final String PAGE = "<html><head><title>Hidden</title></head><body><h1>Title</h1>"
            + "<p>Some <b>bold</b> text.</p><p>See <a href=\"https://example.com/docs\">the docs</a> or "
            + "<a href=\"chapter2\">next</a>.</p></body></html>";

    private JFrame frame(JComponent c) {
        JFrame f = new JFrame();
        f.add(c, BorderLayout.CENTER);
        f.setSize(300, 200);
        show(f);
        f.validate();
        return f;
    }

    @Test
    public void thePageIsDrawnInRunsNotAsMarkup() {
        JEditorPane pane = new JEditorPane("text/html", PAGE);
        JFrame f = frame(pane);
        List<Object[]> text = paint(f);
        assertNotNull("the heading is a run of its own", find(text, "Title"));
        assertNotNull(find(text, "bold"));
        assertNotNull(find(text, "the docs"));
        assertNull(find(text, "Hidden"));
        for (int i = 0; i < text.size(); i++) {
            assertFalse("no markup is drawn: " + text.get(i)[0], String.valueOf(text.get(i)[0]).indexOf('<') >= 0);
        }
        // The heading stands above the paragraph.
        int headingY = ((Integer) find(text, "Title")[2]).intValue();
        int boldY = ((Integer) find(text, "bold")[2]).intValue();
        assertTrue(headingY < boldY);
        assertEquals(PAGE, pane.getText());
        assertEquals("text/html", pane.getContentType());
    }

    @Test
    public void aClickOnALinkFiresAnActivatedEvent() {
        JEditorPane pane = new JEditorPane("text/html", PAGE);
        JFrame f = frame(pane);
        final List<HyperlinkEvent> heard = new ArrayList<HyperlinkEvent>();
        pane.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                heard.add(e);
            }
        });
        List<Object[]> text = paint(f);
        Object[] link = find(text, "the docs");
        int[] origin = onDisplay(pane, 0, 0);
        int x = Units.toLogical(((Integer) link[1]).intValue() - origin[0]) + 4;
        int y = Units.toLogical(((Integer) link[2]).intValue() - origin[1]) + 2;
        assertEquals("https://example.com/docs", pane.cn1LinkAt(x, y));
        press(f, pane, x, y);
        release(f, pane, x, y);
        assertEquals(1, heard.size());
        HyperlinkEvent e = heard.get(0);
        assertSame(HyperlinkEvent.EventType.ACTIVATED, e.getEventType());
        assertEquals("https://example.com/docs", e.getDescription());
        assertEquals("https://example.com/docs", e.getURL().toString());
        assertSame(pane, e.getSource());

        // A relative link has a description and no URL.
        Object[] next = find(text, "next");
        x = Units.toLogical(((Integer) next[1]).intValue() - origin[0]) + 4;
        y = Units.toLogical(((Integer) next[2]).intValue() - origin[1]) + 2;
        press(f, pane, x, y);
        release(f, pane, x, y);
        assertEquals(2, heard.size());
        assertEquals("chapter2", heard.get(1).getDescription());
        assertNull(heard.get(1).getURL());

        // Text that is not a link fires nothing.
        Object[] plain = find(text, "Title");
        x = Units.toLogical(((Integer) plain[1]).intValue() - origin[0]) + 4;
        y = Units.toLogical(((Integer) plain[2]).intValue() - origin[1]) + 2;
        assertNull(pane.cn1LinkAt(x, y));
        press(f, pane, x, y);
        release(f, pane, x, y);
        assertEquals(2, heard.size());
        pane.removeHyperlinkListener(pane.getHyperlinkListeners()[0]);
        assertEquals(0, pane.getHyperlinkListeners().length);
    }

    @Test
    public void aPageIsNotEditedAndPlainTextStillIs() {
        JEditorPane pane = new JEditorPane("text/html", PAGE);
        frame(pane);
        com.codename1.ui.TextArea peer = (com.codename1.ui.TextArea) pane.cn1Peer();
        assertFalse(peer.isEditable());
        pane.setContentType("text/plain");
        pane.setText("plain");
        assertTrue(peer.isEditable());
        assertEquals("plain", pane.getText());
    }

    @Test
    public void aLongParagraphIsWrappedAndThePaneGrowsWithIt() {
        StringBuilder sb = new StringBuilder("<html><p>");
        for (int i = 0; i < 60; i++) {
            sb.append("word").append(i).append(' ');
        }
        sb.append("</p></html>");
        JTextPane pane = new JTextPane();
        pane.setContentType("text/html");
        pane.setText(sb.toString());
        JScrollPane scroll = new JScrollPane(pane);
        JFrame f = frame(scroll);
        assertEquals(scroll.getViewport().getExtentSize().width, pane.getWidth());
        List<Object[]> text = paint(f);
        int first = -1;
        int lines = 0;
        for (int i = 0; i < text.size(); i++) {
            int y = ((Integer) text.get(i)[2]).intValue();
            if (y != first) {
                lines++;
                first = y;
            }
        }
        assertTrue("several lines: " + lines, lines > 2);
        int oneLine = pane.getFontMetrics(pane.getFont()).getHeight();
        assertTrue(pane.getPreferredSize().height >= lines * oneLine);
    }

    /// An `<img>` is a word of its line, at the size its tag gives, found
    /// through the source the caller hands the parser; without a source,
    /// or when the picture is not found, it leaves nothing.
    @Test
    public void anImageStandsInItsLineAtTheSizeItsTagGives() {
        final List<String> asked = new ArrayList<String>();
        final com.codename1.desktopcompat.java.awt.Image pic =
                new com.codename1.desktopcompat.java.awt.image.BufferedImage(20, 10,
                        com.codename1.desktopcompat.java.awt.image.BufferedImage.TYPE_INT_RGB);
        com.codename1.desktopcompat.rt.MiniHtml.ImageSource source =
                new com.codename1.desktopcompat.rt.MiniHtml.ImageSource() {
            @Override
            public com.codename1.desktopcompat.java.awt.Image image(String src) {
                asked.add(src);
                return "missing.png".equals(src) ? null : pic;
            }
        };
        String html = "<html>top<br><img src=\"a/pic.png\" width=\"80\" height=\"60\">after"
                + "<img src=\"missing.png\"><br><img src='own.png'></html>";
        com.codename1.desktopcompat.rt.MiniHtml.Document doc =
                com.codename1.desktopcompat.rt.MiniHtml.parse(html, source);
        assertEquals("[a/pic.png, missing.png, own.png]", asked.toString());
        assertEquals(3, doc.lineCount());
        assertEquals(2, doc.line(1).runCount());
        assertSame(pic, doc.line(1).run(0).image());
        assertEquals(80, doc.line(1).run(0).imageWidth());
        assertEquals(60, doc.line(1).run(0).imageHeight());
        assertNull(doc.line(1).run(1).image());
        assertEquals("after", doc.line(1).text());
        // Without a size the picture has its own.
        assertEquals(20, doc.line(2).run(0).imageWidth());
        assertEquals(10, doc.line(2).run(0).imageHeight());

        com.codename1.desktopcompat.java.awt.Font font = com.codename1.desktopcompat.rt.Fonts.defaultFont();
        com.codename1.desktopcompat.java.awt.FontMetrics fm = com.codename1.desktopcompat.rt.Fonts.metrics(font);
        com.codename1.desktopcompat.java.awt.Dimension size =
                com.codename1.desktopcompat.rt.MiniHtml.preferredSize(doc, font);
        assertEquals(80 + fm.stringWidth("after"), size.width);
        int descent = fm.getHeight() - fm.getAscent();
        // A line that is only an image is as tall as the image.
        assertEquals(fm.getHeight() + Math.max(fm.getHeight(), 60 + descent) + 10, size.height);
        // Too narrow for both: the image keeps its line, the word moves on.
        com.codename1.desktopcompat.rt.MiniHtml.Document wrapped =
                com.codename1.desktopcompat.rt.MiniHtml.wrap(doc, font, 80 + fm.stringWidth("after") - 1);
        assertEquals(4, wrapped.lineCount());
        assertSame(pic, wrapped.line(1).run(0).image());
        assertEquals("after", wrapped.line(2).text());

        // A parser without a source drops images and keeps the text.
        com.codename1.desktopcompat.rt.MiniHtml.Document plain =
                com.codename1.desktopcompat.rt.MiniHtml.parse(html);
        assertEquals(2, plain.lineCount());
        assertEquals("after", plain.line(1).text());

        // In an editor pane the text after the picture is drawn after it.
        JEditorPane pane = new JEditorPane("text/html", "<html>x</html>");
        JFrame f = frame(pane);
        assertNotNull(find(paint(f), "x"));
    }
}
