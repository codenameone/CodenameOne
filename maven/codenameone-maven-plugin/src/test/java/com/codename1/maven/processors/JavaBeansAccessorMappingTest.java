/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/// JavaBeans-accessor support for `MappingAnnotationProcessor`. Compiles a
/// POJO with private fields plus public `getX()` / `setX()` (and `isX()`
/// for booleans) accessors, runs the processor, and asserts that the
/// generated mapper:
///
/// - reads through the bean accessors (`o.getFirstName()`, `o.isActive()`)
///   rather than touching the (private) field directly;
/// - writes through the matching setters (`o.setFirstName(...)`,
///   `o.setActive(...)`).
///
/// The test also round-trips a real instance through `toMap` / `fromMap`
/// to lock in semantic correctness, not just textual shape.
public class JavaBeansAccessorMappingTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void privatePojoFieldsRouteThroughBeanAccessors() throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.User",
                        "package com.example;\n"
                                + "import com.codename1.annotations.Mapped;\n"
                                + "@Mapped\n"
                                + "public class User {\n"
                                + "    private String firstName;\n"
                                + "    private int age;\n"
                                + "    private boolean active;\n"
                                + "    public User() {}\n"
                                + "    public String getFirstName() { return firstName; }\n"
                                + "    public void setFirstName(String v) { firstName = v; }\n"
                                + "    public int getAge() { return age; }\n"
                                + "    public void setAge(int v) { age = v; }\n"
                                + "    public boolean isActive() { return active; }\n"
                                + "    public void setActive(boolean v) { active = v; }\n"
                                + "}\n"),
                classes,
                Arrays.asList(testClassesDir()));

        ProcessorContext ctx = runProcessor(classes);
        assertFalse("processor reported errors: " + ctx.getErrors(), ctx.hasErrors());

        File mapperClass = new File(classes, "com/example/UserCn1Mapper.class");
        assertTrue("generated mapper class should exist", mapperClass.exists());

        // Source-shape inspection: re-run the source-emit path against
        // the same scanned MappedClass so we can grep the literal text.
        MappingAnnotationProcessor proc = new MappingAnnotationProcessor();
        ProcessorContext ctx2 = new ProcessorContext(classes, tmp.newFolder(),
                ClassScanner.scan(classes), new SystemStreamLog());
        proc.start(ctx2);
        for (AnnotatedClass cls : ClassScanner.scan(classes).values()) {
            if (!cls.getClassAnnotations().isEmpty()) proc.processClass(cls, ctx2);
        }
        String generated = invokeGenerateMapperSource(proc);
        assertNotNull("expected generated source for User POJO", generated);

        // Reads route through bean getters.
        assertTrue("toMap should call o.getFirstName(): " + generated,
                generated.contains("o.getFirstName()"));
        assertTrue("toMap should call o.getAge(): " + generated,
                generated.contains("o.getAge()"));
        assertTrue("toMap should call o.isActive() for boolean: " + generated,
                generated.contains("o.isActive()"));

        // Writes route through bean setters.
        assertTrue("fromMap should call o.setFirstName(...): " + generated,
                generated.contains("o.setFirstName("));
        assertTrue("fromMap should call o.setAge(...): " + generated,
                generated.contains("o.setAge("));
        assertTrue("fromMap should call o.setActive(...): " + generated,
                generated.contains("o.setActive("));

        // Direct field access must NOT appear -- the fields are private.
        assertFalse("must not emit direct field access o.firstName: " + generated,
                generated.contains("o.firstName"));
        assertFalse("must not emit direct field access o.age: " + generated,
                generated.contains("o.age"));
        assertFalse("must not emit direct field access o.active: " + generated,
                generated.contains("o.active"));

        // End-to-end round-trip.
        try (URLClassLoader cl = childLoader(classes)) {
            Class<?> userCls = cl.loadClass("com.example.User");
            Class<?> mapperCls = cl.loadClass("com.example.UserCn1Mapper");
            Object mapper = mapperCls.newInstance();

            Object user = userCls.newInstance();
            userCls.getMethod("setFirstName", String.class).invoke(user, "Alice");
            userCls.getMethod("setAge", int.class).invoke(user, 31);
            userCls.getMethod("setActive", boolean.class).invoke(user, true);

            Method toMap = mapperCls.getMethod("toMap", userCls);
            @SuppressWarnings("unchecked")
            Map<String, Object> json = (Map<String, Object>) toMap.invoke(mapper, user);
            assertEquals("Alice", json.get("firstName"));
            assertEquals(Integer.valueOf(31), json.get("age"));
            assertEquals(Boolean.TRUE, json.get("active"));

            Map<String, Object> in = new LinkedHashMap<String, Object>();
            in.put("firstName", "Bob");
            in.put("age", Integer.valueOf(7));
            in.put("active", Boolean.FALSE);
            Method fromMap = mapperCls.getMethod("fromMap", Map.class);
            Object restored = fromMap.invoke(mapper, in);
            assertNotNull(restored);
            assertEquals("Bob", userCls.getMethod("getFirstName").invoke(restored));
            assertEquals(7, ((Integer) userCls.getMethod("getAge").invoke(restored)).intValue());
            assertEquals(Boolean.FALSE, userCls.getMethod("isActive").invoke(restored));
        }
    }

    // ---------------------------------------------------------------
    // Helpers (mirrors RecordMappingTest)
    // ---------------------------------------------------------------

    private static String invokeGenerateMapperSource(MappingAnnotationProcessor proc) throws Exception {
        java.lang.reflect.Field acceptedFld = MappingAnnotationProcessor.class.getDeclaredField("accepted");
        acceptedFld.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.TreeMap<String, Object> accepted = (java.util.TreeMap<String, Object>) acceptedFld.get(proc);
        Object mc = accepted.values().iterator().next();
        Method m = MappingAnnotationProcessor.class
                .getDeclaredMethod("generateMapperSource",
                        Class.forName("com.codename1.maven.processors.MappingAnnotationProcessor$MappedClass"));
        m.setAccessible(true);
        return (String) m.invoke(null, mc);
    }

    private ProcessorContext runProcessor(File classesDir) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classesDir);
        MappingAnnotationProcessor proc = new MappingAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classesDir, tmp.newFolder(),
                index, new SystemStreamLog());
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (!cls.getClassAnnotations().isEmpty()) proc.processClass(cls, ctx);
        }
        proc.finish(ctx);
        return ctx;
    }

    private URLClassLoader childLoader(File classesDir) throws Exception {
        URL[] urls = new URL[] {
                classesDir.toURI().toURL(),
                testClassesDir().toURI().toURL()
        };
        return new URLClassLoader(urls, getClass().getClassLoader());
    }

    private static File testClassesDir() throws Exception {
        URL url = JavaBeansAccessorMappingTest.class.getProtectionDomain()
                .getCodeSource().getLocation();
        return new File(url.toURI());
    }
}
