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
import com.codename1.ui.*;
import com.codename1.io.*;

/**
 * A pushed program doing real networking, against a host-side endpoint reached
 * over `adb reverse` -- the emulator has no DNS, and a hermetic endpoint is a
 * better test anyway.
 *
 * The interesting part is not the request: it is that ConnectionRequest is a
 * framework class being subclassed by interpreted code, so readResponse is an
 * interpreted override the framework calls back into on its own network thread.
 */
public class NetProbe {
    public static void main(String[] a) {
        final StringBuilder r = new StringBuilder();
        ConnectionRequest cr = new ConnectionRequest() {
            protected void readResponse(java.io.InputStream in) throws java.io.IOException {
                r.append("body=").append(Util.readToString(in).trim());
            }
            protected void handleErrorResponseCode(int code, String message) {
                r.append("http ").append(code);
            }
        };
        cr.setUrl("http://127.0.0.1:18080/hello.txt");
        cr.setPost(false);
        NetworkManager.getInstance().addToQueueAndWait(cr);
        System.out.println("PROBE NetProbe: " + (r.length() == 0 ? "no callback" : r.toString())
            + " status=" + cr.getResponseCode());
        new Form("Net").show();
    }
}
