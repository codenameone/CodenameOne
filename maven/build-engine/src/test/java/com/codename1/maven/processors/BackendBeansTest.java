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
import com.codename1.impl.backend.BackendAccess;
import com.codename1.impl.backend.BackendApplication;
import com.codename1.backend.Config;
import com.codename1.backend.HttpServer;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.ProcessorContext;
import com.codename1.build.SystemStreamLog;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
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
import static org.junit.Assert.assertNull;
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
                + "@Component\n"
                + "public class PoliteGreeter implements Greeter {\n"
                + "    @Value(\"${greeting.prefix:Hello}\") private String prefix;\n"
                + "    private int initialized;\n"
                + "    @PostConstruct void init() { initialized++; }\n"
                + "    public String greet(String name) {\n"
                + "        return prefix + \", \" + name + \" (\" + initialized + \")\";\n"
                + "    }\n"
                + "}\n");
        s.put("com.example.Notes", PKG
                + "@Component\n"
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
                + "@Component\n"
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
                + "@Component @Scope(\"request\")\n"
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
        BackendApplication app = (BackendApplication) loader
                .loadClass("com.example.BackendWiring").newInstance();
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        settings.setProperty("greeting.prefix", "Hi");
        // What the generated main does, with the development tools a JVM build has.
        Backend backend = withApplication(withManagement(withMcp(Backend.builder(Config.of(settings, "dev"))
                .quiet()
                .requiresDataSource(), new com.codename1.impl.backend.mcp.DevTools())), app)
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
        assertTrue(bootstrap, bootstrap.contains("cn1Access.application(cn1Builder, new com.example.BackendWiring())"));
        assertTrue("a bean takes a DataSource, so the entry point must ask for a database:\n"
                + bootstrap, bootstrap.contains(".requiresDataSource()"));
        String wiring = proc.generateWiring("com.example");
        assertTrue(wiring, wiring.contains("new com.example.Api("));
        assertTrue("the private field is injected through the woven setter:\n" + wiring,
                wiring.contains(".cn1$inject$greeter("));
    }

    private static Map<String, String> dtoSample() {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Priority", PKG + "public enum Priority { LOW, HIGH }\n");
        s.put("com.example.Tag", PKG + "public class Tag {\n"
                + "    public String name;\n"
                + "    public Tag() { }\n"
                + "    public Tag(String name) { this.name = name; }\n"
                + "}\n");
        s.put("com.example.Note", PKG + "public class Note {\n"
                + "    public long id;\n"
                + "    public String title;\n"
                + "    public Date due;\n"
                + "    public Priority priority;\n"
                + "    public List<Tag> tags;\n"
                + "    @com.codename1.annotations.JsonProperty(\"is_done\") public boolean done;\n"
                + "    @com.codename1.annotations.JsonIgnore public String secret = \"s3cret\";\n"
                + "    public transient String cache = \"cached\";\n"
                + "    private int rank;\n"
                + "    public int getRank() { return rank; }\n"
                + "    public void setRank(int rank) { this.rank = rank; }\n"
                + "    public byte[] blob;\n"
                + "    public Map<String, Integer> counts;\n"
                + "    public Note() { }\n"
                + "}\n");
        s.put("com.example.Special", PKG + "public class Special extends Note {\n"
                + "    public String extra = \"more\";\n"
                + "}\n");
        s.put("com.example.Node", PKG + "public class Node {\n"
                + "    public Node next;\n"
                + "}\n");
        s.put("com.example.Box", PKG + "public class Box {\n"
                + "    public Object content;\n"
                + "    public Map extras = new LinkedHashMap();\n"
                + "}\n");
        s.put("com.example.Loner", PKG + "public class Loner {\n"
                + "    public String x = \"y\";\n"
                + "}\n");
        s.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    static Note note() {\n"
                + "        Note n = new Note();\n"
                + "        n.id = 7; n.title = \"hi\"; n.due = new Date(86400000L);\n"
                + "        n.priority = Priority.HIGH; n.done = true; n.setRank(3);\n"
                + "        n.tags = new ArrayList<Tag>(); n.tags.add(new Tag(\"a\"));\n"
                + "        n.blob = new byte[] {1, 2, 3};\n"
                + "        n.counts = new LinkedHashMap<String, Integer>(); n.counts.put(\"x\", 1);\n"
                + "        return n;\n"
                + "    }\n"
                + "    @GetMapping(\"/note\") public Note one() { return note(); }\n"
                + "    @GetMapping(\"/notes\") public List<Note> all() {\n"
                + "        List<Note> out = new ArrayList<Note>();\n"
                + "        out.add(note()); out.add(new Special());\n"
                + "        return out;\n"
                + "    }\n"
                + "    @PostMapping(\"/echo\") public Note echo(@RequestBody Note n) { return n; }\n"
                + "    @PostMapping(\"/count\") public String count(@RequestBody List<Note> ns) {\n"
                + "        return ns.size() + \":\" + ns.get(1).tags.get(0).name;\n"
                + "    }\n"
                + "    @GetMapping(\"/loop\") public Node loop() {\n"
                + "        Node a = new Node(); a.next = a; return a;\n"
                + "    }\n"
                + "    @GetMapping(\"/box\") public Box box() {\n"
                + "        Box b = new Box(); b.content = new Tag(\"inside\");\n"
                + "        b.extras.put(\"when\", new Date(5L));\n"
                + "        b.extras.put(\"level\", Priority.LOW);\n"
                + "        return b;\n"
                + "    }\n"
                + "    @GetMapping(\"/opaque\") public Box opaque() {\n"
                + "        Box b = new Box(); b.content = new Loner(); return b;\n"
                + "    }\n"
                + "}\n");
        return s;
    }

    @Test
    public void aControllerReturnsAndAcceptsItsOwnClassesAsJson() throws Exception {
        File classes = compile(dtoSample());
        assertNoErrors(process(classes));
        int port = freePort();
        Backend backend = start(classes, port, new Properties());
        try {
            String one = http("GET", port, "/note");
            for (String part : new String[] {"\"id\":7", "\"title\":\"hi\"", "\"due\":86400000",
                    "\"priority\":\"HIGH\"", "\"tags\":[{\"name\":\"a\"}]", "\"is_done\":true",
                    "\"rank\":3", "\"blob\":\"AQID\"", "\"counts\":{\"x\":1}"}) {
                assertTrue(part + " missing from " + one, one.contains(part));
            }
            assertFalse("@JsonIgnore was written: " + one, one.contains("s3cret"));
            assertFalse("a transient field was written: " + one, one.contains("cached"));

            String all = http("GET", port, "/notes");
            assertTrue("a subclass was written as its declared type: " + all,
                    all.contains("\"extra\":\"more\""));

            String echoed = post(port, "/echo", "{\"id\":9,\"title\":\"t\",\"unknown\":[1],"
                    + "\"due\":\"1970-01-02T00:00:00Z\",\"priority\":\"LOW\",\"is_done\":true,"
                    + "\"rank\":5,\"tags\":[{\"name\":\"b\"}],\"blob\":\"AQID\","
                    + "\"counts\":{\"y\":2}}");
            for (String part : new String[] {"\"id\":9", "\"due\":86400000", "\"priority\":\"LOW\"",
                    "\"is_done\":true", "\"rank\":5", "\"tags\":[{\"name\":\"b\"}]",
                    "\"blob\":\"AQID\"", "\"counts\":{\"y\":2}"}) {
                assertTrue(part + " missing from " + echoed, echoed.contains(part));
            }
            assertTrue(post(port, "/count", "[{},{\"tags\":[{\"name\":\"z\"}]}]").equals("2:z"));

            assertEquals("HTTP 400: $.id: expected a whole number from -9223372036854775808 to "
                    + "9223372036854775807, got a string", post(port, "/echo", "{\"id\":\"x\"}"));
            assertEquals("HTTP 400: $[1].tags[0].name: expected a string, got the number 5",
                    post(port, "/count", "[{},{\"tags\":[{\"name\":5}]}]"));
            assertEquals("HTTP 400: $.priority: expected one of LOW, HIGH, got a string",
                    post(port, "/echo", "{\"priority\":\"MEDIUM\"}"));
            assertEquals("HTTP 400: The request body is not valid JSON",
                    post(port, "/echo", "{nope"));
            assertTrue(http("GET", port, "/loop").startsWith("HTTP 500"));
            // An Object field is written by what it holds: a class this build
            // writes goes through its codec, a Date as millis, an enum by name.
            assertEquals("{\"content\":{\"name\":\"inside\"},\"extras\":{\"when\":5,"
                    + "\"level\":\"LOW\"}}", http("GET", port, "/box"));
            // A class with no codec is refused, never written as its toString().
            assertTrue(http("GET", port, "/opaque").startsWith("HTTP 500"));
        } finally {
            backend.stop();
        }
    }

    @Test
    public void theGuidesOrderExampleWorksAsDocumented() throws Exception {
        // The developer guide's own files, not a copy: what the chapter shows is
        // what builds and answers here.
        File dir = new File("../../docs/demos/backend/src/main/java/com/codenameone/"
                + "developerguide/backend/orders");
        assertTrue(dir.getAbsolutePath(), dir.isDirectory());
        Map<String, String> s = new LinkedHashMap<String, String>();
        for (String name : new String[] {"Order", "OrderLine", "OrdersApi"}) {
            s.put("com.codenameone.developerguide.backend.orders." + name, new String(
                    java.nio.file.Files.readAllBytes(new File(dir, name + ".java").toPath()),
                    "UTF-8"));
        }
        File classes = compile(s);
        assertNoErrors(process(classes));
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL()},
                getClass().getClassLoader());
        BackendApplication app = (BackendApplication) loader.loadClass(
                "com.codenameone.developerguide.backend.orders.BackendWiring").newInstance();
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend backend = withApplication(Backend.builder(Config.of(settings, "dev")).quiet(), app)
                .start();
        try {
            String placed = post(port, "/orders",
                    "{\"customer\":\"Ada\",\"lines\":[{\"sku\":\"A-1\",\"quantity\":2}]}");
            assertTrue(placed, placed.matches("\\{\"id\":1,\"customer\":\"Ada\","
                    + "\"placed_at\":[0-9]+,\"lines\":\\[\\{\"sku\":\"A-1\",\"quantity\":2\\}\\]\\}"));
            assertEquals(placed, http("GET", port, "/orders/1"));
            assertTrue(http("GET", port, "/orders/2").startsWith("HTTP 404"));
        } finally {
            backend.stop();
        }
    }

    @Test
    public void anMcpToolReadsAndWritesItsTypesThroughTheCodecs() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Tag", PKG + "public class Tag { public String name; }\n");
        s.put("com.example.Shop", PKG
                + "@Component public class Shop {\n"
                + "    @McpTool(description = \"Totals the ids under a tag\")\n"
                + "    public Tag total(@McpParam(\"ids\") List<Integer> ids,\n"
                + "                     @McpParam(\"tag\") Tag tag) {\n"
                + "        int sum = 0;\n"
                + "        for (Integer id : ids) { sum += id.intValue(); }\n"
                + "        Tag out = new Tag(); out.name = tag.name + sum; return out;\n"
                + "    }\n"
                + "}\n");
        s.put("com.example.Api", PKG + "@RestController public class Api {\n"
                + "    @GetMapping(\"/x\") public String x() { return \"x\"; }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL()},
                getClass().getClassLoader());
        BackendApplication app = (BackendApplication) loader
                .loadClass("com.example.BackendWiring").newInstance();
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend backend = withApplication(withMcp(Backend.builder(Config.of(settings, "dev")).quiet(), null), app).start();
        try {
            String tools = post(port, "/mcp", "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":"
                    + "\"tools/list\"}");
            assertTrue(tools, tools.contains("\"ids\":{\"type\":\"array\""));
            assertTrue(tools, tools.contains("\"tag\":{\"type\":\"object\""));
            // List<Integer> holds Integers, not the parser's Longs, and the Tag
            // that comes back is an object, not its toString().
            String call = post(port, "/mcp", "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":"
                    + "\"tools/call\",\"params\":{\"name\":\"total\",\"arguments\":"
                    + "{\"ids\":[1,2],\"tag\":{\"name\":\"x\"}}}}");
            assertTrue(call, call.contains("\"text\":\"{\\\"name\\\":\\\"x3\\\"}\""));
            assertTrue(call, call.contains("\"isError\":false"));
            String wrong = post(port, "/mcp", "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":"
                    + "\"tools/call\",\"params\":{\"name\":\"total\",\"arguments\":"
                    + "{\"ids\":[\"a\"],\"tag\":{}}}}");
            assertTrue(wrong, wrong.contains("$.ids[0]: expected a whole number"));
            assertTrue(wrong, wrong.contains("\"isError\":true"));
            String missing = post(port, "/mcp", "{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":"
                    + "\"tools/call\",\"params\":{\"name\":\"total\",\"arguments\":"
                    + "{\"ids\":[]}}}");
            assertTrue(missing, missing.contains("Missing required argument \\\"tag\\\""));
        } finally {
            backend.stop();
        }
    }

    @Test
    public void anMcpToolTheCodecsCannotServeIsABuildError() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Shop", PKG
                + "@Component public class Shop {\n"
                + "    @McpTool(description = \"a\")\n"
                + "    public String a(@McpParam(\"ids\") int[] ids) { return \"a\"; }\n"
                + "    @McpTool(description = \"b\")\n"
                + "    public StringBuilder b() { return null; }\n"
                + "}\n");
        String errors = String.valueOf(process(compile(s)).getErrors());
        assertTrue(errors, errors.contains("Parameter 1 of @McpTool"));
        assertTrue(errors, errors.contains("an array, which has no JSON form here"));
        assertTrue(errors, errors.contains("returns java.lang.StringBuilder, which cannot be "
                + "written as JSON"));
    }

    @Test
    public void twoClassesFoldingToOneCodecNameFailTheBuild() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Outer_Inner", PKG + "public class Outer_Inner { public int a; }\n");
        s.put("com.example.Outer", PKG + "public class Outer {\n"
                + "    public static class Inner { public int b; }\n"
                + "}\n");
        s.put("com.example.Api", PKG + "@RestController public class Api {\n"
                + "    @GetMapping(\"/a\") public Outer_Inner a() { return null; }\n"
                + "    @GetMapping(\"/b\") public Outer.Inner b() { return null; }\n"
                + "}\n");
        String errors = String.valueOf(process(compile(s)).getErrors());
        assertTrue(errors, errors.contains("would both get the JSON codec "
                + "com.example.Outer_InnerCn1Json. Rename one of them."));
    }

    @Test
    public void aProjectClassWithAGeneratedNameFailsTheBuild() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Foo", PKG + "@Component public class Foo {\n"
                + "    @Timed(\"foo.work\") public void work() { }\n"
                + "}\n");
        // Exactly the support class the build would generate for Foo's aspects.
        s.put("com.example.FooCn1Aspects", PKG + "public class FooCn1Aspects { }\n");
        s.put("com.example.Api", PKG + "@RestController public class Api {\n"
                + "    @GetMapping(\"/x\") public String x() { return \"x\"; }\n"
                + "}\n");
        String errors = String.valueOf(process(compile(s)).getErrors());
        assertTrue(errors, errors.contains("com.example.FooCn1Aspects already exists, and the "
                + "class the build generates under that name would replace it"));
    }

    @Test
    public void aSubclassReachingTheTypeThroughADependencyIsDispatched() throws Exception {
        // The library is compiled on its own and seen only on the classpath, the
        // way a dependency jar is.
        Map<String, String> lib = new LinkedHashMap<String, String>();
        lib.put("com.lib.LibraryBase", "package com.lib;\n"
                + "public class LibraryBase { public String base = \"b\"; }\n");
        lib.put("com.lib.LibraryResult", "package com.lib;\n"
                + "public class LibraryResult extends LibraryBase { public String mid = \"m\"; }\n");
        File libClasses = compile(lib);
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.AppResult", PKG + "public class AppResult extends "
                + "com.lib.LibraryResult { public String app = \"a\"; }\n");
        s.put("com.example.Api", PKG + "@RestController public class Api {\n"
                + "    @GetMapping(\"/r\") public com.lib.LibraryBase r() { return new AppResult(); }\n"
                + "}\n");
        File classes = tmp.newFolder();
        List<File> cp = new ArrayList<File>(backendClasspath());
        cp.add(libClasses);
        JavaSourceCompiler.compile(s, classes, cp);
        ProcessorContext ctx = process(classes, new RestControllerAnnotationProcessor(),
                libClasses);
        assertNoErrors(ctx);
        String base = BackendJsonCodecs.of(ctx).sources().get("com.lib.LibraryBaseCn1Json");
        assertNotNull(base);
        assertTrue("the project subclass was left out of the dispatch:\n" + base,
                base.contains("v instanceof com.example.AppResult"));
    }

    @Test
    public void aServerWithOnlyManagementStillGetsAnEntryPoint() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Ops", PKG + "@Configuration public class Ops { }\n");
        File classes = compile(s);
        writeProperties(classes, "application.properties", "cn1.management.enabled=true\n");
        assertNoErrors(process(classes));
        assertTrue("cn1.management.enabled alone produced nothing runnable",
                new File(classes, "com/example/BackendApplication.class").isFile());
    }

    @Test
    public void aManagedOperationReturnsItsDtoAsJson() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Tag", PKG + "public class Tag { public String name; }\n");
        s.put("com.example.Reports", PKG
                + "@Component @ManagedResource(objectName = \"reports\")\n"
                + "public class Reports {\n"
                + "    @ManagedOperation public Tag latest() {\n"
                + "        Tag t = new Tag(); t.name = \"q3\"; return t;\n"
                + "    }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL()},
                getClass().getClassLoader());
        BackendApplication app = (BackendApplication) loader
                .loadClass("com.example.BackendWiring").newInstance();
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        settings.setProperty("cn1.management.token", "t0k");
        Backend backend = withApplication(withManagement(Backend.builder(Config.of(settings, "dev")).quiet()), app).start();
        try {
            HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port
                    + "/manage/managed/reports/latest").openConnection();
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Authorization", "Bearer t0k");
            c.setRequestProperty("Content-Type", "application/json");
            c.getOutputStream().write("{}".getBytes("UTF-8"));
            c.getOutputStream().close();
            String body = read(c);
            // An object, not the quoted toString() of one.
            assertTrue(body, body.contains("\"result\":{\"name\":\"q3\"}"));
        } finally {
            backend.stop();
        }

        s.put("com.example.Reports", PKG
                + "@Component @ManagedResource(objectName = \"reports\")\n"
                + "public class Reports {\n"
                + "    @ManagedOperation public StringBuilder latest() { return null; }\n"
                + "}\n");
        String errors = String.valueOf(process(compile(s)).getErrors());
        assertTrue(errors, errors.contains("@ManagedOperation com.example.Reports.latest "
                + "returns java.lang.StringBuilder, which cannot be written as JSON"));
    }

    @Test
    public void aManagedAttributeThatIsNotAGaugeFailsTheBuild() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Cache", PKG + "@Component @ManagedResource(objectName = \"cache\")\n"
                + "public class Cache {\n"
                + "    @ManagedAttribute public String getName() { return \"c\"; }\n"
                + "    @ManagedAttribute public int getSize() { return 1; }\n"
                + "}\n");
        String errors = String.valueOf(process(compile(s)).getErrors());
        assertTrue(errors, errors.contains("@ManagedAttribute com.example.Cache.getName returns "
                + "java.lang.String; an attribute is a gauge"));
        assertFalse("a numeric attribute was refused: " + errors, errors.contains("getSize"));
    }

    @Test
    public void aServerThatOnlyServesFilesStillGetsAnEntryPoint() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Site", PKG + "@Configuration public class Site { }\n");
        File classes = compile(s);
        writeProperties(classes, "application.properties", "cn1.static.root=www\n");
        assertNoErrors(process(classes));
        assertTrue("cn1.static.root alone produced nothing runnable",
                new File(classes, "com/example/BackendApplication.class").isFile());
    }

    @Test
    public void aClassNoCodecCanBeWrittenForIsABuildError() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Page", PKG + "public class Page<T> { public List<T> items; }\n");
        s.put("com.example.Fixed", PKG + "public class Fixed {\n"
                + "    public final String id;\n"
                + "    public Fixed(String id) { this.id = id; }\n"
                + "}\n");
        // One controller each: a class stops at its first refused route.
        s.put("com.example.PageApi", PKG
                + "@RestController public class PageApi {\n"
                + "    @GetMapping(\"/p\") public Page<String> page() { return null; }\n"
                + "}\n");
        s.put("com.example.FixedApi", PKG
                + "@RestController public class FixedApi {\n"
                + "    @PostMapping(\"/f\") public String f(@RequestBody Fixed f) { return f.id; }\n"
                + "}\n");
        s.put("com.example.ArrayApi", PKG
                + "@RestController public class ArrayApi {\n"
                + "    @GetMapping(\"/a\") public int[] a() { return null; }\n"
                + "}\n");
        s.put("com.example.Unordered", PKG + "public class Unordered { public String n; }\n");
        s.put("com.example.SetApi", PKG
                + "@RestController public class SetApi {\n"
                + "    @PostMapping(\"/s\") public String s(@RequestBody TreeSet<Unordered> s) {\n"
                + "        return \"x\";\n"
                + "    }\n"
                + "    @PostMapping(\"/t\") public String t(@RequestBody TreeSet<String> s) {\n"
                + "        return \"x\";\n"
                + "    }\n"
                + "}\n");
        String errors = String.valueOf(process(compile(s)).getErrors());
        assertTrue(errors, errors.contains("has a type variable for its type"));
        assertTrue(errors, errors.contains("has no constructor without arguments"));
        assertTrue(errors, errors.contains("an array, which has no JSON form here other than "
                + "byte[]"));
        assertTrue(errors, errors.contains("a TreeSet of com.example.Unordered, which is not "
                + "Comparable"));
        assertFalse("a TreeSet of String is ordered by String: " + errors,
                errors.contains("TreeSet of java.lang.String"));
    }

    @Test
    public void aPackagedServerLinksManagementAndMcpOnlyWhenAsked() throws Exception {
        Map<String, String> plain = new LinkedHashMap<String, String>();
        plain.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    @GetMapping(\"/x\") public String x() { return \"x\"; }\n"
                + "}\n");
        RestControllerAnnotationProcessor proc = new RestControllerAnnotationProcessor();
        proc.setDevTools(false);
        File classes = compile(plain);
        assertNoErrors(process(classes, proc));
        String bootstrap = proc.generateBootstrap("com.example");
        assertFalse("nothing asked for management, yet the entry point names it:\n" + bootstrap,
                bootstrap.contains("cn1Access.management(cn1Builder)"));
        assertFalse("nothing asked for MCP, yet the entry point names it:\n" + bootstrap,
                bootstrap.contains("cn1Access.mcp("));
        assertFalse(bootstrap, bootstrap.contains("cn1Access.compiledSettings("));

        classes = compile(plain);
        writeProperties(classes, "application.properties", "cn1.management.enabled=true\n"
                + "cn1.management.path=/ops\n"
                + "cn1.mcp.allowedOrigins=https://a.example,https://b.example\n");
        proc = new RestControllerAnnotationProcessor();
        proc.setDevTools(false);
        assertNoErrors(process(classes, proc));
        bootstrap = proc.generateBootstrap("com.example");
        assertTrue(bootstrap, bootstrap.contains("cn1Access.management(cn1Builder)"));
        assertTrue(bootstrap, bootstrap.contains("cn1Access.mcp(cn1Builder, null)"));
        assertTrue(bootstrap, bootstrap.contains("\"cn1.management.enabled\", \"true\""));
        assertTrue(bootstrap, bootstrap.contains("\"cn1.management.path\", \"/ops\""));
        assertTrue(bootstrap, bootstrap.contains(
                "\"cn1.mcp.allowedOrigins\", \"https://a.example,https://b.example\""));
        assertFalse("naming the origins links the endpoint; whether it serves stays the "
                + "endpoint's own default:\n" + bootstrap, bootstrap.contains("cn1.mcp.enabled"));

        // The property, in a profile's file, asks as surely as the base file.
        classes = compile(plain);
        java.io.FileWriter w = new java.io.FileWriter(new File(classes, "application.properties"));
        w.write("cn1.profile=prod\n");
        w.close();
        w = new java.io.FileWriter(new File(classes, "application-prod.properties"));
        w.write("cn1.management.enabled=true\n");
        w.close();
        proc = new RestControllerAnnotationProcessor();
        proc.setDevTools(false);
        assertNoErrors(process(classes, proc));
        assertTrue(proc.generateBootstrap("com.example").contains("cn1Access.management(cn1Builder)"));

        // With no base file at all: Config still loads the profile's file.
        classes = compile(plain);
        w = new java.io.FileWriter(new File(classes, "application-dev.properties"));
        w.write("cn1.management.enabled=true\n");
        w.close();
        proc = new RestControllerAnnotationProcessor();
        proc.setDevTools(false);
        assertNoErrors(process(classes, proc));
        assertTrue("a profile file without a base file was not read",
                proc.generateBootstrap("com.example").contains("cn1Access.management(cn1Builder)"));
    }

    @Test
    public void applicationPropertiesBecomeCompiledSettings() throws Exception {
        File classes = compile(sample());
        writeProperties(classes, "application.properties", "cn1.server.port=8081\n"
                + "cn1.session.store=db\n"
                + "cn1.datasource.url=${DATABASE_URL}\n"
                + "greeting.prefix=Hi\n");
        writeProperties(classes, "application-dev.properties", "cn1.datasource.url=:memory:\n");
        RestControllerAnnotationProcessor proc = new RestControllerAnnotationProcessor();
        assertNoErrors(process(classes, proc));
        String bootstrap = proc.generateBootstrap("com.example");
        for (String pair : new String[] {"\"cn1.server.port\", \"8081\"",
                "\"cn1.session.store\", \"db\"",
                "\"cn1.datasource.url\", \"${DATABASE_URL}\"",
                "\"greeting.prefix\", \"Hi\""}) {
            assertTrue(pair + " missing from:\n" + bootstrap, bootstrap.contains(pair));
        }
        // A profile file describes another deployment; compiling it in would make
        // the dev settings a production binary's defaults.
        assertFalse("a profile file was compiled in:\n" + bootstrap, bootstrap.contains(":memory:"));
    }

    @Test
    public void propertiesFilesAreReadAsUtf8AndAProfileFileStandsAlone() throws Exception {
        File classes = compile(sample());
        // Only the profile's file: a test on that profile must still get it.
        java.nio.file.Files.write(new File(classes, "application-test.properties").toPath(),
                "greeting=Gr\u00fc\u00dfe\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        ProcessorContext ctx = process(classes, new RestControllerAnnotationProcessor());
        assertNull("there is no base file", RestControllerAnnotationProcessor.baseApplicationProperties(ctx));
        java.util.Properties profile = RestControllerAnnotationProcessor.profileApplicationProperties(ctx, "test");
        assertNotNull("the profile file was not found without a base file", profile);
        // Read as UTF-8, as Config reads it at run time, not as ISO-8859-1.
        assertEquals("Gr\u00fc\u00dfe", profile.getProperty("greeting"));
        java.nio.file.Files.write(new File(classes, "application.properties").toPath(),
                "password=p\u00e4ss\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("p\u00e4ss", RestControllerAnnotationProcessor.baseApplicationProperties(ctx)
                .getProperty("password"));
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
                + "@Component public class Jobs {\n"
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
                + "@Component public class Jobs {\n"
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
                + "    @Bean(destroyMethod = \"close\") @Scope(\"request\")\n"
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
        s.put("com.example.Orders", PKG + "@Component public class Orders extends BaseService {\n"
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
        s.put("com.example.Worker", PKG + "@Component public class Worker {\n"
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
        java.lang.reflect.Method body = worker.getDeclaredMethod(
                BackendWeaver.bodyName("com/example/Worker", "work"));
        assertTrue("the woven body lost its synchronized, so executor threads could run it "
                + "at once", java.lang.reflect.Modifier.isSynchronized(body.getModifiers()));
    }

    @Test
    public void emptyNegatedProfilesAndFutureLifecyclesAreCaught() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Everywhere", PKG + "@Component @Profile(\"!\") public class Everywhere {\n"
                + "}\n");
        s.put("com.example.Starter", PKG + "@Component public class Starter {\n"
                + "    @PostConstruct public Future warm() { return AsyncResult.of(\"x\"); }\n"
                + "}\n");
        String errors = String.valueOf(process(compile(s)).getErrors());
        assertTrue(errors, errors.contains("has \"!\", which names no profile"));
        assertTrue(String.valueOf(warnings), String.valueOf(warnings)
                .contains("@PostConstruct method com.example.Starter.warm returns a Future"));
    }

    @Test
    public void anAsyncLifecycleMethodRunsSynchronously() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Warm", PKG + "@Component public class Warm {\n"
                + "    public static volatile String state = \"cold\";\n"
                + "    @PostConstruct @Async public void init() throws Exception {\n"
                + "        Thread.sleep(200);\n"
                + "        state = \"warm\";\n"
                + "    }\n"
                + "}\n");
        s.put("com.example.Api", PKG + "@RestController public class Api {\n"
                + "    @Autowired private Warm warm;\n"
                + "    @GetMapping(\"/x\") public String x() { return Warm.state; }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        assertTrue(String.valueOf(warnings), String.valueOf(warnings)
                .contains("com.example.Warm.init is a lifecycle method"));
        int port = freePort();
        Backend backend = start(classes, port, new Properties());
        try {
            assertEquals("the server was ready before @PostConstruct had run", "warm",
                    http("GET", port, "/x"));
        } finally {
            backend.stop();
        }
    }

    @Test
    public void twoScopedFactoryBeansOfOneClassKeepTheirOwnStandIns() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Label", PKG + "public class Label {\n"
                + "    private final String text;\n"
                + "    public Label() { this(\"none\"); }\n"
                + "    public Label(String text) { this.text = text; }\n"
                + "    public String text() { return text; }\n"
                + "}\n");
        s.put("com.example.Labels", PKG + "@Configuration public class Labels {\n"
                + "    @Bean @Scope(\"request\") public Label east() { return new Label(\"east\"); }\n"
                + "    @Bean @Scope(\"request\") public Label west() { return new Label(\"west\"); }\n"
                + "}\n");
        s.put("com.example.Api", PKG + "@RestController public class Api {\n"
                + "    @Autowired @Qualifier(\"east\") private Label east;\n"
                + "    @Autowired @Qualifier(\"west\") private Label west;\n"
                + "    @GetMapping(\"/x\") public String x() { return east.text() + \",\" + west.text(); }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        int port = freePort();
        Backend backend = start(classes, port, new Properties());
        try {
            assertEquals("east,west", http("GET", port, "/x"));
        } finally {
            backend.stop();
        }
    }

    @Test
    public void aScopedBeanWhoseConstructorCallsAnOverridableMethodStarts() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Greeter", PKG + "@Component @Scope(\"request\") public class Greeter {\n"
                + "    private final String greeting;\n"
                + "    public Greeter() { greeting = prefix() + \" there\"; }\n"
                + "    public String prefix() { return \"hi\"; }\n"
                + "    public String hello() { return greeting; }\n"
                + "}\n");
        s.put("com.example.Api", PKG + "@RestController public class Api {\n"
                + "    @Autowired private Greeter greeter;\n"
                + "    @GetMapping(\"/x\") public String x() { return greeter.hello(); }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        int port = freePort();
        Backend backend = start(classes, port, new Properties());
        try {
            assertEquals("hi there", http("GET", port, "/x"));
        } finally {
            backend.stop();
        }
    }

    @Test
    public void supportNamesStayDistinct() throws Exception {
        // Aa and BB share a Java hash; Outer_Inner and Outer$Inner folded alike.
        assertEquals("Aa".hashCode(), "BB".hashCode());
        assertNotEquals(BackendWeaver.bodyName("p/Aa", "work"),
                BackendWeaver.bodyName("p/BB", "work"));
        assertNotEquals(BackendBeans.baseName("p/Outer_Inner"),
                BackendBeans.baseName("p/Outer$Inner"));
        assertNotEquals(BackendBeans.baseName("p/A$_B"), BackendBeans.baseName("p/A_$B"));
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Aa", PKG + "public class Aa {\n"
                + "    @Timed(\"aa.work\") public String work() { return \"base\"; }\n"
                + "}\n");
        s.put("com.example.BB", PKG + "public class BB extends Aa {\n"
                + "    @Timed(\"bb.work\") public String work() { return \"sub+\" + super.work(); }\n"
                + "}\n");
        s.put("com.example.Outer_Inner", PKG + "public class Outer_Inner {\n"
                + "    @Timed(\"flat.work\") public String work() { return \"flat\"; }\n"
                + "}\n");
        s.put("com.example.Outer", PKG + "public class Outer {\n"
                + "    public static class Inner {\n"
                + "        @Timed(\"nested.work\") public String work() { return \"nested\"; }\n"
                + "    }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL()},
                getClass().getClassLoader());
        Object sub = loader.loadClass("com.example.BB").newInstance();
        assertEquals("sub+base", sub.getClass().getMethod("work").invoke(sub));
        Object flat = loader.loadClass("com.example.Outer_Inner").newInstance();
        assertEquals("flat", flat.getClass().getMethod("work").invoke(flat));
        Object nested = loader.loadClass("com.example.Outer$Inner").newInstance();
        assertEquals("nested", nested.getClass().getMethod("work").invoke(nested));
    }

    @Test
    public void aWovenOverrideCallingSuperRunsTheBaseBody() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Base", PKG + "public class Base {\n"
                + "    @Timed(\"base.work\") public String work() { return \"base\"; }\n"
                + "}\n");
        s.put("com.example.Sub", PKG + "public class Sub extends Base {\n"
                + "    @Timed(\"sub.work\") public String work() { return \"sub+\" + super.work(); }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL()},
                getClass().getClassLoader());
        Object sub = loader.loadClass("com.example.Sub").newInstance();
        try {
            assertEquals("sub+base", sub.getClass().getMethod("work").invoke(sub));
        } catch (java.lang.reflect.InvocationTargetException err) {
            throw new AssertionError("the base body was overridden by the subclass's: "
                    + err.getCause(), err.getCause());
        }
    }

    @Test
    public void sessionScopedBeansAreDestroyedWhenTheSessionIsInvalidated() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Cart", PKG + "@Component @Scope(\"session\") public class Cart {\n"
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
        URLClassLoader loader = (URLClassLoader) BackendAccess.get().applicationOf(backend).getClass()
                .getClassLoader();
        assertEquals(2, loader.loadClass("com.example.Cart").getField("destroyed").getInt(null));
    }

    @Test
    public void aScopedOrOverloadedManagedResourceIsABuildError() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Visits", PKG + "@Component @Scope(\"session\") @ManagedResource\n"
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
        Class<?> log = BackendAccess.get().applicationOf(backend).getClass().getClassLoader()
                .loadClass("com.example.Log");
        assertEquals("the subclass must release its state before the base tears down",
                "open,sub,base,", log.getField("text").get(null));
    }

    @Test
    public void aRequestBeanCanUseAnotherWhileItIsDestroyed() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Clock", PKG + "@Component @Scope(\"request\") public class Clock {\n"
                + "    public String now() { return \"t\"; }\n"
                + "}\n");
        s.put("com.example.Audit", PKG + "@Component @Scope(\"request\") public class Audit {\n"
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
        s.put("com.example.Cart", PKG + "@Component @Scope(\"session\") public class Cart {\n"
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
        Class<?> cart = BackendAccess.get().applicationOf(backend).getClass().getClassLoader()
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
    public void futuresFromToolsAndOperationsAreBuildErrors() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Tools", PKG + "@Component public class Tools {\n"
                + "    @McpTool(description = \"a\")\n"
                + "    public Future pending() { return AsyncResult.of(\"x\"); }\n"
                + "}\n");
        s.put("com.example.Ops", PKG + "@Component @ManagedResource(objectName = \"ops\")\n"
                + "public class Ops {\n"
                + "    @ManagedOperation @Async public Future rebuild() {\n"
                + "        return AsyncResult.of(\"done\");\n"
                + "    }\n"
                + "    @ManagedOperation @Async public void refresh() { }\n"
                + "    @ManagedAttribute public Future getLevel() { return AsyncResult.of(1); }\n"
                + "}\n");
        String errors = String.valueOf(process(compile(s)).getErrors());
        assertTrue(errors, errors.contains("@McpTool method com.example.Tools.pending returns "
                + "a Future"));
        assertTrue(errors, errors.contains("@ManagedOperation com.example.Ops.rebuild returns "
                + "a Future"));
        assertFalse(errors, errors.contains("com.example.Ops.refresh"));
        assertTrue(errors, errors.contains("@ManagedAttribute com.example.Ops.getLevel returns "
                + "a Future"));
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
        // Two names that fold to one Prometheus name clash as surely.
        s.put("com.example.Work", PKG + "@Component public class Work {\n"
                + "    @Timed(\"latency.ms\") public void a() { }\n"
                + "    @Timed(\"latency_ms\") public void b() { }\n"
                + "}\n");
        errors = String.valueOf(process(compile(s)).getErrors());
        assertTrue(errors, errors.contains("would be exported to Prometheus as latency_ms"));
        // The server's own instruments are taken, by name and by Prometheus series.
        s.put("com.example.Work", PKG + "@Component public class Work {\n"
                + "    @Timed(\"http.server.request.duration\") public void a() { }\n"
                + "    @Timed(\"process_uptime\") public void b() { }\n"
                + "}\n");
        errors = String.valueOf(process(compile(s)).getErrors());
        assertTrue(errors, errors.contains("Metric http.server.request.duration for @Timed on "
                + "com.example.Work.a is one of the server's own instruments"));
        assertTrue(errors, errors.contains("would be exported to Prometheus as process_uptime, "
                + "which metric process.uptime (the server's own instruments)"));
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
        BackendApplication app = (BackendApplication) loader
                .loadClass("com.example.BackendWiring").newInstance();
        int port = freePort();
        Properties off = new Properties();
        off.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend backend = withApplication(Backend.builder(Config.of(off, "dev")).quiet(), app).start();
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
        backend = withApplication(Backend.builder(Config.of(on, "dev")).quiet(), app).start();
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
        BackendApplication app = (BackendApplication) loader
                .loadClass("com.example.BackendWiring").newInstance();
        int port = freePort();
        Properties on = new Properties();
        on.setProperty(Config.SERVER_PORT, String.valueOf(port));
        on.setProperty("feature.x", "true");
        Backend first = withApplication(Backend.builder(Config.of(on, "dev")).quiet(), app).start();
        try {
            assertEquals("true", http("GET", port, "/x"));
        } finally {
            first.stop();
        }
        Properties off = new Properties();
        off.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend second = withApplication(Backend.builder(Config.of(off, "dev")).quiet(), app).start();
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
        s.put("com.example.Visits", PKG + "@Component @Scope(\"request\")\n"
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
        s.put("com.example.Visit", PKG + "@Component @Scope(\"request\") public class Visit {\n"
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
        s.put("com.example.Rows", PKG + "@Component public class Rows {\n"
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
        Backend backend = withApplication(Backend.builder(Config.of(settings, "dev")).quiet()
                .requiresDataSource(), (BackendApplication) loader
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
        Backend backend = withApplication(Backend.builder(Config.of(settings, "dev")).quiet(), (BackendApplication) loader
                        .loadClass("com.example.a.BackendWiring").newInstance())
                .start();
        try {
            String jobs = String.valueOf(BackendAccess.get().applicationOf(backend).getScheduler().describe());
            assertTrue(jobs, jobs.contains("com.example.a.Cleanup.run")
                    && jobs.contains("com.example.b.Cleanup.run"));
            assertTrue(BackendAccess.get().applicationOf(backend).getScheduler()
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
        s.put("com.example.Aaa", PKG + "@Component @Scope(\"request\") public class Aaa {\n"
                + "    public void use() { }\n"
                + "    @PreDestroy public void done() { Log.text += \"aaa,\"; }\n"
                + "}\n");
        s.put("com.example.Zzz", PKG + "@Component @Scope(\"request\") public class Zzz {\n"
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
        Class<?> log = BackendAccess.get().applicationOf(backend).getClass().getClassLoader()
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
        s.put("com.example.Visit", PKG + "@Component @Scope(\"request\") public class Visit {\n"
                + "    @Async public void later() { }\n"
                + "}\n");
        s.put("com.example.Jobs", PKG + "@Component public class Jobs {\n"
                + "    @Scheduled(fixedRate = 1000) @Async public void tick() { }\n"
                + "}\n");
        s.put("com.example.Cart", PKG + "@Component @Scope(\"session\") public class Cart {\n"
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
                .contains("is on a @Scope(\"request\") bean"));
        assertTrue(String.valueOf(warnings), String.valueOf(warnings)
                .contains("is on a @Scope(\"session\") bean"));
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
    public void aspectsOnInterfacesAreBuildErrors() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Ledger", PKG + "public interface Ledger {\n"
                + "    @Transactional default void post() { }\n"
                + "    @Timed void total();\n"
                + "}\n");
        s.put("com.example.Audit", PKG + "@Async public interface Audit {\n"
                + "    void record();\n"
                + "}\n");
        String errors = String.valueOf(process(compile(s)).getErrors());
        assertTrue(errors, errors.contains("@Transactional on com.example.Ledger.post is not "
                + "applied"));
        assertTrue(errors, errors.contains("neither this default method"));
        assertTrue(errors, errors.contains("@Timed on com.example.Ledger.total is not applied"));
        assertTrue(errors, errors.contains("@Async on interface com.example.Audit is not "
                + "applied"));
    }

    @Test
    public void aScheduledFutureIsWarnedAbout() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Sync", PKG + "@Component public class Sync {\n"
                + "    @Scheduled(fixedRate = 60000)\n"
                + "    public Future run() { return AsyncResult.of(\"x\"); }\n"
                + "}\n");
        assertNoErrors(process(compile(s)));
        assertTrue(String.valueOf(warnings), String.valueOf(warnings)
                .contains("@Scheduled method com.example.Sync.run returns a Future"));
    }

    @Test
    public void aPrototypeWithJobsIsWarnedAbout() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Ticker", PKG + "@Component @Scope(\"prototype\") public class Ticker {\n"
                + "    @Scheduled(fixedRate = 60000) public void tick() { }\n"
                + "}\n");
        // Built, as Spring runs it; but said, since it behaves as a singleton.
        assertNoErrors(process(compile(s)));
        String warned = String.valueOf(warnings);
        assertTrue(warned, warned.contains("ticker (com.example.Ticker) has @Scheduled methods "
                + "but is prototype-scoped"));
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
        Class<?> base = BackendAccess.get().applicationOf(backend).getClass().getClassLoader()
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
        Class<?> ticker = BackendAccess.get().applicationOf(backend).getClass().getClassLoader()
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
    public void twoBeansOfOneClassScheduleTheirJobsSeparately() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Worker", PKG + "public class Worker {\n"
                + "    public static volatile int runs;\n"
                + "    @Scheduled(fixedRate = 20) public void tick() { runs++; }\n"
                + "}\n");
        s.put("com.example.Setup", PKG + "@Configuration public class Setup {\n"
                + "    @Bean public Worker east() { return new Worker(); }\n"
                + "    @Bean public Worker west() { return new Worker(); }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        // Both started: the same job name twice refused the start.
        Backend backend = start(classes, freePort(), new Properties());
        try {
            String jobs = String.valueOf(BackendAccess.get().applicationOf(backend).getScheduler().describe());
            assertTrue(jobs, jobs.contains("east.tick") && jobs.contains("west.tick"));
        } finally {
            backend.stop();
        }
    }

    @Test
    public void scopedBeansReachedThroughASingletonAreRefusedOffRequest() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Visit", PKG + "@Component @Scope(\"request\") public class Visit { }\n");
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
        s.put("com.example.Visit", PKG + "@Component @Scope(\"request\") public class Visit { }\n");
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
        s.put("com.example.Visit", PKG + "@Component @Scope(\"request\") public class Visit "
                + "extends Worker { }\n");
        assertNoErrors(process(compile(s)));
        String warned = String.valueOf(warnings);
        assertTrue(warned, warned.contains("@Async method com.example.Visit.later is on a "
                + "@Scope(\"request\") bean"));
    }

    @Test
    public void aScopedConfigurationCannotBuildASingleton() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.PerRequest", PKG + "@Configuration @Scope(\"request\") public class PerRequest {\n"
                + "    @Bean public StringBuilder buffer() { return new StringBuilder(); }\n"
                + "}\n");
        String errors = String.valueOf(process(compile(s)).getErrors());
        assertTrue(errors, errors.contains("@Bean method com.example.PerRequest.buffer builds a "
                + "singleton bean"));
        s.put("com.example.PerRequest", PKG + "@Configuration @Scope(\"request\") public class PerRequest {\n"
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
            Class<?> api = BackendAccess.get().applicationOf(backend).getClass().getClassLoader()
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
        s.put("com.example.Cart", PKG + "@Component @Scope(\"session\") public class Cart {\n"
                + "    @Autowired private HttpSession session;\n"
                + "}\n");
        s.put("com.example.Visit", PKG + "@Component @Scope(\"request\") public class Visit { }\n");
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
        s.put("com.example.Cart", PKG + "@Component @Scope(\"session\") public class Cart {\n"
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
            com.codename1.impl.backend.CronSchedule runtime =
                    com.codename1.impl.backend.CronSchedule.parse(e, "UTC");
            com.codename1.impl.backend.CronSchedule fromMasks = new com.codename1.impl.backend.CronSchedule(
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

    @Test
    public void orderDecidesWhereABeanStandsInAnInjectedList() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Step", PKG + "public interface Step { String name(); }\n");
        // Declared against the order asked for: without @Order a list follows the
        // order the build found the beans in, which is this one.
        s.put("com.example.Alpha", PKG + "@Component @Order(30)\n"
                + "public class Alpha implements Step { public String name() { return \"alpha\"; } }\n");
        s.put("com.example.Beta", PKG + "@Component\n"
                + "public class Beta implements Step { public String name() { return \"beta\"; } }\n");
        s.put("com.example.Gamma", PKG + "@Component @Order(-1)\n"
                + "public class Gamma implements Step { public String name() { return \"gamma\"; } }\n");
        s.put("com.example.Delta", PKG + "@Component\n"
                + "public class Delta implements Step { public String name() { return \"delta\"; } }\n");
        s.put("com.example.Steps", PKG + "@Configuration public class Steps {\n"
                + "    @Bean @Order(5) public Step made() {\n"
                + "        return new Step() { public String name() { return \"made\"; } };\n"
                + "    }\n"
                + "}\n");
        s.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    private final List<Step> steps;\n"
                + "    public Api(List<Step> steps) { this.steps = steps; }\n"
                + "    @GetMapping(\"/steps\") public String steps() {\n"
                + "        StringBuilder sb = new StringBuilder();\n"
                + "        for (Step step : steps) { sb.append(step.name()).append(' '); }\n"
                + "        return sb.toString().trim();\n"
                + "    }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        int port = freePort();
        Backend backend = start(classes, port, new Properties());
        try {
            // Lowest value first; the two without one come last, as they were found.
            assertEquals("gamma made alpha beta delta", http("GET", port, "/steps"));
        } finally {
            backend.stop();
        }
    }

    private static final String SECURED = PKG
            + "import com.codename1.backend.security.*;\n"
            + "import com.codename1.backend.security.core.userdetails.*;\n"
            + "import com.codename1.backend.security.crypto.*;\n";

    private static Map<String, String> secured() {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.SecurityConfig", SECURED
                + "@Configuration public class SecurityConfig {\n"
                // Declared against their order: the catch-all first.
                + "    @Bean @Order(2) public SecurityFilterChain pagesChain(HttpSecurity http) {\n"
                + "        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())\n"
                + "            .csrf(csrf -> csrf.ignoringRequestMatchers(\"/open/**\"));\n"
                + "        return http.build();\n"
                + "    }\n"
                + "    @Bean @Order(1) public SecurityFilterChain apiChain(HttpSecurity http) {\n"
                + "        http.securityMatcher(\"/api/**\")\n"
                + "            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())\n"
                + "            .httpBasic(Customizer.withDefaults())\n"
                + "            .csrf(csrf -> csrf.disable());\n"
                + "        return http.build();\n"
                + "    }\n"
                + "    @Bean public PasswordEncoder encoder() { return new BCryptPasswordEncoder(4); }\n"
                + "    @Bean public UserDetailsService users(PasswordEncoder encoder) {\n"
                + "        return new InMemoryUserDetailsManager(User.withUsername(\"ada\")\n"
                + "                .password(encoder.encode(\"ada-pw\")).roles(\"USER\").build());\n"
                + "    }\n"
                + "}\n");
        s.put("com.example.Api", SECURED
                + "@RestController public class Api {\n"
                + "    @GetMapping(\"/api/me\") public String me(Authentication who) {\n"
                + "        return who == null ? \"nobody\" : who.getName();\n"
                + "    }\n"
                + "    @GetMapping(\"/api/user\") public String user(\n"
                + "            @AuthenticationPrincipal UserDetails user,\n"
                + "            @RequestParam(value = \"x\", defaultValue = \"-\") String x) {\n"
                + "        return (user == null ? \"none\" : user.getUsername() + user.getAuthorities()) + x;\n"
                + "    }\n"
                // The principal is a User: declared as another type it is null,
                // not a failed cast.
                + "    @GetMapping(\"/api/odd\") public String odd(@AuthenticationPrincipal Integer n,\n"
                + "            @AuthenticationPrincipal User user) {\n"
                + "        return n + \" \" + (user == null ? \"none\" : user.getUsername());\n"
                + "    }\n"
                + "    @GetMapping(\"/open/who\") public String who(Authentication who,\n"
                + "            @AuthenticationPrincipal UserDetails user,\n"
                + "            @AuthenticationPrincipal String name) {\n"
                + "        return who + \" \" + user + \" \" + name;\n"
                + "    }\n"
                + "    @GetMapping(\"/open/csrf\") public String csrf(CsrfToken token) {\n"
                + "        return token.getHeaderName() + \" \" + token.getParameterName() + \" \"\n"
                + "                + (token.getToken().length() > 40);\n"
                + "    }\n"
                + "}\n");
        return s;
    }

    private static String call(int port, String path, String user, String password)
            throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + path)
                .openConnection();
        if (user != null) {
            c.setRequestProperty("Authorization", "Basic " + com.codename1.backend.Base64.encode(
                    (user + ":" + password).getBytes("UTF-8")));
        }
        return read(c);
    }

    @Test
    public void aSecurityFilterChainBeanLinksTheLayerAndGuardsTheRoutes() throws Exception {
        File classes = compile(secured());
        RestControllerAnnotationProcessor proc = new RestControllerAnnotationProcessor();
        proc.setDevTools(false);
        assertNoErrors(process(classes, proc));
        String bootstrap = proc.generateBootstrap("com.example");
        assertTrue("a chain bean did not link the security layer:\n" + bootstrap,
                bootstrap.contains("        cn1Access.security(cn1Builder);\n"));
        String wiring = proc.generateWiring("com.example");
        // Each chain method gets its own HttpSecurity, handed the beans it picks
        // its user store and encoder from.
        assertEquals(wiring, 2, wiring.split("SecuritySupport\\.http\\(config, new Object\\[\\] \\{"
                + "b_encoder, b_users\\}, new String\\[\\] \\{\"encoder\", \"users\"\\}, "
                + "new boolean\\[\\] \\{false, false\\}\\)", -1).length - 1);
        assertTrue(wiring, wiring.contains("environment.registerSecurityFilterChain(b_pagesChain, 2);"));
        assertTrue(wiring, wiring.contains("environment.registerSecurityFilterChain(b_apiChain, 1);"));

        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL()},
                getClass().getClassLoader());
        BackendApplication app = (BackendApplication) loader
                .loadClass("com.example.BackendWiring").newInstance();
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend.Builder builder = withApplication(Backend.builder(Config.of(settings, "dev")).quiet(), app);
        // What the generated main does.
        BackendAccess.get().security(builder);
        Backend backend = builder.start();
        try {
            // @Order(1) is asked first although it was declared second: /api is
            // its, and needs credentials.
            assertEquals("HTTP 401: Unauthorized", call(port, "/api/me", null, null));
            assertEquals("HTTP 401: Unauthorized", call(port, "/api/me", "ada", "wrong"));
            assertEquals("ada", call(port, "/api/me", "ada", "ada-pw"));
            assertEquals("ada[ROLE_USER]-", call(port, "/api/user", "ada", "ada-pw"));
            assertEquals("ada[ROLE_USER]7", call(port, "/api/user?x=7", "ada", "ada-pw"));
            assertEquals("null ada", call(port, "/api/odd", "ada", "ada-pw"));
            // Nobody signed in: no Authentication, no UserDetails, and the
            // anonymous principal for a parameter of its type.
            assertEquals("null null anonymousUser", call(port, "/open/who", null, null));
            assertEquals("X-CSRF-TOKEN _csrf true", call(port, "/open/csrf", null, null));
        } finally {
            backend.stop();
        }
        // Without the layer linked, a wiring that has chains refuses to start
        // rather than serve its routes to anybody.
        app = (BackendApplication) loader.loadClass("com.example.BackendWiring").newInstance();
        try {
            withApplication(Backend.builder(Config.of(settings, "dev")).quiet(), app).start();
            fail("a server with chains and no security layer started");
        } catch (IllegalStateException refused) {
            assertTrue(refused.getMessage(), refused.getMessage().contains(
                    "the security layer was not linked into this server"));
        }
    }

    /// A chain that takes tokens and API keys, with what verifies each declared
    /// as beans rather than handed to the DSL.
    private static Map<String, String> tokenSecured() {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.TokenConfig", SECURED
                + "import com.codename1.backend.security.apikey.*;\n"
                + "import com.codename1.backend.security.oauth2.jwt.*;\n"
                + "import com.codename1.backend.security.ratelimit.*;\n"
                + "@Configuration public class TokenConfig {\n"
                + "    public static final byte[] SECRET = new byte[32];\n"
                + "    public static final GeneratedApiKey KEY =\n"
                + "            new ApiKeyGenerator().generate(\"ci-bot\", \"deploy\");\n"
                + "    @Bean public SecurityFilterChain api(HttpSecurity http) {\n"
                + "        http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())\n"
                + "            .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))\n"
                + "            .apiKey(Customizer.withDefaults())\n"
                + "            .rateLimit(\"/limited\", RateLimitKeys.principal(), null);\n"
                + "        return http.build();\n"
                + "    }\n"
                + "    @Bean public JwtDecoder decoder() {\n"
                + "        return DefaultJwtDecoder.withSecretKey(SECRET).build();\n"
                + "    }\n"
                + "    @Bean public ApiKeyRepository keys() {\n"
                + "        return new InMemoryApiKeyRepository(KEY.getApiKey());\n"
                + "    }\n"
                + "    @Bean public RateLimiter limiter() { return new InMemoryRateLimiter(1, 3600); }\n"
                + "}\n");
        s.put("com.example.TokenApi", SECURED
                + "import com.codename1.backend.security.apikey.ApiKey;\n"
                + "import com.codename1.backend.security.oauth2.jwt.Jwt;\n"
                + "@RestController public class TokenApi {\n"
                + "    @GetMapping(\"/me\") public String me(Authentication who,\n"
                + "            @AuthenticationPrincipal Jwt jwt, @AuthenticationPrincipal ApiKey key) {\n"
                + "        return who.getName() + who.getAuthorities() + \" \"\n"
                + "                + (jwt == null ? \"-\" : jwt.getClaimAsString(\"tenant\")) + \" \"\n"
                + "                + (key == null ? \"-\" : key.getOwner());\n"
                + "    }\n"
                + "    @GetMapping(\"/limited\") public String limited() { return \"ok\"; }\n"
                + "}\n");
        return s;
    }

    private static String bearer(int port, String path, String credential) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + path)
                .openConnection();
        if (credential != null) {
            c.setRequestProperty("Authorization", "Bearer " + credential);
        }
        int status = c.getResponseCode();
        if (status >= 400) {
            return status + " " + c.getHeaderField("WWW-Authenticate") + " "
                    + c.getHeaderField("Retry-After");
        }
        return read(c);
    }

    @Test
    public void tokenAndApiKeyBeansReachTheChainThatAsksForThem() throws Exception {
        File classes = compile(tokenSecured());
        RestControllerAnnotationProcessor proc = new RestControllerAnnotationProcessor();
        proc.setDevTools(false);
        assertNoErrors(process(classes, proc));
        String wiring = proc.generateWiring("com.example");
        // The decoder, the key repository and the limiter are handed to the
        // HttpSecurity, which picks each by its type.
        assertTrue(wiring, wiring.contains(
                "SecuritySupport.http(config, new Object[] {b_decoder, b_keys, b_limiter}, "
                + "new String[] {\"decoder\", \"keys\", \"limiter\"}, "
                + "new boolean[] {false, false, false})"));

        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL()},
                getClass().getClassLoader());
        BackendApplication app = (BackendApplication) loader
                .loadClass("com.example.BackendWiring").newInstance();
        Object generated = loader.loadClass("com.example.TokenConfig").getField("KEY").get(null);
        String apiKey = (String) generated.getClass().getMethod("getPlaintext").invoke(generated);
        long now = System.currentTimeMillis() / 1000L;
        String token = new com.codename1.backend.security.oauth2.jwt.DefaultJwtEncoder(
                com.codename1.backend.security.crypto.JwkSet.of(
                        com.codename1.backend.security.crypto.Jwk.ofSecret(new byte[32])))
                .encode(com.codename1.backend.security.oauth2.jwt.JwtEncoderParameters.from(
                        com.codename1.backend.security.oauth2.jwt.JwtClaimsSet.builder()
                                .subject("ada").expiresAt(now + 300).claim("scope", "read")
                                .claim("tenant", "acme").build())).getTokenValue();
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend.Builder builder = withApplication(Backend.builder(Config.of(settings, "test")).quiet(),
                app);
        BackendAccess.get().security(builder);
        Backend backend = builder.start();
        try {
            // A JWT: the principal is the Jwt, and is not an ApiKey.
            assertEquals("ada[SCOPE_read] acme -", bearer(port, "/me", token));
            // An API key: the other way round.
            assertEquals("ci-bot[SCOPE_deploy] - ci-bot", bearer(port, "/me", apiKey));
            assertEquals("401 Bearer null", bearer(port, "/me", null));
            assertTrue(bearer(port, "/me", "cn1_wrong").startsWith(
                    "401 Bearer error=\"invalid_token\", error_description=\"The API key is not valid\""));
            assertTrue(bearer(port, "/me", "a.b.c").startsWith("401 Bearer error=\"invalid_token\""));
            // The RateLimiter bean, keyed by who signed in.
            assertEquals("ok", bearer(port, "/limited", token));
            assertEquals("429 null 3600", bearer(port, "/limited", token));
            assertEquals("ok", bearer(port, "/limited", apiKey));
        } finally {
            backend.stop();
        }
    }

    @Test
    public void theChainIsToldWhichOfTwoLimiterBeansIsPrimary() throws Exception {
        // Two RateLimiter beans and a rule that names none: the chain picks the
        // @Primary one, which it can only do because the wiring says which that is.
        Map<String, String> sources = tokenSecured();
        String config = sources.get("com.example.TokenConfig");
        String one = "    @Bean public RateLimiter limiter() { return new InMemoryRateLimiter(1, 3600); }\n";
        assertTrue(config.contains(one));
        sources.put("com.example.TokenConfig", config.replace(one, one
                + "    @Bean @com.codename1.backend.annotations.Primary public RateLimiter wide() {\n"
                + "        return new InMemoryRateLimiter(3, 3600);\n    }\n"));
        File classes = compile(sources);
        RestControllerAnnotationProcessor proc = new RestControllerAnnotationProcessor();
        proc.setDevTools(false);
        assertNoErrors(process(classes, proc));
        String wiring = proc.generateWiring("com.example");
        assertTrue(wiring, wiring.contains("new String[] {\"decoder\", \"keys\", \"limiter\", "
                + "\"wide\"}, new boolean[] {false, false, false, true})"));

        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL()},
                getClass().getClassLoader());
        BackendApplication app = (BackendApplication) loader
                .loadClass("com.example.BackendWiring").newInstance();
        Object generated = loader.loadClass("com.example.TokenConfig").getField("KEY").get(null);
        String apiKey = (String) generated.getClass().getMethod("getPlaintext").invoke(generated);
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend.Builder builder = withApplication(Backend.builder(Config.of(settings, "test")).quiet(),
                app);
        BackendAccess.get().security(builder);
        Backend backend = builder.start();
        try {
            // Three an hour is the @Primary bean's limit; the other bean allows one.
            assertEquals("ok", bearer(port, "/limited", apiKey));
            assertEquals("ok", bearer(port, "/limited", apiKey));
            assertEquals("ok", bearer(port, "/limited", apiKey));
            assertTrue(bearer(port, "/limited", apiKey).startsWith("429 "));
        } finally {
            backend.stop();
        }
    }

    @Test
    public void aServerWithoutAChainLinksNoSecurity() throws Exception {
        Map<String, String> plain = new LinkedHashMap<String, String>();
        plain.put("com.example.Api", PKG
                + "@RestController public class Api {\n"
                + "    @GetMapping(\"/x\") public String x() { return \"x\"; }\n"
                + "}\n");
        RestControllerAnnotationProcessor proc = new RestControllerAnnotationProcessor();
        proc.setDevTools(false);
        assertNoErrors(process(compile(plain), proc));
        String bootstrap = proc.generateBootstrap("com.example");
        assertFalse("nothing declares a chain, yet the entry point names the layer:\n" + bootstrap,
                bootstrap.contains("security"));
        String wiring = proc.generateWiring("com.example");
        assertFalse(wiring, wiring.contains("security"));
        assertFalse(wiring, wiring.contains("Security"));
    }

    @Test
    public void securityParametersAndChainsAreCheckedAtBuildTime() throws Exception {
        // A handler that reads who is signed in, in a module where nobody can be.
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Api", SECURED
                + "@RestController public class Api {\n"
                + "    @GetMapping(\"/me\") public String me(Authentication who) { return \"x\"; }\n"
                + "    @GetMapping(\"/t\") public String t(CsrfToken token) { return \"x\"; }\n"
                + "    @GetMapping(\"/u\") public String u(@AuthenticationPrincipal UserDetails u) {\n"
                + "        return \"x\";\n    }\n"
                + "}\n");
        String errors = String.valueOf(process(compile(s)).getErrors());
        assertTrue(errors, errors.contains("The Authentication parameter 1 of com.example.Api.me "
                + "is filled in by the security layer, and this module declares no "
                + "SecurityFilterChain bean"));
        assertTrue(errors, errors.contains("The CsrfToken parameter 1 of com.example.Api.t"));
        assertTrue(errors, errors.contains("@AuthenticationPrincipal parameter 1 of "
                + "com.example.Api.u"));

        s = secured();
        s.put("com.example.Api", SECURED
                + "@RestController public class Api {\n"
                + "    @GetMapping(\"/n\") public String n(@AuthenticationPrincipal int n) { return \"x\"; }\n"
                + "}\n");
        errors = String.valueOf(process(compile(s)).getErrors());
        assertTrue(errors, errors.contains("@AuthenticationPrincipal parameter 1 of "
                + "com.example.Api.n is a int. Declare the type of the principal"));

        s = secured();
        s.put("com.example.Api", SECURED
                + "@RestController public class Api {\n"
                + "    @GetMapping(\"/n\") public String n(\n"
                + "            @AuthenticationPrincipal @RequestParam(\"n\") String n) { return n; }\n"
                + "}\n");
        errors = String.valueOf(process(compile(s)).getErrors());
        assertTrue(errors, errors.contains("carries more than one binding annotation"));
        assertTrue(errors, errors.contains("@RequestBody or @AuthenticationPrincipal, and drop "
                + "the others"));

        // A chain built later than start-up, or per request, would guard nothing.
        s = secured();
        s.put("com.example.Late", SECURED
                + "@Configuration public class Late {\n"
                + "    @Bean @Lazy public SecurityFilterChain lateChain(HttpSecurity http) {\n"
                + "        return http.securityMatcher(\"/late/**\").build();\n"
                + "    }\n"
                + "    @Bean @Scope(\"request\") public DefaultSecurityFilterChain eachChain(HttpSecurity http) {\n"
                + "        return http.securityMatcher(\"/each/**\").build();\n"
                + "    }\n"
                + "}\n");
        errors = String.valueOf(process(compile(s)).getErrors());
        assertTrue(errors, errors.contains("SecurityFilterChain lateChain (com.codename1.backend."
                + "security.SecurityFilterChain) is @Lazy; the server takes its chains when it "
                + "starts"));
        assertTrue(errors, errors.contains("is request-scoped; the server takes its chains "
                + "when it starts"));
    }

    // ------------------------------------------------------------------ helpers

    private Backend start(File classes, int port, Properties settings) throws Exception {
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL()},
                getClass().getClassLoader());
        BackendApplication app = (BackendApplication) loader
                .loadClass("com.example.BackendWiring").newInstance();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        return withApplication(Backend.builder(Config.of(settings, "dev")).quiet(), app).start();
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

    @Test
    public void multipartPartsAndFormFieldsBindToParameters() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Uploads", PKG
                + "@RestController public class Uploads {\n"
                + "    @PostMapping(\"/upload\")\n"
                + "    public String upload(@RequestPart(\"file\") HttpServer.Part file,\n"
                + "            @RequestPart(\"raw\") byte[] raw, @RequestPart(\"note\") String note,\n"
                + "            @RequestParam(\"title\") String title,\n"
                + "            @RequestPart(value = \"extra\", required = false) String extra) {\n"
                + "        return file.getFilename() + \":\" + file.getSize() + \":\" + raw.length\n"
                + "                + \":\" + note + \":\" + title + \":\" + extra;\n"
                + "    }\n"
                + "    @PostMapping(\"/form\")\n"
                + "    public String form(@RequestParam(\"a\") String a, @RequestParam(\"n\") int n) {\n"
                + "        return a + \"/\" + n;\n"
                + "    }\n"
                + "}\n");
        File classes = compile(s);
        assertNoErrors(process(classes));
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL()},
                getClass().getClassLoader());
        BackendApplication app = (BackendApplication) loader
                .loadClass("com.example.BackendWiring").newInstance();
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend backend = withApplication(Backend.builder(Config.of(settings, "dev")).quiet(), app)
                .start();
        try {
            String boundary = "b0undary";
            String body = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"title\"\r\n\r\nHello\r\n"
                    + "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"note\"\r\n\r\nshort\r\n"
                    + "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"raw\"\r\n\r\nabc\r\n"
                    + "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"a.txt\"\r\n"
                    + "Content-Type: text/plain\r\n\r\n12345\r\n"
                    + "--" + boundary + "--\r\n";
            String type = "multipart/form-data; boundary=" + boundary;
            assertEquals("a.txt:5:3:short:Hello:null", send(port, "/upload", type, body));
            // A missing required part, and a body that is not multipart at all.
            String partial = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"title\"\r\n\r\nHello\r\n"
                    + "--" + boundary + "--\r\n";
            assertTrue(send(port, "/upload", type, partial).startsWith("HTTP 400"));
            assertTrue(send(port, "/upload", type, "--" + boundary + "\r\nbroken")
                    .startsWith("HTTP 400"));
            // A urlencoded form is @RequestParam too, converted like a query value.
            assertEquals("x y/7", send(port, "/form", "application/x-www-form-urlencoded",
                    "a=x+y&n=7"));
        } finally {
            backend.stop();
        }
    }

    private static String send(int port, String path, String type, String body) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + path)
                .openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", type);
        OutputStream out = c.getOutputStream();
        out.write(body.getBytes("UTF-8"));
        out.close();
        return read(c);
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

    // The build's own builder calls go through the runtime's internal access,
    // as the generated entry point does; these keep the tests' chains readable.
    private static Backend.Builder withApplication(Backend.Builder builder,
                                                   BackendApplication application) {
        BackendAccess.get().application(builder, application);
        return builder;
    }

    private static Backend.Builder withMcp(Backend.Builder builder,
                                           com.codename1.impl.backend.mcp.McpServer.Extension devTools) {
        BackendAccess.get().mcp(builder, devTools);
        return builder;
    }

    private static Backend.Builder withManagement(Backend.Builder builder) {
        BackendAccess.get().management(builder);
        return builder;
    }

    private static void writeProperties(File dir, String name, String content) throws IOException {
        java.io.FileWriter w = new java.io.FileWriter(new File(dir, name));
        try {
            w.write(content);
        } finally {
            w.close();
        }
    }
}
