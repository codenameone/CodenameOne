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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two platform scanners classify health usage the same way.
 *
 * <p>They answer different questions -- one picks Health Connect and
 * per-type permissions, the other HealthKit and purpose strings -- but
 * they read the same bytecode and must agree on <em>what the app is
 * doing</em>. Three separate defects on this branch were the same shape:
 * a classification rule fixed in the Android scanner and never mirrored,
 * so the iOS build kept the old behaviour until somebody noticed
 * independently. This fails the build instead.</p>
 *
 * <p>It matches on source text rather than behaviour, which is crude; the
 * rules live in anonymous visitor callbacks with no seam to call. Crude
 * and load-bearing beats absent.</p>
 */
public class HealthScannerParityTest {

    private static String source(String simpleName) throws Exception {
        File f = new File("src/main/java/com/codename1/builders/"
                + simpleName + ".java");
        assertTrue(f.exists(), "scanner source must be readable: "
                + f.getAbsolutePath());
        return new String(Files.readAllBytes(f.toPath()),
                StandardCharsets.UTF_8);
    }

    /**
     * Enabling sensor write-through is store use on both platforms. It is
     * the one call inside the sensors package that needs the health
     * stack, and the package-wide exemption hides it by default.
     */
    @Test
    void bothScannersTreatSensorWriteThroughAsStoreUse() throws Exception {
        for (String builder : new String[] {"AndroidGradleBuilder",
                "IPhoneBuilder"}) {
            String src = source(builder);
            assertTrue(src.contains(
                    "com/codename1/health/sensors/SensorSessionOptions"),
                    builder + " must classify setWriteToStore as store use;"
                            + " the sensors exemption hides it otherwise");
            assertTrue(src.contains("setWriteToStore"), builder);
        }
    }

    /**
     * The shared value types are not store use on either platform. A
     * sensor callback names {@code HealthSample} whatever else it does.
     */
    @Test
    void bothScannersExemptTheSharedModelTypes() throws Exception {
        for (String builder : new String[] {"AndroidGradleBuilder",
                "IPhoneBuilder"}) {
            String src = source(builder);
            assertTrue(src.contains("isSharedHealthModel"),
                    builder + " must exempt the health value types, or a"
                            + " BLE-only app is dragged into the health"
                            + " stack by a listener signature");
        }
    }

    /**
     * Obtaining the workout manager needs both purpose strings on iOS and
     * both direction flags on Android: a workout reads sensor data and
     * writes a session.
     */
    @Test
    void bothScannersTreatWorkoutsAsReadAndWrite() throws Exception {
        for (String builder : new String[] {"AndroidGradleBuilder",
                "IPhoneBuilder"}) {
            String src = source(builder);
            // The classification block, which is the last mention: the
            // earlier ones are the facade condition and its comments.
            int at = src.lastIndexOf("startsWith(\"getWorkouts\")");
            assertTrue(at > 0, builder + " must classify getWorkouts");
            int end = src.indexOf("\n                        }", at);
            if (end < 0) {
                end = Math.min(src.length(), at + 1200);
            }
            String after = src.substring(at, end);
            assertTrue(after.contains("usesHealthRead")
                    && after.contains("usesHealthWrite"),
                    builder + " must set both directions at getWorkouts");
        }
    }
}
