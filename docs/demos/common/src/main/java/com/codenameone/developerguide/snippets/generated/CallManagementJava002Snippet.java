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

class CallManagementJava002Snippet {

    String theirNumber = "+14155551212";
    Signalling signalling = new Signalling();

    static class Signalling {
        void invite(String id, String number) { }
    }

    void snippet() throws Exception {
        // tag::call-management-java-002[]
        String callId = CallId.random();          // the id both ends will use
        Calls.reportIncoming(callId, CallHandle.phone(theirNumber), "Ada Lovelace", false)
                .onResult((session, err) -> {
                    if (err != null) {
                        // The system refused to ring: an emergency call, another
                        // app's call, or Do Not Disturb. Tell the caller.
                        return;
                    }
                    signalling.invite(callId, theirNumber);
                });
        // end::call-management-java-002[]
    }
}
