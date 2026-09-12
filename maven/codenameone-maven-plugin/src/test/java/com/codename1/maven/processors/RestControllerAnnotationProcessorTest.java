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
import static org.junit.Assert.assertFalse;
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
        // THE DECODER DIRECTLY, not through a request. The harness builds its wire
        // bytes with getBytes("UTF-8"), so a target carrying raw octets is
        // re-encoded on the way in and never reaches the router as what was
        // written -- an end-to-end assertion here reports a mismatch whether the
        // decoder is right or wrong, and measures the harness either way.
        Method decode = router.instance.getClass().getDeclaredMethod("decode", String.class);
        decode.setAccessible(true);
        String accented = "caf" + ((char)0xe9);
        assertEquals("the percent-encoded spelling decodes", accented,
                decode.invoke(null, "caf%C3%A9"));
        // THE SAME REQUEST, SENT RAW. A target arrives as one character per octet,
        // so this is what an unescaped accented letter looks like by the time the
        // router sees it -- and it used to come back untouched, because the decoder
        // returned early on any value with no '%' in it.
        assertEquals("and the raw spelling decodes to the same thing", accented,
                decode.invoke(null, "caf" + ((char)0xc3) + ((char)0xa9)));

        // AND THE VALIDATOR AGREES WITH THE DECODER. One character may be spelled
        // half raw and half escaped -- a raw 0xC3 then %A9 is an e-acute, and both
        // decode() above and the server read it as one. Checking each escape RUN
        // on its own saw %A9 alone, called it a stray continuation byte, and
        // answered 400 for a target this router's own decoder accepts.
        Method targetIsUtf8 = router.instance.getClass()
                .getDeclaredMethod("targetIsUtf8", String.class);
        targetIsUtf8.setAccessible(true);
        assertEquals("a character split across both spellings is a character",
                Boolean.TRUE,
                targetIsUtf8.invoke(null, "/api/notes/caf" + ((char)0xc3) + "%A9"));
        // Still refuses what is genuinely malformed: a lead byte with no
        // continuation after it, in either spelling.
        assertEquals("a truncated sequence is still refused", Boolean.FALSE,
                targetIsUtf8.invoke(null, "/api/notes/caf" + ((char)0xc3) + "("));
        assertEquals("and so is the escaped spelling of the same", Boolean.FALSE,
                targetIsUtf8.invoke(null, "/api/notes/caf%C3("));
        // '+' is a literal in a path segment; it means a space only in a query.
        assertEquals("{\"id\":\"a+b\"}", router.text("GET", "/api/notes/a+b"));
        assertEquals("{\"id\":\"42\",\"tag\":\"red\"}",
                router.text("GET", "/api/notes/42/tags/red"));
        assertEquals("{\"q\":\"hi\",\"page\":7}", router.text("GET", "/api/search?q=hi"));
        assertEquals("{\"q\":\"hi\",\"page\":3}", router.text("GET", "/api/search?q=hi&page=3"));
        // Not the default: defaultValue is documented as "used when the request
        // omits it", and "zz" is not an omission. Binding it to 7 handed the
        // handler a page the client never asked for, and neither could tell.
        Object malformed = router.call("GET", "/api/search?q=hi&page=zz", null);
        assertNotNull(malformed);
        assertEquals(400, Router.statusOf(malformed));
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
    public void theBootstrapRefusesToStartWithoutAShutdownHandler() throws Exception {
        // installShutdownHandler answers false when the self-pipe or the sigaction
        // cannot be set up -- under descriptor exhaustion, say. The bootstrap
        // ignored that, and Signals.onShutdown then got -1 from
        // awaitShutdownSignal immediately, stopped the server it had just started,
        // and called System.exit(0): a container that never served a request,
        // reporting success.
        //
        // ASSERTED ON THE EMITTED SOURCE, which is what there is. The generated
        // file is compiled straight to classes and never written anywhere a test
        // can open, and the only behavioural alternative -- calling main() -- starts
        // a server and cannot make the install fail anyway.
        String bootstrap = new RestControllerAnnotationProcessor()
                .generateBootstrap("com.example");
        int install = bootstrap.indexOf("installShutdownHandler()");
        assertTrue("the bootstrap no longer installs a shutdown handler", install >= 0);
        assertTrue("the bootstrap ignores whether the shutdown handler installed:\n"
                        + bootstrap,
                bootstrap.indexOf("if (!com.codename1.backend.Signals."
                        + "installShutdownHandler())") >= 0);
        // And it refuses rather than carrying on, which is the point.
        int throwAt = bootstrap.indexOf("IllegalStateException", install);
        assertTrue("the bootstrap does not fail when the install fails:\n" + bootstrap,
                throwAt > install);
        // Before the server is started, not after: a refusal that has already
        // bound the port is the failure mode this replaces.
        int start = bootstrap.indexOf("HttpServer.start");
        assertTrue("the check must come before the server starts:\n" + bootstrap,
                start < 0 || throwAt < start);
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
    public void aDefaultThatIsNotOfTheTypeIsRefused() throws Exception {
        // to<Type> answers its fallback for anything unparseable, so this bound
        // 0 -- and a non-empty default also skips the required-value guard, so an
        // absent parameter reached the handler as a number nobody wrote. It is
        // the author's own configuration, so it is wrong at build time or never.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/notes\")\n"
                + "    public String all(@RequestParam(value = \"limit\", "
                + "defaultValue = \"oops\") int limit) { return \"[]\"; }\n"
                + "}\n"));
        assertTrue("a default that is not an int should not compile", ctx.hasErrors());
        assertTrue(ctx.getErrors().toString(),
                ctx.getErrors().toString().indexOf("silently replaced by zero") >= 0);
    }

    @Test
    public void aWellFormedDefaultStillCompiles() throws Exception {
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/notes\")\n"
                + "    public String all(@RequestParam(value = \"limit\", "
                + "defaultValue = \"20\") int limit) { return \"[]\"; }\n"
                + "}\n"));
        assertTrue("a valid default must still compile: " + ctx.getErrors(),
                !ctx.hasErrors());
    }

    @Test
    public void anEmptyNumericValueIsRejectedRatherThanZero() throws Exception {
        // "?limit=" is a parameter the client SENT. Treating it as an omission
        // bound the default, so the handler ran on a number nobody wrote -- the
        // same defect as accepting "zz", which is already a 400.
        Router router = generate(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/notes\")\n"
                + "    public String all(@RequestParam(value = \"limit\", "
                + "defaultValue = \"20\") int limit) { return String.valueOf(limit); }\n"
                + "}\n");
        Object empty = router.call("GET", "/notes?limit=", null);
        assertNotNull("GET /notes matched no route", empty);
        assertEquals(400, Router.statusOf(empty));
        // Omitting it entirely still takes the declared default.
        Object absent = router.call("GET", "/notes", null);
        assertNotNull(absent);
        assertEquals(200, Router.statusOf(absent));
        assertEquals("20", Router.bodyOf(absent));
    }

    @Test
    public void aBodyElementOfTheWrongTypeIs400NotACrash() throws Exception {
        // Declaring List<String> does not make the elements strings. "[1]" fills
        // it with a Long, and the handler's first read as a String throws --
        // answering 500 to what is really a malformed request.
        Router router = generate(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "import java.util.List;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @PostMapping(\"/notes\")\n"
                + "    public String add(@RequestBody List<String> body) {\n"
                + "        return body.isEmpty() ? \"\" : body.get(0);\n"
                + "    }\n"
                + "}\n");
        Object wrong = router.call("POST", "/notes", "[1]");
        assertNotNull("POST /notes matched no route", wrong);
        assertEquals(400, Router.statusOf(wrong));
        // The declared shape still works.
        Object right = router.call("POST", "/notes", "[\"hi\"]");
        assertNotNull(right);
        assertEquals(200, Router.statusOf(right));
        assertEquals("hi", Router.bodyOf(right));
    }

    @Test
    public void aNestedBodyElementOfTheWrongTypeIsAlso400() throws Exception {
        // The one-level check stopped at the outer list, because a nested
        // container is not one of the scalar types it looked for. "[[1]]" then
        // reached the handler with a Long where the inner list promised a String.
        Router router = generate(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "import java.util.List;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @PostMapping(\"/rows\")\n"
                + "    public String add(@RequestBody List<List<String>> rows) {\n"
                + "        return rows.isEmpty() || rows.get(0).isEmpty() ? \"\" "
                + ": rows.get(0).get(0);\n"
                + "    }\n"
                + "}\n");
        Object wrong = router.call("POST", "/rows", "[[1]]");
        assertNotNull("POST /rows matched no route", wrong);
        assertEquals(400, Router.statusOf(wrong));
        Object right = router.call("POST", "/rows", "[[\"hi\"]]");
        assertNotNull(right);
        assertEquals(200, Router.statusOf(right));
        assertEquals("hi", Router.bodyOf(right));
    }

    @Test
    public void aGetRouteAnswersHeadUnlessOneIsDeclared() throws Exception {
        // A HEAD asks what a GET would answer, and the server's writer already
        // suppresses the body -- so a controller with only @GetMapping used to
        // answer 404 to every HEAD, which breaks health checks and cache probes.
        Router router = generate(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/notes\")\n"
                + "    public String all() { return \"[]\"; }\n"
                + "}\n");
        Object head = router.call("HEAD", "/notes", null);
        assertNotNull("HEAD /notes matched no route", head);
        assertEquals(200, Router.statusOf(head));
        assertEquals(200, Router.statusOf(router.call("GET", "/notes", null)));
    }

    @Test
    public void anExplicitHeadRouteWinsOverTheGetFallback() throws Exception {
        // The fallback must not swallow the specific case it defers to. Sorted
        // alphabetically GET comes first, so declaring both would have made the
        // HEAD route unreachable the moment the fallback was added.
        Router router = generate(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/notes\")\n"
                + "    public String all() { return \"from-get\"; }\n"
                + "    @RequestMapping(value = \"/notes\", method = \"HEAD\")\n"
                + "    @ResponseStatus(204)\n"
                + "    public void probe() { }\n"
                + "}\n");
        // JUnit 4 order: message first.
        assertEquals("the declared HEAD route must win over the GET fallback",
                204, Router.statusOf(router.call("HEAD", "/notes", null)));
        Object get = router.call("GET", "/notes", null);
        assertEquals(200, Router.statusOf(get));
        assertEquals("from-get", Router.bodyOf(get));
    }

    @Test
    public void adjacentPathVariablesAreRefused() throws Exception {
        // Nothing separates them, so the matcher hands the first variable the
        // whole remainder and then fails because a second is still owed: the
        // route compiled and answered 404 to every request, which is the worst
        // way to be wrong -- the build says fine and the endpoint does not exist.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/{left}{right}\")\n"
                + "    public String both(@PathVariable(\"left\") String left,\n"
                + "                       @PathVariable(\"right\") String right) { return left; }\n"
                + "}\n"));
        assertTrue("adjacent variables should not compile", ctx.hasErrors());
        assertTrue(ctx.getErrors().toString(),
                ctx.getErrors().toString().indexOf("adjacent") >= 0);
    }

    @Test
    public void variablesSeparatedByALiteralStillRoute() throws Exception {
        // The separated form is the one people write, and it has to keep working.
        Router router = generate(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/{left}-{right}\")\n"
                + "    public String both(@PathVariable(\"left\") String left,\n"
                + "                       @PathVariable(\"right\") String right) {\n"
                + "        return left + \"|\" + right;\n"
                + "    }\n"
                + "}\n");
        Object response = router.call("GET", "/a-b", null);
        assertNotNull("GET /a-b matched no route", response);
        assertEquals("a|b", Router.bodyOf(response));
    }

    @Test
    public void anEmptyBooleanValueIsRejectedRatherThanFalse() throws Exception {
        // "?enabled=" is a parameter the client SENT. Binding it to false hands
        // the controller a decision nobody made -- the same defect the numeric
        // bindings were fixed for, one type over.
        Router router = generate(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/flag\")\n"
                + "    public String flag(@RequestParam(value = \"enabled\", "
                + "defaultValue = \"true\") boolean enabled) { return String.valueOf(enabled); }\n"
                + "}\n");
        assertEquals(400, Router.statusOf(router.call("GET", "/flag?enabled=", null)));
        // Omitted entirely still takes the declared default, and a real value works.
        Object absent = router.call("GET", "/flag", null);
        assertEquals(200, Router.statusOf(absent));
        assertEquals("true", Router.bodyOf(absent));
        assertEquals(200, Router.statusOf(router.call("GET", "/flag?enabled=false", null)));
    }

    @Test
    public void aFloatThatOverflowsDoubleIsAlsoRejected() throws Exception {
        // The guard tested the PARSED value for infinity, but parseDouble("1e999")
        // is itself infinite -- so every double-overflowing value looked like a
        // deliberate "Infinity" and was handed to the controller.
        Router router = generate(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/scale\")\n"
                + "    public String scale(@RequestParam(\"f\") float f) { return String.valueOf(f); }\n"
                + "}\n");
        assertEquals(400, Router.statusOf(router.call("GET", "/scale?f=1e999", null)));
        assertEquals(400, Router.statusOf(router.call("GET", "/scale?f=1e100", null)));
        assertEquals(200, Router.statusOf(router.call("GET", "/scale?f=1.5", null)));
    }

    @Test
    public void deeplyNestedBodyElementsAreCheckedAtEveryLevel() throws Exception {
        // The emitter used to stop below the fifth level while the build-time
        // rule accepted the whole shape, so a declaration nested deeper was
        // checked partway and the rest reached the handler unverified.
        Router router = generate(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "import java.util.List;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @PostMapping(\"/deep\")\n"
                + "    public String add(@RequestBody "
                + "List<List<List<List<List<List<String>>>>>> deep) { return \"ok\"; }\n"
                + "}\n");
        // A number at the innermost string position, six levels down.
        Object wrong = router.call("POST", "/deep", "[[[[[[1]]]]]]");
        assertNotNull("POST /deep matched no route", wrong);
        assertEquals(400, Router.statusOf(wrong));
        Object right = router.call("POST", "/deep", "[[[[[[\"hi\"]]]]]]");
        assertNotNull(right);
        assertEquals(200, Router.statusOf(right));
    }

    @Test
    public void aDoubleTooLargeForADoubleIsRejected() throws Exception {
        // parseDouble answers INFINITY for 1e999 rather than throwing, so the
        // guard approved it and the handler ran on an infinite amount -- which
        // Json then writes back as null, giving the client neither its value nor
        // an error.
        Router router = generate(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/amount\")\n"
                + "    public String amount(@RequestParam(\"d\") double d) { return String.valueOf(d); }\n"
                + "}\n");
        Object tooLarge = router.call("GET", "/amount?d=1e999", null);
        assertNotNull("GET /amount matched no route", tooLarge);
        assertEquals(400, Router.statusOf(tooLarge));
        Object ok = router.call("GET", "/amount?d=1.5", null);
        assertNotNull(ok);
        assertEquals(200, Router.statusOf(ok));
    }

    @Test
    public void aFloatTooLargeForAFloatIsRejected() throws Exception {
        // Float.parseFloat answers INFINITY for 1e100 rather than throwing, so
        // the guard approved it and the handler ran on a number the client did
        // not send. Every other width throws and was already refused.
        Router router = generate(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/scale\")\n"
                + "    public String scale(@RequestParam(\"f\") float f) { return String.valueOf(f); }\n"
                + "}\n");
        Object tooLarge = router.call("GET", "/scale?f=1e100", null);
        assertNotNull("GET /scale matched no route", tooLarge);
        assertEquals(400, Router.statusOf(tooLarge));
        // One that fits is still served.
        Object ok = router.call("GET", "/scale?f=1.5", null);
        assertNotNull(ok);
        assertEquals(200, Router.statusOf(ok));
    }

    @Test
    public void aRelativeClassPrefixStillRoutes() throws Exception {
        // Written without the leading slash, which is the ordinary slip. Every
        // request target has one, so the route has to as well or nothing can
        // ever match it and the endpoint answers 404 while the build says fine.
        Router router = generate(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "@RequestMapping(\"api\")\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/notes\")\n"
                + "    public String all() { return \"[]\"; }\n"
                + "}\n");
        Object response = router.call("GET", "/api/notes", null);
        assertNotNull("GET /api/notes matched no route", response);
        assertEquals("[]", Router.bodyOf(response));
    }

    @Test
    public void aBodyOfDtosIsRefused() throws Exception {
        // The descriptor erases this to java.util.List, which binds. What the
        // parser actually supplies is a list of Map, so the first use of an
        // element as a Note throws and the endpoint answers 500 -- having
        // packaged perfectly.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "import java.util.List;\n"
                + "class Note { public String title = \"t\"; }\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @PostMapping(\"/notes\")\n"
                + "    public String add(@RequestBody List<Note> body) { return \"ok\"; }\n"
                + "}\n"));
        assertTrue("a body of DTOs should not compile", ctx.hasErrors());
        assertTrue(ctx.getErrors().toString(),
                ctx.getErrors().toString().indexOf("Cannot bind") >= 0);
    }

    @Test
    public void aBodyOfMapsIsStillAllowed() throws Exception {
        // What the parser really produces, so it has to keep working.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "import java.util.List;\n"
                + "import java.util.Map;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @PostMapping(\"/notes\")\n"
                + "    public String add(@RequestBody List<Map> body) { return \"ok\"; }\n"
                + "}\n"));
        assertTrue("List<Map> is what the parser produces: " + ctx.getErrors(),
                !ctx.hasErrors());
    }

    @Test
    public void aResponseInsideACollectionIsRefused() throws Exception {
        // Returning a Response IS how a route answers, and emitRoute sends it.
        // Inside a collection nothing does: it reaches Json's fallback and comes
        // back as the quoted result of its toString(). The exemption is real at
        // the top and false one level in.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "import com.codename1.backend.HttpServer;\n"
                + "import java.util.List;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/many\")\n"
                + "    public List<HttpServer.Response> many() { return null; }\n"
                + "}\n"));
        assertTrue("a collection of Response should not compile", ctx.hasErrors());
        assertTrue(ctx.getErrors().toString(),
                ctx.getErrors().toString().indexOf("cannot encode") >= 0);
    }

    @Test
    public void aDirectResponseReturnIsSentAsItStands() throws Exception {
        // Not just that it compiles: that the router SENDS it. The comparison
        // this branch turns on used the dotted source spelling of a nested class
        // while the type comes from the descriptor as HttpServer$Response, so it
        // never matched -- the branch that sends a Response was dead, and a
        // controller taking control of its own reply had that reply JSON-encoded
        // instead. Status 418 is the reply here; a JSON-encoded one would be 200.
        Router router = generate(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "import com.codename1.backend.HttpServer;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/one\")\n"
                + "    public HttpServer.Response one() {\n"
                + "        return HttpServer.Response.text(418, \"teapot\");\n"
                + "    }\n"
                + "}\n");
        Object response = router.call("GET", "/one", null);
        assertNotNull("GET /one matched no route", response);
        assertEquals(418, Router.statusOf(response));
        assertEquals("teapot", Router.bodyOf(response));
    }

    @Test
    public void aJdkReturnJsonCannotWriteIsRefused() throws Exception {
        // java.util.Date has no branch in Json.writeValue, so it reaches the
        // final one and is answered as a quoted, implementation-formatted
        // toString() -- a date the client cannot parse back, from a build and a
        // request that both reported success.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/when\")\n"
                + "    public java.util.Date when() { return null; }\n"
                + "}\n"));
        assertTrue("a JDK type Json cannot write should not compile", ctx.hasErrors());
        assertTrue(ctx.getErrors().toString(),
                ctx.getErrors().toString().indexOf("cannot encode") >= 0);
    }

    @Test
    public void aStatusOutsideTheHttpRangeIsRefused() throws Exception {
        // Copied verbatim into the router, emitted verbatim as the status line
        // and as :status -- so a typo turns a working handler into a reply the
        // client rejects or cannot frame.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/oops\")\n"
                + "    @ResponseStatus(700)\n"
                + "    public String oops() { return \"x\"; }\n"
                + "}\n"));
        assertTrue("a status outside 200..599 should not compile", ctx.hasErrors());
        assertTrue(ctx.getErrors().toString(),
                ctx.getErrors().toString().indexOf("between 200 and 599") >= 0);
    }

    @Test
    public void anInformationalStatusIsRefused() throws Exception {
        // In range for HTTP, but not an ANSWER: a generated route sends one
        // response, and a 1xx is interim -- the client waits for a final one
        // that never comes, and the writer ends the response at the headers.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/interim\")\n"
                + "    @ResponseStatus(102)\n"
                + "    public String interim() { return \"x\"; }\n"
                + "}\n"));
        assertTrue("an interim status should not compile", ctx.hasErrors());
        assertTrue(ctx.getErrors().toString(),
                ctx.getErrors().toString().indexOf("between 200 and 599") >= 0);
    }

    @Test
    public void anArrayReturnOtherThanBytesIsRefused() throws Exception {
        // Json writes byte[] as base64 and has no handling for any other array,
        // so this would be answered as the JSON string "[I@1a2b3c" while the
        // build and the request both reported success.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/ids\")\n"
                + "    public int[] ids() { return new int[0]; }\n"
                + "}\n"));
        assertTrue("an array the router cannot encode should not compile", ctx.hasErrors());
        String all = ctx.getErrors().toString();
        assertTrue(all, all.indexOf("cannot encode") >= 0);
    }

    @Test
    public void aByteArrayReturnIsStillAllowed() throws Exception {
        // The one array shape Json does handle: base64, deliberately.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/blob\")\n"
                + "    public byte[] blob() { return new byte[0]; }\n"
                + "}\n"));
        assertTrue("byte[] is encodable and must still compile: " + ctx.getErrors(),
                !ctx.hasErrors());
    }

    @Test
    public void aLiteralAndAVariableInOneControllerBothWork() throws Exception {
        // The single most ordinary pair there is. They DO overlap -- /users/me is
        // a path /users/{id} would answer -- but the router emits every route
        // with no variables before any route with one, so the literal wins its
        // own path and everything else falls through.
        Router router = generate(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/users/me\")\n"
                + "    public String me() { return \"me\"; }\n"
                + "    @GetMapping(\"/users/{id}\")\n"
                + "    public String byId(@PathVariable(\"id\") String id) { return id; }\n"
                + "}\n");
        Object mine = router.call("GET", "/users/me", null);
        assertNotNull("GET /users/me matched no route", mine);
        assertEquals("me", Router.bodyOf(mine));
        Object other = router.call("GET", "/users/42", null);
        assertNotNull("GET /users/42 matched no route", other);
        assertEquals("42", Router.bodyOf(other));
    }

    @Test
    public void aListOfDtosIsRefused() throws Exception {
        // The DESCRIPTOR erases this to java.util.List, which the encodable
        // check waves through on its own name. Every Note in the list would
        // then be written as the quoted result of its toString(), while the
        // build and the request both reported success.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "import java.util.List;\n"
                + "class Note { public String title = \"t\"; }\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/notes\")\n"
                + "    public List<Note> all() { return null; }\n"
                + "}\n"));
        assertTrue("a list of types the router cannot encode should not compile",
                ctx.hasErrors());
        String all = ctx.getErrors().toString();
        assertTrue(all, all.indexOf("cannot encode") >= 0);
    }

    @Test
    public void aVerbTheServerDoesNotRouteIsRefused() throws Exception {
        // HttpServer compares the verb with equals and answers 501 before
        // dispatch, so this route could never be reached -- and nothing said so:
        // the build passed and the endpoint simply did not exist.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @RequestMapping(value = \"/notes\", method = \"get\")\n"
                + "    public String all() { return \"[]\"; }\n"
                + "}\n"));
        assertTrue("a verb the server cannot route should not compile", ctx.hasErrors());
        String all = ctx.getErrors().toString();
        assertTrue(all, all.indexOf("does not route") >= 0);
    }

    @Test
    public void aBodyThatIsNotJsonIsRefused() throws Exception {
        Router router = generate(CONTROLLER_SOURCE);
        // bodyAsMap answers null both for "no body" and for "not JSON", so the
        // controller used to be called with null and the client saw a 404, a 500,
        // or a side effect performed on an argument it never sent.
        Object bad = router.call("POST", "/api/notes", "{not json");
        assertNotNull("POST /api/notes matched no route", bad);
        assertEquals(400, Router.statusOf(bad));
        // A body that is valid JSON still reaches the handler with its status.
        Object good = router.call("POST", "/api/notes", "{\"a\":1}");
        assertNotNull(good);
        assertEquals(201, Router.statusOf(good));
    }

    @Test
    public void aVariableMayContainTheLiteralThatFollowsIt() throws Exception {
        Router router = generate(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/download/{name}.json\")\n"
                + "    public String get(@PathVariable(\"name\") String name) { return name; }\n"
                + "}\n");
        assertEquals("foo", router.text("GET", "/download/foo.json"));
        // The value itself ends in the literal. Taking the first occurrence left
        // ".json" unconsumed and rejected a request this route does match.
        assertEquals("foo.json", router.text("GET", "/download/foo.json.json"));
        // Still one segment, and still anchored at the end.
        assertNull(router.call("GET", "/download/a/b.json", null));
        assertNull(router.call("GET", "/download/foo.jsonx", null));
    }

    @Test
    public void processingTwiceWithoutCleaningStillWorks() throws Exception {
        // The second pass of an incremental build scans target/classes, which by
        // then contains the FIRST pass's NotesRouter. The collision check looked it
        // up unconditionally and reported the processor's own output as a
        // user-defined class it would overwrite -- so every project using
        // @RestController failed its second `mvn process-classes` and only a clean
        // could get it building again.
        File classes = compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/notes\")\n"
                + "    public String all() { return \"[]\"; }\n"
                + "}\n");
        ProcessorContext first = run(classes);
        assertFalse("the first pass should be clean: " + first.getErrors(),
                first.hasErrors());
        assertTrue("the first pass must have written the router it then trips over",
                new File(classes, "com/example/NotesRouter.class").isFile());

        ProcessorContext second = run(classes);
        assertFalse("processing twice without a clean must work: " + second.getErrors(),
                second.hasErrors());
    }

    @Test
    public void aMalformedEscapeIsA400AndNotALiteral() throws Exception {
        // %ZZ is not a character, and decoding it as the three literal characters
        // handed the controller a value no client can have meant. Worse, it
        // ALIASES: %252F is a correctly escaped %2F, and if a bad escape passes
        // through as text then a check written against one spelling is defeated
        // by the other.
        Router router = generate(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/notes/{id}\")\n"
                + "    public String byId(@PathVariable(\"id\") String id) { return \"id=\" + id; }\n"
                + "}\n");
        // A GOOD escape still decodes: %41 is 'A'.
        assertEquals("id=A", router.text("GET", "/notes/%41"));

        // JUnit 4: message first.
        assertEquals("a non-hex escape is a syntax error the client can fix, so 400",
                400, Router.statusOf(router.call("GET", "/notes/%ZZ", null)));
        assertEquals("and so is a truncated one",
                400, Router.statusOf(router.call("GET", "/notes/%2", null)));

        // Two hex digits is not enough. %C3%28 is a truncated two-byte sequence,
        // and new String(_, "UTF-8") answers U+FFFD rather than failing -- so the
        // handler saw exactly what %EF%BF%BD%28 produces. One value, two spellings.
        assertEquals("malformed UTF-8 inside valid escapes is still a 400",
                400, Router.statusOf(router.call("GET", "/notes/%C3%28", null)));

        // And WELL-FORMED multi-byte UTF-8 still decodes, which is the direction a
        // guard like this breaks if it is written carelessly: %C3%A9 is a single
        // accented letter, not two characters and not an error.
        assertEquals("id=" + new String(new byte[] { (byte) 0xC3, (byte) 0xA9 }, "UTF-8"),
                router.text("GET", "/notes/%C3%A9"));
    }

    @Test
    public void anInheritedWritableIsRecognised() throws Exception {
        // Json.writeValue asks `instanceof Writable`, which is satisfied by a
        // SUPERCLASS's implementation. The check read only the directly declared
        // interfaces, so a perfectly ordinary DTO hierarchy failed to compile --
        // and failed at build time, which is the worst place to be wrong about
        // what the runtime will do.
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.Base",
                "package com.example;\n"
                + "import com.codename1.backend.Json;\n"
                + "public abstract class Base implements Json.Writable {\n"
                + "    public void writeTo(com.codename1.backend.ByteSink out) { }\n"
                + "}\n");
        sources.put("com.example.Note",
                "package com.example;\n"
                + "public class Note extends Base {\n"
                + "}\n");
        sources.put("com.example.Notes",
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/notes\")\n"
                + "    public Note one() { return new Note(); }\n"
                + "}\n");
        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(sources, classes, backendClasspath());
        ProcessorContext ctx = run(classes);
        assertFalse("a DTO inheriting Writable is encodable: " + ctx.getErrors(),
                ctx.hasErrors());
    }

    @Test
    public void aTypeThatIsNotWritableAtAllIsStillRefused() throws Exception {
        // The traversal must not turn the check off: a class with no Writable
        // anywhere in its hierarchy is still the malformed contract this refuses.
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.Plain",
                "package com.example;\n"
                + "public class Plain {\n"
                + "    public String name;\n"
                + "}\n");
        sources.put("com.example.Notes",
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/notes\")\n"
                + "    public Plain one() { return new Plain(); }\n"
                + "}\n");
        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(sources, classes, backendClasspath());
        ProcessorContext ctx = run(classes);
        assertTrue("a type Json cannot write must still be refused", ctx.hasErrors());
    }

    @Test
    public void aParameterWithTwoBindingAnnotationsIsRefused() throws Exception {
        // The binding chain is priority-ordered, so this used to bind the header
        // and ignore the @RequestParam without a word. For an authentication input
        // that is the difference between a header a proxy controls and a query
        // string the caller writes -- and the declaration named both, so nobody
        // reading the code could tell which one won.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Bad {\n"
                + "    @GetMapping(\"/whoami\")\n"
                + "    public String who(@RequestHeader(\"Authorization\")\n"
                + "                      @RequestParam(\"token\") String token) {\n"
                + "        return token;\n"
                + "    }\n"
                + "}\n"));
        assertTrue("two binding annotations on one parameter should not compile",
                ctx.hasErrors());
        String all = ctx.getErrors().toString();
        assertTrue(all, all.indexOf("more than one binding annotation") >= 0);
    }

    @Test
    public void aResponseReturnWithAResponseStatusIsRefused() throws Exception {
        // The Response the handler builds carries its own status and is returned
        // untouched, so the annotation is a promise nothing keeps: @ResponseStatus
        // (201) on a method that returns Response.text(200, ...) sends 200.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "import com.codename1.backend.HttpServer;\n"
                + "@RestController\n"
                + "public class Bad {\n"
                + "    @PostMapping(\"/notes\")\n"
                + "    @ResponseStatus(201)\n"
                + "    public HttpServer.Response add() {\n"
                + "        return HttpServer.Response.text(200, \"ok\");\n"
                + "    }\n"
                + "}\n"));
        assertTrue("an ignored @ResponseStatus should not compile", ctx.hasErrors());
        String all = ctx.getErrors().toString();
        assertTrue(all, all.indexOf("silently ignored") >= 0);
    }

    @Test
    public void aResponseReturnWithoutAResponseStatusIsFine() throws Exception {
        // The shape the rule protects: returning a Response is the escape hatch,
        // and it must stay usable.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "import com.codename1.backend.HttpServer;\n"
                + "public class Fine {\n"
                + "}\n"));
        assertFalse("a class with no controller annotation is not our business: "
                + ctx.getErrors(), ctx.hasErrors());

        Router router = generate(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "import com.codename1.backend.HttpServer;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @PostMapping(\"/notes\")\n"
                + "    public HttpServer.Response add() {\n"
                + "        return HttpServer.Response.text(201, \"made\");\n"
                + "    }\n"
                + "}\n");
        assertEquals("the handler's own status is the one that is sent",
                201, Router.statusOf(router.call("POST", "/notes", null)));
    }

    @Test
    public void anOptionalPrimitiveWithoutADefaultIsRefused() throws Exception {
        // required=false says "the client may omit this", and an int cannot hold
        // that: the converter substitutes 0 and the handler cannot tell an omitted
        // value from a client that sent zero.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Bad {\n"
                + "    @GetMapping(\"/notes\")\n"
                + "    public String all(@RequestParam(value = \"limit\", required = false)\n"
                + "                      int limit) { return \"\" + limit; }\n"
                + "}\n"));
        assertTrue("an optional primitive should not compile", ctx.hasErrors());
        String all = ctx.getErrors().toString();
        assertTrue(all, all.indexOf("cannot hold") >= 0);
    }

    @Test
    public void anOptionalParameterWithADefaultIsFine() throws Exception {
        // The way out of the rule has to keep working, or the rule is just a wall.
        // Note the remedy is a defaultValue and NOT a boxed type: path, query and
        // header parameters bind to String and the primitives only, which is why
        // the error message does not suggest one.
        ProcessorContext defaulted = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Defaulted {\n"
                + "    @GetMapping(\"/notes\")\n"
                + "    public String all(@RequestParam(value = \"limit\", required = false,\n"
                + "                      defaultValue = \"10\") int limit) { return \"\" + limit; }\n"
                + "}\n"));
        assertFalse("a default answers the question: " + defaulted.getErrors(),
                defaulted.hasErrors());

        // And a String stays optional without one: it can already be null.
        ProcessorContext text = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Text {\n"
                + "    @GetMapping(\"/notes\")\n"
                + "    public String all(@RequestParam(value = \"q\", required = false)\n"
                + "                      String q) { return \"\" + q; }\n"
                + "}\n"));
        assertFalse("a String carries absent as null: " + text.getErrors(),
                text.hasErrors());
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

    @Test
    public void aGetInOneControllerAndAHeadInAnotherAreRefused() throws Exception {
        // A generated GET block also answers HEAD, so these two DO compete even
        // though the verbs differ -- and across controllers nothing orders them:
        // the bootstrap tries the routers in turn, so whichever it lists first
        // takes the HEAD and the declared handler never runs. Inside ONE
        // controller the same pair is fine, because the comparator emits HEAD's
        // block ahead of GET's fallback.
        ProcessorContext ctx = run(compileBoth(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/notes\")\n"
                + "    public String all() { return \"[]\"; }\n"
                + "}\n",
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Other {\n"
                + "    @RequestMapping(value = \"/notes\", method = \"HEAD\")\n"
                + "    public void probe() { }\n"
                + "}\n"));
        assertTrue("a HEAD hidden by another controller's GET should not compile",
                ctx.hasErrors());
    }

    @Test
    public void aGetAndAHeadInTheSameControllerStillCompile() throws Exception {
        // The other side of that rule. Making the pair collide across controllers
        // must not make the ordinary declaration -- both in one class, which is
        // what the cross-controller message tells people to do -- unwritable.
        ProcessorContext ctx = run(compileBoth(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/notes\")\n"
                + "    public String all() { return \"[]\"; }\n"
                + "    @RequestMapping(value = \"/notes\", method = \"HEAD\")\n"
                + "    public void probe() { }\n"
                + "}\n",
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Other {\n"
                + "    @GetMapping(\"/other\")\n"
                + "    public String other() { return \"x\"; }\n"
                + "}\n"));
        assertFalse("GET and HEAD in one controller are ordered, not ambiguous: "
                + ctx.getErrors(), ctx.hasErrors());
    }

    @Test
    public void aMapBodyKeyedByANonStringIsRefused() throws Exception {
        // A JSON object's names are always strings. Long is a fine body VALUE --
        // every JSON integer arrives as one -- so the element rule approved
        // Map<Long,String>, and the emitted shape check walks values() only. The
        // handler then got a map whose keys violate its own declaration: typed
        // iteration throws, and get(1L) misses the value the client sent.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Counts {\n"
                + "    @PostMapping(\"/counts\")\n"
                + "    public String put(@RequestBody java.util.Map<Long, String> counts) {\n"
                + "        return \"ok\";\n"
                + "    }\n"
                + "}\n"));
        assertTrue("a map keyed by Long cannot be decoded and should not compile",
                ctx.hasErrors());
        String all = ctx.getErrors().toString();
        assertTrue(all, all.indexOf("names are strings") >= 0);
    }

    @Test
    public void routesWithDisjointSuffixesAreNotAmbiguous() throws Exception {
        // No request satisfies both: one ends .json, the other .xml. The overlap
        // check treated every pair of variable-carrying segments as colliding, so
        // a controller that cannot be ambiguous failed to compile -- and the
        // matcher it would have generated handles literals around a variable
        // perfectly well.
        Router router = generate(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/{name}.json\")\n"
                + "    public String json(@PathVariable(\"name\") String name) {\n"
                + "        return \"json:\" + name;\n"
                + "    }\n"
                + "    @GetMapping(\"/{name}.xml\")\n"
                + "    public String xml(@PathVariable(\"name\") String name) {\n"
                + "        return \"xml:\" + name;\n"
                + "    }\n"
                + "}\n");
        assertEquals("json:a", router.text("GET", "/a.json"));
        assertEquals("xml:a", router.text("GET", "/a.xml"));
    }

    @Test
    public void routesWithOverlappingSuffixesAreStillRefused() throws Exception {
        // And the check must still bite where the two CAN collide: a bare
        // variable matches "a.json" as readily as {name}.json does.
        ProcessorContext ctx = run(compileBoth(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/{name}.json\")\n"
                + "    public String json(@PathVariable(\"name\") String name) { return name; }\n"
                + "}\n",
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Other {\n"
                + "    @GetMapping(\"/{anything}\")\n"
                + "    public String any(@PathVariable(\"anything\") String a) { return a; }\n"
                + "}\n"));
        assertTrue("a bare variable answers /a.json too, so these do collide",
                ctx.hasErrors());
    }

    @Test
    public void aMapReturnKeyedByANonStringIsRefused() throws Exception {
        // Json.writeValue calls String.valueOf on every map key whatever it is,
        // so the keys come back as object identity -- "[B@1a2b3c" -- which is
        // different on every run and describes nothing. The key was being checked
        // as though it were a value, and byte[] is a perfectly good value.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Blobs {\n"
                + "    @GetMapping(\"/blobs\")\n"
                + "    public java.util.Map<byte[], String> all() { return null; }\n"
                + "}\n"));
        assertTrue("a map keyed by byte[] cannot be written as JSON", ctx.hasErrors());
    }

    @Test
    public void aMapReturnKeyedByStringIsAccepted() throws Exception {
        // The shape the rule is protecting has to keep working.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Counts {\n"
                + "    @GetMapping(\"/counts\")\n"
                + "    public java.util.Map<String, Long> all() { return null; }\n"
                + "}\n"));
        assertFalse("Map<String,Long> is exactly what Json writes: " + ctx.getErrors(),
                ctx.hasErrors());
    }

    @Test
    public void anUnboundedWildcardReturnElementIsRefused() throws Exception {
        // "?" is not a primitive, it is UNKNOWN. Reaching the no-dot branch it was
        // read as one, so List<?> was approved and a handler returning a DTO or a
        // Date inside it got Json's quoted toString() fallback -- the malformed
        // contract this validation exists to refuse for List<Object>.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @GetMapping(\"/notes\")\n"
                + "    public java.util.List<?> all() { return null; }\n"
                + "}\n"));
        assertTrue("a List<?> return says nothing about what Json must write",
                ctx.hasErrors());
    }

    @Test
    public void aBoundedWildcardElementIsCheckedLikeItsBound() throws Exception {
        // List<? extends String> was accepted with NO runtime element check at
        // all, because every consumer read a bounded wildcard as "claims
        // nothing". The bound is a claim: a body of [1] reached the handler as a
        // list holding a Long, and the first typed read answered 500 where a 400
        // was owed. Normalising the wildcard where type arguments are produced
        // fixes the validation and the emitted check together.
        Router router = generate(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @PostMapping(\"/notes\")\n"
                + "    public String add(@RequestBody"
                + " java.util.List<? extends String> notes) {\n"
                + "        return \"ok\";\n"
                + "    }\n"
                + "}\n");
        assertEquals(200, Router.statusOf(router.call("POST", "/notes", "[\"a\"]")));
        // JUnit 4 order: message first.
        assertEquals("a Long where the bound promised String is the client's mistake, "
                        + "so it is a 400 and not a 500",
                400, Router.statusOf(router.call("POST", "/notes", "[1]")));
    }

    @Test
    public void aSuperBoundedWildcardElementIsNotChecked() throws Exception {
        // The other direction, and it must NOT be normalised the same way:
        // List<? super String> allows a String or any supertype, so an element
        // check against String would reject values the declaration permits.
        Router router = generate(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Notes {\n"
                + "    @PostMapping(\"/notes\")\n"
                + "    public String add(@RequestBody"
                + " java.util.List<? super String> notes) {\n"
                + "        return \"ok\";\n"
                + "    }\n"
                + "}\n");
        assertEquals("? super String permits a Long element, so nothing may reject it",
                200, Router.statusOf(router.call("POST", "/notes", "[1]")));
    }

    @Test
    public void aMapBodyKeyedByABoundedWildcardIsRefused() throws Exception {
        // "? extends Long" is not the same claim as "?". The bound still promises
        // every key is a Long, so `for (Long key : body.keySet())` compiles and
        // then meets the Strings a JSON object really produces -- a 500 for what
        // is a 400. Exempting everything starting with '?' let it through.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Bounded {\n"
                + "    @PostMapping(\"/counts\")\n"
                + "    public String put(@RequestBody"
                + " java.util.Map<? extends Long, String> counts) {\n"
                + "        return \"ok\";\n"
                + "    }\n"
                + "}\n"));
        assertTrue("a bounded wildcard key is still a promise about the key type",
                ctx.hasErrors());
    }

    @Test
    public void aMapBodyKeyedByAnUnboundedWildcardIsAccepted() throws Exception {
        // The unbounded one claims nothing, so it stays legal -- the rule must
        // separate "any key" from "a Long key spelled as a wildcard".
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Unbounded {\n"
                + "    @PostMapping(\"/counts\")\n"
                + "    public String put(@RequestBody java.util.Map<?, ?> counts) {\n"
                + "        return \"ok\";\n"
                + "    }\n"
                + "}\n"));
        assertFalse("Map<?,?> claims nothing about its keys: " + ctx.getErrors(),
                ctx.hasErrors());
    }

    @Test
    public void aMapBodyKeyedByStringIsAccepted() throws Exception {
        // The rule must not swallow the shape it is protecting.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Counts {\n"
                + "    @PostMapping(\"/counts\")\n"
                + "    public String put(@RequestBody java.util.Map<String, Long> counts) {\n"
                + "        return \"ok\";\n"
                + "    }\n"
                + "}\n"));
        assertFalse("Map<String,Long> is exactly what a JSON object decodes to: "
                + ctx.getErrors(), ctx.hasErrors());
    }

    @Test
    public void anOverflowingDoubleDefaultIsRefused() throws Exception {
        // Double.parseDouble("1e999") answers infinity instead of throwing, so
        // this declaration was approved while the RUNTIME guard rejects the same
        // spelling arriving in a request: omit the parameter and the controller
        // runs on an infinity, send it and the client gets a 400. Two answers for
        // one value.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Rates {\n"
                + "    @GetMapping(\"/rate\")\n"
                + "    public String rate(@RequestParam(value = \"r\", defaultValue = \"1e999\")\n"
                + "                       double r) { return String.valueOf(r); }\n"
                + "}\n"));
        assertTrue("a default that parses to infinity should not compile", ctx.hasErrors());
    }

    @Test
    public void anExplicitInfinityDefaultIsStillAllowed() throws Exception {
        // The spelling is what says an infinity was meant, which is the same test
        // the generated guard uses -- so the two cannot disagree about which
        // values are infinities.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Rates {\n"
                + "    @GetMapping(\"/rate\")\n"
                + "    public String rate(@RequestParam(value = \"r\", defaultValue = \"Infinity\")\n"
                + "                       double r) { return String.valueOf(r); }\n"
                + "}\n"));
        assertFalse("a deliberate Infinity is not an overflow: " + ctx.getErrors(),
                ctx.hasErrors());
    }

    @Test
    public void anOverflowingFloatDefaultIsRefused() throws Exception {
        // The float branch had the bug in the other direction: Double.isInfinite
        // was its "did they mean it" test, and Double.parseDouble("1e999") is
        // itself infinite, so every double-overflowing default read as deliberate.
        ProcessorContext ctx = run(compile(
                "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController\n"
                + "public class Rates {\n"
                + "    @GetMapping(\"/rate\")\n"
                + "    public String rate(@RequestParam(value = \"r\", defaultValue = \"1e999\")\n"
                + "                       float r) { return String.valueOf(r); }\n"
                + "}\n"));
        assertTrue("a float default that parses to infinity should not compile",
                ctx.hasErrors());
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
        // Read out of the source rather than guessed from a pair of known names:
        // javac wants the file to match the class, so a test that declared a
        // third name failed to COMPILE and reported that as its result.
        int at = controllerSource.indexOf("public class ");
        String name = controllerSource.substring(at + "public class ".length(),
                controllerSource.indexOf(' ', at + "public class ".length() + 1));
        sources.put("com.example." + name.trim(), controllerSource);
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
