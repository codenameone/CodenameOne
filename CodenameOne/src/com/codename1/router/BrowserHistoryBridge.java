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
package com.codename1.router;

/// Hook for syncing `Router` navigation with the host's URL bar / history stack.
///
/// On the JavaScript port a small JS-side shim translates `window.history`
/// `pushState` / `replaceState` / `popstate` events into router operations and
/// vice versa. App code installs the bridge through
/// `Router.getInstance().setBrowserHistoryBridge(bridge)`; once installed, every
/// router push/pop/replace pushes a matching history entry, and browser-back
/// pops the router stack.
///
/// On native ports this interface is a no-op extension point. iOS and Android
/// don't have a browser address bar -- but a future SceneKit-style URL routing
/// could plug in here without changes to the rest of the router.
///
/// Implementations must be thread-safe; the router calls them on the EDT.
public interface BrowserHistoryBridge {

    /// Called when the Router pushes a new entry. The bridge should add a
    /// corresponding entry to the host history stack.
    void onPush(Location loc);

    /// Called when the Router replaces the top entry. The bridge should swap
    /// the top of the host history stack rather than appending.
    void onReplace(Location loc);

    /// Called when the Router pops. `current` is the new top.
    void onPop(Location current);

    /// Returns the initial path to start the router at, sourced from the host
    /// (e.g., `window.location.pathname + search + hash` on JS). Return `null`
    /// to let the caller pick its own starting path.
    String getInitialPath();
}
