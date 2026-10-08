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
import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.desktopcompat.java.awt.event.InputEvent;
import com.codename1.desktopcompat.javax.swing.MenuSelectionManager;
import com.codename1.desktopcompat.rt.EventBridge;
import com.codename1.desktopcompat.rt.InputState;
import com.codename1.desktopcompat.rt.SecondaryWindows;
import com.codename1.desktopcompat.rt.Units;
import com.codename1.desktopcompat.rt.WindowHost;
import com.codename1.desktopcompat.rt.WindowHosts;
import com.codename1.ui.Command;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;

/// What the window, menu and input tests share: a start with no window
/// left over from another test, the input state and the window manager
/// under the test's control, and both put back afterwards.
public abstract class WindowsTestBase extends KernelTestBase {

    /// The modifiers and the button the next events carry.
    protected final FakeInput input = new FakeInput();

    @Before
    public void cleanWindows() {
        disposeAll();
        EventBridge.setInputState(input);
    }

    @After
    public void restoreWindows() {
        MenuSelectionManager.defaultManager().clearSelectedPath();
        disposeAll();
        EventBridge.setInputState(null);
        WindowHosts.setSecondaryWindows(null);
        WindowHosts.setExitHook(null);
        WindowHosts.setIdleHook(null);
    }

    private static void disposeAll() {
        Window[] ws = Window.getWindows();
        for (int i = ws.length - 1; i >= 0; i--) {
            ws[i].dispose();
        }
    }

    /// The display position of a logical point of `c`.
    protected static int[] at(Component c, int x, int y) {
        com.codename1.ui.Component p = c.cn1Peer();
        return new int[]{p.getAbsoluteX() + Units.toDevice(x), p.getAbsoluteY() + Units.toDevice(y)};
    }

    /// Presses and releases the pointer on a point of `c`, through the
    /// form that hosts its window.
    protected static void click(Window w, Component c, int x, int y) {
        int[] p = at(c, x, y);
        Form f = w.cn1HostForm();
        f.pointerPressed(p[0], p[1]);
        f.pointerReleased(p[0], p[1]);
    }

    protected static void pressOn(Window w, Component c, int x, int y) {
        int[] p = at(c, x, y);
        w.cn1HostForm().pointerPressed(p[0], p[1]);
    }

    protected static void releaseOn(Window w, Component c, int x, int y) {
        int[] p = at(c, x, y);
        w.cn1HostForm().pointerReleased(p[0], p[1]);
    }

    /// Sends a key press and its release to the window.
    protected static void type(Window w, int code) {
        Form f = w.cn1HostForm();
        f.keyPressed(code);
        f.keyReleased(code);
    }

    /// Input state a test sets.
    public static final class FakeInput implements InputState {

        public int modifiers;
        public int button = 1;
        public boolean touch;

        @Override
        public int modifiers() {
            return modifiers;
        }

        @Override
        public int button() {
            return button;
        }

        @Override
        public boolean touch() {
            return touch;
        }

        public void control() {
            modifiers = InputEvent.CTRL_DOWN_MASK;
        }
    }

    /// A window manager that records what it was asked, standing for a
    /// desktop port's.
    public static final class FakeDesktop implements SecondaryWindows {

        public final List<FakeHost> hosts = new ArrayList<FakeHost>();

        @Override
        public boolean supported() {
            return true;
        }

        @Override
        public WindowHost open(Window w) {
            FakeHost h = new FakeHost(w);
            hosts.add(h);
            return h;
        }
    }

    /// One window of the fake window manager.
    public static final class FakeHost implements WindowHost {

        public final Window window;
        public final List<String> log = new ArrayList<String>();
        public List<Command> commands = new ArrayList<Command>();
        public String title;
        public boolean open;

        FakeHost(Window window) {
            this.window = window;
        }

        @Override
        public void open() {
            open = true;
            log.add("open");
        }

        @Override
        public void close() {
            open = false;
            log.add("close");
        }

        @Override
        public void release() {
            log.add("release");
        }

        @Override
        public void title(String t) {
            title = t;
        }

        @Override
        public void icon(Image icon) {
        }

        @Override
        public void bounds() {
            log.add("bounds " + window.getWidth() + "x" + window.getHeight());
        }

        @Override
        public void resizable(boolean resizable) {
            log.add("resizable " + resizable);
        }

        @Override
        public void decorated(boolean decorated) {
            log.add("decorated " + decorated);
        }

        @Override
        public void state(int state) {
            log.add("state " + state);
        }

        @Override
        public void commands(List<Command> list) {
            commands = new ArrayList<Command>(list);
        }

        @Override
        public boolean takesCommands() {
            return true;
        }

        @Override
        public Form form() {
            return null;
        }

        @Override
        public boolean fillsDisplay() {
            return false;
        }

        @Override
        public boolean reusable() {
            return true;
        }
    }
}
