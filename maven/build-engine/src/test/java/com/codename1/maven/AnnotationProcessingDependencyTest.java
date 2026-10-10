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

import com.codename1.build.SystemStreamLog;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.JavaSourceCompiler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// The whole driver, with the processors the build really loads: a contract and
/// a transfer object that live in a library the module depends on are offered to
/// the processors that asked for them, and what those generate lands in the
/// module being built -- never in the dependency.
public class AnnotationProcessingDependencyTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void aDependencysContractAndTransferObjectAreGeneratedIntoTheModule() throws Exception {
        File shared = compileShared();
        File classes = compileApplication();
        String[] before = listing(shared);

        run(classes, Arrays.asList(classes.getPath(), shared.getPath(), clientRuntime().getPath()));

        assertTrue("the client of the dependency's contract",
                new File(classes, "com/example/shared/NotesApiImpl.class").isFile());
        assertTrue(new File(classes, "cn1app/RestClientBootstrap.class").isFile());
        assertTrue("the mapper of the dependency's transfer object",
                new File(classes, "com/example/shared/NoteCn1Mapper.class").isFile());
        assertTrue(new File(classes, "cn1app/MapperBootstrap.class").isFile());
        AnnotatedClass client = ClassScanner.readClass(
                new File(classes, "com/example/shared/NotesApiImpl.class"));
        assertTrue(client.getInterfaceInternalNames().contains("com/example/shared/NotesApi"));
        assertEquals("a dependency is somebody else's output and is never written to",
                Arrays.asList(before), Arrays.asList(listing(shared)));
    }

    /// The second build of the module sees its own first build's output on its
    /// classpath. That is not "a dependency already generated these".
    @Test
    public void theSecondBuildGeneratesThemAgain() throws Exception {
        File shared = compileShared();
        File classes = compileApplication();
        List<String> classpath = Arrays.asList(classes.getPath(), shared.getPath(),
                clientRuntime().getPath());
        run(classes, classpath);
        File client = new File(classes, "com/example/shared/NotesApiImpl.class");
        File bootstrap = new File(classes, "cn1app/RestClientBootstrap.class");
        assertTrue(client.delete());
        assertTrue(bootstrap.delete());
        // What is left -- the mapper, its bootstrap -- is the previous build's.

        run(classes, classpath);

        assertTrue(client.isFile());
        assertTrue(bootstrap.isFile());
        assertTrue(new File(classes, "com/example/shared/NoteCn1Mapper.class").isFile());
    }

    /// Without the client runtime the module is not an application, and code
    /// generated for one could not link in it.
    @Test
    public void aModuleWithoutTheClientRuntimeIsOfferedNothing() throws Exception {
        File shared = compileShared();
        File classes = compileApplication();

        run(classes, Arrays.asList(classes.getPath(), shared.getPath()));

        assertFalse(new File(classes, "com/example/shared/NotesApiImpl.class").exists());
        assertFalse(new File(classes, "com/example/shared/NoteCn1Mapper.class").exists());
        assertFalse(new File(classes, "cn1app").exists());
    }

    /// A module between the library and the application that generated the
    /// client already: the application must not define the same class twice.
    @Test
    public void whatADependencyAlreadyGeneratedIsNotGeneratedAgain() throws Exception {
        File shared = compileShared();
        File middle = tmp.newFolder("middle");
        Map<String, String> generated = new LinkedHashMap<String, String>();
        generated.put("com.example.shared.NotesApiImpl",
                "package com.example.shared;\npublic class NotesApiImpl {}\n");
        generated.put("com.example.shared.NoteCn1Mapper",
                "package com.example.shared;\npublic class NoteCn1Mapper {}\n");
        JavaSourceCompiler.compile(generated, middle, Arrays.asList(testClassesDir()));
        File classes = compileApplication();

        run(classes, Arrays.asList(classes.getPath(), middle.getPath(), shared.getPath(),
                clientRuntime().getPath()));

        assertFalse(new File(classes, "com/example/shared/NotesApiImpl.class").exists());
        assertFalse(new File(classes, "com/example/shared/NoteCn1Mapper.class").exists());
    }

    private void run(File classes, List<String> classpath) throws Exception {
        new AnnotationProcessing(new SystemStreamLog(), classes, tmp.newFolder(), tmp.newFolder(),
                new Properties(), null, Collections.<String>emptyList(), "UTF-8", classpath).run();
    }

    private File compileShared() throws Exception {
        File shared = tmp.newFolder("shared");
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.shared.Note",
                "package com.example.shared;\n"
                + "@com.codename1.annotations.Mapped\n"
                + "public class Note {\n"
                + "    public String title;\n"
                + "    public Note() {}\n"
                + "}\n");
        sources.put("com.example.shared.NotesApi",
                "package com.example.shared;\n"
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient\n"
                + "public interface NotesApi {\n"
                + "    @GET(\"/notes\")\n"
                + "    void list(OnComplete<Response<String>> callback);\n"
                + "}\n");
        JavaSourceCompiler.compile(sources, shared, Arrays.asList(testClassesDir()));
        return shared;
    }

    /// The module itself, which declares nothing annotated.
    private File compileApplication() throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.app.Main",
                        "package com.example.app;\npublic class Main {}\n"),
                classes, Arrays.asList(testClassesDir()));
        return classes;
    }

    private static String[] listing(File dir) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        collect(dir, dir, out);
        Collections.sort(out);
        return out.toArray(new String[0]);
    }

    private static void collect(File root, File dir, java.util.List<String> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collect(root, child, out);
            } else {
                out.add(root.toURI().relativize(child.toURI()).getPath() + ":" + child.length());
            }
        }
    }

    private static File clientRuntime() throws Exception {
        return new File(com.codename1.io.rest.Rest.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
    }

    private static File testClassesDir() throws Exception {
        return new File(AnnotationProcessingDependencyTest.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
    }
}
