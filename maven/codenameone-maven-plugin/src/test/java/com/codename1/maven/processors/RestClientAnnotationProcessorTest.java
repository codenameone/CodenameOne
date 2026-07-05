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
