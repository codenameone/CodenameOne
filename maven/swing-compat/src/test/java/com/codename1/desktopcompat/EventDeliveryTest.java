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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.FlowLayout;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.awt.event.FocusEvent;
import com.codename1.desktopcompat.java.awt.event.FocusListener;
import com.codename1.desktopcompat.java.awt.event.ItemEvent;
import com.codename1.desktopcompat.java.awt.event.ItemListener;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.java.awt.event.KeyListener;
import com.codename1.desktopcompat.java.awt.event.MouseAdapter;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.java.awt.event.MouseMotionAdapter;
import com.codename1.desktopcompat.javax.swing.ButtonGroup;
import com.codename1.desktopcompat.javax.swing.JButton;
import com.codename1.desktopcompat.javax.swing.JCheckBox;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JPanel;
import com.codename1.desktopcompat.javax.swing.JRadioButton;
import com.codename1.desktopcompat.javax.swing.JTextField;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/// Input reaching a frame's form arrives as action, mouse, key and focus
/// events on the right component, in its logical coordinates.
public class EventDeliveryTest extends KernelTestBase {

    private static final class Mice extends MouseAdapter {
        final List<String> log = new ArrayList<String>();
        MouseEvent last;

        private void add(String what, MouseEvent e) {
            log.add(what);
            last = e;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            add("pressed", e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            add("released", e);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            add("clicked", e);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            add("entered", e);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            add("exited", e);
        }
    }

    private static JPanel sized(int w, int h) {
        JPanel p = new JPanel(null);
        p.setPreferredSize(new Dimension(w, h));
        return p;
    }

    @Test
    public void clickingAButtonFiresOneActionEvent() {
        JButton b = new JButton("Go");
        final List<ActionEvent> events = new ArrayList<ActionEvent>();
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                events.add(e);
            }
        });
        Mice mice = new Mice();
        b.addMouseListener(mice);
        JFrame f = new JFrame();
        f.add(b, BorderLayout.SOUTH);
        show(f);
        press(f, b, 5, 5);
        release(f, b, 5, 5);
        assertEquals(1, events.size());
        assertSame(b, events.get(0).getSource());
        assertEquals("Go", events.get(0).getActionCommand());
        assertTrue(mice.log.contains("pressed") && mice.log.contains("released") && mice.log.contains("clicked"));
    }

    @Test
    public void doClickAndTheNativeClickDriveTheModel() {
        JCheckBox box = new JCheckBox("Check");
        final int[] items = new int[1];
        final int[] actions = new int[1];
        box.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                items[0] = e.getStateChange();
            }
        });
        box.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actions[0]++;
            }
        });
        JFrame f = new JFrame();
        f.add(box, BorderLayout.SOUTH);
        show(f);
        box.doClick();
        assertTrue(box.isSelected());
        assertEquals(ItemEvent.SELECTED, items[0]);
        assertEquals(1, actions[0]);
        assertTrue(((com.codename1.ui.CheckBox) box.cn1Peer()).isSelected());
        press(f, box, 5, 5);
        release(f, box, 5, 5);
        assertFalse(box.isSelected());
        assertEquals(ItemEvent.DESELECTED, items[0]);
        assertEquals(2, actions[0]);
        assertFalse(((com.codename1.ui.CheckBox) box.cn1Peer()).isSelected());
    }

    @Test
    public void aButtonGroupKeepsOneRadioButtonSelected() {
        JRadioButton a = new JRadioButton("A", true);
        JRadioButton b = new JRadioButton("B");
        ButtonGroup group = new ButtonGroup();
        group.add(a);
        group.add(b);
        JFrame f = new JFrame();
        JPanel p = new JPanel(new FlowLayout());
        p.add(a);
        p.add(b);
        f.add(p, BorderLayout.CENTER);
        show(f);
        press(f, b, 5, 5);
        release(f, b, 5, 5);
        assertTrue(b.isSelected());
        assertFalse(a.isSelected());
        assertFalse(((com.codename1.ui.RadioButton) a.cn1Peer()).isSelected());
        press(f, b, 5, 5);
        release(f, b, 5, 5);
        assertTrue(b.isSelected());
        assertTrue(((com.codename1.ui.RadioButton) b.cn1Peer()).isSelected());
    }

    @Test
    public void mouseEventsCarryLogicalCoordinatesAndGoToTheListeningAncestor() {
        JPanel outer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JPanel pad = sized(30, 40);
        JPanel inner = sized(100, 60);
        outer.add(pad);
        outer.add(inner);
        Mice mice = new Mice();
        outer.addMouseListener(mice);
        JFrame f = new JFrame();
        f.add(outer, BorderLayout.CENTER);
        show(f);
        assertEquals(30, inner.getX());
        press(f, inner, 7, 9);
        assertEquals("entered", mice.log.get(0));
        assertEquals("pressed", mice.log.get(1));
        assertSame(outer, mice.last.getSource());
        assertEquals(37, mice.last.getX());
        assertEquals(9, mice.last.getY());
        assertEquals(MouseEvent.BUTTON1, mice.last.getButton());
        assertEquals(1, mice.last.getClickCount());
        release(f, inner, 7, 9);
        assertEquals("clicked", mice.log.get(mice.log.size() - 1));

        Mice innerMice = new Mice();
        inner.addMouseListener(innerMice);
        mice.log.clear();
        press(f, inner, 7, 9);
        assertSame(inner, innerMice.last.getSource());
        assertEquals(7, innerMice.last.getX());
        assertEquals(2, innerMice.last.getClickCount());
        assertEquals("exited", mice.log.get(0));
        release(f, inner, 7, 9);
    }

    @Test
    public void aDragGoesToThePressedComponentAndEndsWithoutAClick() {
        JPanel a = sized(50, 50);
        JPanel b = sized(50, 50);
        JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        content.add(a);
        content.add(b);
        Mice mice = new Mice();
        a.addMouseListener(mice);
        final List<MouseEvent> drags = new ArrayList<MouseEvent>();
        a.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                drags.add(e);
            }
        });
        JFrame f = new JFrame();
        f.add(content, BorderLayout.CENTER);
        show(f);
        press(f, a, 10, 10);
        drag(f, b, 20, 30);
        release(f, b, 20, 30);
        assertEquals(1, drags.size());
        assertSame(a, drags.get(0).getSource());
        assertEquals(70, drags.get(0).getX());
        assertEquals(30, drags.get(0).getY());
        assertTrue(mice.log.contains("released"));
        assertFalse(mice.log.contains("clicked"));
    }

    @Test
    public void keysGoToTheFocusOwnerElseToTheWindow() {
        final List<String> log = new ArrayList<String>();
        KeyListener keys = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                log.add("typed " + e.getKeyChar() + " " + name(e));
            }

            @Override
            public void keyPressed(KeyEvent e) {
                log.add("pressed " + e.getKeyCode() + " " + name(e));
            }

            @Override
            public void keyReleased(KeyEvent e) {
                log.add("released " + e.getKeyCode() + " " + name(e));
            }

            private String name(KeyEvent e) {
                return e.getComponent().getName();
            }
        };
        JFrame f = new JFrame();
        f.setName("frame");
        JPanel panel = new JPanel();
        panel.setName("panel");
        f.add(panel, BorderLayout.CENTER);
        f.addKeyListener(keys);
        panel.addKeyListener(keys);
        show(f);
        f.cn1Form().keyPressed('a');
        f.cn1Form().keyReleased('a');
        assertEquals("[pressed " + KeyEvent.VK_A + " frame, typed a frame, released " + KeyEvent.VK_A + " frame]",
                log.toString());
        log.clear();
        assertTrue(panel.requestFocusInWindow());
        assertTrue(panel.isFocusOwner());
        f.cn1Form().keyPressed('7');
        assertEquals("[pressed " + KeyEvent.VK_7 + " panel, typed 7 panel]", log.toString());
    }

    @Test
    public void focusMovesWithFocusLostAndFocusGained() {
        final List<String> log = new ArrayList<String>();
        final JPanel a = new JPanel();
        final JPanel b = new JPanel();
        FocusListener l = new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                log.add((e.getSource() == a ? "a" : "b") + " gained from "
                        + (e.getOppositeComponent() == null ? "none" : e.getOppositeComponent() == a ? "a" : "b"));
            }

            @Override
            public void focusLost(FocusEvent e) {
                log.add((e.getSource() == a ? "a" : "b") + " lost");
            }
        };
        a.addFocusListener(l);
        b.addFocusListener(l);
        JFrame f = new JFrame();
        JPanel content = new JPanel();
        content.add(a);
        content.add(b);
        f.add(content, BorderLayout.CENTER);
        show(f);
        a.requestFocus();
        b.requestFocus();
        assertEquals("[a gained from none, a lost, b gained from a]", log.toString());
        assertSame(b, f.getFocusOwner());
        content.remove(b);
        assertNull(f.getFocusOwner());
        b.setFocusable(false);
        assertFalse(b.requestFocusInWindow());
    }

    @Test
    public void aTextFieldKeepsItsTextInThePeerAndPostsActions() {
        JTextField field = new JTextField("abc", 10);
        final String[] command = new String[1];
        field.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                command[0] = e.getActionCommand();
            }
        });
        JFrame f = new JFrame();
        f.add(field, BorderLayout.NORTH);
        show(f);
        com.codename1.ui.TextField peer = (com.codename1.ui.TextField) field.cn1Peer();
        assertEquals("abc", peer.getText());
        peer.setText("typed");
        assertEquals("typed", field.getText());
        field.postActionEvent();
        assertEquals("typed", command[0]);
        field.setText("set");
        assertEquals("set", peer.getText());
    }
}
