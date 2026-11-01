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

import com.codename1.backend.HttpServer;
import com.codename1.build.SystemStreamLog;
import com.codename1.maven.annotations.JavaSourceCompiler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// A server wires the controllers, beans and entities of a backend library as
/// its own; see [BackendLibraries].
public class BackendLibrariesTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final String LIBRARY_CONTROLLER = "package com.lib;\n"
            + "import com.codename1.backend.annotations.*;\n"
            + "@RestController\n"
            + "@RequestMapping(\"/lib\")\n"
            + "public class LibNotes {\n"
            + "    @GetMapping(\"/ping\")\n"
            + "    public String ping() { return \"pong\"; }\n"
            + "}\n";

    private static final String LIBRARY_ENTITY = "package com.lib;\n"
            + "import com.codename1.annotations.*;\n"
            + "@Entity(table=\"lib_note\")\n"
            + "public class LibNote { @Id public long id; public String text; }\n";

    private static final String LIBRARY_SERVICE = "package com.lib;\n"
            + "@com.codename1.backend.annotations.Component\n"
            + "public class LibService { public String name() { return \"lib\"; } }\n";

    private static final String APP_CONTROLLER = "package com.app;\n"
            + "import com.codename1.backend.annotations.*;\n"
            + "@RestController\n"
            + "@RequestMapping(\"/app\")\n"
            + "public class AppNotes {\n"
            + "    private final com.lib.LibService service;\n"
            + "    public AppNotes(com.lib.LibService service) { this.service = service; }\n"
            + "    @GetMapping(\"/hello\")\n"
            + "    public String hello() { return \"hi\"; }\n"
            + "}\n";

    @Test
    public void aLibrarysControllerAndEntityAreWiredIntoTheServer() throws Exception {
        File library = library(true);
        File classes = app(library);
        process(classes, library);

        assertTrue("library controller merged", new File(classes, "com/lib/LibNotes.class").isFile());
        assertTrue("library router generated into the server",
                new File(classes, "com/lib/LibNotesRouter.class").isFile());
        assertTrue("the server's own router", new File(classes, "com/app/AppNotesRouter.class").isFile());
        assertTrue("library entity registered with the server's dao bootstrap",
                contains(new File(classes, "cn1app/BackendDaoBootstrap.class"), "com/lib/LibNote"));
        assertFalse("the marker describes the library, not the server",
                new File(classes, BackendLibraries.MARKER).exists());
    }

    @Test
    public void anUnmarkedJarIsLeftOnTheClasspath() throws Exception {
        File library = library(false);
        File classes = app(library);
        try {
            process(classes, library);
            fail("an unmarked jar's @Component was wired");
        } catch (com.codename1.build.BuildFailureException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("com.lib.LibService"));
        }
        assertFalse(new File(classes, "com/lib/LibNotes.class").exists());
        assertFalse(new File(classes, "com/lib/LibNotesRouter.class").exists());
    }

    @Test
    public void theModulesOwnClassWins() throws Exception {
        File library = library(true);
        File classes = app(library);
        File own = new File(classes, "com/lib/LibNote.class");
        mkdirs(own.getParentFile());
        byte[] mine = "mine".getBytes(StandardCharsets.UTF_8);
        Files.write(own.toPath(), mine);
        BackendLibraries.merge(Collections.singletonList(library.getAbsolutePath()), classes);
        assertTrue(Arrays.equals(mine, Files.readAllBytes(own.toPath())));
        // Merged again, the module's file is still the module's.
        BackendLibraries.merge(Collections.singletonList(library.getAbsolutePath()), classes);
        assertTrue(Arrays.equals(mine, Files.readAllBytes(own.toPath())));
    }

    @Test
    public void aClassTheModuleCompiledOverAMergedOneIsKept() throws Exception {
        File library = library(true);
        File classes = app(library);
        List<String> cp = Collections.singletonList(library.getAbsolutePath());
        BackendLibraries.merge(cp, classes);
        File merged = new File(classes, "com/lib/LibNote.class");
        assertTrue(merged.isFile());
        // The module adds its own class of that name; javac writes over the copy.
        byte[] mine = "mine".getBytes(StandardCharsets.UTF_8);
        Files.write(merged.toPath(), mine);
        BackendLibraries.merge(cp, classes);
        assertTrue("the module's class survives the undo of the last merge",
                Arrays.equals(mine, Files.readAllBytes(merged.toPath())));
        BackendLibraries.merge(cp, classes);
        assertTrue(Arrays.equals(mine, Files.readAllBytes(merged.toPath())));
    }

    @Test
    public void aMergedClassTheBuildRewroteIsStillTheLibrarys() throws Exception {
        File library = library(true);
        File classes = app(library);
        List<String> cp = Collections.singletonList(library.getAbsolutePath());
        BackendLibraries.merge(cp, classes);
        File merged = new File(classes, "com/lib/LibNote.class");
        byte[] original = Files.readAllBytes(merged.toPath());
        // Weaving rewrites the copy in place; the build then restamps.
        Files.write(merged.toPath(), "woven".getBytes(StandardCharsets.UTF_8));
        BackendLibraries.restamp(classes);
        BackendLibraries.merge(cp, classes);
        assertTrue("the next merge replaces it with the library's class",
                Arrays.equals(original, Files.readAllBytes(merged.toPath())));
    }

    @Test
    public void aDroppedLibraryLeavesNothingBehind() throws Exception {
        File library = library(true);
        File classes = app(library);
        assertEquals(1, BackendLibraries.merge(Collections.singletonList(library.getAbsolutePath()), classes));
        assertTrue(new File(classes, "com/lib/LibNotes.class").isFile());
        assertEquals(0, BackendLibraries.merge(Collections.<String>emptyList(), classes));
        assertFalse(new File(classes, "com/lib/LibNotes.class").exists());
        assertFalse(new File(classes, BackendLibraries.RECORD).exists());
        assertTrue("the server's own classes stay", new File(classes, "com/app/AppNotes.class").isFile());
    }

    @Test
    public void aLibraryModuleIsNotProcessedItself() throws Exception {
        File classes = tmp.newFolder();
        compile(classes, "com.lib.LibNotes", LIBRARY_CONTROLLER);
        File marker = new File(classes, BackendLibraries.MARKER);
        mkdirs(marker.getParentFile());
        Files.write(marker.toPath(), new byte[0]);
        process(classes, null);
        assertFalse(new File(classes, "com/lib/LibNotesRouter.class").exists());
    }

    private File appSources;

    private File app(File library) throws Exception {
        File classes = tmp.newFolder();
        List<File> classpath = new ArrayList<File>(backendClasspath());
        classpath.add(library);
        JavaSourceCompiler.compile(Collections.singletonMap("com.app.AppNotes", APP_CONTROLLER),
                classes, classpath);
        appSources = tmp.newFolder();
        File source = new File(appSources, "com/app/AppNotes.java");
        mkdirs(source.getParentFile());
        Files.write(source.toPath(), APP_CONTROLLER.getBytes(StandardCharsets.UTF_8));
        return classes;
    }

    /// A library jar, marked as a backend library or not.
    private File library(boolean marked) throws Exception {
        File classes = tmp.newFolder();
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.lib.LibNotes", LIBRARY_CONTROLLER);
        sources.put("com.lib.LibNote", LIBRARY_ENTITY);
        sources.put("com.lib.LibService", LIBRARY_SERVICE);
        JavaSourceCompiler.compile(sources, classes, backendClasspath());
        File jar = new File(tmp.newFolder(), "library.jar");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(jar))) {
            if (marked) {
                out.putNextEntry(new ZipEntry(BackendLibraries.MARKER));
                out.closeEntry();
            }
            add(out, classes, classes);
        }
        return jar;
    }

    private void compile(File classes, String name, String source) throws Exception {
        JavaSourceCompiler.compile(Collections.singletonMap(name, source), classes, backendClasspath());
    }

    private void process(File classes, File library) throws Exception {
        List<String> classpath = new ArrayList<String>();
        for (File f : backendClasspath()) {
            classpath.add(f.getAbsolutePath());
        }
        if (library != null) {
            classpath.add(library.getAbsolutePath());
        }
        // A real source root, as Maven passes one: with none, every class reads
        // as having its source, and a library class -- whose source is not the
        // module's -- would pass for the wrong reason.
        File sources = appSources != null ? appSources : tmp.newFolder();
        new AnnotationProcessing(new SystemStreamLog(), classes, tmp.newFolder(), tmp.newFolder(),
                new Properties(), null, Collections.singletonList(sources.getAbsolutePath()),
                "UTF-8", classpath)
                .withBackendLibraries(true)
                .run();
    }

    private static void add(ZipOutputStream out, File root, File dir) throws Exception {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                add(out, root, child);
                continue;
            }
            out.putNextEntry(new ZipEntry(root.toURI().relativize(child.toURI()).getPath()));
            try (FileInputStream in = new FileInputStream(child)) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) > 0) {
                    out.write(buffer, 0, n);
                }
            }
            out.closeEntry();
        }
    }

    private static boolean contains(File file, String text) throws Exception {
        return file.isFile() && new String(Files.readAllBytes(file.toPath()),
                StandardCharsets.ISO_8859_1).contains(text);
    }

    private static void mkdirs(File dir) {
        assertTrue(dir.isDirectory() || dir.mkdirs());
    }

    private static List<File> backendClasspath() throws Exception {
        URL url = HttpServer.class.getProtectionDomain().getCodeSource().getLocation();
        return Arrays.asList(new File(url.toURI()));
    }
}
