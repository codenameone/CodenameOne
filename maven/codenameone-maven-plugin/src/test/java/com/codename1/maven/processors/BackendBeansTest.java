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
            String metrics = http("GET", port, "/manage/prometheus");
            assertTrue(metrics, metrics.contains("http_server_request_duration_bucket"));
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

    private ProcessorContext process(File classes) throws Exception {
        return process(classes, new RestControllerAnnotationProcessor());
    }

    private ProcessorContext process(File classes, RestControllerAnnotationProcessor proc)
            throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classes);
        List<String> cp = new ArrayList<String>();
        for (File f : backendClasspath()) {
            cp.add(f.getAbsolutePath());
        }
        ProcessorContext ctx = new ProcessorContext(classes, tmp.newFolder(), index,
                new SystemStreamLog(), tmp.newFolder(), new Properties(), null,
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
