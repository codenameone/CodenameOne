/*
 * Copyright (c) 2018, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.demos.signin;

import com.codename1.testing.AbstractTest;
import com.codename1.ui.Display;

/**
 * This essentially tests that Firebase doesn't crash with an error.  This test project
 * includes a GoogleService-Info.plist file inside the native/ios directory that is required
 * by firebase in order to work.  If, for some reason, the build server doesn't include this
 * file in the bundle, then firebase will fail with an error like:
 * <Notice>: *** Terminating app due to uncaught exception 'com.firebase.core', reason: '`[FIRApp configure];` (`FirebaseApp.configure()` in Swift) could not find a valid GoogleService-Info.plist in your project. Please download one from https://console.firebase.google.com/.'
 */
public class TestFirebase extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        if ("ios".equals(Display.getInstance().getPlatformName()) && !Display.getInstance().isSimulator()) {
            SignIn.doFirebase();
        }
        return true;
    }
    
}
