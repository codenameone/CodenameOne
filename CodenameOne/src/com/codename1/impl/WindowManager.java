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
    /// - `parentPeer`: the owning window's peer, or null when there is no owner
    ///
    /// #### Returns
    ///
    /// the opaque peer identifying the new window
    public abstract Object createWindow(int windowId, String title, int x, int y,
            int width, int height, boolean decorated, boolean resizable, Object parentPeer);

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

    /// Applies the platform's own modality to the window.
    ///
    /// Codename One blocks input to the windows behind a modal window itself, so a
    /// port that cannot do this stays correct. Implementing it still gives the user
    /// the focus, dimming and taskbar behaviour the platform expects.
    ///
    /// #### Parameters
    ///
    /// - `peer`: the window peer
    ///
    /// - `modal`: true to make the window modal
    public void setModal(Object peer, boolean modal) {
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
}
