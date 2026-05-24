/*
 * Copyright (c) 2008, 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 */
package com.codename1.io.bonjour;

import com.codename1.ui.Display;

/// Browses the local network for Bonjour / mDNS services.
///
/// Bonjour ("zeroconf" / "mDNS-SD") lets clients find network services -- a
/// printer, a music server, another instance of your app -- without knowing
/// their IP address in advance. Services are advertised under a *type* such as
/// `_http._tcp.` or your own private `_myapp._tcp.`. A `BonjourBrowser` watches
/// for services of one type and notifies the listener whenever a service comes
/// online or goes away.
///
/// #### Platform support
///
/// - **Android**: `android.net.nsd.NsdManager`.
/// - **iOS**: `NSNetServiceBrowser` + `NSNetService`. The build pipeline
///   injects `NSLocalNetworkUsageDescription` and the service type into
///   `NSBonjourServices` so iOS 14+ doesn't block discovery.
/// - **Simulator**: JmDNS is used when present on the classpath; otherwise
///   discovery is a no-op and the listener is told the platform is
///   unsupported.
///
/// #### Example
///
/// ```java
/// BonjourBrowser browser = BonjourBrowser.browse("_http._tcp.", new BonjourServiceListener() {
///     public void onServiceResolved(BonjourService s) {
///         Log.p("Found " + s.getName() + " at " + s.getHost() + ":" + s.getPort());
///     }
///     public void onServiceLost(BonjourService s) { /* ... */ }
///     public void onBrowseError(Throwable t) { Log.e(t); }
/// });
///
/// // when finished
/// browser.stop();
/// ```
public final class BonjourBrowser {
    private final Object nativeHandle;
    private final String type;
    private boolean stopped;

    private BonjourBrowser(String type, Object nativeHandle) {
        this.type = type;
        this.nativeHandle = nativeHandle;
    }

    /// Starts browsing for `type` and returns a handle to stop the search.
    /// `type` must be in mDNS form, e.g. `_http._tcp.` (trailing dot
    /// optional). `listener` is invoked on the EDT.
    public static BonjourBrowser browse(String type,
                                        BonjourServiceListener listener) {
        Object handle = Display.getInstance().getBonjourPlatform()
                .startBrowse(type, listener);
        return new BonjourBrowser(type, handle);
    }

    /// `true` if the platform implements Bonjour at all.
    public static boolean isSupported() {
        return Display.getInstance().getBonjourPlatform().isSupported();
    }

    /// The service type passed to `browse(...)`.
    public String getType() {
        return type;
    }

    /// Stops this browser. Idempotent.
    public void stop() {
        if (stopped) {
            return;
        }
        stopped = true;
        Display.getInstance().getBonjourPlatform().stopBrowse(nativeHandle);
    }
}
