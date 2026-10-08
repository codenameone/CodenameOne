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
package com.codename1.desktopcompat.org.jdesktop.swingx;

import com.codename1.desktopcompat.WindowsTestBase;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Cursor;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.javax.swing.AbstractAction;
import com.codename1.desktopcompat.javax.swing.AbstractButton;
import com.codename1.desktopcompat.javax.swing.Action;
import com.codename1.desktopcompat.javax.swing.Icon;
import com.codename1.desktopcompat.javax.swing.JButton;
import com.codename1.desktopcompat.javax.swing.JDialog;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.javax.swing.JPanel;
import com.codename1.desktopcompat.javax.swing.JScrollPane;
import com.codename1.desktopcompat.javax.swing.JTextArea;
import com.codename1.desktopcompat.javax.swing.KeyStroke;
import com.codename1.desktopcompat.org.jdesktop.swingx.action.AbstractActionExt;
import com.codename1.desktopcompat.org.jdesktop.swingx.error.ErrorEvent;
import com.codename1.desktopcompat.org.jdesktop.swingx.error.ErrorListener;
import com.codename1.desktopcompat.org.jdesktop.swingx.error.ErrorSupport;
import com.codename1.desktopcompat.rt.WindowHosts;
import com.codename1.ui.Display;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/// The SwingX windows and what hangs off the action map: the error pane
/// and its dialog, the extended frame and dialog, the collapsible pane's
/// toggle action and the accelerator of an extended action.
public class WindowsAndErrorsTest extends WindowsTestBase {

    private final List<String> order = new ArrayList<String>();

    /// Runs `r` on the event thread once the dialog the test is about to
    /// open is on the screen and the test is blocked in it.
    private void whileBlocked(final Runnable r) {
        Thread later = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    return;
                }
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            r.run();
                            order.add("answered");
                        } catch (RuntimeException e) {
                            order.add("failed: " + e);
                            closeTop();
                        } catch (AssertionError e) {
                            order.add("failed: " + e);
                            closeTop();
                        }
                    }
                });
            }
        });
        later.start();
    }

    private static void closeTop() {
        Window w = WindowHosts.active();
        if (w instanceof JDialog) {
            w.setVisible(false);
        }
    }

    private static <T> T find(Component root, Class<T> type, String text) {
        if (type.isInstance(root)) {
            boolean match = text == null;
            if (!match && root instanceof AbstractButton) {
                match = text.equals(((AbstractButton) root).getText());
            }
            if (!match && root instanceof JLabel) {
                match = text.equals(((JLabel) root).getText());
            }
            if (match) {
                return type.cast(root);
            }
        }
        if (root instanceof Container) {
            Component[] kids = ((Container) root).getComponents();
            for (int i = 0; i < kids.length; i++) {
                T t = find(kids[i], type, text);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }

    private static final class Dot implements Icon {

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
        }

        @Override
        public int getIconWidth() {
            return 8;
        }

        @Override
        public int getIconHeight() {
            return 8;
        }
    }

    @Test
    public void anErrorDialogBlocksShowsTheMessageAndOpensTheTrace() {
        final Throwable cause = new IllegalArgumentException("bad input");
        final Throwable error = new IllegalStateException("It broke", cause);
        whileBlocked(new Runnable() {
            @Override
            public void run() {
                Window top = WindowHosts.active();
                assertTrue("a dialog is showing: " + top, top instanceof JDialog);
                JDialog d = (JDialog) top;
                assertEquals("Error", d.getTitle());
                assertTrue(d.isModal());
                assertNotNull("the message", find(d, JLabel.class, "It broke"));
                JScrollPane scroll = find(d, JScrollPane.class, null);
                assertNotNull(scroll);
                assertFalse("the trace starts closed", scroll.isVisible());
                int closed = d.getHeight();
                find(d, JButton.class, "Details >>").doClick();
                assertTrue(scroll.isVisible());
                assertTrue("the dialog grew: " + closed + " -> " + d.getHeight(), d.getHeight() > closed);
                String trace = find(d, JTextArea.class, null).getText();
                assertTrue(trace, trace.startsWith("java.lang.IllegalStateException: It broke\n"));
                assertTrue(trace, trace.indexOf("Caused by: java.lang.IllegalArgumentException: bad input\n") > 0);
                find(d, JButton.class, "Details <<").doClick();
                assertFalse(scroll.isVisible());
                JButton close = find(d, JButton.class, "Close");
                assertSame(close, d.getRootPane().getDefaultButton());
                close.doClick();
            }
        });
        JXErrorPane.showDialog(error);
        order.add("returned");
        assertEquals("[answered, returned]", order.toString());
        assertFalse("the dialog is gone", WindowHosts.active() instanceof JDialog);
    }

    @Test
    public void anErrorPaneDialogBelongsToTheOwnersWindow() {
        JFrame f = show(new JFrame("owner"));
        JPanel p = new JPanel();
        f.add(p);
        JXErrorPane pane = new JXErrorPane();
        assertEquals("ErrorPaneUI", pane.getUIClassID());
        Icon dot = new Dot();
        pane.setIcon(dot);
        assertSame(dot, pane.getIcon());
        JDialog d = JXErrorPane.createDialog(p, pane);
        assertSame(f, d.getOwner());
        assertFalse(d.isVisible());
        assertSame(dot, find(d, JLabel.class, null).getIcon());
        assertFalse("nothing to detail", find(d, JButton.class, "Details >>").isEnabled());
        d.dispose();
    }

    @Test
    public void errorSupportTellsItsListeners() {
        final List<ErrorEvent> seen = new ArrayList<ErrorEvent>();
        ErrorSupport support = new ErrorSupport(this);
        ErrorListener l = new ErrorListener() {
            @Override
            public void errorOccured(ErrorEvent event) {
                seen.add(event);
            }
        };
        support.addErrorListener(l);
        assertEquals(1, support.getErrorListeners().length);
        Throwable t = new RuntimeException("x");
        support.fireErrorEvent(t);
        assertEquals(1, seen.size());
        assertSame(t, seen.get(0).getThrowable());
        assertSame(this, seen.get(0).getSource());
        support.removeErrorListener(l);
        support.fireErrorEvent(t);
        assertEquals(1, seen.size());
    }

    @Test
    public void escapePressesTheCancelButtonOfAFrame() {
        JXFrame f = new JXFrame("x");
        final int[] cancelled = new int[1];
        JButton cancel = new JButton(new AbstractAction("Cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelled[0]++;
            }
        });
        JButton ok = new JButton("OK");
        JPanel p = new JPanel();
        p.add(ok);
        p.add(cancel);
        f.add(p);
        f.setCancelButton(cancel);
        f.setDefaultButton(ok);
        assertSame(cancel, f.getCancelButton());
        assertSame(ok, f.getDefaultButton());
        assertSame(ok, f.getRootPane().getDefaultButton());
        show(f);
        type(f, KeyEvent.VK_ESCAPE);
        assertEquals(1, cancelled[0]);
        cancel.setEnabled(false);
        type(f, KeyEvent.VK_ESCAPE);
        assertEquals(1, cancelled[0]);
        f.setCancelButton(null);
        cancel.setEnabled(true);
        type(f, KeyEvent.VK_ESCAPE);
        assertEquals(1, cancelled[0]);
    }

    @Test
    public void aWaitingFrameShowsItsWaitPaneAndCursor() {
        JXFrame f = new JXFrame();
        assertEquals(JXFrame.StartPosition.Manual, f.getStartPosition());
        f.setStartPosition(JXFrame.StartPosition.CenterInScreen);
        assertEquals(JXFrame.StartPosition.CenterInScreen, f.getStartPosition());
        Component glass = f.getGlassPane();
        JPanel wait = new JPanel();
        f.setWaitPane(wait);
        assertSame(wait, f.getWaitPane());
        assertSame(glass, f.getGlassPane());
        f.setSize(200, 100);
        show(f);

        f.setWaiting(true);
        assertTrue(f.isWaiting());
        assertTrue(f.isWaitPaneVisible());
        assertTrue(f.isWaitCursorVisible());
        assertSame(wait, f.getGlassPane());
        assertTrue(wait.isVisible());
        assertEquals(Cursor.WAIT_CURSOR, f.getCursor().getType());
        Cursor hand = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        f.setCursor(hand);
        assertEquals("kept for later", Cursor.WAIT_CURSOR, f.getCursor().getType());

        f.setWaiting(false);
        assertFalse(f.isWaiting());
        assertSame(glass, f.getGlassPane());
        assertFalse(wait.isVisible());
        assertSame(hand, f.getCursor());

        assertFalse(f.isIdle());
        f.setIdle(true);
        assertTrue(f.isIdle());
        f.setIdleThreshold(5000L);
        assertEquals(5000L, f.getIdleThreshold());
    }

    @Test
    public void aDialogTakesItsButtonsFromTheContentsActions() {
        final int[] ran = new int[1];
        JPanel content = new JPanel();
        content.setName("Find");
        content.add(new JLabel("what"));
        content.getActionMap().put(JXDialog.EXECUTE_ACTION_COMMAND, new AbstractAction("Search") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ran[0]++;
            }
        });
        JFrame owner = show(new JFrame("owner"));
        JXDialog d = new JXDialog(owner, content);
        assertEquals("Find", d.getTitle());
        assertSame(owner, d.getOwner());
        assertFalse(d.isModal());
        JButton search = find(d, JButton.class, "Search");
        JButton close = find(d, JButton.class, "Close");
        assertNotNull(search);
        assertNotNull(close);
        assertSame(search, d.getRootPane().getDefaultButton());
        assertTrue("packed", d.getWidth() > 0 && d.getHeight() > 0);
        d.setVisible(true);
        search.doClick();
        assertEquals(1, ran[0]);
        assertTrue(d.isVisible());
        type(d, KeyEvent.VK_ESCAPE);
        assertFalse("escape closes", d.isVisible());

        JXDialog plain = new JXDialog(new JPanel());
        assertNull(find(plain, JButton.class, "Execute"));
        plain.setVisible(true);
        find(plain, JButton.class, "Close").doClick();
        assertFalse(plain.isVisible());
    }

    @Test
    public void theToggleActionFlipsACollapsiblePaneAndSwapsItsIcon() {
        JXCollapsiblePane pane = new JXCollapsiblePane();
        pane.setAnimated(false);
        Action toggle = pane.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION);
        assertNotNull(toggle);
        Icon expand = new Dot();
        Icon collapse = new Dot();
        toggle.putValue(JXCollapsiblePane.EXPAND_ICON, expand);
        assertNull("open, and no collapse icon yet", toggle.getValue(Action.SMALL_ICON));
        toggle.putValue(JXCollapsiblePane.COLLAPSE_ICON, collapse);
        assertSame(collapse, toggle.getValue(Action.SMALL_ICON));

        toggle.actionPerformed(new ActionEvent(pane, ActionEvent.ACTION_PERFORMED, "toggle"));
        assertTrue(pane.isCollapsed());
        assertSame(expand, toggle.getValue(Action.SMALL_ICON));
        JButton b = new JButton(toggle);
        b.doClick();
        assertFalse(pane.isCollapsed());
        assertSame(collapse, toggle.getValue(Action.SMALL_ICON));
        pane.setCollapsed(true);
        assertSame(expand, toggle.getValue(Action.SMALL_ICON));
    }

    @Test
    public void anExtendedActionKeepsItsAccelerator() {
        AbstractActionExt a = new AbstractActionExt("Save") {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };
        assertNull(a.getAccelerator());
        KeyStroke k = KeyStroke.getKeyStroke(KeyEvent.VK_S, 0);
        a.setAccelerator(k);
        assertSame(k, a.getAccelerator());
        assertSame(k, a.getValue(Action.ACCELERATOR_KEY));
    }
}
