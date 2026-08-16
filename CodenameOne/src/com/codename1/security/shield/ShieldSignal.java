/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.security.shield;

/// One runtime self-protection observation, such as "a hooking framework is loaded".
///
/// Signals are **reports, not verdicts**. The device never decides it is compromised and never
/// terminates itself over one of these; it reports what it saw and the attestation service decides
/// whether to keep issuing tokens. That ordering matters for two reasons: a hard local exit is
/// trivially patched out of the binary, and it destroys the telemetry that would have told the
/// developer an attack was happening at all.
public final class ShieldSignal {

    /// A rooted Android device.
    public static final String ROOT = "root";
    /// A jailbroken iOS device.
    public static final String JAILBREAK = "jailbreak";
    /// A dynamic instrumentation or hooking framework is present.
    public static final String HOOK = "hook";
    /// The app is running on an emulator or simulator.
    public static final String EMULATOR = "emulator";
    /// A debugger is attached to the process.
    public static final String DEBUGGER = "debugger";
    /// The app's signing certificate does not match the one it was built with.
    public static final String REPACKAGED = "repackaged";
    /// An accessibility service that is not on the allow list is enabled.
    public static final String ACCESSIBILITY = "accessibility";
    /// Another application's window was drawn over this app while it was being touched, which is
    /// how a tapjacking attack presents itself. Unlike the signals above this describes a moment
    /// rather than a property of the device: see
    /// [com.codename1.security.DeviceIntegrity#isScreenObscured()].
    public static final String TAPJACK = "tapjack";

    private final String id;
    private final int severity;
    private final String detail;
    private final long timestamp;

    public ShieldSignal(String id, int severity, String detail) {
        this.id = id;
        this.severity = severity < 0 ? 0 : (severity > 100 ? 100 : severity);
        this.detail = detail;
        this.timestamp = System.currentTimeMillis();
    }

    /// A stable identifier such as [#HOOK]. Engines may report ids this build predates.
    public String getId() {
        return id;
    }

    /// How strongly this points at an attack, 0 to 100. Advisory only -- the service applies the
    /// policy, so a low severity here does not mean the service will ignore it.
    public int getSeverity() {
        return severity;
    }

    /// What was actually observed, for example the offending package or library name. May be null.
    public String getDetail() {
        return detail;
    }

    /// When the observation was made.
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return id + "(" + severity + (detail == null ? "" : ", " + detail) + ")";
    }
}
