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

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.Label;
import com.codename1.desktopcompat.java.awt.ScrollPane;
import com.codename1.desktopcompat.java.awt.TextComponent;
import com.codename1.desktopcompat.java.beans.BeanInfo;
import com.codename1.desktopcompat.java.beans.Beans;
import com.codename1.desktopcompat.java.beans.Encoder;
import com.codename1.desktopcompat.java.beans.ExceptionListener;
import com.codename1.desktopcompat.java.beans.Expression;
import com.codename1.desktopcompat.java.beans.Introspector;
import com.codename1.desktopcompat.java.beans.PersistenceDelegate;
import com.codename1.desktopcompat.java.beans.XMLDecoder;
import com.codename1.desktopcompat.java.beans.XMLEncoder;
import com.codename1.desktopcompat.javax.swing.DefaultListCellRenderer;
import com.codename1.desktopcompat.javax.swing.DefaultListModel;
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.javax.swing.JList;
import com.codename1.desktopcompat.javax.swing.JPanel;
import com.codename1.desktopcompat.javax.swing.JScrollPane;
import com.codename1.desktopcompat.javax.swing.JTextField;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// What the first complete application built on the layer needed that the
/// layer's own tests had not asked for: the classes a bundled library names
/// on paths a device never takes, and a font for a component that is in no
/// window.
public class LibraryLinkageTest extends KernelTestBase {

    /// MigLayout asks every component whether it is one of AWT's heavyweight
    /// widgets before it picks a baseline and a gap for it. The types exist
    /// so that the question links; no Swing component is one, and none can
    /// be made.
    @Test
    public void noSwingComponentIsAnAwtWidget() {
        Component[] all = {new JLabel("a"), new JTextField("b"), new JScrollPane(new JPanel()), new JPanel()};
        for (Component c : all) {
            assertFalse(c instanceof Label);
            assertFalse(c instanceof TextComponent);
            assertFalse(c instanceof ScrollPane);
        }
        try {
            new Label();
            fail("There is no heavyweight label to make");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("javax.swing.JLabel"));
        }
        try {
            new ScrollPane();
            fail("There is no heavyweight scroll pane to make");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("javax.swing.JScrollPane"));
        }
    }

    @Test
    public void anApplicationIsNeverInsideAGuiBuilder() {
        assertFalse(Beans.isDesignTime());
        assertTrue(Beans.isGuiAvailable());
        Beans.setDesignTime(true);
        try {
            assertTrue(Beans.isDesignTime());
        } finally {
            Beans.setDesignTime(false);
        }
    }

    /// A library keeps per-class data in the bean descriptor: the same
    /// descriptor has to come back for the same class.
    @Test
    public void aBeanDescriptorIsRememberedPerClass() throws Exception {
        Introspector.flushCaches();
        BeanInfo info = Introspector.getBeanInfo(JPanel.class, Introspector.IGNORE_ALL_BEANINFO);
        assertEquals("JPanel", info.getBeanDescriptor().getName());
        assertSame(JPanel.class, info.getBeanDescriptor().getBeanClass());
        assertNull(info.getBeanDescriptor().getValue("persistenceDelegate"));
        info.getBeanDescriptor().setValue("persistenceDelegate", "mine");
        assertEquals("mine", Introspector.getBeanInfo(JPanel.class).getBeanDescriptor().getValue("persistenceDelegate"));
        assertNull(Introspector.getBeanInfo(JLabel.class).getBeanDescriptor().getValue("persistenceDelegate"));
        Introspector.flushFromCaches(JPanel.class);
        assertNull(Introspector.getBeanInfo(JPanel.class).getBeanDescriptor().getValue("persistenceDelegate"));
        try {
            Introspector.getBeanInfo(JPanel.class, 9);
            fail("9 is none of the three flags");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
        assertEquals("fooBah", Introspector.decapitalize("FooBah"));
        assertEquals("x", Introspector.decapitalize("X"));
        assertEquals("URL", Introspector.decapitalize("URL"));
        assertEquals("", Introspector.decapitalize(""));
    }

    /// Long-term persistence cannot run without reflection. What a library
    /// does around it -- make an encoder, set a listener, register a
    /// delegate, close it -- works, and the write itself says why it cannot.
    @Test
    public void persistenceCanBeSetUpAndSaysWhyItCannotRun() throws Exception {
        XMLEncoder encoder = new XMLEncoder(new ByteArrayOutputStream());
        ExceptionListener listener = new ExceptionListener() {
            @Override
            public void exceptionThrown(Exception e) {
            }
        };
        encoder.setExceptionListener(listener);
        assertSame(listener, encoder.getExceptionListener());
        PersistenceDelegate delegate = new PersistenceDelegate() {
            @Override
            protected Expression instantiate(Object oldInstance, Encoder out) {
                return new Expression(oldInstance, JPanel.class, "new", new Object[0]);
            }
        };
        encoder.setPersistenceDelegate(JPanel.class, delegate);
        assertSame(delegate, encoder.getPersistenceDelegate(JPanel.class));
        try {
            encoder.writeObject(new JPanel());
            fail("Nothing can be written without reflection");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("XMLEncoder"));
        }
        encoder.flush();
        encoder.close();

        Object value = new Object();
        Expression known = new Expression(value, JPanel.class, "new", null);
        assertSame(value, known.getValue());
        assertEquals("new", known.getMethodName());
        assertSame(JPanel.class, known.getTarget());
        assertEquals(0, known.getArguments().length);
        try {
            new Expression(JPanel.class, "new", new Object[0]).getValue();
            fail("A value that has to be computed needs the call");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("reflection"));
        }

        XMLDecoder decoder = new XMLDecoder(new ByteArrayInputStream(new byte[0]));
        try {
            decoder.readObject();
            fail("Nothing can be read without reflection");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("XMLDecoder"));
        }
        decoder.close();
    }

    /// `getFont().deriveFont(...)` in a cell renderer: on a desktop the look
    /// and feel gave every Swing component a font, so the idiom is written
    /// without a null check, and a renderer is in no window to inherit from.
    @Test
    public void aComponentInNoWindowHasAFont() {
        JLabel alone = new JLabel("alone");
        assertNotNull(alone.getFont());
        assertFalse("Answering a default is not setting one", alone.isFontSet());
        assertEquals(Font.BOLD, alone.getFont().deriveFont(Font.BOLD).getStyle());

        DefaultListModel<String> model = new DefaultListModel<String>();
        model.addElement("one");
        model.addElement("two");
        JList<String> list = new JList<String>(model);
        final boolean[] called = new boolean[1];
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean selected,
                                                          boolean focus) {
                super.getListCellRendererComponent(l, value, index, selected, focus);
                setFont(getFont().deriveFont(Font.BOLD));
                called[0] = true;
                return this;
            }
        });
        // Measures the rows through the renderer, before anything is shown.
        JScrollPane scroll = new JScrollPane(list);
        assertNotNull(scroll.getPreferredSize());
        assertTrue(list.getPreferredSize().height > 0);
        assertTrue(called[0]);

        Font mine = new Font(Font.DIALOG, Font.ITALIC, 17);
        JPanel parent = new JPanel();
        parent.setFont(mine);
        parent.add(alone);
        assertSame("A parent's font still wins over the default", mine, alone.getFont());
    }
}
