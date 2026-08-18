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
package com.codename1.testing;

import com.codename1.impl.WindowManager;
import com.codename1.ui.Display;
import com.codename1.ui.Image;

import java.util.ArrayList;
import java.util.List;

/**
 * A window manager with no operating system behind it, so the desktop windowing
 * API can be driven from a headless unit test.
 *
 * <p>The monitor table is scriptable, which is the point: it lets a test describe a
 * three monitor desktop at mixed scale factors and assert that a window picks up the
 * characteristics of the one it sits on, without needing a second physical
 * display.</p>
 *
 * @author Shai Almog
 */
public class TestWindowManager extends WindowManager {

    /** One fake native window. */
    public static final class FakeWindow {
        private int windowId;
        private String title;
        private int x;
        private int y;
        private int width;
        private int height;
        private boolean decorated;
        private boolean resizable;
        private boolean visible;
        private boolean disposed;
        private boolean modal;
        private boolean alwaysOnTop;
        private boolean focusRequested;
        private int monitor;
        private int paintCount;
        private int modalCalls;
        private boolean modalApplicationWide;
        private FakeWindow modalOwner;
        private boolean utility;
        private boolean inputEnabled = true;
        private FakeWindow owner;
        private boolean positionSet;
        private boolean ownedByMainWindow;
        private int minimumWidth;
        private int minimumHeight;

        public int getWindowId() {
            return windowId;
        }

        public String getTitle() {
            return title;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public boolean isVisible() {
            return visible;
        }

        public boolean isDisposed() {
            return disposed;
        }

        /**
         * How many times setModal() was called on this window. An unbalanced count is
         * exactly the bug that left a native modal blocking after its window closed,
         * so the tests assert on the number of calls rather than only the final state.
         */
        public int getModalCalls() {
            return modalCalls;
        }

        /** True when the last modal call declared application wide scope. */
        public boolean isModalApplicationWide() {
            return modalApplicationWide;
        }

        /** The window the last modal call named as blocked, or null for the main one. */
        public FakeWindow getModalOwner() {
            return modalOwner;
        }

        /** True when the framework asked for a tool/palette window. */
        public boolean isUtility() {
            return utility;
        }

        /** Whether the framework currently allows native input to this window. */
        public boolean isInputEnabled() {
            return inputEnabled;
        }

        /** The owning window handed to createWindow(), or null when there was none. */
        public FakeWindow getOwner() {
            return owner;
        }

        /** True when the application chose the window's position. */
        public boolean isPositionSet() {
            return positionSet;
        }

        /** True when the owner is the application's main window, which has no peer. */
        public boolean isOwnedByMainWindow() {
            return ownedByMainWindow;
        }

        /** The minimum size the framework forwarded, or zero when none was set. */
        public int getMinimumWidth() {
            return minimumWidth;
        }

        public int getMinimumHeight() {
            return minimumHeight;
        }

        public boolean isModal() {
            return modal;
        }

        public boolean isAlwaysOnTop() {
            return alwaysOnTop;
        }

        public boolean isFocusRequested() {
            return focusRequested;
        }

        public boolean isDecorated() {
            return decorated;
        }

        public boolean isResizable() {
            return resizable;
        }

        /** Number of times this window's surface has been flushed. */
        public int getPaintCount() {
            return paintCount;
        }

        /** Which monitor this window currently sits on. */
        public void setMonitor(int monitor) {
            this.monitor = monitor;
        }

        public int getMonitor() {
            return monitor;
        }
    }

    /** One fake monitor. */
    public static final class FakeMonitor {
        private final int[] bounds;
        private final int[] workArea;
        private final double scale;
        private final int dpi;
        private final String name;

        public FakeMonitor(int x, int y, int w, int h, double scale, int dpi, String name) {
            this.bounds = new int[]{x, y, w, h};
            this.workArea = new int[]{x, y, w, h};
            this.scale = scale;
            this.dpi = dpi;
            this.name = name;
        }

        /** Reserves space at the bottom, standing in for a task bar or dock. */
        public FakeMonitor withReservedBottom(int px) {
            workArea[3] = bounds[3] - px;
            return this;
        }

        public double getScale() {
            return scale;
        }
    }

    private final List<FakeWindow> windows = new ArrayList<FakeWindow>();
    private final List<FakeMonitor> monitors = new ArrayList<FakeMonitor>();
    private int primaryMonitor;

    public TestWindowManager() {
        monitors.add(new FakeMonitor(0, 0, 1440, 900, 1.0, 96, "primary"));
    }

    /** Replaces the monitor table. */
    public void setMonitors(List<FakeMonitor> replacement) {
        monitors.clear();
        monitors.addAll(replacement);
    }

    /// Which monitor the application's main window sits on. Scriptable because a
    /// `Form` has no window peer, so this is the only way to describe a main window
    /// that has been dragged to a second display.
    private int mainWindowMonitor;

    public void setMainWindowMonitor(int index) {
        mainWindowMonitor = index;
    }

    @Override
    public int getMonitorForMainWindow() {
        return mainWindowMonitor;
    }

    public void setPrimaryMonitor(int index) {
        primaryMonitor = index;
    }

    /** Every window created through this manager, including disposed ones. */
    public List<FakeWindow> getWindows() {
        return new ArrayList<FakeWindow>(windows);
    }

    /** The most recently created window, which is what most tests assert against. */
    public FakeWindow getLastWindow() {
        if (windows.isEmpty()) {
            return null;
        }
        return windows.get(windows.size() - 1);
    }

    public FakeWindow findWindow(int windowId) {
        for (FakeWindow w : windows) {
            if (w.windowId == windowId) {
                return w;
            }
        }
        return null;
    }

    /**
     * Makes createWindow() answer null, which is what every port does once its fixed
     * native window table is exhausted or the platform refuses.
     */
    public void setCreateFails(boolean createFails) {
        this.createFails = createFails;
    }

    private boolean createFails;

    /// The native image the next capture() should hand back, or null to model a port
    /// that cannot read its own window back. Ports differ here -- JavaSE, Catalyst and
    /// Linux read the window's real raster, while a port with no readback leaves
    /// Window.capture() to re-render -- and both paths need covering.
    private Object captureResult;

    /// Counts capture() calls, so a test can tell "the port was asked and declined"
    /// from "the port was never asked at all".
    private int captureCalls;

    public void setCaptureResult(Object nativeImage) {
        captureResult = nativeImage;
    }

    public int getCaptureCalls() {
        return captureCalls;
    }

    @Override
    public Object capture(Object peer) {
        captureCalls++;
        return win(peer) == null ? null : captureResult;
    }

    public void reset() {
        captureResult = null;
        captureCalls = 0;
        createFails = false;
        mainWindowInputEnabled = true;
        windows.clear();
        monitors.clear();
        monitors.add(new FakeMonitor(0, 0, 1440, 900, 1.0, 96, "primary"));
        primaryMonitor = 0;
        mainWindowMonitor = 0;
    }

    private static FakeWindow win(Object peer) {
        return peer instanceof FakeWindow ? (FakeWindow) peer : null;
    }

    // ---- lifecycle -----------------------------------------------------------

    @Override
    public Object createWindow(int windowId, String title, int x, int y, int width, int height,
            boolean decorated, boolean resizable, Object parentPeer, boolean positionSet,
            boolean ownedByMainWindow) {
        if (createFails) {
            return null;
        }
        FakeWindow w = new FakeWindow();
        w.owner = win(parentPeer);
        w.positionSet = positionSet;
        w.ownedByMainWindow = ownedByMainWindow;
        w.windowId = windowId;
        w.title = title;
        w.x = x;
        w.y = y;
        w.width = width;
        w.height = height;
        w.decorated = decorated;
        w.resizable = resizable;
        windows.add(w);
        return w;
    }

    @Override
    public void show(Object peer) {
        FakeWindow w = win(peer);
        if (w != null) {
            w.visible = true;
        }
    }

    @Override
    public void hide(Object peer) {
        FakeWindow w = win(peer);
        if (w != null) {
            w.visible = false;
        }
    }

    @Override
    public void dispose(Object peer) {
        FakeWindow w = win(peer);
        if (w != null) {
            w.visible = false;
            w.disposed = true;
        }
    }

    // ---- attributes -----------------------------------------------------------

    @Override
    public void setTitle(Object peer, String title) {
        FakeWindow w = win(peer);
        if (w != null) {
            w.title = title;
        }
    }

    @Override
    public void setBounds(Object peer, int x, int y, int width, int height) {
        FakeWindow w = win(peer);
        if (w != null) {
            w.x = x;
            w.y = y;
            w.width = width;
            w.height = height;
        }
    }

    @Override
    public int[] getBounds(Object peer, int[] out) {
        FakeWindow w = win(peer);
        if (w != null) {
            out[0] = w.x;
            out[1] = w.y;
            out[2] = w.width;
            out[3] = w.height;
        }
        return out;
    }

    @Override
    public int getWidth(Object peer) {
        FakeWindow w = win(peer);
        return w == null ? 0 : w.width;
    }

    @Override
    public int getHeight(Object peer) {
        FakeWindow w = win(peer);
        return w == null ? 0 : w.height;
    }

    @Override
    public void setResizable(Object peer, boolean resizable) {
        FakeWindow w = win(peer);
        if (w != null) {
            w.resizable = resizable;
        }
    }

    @Override
    public void setDecorated(Object peer, boolean decorated) {
        FakeWindow w = win(peer);
        if (w != null) {
            w.decorated = decorated;
        }
    }

    @Override
    public void setAlwaysOnTop(Object peer, boolean alwaysOnTop) {
        FakeWindow w = win(peer);
        if (w != null) {
            w.alwaysOnTop = alwaysOnTop;
        }
    }

    @Override
    public void setModal(Object peer, boolean modal, boolean applicationWide, Object ownerPeer) {
        FakeWindow w = win(peer);
        if (w != null) {
            w.modal = modal;
            w.modalCalls++;
            w.modalApplicationWide = applicationWide;
            w.modalOwner = win(ownerPeer);
        }
    }

    @Override
    public void setInputEnabled(Object peer, boolean enabled) {
        FakeWindow w = win(peer);
        if (w != null) {
            w.inputEnabled = enabled;
        }
    }

    @Override
    public void setMainWindowInputEnabled(boolean enabled) {
        mainWindowInputEnabled = enabled;
    }

    /** Whether the framework currently allows native input to the main window. */
    public boolean isMainWindowInputEnabled() {
        return mainWindowInputEnabled;
    }

    private boolean mainWindowInputEnabled = true;

    @Override
    public void setUtilityWindow(Object peer, boolean utility) {
        FakeWindow w = win(peer);
        if (w != null) {
            w.utility = utility;
        }
    }

    @Override
    public void setMinimumSize(Object peer, int width, int height) {
        FakeWindow w = win(peer);
        if (w != null) {
            w.minimumWidth = width;
            w.minimumHeight = height;
        }
    }

    @Override
    public void setIcon(Object peer, Image icon) {
    }

    @Override
    public void requestFocus(Object peer) {
        FakeWindow w = win(peer);
        if (w != null) {
            w.focusRequested = true;
        }
    }

    // ---- rendering ---------------------------------------------------------------

    @Override
    public Object getNativeGraphics(Object peer) {
        // A real TestGraphics rather than a marker object, sized to the window: the
        // paint pass sets a clip on it, so anything else makes flushing the event
        // dispatch thread with a window open blow up.
        FakeWindow w = win(peer);
        int width = w == null ? 1 : Math.max(1, w.width);
        int height = w == null ? 1 : Math.max(1, w.height);
        return new TestCodenameOneImplementation.TestGraphics(width, height);
    }

    @Override
    public void flushGraphics(Object peer, int x, int y, int width, int height) {
        FakeWindow w = win(peer);
        if (w != null) {
            w.paintCount++;
        }
    }

    // ---- monitors ------------------------------------------------------------------

    @Override
    public int getMonitorCount() {
        return monitors.size();
    }

    @Override
    public int[] getMonitorBounds(int monitor, int[] out) {
        int[] b = monitors.get(clamp(monitor)).bounds;
        System.arraycopy(b, 0, out, 0, 4);
        return out;
    }

    @Override
    public int[] getMonitorWorkArea(int monitor, int[] out) {
        int[] b = monitors.get(clamp(monitor)).workArea;
        System.arraycopy(b, 0, out, 0, 4);
        return out;
    }

    @Override
    public int getMonitorDensity(int monitor) {
        int dpi = getMonitorDotsPerInch(monitor);
        if (dpi >= 280) {
            return Display.DENSITY_VERY_HIGH;
        }
        if (dpi >= 200) {
            return Display.DENSITY_HIGH;
        }
        if (dpi >= 140) {
            return Display.DENSITY_MEDIUM;
        }
        return Display.DENSITY_LOW;
    }

    @Override
    public double getMonitorScale(int monitor) {
        return monitors.get(clamp(monitor)).scale;
    }

    @Override
    public int getMonitorDotsPerInch(int monitor) {
        return monitors.get(clamp(monitor)).dpi;
    }

    @Override
    public String getMonitorName(int monitor) {
        return monitors.get(clamp(monitor)).name;
    }

    @Override
    public int getPrimaryMonitor() {
        return primaryMonitor;
    }

    @Override
    public int getMonitorForWindow(Object peer) {
        FakeWindow w = win(peer);
        return w == null ? primaryMonitor : w.monitor;
    }

    private int clamp(int monitor) {
        if (monitor < 0 || monitor >= monitors.size()) {
            return 0;
        }
        return monitor;
    }
}
