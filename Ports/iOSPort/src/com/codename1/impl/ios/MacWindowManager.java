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
package com.codename1.impl.ios;

import com.codename1.impl.WindowManager;
import com.codename1.ui.Desktop;
import com.codename1.ui.Display;
import com.codename1.ui.Image;
import com.codename1.ui.Window;

/**
 * The Mac Catalyst implementation of the desktop windowing contract.
 *
 * <p>Each Codename One window becomes a {@code UIWindowScene}. Unlike the other
 * desktop ports, the window's content is rendered into a mutable image and the
 * finished raster is handed to the scene's view, rather than the window owning a
 * second Metal surface. That is deliberate: the render path caches its device,
 * pipeline state and glyph atlas against the single rendering view, and making
 * those per-scene is a large refactor of the hottest code in the product, without
 * ARC. The scene still owns a real UIKit view hierarchy, so native peers and
 * native text editing work normally inside a window.</p>
 *
 * <p>Multiple scenes have to be enabled in Info.plist for any of this to work. The
 * builder emits that key for the Catalyst slice and nowhere else, and
 * {@code IOSImplementation.getWindowManager()} reads it back out of the bundle, so
 * this manager is never offered to a build that could not actually open a
 * window.</p>
 *
 * @author Shai Almog
 */
public class MacWindowManager extends WindowManager {

    private final IOSImplementation impl;

    MacWindowManager(IOSImplementation impl) {
        this.impl = impl;
        // UIScreen notifications are the only way a Catalyst app learns that a
        // display was attached, removed or changed mode, so a monitor listener
        // depends entirely on this being installed.
        IOSImplementation.nativeInstance.macWindowWatchScreens();
    }

    /** One native window: its slot, and the raster it is rendered through. */
    static final class Peer {
        final int slot;
        final int windowId;
        Object mutableImage;
        int rasterWidth;
        int rasterHeight;

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
        // Catalyst scenes have no owner relation and the window server places them,
        // so both of the new hints are deliberately unused here.
        int s = IOSImplementation.nativeInstance.macWindowCreate(windowId,
                title == null ? "" : title, x, y, width, height, decorated, resizable);
        if (s < 0) {
            return null;
        }
        return new Peer(s, windowId);
    }

    @Override
    public void show(Object p) {
        int s = slot(p);
        if (s >= 0) {
            IOSImplementation.nativeInstance.macWindowShow(s, true);
        }
    }

    @Override
    public void hide(Object p) {
        int s = slot(p);
        if (s >= 0) {
            IOSImplementation.nativeInstance.macWindowShow(s, false);
        }
    }

    @Override
    public void dispose(Object p) {
        Peer w = peer(p);
        if (w == null) {
            return;
        }
        w.mutableImage = null;
        IOSImplementation.nativeInstance.macWindowDestroy(w.slot);
    }

    // ---- attributes ------------------------------------------------------------

    @Override
    public void setTitle(Object p, String title) {
        int s = slot(p);
        if (s >= 0) {
            IOSImplementation.nativeInstance.macWindowSetTitle(s, title == null ? "" : title);
        }
    }

    @Override
    public void setBounds(Object p, int x, int y, int width, int height) {
        int s = slot(p);
        if (s >= 0) {
            IOSImplementation.nativeInstance.macWindowSetBounds(s, x, y, width, height);
        }
    }

    @Override
    public int[] getBounds(Object p, int[] out) {
        int s = slot(p);
        if (s >= 0) {
            IOSImplementation.nativeInstance.macWindowGetBounds(s, out);
        }
        return out;
    }

    @Override
    public int getWidth(Object p) {
        int s = slot(p);
        return s < 0 ? 0 : IOSImplementation.nativeInstance.macWindowGetWidth(s);
    }

    @Override
    public int getHeight(Object p) {
        int s = slot(p);
        return s < 0 ? 0 : IOSImplementation.nativeInstance.macWindowGetHeight(s);
    }

    @Override
    public void requestFocus(Object p) {
        int s = slot(p);
        if (s >= 0) {
            // 3 == present, the one window-state operation Catalyst exposes to a
            // UIKit app; minimize and zoom belong to the window manager there.
            IOSImplementation.nativeInstance.macWindowSetState(s, 3);
        }
    }

    @Override
    public void setIcon(Object p, Image icon) {
        // A Mac window has no per-window icon.
    }

    @Override
    public boolean reopen(Object p) {
        int s = slot(p);
        return s >= 0 && IOSImplementation.nativeInstance.macWindowReopen(s);
    }

    @Override
    public void setInputEnabled(Object p, boolean enabled) {
        int s = slot(p);
        if (s >= 0) {
            // Covers input inside the window. The scene's title bar belongs to
            // AppKit rather than to the application, so its close button stays live
            // even while the window is blocked -- Catalyst offers no way to disable
            // it, and a close there is reported after the fact as a disposal.
            IOSImplementation.nativeInstance.macWindowSetInputEnabled(s, enabled);
        }
    }

    // ---- rendering ------------------------------------------------------------------

    @Override
    public Object getNativeGraphics(Object p) {
        Peer w = peer(p);
        if (w == null) {
            return null;
        }
        // Sized from the framework's window rather than the scene's drawable. The two
        // agree once the scene has settled, but the scene arrives and resizes
        // asynchronously, so a raster allocated from the drawable can be left holding
        // an intermediate size that nothing later reconciles -- the framework paints
        // into it at its own size and the capture then disagrees with the window.
        // The window is what was laid out and painted, so it is what the raster has
        // to match; the drawable is only the fallback until a window exists.
        Window window = Desktop.getInstance().windowById(w.windowId);
        int width = Math.max(1, window != null ? window.getWidth() : getWidth(p));
        int height = Math.max(1, window != null ? window.getHeight() : getHeight(p));
        if (w.mutableImage == null || w.rasterWidth != width || w.rasterHeight != height) {
            w.mutableImage = impl.createMutableImage(width, height, 0xff000000);
            w.rasterWidth = width;
            w.rasterHeight = height;
        }
        return impl.getNativeGraphics(w.mutableImage);
    }

    @Override
    public void flushGraphics(Object p, int x, int y, int width, int height) {
        Peer w = peer(p);
        if (w == null || w.mutableImage == null) {
            return;
        }
        // Read the finished frame back and hand it to the scene's view. The whole
        // raster is presented rather than the dirty rect because the view holds one
        // image; the dirty region still bounds what was actually redrawn into it.
        int[] argb = new int[w.rasterWidth * w.rasterHeight];
        impl.getRGB(w.mutableImage, argb, 0, 0, 0, w.rasterWidth, w.rasterHeight);
        IOSImplementation.nativeInstance.macWindowPresent(w.slot, argb,
                w.rasterWidth, w.rasterHeight);
    }

    @Override
    public Object capture(Object p) {
        // The window's content already lives in a mutable image -- that is how it is
        // rendered on this platform -- so a capture is simply that raster. The live
        // image is returned rather than a copy, so a caller that wants a stable frame
        // should capture between paints, which is what the screenshot harness does.
        Peer w = peer(p);
        if (w == null) {
            return null;
        }
        return w.mutableImage;
    }

    // ---- monitors ----------------------------------------------------------------------

    @Override
    public int getMonitorCount() {
        return Math.max(1, IOSImplementation.nativeInstance.macMonitorCount());
    }

    @Override
    public int[] getMonitorBounds(int monitor, int[] out) {
        IOSImplementation.nativeInstance.macMonitorBounds(monitor, false, out);
        return out;
    }

    @Override
    public int[] getMonitorWorkArea(int monitor, int[] out) {
        IOSImplementation.nativeInstance.macMonitorBounds(monitor, true, out);
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
        return IOSImplementation.nativeInstance.macMonitorScaleTimes100(monitor) / 100.0;
    }

    @Override
    public int getMonitorDotsPerInch(int monitor) {
        int dpi = IOSImplementation.nativeInstance.macMonitorDpi(monitor);
        return dpi > 0 ? dpi : 96;
    }

    @Override
    public String getMonitorName(int monitor) {
        return "display-" + monitor;
    }

    @Override
    public int getPrimaryMonitor() {
        return Math.max(0, IOSImplementation.nativeInstance.macPrimaryMonitor());
    }

    @Override
    public int getMonitorForWindow(Object p) {
        int s = slot(p);
        if (s < 0) {
            return getPrimaryMonitor();
        }
        return Math.max(0, IOSImplementation.nativeInstance.macMonitorForWindow(s));
    }
}
