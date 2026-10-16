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

import com.codename1.build.SystemStreamLog;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.ProcessorContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/// The source half of a served contract: for a backend module whose classpath
/// holds a `@RestClient` interface in a dependency, `emitStubs` writes the
/// `<Api>Server` interface and the `<Api>Controller` that maps each operation to
/// it. What is written is COMPILED here, against the backend runtime and without
/// the client's, because that is the classpath it is compiled on in a build.
public class RestServerStubsTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final String NOTE =
            "package com.example;\n"
            + "public class Note {\n"
            + "    public String title;\n"
            + "}\n";

    private static final String API =
            "package com.example;\n"
            + "import com.codename1.annotations.rest.*;\n"
            + "import com.codename1.io.rest.Response;\n"
            + "import com.codename1.util.OnComplete;\n"
            + "@RestClient\n"
            + "public interface NotesApi {\n"
            + "    public static class Draft {\n"
            + "        public String text;\n"
            + "    }\n"
            + "    @GET(\"/notes/{id}\")\n"
            + "    void get(@Path(\"id\") long id, OnComplete<Response<Note>> callback);\n"
            + "    @GET(\"/notes\")\n"
            + "    void search(@Query(\"q\") String text, @Query(\"limit\") int limit,\n"
            + "                @Header(\"X-Tenant\") String tenant,\n"
            + "                OnComplete<Response<java.util.List<Note>>> callback);\n"
            + "    @POST(\"/notes\")\n"
            + "    void create(@Body Draft draft, OnComplete<Response<Note>> callback);\n"
            + "    @PUT(\"/notes/{id}\")\n"
            + "    void replace(@Path(\"id\") long id, @Body Note note,\n"
            + "                 OnComplete<Response<Note>> callback);\n"
            + "    @PATCH(\"/notes/{id}\")\n"
            + "    void rename(@Path(\"id\") long id, @Query(\"title\") String title,\n"
            + "                OnComplete<Response<Note>> callback);\n"
            + "    @DELETE(\"/notes/{id}\")\n"
            + "    void remove(@Path(\"id\") long id, OnComplete<Response<String>> callback);\n"
            + "}\n";

    @Test
    public void aDependencyContractGetsItsServerInterfaceAndController() throws Exception {
        File shared = compileShared(API);
        File classes = tmp.newFolder("classes");
        ProcessorContext ctx = emit(classes, Arrays.asList(classes.getPath(), shared.getPath(),
                backendRuntime().getPath()));

        assertFalse(String.valueOf(ctx.getErrors()), ctx.hasErrors());
        Map<String, String> stubs = ctx.getEmittedStubSources();
        assertEquals("one interface and one controller: " + stubs.keySet(), 2, stubs.size());
        String server = stubs.get("com/example/NotesApiServer");
        String controller = stubs.get("com/example/NotesApiController");
        assertNotNull(stubs.keySet().toString(), server);
        assertNotNull(stubs.keySet().toString(), controller);

        // A source in the stub directory is the developer's to implement and
        // every later processor's to see; the marker would hide it from them.
        assertFalse(server, server.contains("Generated\n"));
        assertTrue(server, server.contains("public interface NotesApiServer"));
        assertTrue(server, server.contains("com.example.Note get(long id)"));
        // A member class is written the way source spells it.
        assertTrue(server, server.contains("com.example.NotesApi.Draft"));
        assertFalse(server, server.contains("NotesApi$Draft"));

        assertTrue(controller, controller.contains("@com.codename1.backend.annotations.RestController\n"));
        assertTrue(controller, controller.contains("public NotesApiController(NotesApiServer impl)"));
        assertTrue(controller, controller.contains(
                "@com.codename1.backend.annotations.GetMapping(\"/notes/{id}\")"));
        assertTrue(controller, controller.contains(
                "@com.codename1.backend.annotations.PostMapping(\"/notes\")"));
        assertTrue(controller, controller.contains(
                "@com.codename1.backend.annotations.PutMapping(\"/notes/{id}\")"));
        assertTrue(controller, controller.contains(
                "@com.codename1.backend.annotations.PatchMapping(\"/notes/{id}\")"));
        assertTrue(controller, controller.contains(
                "@com.codename1.backend.annotations.DeleteMapping(\"/notes/{id}\")"));
        assertTrue(controller, controller.contains(
                "@com.codename1.backend.annotations.PathVariable(\"id\") long id"));
        assertTrue(controller, controller.contains(
                "@com.codename1.backend.annotations.RequestBody com.example.NotesApi.Draft body"));
        // The client leaves a null reference argument off the request, so the
        // server must not require it; a primitive is always sent.
        assertTrue(controller, controller.contains(
                "RequestParam(value = \"q\", required = false) java.lang.String q"));
        assertTrue(controller, controller.contains("RequestParam(value = \"limit\") int limit"));
        assertTrue(controller, controller.contains(
                "RequestHeader(value = \"X-Tenant\", required = false) java.lang.String"));
        assertTrue(controller, controller.contains("return impl.search(q, limit, "));

        // And the two compile together, beside a component implementing the
        // interface, on the classpath a backend module has.
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.NotesApiServer", server);
        sources.put("com.example.NotesApiController", controller);
        sources.put("com.example.NotesEndpoint",
                "package com.example;\n"
                + "public class NotesEndpoint implements NotesApiServer {\n"
                + "    public Note get(long id) { return new Note(); }\n"
                + "    public java.util.List<Note> search(String q, int limit, String tenant) {\n"
                + "        return new java.util.ArrayList<Note>();\n"
                + "    }\n"
                + "    public Note create(NotesApi.Draft draft) { return new Note(); }\n"
                + "    public Note replace(long id, Note note) { return note; }\n"
                + "    public Note rename(long id, String title) { return new Note(); }\n"
                + "    public String remove(long id) { return \"gone\"; }\n"
                + "}\n");
        JavaSourceCompiler.compile(sources, classes,
                Arrays.asList(shared, backendRuntime(), testClassesDir()));
        assertTrue(new File(classes, "com/example/NotesApiController.class").isFile());
        assertTrue(new File(classes, "com/example/NotesEndpoint.class").isFile());
    }

    /// A module with the client runtime is an application: the contract there is
    /// to be CALLED, and its server half stays behind `cn1.restServer`.
    @Test
    public void nothingIsWrittenForAModuleWithTheClientRuntime() throws Exception {
        File shared = compileShared(API);
        File classes = tmp.newFolder("classes");
        ProcessorContext ctx = emit(classes, Arrays.asList(classes.getPath(), shared.getPath(),
                backendRuntime().getPath(), clientRuntime().getPath()));
        assertFalse(String.valueOf(ctx.getErrors()), ctx.hasErrors());
        assertTrue(ctx.getEmittedStubSources().isEmpty());
    }

    @Test
    public void nothingIsWrittenForAModuleWithoutTheBackendRuntime() throws Exception {
        File shared = compileShared(API);
        File classes = tmp.newFolder("classes");
        ProcessorContext ctx = emit(classes, Arrays.asList(classes.getPath(), shared.getPath()));
        assertFalse(String.valueOf(ctx.getErrors()), ctx.hasErrors());
        assertTrue(ctx.getEmittedStubSources().isEmpty());
    }

    @Test
    public void aBackendWithNoContractOnItsClasspathWritesNothing() throws Exception {
        File classes = tmp.newFolder("classes");
        ProcessorContext ctx = emit(classes, Arrays.asList(classes.getPath(),
                backendRuntime().getPath()));
        assertFalse(String.valueOf(ctx.getErrors()), ctx.hasErrors());
        assertTrue(ctx.getEmittedStubSources().isEmpty());
    }

    /// A served contract has no binding for a cookie. Said at build time, and
    /// neither half written, in place of a controller that does not compile.
    @Test
    public void aCookieParameterIsRefusedWithAReason() throws Exception {
        File shared = compileShared(
                "package com.example;\n"
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient\n"
                + "public interface NotesApi {\n"
                + "    @GET(\"/notes\")\n"
                + "    void list(@Cookie(\"sid\") String session,\n"
                + "              OnComplete<Response<String>> callback);\n"
                + "}\n");
        File classes = tmp.newFolder("classes");
        ProcessorContext ctx = emit(classes, Arrays.asList(classes.getPath(), shared.getPath(),
                backendRuntime().getPath()));
        assertTrue("a cookie binding must be reported", ctx.hasErrors());
        assertTrue(ctx.getErrors().toString(), ctx.getErrors().toString().contains("cookie"));
        assertTrue(ctx.getEmittedStubSources().toString(), ctx.getEmittedStubSources().isEmpty());
    }

    /// The processor is one instance per build and `emitStubs` shares its state
    /// with the class pass, so a second module must not inherit the first's.
    @Test
    public void emittingTwiceDoesNotCarryStateOver() throws Exception {
        File shared = compileShared(API);
        File classes = tmp.newFolder("classes");
        List<String> classpath = Arrays.asList(classes.getPath(), shared.getPath(),
                backendRuntime().getPath());
        RestServerAnnotationProcessor proc = new RestServerAnnotationProcessor();
        ProcessorContext first = context(classes, classpath);
        proc.emitStubs(first);
        ProcessorContext second = context(classes,
                Arrays.asList(classes.getPath(), backendRuntime().getPath()));
        proc.emitStubs(second);
        assertEquals(2, first.getEmittedStubSources().size());
        assertTrue(second.getEmittedStubSources().isEmpty());
    }

    private File compileShared(String api) throws Exception {
        File shared = tmp.newFolder("shared");
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.Note", NOTE);
        sources.put("com.example.NotesApi", api);
        JavaSourceCompiler.compile(sources, shared, Arrays.asList(testClassesDir()));
        return shared;
    }

    private ProcessorContext emit(File classes, List<String> classpath) throws Exception {
        ProcessorContext ctx = context(classes, classpath);
        new RestServerAnnotationProcessor().emitStubs(ctx);
        return ctx;
    }

    private ProcessorContext context(File classes, List<String> classpath) throws Exception {
        return new ProcessorContext(classes, tmp.newFolder(),
                Collections.<String, AnnotatedClass>emptyMap(), new SystemStreamLog(),
                null, null, null, null, null, classpath);
    }

    private static File backendRuntime() throws Exception {
        return new File(com.codename1.backend.Backend.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
    }

    private static File clientRuntime() throws Exception {
        return new File(com.codename1.io.rest.Rest.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
    }

    private static File testClassesDir() throws Exception {
        return new File(RestServerStubsTest.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
    }
}
