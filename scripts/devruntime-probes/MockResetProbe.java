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
import com.codename1.social.*;

/**
 * Whether a second pushed program inherits the first one's login.
 *
 * A provider is a framework singleton, so the mock survives the program that
 * used it. Push MockProbe (which logs in) and then this: a fresh program must
 * start logged out.
 */
public class MockResetProbe {
    public static void main(String[] a) {
        FacebookConnect fb = FacebookConnect.getInstance();
        System.out.println("PROBE MockResetProbe: instance=" + fb.getClass().getName());
        System.out.println("PROBE MockResetProbe: token="
                + (fb.getAccessToken() == null ? "none" : "carried over"));
        System.out.println("PROBE MockResetProbe: loggedIn=" + fb.isUserLoggedIn());
        new Form("Reset").show();
    }
}
