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

import com.codename1.impl.call.CallRequests;

/// Something the **system** is asking the app to do to a call: the user
/// pressed answer on the lock screen, hung up from the car, or tapped the
/// keypad.
///
/// #### Why this is an object and not just a callback
///
/// Both platforms require the app to say whether it managed to do what was
/// asked, and to say so within a few seconds. On iOS an unanswered
/// `CXAction` times out and the system call UI and the app then disagree
/// about the state of the call, permanently and with nothing in the log.
///
/// Most apps should ignore all of this: **doing nothing is correct**. If the
/// listener returns without touching the action, it is fulfilled
/// automatically. Only an app that has to do slow asynchronous work before it
/// knows whether it can answer -- renegotiating a session, say -- needs to
/// call [#defer()] and then [#fulfill()] or [#fail()] itself.
///
/// An action that is deferred and then forgotten is failed by a safety timer
/// rather than left to time out, because a failed action puts the system UI
/// back in a state the user can act on, and a timed-out one does not.
public final class CallAction {
    private final long token;
    private final String callId;
    private boolean deferred;
    private boolean answered;

    CallAction(long token, String callId) {
        this.token = token;
        this.callId = callId;
    }

    /// The call this action is about.
    public String getCallId() {
        return callId;
    }

    /// Takes responsibility for answering this action later.
    ///
    /// After calling this the listener **must** call [#fulfill()] or
    /// [#fail()], and should do it within about three seconds. A deferred
    /// action that is never answered is failed automatically.
    public void defer() {
        deferred = true;
    }

    /// Reports that the app did what was asked.
    public void fulfill() {
        answer(true);
    }

    /// Reports that the app could not do what was asked, putting the system
    /// UI back into a state the user can act on.
    public void fail() {
        answer(false);
    }

    /// Whether the listener took responsibility for answering.
    boolean isDeferred() {
        return deferred;
    }

    /// Whether this has already been answered.
    boolean isAnswered() {
        return answered;
    }

    /// Answers unless the application already has.
    ///
    /// The port ignores a second answer for the same token, so the race
    /// between the safety net and a slow application is harmless; this flag
    /// only keeps the common case off the bridge.
    void answer(boolean fulfilled) {
        synchronized (this) {
            if (answered) {
                return;
            }
            answered = true;
        }
        com.codename1.call.spi.CallBridge b = CallRequests.bridge();
        if (b != null) {
            b.completeAction(token, fulfilled);
        }
    }
}
