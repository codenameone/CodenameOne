/*
 * Copyright (c) 2008, 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 */
package com.codename1.io;

import com.codename1.impl.CodenameOneImplementation;

/// Internal bridge exposing the platform implementation to subpackages of
/// `com.codename1.io` (for example `com.codename1.io.wifi`,
/// `com.codename1.io.bonjour`, `com.codename1.io.usb`). Applications should
/// not depend on this class -- it is part of the framework's internal SPI and
/// may change without notice.
public final class IOImpl {
    private IOImpl() {
    }

    /// Returns the current platform implementation. Never `null` after the
    /// framework has been initialised by `Display`.
    public static CodenameOneImplementation impl() {
        return Util.getImplementation();
    }
}
