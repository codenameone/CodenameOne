/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
///
/// #### Since 8.0
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
