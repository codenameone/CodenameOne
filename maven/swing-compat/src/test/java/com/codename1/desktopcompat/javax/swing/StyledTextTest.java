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
import static org.junit.Assert.fail;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.geom.Point2D;
import com.codename1.desktopcompat.javax.swing.event.DocumentEvent;
import com.codename1.desktopcompat.javax.swing.event.DocumentListener;
import com.codename1.desktopcompat.javax.swing.text.AttributeSet;
import com.codename1.desktopcompat.javax.swing.text.BadLocationException;
import com.codename1.desktopcompat.javax.swing.text.DefaultStyledDocument;
import com.codename1.desktopcompat.javax.swing.text.Element;
import com.codename1.desktopcompat.javax.swing.text.MutableAttributeSet;
import com.codename1.desktopcompat.javax.swing.text.PlainDocument;
import com.codename1.desktopcompat.javax.swing.text.SimpleAttributeSet;
import com.codename1.desktopcompat.javax.swing.text.Style;
import com.codename1.desktopcompat.javax.swing.text.StyleConstants;
import com.codename1.desktopcompat.javax.swing.text.StyleContext;
import com.codename1.desktopcompat.javax.swing.text.StyledDocument;
import com.codename1.desktopcompat.rt.StyledText;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/// Styled text: the attributes of a document by run and by paragraph,
/// the pane that paints them, and the mapping between a point of a text
/// component and an offset of its document.
public class StyledTextTest extends KernelTestBase {

    private static SimpleAttributeSet color(Color c) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setForeground(a, c);
        return a;
    }

    @Test
    public void anAttributeSetKeepsWhatItIsGivenAndAsksItsParentForTheRest() {
        SimpleAttributeSet parent = new SimpleAttributeSet();
        StyleConstants.setItalic(parent, true);
        StyleConstants.setFontSize(parent, 20);
        SimpleAttributeSet a = new SimpleAttributeSet();
        assertTrue(a.isEmpty());
        assertFalse(StyleConstants.isBold(a));
        assertEquals(12, StyleConstants.getFontSize(a));
        assertEquals("Monospaced", StyleConstants.getFontFamily(a));
        assertEquals(Color.black, StyleConstants.getForeground(a));
        StyleConstants.setBold(a, true);
        StyleConstants.setUnderline(a, true);
        StyleConstants.setStrikeThrough(a, true);
        StyleConstants.setForeground(a, Color.red);
        StyleConstants.setBackground(a, Color.yellow);
        StyleConstants.setFontFamily(a, "Serif");
        StyleConstants.setAlignment(a, StyleConstants.ALIGN_CENTER);
        a.setResolveParent(parent);
        assertTrue(StyleConstants.isBold(a));
        assertTrue(StyleConstants.isUnderline(a));
        assertTrue(StyleConstants.isStrikeThrough(a));
        assertTrue("from the parent", StyleConstants.isItalic(a));
        assertEquals(20, StyleConstants.getFontSize(a));
        assertFalse(a.isDefined(StyleConstants.Italic));
        assertEquals(Color.red, StyleConstants.getForeground(a));
        assertEquals(Color.yellow, StyleConstants.getBackground(a));
        assertEquals("Serif", StyleConstants.getFontFamily(a));
        assertEquals(StyleConstants.ALIGN_CENTER, StyleConstants.getAlignment(a));
        assertSame(parent, a.getResolveParent());
        AttributeSet copy = a.copyAttributes();
        assertTrue(copy.isEqual(a));
        assertEquals(a, copy);
        a.removeAttribute(StyleConstants.Bold);
        assertFalse(StyleConstants.isBold(a));
        assertFalse(copy.isEqual(a));
        assertTrue(a.containsAttribute(StyleConstants.Foreground, Color.red));
        a.removeAttributes(color(Color.blue));
        assertTrue("another value is not removed", a.isDefined(StyleConstants.Foreground));
        a.removeAttributes(color(Color.red));
        assertFalse(a.isDefined(StyleConstants.Foreground));
        assertEquals(0, SimpleAttributeSet.EMPTY.getAttributeCount());
    }

    @Test
    public void aStyleTellsItsListenersAndAContextKeepsItByName() {
        StyleContext context = new StyleContext();
        Style def = context.getStyle(StyleContext.DEFAULT_STYLE);
        assertNotNull(def);
        assertEquals("default", def.getName());
        Style warn = context.addStyle("warn", def);
        assertSame(warn, context.getStyle("warn"));
        assertSame(def, warn.getResolveParent());
        final int[] changes = {0};
        warn.addChangeListener(new com.codename1.desktopcompat.javax.swing.event.ChangeListener() {
            @Override
            public void stateChanged(com.codename1.desktopcompat.javax.swing.event.ChangeEvent e) {
                changes[0]++;
            }
        });
        StyleConstants.setForeground(warn, Color.red);
        assertEquals(1, changes[0]);
        StyleConstants.setBold(def, true);
        assertTrue("through the parent", StyleConstants.isBold(warn));
        Font f = context.getFont(warn);
        assertTrue(f.isBold());
        assertEquals(12, f.getSize());
        context.removeStyle("warn");
        assertNull(context.getStyle("warn"));
    }

    @Test
    public void aDocumentKeepsTheAttributesOfItsRuns() throws BadLocationException {
        DefaultStyledDocument doc = new DefaultStyledDocument();
        doc.insertString(0, "plain ", null);
        doc.insertString(doc.getLength(), "red", color(Color.red));
        doc.insertString(doc.getLength(), " tail", null);
        assertEquals("plain red tail", doc.getText(0, doc.getLength()));
        Element run = doc.getCharacterElement(7);
        assertTrue(run.isLeaf());
        assertEquals(6, run.getStartOffset());
        assertEquals(9, run.getEndOffset());
        assertEquals(Color.red, StyleConstants.getForeground(run.getAttributes()));
        assertNull(doc.getCharacterElement(2).getAttributes().getAttribute(StyleConstants.Foreground));
        // The last run takes in the break that ends the document.
        assertEquals(doc.getLength() + 1, doc.getCharacterElement(12).getEndOffset());
        Element root = doc.getDefaultRootElement();
        assertEquals(1, root.getElementCount());
        assertEquals(3, root.getElement(0).getElementCount());
        assertSame(root, root.getElement(0).getParentElement());

        // Attributes over part of two runs cut both.
        SimpleAttributeSet bold = new SimpleAttributeSet();
        StyleConstants.setBold(bold, true);
        final List<String> events = new ArrayList<String>();
        doc.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                events.add("insert");
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                events.add("remove");
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                events.add("change " + e.getOffset() + "+" + e.getLength());
            }
        });
        doc.setCharacterAttributes(4, 4, bold, false);
        assertEquals("[change 4+4]", events.toString());
        Element cut = doc.getCharacterElement(7);
        assertEquals(6, cut.getStartOffset());
        assertEquals(8, cut.getEndOffset());
        assertTrue(StyleConstants.isBold(cut.getAttributes()));
        assertEquals("added, not put in place", Color.red, StyleConstants.getForeground(cut.getAttributes()));
        assertFalse(StyleConstants.isBold(doc.getCharacterElement(8).getAttributes()));
        doc.setCharacterAttributes(6, 2, bold, true);
        assertNull(doc.getCharacterElement(7).getAttributes().getAttribute(StyleConstants.Foreground));

        // What is left of a run that was cut keeps its attributes.
        doc.remove(4, 4);
        assertEquals("plaid tail", doc.getText(0, doc.getLength()));
        assertEquals(Color.red, StyleConstants.getForeground(doc.getCharacterElement(4).getAttributes()));
        assertEquals(4, doc.getCharacterElement(4).getStartOffset());
        assertEquals(5, doc.getCharacterElement(4).getEndOffset());
    }

    @Test
    public void paragraphsHaveAttributesAndAStyleOfTheirOwn() throws BadLocationException {
        DefaultStyledDocument doc = new DefaultStyledDocument();
        doc.insertString(0, "one\ntwo\nthree", null);
        Element root = doc.getDefaultRootElement();
        assertEquals(3, root.getElementCount());
        assertEquals(0, root.getElement(0).getStartOffset());
        assertEquals(4, root.getElement(0).getEndOffset());
        assertEquals(4, doc.getParagraphElement(5).getStartOffset());
        assertEquals(8, doc.getParagraphElement(5).getEndOffset());
        assertEquals(1, root.getElementIndex(5));
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(5, 1, center, false);
        assertEquals(StyleConstants.ALIGN_CENTER, StyleConstants.getAlignment(doc.getParagraphElement(4)
                .getAttributes()));
        assertEquals(StyleConstants.ALIGN_LEFT, StyleConstants.getAlignment(doc.getParagraphElement(0)
                .getAttributes()));
        // The style a paragraph resolves in gives its runs what they lack.
        Style loud = doc.addStyle("loud", null);
        StyleConstants.setForeground(loud, Color.blue);
        doc.setLogicalStyle(9, loud);
        assertSame(loud, doc.getLogicalStyle(10));
        assertEquals("default", doc.getLogicalStyle(0).getName());
        assertEquals(Color.blue, doc.getCharacterElement(10).getAttributes().getAttribute(StyleConstants.Foreground));
        assertNull(doc.getCharacterElement(1).getAttributes().getAttribute(StyleConstants.Foreground));
        // Joining two paragraphs keeps the attributes of the first.
        doc.remove(3, 1);
        assertEquals(2, root.getElementCount());
        assertEquals(StyleConstants.ALIGN_LEFT, StyleConstants.getAlignment(doc.getParagraphElement(4)
                .getAttributes()));
        // Splitting one gives both halves its attributes.
        doc.insertString(doc.getLength(), "\nfour", null);
        assertEquals(3, root.getElementCount());
        assertSame(loud, doc.getLogicalStyle(doc.getLength()));
    }

    private JTextPane pane;

    private JFrame shown(JTextPane p) {
        pane = p;
        JFrame f = new JFrame();
        f.setLayout(new BorderLayout());
        p.setFont(new Font("Dialog", Font.PLAIN, 14));
        p.setBackground(Color.white);
        p.setForeground(new Color(0x10, 0x20, 0x30));
        f.add(p, BorderLayout.CENTER);
        show(f);
        return f;
    }

    @Test
    public void aTextPaneWantsAStyledDocument() {
        JTextPane p = new JTextPane();
        assertTrue(p.getDocument() instanceof StyledDocument);
        assertSame(p.getDocument(), p.getStyledDocument());
        try {
            p.setDocument(new PlainDocument());
            fail("Model must be StyledDocument");
        } catch (IllegalArgumentException expected) {
            assertTrue(p.getDocument() instanceof StyledDocument);
        }
        DefaultStyledDocument doc = new DefaultStyledDocument();
        p.setStyledDocument(doc);
        assertSame(doc, p.getStyledDocument());
        Style s = p.addStyle("s", null);
        assertSame(s, p.getStyle("s"));
        assertSame(s, doc.getStyle("s"));
    }

    @Test
    public void aPaneWithoutAttributesIsAPlainEditableArea() throws BadLocationException {
        JTextPane p = new JTextPane();
        p.getStyledDocument().insertString(0, "just text", null);
        shown(p);
        assertTrue(p.cn1Peer() instanceof com.codename1.ui.TextArea);
        assertTrue(((com.codename1.ui.TextArea) p.cn1Peer()).isEditable());
        assertEquals("just text", ((com.codename1.ui.TextArea) p.cn1Peer()).getText());
        // The first attribute hands the drawing to the pane.
        p.getStyledDocument().setCharacterAttributes(0, 4, color(Color.red), false);
        assertFalse(((com.codename1.ui.TextArea) p.cn1Peer()).isEditable());
    }

    @Test
    public void theRunsArePaintedInTheirColorsEachWhereItsTextIs() throws BadLocationException {
        JTextPane p = new JTextPane();
        StyledDocument doc = p.getStyledDocument();
        doc.insertString(0, "aaaa ", null);
        doc.insertString(doc.getLength(), "bbbb", color(Color.red));
        SimpleAttributeSet marked = color(Color.blue);
        StyleConstants.setBackground(marked, Color.yellow);
        StyleConstants.setUnderline(marked, true);
        doc.insertString(doc.getLength(), "\ncccc", marked);
        JFrame f = shown(p);
        int[][] rows = raster(f);
        Rectangle a = p.modelToView(0);
        Rectangle b = p.modelToView(5);
        Rectangle bEnd = p.modelToView(9);
        Rectangle c = p.modelToView(10);
        Rectangle cEnd = p.modelToView(14);
        assertEquals("the second paragraph is a line down", a.y + a.height, c.y);
        assertEquals(a.x, c.x);
        assertTrue(b.x > a.x && bEnd.x > b.x && cEnd.x > c.x);
        int ink = 0x102030;
        // A string is drawn as a box of the size it measures.
        assertTrue("plain text in the pane's color",
                count(rows, p, a.x, a.y, b.x - a.x, a.height, ink) > 0);
        assertEquals(0, count(rows, p, a.x, a.y, b.x - a.x, a.height, 0xff0000));
        assertTrue("the red run", count(rows, p, b.x, b.y, bEnd.x - b.x, b.height, 0xff0000) > 0);
        assertEquals(0, count(rows, p, b.x, b.y, bEnd.x - b.x, b.height, ink));
        assertTrue("the blue run", count(rows, p, c.x, c.y, cEnd.x - c.x, c.height, 0x0000ff) > 0);
        assertEquals("nothing of the first line's colors on the second", 0,
                count(rows, p, c.x, c.y, cEnd.x - c.x, c.height, 0xff0000));

        // The recorded strings: one per run.
        List<Object[]> text = paint(f);
        assertNotNull(find(text, "aaaa "));
        assertNotNull(find(text, "bbbb"));
        assertNotNull(find(text, "cccc"));
        assertEquals(p.getText(), "aaaa bbbb\ncccc");
    }

    @Test
    public void aBackgroundStaysUnderItsRun() throws BadLocationException {
        DefaultStyledDocument doc = new DefaultStyledDocument();
        SimpleAttributeSet marked = new SimpleAttributeSet();
        StyleConstants.setBackground(marked, Color.yellow);
        doc.insertString(0, "    ", marked);
        doc.insertString(4, "    ", null);
        JTextPane p = new JTextPane(doc);
        JFrame f = shown(p);
        int[][] rows = raster(f);
        Rectangle from = p.modelToView(0);
        Rectangle to = p.modelToView(4);
        Rectangle end = p.modelToView(8);
        // The run beside the marked one has none of its background.
        assertTrue(to.x > from.x);
        assertEquals("no yellow behind the plain run", 0,
                count(rows, p, to.x, to.y, end.x - to.x, to.height, 0xffff00));
        StyledText layout = new StyledText(doc, p.getFont(), Color.black, 400);
        assertEquals(1, layout.getLineCount());
        assertTrue(StyledText.styled(doc));
        assertFalse(StyledText.styled(new DefaultStyledDocument()));
    }

    @Test
    public void aParagraphIsBrokenAtItsSpacesAndAlignedAsItAsks() throws BadLocationException {
        DefaultStyledDocument doc = new DefaultStyledDocument();
        doc.insertString(0, "aaaa bbbb cccc dddd", color(Color.red));
        Font font = new Font("Dialog", Font.PLAIN, 14);
        StyledText whole = new StyledText(doc, font, Color.black, 0);
        assertEquals(1, whole.getLineCount());
        int four = whole.caretAt(4).x;
        int five = whole.caretAt(5).x;
        int all = whole.getWidth();
        // Room for two words and no more.
        StyledText narrow = new StyledText(doc, font, Color.black, five + four + 1);
        assertEquals(2, narrow.getLineCount());
        assertEquals(0, narrow.caretAt(0).y);
        assertEquals("the third word starts the second line", 0, narrow.caretAt(10).x);
        assertEquals(narrow.caretAt(0).height, narrow.caretAt(10).y);
        assertEquals(10, narrow.offsetAt(0, narrow.caretAt(10).y + 1));
        assertEquals(narrow.getHeight(), 2 * narrow.caretAt(0).height);
        // A word wider than the room is broken where it must be.
        StyledText tiny = new StyledText(doc, font, Color.black, Math.max(1, four / 2));
        assertTrue(tiny.getLineCount() >= 8);
        // The nearer half of a character decides.
        int one = whole.caretAt(1).x;
        assertEquals(0, whole.offsetAt(one / 2 - 1, 1));
        assertEquals(1, whole.offsetAt(one / 2 + 1, 1));
        assertEquals(19, whole.offsetAt(all + 50, 1));
        assertEquals("below the last line is on it", 10, narrow.offsetAt(0, 5000));

        SimpleAttributeSet right = new SimpleAttributeSet();
        StyleConstants.setAlignment(right, StyleConstants.ALIGN_RIGHT);
        doc.setParagraphAttributes(0, 1, right, false);
        StyledText aligned = new StyledText(doc, font, Color.black, all + 100);
        assertEquals(100, aligned.caretAt(0).x);
        StyleConstants.setAlignment(right, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, 1, right, false);
        assertEquals(50, new StyledText(doc, font, Color.black, all + 100).caretAt(0).x);
    }

    @Test
    public void aRunInAnotherFontStandsAfterTheRunBeforeIt() throws BadLocationException {
        DefaultStyledDocument doc = new DefaultStyledDocument();
        SimpleAttributeSet big = new SimpleAttributeSet();
        StyleConstants.setFontSize(big, 28);
        StyleConstants.setBold(big, true);
        doc.insertString(0, "xx", null);
        doc.insertString(2, "XX", big);
        Font font = new Font("Dialog", Font.PLAIN, 14);
        StyledText layout = new StyledText(doc, font, Color.black, 0);
        StyledText plain = new StyledText(new DefaultStyledDocument() {
            {
                try {
                    insertString(0, "xx", null);
                } catch (BadLocationException e) {
                    throw new IllegalStateException(e);
                }
            }
        }, font, Color.black, 0);
        assertEquals(plain.getWidth(), layout.caretAt(2).x);
        assertTrue(layout.getWidth() > plain.getWidth());
        assertTrue("a line is at least as high as the pane's font", layout.getHeight() >= plain.getHeight());
        assertEquals(1, layout.getLineCount());
    }

    @Test
    public void thePaneGivesTheSelectionAttributesAndInsertsWithItsInputAttributes() throws BadLocationException {
        JTextPane p = new JTextPane();
        p.setText("hello world");
        p.select(6, 11);
        p.setCharacterAttributes(color(Color.red), false);
        StyledDocument doc = p.getStyledDocument();
        assertEquals(Color.red, StyleConstants.getForeground(doc.getCharacterElement(7).getAttributes()));
        assertNull(doc.getCharacterElement(1).getAttributes().getAttribute(StyleConstants.Foreground));
        // With nothing selected the attributes are those of what is typed.
        p.setCaretPosition(0);
        MutableAttributeSet in = p.getInputAttributes();
        assertEquals(0, in.getAttributeCount());
        p.setCharacterAttributes(color(Color.blue), true);
        assertEquals(Color.blue, StyleConstants.getForeground(in));
        p.replaceSelection("> ");
        assertEquals("> hello world", p.getText());
        assertEquals(Color.blue, StyleConstants.getForeground(doc.getCharacterElement(0).getAttributes()));
        assertEquals(Color.red, StyleConstants.getForeground(doc.getCharacterElement(9).getAttributes()));
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        p.setParagraphAttributes(center, false);
        assertEquals(StyleConstants.ALIGN_CENTER, StyleConstants.getAlignment(p.getParagraphAttributes()));
    }

    @Test
    public void aPointOfATextAreaFindsItsOffsetAndBack() throws BadLocationException {
        JFrame f = new JFrame();
        f.setLayout(new BorderLayout());
        JTextArea area = new JTextArea("abcdef\nghij\n\nklm");
        assertEquals("no size yet", -1, area.viewToModel(new Point(3, 3)));
        assertNull(area.modelToView(0));
        f.add(area, BorderLayout.CENTER);
        show(f);
        Rectangle first = area.modelToView(0);
        Rectangle third = area.modelToView(3);
        Rectangle second = area.modelToView(8);
        Rectangle empty = area.modelToView(12);
        Rectangle last = area.modelToView(14);
        assertEquals(1, first.width);
        assertTrue(first.height > 0);
        assertTrue(third.x > first.x);
        assertEquals(first.y, third.y);
        assertEquals(first.y + first.height, second.y);
        assertEquals(first.y + 2 * first.height, empty.y);
        assertEquals(first.x, empty.x);
        assertEquals(first.y + 3 * first.height, last.y);
        assertEquals(3, area.viewToModel(new Point(third.x, third.y + 2)));
        assertEquals(8, area.viewToModel(new Point(second.x, second.y + 2)));
        assertEquals(12, area.viewToModel(new Point(first.x + 200, empty.y + 2)));
        assertEquals("past the end of a row is its end", 6, area.viewToModel(new Point(first.x + 2000, first.y + 2)));
        assertEquals(14, area.viewToModel2D(new Point2D.Double(last.x, last.y + 2)));
        assertEquals(last, area.modelToView2D(14));
        try {
            area.modelToView(99);
            fail("an offset outside the document");
        } catch (BadLocationException expected) {
            assertEquals(99, expected.offsetRequested());
        }
    }
}
