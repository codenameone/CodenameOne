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

class CallManagementJava003Snippet {

    Signalling signalling = new Signalling();
    MediaEngine media = new MediaEngine();

    static class Signalling {
        void place(String id, String number) { }
    }

    static class MediaEngine {
        void start(String id) { }
    }

    void snippet() throws Exception {
        // tag::call-management-java-003[]
        String callId = CallId.random();
        Calls.reportOutgoing(callId, CallHandle.phone("+14155551212"), "Ada", false)
                .onResult((session, err) -> {
                    if (err != null) {
                        return;
                    }
                    session.reportStartedConnecting();     // we are ringing them
                    signalling.place(callId, "+14155551212");
                    // ... and once the far end picks up:
                    session.reportConnected();             // starts the duration
                });
        // end::call-management-java-003[]
    }
}
