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
import java.util.Map;

import static org.junit.Assert.assertEquals;
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
                    + "}\n";

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
                        for (int i = 0; i < tags.size(); i++) {
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

        // An element of the wrong shape becomes null rather than a mistyped object.
        java.util.List mixed = new java.util.ArrayList();
        mixed.add("not an object");
        java.util.Map petMixed = new java.util.LinkedHashMap();
        petMixed.put("tags", mixed);
        java.util.Map mixedOut = (java.util.Map) dispatch.invoke(dispatcher, "POST", "/pet",
                null, petMixed);
        assertNull(((java.util.List) mixedOut.get("tags")).get(0));
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

    @Test
    public void generatesNothingWhenTheServerHalfIsOff() throws Exception {
        System.clearProperty("cn1.restServer");
        File classes = compileApi();
        ProcessorContext ctx = runProcessor(classes);
        assertNoErrors(ctx);
        assertTrue("the server half must be opt-in so existing app builds do not grow",
                !new File(classes, "com/example/GreeterApiDispatcher.class").isFile());
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
        Map<String, AnnotatedClass> index = ClassScanner.scan(classesDir);
        RestServerAnnotationProcessor proc = new RestServerAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classesDir, tmp.newFolder(),
                index, new SystemStreamLog());
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
