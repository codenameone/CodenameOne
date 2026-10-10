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
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// End-to-end test for `RestClientAnnotationProcessor`. Compiles a
/// `@RestClient` interface alongside a fixture `@Mapped` POJO, runs the
/// processor, then asserts both the impl class and the bootstrap have been
/// produced -- and the impl class's source carries the expected
/// `Rest.<verb>` + `fetchAsMapped` invocations.
public class RestClientAnnotationProcessorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void emitsImplAndBootstrapForPetApi() throws Exception {
        File classes = tmp.newFolder("classes");
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.Pet",
                "package com.example;\n"
                        + "import com.codename1.annotations.Mapped;\n"
                        + "@Mapped public class Pet {\n"
                        + "    public Long id;\n"
                        + "    public String name;\n"
                        + "    public Pet() {}\n"
                        + "}\n");
        sources.put("com.example.PetApi",
                "package com.example;\n"
                        + "import com.codename1.annotations.rest.*;\n"
                        + "import com.codename1.io.rest.Response;\n"
                        + "import com.codename1.util.OnComplete;\n"
                        + "@RestClient\n"
                        + "public interface PetApi {\n"
                        + "    @GET(\"/pet/{petId}\")\n"
                        + "    void getPetById(@Path(\"petId\") Long petId,\n"
                        + "                    @Header(\"Authorization\") String bearerToken,\n"
                        + "                    OnComplete<Response<Pet>> callback);\n"
                        + "    @POST(\"/pet\")\n"
                        + "    void addPet(@Body Pet body,\n"
                        + "                @Header(\"Authorization\") String bearerToken,\n"
                        + "                OnComplete<Response<Pet>> callback);\n"
                        + "    @GET(\"/pets\")\n"
                        + "    void findAll(@Query(\"status\") String status,\n"
                        + "                 OnComplete<Response<java.util.List<Pet>>> callback);\n"
                        + "    static PetApi of(String baseUrl) {\n"
                        + "        return com.codename1.io.rest.RestClients.create(PetApi.class, baseUrl);\n"
                        + "    }\n"
                        + "}\n");
        JavaSourceCompiler.compile(sources, classes, Arrays.asList(testClassesDir()));

        ProcessorContext ctx = runProcessor(classes);
        if (ctx.hasErrors()) {
            StringBuilder sb = new StringBuilder("processor reported errors:\n");
            for (ProcessorContext.ProcessingError e : ctx.getErrors()) sb.append(' ').append(e).append('\n');
            fail(sb.toString());
        }

        // The processor compiled and wrote out PetApiImpl + RestClientBootstrap.
        File impl = new File(classes, "com/example/PetApiImpl.class");
        File boot = new File(classes, "cn1app/RestClientBootstrap.class");
        assertTrue("expected PetApiImpl.class at " + impl, impl.exists());
        assertTrue("expected RestClientBootstrap.class at " + boot, boot.exists());

        // Re-run the processor against a fresh sources map to recover the in-memory
        // Java source so we can string-search the generated body. This is the
        // simplest way to assert on what was generated without exposing the
        // emitted-sources map outside the processor.
        String implSrc = generateImplSourceForFixture(classes);
        assertTrue("getPetById should call Rest.get",
                implSrc.contains("com.codename1.io.rest.Rest.get(_url)"));
        assertTrue("getPetById should embed path param via String.valueOf",
                implSrc.contains("\"/pet/\" + String.valueOf(petId)"));
        assertTrue("getPetById should attach Authorization header",
                implSrc.contains("_rb.header(\"Authorization\", AuthorizationHeader)"));
        assertTrue("getPetById should fetch as mapped Pet",
                implSrc.contains("_rb.fetchAsMapped(com.example.Pet.class, callback)"));
        assertTrue("HTTP error statuses should be delivered to the callback",
                implSrc.contains("_rb.onErrorCodeString(new com.codename1.io.rest.ErrorCodeHandler<String>()"));
        assertTrue("generated clients should consume transport exceptions through the existing error listener API",
                implSrc.contains("_rb.onError(new com.codename1.ui.events.ActionListener<com.codename1.io.NetworkEvent>()"));
        assertTrue("transport exceptions should be delivered to the callback",
                implSrc.contains("callback.completed(null)"));
        assertTrue("error response should be forwarded without falling through to the default dialog",
                implSrc.contains("callback.completed((com.codename1.io.rest.Response)_r)"));

        assertTrue("addPet should call Rest.post",
                implSrc.contains("com.codename1.io.rest.Rest.post(_url)"));
        assertTrue("addPet should serialize body via Mappers.toJson",
                implSrc.contains("com.codename1.mapping.Mappers.toJson(body)"));

        assertTrue("findAll should append status query param",
                implSrc.contains("_rb.queryParam(\"status\", String.valueOf(status))"));
        assertTrue("findAll should fetch as mapped list",
                implSrc.contains("_rb.fetchAsMappedList(com.example.Pet.class, callback)"));

        // Bootstrap registers our PetApi.
        String bootSrc = generateBootstrapSourceForFixture(classes);
        assertTrue("bootstrap should register PetApi",
                bootSrc.contains("RestClients.register(com.example.PetApi.class"));
        assertTrue("bootstrap should instantiate PetApiImpl",
                bootSrc.contains("new com.example.PetApiImpl(baseUrl)"));
    }

    @Test
    public void rejectsMethodWithMultipleVerbAnnotations() throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.BadApi",
                        "package com.example;\n"
                                + "import com.codename1.annotations.rest.*;\n"
                                + "import com.codename1.io.rest.Response;\n"
                                + "import com.codename1.util.OnComplete;\n"
                                + "@RestClient\n"
                                + "public interface BadApi {\n"
                                + "    @GET(\"/a\")\n"
                                + "    @POST(\"/a\")\n"
                                + "    void mixedVerbs(OnComplete<Response<String>> cb);\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir()));

        ProcessorContext ctx = runProcessor(classes);
        assertTrue("expected error on multi-verb method", ctx.hasErrors());
    }

    @Test
    public void rejectsRestClientOnNonInterface() throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Bad",
                        "package com.example;\n"
                                + "import com.codename1.annotations.rest.RestClient;\n"
                                + "@RestClient public class Bad {}\n"),
                classes, Arrays.asList(testClassesDir()));

        ProcessorContext ctx = runProcessor(classes);
        assertTrue("expected error on @RestClient applied to a class", ctx.hasErrors());
    }

    /// A primitive parameter cannot be compared with null, so the guard that
    /// leaves an absent query parameter or header off the request has to be
    /// written for reference types only. Guarding both generated a client that
    /// did not compile, for any contract with an `int` page or a `double`
    /// coordinate in it.
    @Test
    public void primitiveQueryAndHeaderParametersAreNotNullChecked() throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.GeoApi",
                        "package com.example;\n"
                                + "import com.codename1.annotations.rest.*;\n"
                                + "import com.codename1.io.rest.Response;\n"
                                + "import com.codename1.util.OnComplete;\n"
                                + "@RestClient\n"
                                + "public interface GeoApi {\n"
                                + "    @GET(\"/geo/search\")\n"
                                + "    void search(@Query(\"q\") String text,\n"
                                + "                @Query(\"lat\") double lat,\n"
                                + "                @Query(\"limit\") int limit,\n"
                                + "                @Header(\"X-Page\") long page,\n"
                                + "                OnComplete<Response<String>> callback);\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir()));

        ProcessorContext ctx = runProcessor(classes);
        assertFalse("the generated client must compile: " + ctx.getErrors(), ctx.hasErrors());
        assertTrue(new File(classes, "com/example/GeoApiImpl.class").exists());

        String implSrc = generateImplSourceForFixture(classes);
        assertTrue("a reference parameter keeps its guard: " + implSrc,
                implSrc.contains("!= null) _rb.queryParam(\"q\""));
        assertFalse(implSrc, implSrc.contains("!= null) _rb.queryParam(\"lat\""));
        assertFalse(implSrc, implSrc.contains("!= null) _rb.queryParam(\"limit\""));
        assertFalse(implSrc, implSrc.contains("!= null) _rb.header(\"X-Page\""));
    }

    /// The cookie guard is a block, not a statement, and the header value is a
    /// String: both had their own way of not compiling for a primitive.
    @Test
    public void primitiveCookieAndHeaderParametersCompile() throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.VisitApi",
                        "package com.example;\n"
                                + "import com.codename1.annotations.rest.*;\n"
                                + "import com.codename1.io.rest.Response;\n"
                                + "import com.codename1.util.OnComplete;\n"
                                + "@RestClient\n"
                                + "public interface VisitApi {\n"
                                + "    @GET(\"/visits\")\n"
                                + "    void count(@Cookie(\"shard\") int shard,\n"
                                + "               @Cookie(\"sid\") String session,\n"
                                + "               @Header(\"X-Dry-Run\") boolean dryRun,\n"
                                + "               @Header(\"X-Trace\") String trace,\n"
                                + "               OnComplete<Response<String>> callback);\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir()));

        ProcessorContext ctx = runProcessor(classes);
        assertFalse("the generated client must compile: " + ctx.getErrors(), ctx.hasErrors());
        assertTrue(new File(classes, "com/example/VisitApiImpl.class").exists());

        String implSrc = generateImplSourceForFixture(classes);
        assertFalse(implSrc, implSrc.contains("if (shardCookie != null)"));
        assertTrue(implSrc, implSrc.contains("if (sidCookie != null) {"));
        assertTrue(implSrc, implSrc.contains("_rb.header(\"X-Dry-Run\", String.valueOf("));
        assertTrue(implSrc, implSrc.contains("!= null) _rb.header(\"X-Trace\", "));
    }

    /// A contract that lives in a library the application depends on gets its
    /// client generated in the application -- on every build, not only the
    /// first. The application's output directory is on its own compile
    /// classpath and holds the client the previous build generated; taking that
    /// for "a dependency already supplies the client" left the second build
    /// with a bootstrap that registered nothing, and an application that threw
    /// on its first call.
    @Test
    public void regeneratesADependencyContractOverThePreviousBuildsOutput() throws Exception {
        File shared = tmp.newFolder("shared");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.NotesApi",
                        "package com.example;\n"
                                + "import com.codename1.annotations.rest.*;\n"
                                + "import com.codename1.io.rest.Response;\n"
                                + "import com.codename1.util.OnComplete;\n"
                                + "@RestClient\n"
                                + "public interface NotesApi {\n"
                                + "    @GET(\"/notes\")\n"
                                + "    void list(OnComplete<Response<String>> callback);\n"
                                + "}\n"),
                shared, Arrays.asList(testClassesDir()));

        // What the previous build left behind.
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.NotesApiImpl",
                        "package com.example;\npublic class NotesApiImpl {}\n"),
                classes, Arrays.asList(testClassesDir()));

        // The client runtime is what tells the processor this module is an
        // application, and so one that wants the client half of a contract.
        File core = new File(com.codename1.io.rest.Rest.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        java.util.List<String> classpath = Arrays.asList(classes.getPath(), shared.getPath(),
                core.getPath());
        RestClientAnnotationProcessor proc = new RestClientAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classes, tmp.newFolder(),
                java.util.Collections.<String, AnnotatedClass>emptyMap(), new SystemStreamLog(),
                null, null, null, null, null, classpath);
        assertTrue(proc.acceptsDependencyClasses(ctx));
        Map<String, AnnotatedClass> dependencies =
                com.codename1.maven.annotations.DependencyClasses.scan(
                        classpath, classes, proc.getAnnotationDescriptors());
        assertTrue("the contract is found in the dependency",
                dependencies.containsKey("com/example/NotesApi"));
        ctx.setDependencyIndex(dependencies);

        // The build hands the module's classpath to the generated-source
        // compile, which is how the client sees the contract it implements.
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

        assertFalse(String.valueOf(ctx.getErrors()), ctx.hasErrors());
        assertTrue("the client is registered",
                new File(classes, "cn1app/RestClientBootstrap.class").exists());
        AnnotatedClass regenerated = ClassScanner.readClass(
                new File(classes, "com/example/NotesApiImpl.class"));
        assertTrue("the stale client is replaced by one that implements the contract",
                regenerated.getInterfaceInternalNames().contains("com/example/NotesApi"));
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private ProcessorContext runProcessor(File classesDir) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classesDir);
        RestClientAnnotationProcessor proc = new RestClientAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classesDir, tmp.newFolder(),
                index, new SystemStreamLog());
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (!cls.getClassAnnotations().isEmpty()) proc.processClass(cls, ctx);
        }
        proc.finish(ctx);
        return ctx;
    }

    /// Re-runs the processor's source-generation step via reflection so we can
    /// string-assert on the generated source body. The processor itself
    /// compiles and discards the sources at finish() time.
    private String generateImplSourceForFixture(File classesDir) throws Exception {
        return invokeGenerator(classesDir, "generateImplSource");
    }

    private String generateBootstrapSourceForFixture(File classesDir) throws Exception {
        return invokeGenerator(classesDir, "generateBootstrapSource");
    }

    private String invokeGenerator(File classesDir, String which) throws Exception {
        // Rebuild the processor's accumulator by running start() + processClass().
        Map<String, AnnotatedClass> index = ClassScanner.scan(classesDir);
        RestClientAnnotationProcessor proc = new RestClientAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classesDir, tmp.newFolder(),
                index, new SystemStreamLog());
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (!cls.getClassAnnotations().isEmpty()) proc.processClass(cls, ctx);
        }
        java.lang.reflect.Field f = RestClientAnnotationProcessor.class.getDeclaredField("accepted");
        f.setAccessible(true);
        java.util.TreeMap<?, ?> accepted = (java.util.TreeMap<?, ?>) f.get(proc);
        if ("generateImplSource".equals(which)) {
            Object petApi = accepted.values().iterator().next();
            java.lang.reflect.Method m = RestClientAnnotationProcessor.class
                    .getDeclaredMethod("generateImplSource",
                            Class.forName("com.codename1.maven.processors.RestClientAnnotationProcessor$RestApi"));
            m.setAccessible(true);
            return (String) m.invoke(null, petApi);
        }
        java.lang.reflect.Method m = RestClientAnnotationProcessor.class
                .getDeclaredMethod("generateBootstrapSource", Iterable.class);
        m.setAccessible(true);
        return (String) m.invoke(null, accepted.values());
    }

    private static File testClassesDir() throws Exception {
        URL url = RestClientAnnotationProcessorTest.class.getProtectionDomain()
                .getCodeSource().getLocation();
        return new File(url.toURI());
    }
}
