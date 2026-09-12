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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Proves the server half of the shared `@RestClient` contract: one annotated
/// interface produces a synchronous server interface and a dispatcher that
/// actually routes, binds and invokes. The dispatcher is loaded and CALLED here
/// rather than merely inspected -- a generated router that compiles but routes
/// nowhere is the failure this test exists to catch.
public class RestServerAnnotationProcessorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Before
    public void enableServerHalf() {
        System.setProperty("cn1.restServer", "true");
    }

    @After
    public void disableServerHalf() {
        System.clearProperty("cn1.restServer");
    }

    private static final String DTO_SOURCE =
            "package com.example;\n"
                    + "public class Pet {\n"
                    + "    public long id;\n"
                    + "    public String name;\n"
                    + "    public boolean good;\n"
                    + "    public double weight;\n"
                    + "    public java.util.List<Tag> tags;\n"
                    + "    public java.util.List<Integer> weights;\n"
                    + "    public Pet() {}\n"
                    + "}\n";

    private static final String TAG_SOURCE =
            "package com.example;\n"
                    + "public class Tag {\n"
                    + "    public String label;\n"
                    + "    public int weight;\n"
                    + "    public Tag() {}\n"
                    + "}\n";

    private static final String API_SOURCE =
            "package com.example;\n"
                    + "import com.codename1.annotations.rest.*;\n"
                    + "import com.codename1.io.rest.Response;\n"
                    + "import com.codename1.util.OnComplete;\n"
                    + "@RestClient\n"
                    + "public interface GreeterApi {\n"
                    + "    @GET(\"/greet/{name}\")\n"
                    + "    void greet(@Path(\"name\") String name,\n"
                    + "               @Query(\"loud\") String loud,\n"
                    + "               OnComplete<Response<String>> callback);\n"
                    + "    @POST(\"/echo\")\n"
                    + "    void echo(@Body String body, OnComplete<Response<String>> callback);\n"
                    + "    @GET(\"/whoami\")\n"
                    + "    void whoami(@Header(\"X-User\") String user,\n"
                    + "                @Cookie(\"session\") String session,\n"
                    + "                OnComplete<Response<String>> callback);\n"
                    + "    @POST(\"/pet\")\n"
                    + "    void addPet(@Body Pet pet, OnComplete<Response<Pet>> callback);\n"
                    + "    @GET(\"/pets\")\n"
                    + "    void listPets(OnComplete<Response<java.util.List<Pet>>> callback);\n"
                    + "    @POST(\"/weights\")\n"
                    + "    void weights(@Body java.util.List<Integer> weights,\n"
                    + "                 OnComplete<Response<String>> callback);\n"
                    + "    @POST(\"/labels\")\n"
                    + "    void labels(@Body java.util.Set<String> labels,\n"
                    + "                OnComplete<Response<String>> callback);\n"
                    + "}\n";

    @Test
    public void aStringBodyMustHaveArrivedAsAString() throws Exception {
        // Declaring @Body String does not make the body a string. A client sending
        // the number 1 or an object used to be coerced with String.valueOf, so the
        // handler saw "1" or "{a=1}" as though those had been sent as JSON strings,
        // instead of the IllegalArgumentException the transport turns into a 400.
        File classes = compileApi();
        ProcessorContext ctx = runProcessor(classes);
        assertNoErrors(ctx);
        URLClassLoader loader = new URLClassLoader(
                new URL[]{classes.toURI().toURL(), testClassesDir().toURI().toURL()},
                getClass().getClassLoader());
        Class<?> serverItf = loader.loadClass("com.example.GreeterApiServer");
        final Object[] received = new Object[1];
        Object handler = Proxy.newProxyInstance(loader, new Class<?>[]{serverItf},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method m, Object[] args) {
                        if ("echo".equals(m.getName())) {
                            received[0] = args[0];
                            return args[0];
                        }
                        return null;
                    }
                });
        Class<?> dispatcherClass = loader.loadClass("com.example.GreeterApiDispatcher");
        Object dispatcher = dispatcherClass.getConstructor(serverItf).newInstance(handler);
        Method dispatch = dispatcherClass.getMethod("dispatch",
                String.class, String.class, java.util.Map.class, Object.class);

        dispatch.invoke(dispatcher, "POST", "/echo", null, "hello");
        assertEquals("a genuine string body must still arrive", "hello", received[0]);

        received[0] = null;
        try {
            dispatch.invoke(dispatcher, "POST", "/echo", null, Long.valueOf(1));
            fail("a JSON number reaching a String body should be refused, not stringified");
        } catch (java.lang.reflect.InvocationTargetException expected) {
            assertTrue(String.valueOf(expected.getCause()),
                    expected.getCause() instanceof IllegalArgumentException);
        }
        assertNull("the handler must not have been called at all", received[0]);
        loader.close();
    }

    @Test
    public void aScalarBodyStillArrivesThroughItsTextForm() throws Exception {
        // The other half of the rule above: a declared `int` body IS fed by
        // rendering whatever arrived and parsing it, so that a JSON number reaching
        // an int body behaves like one reaching an int query parameter. Making the
        // string helper strict without splitting it broke exactly this.
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.TallyApi",
                "package com.example;\n"
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient\n"
                + "public interface TallyApi {\n"
                + "    @POST(\"/tally\")\n"
                + "    void tally(@Body int count, OnComplete<Response<String>> callback);\n"
                + "}\n");
        File classes = compileSources(sources);
        ProcessorContext ctx = runProcessor(classes);
        assertNoErrors(ctx);
        URLClassLoader loader = new URLClassLoader(
                new URL[]{classes.toURI().toURL(), testClassesDir().toURI().toURL()},
                getClass().getClassLoader());
        Class<?> serverItf = loader.loadClass("com.example.TallyApiServer");
        final Object[] received = new Object[1];
        Object handler = Proxy.newProxyInstance(loader, new Class<?>[]{serverItf},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method m, Object[] args) {
                        received[0] = args[0];
                        return "ok";
                    }
                });
        Class<?> dispatcherClass = loader.loadClass("com.example.TallyApiDispatcher");
        Object dispatcher = dispatcherClass.getConstructor(serverItf).newInstance(handler);
        Method dispatch = dispatcherClass.getMethod("dispatch",
                String.class, String.class, java.util.Map.class, Object.class);
        // What the JSON reader really produces for the body `7`.
        dispatch.invoke(dispatcher, "POST", "/tally", null, Long.valueOf(7));
        assertEquals(Integer.valueOf(7), received[0]);
        loader.close();
    }

    @Test
    public void anOverflowingFloatingPointQueryIsRefused() throws Exception {
        // parseDouble and valueOf do not FAIL on a value too large: 1e999 comes
        // back as infinity, so the handler ran on a number the client never sent
        // and Json wrote it back as null -- neither the value nor an error. The
        // boxed helpers had no check at all, and the float one asked
        // !Double.isInfinite(d), which parseDouble("1e999") already satisfies.
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.RateApi",
                "package com.example;\n"
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient\n"
                + "public interface RateApi {\n"
                + "    @GET(\"/rate\")\n"
                + "    void rate(@Query(\"d\") double d, @Query(\"bd\") Double bd,\n"
                + "              @Query(\"f\") float f, @Query(\"bf\") Float bf,\n"
                + "              OnComplete<Response<String>> callback);\n"
                + "}\n");
        File classes = compileSources(sources);
        ProcessorContext ctx = runProcessor(classes);
        assertNoErrors(ctx);
        URLClassLoader loader = new URLClassLoader(
                new URL[]{classes.toURI().toURL(), testClassesDir().toURI().toURL()},
                getClass().getClassLoader());
        Class<?> dispatcherClass = loader.loadClass("com.example.RateApiDispatcher");

        // Every one of the four bindings, because each reached the value by its
        // own helper and only one of them was guarded at all.
        String[][] cases = {
                {"d", "1e999"}, {"bd", "1e999"}, {"f", "1e50"}, {"bf", "1e50"},
        };
        for (int i = 0; i < cases.length; i++) {
            assertRefused(loader, dispatcherClass, cases[i][0], cases[i][1]);
        }

        // And a value that really spells an infinity is still accepted, which is
        // what the parse means by it -- the guard is about overflow, not about
        // infinities the client asked for.
        Object answered = dispatch(loader, dispatcherClass, "d", "Infinity");
        assertNotNull("a deliberate Infinity must still bind", answered);
        loader.close();
    }

    private void assertRefused(URLClassLoader loader, Class<?> dispatcherClass,
                               String param, String value) throws Exception {
        try {
            dispatch(loader, dispatcherClass, param, value);
            fail(param + "=" + value + " overflows and should not reach the handler");
        } catch (java.lang.reflect.InvocationTargetException expected) {
            Throwable cause = expected.getCause();
            assertTrue(param + "=" + value + " failed with " + cause,
                    cause instanceof NumberFormatException
                            || cause instanceof IllegalArgumentException);
        }
    }

    /** Calls the generated dispatcher with one query parameter set. */
    private Object dispatch(URLClassLoader loader, Class<?> dispatcherClass,
                            String param, String value) throws Exception {
        Class<?> serverItf = loader.loadClass("com.example.RateApiServer");
        Object handler = Proxy.newProxyInstance(loader, new Class<?>[]{serverItf},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method m, Object[] args) {
                        return "ok";
                    }
                });
        Object dispatcher = dispatcherClass.getConstructor(serverItf).newInstance(handler);
        Method d = dispatcherClass.getMethod("dispatch",
                String.class, String.class, java.util.Map.class, Object.class);
        return d.invoke(dispatcher, "GET", "/rate?" + param + "=" + value, null, null);
    }

    @Test
    public void anEmbeddedPlaceholderIsBoundLikeTheClientBindsIt() throws Exception {
        // The CLIENT generator substitutes {name} anywhere in the template, so
        // /files/{name}.json has always produced a working client. The server
        // generator only recognised a placeholder that owned a whole segment, so
        // turning server generation on reported that @Path("name") was absent and
        // refused a contract that already worked -- two halves of one annotation
        // disagreeing about what it means.
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.FileApi",
                "package com.example;\n"
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient\n"
                + "public interface FileApi {\n"
                + "    @GET(\"/files/{name}.json\")\n"
                + "    void get(@Path(\"name\") String name,\n"
                + "             OnComplete<Response<String>> callback);\n"
                + "}\n");
        File classes = compileSources(sources);
        ProcessorContext ctx = runProcessor(classes);
        assertNoErrors(ctx);

        URLClassLoader loader = new URLClassLoader(
                new URL[]{classes.toURI().toURL(), testClassesDir().toURI().toURL()},
                getClass().getClassLoader());
        Class<?> serverItf = loader.loadClass("com.example.FileApiServer");
        final Object[] seen = new Object[1];
        Object handler = Proxy.newProxyInstance(loader, new Class<?>[]{serverItf},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method m, Object[] args) {
                        seen[0] = args[0];
                        return "ok";
                    }
                });
        Class<?> dispatcherClass = loader.loadClass("com.example.FileApiDispatcher");
        Object dispatcher = dispatcherClass.getConstructor(serverItf).newInstance(handler);
        Method dispatch = dispatcherClass.getMethod("dispatch",
                String.class, String.class, java.util.Map.class, Object.class);

        assertNotNull("the route did not match at all",
                dispatch.invoke(dispatcher, "GET", "/files/report.json", null, null));
        assertEquals("the value between the literals is what binds",
                "report", seen[0]);

        // THE SAME REQUEST IN ITS TWO SPELLINGS. A target arrives as one character
        // per request octet, so an accented name may be sent percent-encoded or
        // raw and both are valid. The raw one used to bind untouched -- two
        // characters where the escaped spelling bound one -- because the decoder
        // returned early whenever the value held no '%'.
        seen[0] = null;
        assertNotNull("the escaped spelling must match",
                dispatch.invoke(dispatcher, "GET", "/files/caf%C3%A9.json", null, null));
        Object escapedBinding = seen[0];
        assertEquals("caf" + ((char)0xe9), escapedBinding);
        seen[0] = null;
        assertNotNull("and so must the raw one",
                dispatch.invoke(dispatcher, "GET",
                        "/files/caf" + ((char)0xc3) + ((char)0xa9) + ".json", null, null));
        assertEquals("both spellings of one request bind the same value",
                escapedBinding, seen[0]);

        // The literals are part of the match, not decoration.
        seen[0] = null;
        assertNull("a different extension must not match",
                dispatch.invoke(dispatcher, "GET", "/files/report.xml", null, null));
        assertNull("and an empty value is not a segment",
                dispatch.invoke(dispatcher, "GET", "/files/.json", null, null));
    }

    @Test
    public void headIsAnsweredByTheGetRoute() throws Exception {
        // RFC 9110 defines HEAD as GET without the content, HttpServer routes it,
        // and it strips the body itself on both protocols. The generated
        // dispatcher required the verb to equal GET exactly, so a contract-served
        // path answered 404 to a HEAD that the SAME path served through
        // @RestController answered normally. Two generators in one product
        // disagreeing about one request.
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.HeadApi",
                "package com.example;\n"
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient\n"
                + "public interface HeadApi {\n"
                + "    @GET(\"/thing\")\n"
                + "    void read(OnComplete<Response<String>> callback);\n"
                + "    @POST(\"/thing\")\n"
                + "    void write(@Body String body, OnComplete<Response<String>> callback);\n"
                + "}\n");
        File classes = compileSources(sources);
        ProcessorContext ctx = runProcessor(classes);
        assertNoErrors(ctx);

        URLClassLoader loader = new URLClassLoader(
                new URL[]{classes.toURI().toURL(), testClassesDir().toURI().toURL()},
                getClass().getClassLoader());
        Class<?> serverItf = loader.loadClass("com.example.HeadApiServer");
        final String[] called = new String[1];
        Object handler = Proxy.newProxyInstance(loader, new Class<?>[]{serverItf},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method m, Object[] args) {
                        called[0] = m.getName();
                        return "ok";
                    }
                });
        Class<?> dispatcherClass = loader.loadClass("com.example.HeadApiDispatcher");
        Object dispatcher = dispatcherClass.getConstructor(serverItf).newInstance(handler);
        Method dispatch = dispatcherClass.getMethod("dispatch",
                String.class, String.class, java.util.Map.class, Object.class);
        Method hasRoute = dispatcherClass.getMethod("hasRoute", String.class, String.class);

        assertTrue("HEAD must have a route wherever GET does",
                (Boolean) hasRoute.invoke(dispatcher, "HEAD", "/thing"));
        called[0] = null;
        assertNotNull("HEAD must reach the GET handler",
                dispatch.invoke(dispatcher, "HEAD", "/thing", null, null));
        assertEquals("and it must be the GET operation that runs", "read", called[0]);

        // The two halves that must not have moved: GET still works, and HEAD is
        // not a skeleton key to the other verbs.
        called[0] = null;
        assertNotNull(dispatch.invoke(dispatcher, "GET", "/thing", null, null));
        assertEquals("read", called[0]);
        assertFalse("HEAD must not match a route that is not a GET",
                (Boolean) hasRoute.invoke(dispatcher, "HEAD", "/missing"));
        assertNull("an unrelated verb still does not match",
                dispatch.invoke(dispatcher, "DELETE", "/thing", null, null));
    }

    @Test
    public void aFlagStyleQueryBindsAsPresentAndEmpty() throws Exception {
        // "?flag" with no '=' is PRESENT and empty.
        // HttpServer.Request.queryParam answers "" for it deliberately, and the
        // generated helper required the '=' and answered null -- so the same bytes
        // bound one way through a @RestController and another through a contract,
        // which reaches required-versus-default handling before the handler sees it.
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.FlagApi",
                "package com.example;\n"
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient\n"
                + "public interface FlagApi {\n"
                + "    @GET(\"/search\")\n"
                + "    void search(@Query(\"flag\") String flag,\n"
                + "                OnComplete<Response<String>> callback);\n"
                + "}\n");
        File classes = compileSources(sources);
        ProcessorContext ctx = runProcessor(classes);
        assertNoErrors(ctx);

        URLClassLoader loader = new URLClassLoader(
                new URL[]{classes.toURI().toURL(), testClassesDir().toURI().toURL()},
                getClass().getClassLoader());
        Class<?> serverItf = loader.loadClass("com.example.FlagApiServer");
        final Object[] seen = new Object[1];
        Object handler = Proxy.newProxyInstance(loader, new Class<?>[]{serverItf},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method m, Object[] args) {
                        seen[0] = args[0];
                        return "ok";
                    }
                });
        Class<?> dispatcherClass = loader.loadClass("com.example.FlagApiDispatcher");
        Object dispatcher = dispatcherClass.getConstructor(serverItf).newInstance(handler);
        Method dispatch = dispatcherClass.getMethod("dispatch",
                String.class, String.class, java.util.Map.class, Object.class);

        seen[0] = "unset";
        assertNotNull(dispatch.invoke(dispatcher, "GET", "/search?flag", null, null));
        assertEquals("a binding with no '=' is present and empty", "", seen[0]);

        // The spellings around it must not have moved.
        seen[0] = "unset";
        assertNotNull(dispatch.invoke(dispatcher, "GET", "/search?flag=", null, null));
        assertEquals("an explicit empty value is still empty", "", seen[0]);
        seen[0] = "unset";
        assertNotNull(dispatch.invoke(dispatcher, "GET", "/search?flag=on", null, null));
        assertEquals("a value still binds", "on", seen[0]);
        seen[0] = "unset";
        assertNotNull(dispatch.invoke(dispatcher, "GET", "/search?other", null, null));
        assertNull("a parameter that is genuinely absent is still null", seen[0]);
        seen[0] = "unset";
        assertNotNull(dispatch.invoke(dispatcher, "GET", "/search?flagged", null, null));
        assertNull("and a name this one is a prefix of is not this one", seen[0]);
    }

    @Test
    public void twoPlaceholdersInOneSegmentAreRefusedWithAReason() throws Exception {
        // Supporting one embedded placeholder does not mean guessing at two.
        // "{a}-{b}" gives no way to decide where the first value ends, and a
        // server that picks one binds something the client never meant -- so the
        // developer is told, rather than left with a route that never matches.
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.PairApi",
                "package com.example;\n"
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient\n"
                + "public interface PairApi {\n"
                + "    @GET(\"/pair/{a}-{b}\")\n"
                + "    void pair(@Path(\"a\") String a, @Path(\"b\") String b,\n"
                + "              OnComplete<Response<String>> callback);\n"
                + "}\n");
        ProcessorContext ctx = runProcessor(compileSources(sources));
        assertTrue("two placeholders in one segment cannot be split", ctx.hasErrors());
        String all = ctx.getErrors().toString();
        assertTrue(all, all.indexOf("more than one placeholder") >= 0);
    }

    @Test
    public void aPlaceholderRepeatedInTwoSegmentsIsRefused() throws Exception {
        // Every other check passes for this route: the @Path finds a placeholder,
        // the placeholder finds its @Path, and neither segment holds two. The two
        // halves still describe different routes -- the client can only send
        // /pairs/a/a, while the server accepts /pairs/a/b and reads "a".
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.PairApi",
                "package com.example;\n"
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient\n"
                + "public interface PairApi {\n"
                + "    @GET(\"/pairs/{id}/{id}\")\n"
                + "    void pair(@Path(\"id\") String id,\n"
                + "              OnComplete<Response<String>> callback);\n"
                + "}\n");
        ProcessorContext ctx = runProcessor(compileSources(sources));
        assertTrue("a route cannot hold the same placeholder twice", ctx.hasErrors());
        String all = ctx.getErrors().toString();
        assertTrue(all, all.indexOf("more than once") >= 0);
    }

    @Test
    public void twoPlaceholdersWithDifferentNamesStillCompile() throws Exception {
        // The other direction, so the check above cannot pass by refusing every
        // route with two placeholders in it.
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.MoveApi",
                "package com.example;\n"
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient\n"
                + "public interface MoveApi {\n"
                + "    @GET(\"/move/{from}/{to}\")\n"
                + "    void move(@Path(\"from\") String from, @Path(\"to\") String to,\n"
                + "              OnComplete<Response<String>> callback);\n"
                + "}\n");
        ProcessorContext ctx = runProcessor(compileSources(sources));
        assertFalse("two differently named placeholders are an ordinary route: "
                + ctx.getErrors(), ctx.hasErrors());
    }

    @Test
    public void aJsonStringWhereANumberIsDeclaredIsRefused() throws Exception {
        // The value has already been TYPED by the parser here, so "7" against an
        // int field is the client disagreeing with the contract -- and the
        // generated client could never have produced it. Parsing it anyway left
        // the handler unable to tell a number from a string that looks like one.
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.Counter",
                "package com.example;\n"
                + "public class Counter {\n"
                + "    public int count;\n"
                + "    public Counter() {}\n"
                + "}\n");
        sources.put("com.example.CountApi",
                "package com.example;\n"
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient\n"
                + "public interface CountApi {\n"
                + "    @POST(\"/count\")\n"
                + "    void put(@Body Counter c, OnComplete<Response<Counter>> callback);\n"
                + "}\n");
        File classes = compileSources(sources);
        ProcessorContext ctx = runProcessor(classes);
        assertNoErrors(ctx);

        URLClassLoader loader = new URLClassLoader(
                new URL[]{classes.toURI().toURL(), testClassesDir().toURI().toURL()},
                getClass().getClassLoader());
        Method fromMap = loader.loadClass("com.example.CounterJson").getMethod("fromMap", Map.class);

        Map asNumber = new java.util.LinkedHashMap();
        asNumber.put("count", Long.valueOf(7));
        assertEquals(7, loader.loadClass("com.example.Counter").getField("count")
                .get(fromMap.invoke(null, asNumber)));

        Map asText = new java.util.LinkedHashMap();
        asText.put("count", "7");
        try {
            fromMap.invoke(null, asText);
            fail("a JSON string where an int is declared should be refused");
        } catch (java.lang.reflect.InvocationTargetException expected) {
            assertTrue(String.valueOf(expected.getCause()),
                    expected.getCause() instanceof IllegalArgumentException);
        }

        // An ABSENT field is still zero -- the rule is about a wrong type, not a
        // missing one.
        Map absent = new java.util.LinkedHashMap();
        assertEquals(0, loader.loadClass("com.example.Counter").getField("count")
                .get(fromMap.invoke(null, absent)));
        loader.close();
    }

    @Test
    public void aTextBindingStillParsesItsText() throws Exception {
        // The other half of the rule, and the reason the two paths are separate: a
        // QUERY parameter really does arrive as text, so parsing it is not
        // leniency, it is the only thing that could work. Tightening the JSON
        // decoders must not reach this.
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.QueryApi",
                "package com.example;\n"
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient\n"
                + "public interface QueryApi {\n"
                + "    @GET(\"/count\")\n"
                + "    void get(@Query(\"n\") int n, OnComplete<Response<String>> callback);\n"
                + "}\n");
        File classes = compileSources(sources);
        ProcessorContext ctx = runProcessor(classes);
        assertNoErrors(ctx);
        URLClassLoader loader = new URLClassLoader(
                new URL[]{classes.toURI().toURL(), testClassesDir().toURI().toURL()},
                getClass().getClassLoader());
        Class<?> serverItf = loader.loadClass("com.example.QueryApiServer");
        final Object[] seen = new Object[1];
        Object handler = Proxy.newProxyInstance(loader, new Class<?>[]{serverItf},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method m, Object[] args) {
                        seen[0] = args[0];
                        return "ok";
                    }
                });
        Class<?> dispatcherClass = loader.loadClass("com.example.QueryApiDispatcher");
        Object dispatcher = dispatcherClass.getConstructor(serverItf).newInstance(handler);
        Method dispatch = dispatcherClass.getMethod("dispatch",
                String.class, String.class, java.util.Map.class, Object.class);
        dispatch.invoke(dispatcher, "GET", "/count?n=7", null, null);
        assertEquals(Integer.valueOf(7), seen[0]);
        loader.close();
    }

    @Test
    public void processingAContractTwiceWithoutCleaningStillWorks() throws Exception {
        // Same rule as the controller half: the second pass of an incremental
        // build scans target/classes and finds the ApiServer, ApiDispatcher and
        // DTO codecs the first pass wrote. Reported as existing application
        // classes, an unchanged contract could be processed exactly once per
        // clean output directory.
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.NoteApi",
                "package com.example;\n"
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient\n"
                + "public interface NoteApi {\n"
                + "    @GET(\"/notes\")\n"
                + "    void all(OnComplete<Response<String>> callback);\n"
                + "}\n");
        File classes = compileSources(sources);
        ProcessorContext first = runProcessor(classes);
        assertNoErrors(first);
        assertTrue("the first pass must have written the dispatcher it then trips over",
                new File(classes, "com/example/NoteApiDispatcher.class").isFile());

        ProcessorContext second = runProcessor(classes);
        assertFalse("processing twice without a clean must work: " + second.getErrors(),
                second.hasErrors());
    }

    @Test
    public void aRealCollisionIsStillRefused() throws Exception {
        // The marker must not turn the guard off. A class the DEVELOPER wrote with
        // the generated name carries no marker, and generating over it would
        // silently replace their code in the output directory.
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.NoteApiDispatcher",
                "package com.example;\n"
                + "public class NoteApiDispatcher {\n"
                + "    public String mine() { return \"handwritten\"; }\n"
                + "}\n");
        sources.put("com.example.NoteApi",
                "package com.example;\n"
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient\n"
                + "public interface NoteApi {\n"
                + "    @GET(\"/notes\")\n"
                + "    void all(OnComplete<Response<String>> callback);\n"
                + "}\n");
        ProcessorContext ctx = runProcessor(compileSources(sources));
        assertTrue("a hand-written class of that name must still be protected",
                ctx.hasErrors());
        String all = ctx.getErrors().toString();
        assertTrue(all, all.indexOf("already exists") >= 0);
    }

    @Test
    public void disjointSuffixesAreNotTheSameShape() throws Exception {
        // The matcher supports embedded placeholders now, so these two are
        // perfectly writable -- but the duplicate-shape check collapsed each whole
        // segment to "{}", making them identical and refusing the contract.
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.FileApi",
                "package com.example;\n"
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient\n"
                + "public interface FileApi {\n"
                + "    @GET(\"/{name}.json\")\n"
                + "    void json(@Path(\"name\") String name,\n"
                + "              OnComplete<Response<String>> callback);\n"
                + "    @GET(\"/{name}.xml\")\n"
                + "    void xml(@Path(\"name\") String name,\n"
                + "             OnComplete<Response<String>> callback);\n"
                + "}\n");
        ProcessorContext ctx = runProcessor(compileSources(sources));
        assertFalse("no request satisfies both, so they are not duplicates: "
                + ctx.getErrors(), ctx.hasErrors());
    }

    @Test
    public void aRelativeTemplateStillMatchesTheRequest() throws Exception {
        // A contract written without the leading slash resolves against a base URL
        // ending in "/", so the CLIENT requests /notes. The server split the
        // template to one segment while the incoming path splits to two, so the
        // route could never match -- a contract that works as a client and answers
        // nothing as a server.
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.RelApi",
                "package com.example;\n"
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient\n"
                + "public interface RelApi {\n"
                + "    @GET(\"notes\")\n"
                + "    void all(OnComplete<Response<String>> callback);\n"
                + "}\n");
        File classes = compileSources(sources);
        ProcessorContext ctx = runProcessor(classes);
        assertNoErrors(ctx);
        URLClassLoader loader = new URLClassLoader(
                new URL[]{classes.toURI().toURL(), testClassesDir().toURI().toURL()},
                getClass().getClassLoader());
        Class<?> serverItf = loader.loadClass("com.example.RelApiServer");
        Object handler = Proxy.newProxyInstance(loader, new Class<?>[]{serverItf},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method m, Object[] args) {
                        return "ok";
                    }
                });
        Class<?> dispatcherClass = loader.loadClass("com.example.RelApiDispatcher");
        Object dispatcher = dispatcherClass.getConstructor(serverItf).newInstance(handler);
        Method dispatch = dispatcherClass.getMethod("dispatch",
                String.class, String.class, java.util.Map.class, Object.class);
        assertNotNull("the route the client requests must be the one the server answers",
                dispatch.invoke(dispatcher, "GET", "/notes", null, null));
        loader.close();
    }

    @Test
    public void theDispatcherRefusesAMalformedEscape() throws Exception {
        // The same rule as the router's, in the other generator -- and the
        // decoder here had a second hole besides: Integer.parseInt(_, 16) accepts
        // a sign, so "%+1" decoded to the byte 1. Two more spellings of a value
        // the client never wrote.
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.NoteApi",
                "package com.example;\n"
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient\n"
                + "public interface NoteApi {\n"
                + "    @GET(\"/notes/{id}\")\n"
                + "    void byId(@Path(\"id\") String id, OnComplete<Response<String>> callback);\n"
                + "}\n");
        File classes = compileSources(sources);
        ProcessorContext ctx = runProcessor(classes);
        assertNoErrors(ctx);
        URLClassLoader loader = new URLClassLoader(
                new URL[]{classes.toURI().toURL(), testClassesDir().toURI().toURL()},
                getClass().getClassLoader());
        Class<?> serverItf = loader.loadClass("com.example.NoteApiServer");
        final Object[] seen = new Object[1];
        Object handler = Proxy.newProxyInstance(loader, new Class<?>[]{serverItf},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method m, Object[] args) {
                        seen[0] = args[0];
                        return "ok";
                    }
                });
        Class<?> dispatcherClass = loader.loadClass("com.example.NoteApiDispatcher");
        Object dispatcher = dispatcherClass.getConstructor(serverItf).newInstance(handler);
        Method dispatch = dispatcherClass.getMethod("dispatch",
                String.class, String.class, java.util.Map.class, Object.class);

        dispatch.invoke(dispatcher, "GET", "/notes/%41", null, null);
        assertEquals("a well-formed escape still decodes", "A", seen[0]);

        seen[0] = null;
        try {
            dispatch.invoke(dispatcher, "GET", "/notes/%ZZ", null, null);
            fail("a non-hex escape should be refused, not passed through as text");
        } catch (java.lang.reflect.InvocationTargetException expected) {
            assertTrue(String.valueOf(expected.getCause()),
                    expected.getCause() instanceof IllegalArgumentException);
        }
        assertNull("the handler must not have run", seen[0]);

        try {
            dispatch.invoke(dispatcher, "GET", "/notes/%+1", null, null);
            fail("a signed hex pair is not a hex pair");
        } catch (java.lang.reflect.InvocationTargetException expected) {
            assertTrue(String.valueOf(expected.getCause()),
                    expected.getCause() instanceof IllegalArgumentException);
        }
    }

    @Test
    public void aFieldInheritedFromADependencyIsTransferred() throws Exception {
        // transferredFields() promises the superclass's fields, and stopped at the
        // first superclass this build did not compile: lookup() sees only the
        // project's own output. A DTO extending a class from a DEPENDENCY therefore
        // lost every inherited field from toMap() and fromMap() -- silently, and on
        // both ends of the wire, so the two agreed about a value neither sent.
        File dependency = tmp.newFolder();
        Map<String, String> base = new java.util.LinkedHashMap<String, String>();
        base.put("com.dep.Animal",
                "package com.dep;\n"
                + "public class Animal {\n"
                + "    public String species;\n"
                + "    public Animal() {}\n"
                + "}\n");
        JavaSourceCompiler.compile(base, dependency, Arrays.asList(testClassesDir()));

        List<File> withDependency = new java.util.ArrayList<File>();
        withDependency.add(testClassesDir());
        withDependency.add(dependency);
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.Cat",
                "package com.example;\n"
                + "public class Cat extends com.dep.Animal {\n"
                + "    public String name;\n"
                + "    public Cat() {}\n"
                + "}\n");
        sources.put("com.example.CatApi",
                "package com.example;\n"
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient\n"
                + "public interface CatApi {\n"
                + "    @POST(\"/cat\")\n"
                + "    void add(@Body Cat cat, OnComplete<Response<Cat>> callback);\n"
                + "}\n");
        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(sources, classes, withDependency);

        ProcessorContext ctx = runProcessor(classes,
                java.util.Arrays.asList(dependency.getAbsolutePath()));
        assertNoErrors(ctx);

        URLClassLoader loader = new URLClassLoader(
                new URL[]{classes.toURI().toURL(), dependency.toURI().toURL(),
                        testClassesDir().toURI().toURL()},
                getClass().getClassLoader());
        Class<?> codec = loader.loadClass("com.example.CatJson");
        Map inbound = new java.util.LinkedHashMap();
        inbound.put("name", "Tom");
        inbound.put("species", "cat");
        Object decoded = codec.getMethod("fromMap", Map.class).invoke(null, inbound);
        assertEquals("the inherited field must survive the round trip",
                "cat", loader.loadClass("com.dep.Animal").getField("species").get(decoded));
        loader.close();
    }

    @Test
    public void twoDynamicRoutesThatOverlapAreRefused() throws Exception {
        // Different shapes, and /a/b/c satisfies both. Neither is more specific, so
        // literal-first ordering cannot break the tie and dispatch answers with
        // whichever it emitted first.
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.AmbiguousApi",
                "package com.example;\n"
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient\n"
                + "public interface AmbiguousApi {\n"
                + "    @GET(\"/a/{x}/c\")\n"
                + "    void one(@Path(\"x\") String x, OnComplete<Response<String>> callback);\n"
                + "    @GET(\"/a/b/{y}\")\n"
                + "    void two(@Path(\"y\") String y, OnComplete<Response<String>> callback);\n"
                + "}\n");
        ProcessorContext ctx = runProcessor(compileSources(sources));
        assertTrue("two routes that both answer /a/b/c should not compile", ctx.hasErrors());
    }

    @Test
    public void aJsonIntegerTooLargeForTheFieldIsRefused() throws Exception {
        // The parser answers a Long for any JSON integer, and intValue() on
        // 2147483648 is -2147483648: the handler used to be handed a different
        // number from the one the client sent, silently.
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.Counter",
                "package com.example;\n"
                + "public class Counter {\n"
                + "    public int count;\n"
                + "    public Counter() {}\n"
                + "}\n");
        sources.put("com.example.CounterApi",
                "package com.example;\n"
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient\n"
                + "public interface CounterApi {\n"
                + "    @POST(\"/count\")\n"
                + "    void put(@Body Counter c, OnComplete<Response<Counter>> callback);\n"
                + "}\n");
        File classes = compileSources(sources);
        ProcessorContext ctx = runProcessor(classes);
        assertNoErrors(ctx);

        URLClassLoader loader = new URLClassLoader(
                new URL[]{classes.toURI().toURL(), testClassesDir().toURI().toURL()},
                getClass().getClassLoader());
        Class<?> codec = loader.loadClass("com.example.CounterJson");
        Method fromMap = codec.getMethod("fromMap", Map.class);

        Map inRange = new java.util.LinkedHashMap();
        inRange.put("count", Long.valueOf(7));
        Object decoded = fromMap.invoke(null, inRange);
        assertEquals(7, loader.loadClass("com.example.Counter").getField("count").get(decoded));

        Map tooLarge = new java.util.LinkedHashMap();
        tooLarge.put("count", Long.valueOf(2147483648L));
        try {
            fromMap.invoke(null, tooLarge);
            fail("a value that does not fit the field should not be narrowed into it");
        } catch (java.lang.reflect.InvocationTargetException expected) {
            assertTrue(String.valueOf(expected.getCause()),
                    expected.getCause() instanceof IllegalArgumentException);
        }
    }

    @Test
    public void aDtoCarriesTheFieldsItInherits() throws Exception {
        // AnnotatedClass.getFields() reads one class file, so the base's fields were
        // invisible to the codec: a Cat went over the wire with no species at all,
        // and the decoder left it null on the way back.
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.Animal",
                "package com.example;\n"
                + "public class Animal {\n"
                + "    public String species;\n"
                + "    public Animal() {}\n"
                + "}\n");
        sources.put("com.example.Cat",
                "package com.example;\n"
                + "public class Cat extends Animal {\n"
                + "    public String name;\n"
                + "    public Cat() {}\n"
                + "}\n");
        sources.put("com.example.CatApi",
                "package com.example;\n"
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient\n"
                + "public interface CatApi {\n"
                + "    @GET(\"/cat\")\n"
                + "    void get(OnComplete<Response<Cat>> callback);\n"
                + "}\n");
        File classes = compileSources(sources);
        ProcessorContext ctx = runProcessor(classes);
        assertNoErrors(ctx);

        URLClassLoader loader = new URLClassLoader(
                new URL[]{classes.toURI().toURL(), testClassesDir().toURI().toURL()},
                getClass().getClassLoader());
        Class<?> cat = loader.loadClass("com.example.Cat");
        Object instance = cat.newInstance();
        cat.getField("name").set(instance, "Tom");
        cat.getField("species").set(instance, "felis");

        Class<?> codec = loader.loadClass("com.example.CatJson");
        Method toMap = codec.getMethod("toMap", cat);
        Map encoded = (Map) toMap.invoke(null, instance);
        assertEquals("Tom", encoded.get("name"));
        assertEquals("the inherited field is missing from the wire shape",
                "felis", encoded.get("species"));

        // And back, so the loss is not merely one-directional.
        Method fromMap = codec.getMethod("fromMap", Map.class);
        Object decoded = fromMap.invoke(null, encoded);
        assertEquals("felis", cat.getField("species").get(decoded));
    }

    @Test
    public void aMalformedBooleanIsRefusedRatherThanTakenAsFalse() throws Exception {
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.FlagApi",
                "package com.example;\n"
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient\n"
                + "public interface FlagApi {\n"
                + "    @GET(\"/flag\")\n"
                + "    void flag(@Query(\"on\") boolean on,\n"
                + "              OnComplete<Response<String>> callback);\n"
                + "}\n");
        File classes = compileSources(sources);
        ProcessorContext ctx = runProcessor(classes);
        assertNoErrors(ctx);

        URLClassLoader loader = new URLClassLoader(
                new URL[]{classes.toURI().toURL(), testClassesDir().toURI().toURL()},
                getClass().getClassLoader());
        Class<?> serverItf = loader.loadClass("com.example.FlagApiServer");
        Object handler = java.lang.reflect.Proxy.newProxyInstance(loader,
                new Class<?>[]{serverItf}, new java.lang.reflect.InvocationHandler() {
                    public Object invoke(Object proxy, Method m, Object[] args) {
                        return "on=" + args[0];
                    }
                });
        Class<?> dispatcherClass = loader.loadClass("com.example.FlagApiDispatcher");
        Object dispatcher = dispatcherClass.getConstructor(serverItf).newInstance(handler);
        Method dispatch = dispatcherClass.getMethod("dispatch",
                String.class, String.class, Map.class, Object.class);

        assertEquals("on=true", dispatch.invoke(dispatcher, "GET", "/flag?on=true", null, null));
        assertEquals("on=false", dispatch.invoke(dispatcher, "GET", "/flag?on=false", null, null));
        // "treu" used to arrive as an explicit false, so the handler ran on a value
        // the client never sent and nothing anywhere said so.
        try {
            dispatch.invoke(dispatcher, "GET", "/flag?on=treu", null, null);
            fail("a value that is not a boolean should not bind as false");
        } catch (java.lang.reflect.InvocationTargetException expected) {
            assertTrue(String.valueOf(expected.getCause()),
                    expected.getCause() instanceof IllegalArgumentException);
        }
    }

    @Test
    public void aCollectionOfCollectionsOfDtosIsRefused() throws Exception {
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.Tag", TAG_SOURCE);
        sources.put("com.example.NestedApi",
                "package com.example;\n"
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient\n"
                + "public interface NestedApi {\n"
                + "    @GET(\"/nested\")\n"
                + "    void nested(OnComplete<Response<java.util.List<java.util.List<Tag>>>> callback);\n"
                + "}\n");
        ProcessorContext ctx = runProcessor(compileSources(sources));
        // The codec reaches the outer elements only, so the Tags inside would have
        // been written as their toString(). A build error beats wrong JSON.
        assertTrue("a shape the codec cannot encode should not compile", ctx.hasErrors());
    }

    @Test
    public void generatesServerInterfaceAndWorkingDispatcher() throws Exception {
        File classes = compileApi();
        ProcessorContext ctx = runProcessor(classes);
        assertNoErrors(ctx);

        assertTrue("server interface was not emitted",
                new File(classes, "com/example/GreeterApiServer.class").isFile());
        assertTrue("dispatcher was not emitted",
                new File(classes, "com/example/GreeterApiDispatcher.class").isFile());

        URLClassLoader loader = new URLClassLoader(
                new URL[]{classes.toURI().toURL(), testClassesDir().toURI().toURL()},
                getClass().getClassLoader());
        Class<?> serverItf = loader.loadClass("com.example.GreeterApiServer");

        // The callback parameter must have become the return type, and the
        // callback itself must be gone from the signature.
        Method greet = serverItf.getMethod("greet", String.class, String.class);
        assertEquals(String.class, greet.getReturnType());

        Object handler = Proxy.newProxyInstance(loader, new Class<?>[]{serverItf},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method m, Object[] args) throws Exception {
                        String n = m.getName();
                        if ("greet".equals(n)) return "hello " + args[0] + "/loud=" + args[1];
                        if ("echo".equals(n)) return "echoed:" + args[0];
                        if ("whoami".equals(n)) return "user=" + args[0] + ",session=" + args[1];
                        if ("addPet".equals(n)) {
                            Object pet = args[0];
                            // round-trips the DTO straight back out
                            return pet;
                        }
                        if ("listPets".equals(n)) {
                            Class<?> petClass = proxy.getClass().getClassLoader().loadClass("com.example.Pet");
                            Object p1 = petClass.newInstance();
                            petClass.getField("id").setLong(p1, 7L);
                            petClass.getField("name").set(p1, "Rex");
                            java.util.List out = new java.util.ArrayList();
                            out.add(p1);
                            return out;
                        }
                        return null;
                    }
                });

        Class<?> dispatcherClass = loader.loadClass("com.example.GreeterApiDispatcher");
        Object dispatcher = dispatcherClass.getConstructor(serverItf).newInstance(handler);
        Method dispatch = dispatcherClass.getMethod("dispatch",
                String.class, String.class, java.util.Map.class, Object.class);
        Method hasRoute = dispatcherClass.getMethod("hasRoute", String.class, String.class);

        assertEquals("hello Shai/loud=yes",
                dispatch.invoke(dispatcher, "GET", "/greet/Shai?loud=yes", null, null));
        // Absent query parameter binds to null rather than failing the route.
        assertEquals("hello Shai/loud=null",
                dispatch.invoke(dispatcher, "GET", "/greet/Shai", null, null));
        // Percent-encoding in a path segment is decoded before binding.
        assertEquals("hello Shai Almog/loud=null",
                dispatch.invoke(dispatcher, "GET", "/greet/Shai%20Almog", null, null));
        // '+' is a space in a query value.
        assertEquals("hello Shai/loud=a b",
                dispatch.invoke(dispatcher, "GET", "/greet/Shai?loud=a+b", null, null));
        assertEquals("echoed:{\"a\":1}",
                dispatch.invoke(dispatcher, "POST", "/echo", null, "{\"a\":1}"));

        // Headers bind case-insensitively; cookies come out of the Cookie header.
        java.util.Map headers = new java.util.LinkedHashMap();
        headers.put("x-user", "shai");
        headers.put("Cookie", "theme=dark; session=abc123; other=x");
        assertEquals("user=shai,session=abc123",
                dispatch.invoke(dispatcher, "GET", "/whoami", headers, null));
        assertEquals("user=null,session=null",
                dispatch.invoke(dispatcher, "GET", "/whoami", null, null));

        // A DTO body is decoded from the request Map into the typed parameter, and
        // a DTO result is encoded back to a Map.
        java.util.Map petIn = new java.util.LinkedHashMap();
        petIn.put("id", Long.valueOf(42));      // the JSON reader hands integers back as Long
        petIn.put("name", "Fido");
        petIn.put("good", Boolean.TRUE);
        petIn.put("weight", Double.valueOf(12.5));
        Object out = dispatch.invoke(dispatcher, "POST", "/pet", null, petIn);
        assertTrue("a DTO result must come back as a Map", out instanceof java.util.Map);
        java.util.Map petOut = (java.util.Map) out;
        assertEquals(Long.valueOf(42), petOut.get("id"));
        assertEquals("Fido", petOut.get("name"));
        assertEquals(Boolean.TRUE, petOut.get("good"));
        assertEquals(Double.valueOf(12.5), petOut.get("weight"));

        // A List<DTO> result is encoded element by element.
        Object listOut = dispatch.invoke(dispatcher, "GET", "/pets", null, null);
        assertTrue(listOut instanceof java.util.List);
        java.util.Map first = (java.util.Map) ((java.util.List) listOut).get(0);
        assertEquals("Rex", first.get("name"));
        assertEquals(Long.valueOf(7), first.get("id"));

        // Route matching is by verb AND path, and hasRoute is what separates
        // "no such route" from "the handler returned null".
        assertTrue((Boolean) hasRoute.invoke(dispatcher, "GET", "/greet/Shai"));
        assertTrue(!(Boolean) hasRoute.invoke(dispatcher, "GET", "/nope"));
        assertTrue(!(Boolean) hasRoute.invoke(dispatcher, "POST", "/greet/Shai"));
        assertNull(dispatch.invoke(dispatcher, "GET", "/nope", null, null));
        loader.close();
    }

    /**
     * A DTO with a public final field cannot be round-tripped, so it is refused.
     *
     * The encoder writes every public field and the decoder assigns them after a
     * no-argument construction, so a final one goes out and silently does not come
     * back: the handler sees the initializer and the client's value is gone, with
     * nothing failing to say so. A contract that cannot be honoured should not
     * compile.
     */
    @Test
    public void refusesADtoWithAFinalField() throws Exception {
        File classes = tmp.newFolder();
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.Frozen",
                "package com.example;\n"
                + "public class Frozen {\n"
                + "    public final String label = \"set at construction\";\n"
                + "    public String mutable;\n"
                + "    public Frozen() {}\n"
                + "}\n");
        sources.put("com.example.FrozenApi",
                "package com.example;\n"
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient\n"
                + "public interface FrozenApi {\n"
                + "    @POST(\"/frozen\")\n"
                + "    void send(@Body Frozen f, OnComplete<Response<String>> callback);\n"
                + "}\n");
        JavaSourceCompiler.compile(sources, classes, Arrays.asList(testClassesDir()));
        assertTrue("a public final DTO field must fail the build, not be dropped in transit",
                runProcessor(classes).hasErrors());
    }

    /**
     * A DTO's collection FIELD arrives as its declared element type too.
     *
     * The body-parameter case was fixed first; this is the same defect one level in,
     * where the elements land in a field rather than an argument. A List<Integer>
     * full of Longs is a ClassCastException on the JVM at the first read, and on the
     * translated target a Long read as an Integer with no complaint at all.
     */
    @Test
    public void convertsScalarElementsInADtoCollectionField() throws Exception {
        File classes = compileApi();
        ProcessorContext ctx = runProcessor(classes);
        assertNoErrors(ctx);
        URLClassLoader loader = new URLClassLoader(
                new java.net.URL[]{classes.toURI().toURL(), testClassesDir().toURI().toURL()},
                getClass().getClassLoader());
        Class<?> serverItf = loader.loadClass("com.example.GreeterApiServer");
        final Object[] received = new Object[1];
        Object handler = Proxy.newProxyInstance(loader, new Class<?>[]{serverItf},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method m, Object[] args) {
                        if ("addPet".equals(m.getName())) {
                            received[0] = args[0];
                            return args[0];
                        }
                        return null;
                    }
                });
        Class<?> dispatcherClass = loader.loadClass("com.example.GreeterApiDispatcher");
        Object dispatcher = dispatcherClass.getConstructor(serverItf).newInstance(handler);
        Method dispatch = dispatcherClass.getMethod("dispatch",
                String.class, String.class, java.util.Map.class, Object.class);

        java.util.Map body = new java.util.LinkedHashMap();
        body.put("name", "Rex");
        // What the JSON reader really produces for [3, 4].
        body.put("weights", java.util.Arrays.asList(Long.valueOf(3), Long.valueOf(4)));
        dispatch.invoke(dispatcher, "POST", "/pet", null, body);

        assertNotNull("the DTO never reached the handler", received[0]);
        java.util.List weights = (java.util.List)
                received[0].getClass().getField("weights").get(received[0]);
        assertNotNull("the collection field was not decoded", weights);
        assertEquals("an Integer element must not still be a Long",
                Integer.class, weights.get(0).getClass());
        assertEquals(Integer.valueOf(3), weights.get(0));
        loader.close();
    }

    /**
     * A collection body arrives as its DECLARED element type, not the parser's.
     *
     * The JSON reader produces Long for every integer, so a `List<Integer>` handed
     * over raw is a list of Longs wearing a List<Integer> label. The JVM reveals
     * that as a ClassCastException the first time the handler reads an element;
     * ParparVM's CHECKCAST is unchecked, so there it reads an Integer's fields out
     * of a Long and keeps going. Asserting on the element's runtime class is the
     * only way to see the difference.
     */
    @Test
    public void convertsScalarElementsInACollectionBody() throws Exception {
        File classes = compileApi();
        ProcessorContext ctx = runProcessor(classes);
        assertNoErrors(ctx);
        URLClassLoader loader = new URLClassLoader(
                new java.net.URL[]{ classes.toURI().toURL() }, getClass().getClassLoader());
        Class<?> serverInterface = loader.loadClass("com.example.GreeterApiServer");
        final Object[] received = new Object[2];
        Object impl = Proxy.newProxyInstance(loader, new Class<?>[]{ serverInterface },
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if ("weights".equals(method.getName())) {
                            received[0] = args[0];
                        } else if ("labels".equals(method.getName())) {
                            received[1] = args[0];
                        }
                        return "ok";
                    }
                });
        Class<?> dispatcherClass = loader.loadClass("com.example.GreeterApiDispatcher");
        Object dispatcher = dispatcherClass.getConstructor(serverInterface).newInstance(impl);
        Method dispatch = dispatcherClass.getMethod("dispatch", String.class, String.class,
                java.util.Map.class, Object.class);

        dispatch.invoke(dispatcher, "POST", "/weights", null,
                java.util.Arrays.asList(Long.valueOf(3), Long.valueOf(4)));
        java.util.List weights = (java.util.List) received[0];
        assertNotNull("the list body did not reach the handler", weights);
        assertEquals("an Integer element must not still be a Long",
                Integer.class, weights.get(0).getClass());
        assertEquals(Integer.valueOf(3), weights.get(0));

        dispatch.invoke(dispatcher, "POST", "/labels", null,
                java.util.Arrays.asList("a", "b"));
        assertTrue("a Set body must arrive as a Set", received[1] instanceof java.util.Set);
        assertEquals(2, ((java.util.Set) received[1]).size());
        loader.close();
    }

    /**
     * The request body's SHAPE is the client's choice, so nothing the dispatcher
     * does with it may rest on a cast.
     *
     * This is not a style point. ParparVM's CHECKCAST is unchecked by default (see
     * CLAUDE.md), so `(Map)body` over a String does not throw on a translated
     * server -- it reads a String's header as a Map's, and the process dies taking
     * every in-flight connection with it. A four-byte body once did exactly that.
     * The JVM only reveals it as a ClassCastException, which is why this asserts on
     * the MESSAGE: an IllegalArgumentException naming the expected shape is a 400,
     * and a ClassCastException is a 500 here and a crash there.
     */
    @Test
    public void refusesABodyOfTheWrongShapeInsteadOfCastingIt() throws Exception {
        File classes = compileApi();
        ProcessorContext ctx = runProcessor(classes);
        assertNoErrors(ctx);

        URLClassLoader loader = new URLClassLoader(
                new URL[]{classes.toURI().toURL(), testClassesDir().toURI().toURL()},
                getClass().getClassLoader());
        Class<?> serverItf = loader.loadClass("com.example.GreeterApiServer");
        Object handler = Proxy.newProxyInstance(loader, new Class<?>[]{serverItf},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method m, Object[] args) {
                        return "addPet".equals(m.getName()) ? args[0] : null;
                    }
                });
        Class<?> dispatcherClass = loader.loadClass("com.example.GreeterApiDispatcher");
        Object dispatcher = dispatcherClass.getConstructor(serverItf).newInstance(handler);
        Method dispatch = dispatcherClass.getMethod("dispatch",
                String.class, String.class, java.util.Map.class, Object.class);

        // A route declaring a DTO, handed a string, a number and an array.
        Object[] wrongShapes = new Object[]{"not an object", Long.valueOf(42),
                new java.util.ArrayList()};
        for (Object wrong : wrongShapes) {
            try {
                dispatch.invoke(dispatcher, "POST", "/pet", null, wrong);
                fail("a " + wrong.getClass().getSimpleName()
                        + " body must be rejected, not cast to a Map");
            } catch (java.lang.reflect.InvocationTargetException err) {
                Throwable cause = err.getCause();
                assertTrue("expected a 400-shaped rejection, got " + cause,
                        cause instanceof IllegalArgumentException);
                assertTrue("the message must say what was expected: " + cause.getMessage(),
                        cause.getMessage().indexOf("JSON object") >= 0);
            }
        }

        // Null stays null: an absent body is not a malformed one.
        assertNull(dispatch.invoke(dispatcher, "POST", "/pet", null, null));
        loader.close();
    }

    /**
     * A DTO-typed collection field has to be converted element by element in BOTH
     * directions. Handing the handler the decoded Maps typed as Tags is a lie the
     * JVM catches at the first field read and a translated binary does not catch at
     * all; writing the Tags back without converting them serialises toString().
     */
    @Test
    public void roundTripsACollectionOfNestedDtos() throws Exception {
        File classes = compileApi();
        ProcessorContext ctx = runProcessor(classes);
        assertNoErrors(ctx);

        URLClassLoader loader = new URLClassLoader(
                new URL[]{classes.toURI().toURL(), testClassesDir().toURI().toURL()},
                getClass().getClassLoader());
        final Class<?> tagClass = loader.loadClass("com.example.Tag");
        Class<?> serverItf = loader.loadClass("com.example.GreeterApiServer");
        // The handler READS a typed field off every element, which is the operation
        // a List of Maps typed as Tags fails at.
        Object handler = Proxy.newProxyInstance(loader, new Class<?>[]{serverItf},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method m, Object[] args) throws Exception {
                        if (!"addPet".equals(m.getName())) {
                            return null;
                        }
                        Object pet = args[0];
                        java.util.List tags = (java.util.List) pet.getClass()
                                .getField("tags").get(pet);
                        double total = 0;
                        // A null collection is a legal thing for the client to
                        // send, so this fixture has to survive it -- it is the
                        // value the "stays null" case below asserts arrives.
                        for (int i = 0; tags != null && i < tags.size(); i++) {
                            Object tag = tags.get(i);
                            if (tag == null) {
                                continue; // an element of the wrong shape decodes to null
                            }
                            assertTrue("element " + i + " is a " + tag.getClass().getName()
                                    + ", not a Tag", tagClass.isInstance(tag));
                            total += tagClass.getField("weight").getInt(tag);
                        }
                        pet.getClass().getField("weight").setDouble(pet, total);
                        return pet;
                    }
                });
        Class<?> dispatcherClass = loader.loadClass("com.example.GreeterApiDispatcher");
        Object dispatcher = dispatcherClass.getConstructor(serverItf).newInstance(handler);
        Method dispatch = dispatcherClass.getMethod("dispatch",
                String.class, String.class, java.util.Map.class, Object.class);

        java.util.Map friendly = new java.util.LinkedHashMap();
        friendly.put("label", "friendly");
        friendly.put("weight", Long.valueOf(3));
        java.util.Map loud = new java.util.LinkedHashMap();
        loud.put("label", "loud");
        loud.put("weight", Long.valueOf(1));
        java.util.List tagMaps = new java.util.ArrayList();
        tagMaps.add(friendly);
        tagMaps.add(loud);
        java.util.Map petIn = new java.util.LinkedHashMap();
        petIn.put("name", "Rex");
        petIn.put("tags", tagMaps);

        java.util.Map petOut = (java.util.Map) dispatch.invoke(dispatcher, "POST", "/pet",
                null, petIn);
        assertEquals(Double.valueOf(4), petOut.get("weight"));
        java.util.List tagsOut = (java.util.List) petOut.get("tags");
        assertTrue("nested DTOs must be written back as Maps, not as objects",
                tagsOut.get(0) instanceof java.util.Map);
        assertEquals("friendly", ((java.util.Map) tagsOut.get(0)).get("label"));

        // An element of the wrong shape is REFUSED. This assertion used to expect
        // null, which was itself an improvement on handing the handler a Map
        // wearing a Tag's type -- but null is a value the client can legitimately
        // send, so substituting it for a mistake made the two indistinguishable
        // and let the handler act on a collection with a hole in it. Throwing is
        // what lets the transport answer 400, which is what the request deserves.
        java.util.List mixed = new java.util.ArrayList();
        mixed.add("not an object");
        java.util.Map petMixed = new java.util.LinkedHashMap();
        petMixed.put("tags", mixed);
        try {
            dispatch.invoke(dispatcher, "POST", "/pet", null, petMixed);
            fail("a scalar where a Tag was declared must be refused, not nulled");
        } catch (java.lang.reflect.InvocationTargetException expected) {
            assertTrue(String.valueOf(expected.getCause()),
                    expected.getCause() instanceof IllegalArgumentException);
        }
        // THE CONTAINER, by the same rule as the element above. A scalar or an
        // object where an array was declared answered null, so the handler was
        // invoked with a null field and the client's mistake was indistinguishable
        // from an explicit JSON null -- the very distinction the element check
        // beside it exists to preserve, missing one level up.
        Object[] notArrays = {"a string", Long.valueOf(7),
                              new java.util.LinkedHashMap(), Boolean.TRUE};
        for (int i = 0; i < notArrays.length; i++) {
            java.util.Map petShape = new java.util.LinkedHashMap();
            petShape.put("tags", notArrays[i]);
            try {
                dispatch.invoke(dispatcher, "POST", "/pet", null, petShape);
                fail("a " + notArrays[i].getClass().getName()
                        + " where an array was declared must be refused, not nulled");
            } catch (java.lang.reflect.InvocationTargetException expected) {
                assertTrue(String.valueOf(expected.getCause()),
                        expected.getCause() instanceof IllegalArgumentException);
            }
        }

        // A genuine JSON null element still passes through as null.
        java.util.List withNull = new java.util.ArrayList();
        withNull.add(null);
        java.util.Map petNull = new java.util.LinkedHashMap();
        petNull.put("tags", withNull);
        java.util.Map nullOut = (java.util.Map) dispatch.invoke(dispatcher, "POST", "/pet",
                null, petNull);
        assertNull(((java.util.List) nullOut.get("tags")).get(0));

        // And a null COLLECTION is still a null collection. That is the half the
        // fix must not have taken away: null is a legal value for the field, and
        // only a non-null value of the wrong shape is the client being wrong.
        java.util.Map petAbsent = new java.util.LinkedHashMap();
        petAbsent.put("name", "Rex");
        petAbsent.put("tags", null);
        java.util.Map absentOut = (java.util.Map) dispatch.invoke(dispatcher, "POST", "/pet",
                null, petAbsent);
        assertNull("an explicit null array must stay null", absentOut.get("tags"));
        loader.close();
    }

    @Test
    public void refusesAParameterItCannotBind() throws Exception {
        File classes = tmp.newFolder("classes-unbindable");
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.HeaderApi",
                "package com.example;\n"
                        + "import com.codename1.annotations.rest.*;\n"
                        + "import com.codename1.io.rest.Response;\n"
                        + "import com.codename1.util.OnComplete;\n"
                        + "@RestClient\n"
                        + "public interface HeaderApi {\n"
                        + "    @GET(\"/thing\")\n"
                        + "    void thing(String noAnnotation,\n"
                        + "               OnComplete<Response<String>> callback);\n"
                        + "}\n");
        JavaSourceCompiler.compile(sources, classes, Arrays.asList(testClassesDir()));
        ProcessorContext ctx = runProcessor(classes);
        assertTrue("a parameter with no binding annotation must fail the build, not bind to null",
                ctx.hasErrors());
    }

    /**
     * A @Path that names no placeholder is a typo, and it used to bind null -- or 0
     * for a primitive -- while the route still matched, so the handler ran with the
     * wrong identifier and the build said nothing.
     */
    @Test
    public void refusesAPathBindingThatMatchesNoPlaceholder() throws Exception {
        assertTrue("a @Path naming no placeholder must fail the build",
                processApi("TypoApi",
                        "    @GET(\"/users/{id}\")\n"
                        + "    void user(@Path(\"userId\") String id,\n"
                        + "              OnComplete<Response<String>> callback);\n").hasErrors());
    }

    /**
     * Two routes of one verb and shape compile to the same predicate, and dispatch
     * returns on the first match -- so the second can never be reached however it is
     * called. The placeholder NAMES differ; the router never sees them.
     */
    @Test
    public void refusesTwoRoutesOfTheSameShape() throws Exception {
        assertTrue("an unreachable duplicate route must fail the build",
                processApi("AmbiguousApi",
                        "    @GET(\"/pets/{id}\")\n"
                        + "    void byId(@Path(\"id\") String id,\n"
                        + "              OnComplete<Response<String>> callback);\n"
                        + "    @GET(\"/pets/{name}\")\n"
                        + "    void byName(@Path(\"name\") String name,\n"
                        + "                OnComplete<Response<String>> callback);\n").hasErrors());
    }

    /**
     * Collection belongs with List and Set. The parser answers an ArrayList for
     * every JSON array, so a Collection<T> that is not recognised as a collection
     * SHAPE falls through to a guarded cast, which erases -- leaving a collection
     * of Map under a Collection<Note> declaration, which throws on first use.
     */
    @Test
    public void decodesACollectionBodyLikeAListOne() throws Exception {
        assertNoErrors(processApi("CollectionApi",
                "    @POST(\"/notes\")\n"
                + "    void add(@Body java.util.Collection<String> notes,\n"
                + "             OnComplete<Response<String>> callback);\n"));
    }

    /**
     * A Map's values are handed over as the parser built them, and nothing walks
     * them applying the declared type the way collection elements are walked. So
     * Map<String,Integer> is a map of Long at runtime and the handler's first
     * read as an Integer throws, from a contract that processed cleanly.
     */
    @Test
    public void refusesAMapOfATypeTheParserDoesNotProduce() throws Exception {
        assertTrue("a map of Integer must fail the build",
                processApi("MapApi",
                        "    @POST(\"/counts\")\n"
                        + "    void put(@Body java.util.Map<String, Integer> counts,\n"
                        + "             OnComplete<Response<String>> callback);\n").hasErrors());
    }

    /** A map of what the parser DOES produce still works. */
    @Test
    public void allowsAMapOfTheTypesTheParserProduces() throws Exception {
        assertNoErrors(processApi("MapOkApi",
                "    @POST(\"/counts\")\n"
                + "    void put(@Body java.util.Map<String, Long> counts,\n"
                + "             OnComplete<Response<String>> callback);\n"));
    }

    /**
     * A placeholder nothing binds. The client substitutes the placeholder's own
     * NAME, so it asks for /users/id literally, while the server matches any
     * value there and hands it to nobody -- two halves agreeing on a route whose
     * variable cannot be supplied or read.
     */
    @Test
    public void refusesAPlaceholderNothingBinds() throws Exception {
        assertTrue("a placeholder with no @Path must fail the build",
                processApi("UnboundApi",
                        "    @GET(\"/users/{id}\")\n"
                        + "    void user(OnComplete<Response<String>> callback);\n").hasErrors());
    }

    /**
     * A literal beside a placeholder is NOT ambiguous: the dispatcher emits every
     * route without a placeholder before every route with one, so /users/me takes
     * its own path and every other value falls through to {id}. This is the most
     * ordinary pair there is, and refusing it left no way to write it.
     */
    @Test
    public void allowsALiteralBesideAPlaceholder() throws Exception {
        assertNoErrors(processApi("LiteralApi",
                "    @GET(\"/users/me\")\n"
                + "    void me(OnComplete<Response<String>> callback);\n"
                + "    @GET(\"/users/{id}\")\n"
                + "    void byId(@Path(\"id\") String id,\n"
                + "              OnComplete<Response<String>> callback);\n"));
    }

    /** Two routes of the same shape but DIFFERENT verbs are not ambiguous. */
    @Test
    public void allowsTheSameShapeUnderDifferentVerbs() throws Exception {
        assertNoErrors(processApi("VerbsApi",
                "    @GET(\"/pets/{id}\")\n"
                + "    void read(@Path(\"id\") String id,\n"
                + "              OnComplete<Response<String>> callback);\n"
                + "    @DELETE(\"/pets/{id}\")\n"
                + "    void remove(@Path(\"id\") String id,\n"
                + "                OnComplete<Response<String>> callback);\n"));
    }

    /** Compiles one throwaway contract and runs the processor over it. */
    private ProcessorContext processApi(String name, String methods) throws Exception {
        File classes = tmp.newFolder();
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example." + name,
                "package com.example;\n"
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient\n"
                + "public interface " + name + " {\n"
                + methods
                + "}\n");
        JavaSourceCompiler.compile(sources, classes, Arrays.asList(testClassesDir()));
        return runProcessor(classes);
    }

    @Test
    public void generatesNothingWhenTheServerHalfIsOff() throws Exception {
        System.clearProperty("cn1.restServer");
        File classes = compileApi();
        ProcessorContext ctx = runProcessor(classes);
        assertNoErrors(ctx);
        assertTrue("the server half must be opt-in so existing app builds do not grow",
                !new File(classes, "com/example/GreeterApiDispatcher.class").isFile());
    }

    /** Compiles an arbitrary set of sources, for the cases the shared fixture cannot express. */
    private File compileSources(Map<String, String> sources) throws Exception {
        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(sources, classes, Arrays.asList(testClassesDir()));
        return classes;
    }

    private File compileApi() throws Exception {
        File classes = tmp.newFolder();
        Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.Tag", TAG_SOURCE);
        sources.put("com.example.Pet", DTO_SOURCE);
        sources.put("com.example.GreeterApi", API_SOURCE);
        JavaSourceCompiler.compile(sources, classes, Arrays.asList(testClassesDir()));
        return classes;
    }

    private void assertNoErrors(ProcessorContext ctx) {
        if (ctx.hasErrors()) {
            StringBuilder sb = new StringBuilder("processor reported errors:\n");
            for (ProcessorContext.ProcessingError e : ctx.getErrors()) sb.append(' ').append(e).append('\n');
            fail(sb.toString());
        }
    }

    private ProcessorContext runProcessor(File classesDir) throws Exception {
        return runProcessor(classesDir, Collections.<String>emptyList());
    }

    /**
     * With a compile CLASSPATH, the way both real mojos build the context.
     *
     * Without one, a superclass supplied by a dependency is invisible -- which is
     * the whole point of the test that uses this.
     */
    private ProcessorContext runProcessor(File classesDir, List<String> classpath)
            throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classesDir);
        RestServerAnnotationProcessor proc = new RestServerAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classesDir, tmp.newFolder(),
                index, new SystemStreamLog(), tmp.newFolder(), new Properties(), null,
                Collections.<String>emptyList(), "UTF-8", classpath);
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (!cls.getClassAnnotations().isEmpty()) proc.processClass(cls, ctx);
        }
        proc.finish(ctx);
        return ctx;
    }

    private static File testClassesDir() throws Exception {
        URL url = RestServerAnnotationProcessorTest.class.getProtectionDomain()
                .getCodeSource().getLocation();
        return new File(url.toURI());
    }
}
