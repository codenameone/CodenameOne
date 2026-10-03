/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.backend.HttpServer;
import com.codename1.backend.test.MockMvc;
import com.codename1.impl.backend.test.TestContext;
import com.codename1.impl.backend.test.TestContexts;
import com.codename1.impl.backend.test.TestEnvironment;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.ProcessorContext;
import com.codename1.build.BuildFailureException;
import com.codename1.build.SystemStreamLog;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// The test pass end to end on the JVM: an application and a `@BackendTest`
/// class compiled, the pass run over them, and the generated context started and
/// used the way the JUnit extension uses it.
public class BackendTestGeneratorTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final String MAIN = "package com.example;\n"
            + "import com.codename1.backend.annotations.*;\n";

    @After
    public void stopTheApplication() {
        TestContexts.shutdown();
    }

    private static Map<String, String> application() {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Clock", MAIN + "public interface Clock { long now(); }\n");
        s.put("com.example.SystemClock", MAIN + "@Component public class SystemClock implements Clock {\n"
                + "    public long now() { return System.currentTimeMillis(); }\n}\n");
        s.put("com.example.Greeter", MAIN + "@Component public class Greeter {\n"
                + "    public String greet(String name) { return \"Hello, \" + name; }\n}\n");
        s.put("com.example.Api", MAIN + "@RestController public class Api {\n"
                + "    private final Greeter greeter;\n"
                + "    private final Clock clock;\n"
                + "    public Api(Greeter greeter, Clock clock) { this.greeter = greeter; this.clock = clock; }\n"
                + "    @GetMapping(\"/greet/{name}\") public String greet(@PathVariable(\"name\") String name) {\n"
                + "        return greeter.greet(name);\n    }\n"
                + "    @GetMapping(\"/now\") public String now() { return String.valueOf(clock.now()); }\n"
                + "}\n");
        return s;
    }

    @Test
    public void aGeneratedContextStartsTheApplicationWithTheTestsBeans() throws Exception {
        File classes = mainBuild(application());
        Map<String, String> t = new LinkedHashMap<String, String>();
        t.put("com.example.ApiTest", "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "import com.codename1.backend.test.*;\n"
                + "import static com.codename1.backend.test.MockMvcRequestBuilders.*;\n"
                + "import static com.codename1.backend.test.MockMvcResultMatchers.*;\n"
                + "@BackendTest(properties = \"greeting.mode=test\")\n"
                + "public class ApiTest {\n"
                + "    @Autowired private MockMvc mvc;\n"
                + "    @Autowired private Greeter greeter;\n"
                + "    @LocalServerPort private int port;\n"
                + "    @TestConfiguration\n"
                + "    static class Fakes {\n"
                + "        @Bean @Primary public Clock fixedClock() {\n"
                + "            return new Clock() { public long now() { return 42; } };\n"
                + "        }\n"
                + "    }\n"
                + "    @org.junit.jupiter.api.Test\n"
                + "    void greets() throws Exception {\n"
                + "        mvc.perform(get(\"/greet/{name}\", \"Ada Lovelace\"))\n"
                + "                .andExpect(status().isOk())\n"
                + "                .andExpect(content().string(\"Hello, Ada Lovelace\"));\n"
                + "        mvc.perform(get(\"/now\")).andExpect(content().string(\"42\"));\n"
                + "        mvc.perform(get(\"/missing\")).andExpect(status().isNotFound());\n"
                + "        if (!\"Hello, Bo\".equals(greeter.greet(\"Bo\"))) throw new AssertionError(\"greeter\");\n"
                + "        if (port <= 0) throw new AssertionError(\"port \" + port);\n"
                + "    }\n"
                + "    @org.junit.jupiter.api.Test\n"
                + "    void failsLoudly() throws Exception {\n"
                + "        mvc.perform(get(\"/now\")).andExpect(content().string(\"43\"));\n"
                + "    }\n"
                + "}\n");
        File tests = testBuild(classes, t);
        URLClassLoader loader = new URLClassLoader(new URL[] {tests.toURI().toURL(),
                classes.toURI().toURL()}, getClass().getClassLoader());
        Object context = loader.loadClass("com.example.ApiTestCn1TestContext").newInstance();
        assertTrue(context instanceof TestContext);
        TestEnvironment env = TestContexts.acquire((TestContext) context);
        Object test = loader.loadClass("com.example.ApiTest").newInstance();
        ((TestContext) context).inject(test, env);
        invoke(test, "greets");
        try {
            invoke(test, "failsLoudly");
            fail("a failed expectation passed");
        } catch (AssertionError expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("expected:<43> but was:<42>"));
        }
        // A second class with the same configuration gets the same application.
        assertTrue(env == TestContexts.acquire((TestContext) loader
                .loadClass("com.example.ApiTestCn1TestContext").newInstance()));
    }

    @Test
    public void anInheritedBackendTestGivesEachSubclassItsContext() throws Exception {
        // As @SpringBootTest is: the annotation, the injected fields and the tests
        // on an abstract base; the concrete subclass is what runs.
        File classes = mainBuild(application());
        Map<String, String> t = new LinkedHashMap<String, String>();
        t.put("com.example.AbstractApiTest", "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "import com.codename1.backend.test.*;\n"
                + "import static com.codename1.backend.test.MockMvcRequestBuilders.*;\n"
                + "import static com.codename1.backend.test.MockMvcResultMatchers.*;\n"
                + "@BackendTest\n"
                + "public abstract class AbstractApiTest {\n"
                + "    @Autowired protected MockMvc mvc;\n"
                + "    @org.junit.jupiter.api.Test\n"
                + "    void greets() throws Exception {\n"
                + "        mvc.perform(get(\"/greet/{name}\", \"Ada\")).andExpect(content().string(\"Hello, Ada\"));\n"
                + "    }\n"
                + "}\n");
        t.put("com.example.ConcreteApiTest", "package com.example;\n"
                + "public class ConcreteApiTest extends AbstractApiTest {\n"
                + "}\n");
        File tests = testBuild(classes, t);
        assertFalse("the abstract base got a context of its own",
                new File(tests, "com/example/AbstractApiTestCn1TestContext.class").isFile());
        URLClassLoader loader = new URLClassLoader(new URL[] {tests.toURI().toURL(),
                classes.toURI().toURL()}, getClass().getClassLoader());
        try {
            Object context = loader.loadClass("com.example.ConcreteApiTestCn1TestContext").newInstance();
            TestEnvironment env = TestContexts.acquire((TestContext) context);
            Object test = loader.loadClass("com.example.ConcreteApiTest").newInstance();
            ((TestContext) context).inject(test, env);
            java.lang.reflect.Method greets = loader.loadClass("com.example.AbstractApiTest")
                    .getDeclaredMethod("greets");
            greets.setAccessible(true);
            greets.invoke(test);
        } finally {
            loader.close();
        }
    }

    @Test
    public void malformedInputToGeneratedBindingsIsA400() throws Exception {
        // A binary body that is not UTF-8 for a @RequestBody String, and a malformed
        // multipart body behind an optional @RequestParam: the client's errors, so
        // 400s from the generated guards rather than 500s from the argument list or
        // a handler run on a body nothing parsed.
        Map<String, String> app = application();
        app.put("com.example.Inputs", MAIN + "@RestController public class Inputs {\n"
                + "    @PostMapping(\"/text\") public String text(@RequestBody String body) {\n"
                + "        return body;\n    }\n"
                + "    @PostMapping(\"/form\") public String form(\n"
                + "            @RequestParam(value = \"a\", required = false) String a) {\n"
                + "        return \"ran:\" + a;\n    }\n"
                + "}\n");
        File classes = mainBuild(app);
        Map<String, String> t = new LinkedHashMap<String, String>();
        t.put("com.example.InputsTest", "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "import com.codename1.backend.test.*;\n"
                + "import static com.codename1.backend.test.MockMvcRequestBuilders.*;\n"
                + "import static com.codename1.backend.test.MockMvcResultMatchers.*;\n"
                + "@BackendTest\n"
                + "public class InputsTest {\n"
                + "    @Autowired MockMvc mvc;\n"
                + "    void check() throws Exception {\n"
                + "        mvc.perform(post(\"/text\").contentType(\"application/octet-stream\")\n"
                + "                .content(new byte[] {(byte) 0xff, (byte) 0xfe}))\n"
                + "                .andExpect(status().isBadRequest());\n"
                + "        mvc.perform(post(\"/text\").contentType(\"application/octet-stream\")\n"
                + "                .content(\"plain\")).andExpect(content().string(\"plain\"));\n"
                + "        mvc.perform(post(\"/form\").contentType(\"multipart/form-data\")\n"
                + "                .content(\"--x\\r\\n\\r\\nhi\\r\\n--x--\"))\n"
                + "                .andExpect(status().isBadRequest());\n"
                + "        mvc.perform(post(\"/form\").param(\"a\", \"1\"))\n"
                + "                .andExpect(content().string(\"ran:1\"));\n"
                + "    }\n"
                + "}\n");
        File tests = testBuild(classes, t);
        URLClassLoader loader = new URLClassLoader(new URL[] {tests.toURI().toURL(),
                classes.toURI().toURL()}, getClass().getClassLoader());
        try {
            Object context = loader.loadClass("com.example.InputsTestCn1TestContext").newInstance();
            TestEnvironment env = TestContexts.acquire((TestContext) context);
            Object test = loader.loadClass("com.example.InputsTest").newInstance();
            ((TestContext) context).inject(test, env);
            invoke(test, "check");
        } finally {
            loader.close();
        }
    }

    @Test
    public void onlyADirectlyNestedTestConfigurationAppliesToItsTest() throws Exception {
        // Helper.Fakes is nested one level deeper; it must not join OuterTest's
        // context, where its second Clock would make the injection ambiguous.
        File classes = mainBuild(application());
        Map<String, String> t = new LinkedHashMap<String, String>();
        t.put("com.example.OuterTest", "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "import com.codename1.backend.test.*;\n"
                + "@BackendTest\n"
                + "public class OuterTest {\n"
                + "    @Autowired Clock clock;\n"
                + "    static class Helper {\n"
                + "        @TestConfiguration\n"
                + "        static class Fakes {\n"
                + "            @Bean public Clock other() { return null; }\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        File tests = testBuild(classes, t);
        assertTrue("the outer test's context was not generated",
                new File(tests, "com/example/OuterTestCn1TestContext.class").isFile());
    }

    @Test
    public void testsSplitOverTwoOutputDirectoriesAreOneHierarchy() throws Exception {
        // A mixed Java and Kotlin Gradle test set: the @BackendTest base in one
        // compiler's output, the concrete test in the other's. The base's private
        // injected field is woven where its class file is.
        File classes = mainBuild(application());
        List<File> cp = new ArrayList<File>(classpath());
        cp.add(0, classes);
        File javaOut = tmp.newFolder();
        Map<String, String> base = new LinkedHashMap<String, String>();
        base.put("com.example.AbstractServedTest", "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "import com.codename1.backend.test.*;\n"
                + "import static com.codename1.backend.test.MockMvcRequestBuilders.*;\n"
                + "import static com.codename1.backend.test.MockMvcResultMatchers.*;\n"
                + "@BackendTest\n"
                + "public abstract class AbstractServedTest {\n"
                + "    @Autowired private MockMvc mvc;\n"
                + "    public void greets() throws Exception {\n"
                + "        mvc.perform(get(\"/greet/{name}\", \"Bo\")).andExpect(content().string(\"Hello, Bo\"));\n"
                + "    }\n"
                + "}\n");
        JavaSourceCompiler.compile(base, javaOut, cp);
        File kotlinOut = tmp.newFolder();
        Map<String, String> sub = new LinkedHashMap<String, String>();
        sub.put("com.example.ConcreteServedTest", "package com.example;\n"
                + "public class ConcreteServedTest extends AbstractServedTest {\n"
                + "}\n");
        List<File> subCp = new ArrayList<File>(cp);
        subCp.add(0, javaOut);
        JavaSourceCompiler.compile(sub, kotlinOut, subCp);
        List<String> elements = new ArrayList<String>();
        elements.add(javaOut.getAbsolutePath());
        for (File f : cp) {
            elements.add(f.getAbsolutePath());
        }
        BackendTests.process(Collections.singletonList(classes), kotlinOut,
                Collections.singletonList(javaOut), tmp.newFolder(), tmp.newFolder(),
                Collections.<String>emptyList(), "UTF-8", elements, false, null, new SystemStreamLog());
        assertTrue("the concrete test in the other directory got no context",
                new File(kotlinOut, "com/example/ConcreteServedTestCn1TestContext.class").isFile());
        URLClassLoader loader = new URLClassLoader(new URL[] {kotlinOut.toURI().toURL(),
                javaOut.toURI().toURL(), classes.toURI().toURL()}, getClass().getClassLoader());
        try {
            Object context = loader.loadClass("com.example.ConcreteServedTestCn1TestContext").newInstance();
            TestEnvironment env = TestContexts.acquire((TestContext) context);
            Object test = loader.loadClass("com.example.ConcreteServedTest").newInstance();
            ((TestContext) context).inject(test, env);
            loader.loadClass("com.example.AbstractServedTest").getMethod("greets").invoke(test);
        } finally {
            loader.close();
        }
    }

    @Test
    public void aTestThatReturnsAValueIsABuildError() throws Exception {
        Map<String, String> t = new LinkedHashMap<String, String>();
        t.put("t.ValueTest", "package t;\n"
                + "public class ValueTest {\n"
                + "    @org.junit.jupiter.api.Test int counts() { return 1; }\n"
                + "}\n");
        File tests = tmp.newFolder();
        List<File> cp = new ArrayList<File>(classpath());
        JavaSourceCompiler.compile(t, tests, cp);
        List<String> elements = new ArrayList<String>();
        elements.add(tests.getAbsolutePath());
        for (File f : cp) {
            elements.add(f.getAbsolutePath());
        }
        try {
            BackendTests.process(tmp.newFolder(), tests, tmp.newFolder(), tmp.newFolder(),
                    Collections.<String>emptyList(), "UTF-8", elements, true, new SystemStreamLog());
            fail("a value-returning test was accepted for the compiled run");
        } catch (BuildFailureException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("returns a value"));
        }
    }

    @Test
    public void anAmbiguousInjectionIsABuildError() throws Exception {
        File classes = mainBuild(application());
        Map<String, String> t = new LinkedHashMap<String, String>();
        t.put("com.example.BadTest", "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "import com.codename1.backend.test.*;\n"
                + "@BackendTest\n"
                + "public class BadTest {\n"
                + "    @Autowired Clock clock;\n"
                + "    @TestConfiguration\n"
                + "    static class Fakes {\n"
                + "        @Bean public Clock other() { return null; }\n"
                + "    }\n"
                + "}\n");
        try {
            testBuild(classes, t);
            fail("an injection two beans could fill was accepted");
        } catch (BuildFailureException expected) {
            // The application's own constructor injection is where it shows first:
            // the test bean is a second Clock, and neither is @Primary.
            assertTrue(expected.getMessage(), expected.getMessage().contains(
                    "could receive any of systemClock (com.example.SystemClock), other "
                    + "(com.example.Clock)"));
        }
    }

    @Test
    public void theCompiledRunnerRunsWhatJUnitWouldAndCanReachIt() throws Exception {
        // A base class in another package: its package-private test and lifecycle
        // method are reached through a generated helper in that package, and a
        // test the subclass overrides without @Test is not a test any more.
        Map<String, String> t = new LinkedHashMap<String, String>();
        t.put("base.Base", "package base;\n"
                + "import org.junit.jupiter.api.*;\n"
                + "public abstract class Base {\n"
                + "    public static java.util.List<String> calls = new java.util.ArrayList<String>();\n"
                + "    @BeforeEach void setUp() { calls.add(\"setUp\"); }\n"
                + "    @Test void inherited() { calls.add(\"inherited\"); }\n"
                + "    @Test public void overridden() { calls.add(\"base-overridden\"); }\n"
                + "    @Test void packagePrivate() { calls.add(\"base-package-private\"); }\n"
                + "}\n");
        t.put("t.Checks", "package t;\n"
                + "public interface Checks {\n"
                + "    @org.junit.jupiter.api.Test default void fromInterface() {\n"
                + "        base.Base.calls.add(\"interface\");\n"
                + "    }\n"
                + "}\n");
        t.put("t.Fast", "package t;\n"
                + "@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)\n"
                + "@java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)\n"
                + "@org.junit.jupiter.api.Test\n"
                + "public @interface Fast {\n"
                + "}\n");
        t.put("t.Skipped", "package t;\n"
                + "@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)\n"
                + "@java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)\n"
                + "@org.junit.jupiter.api.Test\n"
                + "@org.junit.jupiter.api.Disabled(\"not on this target\")\n"
                + "public @interface Skipped {\n"
                + "}\n");
        t.put("t.InterfaceOnlyTest", "package t;\n"
                + "public class InterfaceOnlyTest implements Checks {\n"
                + "}\n");
        t.put("t.GatedTest", "package t;\n"
                + "public class GatedTest {\n"
                + "    @org.junit.jupiter.api.BeforeAll static void gate() {\n"
                + "        org.junit.jupiter.api.Assumptions.assumeTrue(false, \"no database here\");\n"
                + "    }\n"
                + "    @org.junit.jupiter.api.Test void gated() { base.Base.calls.add(\"gated\"); }\n"
                + "}\n");
        t.put("t.SubTest", "package t;\n"
                + "public class SubTest extends base.Base {\n"
                + "    @Override public void overridden() { calls.add(\"not-a-test\"); }\n"
                // Not an override: Base.packagePrivate is package-private in another
                // package, so JUnit still runs the base test.
                + "    void packagePrivate() { calls.add(\"sub-package-private\"); }\n"
                + "    @org.junit.jupiter.api.Test void own() { calls.add(\"own\"); }\n"
                + "    @Fast void composed() { calls.add(\"composed\"); }\n"
                + "    @Skipped void composedDisabled() { calls.add(\"ran-disabled\"); }\n"
                + "}\n");
        File classes = tmp.newFolder();
        File tests = tmp.newFolder();
        List<File> cp = new ArrayList<File>(classpath());
        JavaSourceCompiler.compile(t, tests, cp);
        List<String> elements = new ArrayList<String>();
        elements.add(tests.getAbsolutePath());
        for (File f : cp) {
            elements.add(f.getAbsolutePath());
        }
        BackendTests.process(classes, tests, tmp.newFolder(), tmp.newFolder(),
                Collections.<String>emptyList(), "UTF-8", elements, true, new SystemStreamLog());
        assertTrue("a helper was generated in the base class's package",
                new File(tests, "base/BaseCn1TestAccess.class").isFile());
        List<java.net.URL> urls = new ArrayList<java.net.URL>();
        urls.add(tests.toURI().toURL());
        java.net.URLClassLoader loader = new java.net.URLClassLoader(
                urls.toArray(new java.net.URL[0]), getClass().getClassLoader());
        try {
            loader.loadClass("t.SubTestCn1TestRunner").getMethod("run").invoke(null);
            loader.loadClass("t.InterfaceOnlyTestCn1TestRunner").getMethod("run").invoke(null);
            java.lang.reflect.Field failed = com.codename1.impl.backend.test.TestRun.class
                    .getDeclaredField("failed");
            failed.setAccessible(true);
            int failuresBefore = failed.getInt(null);
            loader.loadClass("t.GatedTestCn1TestRunner").getMethod("run").invoke(null);
            assertEquals("an @BeforeAll assumption that does not hold is a skip, not a failure",
                    failuresBefore, failed.getInt(null));
            @SuppressWarnings("unchecked")
            List<String> calls = (List<String>) loader.loadClass("base.Base").getField("calls").get(null);
            assertTrue(calls.toString(), calls.contains("inherited"));
            assertTrue(calls.toString(), calls.contains("own"));
            assertTrue(calls.toString(), calls.contains("setUp"));
            assertFalse("an override without @Test ran as a test: " + calls,
                    calls.contains("not-a-test") || calls.contains("base-overridden"));
            assertFalse("a test ran although its @BeforeAll aborted: " + calls, calls.contains("gated"));
            assertTrue("a package-private base test a same-signature method in another package "
                    + "does not override was dropped: " + calls, calls.contains("base-package-private"));
            assertTrue("a test inherited from an interface did not run: " + calls,
                    calls.contains("interface"));
            assertTrue("a test marked with a composed @Test annotation did not run: " + calls,
                    calls.contains("composed"));
            assertFalse("a test disabled through a composed annotation ran: " + calls,
                    calls.contains("ran-disabled"));
        } finally {
            loader.close();
        }
    }

    private static void invoke(Object test, String name) throws Exception {
        java.lang.reflect.Method m = test.getClass().getDeclaredMethod(name);
        m.setAccessible(true);
        try {
            m.invoke(test);
        } catch (InvocationTargetException err) {
            if (err.getCause() instanceof Exception) {
                throw (Exception) err.getCause();
            }
            if (err.getCause() instanceof Error) {
                throw (Error) err.getCause();
            }
            throw err;
        }
    }

    /// Compiles and processes the application the way process-annotations does,
    /// wiring record included.
    private File mainBuild(Map<String, String> sources) throws Exception {
        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(sources, classes, classpath());
        Map<String, AnnotatedClass> index = ClassScanner.scan(classes);
        List<String> cp = new ArrayList<String>();
        for (File f : classpath()) {
            cp.add(f.getAbsolutePath());
        }
        ProcessorContext ctx = new ProcessorContext(classes, tmp.newFolder(), index,
                new SystemStreamLog(), tmp.newFolder(), new Properties(), null,
                Collections.<String>emptyList(), "UTF-8", cp);
        BackendBeanAnnotationProcessor beans = new BackendBeanAnnotationProcessor();
        RestControllerAnnotationProcessor proc = new RestControllerAnnotationProcessor();
        beans.start(ctx);
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (!cls.getClassAnnotations().isEmpty()) {
                proc.processClass(cls, ctx);
            }
        }
        beans.finish(ctx);
        proc.finish(ctx);
        if (ctx.hasErrors()) {
            fail("the processors reported errors: " + ctx.getErrors());
        }
        for (Map.Entry<String, byte[]> e : ctx.getEmittedResources().entrySet()) {
            File f = new File(classes, e.getKey());
            f.getParentFile().mkdirs();
            Files.write(f.toPath(), e.getValue());
        }
        return classes;
    }

    private File testBuild(File classes, Map<String, String> sources) throws Exception {
        File tests = tmp.newFolder();
        List<File> cp = new ArrayList<File>(classpath());
        cp.add(0, classes);
        JavaSourceCompiler.compile(sources, tests, cp);
        List<String> elements = new ArrayList<String>();
        for (File f : cp) {
            elements.add(f.getAbsolutePath());
        }
        BackendTests.process(classes, tests, tmp.newFolder(), tmp.newFolder(),
                Collections.<String>emptyList(), "UTF-8", elements, false, new SystemStreamLog());
        return tests;
    }

    private static List<File> classpath() throws Exception {
        List<File> out = new ArrayList<File>();
        out.add(jarOf(HttpServer.class));
        out.add(jarOf(MockMvc.class));
        out.add(jarOf(org.junit.jupiter.api.Test.class));
        return out;
    }

    private static File jarOf(Class<?> c) throws Exception {
        return new File(c.getProtectionDomain().getCodeSource().getLocation().toURI());
    }
}
