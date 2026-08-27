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
package com.codename1.call.session;

import com.codename1.call.CallEndReason;
import com.codename1.call.CallHandle;

/// What the system asks of a call, and what it tells the app about one.
///
/// Every method arrives on the EDT. [CallActionAdapter] implements them all
/// as no-ops, so an app can override only what it needs.
///
/// The `...Requested` methods are the user acting through the system's own
/// UI -- the lock screen, the car, a headset button -- rather than through
/// this app's. Each carries a [CallAction]; ignoring it fulfills it, which is
/// the right behaviour for almost every app. See [CallAction] for the case
/// where it is not.
public interface CallActionListener {

    /// The user answered. Start media when [#audioSessionActivated] arrives,
    /// not here.
    void answerRequested(String callId, CallAction action);

    /// The user hung up.
    void endRequested(String callId, CallAction action);

    /// The user held or resumed the call, or the system did in order to give
    /// the audio to something else.
    void holdRequested(String callId, boolean held, CallAction action);

    /// The user muted or unmuted the call.
    void muteRequested(String callId, boolean muted, CallAction action);

    /// The user typed on the system keypad.
    void dtmfRequested(String callId, String digits, CallAction action);

    /// The system is asking this app to **place** a call: the user tapped an
    /// entry in Recents, or asked a voice assistant to call somebody through
    /// this app. No call exists yet -- report one with
    /// [Calls#reportOutgoing] using the id carried here.
    ///
    /// Unlike every other action here, an unanswered request is **failed**
    /// rather than fulfilled, and the system call ends. Nothing else can
    /// place the call, so reporting success for a request the app ignored
    /// would leave it dialing for ever. An app that does not place calls on
    /// the system's behalf can leave this alone; one that does must either
    /// report the call or answer the action itself.
    void startCallRequested(String callId, CallHandle handle, boolean video,
            CallAction action);

    /// The operating system has activated the audio session. **This is where
    /// media starts.** See [CallAudioSession] for why it is not where the
    /// call is answered.
    void audioSessionActivated(CallAudioSession session);

    /// The operating system has taken the audio back. Stop media.
    void audioSessionDeactivated(String callId);

    /// The call ended for a reason that did not come from this app -- the far
    /// end hung up, or the system ended it.
    void callEnded(String callId, CallEndReason reason);

    /// **Every call this app had is gone.**
    ///
    /// The system's call provider was reset, which happens when the user
    /// switches the app's calling off or the platform recovers from an error.
    /// Tear down media and forget every session, but do **not** call
    /// [CallSession#end] on them: they no longer exist as far as the system
    /// is concerned, and ending a call on a reset provider is undefined.
    ///
    /// An app that does not implement this leaks its media engine and shows
    /// calls that are not there.
    void providerReset();
}
