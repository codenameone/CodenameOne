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
package com.codename1.impl.call;

import com.codename1.call.spi.CallBridge;
import com.codename1.impl.async.EdtResult;
import com.codename1.impl.async.PendingMap;
import com.codename1.ui.Display;

import java.util.concurrent.atomic.AtomicInteger;

/// The bits every `com.codename1.call` facade needs and none of them owns:
/// the bridge lookup, one request-id counter for the whole family, and the
/// maps that pair a native answer with the caller waiting for it.
///
/// @hidden not part of the public API.
public final class CallRequests {

    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private static CallBridge testBridge;

    /// Operations whose answer is just "it worked" or an exception:
    /// reporting a call, ending one, holding, muting, sending digits.
    private static final PendingMap<Boolean> ACKS = new PendingMap<Boolean>();

    /// Permission requests in flight, from every facade.
    ///
    /// One map rather than one per facade, because there is one answer path:
    /// a port reports the outcome through
    /// `com.codename1.call.session.Calls#deliverPermissionResult` whichever
    /// entry point asked. A per-facade map would leave a request opened by
    /// the directory facade unfindable, and the caller would hold a resource
    /// that never settled -- the failure the SPI documentation calls worse
    /// than an outright error.
    ///
    /// Safe to share because request ids come from one counter, so an id is
    /// in at most one map and an answer cannot be matched to the wrong
    /// operation.
    private static final PendingMap<Integer> PERMISSIONS =
            new PendingMap<Integer>();

    /// Operations answering with a string: the VoIP push token, a
    /// directory status record.
    private static final PendingMap<String> STRINGS = new PendingMap<String>();

    /// Operations answering with a count -- currently only the pending-call
    /// drain.
    private static final PendingMap<Integer> COUNTS = new PendingMap<Integer>();

    private CallRequests() {
    }

    /// The active port's bridge, or `null` where no port implements one.
    ///
    /// Guarded on `Display.isInitialized()` rather than on the instance
    /// being non-null: `Display.getInstance()` hands back its singleton long
    /// before `Display.init` has given it an implementation, and asking that
    /// for a bridge throws. A unit test and an app that touches a facade
    /// from a static initializer both reach this that way.
    ///
    /// #### Returns
    ///
    /// the bridge, or null
    public static synchronized CallBridge bridge() {
        if (testBridge != null) {
            return testBridge;
        }
        if (!Display.isInitialized()) {
            return null;
        }
        return Display.getInstance().getCallBridge();
    }

    /// Installs a bridge and clears every facade's static state, so one test
    /// cannot see the calls, listeners or in-flight requests of the test
    /// that ran before it.
    ///
    /// The facades are static -- there is no instance for a test to throw
    /// away -- which makes shared state order-dependent: a listener a
    /// previous test forgot to remove fires during this one, and the failure
    /// looks like a bug in whichever test happened to run second.
    ///
    /// Passing `null` gives a bridgeless framework without waiting for
    /// `Display` to be absent, which is what the degradation tests need.
    ///
    /// @hidden not part of the public API; test-only.
    ///
    /// #### Parameters
    ///
    /// - `bridge`: the bridge to install, or null for none
    public static void resetForTest(CallBridge bridge) {
        CallBridge previous;
        synchronized (CallRequests.class) {
            previous = testBridge;
            testBridge = bridge;
        }
        // The simulation schedules its answers through Display.setTimeout,
        // which hands back nothing to cancel -- so a bridge left behind by a
        // finished test goes on delivering into the next one. Told to stop
        // instead. Tested with instanceof rather than cast for the reason
        // check-cast-semantics.sh gives.
        if (previous instanceof LocalCallBridge) {
            ((LocalCallBridge) previous).retire();
        }
        ACKS.failAll(new IllegalStateException("reset"));
        PERMISSIONS.failAll(new IllegalStateException("reset"));
        STRINGS.failAll(new IllegalStateException("reset"));
        COUNTS.failAll(new IllegalStateException("reset"));
        com.codename1.call.session.Calls.resetForTest();
        com.codename1.call.voip.VoipPush.resetForTest();
        com.codename1.call.directory.CallDirectory.resetForTest();
    }

    /// The next request id.
    ///
    /// Ids come from one counter shared by every facade so that an id lives
    /// in exactly one map and an answer can never be matched against the
    /// wrong operation.
    ///
    /// #### Returns
    ///
    /// a request id no other in-flight operation is using
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

    /// Registers a permission request.
    public static EdtResult<Integer> openPermissionRequest(int requestId) {
        return PERMISSIONS.open(requestId);
    }

    /// Claims a permission request, or null when nothing waits on it.
    public static EdtResult<Integer> takePermissionRequest(int requestId) {
        return PERMISSIONS.take(requestId);
    }

    /// Registers a request answering with a string.
    public static EdtResult<String> openString(int requestId) {
        return STRINGS.open(requestId);
    }

    /// Claims a string request, or null when nothing waits on it.
    public static EdtResult<String> takeString(int requestId) {
        return STRINGS.take(requestId);
    }

    /// Registers a request answering with a count.
    public static EdtResult<Integer> openCount(int requestId) {
        return COUNTS.open(requestId);
    }

    /// Claims a count request, or null when nothing waits on it.
    public static EdtResult<Integer> takeCount(int requestId) {
        return COUNTS.take(requestId);
    }

    /// Fails everything in flight, for a port that has lost its provider.
    ///
    /// #### Parameters
    ///
    /// - `failure`: what to fail them with
    public static void failAll(Throwable failure) {
        ACKS.failAll(failure);
        PERMISSIONS.failAll(failure);
        STRINGS.failAll(failure);
        COUNTS.failAll(failure);
    }
}
