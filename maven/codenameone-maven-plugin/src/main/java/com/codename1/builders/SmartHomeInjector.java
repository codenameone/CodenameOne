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
package com.codename1.builders;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Pushes the Android half of {@code com.codename1.home} into an app build.
 *
 * <p>The Codename One Android port compiles against a fixed, old
 * {@code android.jar} with no Play services on it, so it cannot reference the
 * Matter commissioning API at all. What it can do is declare an interface --
 * {@code com.codename1.impl.android.SmartHomeDelegate} -- and read back an
 * implementation somebody else registered. This class is the somebody else:
 * it copies that implementation into the app's own source tree, where the
 * app's Gradle compiles it at a modern {@code compileSdkVersion} against the
 * Play services dependency the catalog added.</p>
 *
 * <p>The same trick as {@code MapsProviderInjector} and the Health Connect
 * bridge, and for the same reason: the port has to stay free of an SDK that
 * only some apps want.</p>
 *
 * <p>It is inert unless the class scanner saw {@code com.codename1.home}, so
 * a build that never mentioned smart home is byte-for-byte unaffected.</p>
 */
public final class SmartHomeInjector {

    /**
     * Woven into the generated activity's {@code onCreate}, next to the other
     * startup registrations.
     *
     * <p>Registration has to happen before any application code runs, because
     * an app that reaches {@code SmartHome.getInstance()} in its own
     * {@code init} would otherwise resolve a bridge with no delegate behind it
     * and cache the answer -- and the smart-home API would report itself
     * unsupported for the life of the process on a device that supports it
     * perfectly well.</p>
     */
    private static final String REGISTER_CALL =
            "        com.codename1.impl.android.home."
            + "MatterCommissioningBridge.register();\n";

    private static final String TEMPLATE =
            "/com/codename1/builders/home/MatterCommissioningBridge.javas";

    private static final String TARGET_PACKAGE =
            "com" + File.separator + "codename1" + File.separator + "impl"
            + File.separator + "android" + File.separator + "home";

    private SmartHomeInjector() {
    }

    /**
     * Copies the smart-home delegate into the app's sources and returns the
     * startup snippet that registers it.
     *
     * @param exec   the running builder, for resource access
     * @param srcDir the generated project's Java source root
     * @return the {@code onCreate} snippet, never null
     */
    public static String injectAndroid(Executor exec, File srcDir) {
        try {
            File pkgDir = new File(srcDir, TARGET_PACKAGE);
            pkgDir.mkdirs();
            copyResource(exec, TEMPLATE,
                    new File(pkgDir, "MatterCommissioningBridge.java"));
        } catch (Exception ex) {
            // Loud, because the alternative is an app that compiles, runs and
            // silently reports smart home as unsupported -- which looks like a
            // device problem rather than a build problem.
            throw new RuntimeException(
                    "Failed to inject the smart-home bridge", ex);
        }
        return REGISTER_CALL;
    }

    private static void copyResource(Executor exec, String resource, File out)
            throws Exception {
        InputStream is = exec.getResourceAsStream(resource);
        if (is == null) {
            throw new IllegalStateException(
                    "Missing smart-home template resource: " + resource);
        }
        FileOutputStream os = new FileOutputStream(out);
        try {
            Executor.copy(is, os);
        } finally {
            os.close();
            is.close();
        }
    }
}
