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

import java.util.Map;

/// Platform-supplied implementation of the Bonjour / mDNS APIs.
/// Application code talks to `BonjourBrowser` / `BonjourPublisher`; the
/// facade fetches this via `Display.getInstance().getBonjourPlatform()`.
///
/// Part of the framework's service-provider interface, not intended for
/// application use.
public class BonjourPlatform {
    public boolean isSupported() {
        return false;
    }

    public Object startBrowse(String type, BonjourServiceListener listener) {
        if (listener != null) {
            listener.onBrowseError(new UnsupportedOperationException(
                    "Bonjour is not supported on this platform"));
        }
        return null;
    }

    public void stopBrowse(Object handle) {
    }

    public Object startPublish(String name, String type, int port,
                               Map<String, String> txt) {
        return null;
    }

    public void stopPublish(Object handle) {
    }
}
