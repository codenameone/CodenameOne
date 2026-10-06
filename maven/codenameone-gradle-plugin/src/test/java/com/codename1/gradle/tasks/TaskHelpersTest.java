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
package com.codename1.gradle.tasks;

import com.codename1.project.BuildSystem;
import com.codename1.project.ProjectDescriptor;
import com.codename1.project.ProjectKind;
import com.codename1.project.ProjectLayout;
import com.codename1.project.ProjectLayouts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskHelpersTest {
    @TempDir
    Path tmp;

    private File touch(String path) throws IOException {
        File f = new File(tmp.toFile(), path);
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), new byte[0]);
        return f;
    }

    /// The Kotlin compliance pass knows the Java classes javac has not written
    /// yet by their files.
    @Test
    void javaTypesAreNamedByTheirFiles() throws IOException {
        touch("java/a/b/Main.java");
        touch("java/a/b/package-info.java");
        touch("java/Top.java");
        touch("java/a/notes.txt");
        assertEquals(new HashSet<String>(Arrays.asList("a/b/Main", "Top")),
                ComplianceAction.javaTypesUnder(new File(tmp.toFile(), "java")));
        assertEquals(Collections.emptySet(), ComplianceAction.javaTypesUnder(new File(tmp.toFile(), "missing")));
    }

    /// The GUI tools and the simulator read the project through this file, so it
    /// must describe the Gradle layout and name the app.
    @Test
    void theDescriptorDescribesTheGradleProject() throws IOException {
        File root = tmp.toFile();
        ProjectLayout layout = ProjectLayouts.of(BuildSystem.GRADLE, ProjectKind.APP, root, root);
        Properties effective = new Properties();
        effective.setProperty("codename1.mainName", " MyApp ");
        effective.setProperty("codename1.packageName", "com.acme");
        File file = new File(root, "build/codenameone/project.properties");
        PrepareSimulatorTask.writeDescriptor(layout, file, effective,
                Collections.singletonList(new File(root, "build/generated/sources/rad-views").getAbsolutePath()));

        ProjectDescriptor d = ProjectDescriptor.read(file);
        assertEquals("MyApp", d.get("mainName"));
        assertEquals("com.acme", d.get("packageName"));
        assertTrue(d.getAll("sourceRoot").contains(new File(root, "build/generated/sources/rad-views").getAbsolutePath()),
                String.valueOf(d.getAll("sourceRoot")));
        ProjectLayout back = d.toLayout();
        assertEquals(BuildSystem.GRADLE, back.buildSystem());
        assertEquals(layout.javaSourceDir(), back.javaSourceDir());
    }

    /// Java and Kotlin compile into separate directories; the same generated
    /// path in both means one language's registry is lost. The ORM enhancer's
    /// per-directory bookkeeping is the one legitimate overlap.
    @Test
    void theSameGeneratedPathInBothClassDirectoriesIsACollision() throws IOException {
        touch("java/cn1app/DaoBootstrap.class");
        touch("java/a/Main.class");
        touch("java/" + SplitOutputCheck.PER_DIRECTORY);
        touch("kotlin/cn1app/DaoBootstrap.class");
        touch("kotlin/a/Helper.class");
        touch("kotlin/" + SplitOutputCheck.PER_DIRECTORY);
        assertEquals(Collections.singletonList("cn1app/DaoBootstrap.class"), SplitOutputCheck.collisions(
                new File(tmp.toFile(), "java"), new File(tmp.toFile(), "kotlin")));
        assertEquals(Collections.emptyList(), SplitOutputCheck.collisions(
                new File(tmp.toFile(), "java"), new File(tmp.toFile(), "missing")));
    }

    /// A mixed backend with annotated classes in both languages: each processing
    /// pass writes its own entry point record, and the check refuses the pair
    /// rather than let one half-wired application win the classpath.
    @Test
    void twoBackendWiringsAreACollision() throws IOException {
        touch("java/META-INF/cn1-backend-main");
        touch("java/META-INF/cn1-backend-wiring");
        touch("kotlin/META-INF/cn1-backend-main");
        touch("kotlin/META-INF/cn1-backend-wiring");
        assertEquals(java.util.Arrays.asList("META-INF/cn1-backend-main", "META-INF/cn1-backend-wiring"),
                SplitOutputCheck.collisions(new File(tmp.toFile(), "java"), new File(tmp.toFile(), "kotlin")));
    }
}
