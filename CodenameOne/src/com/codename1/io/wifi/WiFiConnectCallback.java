/*
 * Copyright (c) 2008, 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 */
package com.codename1.io.wifi;

/// Callback for `WiFi.connect(...)`. Invoked once on the EDT.
public interface WiFiConnectCallback {
    /// `connected` is `true` if the association succeeded. `error` is `null`
    /// on success and holds the rejection cause otherwise (cancelled by user,
    /// wrong password, unreachable AP, ...).
    void onConnectResult(boolean connected, Throwable error);
}
