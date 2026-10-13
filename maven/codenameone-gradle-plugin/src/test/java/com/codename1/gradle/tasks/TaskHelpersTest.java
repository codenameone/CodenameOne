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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    /// The reference assemblies are published under the runtime's own artifact
    /// id, so a file name that only starts like the runtime's is not enough to
    /// go by: translating against the jar of assemblies finds no class in it.
    @Test
    void theUnityRuntimeIsNotMistakenForItsReferenceAssemblies() {
        assertTrue(CompileUnityTask.isRuntimeJar("codenameone-unity-compat-1.0.jar"));
        assertTrue(CompileUnityTask.isRuntimeJar("codenameone-unity-compat-1.0-20260102.030405-6.jar"));
        assertFalse(CompileUnityTask.isRuntimeJar("codenameone-unity-compat-1.0-references.jar"));
        assertFalse(CompileUnityTask.isRuntimeJar("codenameone-unity-compat-1.0-sources.jar"));
        assertFalse(CompileUnityTask.isRuntimeJar("codenameone-unity-compat-1.0.pom"));
        assertFalse(CompileUnityTask.isRuntimeJar("codenameone-core-1.0.jar"));

        File references = new File("repo/codenameone-unity-compat-1.0-references.jar");
        File runtime = new File("repo/codenameone-unity-compat-1.0.jar");
        assertEquals(runtime, CompileUnityTask.runtimeJar(Arrays.asList(
                new File("repo/codenameone-core-1.0.jar"), references, runtime)));
        assertNull(CompileUnityTask.runtimeJar(Collections.singletonList(references)));
    }

    /// `compileUnity` used to be skipped whenever `src/main/unity` held no
    /// project, which also skipped it on the build after the project was
    /// deleted: its generated sources stayed a source root and its classes
    /// stayed in the application's output until `clean`. It now has work while
    /// a directory it owns holds anything, and that work empties all three.
    @Test
    void aDeletedUnityProjectLeavesTheTaskWorkUntilItsOutputIsGone() throws Exception {
        File unity = new File(tmp.toFile(), "src/main/unity");
        File generated = new File(tmp.toFile(), "build/generated/sources/cn1-unity");
        File classes = new File(tmp.toFile(), "build/cn1-unity/classes");
        File state = new File(tmp.toFile(), "build/cn1-unity/state");
        // Never had one: nothing to do, and no directory is made to find out.
        assertFalse(CompileUnityTask.hasWork(unity, generated, classes, state));
        assertFalse(new File(tmp.toFile(), "build").exists());

        // What a build of a project leaves, the project itself now deleted.
        touch("build/generated/sources/cn1-unity/com/codename1/generated/unity/UnityAppImpl.java");
        touch("build/generated/sources/cn1-unity/a/b/Main.java");
        touch("build/cn1-unity/classes/global/Player.class");
        touch("build/cn1-unity/classes/sheet.png");
        touch("build/cn1-unity/classes/unrecorded/Old.class");
        touch("build/cn1-unity/state/unity/state.txt");
        touch("build/cn1-unity/state/unity/app/global/Player.class");
        Files.write(new File(state, "unity/installed.txt").toPath(),
                "global/Player.class\nsheet.png\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertTrue(CompileUnityTask.hasWork(unity, generated, classes, state));

        assertFalse(CompileUnityTask.build(new com.codename1.maven.UnityProjectBuilder(unity, generated, classes,
                state, null, null, Collections.<File>emptyList(), null, "a.b", "Main",
                Collections.<File>emptyList(), null), generated, classes, state));
        assertEquals(0, generated.list().length, "no generated source is left to compile");
        assertEquals(0, classes.list().length, "no class or image is left to package");
        assertEquals(0, state.list().length, "no state is left to match a project that comes back");
        assertFalse(CompileUnityTask.hasWork(unity, generated, classes, state), "and the next build skips the task");

        // Half a project is none, and is treated the same.
        touch("src/main/unity/Assets/Player.cs");
        touch("build/cn1-unity/classes/global/Player.class");
        assertTrue(CompileUnityTask.hasWork(unity, generated, classes, state));
        touch("src/main/unity/ProjectSettings/ProjectSettings.asset");
        assertTrue(CompileUnityTask.hasWork(unity, new File(tmp.toFile(), "none")), "a project is always work");
    }

    /// The task fails with the builder's own message when the project is there
    /// and the runtime is not, rather than translating against nothing: this is
    /// the path a build takes when the dependency was excluded by hand.
    @Test
    void aUnityProjectWithoutTheRuntimeIsRefusedByTheSharedBuilder() throws IOException {
        touch("unity/Assets/Player.cs");
        touch("unity/ProjectSettings/ProjectSettings.asset");
        File unity = new File(tmp.toFile(), "unity");
        assertTrue(com.codename1.maven.UnityProjectBuilder.isUnityProject(unity));
        assertFalse(com.codename1.maven.UnityProjectBuilder.isUnityProject(new File(unity, "Assets")));
        com.codename1.maven.UnityProjectBuilder builder = new com.codename1.maven.UnityProjectBuilder(unity,
                new File(tmp.toFile(), "generated"), new File(tmp.toFile(), "classes"),
                new File(tmp.toFile(), "state"), CompileUnityTask.runtimeJar(Collections.<File>emptyList()), null,
                Collections.<File>emptyList(), null, "a.b", "Main", Collections.<File>emptyList(), null);
        com.codename1.builders.BuildException ex = org.junit.jupiter.api.Assertions.assertThrows(
                com.codename1.builders.BuildException.class, builder::run);
        assertTrue(ex.getMessage().contains("codenameone-unity-compat"), ex.getMessage());
        assertFalse(new File(tmp.toFile(), "classes").exists(), "nothing is written before the runtime is found");
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
