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

/// A [CallActionListener] whose methods all do nothing, for overriding the
/// few that matter.
///
/// Note that leaving [#providerReset()] as a no-op is a real decision and
/// usually the wrong one -- see the method on the interface.
public class CallActionAdapter implements CallActionListener {

    public void answerRequested(String callId, CallAction action) {
    }

    public void endRequested(String callId, CallAction action) {
    }

    public void holdRequested(String callId, boolean held, CallAction action) {
    }

    public void muteRequested(String callId, boolean muted, CallAction action) {
    }

    public void dtmfRequested(String callId, String digits, CallAction action) {
    }

    public void startCallRequested(String callId, CallHandle handle,
            boolean video, CallAction action) {
    }

    public void audioSessionActivated(CallAudioSession session) {
    }

    public void audioSessionDeactivated(String callId) {
    }

    public void callEnded(String callId, CallEndReason reason) {
    }

    public void providerReset() {
    }
}
