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
package com.codename1.ui;

import com.codename1.impl.WindowManager;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.util.EventDispatcher;

import java.util.ArrayList;

/// The desktop a windowed application runs on: the monitors attached to it and the
/// `Window` instances open on them.
///
/// This sits alongside `Display` rather than replacing any of it. `Display` answers
/// "how big is the application's main surface", which is the only question a phone
/// has; `Desktop` answers "what screens exist and what windows are open", which only
/// a windowing system can answer.
///
/// Every method degrades safely on a platform with no windowing system:
/// `#getWindows()` returns an empty array, `#getMonitors()` returns a single monitor
/// describing the main display, and `#getFocusedWindow()` returns null. Only
/// constructing a `Window` throws.
///
/// @author Shai Almog
public final class Desktop {

    private static final Desktop INSTANCE = new Desktop();
    private static final int[] BOUNDS_SCRATCH = new int[4];

    private final ArrayList<Window> windows = new ArrayList<Window>();
    private final EventDispatcher monitorListeners = new EventDispatcher();
    private final EventDispatcher windowListeners = new EventDispatcher();
    private Window focusedWindow;
    private int nextWindowId = 1;

    private Desktop() {
    }

    /// Returns the singleton instance.
    ///
    /// #### Returns
    ///
    /// the desktop instance
    public static Desktop getInstance() {
        return INSTANCE;
    }

    /// Indicates whether this platform has a windowing system, and therefore whether
    /// `Window` can be used at all.
    ///
    /// #### Returns
    ///
    /// true if additional native windows can be opened
    public static boolean isSupported() {
        return Display.impl != null && Display.impl.getWindowManager() != null;
    }

    private static WindowManager manager() {
        if (Display.impl == null) {
            return null;
        }
        return Display.impl.getWindowManager();
    }

    // ---- monitors --------------------------------------------------------------

    /// Returns every monitor attached to the desktop.
    ///
    /// On a platform with no windowing system this reports a single monitor covering
    /// the main display, so layout code that positions against a monitor works
    /// everywhere.
    ///
    /// #### Returns
    ///
    /// the monitors, never empty and never null
    public Monitor[] getMonitors() {
        WindowManager wm = manager();
        if (wm == null) {
            return new Monitor[]{fallbackMonitor()};
        }
        int count = wm.getMonitorCount();
        if (count <= 0) {
            return new Monitor[]{fallbackMonitor()};
        }
        int primary = wm.getPrimaryMonitor();
        Monitor[] out = new Monitor[count];
        for (int iter = 0; iter < count; iter++) {
            out[iter] = readMonitor(wm, iter, primary);
        }
        return out;
    }

    /// Returns the monitor the platform treats as the origin of the desktop.
    ///
    /// #### Returns
    ///
    /// the primary monitor
    public Monitor getPrimaryMonitor() {
        WindowManager wm = manager();
        if (wm == null) {
            return fallbackMonitor();
        }
        return readMonitor(wm, wm.getPrimaryMonitor(), wm.getPrimaryMonitor());
    }

    /// Returns the monitor containing the given desktop coordinate.
    ///
    /// #### Parameters
    ///
    /// - `x`: the x coordinate in desktop space
    ///
    /// - `y`: the y coordinate in desktop space
    ///
    /// #### Returns
    ///
    /// the monitor containing that point, or the primary monitor when none does
    public Monitor getMonitorAt(int x, int y) {
        Monitor[] all = getMonitors();
        for (Monitor m : all) {
            if (m.getBounds().contains(x, y)) {
                return m;
            }
        }
        return getPrimaryMonitor();
    }

    /// Returns the monitor a top level is currently displayed on.
    ///
    /// #### Parameters
    ///
    /// - `topLevel`: the form or window to locate
    ///
    /// #### Returns
    ///
    /// the monitor it sits on, or the primary monitor when that cannot be determined
    public Monitor getMonitorFor(TopLevelContainer topLevel) {
        WindowManager wm = manager();
        if (wm != null && topLevel instanceof Window) {
            Object peer = ((Window) topLevel).getNativePeer();
            if (peer != null) {
                return readMonitor(wm, wm.getMonitorForWindow(peer), wm.getPrimaryMonitor());
            }
        }
        return getPrimaryMonitor();
    }

    /// Returns the union of every monitor's bounds.
    ///
    /// #### Returns
    ///
    /// the whole desktop area
    public Rectangle getDesktopBounds() {
        Monitor[] all = getMonitors();
        Rectangle out = all[0].getBounds();
        for (int iter = 1; iter < all.length; iter++) {
            Rectangle b = all[iter].getBounds();
            int x = Math.min(out.getX(), b.getX());
            int y = Math.min(out.getY(), b.getY());
            int right = Math.max(out.getX() + out.getWidth(), b.getX() + b.getWidth());
            int bottom = Math.max(out.getY() + out.getHeight(), b.getY() + b.getHeight());
            out = new Rectangle(x, y, right - x, bottom - y);
        }
        return out;
    }

    private Monitor readMonitor(WindowManager wm, int index, int primary) {
        int[] b;
        int[] w;
        synchronized (BOUNDS_SCRATCH) {
            b = copyOf(wm.getMonitorBounds(index, BOUNDS_SCRATCH));
            w = copyOf(wm.getMonitorWorkArea(index, BOUNDS_SCRATCH));
        }
        return new Monitor(index,
                new Rectangle(b[0], b[1], b[2], b[3]),
                new Rectangle(w[0], w[1], w[2], w[3]),
                wm.getMonitorDensity(index),
                wm.getMonitorScale(index),
                wm.getMonitorDotsPerInch(index),
                wm.getMonitorName(index),
                index == primary);
    }

    private static int[] copyOf(int[] src) {
        if (src == null) {
            return new int[4];
        }
        return new int[]{src[0], src[1], src[2], src[3]};
    }

    private Monitor fallbackMonitor() {
        int w = Display.impl == null ? 0 : Display.impl.getDisplayWidth();
        int h = Display.impl == null ? 0 : Display.impl.getDisplayHeight();
        Rectangle r = new Rectangle(0, 0, w, h);
        int density = Display.getInstance().getDeviceDensity();
        return new Monitor(0, r, new Rectangle(r), density, 1.0,
                Display.getInstance().convertToPixels(254, true) / 10, "main", true);
    }

    // ---- windows ----------------------------------------------------------------

    /// Returns every window currently open, not counting the application's main form.
    ///
    /// #### Returns
    ///
    /// the open windows, empty when there are none or the platform has no windows
    public Window[] getWindows() {
        synchronized (windows) {
            return windows.toArray(new Window[windows.size()]);
        }
    }

    /// Returns the window that currently holds keyboard focus.
    ///
    /// #### Returns
    ///
    /// the focused window, or null when the main form has focus or none is open
    public Window getFocusedWindow() {
        return focusedWindow;
    }

    /// Adds a listener notified when a monitor is attached, removed or reconfigured.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to add
    public void addMonitorListener(ActionListener l) {
        monitorListeners.addListener(l);
        // Touching the manager is what makes a port start watching for display
        // changes; without it a listener registered before anything else looked at
        // the windowing system would never hear about one.
        Display.impl.getWindowManager();
    }

    /// Removes a previously added monitor listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to remove
    public void removeMonitorListener(ActionListener l) {
        monitorListeners.removeListener(l);
    }

    /// Adds a listener notified when any window is shown, hidden, moved or resized.
    ///
    /// This is the multi-window counterpart of
    /// `Display#addWindowListener(com.codename1.ui.events.ActionListener)`, which
    /// continues to report only the application's main window.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to add
    public void addWindowListener(ActionListener l) {
        windowListeners.addListener(l);
    }

    /// Removes a previously added window listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to remove
    public void removeWindowListener(ActionListener l) {
        windowListeners.removeListener(l);
    }

    // ---- framework internals -------------------------------------------------------

    int nextWindowId() {
        return nextWindowId++;
    }

    void registerWindow(Window w) {
        synchronized (windows) {
            if (!windows.contains(w)) {
                windows.add(w);
            }
        }
    }

    /// Returns the open windows owned by the given top level. Used by
    /// `Window#dispose()`, which cannot outlive its children -- the platform would
    /// leave them open with nothing behind them.
    Window[] windowsOwnedBy(TopLevelContainer owner) {
        ArrayList<Window> owned = new ArrayList<Window>();
        synchronized (windows) {
            int len = windows.size();
            for (int iter = 0; iter < len; iter++) {
                Window w = windows.get(iter);
                if (w.getOwnerWindow() == owner) { //NOPMD CompareObjectsWithEquals
                    owned.add(w);
                }
            }
        }
        return owned.toArray(new Window[owned.size()]);
    }

    void deregisterWindow(Window w) {
        synchronized (windows) {
            windows.remove(w);
        }
        if (focusedWindow == w) { //NOPMD CompareObjectsWithEquals
            focusedWindow = null;
        }
    }

    void setFocusedWindow(Window w) {
        focusedWindow = w;
    }

    /// Returns the window carrying the given framework assigned id, which is how an
    /// event that arrived off the event dispatch thread is routed back to its tree.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id from `Window#getWindowId()`
    ///
    /// #### Returns
    ///
    /// the matching window, or null when none is open with that id
    public Window windowById(int windowId) {
        synchronized (windows) {
            int len = windows.size();
            for (int iter = 0; iter < len; iter++) {
                Window w = windows.get(iter);
                if (w.getWindowId() == windowId) {
                    return w;
                }
            }
        }
        return null;
    }

    boolean hasOpenWindows() {
        synchronized (windows) {
            return !windows.isEmpty();
        }
    }

    boolean hasVisibleWindows() {
        synchronized (windows) {
            int len = windows.size();
            for (int iter = 0; iter < len; iter++) {
                if (windows.get(iter).isWindowShowing()) {
                    return true;
                }
            }
        }
        return false;
    }

    /// Snapshot used by the paint pass. Callers iterate by index because a nested
    /// event loop can dispose a window mid iteration.
    ArrayList<Window> windowList() {
        return windows;
    }

    void fireMonitorChanged() {
        monitorListeners.fireActionEvent(new ActionEvent(this));
    }

    void fireWindowEvent(ActionEvent evt) {
        windowListeners.fireActionEvent(evt);
    }

    /// Disposes every open window. Invoked as the application shuts down so a window
    /// cannot outlive the event dispatch thread that paints it.
    void disposeAll() {
        Window[] all = getWindows();
        for (Window w : all) {
            w.dispose();
        }
    }
}
