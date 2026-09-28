/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided by
 * Oracle in the LICENSE file that accompanied this code.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Finding the artifacts the translator writes beside the bundle, so they can be copied out of a
 * build directory that is about to be deleted.
 */
class JavaScriptBuilderBuildArtifactsTest {

    private static List<String> namesOf(File[] found) {
        List<String> names = new ArrayList<String>();
        for (File f : found) {
            names.add(f.getName());
        }
        return names;
    }

    @Test
    void findsTheConfigurationAndTheReportBesideTheBundle() throws Exception {
        File dist = Files.createTempDirectory("js-artifacts").toFile();
        File bundle = new File(dist, "MyApp-js");
        assertTrue(bundle.mkdirs());
        assertTrue(new File(dist, "MyApp-js-cn1-security").mkdirs());
        Files.write(new File(dist, "MyApp-js-suspension-report.txt").toPath(),
                "report".getBytes(StandardCharsets.UTF_8));

        List<String> found = namesOf(JavaScriptBuilder.locateBuildArtifacts(bundle));
        assertEquals(2, found.size(), found.toString());
        assertTrue(found.contains("MyApp-js-cn1-security"), found.toString());
        assertTrue(found.contains("MyApp-js-suspension-report.txt"), found.toString());
    }

    @Test
    void ignoresAnotherApplicationsArtifacts() throws Exception {
        // Every bundle of a destination shares dist/, so a sweep of the bundle's siblings would
        // hand this build the other application's configuration -- whose policy is hashed from
        // a page this bundle does not contain.
        File dist = Files.createTempDirectory("js-artifacts-two").toFile();
        File bundle = new File(dist, "MyApp-js");
        assertTrue(bundle.mkdirs());
        assertTrue(new File(dist, "MyApp-js-cn1-security").mkdirs());
        assertTrue(new File(dist, "Other-js-cn1-security").mkdirs());

        List<String> found = namesOf(JavaScriptBuilder.locateBuildArtifacts(bundle));
        assertEquals(1, found.size(), found.toString());
        assertTrue(found.contains("MyApp-js-cn1-security"), found.toString());
    }

    @Test
    void ignoresACopyOfTheBundleItself() throws Exception {
        // When renameTo fails the builder copies instead, leaving the original bundle beside
        // the new one. A sibling sweep would pick up a second copy of the whole application.
        File dist = Files.createTempDirectory("js-artifacts-copy").toFile();
        File bundle = new File(dist, "MyApp-js");
        assertTrue(bundle.mkdirs());
        assertTrue(new File(dist, "Translator-js").mkdirs());

        assertEquals(0, JavaScriptBuilder.locateBuildArtifacts(bundle).length);
    }

    @Test
    void reportsNothingWhenTheTranslatorWroteNothing() throws Exception {
        File dist = Files.createTempDirectory("js-artifacts-none").toFile();
        File bundle = new File(dist, "MyApp-js");
        assertTrue(bundle.mkdirs());

        assertEquals(0, JavaScriptBuilder.locateBuildArtifacts(bundle).length);
    }
}
