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
package com.codename1.desktopcompat;

import com.codename1.desktopcompat.java.awt.FlowLayout;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JPasswordField;
import com.codename1.desktopcompat.javax.swing.JTextArea;
import com.codename1.desktopcompat.javax.swing.JTextField;
import com.codename1.desktopcompat.javax.swing.event.CaretEvent;
import com.codename1.desktopcompat.javax.swing.event.CaretListener;
import com.codename1.desktopcompat.javax.swing.event.DocumentEvent;
import com.codename1.desktopcompat.javax.swing.event.DocumentListener;
import com.codename1.desktopcompat.javax.swing.text.AbstractDocument;
import com.codename1.desktopcompat.javax.swing.text.AttributeSet;
import com.codename1.desktopcompat.javax.swing.text.BadLocationException;
import com.codename1.desktopcompat.javax.swing.text.DocumentFilter;
import com.codename1.desktopcompat.javax.swing.text.PlainDocument;
import com.codename1.desktopcompat.javax.swing.text.Position;
import com.codename1.ui.Display;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// The document of a text component and the Codename One widget behind it
/// stay the same in both directions, the filter sees native typing, and
/// the caret follows the text.
public class TextSyncTest extends KernelTestBase {

    /// Records document events as short strings naming kind, offset and length.
    private static final class Log implements DocumentListener {
        final List<String> events = new ArrayList<String>();

        @Override
        public void insertUpdate(DocumentEvent e) {
            events.add("insert@" + e.getOffset() + "+" + e.getLength());
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            events.add("remove@" + e.getOffset() + "+" + e.getLength());
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            events.add("change");
        }
    }

    private JTextField field(String text) {
        JFrame f = new JFrame();
        f.setLayout(new FlowLayout());
        JTextField t = new JTextField(text, 10);
        f.add(t);
        show(f);
        return t;
    }

    private static com.codename1.ui.TextArea peer(com.codename1.desktopcompat.java.awt.Component c) {
        return (com.codename1.ui.TextArea) c.cn1Peer();
    }

    @Test
    public void documentChangesReachTheWidget() throws Exception {
        JTextField t = field("abc");
        assertEquals("abc", peer(t).getText());
        t.setText("hello");
        assertEquals("hello", peer(t).getText());
        t.getDocument().insertString(5, "!", null);
        assertEquals("hello!", peer(t).getText());
        t.getDocument().remove(0, 1);
        assertEquals("ello!", peer(t).getText());
    }

    @Test
    public void setTextFiresOneRemoveAndOneInsert() {
        JTextField t = field("abc");
        Log log = new Log();
        t.getDocument().addDocumentListener(log);
        t.setText("xy");
        assertEquals("[remove@0+3, insert@0+2]", log.events.toString());
        assertEquals("xy", peer(t).getText());
    }

    @Test
    public void nativeTypingBecomesAnInsertAtItsOffset() {
        JTextField t = field("abc");
        Log log = new Log();
        t.getDocument().addDocumentListener(log);
        peer(t).setText("abXYc");
        assertEquals("abXYc", t.getText());
        assertEquals("[insert@2+2]", log.events.toString());
        assertEquals(4, t.getCaretPosition());
    }

    @Test
    public void nativeDeletionBecomesARemove() {
        JTextField t = field("abcdef");
        Log log = new Log();
        t.getDocument().addDocumentListener(log);
        peer(t).setText("abef");
        assertEquals("abef", t.getText());
        assertEquals("[remove@2+2]", log.events.toString());
        assertEquals(2, t.getCaretPosition());
    }

    @Test
    public void nativeReplacementIsARemoveAndAnInsert() {
        JTextField t = field("one two");
        Log log = new Log();
        t.getDocument().addDocumentListener(log);
        peer(t).setText("one 2");
        assertEquals("[remove@4+3, insert@4+1]", log.events.toString());
        assertEquals("one 2", t.getText());
    }

    /// Lets only digits into the document.
    private static final class Digits extends DocumentFilter {
        private static boolean ok(String s) {
            for (int i = 0; s != null && i < s.length(); i++) {
                if (s.charAt(i) < '0' || s.charAt(i) > '9') {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            if (ok(string)) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (ok(text)) {
                super.replace(fb, offset, length, text, attrs);
            }
        }
    }

    @Test
    public void aFilterVetoTakesTheTypingBackOutOfTheWidget() {
        JTextField t = field("12");
        ((AbstractDocument) t.getDocument()).setDocumentFilter(new Digits());
        Log log = new Log();
        t.getDocument().addDocumentListener(log);
        peer(t).setText("12a");
        assertEquals("12", t.getText());
        assertEquals("12", peer(t).getText());
        assertTrue(log.events.isEmpty());
        peer(t).setText("123");
        assertEquals("123", t.getText());
        assertEquals("[insert@2+1]", log.events.toString());
        t.setText("abc");
        assertEquals("123", t.getText());
    }

    @Test
    public void aFilterThatTransformsRewritesTheWidget() {
        JTextField t = field("");
        ((AbstractDocument) t.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; text != null && i < text.length(); i++) {
                    char c = text.charAt(i);
                    sb.append(c >= 'a' && c <= 'z' ? (char) (c - 32) : c);
                }
                fb.replace(offset, length, sb.toString(), attrs);
            }

            @Override
            public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs)
                    throws BadLocationException {
                replace(fb, offset, 0, text, attrs);
            }
        });
        peer(t).setText("ab");
        assertEquals("AB", t.getText());
        assertEquals("AB", peer(t).getText());
        peer(t).setText("ABc");
        assertEquals("ABC", peer(t).getText());
    }

    @Test
    public void aListenerSeesEachChangeOnce() {
        JTextField t = field("");
        Log log = new Log();
        t.getDocument().addDocumentListener(log);
        t.setText("a");
        peer(t).setText("ab");
        t.replaceSelection("c");
        assertEquals("[insert@0+1, insert@1+1, insert@2+1]", log.events.toString());
        assertEquals("abc", peer(t).getText());
    }

    @Test
    public void lineBreaksBecomeSpacesInAField() {
        JTextField t = field("");
        t.setText("a\nb");
        assertEquals("a b", t.getText());
    }

    @Test
    public void caretAndSelection() {
        JTextField t = field("");
        final List<String> carets = new ArrayList<String>();
        t.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                carets.add(e.getDot() + "/" + e.getMark());
            }
        });
        t.setText("hello");
        assertEquals(5, t.getCaretPosition());
        assertNull(t.getSelectedText());
        t.select(1, 3);
        assertEquals("el", t.getSelectedText());
        assertEquals(1, t.getSelectionStart());
        assertEquals(3, t.getSelectionEnd());
        assertEquals(3, t.getCaret().getDot());
        assertEquals(1, t.getCaret().getMark());
        t.replaceSelection("XY");
        assertEquals("hXYlo", t.getText());
        assertEquals(3, t.getCaretPosition());
        assertNull(t.getSelectedText());
        t.selectAll();
        assertEquals("hXYlo", t.getSelectedText());
        t.setCaretPosition(0);
        t.moveCaretPosition(2);
        assertEquals("hX", t.getSelectedText());
        assertTrue(carets.contains("5/5"));
        assertTrue(carets.contains("3/1"));
        try {
            t.setCaretPosition(99);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals(2, t.getCaretPosition());
        }
        t.select(-5, 99);
        assertEquals("hXYlo", t.getSelectedText());
    }

    @Test
    public void theCaretFollowsTheText() throws Exception {
        JTextField t = field("abcdef");
        t.setCaretPosition(3);
        t.getDocument().insertString(0, "xx", null);
        assertEquals(5, t.getCaretPosition());
        t.getDocument().insertString(8, "yy", null);
        assertEquals(5, t.getCaretPosition());
        t.getDocument().remove(0, 4);
        assertEquals(1, t.getCaretPosition());
        t.getDocument().remove(0, 3);
        assertEquals(0, t.getCaretPosition());
    }

    @Test
    public void cutCopyAndPasteUseTheClipboard() {
        JTextField t = field("hello world");
        t.select(0, 5);
        t.copy();
        assertEquals("hello", Display.getInstance().getPasteDataFromClipboard());
        t.select(5, 11);
        t.cut();
        assertEquals("hello", t.getText());
        assertEquals(" world", Display.getInstance().getPasteDataFromClipboard());
        t.setCaretPosition(0);
        t.paste();
        assertEquals(" worldhello", t.getText());
        t.setEditable(false);
        t.selectAll();
        t.cut();
        t.paste();
        t.replaceSelection("x");
        assertEquals(" worldhello", t.getText());
        assertTrue(!peer(t).isEditable());
    }

    @Test
    public void theActionListenersHearTheCommand() {
        JTextField t = field("go");
        final List<String> commands = new ArrayList<String>();
        t.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                commands.add(e.getActionCommand());
            }
        });
        t.postActionEvent();
        t.setActionCommand("search");
        t.postActionEvent();
        assertEquals("[go, search]", commands.toString());
    }

    @Test
    public void alignmentAndColumnsReachTheWidget() {
        JTextField t = field("x");
        assertEquals(10, peer(t).getColumns());
        t.setColumns(20);
        assertEquals(20, peer(t).getColumns());
        t.setHorizontalAlignment(JTextField.RIGHT);
        assertEquals(com.codename1.ui.Component.RIGHT, peer(t).getAlignment());
    }

    @Test
    public void passwordField() {
        JFrame f = new JFrame();
        JPasswordField p = new JPasswordField("secret", 8);
        f.add(p);
        show(f);
        assertArrayEquals("secret".toCharArray(), p.getPassword());
        assertEquals(com.codename1.ui.TextArea.PASSWORD, peer(p).getConstraint() & com.codename1.ui.TextArea.PASSWORD);
        assertTrue(p.echoCharIsSet());
        p.setEchoChar((char) 0);
        assertEquals(0, peer(p).getConstraint() & com.codename1.ui.TextArea.PASSWORD);
        Display.getInstance().copyToClipboard("kept");
        p.selectAll();
        p.copy();
        assertEquals("kept", Display.getInstance().getPasteDataFromClipboard());
    }

    @Test
    public void textAreaMatchesTheDesktopOne() throws Exception {
        JTextArea ours = new JTextArea("one\ntwo", 3, 20);
        javax.swing.JTextArea real = new javax.swing.JTextArea("one\ntwo", 3, 20);
        ours.append("\nthree");
        real.append("\nthree");
        ours.insert("zero\n", 0);
        real.insert("zero\n", 0);
        ours.replaceRange("TWO", 9, 12);
        real.replaceRange("TWO", 9, 12);
        assertEquals(real.getText(), ours.getText());
        assertEquals(real.getLineCount(), ours.getLineCount());
        for (int i = 0; i < real.getLineCount(); i++) {
            assertEquals(real.getLineStartOffset(i), ours.getLineStartOffset(i));
            assertEquals(real.getLineEndOffset(i), ours.getLineEndOffset(i));
        }
        for (int i = 0; i <= real.getDocument().getLength(); i++) {
            assertEquals(real.getLineOfOffset(i), ours.getLineOfOffset(i));
        }
        assertEquals(real.getRows(), ours.getRows());
        assertEquals(real.getColumns(), ours.getColumns());
        assertEquals(real.getTabSize(), ours.getTabSize());
        try {
            ours.getLineStartOffset(9);
            fail();
        } catch (BadLocationException expected) {
            assertTrue(expected.offsetRequested() > 0);
        }
    }

    @Test
    public void textAreaSyncsWithItsWidget() {
        JFrame f = new JFrame();
        JTextArea a = new JTextArea("first", 4, 20);
        f.add(a);
        show(f);
        assertEquals("first", peer(a).getText());
        a.append("\nsecond");
        assertEquals("first\nsecond", peer(a).getText());
        peer(a).setText("first\nsecond\nthird");
        assertEquals(3, a.getLineCount());
        assertEquals("first\nsecond\nthird", a.getText());
    }

    @Test
    public void positionsMoveWithTheText() throws Exception {
        PlainDocument d = new PlainDocument();
        d.insertString(0, "abcdef", null);
        Position mid = d.createPosition(3);
        Position end = d.createPosition(6);
        d.insertString(0, "xx", null);
        assertEquals(5, mid.getOffset());
        assertEquals(8, end.getOffset());
        d.remove(1, 6);
        assertEquals(1, mid.getOffset());
        assertEquals(2, end.getOffset());
        try {
            d.getText(1, 5);
            fail();
        } catch (BadLocationException expected) {
            assertTrue(expected.offsetRequested() >= 0);
        }
        try {
            d.insertString(9, "x", null);
            fail();
        } catch (BadLocationException expected) {
            assertEquals(9, expected.offsetRequested());
        }
    }

    @Test
    public void theLinesAreTheElementsOfAPlainDocument() throws Exception {
        PlainDocument ours = new PlainDocument();
        javax.swing.text.PlainDocument real = new javax.swing.text.PlainDocument();
        String text = "a\n\nbcd\nlast";
        ours.insertString(0, text, null);
        real.insertString(0, text, null);
        com.codename1.desktopcompat.javax.swing.text.Element or = ours.getDefaultRootElement();
        javax.swing.text.Element rr = real.getDefaultRootElement();
        assertEquals(rr.getElementCount(), or.getElementCount());
        for (int i = 0; i < rr.getElementCount(); i++) {
            assertEquals(rr.getElement(i).getStartOffset(), or.getElement(i).getStartOffset());
            assertEquals(rr.getElement(i).getEndOffset(), or.getElement(i).getEndOffset());
        }
        for (int i = 0; i <= text.length(); i++) {
            assertEquals(rr.getElementIndex(i), or.getElementIndex(i));
            assertEquals(real.getParagraphElement(i).getStartOffset(), ours.getParagraphElement(i).getStartOffset());
        }
    }
}
