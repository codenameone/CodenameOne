/*
 * Copyright (c) 2021, 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.maven;

import com.codename1.ant.SortedProperties;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CN1BuildMojoTest {

    @Test
    public void mergeRequiredPropertiesAllowsLowerJavaVersionLibrary() throws Exception {
        CN1BuildMojo mojo = new CN1BuildMojo();
        Method method = CN1BuildMojo.class.getDeclaredMethod("mergeRequiredProperties", String.class, Properties.class, Properties.class);
        method.setAccessible(true);

        SortedProperties projectProps = new SortedProperties();
        projectProps.setProperty("codename1.arg.java.version", "17");

        SortedProperties libProps = new SortedProperties();
        libProps.setProperty("codename1.arg.java.version", "8");

        SortedProperties merged = (SortedProperties) method.invoke(mojo, "test-lib", libProps, projectProps);
        assertEquals("17", merged.getProperty("codename1.arg.java.version"));
    }

    @Test
    public void mergeRequiredPropertiesStillFailsOnOtherConflicts() throws Exception {
        CN1BuildMojo mojo = new CN1BuildMojo();
        Method method = CN1BuildMojo.class.getDeclaredMethod("mergeRequiredProperties", String.class, Properties.class, Properties.class);
        method.setAccessible(true);

        SortedProperties projectProps = new SortedProperties();
        projectProps.setProperty("codename1.arg.java.version", "17");
        projectProps.setProperty("codename1.arg.test", "project");

        SortedProperties libProps = new SortedProperties();
        libProps.setProperty("codename1.arg.java.version", "8");
        libProps.setProperty("codename1.arg.test", "lib");

        try {
            method.invoke(mojo, "test-lib", libProps, projectProps);
            fail("Expected a property conflict exception");
        } catch (InvocationTargetException ex) {
            assertTrue(ex.getCause().getMessage().contains("Property codename1.arg.test has a conflict"));
        }
    }

    @Test
    public void stripsTheFrameworkTheServerReSupplies() {
        assertTrue(CN1BuildMojo.isStrippedFromStagedJar("com.codenameone", "codenameone-core", "provided", "ios-device"));
        assertTrue(CN1BuildMojo.isStrippedFromStagedJar("com.codenameone", "codenameone-core", "provided", "ios-source"));
        assertTrue(CN1BuildMojo.isStrippedFromStagedJar("com.codenameone", "java-runtime", "provided", "android-device"));
    }

    @Test
    public void keepsTheFrameworkForLocalJavascriptBuilds() {
        // ParparVM translates locally for these, so it needs every class in the jar.
        assertFalse(CN1BuildMojo.isStrippedFromStagedJar("com.codenameone", "codenameone-core", "provided", "local-javascript"));
        assertFalse(CN1BuildMojo.isStrippedFromStagedJar("com.codenameone", "java-runtime", "provided", "local-javascript"));
    }

    @Test
    public void stripsKotlinStdlibOnlyForServerBuilds() {
        assertTrue(CN1BuildMojo.isStrippedFromStagedJar("org.jetbrains.kotlin", "kotlin-stdlib", "compile", "ios-device"));
        // ios-source generates the Xcode project on this machine, so it is a local
        // target and bundling kotlin-stdlib is simpler than having it re-supplied.
        assertFalse(CN1BuildMojo.isStrippedFromStagedJar("org.jetbrains.kotlin", "kotlin-stdlib", "compile", "ios-source"));
    }

    @Test
    public void keepsTheApplicationsOwnCompileDependencies() {
        assertFalse(CN1BuildMojo.isStrippedFromStagedJar("com.mycompany", "myproject-common", "compile", "ios-source"));
        assertFalse(CN1BuildMojo.isStrippedFromStagedJar("com.codenameone", "cn1-admob-lib", "compile", "ios-device"));
        assertTrue(CN1BuildMojo.isStrippedFromStagedJar("org.junit.jupiter", "junit-jupiter", "test", "ios-device"));
    }

    @Test
    public void keepsArtifactsWithoutAScope() {
        // A null scope is not a statement that the artifact is outside the application.
        assertFalse(CN1BuildMojo.isStrippedFromStagedJar("com.mycompany", "some-lib", null, "ios-device"));
    }

    @Test
    public void onlyTheFrameworkCountsAsSuppliedByTheBuildServer() {
        assertTrue(CN1BuildMojo.isSuppliedByBuildServer("com.codenameone", "codenameone-core", "ios-device"));
        assertTrue(CN1BuildMojo.isSuppliedByBuildServer("com.codenameone", "java-runtime", "ios-source"));
        assertTrue(CN1BuildMojo.isSuppliedByBuildServer("org.jetbrains.kotlin", "kotlin-stdlib", "ios-device"));
        assertFalse(CN1BuildMojo.isSuppliedByBuildServer("com.codenameone", "codenameone-core", "local-javascript"));
        // A `provided` scope third party dependency is stripped from the jar but nobody
        // puts it back, so a reference into it stays reportable.
        assertTrue(CN1BuildMojo.isStrippedFromStagedJar("com.thirdparty", "some-api", "provided", "ios-device"));
        assertFalse(CN1BuildMojo.isSuppliedByBuildServer("com.thirdparty", "some-api", "ios-device"));
    }
}
