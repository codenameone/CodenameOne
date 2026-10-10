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

import com.codename1.build.BuildFailureException;
import com.codename1.build.SystemStreamLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// `backendWebApp` and the half of `runBackend` that serves what it staged.
/// The Gradle API jar cannot build a real Project here, so the tasks' work is
/// in static methods and those are what is tested.
class BackendWebAppTaskTest {
    @TempDir
    Path tmp;

    private File zip(String path, String... namesAndContents) throws IOException {
        File file = new File(tmp.toFile(), path);
        file.getParentFile().mkdirs();
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
        try {
            for (int i = 0; i < namesAndContents.length; i += 2) {
                out.putNextEntry(new ZipEntry(namesAndContents[i]));
                out.write(namesAndContents[i + 1].getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        } finally {
            out.close();
        }
        return file;
    }

    private File write(String path, String text) throws IOException {
        File file = new File(tmp.toFile(), path);
        file.getParentFile().mkdirs();
        Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    /// A Gradle project has one build directory for every platform, so the
    /// bundle is the archive there that is an application -- not the source
    /// archive a cloud build left beside it.
    @Test
    void stagesTheBundleTheApplicationsBuildLeft() throws Exception {
        zip("app/build/sources.zip", "src/Main.java", "class Main {}");
        zip("app/build/MyApp.zip", "index.html", "<html>app</html>", "js/app.js", "run();");
        File output = new File(tmp.toFile(), "app/backend/build/webapp");

        int staged = BackendWebAppTask.stage(new SystemStreamLog(), null,
                new File(tmp.toFile(), "app/build"), output);

        assertEquals(2, staged);
        assertEquals("<html>app</html>", read(new File(output, "index.html")));
        assertEquals("run();", read(new File(output, "js/app.js")));
    }

    @Test
    void aGivenBundleIsStagedInPlaceOfTheApplicationsOwn() throws Exception {
        zip("app/build/MyApp.zip", "index.html", "<html>built here</html>");
        File elsewhere = zip("ci/bundle.zip", "index.html", "<html>built elsewhere</html>");
        File output = new File(tmp.toFile(), "out");

        BackendWebAppTask.stage(new SystemStreamLog(), elsewhere, new File(tmp.toFile(), "app/build"), output);

        assertEquals("<html>built elsewhere</html>", read(new File(output, "index.html")));
    }

    /// A backend at the root of its build has no application to build, and
    /// still hosts a bundle it is handed.
    @Test
    void aBackendOfItsOwnStagesOnlyAGivenBundle() throws Exception {
        File output = new File(tmp.toFile(), "out");
        BuildFailureException refused = assertThrows(BuildFailureException.class,
                () -> BackendWebAppTask.stage(new SystemStreamLog(), null, null, output));
        assertTrue(refused.getMessage().contains("-Pcn1.backend.webapp.bundle"), refused.getMessage());

        File unpacked = write("unpacked/index.html", "<html>dir</html>").getParentFile();
        assertEquals(1, BackendWebAppTask.stage(new SystemStreamLog(), unpacked, null, output));
        assertEquals("<html>dir</html>", read(new File(output, "index.html")));
    }

    @Test
    void aBuildThatLeftNoBundleIsReported() throws Exception {
        zip("app/build/sources.zip", "src/Main.java", "class Main {}");
        File build = new File(tmp.toFile(), "app/build");
        BuildFailureException refused = assertThrows(BuildFailureException.class,
                () -> BackendWebAppTask.stage(new SystemStreamLog(), null, build, new File(tmp.toFile(), "out")));
        assertTrue(refused.getMessage().contains(build.getPath()), refused.getMessage());
    }

    @Test
    void runBackendServesWhatWasStaged() throws Exception {
        File backend = new File(tmp.toFile(), "backend");
        File webApp = new File(backend, "build/webapp");
        assertTrue(webApp.mkdirs());
        // Nothing staged: the server is told nothing, and serves no app.
        assertNull(RunBackendTask.webAppOption(webApp, backend, Collections.<String, String>emptyMap(),
                Collections.<String>emptyList()));
        assertNull(RunBackendTask.webAppOption(null, backend, Collections.<String, String>emptyMap(),
                Collections.<String>emptyList()));

        write("backend/build/webapp/index.html", "<html></html>");
        assertEquals("-Dcn1.webapp.root=" + webApp.getAbsolutePath(), RunBackendTask.webAppOption(webApp, backend,
                Collections.<String, String>emptyMap(), Arrays.asList("-Xmx1g", "-Dcn1.profile=dev")));
    }

    /// A system property outranks the backend's own files, so supplying one
    /// would override a project that says where its application is.
    @Test
    void runBackendLeavesAConfiguredRootAlone() throws Exception {
        File backend = new File(tmp.toFile(), "backend");
        File webApp = write("backend/build/webapp/index.html", "<html></html>").getParentFile();

        assertNull(RunBackendTask.webAppOption(webApp, backend, Collections.<String, String>emptyMap(),
                Arrays.asList("-Xmx1g", "-Dcn1.webapp.root=/srv/app")));
        assertNull(RunBackendTask.webAppOption(webApp, backend,
                Collections.singletonMap("CN1_WEBAPP_ROOT", "/srv/app"), Collections.<String>emptyList()));

        write("backend/application.properties", "cn1.webapp.root=public\n");
        assertNull(RunBackendTask.webAppOption(webApp, backend, Collections.<String, String>emptyMap(),
                Collections.<String>emptyList()));
    }
}
