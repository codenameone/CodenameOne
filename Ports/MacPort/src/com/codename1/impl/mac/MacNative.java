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

/// The AppKit-only native bindings.
///
/// ParparVM mangles the declaring package and class into every C symbol, so a
/// native's class is the unit at which it can be included in or excluded from a
/// port. `IOSNative` is shared verbatim with the iOS port and may therefore only
/// declare natives whose implementation compiles against both `iphoneos` and
/// `macosx`. Anything that needs AppKit belongs here, where the iOS build never
/// sees it -- and its Catalyst counterpart belongs on
/// `com.codename1.impl.ios.CatalystWindowNative`, which this port does not ship.
///
/// Windows are addressed by an `int` slot rather than by a pointer, so no native
/// address ever crosses into Java and a stale slot fails as a bounds check
/// instead of as a wild pointer.
///
/// @author Shai Almog
class MacNative {

    /// Creates a window and returns its slot, or a negative number on failure.
    /// `positionSet` distinguishes a window explicitly placed at 0,0 from one
    /// that was never placed; inferred from the coordinates alone the two are
    /// the same, and the window server then puts the second one wherever it
    /// likes.
    /// `ownerSlot` is what makes an owned window behave like one: AppKit keeps a
    /// child window above its owner and takes it along when the owner is
    /// minimized, hidden or closed. -2 asks for the application's main window,
    /// -1 leaves the window unowned, and anything else is another window's slot.
    /// Unowned has to be its own value rather than a default, so a genuinely
    /// independent window is not quietly made a child of the main one.
    native int macWindowCreate(int windowId, String title, int x, int y, int width, int height,
            boolean decorated, boolean resizable, int ownerSlot, boolean positionSet);

    native void macWindowDestroy(int slot);

    native void macWindowShow(int slot, boolean visible);

    native void macWindowSetTitle(int slot, String title);

    native void macWindowSetBounds(int slot, int x, int y, int width, int height);

    /// Fills `out` with x, y, width, height in Codename One pixels.
    native void macWindowGetBounds(int slot, int[] out);

    native boolean macMainWindowGetBounds(int[] out);

    native int macWindowGetWidth(int slot);

    native int macWindowGetHeight(int slot);

    native void macWindowSetResizable(int slot, boolean resizable);

    native void macWindowSetDecorated(int slot, boolean decorated);

    native void macWindowSetMinimumSize(int slot, int width, int height);

    native void macWindowSetAlwaysOnTop(int slot, boolean alwaysOnTop);

    /// Switches the window between an `NSWindow` and an `NSPanel`. AppKit decides
    /// panel behaviour at creation, so this recreates the window; the caller sees
    /// only that the flag took effect.
    native void macWindowSetUtility(int slot, boolean utility);

    native void macWindowMinimize(int slot);

    native void macWindowRestore(int slot);

    native void macWindowToggleMaximize(int slot);

    native void macWindowRequestFocus(int slot);

    native void macWindowSetInputEnabled(int slot, boolean enabled);

    native void macMainWindowSetInputEnabled(boolean enabled);

    /// Begins a modal session for the window, or ends the one it holds.
    native void macWindowSetModal(int slot, boolean modal);

    native boolean macWindowReopen(int slot);

    native void macWindowSetIcon(int slot, int[] argb, int width, int height);

    /// Directs the shared drawing pipeline at this window's Metal layer. Every
    /// operation queued after this and before the matching flush lands there.
    native void macWindowBeginPaint(int slot);

    /// Draws everything queued since `macWindowBeginPaint` and presents it. The
    /// rectangle bounds the region actually repainted.
    native void macWindowFlush(int slot, int x, int y, int width, int height);

    /// Reads the window's last presented frame back into `argb`.
    native boolean macWindowCapture(int slot, int[] argb, int width, int height);

    /// Starts observing screen configuration changes, which arrive back as
    /// `CN1MacWindowDeliverMonitorsChanged`.
    native void macWindowWatchScreens();

    native int macMonitorCount();

    native int macPrimaryMonitor();

    /// Fills `out` with x, y, width, height for the monitor, in Codename One
    /// pixels and in a top-left origin space rather than AppKit's bottom-left
    /// one.
    native void macMonitorBounds(int monitor, boolean workArea, int[] out);

    native int macMonitorDpi(int monitor);

    native int macMonitorScaleTimes100(int monitor);

    native int macMonitorForWindow(int slot);

    native int macMonitorForMainWindow();

    native String macMonitorName(int monitor);
}
