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

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// `cn1:backend-webapp` without the minute-long browser build: what it does
/// with a bundle it is handed, and when it does nothing at all.
class BackendWebAppMojoTest {
    @TempDir
    Path tmp;

    @Test
    void aBundleBuiltElsewhereIsStagedWithoutABuild() throws Exception {
        File out = tmp.resolve("backend/target/webapp").toFile();
        BackendWebAppMojo mojo = mojo(server("backend"), out);
        set(mojo, "bundle", bundle());

        mojo.execute();

        assertEquals("<html></html>", new String(
                Files.readAllBytes(new File(out, "index.html").toPath()), "UTF-8"));
        assertTrue(new File(out, "js/app.js").isFile());
    }

    @Test
    void aModuleThatIsNotAServerIsPassedOverInAReactor() throws Exception {
        MavenProject shared = new MavenProject();
        shared.setArtifactId("shared");
        shared.setFile(tmp.resolve("shared/pom.xml").toFile());
        File out = tmp.resolve("shared/target/webapp").toFile();
        BackendWebAppMojo mojo = mojo(shared, out);
        set(mojo, "bundle", bundle());
        set(mojo, "reactorProjects", Arrays.asList(shared, server("backend")));

        mojo.execute();

        assertFalse(out.exists(), "the shared library is not a server and hosts nothing");
    }

    @Test
    void aServerWithNoApplicationAboveItIsToldWhatToPass() throws Exception {
        // The directory above the module is not a Codename One project, and
        // running a build there would build whatever happens to be in it.
        BackendWebAppMojo mojo = mojo(server("backend"), tmp.resolve("backend/target/webapp").toFile());
        MojoFailureException refused = assertThrows(MojoFailureException.class, mojo::execute);
        assertTrue(refused.getMessage().contains("cn1.backend.webapp.appRoot"), refused.getMessage());
        assertTrue(refused.getMessage().contains("cn1.backend.webapp.bundle"), refused.getMessage());
    }

    @Test
    void theBuildItStartsDoesNotStartItAgain() throws Exception {
        File out = tmp.resolve("backend/target/webapp").toFile();
        BackendWebAppMojo mojo = mojo(server("backend"), out);
        set(mojo, "bundle", bundle());
        set(mojo, "nested", Boolean.TRUE);
        mojo.execute();
        assertFalse(out.exists());

        set(mojo, "nested", Boolean.FALSE);
        set(mojo, "skip", Boolean.TRUE);
        mojo.execute();
        assertFalse(out.exists());
    }

    private MavenProject server(String artifactId) {
        MavenProject project = new MavenProject();
        project.setArtifactId(artifactId);
        project.setFile(tmp.resolve(artifactId + "/pom.xml").toFile());
        Dependency runtime = new Dependency();
        runtime.setGroupId("com.codenameone");
        runtime.setArtifactId("codenameone-backend");
        project.getModel().addDependency(runtime);
        return project;
    }

    private static BackendWebAppMojo mojo(MavenProject project, File output) throws Exception {
        BackendWebAppMojo mojo = new BackendWebAppMojo();
        set(mojo, "project", project);
        set(mojo, "output", output);
        return mojo;
    }

    private File bundle() throws IOException {
        File file = tmp.resolve("app.zip").toFile();
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
        try {
            out.putNextEntry(new ZipEntry("index.html"));
            out.write("<html></html>".getBytes("UTF-8"));
            out.closeEntry();
            out.putNextEntry(new ZipEntry("js/app.js"));
            out.write("run();".getBytes("UTF-8"));
            out.closeEntry();
        } finally {
            out.close();
        }
        return file;
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }
}
