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

import com.codename1.backend.Backend;
import com.codename1.backend.Config;
import com.codename1.build.SystemStreamLog;
import com.codename1.impl.backend.BackendAccess;
import com.codename1.impl.backend.BackendApplication;
import com.codename1.mapping.Mapper;
import com.codename1.mapping.Mappers;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.ProcessorContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/// A Java record as the transfer object of a shared `@RestClient` contract, end
/// to end: the contract module is compiled at the running JDK's level (records
/// need 16 or newer), the app's mappers are generated from it, the backend's
/// server interface and controller are generated from it as a dependency, and a
/// record the client encodes is posted to a running server, built there through
/// its canonical constructor, and answered back to the client's decoder. The
/// record carries an enum, a nested record, a list of records, a list of
/// strings, primitives, and a component the server ignores.
public class RecordDtoTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final String PKG = "package com.example;\n";

    @Test
    public void aRecordDtoRoundTripsBetweenTheClientAndTheServer() throws Exception {
        assumeTrue("records need Java 16 or newer", javaSpecAtLeast(16));
        File shared = compileContract();

        // -- the app: a mapper per @Mapped record, as the app module generates.
        File app = tmp.newFolder("app");
        copy(shared.toPath(), app.toPath());
        ProcessorContext mapping = new ProcessorContext(app, tmp.newFolder(), ClassScanner.scan(app),
                new SystemStreamLog());
        MappingAnnotationProcessor mapper = new MappingAnnotationProcessor();
        mapper.start(mapping);
        for (AnnotatedClass cls : ClassScanner.scan(app).values()) {
            if (!cls.getClassAnnotations().isEmpty()) {
                mapper.processClass(cls, mapping);
            }
        }
        mapper.finish(mapping);
        assertFalse(String.valueOf(mapping.getErrors()), mapping.hasErrors());
        // The generated client implementation compiles against the record contract.
        Map<String, AnnotatedClass> appIndex = ClassScanner.scan(app);
        RestClientAnnotationProcessor restClient = new RestClientAnnotationProcessor();
        ProcessorContext clientCtx = new ProcessorContext(app, tmp.newFolder(), appIndex,
                new SystemStreamLog());
        restClient.start(clientCtx);
        for (AnnotatedClass cls : appIndex.values()) {
            if (!cls.getClassAnnotations().isEmpty()) {
                restClient.processClass(cls, clientCtx);
            }
        }
        restClient.finish(clientCtx);
        assertFalse(String.valueOf(clientCtx.getErrors()), clientCtx.hasErrors());
        assertTrue(new File(app, "com/example/RidesApiImpl.class").isFile());
        URLClassLoader client = new URLClassLoader(new URL[] {app.toURI().toURL()},
                getClass().getClassLoader());

        // -- the server: the contract arrives as a dependency.
        File classes = tmp.newFolder("classes");
        List<String> classpath = Arrays.asList(classes.getPath(), shared.getPath(),
                backendRuntime().getPath());
        ProcessorContext stubs = new ProcessorContext(classes, tmp.newFolder(),
                Collections.<String, AnnotatedClass>emptyMap(), new SystemStreamLog(),
                null, null, null, null, null, classpath);
        new RestServerAnnotationProcessor().emitStubs(stubs);
        assertFalse(String.valueOf(stubs.getErrors()), stubs.hasErrors());
        Map<String, String> server = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> e : stubs.getEmittedStubSources().entrySet()) {
            server.put(e.getKey().replace('/', '.'), e.getValue());
        }
        server.put("com.example.RidesEndpoint", PKG
                + "import java.util.*;\n"
                + "@com.codename1.backend.annotations.Component\n"
                + "public class RidesEndpoint implements RidesApiServer {\n"
                + "    private final List<Ride> rides = new ArrayList<Ride>();\n"
                + "    public Ride create(Ride r) {\n"
                + "        String id = \"r\" + (rides.size() + 1) + (r.note() == null ? \"\" : \"!\");\n"
                + "        Ride saved = new Ride(id, r.kind(), r.pickup(),\n"
                + "                r.stops(), r.fareCents() * 2, !r.cash(), r.tags(), \"kept\");\n"
                + "        rides.add(saved);\n"
                + "        return saved;\n"
                + "    }\n"
                + "    public List<Ride> list() { return rides; }\n"
                + "}\n");
        JavaSourceCompiler.compile(server, classes, Arrays.asList(shared, backendRuntime()), null);
        ProcessorContext ctx = new ProcessorContext(classes, tmp.newFolder(), ClassScanner.scan(classes),
                new SystemStreamLog(), tmp.newFolder(), new Properties(), null,
                Collections.<String>emptyList(), "UTF-8", classpath);
        BackendBeanAnnotationProcessor beans = new BackendBeanAnnotationProcessor();
        RestControllerAnnotationProcessor controllers = new RestControllerAnnotationProcessor();
        beans.start(ctx);
        controllers.start(ctx);
        for (AnnotatedClass cls : ctx.getClassIndex().values()) {
            if (!cls.getClassAnnotations().isEmpty()) {
                controllers.processClass(cls, ctx);
            }
        }
        beans.finish(ctx);
        controllers.finish(ctx);
        assertFalse(String.valueOf(ctx.getErrors()), ctx.hasErrors());
        URLClassLoader serverLoader = new URLClassLoader(new URL[] {classes.toURI().toURL(),
                shared.toURI().toURL()}, getClass().getClassLoader());
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend.Builder builder = Backend.builder(Config.of(settings, "dev")).quiet();
        BackendAccess.get().application(builder, (BackendApplication) serverLoader
                .loadClass("com.example.BackendWiring").newInstance());
        Backend backend = builder.start();
        try {
            for (String name : new String[] {"Place", "Ride"}) {
                Mappers.register((Mapper<?>) client.loadClass("com.example." + name + "Cn1Mapper")
                        .newInstance());
            }
            Class<?> place = client.loadClass("com.example.Place");
            Class<?> ride = client.loadClass("com.example.Ride");
            Class<?> kind = client.loadClass("com.example.Kind");
            Object home = place.getConstructor(double.class, double.class, String.class)
                    .newInstance(32.5, 34.75, "home");
            Object stop = place.getConstructor(double.class, double.class, String.class)
                    .newInstance(1.0, -2.5, null);
            Object sent = ride.getConstructors()[0].newInstance(null, kind.getEnumConstants()[1], home,
                    new ArrayList<Object>(Arrays.asList(stop)), 1250L, true,
                    new ArrayList<Object>(Arrays.asList("quiet", "pet")), null);
            // An ignored component is neither read nor written: the server gets
            // its default however the body spells it, and never answers it.
            String body = "{\"note\":\"secret\"," + Mappers.toJson(sent).substring(1);

            String created = http("POST", port, "/rides", body);
            Object back = Mappers.fromJson(created, ride);
            assertEquals(created, "r1", call(back, "id"));
            assertEquals(created, kind.getEnumConstants()[1], call(back, "kind"));
            assertEquals(created, Long.valueOf(2500), call(back, "fareCents"));
            assertEquals(created, Boolean.FALSE, call(back, "cash"));
            assertEquals(created, Arrays.asList("quiet", "pet"), call(back, "tags"));
            assertEquals(created, home, call(back, "pickup"));
            assertFalse(created, created.contains("note"));
            assertEquals(created, Collections.singletonList(stop), call(back, "stops"));

            String listed = http("GET", port, "/rides", null);
            assertTrue(listed, listed.startsWith("[{\"id\":\"r1\",\"kind\":\"XL\",\"pickup\":{\"lat\":32.5,"));
            assertTrue(listed, listed.contains("\"stops\":[{\"lat\":1.0,\"lng\":-2.5,\"label\":null}]"));
        } finally {
            backend.stop();
            serverLoader.close();
            client.close();
        }
    }

    /// The legacy `cn1.restServer` codec moves public fields and decodes through a
    /// no-arg constructor, so a record would encode as `{}`. It must refuse one,
    /// naming the backend path that does handle records.
    @Test
    public void theLegacyRestServerCodecRefusesARecord() throws Exception {
        assumeTrue("records need Java 16 or newer", javaSpecAtLeast(16));
        File shared = compileContract();
        String before = System.getProperty("cn1.restServer");
        System.setProperty("cn1.restServer", "true");
        try {
            Map<String, AnnotatedClass> index = ClassScanner.scan(shared);
            RestServerAnnotationProcessor proc = new RestServerAnnotationProcessor();
            ProcessorContext ctx = new ProcessorContext(shared, tmp.newFolder(), index,
                    new SystemStreamLog(), tmp.newFolder(), new Properties(), null,
                    Collections.<String>emptyList(), "UTF-8",
                    Arrays.asList(shared.getPath(), testClassesDir().getPath()));
            proc.start(ctx);
            for (AnnotatedClass cls : index.values()) {
                if (!cls.getClassAnnotations().isEmpty()) {
                    proc.processClass(cls, ctx);
                }
            }
            proc.finish(ctx);
            assertTrue("expected a refusal", ctx.hasErrors());
            assertTrue(String.valueOf(ctx.getErrors()),
                    String.valueOf(ctx.getErrors()).contains("is a record"));
        } finally {
            if (before == null) {
                System.clearProperty("cn1.restServer");
            } else {
                System.setProperty("cn1.restServer", before);
            }
        }
    }

    /// The shared contract module, compiled at the running JDK's level (records
    /// need 16 or newer, which every caller assumes first).
    private File compileContract() throws Exception {
        Map<String, String> contract = new LinkedHashMap<String, String>();
        contract.put("com.example.Kind", PKG + "public enum Kind { STANDARD, XL }\n");
        contract.put("com.example.Place", PKG + "import com.codename1.annotations.Mapped;\n"
                + "@Mapped public record Place(double lat, double lng, String label) { }\n");
        // Components named after the generated mapper's own locals (`_kids`,
        // `_i`, `_e`, `_ch`, `_v`): a record's components are gathered in
        // locals too, and these once redeclared them, so the module failed to
        // build on the generated source.
        contract.put("com.example.Household", PKG + "import com.codename1.annotations.Mapped;\n"
                + "@Mapped public record Household(java.util.List<String> kids, int i, String e,\n"
                + "        String ch, String v, String m, String o) { }\n");
        contract.put("com.example.Ride", PKG + "import com.codename1.annotations.Mapped;\n"
                + "@Mapped public record Ride(String id, Kind kind, Place pickup,\n"
                + "        java.util.List<Place> stops, long fareCents, boolean cash,\n"
                + "        java.util.List<String> tags,\n"
                + "        @com.codename1.annotations.JsonIgnore String note) { }\n");
        contract.put("com.example.RidesApi", PKG
                + "import com.codename1.annotations.rest.*;\n"
                + "import com.codename1.io.rest.Response;\n"
                + "import com.codename1.util.OnComplete;\n"
                + "@RestClient public interface RidesApi {\n"
                + "    @POST(\"/rides\") void create(@Body Ride ride, OnComplete<Response<Ride>> callback);\n"
                + "    @GET(\"/rides\") void list(OnComplete<Response<java.util.List<Ride>>> callback);\n"
                + "}\n");
        File shared = tmp.newFolder("shared");
        // null: the running JDK's level.
        JavaSourceCompiler.compile(contract, shared, Arrays.asList(testClassesDir()), null);
        return shared;
    }

    private static Object call(Object target, String accessor) throws Exception {
        Method m = target.getClass().getMethod(accessor);
        return m.invoke(target);
    }

    private static String http(String method, int port, String path, String body) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + path)
                .openConnection();
        c.setRequestMethod(method);
        if (body != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            OutputStream out = c.getOutputStream();
            out.write(body.getBytes("UTF-8"));
            out.close();
        }
        int status = c.getResponseCode();
        InputStream in = status >= 400 ? c.getErrorStream() : c.getInputStream();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        if (in != null) {
            byte[] chunk = new byte[4096];
            int n;
            while ((n = in.read(chunk)) > 0) {
                buffer.write(chunk, 0, n);
            }
        }
        String text = new String(buffer.toByteArray(), "UTF-8");
        return status >= 400 ? "HTTP " + status + ": " + text : text;
    }

    private static void copy(final Path from, final Path to) throws Exception {
        try (java.util.stream.Stream<Path> all = Files.walk(from)) {
            for (Path p : (Iterable<Path>) all::iterator) {
                Path target = to.resolve(from.relativize(p).toString());
                if (Files.isDirectory(p)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(p, target);
                }
            }
        }
    }

    private static int freePort() throws Exception {
        ServerSocket s = new ServerSocket(0);
        try {
            return s.getLocalPort();
        } finally {
            s.close();
        }
    }

    private static File backendRuntime() throws Exception {
        return new File(Backend.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    private static File testClassesDir() throws Exception {
        return new File(RecordDtoTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    private static boolean javaSpecAtLeast(int target) {
        String spec = System.getProperty("java.specification.version", "");
        try {
            return spec.startsWith("1.") ? Integer.parseInt(spec.substring(2)) >= target
                    : Integer.parseInt(spec) >= target;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
}
