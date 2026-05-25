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
package com.codename1.maven.processors;

import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.ProcessorContext;
import com.codename1.router.Router;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// End-to-end test for the bytecode emitter.
///
/// 1. Compile two `@Route`-annotated fixture classes into a temp class dir.
/// 2. Scan that dir with `ClassScanner`.
/// 3. Run `RouteAnnotationProcessor` over the index.
/// 4. Write the emitted `RoutesIndex.class` (and dispatcher) back to disk.
/// 5. Load `RoutesIndex` via a child classloader rooted at the temp dir.
/// 6. Invoke `RoutesIndex.register()` and assert the **test stub** Router
///    recorded the expected patterns and that the builders return the right
///    Form instances.
public class RouteAnnotationProcessorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void emitsWorkingRoutesIndex() throws Exception {
        File classesDir = tmp.newFolder("classes");
        compileFixtures(classesDir);

        Map<String, AnnotatedClass> index = ClassScanner.scan(classesDir);
        assertTrue("expected fixtures to be present",
                index.containsKey("com/example/Home") && index.containsKey("com/example/Profile"));

        RouteAnnotationProcessor proc = new RouteAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classesDir, tmp.newFolder("stubs"),
                index, new SystemStreamLog());
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (intersects(proc.getAnnotationDescriptors(), cls.getClassAnnotations().keySet())) {
                proc.processClass(cls, ctx);
            }
        }
        proc.finish(ctx);

        assertNoErrors(ctx);
        assertEquals("processor should have emitted RoutesIndex + Builder",
                2, ctx.getEmittedClasses().size());

        // Write the emitted bytecode under classesDir so the child classloader
        // can resolve it on the file system.
        flushEmitted(ctx, classesDir);

        // Reset the stub Router so we observe ONLY the calls from register().
        Router.getInstance().reset();

        // Load RoutesIndex from a fresh classloader rooted at the temp dir
        // PLUS the plugin's test-classes (so the @Route / Form / Router stubs
        // resolve from the parent classloader).
        URLClassLoader cl = new URLClassLoader(
                new URL[] { classesDir.toURI().toURL() },
                RouteAnnotationProcessorTest.class.getClassLoader());
        try {
            Class<?> idx = Class.forName(
                    "com.codename1.router.generated.RoutesIndex", true, cl);
            Method register = idx.getDeclaredMethod("register");
            register.invoke(null);
        } finally {
            cl.close();
        }

        List<Router.Recorded> recorded = Router.getInstance().recorded;
        // TreeMap sort means /home, /profile/:id come back in pattern order.
        assertEquals(2, recorded.size());
        assertEquals("/home", recorded.get(0).pattern);
        assertEquals("/profile/:id", recorded.get(1).pattern);
        assertNotNull(recorded.get(0).builder);
        assertNotNull(recorded.get(1).builder);

        // Invoke each builder: home should build with no-arg ctor, profile
        // should build with the RouteContext ctor.
        com.codename1.router.RouteContext ctxValue =
                new com.codename1.router.RouteContext("/profile/:id");
        assertEquals("com.example.Home",
                recorded.get(0).builder.build(ctxValue).getClass().getName());
        assertEquals("com.example.Profile",
                recorded.get(1).builder.build(ctxValue).getClass().getName());
    }

    @Test
    public void rejectsNonFormSubclass() throws Exception {
        File classesDir = tmp.newFolder("classes");
        String src = "package com.example;\n"
                + "import com.codename1.annotations.Route;\n"
                + "@Route(\"/bad\")\n"
                + "public class NotForm {\n"
                + "  public NotForm() {}\n"
                + "}\n";
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.NotForm", src),
                classesDir,
                Arrays.asList(testClassesDir()));

        runProcessor(classesDir);
        // Run again to observe ctx — re-run because runProcessor returns void.
        ProcessorContext ctx = runProcessor(classesDir);
        assertTrue("expected validation error for non-Form @Route", ctx.hasErrors());
        boolean mentionsForm = false;
        for (ProcessorContext.ProcessingError e : ctx.getErrors()) {
            if (e.getMessage().contains("extend com.codename1.ui.Form")) {
                mentionsForm = true;
                break;
            }
        }
        assertTrue("error message should mention Form requirement", mentionsForm);
        assertEquals("no bytecode should be emitted when validation fails",
                0, ctx.getEmittedClasses().size());
    }

    @Test
    public void rejectsEmptyPattern() throws Exception {
        File classesDir = tmp.newFolder("classes");
        String src = "package com.example;\n"
                + "import com.codename1.annotations.Route;\n"
                + "import com.codename1.ui.Form;\n"
                + "@Route(\"\")\n"
                + "public class Empty extends Form {\n"
                + "  public Empty() {}\n"
                + "}\n";
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Empty", src),
                classesDir,
                Arrays.asList(testClassesDir()));

        ProcessorContext ctx = runProcessor(classesDir);
        assertTrue("empty @Route should be rejected", ctx.hasErrors());
    }

    @Test
    public void rejectsPatternMissingLeadingSlash() throws Exception {
        File classesDir = tmp.newFolder("classes");
        String src = "package com.example;\n"
                + "import com.codename1.annotations.Route;\n"
                + "import com.codename1.ui.Form;\n"
                + "@Route(\"home\")\n"
                + "public class Home extends Form {\n"
                + "  public Home() {}\n"
                + "}\n";
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Home", src),
                classesDir,
                Arrays.asList(testClassesDir()));

        ProcessorContext ctx = runProcessor(classesDir);
        assertTrue("missing-slash pattern must be rejected", ctx.hasErrors());
    }

    @Test
    public void rejectsDuplicatePatternAcrossClasses() throws Exception {
        File classesDir = tmp.newFolder("classes");
        Map<String, String> sources = new HashMap<String, String>();
        sources.put("com.example.A",
                "package com.example; import com.codename1.annotations.Route; import com.codename1.ui.Form;\n"
                        + "@Route(\"/dup\") public class A extends Form { public A() {} }\n");
        sources.put("com.example.B",
                "package com.example; import com.codename1.annotations.Route; import com.codename1.ui.Form;\n"
                        + "@Route(\"/dup\") public class B extends Form { public B() {} }\n");
        JavaSourceCompiler.compile(sources, classesDir, Arrays.asList(testClassesDir()));

        ProcessorContext ctx = runProcessor(classesDir);
        assertTrue("duplicate pattern must be rejected", ctx.hasErrors());
        boolean mentionsDuplicate = false;
        for (ProcessorContext.ProcessingError e : ctx.getErrors()) {
            if (e.getMessage().contains("duplicate @Route pattern")) {
                mentionsDuplicate = true;
                break;
            }
        }
        assertTrue(mentionsDuplicate);
    }

    @Test
    public void rejectsAbstractAnnotatedClass() throws Exception {
        File classesDir = tmp.newFolder("classes");
        String src = "package com.example;\n"
                + "import com.codename1.annotations.Route;\n"
                + "import com.codename1.ui.Form;\n"
                + "@Route(\"/x\")\n"
                + "public abstract class Abstr extends Form {\n"
                + "  public Abstr() {}\n"
                + "}\n";
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Abstr", src),
                classesDir,
                Arrays.asList(testClassesDir()));

        ProcessorContext ctx = runProcessor(classesDir);
        assertTrue("abstract @Route classes must be rejected", ctx.hasErrors());
    }

    @Test
    public void stubSourceIsEmitted() throws Exception {
        File classesDir = tmp.newFolder("classes");
        RouteAnnotationProcessor proc = new RouteAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classesDir, tmp.newFolder("stubs"),
                new LinkedHashMap<String, AnnotatedClass>(), new SystemStreamLog());
        proc.emitStubs(ctx);
        String stub = ctx.getEmittedStubSources()
                .get("com/codename1/router/generated/RoutesIndex");
        assertNotNull("stub source must be emitted", stub);
        assertTrue(stub.contains("public final class RoutesIndex"));
        assertTrue(stub.contains("public static void register()"));
    }

    // ------------------------------------------------------------------------
    // Test helpers
    // ------------------------------------------------------------------------

    private static void assertNoErrors(ProcessorContext ctx) {
        if (!ctx.hasErrors()) return;
        StringBuilder sb = new StringBuilder("unexpected processor errors:\n");
        for (ProcessorContext.ProcessingError e : ctx.getErrors()) {
            sb.append("  ").append(e).append('\n');
        }
        fail(sb.toString());
    }

    private ProcessorContext runProcessor(File classesDir) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classesDir);
        RouteAnnotationProcessor proc = new RouteAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classesDir, tmp.newFolder(),
                index, new SystemStreamLog());
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (intersects(proc.getAnnotationDescriptors(), cls.getClassAnnotations().keySet())) {
                proc.processClass(cls, ctx);
            }
        }
        proc.finish(ctx);
        return ctx;
    }

    private void compileFixtures(File classesDir) throws Exception {
        Map<String, String> sources = new HashMap<String, String>();
        sources.put("com.example.Home",
                "package com.example;\n"
                        + "import com.codename1.annotations.Route;\n"
                        + "import com.codename1.ui.Form;\n"
                        + "@Route(\"/home\")\n"
                        + "public class Home extends Form {\n"
                        + "  public Home() {}\n"
                        + "}\n");
        sources.put("com.example.Profile",
                "package com.example;\n"
                        + "import com.codename1.annotations.Route;\n"
                        + "import com.codename1.router.RouteContext;\n"
                        + "import com.codename1.ui.Form;\n"
                        + "@Route(\"/profile/:id\")\n"
                        + "public class Profile extends Form {\n"
                        + "  public Profile() {}\n"
                        + "  public Profile(RouteContext ctx) {}\n"
                        + "}\n");
        JavaSourceCompiler.compile(sources, classesDir, Arrays.asList(testClassesDir()));
    }

    private static void flushEmitted(ProcessorContext ctx, File outRoot) throws Exception {
        for (Map.Entry<String, byte[]> e : ctx.getEmittedClasses().entrySet()) {
            File f = new File(outRoot, e.getKey() + ".class");
            File parent = f.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("could not create " + parent);
            }
            FileOutputStream fos = new FileOutputStream(f);
            try {
                fos.write(e.getValue());
            } finally {
                fos.close();
            }
        }
    }

    private static boolean intersects(java.util.Set<String> a, java.util.Set<String> b) {
        for (String s : a) if (b.contains(s)) return true;
        return false;
    }

    private static File testClassesDir() throws Exception {
        URL url = RouteAnnotationProcessorTest.class.getProtectionDomain()
                .getCodeSource().getLocation();
        return new File(url.toURI());
    }
}
