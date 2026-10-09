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
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompileUnityMojoTest {

    private static void set(Object mojo, String field, Object value) throws Exception {
        Field f = mojo.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(mojo, value);
    }

    private static Artifact artifact(String id, String classifier, File file) {
        DefaultArtifact a = new DefaultArtifact("com.codenameone", id, "1.0", "compile", "jar", classifier,
                new DefaultArtifactHandler("jar"));
        a.setFile(file);
        return a;
    }

    /// The goal is bound in every application's pom. For one without
    /// `src/main/unity` it must touch nothing: the mojo here has no project,
    /// no repository system and no log, and any use of one would throw.
    @Test
    public void isANoOpWithoutUnitySources(@TempDir File dir) throws Exception {
        CompileUnityMojo mojo = new CompileUnityMojo();
        set(mojo, "unitySourceDir", new File(dir, "src/main/unity"));
        mojo.executeImpl();
        // A directory of that name that is not a Unity project is no more one.
        assertTrue(new File(dir, "src/main/unity/Assets").mkdirs());
        mojo.executeImpl();
        assertEquals(1, dir.list().length, "nothing was written beside src");
    }

    /// `-Dcn1.unity.skip` leaves a real project out of the build, again
    /// before anything is resolved.
    @Test
    public void skipLeavesARealProjectAlone(@TempDir File dir) throws Exception {
        File unity = new File(dir, "src/main/unity");
        assertTrue(new File(unity, "Assets").mkdirs());
        assertTrue(new File(unity, "ProjectSettings").mkdirs());
        CompileUnityMojo mojo = new CompileUnityMojo();
        set(mojo, "unitySourceDir", unity);
        set(mojo, "skipUnity", Boolean.TRUE);
        mojo.executeImpl();
        assertEquals(1, dir.list().length, "nothing was written beside src");
    }

    /// The reference assemblies are an artifact of the runtime's own id; the
    /// runtime jar is the one without a classifier, whichever comes first.
    @Test
    public void picksTheRuntimeJarAndNotItsReferences(@TempDir File dir) throws Exception {
        File jar = new File(dir, "runtime.jar");
        File refs = new File(dir, "references.jar");
        Files.write(jar.toPath(), new byte[0]);
        Files.write(refs.toPath(), new byte[0]);
        Artifact runtime = artifact("codenameone-unity-compat", null, jar);
        Artifact references = artifact("codenameone-unity-compat", "references", refs);
        Artifact core = artifact("codenameone-core", null, jar);
        assertSame(runtime, CompileUnityMojo.runtimeArtifact(Arrays.asList(core, references, runtime)));
        assertNull(CompileUnityMojo.runtimeArtifact(Arrays.asList(core, references)));
        assertNull(CompileUnityMojo.runtimeArtifact(Collections.emptyList()));
        // Unresolved, it is as good as absent: the builder then names the
        // missing dependency instead of failing on a null file.
        assertNull(CompileUnityMojo.runtimeArtifact(
                Collections.singletonList(artifact("codenameone-unity-compat", null, null))));
    }

    @Test
    public void importNeedsAProjectToImport() throws Exception {
        ImportUnityProjectMojo mojo = new ImportUnityProjectMojo();
        MojoFailureException e = assertThrows(MojoFailureException.class, mojo::executeImpl);
        assertTrue(e.getMessage().contains("-Dsource="), e.getMessage());
    }
}
