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
package com.codename1.impl.javase;

import com.codename1.testing.junit.CodenameOneTest;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Toolbar;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.plaf.UIManager;

import org.junit.jupiter.api.Test;

import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Drives the <em>real</em> JavaSE desktop port (booted by {@link CodenameOneTest}) and asserts
 * the desktop window chrome end to end against the actual Swing {@link JFrame}:
 * <ul>
 *   <li><b>native</b> mode: the form title reaches the OS window title bar, the toolbar commands
 *       are bridged to a real {@link JMenuBar}, and triggering the menu item runs the Codename One
 *       command through the AWT&rarr;CN1 thread hop.</li>
 *   <li><b>custom</b> mode: Codename One installs its own title-bar chrome (close caption button).</li>
 * </ul>
 * Screenshot tests cannot see native window chrome, so these assertions read the JFrame directly.
 * Requires a graphical display; skipped on a headless JVM (the extension also aborts there).
 */
@CodenameOneTest
public class DesktopChromeUITest {

    @Test
    public void nativeModeShowsOsTitleBarAndMenuBar() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        assumeTrue(Display.getInstance().isDesktop(), "needs the desktop port (no skin)");

        final AtomicBoolean commandFired = new AtomicBoolean(false);
        runOnCn1AndWait(new Runnable() {
            @Override
            public void run() {
                JavaSEPort.setDesktopTitleBarMode("native");
                JavaSEPort.setDesktopInteractiveScrollbars(true);
                Hashtable props = new Hashtable();
                props.put("@desktopTitleBarMode", "native");
                props.put("@interactiveScrollBool", "true");
                UIManager.getInstance().addThemeProps(props);
                Toolbar.setGlobalToolbar(true);

                Form f = new Form("RobotProbe");
                Command ping = new Command("Ping") {
                    @Override
                    public void actionPerformed(ActionEvent ev) {
                        commandFired.set(true);
                    }
                };
                f.addCommand(ping);
                f.show();
            }
        });
        flushAwt();

        // interactive desktop scrollbars must be active in this mode
        assertTrue(UIManager.getInstance().getLookAndFeel().isInteractiveScroll(),
                "interactiveScrollBool must enable interactive scrollbars on the desktop");

        final JFrame frame = cn1Frame();
        assertNotNull(frame, "the desktop port must own a JFrame");

        final AtomicReference<String> title = new AtomicReference<String>();
        final AtomicReference<JMenuItem> pingItem = new AtomicReference<JMenuItem>();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                title.set(frame.getTitle());
                JMenuBar mb = frame.getJMenuBar();
                if (mb != null) {
                    pingItem.set(findMenuItem(mb, "Ping"));
                }
            }
        });

        assertEquals("RobotProbe", title.get(), "the form title must reach the native OS title bar");
        assertNotNull(pingItem.get(), "the toolbar command must appear in the native menu bar");

        // drive the real Swing menu item; its listener hops back onto the CN1 EDT to run the command
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                pingItem.get().doClick();
            }
        });
        long deadline = System.currentTimeMillis() + 5000;
        while (!commandFired.get() && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertTrue(commandFired.get(),
                "activating the native menu item must invoke the Codename One command");
    }

    @Test
    public void nativeMenuPlacesCommandsByDesktopHint() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        assumeTrue(Display.getInstance().isDesktop(), "needs the desktop port (no skin)");

        runOnCn1AndWait(new Runnable() {
            @Override
            public void run() {
                JavaSEPort.setDesktopTitleBarMode("native");
                Toolbar.setGlobalToolbar(true);
                Form f = new Form("HintProbe");
                Command open = new Command("Open Thing");
                open.setDesktopMenu(Command.DESKTOP_MENU_FILE);
                Command plain = new Command("Plain Thing");
                f.addCommand(plain);
                f.addCommand(open);
                f.show();
            }
        });
        flushAwt();

        final JFrame frame = cn1Frame();
        assertNotNull(frame);
        final AtomicReference<JMenuItem> inFile = new AtomicReference<JMenuItem>();
        final AtomicReference<JMenuItem> inCommands = new AtomicReference<JMenuItem>();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                JMenuBar mb = frame.getJMenuBar();
                if (mb != null) {
                    inFile.set(itemInMenuTitled(mb, "File", "Open Thing"));
                    inCommands.set(itemInMenuTitled(mb, "Commands", "Plain Thing"));
                }
            }
        });
        assertNotNull(inFile.get(), "a command hinted DESKTOP_MENU_FILE must appear under a File menu");
        assertNotNull(inCommands.get(), "a command with no hint must appear under the default Commands menu");
    }

    @Test
    public void customModeInstallsCn1TitleBarChrome() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        assumeTrue(Display.getInstance().isDesktop(), "needs the desktop port (no skin)");

        final AtomicReference<Form> shown = new AtomicReference<Form>();
        runOnCn1AndWait(new Runnable() {
            @Override
            public void run() {
                JavaSEPort.setDesktopTitleBarMode("custom");
                Hashtable props = new Hashtable();
                props.put("@desktopTitleBarMode", "custom");
                UIManager.getInstance().addThemeProps(props);
                Toolbar.setGlobalToolbar(true);

                Form f = new Form("CustomChrome");
                f.show();
                shown.set(f);
            }
        });
        flushAwt();

        final AtomicBoolean hasTitleBar = new AtomicBoolean(false);
        final AtomicBoolean hasCloseButton = new AtomicBoolean(false);
        runOnCn1AndWait(new Runnable() {
            @Override
            public void run() {
                hasTitleBar.set(containsUiid(shown.get(), "WindowTitleBar"));
                hasCloseButton.set(containsUiid(shown.get(), "WindowCloseButton"));
            }
        });
        assertTrue(hasTitleBar.get(), "custom mode must install a CN1 WindowTitleBar");
        assertTrue(hasCloseButton.get(), "custom mode must add a close caption button");
    }

    // ---- helpers ----

    private JFrame cn1Frame() {
        for (Frame f : Frame.getFrames()) {
            if (f instanceof JFrame && f.isVisible()) {
                return (JFrame) f;
            }
        }
        return null;
    }

    private void flushAwt() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    private void runOnCn1AndWait(final Runnable r) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                try {
                    r.run();
                } catch (Throwable t) {
                    err.set(t);
                } finally {
                    latch.countDown();
                }
            }
        });
        assertTrue(latch.await(15, TimeUnit.SECONDS), "Codename One EDT work timed out");
        if (err.get() != null) {
            throw new RuntimeException(err.get());
        }
    }

    private JMenuItem findMenuItem(JMenuBar bar, String text) {
        for (int i = 0; i < bar.getMenuCount(); i++) {
            JMenu menu = bar.getMenu(i);
            if (menu == null) {
                continue;
            }
            for (int j = 0; j < menu.getItemCount(); j++) {
                JMenuItem item = menu.getItem(j);
                if (item != null && text.equals(item.getText())) {
                    return item;
                }
            }
        }
        return null;
    }

    private JMenuItem itemInMenuTitled(JMenuBar bar, String menuTitle, String itemText) {
        for (int i = 0; i < bar.getMenuCount(); i++) {
            JMenu menu = bar.getMenu(i);
            if (menu == null || !menuTitle.equals(menu.getText())) {
                continue;
            }
            for (int j = 0; j < menu.getItemCount(); j++) {
                JMenuItem item = menu.getItem(j);
                if (item != null && itemText.equals(item.getText())) {
                    return item;
                }
            }
        }
        return null;
    }

    private boolean containsUiid(Container root, String uiid) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component c = root.getComponentAt(i);
            if (uiid.equals(c.getUIID())) {
                return true;
            }
            if (c instanceof Container && containsUiid((Container) c, uiid)) {
                return true;
            }
        }
        return false;
    }
}
