/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.maven.processors;

import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.ProcessorContext;
import com.codename1.router.Navigation;
import com.codename1.router.NavigationEntry;

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
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// End-to-end test: compile @Route-annotated fixtures, run the processor,
/// load the generated Routes class in a child classloader, and verify the
/// installed RouteDispatcher resolves URLs through Navigation.
public class RouteAnnotationProcessorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @After
    public void resetNavigation() {
        Navigation.resetForTest();
    }

    @Test
    public void classLevelRouteWithPathVariableIsDispatched() throws Exception {
        File classes = compileFixtures(
                "com.example.Profile",
                "package com.example;\n"
                        + "import com.codename1.annotations.Route;\n"
                        + "import com.codename1.annotations.RouteParam;\n"
                        + "import com.codename1.ui.Form;\n"
                        + "@Route(\"/users/:id\")\n"
                        + "public class Profile extends Form {\n"
                        + "    public final String boundId;\n"
                        + "    public Profile(@RouteParam(\"id\") String id) {\n"
                        + "        this.boundId = id;\n"
                        + "        setTitle(\"Profile \" + id);\n"
                        + "    }\n"
                        + "}\n");
        runProcessorAndBootstrap(classes);

        assertNotNull("Routes.bootstrap should install a dispatcher into Navigation",
                Navigation.getDispatcherForTest());

        assertTrue(Navigation.navigate("https://example.com/users/42"));
        NavigationEntry top = Navigation.getCurrent();
        assertNotNull(top);
        assertEquals("https://example.com/users/42", top.getPath());
        assertEquals("Profile 42", top.getTitle());
    }

    @Test
    public void methodLevelRouteFactoryIsDispatched() throws Exception {
        File classes = compileFixtures(
                "com.example.AppRoutes",
                "package com.example;\n"
                        + "import com.codename1.annotations.Route;\n"
                        + "import com.codename1.annotations.RouteParam;\n"
                        + "import com.codename1.ui.Form;\n"
                        + "public class AppRoutes {\n"
                        + "    @Route(\"/home\")\n"
                        + "    public static Form home() {\n"
                        + "        Form f = new Form();\n"
                        + "        f.setTitle(\"Home\");\n"
                        + "        return f;\n"
                        + "    }\n"
                        + "    @Route(\"/users/:id\")\n"
                        + "    public static Form profile(@RouteParam(\"id\") String id) {\n"
                        + "        Form f = new Form();\n"
                        + "        f.setTitle(\"User \" + id);\n"
                        + "        return f;\n"
                        + "    }\n"
                        + "}\n");
        runProcessorAndBootstrap(classes);

        assertTrue(Navigation.navigate("/home"));
        assertEquals("Home", Navigation.getCurrent().getTitle());

        assertTrue(Navigation.navigate("https://app.example/users/abc"));
        assertEquals("User abc", Navigation.getCurrent().getTitle());

        assertEquals(2, Navigation.getStack().size());
        assertEquals("/home", Navigation.getStack().get(0).getPath());
    }

    @Test
    public void navigationStackSupportsBackAndPopTo() throws Exception {
        File classes = compileFixtures(
                "com.example.Routes",
                "package com.example;\n"
                        + "import com.codename1.annotations.Route;\n"
                        + "import com.codename1.ui.Form;\n"
                        + "public class Routes {\n"
                        + "    @Route(\"/a\")\n"
                        + "    public static Form a() { Form f = new Form(); f.setTitle(\"A\"); return f; }\n"
                        + "    @Route(\"/b\")\n"
                        + "    public static Form b() { Form f = new Form(); f.setTitle(\"B\"); return f; }\n"
                        + "    @Route(\"/c\")\n"
                        + "    public static Form c() { Form f = new Form(); f.setTitle(\"C\"); return f; }\n"
                        + "}\n");
        runProcessorAndBootstrap(classes);

        Navigation.navigate("/a");
        Navigation.navigate("/b");
        NavigationEntry b = Navigation.getCurrent();
        Navigation.navigate("/c");

        assertEquals(3, Navigation.getStack().size());
        assertEquals("C", Navigation.getCurrent().getTitle());

        assertTrue(Navigation.back());
        assertEquals("B", Navigation.getCurrent().getTitle());
        assertSame(b, Navigation.getCurrent());

        NavigationEntry a = Navigation.getStack().get(0);
        assertTrue(Navigation.popTo(a));
        assertEquals(1, Navigation.getStack().size());
        assertEquals("A", Navigation.getCurrent().getTitle());

        assertFalse(Navigation.back());
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

    private void runProcessorAndBootstrap(File classesDir) throws Exception {
        runProcessor(classesDir, /*expectNoErrors*/ true);
        URLClassLoader cl = new URLClassLoader(
                new URL[] { classesDir.toURI().toURL() },
                RouteAnnotationProcessorTest.class.getClassLoader());
        try {
            // Pre-warm the loader for every fixture .class so the JVM's
            // INVOKESTATIC resolver in the generated Routes can resolve the
            // fixture targets. Some JDK 8 configurations refuse the find when
            // it happens lazily during dispatch, even though cl.getResource
            // points at the file -- prewalking the tree sidesteps that.
            java.nio.file.Files.walkFileTree(classesDir.toPath(),
                    new java.nio.file.SimpleFileVisitor<java.nio.file.Path>() {
                @Override
                public java.nio.file.FileVisitResult visitFile(java.nio.file.Path f,
                        java.nio.file.attribute.BasicFileAttributes a) {
                    String rel = classesDir.toPath().relativize(f).toString();
                    if (rel.endsWith(".class")) {
                        String fqn = rel.replace(java.io.File.separatorChar, '.')
                                .replaceAll("\\.class$", "");
                        try {
                            Class.forName(fqn, false, cl);
                        } catch (Throwable ignored) {
                            // best-effort
                        }
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
            proc.processClass(cls, ctx);
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

    private static File testClassesDir() throws Exception {
        URL url = RouteAnnotationProcessorTest.class.getProtectionDomain()
                .getCodeSource().getLocation();
        return new File(url.toURI());
    }
}
