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
import com.codename1.vpn.*;
import com.codename1.vpn.profile.*;

class VpnJava002Snippet {

    Ui ui = new Ui();

    static class Ui {
        void showConnecting() { }
        void showConnected() { }
        void showOffline() { }
    }

    void snippet() throws Exception {
        // tag::vpn-java-002[]
        Vpn.addStatusListener(status -> {
            switch (status) {
                case CONNECTING:
                    ui.showConnecting();
                    break;
                case CONNECTED:
                    ui.showConnected();
                    break;
                default:
                    ui.showOffline();        // it can drop without being asked
                    break;
            }
        });
        // end::vpn-java-002[]
    }
}
