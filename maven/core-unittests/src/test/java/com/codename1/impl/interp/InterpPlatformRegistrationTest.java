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
package com.codename1.impl.interp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each port must register its linker, and this is the only thing that says so.
 *
 * <p>A port that does not register one leaves {@code InterpPlatform.isAvailable()}
 * false, and the device runtime reports "this build has no interpreter
 * bindings" -- which reads like a build-flag problem and sends you looking at
 * the build. Nothing else fails: the app installs, starts, and refuses to run
 * anything.</p>
 *
 * <p>The registration has already been lost once, to a rebase that rewrote
 * {@code AndroidImplementation.java} wholesale over a line-ending difference.
 * One line vanished out of fourteen thousand and no test noticed, because
 * every test that could have noticed needs a device. This one reads the source
 * instead, which is worth more than its ugliness: it costs nothing and it runs
 * on every push.</p>
 *
 * @author Shai Almog
 */
class InterpPlatformRegistrationTest {

    /// Repository root, found by walking up from the module the test runs in.
    private static File repositoryRoot() {
        File at = new File("").getAbsoluteFile();
        for (int i = 0; i < 6 && at != null; i++) {
            if (new File(at, "CodenameOne/src/com/codename1/impl/interp").isDirectory()) {
                return at;
            }
            at = at.getParentFile();
        }
        throw new IllegalStateException("cannot find the repository root");
    }

    private static void assertRegisters(String portSource, String linker) throws Exception {
        File f = new File(repositoryRoot(), portSource);
        assertTrue(f.isFile(), portSource + " is missing");
        String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        assertTrue(text.indexOf("InterpPlatform.register(new " + linker) >= 0,
                portSource + " must call InterpPlatform.register(new " + linker + "()):"
                + " without it the port has no interpreter bindings and the device"
                + " runtime refuses every push, with a message that blames the build");
    }

    @Test
    @DisplayName("the Android port registers its linker")
    void androidRegistersItsLinker() throws Exception {
        assertRegisters("Ports/Android/src/com/codename1/impl/android/AndroidImplementation.java",
                "InterpAndroidLinker");
    }

    @Test
    @DisplayName("the iOS port registers its linker")
    void iosRegistersItsLinker() throws Exception {
        assertRegisters("Ports/iOSPort/src/com/codename1/impl/ios/IOSImplementation.java",
                "InterpIOSLinker");
    }

    @Test
    @DisplayName("the JavaSE simulator port registers its linker")
    void javaseRegistersItsLinker() throws Exception {
        assertRegisters("Ports/JavaSE/src/com/codename1/impl/javase/JavaSEPort.java",
                "InterpJavaSELinker");
    }
}
