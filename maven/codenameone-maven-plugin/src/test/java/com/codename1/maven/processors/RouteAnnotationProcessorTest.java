/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.maven.processors;

import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.ProcessorContext;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// End-to-end test for `RouteAnnotationProcessor`. Compiles `@Route`-annotated
/// fixtures against the real `cn1-core` types on the plugin's classpath, runs
/// the processor, and verifies the structure of the generated `Routes` class
/// by reading its bytecode with ASM.
///
/// Bytecode inspection rather than runtime invocation: the processor itself
/// invokes `javac` (so a malformed generated source fails the build at
/// process-classes time), and reading the .class file we just wrote sidesteps
/// the JDK-8-on-Linux classloader-visibility issues that surface when a
/// child URL classloader tries to share static state with classes loaded
/// from the surefire classpath.
public class RouteAnnotationProcessorTest {

    private static final String ROUTES_INTERNAL = "com/codename1/router/generated/Routes";
    private static final String ROUTES_PATH = ROUTES_INTERNAL + ".class";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void classLevelRouteWithPathVariableProducesRoutesClass() throws Exception {
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
                        + "    }\n"
                        + "}\n");
        runProcessorOrFail(classes);

        RoutesIntrospection rx = readRoutes(classes);
        assertTrue("Routes.bootstrap should install via Navigation.setDispatcher",
                rx.bootstrapInstallsViaNavigation);
        assertTrue("dispatch should return Form",
                rx.dispatchReturnsForm);
        assertTrue("dispatch should construct com.example.Profile for the route",
                rx.instantiates("com/example/Profile"));
    }

    @Test
    public void methodLevelRouteFactoryProducesStaticInvoke() throws Exception {
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
        runProcessorOrFail(classes);

        RoutesIntrospection rx = readRoutes(classes);
        assertTrue(rx.bootstrapInstallsViaNavigation);
        assertTrue("dispatch should invoke com.example.AppRoutes.home",
                rx.invokesStatic("com/example/AppRoutes", "home"));
        assertTrue("dispatch should invoke com.example.AppRoutes.profile",
                rx.invokesStatic("com/example/AppRoutes", "profile"));
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
        assertFalse(new File(classes, ROUTES_PATH).exists());
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

    private void runProcessorOrFail(File classesDir) throws Exception {
        ProcessorContext ctx = runProcessor(classesDir, /*expectNoErrors*/ true);
        assertTrue("processor should write the Routes class to " + ROUTES_PATH,
                new File(classesDir, ROUTES_PATH).exists());
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

    /// ASM-based introspection of the generated Routes class. Captures the
    /// answers to the questions we want to assert in tests.
    private static RoutesIntrospection readRoutes(File classesDir) throws Exception {
        File routesFile = new File(classesDir, ROUTES_PATH);
        assertTrue("generated Routes.class missing: " + routesFile, routesFile.exists());
        byte[] bytes = Files.readAllBytes(routesFile.toPath());
        final RoutesIntrospection rx = new RoutesIntrospection();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                              String signature, String[] exceptions) {
                if ("bootstrap".equals(name)) {
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String mname,
                                                     String desc, boolean iface) {
                            if (opcode == Opcodes.INVOKESTATIC
                                    && "com/codename1/router/Navigation".equals(owner)
                                    && "setDispatcher".equals(mname)) {
                                rx.bootstrapInstallsViaNavigation = true;
                            }
                        }
                    };
                }
                if ("dispatch".equals(name)) {
                    if (descriptor != null && descriptor.endsWith(")Lcom/codename1/ui/Form;")) {
                        rx.dispatchReturnsForm = true;
                    }
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitTypeInsn(int opcode, String type) {
                            if (opcode == Opcodes.NEW) {
                                rx.newInstances.add(type);
                            }
                        }

                        @Override
                        public void visitMethodInsn(int opcode, String owner, String mname,
                                                     String desc, boolean iface) {
                            if (opcode == Opcodes.INVOKESTATIC) {
                                rx.staticInvokes.add(owner + "#" + mname);
                            }
                        }
                    };
                }
                return null;
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return rx;
    }

    private static void assertFalse(boolean condition) {
        org.junit.Assert.assertFalse(condition);
    }

    private static final class RoutesIntrospection {
        boolean bootstrapInstallsViaNavigation;
        boolean dispatchReturnsForm;
        final List<String> newInstances = new ArrayList<String>();
        final List<String> staticInvokes = new ArrayList<String>();

        boolean instantiates(String internalName) {
            return newInstances.contains(internalName);
        }

        boolean invokesStatic(String owner, String method) {
            return staticInvokes.contains(owner + "#" + method);
        }
    }
}
