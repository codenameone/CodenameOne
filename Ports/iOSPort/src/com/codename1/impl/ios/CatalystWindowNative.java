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

/// The Mac Catalyst desktop-windowing natives, implemented in CN1MacWindows.m.
///
/// These live on their own binding class rather than on `IOSNative` because
/// ParparVM mangles the declaring package and class into every C symbol, so a
/// class is the unit at which a native can be excluded from a port. `IOSNative`
/// is shared verbatim with the native macOS port, whose windowing is AppKit and
/// whose window natives are therefore entirely different functions; leaving
/// these there would oblige that port to supply 29 `UIWindowScene` symbols it
/// has no implementation for.
///
/// Nothing here is available off Mac Catalyst. On iOS, iPadOS, tvOS and watchOS
/// the implementations compile out to no-ops, and on the native macOS port the
/// class is not shipped at all.
///
/// A window is addressed by the slot returned from `#macWindowCreate`. The
/// windowId passed in is the framework's own id, stored natively and echoed back
/// on every callback so events route without a lookup.
///
/// @author Shai Almog
class CatalystWindowNative {
    // ---- Mac Catalyst desktop windows (CN1MacWindows.m) ---------------------
    //
    // A window is addressed by the slot returned from macWindowCreate. The
    // windowId passed in is the framework's own id, stored natively and echoed
    // back on every callback so events route without a lookup. Every one of these
    // is a no-op on iOS proper, where the implementation is compiled out.

    native int macWindowCreate(int windowId, String title, int x, int y, int width, int height,
            boolean decorated, boolean resizable, boolean positionSet);

    native void macWindowDestroy(int slot);

    native void macWindowShow(int slot, boolean visible);

    /** The token of the scene request currently outstanding for this window, or 0. */
    native int macWindowRequestSeq(int slot);

    native void macWindowSetTitle(int slot, String title);

    native void macWindowSetBounds(int slot, int x, int y, int width, int height);

    native void macWindowGetBounds(int slot, int[] out);

    native boolean macMainWindowGetBounds(int[] out);

    native int macWindowGetWidth(int slot);

    native int macWindowGetHeight(int slot);

    native void macWindowSetState(int slot, int state);

    /** Requests a scene again after one was destroyed without the app getting a say. */
    /// Applies a resizability change to a window that may already have a scene.
    /// Records which window is being edited, so the native editor lands in its view.
    native void macWindowSetEditingSlot(int slot);

    native void macWindowSetResizable(int slot, boolean resizable);

    /// Applies a decoration change to a window that may already have a scene.
    native void macWindowSetDecorated(int slot, boolean decorated);

    /// Records a minimum size and applies it to an existing scene.
    native void macWindowSetMinimumSize(int slot, int width, int height);

    native boolean macWindowReopen(int slot);

    /** Enables or disables touch input, used while a modal window blocks this one. */
    native void macWindowSetInputEnabled(int slot, boolean enabled);

    native void macMainWindowSetInputEnabled(boolean enabled);

    /** Starts reporting display attach/remove/mode changes; idempotent. */
    native void macWindowWatchScreens();

    /**
     * Presents one rendered frame. The pixels are the window's own raster; the
     * native side wraps them in a CGImage and assigns it to the view's layer.
     */
    native void macWindowPresent(int slot, int[] argb, int width, int height);

    /**
     * True when the app's Info.plist actually enables multiple scenes. Without it
     * the system refuses to activate a second scene, so this is what decides
     * whether the windowing API reports itself supported.
     */
    native boolean macMultiWindowSupported();

    native int macMonitorCount();

    native int macPrimaryMonitor();

    native void macMonitorBounds(int monitor, boolean workArea, int[] out);

    native int macMonitorDpi(int monitor);

    native int macMonitorScaleTimes100(int monitor);

    native int macMonitorForWindow(int slot);

    /// The monitor the application's own Catalyst scene is on. The main window has
    /// no slot, so `#macMonitorForWindow(int)` cannot answer for it.
    native int macMonitorForMainWindow();

    /// Attaches a peer to the Catalyst window that owns it. Returns false when the
    /// window has no content view yet, so the caller keeps the peer where it is.
    native boolean macWindowAttachPeer(long peer, int slot, int x, int y, int w, int h);
}
