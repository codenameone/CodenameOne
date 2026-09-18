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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/// Records support for `MappingAnnotationProcessor`. Compiles a tiny
/// `record Pet(String name, int age) {}` annotated `@Mapped`, runs the
/// processor, then exercises the generated mapper through a child
/// classloader (we can't refer to the generated class directly because
/// it doesn't exist until the processor runs).
///
/// Records are a Java 16+ feature; on older JVMs the source-compile step
/// can't be invoked at all, so the test is skipped via JUnit Assume. The
/// fixture is also the one compile in the tree that asks JavaSourceCompiler
/// for a language newer than 8: its default is Java 8 bytecode because that
/// is what the translator reads, and a record cannot be expressed in it.
public class RecordMappingTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void recordRoundTripsThroughGeneratedMapper() throws Exception {
        assumeTrue("records require Java 16+", javaSpecAtLeast(16));

        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Pet",
                        "package com.example;\n"
                                + "import com.codename1.annotations.Mapped;\n"
                                + "@Mapped\n"
                                + "public record Pet(String name, int age) {}\n"),
                classes,
                Arrays.asList(testClassesDir()),
                // null: the running JDK's own level, which the assume above has
                // already established is 16 or newer.
                null);

        // Run the processor and capture the generated mapper source so we
        // can assert on its shape (accessor reads + canonical ctor call).
        ProcessorContext ctx = runProcessor(classes);
        assertTrue("processor reported errors: " + ctx.getErrors(), !ctx.hasErrors());

        File mapperClass = new File(classes, "com/example/PetCn1Mapper.class");
        assertTrue("generated mapper class should exist", mapperClass.exists());

        // Direct source-shape inspection: re-run the source-emit path
        // against the same scanned MappedClass so we can grep the literal
        // text. This is the contract the task asks us to lock in.
        MappingAnnotationProcessor proc = new MappingAnnotationProcessor();
        ProcessorContext ctx2 = new ProcessorContext(classes, tmp.newFolder(),
                ClassScanner.scan(classes), new SystemStreamLog());
        proc.start(ctx2);
        for (AnnotatedClass cls : ClassScanner.scan(classes).values()) {
            if (!cls.getClassAnnotations().isEmpty()) proc.processClass(cls, ctx2);
        }
        String generated = invokeGenerateMapperSource(proc);
        assertNotNull("expected generated source for Pet record", generated);
        assertTrue("fromMap should construct via canonical ctor: " + generated,
                generated.contains("new com.example.Pet(_name, _age)"));
        assertTrue("toMap should read via record accessor o.name(): " + generated,
                generated.contains("o.name()"));
        assertTrue("toMap should read via record accessor o.age(): " + generated,
                generated.contains("o.age()"));

        // End-to-end: load the generated mapper and round-trip a Pet.
        try (URLClassLoader cl = childLoader(classes)) {
            Class<?> petCls = cl.loadClass("com.example.Pet");
            Class<?> mapperCls = cl.loadClass("com.example.PetCn1Mapper");
            Object mapper = mapperCls.newInstance();

            // Construct a Pet via the canonical constructor.
            Object pet = petCls.getConstructor(String.class, int.class)
                    .newInstance("Fido", 4);

            Method toMap = mapperCls.getMethod("toMap", petCls);
            @SuppressWarnings("unchecked")
            Map<String, Object> json = (Map<String, Object>) toMap.invoke(mapper, pet);
            assertEquals("Fido", json.get("name"));
            assertEquals(Integer.valueOf(4), json.get("age"));

            Map<String, Object> in = new LinkedHashMap<String, Object>();
            in.put("name", "Rex");
            in.put("age", Integer.valueOf(7));
            Method fromMap = mapperCls.getMethod("fromMap", Map.class);
            Object restored = fromMap.invoke(mapper, in);
            assertNotNull(restored);
            // Read back via the synthesised record accessors.
            assertEquals("Rex", petCls.getMethod("name").invoke(restored));
            assertEquals(7, ((Integer) petCls.getMethod("age").invoke(restored)).intValue());
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /// Pulls the package-private `generateMapperSource(MappedClass)` out
    /// of `MappingAnnotationProcessor` via reflection so the test can
    /// grep the emitted source text. We do this so the assertion is
    /// against the source the processor would compile, not against the
    /// resulting `.class` bytecode (where field/method calls are harder
    /// to spot).
    private static String invokeGenerateMapperSource(MappingAnnotationProcessor proc) throws Exception {
        // Walk the proc's accepted map -- there is exactly one entry for
        // this test (com.example.Pet).
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
        URL url = RecordMappingTest.class.getProtectionDomain()
                .getCodeSource().getLocation();
        return new File(url.toURI());
    }

    private static boolean javaSpecAtLeast(int target) {
        String spec = System.getProperty("java.specification.version", "");
        try {
            // Java 9+ reports a major-only spec like "17"; Java 8 reports "1.8".
            if (spec.startsWith("1.")) {
                return Integer.parseInt(spec.substring(2)) >= target;
            }
            return Integer.parseInt(spec) >= target;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
}
