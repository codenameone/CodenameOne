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
package com.codenameone.developerguide.snippets.generated;

import com.codename1.ui.*;
import com.codename1.io.*;
import com.codename1.util.*;
import java.io.*;
import java.util.*;
import com.codename1.call.*;
import com.codename1.call.session.*;
import com.codename1.call.voip.*;
import com.codename1.call.directory.*;

class CallManagementJava001Snippet {

    Signalling signalling = new Signalling();
    MediaEngine media = new MediaEngine();

    static class Signalling {
        void accept(String id) { }
        void hangUp(String id) { }
    }

    static class MediaEngine {
        void start(String id) { }
        void stop() { }
        void stopEverything() { }
        void setMuted(boolean b) { }
    }

    void snippet() throws Exception {
        // tag::call-management-java-001[]
        if (!Calls.isSupported()) {
            return;                        // fall back to an in-app call screen
        }
        Calls.configure(new CallConfiguration().displayName("Acme Talk"));

        Calls.addActionListener(new CallActionAdapter() {
            public void answerRequested(String callId, CallAction action) {
                signalling.accept(callId);
            }

            public void endRequested(String callId, CallAction action) {
                signalling.hangUp(callId);
                media.stop();
            }

            public void audioSessionActivated(CallAudioSession session) {
                media.start(session.getCallId());   // here, NOT in answerRequested
            }

            public void audioSessionDeactivated(String callId) {
                media.stop();
            }

            public void providerReset() {
                media.stopEverything();             // every call is gone
            }
        });
        // end::call-management-java-001[]
    }
}
