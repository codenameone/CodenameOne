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

/// Security mode advertised by an access point. Used both for scan results
/// and when calling `WiFi.connect(...)`. The value passed to `connect` must
/// match the AP's actual security mode.
public enum WiFiSecurity {
    /// Open network, no encryption.
    OPEN,

    /// Legacy WEP. Considered broken; some platforms refuse to connect at all.
    WEP,

    /// WPA / WPA2 personal (PSK).
    WPA_PSK,

    /// WPA3 personal (SAE).
    WPA3_SAE,

    /// Enterprise EAP (RADIUS-backed). Not directly supported by `connect`;
    /// applications must supply an enterprise configuration through platform
    /// hooks if they need it.
    EAP,

    /// Unknown or platform did not report a security mode.
    UNKNOWN
}
