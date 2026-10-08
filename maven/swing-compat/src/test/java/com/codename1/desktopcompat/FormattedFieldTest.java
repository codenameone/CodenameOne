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
import com.codename1.desktopcompat.java.awt.event.FocusEvent;
import com.codename1.desktopcompat.java.beans.PropertyChangeEvent;
import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.javax.swing.InputVerifier;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.JEditorPane;
import com.codename1.desktopcompat.javax.swing.JFormattedTextField;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JTextField;
import com.codename1.desktopcompat.javax.swing.JTextPane;
import com.codename1.desktopcompat.javax.swing.text.DefaultFormatter;
import com.codename1.desktopcompat.javax.swing.text.NumberFormatter;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Formatted fields commit and revert their value the way the desktop
/// ones do; the editor pane shows markup as text.
public class FormattedFieldTest extends KernelTestBase {

    private JFormattedTextField shown(Object value) {
        JFrame f = new JFrame();
        f.setLayout(new FlowLayout());
        JFormattedTextField t = new JFormattedTextField(value);
        t.setColumns(10);
        f.add(t);
        show(f);
        return t;
    }

    private static void typed(JFormattedTextField t, String text) {
        ((com.codename1.ui.TextArea) t.cn1Peer()).setText(text);
    }

    @Test
    public void aNumberValueIsShownAndReadBackAsItsClass() throws Exception {
        JFormattedTextField t = shown(Integer.valueOf(42));
        assertEquals("42", t.getText());
        assertTrue(t.getFormatter() instanceof NumberFormatter);
        typed(t, "57");
        assertEquals(Integer.valueOf(42), t.getValue());
        assertTrue(t.isEditValid());
        final List<Object> changes = new ArrayList<Object>();
        t.addPropertyChangeListener("value", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                changes.add(e.getNewValue());
            }
        });
        t.commitEdit();
        assertEquals(Integer.valueOf(57), t.getValue());
        assertEquals("[57]", changes.toString());
        typed(t, "seven");
        assertFalse(t.isEditValid());
        try {
            t.commitEdit();
            fail();
        } catch (ParseException expected) {
            assertEquals(Integer.valueOf(57), t.getValue());
        }
    }

    @Test
    public void losingTheFocusCommitsOrReverts() {
        JFormattedTextField t = shown(Integer.valueOf(1));
        typed(t, "25");
        t.dispatchEvent(new FocusEvent(t, FocusEvent.FOCUS_LOST));
        assertEquals(Integer.valueOf(25), t.getValue());
        typed(t, "junk");
        t.dispatchEvent(new FocusEvent(t, FocusEvent.FOCUS_LOST));
        assertEquals(Integer.valueOf(25), t.getValue());
        assertEquals("25", t.getText());
        t.setFocusLostBehavior(JFormattedTextField.PERSIST);
        typed(t, "junk");
        t.dispatchEvent(new FocusEvent(t, FocusEvent.FOCUS_LOST));
        assertEquals("junk", t.getText());
        t.setFocusLostBehavior(JFormattedTextField.REVERT);
        typed(t, "99");
        t.dispatchEvent(new FocusEvent(t, FocusEvent.FOCUS_LOST));
        assertEquals("25", t.getText());
        assertEquals(Integer.valueOf(25), t.getValue());
        try {
            t.setFocusLostBehavior(9);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals(JFormattedTextField.REVERT, t.getFocusLostBehavior());
        }
    }

    @Test
    public void finishingTheEditCommitsBeforeTheListenersHear() {
        final JFormattedTextField t = shown(Integer.valueOf(1));
        final List<Object> seen = new ArrayList<Object>();
        t.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                seen.add(t.getValue());
            }
        });
        typed(t, "8");
        t.postActionEvent();
        assertEquals("[8]", seen.toString());
        typed(t, "x");
        t.postActionEvent();
        assertEquals("[8]", seen.toString());
    }

    @Test
    public void aNumberFormatterKeepsToItsRange() throws Exception {
        NumberFormat nf = NumberFormat.getIntegerInstance();
        nf.setGroupingUsed(false);
        NumberFormatter f = new NumberFormatter(nf);
        f.setValueClass(Integer.class);
        f.setMinimum(Integer.valueOf(0));
        f.setMaximum(Integer.valueOf(100));
        assertEquals(Integer.valueOf(50), f.stringToValue("50"));
        assertEquals("1234", f.valueToString(Integer.valueOf(1234)));
        assertEquals("", f.valueToString(null));
        assertNull(f.stringToValue(""));
        try {
            f.stringToValue("101");
            fail();
        } catch (ParseException expected) {
            assertEquals(Integer.valueOf(100), f.getMaximum());
        }
        JFormattedTextField t = new JFormattedTextField(f);
        t.setValue(Integer.valueOf(7));
        assertEquals("7", t.getText());
        assertEquals(f, t.getFormatter());
    }

    @Test
    public void theDefaultFormatterReadsTheValueClassBack() throws Exception {
        DefaultFormatter f = new DefaultFormatter();
        assertEquals("abc", f.stringToValue("abc"));
        f.setValueClass(Double.class);
        assertEquals(Double.valueOf(1.5), f.stringToValue("1.5"));
        assertEquals("2.5", f.valueToString(Double.valueOf(2.5)));
        try {
            f.stringToValue("abc");
            fail();
        } catch (ParseException expected) {
            assertTrue(f.getAllowsInvalid());
        }
        JFormattedTextField text = new JFormattedTextField("hello");
        assertEquals("hello", text.getText());
        text.setText("bye");
        text.commitEdit();
        assertEquals("bye", text.getValue());
    }

    @Test
    public void anInputVerifierKeepsTheFocus() {
        JFrame f = new JFrame();
        f.setLayout(new FlowLayout());
        JTextField t = new JTextField("abc", 8);
        f.add(t);
        show(f);
        final int[] asked = {0};
        InputVerifier v = new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                asked[0]++;
                return ((JTextField) input).getText().length() > 0;
            }
        };
        t.setInputVerifier(v);
        assertEquals(v, t.getInputVerifier());
        t.dispatchEvent(new FocusEvent(t, FocusEvent.FOCUS_LOST));
        assertEquals(1, asked[0]);
        t.setText("");
        t.dispatchEvent(new FocusEvent(t, FocusEvent.FOCUS_LOST));
        assertEquals(2, asked[0]);
    }

    @Test
    public void anEditorPaneShowsMarkupAsText() {
        JEditorPane p = new JEditorPane("text/html; charset=utf-8",
                "<html><head><title>t</title></head><body><h1>Title</h1>\n<p>One &amp; <b>two</b><br>three</p>"
                + "<ul><li>a</li><li>b &lt; c</li></ul></body></html>");
        assertEquals("text/html", p.getContentType());
        assertEquals("Title\nOne & two\nthree\na\nb < c", p.getDocument().getLength() == 0 ? "" : docText(p));
        assertTrue(p.getText().startsWith("<html>"));
        p.setContentType("text/plain");
        p.setText("<b>kept</b>");
        assertEquals("<b>kept</b>", p.getText());
        JTextPane pane = new JTextPane();
        pane.setText("plain");
        assertEquals("plain", pane.getText());
        JFrame f = new JFrame();
        f.add(pane);
        show(f);
        assertEquals("plain", ((com.codename1.ui.TextArea) pane.cn1Peer()).getText());
    }

    private static String docText(JEditorPane p) {
        try {
            return p.getDocument().getText(0, p.getDocument().getLength());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
