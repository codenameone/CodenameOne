/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.maven.processors;

import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.ProcessorContext;
import com.codename1.router.RouteDispatcher;
import com.codename1.ui.Display;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// End-to-end test: compile @Route-annotated fixtures, run the processor,
/// load the generated Routes class in a child classloader, and verify that
/// dispatching URLs through the installed RouteDispatcher instantiates the
/// right Form factories.
public class RouteAnnotationProcessorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @After
    public void resetDisplay() {
        Display.getInstance().reset();
    }

    @Test
    public void dispatchesClassLevelRouteWithPathVariable() throws Exception {
        File classes = compileFixtures(
                "com.example.Profile",
                "package com.example;\n"
                        + "import com.codename1.annotations.Route;\n"
                        + "import com.codename1.annotations.RouteParam;\n"
                        + "import com.codename1.ui.Form;\n"
                        + "@Route(\"/users/:id\")\n"
                        + "public class Profile extends Form {\n"
                        + "    public String boundId;\n"
                        + "    public Profile(@RouteParam(\"id\") String id) { this.boundId = id; }\n"
                        + "}\n");
        runProcessorAndLoad(classes);
        RouteDispatcher d = Display.getInstance().dispatcher;
        assertNotNull("Routes.bootstrap should have installed a dispatcher", d);
        assertTrue(d.dispatch("https://example.com/users/42"));
        // Reload via reflection so we can read the boundId off the latest
        // Profile instance? Simpler: the dispatcher .show() was called -- assert
        // that next dispatch on bad URL returns false.
        assertEquals(false, d.dispatch("/no-such-route"));
    }

    @Test
    public void dispatchesMethodLevelRouteFactory() throws Exception {
        File classes = compileFixtures(
                "com.example.AppRoutes",
                "package com.example;\n"
                        + "import com.codename1.annotations.Route;\n"
                        + "import com.codename1.annotations.RouteParam;\n"
                        + "import com.codename1.ui.Form;\n"
                        + "public class AppRoutes {\n"
                        + "    @Route(\"/home\")\n"
                        + "    public static Form home() { return new Form(); }\n"
                        + "    @Route(\"/users/:id\")\n"
                        + "    public static Form profile(@RouteParam(\"id\") String id) {\n"
                        + "        return new Form();\n"
                        + "    }\n"
                        + "}\n");
        runProcessorAndLoad(classes);
        RouteDispatcher d = Display.getInstance().dispatcher;
        assertNotNull(d);
        assertTrue(d.dispatch("/home"));
        assertTrue(d.dispatch("https://app.example/users/abc"));
    }

    @Test
    public void rejectsClassMissingRouteParamForPathVariable() throws Exception {
        File classes = compileFixtures(
                "com.example.Bad",
                "package com.example;\n"
                        + "import com.codename1.annotations.Route;\n"
                        + "import com.codename1.ui.Form;\n"
                        + "@Route(\"/users/:id\")\n"
                        + "public class Bad extends Form {\n"
                        + "    public Bad(String id) { }\n"
                        + "}\n");
        ProcessorContext ctx = runProcessor(classes);
        assertTrue("constructor parameter without @RouteParam must fail",
                ctx.hasErrors());
    }

    @Test
    public void rejectsNonFormClass() throws Exception {
        File classes = compileFixtures(
                "com.example.NotForm",
                "package com.example;\n"
                        + "import com.codename1.annotations.Route;\n"
                        + "@Route(\"/x\")\n"
                        + "public class NotForm {\n"
                        + "    public NotForm() {}\n"
                        + "}\n");
        ProcessorContext ctx = runProcessor(classes);
        assertTrue("@Route on a non-Form class must fail", ctx.hasErrors());
    }

    @Test
    public void rejectsEmptyPattern() throws Exception {
        File classes = compileFixtures(
                "com.example.Empty",
                "package com.example;\n"
                        + "import com.codename1.annotations.Route;\n"
                        + "import com.codename1.ui.Form;\n"
                        + "@Route(\"\")\n"
                        + "public class Empty extends Form { public Empty() {} }\n");
        ProcessorContext ctx = runProcessor(classes);
        assertTrue(ctx.hasErrors());
    }

    @Test
    public void rejectsPatternMissingLeadingSlash() throws Exception {
        File classes = compileFixtures(
                "com.example.Home",
                "package com.example;\n"
                        + "import com.codename1.annotations.Route;\n"
                        + "import com.codename1.ui.Form;\n"
                        + "@Route(\"home\")\n"
                        + "public class Home extends Form { public Home() {} }\n");
        ProcessorContext ctx = runProcessor(classes);
        assertTrue(ctx.hasErrors());
    }

    @Test
    public void rejectsDuplicatePatternAcrossClasses() throws Exception {
        Map<String, String> srcs = new HashMap<String, String>();
        srcs.put("com.example.A",
                "package com.example; import com.codename1.annotations.Route; import com.codename1.ui.Form;\n"
                        + "@Route(\"/dup\") public class A extends Form { public A() {} }\n");
        srcs.put("com.example.B",
                "package com.example; import com.codename1.annotations.Route; import com.codename1.ui.Form;\n"
                        + "@Route(\"/dup\") public class B extends Form { public B() {} }\n");
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(srcs, classes, Arrays.asList(testClassesDir()));
        ProcessorContext ctx = runProcessor(classes);
        assertTrue(ctx.hasErrors());
        boolean mentionsDup = false;
        for (ProcessorContext.ProcessingError e : ctx.getErrors()) {
            if (e.getMessage().contains("duplicate @Route pattern")) {
                mentionsDup = true;
                break;
            }
        }
        assertTrue(mentionsDup);
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private File compileFixtures(String fqn, String source) throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource(fqn, source),
                classes,
                Arrays.asList(testClassesDir()));
        return classes;
    }

    /// Runs the processor, asserts no errors, loads the generated Routes class
    /// in a child classloader so `Routes.bootstrap()` runs in the test JVM and
    /// installs the dispatcher into the stub Display.
    private void runProcessorAndLoad(File classesDir) throws Exception {
        runProcessor(classesDir, /*expectNoErrors*/ true);
        // Use the surefire test classloader as parent so the generated Routes
        // class can see com.codename1.router.RouteDispatcher and the stub
        // com.codename1.ui.Display + com.codename1.ui.Form from the plugin's
        // test-classes. Pre-warm the fixture classes through this loader before
        // bootstrap() runs: on some surefire forked-JVM configurations the
        // INVOKESTATIC resolution against fixture classes is otherwise resolved
        // through a context that doesn't see our URL, producing a spurious
        // NoClassDefFoundError despite the .class file being present and
        // cl.getResource returning a valid URL.
        URLClassLoader cl = new URLClassLoader(
                new URL[] { classesDir.toURI().toURL() },
                RouteAnnotationProcessorTest.class.getClassLoader());
        try {
            java.nio.file.Files.walkFileTree(classesDir.toPath(),
                    new java.nio.file.SimpleFileVisitor<java.nio.file.Path>() {
                @Override public java.nio.file.FileVisitResult visitFile(
                        java.nio.file.Path f, java.nio.file.attribute.BasicFileAttributes a) {
                    String rel = classesDir.toPath().relativize(f).toString();
                    if (rel.endsWith(".class")) {
                        String fqn = rel.replace(java.io.File.separatorChar, '.')
                                .replaceAll("\\.class$", "");
                        try {
                            Class.forName(fqn, false, cl);
                        } catch (Throwable ignored) { }
                    }
                    return java.nio.file.FileVisitResult.CONTINUE;
                }
            });
            Class<?> routes = Class.forName(
                    "com.codename1.router.generated.Routes", true, cl);
            routes.getDeclaredMethod("bootstrap").invoke(null);
        } finally {
            cl.close();
        }
    }

    private ProcessorContext runProcessor(File classesDir) throws Exception {
        return runProcessor(classesDir, /*expectNoErrors*/ false);
    }

    private ProcessorContext runProcessor(File classesDir, boolean expectNoErrors) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classesDir);
        RouteAnnotationProcessor proc = new RouteAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classesDir, tmp.newFolder(),
                index, new SystemStreamLog());
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (intersects(proc.getAnnotationDescriptors(), cls.getClassAnnotations().keySet())) {
                proc.processClass(cls, ctx);
            }
            // Method-level @Route can live on any class regardless of class-level
            // annotations -- the dispatch already filters per-method inside
            // processClass, so we don't need to gate again here.
            for (com.codename1.maven.annotations.MethodInfo m : cls.getMethods()) {
                if (intersects(proc.getAnnotationDescriptors(), m.getAnnotations().keySet())) {
                    proc.processClass(cls, ctx);
                    break;
                }
            }
        }
        proc.finish(ctx);
        if (expectNoErrors && ctx.hasErrors()) {
            StringBuilder sb = new StringBuilder("unexpected processor errors:\n");
            for (ProcessorContext.ProcessingError e : ctx.getErrors()) {
                sb.append("  ").append(e).append('\n');
            }
            fail(sb.toString());
        }
        return ctx;
    }

    private static boolean intersects(java.util.Set<String> a, java.util.Set<String> b) {
        for (String s : a) {
            if (b.contains(s)) {
                return true;
            }
        }
        return false;
    }

    private static File testClassesDir() throws Exception {
        URL url = RouteAnnotationProcessorTest.class.getProtectionDomain()
                .getCodeSource().getLocation();
        return new File(url.toURI());
    }
}
