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

import com.codename1.io.IOImpl;

import java.util.Hashtable;
import java.util.Map;

/// Advertises a Bonjour / mDNS service on the local network.
///
/// A publisher registers a service of a given type and port; once registered
/// any client on the same network running `BonjourBrowser.browse(type, ...)`
/// will see it. `txt` records carry small key/value metadata (typical limit
/// 255 bytes per value).
///
/// #### Example -- advertise an HTTP service on port 8080
///
/// ```java
/// BonjourPublisher pub = BonjourPublisher.publish(
///         "MyServer", "_http._tcp.", 8080, null);
///
/// // when shutting down
/// pub.unpublish();
/// ```
///
/// On iOS the build pipeline appends `type` to `NSBonjourServices` in
/// Info.plist automatically when this class is referenced; without that entry
/// iOS 14+ silently rejects the publish.
public final class BonjourPublisher {
    private final Object nativeHandle;
    private final String name;
    private final String type;
    private final int port;
    private boolean unpublished;

    private BonjourPublisher(String name, String type, int port,
                             Object nativeHandle) {
        this.name = name;
        this.type = type;
        this.port = port;
        this.nativeHandle = nativeHandle;
    }

    /// Publishes a new service. `name` is shown to humans browsing services
    /// and must be unique on the local subnet; the OS may append a suffix to
    /// resolve collisions. `type` is the mDNS type (e.g. `_http._tcp.`).
    /// `port` is the listening port on this device. `txt` may be `null` or a
    /// String->String map of metadata.
    public static BonjourPublisher publish(String name, String type, int port,
                                           Map<String, String> txt) {
        if (txt == null) {
            txt = new Hashtable<String, String>();
        }
        Object handle = IOImpl.impl()
                .startBonjourPublish(name, type, port, txt);
        return new BonjourPublisher(name, type, port, handle);
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public int getPort() {
        return port;
    }

    /// Removes the advertisement. Idempotent.
    public void unpublish() {
        if (unpublished) {
            return;
        }
        unpublished = true;
        IOImpl.impl().stopBonjourPublish(nativeHandle);
    }
}
