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

class CallManagementJava004Snippet {

    Signalling signalling = new Signalling();
    History history = new History();

    static class Signalling {
        void attach(String id, String data) { }
        void registerVoip(String token) { }
    }

    static class History {
        void addMissed(CallHandle h) { }
    }

    void snippet() throws Exception {
        // tag::call-management-java-004[]
        VoipPush.setListener(new VoipPushListener() {
            public void callReceived(PushedCall call) {
                if (call.isStale()) {
                    history.addMissed(call.getHandle());   // it is already over
                    return;
                }
                // The phone is ALREADY ringing. Attach media and wait for the
                // ordinary answerRequested; do not report the call again.
                signalling.attach(call.getSession().getCallId(), call.getData());
            }

            public void tokenChanged(String token) {
                signalling.registerVoip(token);
            }
        });
        VoipPush.register();
        // end::call-management-java-004[]
    }
}
