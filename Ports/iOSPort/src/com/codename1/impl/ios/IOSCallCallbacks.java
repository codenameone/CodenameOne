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
package com.codename1.impl.ios;

import com.codename1.call.directory.CallDirectory;
import com.codename1.call.session.Calls;
import com.codename1.call.voip.VoipPush;
import com.codename1.vpn.profile.Vpn;

/// Static callback surface invoked from `CN1Call` and `CN1Vpn` when CallKit,
/// PushKit or NEVPNManager answer.
///
/// Mirrors [IOSNearbyCallbacks]: the static initializer calls each entry point
/// once, guarded so it has no effect, purely to keep the ParparVM dead-code
/// eliminator from stripping targets that no Java code calls. Without that
/// guard the optimizer replaces them with empty stubs and every operation
/// hangs waiting for an answer that was compiled away -- a failure with
/// nothing in the log to explain it.
///
/// Everything here forwards straight to the public facades, which own EDT
/// dispatch. That matters here specifically: `CXProviderDelegate` runs on a
/// queue of the provider's choosing, `PKPushRegistryDelegate` runs on the main
/// thread, and `NEVPNStatusDidChangeNotification` arrives on whichever thread
/// posted it. Under ParparVM none of those is the Codename One EDT.
final class IOSCallCallbacks {

    private static boolean dceGuard;

    static {
        // Keep the native callback targets reachable for the iOS VM
        // optimizer.
        dceGuard = true;
        ack(0, false, 0, null);
        permissionResult(0, 0);
        answerRequested(null, 0);
        endRequested(null, 0);
        holdRequested(null, false, 0);
        muteRequested(null, false, 0);
        dtmfRequested(null, null, 0);
        startCallRequested(null, null, false, 0);
        audioActivated(null, 0);
        audioDeactivated(null);
        callEnded(null, 0);
        providerReset();
        pushedCall(null, null, null, false, false, false, null, 0);
        pendingCallsDrained(0, 0);
        voipToken(0, null);
        voipRegistrationFailed(0, 0, null);
        directoryStatus(0, null);
        vpnAck(0, false, 0, null);
        vpnProfile(0, null);
        vpnProfileFailed(0, 0, null);
        vpnStatusChanged(-1);
        dceGuard = false;
    }

    private IOSCallCallbacks() {
    }

    /// Creates the call bridge's callback linkage.
    ///
    /// Exists so the bridge's constructor has something to touch: loading this
    /// class runs the guard above, which is what keeps the entry points alive.
    ///
    /// @param nativeInstance the port's native surface
    static void install(IOSNative nativeInstance) {
        // Deliberately empty of behaviour. Referencing the class is the point.
    }

    // ------------------------------------------------------------------
    // calls
    // ------------------------------------------------------------------

    static void ack(int requestId, boolean ok, int errorOrdinal, String message) {
        if (dceGuard) {
            return;
        }
        Calls.deliverAck(requestId, ok, errorOrdinal, message);
    }

    static void permissionResult(int requestId, int grantedMask) {
        if (dceGuard) {
            return;
        }
        Calls.deliverPermissionResult(requestId, grantedMask);
    }

    static void answerRequested(String callId, long actionToken) {
        if (dceGuard) {
            return;
        }
        Calls.deliverAnswer(callId, actionToken);
    }

    static void endRequested(String callId, long actionToken) {
        if (dceGuard) {
            return;
        }
        Calls.deliverEndRequest(callId, actionToken);
    }

    static void holdRequested(String callId, boolean held, long actionToken) {
        if (dceGuard) {
            return;
        }
        Calls.deliverHold(callId, held, actionToken);
    }

    static void muteRequested(String callId, boolean muted, long actionToken) {
        if (dceGuard) {
            return;
        }
        Calls.deliverMute(callId, muted, actionToken);
    }

    static void dtmfRequested(String callId, String digits, long actionToken) {
        if (dceGuard) {
            return;
        }
        Calls.deliverDtmf(callId, digits, actionToken);
    }

    static void startCallRequested(String callId, String handleWire,
            boolean video, long actionToken) {
        if (dceGuard) {
            return;
        }
        Calls.deliverStartCallRequest(callId, handleWire, video, actionToken);
    }

    static void audioActivated(String callId, int routeOrdinal) {
        if (dceGuard) {
            return;
        }
        Calls.deliverAudioActivated(callId, routeOrdinal);
    }

    static void audioDeactivated(String callId) {
        if (dceGuard) {
            return;
        }
        Calls.deliverAudioDeactivated(callId);
    }

    static void callEnded(String callId, int reasonOrdinal) {
        if (dceGuard) {
            return;
        }
        Calls.deliverCallEnded(callId, reasonOrdinal);
    }

    static void providerReset() {
        if (dceGuard) {
            return;
        }
        Calls.deliverProviderReset();
    }

    // ------------------------------------------------------------------
    // VoIP push
    // ------------------------------------------------------------------

    static void pushedCall(String callId, String handleWire, String displayName,
            boolean video, boolean stale, boolean synthesizedId, String data,
            long receivedAt) {
        if (dceGuard) {
            return;
        }
        VoipPush.deliverPushedCall(callId, handleWire, displayName, video,
                stale, synthesizedId, data, receivedAt);
    }

    static void pendingCallsDrained(int requestId, int count) {
        if (dceGuard) {
            return;
        }
        VoipPush.deliverPendingCallsDrained(requestId, count);
    }

    static void voipToken(int requestId, String token) {
        if (dceGuard) {
            return;
        }
        VoipPush.deliverToken(requestId, token);
    }

    static void voipRegistrationFailed(int requestId, int errorOrdinal,
            String message) {
        if (dceGuard) {
            return;
        }
        VoipPush.deliverRegistrationFailed(requestId, errorOrdinal, message);
    }

    // ------------------------------------------------------------------
    // directory
    // ------------------------------------------------------------------

    static void directoryStatus(int requestId, String statusWire) {
        if (dceGuard) {
            return;
        }
        CallDirectory.deliverStatus(requestId, statusWire);
    }

    // ------------------------------------------------------------------
    // VPN
    // ------------------------------------------------------------------

    static void vpnAck(int requestId, boolean ok, int errorOrdinal,
            String message) {
        if (dceGuard) {
            return;
        }
        Vpn.deliverAck(requestId, ok, errorOrdinal, message);
    }

    static void vpnProfile(int requestId, String profileWire) {
        if (dceGuard) {
            return;
        }
        Vpn.deliverProfile(requestId, profileWire);
    }

    static void vpnProfileFailed(int requestId, int errorOrdinal,
            String message) {
        if (dceGuard) {
            return;
        }
        Vpn.deliverProfileFailed(requestId, errorOrdinal, message);
    }

    static void vpnStatusChanged(int statusOrdinal) {
        if (dceGuard) {
            return;
        }
        Vpn.deliverStatusChanged(statusOrdinal);
    }
}
