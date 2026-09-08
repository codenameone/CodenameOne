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
import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Proves the generated router ROUTES. The class it produces is compiled, loaded
/// and called with real `HttpServer.Request` objects here, because a router that
/// compiles and matches nothing is exactly the failure this is for -- and because
/// the matching runs on the request's bytes, which only a real Request has.
public class RestControllerAnnotationProcessorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final String CONTROLLER_SOURCE =
            "package com.example;\n"
            + "import com.codename1.backend.annotations.*;\n"
            + "import java.util.*;\n"
            + "@RestController\n"
            + "@RequestMapping(\"/api\")\n"
            + "public class Notes {\n"
            + "    @GetMapping(\"/healthz\")\n"
            + "    public String health() { return \"ok\"; }\n"
            + "    @GetMapping(\"/notes/{id}\")\n"
            + "    public Map note(@PathVariable(\"id\") String id) {\n"
            + "        Map m = new LinkedHashMap(); m.put(\"id\", id); return m;\n"
            + "    }\n"
            + "    @GetMapping(\"/notes/{id}/tags/{tag}\")\n"
            + "    public Map tag(@PathVariable(\"id\") String id, @PathVariable(\"tag\") String tag) {\n"
            + "        Map m = new LinkedHashMap(); m.put(\"id\", id); m.put(\"tag\", tag); return m;\n"
            + "    }\n"
            + "    @GetMapping(\"/search\")\n"
            + "    public Map search(@RequestParam(\"q\") String q,\n"
            + "                      @RequestParam(value=\"page\", defaultValue=\"7\") int page) {\n"
            + "        Map m = new LinkedHashMap(); m.put(\"q\", q);\n"
            + "        m.put(\"page\", Integer.valueOf(page)); return m;\n"
            + "    }\n"
            + "    @GetMapping(\"/agent\")\n"
            + "    public String agent(@RequestHeader(\"user-agent\") String ua) { return ua; }\n"
            + "    @PostMapping(\"/notes\")\n"
            + "    @ResponseStatus(201)\n"
            + "    public Map create(@RequestBody Map body) { return body; }\n"
            + "    @GetMapping(\"/tags\")\n"
            + "    public Set tags() { return new LinkedHashSet(Arrays.asList(\"a\", \"b\")); }\n"
            + "    @GetMapping(\"/boom\")\n"
            + "    public String boom() throws java.io.IOException {\n"
            + "        throw new java.io.IOException(\"from the handler\");\n"
            + "    }\n"
            + "}\n";

    @Test
    public void routesEveryBindingKind() throws Exception {
        Router router = generate(CONTROLLER_SOURCE);

        assertEquals("ok", router.text("GET", "/api/healthz"));
        // A query string is not part of the route. Matching the whole target instead
        // of the path is the bug this asserts against.
        assertEquals("ok", router.text("GET", "/api/healthz?probe=1"));
        assertEquals("{\"id\":\"42\"}", router.text("GET", "/api/notes/42"));
        assertEquals("{\"id\":\"42\"}", router.text("GET", "/api/notes/42?x=1"));
        assertEquals("{\"id\":\"a b\"}", router.text("GET", "/api/notes/a%20b"));
        // '+' is a literal in a path segment; it means a space only in a query.
        assertEquals("{\"id\":\"a+b\"}", router.text("GET", "/api/notes/a+b"));
        assertEquals("{\"id\":\"42\",\"tag\":\"red\"}",
                router.text("GET", "/api/notes/42/tags/red"));
        assertEquals("{\"q\":\"hi\",\"page\":7}", router.text("GET", "/api/search?q=hi"));
        assertEquals("{\"q\":\"hi\",\"page\":3}", router.text("GET", "/api/search?q=hi&page=3"));
        // A query string is user input: an unparseable number takes the default
        // rather than failing the request.
        assertEquals("{\"q\":\"hi\",\"page\":7}", router.text("GET", "/api/search?q=hi&page=zz"));
        assertEquals("[\"a\",\"b\"]", router.text("GET", "/api/tags"));
    }

    @Test
    public void bindsBodyAndStatus() throws Exception {
        Router router = generate(CONTROLLER_SOURCE);
        Object response = router.call("POST", "/api/notes", "{\"body\":\"hi\"}");
        assertNotNull("POST /api/notes did not match", response);
        assertEquals(201, ((HttpServer.Response) response).getStatus());
        assertEquals("{\"body\":\"hi\"}", Router.bodyOf(response));
    }

    @Test
    public void doesNotMatchWhatItShouldNot() throws Exception {
        Router router = generate(CONTROLLER_SOURCE);
        // A path variable is ONE segment: without that guard /notes/{id} swallows
        // /notes/1/2 and hands the method "1/2" as the id.
        assertNull(router.call("GET", "/api/notes/1/2", null));
        assertNull(router.call("GET", "/api/nope", null));
        assertNull("the method is part of the route", router.call("POST", "/api/healthz", null));
        assertNull("the class-level base path applies", router.call("GET", "/healthz", null));
    }

    @Test
    public void aHandlerMayThrow() throws Exception {
        Router router = generate(CONTROLLER_SOURCE);
        try {
            router.call("GET", "/api/boom", null);
            fail("the handler's IOException should reach the server");
        } catch (java.lang.reflect.InvocationTargetException err) {
            assertTrue(err.getCause() instanceof java.io.IOException);
        }
    }

    @Test
    public void namesTheBootstrapForThePackagingGoal() throws Exception {
        ProcessorContext ctx = run(compile(CONTROLLER_SOURCE));
        byte[] name = ctx.getEmittedResources()
                .get(RestControllerAnnotationProcessor.MAIN_CLASS_RESOURCE);
        assertNotNull("the generated main class was not recorded", name);
        assertEquals("com.example.BackendApplication", new String(name, "UTF-8"));
    }

    @Test
    public void refusesAParameterItCannotBind() throws Exception {
        String source =
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Bad {\n"
                + "    @GetMapping(\"/x\")\n"
                + "    public String x(String unannotated) { return unannotated; }\n"
                + "}\n";
        ProcessorContext ctx = run(compile(source));
        assertTrue("an unbindable parameter must be reported, not guessed at",
                ctx.hasErrors());
    }

    @Test
    public void refusesAPathVariableThatIsNotInTheRoute() throws Exception {
        String source =
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Bad {\n"
                + "    @GetMapping(\"/x/{id}\")\n"
                + "    public String x(@PathVariable(\"other\") String id) { return id; }\n"
                + "}\n";
        assertTrue(run(compile(source)).hasErrors());
    }

    // ----------------------------------------------------------------

    /// The generated router, loaded and callable.
    /// A second controller for the cases the first cannot express: a void route,
    /// a parameter that is explicitly optional, and a required body.
    private static final String OPTIONAL_SOURCE =
            "package com.example;\n"
            + "import com.codename1.backend.annotations.*;\n"
            + "import java.util.*;\n"
            + "@RestController\n"
            + "@RequestMapping(\"/api\")\n"
            + "public class Notes {\n"
            + "    @DeleteMapping(\"/notes/{id}\")\n"
            + "    public void remove(@PathVariable(\"id\") String id) { }\n"
            + "    @GetMapping(\"/opt\")\n"
            + "    public String opt(@RequestParam(value=\"q\", required=false) String q) {\n"
            + "        return q == null ? \"none\" : q;\n"
            + "    }\n"
            + "    @PostMapping(\"/notes\")\n"
            + "    public String create(@RequestBody String body) { return body; }\n"
            + "}\n";

    @Test
    public void aVoidRouteAnswersNoContent() throws Exception {
        Router router = generate(OPTIONAL_SOURCE);
        Object response = router.call("DELETE", "/api/notes/42", null);
        assertNotNull("DELETE /api/notes/42 matched no route", response);
        // ResponseStatus documents this default; the generator used to answer 200
        // for a void method, which made that javadoc wrong.
        assertEquals(204, Router.statusOf(response));
    }

    @Test
    public void anAbsentRequiredParamIsRefused() throws Exception {
        Router router = generate(CONTROLLER_SOURCE);
        // q is @RequestParam("q"), so required defaults to true.
        Object missing = router.call("GET", "/api/search", null);
        assertNotNull("GET /api/search matched no route", missing);
        assertEquals(400, Router.statusOf(missing));
        assertTrue(Router.bodyOf(missing), Router.bodyOf(missing).indexOf("q") >= 0);
        // and the route still works when it is supplied
        assertEquals("{\"q\":\"hi\",\"page\":7}", router.text("GET", "/api/search?q=hi"));
    }

    @Test
    public void anAbsentRequiredHeaderIsRefused() throws Exception {
        Router router = generate(CONTROLLER_SOURCE);
        // This harness builds a Request with an empty header index, so no header
        // is bindable through it -- which makes it exactly the "the client did
        // not send it" case that @RequestHeader's required element describes.
        Object response = router.call("GET", "/api/agent", null);
        assertNotNull("GET /api/agent matched no route", response);
        assertEquals(400, Router.statusOf(response));
        assertTrue(Router.bodyOf(response),
                Router.bodyOf(response).indexOf("user-agent") >= 0);
    }

    @Test
    public void anOptionalParamIsStillOptional() throws Exception {
        Router router = generate(OPTIONAL_SOURCE);
        // required=false, so its absence is not an error -- the guard must not
        // have been emitted for it.
        assertEquals("none", router.text("GET", "/api/opt"));
        assertEquals("hi", router.text("GET", "/api/opt?q=hi"));
    }

    @Test
    public void anAbsentRequiredBodyIsRefused() throws Exception {
        Router router = generate(OPTIONAL_SOURCE);
        Object response = router.call("POST", "/api/notes", null);
        assertNotNull("POST /api/notes matched no route", response);
        assertEquals(400, Router.statusOf(response));
        assertEquals("body", router.text2("POST", "/api/notes", "body"));
    }

    @Test
    public void twoRoutesOfTheSameShapeAreRefused() throws Exception {
        // The variable names differ; nothing a request carries does. The second
        // method could never have run.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/notes/{id}\")\n"
                + "    public String byId(@PathVariable(\"id\") String id) { return id; }\n"
                + "    @GetMapping(\"/notes/{name}\")\n"
                + "    public String byName(@PathVariable(\"name\") String name) { return name; }\n"
                + "}\n"));
        assertTrue("a shape that can never match should not compile", ctx.hasErrors());
        String all = ctx.getErrors().toString();
        assertTrue(all, all.indexOf("can never run") >= 0);
    }

    @Test
    public void twoControllersOfTheSameShapeAreRefused() throws Exception {
        // The bootstrap chains the routers and returns the first non-null answer,
        // so a collision ACROSS controllers hides the later one exactly as a
        // collision inside one does. Checking each controller alone missed it.
        ProcessorContext ctx = run(compileBoth(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/notes/{id}\")\n"
                + "    public String byId(@PathVariable(\"id\") String id) { return id; }\n"
                + "}\n",
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Other {\n"
                + "    @GetMapping(\"/notes/{name}\")\n"
                + "    public String byName(@PathVariable(\"name\") String name) { return name; }\n"
                + "}\n"));
        assertTrue("a shape claimed by two controllers should not compile", ctx.hasErrors());
        String all = ctx.getErrors().toString();
        assertTrue(all, all.indexOf("can never run") >= 0);
    }

    private static final class Router {
        private final Object instance;
        private final Method handle;
        private static Constructor<?> requestCtor;
        private static Field bodyField;
        private static Field deferredField;
        private static Field hasDeferredField;
        private static Field statusField;

        Router(Object instance, Method handle) {
            this.instance = instance;
            this.handle = handle;
        }

        Object call(String method, String target, String body) throws Exception {
            return handle.invoke(instance, request(method, target, body));
        }

        String text2(String method, String target, String body) throws Exception {
            Object response = call(method, target, body);
            assertNotNull(method + " " + target + " matched no route", response);
            return bodyOf(response);
        }

        String text(String method, String target) throws Exception {
            Object response = call(method, target, null);
            assertNotNull(method + " " + target + " matched no route", response);
            return bodyOf(response);
        }

        static int statusOf(Object response) throws Exception {
            reflect();
            return statusField.getInt(response);
        }

        static String bodyOf(Object response) throws Exception {
            reflect();
            if (hasDeferredField.getBoolean(response)) {
                // respondJson leaves the value unserialised for the writer; rendering
                // it here is what the server does at write time.
                return Json.write(deferredField.get(response));
            }
            return new String((byte[]) bodyField.get(response), "UTF-8");
        }

        private static HttpServer.Request request(String method, String target, String body)
                throws Exception {
            reflect();
            byte[] raw = (method + " " + target + " HTTP/1.1\r\nUser-Agent: probe\r\n\r\n")
                    .getBytes("UTF-8");
            return (HttpServer.Request) requestCtor.newInstance(method, target, "HTTP/1.1", raw,
                    new int[0], 0, body, Integer.valueOf(method.length() + 1),
                    Integer.valueOf(target.getBytes("UTF-8").length));
        }

        private static synchronized void reflect() throws Exception {
            if (requestCtor != null) {
                return;
            }
            Class<?> req = HttpServer.Request.class;
            requestCtor = req.getDeclaredConstructor(String.class, String.class, String.class,
                    byte[].class, int[].class, int.class, String.class, int.class, int.class);
            requestCtor.setAccessible(true);
            Class<?> res = HttpServer.Response.class;
            bodyField = res.getDeclaredField("body");
            bodyField.setAccessible(true);
            deferredField = res.getDeclaredField("deferredJson");
            deferredField.setAccessible(true);
            hasDeferredField = res.getDeclaredField("hasDeferredJson");
            hasDeferredField.setAccessible(true);
            statusField = res.getDeclaredField("status");
            statusField.setAccessible(true);
        }
    }

    private Router generate(String controllerSource) throws Exception {
        File classes = compile(controllerSource);
        ProcessorContext ctx = run(classes);
        if (ctx.hasErrors()) {
            StringBuilder sb = new StringBuilder("processor reported errors:\n");
            for (ProcessorContext.ProcessingError e : ctx.getErrors()) {
                sb.append(' ').append(e).append('\n');
            }
            fail(sb.toString());
        }
        URLClassLoader loader = new URLClassLoader(new URL[]{ classes.toURI().toURL() },
                getClass().getClassLoader());
        Class<?> controller = loader.loadClass("com.example.Notes");
        Class<?> router = loader.loadClass("com.example.NotesRouter");
        Object instance = router.getConstructor(controller).newInstance(controller.newInstance());
        return new Router(instance, router.getMethod("handle", HttpServer.Request.class));
    }

    private File compile(String controllerSource) throws Exception {
        File classes = tmp.newFolder();
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put(controllerSource.indexOf("class Notes") >= 0
                ? "com.example.Notes" : "com.example.Bad", controllerSource);
        JavaSourceCompiler.compile(sources, classes, backendClasspath());
        return classes;
    }

    /** Compiles two controllers into one output, the way a real project has them. */
    private File compileBoth(String first, String second) throws Exception {
        File classes = tmp.newFolder();
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.Notes", first);
        sources.put("com.example.Other", second);
        JavaSourceCompiler.compile(sources, classes, backendClasspath());
        return classes;
    }

    private ProcessorContext run(File classes) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classes);
        RestControllerAnnotationProcessor proc = new RestControllerAnnotationProcessor();
        List<String> cp = new java.util.ArrayList<String>();
        for (File f : backendClasspath()) {
            cp.add(f.getAbsolutePath());
        }
        ProcessorContext ctx = new ProcessorContext(classes, tmp.newFolder(), index,
                new SystemStreamLog(), tmp.newFolder(), new Properties(), null,
                Collections.<String>emptyList(), "UTF-8", cp);
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (!cls.getClassAnnotations().isEmpty()) {
                proc.processClass(cls, ctx);
            }
        }
        proc.finish(ctx);
        return ctx;
    }

    /// Where the backend runtime the generated code names actually sits. Taken from
    /// the loaded class rather than from a path, so it follows the test classpath.
    private static List<File> backendClasspath() throws Exception {
        URL url = HttpServer.class.getProtectionDomain().getCodeSource().getLocation();
        return Arrays.asList(new File(url.toURI()));
    }
}
