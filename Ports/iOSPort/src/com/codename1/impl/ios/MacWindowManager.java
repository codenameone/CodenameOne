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
 * <p><b>Operations Catalyst cannot express.</b> These stay on the SPI's no-op
 * defaults, and are listed here rather than left to be discovered one at a time:
 * {@code setAlwaysOnTop}, {@code setUtilityWindow}, {@code minimize},
 * {@code restore} and {@code toggleMaximize} have no public UIKit equivalent for a
 * {@code UIWindowScene} -- AppKit owns that behaviour and Catalyst does not expose
 * it. {@code setModal} is a no-op because modality is decided by the framework and
 * enforced through {@code setInputEnabled}, which this port does implement, so
 * there is no native flag to set. {@code setPaintDirtyRegionClip} is an
 * optimisation the Java SE port also leaves out.</p>
 *
 * <p>{@code setDecorated} is partial by necessity: Catalyst cannot remove the
 * window frame, so it hides the title bar's title and toolbar, which is the part an
 * application supplying its own chrome needs.</p>
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
        /// Reused frame transfer buffer. A fresh int[] per flush allocated the whole
        /// window's raster every repaint -- about 33MB for a 4K window, so roughly
        /// 2GB/s at 60fps -- which is enough garbage to stall an animating window or
        /// have the system kill the app. Reallocated only when the raster resizes.
        int[] frameBuffer;
        /// The peer of the window that owns this one, or null.
        ///
        /// Catalyst scenes have no owner relation of their own, so the promise that an
        /// owned window follows its owner -- which the other three desktop ports get
        /// from the platform -- has to be kept here instead.
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

    /// Stands in for the application's main scene, which has no `Peer` of its own.
    /// A window owned by the main `Form` records this, so it still has an owner the
    /// cascade below can start from.
    private static final Object MAIN_WINDOW = new Object();


    private static Peer peer(Object p) {
        return p instanceof Peer ? (Peer) p : null;
    }

    /// Slot of the window hosting the given component, or -1 for the application's
    /// main scene. The native editor needs it to land in the right window's view.
    static int slotForComponent(com.codename1.ui.Component cmp) {
        Object peer = com.codename1.ui.Display.getInstance().getWindowPeerForComponent(cmp);
        return peer == null ? -1 : slot(peer);
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
        // Catalyst scenes have no native owner relation, so ownership is tracked here
        // and the cascade is emulated. A window owned by the main Form has a null
        // parentPeer -- the main scene has no Peer -- so ownedByMainWindow is the only
        // signal that it is owned at all; recording MAIN_WINDOW for it is what lets
        // the main scene take its owned windows down with it. positionSet matters too:
        // inferring it from the coordinates makes a window explicitly placed at 0,0
        // look unplaced, and the window server then puts it wherever it likes.
        int s = IOSImplementation.nativeInstance.macWindowCreate(windowId,
                title == null ? "" : title, x, y, width, height, decorated, resizable,
                positionSet);
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
    public void show(Object p) {
        Peer w = peer(p);
        if (w == null) {
            return;
        }
        // An owned window cannot be on screen while its owner is not, and the cascade
        // only reacts to an owner *changing* visibility -- it would never revisit a
        // window shown while its owner was already away. The owner is brought back
        // rather than this window left pending: Window.show() has already taken the
        // modal blocker by the time this runs, so a window that is never mapped would
        // block input to every other window and leave showModal() waiting for a window
        // nobody can see.
        showOwnerChain(w);
        w.visible = true;
        w.hiddenByOwner = false;
        IOSImplementation.nativeInstance.macWindowShow(w.slot, true);
        // Only the ones this owner took down. A child hidden by the application stays
        // hidden, exactly as AWT and GTK behave when an owner is shown again.
        cascadeFrom(w, true);
    }

    /// Maps every owner above the given window that is currently away, furthest first
    /// so an owner is never mapped before its own owner.
    ///
    /// A window owned by the main `Form` is mapped without this bringing the main
    /// scene back: un-minimizing the application's own scene is the window server's
    /// to do, not ours. Mapping anyway is the deliberate choice -- leaving the window
    /// pending instead would deadlock a modal, which is the worse of the two.
    private static void showOwnerChain(Peer w) {
        if (!(w.owner instanceof Peer)) {
            return;
        }
        Peer above = (Peer) w.owner;
        showOwnerChain(above);
        if (!above.visible) {
            above.visible = true;
            above.hiddenByOwner = false;
            IOSImplementation.nativeInstance.macWindowShow(above.slot, true);
            com.codename1.ui.Display.getInstance().windowShowNotify(above.windowId);
        }
    }

    /// The live windows owned by the given peer.
    private static java.util.List<Peer> ownedBy(Object ownerPeer) {
        java.util.List<Peer> out = new java.util.ArrayList<Peer>();
        synchronized (peers) {
            for (Peer each : peers) {
                if (each.owner == ownerPeer) { //NOPMD CompareObjectsWithEquals
                    out.add(each);
                }
            }
        }
        return out;
    }

    /// Records a window's new visibility and takes the windows it owns with it, which
    /// is how a Catalyst window follows its owner being minimized by the user -- there
    /// is no scene-level owner relation to do it for us.
    ///
    /// Runs on the EDT. The caller marshals it there so a native slot cannot be freed
    /// by a concurrent dispose and handed to a new window between the lookup here and
    /// the native call.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the window whose visibility changed, 0 for the main window
    ///
    /// - `shown`: true when it became visible, false when it went away
    static void windowVisibilityChanged(int windowId, boolean shown) {
        // Window id 0 is the main window, which has no Peer; windows owned by the main
        // Form carry the MAIN_WINDOW sentinel instead.
        Object owner = windowId == 0 ? MAIN_WINDOW : peerForWindowId(windowId);
        if (owner == null) {
            return;
        }
        if (owner instanceof Peer) {
            // Record what the platform just did to this window before cascading from
            // it. Without this, a window the user minimized on its own still looks
            // visible here, so a later owner hide marks it hidden-by-owner and the
            // owner's restore brings back a window the user had put away. The flag is
            // cleared either way: this change came from the platform, not from an
            // owner, so no owner may undo it.
            Peer self = (Peer) owner;
            self.visible = shown;
            self.hiddenByOwner = false;
        }
        cascadeFrom(owner, shown);
    }

    private static Peer peerForWindowId(int windowId) {
        synchronized (peers) {
            for (Peer each : peers) {
                if (each.windowId == windowId) {
                    return each;
                }
            }
        }
        return null;
    }

    /// Applies an owner's visibility to every window it owns, to any depth, and tells
    /// the framework about each window that actually changed.
    ///
    /// Ownership is only ever assigned when a window is created, so the graph is a
    /// tree and this cannot cycle.
    private static void cascadeFrom(Object owner, boolean shown) {
        for (Peer child : ownedBy(owner)) {
            boolean changed = false;
            if (shown) {
                if (child.hiddenByOwner) {
                    child.hiddenByOwner = false;
                    child.visible = true;
                    IOSImplementation.nativeInstance.macWindowShow(child.slot, true);
                    changed = true;
                }
            } else if (child.visible) {
                child.hiddenByOwner = true;
                child.visible = false;
                IOSImplementation.nativeInstance.macWindowShow(child.slot, false);
                changed = true;
            }
            if (changed) {
                // Setting the native hidden flag alone leaves the framework believing
                // the window is still up: it keeps painting it and fires no lifecycle
                // event. macWindowShow reports nothing back, so report it here.
                if (shown) {
                    com.codename1.ui.Display.getInstance().windowShowNotify(child.windowId);
                } else {
                    com.codename1.ui.Display.getInstance().windowHideNotify(child.windowId);
                }
            }
            // Going down, a descendant has to follow even when its own parent was
            // already hidden by the application. Coming back up, only a child that
            // actually reappeared may restore the windows it owns.
            if (!shown || child.visible) {
                cascadeFrom(child, shown);
            }
        }
    }

    @Override
    public void hide(Object p) {
        Peer w = peer(p);
        if (w == null) {
            return;
        }
        w.visible = false;
        // An explicit hide takes the window's visibility over from any owner. Leaving
        // this set would let the owner's restore show a window the application had
        // deliberately hidden, and report it restored while its component hierarchy is
        // still invisible.
        w.hiddenByOwner = false;
        IOSImplementation.nativeInstance.macWindowShow(w.slot, false);
        // An owned window cannot stay on screen without its owner. Recorded as
        // hidden-by-owner so showing the owner again brings back exactly the children
        // it took down, and not ones the application hid itself.
        cascadeFrom(w, false);
    }

    @Override
    public void dispose(Object p) {
        Peer w = peer(p);
        if (w != null) {
            synchronized (peers) {
                peers.remove(w);
                // An owned window outliving its owner would keep a dangling reference
                // and could be matched against a later peer at the same address.
                for (Peer each : peers) {
                    if (each.owner == w) { //NOPMD CompareObjectsWithEquals
                        each.owner = null;
                    }
                }
            }
        }
        if (w == null) {
            return;
        }
        w.mutableImage = null;
        // Released with the raster it belongs to; a disposed window has no frames left
        // to present, and on a large display this is a few tens of megabytes.
        w.frameBuffer = null;
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

    /// {@inheritDoc}
    ///
    /// Implemented rather than left as the inherited no-op. The framework's event
    /// filter drops packed input before it reaches a component, but a UIKit peer --
    /// a native editor, a web view, a media control -- is handed its touches
    /// directly by the window server and never passes through that filter, so the
    /// main window's peers stayed interactive under an application modal.
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
        return IOSImplementation.nativeInstance.macMainWindowGetBounds(out) ? out : null;
    }

    @Override
    public void setMainWindowInputEnabled(boolean enabled) {
        IOSImplementation.nativeInstance.macMainWindowSetInputEnabled(enabled);
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
        int needed = w.rasterWidth * w.rasterHeight;
        if (w.frameBuffer == null || w.frameBuffer.length < needed) {
            w.frameBuffer = new int[needed];
        }
        int[] argb = w.frameBuffer;
        impl.getRGB(w.mutableImage, argb, 0, 0, 0, w.rasterWidth, w.rasterHeight);
        IOSImplementation.nativeInstance.macWindowPresent(w.slot, argb,
                w.rasterWidth, w.rasterHeight);
    }

    @Override
    public Object capture(Object p) {
        // The window's content already lives in a mutable image -- that is how it is
        // rendered on this platform -- but that raster is the live one the next frame
        // paints into, so it cannot be handed out directly. Returning it made a
        // retained capture change under the caller as the window repainted, and let
        // anyone who asked it for a Graphics paint straight into what the window
        // presents. Every other port returns an independent readback and
        // Window.capture() is documented as the contents at the moment it is called.
        Peer w = peer(p);
        if (w == null || w.mutableImage == null) {
            return null;
        }
        int width = w.rasterWidth;
        int height = w.rasterHeight;
        if (width <= 0 || height <= 0) {
            return null;
        }
        // Read the pixels out and build a separate image from them. Immutable on
        // purpose: a snapshot has no writable graphics, so the previous failure mode
        // cannot come back through the copy either.
        Image live = Image.createImage(w.mutableImage);
        return Image.createImage(live.getRGB(), width, height).getImage();
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

    @Override
    public void setResizable(Object p, boolean resizable) {
        // Without this the SPI's no-op ran, so a window shown and then made
        // non-resizable stayed draggable while the framework reported it fixed.
        int s = slot(p);
        if (s >= 0) {
            IOSImplementation.nativeInstance.macWindowSetResizable(s, resizable);
        }
    }

    @Override
    public void setDecorated(Object p, boolean decorated) {
        // Catalyst cannot remove the window frame the way an undecorated desktop
        // window does, but it can hide the title bar's title and toolbar, which is
        // what an application supplying its own chrome needs. Without this the
        // framework reported the window as undecorated while it kept a standard
        // title bar -- and could show two sets of chrome at once.
        int s = slot(p);
        if (s >= 0) {
            IOSImplementation.nativeInstance.macWindowSetDecorated(s, decorated);
        }
    }

    @Override
    public void setMinimumSize(Object p, int width, int height) {
        // Window.sizeChangedInternal deliberately does not clamp, so without this
        // the constraint existed only in the getter and the user could resize below
        // it.
        int s = slot(p);
        if (s >= 0) {
            IOSImplementation.nativeInstance.macWindowSetMinimumSize(s, width, height);
        }
    }

    @Override
    public int getMonitorForMainWindow() {
        // The default answers the primary monitor, which is wrong here: the
        // application's own scene moves between displays like any other window, so a
        // Form positioned against reported the wrong work area, scale and density
        // once it had been dragged to an external screen.
        return Math.max(0, IOSImplementation.nativeInstance.macMonitorForMainWindow());
    }
}
