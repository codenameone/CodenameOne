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
package com.codename1.impl.vpn;

import com.codename1.impl.async.EdtResult;
import com.codename1.impl.async.PendingMap;
import com.codename1.ui.Display;
import com.codename1.vpn.spi.VpnBridge;

import java.util.concurrent.atomic.AtomicInteger;

/// The bridge lookup, the request-id counter and the pending maps for the
/// `com.codename1.vpn` family.
///
/// @hidden not part of the public API.
public final class VpnRequests {

    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private static VpnBridge testBridge;

    private static final PendingMap<Boolean> ACKS = new PendingMap<Boolean>();

    private static final PendingMap<String> STRINGS = new PendingMap<String>();

    private VpnRequests() {
    }

    /// The active port's bridge, or `null` where no port implements one.
    ///
    /// Guarded on `Display.isInitialized()` for the reason
    /// `com.codename1.impl.call.CallRequests#bridge()` gives.
    public static synchronized VpnBridge bridge() {
        if (testBridge != null) {
            return testBridge;
        }
        if (!Display.isInitialized()) {
            return null;
        }
        return Display.getInstance().getVpnBridge();
    }

    /// Installs a bridge and clears the facade's static state.
    ///
    /// @hidden not part of the public API; test-only.
    public static void resetForTest(VpnBridge bridge) {
        synchronized (VpnRequests.class) {
            testBridge = bridge;
        }
        ACKS.failAll(new IllegalStateException("reset"));
        STRINGS.failAll(new IllegalStateException("reset"));
        com.codename1.vpn.profile.Vpn.resetForTest();
    }

    /// The next request id, from one counter shared by the family.
    public static int nextId() {
        return NEXT_ID.getAndIncrement();
    }

    /// Registers an acknowledgement request.
    public static EdtResult<Boolean> openAck(int requestId) {
        return ACKS.open(requestId);
    }

    /// Claims an acknowledgement request, or null when nothing waits on it.
    public static EdtResult<Boolean> takeAck(int requestId) {
        return ACKS.take(requestId);
    }

    /// Registers a request answering with a string.
    public static EdtResult<String> openString(int requestId) {
        return STRINGS.open(requestId);
    }

    /// Claims a string request, or null when nothing waits on it.
    public static EdtResult<String> takeString(int requestId) {
        return STRINGS.take(requestId);
    }
}
