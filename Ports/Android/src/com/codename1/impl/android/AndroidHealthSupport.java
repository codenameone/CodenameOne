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
package com.codename1.impl.android;

/// Registry that links the Android port to the injected Health Connect
/// bridge.
///
/// The runtime Android port carries no compile-time dependency on
/// `androidx.health.connect` -- it is only on the classpath when the app
/// actually uses `com.codename1.health`. The build injects a Kotlin
/// implementation of [HealthConnectDelegate] into the generated project and
/// registers it here during app startup; [AndroidHealth] reads it back.
///
/// When health is not bundled this stays null and the health API degrades
/// to reporting itself unsupported, exactly as
/// [AndroidCarSupport] does when Android Auto is absent.
public final class AndroidHealthSupport {

    private static volatile HealthConnectDelegate delegate;

    private AndroidHealthSupport() {
    }

    /// Called by the injected bridge to publish itself at app startup.
    public static void setDelegate(HealthConnectDelegate d) {
        delegate = d;
    }

    /// The registered bridge, or null when Health Connect is not bundled.
    public static HealthConnectDelegate getDelegate() {
        return delegate;
    }
}
