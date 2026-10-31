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

import com.codename1.build.SystemStreamLog;
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

    /// A field typed as a nested class, a list of one, or a nested enum: the
    /// generated mapper must name them canonically (`Download.DownloadFile`),
    /// since javac does not resolve the binary `Download$DownloadFile` in source.
    @Test
    public void nestedClassAndEnumFieldsCompileAndRoundTrip() throws Exception {
        File classes = tmp.newFolder("classes");
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.Download",
                "package com.example;\n"
                        + "import com.codename1.annotations.Mapped;\n"
                        + "import java.util.List;\n"
                        + "@Mapped public class Download {\n"
                        + "    public DownloadFile main;\n"
                        + "    public List<DownloadFile> files;\n"
                        + "    public DownloadType type;\n"
                        + "    public List<DownloadType> types;\n"
                        + "    public Download() {}\n"
                        + "    @Mapped public static class DownloadFile {\n"
                        + "        public String name;\n"
                        + "        public DownloadFile() {}\n"
                        + "    }\n"
                        + "    public enum DownloadType { FULL, DELTA }\n"
                        + "}\n");
        JavaSourceCompiler.compile(sources, classes, Arrays.asList(testClassesDir()));
        runProcessorOrFail(classes);

        try (URLClassLoader cl = childLoader(classes)) {
            Class<?> dlCls = cl.loadClass("com.example.Download");
            Class<?> fileCls = cl.loadClass("com.example.Download$DownloadFile");
            Class<?> typeCls = cl.loadClass("com.example.Download$DownloadType");
            Class<?> mapperCls = cl.loadClass("com.example.DownloadCn1Mapper");
            Object mapper = mapperCls.newInstance();

            Map<String, Object> f1 = new LinkedHashMap<String, Object>();
            f1.put("name", "a.bin");
            Map<String, Object> f2 = new LinkedHashMap<String, Object>();
            f2.put("name", "b.bin");
            Map<String, Object> in = new LinkedHashMap<String, Object>();
            in.put("main", f1);
            in.put("files", Arrays.asList(f1, f2));
            in.put("type", "DELTA");
            in.put("types", Arrays.asList("FULL"));
            // Nested mappers resolve through the registry, so install them.
            Class.forName("com.example.Download$DownloadFileCn1Mapper", true, cl)
                    .getMethod("register").invoke(null);
            mapperCls.getMethod("register").invoke(null);
            Object d = mapperCls.getMethod("fromMap", Map.class).invoke(mapper, in);
            Object main = dlCls.getField("main").get(d);
            assertEquals("a.bin", fileCls.getField("name").get(main));
            List<?> files = (List<?>) dlCls.getField("files").get(d);
            assertEquals(2, files.size());
            assertEquals("b.bin", fileCls.getField("name").get(files.get(1)));
            Object delta = typeCls.getMethod("valueOf", String.class).invoke(null, "DELTA");
            assertEquals(delta, dlCls.getField("type").get(d));

            @SuppressWarnings("unchecked")
            Map<String, Object> out = (Map<String, Object>) mapperCls
                    .getMethod("toMap", dlCls).invoke(mapper, d);
            assertEquals("DELTA", out.get("type"));
            assertEquals(Arrays.asList("FULL"), out.get("types"));
        }
    }

    /// Properties inherited from an abstract, non-@Mapped base class are
    /// mapped too, both ways, alongside the subclass's own.
    @Test
    public void inheritedPropertiesFromPlainBaseRoundTrip() throws Exception {
        File classes = tmp.newFolder("classes");
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.ModelObject",
                "package com.example;\n"
                        + "import java.util.List;\n"
                        + "public abstract class ModelObject {\n"
                        + "    private String id;\n"
                        + "    private boolean active;\n"
                        + "    private List<String> personIds;\n"
                        + "    public String getId() { return id; }\n"
                        + "    public void setId(String v) { id = v; }\n"
                        + "    public boolean isActive() { return active; }\n"
                        + "    public void setActive(boolean v) { active = v; }\n"
                        + "    public List<String> getPersonIds() { return personIds; }\n"
                        + "    public void setPersonIds(List<String> v) { personIds = v; }\n"
                        + "}\n");
        sources.put("com.example.RealWorldApp",
                "package com.example;\n"
                        + "import com.codename1.annotations.Mapped;\n"
                        + "@Mapped public class RealWorldApp extends ModelObject {\n"
                        + "    private String url;\n"
                        + "    public RealWorldApp() {}\n"
                        + "    public String getUrl() { return url; }\n"
                        + "    public void setUrl(String v) { url = v; }\n"
                        + "}\n");
        JavaSourceCompiler.compile(sources, classes, Arrays.asList(testClassesDir()));
        runProcessorOrFail(classes);

        try (URLClassLoader cl = childLoader(classes)) {
            Class<?> appCls = cl.loadClass("com.example.RealWorldApp");
            Class<?> mapperCls = cl.loadClass("com.example.RealWorldAppCn1Mapper");
            Object mapper = mapperCls.newInstance();
            Map<String, Object> in = new LinkedHashMap<String, Object>();
            in.put("id", "a1");
            in.put("active", Boolean.TRUE);
            in.put("personIds", Arrays.asList("p1", "p2"));
            in.put("url", "https://example.com");
            Object app = mapperCls.getMethod("fromMap", Map.class).invoke(mapper, in);
            assertEquals("a1", appCls.getMethod("getId").invoke(app));
            assertEquals(Boolean.TRUE, appCls.getMethod("isActive").invoke(app));
            assertEquals(Arrays.asList("p1", "p2"), appCls.getMethod("getPersonIds").invoke(app));
            assertEquals("https://example.com", appCls.getMethod("getUrl").invoke(app));

            @SuppressWarnings("unchecked")
            Map<String, Object> out = (Map<String, Object>) mapperCls
                    .getMethod("toMap", appCls).invoke(mapper, app);
            assertEquals("a1", out.get("id"));
            assertEquals(Boolean.TRUE, out.get("active"));
            assertEquals(Arrays.asList("p1", "p2"), out.get("personIds"));
            assertEquals("https://example.com", out.get("url"));
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

    /// A `@Mapped` type in a library the application depends on is mapped in the
    /// application, and a type it refers to -- an enum, which carries no
    /// annotation and so is in no index -- is still recognised for what it is.
    @Test
    public void aDependencyClassIsMappedIntoTheModule() throws Exception {
        File shared = tmp.newFolder("shared");
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.shared.Level",
                "package com.example.shared;\npublic enum Level { LOW, HIGH }\n");
        sources.put("com.example.shared.Task",
                "package com.example.shared;\n"
                        + "import com.codename1.annotations.*;\n"
                        + "@Mapped\n"
                        + "public class Task {\n"
                        + "    public String title;\n"
                        + "    public Level level;\n"
                        + "    public Task() {}\n"
                        + "}\n");
        JavaSourceCompiler.compile(sources, shared, Arrays.asList(testClassesDir()));
        File classes = tmp.newFolder("classes");

        ProcessorContext ctx = runOverDependencies(classes, shared);

        assertFalse(String.valueOf(ctx.getErrors()), ctx.hasErrors());
        assertTrue(new File(classes, "com/example/shared/TaskCn1Mapper.class").exists());
        assertTrue(new File(classes, "cn1app/MapperBootstrap.class").exists());
        assertFalse("a dependency is never written to",
                new File(shared, "com/example/shared/TaskCn1Mapper.class").exists());

        URL[] urls = {classes.toURI().toURL(), shared.toURI().toURL(),
                testClassesDir().toURI().toURL()};
        try (URLClassLoader cl = new URLClassLoader(urls, getClass().getClassLoader())) {
            Class<?> taskCls = cl.loadClass("com.example.shared.Task");
            Class<?> mapperCls = cl.loadClass("com.example.shared.TaskCn1Mapper");
            Object mapper = mapperCls.newInstance();
            Map<String, Object> in = new LinkedHashMap<String, Object>();
            in.put("title", "Ship");
            in.put("level", "HIGH");
            Object task = mapperCls.getMethod("fromMap", Map.class).invoke(mapper, in);
            assertEquals("Ship", taskCls.getField("title").get(task));
            assertEquals("the enum is read by name, as one of the module's own is",
                    "HIGH", String.valueOf(taskCls.getField("level").get(task)));
            @SuppressWarnings("unchecked")
            Map<String, Object> out = (Map<String, Object>)
                    mapperCls.getMethod("toMap", taskCls).invoke(mapper, task);
            assertEquals("HIGH", out.get("level"));
        }
    }

    /// The module that owns the type, or one between it and this, mapped it
    /// already: a second mapper here would define one class twice in the build.
    @Test
    public void aDependencyClassAlreadyMappedIsLeftAlone() throws Exception {
        File shared = tmp.newFolder("shared");
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.shared.Task",
                "package com.example.shared;\n"
                        + "@com.codename1.annotations.Mapped\n"
                        + "public class Task {\n"
                        + "    public String title;\n"
                        + "    public Task() {}\n"
                        + "}\n");
        sources.put("com.example.shared.TaskCn1Mapper",
                "package com.example.shared;\npublic class TaskCn1Mapper {}\n");
        JavaSourceCompiler.compile(sources, shared, Arrays.asList(testClassesDir()));
        File classes = tmp.newFolder("classes");

        ProcessorContext ctx = runOverDependencies(classes, shared);

        assertFalse(String.valueOf(ctx.getErrors()), ctx.hasErrors());
        assertFalse(new File(classes, "com/example/shared/TaskCn1Mapper.class").exists());
    }

    /// Runs the processor the way the driver does for a module that declares
    /// nothing itself and depends on `shared`.
    private ProcessorContext runOverDependencies(File classes, File shared) throws Exception {
        File core = new File(com.codename1.mapping.Mappers.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        java.util.List<String> classpath = Arrays.asList(classes.getPath(), shared.getPath(),
                core.getPath());
        MappingAnnotationProcessor proc = new MappingAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classes, tmp.newFolder(),
                java.util.Collections.<String, AnnotatedClass>emptyMap(), new SystemStreamLog(),
                null, null, null, null, null, classpath);
        assertTrue(proc.acceptsDependencyClasses(ctx));
        assertFalse("a module without the mapper runtime is not offered them",
                proc.acceptsDependencyClasses(new ProcessorContext(classes, tmp.newFolder(),
                        java.util.Collections.<String, AnnotatedClass>emptyMap(),
                        new SystemStreamLog(), null, null, null, null, null,
                        Arrays.asList(classes.getPath(), shared.getPath()))));
        Map<String, AnnotatedClass> dependencies =
                com.codename1.maven.annotations.DependencyClasses.scan(
                        classpath, classes, proc.getAnnotationDescriptors());
        assertTrue(dependencies.containsKey("com/example/shared/Task"));
        ctx.setDependencyIndex(dependencies);
        JavaSourceCompiler.setProjectClasspath(Arrays.asList(shared, core));
        try {
            proc.start(ctx);
            for (AnnotatedClass cls : dependencies.values()) {
                proc.processClass(cls, ctx);
            }
            proc.finish(ctx);
        } finally {
            JavaSourceCompiler.clearProjectClasspath();
        }
        return ctx;
    }

    /// A JSON parser hands every number over as a `Double`, so the elements of a
    /// `List<Integer>` have to be converted and not cast. Cast, this threw
    /// ClassCastException here and, on ParparVM, where a cast is unchecked, left
    /// a `Double` in the list.
    @Test
    public void scalarListElementsAreConvertedFromWhatTheParserProduces() throws Exception {
        File classes = compileFixture(
                "com.example.Series",
                "package com.example;\n"
                        + "import com.codename1.annotations.*;\n"
                        + "import java.util.List;\n"
                        + "@Mapped public class Series {\n"
                        + "    public List<Integer> counts;\n"
                        + "    public List<Long> totals;\n"
                        + "    public List<Double> rates;\n"
                        + "    public List<String> names;\n"
                        + "    public List<Boolean> flags;\n"
                        + "    public Series() {}\n"
                        + "}\n");
        runProcessorOrFail(classes);
        try (URLClassLoader cl = childLoader(classes)) {
            Class<?> seriesCls = cl.loadClass("com.example.Series");
            Class<?> mapperCls = cl.loadClass("com.example.SeriesCn1Mapper");
            Object mapper = mapperCls.newInstance();
            Map<String, Object> json = new LinkedHashMap<String, Object>();
            json.put("counts", Arrays.<Object>asList(Double.valueOf(3), Long.valueOf(4), "five", null));
            json.put("totals", Arrays.<Object>asList(Double.valueOf(7)));
            json.put("rates", Arrays.<Object>asList(Long.valueOf(2), Double.valueOf(0.5)));
            json.put("names", Arrays.<Object>asList("a", null));
            json.put("flags", Arrays.<Object>asList(Boolean.TRUE, "yes"));
            Object restored = mapperCls.getMethod("fromMap", Map.class).invoke(mapper, json);
            assertEquals(Arrays.asList(Integer.valueOf(3), Integer.valueOf(4), null, null),
                    seriesCls.getField("counts").get(restored));
            assertEquals(Arrays.asList(Long.valueOf(7)), seriesCls.getField("totals").get(restored));
            assertEquals(Arrays.asList(Double.valueOf(2), Double.valueOf(0.5)),
                    seriesCls.getField("rates").get(restored));
            assertEquals(Arrays.asList("a", null), seriesCls.getField("names").get(restored));
            assertEquals(Arrays.asList(Boolean.TRUE, null), seriesCls.getField("flags").get(restored));
        }
    }

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
                        // Boxed primitives share their kind with the unboxed form, so
                        // only these can be null. Left unset on the "empty" instance.
                        + "    public Boolean flag;\n"
                        + "    public Character initial;\n"
                        + "    public Base ref;\n"
                        + "    public List<Base> refs;\n"
                        // No mapper for Object: the map path stores elements raw, so
                        // a number must stay a number rather than becoming a string.
                        + "    public List<Object> mixed;\n"
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
            swatchCls.getField("flag").set(populated, Boolean.TRUE);
            swatchCls.getField("initial").set(populated, Character.valueOf('x'));
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
            List<Object> mixed = new ArrayList<Object>();
            mixed.add(Integer.valueOf(5));
            mixed.add(Boolean.TRUE);
            mixed.add("s");
            swatchCls.getField("mixed").set(populated, mixed);
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
            assertTrue("an unmapped list element must keep its JSON type: " + json,
                    json.contains("\"mixed\":[5,true,\"s\"]"));
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

    // ---------------------------------------------------------------
    // JSON options shared with the backend codecs (#5995)
    // ---------------------------------------------------------------

    @Test
    public void namesAliasesRequiredStrictAndOmitNull() throws Exception {
        Map<String, String> src = new LinkedHashMap<String, String>();
        src.put("jx.names.Task", "package jx.names;\n"
                + "import com.codename1.annotations.*;\n"
                + "@Mapped @JsonStrict @JsonOmitNull public class Task {\n"
                + "    @JsonProperty(value = \"title\", aliases = {\"name\", \"label\"}) public String t;\n"
                + "    @JsonProperty(aliases = {\"old_due\"}) public String due;\n"
                + "    @JsonProperty(required = true) public String id;\n"
                + "    public Integer count;\n"
                + "    public int n;\n"
                + "    @JsonProperty(aliases = {\"when\"}) public java.util.Date at;\n"
                + "    public Task() {}\n"
                + "}\n");
        src.put("jx.names.Loose", "package jx.names;\n"
                + "@com.codename1.annotations.Mapped public class Loose {\n"
                + "    public String a;\n"
                + "    public Loose() {}\n"
                + "}\n");
        src.put("jx.names.Box", "package jx.names;\n"
                + "@com.codename1.annotations.Mapped public class Box {\n"
                + "    public java.util.List<Task> items;\n"
                + "    public Box() {}\n"
                + "}\n");
        try (URLClassLoader cl = buildAndRegister(src, "jx.names.Task", "jx.names.Loose", "jx.names.Box")) {
            Class<?> task = cl.loadClass("jx.names.Task");
            Object t = fromJson(cl, "{\"name\":\"x\",\"old_due\":\"d\",\"id\":null}", task);
            assertEquals("x", task.getField("t").get(t));
            assertEquals("d", task.getField("due").get(t));
            assertNull("explicit null satisfies required", task.getField("id").get(t));
            t = fromJson(cl, "{\"label\":\"l\",\"title\":\"a\",\"name\":\"b\",\"id\":\"1\"}", task);
            assertEquals("the primary name wins over aliases", "a", task.getField("t").get(t));

            assertEquals("$.id: required member is missing", failure(cl, "{\"title\":\"a\"}", task));
            assertEquals("$.zzz: unknown member", failure(cl, "{\"id\":\"1\",\"zzz\":1}", task));
            // A refused value is reported under the alias it came in, and a
            // date is checked against its month and the clock.
            for (String bad : new String[] {"2026-02-30", "2025-02-29", "2026-04-31",
                    "2026-10-25T24:30:00Z", "2026-10-25T10:00:60Z", "2026-10-25T10:00+99:00"}) {
                String why = failure(cl, "{\"id\":\"1\",\"when\":\"" + bad + "\"}", task);
                assertTrue(bad + " -> " + why, why.startsWith("$.when: "));
            }
            t = fromJson(cl, "{\"id\":\"1\",\"when\":\"2024-02-29T23:59:59Z\"}", task);
            assertEquals(1709251199000L, ((java.util.Date) task.getField("at").get(t)).getTime());
            Object loose = fromJson(cl, "{\"a\":\"v\",\"zzz\":1}", cl.loadClass("jx.names.Loose"));
            assertEquals("v", loose.getClass().getField("a").get(loose));
            assertEquals("$.items[2].id: required member is missing",
                    failure(cl, "{\"items\":[{\"id\":\"1\"},{\"id\":\"2\"},{}]}", cl.loadClass("jx.names.Box")));

            Object w = task.newInstance();
            task.getField("id").set(w, "i");
            assertEquals("{\"id\":\"i\",\"n\":0}", toJson(cl, w));
            task.getField("t").set(w, "x");
            task.getField("count").set(w, Integer.valueOf(3));
            assertEquals("{\"title\":\"x\",\"id\":\"i\",\"count\":3,\"n\":0}", toJson(cl, w));
            assertDirectMatchesMap(cl, cl.loadClass("jx.names.TaskCn1Mapper"),
                    cl.loadClass("jx.names.TaskCn1Mapper").newInstance(), w);
        }
    }

    @Test
    public void jsonFormatWritesStringsAndReadsBothShapes() throws Exception {
        Map<String, String> src = new LinkedHashMap<String, String>();
        src.put("jx.fmt.Ev", "package jx.fmt;\n"
                + "import com.codename1.annotations.*;\n"
                + "import java.util.Date;\n"
                + "@Mapped public class Ev {\n"
                + "    @JsonFormat(JsonFormat.Shape.STRING) public Date at;\n"
                + "    public Date plain;\n"
                + "    @JsonFormat(JsonFormat.Shape.NUMBER) public Date num;\n"
                + "    @JsonFormat(JsonFormat.Shape.STRING) public long big;\n"
                + "    @JsonFormat(JsonFormat.Shape.STRING) public Double ratio;\n"
                + "    public Ev() {}\n"
                + "}\n");
        try (URLClassLoader cl = buildAndRegister(src, "jx.fmt.Ev")) {
            Class<?> ev = cl.loadClass("jx.fmt.Ev");
            Object e = ev.newInstance();
            ev.getField("at").set(e, new java.util.Date(1700000000123L));
            ev.getField("num").set(e, new java.util.Date(5L));
            ev.getField("big").setLong(e, 12345678901234L);
            ev.getField("ratio").set(e, Double.valueOf(1.5));
            String json = toJson(cl, e);
            assertEquals("{\"at\":\"2023-11-14T22:13:20.123Z\",\"plain\":null,\"num\":5,"
                    + "\"big\":\"12345678901234\",\"ratio\":\"1.5\"}", json);
            assertDirectMatchesMap(cl, cl.loadClass("jx.fmt.EvCn1Mapper"),
                    cl.loadClass("jx.fmt.EvCn1Mapper").newInstance(), e);

            Object back = fromJson(cl, json, ev);
            assertEquals(new java.util.Date(1700000000123L), ev.getField("at").get(back));
            assertEquals(12345678901234L, ev.getField("big").getLong(back));
            assertEquals(Double.valueOf(1.5), ev.getField("ratio").get(back));

            back = fromJson(cl, "{\"at\":1700000000123,\"plain\":\"2023-11-14\","
                    + "\"num\":\"2023-11-15T00:13:20.123+02:00\",\"big\":7,\"ratio\":2}", ev);
            assertEquals(new java.util.Date(1700000000123L), ev.getField("at").get(back));
            assertEquals(new java.util.Date(1699920000000L), ev.getField("plain").get(back));
            assertEquals(new java.util.Date(1700000000123L), ev.getField("num").get(back));
            assertEquals(7L, ev.getField("big").getLong(back));
            assertEquals(Double.valueOf(2), ev.getField("ratio").get(back));
            assertTrue(failure(cl, "{\"at\":\"yesterday\"}", ev).startsWith("$.at: "));
        }
    }

    @Test
    public void adaptersOnFieldAndOnClass() throws Exception {
        Map<String, String> src = new LinkedHashMap<String, String>();
        src.put("jx.ad.Money", "package jx.ad;\n"
                + "public class Money { public final long cents; public Money(long c) { cents = c; } }\n");
        src.put("jx.ad.MoneyAdapter", "package jx.ad;\n"
                + "public class MoneyAdapter implements com.codename1.annotations.JsonTypeAdapter<Money> {\n"
                + "    public Object toJson(Money m) { return \"$\" + m.cents; }\n"
                + "    public Money fromJson(Object j) {\n"
                + "        if (!(j instanceof String) || !((String) j).startsWith(\"$\")) throw new IllegalArgumentException(\"bad money\");\n"
                + "        return new Money(Long.parseLong(((String) j).substring(1)));\n"
                + "    }\n"
                + "}\n");
        src.put("jx.ad.Point", "package jx.ad;\n"
                + "@com.codename1.annotations.JsonAdapter(PointAdapter.class)\n"
                + "public class Point { public int x, y; public Point(int x, int y) { this.x = x; this.y = y; } }\n");
        src.put("jx.ad.PointAdapter", "package jx.ad;\n"
                + "import java.util.*;\n"
                + "public class PointAdapter implements com.codename1.annotations.JsonTypeAdapter<Point> {\n"
                + "    public Object toJson(Point p) { return Arrays.asList(Integer.valueOf(p.x), Integer.valueOf(p.y)); }\n"
                + "    public Point fromJson(Object j) {\n"
                + "        List l = (List) j;\n"
                + "        return new Point(((Number) l.get(0)).intValue(), ((Number) l.get(1)).intValue());\n"
                + "    }\n"
                + "}\n");
        src.put("jx.ad.Order", "package jx.ad;\n"
                + "import com.codename1.annotations.*;\n"
                + "@Mapped public class Order {\n"
                + "    @JsonAdapter(MoneyAdapter.class) public Money total;\n"
                + "    public Point at;\n"
                + "    public java.util.List<Point> path;\n"
                + "    public Point[] marks;\n"
                + "    public Order() {}\n"
                + "}\n");
        try (URLClassLoader cl = buildAndRegister(src, "jx.ad.Order")) {
            Class<?> order = cl.loadClass("jx.ad.Order");
            String json = "{\"total\":\"$150\",\"at\":[1,2],\"path\":[[3,4]],\"marks\":[[5,6]]}";
            Object o = fromJson(cl, json, order);
            Object total = order.getField("total").get(o);
            assertEquals(150L, total.getClass().getField("cents").getLong(total));
            Object at = order.getField("at").get(o);
            assertEquals(2, at.getClass().getField("y").getInt(at));
            assertEquals(json, toJson(cl, o));
            assertDirectMatchesMap(cl, cl.loadClass("jx.ad.OrderCn1Mapper"),
                    cl.loadClass("jx.ad.OrderCn1Mapper").newInstance(), o);
            assertEquals("$.total: bad money", failure(cl, "{\"total\":\"150\"}", order));
            // The adapter decides what an explicit null is, as on the server.
            assertEquals("$.total: bad money", failure(cl, "{\"total\":null}", order));
        }
    }

    @Test
    public void jsonCreatorBuildsImmutableClasses() throws Exception {
        Map<String, String> src = new LinkedHashMap<String, String>();
        src.put("jx.cr.Imm", "package jx.cr;\n"
                + "import com.codename1.annotations.*;\n"
                + "@Mapped public class Imm {\n"
                + "    private final String name;\n"
                + "    private final int age;\n"
                + "    public final String tag;\n"
                + "    @JsonCreator public Imm(@JsonProperty(\"name\") String name,\n"
                + "            @JsonProperty(value = \"age\", required = true) int age) {\n"
                + "        this.name = name; this.age = age; this.tag = \"t\" + age;\n"
                + "    }\n"
                + "    public String getName() { return name; }\n"
                + "    public int getAge() { return age; }\n"
                + "}\n");
        src.put("jx.cr.Code", "package jx.cr;\n"
                + "import com.codename1.annotations.*;\n"
                + "@Mapped public class Code {\n"
                + "    public final String v;\n"
                + "    private Code(String v) { this.v = v; }\n"
                + "    @JsonCreator public static Code of(@JsonProperty(\"v\") String v) { return new Code(v == null ? \"none\" : v); }\n"
                + "}\n");
        try (URLClassLoader cl = buildAndRegister(src, "jx.cr.Imm", "jx.cr.Code")) {
            Class<?> imm = cl.loadClass("jx.cr.Imm");
            Object o = fromJson(cl, "{\"name\":\"b\",\"age\":4,\"tag\":\"ignored\"}", imm);
            assertEquals("b", imm.getMethod("getName").invoke(o));
            assertEquals(4, imm.getMethod("getAge").invoke(o));
            assertEquals("t4", imm.getField("tag").get(o));
            assertEquals("{\"name\":\"b\",\"age\":4,\"tag\":\"t4\"}", toJson(cl, o));
            assertEquals("$.age: required member is missing", failure(cl, "{\"name\":\"b\"}", imm));
            o = fromJson(cl, "{\"age\":1}", imm);
            assertNull(imm.getMethod("getName").invoke(o));
            Class<?> code = cl.loadClass("jx.cr.Code");
            Object c = fromJson(cl, "{}", code);
            assertEquals("none", code.getField("v").get(c));
        }
    }

    @Test
    public void genericFieldResolvesItsTypeArguments() throws Exception {
        Map<String, String> src = new LinkedHashMap<String, String>();
        src.put("jx.gen.Page", "package jx.gen;\n"
                + "@com.codename1.annotations.Mapped public class Page<T> {\n"
                + "    public java.util.List<T> items;\n"
                + "    public T first;\n"
                + "    public T[] more;\n"
                + "    public int total;\n"
                + "    public Page() {}\n"
                + "}\n");
        src.put("jx.gen.User", "package jx.gen;\n"
                + "@com.codename1.annotations.Mapped public class User { public String name; public User() {} }\n");
        src.put("jx.gen.Holder", "package jx.gen;\n"
                + "@com.codename1.annotations.Mapped public class Holder {\n"
                + "    public Page<User> page;\n"
                + "    public Holder() {}\n"
                + "}\n");
        try (URLClassLoader cl = buildAndRegister(src, "jx.gen.Page", "jx.gen.User", "jx.gen.Holder")) {
            Class<?> holder = cl.loadClass("jx.gen.Holder");
            Class<?> user = cl.loadClass("jx.gen.User");
            String json = "{\"page\":{\"items\":[{\"name\":\"a\"},{\"name\":\"b\"}],\"first\":{\"name\":\"c\"},"
                    + "\"more\":[{\"name\":\"d\"}],\"total\":2}}";
            Object h = fromJson(cl, json, holder);
            Object page = holder.getField("page").get(h);
            List<?> items = (List<?>) page.getClass().getField("items").get(page);
            assertTrue(user.isInstance(items.get(1)));
            assertEquals("b", user.getField("name").get(items.get(1)));
            assertTrue(user.isInstance(page.getClass().getField("first").get(page)));
            Object[] more = (Object[]) page.getClass().getField("more").get(page);
            assertTrue(user.isInstance(more[0]));
            assertEquals(json, toJson(cl, h));
        }
    }

    @Test
    public void arraysMapToJsonArrays() throws Exception {
        Map<String, String> src = new LinkedHashMap<String, String>();
        src.put("jx.arr.Color", "package jx.arr;\npublic enum Color { RED, GREEN }\n");
        src.put("jx.arr.Dto", "package jx.arr;\n"
                + "@com.codename1.annotations.Mapped public class Dto { public String name; public Dto() {} }\n");
        src.put("jx.arr.Arr", "package jx.arr;\n"
                + "@com.codename1.annotations.Mapped public class Arr {\n"
                + "    public int[] nums;\n"
                + "    public boolean[] flags;\n"
                + "    public String[] tags;\n"
                + "    public Dto[] dtos;\n"
                + "    public Color[] colors;\n"
                + "    public java.util.Date[] when;\n"
                + "    public byte[] raw;\n"
                + "    public Arr() {}\n"
                + "}\n");
        try (URLClassLoader cl = buildAndRegister(src, "jx.arr.Dto", "jx.arr.Arr")) {
            Class<?> arr = cl.loadClass("jx.arr.Arr");
            String json = "{\"nums\":[1,2],\"flags\":[true,false],\"tags\":[\"a\",null],"
                    + "\"dtos\":[{\"name\":\"u\"},null],\"colors\":[\"RED\"],\"when\":[5],\"raw\":\"AQI=\"}";
            Object a = fromJson(cl, json, arr);
            assertTrue(Arrays.equals(new int[] {1, 2}, (int[]) arr.getField("nums").get(a)));
            Object[] dtos = (Object[]) arr.getField("dtos").get(a);
            assertEquals("jx.arr.Dto", dtos.getClass().getComponentType().getName());
            assertEquals("u", dtos[0].getClass().getField("name").get(dtos[0]));
            assertEquals(json, toJson(cl, a));
            assertDirectMatchesMap(cl, cl.loadClass("jx.arr.ArrCn1Mapper"),
                    cl.loadClass("jx.arr.ArrCn1Mapper").newInstance(), a);
            assertTrue(failure(cl, "{\"nums\":[1,{}]}", arr).startsWith("$.nums[1]: expected a number"));
            // Refused, not truncated or dropped, as the server refuses them.
            assertTrue(failure(cl, "{\"nums\":[1.5]}", arr).startsWith("$.nums[0]: expected a whole number"));
            assertTrue(failure(cl, "{\"nums\":[3000000000]}", arr).startsWith("$.nums[0]: expected a whole number"));
            assertTrue(failure(cl, "{\"dtos\":[1]}", arr).startsWith("$.dtos[0]: expected a JSON object"));
        }
    }

    @Test
    public void unsupportedJsonShapesAreBuildErrors() throws Exception {
        assertBuildError("grid", "jx.err1.Grid", "package jx.err1;\n"
                + "@com.codename1.annotations.Mapped public class Grid { public int[][] grid; public Grid() {} }\n");
        assertBuildError("jx.err2.Fmt.name", "jx.err2.Fmt", "package jx.err2;\n"
                + "import com.codename1.annotations.*;\n"
                + "@Mapped public class Fmt { @JsonFormat(JsonFormat.Shape.STRING) public String name; public Fmt() {} }\n");
        assertBuildError("jx.err3.Two", "jx.err3.Two", "package jx.err3;\n"
                + "import com.codename1.annotations.*;\n"
                + "@Mapped public class Two {\n"
                + "    public String a;\n"
                + "    @JsonCreator public Two(@JsonProperty(\"a\") String a) { this.a = a; }\n"
                + "    @JsonCreator public static Two of(@JsonProperty(\"a\") String a) { return new Two(a); }\n"
                + "}\n");
        assertBuildError("jx.err4.NoName", "jx.err4.NoName", "package jx.err4;\n"
                + "import com.codename1.annotations.*;\n"
                + "@Mapped public class NoName {\n"
                + "    public final String a;\n"
                + "    @JsonCreator public NoName(String a) { this.a = a; }\n"
                + "}\n");
        Map<String, String> src = new LinkedHashMap<String, String>();
        src.put("jx.err5.Bad", "package jx.err5;\n"
                + "public class Bad implements com.codename1.annotations.JsonTypeAdapter<String> {\n"
                + "    public Bad(int x) {}\n"
                + "    public Object toJson(String s) { return s; }\n"
                + "    public String fromJson(Object o) { return String.valueOf(o); }\n"
                + "}\n");
        src.put("jx.err5.Holder", "package jx.err5;\n"
                + "import com.codename1.annotations.*;\n"
                + "@Mapped public class Holder { @JsonAdapter(Bad.class) public String s; public Holder() {} }\n");
        assertBuildError("jx.err5.Holder.s", src);
    }

    @Test
    public void explicitNullAndAbsentMemberFollowTheBackend() throws Exception {
        Map<String, String> src = new LinkedHashMap<String, String>();
        src.put("jx.nul.N", "package jx.nul;\n"
                + "import com.codename1.annotations.*;\n"
                + "@Mapped public class N {\n"
                + "    public String note = \"unset\";\n"
                + "    public int plain = 7;\n"
                + "    @JsonProperty(required = true) public int must;\n"
                + "    public java.util.List<Integer> nums;\n"
                + "    public Integer[] boxes;\n"
                + "    public N() {}\n"
                + "}\n");
        src.put("jx.nul.S", "package jx.nul;\n"
                + "@com.codename1.annotations.Mapped @com.codename1.annotations.JsonStrict\n"
                + "public class S { public String note = \"unset\"; public S() {} }\n");
        try (URLClassLoader cl = buildAndRegister(src, "jx.nul.N", "jx.nul.S")) {
            Class<?> n = cl.loadClass("jx.nul.N");
            Object o = fromJson(cl, "{\"must\":1}", n);
            assertEquals("absent keeps the initializer", "unset", n.getField("note").get(o));
            o = fromJson(cl, "{\"must\":1,\"note\":null,\"plain\":null,"
                    + "\"nums\":[1,null],\"boxes\":[2,null]}", n);
            // Before these options the parser dropped a null member, so a class
            // that opts into none of them keeps its value exactly as it did.
            assertEquals("explicit null keeps the value by default", "unset", n.getField("note").get(o));
            Class<?> strict = cl.loadClass("jx.nul.S");
            Object so = fromJson(cl, "{\"note\":null}", strict);
            assertNull("explicit null clears a reference in a @JsonStrict class",
                    strict.getField("note").get(so));
            assertEquals("a primitive ignores null", 7, n.getField("plain").getInt(o));
            assertEquals(Arrays.asList(Integer.valueOf(1), null), n.getField("nums").get(o));
            assertTrue(Arrays.equals(new Integer[] {2, null}, (Integer[]) n.getField("boxes").get(o)));
            assertEquals("$.must: expected a value, got null", failure(cl, "{\"must\":null}", n));
        }
    }

    // ---------------------------------------------------------------
    // Helpers for the JSON option tests
    // ---------------------------------------------------------------

    private URLClassLoader buildAndRegister(Map<String, String> sources, String... mapped) throws Exception {
        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(sources, classes, Arrays.asList(testClassesDir()));
        runProcessorOrFail(classes);
        URLClassLoader cl = childLoader(classes);
        for (String m : mapped) {
            cl.loadClass(m + "Cn1Mapper").getMethod("register").invoke(null);
        }
        return cl;
    }

    private void assertBuildError(String expected, String fqn, String source) throws Exception {
        assertBuildError(expected, JavaSourceCompiler.singleSource(fqn, source));
    }

    private void assertBuildError(String expected, Map<String, String> sources) throws Exception {
        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(sources, classes, Arrays.asList(testClassesDir()));
        ProcessorContext ctx = runProcessor(classes);
        assertTrue("expected a build error naming " + expected, ctx.hasErrors());
        StringBuilder all = new StringBuilder();
        for (ProcessorContext.ProcessingError e : ctx.getErrors()) all.append(e).append('\n');
        assertTrue("error should name " + expected + ": " + all, all.indexOf(expected) >= 0);
    }

    private static Object fromJson(ClassLoader cl, String json, Class<?> type) throws Exception {
        try {
            return cl.loadClass("com.codename1.mapping.Mappers")
                    .getMethod("fromJson", String.class, Class.class).invoke(null, json, type);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof Exception) throw (Exception) e.getCause();
            throw e;
        }
    }

    private static String toJson(ClassLoader cl, Object o) throws Exception {
        return (String) cl.loadClass("com.codename1.mapping.Mappers")
                .getMethod("toJson", Object.class).invoke(null, o);
    }

    /// The message of the IllegalArgumentException reading `json` must fail with.
    private static String failure(ClassLoader cl, String json, Class<?> type) throws Exception {
        try {
            fromJson(cl, json, type);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
        fail("expected " + json + " to be refused");
        return null;
    }
}
