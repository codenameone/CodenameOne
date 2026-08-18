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
package com.codename1.impl.windows;

import com.codename1.impl.WindowManager;
import com.codename1.ui.Display;
import com.codename1.ui.Image;

/**
 * The native Windows implementation of the desktop windowing contract.
 *
 * <p>Each Codename One window is a slot in the native table in
 * {@code cn1_windows_desktopwindow.cpp}, with its own HWND, its own
 * {@code ID2D1HwndRenderTarget} and its own {@code CN1Graphics}. The application's
 * main window is deliberately not part of that table -- it stays in {@code cn1Win}
 * exactly as before -- so nothing about the single-window path changes.</p>
 *
 * @author Shai Almog
 */
public class WindowsWindowManager extends WindowManager {

    /** One native window, identified by its slot in the native table. */
    static final class Peer {
        final int slot;
        final int windowId;

        Peer(int slot, int windowId) {
            this.slot = slot;
            this.windowId = windowId;
        }
    }

    private static Peer peer(Object p) {
        return p instanceof Peer ? (Peer) p : null;
    }

    private static int slot(Object p) {
        Peer w = peer(p);
        return w == null ? -1 : w.slot;
    }

    // ---- lifecycle -----------------------------------------------------------

    @Override
    public Object createWindow(int windowId, String title, int x, int y, int width, int height,
            boolean decorated, boolean resizable, Object parentPeer, boolean positionSet,
            boolean ownedByMainWindow) {
        // The owner HWND is what makes an owned window stay above its owner and
        // minimize with it. -1 is another window's slot being absent: -2 asks for the
        // application's main window, and anything else leaves the window unowned, so
        // an unowned window is not silently made a child of the main one.
        int ownerSlot = parentPeer != null ? slot(parentPeer) : (ownedByMainWindow ? -2 : -1);
        int slot = WindowsNative.desktopWindowCreate(windowId, title == null ? "" : title,
                x, y, width, height, decorated, resizable, ownerSlot, positionSet);
        if (slot < 0) {
            return null;
        }
        return new Peer(slot, windowId);
    }

    @Override
    public void show(Object peerObj) {
        int s = slot(peerObj);
        if (s >= 0) {
            WindowsNative.desktopWindowShow(s, true);
        }
    }

    @Override
    public void hide(Object peerObj) {
        int s = slot(peerObj);
        if (s >= 0) {
            WindowsNative.desktopWindowShow(s, false);
        }
    }

    @Override
    public void dispose(Object peerObj) {
        int s = slot(peerObj);
        if (s >= 0) {
            WindowsNative.desktopWindowDestroy(s);
        }
    }

    // ---- attributes ------------------------------------------------------------

    @Override
    public void setTitle(Object peerObj, String title) {
        int s = slot(peerObj);
        if (s >= 0) {
            WindowsNative.desktopWindowSetTitle(s, title == null ? "" : title);
        }
    }

    @Override
    public void setBounds(Object peerObj, int x, int y, int width, int height) {
        int s = slot(peerObj);
        if (s >= 0) {
            WindowsNative.desktopWindowSetBounds(s, x, y, width, height);
        }
    }

    @Override
    public int[] getBounds(Object peerObj, int[] out) {
        int s = slot(peerObj);
        if (s >= 0) {
            WindowsNative.desktopWindowGetBounds(s, out);
        }
        return out;
    }

    @Override
    public int getWidth(Object peerObj) {
        int s = slot(peerObj);
        return s < 0 ? 0 : WindowsNative.desktopWindowGetWidth(s);
    }

    @Override
    public int getHeight(Object peerObj) {
        int s = slot(peerObj);
        return s < 0 ? 0 : WindowsNative.desktopWindowGetHeight(s);
    }

    @Override
    public void setResizable(Object peerObj, boolean resizable) {
        int s = slot(peerObj);
        if (s >= 0) {
            WindowsNative.desktopWindowSetResizable(s, resizable);
        }
    }

    @Override
    public void setAlwaysOnTop(Object peerObj, boolean alwaysOnTop) {
        int s = slot(peerObj);
        if (s >= 0) {
            WindowsNative.desktopWindowSetAlwaysOnTop(s, alwaysOnTop);
        }
    }

    @Override
    public void setMinimumSize(Object peerObj, int width, int height) {
        int s = slot(peerObj);
        if (s >= 0) {
            WindowsNative.desktopWindowSetMinimumSize(s, width, height);
        }
    }

    @Override
    public void setUtilityWindow(Object peerObj, boolean utility) {
        int s = slot(peerObj);
        if (s >= 0) {
            WindowsNative.desktopWindowSetUtility(s, utility);
        }
    }

    @Override
    public void setModal(Object peerObj, boolean modal, boolean applicationWide, Object ownerPeer) {
        // Nothing to do: which windows are blocked is decided by the framework and
        // delivered through setInputEnabled/setMainWindowInputEnabled. Deriving it
        // here from one call cannot express nesting, scope or ownership -- it left
        // the other secondary windows enabled under application modality and
        // re-enabled everything an outer modal was still blocking.
    }

    @Override
    public void setInputEnabled(Object peerObj, boolean enabled) {
        int s = slot(peerObj);
        if (s >= 0) {
            WindowsNative.desktopWindowSetEnabled(s, enabled);
        }
    }

    @Override
    public void setMainWindowInputEnabled(boolean enabled) {
        WindowsNative.mainWindowSetEnabled(enabled);
    }

    @Override
    public void setIcon(Object peerObj, Image icon) {
        // Not supported yet: the port has no HICON conversion for a CN1 image.
    }

    @Override
    public void requestFocus(Object peerObj) {
        int s = slot(peerObj);
        if (s >= 0) {
            WindowsNative.desktopWindowFocus(s);
        }
    }

    @Override
    public void minimize(Object peerObj) {
        int s = slot(peerObj);
        if (s >= 0) {
            WindowsNative.desktopWindowSetState(s, 1);
        }
    }

    @Override
    public void restore(Object peerObj) {
        int s = slot(peerObj);
        if (s >= 0) {
            WindowsNative.desktopWindowSetState(s, 0);
        }
    }

    @Override
    public void toggleMaximize(Object peerObj) {
        int s = slot(peerObj);
        if (s >= 0) {
            WindowsNative.desktopWindowSetState(s, 2);
        }
    }

    // ---- rendering ------------------------------------------------------------------

    @Override
    public Object getNativeGraphics(Object peerObj) {
        int s = slot(peerObj);
        if (s < 0) {
            return null;
        }
        return Long.valueOf(WindowsNative.desktopWindowGraphics(s));
    }

    @Override
    public void flushGraphics(Object peerObj, int x, int y, int width, int height) {
        int s = slot(peerObj);
        if (s < 0) {
            return;
        }
        long g = WindowsNative.desktopWindowGraphics(s);
        if (g != 0) {
            WindowsNative.flushGraphics(g, x, y, width, height);
        }
    }

    @Override
    public void setPaintDirtyRegionClip(Object peerObj, int x, int y, int width, int height) {
        int s = slot(peerObj);
        if (s < 0) {
            return;
        }
        long g = WindowsNative.desktopWindowGraphics(s);
        if (g != 0) {
            // Direct2D retains the surface between presents, so a clip set while a
            // component paints has to be confined to the region about to be flushed
            // or a fill escapes into pixels nothing repainted (issue #5273).
            WindowsNative.setFlushRect(g, x, y, width, height);
        }
    }

    // ---- monitors ----------------------------------------------------------------------

    @Override
    public int getMonitorCount() {
        return Math.max(1, WindowsNative.monitorCount());
    }

    @Override
    public int[] getMonitorBounds(int monitor, int[] out) {
        WindowsNative.monitorBounds(monitor, false, out);
        return out;
    }

    @Override
    public int[] getMonitorWorkArea(int monitor, int[] out) {
        WindowsNative.monitorBounds(monitor, true, out);
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
        // Windows expresses per-monitor scaling as DPI against a 96dpi baseline.
        return getMonitorDotsPerInch(monitor) / 96.0;
    }

    @Override
    public int getMonitorDotsPerInch(int monitor) {
        int dpi = WindowsNative.monitorDpi(monitor);
        return dpi > 0 ? dpi : 96;
    }

    @Override
    public String getMonitorName(int monitor) {
        return "display-" + monitor;
    }

    @Override
    public int getPrimaryMonitor() {
        return Math.max(0, WindowsNative.primaryMonitor());
    }

    @Override
    public int getMonitorForWindow(Object peerObj) {
        int s = slot(peerObj);
        if (s < 0) {
            return getPrimaryMonitor();
        }
        return Math.max(0, WindowsNative.monitorForWindow(s));
    }
}
