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

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
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

        assertTrue(new File(classes, "com/example/UserCn1Mapper.class").exists());
        assertTrue(new File(classes, "cn1app/MapperBootstrap.class").exists());

        // Load both the fixture and the generated mapper through a single child
        // classloader so the generic bound on Mapper<User> resolves against the
        // same User class.
        try (URLClassLoader cl = childLoader(classes)) {
            Class<?> userCls = cl.loadClass("com.example.User");
            Class<?> mapperCls = cl.loadClass("com.example.UserCn1Mapper");
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
    public void enumFieldsRoundTripThroughJson() throws Exception {
        File classes = tmp.newFolder("classes");
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.Color",
                "package com.example;\n"
                        + "public enum Color { RED, GREEN, BLUE }\n");
        sources.put("com.example.Paint",
                "package com.example;\n"
                        + "import com.codename1.annotations.Mapped;\n"
                        + "import java.util.List;\n"
                        + "@Mapped public class Paint {\n"
                        + "    public Color primary;\n"
                        + "    public List<Color> palette;\n"
                        + "    public Paint() {}\n"
                        + "}\n");
        JavaSourceCompiler.compile(sources, classes, Arrays.asList(testClassesDir()));
        runProcessorOrFail(classes);

        try (URLClassLoader cl = childLoader(classes)) {
            Class<?> colorCls = cl.loadClass("com.example.Color");
            Class<?> paintCls = cl.loadClass("com.example.Paint");
            Class<?> mapperCls = cl.loadClass("com.example.PaintCn1Mapper");
            Object mapper = mapperCls.newInstance();
            Method valueOf = colorCls.getMethod("valueOf", String.class);
            Object red = valueOf.invoke(null, "RED");
            Object blue = valueOf.invoke(null, "BLUE");

            Object paint = paintCls.newInstance();
            paintCls.getField("primary").set(paint, red);
            List<Object> pal = new ArrayList<Object>();
            pal.add(red);
            pal.add(blue);
            paintCls.getField("palette").set(paint, pal);

            // toMap: enum -> name(); List<enum> -> List<String>.
            Method toMap = mapperCls.getMethod("toMap", paintCls);
            @SuppressWarnings("unchecked")
            Map<String, Object> json = (Map<String, Object>) toMap.invoke(mapper, paint);
            assertEquals("RED", json.get("primary"));
            assertEquals(Arrays.asList("RED", "BLUE"), json.get("palette"));

            // fromMap: name -> enum constant; list of names -> list of enums.
            Map<String, Object> in = new LinkedHashMap<String, Object>();
            in.put("primary", "GREEN");
            in.put("palette", Arrays.asList("BLUE", "RED"));
            Method fromMap = mapperCls.getMethod("fromMap", Map.class);
            Object restored = fromMap.invoke(mapper, in);
            assertEquals(valueOf.invoke(null, "GREEN"), paintCls.getField("primary").get(restored));
            List<?> rpal = (List<?>) paintCls.getField("palette").get(restored);
            assertEquals(blue, rpal.get(0));
            assertEquals(red, rpal.get(1));

            // Unknown enum names decode to null rather than throwing.
            Map<String, Object> bad = new LinkedHashMap<String, Object>();
            bad.put("primary", "MAGENTA");
            Object r2 = fromMap.invoke(mapper, bad);
            assertNull(paintCls.getField("primary").get(r2));
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
            Class<?> mapperCls = cl.loadClass("com.example.ItemCn1Mapper");
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

    /**
     * The direct JSON writer must produce byte-for-byte what the map path produces.
     *
     * That is Mapper.Direct's entire contract, and nothing was checking it: two
     * divergences shipped past review because every existing test exercises one path
     * or the other, never both against each other. A null list serialised as `null`
     * on the direct path and `[]` through the map, and enum elements went through
     * toString() rather than name() -- so an enum that overrides toString() produced
     * JSON that could not be read back at all.
     *
     * Asserting equality of the two paths rather than against a literal is deliberate:
     * it keeps holding when a new field kind is added, without anyone remembering to
     * come back and extend a hand-written expectation.
     */
    @Test
    public void directJsonMatchesTheMapPathExactly() throws Exception {
        File classes = tmp.newFolder("direct-parity-classes");
        Map<String, String> sources = new LinkedHashMap<String, String>();
        // toString() deliberately disagrees with name(): if the direct path uses the
        // wrong one, the two outputs differ and this test says so.
        sources.put("com.example.Shade",
                "package com.example;\n"
                        + "public enum Shade {\n"
                        + "    LIGHT, DARK;\n"
                        + "    @Override public String toString() { return \"shade-\" + name().toLowerCase(); }\n"
                        + "}\n");
        // A mapped base plus an UNMAPPED subclass: the polymorphic case where a
        // runtime-class mapper lookup finds nothing and falls back to toString(),
        // while the map path finds the mapper for the DECLARED type.
        sources.put("com.example.Base",
                "package com.example;\n"
                        + "import com.codename1.annotations.Mapped;\n"
                        + "@Mapped public class Base {\n"
                        + "    public String tag;\n"
                        + "    public Base() {}\n"
                        + "}\n");
        sources.put("com.example.Derived",
                "package com.example;\n"
                        + "public class Derived extends Base {\n"
                        + "    public Derived() {}\n"
                        + "    @Override public String toString() { return \"derived-tostring\"; }\n"
                        + "}\n");
        sources.put("com.example.Swatch",
                "package com.example;\n"
                        + "import com.codename1.annotations.Mapped;\n"
                        + "import com.codename1.annotations.JsonProperty;\n"
                        + "import com.codename1.properties.Property;\n"
                        + "import java.util.List;\n"
                        + "@Mapped public class Swatch {\n"
                        // Property<Date>: the map path stores the Date RAW, so
                        // JSONWriter renders its toString(). appendJsonValue would
                        // render epoch millis instead -- a silent wire change.
                        + "    public final Property<java.util.Date, Swatch> due = new Property<java.util.Date, Swatch>(\"due\");\n"
                        + "    public String name;\n"
                        + "    public int count;\n"
                        + "    public Shade shade;\n"
                        + "    public List<Shade> shades;\n"
                        + "    public List<String> tags;\n"
                        + "    public java.util.Date when;\n"
                        // A key needing JSON escaping, which escape() alone only made
                        // compile.
                        + "    @JsonProperty(\"od\\\"d\\\\key\") public String odd;\n"
                        // Declared as the mapped base, populated with the subclass.
                        + "    public Base ref;\n"
                        + "    public List<Base> refs;\n"
                        + "    public Swatch() {}\n"
                        + "}\n");
        JavaSourceCompiler.compile(sources, classes, Arrays.asList(testClassesDir()));
        runProcessorOrFail(classes);

        try (URLClassLoader cl = childLoader(classes)) {
            Class<?> shadeCls = cl.loadClass("com.example.Shade");
            Class<?> swatchCls = cl.loadClass("com.example.Swatch");
            Class<?> mapperCls = cl.loadClass("com.example.SwatchCn1Mapper");
            Object mapper = mapperCls.newInstance();
            Method valueOf = shadeCls.getMethod("valueOf", String.class);
            Object dark = valueOf.invoke(null, "DARK");

            // The generated mapper must actually BE on the direct path, or this test
            // compares the map path with itself and passes while proving nothing.
            Class<?> directCls = cl.loadClass("com.codename1.mapping.Mapper$Direct");
            assertTrue("the generated mapper should implement Mapper.Direct",
                    directCls.isInstance(mapper));

            Object populated = swatchCls.newInstance();
            swatchCls.getField("name").set(populated, "teal");
            swatchCls.getField("count").setInt(populated, 3);
            swatchCls.getField("shade").set(populated, dark);
            List<Object> shades = new ArrayList<Object>();
            shades.add(valueOf.invoke(null, "LIGHT"));
            shades.add(dark);
            swatchCls.getField("shades").set(populated, shades);
            swatchCls.getField("tags").set(populated, Arrays.asList("a", "b"));
            swatchCls.getField("when").set(populated, new java.util.Date(1234567890L));
            swatchCls.getField("odd").set(populated, "quoted");
            // Base's mapper has to be REGISTERED or the declared-type lookup finds
            // nothing and both paths fall back to toString() -- agreeing with each
            // other while proving nothing about the polymorphic case. Registering it
            // is what makes the two paths able to differ: the old code looked the
            // mapper up by the runtime class (Derived, unmapped -> toString), the new
            // code by the declared one (Base, mapped -> object).
            Class<?> mappersRegCls = cl.loadClass("com.codename1.mapping.Mappers");
            Class<?> mapperIface = cl.loadClass("com.codename1.mapping.Mapper");
            Object baseMapper = cl.loadClass("com.example.BaseCn1Mapper").newInstance();
            mappersRegCls.getMethod("register", mapperIface).invoke(null, baseMapper);

            Class<?> derivedCls = cl.loadClass("com.example.Derived");
            Object derived = derivedCls.newInstance();
            derivedCls.getField("tag").set(derived, "sub");
            swatchCls.getField("ref").set(populated, derived);
            List<Object> refs = new ArrayList<Object>();
            refs.add(derived);
            swatchCls.getField("refs").set(populated, refs);
            Object dueProp = swatchCls.getField("due").get(populated);
            dueProp.getClass().getMethod("set", Object.class)
                    .invoke(dueProp, new java.util.Date(99000L));

            // Every list left null: the case that diverged.
            Object empty = swatchCls.newInstance();

            String json = assertDirectMatchesMap(cl, mapperCls, mapper, populated);
            // Pinned individually: assertEquals reports only the FIRST difference, so
            // without these a single un-fixed case would mask the rest.
            assertTrue("the JSON key must be escaped, not emitted raw: " + json,
                    json.contains("\"od\\\"d\\\\key\":\"quoted\""));
            assertTrue("a declared-mapped field holding an unmapped subclass must "
                            + "serialise as an object, not toString(): " + json,
                    json.contains("\"ref\":{\"tag\":\"sub\"}"));
            assertTrue("the same applies to list elements: " + json,
                    json.contains("\"refs\":[{\"tag\":\"sub\"}]"));
            assertFalse("nothing should have fallen back to toString(): " + json,
                    json.contains("derived-tostring"));
            assertDirectMatchesMap(cl, mapperCls, mapper, empty);
        }
    }

    /** Both routes, on one instance, compared as text. Returns the agreed JSON. */
    private static String assertDirectMatchesMap(URLClassLoader cl, Class<?> mapperCls,
                                               Object mapper, Object instance) throws Exception {
        Class<?> writerCls = cl.loadClass("com.codename1.io.JSONWriter");

        Method toMap = mapperCls.getMethod("toMap", instance.getClass());
        Object asMap = toMap.invoke(mapper, instance);
        String viaMap = (String) writerCls.getMethod("toJson", Object.class).invoke(null, asMap);

        // The generated mapper's OWN direct writer, not Mappers.appendJson: that
        // goes through the registry, which this isolated classloader never
        // populates, so it would quietly fall back to toString() and compare the
        // map path against an object identity string.
        StringBuilder out = new StringBuilder();
        mapperCls.getMethod("toJson", instance.getClass(), StringBuilder.class)
                .invoke(mapper, instance, out);
        String viaDirect = out.toString();

        assertEquals("direct JSON must match the map path exactly", viaMap, viaDirect);
        return viaDirect;
    }

    private static File testClassesDir() throws Exception {
        URL url = MappingAnnotationProcessorTest.class.getProtectionDomain()
                .getCodeSource().getLocation();
        return new File(url.toURI());
    }
}
