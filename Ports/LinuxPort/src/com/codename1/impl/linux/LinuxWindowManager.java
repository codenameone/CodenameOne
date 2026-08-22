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
package com.codename1.impl.linux;

import com.codename1.impl.WindowManager;
import com.codename1.ui.Display;
import com.codename1.ui.Image;

/**
 * The native Linux implementation of the desktop windowing contract.
 *
 * <p>Each Codename One window is a slot in the native table in
 * {@code cn1_linux_desktopwindow.c}, with its own GtkWindow, drawing area, peer
 * overlay and cairo back buffer. The application's main window keeps its own file
 * statics and is not part of that table, so the single-window path is unchanged.</p>
 *
 * @author Shai Almog
 */
public class LinuxWindowManager extends WindowManager {

    /** Flag selectors matching {@code cn1DesktopFlagOnMain}. */
    private static final int FLAG_RESIZABLE = 0;
    private static final int FLAG_ALWAYS_ON_TOP = 1;
    private static final int FLAG_MODAL = 2;
    private static final int FLAG_DECORATED = 3;
    private static final int FLAG_UTILITY = 4;
    private static final int FLAG_SENSITIVE = 5;

    /** State selectors matching {@code cn1DesktopStateOnMain}. */
    private static final int STATE_RESTORE = 0;
    private static final int STATE_MINIMIZE = 1;
    private static final int STATE_TOGGLE_MAXIMIZE = 2;
    private static final int STATE_PRESENT = 3;

    /** One native window, identified by its slot in the native table. */
    static final class Peer {
        final int slot;
        final int windowId;
        /// The peer of the window that owns this one, or null.
        ///
        /// GTK expresses ownership as a transient-for hint, which keeps an owned
        /// window above its owner but does not take it down with it -- unlike Win32,
        /// where the window manager hides owned windows and reports it. So the
        /// cascade is kept here, as the Catalyst port does for the same reason.
        Object owner;
        /// True while this window is hidden only because its owner is.
        boolean hiddenByOwner;
        boolean visible;

        Peer(int slot, int windowId) {
            this.slot = slot;
            this.windowId = windowId;
        }
    }

    /// Every live window, so an owner can find the windows it owns.
    private static final java.util.List<Peer> peers = new java.util.ArrayList<Peer>();

    /// Stands in for the application's main window, which has no `Peer`.
    private static final Object MAIN_WINDOW = new Object();

    private static java.util.List<Peer> ownedBy(Object owner) {
        java.util.List<Peer> out = new java.util.ArrayList<Peer>();
        synchronized (peers) {
            for (Peer each : peers) {
                if (each.owner == owner) { //NOPMD CompareObjectsWithEquals
                    out.add(each);
                }
            }
        }
        return out;
    }

    /// Applies an owner's visibility to every window it owns, to any depth, and tells
    /// the framework about each window that actually changed.
    ///
    /// Ownership is only assigned when a window is created, so the graph is a tree and
    /// this cannot cycle.
    private static void cascadeFrom(Object owner, boolean shown) {
        for (Peer child : ownedBy(owner)) {
            boolean changed = false;
            if (shown) {
                if (child.hiddenByOwner) {
                    child.hiddenByOwner = false;
                    child.visible = true;
                    LinuxNative.desktopWindowShow(child.slot, true);
                    changed = true;
                }
            } else if (child.visible) {
                child.hiddenByOwner = true;
                child.visible = false;
                LinuxNative.desktopWindowShow(child.slot, false);
                changed = true;
            }
            if (changed) {
                // Unmapping the native window alone leaves the framework believing it
                // is up: it keeps painting and animating it and fires no lifecycle
                // event.
                if (shown) {
                    com.codename1.ui.Display.getInstance().windowShowNotify(child.windowId);
                } else {
                    com.codename1.ui.Display.getInstance().windowHideNotify(child.windowId);
                }
            }
            // Going down, a descendant follows even when its own parent was already
            // hidden by the application. Coming back up, only a child that actually
            // reappeared may restore the windows it owns.
            if (!shown || child.visible) {
                cascadeFrom(child, shown);
            }
        }
    }

    private static int slot(Object p) {
        return p instanceof Peer ? ((Peer) p).slot : -1;
    }

    /// The desktop-window slot hosting the given component, or
    /// `LinuxImplementation#MAIN_WINDOW_SLOT` when it lives in the application's main
    /// window. Native peers, the text editor and the browser all need this to reach
    /// the right window's overlay; without it they were placed over the main window
    /// whatever window they belonged to.
    static int slotForComponent(com.codename1.ui.Component cmp) {
        Object peer = com.codename1.ui.Display.getInstance().getWindowPeerForComponent(cmp);
        if (peer == null) {
            return LinuxImplementation.MAIN_WINDOW_SLOT;
        }
        int s = slot(peer);
        return s < 0 ? LinuxImplementation.MAIN_WINDOW_SLOT : s;
    }

    // ---- lifecycle -----------------------------------------------------------

    @Override
    public Object createWindow(int windowId, String title, int x, int y, int width, int height,
            boolean decorated, boolean resizable, Object parentPeer, boolean positionSet,
            boolean ownedByMainWindow) {
        // The transient parent is what makes an owned window stay above its owner and
        // is also what scopes GTK's modality. -2 asks for the application's main
        // window; -1 leaves the window unowned rather than silently parenting it.
        int ownerSlot = parentPeer != null ? slot(parentPeer) : (ownedByMainWindow ? -2 : -1);
        int s = LinuxNative.desktopWindowCreate(windowId, title == null ? "" : title,
                x, y, width, height, decorated, resizable, ownerSlot, positionSet);
        if (s < 0) {
            return null;
        }
        Peer created = new Peer(s, windowId);
        created.owner = ownedByMainWindow ? MAIN_WINDOW : parentPeer;
        synchronized (peers) {
            peers.add(created);
        }
        return created;
    }

    @Override
    public void show(Object peer) {
        int s = slot(peer);
        if (s < 0) {
            return;
        }
        Peer w = (Peer) peer;
        w.visible = true;
        w.hiddenByOwner = false;
        LinuxNative.desktopWindowShow(s, true);
        // Only the ones this owner took down. A child hidden by the application stays
        // hidden, exactly as AWT and the Catalyst port behave.
        cascadeFrom(w, true);
    }

    @Override
    public void hide(Object peer) {
        int s = slot(peer);
        if (s < 0) {
            return;
        }
        Peer w = (Peer) peer;
        w.visible = false;
        // An explicit hide takes the window's visibility over from any owner, so the
        // owner's restore must not bring it back.
        w.hiddenByOwner = false;
        LinuxNative.desktopWindowShow(s, false);
        // GTK leaves owned windows alone when their transient parent is unmapped, so
        // without this the children either stayed on screen without their owner or
        // were unmapped with no notification -- either way the framework went on
        // painting and animating them.
        cascadeFrom(w, false);
    }

    @Override
    public void dispose(Object peer) {
        int s = slot(peer);
        if (peer instanceof Peer) {
            synchronized (peers) {
                peers.remove(peer);
                // An owned window outliving its owner would keep a dangling reference
                // and could be matched against a later peer at the same address.
                for (Peer each : peers) {
                    if (each.owner == peer) { //NOPMD CompareObjectsWithEquals
                        each.owner = null;
                    }
                }
            }
        }
        if (s >= 0) {
            LinuxNative.desktopWindowDestroy(s);
        }
    }

    // ---- attributes ------------------------------------------------------------

    @Override
    public void setTitle(Object peer, String title) {
        int s = slot(peer);
        if (s >= 0) {
            LinuxNative.desktopWindowSetTitle(s, title == null ? "" : title);
        }
    }

    @Override
    public void setBounds(Object peer, int x, int y, int width, int height) {
        int s = slot(peer);
        if (s >= 0) {
            LinuxNative.desktopWindowSetBounds(s, x, y, width, height);
        }
    }

    /// {@inheritDoc}
    ///
    /// A Form lives in the application's own window, so centring a window over a
    /// Form means centring over that window. Left unimplemented the framework fell
    /// back to the monitor work area, which is a different place whenever the main
    /// window has been moved, resized or simply does not fill the screen.
    @Override
    public int[] getMainWindowBounds(int[] out) {
        if (out == null || out.length < 4) {
            return null;
        }
        return LinuxNative.mainWindowGetBounds(out) ? out : null;
    }

    @Override
    public int[] getBounds(Object peer, int[] out) {
        int s = slot(peer);
        if (s >= 0) {
            LinuxNative.desktopWindowGetBounds(s, out);
        }
        return out;
    }

    @Override
    public int getWidth(Object peer) {
        int s = slot(peer);
        return s < 0 ? 0 : LinuxNative.desktopWindowGetWidth(s);
    }

    @Override
    public int getHeight(Object peer) {
        int s = slot(peer);
        return s < 0 ? 0 : LinuxNative.desktopWindowGetHeight(s);
    }

    @Override
    public void setResizable(Object peer, boolean resizable) {
        int s = slot(peer);
        if (s >= 0) {
            LinuxNative.desktopWindowSetFlag(s, FLAG_RESIZABLE, resizable);
        }
    }

    @Override
    public void setDecorated(Object peer, boolean decorated) {
        int s = slot(peer);
        if (s >= 0) {
            LinuxNative.desktopWindowSetFlag(s, FLAG_DECORATED, decorated);
        }
    }

    @Override
    public void setAlwaysOnTop(Object peer, boolean alwaysOnTop) {
        int s = slot(peer);
        if (s >= 0) {
            LinuxNative.desktopWindowSetFlag(s, FLAG_ALWAYS_ON_TOP, alwaysOnTop);
        }
    }

    @Override
    public void setMinimumSize(Object peer, int width, int height) {
        int s = slot(peer);
        if (s >= 0) {
            LinuxNative.desktopWindowSetMinimumSize(s, width, height);
        }
    }

    @Override
    public void setUtilityWindow(Object peer, boolean utility) {
        int s = slot(peer);
        if (s >= 0) {
            LinuxNative.desktopWindowSetFlag(s, FLAG_UTILITY, utility);
        }
    }

    @Override
    public void setModal(Object peer, boolean modal, boolean applicationWide, Object ownerPeer) {
        // Codename One blocks input itself; this only gives the window manager the hint
        // it needs for correct stacking and focus.
        //
        // Only for an application wide modal, though. gtk_window_set_modal() makes the
        // window modal for the whole application rather than for its transient parent,
        // so raising it for MODALITY_WINDOW made every other window and the main form
        // unusable -- while Display.blocks() deliberately blocks only the owner. A
        // window scoped modal expresses its scope through the per-window sensitivity
        // wiring in setInputEnabled instead.
        int s = slot(peer);
        if (s >= 0) {
            LinuxNative.desktopWindowSetFlag(s, FLAG_MODAL, modal && applicationWide);
        }
    }

    @Override
    public void setInputEnabled(Object peer, boolean enabled) {
        int s = slot(peer);
        if (s >= 0) {
            LinuxNative.desktopWindowSetFlag(s, FLAG_SENSITIVE, enabled);
        }
    }

    @Override
    public void setMainWindowInputEnabled(boolean enabled) {
        LinuxNative.mainWindowSetSensitive(enabled);
    }

    @Override
    public void setIcon(Object peer, Image icon) {
        // Not supported yet: the port has no GdkPixbuf conversion for a CN1 image.
    }

    @Override
    public void requestFocus(Object peer) {
        int s = slot(peer);
        if (s >= 0) {
            LinuxNative.desktopWindowSetState(s, STATE_PRESENT);
        }
    }

    @Override
    public void minimize(Object peer) {
        int s = slot(peer);
        if (s >= 0) {
            LinuxNative.desktopWindowSetState(s, STATE_MINIMIZE);
        }
    }

    @Override
    public void restore(Object peer) {
        int s = slot(peer);
        if (s >= 0) {
            LinuxNative.desktopWindowSetState(s, STATE_RESTORE);
        }
    }

    @Override
    public void toggleMaximize(Object peer) {
        int s = slot(peer);
        if (s >= 0) {
            LinuxNative.desktopWindowSetState(s, STATE_TOGGLE_MAXIMIZE);
        }
    }

    // ---- rendering ------------------------------------------------------------------

    @Override
    public Object getNativeGraphics(Object peer) {
        int s = slot(peer);
        if (s < 0) {
            return null;
        }
        return Long.valueOf(LinuxNative.desktopWindowGraphics(s));
    }

    @Override
    public void flushGraphics(Object peer, int x, int y, int width, int height) {
        int s = slot(peer);
        if (s >= 0) {
            LinuxNative.desktopWindowFlush(s, x, y, width, height);
        }
    }

    /// Reads this window's own back buffer back, rather than letting
    /// `com.codename1.ui.Window#capture()` fall back to re-rendering the component
    /// tree. The fallback produces the content the window *should* be showing, so it
    /// cannot tell a correct window from one whose raster and hierarchy disagree --
    /// which is exactly what the windowed screenshot goldens are here to catch.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window's native peer
    ///
    /// #### Returns
    ///
    /// the native image, or null when the window has no surface to read
    @Override
    public Object capture(Object peer) {
        int s = slot(peer);
        if (s < 0) {
            return null;
        }
        byte[] png = LinuxNative.captureDesktopWindowToPngBytes(s);
        if (png == null || png.length == 0) {
            return null;
        }
        long img = LinuxNative.createImageFromBytes(png, 0, png.length);
        if (img == 0) {
            return null;
        }
        return Long.valueOf(img);
    }

    @Override
    public void setPaintDirtyRegionClip(Object peer, int x, int y, int width, int height) {
        int s = slot(peer);
        if (s < 0) {
            return;
        }
        long g = LinuxNative.desktopWindowGraphics(s);
        if (g != 0) {
            // Cairo draws into a persistent surface, so a clip set while a component
            // paints has to be confined to the region about to be flushed or an
            // oversized fill leaves stale pixels behind (issue #5273).
            LinuxNative.setFlushRect(g, x, y, width, height);
        }
    }

    // ---- monitors ----------------------------------------------------------------------

    @Override
    public int getMonitorCount() {
        return Math.max(1, LinuxNative.monitorCount());
    }

    @Override
    public int[] getMonitorBounds(int monitor, int[] out) {
        LinuxNative.monitorBounds(monitor, false, out);
        return out;
    }

    @Override
    public int[] getMonitorWorkArea(int monitor, int[] out) {
        LinuxNative.monitorBounds(monitor, true, out);
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
        // GTK exposes only an integer scale factor, which is what actually governs
        // how the toolkit renders, so that is what a window's scale reports.
        int scale = LinuxNative.monitorScale(monitor);
        return scale > 0 ? scale : 1.0;
    }

    @Override
    public int getMonitorDotsPerInch(int monitor) {
        int dpi = LinuxNative.monitorDpi(monitor);
        return dpi > 0 ? dpi : 96;
    }

    @Override
    public String getMonitorName(int monitor) {
        return "display-" + monitor;
    }

    @Override
    public int getPrimaryMonitor() {
        return Math.max(0, LinuxNative.primaryMonitor());
    }

    @Override
    public int getMonitorForWindow(Object peer) {
        int s = slot(peer);
        if (s < 0) {
            return getPrimaryMonitor();
        }
        return Math.max(0, LinuxNative.monitorForWindow(s));
    }

    @Override
    public int getMonitorForMainWindow() {
        return Math.max(0, LinuxNative.monitorForMainWindow());
    }
}
