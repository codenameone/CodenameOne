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

import com.codename1.build.shared.PlatformFeatureCatalog;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A convenience class in {@code com.codename1.ai.vision} that builds an
 * analyzer for the caller is still selected by both builders.
 *
 * <p>Nothing else catches this. The builders decide which native vision
 * dependency to package by scanning the <em>application's</em> classes for the
 * concrete analyzer types, and core is not part of that scan. So an app that
 * references only {@code CodeScanner} never names {@code BarcodeScanner}, the
 * scan sees no barcode analyzer, the Android adapter is pruned and the iOS
 * natives are left out -- and the build is green while the feature is
 * inert on the device. The same trap swallows the camera natives for a class
 * that opens a camera without the app naming {@code com.codename1.camera}.</p>
 *
 * <p>This test walks the vision sources rather than a hand-written list, so a
 * convenience class added later fails here until it is mapped.</p>
 */
class HighLevelVisionDependencyTest {
    /** The concrete analyzers the builders key their dependencies on. */
    private static final String[] ANALYZERS = {
        "TextRecognizer", "BarcodeScanner", "FaceDetector", "ImageLabeler",
        "PoseDetector", "SelfieSegmenter", "DocumentScanner"
    };

    private static final File VISION_DIR = new File(
            "../../CodenameOne/src/com/codename1/ai/vision");

    private static String read(File file) throws Exception {
        assertTrue(file.exists(),
                "source must be readable: " + file.getAbsolutePath());
        return new String(Files.readAllBytes(file.toPath()),
                StandardCharsets.UTF_8);
    }

    /**
     * Strips comments so a javadoc sample does not read as a real reference.
     * {@code TextScript}'s documentation constructs a {@code TextRecognizer}
     * to show how a script is selected, which is not the class depending on
     * the analyzer.
     */
    private static String code(File file) throws Exception {
        String body = read(file);
        StringBuilder out = new StringBuilder(body.length());
        int i = 0;
        while (i < body.length()) {
            if (body.startsWith("/*", i)) {
                int end = body.indexOf("*/", i + 2);
                i = end < 0 ? body.length() : end + 2;
            } else if (body.startsWith("//", i)) {
                int end = body.indexOf('\n', i);
                i = end < 0 ? body.length() : end;
            } else {
                out.append(body.charAt(i));
                i++;
            }
        }
        return out.toString();
    }

    /**
     * Vision classes that construct an analyzer themselves, paired with the
     * analyzer they construct.
     */
    private static List<String[]> analyzerBuildingClasses() throws Exception {
        assertTrue(VISION_DIR.isDirectory(),
                "vision sources must be readable: "
                        + VISION_DIR.getAbsolutePath());
        List<String[]> out = new ArrayList<String[]>();
        File[] sources = VISION_DIR.listFiles();
        assertNotNull(sources);
        for (File source : sources) {
            String name = source.getName();
            if (!name.endsWith(".java")) {
                continue;
            }
            String simple = name.substring(0, name.length() - ".java".length());
            if (isAnalyzer(simple)) {
                continue;
            }
            String body = code(source);
            for (String analyzer : ANALYZERS) {
                if (body.contains("new " + analyzer + "(")) {
                    out.add(new String[] {simple, analyzer});
                }
            }
        }
        return out;
    }

    private static boolean isAnalyzer(String simpleName) {
        for (String analyzer : ANALYZERS) {
            if (analyzer.equals(simpleName)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void aConvenienceClassSelectsTheSameAndroidAdapterAsItsAnalyzer()
            throws Exception {
        List<String[]> classes = analyzerBuildingClasses();
        assertTrue(!classes.isEmpty(),
                "CodeScanner builds a BarcodeScanner, so the scan of the "
                        + "vision sources must not come back empty -- an empty "
                        + "result means this test stopped testing anything");
        for (String[] pair : classes) {
            String expected = AndroidGradleBuilder.androidAiAdapterSource(
                    "com/codename1/ai/vision/" + pair[1]);
            assertNotNull(expected, pair[1] + " has no Android adapter");
            assertEquals(expected,
                    AndroidGradleBuilder.androidAiAdapterSource(
                            "com/codename1/ai/vision/" + pair[0]),
                    pair[0] + " builds a " + pair[1] + " but does not select"
                            + " its Android adapter, so the adapter is pruned"
                            + " and the feature reports itself unsupported");
        }
    }

    @Test
    void aConvenienceClassIsAlsoAVisionFeatureOnIos() throws Exception {
        String builder = read(new File(
                "src/main/java/com/codename1/builders/IPhoneBuilder.java"));
        for (String[] pair : analyzerBuildingClasses()) {
            assertTrue(builder.contains(
                    "\"com/codename1/ai/vision/" + pair[0] + "\""),
                    pair[0] + " builds a " + pair[1] + " but IPhoneBuilder"
                            + " never names it, so an app referencing only the"
                            + " convenience class gets no vision natives");
        }
    }

    /** The Android artifacts the catalog adds for one class. */
    private static List<String> androidDeps(String simpleName) {
        List<String> out = new ArrayList<String>();
        for (PlatformFeatureCatalog.Entry entry : PlatformFeatureCatalog
                .matchesFor("com/codename1/ai/vision/" + simpleName)) {
            out.addAll(entry.androidGradleDeps());
        }
        return out;
    }

    @Test
    void aConvenienceClassCarriesItsAnalyzersOwnDependencies()
            throws Exception {
        // Retaining the adapter source is only half of it. The adapter is
        // compiled inside the generated app against an artifact the catalog
        // adds, so a convenience class that selects the adapter without
        // selecting the artifact produces a project that does not compile.
        for (String[] pair : analyzerBuildingClasses()) {
            List<String> analyzer = androidDeps(pair[1]);
            assertTrue(!analyzer.isEmpty(),
                    pair[1] + " has no catalog Android dependency");
            List<String> convenience = androidDeps(pair[0]);
            for (String dep : analyzer) {
                assertTrue(convenience.contains(dep),
                        pair[0] + " builds a " + pair[1] + " but the catalog"
                                + " does not add " + dep + " for it, so the"
                                + " retained adapter has nothing to compile"
                                + " against");
            }
        }
    }

    @Test
    void aVisionClassThatOpensTheCameraCarriesTheCameraDependencies()
            throws Exception {
        // Same trap one level over: an app that references only CodeScanner or
        // VisionCameraView never names com.codename1.camera, so the camera
        // entry never fires and the preview opens on hardware the build did
        // not provision.
        for (String simple : cameraOpeningClasses()) {
            List<String> deps = androidDeps(simple);
            assertTrue(deps.contains("androidx.camera:camera-core:1.3.4"),
                    simple + " opens a camera but the catalog adds no CameraX"
                            + " artifact for it");
            boolean permission = false;
            boolean avFoundation = false;
            for (PlatformFeatureCatalog.Entry entry : PlatformFeatureCatalog
                    .matchesFor("com/codename1/ai/vision/" + simple)) {
                permission |= entry.androidPermissions()
                        .contains("android.permission.CAMERA");
                avFoundation |= entry.iosFrameworks().contains("AVFoundation");
            }
            assertTrue(permission,
                    simple + " opens a camera but the catalog requests no"
                            + " CAMERA permission for it");
            assertTrue(avFoundation,
                    simple + " opens a camera but the catalog links no"
                            + " AVFoundation for it");
        }
    }

    /** Vision classes that call Camera.open, directly or through the view. */
    private static List<String> cameraOpeningClasses() throws Exception {
        List<String> out = new ArrayList<String>();
        File[] sources = VISION_DIR.listFiles();
        assertNotNull(sources);
        for (File source : sources) {
            String name = source.getName();
            if (!name.endsWith(".java")) {
                continue;
            }
            String body = code(source);
            // VisionPipeline consumes a session the application opened; the
            // classes this finds are the ones that call Camera.open
            // themselves, directly or through VisionCameraView.
            if (body.contains("Camera.open(")
                    || body.contains("new VisionCameraView")) {
                out.add(name.substring(0, name.length() - ".java".length()));
            }
        }
        assertTrue(!out.isEmpty(),
                "VisionCameraView opens the camera, so this scan must not come"
                        + " back empty");
        return out;
    }

    @Test
    void aVisionClassThatOpensTheCameraSelectsTheCameraNatives()
            throws Exception {
        String builder = read(new File(
                "src/main/java/com/codename1/builders/IPhoneBuilder.java"));
        for (String simple : cameraOpeningClasses()) {
            assertTrue(builder.contains(
                    "\"com/codename1/ai/vision/" + simple + "\""),
                    simple + " opens a camera but IPhoneBuilder never names"
                            + " it, so usesCn1Camera stays false and the"
                            + " AVFoundation preview natives are left out");
        }
    }
}
