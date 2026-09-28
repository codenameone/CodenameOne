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

import com.codename1.backend.Backend;
import com.codename1.backend.Config;
import com.codename1.backend.HttpServer;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.ProcessorContext;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// The Spring-style programming model, end to end: sources that use it are
/// compiled, the processors resolve, weave and generate, and the generated
/// wiring then RUNS -- a real server on a real port, a real SQLite database --
/// because a wiring that compiles and injects the wrong bean, or a transaction
/// that begins and never rolls back, is exactly what a source-text assertion
/// would miss.
public class BackendBeansTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final String PKG = "package com.example;\n"
            + "import com.codename1.backend.*;\n"
            + "import com.codename1.backend.annotations.*;\n"
            + "import java.util.*;\n"
            + "import java.util.concurrent.*;\n"
            + "import java.util.concurrent.atomic.*;\n";

    private static Map<String, String> sample() {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Greeter", PKG
                + "public interface Greeter { String greet(String name); }\n");
        s.put("com.example.PoliteGreeter", PKG
                + "@Service\n"
                + "public class PoliteGreeter implements Greeter {\n"
                + "    @Value(\"${greeting.prefix:Hello}\") private String prefix;\n"
                + "    private int initialized;\n"
                + "    @PostConstruct void init() { initialized++; }\n"
                + "    public String greet(String name) {\n"
                + "        return prefix + \", \" + name + \" (\" + initialized + \")\";\n"
                + "    }\n"
                + "}\n");
        s.put("com.example.Notes", PKG
                + "@Repository\n"
                + "public class Notes {\n"
                + "    private final DataSource db;\n"
                + "    public Notes(DataSource db) { this.db = db; }\n"
                + "    @PostConstruct public void schema() throws java.io.IOException {\n"
                + "        db.execute(\"CREATE TABLE IF NOT EXISTS notes (text TEXT)\", null);\n"
                + "    }\n"
                + "    @Transactional\n"
                + "    public void addTwo(String a, String b) throws java.io.IOException {\n"
                + "        db.execute(\"INSERT INTO notes (text) VALUES (?)\", new Object[] {a});\n"
                + "        if (b == null) { throw new IllegalStateException(\"no second note\"); }\n"
                + "        db.execute(\"INSERT INTO notes (text) VALUES (?)\", new Object[] {b});\n"
                + "    }\n"
                + "    @Transactional(readOnly = true)\n"
                + "    public int count() throws java.io.IOException {\n"
                + "        Map row = db.queryOne(\"SELECT COUNT(*) AS n FROM notes\", null);\n"
                + "        return ((Number) row.get(\"n\")).intValue();\n"
                + "    }\n"
                + "}\n");
        s.put("com.example.Jobs", PKG
                + "@Service\n"
                + "public class Jobs {\n"
                + "    public final AtomicInteger runs = new AtomicInteger();\n"
                + "    @Scheduled(fixedRate = 20)\n"
                + "    public void tick() { runs.incrementAndGet(); }\n"
                + "    @Async\n"
                + "    public Future later(String v) {\n"
                + "        return AsyncResult.of(v + \" on \" + (Thread.currentThread().getName()"
                + ".startsWith(\"cn1-task\") ? \"a task thread\" : \"the caller\"));\n"
                + "    }\n"
                + "}\n");
        s.put("com.example.RequestInfo", PKG
                + "@Component @RequestScope\n"
                + "public class RequestInfo {\n"
                + "    private final String id = String.valueOf(System.nanoTime());\n"
                + "    public String id() { return id; }\n"
                + "}\n");
        s.put("com.example.Cache", PKG
                + "@Component @ManagedResource(objectName = \"cache\")\n"
                + "public class Cache {\n"
                + "    private int cleared;\n"
                + "    @ManagedAttribute public int getSize() { return 3 - cleared; }\n"
                + "    @ManagedOperation public void clear() { cleared = 3; }\n"
                + "}\n");
        s.put("com.example.Api", PKG
                + "@RestController\n"
                + "public class Api {\n"
                + "    @Autowired private Greeter greeter;\n"
                + "    @Autowired private RequestInfo request;\n"
                + "    private final Notes notes;\n"
                + "    private final Jobs jobs;\n"
                + "    public Api(Notes notes, Jobs jobs) { this.notes = notes; this.jobs = jobs; }\n"
                + "    @GetMapping(\"/hello/{name}\")\n"
                + "    public String hello(@PathVariable(\"name\") String name) {\n"
                + "        return greeter.greet(name);\n"
                + "    }\n"
                + "    @PostMapping(\"/notes\")\n"
                + "    public String add(@RequestParam(\"a\") String a,\n"
                + "                      @RequestParam(value = \"b\", required = false) String b)\n"
                + "            throws Exception {\n"
                + "        try { notes.addTwo(a, b); } catch (IllegalStateException e) {\n"
                + "            return \"rolled back\";\n"
                + "        }\n"
                + "        return \"ok\";\n"
                + "    }\n"
                + "    @GetMapping(\"/notes/count\")\n"
                + "    public String count() throws Exception { return String.valueOf(notes.count()); }\n"
                + "    @GetMapping(\"/async\")\n"
                + "    public String async() throws Exception { return (String) jobs.later(\"x\").get(); }\n"
                + "    @GetMapping(\"/ticks\")\n"
                + "    public String ticks() { return String.valueOf(jobs.runs.get()); }\n"
                + "    @GetMapping(\"/rid\")\n"
                + "    public String rid() { return request.id() + \"|\" + request.id(); }\n"
                + "    @McpTool(description = \"Greets someone\")\n"
                + "    public String greet(@McpParam(\"name\") String name) { return greeter.greet(name); }\n"
                + "}\n");
        return s;
    }

    @Test
    public void theGeneratedWiringRunsTheSample() throws Exception {
        File classes = compile(sample());
        ProcessorContext ctx = process(classes);
        assertNoErrors(ctx);
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL()},
                getClass().getClassLoader());
        Backend.Application app = (Backend.Application) loader
                .loadClass("com.example.BackendWiring").newInstance();
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        settings.setProperty("greeting.prefix", "Hi");
        // What the generated main does, with the development tools a JVM build has.
        Backend backend = Backend.builder(Config.of(settings, "dev"))
                .quiet()
                .requiresDataSource()
                .mcp(new com.codename1.backend.mcp.DevTools())
                .application(app)
                .start();
        try {
            // Field injection into a private field, @Value, and a package-private
            // @PostConstruct that ran exactly once.
            assertEquals("Hi, Ada (1)", http("GET", port, "/hello/Ada"));
            // A transaction that commits, and one whose unchecked exception
            // rolls back the insert that came before it.
            assertEquals("ok", http("POST", port, "/notes?a=one&b=two"));
            assertEquals("2", http("GET", port, "/notes/count"));
            assertEquals("rolled back", http("POST", port, "/notes?a=three"));
            assertEquals("the first insert of a rolled-back transaction was kept", "2",
                    http("GET", port, "/notes/count"));
            // @Async runs on a task thread and the caller's Future completes.
            assertEquals("x on a task thread", http("GET", port, "/async"));
            // Request scope: one instance within a request, another in the next.
            String first = http("GET", port, "/rid");
            String[] halves = first.split("\\|");
            assertEquals("a request-scoped bean changed within one request", halves[0], halves[1]);
            assertNotEquals("two requests shared a request-scoped bean", first,
                    http("GET", port, "/rid"));
            // The scheduled job fires on its own.
            long deadline = System.currentTimeMillis() + 5000;
            while (Integer.parseInt(http("GET", port, "/ticks")) < 3
                    && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            assertTrue("the fixed-rate job did not run",
                    Integer.parseInt(http("GET", port, "/ticks")) >= 3);
            // The MCP endpoint lists and calls the tool, and the dev tools see
            // the routes and the managed bean.
            String tools = post(port, "/mcp",
                    "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}");
            assertTrue(tools, tools.contains("\"greet\""));
            String call = post(port, "/mcp", "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":"
                    + "\"tools/call\",\"params\":{\"name\":\"greet\",\"arguments\":"
                    + "{\"name\":\"Bob\"}}}");
            assertTrue(call, call.contains("Hi, Bob (1)"));
            String managed = http("GET", port, "/manage/managed");
            assertTrue(managed, managed.contains("\"size\":3"));
            String routes = post(port, "/mcp", "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":"
                    + "\"tools/call\",\"params\":{\"name\":\"backend_routes\"}}");
            // backend_call sends a request through the running server. Its method
            // was once named like McpTool.call, and the tool recursed into itself.
            String sent = post(port, "/mcp", "{\"jsonrpc\":\"2.0\",\"id\":31,\"method\":"
                    + "\"tools/call\",\"params\":{\"name\":\"backend_call\",\"arguments\":"
                    + "{\"method\":\"GET\",\"path\":\"/hello/Cy\"}}}");
            assertTrue(sent, sent.contains("Hi, Cy") && sent.contains("\"isError\":false"));
            assertTrue(routes, routes.contains("/hello/{name}"));
            String beans = post(port, "/mcp", "{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":"
                    + "\"tools/call\",\"params\":{\"name\":\"backend_beans\"}}");
            assertTrue(beans, beans.contains("politeGreeter"));
            String sql = post(port, "/mcp", "{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":"
                    + "\"tools/call\",\"params\":{\"name\":\"backend_sql\",\"arguments\":"
                    + "{\"sql\":\"SELECT text FROM notes ORDER BY text\"}}}");
            assertTrue(sql, sql.contains("one") && sql.contains("two") && !sql.contains("three"));
            String refused = post(port, "/mcp", "{\"jsonrpc\":\"2.0\",\"id\":6,\"method\":"
                    + "\"tools/call\",\"params\":{\"name\":\"backend_sql\",\"arguments\":"
                    + "{\"sql\":\"DELETE FROM notes\"}}}");
            assertTrue("a write ran without write=true: " + refused,
                    refused.contains("\"isError\":true"));
            // Begins like a read, deletes like a write: the engine must refuse it.
            String disguised = post(port, "/mcp", "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":"
                    + "\"tools/call\",\"params\":{\"name\":\"backend_sql\",\"arguments\":"
                    + "{\"sql\":\"WITH x AS (SELECT 1) DELETE FROM notes\"}}}");
            assertTrue("a disguised write ran without write=true: " + disguised,
                    disguised.contains("\"isError\":true"));
            String still = post(port, "/mcp", "{\"jsonrpc\":\"2.0\",\"id\":8,\"method\":"
                    + "\"tools/call\",\"params\":{\"name\":\"backend_sql\",\"arguments\":"
                    + "{\"sql\":\"SELECT text FROM notes ORDER BY text\"}}}");
            assertTrue(still, still.contains("one") && still.contains("two"));
            String metrics = http("GET", port, "/manage/prometheus");
            assertTrue(metrics, metrics.contains("http_server_request_duration_bucket"));
            // The scheduler is built by the application after the server knows it
            // measures; its runs must still be recorded.
            assertTrue("the scheduled job's runs were not measured: " + metrics,
                    metrics.contains("Jobs.tick"));
        } finally {
            backend.stop();
        }
    }

    @Test
    public void theEntryPointNamesTheWiring() throws Exception {
        File classes = compile(sample());
        RestControllerAnnotationProcessor proc = new RestControllerAnnotationProcessor();
        ProcessorContext ctx = process(classes, proc);
        assertNoErrors(ctx);
        String bootstrap = proc.generateBootstrap("com.example");
        assertTrue(bootstrap, bootstrap.contains(".application(new com.example.BackendWiring())"));
        assertTrue("a bean takes a DataSource, so the entry point must ask for a database:\n"
                + bootstrap, bootstrap.contains(".requiresDataSource()"));
        String wiring = proc.generateWiring("com.example");
        assertTrue(wiring, wiring.contains("new com.example.Api("));
        assertTrue("the private field is injected through the woven setter:\n" + wiring,
                wiring.contains(".cn1$inject$greeter("));
    }

    @Test
    public void aMissingBeanIsABuildErrorNamingTheInjectionPoint() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Mailer", PKG + "public interface Mailer { }\n");
        s.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    public Api(Mailer mailer) { }\n"
                + "    @GetMapping(\"/x\") public String x() { return \"x\"; }\n"
                + "}\n");
        ProcessorContext ctx = process(compile(s));
        assertTrue("a constructor needing a bean nobody provides built", ctx.hasErrors());
        assertTrue(String.valueOf(ctx.getErrors()), String.valueOf(ctx.getErrors())
                .contains("constructor parameter 1 of com.example.Api needs a com.example.Mailer"));
    }

    @Test
    public void twoCandidatesWithoutPrimaryAreAmbiguous() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Mailer", PKG + "public interface Mailer { }\n");
        s.put("com.example.SmtpMailer", PKG + "@Component public class SmtpMailer implements Mailer { }\n");
        s.put("com.example.LogMailer", PKG + "@Component public class LogMailer implements Mailer { }\n");
        s.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    public Api(Mailer mailer) { }\n"
                + "    @GetMapping(\"/x\") public String x() { return \"x\"; }\n"
                + "}\n");
        ProcessorContext ctx = process(compile(s));
        assertTrue(String.valueOf(ctx.getErrors()), String.valueOf(ctx.getErrors())
                .contains("Mark one @Primary, or name one with @Qualifier"));
        s.put("com.example.LogMailer", PKG
                + "@Component @Primary public class LogMailer implements Mailer { }\n");
        assertNoErrors(process(compile(s)));
    }

    @Test
    public void aConstructorCycleIsRefused() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.A", PKG + "@Component public class A { public A(B b) { } }\n");
        s.put("com.example.B", PKG + "@Component public class B { public B(A a) { } }\n");
        ProcessorContext ctx = process(compile(s));
        assertTrue(String.valueOf(ctx.getErrors()), String.valueOf(ctx.getErrors())
                .contains("The constructors form a cycle"));
    }

    @Test
    public void aFieldCycleIsFine() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.A", PKG + "@Component public class A { @Autowired B b; public B b() { return b; } }\n");
        s.put("com.example.B", PKG + "@Component public class B { @Autowired A a; public A a() { return a; } }\n");
        s.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    private final A a;\n"
                + "    public Api(A a) { this.a = a; }\n"
                + "    @GetMapping(\"/x\") public String x() {\n"
                + "        return String.valueOf(a.b().a() == a);\n"
                + "    }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        int port = freePort();
        Backend backend = start(classes, port, new Properties());
        try {
            assertEquals("true", http("GET", port, "/x"));
        } finally {
            backend.stop();
        }
    }

    @Test
    public void anInvalidCronExpressionIsABuildError() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Jobs", PKG
                + "@Service public class Jobs {\n"
                + "    @Scheduled(cron = \"0 0 25 * * *\") public void never() { }\n"
                + "}\n");
        ProcessorContext ctx = process(compile(s));
        assertTrue(String.valueOf(ctx.getErrors()), String.valueOf(ctx.getErrors())
                .contains("the hour field of \"0 0 25 * * *\" names 25"));
    }

    @Test
    public void anAsyncMethodMustReturnVoidOrFuture() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Jobs", PKG
                + "@Service public class Jobs {\n"
                + "    @Async public String now() { return \"x\"; }\n"
                + "}\n");
        ProcessorContext ctx = process(compile(s));
        assertTrue(String.valueOf(ctx.getErrors()), String.valueOf(ctx.getErrors())
                .contains("can only receive nothing or a java.util.concurrent.Future"));
    }

    @Test
    public void profilesPickTheBeanAtStartUp() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Mailer", PKG + "public interface Mailer { String name(); }\n");
        s.put("com.example.DevMailer", PKG + "@Component @Profile(\"dev\") public class DevMailer "
                + "implements Mailer { public String name() { return \"dev\"; } }\n");
        s.put("com.example.ProdMailer", PKG + "@Component @Profile(\"!dev\") public class ProdMailer "
                + "implements Mailer { public String name() { return \"prod\"; } }\n");
        s.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    private final Mailer mailer;\n"
                + "    public Api(Mailer mailer) { this.mailer = mailer; }\n"
                + "    @GetMapping(\"/x\") public String x() { return mailer.name(); }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        int port = freePort();
        Backend dev = start(classes, port, new Properties());
        try {
            assertEquals("dev", http("GET", port, "/x"));
        } finally {
            dev.stop();
        }
    }

    @Test
    public void aFactoryBeanIsConditionalOnItsConfigurationClass() throws Exception {
        // A @Profile("prod") configuration's @Bean methods, static or not, exist
        // only on prod -- as in Spring -- rather than being built on every
        // profile, the instance one through a configuration object that is null.
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Marker", PKG + "public class Marker { }\n");
        s.put("com.example.ProdConfig", PKG + "@Configuration @Profile(\"prod\")\n"
                + "@ConditionalOnProperty(\"prod.enabled\") public class ProdConfig {\n"
                + "    @Bean public StringBuilder prodThing() { return new StringBuilder(\"p\"); }\n"
                + "    @Bean public static Marker prodMarker() { return new Marker(); }\n"
                + "}\n");
        s.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    @Autowired(required = false) private StringBuilder thing;\n"
                + "    @Autowired(required = false) private Marker marker;\n"
                + "    @GetMapping(\"/x\") public String x() {\n"
                + "        return (thing != null) + \",\" + (marker != null);\n"
                + "    }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        int port = freePort();
        Backend dev = start(classes, port, new Properties());
        try {
            assertEquals("false,false", http("GET", port, "/x"));
        } finally {
            dev.stop();
        }
    }

    @Test
    public void aRequestScopedFactoryBeanIsDestroyedByItsDestroyMethod() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Handle", PKG + "public class Handle {\n"
                + "    public static int closed;\n"
                + "    public void close() { closed++; }\n"
                + "    public String use() { return \"used\"; }\n"
                + "}\n");
        s.put("com.example.Handles", PKG + "@Configuration public class Handles {\n"
                + "    @Bean(destroyMethod = \"close\") @RequestScope\n"
                + "    public Handle handle() { return new Handle(); }\n"
                + "}\n");
        s.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    @Autowired private Handle handle;\n"
                + "    @GetMapping(\"/x\") public String x() {\n"
                + "        handle.use();\n"
                + "        return String.valueOf(Handle.closed);\n"
                + "    }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        int port = freePort();
        Backend backend = start(classes, port, new Properties());
        try {
            assertEquals("0", http("GET", port, "/x"));
            assertEquals("the first request's bean was never closed", "1",
                    http("GET", port, "/x"));
        } finally {
            backend.stop();
        }
    }

    @Test
    public void membersInheritedFromABaseClassAreInjected() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Clock", PKG + "@Component public class Clock "
                + "{ public String now() { return \"t\"; } }\n");
        s.put("com.example.BaseService", PKG + "public abstract class BaseService {\n"
                + "    @Autowired private Clock clock;\n"
                + "    @Value(\"${label:base}\") protected String label;\n"
                + "    protected int inits;\n"
                + "    @PostConstruct void baseInit() { inits++; }\n"
                + "    protected String stamp() { return clock.now() + label; }\n"
                + "}\n");
        s.put("com.example.Orders", PKG + "@Service public class Orders extends BaseService {\n"
                + "    @PostConstruct void ownInit() { inits += 10; }\n"
                + "    public String describe() { return stamp() + inits; }\n"
                + "}\n");
        s.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    private final Orders orders;\n"
                + "    public Api(Orders orders) { this.orders = orders; }\n"
                + "    @GetMapping(\"/x\") public String x() { return orders.describe(); }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        int port = freePort();
        Backend backend = start(classes, port, new Properties());
        try {
            // Base field and value injected, base @PostConstruct run, then the bean's.
            assertEquals("tbase11", http("GET", port, "/x"));
        } finally {
            backend.stop();
        }
    }

    @Test
    public void aMissingBeanDefaultStepsAsideForAnotherImplementation() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Mailer", PKG + "public interface Mailer { String name(); }\n");
        s.put("com.example.DefaultMailer", PKG + "@Component @ConditionalOnMissingBean\n"
                + "public class DefaultMailer implements Mailer "
                + "{ public String name() { return \"default\"; } }\n");
        s.put("com.example.SmtpMailer", PKG + "@Component public class SmtpMailer "
                + "implements Mailer { public String name() { return \"smtp\"; } }\n");
        s.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    private final Mailer mailer;\n"
                + "    public Api(Mailer mailer) { this.mailer = mailer; }\n"
                + "    @GetMapping(\"/x\") public String x() { return mailer.name(); }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        int port = freePort();
        Backend backend = start(classes, port, new Properties());
        try {
            assertEquals("smtp", http("GET", port, "/x"));
        } finally {
            backend.stop();
        }
    }

    @Test
    public void aSynchronizedAsyncMethodHoldsItsMonitorWhereTheBodyRuns() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Worker", PKG + "@Service public class Worker {\n"
                + "    @Async public synchronized void work() { }\n"
                + "}\n");
        s.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    private final Worker w;\n"
                + "    public Api(Worker w) { this.w = w; }\n"
                + "    @GetMapping(\"/x\") public String x() { w.work(); return \"ok\"; }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL()},
                getClass().getClassLoader());
        Class<?> worker = loader.loadClass("com.example.Worker");
        java.lang.reflect.Method body = worker.getDeclaredMethod("work$cn1body");
        assertTrue("the woven body lost its synchronized, so executor threads could run it "
                + "at once", java.lang.reflect.Modifier.isSynchronized(body.getModifiers()));
    }

    @Test
    public void sessionScopedBeansAreDestroyedWhenTheSessionIsInvalidated() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Cart", PKG + "@Component @SessionScope public class Cart {\n"
                + "    public static int destroyed;\n"
                + "    private int items;\n"
                + "    public int add() { return ++items; }\n"
                + "    @PreDestroy void close() { destroyed++; }\n"
                + "}\n");
        s.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    @Autowired private Cart cart;\n"
                + "    @GetMapping(\"/add\") public String add() { return String.valueOf(cart.add()); }\n"
                + "    @GetMapping(\"/logout\") public String logout(HttpServer.Request r) {\n"
                + "        r.getSession(true).invalidate();\n"
                + "        return \"out\";\n"
                + "    }\n"
                + "    @GetMapping(\"/destroyed\") public String destroyed() {\n"
                + "        return String.valueOf(Cart.destroyed);\n"
                + "    }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        int port = freePort();
        Backend backend = start(classes, port, new Properties());
        try {
            HttpURLConnection first = (HttpURLConnection) new URL("http://127.0.0.1:" + port
                    + "/add").openConnection();
            assertEquals("1", read(first));
            String cookie = first.getHeaderField("Set-Cookie");
            String pair = cookie.substring(0, cookie.indexOf(';'));
            HttpURLConnection out = (HttpURLConnection) new URL("http://127.0.0.1:" + port
                    + "/logout").openConnection();
            out.setRequestProperty("Cookie", pair);
            assertEquals("out", read(out));
            assertEquals("the invalidated session's bean was never destroyed", "1",
                    http("GET", port, "/destroyed"));
            // A session still open when the server stops is destroyed with it.
            assertEquals("1", http("GET", port, "/add"));
        } finally {
            backend.stop();
        }
        URLClassLoader loader = (URLClassLoader) backend.getApplication().getClass()
                .getClassLoader();
        assertEquals(2, loader.loadClass("com.example.Cart").getField("destroyed").getInt(null));
    }

    @Test
    public void aScopedOrOverloadedManagedResourceIsABuildError() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Visits", PKG + "@Component @SessionScope @ManagedResource\n"
                + "public class Visits {\n"
                + "    @ManagedAttribute public int getCount() { return 1; }\n"
                + "}\n");
        s.put("com.example.Cache", PKG + "@Component @ManagedResource public class Cache {\n"
                + "    @ManagedOperation public void evict() { }\n"
                + "    @ManagedOperation public void evict(String key) { }\n"
                + "}\n");
        ProcessorContext ctx = process(compile(s));
        String errors = String.valueOf(ctx.getErrors());
        assertTrue(errors, errors.contains("must be a singleton"));
        assertTrue(errors, errors.contains("is overloaded"));
    }

    @Test
    public void aFactoryBeanRunsInheritedCallbacksAndDestroysSubclassFirst() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Log", PKG + "public class Log { public static String text = \"\"; }\n");
        s.put("com.example.BaseClient", PKG + "public class BaseClient {\n"
                + "    @PostConstruct public void open() { Log.text += \"open,\"; }\n"
                + "    @PreDestroy public void closeBase() { Log.text += \"base,\"; }\n"
                + "}\n");
        s.put("com.example.Client", PKG + "public class Client extends BaseClient {\n"
                + "    @PreDestroy public void flush() { Log.text += \"sub,\"; }\n"
                + "}\n");
        s.put("com.example.Clients", PKG + "@Configuration public class Clients {\n"
                + "    @Bean public Client client() { return new Client(); }\n"
                + "}\n");
        s.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    @Autowired private Client client;\n"
                + "    @GetMapping(\"/x\") public String x() { return Log.text; }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        int port = freePort();
        Backend backend = start(classes, port, new Properties());
        try {
            assertEquals("open,", http("GET", port, "/x"));
        } finally {
            backend.stop();
        }
        Class<?> log = backend.getApplication().getClass().getClassLoader()
                .loadClass("com.example.Log");
        assertEquals("the subclass must release its state before the base tears down",
                "open,sub,base,", log.getField("text").get(null));
    }

    @Test
    public void aRequestBeanCanUseAnotherWhileItIsDestroyed() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Clock", PKG + "@Component @RequestScope public class Clock {\n"
                + "    public String now() { return \"t\"; }\n"
                + "}\n");
        s.put("com.example.Audit", PKG + "@Component @RequestScope public class Audit {\n"
                + "    public static String last = \"none\";\n"
                + "    @Autowired private Clock clock;\n"
                + "    public void touch() { }\n"
                + "    @PreDestroy public void done() { last = clock.now(); }\n"
                + "}\n");
        s.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    @Autowired private Audit audit;\n"
                + "    @GetMapping(\"/x\") public String x() { audit.touch(); return Audit.last; }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        int port = freePort();
        Backend backend = start(classes, port, new Properties());
        try {
            assertEquals("none", http("GET", port, "/x"));
            assertEquals("the @PreDestroy could not reach a request-scoped dependency", "t",
                    http("GET", port, "/x"));
        } finally {
            backend.stop();
        }
    }

    @Test
    public void aSessionBeanBuiltByAFailingRequestIsStillDestroyed() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Cart", PKG + "@Component @SessionScope public class Cart {\n"
                + "    public static int destroyed;\n"
                + "    public void add() { }\n"
                + "    @PreDestroy void close() { destroyed++; }\n"
                + "}\n");
        s.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    @Autowired private Cart cart;\n"
                + "    @GetMapping(\"/fail\") public String fail() {\n"
                + "        cart.add();\n"
                + "        throw new IllegalStateException(\"boom\");\n"
                + "    }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        int port = freePort();
        Backend backend = start(classes, port, new Properties());
        try {
            HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port
                    + "/fail").openConnection();
            assertEquals(500, c.getResponseCode());
        } finally {
            backend.stop();
        }
        Class<?> cart = backend.getApplication().getClass().getClassLoader()
                .loadClass("com.example.Cart");
        assertEquals("a session bean built by a failing request was dropped undestroyed", 1,
                cart.getField("destroyed").getInt(null));
    }

    @Test
    public void aConfigurationThatStepsAsideTakesItsFactoriesWithIt() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Store", PKG + "public interface Store { String name(); }\n");
        s.put("com.example.DefaultStores", PKG
                + "@Configuration @ConditionalOnMissingBean(DbStore.class)\n"
                + "public class DefaultStores {\n"
                + "    @Bean public static Store memoryStore() {\n"
                + "        return new Store() { public String name() { return \"memory\"; } };\n"
                + "    }\n"
                + "}\n");
        s.put("com.example.DbStore", PKG + "@Component public class DbStore implements Store "
                + "{ public String name() { return \"db\"; } }\n");
        s.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    private final Store store;\n"
                + "    public Api(Store store) { this.store = store; }\n"
                + "    @GetMapping(\"/x\") public String x() { return store.name(); }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        int port = freePort();
        Backend backend = start(classes, port, new Properties());
        try {
            assertEquals("db", http("GET", port, "/x"));
        } finally {
            backend.stop();
        }
    }

    @Test
    public void oneMetricNameForTwoKindsIsABuildError() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Work", PKG + "@Component public class Work {\n"
                + "    @Timed(\"work\") @Counted(\"work\") public void both() { }\n"
                + "}\n");
        String errors = String.valueOf(process(compile(s)).getErrors());
        assertTrue(errors, errors.contains("Metric work is a counter for @Counted on "
                + "com.example.Work.both and a histogram for @Timed on com.example.Work.both"));
        // Across two methods too: the instruments are the process's.
        s.put("com.example.Work", PKG + "@Component public class Work {\n"
                + "    @Timed(\"jobs\") public void a() { }\n"
                + "    @Counted(\"jobs\") public void b() { }\n"
                + "}\n");
        errors = String.valueOf(process(compile(s)).getErrors());
        assertTrue(errors, errors.contains("Metric jobs is a"));
        s.put("com.example.Work", PKG + "@Component public class Work {\n"
                + "    @Timed(\"jobs.time\") @Counted(\"jobs\") public void a() { }\n"
                + "    @Counted(\"jobs\") public void b() { }\n"
                + "}\n");
        assertNoErrors(process(compile(s)));
    }

    @Test
    public void anOverloadedPropertySetterBindsOnce() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Timeouts", PKG + "@Component @ConfigurationProperties(\"t\")\n"
                + "public class Timeouts {\n"
                + "    public String calls = \"\";\n"
                + "    private int timeout;\n"
                + "    public void setTimeout(String v) { calls += \"string:\" + v + \",\"; }\n"
                + "    public void setTimeout(int v) { timeout = v; calls += \"int:\" + v + \",\"; }\n"
                + "    public int getTimeout() { return timeout; }\n"
                + "}\n");
        s.put("com.example.Api", PKG + "@RestController public class Api {\n"
                + "    @Autowired private Timeouts timeouts;\n"
                + "    @GetMapping(\"/x\") public String x() { return timeouts.calls; }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty("t.timeout", "5");
        Backend backend = start(classes, port, settings);
        try {
            assertEquals("each overload bound the same key", "int:5,", http("GET", port, "/x"));
        } finally {
            backend.stop();
        }
    }

    @Test
    public void anInactiveControllerIsNotListed() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Api", PKG + "@RestController public class Api {\n"
                + "    @GetMapping(\"/x\") public String x() { return \"x\"; }\n"
                + "}\n");
        s.put("com.example.Admin", PKG + "@RestController @ConditionalOnProperty(\"admin.on\")\n"
                + "public class Admin {\n"
                + "    @GetMapping(\"/admin\") public String admin() { return \"admin\"; }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL()},
                getClass().getClassLoader());
        Backend.Application app = (Backend.Application) loader
                .loadClass("com.example.BackendWiring").newInstance();
        int port = freePort();
        Properties off = new Properties();
        off.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend backend = Backend.builder(Config.of(off, "dev")).quiet().application(app).start();
        try {
            String listed = String.valueOf(app.describeRoutes());
            assertTrue(listed, listed.contains("/x"));
            assertFalse("an inactive controller's route was listed: " + listed,
                    listed.contains("/admin"));
        } finally {
            backend.stop();
        }
        Properties on = new Properties();
        on.setProperty(Config.SERVER_PORT, String.valueOf(port));
        on.setProperty("admin.on", "true");
        backend = Backend.builder(Config.of(on, "dev")).quiet().application(app).start();
        try {
            assertTrue(String.valueOf(app.describeRoutes()).contains("/admin"));
        } finally {
            backend.stop();
        }
    }

    @Test
    public void anInactiveOptionalDependencyKeepsTheInitializer() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Feature", PKG + "public class Feature {\n"
                + "    public String name() { return \"fallback\"; }\n"
                + "}\n");
        s.put("com.example.RealFeature", PKG + "@Component @ConditionalOnProperty(\"feature.x\")\n"
                + "public class RealFeature extends Feature {\n"
                + "    public String name() { return \"real\"; }\n"
                + "}\n");
        s.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    @Autowired(required = false) private Feature feature = new Feature();\n"
                + "    private Feature viaSetter = new Feature();\n"
                + "    @Autowired(required = false) public void use(Feature f) { viaSetter = f; }\n"
                + "    @GetMapping(\"/x\") public String x() {\n"
                + "        return feature.name() + \" \" + viaSetter.name();\n"
                + "    }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        int port = freePort();
        Backend off = start(classes, port, new Properties());
        try {
            assertEquals("an inactive conditional bean overwrote the initializer with null",
                    "fallback fallback", http("GET", port, "/x"));
        } finally {
            off.stop();
        }
        Properties settings = new Properties();
        settings.setProperty("feature.x", "true");
        Backend on = start(classes, port, settings);
        try {
            assertEquals("real real", http("GET", port, "/x"));
        } finally {
            on.stop();
        }
    }

    @Test
    public void aRestartedApplicationForgetsTheBeansOfTheLastStart() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Feature", PKG + "@Component @ConditionalOnProperty(\"feature.x\")\n"
                + "public class Feature { }\n");
        s.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    @Autowired(required = false) private Feature feature;\n"
                + "    @GetMapping(\"/x\") public String x() { return String.valueOf(feature != null); }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL()},
                getClass().getClassLoader());
        Backend.Application app = (Backend.Application) loader
                .loadClass("com.example.BackendWiring").newInstance();
        int port = freePort();
        Properties on = new Properties();
        on.setProperty(Config.SERVER_PORT, String.valueOf(port));
        on.setProperty("feature.x", "true");
        Backend first = Backend.builder(Config.of(on, "dev")).quiet().application(app).start();
        try {
            assertEquals("true", http("GET", port, "/x"));
        } finally {
            first.stop();
        }
        Properties off = new Properties();
        off.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend second = Backend.builder(Config.of(off, "dev")).quiet().application(app).start();
        try {
            assertEquals("the second start injected the first start's destroyed bean",
                    "false", http("GET", port, "/x"));
        } finally {
            second.stop();
        }
    }

    @Test
    public void aModuleWithOnlyAManagedResourceGetsAnApplication() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Stats", PKG + "@Component @ManagedResource public class Stats {\n"
                + "    @ManagedAttribute public int getCount() { return 1; }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        assertTrue("no application was generated for a managed-resource-only module",
                new File(classes, "com/example/BackendWiring.class").isFile());
    }

    @Test
    public void aScopedBeanForwardsMethodsItInheritsFromALibrary() throws Exception {
        Map<String, String> lib = new LinkedHashMap<String, String>();
        lib.put("org.lib.BaseCounter", "package org.lib;\n"
                + "public class BaseCounter {\n"
                + "    private int n;\n"
                + "    public int next() { return ++n; }\n"
                + "}\n");
        File libClasses = tmp.newFolder();
        JavaSourceCompiler.compile(lib, libClasses, backendClasspath());
        List<File> cp = new ArrayList<File>(backendClasspath());
        cp.add(libClasses);
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Visits", PKG + "@Component @RequestScope\n"
                + "public class Visits extends org.lib.BaseCounter { }\n");
        s.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    @Autowired private Visits visits;\n"
                + "    @GetMapping(\"/x\") public String x() {\n"
                + "        visits.next();\n"
                + "        return String.valueOf(visits.next());\n"
                + "    }\n"
                + "}\n");
        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(s, classes, cp);
        assertNoErrors(process(classes, new RestControllerAnnotationProcessor(), libClasses));
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL(),
                libClasses.toURI().toURL()}, getClass().getClassLoader());
        Class<?> proxy = loader.loadClass("com.example.VisitsCn1Scoped");
        assertNotNull("the stand-in does not forward next(), which its bean inherits",
                proxy.getDeclaredMethod("next"));
    }

    @Test
    public void aRequestBeanDestroyedAfterTheHandlerCanStillUseTheSession() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Visit", PKG + "@Component @RequestScope public class Visit {\n"
                + "    @Autowired private HttpServer.Request request;\n"
                + "    public void touch() { }\n"
                + "    @PreDestroy public void done() {\n"
                + "        request.getSession(true).setAttribute(\"seen\", \"yes\");\n"
                + "    }\n"
                + "}\n");
        s.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    @Autowired private Visit visit;\n"
                + "    @GetMapping(\"/x\") public String x(HttpServer.Request r) {\n"
                + "        visit.touch();\n"
                + "        HttpSession session = r.getSession(false);\n"
                + "        return session == null ? \"none\" : String.valueOf(session.getAttribute(\"seen\"));\n"
                + "    }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        int port = freePort();
        Backend backend = start(classes, port, new Properties());
        try {
            HttpURLConnection first = (HttpURLConnection) new URL("http://127.0.0.1:" + port
                    + "/x").openConnection();
            assertEquals("none", read(first));
            String cookie = first.getHeaderField("Set-Cookie");
            assertNotNull("the session a @PreDestroy started was never sent to the client",
                    cookie);
            HttpURLConnection second = (HttpURLConnection) new URL("http://127.0.0.1:" + port
                    + "/x").openConnection();
            second.setRequestProperty("Cookie", cookie.substring(0, cookie.indexOf(';')));
            assertEquals("yes", read(second));
        } finally {
            backend.stop();
        }
    }

    @Test
    public void aFailedStatementRollsBackAndAnotherCheckedExceptionCommits() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Rows", PKG + "@Service public class Rows {\n"
                + "    private final DataSource db;\n"
                + "    public Rows(DataSource db) { this.db = db; }\n"
                + "    @PostConstruct public void schema() throws java.io.IOException {\n"
                + "        db.execute(\"CREATE TABLE r (v TEXT)\", null);\n"
                + "    }\n"
                + "    @Transactional public void badStatement() throws java.io.IOException {\n"
                + "        db.execute(\"INSERT INTO r (v) VALUES ('a')\", null);\n"
                + "        db.execute(\"INSERT INTO no_such_table (v) VALUES ('b')\", null);\n"
                + "    }\n"
                + "    @Transactional public void fileFailure() throws java.io.IOException {\n"
                + "        db.execute(\"INSERT INTO r (v) VALUES ('c')\", null);\n"
                + "        throw new java.io.IOException(\"the file was not there\");\n"
                + "    }\n"
                + "    public int count() throws java.io.IOException {\n"
                + "        return ((Number) db.queryOne(\"SELECT COUNT(*) AS n FROM r\", null)"
                + ".get(\"n\")).intValue();\n"
                + "    }\n"
                + "}\n");
        s.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    private final Rows rows;\n"
                + "    public Api(Rows rows) { this.rows = rows; }\n"
                + "    @GetMapping(\"/bad\") public String bad() throws java.io.IOException {\n"
                + "        try { rows.badStatement(); } catch (DataAccessException e) { }\n"
                + "        return String.valueOf(rows.count());\n"
                + "    }\n"
                + "    @GetMapping(\"/file\") public String file() throws java.io.IOException {\n"
                + "        try { rows.fileFailure(); } catch (java.io.IOException e) { }\n"
                + "        return String.valueOf(rows.count());\n"
                + "    }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        int port = freePort();
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL()},
                getClass().getClassLoader());
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend backend = Backend.builder(Config.of(settings, "dev")).quiet()
                .requiresDataSource()
                .application((Backend.Application) loader
                        .loadClass("com.example.BackendWiring").newInstance())
                .start();
        try {
            // As in Spring, where a failed statement is an unchecked
            // DataAccessException: the insert before it is undone.
            assertEquals("a failed statement committed the work before it", "0",
                    http("GET", port, "/bad"));
            // And any other checked exception commits, as Spring's rule says.
            assertEquals("1", http("GET", port, "/file"));
        } finally {
            backend.stop();
        }
    }

    @Test
    public void duplicateToolAndManagedNamesAreBuildErrors() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.a.Stats", "package com.example.a;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@Component @ManagedResource public class Stats {\n"
                + "    @ManagedAttribute public int getN() { return 1; }\n"
                + "    @McpTool(description = \"x\") public String find() { return \"a\"; }\n"
                + "}\n");
        s.put("com.example.b.Stats", "package com.example.b;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@Component(\"otherStats\") @ManagedResource public class Stats {\n"
                + "    @ManagedAttribute public int getN() { return 2; }\n"
                + "    @McpTool(description = \"y\") public String find() { return \"b\"; }\n"
                + "}\n");
        ProcessorContext ctx = process(compile(s));
        String errors = String.valueOf(ctx.getErrors());
        assertTrue(errors, errors.contains("Two @McpTool methods are named \"find\""));
        assertTrue(errors, errors.contains("Two @ManagedResource beans are named \"Stats\""));
    }

    @Test
    public void jobsOfSameNamedClassesGetDistinctNames() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        for (String pkg : new String[] {"a", "b"}) {
            s.put("com.example." + pkg + ".Cleanup", "package com.example." + pkg + ";\n"
                    + "import com.codename1.backend.annotations.*;\n"
                    + "@Component" + ("b".equals(pkg) ? "(\"cleanupB\")" : "")
                    + " public class Cleanup {\n"
                    + "    @Scheduled(fixedDelay = 1000000, initialDelay = 1000000)\n"
                    + "    public void run() { }\n"
                    + "}\n");
        }
        File classes = compile(s);
        assertNoErrors(process(classes));
        int port = freePort();
        // The entry point goes in the first bean's package.
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL()},
                getClass().getClassLoader());
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend backend = Backend.builder(Config.of(settings, "dev")).quiet()
                .application((Backend.Application) loader
                        .loadClass("com.example.a.BackendWiring").newInstance())
                .start();
        try {
            String jobs = String.valueOf(backend.getApplication().getScheduler().describe());
            assertTrue(jobs, jobs.contains("com.example.a.Cleanup.run")
                    && jobs.contains("com.example.b.Cleanup.run"));
            assertTrue(backend.getApplication().getScheduler()
                    .trigger("com.example.b.Cleanup.run"));
        } finally {
            backend.stop();
        }
    }

    @Test
    public void beansAreDestroyedAfterWhatDependsOnThem() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Log", PKG + "public class Log { public static String text = \"\"; }\n");
        // Aaa is found first and Zzz depends on it: Zzz must go first.
        s.put("com.example.Aaa", PKG + "@Component @RequestScope public class Aaa {\n"
                + "    public void use() { }\n"
                + "    @PreDestroy public void done() { Log.text += \"aaa,\"; }\n"
                + "}\n");
        s.put("com.example.Zzz", PKG + "@Component @RequestScope public class Zzz {\n"
                + "    @Autowired private Aaa aaa;\n"
                + "    public void use() { aaa.use(); }\n"
                + "    @PreDestroy public void done() { Log.text += \"zzz,\"; }\n"
                + "}\n");
        s.put("com.example.Store", PKG + "@Component public class Store {\n"
                + "    @PreDestroy public void close() { Log.text += \"store,\"; }\n"
                + "}\n");
        s.put("com.example.Report", PKG + "@Component @Lazy public class Report {\n"
                + "    @Autowired private Store store;\n"
                + "    public String make() { return \"r\"; }\n"
                + "    @PreDestroy public void flush() { Log.text += \"report,\"; }\n"
                + "}\n");
        s.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    @Autowired private Zzz zzz;\n"
                + "    @Autowired private Report report;\n"
                + "    @GetMapping(\"/x\") public String x() { zzz.use(); report.make(); "
                + "return Log.text; }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        int port = freePort();
        Backend backend = start(classes, port, new Properties());
        try {
            http("GET", port, "/x");
            assertEquals("same-scope beans were destroyed before their dependents",
                    "zzz,aaa,", http("GET", port, "/x"));
        } finally {
            backend.stop();
        }
        Class<?> log = backend.getApplication().getClass().getClassLoader()
                .loadClass("com.example.Log");
        String text = String.valueOf(log.getField("text").get(null));
        assertTrue("a lazy bean was destroyed after the eager bean it uses: " + text,
                text.endsWith("report,store,"));
    }

    @Test
    public void managedGaugesLeaveWithTheirServer() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Stats", PKG + "@Component @ManagedResource(objectName = \"gaugestats\")\n"
                + "public class Stats {\n"
                + "    @ManagedAttribute public int getCount() { return 7; }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        int port = freePort();
        Backend backend = start(classes, port, new Properties());
        try {
            assertNotNull(com.codename1.backend.metrics.Metrics.get("gaugestats.count"));
        } finally {
            backend.stop();
        }
        assertTrue("a stopped server's managed gauge still reads its destroyed bean",
                com.codename1.backend.metrics.Metrics.get("gaugestats.count") == null);
    }

    @Test
    public void asyncOnARequestBeanOrAScheduledMethodIsABuildError() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Visit", PKG + "@Component @RequestScope public class Visit {\n"
                + "    @Async public void later() { }\n"
                + "}\n");
        s.put("com.example.Jobs", PKG + "@Component public class Jobs {\n"
                + "    @Scheduled(fixedRate = 1000) @Async public void tick() { }\n"
                + "}\n");
        s.put("com.example.Cart", PKG + "@Component @SessionScope public class Cart {\n"
                + "    @Async public void recount() { }\n"
                + "}\n");
        s.put("com.example.Stats", PKG + "@Component @ManagedResource(objectName = \"cache/main\")\n"
                + "public class Stats {\n"
                + "    @ManagedAttribute public int getN() { return 1; }\n"
                + "}\n");
        ProcessorContext ctx = process(compile(s));
        String errors = String.valueOf(ctx.getErrors());
        // Warnings, as Spring runs them: the task calls the instance itself.
        assertTrue(String.valueOf(warnings), String.valueOf(warnings)
                .contains("is on a @RequestScope bean"));
        assertTrue(String.valueOf(warnings), String.valueOf(warnings)
                .contains("is on a @SessionScope bean"));
        assertFalse(errors, errors.contains("is on a @"));
        assertTrue(errors, errors.contains("is a segment of the management URL"));
        assertTrue(errors, errors.contains("is also @Async"));
    }

    @Test
    public void aFieldDependencyIsInitializedBeforeItsUser() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        // Aaa is found first and uses Zzz, injected into a field, in its own
        // @PostConstruct: Zzz's must have run by then.
        s.put("com.example.Aaa", PKG + "@Component public class Aaa {\n"
                + "    @Autowired private Zzz zzz;\n"
                + "    public String seen = \"unset\";\n"
                + "    @PostConstruct public void init() { seen = zzz.state(); }\n"
                + "}\n");
        s.put("com.example.Zzz", PKG + "@Component public class Zzz {\n"
                + "    private String state = \"cold\";\n"
                + "    @PostConstruct public void warm() { state = \"warm\"; }\n"
                + "    public String state() { return state; }\n"
                + "}\n");
        s.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    @Autowired private Aaa aaa;\n"
                + "    @GetMapping(\"/x\") public String x() { return aaa.seen; }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        int port = freePort();
        Backend backend = start(classes, port, new Properties());
        try {
            assertEquals("a @PostConstruct ran before its field dependency's", "warm",
                    http("GET", port, "/x"));
        } finally {
            backend.stop();
        }
    }

    @Test
    public void aClassLevelAsyncReachesOnlyItsOwnMethods() throws Exception {
        // The subclass's @Async weaves only what the subclass declares, so the
        // inherited scheduled method stays synchronous and is valid.
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Base", PKG + "public class Base {\n"
                + "    @Scheduled(fixedRate = 1000) public void tick() { }\n"
                + "}\n");
        s.put("com.example.Worker", PKG + "@Component @Async public class Worker extends Base {\n"
                + "    public void send() { }\n"
                + "}\n");
        String errors = String.valueOf(process(compile(s)).getErrors());
        assertFalse(errors, errors.contains("is also @Async"));

        // The base class's own @Async does reach it, inherited or not.
        s = new LinkedHashMap<String, String>();
        s.put("com.example.Base", PKG + "@Async public class Base {\n"
                + "    @Scheduled(fixedRate = 1000) public void tick() { }\n"
                + "}\n");
        s.put("com.example.Worker", PKG + "@Component public class Worker extends Base {\n"
                + "}\n");
        errors = String.valueOf(process(compile(s)).getErrors());
        assertTrue(errors, errors.contains("is also @Async"));
    }

    @Test
    public void anAsyncRouteReturningAFutureIsABuildError() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Api", PKG + "@RestController public class Api {\n"
                + "    @GetMapping(\"/x\") @Async\n"
                + "    public Future<String> x() { return AsyncResult.of(\"x\"); }\n"
                + "}\n");
        String errors = String.valueOf(process(compile(s)).getErrors());
        assertTrue(errors, errors.contains("com.example.Api.x returns a "
                + "java.util.concurrent.Future"));
        assertTrue(errors, errors.contains("the client would get the pending task"));
    }

    @Test
    public void asyncToolsAndDuplicateParametersAreBuildErrors() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Tools", PKG + "@Component public class Tools {\n"
                + "    @McpTool(description = \"a\") @Async\n"
                + "    public Future slow() { return AsyncResult.of(\"x\"); }\n"
                + "    @McpTool(description = \"b\")\n"
                + "    public String pair(@McpParam(\"id\") String a, @McpParam(\"id\") String b) {\n"
                + "        return a + b;\n"
                + "    }\n"
                + "}\n");
        ProcessorContext ctx = process(compile(s));
        String errors = String.valueOf(ctx.getErrors());
        assertTrue(errors, errors.contains("is also @Async"));
        assertTrue(errors, errors.contains("named \"id\" like another"));
    }

    @Test
    public void anEscapedWebSocketPathIsABuildError() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Chat", PKG + "@WebSocketMapping(\"/ch%61t\")\n"
                + "public class Chat implements WebSocket {\n"
                + "    public void onOpen(WebSocketSession s) { }\n"
                + "    public void onText(WebSocketSession s, String m) { }\n"
                + "    public void onBinary(WebSocketSession s, byte[] m, int o, int l) { }\n"
                + "}\n");
        ProcessorContext ctx = process(compile(s));
        assertTrue(String.valueOf(ctx.getErrors()), String.valueOf(ctx.getErrors())
                .contains("must not contain a percent escape"));
    }

    @Test
    public void anInheritedScheduledMethodRuns() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.BaseJob", PKG + "public abstract class BaseJob {\n"
                + "    public static int runs;\n"
                + "    @Scheduled(fixedRate = 20) public void tick() { runs++; }\n"
                + "}\n");
        s.put("com.example.Cleanup", PKG + "@Component public class Cleanup extends BaseJob { }\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        int port = freePort();
        Backend backend = start(classes, port, new Properties());
        Class<?> base = backend.getApplication().getClass().getClassLoader()
                .loadClass("com.example.BaseJob");
        try {
            long deadline = System.currentTimeMillis() + 5000;
            while (base.getField("runs").getInt(null) < 2
                    && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            assertTrue("an inherited @Scheduled method never ran",
                    base.getField("runs").getInt(null) >= 2);
        } finally {
            backend.stop();
        }
    }

    @Test
    public void aFactoryBuiltBeanKeepsItsScheduledJobs() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Ticker", PKG + "public class Ticker {\n"
                + "    public static volatile int runs;\n"
                + "    @Scheduled(fixedRate = 20) public void tick() { runs++; }\n"
                + "}\n");
        s.put("com.example.Setup", PKG + "@Configuration public class Setup {\n"
                + "    @Bean public Ticker ticker() { return new Ticker(); }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        Backend backend = start(classes, freePort(), new Properties());
        Class<?> ticker = backend.getApplication().getClass().getClassLoader()
                .loadClass("com.example.Ticker");
        try {
            long deadline = System.currentTimeMillis() + 5000;
            while (ticker.getField("runs").getInt(null) < 2
                    && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            assertTrue("a @Bean-built class's @Scheduled method never ran",
                    ticker.getField("runs").getInt(null) >= 2);
        } finally {
            backend.stop();
        }
    }

    @Test
    public void scopedBeansReachedThroughASingletonAreRefusedOffRequest() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Visit", PKG + "@Component @RequestScope public class Visit { }\n");
        s.put("com.example.Helper", PKG + "@Component public class Helper {\n"
                + "    @Autowired private Visit visit;\n"
                + "}\n");
        s.put("com.example.Chat", PKG + "@WebSocketMapping(\"/chat\")\n"
                + "public class Chat implements WebSocket {\n"
                + "    @Autowired private Helper helper;\n"
                + "    public void onOpen(WebSocketSession s) { }\n"
                + "    public void onText(WebSocketSession s, String m) { }\n"
                + "    public void onBinary(WebSocketSession s, byte[] m, int o, int l) { }\n"
                + "}\n");
        s.put("com.example.Nightly", PKG + "@Component public class Nightly {\n"
                + "    public Nightly(Helper helper) { }\n"
                + "    @Scheduled(fixedRate = 60000) public void run() { }\n"
                + "}\n");
        // Warned, not refused: Spring starts this, and the stand-in throws only
        // when it is really used with no request current.
        assertNoErrors(process(compile(s)));
        String warned = String.valueOf(warnings);
        assertTrue(warned, warned.contains("Websocket endpoint chat (com.example.Chat) reaches "
                + "visit (com.example.Visit) through helper (com.example.Helper)"));
        assertTrue(warned, warned.contains("A websocket callback runs outside any HTTP request"));
        assertTrue(warned, warned.contains("nightly (com.example.Nightly) reaches visit"));
        assertTrue(warned, warned.contains("A scheduled job runs outside any HTTP request"));
        // The singleton itself may inject it: it is used from requests.
        s.remove("com.example.Chat");
        s.remove("com.example.Nightly");
        assertNoErrors(process(compile(s)));
        assertFalse(String.valueOf(warnings), String.valueOf(warnings).contains("reaches"));
    }

    @Test
    public void anAsyncBeanReachingAScopedBeanIsWarned() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Visit", PKG + "@Component @RequestScope public class Visit { }\n");
        s.put("com.example.Helper", PKG + "@Component public class Helper {\n"
                + "    @Autowired private Visit visit;\n"
                + "}\n");
        s.put("com.example.Mailer", PKG + "@Component public class Mailer {\n"
                + "    @Autowired private Helper helper;\n"
                + "    @Async public void send() { }\n"
                + "}\n");
        // Warned, as Spring starts it and fails only on a use off the request.
        assertNoErrors(process(compile(s)));
        String warned = String.valueOf(warnings);
        assertTrue(warned, warned.contains("Bean with @Async methods mailer (com.example.Mailer) "
                + "reaches visit (com.example.Visit) through helper (com.example.Helper)"));
    }

    @Test
    public void oneExecutorNameWithTwoThreadKindsIsRefused() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Reports", PKG + "@Component public class Reports {\n"
                + "    @Async(value = \"reports\", thread = ThreadKind.PLATFORM) public void db() { }\n"
                + "    @Async(value = \"reports\", thread = ThreadKind.VIRTUAL) public void cpu() { }\n"
                + "}\n");
        String errors = String.valueOf(process(compile(s)).getErrors());
        assertTrue(errors, errors.contains("Executor \"reports\" is asked for"));
        s.put("com.example.Reports", PKG + "@Component public class Reports {\n"
                + "    @Async(value = \"reports\", thread = ThreadKind.PLATFORM) public void db() { }\n"
                + "    @Async(value = \"reports\", thread = ThreadKind.AUTO) public void any() { }\n"
                + "}\n");
        assertNoErrors(process(compile(s)));
    }

    @Test
    public void aScopedBeanInheritingAnAsyncMethodIsWarned() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Worker", PKG + "public abstract class Worker {\n"
                + "    @Async public void later() { }\n"
                + "}\n");
        s.put("com.example.Visit", PKG + "@Component @RequestScope public class Visit "
                + "extends Worker { }\n");
        assertNoErrors(process(compile(s)));
        String warned = String.valueOf(warnings);
        assertTrue(warned, warned.contains("@Async method com.example.Visit.later is on a "
                + "@RequestScope bean"));
    }

    @Test
    public void aScopedConfigurationCannotBuildASingleton() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.PerRequest", PKG + "@Configuration @RequestScope public class PerRequest {\n"
                + "    @Bean public StringBuilder buffer() { return new StringBuilder(); }\n"
                + "}\n");
        String errors = String.valueOf(process(compile(s)).getErrors());
        assertTrue(errors, errors.contains("@Bean method com.example.PerRequest.buffer builds a "
                + "singleton bean"));
        s.put("com.example.PerRequest", PKG + "@Configuration @RequestScope public class PerRequest {\n"
                + "    @Bean public static StringBuilder buffer() { return new StringBuilder(); }\n"
                + "}\n");
        assertNoErrors(process(compile(s)));
    }

    @Test
    public void aConditionalPrimaryFallsBackToTheActiveAlternative() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Greeter", PKG + "public interface Greeter { String greet(); }\n");
        s.put("com.example.ProdGreeter", PKG + "@Component @Primary @Profile(\"prod\") "
                + "public class ProdGreeter implements Greeter {\n"
                + "    public String greet() { return \"prod\"; }\n"
                + "}\n");
        s.put("com.example.DevGreeter", PKG + "@Component @Profile(\"dev\") "
                + "public class DevGreeter implements Greeter {\n"
                + "    public String greet() { return \"dev\"; }\n"
                + "}\n");
        s.put("com.example.Api", PKG + "@RestController public class Api {\n"
                + "    private final Greeter greeter;\n"
                + "    public Api(Greeter greeter) { this.greeter = greeter; }\n"
                + "    @GetMapping(\"/who\") public String who() { return greeter.greet(); }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        int port = freePort();
        Backend backend = start(classes, port, new Properties());   // the dev profile
        try {
            String answer = http("GET", port, "/who");
            assertTrue(answer, answer.contains("dev"));
        } finally {
            backend.stop();
        }
    }

    @Test
    public void aPrototypeControllerIsBuiltOnce() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Api", PKG + "@RestController @Scope(\"prototype\") public class Api {\n"
                + "    public static final AtomicInteger BUILT = new AtomicInteger();\n"
                + "    public Api() { BUILT.incrementAndGet(); }\n"
                + "    @GetMapping(\"/x\") public String x() { return \"x\"; }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        Backend backend = start(classes, freePort(), new Properties());
        try {
            Class<?> api = backend.getApplication().getClass().getClassLoader()
                    .loadClass("com.example.Api");
            assertEquals("the router's controller was built twice, one discarded", 1,
                    ((java.util.concurrent.atomic.AtomicInteger) api.getField("BUILT").get(null))
                            .get());
        } finally {
            backend.stop();
        }
    }

    @Test
    public void aFactoryReturningNullStopsTheStart() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Setup", PKG + "@Configuration public class Setup {\n"
                + "    @Bean public StringBuilder buffer() { return null; }\n"
                + "}\n");
        s.put("com.example.Api", PKG + "@RestController public class Api {\n"
                + "    public Api(StringBuilder buffer) { }\n"
                + "    @GetMapping(\"/x\") public String x() { return \"x\"; }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        try {
            start(classes, freePort(), new Properties()).stop();
            fail("a @Bean method that returned null let the server start");
        } catch (Exception expected) {
            String all = String.valueOf(expected);
            for (Throwable t = expected.getCause(); t != null; t = t.getCause()) {
                all += " / " + t;
            }
            assertTrue(all, all.contains("@Bean com.example.Setup.buffer returned null"));
        }
    }

    @Test
    public void sessionBeansAndSocketsCannotHoldRequestState() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Cart", PKG + "@Component @SessionScope public class Cart {\n"
                + "    @Autowired private HttpSession session;\n"
                + "}\n");
        s.put("com.example.Visit", PKG + "@Component @RequestScope public class Visit { }\n");
        s.put("com.example.Chat", PKG + "@WebSocketMapping(\"/chat\")\n"
                + "public class Chat implements WebSocket {\n"
                + "    @Autowired private Visit visit;\n"
                + "    public void onOpen(WebSocketSession s) { }\n"
                + "    public void onText(WebSocketSession s, String m) { }\n"
                + "    public void onBinary(WebSocketSession s, byte[] m, int o, int l) { }\n"
                + "}\n");
        ProcessorContext ctx = process(compile(s));
        String errors = String.valueOf(ctx.getErrors());
        assertTrue(errors, errors.contains("a session bean outlives the request"));
        assertTrue(String.valueOf(warnings), String.valueOf(warnings)
                .contains("A websocket callback runs outside any HTTP request"));
    }

    @Test
    public void everyScopeAndBindingWorksAtRunTime() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Handler", PKG + "public interface Handler { String name(); }\n");
        s.put("com.example.AHandler", PKG + "@Component public class AHandler implements Handler "
                + "{ public String name() { return \"a\"; } }\n");
        s.put("com.example.BHandler", PKG + "@Component(\"special\") public class BHandler "
                + "implements Handler { public String name() { return \"b\"; } }\n");
        s.put("com.example.Ticket", PKG + "@Component @Scope(\"prototype\") public class Ticket {\n"
                + "    private static int made;\n"
                + "    private final int number = ++made;\n"
                + "    public int number() { return number; }\n"
                + "}\n");
        // The expensive part is @PostConstruct, which is the contract: the
        // generated stand-in extends the class and so runs its constructor once
        // at start-up, but only the real instance -- built on first use -- is
        // initialized.
        s.put("com.example.Expensive", PKG + "@Component @Lazy public class Expensive {\n"
                + "    public static int built;\n"
                + "    @PostConstruct void warm() { built++; }\n"
                + "    public String hello() { return \"lazy\"; }\n"
                + "}\n");
        s.put("com.example.Cart", PKG + "@Component @SessionScope public class Cart {\n"
                + "    private int items;\n"
                + "    public int add() { return ++items; }\n"
                + "}\n");
        s.put("com.example.Limits", PKG + "@Component @ConfigurationProperties(\"limits\")\n"
                + "public class Limits {\n"
                + "    private int maxSize = 5;\n"
                + "    private String label = \"none\";\n"
                + "    public void setMaxSize(int v) { maxSize = v; }\n"
                + "    public void setLabel(String v) { label = v; }\n"
                + "    public String describe() { return label + \"/\" + maxSize; }\n"
                + "}\n");
        s.put("com.example.Clients", PKG + "@Configuration public class Clients {\n"
                + "    @Bean public StringBuilder greeting(@Value(\"${greeting:hi}\") String g) {\n"
                + "        return new StringBuilder(g);\n"
                + "    }\n"
                + "}\n");
        s.put("com.example.Beta", PKG + "@Component @ConditionalOnProperty(\"feature.beta\")\n"
                + "public class Beta { }\n");
        s.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    @Autowired private List<Handler> handlers;\n"
                + "    @Autowired @Qualifier(\"special\") private Handler special;\n"
                + "    @Autowired private Ticket first;\n"
                + "    @Autowired private Ticket second;\n"
                + "    @Autowired private Expensive expensive;\n"
                + "    @Autowired private Cart cart;\n"
                + "    @Autowired private Limits limits;\n"
                + "    @Autowired private StringBuilder greeting;\n"
                + "    @Autowired(required = false) private Beta beta;\n"
                + "    @GetMapping(\"/handlers\") public String handlers() {\n"
                + "        String out = \"\";\n"
                + "        for (Handler h : handlers) { out += h.name(); }\n"
                + "        return out + \"|\" + special.name();\n"
                + "    }\n"
                + "    @GetMapping(\"/tickets\") public String tickets() {\n"
                + "        return first.number() + \",\" + second.number();\n"
                + "    }\n"
                + "    @GetMapping(\"/lazy\") public String lazy() {\n"
                + "        int before = Expensive.built;\n"
                + "        return before + \":\" + expensive.hello() + \":\" + Expensive.built;\n"
                + "    }\n"
                + "    @GetMapping(\"/cart\") public String cart() { return String.valueOf(cart.add()); }\n"
                + "    @GetMapping(\"/config\") public String config() {\n"
                + "        return limits.describe() + \"|\" + greeting + \"|\" + (beta != null);\n"
                + "    }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty("limits.max-size", "9");
        settings.setProperty("limits.label", "gold");
        settings.setProperty("greeting", "hey");
        Backend backend = start(classes, port, settings);
        try {
            assertEquals("ab|b", http("GET", port, "/handlers"));
            String[] tickets = http("GET", port, "/tickets").split(",");
            assertNotEquals("a prototype was shared between two injection points",
                    tickets[0], tickets[1]);
            assertEquals("a lazy bean was built before its first use", "0:lazy:1",
                    http("GET", port, "/lazy"));
            assertEquals("gold/9|hey|false", http("GET", port, "/config"));
            // One cart per session: the same cookie keeps adding to one cart.
            HttpURLConnection first = (HttpURLConnection) new URL("http://127.0.0.1:" + port
                    + "/cart").openConnection();
            assertEquals("1", read(first));
            String cookie = first.getHeaderField("Set-Cookie");
            assertTrue("a session-scoped bean did not start a session", cookie != null);
            String pair = cookie.substring(0, cookie.indexOf(';'));
            HttpURLConnection again = (HttpURLConnection) new URL("http://127.0.0.1:" + port
                    + "/cart").openConnection();
            again.setRequestProperty("Cookie", pair);
            assertEquals("2", read(again));
            assertEquals("a new client shared another's session bean", "1",
                    http("GET", port, "/cart"));
        } finally {
            backend.stop();
        }
    }

    @Test
    public void theCronCompilerAgreesWithTheRuntimeParser() throws Exception {
        String[] expressions = {"0 0 * * * *", "*/15 * * * * *", "0 30 9-17 * * MON-FRI",
                "0 0 0 1 JAN,JUL ?", "5,10,15 1/5 0-23/2 L * *", "@daily", "@hourly",
                "0 0 12 ? * SUN", "0 0 0 * * 7"};
        for (String e : expressions) {
            CronCompiler built = CronCompiler.compile(e);
            com.codename1.backend.CronSchedule runtime =
                    com.codename1.backend.CronSchedule.parse(e, "UTC");
            com.codename1.backend.CronSchedule fromMasks = new com.codename1.backend.CronSchedule(
                    built.seconds, built.minutes, built.hours, built.daysOfMonth, built.months,
                    built.daysOfWeek, built.lastDayOfMonth, "UTC", e);
            long t = 1767225600000L; // 2026-01-01T00:00:00Z
            for (int i = 0; i < 20; i++) {
                long a = runtime.next(t);
                long b = fromMasks.next(t);
                assertEquals("the build and the runtime disagree about " + e, a, b);
                t = a;
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private Backend start(File classes, int port, Properties settings) throws Exception {
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL()},
                getClass().getClassLoader());
        Backend.Application app = (Backend.Application) loader
                .loadClass("com.example.BackendWiring").newInstance();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        return Backend.builder(Config.of(settings, "dev")).quiet().application(app).start();
    }

    private File compile(Map<String, String> sources) throws Exception {
        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(sources, classes, backendClasspath());
        return classes;
    }

    /// What the last process() logged as warnings.
    private final List<String> warnings = new ArrayList<String>();

    private ProcessorContext process(File classes) throws Exception {
        return process(classes, new RestControllerAnnotationProcessor());
    }

    private ProcessorContext process(File classes, RestControllerAnnotationProcessor proc,
                                     File... extraClasspath) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classes);
        List<String> cp = new ArrayList<String>();
        for (File f : backendClasspath()) {
            cp.add(f.getAbsolutePath());
        }
        for (File f : extraClasspath) {
            cp.add(f.getAbsolutePath());
        }
        warnings.clear();
        ProcessorContext ctx = new ProcessorContext(classes, tmp.newFolder(), index,
                new SystemStreamLog() {
                    @Override
                    public void warn(CharSequence content) {
                        warnings.add(String.valueOf(content));
                        super.warn(content);
                    }
                }, tmp.newFolder(), new Properties(), null,
                Collections.<String>emptyList(), "UTF-8", cp);
        BackendBeanAnnotationProcessor beans = new BackendBeanAnnotationProcessor();
        beans.start(ctx);
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (!cls.getClassAnnotations().isEmpty()) {
                proc.processClass(cls, ctx);
            }
        }
        beans.finish(ctx);
        proc.finish(ctx);
        return ctx;
    }

    private static void assertNoErrors(ProcessorContext ctx) {
        if (ctx.hasErrors()) {
            fail("the processors reported errors: " + ctx.getErrors());
        }
    }

    private static List<File> backendClasspath() throws Exception {
        URL url = HttpServer.class.getProtectionDomain().getCodeSource().getLocation();
        return Arrays.asList(new File(url.toURI()));
    }

    private static int freePort() throws Exception {
        ServerSocket s = new ServerSocket(0);
        try {
            return s.getLocalPort();
        } finally {
            s.close();
        }
    }

    private static String http(String method, int port, String path) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + path)
                .openConnection();
        c.setRequestMethod(method);
        if ("POST".equals(method)) {
            c.setDoOutput(true);
            c.getOutputStream().close();
        }
        return read(c);
    }

    private static String post(int port, String path, String json) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + path)
                .openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json");
        OutputStream out = c.getOutputStream();
        out.write(json.getBytes("UTF-8"));
        out.close();
        return read(c);
    }

    private static String read(HttpURLConnection c) throws Exception {
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
        String body = new String(buffer.toByteArray(), "UTF-8");
        if (status >= 400) {
            return "HTTP " + status + ": " + body;
        }
        return body;
    }
}
