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
package com.codename1.impl.javase;

import com.codename1.project.BuildSystem;
import com.codename1.project.ProjectLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/// How the simulator finds the project it runs, for each build tool. The
/// Maven and Ant answers must be the ones the simulator always gave; Gradle's
/// are new.
public class SimulatorProjectTest {
    private final String savedUserDir = System.getProperty("user.dir");

    @AfterEach
    void restoreUserDir() {
        System.setProperty("user.dir", savedUserDir);
        SimulatorProject.reset();
    }

    private static File write(File dir, String name, String content) throws IOException {
        File f = new File(dir, name);
        f.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return f;
    }

    private static File canonical(File f) throws IOException {
        return f.getCanonicalFile();
    }

    private static File gradleProject(File root) throws IOException {
        write(root, "settings.gradle.kts", "rootProject.name = \"app\"\n");
        write(root, "build.gradle.kts", "plugins { id(\"com.codenameone\") }\n");
        write(root, "codenameone_settings.properties", "codename1.mainName=App\n");
        new File(root, "src/main/java").mkdirs();
        return root;
    }

    private static File mavenProject(File root) throws IOException {
        write(root, "pom.xml", "<project/>");
        write(root, "common/pom.xml", "<project/>");
        write(root, "common/codenameone_settings.properties", "codename1.mainName=App\n");
        new File(root, "javase").mkdirs();
        write(root, "javase/pom.xml", "<project/>");
        return root;
    }

    @Test
    void gradleProjectResolvesAtItsRoot(@TempDir Path tmp) throws IOException {
        File root = gradleProject(tmp.toFile());
        ProjectLayout layout = SimulatorProject.locate(root);
        assertNotNull(layout);
        assertEquals(BuildSystem.GRADLE, layout.buildSystem());
        assertEquals(canonical(root), canonical(layout.projectDir()));

        System.setProperty("user.dir", root.getAbsolutePath());
        SimulatorProject.reset();
        assertEquals(canonical(new File(root, "codenameone_settings.properties")),
                canonical(SimulatorProject.settingsFile()));
        assertEquals(canonical(new File(root, "build/classes/java/main")),
                canonical(SimulatorProject.classesDir(root)));
        assertEquals(canonical(new File(root, "build/resources/main")),
                canonical(SimulatorProject.resourcesOutputDir(root)));
        assertEquals(canonical(new File(root, "build")), canonical(SimulatorProject.buildDir(root)));
    }

    @Test
    void mavenProjectResolvesToCommonFromEveryOldStartingPoint(@TempDir Path tmp) throws IOException {
        File root = mavenProject(tmp.toFile());
        File common = new File(root, "common");
        // The old lookups: cwd, cwd/common and ../common.
        for (File start : new File[]{common, root, new File(root, "javase")}) {
            ProjectLayout layout = SimulatorProject.locate(start);
            assertNotNull(layout, "from " + start);
            assertEquals(BuildSystem.MAVEN, layout.buildSystem());
            assertEquals(canonical(common), canonical(layout.projectDir()), "from " + start);
        }
        assertEquals(new File(common, "target" + File.separator + "classes"), SimulatorProject.classesDir(common));
        assertEquals(new File(common, "target"), SimulatorProject.buildDir(common));
    }

    @Test
    void antProjectKeepsTheMavenOutputPathsTheSimulatorAlwaysUsed(@TempDir Path tmp) throws IOException {
        File root = tmp.toFile();
        write(root, "codenameone_settings.properties", "codename1.mainName=App\n");
        write(root, "build.xml", "<project/>");
        ProjectLayout layout = SimulatorProject.locate(root);
        assertNotNull(layout);
        assertEquals(BuildSystem.ANT, layout.buildSystem());
        assertEquals(new File(root, "target" + File.separator + "classes"), SimulatorProject.classesDir(root));
    }

    @Test
    void searchIsNoWiderThanBefore(@TempDir Path tmp) throws IOException {
        File root = gradleProject(tmp.toFile());
        File deep = new File(root, "src/main/java");
        // Inside the project but not at its root, nor in a direct child of it.
        assertNull(SimulatorProject.locate(deep));
        File unrelated = new File(tmp.toFile(), "elsewhere/nested");
        unrelated.mkdirs();
        assertNull(SimulatorProject.locate(unrelated));
    }

    @Test
    void noProjectFallsBackToTheWorkingDirectory(@TempDir Path tmp) throws IOException {
        File dir = tmp.toFile();
        System.setProperty("user.dir", dir.getAbsolutePath());
        SimulatorProject.reset();
        assertNull(SimulatorProject.current());
        assertEquals(new File(dir.getAbsoluteFile(), "codenameone_settings.properties"),
                SimulatorProject.settingsFile());
        assertEquals(new File(dir, "target" + File.separator + "classes"), SimulatorProject.classesDir(dir));
    }
}
