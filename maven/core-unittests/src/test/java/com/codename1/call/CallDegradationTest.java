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
package com.codename1.call;

import com.codename1.call.directory.CallDirectory;
import com.codename1.call.directory.DirectoryEntry;
import com.codename1.call.session.CallConfiguration;
import com.codename1.call.session.Calls;
import com.codename1.call.voip.VoipPush;
import com.codename1.impl.call.CallRequests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * A port that implements nothing.
 *
 * <p>This is the ordinary case for the desktop, and it is the case that has to
 * stay boring: an app that references the call API and runs where there is no
 * call API must get honest answers, not exceptions. Every entry point is
 * exercised here precisely because none of them is interesting.</p>
 */
public class CallDegradationTest {

    @BeforeEach
    public void noBridge() {
        CallRequests.resetForTest(null);
    }

    @AfterEach
    public void clear() {
        CallRequests.resetForTest(null);
    }

    @Test
    public void everyCapabilityQueryAnswersFalselyRatherThanThrowing() {
        assertFalse(Calls.isSupported());
        assertFalse(VoipPush.isSupported());
        assertFalse(CallDirectory.isSupported());
        assertEquals(0, Calls.getCapabilities());
        assertEquals(0, Calls.getGrantedPermissions());
        assertSame(CallAvailability.UNSUPPORTED, Calls.getAvailability());
    }

    @Test
    public void reportingACallFailsWithNotSupported() {
        CallAwait.assertFailedWith(CallError.NOT_SUPPORTED,
                Calls.reportIncoming(CallId.random(),
                        CallHandle.phone("+14155551212"), "Ada", false));
        CallAwait.assertFailedWith(CallError.NOT_SUPPORTED,
                Calls.reportOutgoing(CallId.random(),
                        CallHandle.generic("ada"), "Ada", false));
    }

    @Test
    public void configuringFailsWithNotSupported() {
        CallAwait.assertFailedWith(CallError.NOT_SUPPORTED,
                Calls.configure(new CallConfiguration()));
        CallAwait.assertFailedWith(CallError.NOT_SUPPORTED,
                Calls.requestPermissions(1));
    }

    @Test
    public void audioControlFailsWithNotSupported() {
        CallAwait.assertFailedWith(CallError.NOT_SUPPORTED,
                Calls.setAudioRoute(
                        com.codename1.call.session.CallAudioRoute.SPEAKER));
        CallAwait.assertFailedWith(CallError.NOT_SUPPORTED,
                Calls.showAudioRoutePicker(CallId.random()));
        assertSame(com.codename1.call.session.CallAudioRoute.UNKNOWN,
                Calls.getAudioRoute());
    }

    @Test
    public void voipRegistrationFailsWithNotSupported() {
        CallAwait.assertFailedWith(CallError.NOT_SUPPORTED, VoipPush.register());
        assertNull(VoipPush.getToken());
    }

    @Test
    public void theDirectoryFailsWithNotSupported() {
        CallAwait.assertFailedWith(CallError.NOT_SUPPORTED,
                CallDirectory.setEntries(new DirectoryEntry[]{
                    new DirectoryEntry(14155551212L, "Acme")}));
        CallAwait.assertFailedWith(CallError.NOT_SUPPORTED,
                CallDirectory.reload());
        CallAwait.assertFailedWith(CallError.NOT_SUPPORTED,
                CallDirectory.getStatus());
        CallAwait.assertFailedWith(CallError.NOT_SUPPORTED,
                CallDirectory.requestScreeningRole());
    }

    @Test
    public void listenersAndUnregisteringAreSafeWithNoBridge() {
        // An app tears down in onStop whatever happened at startup, and that
        // teardown must not be the thing that crashes it on a port with no
        // call support.
        Calls.addActionListener(new com.codename1.call.session.CallActionAdapter());
        Calls.removeActionListener(null);
        VoipPush.setListener(null);
        VoipPush.unregister();
        assertNotNull(Calls.getSessions());
        assertEquals(0, Calls.getSessions().length);
    }

    @Test
    public void lookingUpAnUnknownSessionAnswersNull() {
        assertNull(Calls.getSession(CallId.random()));
        assertNull(Calls.getSession("not-an-id"));
        assertNull(Calls.getSession(null));
    }
}
