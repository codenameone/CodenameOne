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
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Nothing here raises an app's deployment target, and that is a measured result: a target built
 * at IPHONEOS_DEPLOYMENT_TARGET=13.0 with availability-fenced App Intents declarations emits a
 * complete Metadata.appintents containing every intent and entity. Xcode does not gate metadata
 * extraction on the deployment target.
 *
 * These pin that no configuration silently costs an app its older devices, and that the opt-in
 * floor still behaves when a project asks for one.
 */
class IPhoneBuilderIntentsDeploymentTargetTest {

    private static BuildRequest request(String... kv) {
        BuildRequest r = new BuildRequest();
        r.setMainClass("MyApp");
        r.setPackageName("com.example");
        for (int i = 0; i < kv.length; i += 2) {
            r.putArgument(kv[i], kv[i + 1]);
        }
        return r;
    }

    /** Drives the private manifest parse and reports the floor the builder settled on. */
    private static String floorFor(File resDir, BuildRequest request, boolean usesIntents)
            throws Exception {
        IPhoneBuilder b = new IPhoneBuilder();
        Field uses = IPhoneBuilder.class.getDeclaredField("usesIntents");
        uses.setAccessible(true);
        uses.setBoolean(b, usesIntents);

        Method parse = IPhoneBuilder.class.getDeclaredMethod("parseIntentsManifest",
                File.class, BuildRequest.class);
        parse.setAccessible(true);
        try {
            parse.invoke(b, resDir, request);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }

        Method target = IPhoneBuilder.class.getDeclaredMethod("getDeploymentTarget",
                BuildRequest.class);
        target.setAccessible(true);
        return (String) target.invoke(b, request);
    }

    private static File manifest(Path dir, String json) throws IOException {
        File f = new File(dir.toFile(), "intents.json");
        FileWriter w = new FileWriter(f);
        try {
            w.write(json);
        } finally {
            w.close();
        }
        return dir.toFile();
    }

    @Test
    void anAppThatNeverTouchesIntentsIsUnaffected(@TempDir Path dir) throws Exception {
        String floor = floorFor(dir.toFile(), request(), false);

        assertTrue(compare(floor, "16.0") < 0,
                "an app with no intents must not be pushed to the App Intents floor, got " + floor);
    }

    @Test
    void indexingOnlyDoesNotRaiseTheFloor(@TempDir Path dir) throws Exception {
        // The processor emits a manifest with no intents when the project declares only
        // entity types, or when the app just calls Intents.index at runtime.
        File res = manifest(dir, "{\n  \"schema\": 1,\n  \"intents\": [],\n  \"entities\": []\n}\n");

        String floor = floorFor(res, request(), true);

        assertTrue(compare(floor, "16.0") < 0,
                "Core Spotlight predates the current floor, so indexing must cost nothing; got "
                        + floor);
    }

    @Test
    void aDeclaredIntentDoesNotRaiseTheFloor(@TempDir Path dir) throws Exception {
        // The interesting case, and the one that would quietly drop iOS 13-15 users if it were
        // wrong. Verified against Xcode: the metadata is emitted at 13.0 regardless.
        File res = manifest(dir, "{\"schema\": 1, \"intents\": [{\"id\": \"log_workout\"}]}");

        String floor = floorFor(res, request(), true);

        assertTrue(compare(floor, "16.0") < 0,
                "declaring an intent must not cost an app its older devices; got " + floor);
    }

    @Test
    void aProjectMayAskForTheFloorAnyway(@TempDir Path dir) throws Exception {
        // For a project that would rather not ship a build whose intents are inert on part of
        // its audience.
        File res = manifest(dir, "{\"schema\": 1, \"intents\": [{\"id\": \"log_workout\"}]}");

        String floor = floorFor(res, request("ios.intents.minDeploymentTarget", "16.0"), true);

        assertEquals("16.0", floor);
    }

    @Test
    void optingOutOfAppIntentsKeepsTheLowerFloor(@TempDir Path dir) throws Exception {
        File res = manifest(dir, "{\"schema\": 1, \"intents\": [{\"id\": \"log_workout\"}]}");

        String floor = floorFor(res, request("ios.intents.appIntents", "false"), true);

        assertTrue(compare(floor, "16.0") < 0,
                "ios.intents.appIntents=false is the documented way to stay low, got " + floor);
    }

    @Test
    void theOptInFloorIsConfigurable(@TempDir Path dir) throws Exception {
        File res = manifest(dir, "{\"schema\": 1, \"intents\": [{\"id\": \"log_workout\"}]}");

        String floor = floorFor(res, request("ios.intents.minDeploymentTarget", "17.0"), true);

        assertEquals("17.0", floor);
    }

    @Test
    void anEmptyFloorLeavesTheTargetAloneEntirely(@TempDir Path dir) throws Exception {
        // The default, stated explicitly.
        File res = manifest(dir, "{\"schema\": 1, \"intents\": [{\"id\": \"log_workout\"}]}");

        String floor = floorFor(res, request("ios.intents.minDeploymentTarget", ""), true);

        assertTrue(compare(floor, "16.0") < 0, "got " + floor);
    }

    @Test
    void anExplicitPinIsFineByDefault(@TempDir Path dir) throws Exception {
        // Nothing contributes a floor, so a pinned target is simply honoured. This used to be
        // a build failure, which was a cost invented for no benefit.
        File res = manifest(dir, "{\"schema\": 1, \"intents\": [{\"id\": \"log_workout\"}]}");

        assertEquals("13.0", floorFor(res, request("ios.deployment_target", "13.0"), true));
    }

    @Test
    void askingForAFloorBelowAnExplicitPinIsReported(@TempDir Path dir) throws Exception {
        // Only reachable when a project explicitly asks for the floor and also pins lower --
        // two deliberate settings that contradict each other.
        File res = manifest(dir, "{\"schema\": 1, \"intents\": [{\"id\": \"log_workout\"}]}");

        BuildException e = assertThrows(BuildException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() throws Throwable {
                floorFor(res, request("ios.deployment_target", "13.0",
                        "ios.intents.minDeploymentTarget", "16.0"), true);
            }
        });

        String msg = e.getMessage();
        assertTrue(msg.contains("13.0"), msg);
        assertTrue(msg.contains("ios.intents.minDeploymentTarget"), msg);
    }

    @Test
    void aPinAtOrAboveTheFloorIsFine(@TempDir Path dir) throws Exception {
        File res = manifest(dir, "{\"schema\": 1, \"intents\": [{\"id\": \"log_workout\"}]}");

        String floor = floorFor(res, request("ios.deployment_target", "17.2"), true);

        assertEquals("17.2", floor);
    }

    @Test
    void aMissingManifestIsNotAnError(@TempDir Path dir) throws Exception {
        // An app may reference com.codename1.intents purely to index content, in which case
        // the processor emits nothing. Surfaces fails the build here; intents must not.
        String floor = floorFor(dir.toFile(), request(), true);

        assertTrue(compare(floor, "16.0") < 0, "got " + floor);
    }

    private static int compare(String a, String b) throws Exception {
        Method m = IPhoneBuilder.class.getDeclaredMethod("compareVersionStrings",
                String.class, String.class);
        m.setAccessible(true);
        return ((Integer) m.invoke(null, a, b)).intValue();
    }
}
