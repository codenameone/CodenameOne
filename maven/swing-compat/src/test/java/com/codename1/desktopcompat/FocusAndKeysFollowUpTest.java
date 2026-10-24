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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.FlowLayout;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.java.awt.event.MouseAdapter;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.javax.swing.InputVerifier;
import com.codename1.desktopcompat.javax.swing.JButton;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JList;
import com.codename1.desktopcompat.javax.swing.JPanel;
import com.codename1.desktopcompat.javax.swing.JScrollPane;
import com.codename1.desktopcompat.javax.swing.JSplitPane;
import com.codename1.desktopcompat.javax.swing.KeyStroke;
import com.codename1.desktopcompat.rt.EventBridge;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/// What every component gets from `JComponent`: an input verifier that
/// can hold the focus, scrolling itself into view, and the keys of the
/// list and the split pane.
public class FocusAndKeysFollowUpTest extends WindowsTestBase {

    private static void key(Component target, int code) {
        JComponent.cn1KeyBindings(target, new KeyEvent(target, KeyEvent.KEY_PRESSED, 0L, 0, code,
                KeyEvent.CHAR_UNDEFINED));
    }

    @Test
    public void anInputVerifierOfAnyComponentHoldsTheFocus() {
        JFrame f = new JFrame("verify");
        f.getContentPane().setLayout(new FlowLayout());
        JButton a = new JButton("a");
        JButton b = new JButton("b");
        JButton help = new JButton("help");
        f.getContentPane().add(a);
        f.getContentPane().add(b);
        f.getContentPane().add(help);
        f.setSize(300, 200);
        show(f);
        final boolean[] valid = new boolean[1];
        final int[] asked = new int[1];
        InputVerifier v = new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                asked[0]++;
                return valid[0];
            }
        };
        a.setInputVerifier(v);
        assertSame(v, a.getInputVerifier());
        assertTrue(b.getVerifyInputWhenFocusTarget());
        assertTrue(a.requestFocusInWindow());
        assertTrue(a.isFocusOwner());

        assertFalse("the verifier refuses", b.requestFocusInWindow());
        assertTrue(a.isFocusOwner());
        assertEquals(1, asked[0]);

        help.setVerifyInputWhenFocusTarget(false);
        assertTrue("a target that does not ask takes the focus", help.requestFocusInWindow());
        assertTrue(help.isFocusOwner());
        assertEquals(1, asked[0]);

        assertTrue(a.requestFocusInWindow());
        valid[0] = true;
        assertTrue(b.requestFocusInWindow());
        assertTrue(b.isFocusOwner());
    }

    @Test
    public void aComponentScrollsItselfIntoView() {
        JFrame f = new JFrame("scroll");
        JPanel tall = new JPanel(null);
        tall.setPreferredSize(new Dimension(200, 2000));
        JButton far = new JButton("far");
        far.setBounds(10, 1500, 80, 30);
        tall.add(far);
        JScrollPane sp = new JScrollPane(tall);
        f.getContentPane().add(sp, BorderLayout.CENTER);
        f.setSize(300, 300);
        show(f);
        assertEquals(0, sp.getViewport().getViewPosition().y);
        far.scrollRectToVisible(new Rectangle(0, 0, 80, 30));
        Rectangle seen = sp.getViewport().getViewRect();
        assertTrue("the button is in view: " + seen, seen.y <= 1500 && seen.y + seen.height >= 1530);
    }

    @Test
    public void aListFollowsTheArrowKeys() {
        JFrame f = new JFrame("list");
        JList<String> list = new JList<String>(new String[] {"a", "b", "c", "d"});
        f.getContentPane().add(list, BorderLayout.CENTER);
        f.setSize(200, 300);
        show(f);
        assertEquals("selectNextRow", list.getInputMap().get(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)));
        list.setSelectedIndex(0);
        key(list, KeyEvent.VK_DOWN);
        key(list, KeyEvent.VK_DOWN);
        assertEquals(2, list.getSelectedIndex());
        key(list, KeyEvent.VK_UP);
        assertEquals(1, list.getSelectedIndex());
        key(list, KeyEvent.VK_END);
        assertEquals(3, list.getSelectedIndex());
        key(list, KeyEvent.VK_DOWN);
        assertEquals("it stops at the last row", 3, list.getSelectedIndex());
        key(list, KeyEvent.VK_HOME);
        assertEquals(0, list.getSelectedIndex());
    }

    @Test
    public void aListSelectsOnThePressAndTakesTheFocus() {
        JFrame f = new JFrame("press");
        JList<String> list = new JList<String>(new String[] {"a", "b", "c"});
        f.getContentPane().add(list, BorderLayout.CENTER);
        f.setSize(200, 300);
        show(f);
        Rectangle second = list.getCellBounds(1, 1);
        pressOn(f, list, second.x + 5, second.y + second.height / 2);
        assertEquals("selected before the release", 1, list.getSelectedIndex());
        assertTrue(list.isFocusOwner());
        releaseOn(f, list, second.x + 5, second.y + second.height / 2);
        assertEquals(1, list.getSelectedIndex());
    }

    @Test
    public void aSplitPaneMovesItsDividerWithTheKeysWhileItHasTheFocus() {
        JFrame f = new JFrame("split");
        JButton left = new JButton("left");
        left.setMinimumSize(new Dimension(20, 20));
        JButton right = new JButton("right");
        right.setMinimumSize(new Dimension(20, 20));
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        f.getContentPane().add(sp, BorderLayout.CENTER);
        f.setSize(400, 200);
        show(f);
        sp.setDividerLocation(150);
        left.requestFocusInWindow();
        key(left, KeyEvent.VK_RIGHT);
        assertEquals("a focused child keeps its arrow keys", 150, sp.getDividerLocation());
        sp.setFocusable(true);
        assertTrue(sp.requestFocusInWindow());
        key(sp, KeyEvent.VK_RIGHT);
        assertEquals(160, sp.getDividerLocation());
        key(sp, KeyEvent.VK_LEFT);
        key(sp, KeyEvent.VK_LEFT);
        assertEquals(140, sp.getDividerLocation());
        key(sp, KeyEvent.VK_HOME);
        assertEquals(sp.getMinimumDividerLocation(), sp.getDividerLocation());
        key(sp, KeyEvent.VK_END);
        assertEquals(sp.getMaximumDividerLocation(), sp.getDividerLocation());
    }

    @Test
    public void aClickCountBelongsToAPlaceAndCanBeReset() {
        JFrame f = new JFrame("clicks");
        JPanel p = new JPanel(null);
        final List<Integer> counts = new ArrayList<Integer>();
        p.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                counts.add(Integer.valueOf(e.getClickCount()));
            }
        });
        f.getContentPane().add(p, BorderLayout.CENTER);
        f.setSize(300, 300);
        show(f);
        click(f, p, 20, 20);
        click(f, p, 20, 20);
        click(f, p, 200, 200);
        assertEquals("a click somewhere else starts over", "[1, 2, 1]", counts.toString());
        counts.clear();
        click(f, p, 200, 200);
        EventBridge.resetClickCount();
        click(f, p, 200, 200);
        assertEquals("[2, 1]", counts.toString());
    }
}
