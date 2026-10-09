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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What counts as a source change for the local builds' up-to-date checks
/// (the generated Android/Xcode project, the cached APK). A dependency counts:
/// a rebuilt common jar left the generated Android project holding the old
/// classes and "Sources have not changed" skipped regenerating it.
public class SourcesModificationTimeTest {

    @Test
    public void aNewerDependencyIsASourceChange(@TempDir Path tmp) throws Exception {
        File root = tmp.toFile();
        File common = new File(root, "common");
        File android = new File(root, "android");
        new File(common, "src/main/java").mkdirs();
        android.mkdirs();
        File settings = new File(common, "codenameone_settings.properties");
        Files.write(settings.toPath(), "codename1.packageName=x\n".getBytes("UTF-8"));
        File source = new File(common, "src/main/java/A.java");
        Files.write(source.toPath(), "class A {}\n".getBytes("UTF-8"));
        long old = System.currentTimeMillis() - 60000;
        source.setLastModified(old);
        settings.setLastModified(old);
        new File(common, "src/main/java").setLastModified(old);
        new File(common, "src/main").setLastModified(old);
        new File(common, "src").setLastModified(old);

        File jar = new File(root, "dep.jar");
        Files.write(jar.toPath(), new byte[] {1});
        long newer = old + 30000;
        jar.setLastModified(newer);

        MavenProject project = new MavenProject();
        project.setBuild(new org.apache.maven.model.Build());
        project.setFile(new File(android, "pom.xml"));
        project.getProperties().setProperty("codename1.platform", "android");
        OpenSettingsMojo mojo = new OpenSettingsMojo();
        mojo.project = project;

        assertTrue(mojo.getSourcesModificationTime() < newer, "precondition: the sources are older");
        DefaultArtifact dep = new DefaultArtifact("g", "dep", "1", Artifact.SCOPE_COMPILE, "jar", null,
                new DefaultArtifactHandler("jar"));
        dep.setFile(jar);
        project.setArtifacts(Collections.<Artifact>singleton(dep));
        assertEquals(jar.lastModified(), mojo.getSourcesModificationTime());
        // The common module's own check is unaffected.
        assertTrue(mojo.getSourcesModificationTime(true) < newer);
    }
}
