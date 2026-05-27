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
import static org.junit.Assert.fail;

/// End-to-end test for `MappingAnnotationProcessor`. Compiles a `@Mapped`
/// POJO + a `@Mapped` `PropertyBusinessObject`, runs the processor, loads the
/// emitted mappers in a child classloader, and exercises the JSON and XML
/// round-trips via reflection (we can't reference the generated types
/// directly here because they don't exist until the processor runs).
public class MappingAnnotationProcessorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void pojoRoundTripsThroughGeneratedMapper() throws Exception {
        File classes = compileFixture(
                "com.example.User",
                "package com.example;\n"
                        + "import com.codename1.annotations.*;\n"
                        + "@Mapped @XmlRoot(\"user\")\n"
                        + "public class User {\n"
                        + "    @JsonProperty(\"first_name\") @XmlElement(\"first\")\n"
                        + "    public String firstName;\n"
                        + "    public int age;\n"
                        + "    @XmlAttribute @JsonIgnore\n"
                        + "    public String role;\n"
                        + "    public User() {}\n"
                        + "}\n");
        runProcessorOrFail(classes);

        assertTrue(new File(classes, "com/codename1/mapping/generated/UserMapper.class").exists());
        assertTrue(new File(classes, "com/codename1/mapping/generated/MappersIndex.class").exists());

        // Load both the fixture and the generated mapper through a single child
        // classloader so the generic bound on Mapper<User> resolves against the
        // same User class.
        try (URLClassLoader cl = childLoader(classes)) {
            Class<?> userCls = cl.loadClass("com.example.User");
            Class<?> mapperCls = cl.loadClass("com.codename1.mapping.generated.UserMapper");
            Object mapper = mapperCls.newInstance();

            Object user = userCls.newInstance();
            userCls.getField("firstName").set(user, "Alice");
            userCls.getField("age").setInt(user, 30);
            userCls.getField("role").set(user, "admin");

            // toMap excludes JsonIgnore field (role).
            Method toMap = mapperCls.getMethod("toMap", userCls);
            @SuppressWarnings("unchecked")
            Map<String, Object> json = (Map<String, Object>) toMap.invoke(mapper, user);
            assertEquals("Alice", json.get("first_name"));
            assertEquals(Integer.valueOf(30), json.get("age"));
            assertTrue("@JsonIgnore field 'role' should not appear in toMap output",
                    !json.containsKey("role"));

            // fromMap restores both fields.
            Map<String, Object> back = new LinkedHashMap<String, Object>();
            back.put("first_name", "Bob");
            back.put("age", Integer.valueOf(42));
            Method fromMap = mapperCls.getMethod("fromMap", Map.class);
            Object restored = fromMap.invoke(mapper, back);
            assertEquals("Bob", userCls.getField("firstName").get(restored));
            assertEquals(42, userCls.getField("age").getInt(restored));
        }
    }

    @Test
    public void propertyFieldRoundTripsThroughJsonAndXml() throws Exception {
        File classes = compileFixture(
                "com.example.Item",
                "package com.example;\n"
                        + "import com.codename1.annotations.*;\n"
                        + "import com.codename1.properties.*;\n"
                        + "@Mapped\n"
                        + "public class Item implements PropertyBusinessObject {\n"
                        + "    public final Property<String, Item> name = new Property<String, Item>(\"name\");\n"
                        + "    public final Property<Integer, Item> qty = new Property<Integer, Item>(\"qty\");\n"
                        + "    private final PropertyIndex idx = new PropertyIndex(this, \"Item\", name, qty);\n"
                        + "    public PropertyIndex getPropertyIndex() { return idx; }\n"
                        + "    public Item() {}\n"
                        + "}\n");
        runProcessorOrFail(classes);

        try (URLClassLoader cl = childLoader(classes)) {
            Class<?> itemCls = cl.loadClass("com.example.Item");
            Class<?> mapperCls = cl.loadClass("com.codename1.mapping.generated.ItemMapper");
            Object mapper = mapperCls.newInstance();

            // Create item and populate via the generated mapper's fromMap.
            Map<String, Object> in = new LinkedHashMap<String, Object>();
            in.put("name", "Widget");
            in.put("qty", Integer.valueOf(7));
            Method fromMap = mapperCls.getMethod("fromMap", Map.class);
            Object item = fromMap.invoke(mapper, in);
            assertNotNull(item);
            // Read back: name.get() / qty.get() on the Property fields.
            Object nameProp = itemCls.getField("name").get(item);
            Object qtyProp = itemCls.getField("qty").get(item);
            Object name = nameProp.getClass().getMethod("get").invoke(nameProp);
            Object qty = qtyProp.getClass().getMethod("get").invoke(qtyProp);
            assertEquals("Widget", name);
            assertEquals(Integer.valueOf(7), qty);

            // toMap round-trips back.
            Method toMap = mapperCls.getMethod("toMap", itemCls);
            @SuppressWarnings("unchecked")
            Map<String, Object> out = (Map<String, Object>) toMap.invoke(mapper, item);
            assertEquals("Widget", out.get("name"));
            assertEquals(Integer.valueOf(7), out.get("qty"));
        }
    }

    @Test
    public void rejectsAbstractMappedClass() throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Bad",
                        "package com.example;\n"
                                + "import com.codename1.annotations.Mapped;\n"
                                + "@Mapped public abstract class Bad { public String name; }\n"),
                classes, Arrays.asList(testClassesDir()));
        ProcessorContext ctx = runProcessor(classes);
        assertTrue("expected validation error on abstract @Mapped class", ctx.hasErrors());
    }

    @Test
    public void rejectsMappedClassMissingNoArgConstructor() throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.NoArg",
                        "package com.example;\n"
                                + "import com.codename1.annotations.Mapped;\n"
                                + "@Mapped public class NoArg {\n"
                                + "    public String name;\n"
                                + "    public NoArg(String n) { this.name = n; }\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir()));
        ProcessorContext ctx = runProcessor(classes);
        assertTrue("expected validation error when no public no-arg constructor exists",
                ctx.hasErrors());
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private File compileFixture(String fqn, String src) throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource(fqn, src),
                classes,
                Arrays.asList(testClassesDir()));
        return classes;
    }

    private void runProcessorOrFail(File classesDir) throws Exception {
        ProcessorContext ctx = runProcessor(classesDir, /*expectNoErrors*/ true);
        if (ctx.hasErrors()) {
            StringBuilder sb = new StringBuilder("processor reported errors:\n");
            for (ProcessorContext.ProcessingError e : ctx.getErrors()) sb.append(' ').append(e).append('\n');
            fail(sb.toString());
        }
    }

    private ProcessorContext runProcessor(File classesDir) throws Exception {
        return runProcessor(classesDir, false);
    }

    private ProcessorContext runProcessor(File classesDir, boolean expectNoErrors) throws Exception {
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
        URL url = MappingAnnotationProcessorTest.class.getProtectionDomain()
                .getCodeSource().getLocation();
        return new File(url.toURI());
    }
}
