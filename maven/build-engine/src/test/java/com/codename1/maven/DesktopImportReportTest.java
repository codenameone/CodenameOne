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
package com.codename1.maven;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/// What an import tells the developer about the Java level it was written
/// for.
public class DesktopImportReportTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void aProjectNewerThanTheBuildIsToldWhatThatCosts() {
        assertNull(DesktopImportReport.javaLevelWarning(0));
        assertNull(DesktopImportReport.javaLevelWarning(8));
        assertNull(DesktopImportReport.javaLevelWarning(17));
        String warning = DesktopImportReport.javaLevelWarning(21);
        assertTrue(warning, warning.startsWith("The project is built for Java 21; a Codename One application is "
                + "compiled as Java 17."));
        assertTrue(warning, warning.contains("-source/-target 17"));
        assertTrue(warning, warning.contains("compile error"));
        assertTrue(warning, warning.contains("compliance check"));
    }

    private int level(String name, String build) throws Exception {
        File dir = tmp.newFolder();
        Files.write(new File(dir, name).toPath(), build.getBytes("UTF-8"));
        return DesktopProjectImporter.javaLevel(dir, dir);
    }

    @Test
    public void theJavaLevelIsReadFromTheProjectsBuild() throws Exception {
        // The compiler plugin's own configuration wins over the properties.
        assertEquals(21, level("pom.xml", "<project><properties>"
                + "<maven.compiler.source>25</maven.compiler.source></properties><build><plugins><plugin>"
                + "<artifactId>maven-compiler-plugin</artifactId><configuration><source>21</source>"
                + "<target>21</target></configuration></plugin></plugins></build></project>"));
        assertEquals(25, level("pom.xml", "<project><properties>"
                + "<maven.compiler.source>25</maven.compiler.source></properties></project>"));
        // release before source, and a property resolved in the same file.
        assertEquals(17, level("pom.xml", "<project><properties><java.version>17</java.version></properties>"
                + "<build><plugins><plugin><artifactId>maven-compiler-plugin</artifactId><configuration>"
                + "<source>11</source><release>${java.version}</release></configuration></plugin></plugins>"
                + "</build></project>"));
        assertEquals(8, level("pom.xml", "<project><properties>"
                + "<maven.compiler.target>1.8</maven.compiler.target></properties></project>"));
        assertEquals(0, level("pom.xml", "<project><properties>"
                + "<maven.compiler.release>${elsewhere}</maven.compiler.release></properties></project>"));
        assertEquals(0, level("pom.xml", "<project/>"));

        assertEquals(21, level("build.gradle", "java {\n  toolchain {\n"
                + "    languageVersion = JavaLanguageVersion.of(21)\n  }\n}\n"));
        assertEquals(21, level("build.gradle.kts", "kotlin {\n  jvmToolchain(21)\n}\n"));
        assertEquals(19, level("build.gradle", "sourceCompatibility = JavaVersion.VERSION_19\n"));
        assertEquals(8, level("build.gradle", "sourceCompatibility = JavaVersion.VERSION_1_8\n"));
        assertEquals(11, level("build.gradle", "sourceCompatibility = '11'\n"));
        assertEquals(0, level("build.gradle", "dependencies { }\n"));
    }

    @Test
    public void aModuleInheritsTheLevelOfTheProjectItIsIn() throws Exception {
        File root = tmp.newFolder();
        File module = new File(root, "app");
        assertTrue(module.mkdirs());
        Files.write(new File(root, "pom.xml").toPath(), ("<project><properties>"
                + "<maven.compiler.release>21</maven.compiler.release></properties></project>").getBytes("UTF-8"));
        Files.write(new File(module, "pom.xml").toPath(), "<project/>".getBytes("UTF-8"));
        assertEquals(21, DesktopProjectImporter.javaLevel(root, module));
    }
}
