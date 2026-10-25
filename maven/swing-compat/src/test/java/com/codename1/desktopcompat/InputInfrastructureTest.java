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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.FlowLayout;
import com.codename1.desktopcompat.java.awt.KeyboardFocusManager;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.awt.event.InputEvent;
import com.codename1.desktopcompat.java.awt.event.KeyAdapter;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.java.awt.event.MouseAdapter;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.java.awt.event.MouseWheelEvent;
import com.codename1.desktopcompat.java.awt.event.MouseWheelListener;
import com.codename1.desktopcompat.javax.swing.AbstractAction;
import com.codename1.desktopcompat.javax.swing.JButton;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JPanel;
import com.codename1.desktopcompat.javax.swing.KeyStroke;
import com.codename1.desktopcompat.javax.swing.SwingUtilities;
import com.codename1.desktopcompat.rt.EventBridge;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/// Modifiers, buttons, click counts, enter and exit, the wheel, key
/// bindings under each condition, key typed, and focus traversal.
public class InputInfrastructureTest extends WindowsTestBase {

    private static JPanel sized(int w, int h) {
        JPanel p = new JPanel();
        p.setPreferredSize(new Dimension(w, h));
        return p;
    }

    private static final class Mice extends MouseAdapter {
        final List<String> log = new ArrayList<String>();
        final List<MouseEvent> events = new ArrayList<MouseEvent>();

        private void add(String s, MouseEvent e) {
            log.add(s);
            events.add(e);
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

        MouseEvent last(String kind) {
            for (int i = log.size() - 1; i >= 0; i--) {
                if (kind.equals(log.get(i))) {
                    return events.get(i);
                }
            }
            return null;
        }
    }

    private static final class Counter extends AbstractAction {
        int count;
        ActionEvent last;

        @Override
        public void actionPerformed(ActionEvent e) {
            count++;
            last = e;
        }
    }

    private JFrame frame(JPanel content) {
        JFrame f = new JFrame("input");
        f.add(content, BorderLayout.CENTER);
        return show(f);
    }

    @Test
    public void mouseEventsCarryTheModifiersAndTheButton() {
        JPanel p = sized(100, 100);
        Mice mice = new Mice();
        p.addMouseListener(mice);
        JFrame f = frame(p);
        input.modifiers = InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK;
        input.button = 2;
        click(f, p, 10, 10);
        MouseEvent press = mice.last("pressed");
        assertTrue(press.isShiftDown());
        assertTrue(press.isControlDown());
        assertFalse(press.isAltDown());
        assertEquals(MouseEvent.BUTTON2, press.getButton());
        assertTrue((press.getModifiersEx() & InputEvent.BUTTON2_DOWN_MASK) != 0);
        assertTrue(SwingUtilities.isMiddleMouseButton(press));
        assertFalse(SwingUtilities.isLeftMouseButton(press));
        MouseEvent release = mice.last("released");
        assertEquals(MouseEvent.BUTTON2, release.getButton());
        assertEquals(0, release.getModifiersEx() & InputEvent.BUTTON2_DOWN_MASK);
        assertTrue(mice.last("clicked").isShiftDown());
    }

    @Test
    public void clicksInOnePlaceCountUpAndAClickElsewhereStartsAgain() {
        JPanel p = sized(200, 200);
        Mice mice = new Mice();
        p.addMouseListener(mice);
        JFrame f = frame(p);
        click(f, p, 20, 20);
        assertEquals(1, mice.last("clicked").getClickCount());
        click(f, p, 20, 20);
        assertEquals(2, mice.last("pressed").getClickCount());
        assertEquals(2, mice.last("clicked").getClickCount());
        click(f, p, 20, 20);
        assertEquals(3, mice.last("clicked").getClickCount());
        click(f, p, 120, 120);
        assertEquals(1, mice.last("clicked").getClickCount());
        input.button = 3;
        click(f, p, 120, 120);
        assertEquals(1, mice.last("clicked").getClickCount());
    }

    @Test
    public void aDisabledComponentGetsNoMouseEvents() {
        JPanel p = sized(100, 100);
        Mice mice = new Mice();
        p.addMouseListener(mice);
        JFrame f = frame(p);
        p.setEnabled(false);
        click(f, p, 10, 10);
        assertTrue(mice.log.toString(), mice.log.isEmpty());
        p.setEnabled(true);
        click(f, p, 10, 10);
        assertTrue(mice.log.contains("clicked"));
    }

    @Test
    public void thePointerEntersOneComponentAndExitsTheOther() {
        JPanel outer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JPanel a = sized(50, 50);
        JPanel b = sized(50, 50);
        outer.add(a);
        outer.add(b);
        Mice onA = new Mice();
        Mice onB = new Mice();
        a.addMouseListener(onA);
        b.addMouseListener(onB);
        JFrame f = frame(outer);
        pressOn(f, a, 5, 5);
        releaseOn(f, a, 5, 5);
        assertEquals("entered", onA.log.get(0));
        pressOn(f, b, 5, 5);
        assertEquals("exited", onA.log.get(onA.log.size() - 1));
        assertEquals("entered", onB.log.get(0));
        assertEquals("pressed", onB.log.get(1));
        releaseOn(f, b, 5, 5);
        EventBridge.pointerExit(f);
        assertEquals("exited", onB.log.get(onB.log.size() - 1));
    }

    @Test
    public void theSecondaryButtonIsThePopupTrigger() {
        JPanel p = sized(100, 100);
        Mice mice = new Mice();
        p.addMouseListener(mice);
        JFrame f = frame(p);
        click(f, p, 10, 10);
        assertFalse(mice.last("pressed").isPopupTrigger());
        input.button = 3;
        click(f, p, 10, 10);
        assertTrue(mice.last("pressed").isPopupTrigger());
        assertTrue(SwingUtilities.isRightMouseButton(mice.last("pressed")));
    }

    @Test
    public void theWheelTurnsTheWayCodenameOneReportsIt() {
        JPanel p = sized(100, 100);
        final List<MouseWheelEvent> got = new ArrayList<MouseWheelEvent>();
        p.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                got.add(e);
            }
        });
        JFrame f = frame(p);
        int[] at = at(p, 10, 10);
        // Codename One: a positive vertical delta is the wheel turned
        // away from the user, which AWT reports as a negative rotation.
        EventBridge.wheel(f, new com.codename1.ui.events.WheelEvent(f.cn1Form(), at[0], at[1], 0, 30, false, 0));
        assertEquals(1, got.size());
        assertEquals(-1, got.get(0).getWheelRotation());
        assertFalse(got.get(0).isShiftDown());
        EventBridge.wheel(f, new com.codename1.ui.events.WheelEvent(f.cn1Form(), at[0], at[1], 0, -30, false, 0));
        assertEquals(1, got.get(1).getWheelRotation());
        EventBridge.wheel(f, new com.codename1.ui.events.WheelEvent(f.cn1Form(), at[0], at[1], 30, 0, false, 0));
        assertTrue(got.get(2).isShiftDown());
    }

    @Test
    public void aKeyIsPressedTypedAndReleasedWithItsVirtualKeyAndCharacter() {
        JPanel p = sized(100, 100);
        final List<String> log = new ArrayList<String>();
        JFrame f = frame(p);
        f.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                log.add("pressed " + e.getKeyCode() + " " + e.getKeyChar());
            }

            @Override
            public void keyTyped(KeyEvent e) {
                log.add("typed " + e.getKeyCode() + " " + e.getKeyChar());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                log.add("released " + e.getKeyCode() + " " + e.isShiftDown());
            }
        });
        type(f, 'a');
        assertEquals("pressed " + KeyEvent.VK_A + " a", log.get(0));
        assertEquals("typed " + KeyEvent.VK_UNDEFINED + " a", log.get(1));
        assertEquals("released " + KeyEvent.VK_A + " false", log.get(2));
        log.clear();
        input.modifiers = InputEvent.SHIFT_DOWN_MASK;
        type(f, 'A');
        assertEquals("pressed " + KeyEvent.VK_A + " A", log.get(0));
        assertEquals("released " + KeyEvent.VK_A + " true", log.get(2));
        log.clear();
        input.control();
        type(f, 's');
        assertEquals("no key typed with control held: " + log, 2, log.size());
    }

    @Test
    public void aBindingRunsWhenItsComponentHasTheFocus() {
        JPanel p = new JPanel(new FlowLayout());
        JButton a = new JButton("A");
        JButton b = new JButton("B");
        p.add(a);
        p.add(b);
        JFrame f = frame(p);
        Counter action = new Counter();
        a.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, 0), "x");
        a.getActionMap().put("x", action);
        b.requestFocusInWindow();
        type(f, 'x');
        assertEquals(0, action.count);
        a.requestFocusInWindow();
        type(f, 'x');
        assertEquals(1, action.count);
        assertSame(a, action.last.getSource());
        action.setEnabled(false);
        type(f, 'x');
        assertEquals(1, action.count);
    }

    @Test
    public void aBindingOfAnAncestorRunsForAFocusedDescendant() {
        JPanel left = new JPanel(new FlowLayout());
        JPanel right = new JPanel(new FlowLayout());
        JButton inLeft = new JButton("L");
        JButton inRight = new JButton("R");
        left.add(inLeft);
        right.add(inRight);
        JPanel both = new JPanel(new FlowLayout());
        both.add(left);
        both.add(right);
        JFrame f = frame(both);
        Counter action = new Counter();
        left.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "d");
        left.getActionMap().put("d", action);
        inRight.requestFocusInWindow();
        type(f, 'd');
        assertEquals(0, action.count);
        inLeft.requestFocusInWindow();
        type(f, 'd');
        assertEquals(1, action.count);
        assertSame(left, action.last.getSource());
    }

    @Test
    public void aWindowBindingRunsWhereverTheFocusIsAndNeedsItsModifiers() {
        JPanel p = new JPanel(new FlowLayout());
        JButton a = new JButton("A");
        JPanel other = sized(20, 20);
        p.add(a);
        p.add(other);
        JFrame f = frame(p);
        final List<String> commands = new ArrayList<String>();
        other.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                commands.add(e.getActionCommand());
            }
        }, "save", KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        assertEquals(JComponent.WHEN_IN_FOCUSED_WINDOW,
                other.getConditionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK)));
        assertEquals(1, other.getRegisteredKeyStrokes().length);
        type(f, 's');
        assertTrue(commands.isEmpty());
        input.control();
        type(f, 's');
        assertEquals(1, commands.size());
        assertEquals("save", commands.get(0));
        a.requestFocusInWindow();
        type(f, 's');
        assertEquals(2, commands.size());
        other.unregisterKeyboardAction(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        type(f, 's');
        assertEquals(2, commands.size());
    }

    @Test
    public void aKeyListenerThatConsumesTheKeyKeepsItFromTheBindings() {
        JPanel p = new JPanel(new FlowLayout());
        JButton a = new JButton("A");
        p.add(a);
        JFrame f = frame(p);
        Counter action = new Counter();
        a.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, 0), "x");
        a.getActionMap().put("x", action);
        a.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                e.consume();
            }
        });
        a.requestFocusInWindow();
        type(f, 'x');
        assertEquals(0, action.count);
    }

    @Test
    public void aBindingOnTheReleaseOfAKeyAndOneOnATypedCharacter() {
        JPanel p = sized(50, 50);
        JFrame f = frame(p);
        Counter released = new Counter();
        Counter typed = new Counter();
        p.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0, true), "r");
        p.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('q'), "q");
        p.getActionMap().put("r", released);
        p.getActionMap().put("q", typed);
        f.cn1Form().keyPressed('r');
        assertEquals(0, released.count);
        f.cn1Form().keyReleased('r');
        assertEquals(1, released.count);
        type(f, 'q');
        assertEquals(1, typed.count);
    }

    @Test
    public void theTabKeyMovesTheFocusForwardAndWithShiftBackward() {
        JPanel p = new JPanel(new FlowLayout());
        JButton a = new JButton("A");
        JButton b = new JButton("B");
        JButton c = new JButton("C");
        p.add(a);
        p.add(b);
        p.add(c);
        JFrame f = frame(p);
        a.requestFocusInWindow();
        KeyboardFocusManager m = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        assertSame(a, m.getFocusOwner());
        assertSame(f, m.getActiveWindow());
        type(f, '\t');
        assertSame(b, m.getFocusOwner());
        b.setEnabled(true);
        c.setEnabled(false);
        type(f, '\t');
        assertSame("a disabled component is skipped", a, m.getFocusOwner());
        input.modifiers = InputEvent.SHIFT_DOWN_MASK;
        type(f, '\t');
        assertSame(b, m.getFocusOwner());
        input.modifiers = 0;
        b.transferFocus();
        assertSame(a, m.getFocusOwner());
        assertNotNull(m.getDefaultFocusTraversalPolicy());
        assertNull(f.getFocusTraversalPolicy());
        m.clearGlobalFocusOwner();
        assertNull(m.getFocusOwner());
    }

    @Test
    public void theToolTipIsTheToolTipOfTheCodenameOneWidget() {
        JPanel p = new JPanel(new FlowLayout());
        JButton a = new JButton("A");
        a.setToolTipText("before");
        p.add(a);
        frame(p);
        assertEquals("before", a.cn1Peer().getTooltip());
        a.setToolTipText("after");
        assertEquals("after", a.cn1Peer().getTooltip());
        assertEquals("after", a.getToolTipText());
    }
}
