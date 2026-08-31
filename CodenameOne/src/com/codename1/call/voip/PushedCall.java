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
package com.codename1.call.voip;

import com.codename1.call.CallHandle;
import com.codename1.call.session.CallSession;

/// A call that **is already ringing** when the app finds out about it.
///
/// This is the part of VoIP that surprises people. On iOS a call arriving as a
/// push must be shown to the user before any of this app's code runs -- the
/// operating system requires it and kills the app otherwise -- so by the time
/// a [VoipPushListener] is told, the phone is ringing, the lock screen shows
/// the caller, and the user may already have answered.
///
/// The app's job is therefore not to decide whether to ring. It is to attach
/// media to a call that exists, and then wait for
/// `com.codename1.call.session.CallActionListener#answerRequested` like any
/// other call. Android does not have the same constraint, but the port
/// synthesizes the same shape so that application code has one path.
public final class PushedCall {
    private final CallSession session;
    private final String data;
    private final boolean stale;
    private final boolean synthesizedId;
    private final long receivedAt;

    PushedCall(CallSession session, String data, boolean stale,
            boolean synthesizedId, long receivedAt) {
        this.session = session;
        this.data = data;
        this.stale = stale;
        this.synthesizedId = synthesizedId;
        this.receivedAt = receivedAt;
    }

    /// The call, already reported to the system and usually ringing.
    public CallSession getSession() {
        return session;
    }

    /// Who is calling, as the push said.
    public CallHandle getHandle() {
        return session.getHandle();
    }

    /// The opaque `data` field from the push payload, or null.
    ///
    /// The framework never parses this. It exists because the deadline-bound
    /// native path cannot run app code, so anything the app needs -- a room
    /// id, a session token -- has to travel through and be read later.
    public String getData() {
        return data;
    }

    /// **The call is over and cannot be answered.**
    ///
    /// True for a call that rang while the app was not running and was
    /// finished -- unanswered, cancelled, or killed with the process --
    /// before the app got here. Log it as a missed call and show it in
    /// history; do not attach media, and do not expect an answer action.
    public boolean isStale() {
        return stale;
    }

    /// Whether the identifier was invented locally because the push payload
    /// carried none or carried a malformed one.
    ///
    /// The call was still rung -- refusing would have killed the app -- but
    /// the identifier will not match the sender's, so signalling keyed on it
    /// will not line up. This flag exists so that a server-side bug is
    /// findable instead of presenting as calls that mysteriously never
    /// connect.
    public boolean isIdentifierSynthesized() {
        return synthesizedId;
    }

    /// When the push arrived, in wall-clock milliseconds.
    public long getReceivedAt() {
        return receivedAt;
    }
}
