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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The legacy biometric backend goes through {@code FingerprintManagerCompat}
 * and must survive every {@code compileSdk}, because an application compiled
 * against API 37 still runs on the API 23-28 devices where the platform API it
 * wraps is the only biometric API there is. Only the BiometricPrompt-backed
 * package has a floor.
 *
 * <p>Issue #5701 is what happens when a port source names the platform
 * {@code FingerprintManager} instead: an unmodified Hello World generated
 * against an API 37 platform failed {@code compileDebugJavaWithJavac} on a
 * class that platform removed.</p>
 */
class AndroidBiometricSourceSelectionTest {

    private static final String LEGACY = "com/codename1/impl/android/fingerprint";
    private static final String MODERN = "com/codename1/impl/android/biometrics";

    private static File plant(Path srcDir) throws IOException {
        for (String pkg : new String[]{LEGACY, MODERN}) {
            Path dir = srcDir.resolve(pkg);
            Files.createDirectories(dir);
            Files.write(dir.resolve("Backend.java"), "class Backend {}".getBytes("UTF-8"));
        }
        return srcDir.toFile();
    }

    private static boolean present(Path srcDir, String pkg) {
        return Files.exists(srcDir.resolve(pkg).resolve("Backend.java"));
    }

    @Test
    void api37KeepsTheFingerprintPackage(@TempDir Path srcDir) throws IOException {
        AndroidGradleBuilder.pruneBiometricSourcesForCompileSdk(plant(srcDir), 37);
        assertTrue(present(srcDir, LEGACY),
                "the compat-backed legacy package compiles against API 37 and is "
                        + "the only biometric API an API 23-28 device answers, "
                        + "which an app built against 37 still runs on");
        assertTrue(present(srcDir, MODERN));
    }

    @Test
    void api36KeepsBoth(@TempDir Path srcDir) throws IOException {
        AndroidGradleBuilder.pruneBiometricSourcesForCompileSdk(plant(srcDir), 36);
        assertTrue(present(srcDir, LEGACY));
        assertTrue(present(srcDir, MODERN));
    }

    @Test
    void theLegacyCompileSdksKeepBoth(@TempDir Path tmp) throws IOException {
        // 28 is the bottom of compileSdkInt's ladder and the floor the modern
        // backend is written to, so the legacy
        // android.useGradle8=false / android.buildToolsVersion=28 configuration
        // keeps both. An APK compiled here still runs on an API 37 device,
        // where the legacy backend's platform API is gone and BiometricPrompt
        // is the only thing left to serve it.
        for (int sdk = 28; sdk <= 30; sdk++) {
            Path srcDir = tmp.resolve("sdk" + sdk);
            Files.createDirectories(srcDir);
            AndroidGradleBuilder.pruneBiometricSourcesForCompileSdk(plant(srcDir), sdk);
            assertTrue(present(srcDir, LEGACY), "compileSdk " + sdk);
            assertTrue(present(srcDir, MODERN), "compileSdk " + sdk);
        }
    }

    @Test
    void belowBiometricPromptItselfTheModernPackageGoes(@TempDir Path srcDir) throws IOException {
        // BiometricPrompt arrives in 28, so 27 cannot compile the package at
        // all. compileSdkInt does not generate this, but it is what the floor
        // means and the legacy backend still covers it.
        AndroidGradleBuilder.pruneBiometricSourcesForCompileSdk(plant(srcDir), 27);
        assertTrue(present(srcDir, LEGACY));
        assertFalse(present(srcDir, MODERN));
    }

    @Test
    void anUnknownCompileSdkDeletesNothing(@TempDir Path srcDir) throws IOException {
        // compileSdkInt returns 0 when it cannot parse the version. Deleting on
        // that would silently drop a backend the compile would have accepted.
        AndroidGradleBuilder.pruneBiometricSourcesForCompileSdk(plant(srcDir), 0);
        assertTrue(present(srcDir, LEGACY));
        assertTrue(present(srcDir, MODERN));
    }

    /// A compileSdk that deleted both packages would leave
    /// `AndroidBiometrics.backend()` with nothing to load and biometrics
    /// silently off for every device.
    @Test
    void everyCompileSdkKeepsAtLeastOneBackend(@TempDir Path tmp) throws IOException {
        for (int sdk = 21; sdk <= 45; sdk++) {
            Path srcDir = tmp.resolve("sdk" + sdk);
            Files.createDirectories(srcDir);
            AndroidGradleBuilder.pruneBiometricSourcesForCompileSdk(plant(srcDir), sdk);
            assertTrue(present(srcDir, LEGACY) || present(srcDir, MODERN),
                    "compileSdk " + sdk + " deleted both biometric backends");
        }
    }

    @Test
    void anAbsentPackageIsNotAnError(@TempDir Path srcDir) {
        AndroidGradleBuilder.pruneBiometricSourcesForCompileSdk(srcDir.toFile(), 37);
    }
}
