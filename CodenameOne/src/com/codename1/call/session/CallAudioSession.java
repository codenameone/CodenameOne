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

/// The moment the operating system hands a call its audio, and the moment it
/// takes it back.
///
/// This exists as a type rather than as a bare callback because **when** the
/// audio arrives is the part of a call implementation that is easiest to get
/// wrong and hardest to notice. On iOS the app configures the audio session's
/// category before reporting a call but must **not** activate it; CallKit
/// activates it and only then may media start. An app that starts its audio
/// engine when it answers the call instead of when this arrives gets a call
/// with no sound and no error message.
///
/// Android has no equivalent handoff, so the Android port and the simulation
/// synthesize this the moment the connection becomes active. Application code
/// therefore has one place to start media on every platform.
public final class CallAudioSession {
    private final String callId;
    private final CallAudioRoute route;

    CallAudioSession(String callId, CallAudioRoute route) {
        this.callId = callId;
        this.route = route == null ? CallAudioRoute.UNKNOWN : route;
    }

    /// The call this audio belongs to.
    public String getCallId() {
        return callId;
    }

    /// Where the audio is going as of this moment. It can change afterwards;
    /// [Calls#getAudioRoute()] is the live answer.
    public CallAudioRoute getRoute() {
        return route;
    }
}
