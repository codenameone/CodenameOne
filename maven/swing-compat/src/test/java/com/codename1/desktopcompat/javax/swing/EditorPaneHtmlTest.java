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
}
