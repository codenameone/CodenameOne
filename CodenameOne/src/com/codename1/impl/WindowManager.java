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
package com.codename1.impl;

import com.codename1.ui.Command;
import com.codename1.ui.Image;

/// The whole native windowing contract for a port, kept out of
/// `CodenameOneImplementation` so that adding desktop windows does not add several
/// dozen methods to an already very large class.
///
/// A port that has a windowing system returns an instance from
/// `CodenameOneImplementation#getWindowManager()`; one that has none returns null.
/// That null **is** the capability query, so there is no separate supported flag
/// that could drift out of step with it.
///
/// Windows are identified by two different handles. The `windowId` is an int chosen
/// by the framework and handed to `#createWindow` -- a port must store it and pass it
/// back on every event callback, because input arrives on the platform's own thread
/// where a map lookup would need locking. The peer is the opaque object the port
/// returns from `#createWindow`, and it is what every other method here takes.
///
/// Unless a method says otherwise it is invoked on the Codename One event dispatch
/// thread, exactly like the single window methods it mirrors on
/// `CodenameOneImplementation`. A port that needs its own UI thread marshals
/// internally.
///
/// Only the operations every windowing system provides are abstract. Everything a
/// platform might reasonably lack has an inert default, so a later addition here
/// never breaks an existing port.
///
/// @author Shai Almog
public abstract class WindowManager {

    // ---- window lifecycle -----------------------------------------------------

    /// Creates a native window without showing it.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: framework assigned id, to be echoed back on every event
    ///
    /// - `title`: the initial window title
    ///
    /// - `x`: the initial x position in desktop coordinates
    ///
    /// - `y`: the initial y position in desktop coordinates
    ///
    /// - `width`: the initial width
    ///
    /// - `height`: the initial height
    ///
    /// - `decorated`: true for a normal titled and bordered window
    ///
    /// - `resizable`: true if the user may resize it
    ///
    /// - `parentPeer`: the owning window's peer, or null when the owner is the
    ///   application's main window or there is no owner at all -- see
    ///   `ownedByMainWindow`
    ///
    /// - `positionSet`: true when `x` and `y` are a position the application chose.
    ///   When false the platform places the window. A negative coordinate cannot
    ///   serve as the "unspecified" marker, because a monitor left of or above the
    ///   primary display has a negative origin and a window can legitimately be
    ///   restored onto it.
    ///
    /// - `ownedByMainWindow`: true when the owner is the application's main window,
    ///   which has no peer here. With `parentPeer` null this is what separates an
    ///   owned window from an unowned top level one.
    ///
    /// #### Returns
    ///
    /// the opaque peer identifying the new window
    public abstract Object createWindow(int windowId, String title, int x, int y,
            int width, int height, boolean decorated, boolean resizable, Object parentPeer,
            boolean positionSet, boolean ownedByMainWindow);

    /// Maps the window onto the screen.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    public abstract void show(Object peer);

    /// Unmaps the window, leaving it able to be shown again.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    public abstract void hide(Object peer);

    /// Destroys the window and releases its native resources.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    public abstract void dispose(Object peer);

    // ---- window attributes ------------------------------------------------------

    /// Sets the window title.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    ///
    /// - `title`: the title to display
    public abstract void setTitle(Object peer, String title);

    /// Moves and resizes the window.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    ///
    /// - `x`: the x position in desktop coordinates
    ///
    /// - `y`: the y position in desktop coordinates
    ///
    /// - `width`: the new width
    ///
    /// - `height`: the new height
    public abstract void setBounds(Object peer, int x, int y, int width, int height);

    /// Reads the window bounds, including any native chrome.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    ///
    /// - `out`: a four element array receiving x, y, width and height
    ///
    /// #### Returns
    ///
    /// the array that was passed in
    public abstract int[] getBounds(Object peer, int[] out);

    /// Returns the width of the window's drawable area in device pixels.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    ///
    /// #### Returns
    ///
    /// the drawable width
    public abstract int getWidth(Object peer);

    /// Returns the height of the window's drawable area in device pixels.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    ///
    /// #### Returns
    ///
    /// the drawable height
    public abstract int getHeight(Object peer);

    /// Sets whether the user may resize the window.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    ///
    /// - `resizable`: true to allow resizing
    public void setResizable(Object peer, boolean resizable) {
    }

    /// Sets the smallest size the user may resize the window to, or clears the
    /// constraint when either dimension is zero or less.
    ///
    /// A port that cannot express this leaves the default in place; the framework
    /// additionally clamps a delivered resize, so the constraint holds either way.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    ///
    /// - `width`: the minimum width in Codename One pixels
    ///
    /// - `height`: the minimum height in Codename One pixels
    public void setMinimumSize(Object peer, int width, int height) {
    }

    /// Sets whether the platform draws the title bar and border.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    ///
    /// - `decorated`: true for native decorations
    public void setDecorated(Object peer, boolean decorated) {
    }

    /// Keeps the window above the other windows of the application.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    ///
    /// - `alwaysOnTop`: true to float the window
    public void setAlwaysOnTop(Object peer, boolean alwaysOnTop) {
    }

    /// Marks the window as a tool or palette window, which the platform keeps out of
    /// the task bar and window switcher and typically gives lighter chrome.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    ///
    /// - `utility`: true for a utility window
    public void setUtilityWindow(Object peer, boolean utility) {
    }

    /// Applies the platform's own modality to the window.
    ///
    /// Codename One blocks input to the windows behind a modal window itself, so a
    /// port that cannot do this stays correct. Implementing it still gives the user
    /// the focus, dimming and taskbar behaviour the platform expects.
    ///
    /// The scope matters, because a port typically implements this by disabling
    /// another window: an application modal blocks everything, while a window modal
    /// blocks only the window that owns it, and disabling the main window for the
    /// latter would make an unrelated part of the application unusable.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    ///
    /// - `modal`: true to make the window modal
    ///
    /// - `applicationWide`: true for `com.codename1.ui.Window#MODALITY_APPLICATION`,
    ///   false for `com.codename1.ui.Window#MODALITY_WINDOW`
    ///
    /// - `ownerPeer`: the peer of the window this one blocks, or null when it blocks
    ///   the application's main window or nothing at all
    public void setModal(Object peer, boolean modal, boolean applicationWide, Object ownerPeer) {
    }

    /// Rebuilds a window's native surface after the platform destroyed it without
    /// asking, and reports whether that succeeded.
    ///
    /// Only needed where the platform's own close control cannot be disabled: Mac
    /// Catalyst hands a scene disconnect over after the fact, so a window a modal is
    /// blocking can be closed by the user even though the framework forbids it. Being
    /// able to put it back is what keeps that from breaking the modality contract.
    ///
    /// A port that cannot do this returns false and the window is disposed instead,
    /// which is honest -- the surface really is gone.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    ///
    /// #### Returns
    ///
    /// true if the native surface is being rebuilt
    public boolean reopen(Object peer) {
        return false;
    }

    /// Enables or disables native input for one window.
    ///
    /// The framework calls this for every open window whenever the modal stack
    /// changes, having already worked out which of them are blocked -- that answer
    /// depends on the whole stack, on each window's modality scope and on who owns it,
    /// so a port must not try to derive it from `#setModal`.
    ///
    /// Worth implementing even though the framework filters input itself, because a
    /// blocked window's own title bar is outside that filter: its close button still
    /// reaches the application.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    ///
    /// - `enabled`: false while the window is blocked by a modal window
    public void setInputEnabled(Object peer, boolean enabled) {
    }

    /// Enables or disables native input for the application's main window, which has
    /// no peer. See `#setInputEnabled(Object, boolean)`.
    ///
    /// #### Parameters
    ///
    /// - `enabled`: false while the main window is blocked by a modal window
    public void setMainWindowInputEnabled(boolean enabled) {
    }

    /// Sets the window icon where the platform shows one.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    ///
    /// - `icon`: the icon to display
    public void setIcon(Object peer, Image icon) {
    }

    /// Raises the window and gives it keyboard focus.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    public void requestFocus(Object peer) {
    }

    /// Minimizes the window.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    public void minimize(Object peer) {
    }

    /// Restores a minimized window.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    public void restore(Object peer) {
    }

    /// Toggles the window between maximized and its previous size.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    public void toggleMaximize(Object peer) {
    }

    // ---- rendering ----------------------------------------------------------------

    /// Returns the native graphics for this window's drawable. Called once per frame
    /// on the event dispatch thread, mirroring
    /// `CodenameOneImplementation#getNativeGraphics()`.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    ///
    /// #### Returns
    ///
    /// the native graphics object
    public abstract Object getNativeGraphics(Object peer);

    /// Presents the given region of the window, mirroring
    /// `CodenameOneImplementation#flushGraphics(int, int, int, int)`.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    ///
    /// - `x`: the region's x origin
    ///
    /// - `y`: the region's y origin
    ///
    /// - `width`: the region width
    ///
    /// - `height`: the region height
    public abstract void flushGraphics(Object peer, int x, int y, int width, int height);

    /// Per window counterpart of
    /// `CodenameOneImplementation#setPaintDirtyRegionClip(int, int, int, int)`, used
    /// by the immediate mode ports to confine a component's clip to the region that
    /// is about to be flushed.
    ///
    /// The default is inert rather than a delegation to the main surface version, so
    /// that a port which has not opted in cannot clamp a window's clip against the
    /// main window's state.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    ///
    /// - `x`: the region's x origin
    ///
    /// - `y`: the region's y origin
    ///
    /// - `width`: the region width
    ///
    /// - `height`: the region height
    public void setPaintDirtyRegionClip(Object peer, int x, int y, int width, int height) {
    }

    /// Captures the window's current contents.
    ///
    /// The ordinary screenshot path can only see the main surface, so the windowed
    /// screenshot tests depend on this.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    ///
    /// #### Returns
    ///
    /// a native image of the window, or null when the port cannot capture one
    public Object capture(Object peer) {
        return null;
    }

    /// Installs this window's commands into whatever command surface the platform
    /// offers for a secondary window, typically a native menu bar on its own frame.
    ///
    /// A no-op by default. `com.codename1.ui.TopLevelContainer#addCommand` is shared
    /// with `Form`, so a `Window` accepts commands everywhere; a port with nowhere to
    /// put them simply does not show them, and an application can still activate them
    /// through `com.codename1.ui.Window#dispatchCommand`.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window's native peer
    ///
    /// - `commands`: the window's commands in the order they were added, never null
    public void setCommands(Object peer, Command[] commands) {
    }

    // ---- monitors --------------------------------------------------------------------

    /// Returns the number of monitors attached to the desktop.
    ///
    /// #### Returns
    ///
    /// the monitor count, at least one
    public abstract int getMonitorCount();

    /// Reads a monitor's full bounds in desktop coordinates.
    ///
    /// #### Parameters
    ///
    /// - `monitor`: the monitor offset
    ///
    /// - `out`: a four element array receiving x, y, width and height
    ///
    /// #### Returns
    ///
    /// the array that was passed in
    public abstract int[] getMonitorBounds(int monitor, int[] out);

    /// Reads the part of a monitor that is usable by windows, which excludes the
    /// task bar, dock and any reserved panels.
    ///
    /// #### Parameters
    ///
    /// - `monitor`: the monitor offset
    ///
    /// - `out`: a four element array receiving x, y, width and height
    ///
    /// #### Returns
    ///
    /// the array that was passed in
    public abstract int[] getMonitorWorkArea(int monitor, int[] out);

    /// Returns the density bucket of a monitor, as one of the `Display` density
    /// constants.
    ///
    /// #### Parameters
    ///
    /// - `monitor`: the monitor offset
    ///
    /// #### Returns
    ///
    /// the density constant
    public abstract int getMonitorDensity(int monitor);

    /// Returns a monitor's backing scale, such as one for a standard display and two
    /// for a high resolution one.
    ///
    /// #### Parameters
    ///
    /// - `monitor`: the monitor offset
    ///
    /// #### Returns
    ///
    /// the scale factor
    public abstract double getMonitorScale(int monitor);

    /// Returns a monitor's dots per inch.
    ///
    /// #### Parameters
    ///
    /// - `monitor`: the monitor offset
    ///
    /// #### Returns
    ///
    /// the resolution in dots per inch
    public abstract int getMonitorDotsPerInch(int monitor);

    /// Returns a human readable name for a monitor.
    ///
    /// #### Parameters
    ///
    /// - `monitor`: the monitor offset
    ///
    /// #### Returns
    ///
    /// the monitor name
    public abstract String getMonitorName(int monitor);

    /// Returns the offset of the primary monitor.
    ///
    /// #### Returns
    ///
    /// the primary monitor offset
    public abstract int getPrimaryMonitor();

    /// Returns the offset of the monitor a window currently sits on.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    ///
    /// #### Returns
    ///
    /// the monitor offset
    public abstract int getMonitorForWindow(Object peer);

    /// Returns the monitor the application's main window currently sits on.
    ///
    /// This is not answerable through `#getMonitorForWindow(Object)`, which takes a
    /// secondary window's peer; the main window has none. Without it, asking for the
    /// monitor of the main `Form` reported the primary monitor even when the
    /// application had been dragged to a second display, so an application
    /// positioning a window against the main form got the wrong work area, scale and
    /// density.
    ///
    /// The default answers the primary monitor, which is correct for a port whose
    /// main window cannot move between displays.
    ///
    /// #### Returns
    ///
    /// the monitor offset
    public int getMonitorForMainWindow() {
        return getPrimaryMonitor();
    }
}
