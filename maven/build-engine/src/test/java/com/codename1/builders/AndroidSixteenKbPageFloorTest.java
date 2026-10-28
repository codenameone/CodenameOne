/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Every Android dependency the builder injects that carries native code is
/// pinned at or above a release measured to be linked for 16 KB pages.
///
/// The authority is `scripts/check-android-dependency-alignment.py`, which
/// downloads the AARs and reads their ELF headers; it needs the network, so it
/// runs in its own workflow. This is the offline half: it fails the ordinary
/// unit-test run when a pin drops below what was measured, with no network.
/// Google Play refuses an upload containing a 4 KB aligned 64-bit library, and
/// the developer cannot fix one that the build added. CameraX 1.3.4 reached
/// every `CodeScanner` app that way.
class AndroidSixteenKbPageFloorTest {

    /// group:artifact -> the lowest release measured with every 64-bit library,
    /// including transitive ones, at `p_align` 0x4000 or more. Measured, never
    /// guessed: raise a floor only with a run of the script behind it.
    private static final Map<String, String> FLOORS = new LinkedHashMap<String, String>();

    static {
        // camera-core carries the natives; the other CameraX modules depend on
        // it at their own version, so each is held to the same floor.
        FLOORS.put("androidx.camera:camera-core", "1.4.0");
        FLOORS.put("androidx.camera:camera-camera2", "1.4.0");
        FLOORS.put("androidx.camera:camera-lifecycle", "1.4.0");
        FLOORS.put("androidx.camera:camera-view", "1.4.0");
        FLOORS.put("androidx.camera:camera-video", "1.4.0");
        FLOORS.put("com.google.mlkit:barcode-scanning", "17.3.0");
        FLOORS.put("com.google.mlkit:face-detection", "16.1.7");
        FLOORS.put("com.google.mlkit:image-labeling", "17.0.9");
        FLOORS.put("com.google.mlkit:text-recognition", "16.0.1");
        FLOORS.put("com.google.mlkit:pose-detection", "18.0.0-beta5");
        FLOORS.put("com.google.mlkit:segmentation-selfie", "16.0.0-beta6");
        FLOORS.put("com.google.ai.edge.litert:litert", "1.4.2");
        FLOORS.put("com.microsoft.onnxruntime:onnxruntime-android", "1.21.1");
        // 0.1.0 is 4 KB aligned; 0.1.1 is the first aligned release.
        FLOORS.put("com.scottyab:rootbeer-lib", "0.1.1");
    }

    /// group:artifact prefixes that never resolve to 16 KB aligned natives:
    /// TensorFlow's own coordinates stop at 4 KB aligned releases (2.16.1 /
    /// 0.4.4; later ones relocate to LiteRT 1.0.1, also 4 KB aligned).
    private static final String[] BANNED = {
        "org.tensorflow:tensorflow-lite:",
        "org.tensorflow:tensorflow-lite-support:",
    };

    @Test
    void everyNativePinIsAtOrAboveItsMeasuredFloor() throws Exception {
        Map<String, Integer> seen = new LinkedHashMap<String, Integer>();
        for (String floor : FLOORS.keySet()) {
            seen.put(floor, 0);
        }
        List<String> below = new ArrayList<String>();
        for (String line : AndroidDependencyCoordinates.lines()) {
            String coordinate = line.substring(line.indexOf('\t') + 1);
            for (String banned : BANNED) {
                if (coordinate.startsWith(banned)) {
                    below.add(line + " (no 16 KB aligned release exists)");
                }
            }
            String[] parts = coordinate.replace("@aar", "").split(":");
            String module = parts[0] + ":" + parts[1];
            String floor = FLOORS.get(module);
            if (floor == null) {
                continue;
            }
            seen.put(module, seen.get(module) + 1);
            if (compareVersions(parts[2], floor) < 0) {
                below.add(line + " (needs " + floor + " or later)");
            }
        }
        if (!below.isEmpty()) {
            fail("4 KB aligned native pins, which Google Play rejects:\n  "
                    + String.join("\n  ", below));
        }
        for (Map.Entry<String, Integer> floor : seen.entrySet()) {
            // A floor nothing pins is a table that stopped describing the
            // builder; drop it, or find where the pin went.
            assertTrue(floor.getValue() > 0, floor.getKey()
                    + " has a 16 KB floor but no builder dependency uses it");
        }
    }

    @Test
    void theListerCoversTheCatalogAndThePlayServiceDefaults() throws Exception {
        List<String> lines = AndroidDependencyCoordinates.lines();
        boolean camera = false;
        boolean playService = false;
        for (String line : lines) {
            String[] parts = line.split("\t");
            assertEquals(2, parts.length, "not <group><TAB><coordinate>: " + line);
            assertTrue(parts[1].matches("[A-Za-z0-9_.\\-]+:[A-Za-z0-9_.\\-]+:[A-Za-z0-9_.\\-+]+(@aar)?"),
                    "not a resolvable coordinate: " + line);
            camera |= line.startsWith("catalog:com/codename1/ai/vision/CodeScanner\t"
                    + "androidx.camera:camera-core:");
            playService |= line.startsWith("play-services:vision\t");
        }
        assertTrue(camera, "CodeScanner's CameraX is missing from the list");
        assertTrue(playService, "the play-services defaults are missing from the list");
    }

    @Test
    void everyCoordinateTheBuilderSourceNamesIsListed() throws Exception {
        // The ratchet: a new hard-coded dependency in any builder source fails
        // here, offline, until it is added to the lister (and so reaches the
        // networked check) or recorded as not shipped.
        Set<String> known = new LinkedHashSet<String>();
        known.addAll(AndroidDependencyCoordinates.INJECTED.keySet());
        known.addAll(AndroidDependencyCoordinates.NOT_SHIPPED.keySet());
        for (String artifact : AndroidDependencyCoordinates.playServiceCallSites().keySet()) {
            known.add("com.google.android.gms:play-services-" + artifact);
        }
        assertTrue(AndroidDependencyCoordinates.playServiceCallSites().size() >= 15,
                "the play-services call-site pattern stopped matching the builder");
        List<String> unlisted = new ArrayList<String>();
        for (String prefix : AndroidDependencyCoordinates.coordinatePrefixesInSource()) {
            if (!known.contains(prefix)) {
                unlisted.add(prefix);
            }
        }
        assertTrue(unlisted.isEmpty(), "builder sources name Android dependencies the"
                + " 16 KB check never resolves; add them to AndroidDependencyCoordinates."
                + "INJECTED (or NOT_SHIPPED): " + unlisted);
    }

    @Test
    void everyCameraXModuleIsPinnedAtOneVersion() throws Exception {
        // camera-view at 1.4.2 beside camera-core at 1.3.4 would pull the 4 KB
        // aligned core back in transitively while every top-level pin read
        // clean.
        Set<String> versions = new LinkedHashSet<String>();
        for (String line : AndroidDependencyCoordinates.lines()) {
            String coordinate = line.substring(line.indexOf('\t') + 1);
            if (coordinate.startsWith("androidx.camera:")) {
                versions.add(coordinate.substring(coordinate.lastIndexOf(':') + 1));
            }
        }
        assertEquals(1, versions.size(), "CameraX modules pinned at " + versions);
    }

    @Test
    void versionsCompareNumericallyAndPreReleasesSortBeforeTheRelease() {
        assertTrue(compareVersions("1.10.0", "1.4.0") > 0);
        assertTrue(compareVersions("1.3.4", "1.4.0") < 0);
        assertTrue(compareVersions("18.0.0-beta5", "18.0.0-beta5") == 0);
        assertTrue(compareVersions("18.0.0-beta3", "18.0.0-beta5") < 0);
        assertTrue(compareVersions("18.0.0-beta10", "18.0.0-beta5") > 0);
        assertTrue(compareVersions("18.0.0", "18.0.0-beta5") > 0);
        assertTrue(compareVersions("17.0.0-beta1", "21.0.0") < 0);
    }

    /// Numeric segments compare as numbers, a letter run as text, and a
    /// version that ends where the other continues with a qualifier
    /// (`-beta5`) is the later one, because a pre-release precedes its release.
    static int compareVersions(String a, String b) {
        List<Object> left = tokens(a);
        List<Object> right = tokens(b);
        for (int i = 0; i < Math.max(left.size(), right.size()); i++) {
            if (i >= left.size()) {
                return right.get(i) instanceof String ? 1 : -1;
            }
            if (i >= right.size()) {
                return left.get(i) instanceof String ? -1 : 1;
            }
            Object l = left.get(i);
            Object r = right.get(i);
            int c;
            if (l instanceof Long && r instanceof Long) {
                c = ((Long) l).compareTo((Long) r);
            } else if (l instanceof String && r instanceof String) {
                c = ((String) l).compareTo((String) r);
            } else {
                // A number against a qualifier: the release segment wins.
                c = l instanceof Long ? 1 : -1;
            }
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    private static List<Object> tokens(String version) {
        List<Object> out = new ArrayList<Object>();
        StringBuilder run = new StringBuilder();
        Boolean digits = null;
        for (char ch : (version + ".").toCharArray()) {
            boolean digit = ch >= '0' && ch <= '9';
            boolean letter = (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
            if ((digit || letter) && (digits == null || digits == digit)) {
                run.append(ch);
                digits = digit;
                continue;
            }
            if (run.length() > 0) {
                out.add(digits ? (Object) Long.valueOf(run.toString()) : run.toString());
                run.setLength(0);
            }
            digits = null;
            if (digit || letter) {
                run.append(ch);
                digits = digit;
            }
        }
        return out;
    }
}
