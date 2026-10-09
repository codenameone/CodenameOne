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
import static org.junit.Assert.assertTrue;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.desktopcompat.java.awt.FlowLayout;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.javax.swing.JButton;
import com.codename1.desktopcompat.javax.swing.JCheckBox;
import com.codename1.desktopcompat.javax.swing.JFrame;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/// The keyboard as the form of a desktop port delivers it: the key codes
/// of that port, through the form, with the form free to do with the key
/// what a form of a desktop does -- move the focus on Tab by its own
/// rules. The focus has to end where Swing puts it, and a key has to act
/// once.
public class PortKeyboardTest extends WindowsTestBase {

    private static final int FIRE = -90;
    private static final int TAB = 9;

    private final List<String> seen = new ArrayList<String>();

    @Before
    public void desk() {
        HeadlessImplementation.setDesktop(true);
    }

    @After
    public void noDesk() {
        HeadlessImplementation.setDesktop(false);
    }

    private JButton button(final String name) {
        JButton b = new JButton(name);
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                seen.add(name);
            }
        });
        return b;
    }

    private static void key(JFrame f, int code) {
        f.cn1Form().keyPressed(code);
        f.cn1Form().keyReleased(code);
        MainThreadRule.drain();
    }

    @Test
    public void tabMovesTheFocusOneComponentOnAndTheKeyThatFiresActsOnce() {
        JFrame f = new JFrame("keys");
        f.getContentPane().setLayout(new FlowLayout());
        JButton a = button("a");
        JButton b = button("b");
        JCheckBox c = new JCheckBox("c");
        JButton d = button("d");
        f.getContentPane().add(a);
        f.getContentPane().add(b);
        f.getContentPane().add(c);
        f.getContentPane().add(d);
        f.setSize(400, 200);
        show(f);
        assertTrue(a.requestFocusInWindow());
        key(f, TAB);
        assertTrue("one Tab is one step", b.isFocusOwner());
        key(f, FIRE);
        assertEquals("[b]", seen.toString());
        key(f, TAB);
        assertTrue(c.isFocusOwner());
        key(f, FIRE);
        assertTrue(c.isSelected());
        key(f, TAB);
        assertTrue(d.isFocusOwner());
        key(f, 'q');
        key(f, 16);
        assertTrue("a key nothing wants leaves the focus alone", d.isFocusOwner());
        key(f, FIRE);
        assertEquals("[b, d]", seen.toString());
    }

    /// A text field is edited by the port's own editor, which takes Tab
    /// itself and asks the form for the component after the one it edits.
    /// The answer has to be the next component Swing would focus.
    @Test
    public void theComponentAfterATextFieldIsTheOneThePortEditsNext() {
        JFrame f = new JFrame("fields");
        f.getContentPane().setLayout(new FlowLayout());
        com.codename1.desktopcompat.javax.swing.JLabel l = new com.codename1.desktopcompat.javax.swing.JLabel("Name");
        com.codename1.desktopcompat.javax.swing.JTextField name =
                new com.codename1.desktopcompat.javax.swing.JTextField("Ada", 10);
        com.codename1.desktopcompat.javax.swing.JPasswordField pw =
                new com.codename1.desktopcompat.javax.swing.JPasswordField(10);
        f.getContentPane().add(l);
        f.getContentPane().add(name);
        f.getContentPane().add(pw);
        f.getContentPane().add(button("ok"));
        f.setSize(400, 200);
        show(f);
        com.codename1.ui.Component next = f.cn1Form().getNextComponent(name.cn1Peer());
        assertTrue("after the name comes " + next, next == pw.cn1Peer());
        assertTrue(f.cn1Form().getPreviousComponent(pw.cn1Peer()) == name.cn1Peer());
    }

    /// A button narrower than its text keeps the text still with the
    /// focus on it; the native label scrolled it back and forth.
    @Test
    public void theTextOfANarrowButtonDoesNotScroll() {
        JFrame f = new JFrame("narrow");
        f.getContentPane().setLayout(null);
        JButton b = button("A button with more text than room");
        f.getContentPane().add(b);
        b.setBounds(10, 10, 60, 30);
        f.setSize(400, 200);
        show(f);
        assertTrue(b.cn1Peer() instanceof com.codename1.ui.Label);
        com.codename1.ui.Label label = (com.codename1.ui.Label) b.cn1Peer();
        assertTrue(b.requestFocusInWindow());
        label.requestFocus();
        MainThreadRule.drain();
        assertTrue("the text is scrolling", !label.isTickerRunning());
    }

    @Test
    public void enterFiresTheDefaultButton() {
        JFrame f = new JFrame("default");
        f.getContentPane().setLayout(new FlowLayout());
        JCheckBox c = new JCheckBox("c");
        JButton ok = button("ok");
        f.getContentPane().add(c);
        f.getContentPane().add(ok);
        f.getRootPane().setDefaultButton(ok);
        f.setSize(400, 200);
        show(f);
        assertTrue(c.requestFocusInWindow());
        key(f, 10);
        assertEquals("[ok]", seen.toString());
    }
}
