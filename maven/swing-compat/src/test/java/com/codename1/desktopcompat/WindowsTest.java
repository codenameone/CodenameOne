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
import com.codename1.desktopcompat.java.awt.Frame;
import com.codename1.desktopcompat.java.awt.GraphicsEnvironment;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.Toolkit;
import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.awt.event.MouseAdapter;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.java.awt.event.WindowEvent;
import com.codename1.desktopcompat.java.awt.event.WindowListener;
import com.codename1.desktopcompat.javax.swing.JButton;
import com.codename1.desktopcompat.javax.swing.JDialog;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.javax.swing.JLayeredPane;
import com.codename1.desktopcompat.javax.swing.JPanel;
import com.codename1.desktopcompat.javax.swing.JWindow;
import com.codename1.desktopcompat.javax.swing.SwingUtilities;
import com.codename1.desktopcompat.javax.swing.WindowConstants;
import com.codename1.desktopcompat.rt.DesktopLifecycle;
import com.codename1.desktopcompat.rt.DialogForm;
import com.codename1.desktopcompat.rt.FrameForm;
import com.codename1.desktopcompat.rt.WindowHosts;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.ui.Display;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/// Frames, dialogs and plain windows on both form factors: the stack of
/// forms, and the window manager behind its seam.
public class WindowsTest extends WindowsTestBase {

    private static final class Log implements WindowListener {
        final List<String> log = new ArrayList<String>();

        @Override
        public void windowOpened(WindowEvent e) {
            log.add("opened");
        }

        @Override
        public void windowClosing(WindowEvent e) {
            log.add("closing");
        }

        @Override
        public void windowClosed(WindowEvent e) {
            log.add("closed");
        }

        @Override
        public void windowIconified(WindowEvent e) {
            log.add("iconified");
        }

        @Override
        public void windowDeiconified(WindowEvent e) {
            log.add("deiconified");
        }

        @Override
        public void windowActivated(WindowEvent e) {
            log.add("activated");
        }

        @Override
        public void windowDeactivated(WindowEvent e) {
            log.add("deactivated");
        }
    }

    @Test
    public void aFrameIsOpenedActivatedAndClosedOnce() {
        JFrame f = new JFrame("One");
        Log log = new Log();
        f.addWindowListener(log);
        f.setVisible(true);
        assertEquals("[opened, activated]", log.log.toString());
        assertTrue(f.isActive());
        assertSame(f.cn1Form(), Display.getInstance().getCurrent());
        assertEquals("One", f.cn1Form().getTitle());
        f.setTitle("Two");
        assertEquals("Two", f.cn1Form().getTitle());
        f.setVisible(false);
        f.setVisible(true);
        assertEquals("[opened, activated, deactivated, activated]", log.log.toString());
        f.dispose();
        assertEquals("closed", log.log.get(log.log.size() - 1));
        assertFalse(f.isDisplayable());
        assertEquals(0, Window.getWindows().length);
    }

    @Test
    public void theFrameFillsTheScreen() {
        JFrame f = new JFrame();
        f.setSize(300, 200);
        show(f);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        assertEquals(screen.width, f.getWidth());
        assertTrue(f.getHeight() > 200);
        Rectangle b = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .getDefaultConfiguration().getBounds();
        assertEquals(screen.width, b.width);
        assertEquals(screen.height, b.height);
        assertFalse(GraphicsEnvironment.isHeadless());
        assertNotNull(f.getGraphicsConfiguration());
        assertEquals(0, Toolkit.getDefaultToolkit().getScreenInsets(f.getGraphicsConfiguration()).top);
    }

    @Test
    public void theCloseOperationsDoWhatTheySay() {
        JFrame hide = show(new JFrame("hide"));
        Log log = new Log();
        hide.addWindowListener(log);
        hide.cn1Closing();
        assertFalse(hide.isVisible());
        assertTrue(hide.isDisplayable());
        assertTrue(log.log.contains("closing"));
        assertFalse(log.log.contains("closed"));

        JFrame dispose = new JFrame("dispose");
        dispose.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        Log disposed = new Log();
        dispose.addWindowListener(disposed);
        show(dispose);
        dispose.cn1Closing();
        assertFalse(dispose.isDisplayable());
        assertTrue(disposed.log.contains("closed"));

        JFrame nothing = new JFrame("nothing");
        nothing.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        show(nothing);
        nothing.cn1Closing();
        assertTrue(nothing.isVisible());

        final int[] exits = new int[1];
        WindowHosts.setExitHook(new Runnable() {
            @Override
            public void run() {
                exits[0]++;
            }
        });
        JFrame exit = new JFrame("exit");
        exit.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        show(exit);
        assertEquals(0, exits[0]);
        exit.cn1Closing();
        assertEquals(1, exits[0]);
    }

    @Test
    public void theBackCommandOfTheFormAsksTheFrameToClose() {
        JFrame f = new JFrame("back");
        f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        show(f);
        FrameForm form = f.cn1Form();
        assertNotNull(form.getBackCommand());
        form.getBackCommand().actionPerformed(new com.codename1.ui.events.ActionEvent(form));
        assertFalse(f.isDisplayable());
    }

    @Test
    public void onAPhoneASecondFrameIsAFormOverTheFirstAndClosingItGoesBack() {
        JFrame first = show(new JFrame("first"));
        JFrame second = new JFrame("second");
        Log firstLog = new Log();
        first.addWindowListener(firstLog);
        show(second);
        assertSame(second.cn1Form(), Display.getInstance().getCurrent());
        assertTrue(second.isActive());
        assertFalse(first.isActive());
        assertEquals("[deactivated]", firstLog.log.toString());
        assertEquals(2, Window.getWindows().length);
        assertEquals(2, Frame.getFrames().length);
        second.dispose();
        MainThreadRule.drain();
        assertSame(first.cn1Form(), Display.getInstance().getCurrent());
        assertTrue(first.isActive());
        first.toFront();
        assertTrue(first.isActive());
    }

    @Test
    public void whereThereIsAWindowManagerLaterFramesAreWindowsOfIt() {
        FakeDesktop desktop = new FakeDesktop();
        WindowHosts.setSecondaryWindows(desktop);
        JFrame first = show(new JFrame("first"));
        assertNotNull("the first frame keeps the application's form", first.cn1Form());
        assertEquals(0, desktop.hosts.size());

        JFrame second = new JFrame("second");
        second.setResizable(false);
        second.setSize(320, 240);
        second.setVisible(true);
        assertEquals(1, desktop.hosts.size());
        FakeHost host = desktop.hosts.get(0);
        assertSame(second, host.window);
        assertSame(host, second.cn1Host());
        assertNull(second.cn1Form());
        assertEquals("second", host.title);
        assertTrue(host.open);
        assertTrue(host.log.contains("resizable false"));
        assertTrue(host.log.contains("decorated true"));
        assertSame("the first frame stays where it is", first.cn1Form(), Display.getInstance().getCurrent());
        assertEquals(320, second.getWidth());
        assertEquals(240, second.getHeight());
        assertTrue(second.isActive());

        second.setSize(400, 300);
        assertTrue(host.log.contains("bounds 400x300"));
        second.setExtendedState(Frame.MAXIMIZED_BOTH);
        assertTrue(host.log.contains("state " + Frame.MAXIMIZED_BOTH));
        second.setTitle("renamed");
        assertEquals("renamed", host.title);

        second.setVisible(false);
        assertFalse(host.open);
        assertTrue(first.isActive());
        second.setVisible(true);
        assertEquals("the window is reused", 1, desktop.hosts.size());
        second.dispose();
        assertTrue(host.log.contains("release"));

        JDialog d = new JDialog(first, "dialog", false);
        d.setSize(100, 100);
        d.setVisible(true);
        assertEquals(2, desktop.hosts.size());
        assertSame(d, desktop.hosts.get(1).window);
        d.dispose();
    }

    @Test
    public void aDialogFloatsOverTheFrameAtItsOwnSizeAndKnowsItsOwner() {
        JFrame f = show(new JFrame("owner"));
        JDialog d = new JDialog(f, "dialog", false);
        d.add(new JLabel("content"), BorderLayout.CENTER);
        d.setSize(200, 120);
        d.setVisible(true);
        assertTrue(d.cn1Host() instanceof DialogForm);
        assertNull(d.cn1Form());
        assertSame(f, d.getOwner());
        assertEquals(1, f.getOwnedWindows().length);
        assertSame(d, f.getOwnedWindows()[0]);
        assertSame(d, WindowHosts.active());
        assertNull(SwingUtilities.getWindowAncestor(d));
        assertTrue("width " + d.getWidth(), d.getWidth() > 150 && d.getWidth() <= 200);
        assertTrue("height " + d.getHeight(), d.getHeight() > 80 && d.getHeight() <= 120);
        assertEquals(1, Window.getOwnerlessWindows().length);
        f.dispose();
        assertFalse("disposing of the owner disposes of the dialog", d.isDisplayable());
    }

    @Test
    public void aModalDialogBlocksItsCallerUntilItIsDisposedOf() {
        JFrame f = show(new JFrame("owner"));
        final JDialog d = new JDialog(f, "modal", true);
        JButton ok = new JButton("OK");
        d.add(ok, BorderLayout.CENTER);
        d.setSize(150, 100);
        final List<String> order = new ArrayList<String>();
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                order.add("clicked");
                d.dispose();
            }
        });
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                order.add(d.isVisible() ? "showing" : "not showing");
                ((JButton) d.getContentPane().getComponent(0)).doClick();
            }
        });
        order.add("before");
        d.setVisible(true);
        order.add("after");
        assertEquals("[before, showing, clicked, after]", order.toString());
        assertFalse(d.isVisible());
        assertSame(f, WindowHosts.active());
    }

    @Test
    public void whileAModalDialogBlocksEventsKeepRunning() {
        JFrame f = show(new JFrame("owner"));
        final JDialog d = new JDialog(f, "modal", true);
        d.setSize(150, 100);
        final List<String> order = new ArrayList<String>();
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
                        order.add(Display.getInstance().getCurrent() == d.cn1HostForm() ? "on screen" : "missing");
                        order.add(d.isActive() ? "active" : "inactive");
                        d.setVisible(false);
                    }
                });
            }
        });
        later.start();
        d.setVisible(true);
        order.add("returned");
        assertEquals("[on screen, active, returned]", order.toString());
        assertTrue("hidden, not disposed of", d.isDisplayable());
        assertTrue(f.isActive());
    }

    @Test
    public void aModelessDialogDoesNotBlock() {
        JFrame f = show(new JFrame("owner"));
        JDialog d = new JDialog(f, "modeless");
        d.setSize(100, 100);
        d.setVisible(true);
        assertTrue(d.isVisible());
        d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        d.cn1Closing();
        assertFalse(d.isDisplayable());
    }

    @Test
    public void aDialogGetsThePointerThroughItsOwnHost() {
        JFrame f = show(new JFrame("owner"));
        JDialog d = new JDialog(f, "dialog");
        JPanel p = new JPanel();
        final int[] clicks = new int[1];
        p.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                clicks[0]++;
            }
        });
        d.add(p, BorderLayout.CENTER);
        d.setSize(200, 200);
        d.setVisible(true);
        click(d, p, 20, 20);
        assertEquals(1, clicks[0]);
    }

    @Test
    public void thePanesOfARootPaneStackInLayers() {
        JFrame f = new JFrame("layers");
        JPanel low = new JPanel();
        JPanel high = new JPanel();
        JPanel top = new JPanel();
        JLayeredPane lp = f.getLayeredPane();
        lp.add(low, JLayeredPane.DEFAULT_LAYER);
        lp.add(top, JLayeredPane.POPUP_LAYER);
        lp.add(high, JLayeredPane.MODAL_LAYER);
        show(f);
        assertSame(lp, f.getRootPane().getLayeredPane());
        assertEquals(0, lp.getIndexOf(top));
        assertEquals(1, lp.getIndexOf(high));
        assertEquals(2, lp.getIndexOf(low));
        assertTrue(lp.getIndexOf(f.getContentPane()) > 2);
        assertEquals(JLayeredPane.POPUP_LAYER.intValue(), lp.highestLayer());
        assertEquals(JLayeredPane.FRAME_CONTENT_LAYER.intValue(), lp.lowestLayer());
        assertEquals(1, lp.getComponentCountInLayer(JLayeredPane.MODAL_LAYER.intValue()));
        JPanel second = new JPanel();
        lp.add(second, JLayeredPane.MODAL_LAYER);
        assertEquals(1, lp.getPosition(second));
        lp.moveToFront(second);
        assertEquals(0, lp.getPosition(second));
        assertEquals(1, lp.getPosition(high));
        lp.setLayer(low, JLayeredPane.DRAG_LAYER.intValue());
        assertEquals(0, lp.getIndexOf(low));
        assertSame(f.getRootPane().getGlassPane(), f.getGlassPane());
        assertFalse(f.getGlassPane().isVisible());
        assertEquals(f.getRootPane().getWidth(), f.getGlassPane().getWidth());
        assertEquals(f.getRootPane().getHeight(), f.getContentPane().getHeight());
    }

    @Test
    public void aVisibleGlassPaneTakesThePointerFromTheContent() {
        JFrame f = new JFrame("glass");
        JPanel content = new JPanel();
        final int[] contentClicks = new int[1];
        final int[] glassClicks = new int[1];
        content.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                contentClicks[0]++;
            }
        });
        f.add(content, BorderLayout.CENTER);
        JPanel glass = new JPanel();
        glass.setOpaque(false);
        glass.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                glassClicks[0]++;
            }
        });
        f.setGlassPane(glass);
        show(f);
        click(f, content, 30, 30);
        assertEquals(1, contentClicks[0]);
        glass.setVisible(true);
        f.validate();
        click(f, content, 30, 30);
        assertEquals(1, contentClicks[0]);
        assertEquals(1, glassClicks[0]);
    }

    @Test
    public void theEnterKeyClicksTheDefaultButton() {
        JFrame f = new JFrame("default");
        JButton ok = new JButton("OK");
        final int[] clicks = new int[1];
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clicks[0]++;
            }
        });
        f.add(ok, BorderLayout.SOUTH);
        f.getRootPane().setDefaultButton(ok);
        show(f);
        type(f, '\n');
        assertEquals(1, clicks[0]);
        ok.setEnabled(false);
        type(f, '\n');
        assertEquals(1, clicks[0]);
    }

    @Test
    public void aPlainWindowShownFirstFillsAFormAndOverAFrameFloats() {
        JWindow splash = new JWindow();
        splash.add(new JLabel("splash"), BorderLayout.CENTER);
        splash.setVisible(true);
        assertNotNull(splash.cn1Form());
        splash.dispose();
        JFrame f = show(new JFrame("main"));
        JWindow tip = new JWindow(f);
        tip.setSize(80, 40);
        tip.setVisible(true);
        assertTrue(tip.cn1Host() instanceof DialogForm);
        tip.dispose();
    }

    @Test
    public void iconifyingAndStateChangesAreReported() {
        JFrame f = show(new JFrame("state"));
        Log log = new Log();
        f.addWindowListener(log);
        f.cn1Iconified(true);
        f.cn1Iconified(false);
        assertEquals("[iconified, deiconified]", log.log.toString());
        f.setExtendedState(Frame.MAXIMIZED_BOTH);
        assertEquals(Frame.MAXIMIZED_BOTH, f.getExtendedState());
    }

    @Test
    public void theLifecycleRunsMainKeepsRunningAndMapsThePlatformCalls() {
        final JFrame[] made = new JFrame[1];
        final Log log = new Log();
        final int[] exits = new int[1];
        DesktopLifecycle app = new DesktopLifecycle() {
            @Override
            protected void runMain() {
                made[0] = new JFrame("app");
                made[0].setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                made[0].addWindowListener(log);
                made[0].setVisible(true);
            }
        };
        app.runApp();
        WindowHosts.setExitHook(new Runnable() {
            @Override
            public void run() {
                exits[0]++;
            }
        });
        MainThreadRule.drain();
        assertNotNull(made[0]);
        assertSame(made[0].cn1Form(), Display.getInstance().getCurrent());
        log.log.clear();
        app.stop();
        assertEquals("[deactivated, iconified]", log.log.toString());
        log.log.clear();
        app.start();
        assertEquals("[deiconified, activated]", log.log.toString());
        log.log.clear();
        app.destroy();
        assertEquals("[closing]", log.log.toString());
        assertEquals(0, exits[0]);
    }

    @Test
    public void aMainThatShowsNothingLeavesABlankFormOnTheScreen() {
        new com.codename1.ui.Form("stale").show();
        DesktopLifecycle app = new DesktopLifecycle() {
            @Override
            protected void runMain() {
            }
        };
        app.runApp();
        MainThreadRule.drain();
        assertNotNull(Display.getInstance().getCurrent());
        assertFalse(Display.getInstance().getCurrent() instanceof FrameForm);
    }
}
