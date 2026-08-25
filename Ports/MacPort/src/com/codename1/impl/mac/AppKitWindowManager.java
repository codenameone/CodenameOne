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
package com.codename1.impl.mac;

import com.codename1.impl.WindowManager;
import com.codename1.ui.Display;
import com.codename1.ui.Image;

/// The native macOS implementation of the desktop windowing contract.
///
/// Every Codename One window is a real `NSWindow` owning a layer-hosted view
/// backed by its own `CAMetalLayer`. That is the whole point of the port: the
/// Mac Catalyst manager renders a window's content into a mutable image and
/// hands the finished raster to the scene's view, which costs a full readback
/// and re-upload per window per frame. Here the window's content is drawn
/// straight into its own drawable and presented, so nothing is copied.
///
/// The operations Catalyst leaves on the SPI's no-op defaults --
/// `setAlwaysOnTop`, `setUtilityWindow`, `minimize`, `restore`,
/// `toggleMaximize`, `setModal` and a real undecorated window -- are all
/// ordinary AppKit and are implemented here.
///
/// @author Shai Almog
class AppKitWindowManager extends WindowManager {

    /// The owner recorded for a window owned by the application's main window,
    /// which has no peer of its own to point at.
    private static final Object MAIN_WINDOW = new Object();

    private final MacImplementation impl;
    private final MacNative nativeInstance = new MacNative();
    private final int[] boundsScratch = new int[4];

    AppKitWindowManager(MacImplementation impl) {
        this.impl = impl;
        nativeInstance.macWindowWatchScreens();
    }

    /// A window's identity on this side of the bridge. The slot addresses the
    /// native window and the id addresses the framework's, and the two are kept
    /// together so neither has to be looked up from the other.
    private static final class Peer {
        final int slot;
        final int windowId;
        Object owner;
        int[] captureBuffer;

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

    @Override
    public Object createWindow(int windowId, String title, int x, int y, int width, int height,
            boolean decorated, boolean resizable, Object parentPeer, boolean positionSet,
            boolean ownedByMainWindow) {
        int s = nativeInstance.macWindowCreate(windowId, title == null ? "" : title,
                x, y, width, height, decorated, resizable, positionSet);
        if (s < 0) {
            return null;
        }
        Peer created = new Peer(s, windowId);
        created.owner = ownedByMainWindow ? MAIN_WINDOW : parentPeer;
        return created;
    }

    @Override
    public void show(Object p) {
        int s = slot(p);
        if (s >= 0) {
            nativeInstance.macWindowShow(s, true);
        }
    }

    @Override
    public void hide(Object p) {
        int s = slot(p);
        if (s >= 0) {
            nativeInstance.macWindowShow(s, false);
        }
    }

    @Override
    public void dispose(Object p) {
        int s = slot(p);
        if (s >= 0) {
            nativeInstance.macWindowDestroy(s);
        }
    }

    @Override
    public boolean reopen(Object p) {
        int s = slot(p);
        return s >= 0 && nativeInstance.macWindowReopen(s);
    }

    @Override
    public void setTitle(Object p, String title) {
        int s = slot(p);
        if (s >= 0) {
            nativeInstance.macWindowSetTitle(s, title == null ? "" : title);
        }
    }

    @Override
    public void setBounds(Object p, int x, int y, int width, int height) {
        int s = slot(p);
        if (s >= 0) {
            nativeInstance.macWindowSetBounds(s, x, y, width, height);
        }
    }

    @Override
    public int[] getBounds(Object p, int[] out) {
        int s = slot(p);
        if (s >= 0) {
            nativeInstance.macWindowGetBounds(s, out);
        }
        return out;
    }

    @Override
    public int getWidth(Object p) {
        int s = slot(p);
        return s < 0 ? 0 : nativeInstance.macWindowGetWidth(s);
    }

    @Override
    public int getHeight(Object p) {
        int s = slot(p);
        return s < 0 ? 0 : nativeInstance.macWindowGetHeight(s);
    }

    @Override
    public void setResizable(Object p, boolean resizable) {
        int s = slot(p);
        if (s >= 0) {
            nativeInstance.macWindowSetResizable(s, resizable);
        }
    }

    @Override
    public void setDecorated(Object p, boolean decorated) {
        int s = slot(p);
        if (s >= 0) {
            nativeInstance.macWindowSetDecorated(s, decorated);
        }
    }

    @Override
    public void setMinimumSize(Object p, int width, int height) {
        int s = slot(p);
        if (s >= 0) {
            nativeInstance.macWindowSetMinimumSize(s, width, height);
        }
    }

    @Override
    public void setAlwaysOnTop(Object p, boolean alwaysOnTop) {
        int s = slot(p);
        if (s >= 0) {
            nativeInstance.macWindowSetAlwaysOnTop(s, alwaysOnTop);
        }
    }

    @Override
    public void setUtilityWindow(Object p, boolean utility) {
        int s = slot(p);
        if (s >= 0) {
            nativeInstance.macWindowSetUtility(s, utility);
        }
    }

    @Override
    public void minimize(Object p) {
        int s = slot(p);
        if (s >= 0) {
            nativeInstance.macWindowMinimize(s);
        }
    }

    @Override
    public void restore(Object p) {
        int s = slot(p);
        if (s >= 0) {
            nativeInstance.macWindowRestore(s);
        }
    }

    @Override
    public void toggleMaximize(Object p) {
        int s = slot(p);
        if (s >= 0) {
            nativeInstance.macWindowToggleMaximize(s);
        }
    }

    @Override
    public void requestFocus(Object p) {
        int s = slot(p);
        if (s >= 0) {
            nativeInstance.macWindowRequestFocus(s);
        }
    }

    @Override
    public void setInputEnabled(Object p, boolean enabled) {
        int s = slot(p);
        if (s >= 0) {
            nativeInstance.macWindowSetInputEnabled(s, enabled);
        }
    }

    @Override
    public void setMainWindowInputEnabled(boolean enabled) {
        nativeInstance.macMainWindowSetInputEnabled(enabled);
    }

    @Override
    public void setModal(Object p, boolean modal, boolean applicationWide, Object ownerPeer) {
        int s = slot(p);
        if (s < 0) {
            return;
        }
        // The framework enforces modality itself through setInputEnabled, so this
        // is not what makes the window modal -- it is what makes it behave like a
        // Mac modal window, which is a different thing the user can see: the
        // window keeps key focus and the application's other windows do not take
        // it back by being clicked.
        nativeInstance.macWindowSetModal(s, modal);
    }

    @Override
    public void setIcon(Object p, Image icon) {
        int s = slot(p);
        if (s < 0 || icon == null) {
            return;
        }
        int width = icon.getWidth();
        int height = icon.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        nativeInstance.macWindowSetIcon(s, icon.getRGB(), width, height);
    }

    // ---- rendering ---------------------------------------------------------------------

    @Override
    public Object getNativeGraphics(Object p) {
        int s = slot(p);
        if (s < 0) {
            return null;
        }
        // Points the shared drawing pipeline at this window's own Metal layer and
        // hands back the same global Graphics the main window uses. There is one
        // encoder because the event dispatch thread paints one window at a time,
        // so "the surface currently being drawn into" is genuinely a single value
        // -- which is why no offscreen raster is needed here at all.
        nativeInstance.macWindowBeginPaint(s);
        return impl.getNativeGraphics();
    }

    @Override
    public void flushGraphics(Object p, int x, int y, int width, int height) {
        int s = slot(p);
        if (s >= 0) {
            nativeInstance.macWindowFlush(s, x, y, width, height);
        }
    }

    @Override
    public Object capture(Object p) {
        Peer w = peer(p);
        if (w == null) {
            return null;
        }
        int width = getWidth(p);
        int height = getHeight(p);
        if (width <= 0 || height <= 0) {
            return null;
        }
        int needed = width * height;
        if (w.captureBuffer == null || w.captureBuffer.length < needed) {
            w.captureBuffer = new int[needed];
        }
        if (!nativeInstance.macWindowCapture(w.slot, w.captureBuffer, width, height)) {
            return null;
        }
        // Immutable and copied out on purpose: Window.capture() is documented as
        // the contents at the moment it is called, so it must not change under
        // the caller as the window repaints.
        int[] argb = new int[needed];
        System.arraycopy(w.captureBuffer, 0, argb, 0, needed);
        return Image.createImage(argb, width, height).getImage();
    }

    // ---- monitors ----------------------------------------------------------------------

    @Override
    public int getMonitorCount() {
        return Math.max(1, nativeInstance.macMonitorCount());
    }

    @Override
    public int[] getMonitorBounds(int monitor, int[] out) {
        nativeInstance.macMonitorBounds(monitor, false, out);
        return out;
    }

    @Override
    public int[] getMonitorWorkArea(int monitor, int[] out) {
        nativeInstance.macMonitorBounds(monitor, true, out);
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
        // Carried across the bridge as an int, so scaled by a hundred.
        return nativeInstance.macMonitorScaleTimes100(monitor) / 100.0;
    }

    @Override
    public int getMonitorDotsPerInch(int monitor) {
        int dpi = nativeInstance.macMonitorDpi(monitor);
        return dpi > 0 ? dpi : 96;
    }

    @Override
    public String getMonitorName(int monitor) {
        String name = nativeInstance.macMonitorName(monitor);
        return name == null || name.length() == 0 ? "display-" + monitor : name;
    }

    @Override
    public int getPrimaryMonitor() {
        return Math.max(0, nativeInstance.macPrimaryMonitor());
    }

    @Override
    public int getMonitorForWindow(Object p) {
        int s = slot(p);
        if (s < 0) {
            return getPrimaryMonitor();
        }
        return Math.max(0, nativeInstance.macMonitorForWindow(s));
    }

    @Override
    public int getMonitorForMainWindow() {
        // The application's own window moves between displays like any other, so
        // the SPI default of "the primary monitor" reports the wrong work area,
        // scale and density the moment it has been dragged to an external screen.
        return Math.max(0, nativeInstance.macMonitorForMainWindow());
    }

    @Override
    public int[] getMainWindowBounds(int[] out) {
        if (!nativeInstance.macMainWindowGetBounds(out)) {
            return super.getMainWindowBounds(out);
        }
        return out;
    }
}
