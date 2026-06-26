/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.impl.ios;

import java.io.File;

/**
 * The iOS implementation running on a desktop JVM under the simulator: the
 * Java side of the port executes as regular bytecode while the native methods
 * resolve into libcn1sim.dylib (the port's Metal rendering pipeline plus the
 * cn1jni compatibility runtime) through generated JNI shims.
 *
 * <p>This subclass adapts the device implementation to the desktop host:
 * lifecycle glue (the device app-delegate flow is driven by the simulator
 * backend instead), a sandboxed file system root, and overrides for features
 * that are stubbed in the desktop library.</p>
 */
public class IOSDesktopImplementation extends IOSImplementation {
    private static final Runnable NOOP = new Runnable() {
        public void run() {
        }
    };

    @Override
    public void init(Object m) {
        // the device flow casts m to Runnable (the app delegate passes the
        // lifecycle object); under the simulator m may be anything
        super.init(m instanceof Runnable ? m : NOOP);
    }

    /**
     * Unblocks initEDT - the simulator backend calls this once the rendering
     * surface is attached, mirroring the device app delegate's callback.
     */
    public static void fireNativeReady() {
        IOSImplementation.callback();
    }

    @Override
    public boolean isNativeBrowserComponentSupported() {
        return false;
    }

    @Override
    public boolean hasCamera() {
        return false;
    }

    /**
     * The desktop library routes the iOS documents/caches directories to a
     * sandbox under the user's home rather than the real ~/Documents.
     */
    @Override
    public String getAppHomePath() {
        String home = System.getProperty("cn1.sim.home");
        if (home != null) {
            File f = new File(home);
            f.mkdirs();
            return "file://" + f.getAbsolutePath() + "/";
        }
        return super.getAppHomePath();
    }
}
