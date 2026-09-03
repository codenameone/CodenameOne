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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeps {@code LocationButtonManifestFragments}'s framework-class list honest
 * against the framework it filters.
 *
 * <p>The library scan searches customer bytecode for three markers -- the
 * {@code LocationButton} class name and the two persistent-location method
 * names -- and has to ignore the framework's own classes, because a framework
 * jar is staged beside the submitted libraries and would otherwise report every
 * application as a user of everything.</p>
 *
 * <p>That list is exact names rather than package prefixes, so that a cn1lib's
 * own helper sitting in {@code com.codename1.location} or {@code
 * com.codename1.impl} is still inspected. The cost of exactness is drift: a NEW
 * framework class mentioning a marker would be treated as application code, and
 * every application would then look like a user of the location button. That
 * fails in the dangerous direction -- today it refuses every Android build.</p>
 *
 * <p>So this walks the framework as actually built and fails when a class
 * carrying a marker is not covered. Adding one is then a deliberate act with a
 * failing test in front of it, rather than something noticed in production.</p>
 */
public class LocationButtonMarkerCoverageTest {

    /** The markers the scan searches for, which is what has to be covered. */
    private static final String[] MARKERS = {
        "com/codename1/location/LocationButton",
        "addGeoFencing",
        "setBackgroundLocationListener",
        "setLocationListener",
        "getCurrentLocation",
        "getCurrentLocationSync",
        "getLastKnownLocation",
        "com/codename1/location/GeofenceManager",
        "com/codename1/maps/MapComponent",
    };

    /**
     * The built framework trees this can see from the plugin module: the core
     * and the Android port, which are what an Android build stages.
     */
    private static final String[] TREES = {
        "../core/target/classes",
        "../android/target/classes",
    };

    @Test
    public void everyFrameworkClassCarryingAMarkerIsFiltered() throws Exception {
        List<File> roots = new ArrayList<File>();
        for (String tree : TREES) {
            File f = new File(tree);
            if (f.isDirectory()) {
                roots.add(f);
            }
        }
        // Not skipped when the trees are missing. A check that passes because
        // it found nothing to look at is not a check, and this one exists
        // precisely to catch something nobody remembered to look for.
        assertTrue(!roots.isEmpty(),
                "no built framework classes found under " + TREES[0] + " or "
                + TREES[1] + "; build the core and the Android port first "
                + "(mvn -Pcompile-android -pl android -am -DskipTests compile)");

        List<String> uncovered = new ArrayList<String>();
        int scanned = 0;
        for (File root : roots) {
            scanned += collect(root, root, uncovered);
        }
        assertTrue(scanned > 0, "no .class files under " + roots);
        assertTrue(uncovered.isEmpty(),
                "these framework classes carry a location marker but are not in "
                + "LocationButtonManifestFragments.FRAMEWORK_CLASSES, so every "
                + "application would be reported as using the location button: "
                + uncovered);
    }

    private static int collect(File root, File dir, List<String> uncovered)
            throws Exception {
        int scanned = 0;
        File[] children = dir.listFiles();
        if (children == null) {
            return 0;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                scanned += collect(root, child, uncovered);
                continue;
            }
            if (!child.getName().endsWith(".class")) {
                continue;
            }
            scanned++;
            String rel = root.toPath().relativize(child.toPath()).toString()
                    .replace('\\', '/');
            // ISO-8859-1 for the reason the scan itself uses it: every byte
            // maps to one character, so nothing in a constant pool is dropped.
            String text = new String(Files.readAllBytes(child.toPath()),
                    StandardCharsets.ISO_8859_1);
            for (String marker : MARKERS) {
                // The same pair of questions the scanner asks: is this class
                // named on the list, or does its own InnerClasses attribute say
                // it is nested inside one. A name test alone cannot answer the
                // second -- that is the whole reason the scanner reads bytes --
                // so this reads them too rather than checking something easier
                // than what production does.
                if (text.contains(marker)
                        && !LocationButtonManifestFragments
                                .isFrameworkClass(rel)
                        && !LocationButtonManifestFragments
                                .isNestedInsideFramework(text)) {
                    uncovered.add(rel + " (" + marker + ")");
                    break;
                }
            }
        }
        return scanned;
    }
}
