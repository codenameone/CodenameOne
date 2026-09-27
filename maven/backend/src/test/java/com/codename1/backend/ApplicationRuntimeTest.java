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
package com.codename1.backend;

import com.codename1.backend.mcp.McpServer;
import com.codename1.backend.mcp.McpTool;
import com.codename1.backend.metrics.Counter;
import com.codename1.backend.metrics.Histogram;
import com.codename1.backend.metrics.Metrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The runtime pieces the build-generated wiring calls, each on its own: cron,
 * the scheduler, background tasks, sessions, metrics and the MCP endpoint. The
 * whole path from annotations to a running server is the plugin's
 * BackendBeansTest; these pin down behaviour that test cannot reach cheaply.
 */
class ApplicationRuntimeTest {

    // ------------------------------------------------------------------ cron

    @Test
    @DisplayName("a configured cron expression naming a day no allowed month has is refused")
    void impossibleCronIsRefused() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> CronSchedule.parse("0 0 0 30 2 *", "UTC"));
        assertTrue(e.getMessage().contains("never fire"), e.getMessage());
        CronSchedule.parse("0 0 0 29 2 *", "UTC");          // a leap day exists
    }

    @Test
    @DisplayName("a cron time a spring-forward night skips does not fire an hour late")
    void cronSkipsTheDstGap() {
        CronSchedule s = CronSchedule.parse("0 30 2 * * *", "America/New_York");
        // From 2026-03-07 12:00Z: 02:30 on the 8th does not exist in New York
        // (02:00 jumps to 03:00), so the next firing is 02:30 EDT on the 9th,
        // not 03:30 EDT on the 8th (1772955000000).
        assertEquals(1773037800000L, s.next(1772884800000L));
    }

    @Test
    @DisplayName("cron finds the next matching second, and the ones after it")
    void cronNext() {
        CronSchedule every15 = CronSchedule.parse("*/15 * * * * *", "UTC");
        long t = 1767225600000L; // 2026-01-01T00:00:00Z
        assertEquals(t + 15000, every15.next(t));
        assertEquals(t + 30000, every15.next(t + 15000));
        CronSchedule weekdays = CronSchedule.parse("0 30 9 * * MON-FRI", "UTC");
        // 2026-01-01 is a Thursday: the next is that day at 09:30.
        assertEquals(t + (9 * 3600 + 30 * 60) * 1000L, weekdays.next(t));
        // From Friday 09:30, the next is Monday 09:30.
        long friday = t + 86400000L + (9 * 3600 + 30 * 60) * 1000L;
        assertEquals(friday + 3 * 86400000L, weekdays.next(friday));
    }

    @Test
    @DisplayName("the last day of the month, and a date that never exists")
    void cronLastDayAndNever() {
        CronSchedule last = CronSchedule.parse("0 0 0 L * *", "UTC");
        long jan1 = 1767225600000L;
        assertEquals(jan1 + 30 * 86400000L, last.next(jan1)); // Jan 31
        // The 30th of February is refused when parsed now; built from masks
        // directly, as the generated code does, it still answers "never".
        long feb = 1L << 2;
        long day30 = 1L << 30;
        assertEquals(-1, new CronSchedule(1L, 1L, 1L, day30, feb, 0x7fL, false, "UTC",
                "0 0 0 30 2 *").next(jan1));
    }

    @Test
    @DisplayName("a zone moves the wall clock the expression is read in")
    void cronZone() {
        CronSchedule noonBerlin = CronSchedule.parse("0 0 12 * * *", "Europe/Berlin");
        long jan1 = 1767225600000L;
        // Winter: Berlin is UTC+1, so noon there is 11:00 UTC.
        assertEquals(jan1 + 11 * 3600000L, noonBerlin.next(jan1));
        CronSchedule fixed = CronSchedule.parse("0 0 12 * * *", "+05:30");
        assertEquals(jan1 + (6 * 3600 + 30 * 60) * 1000L, fixed.next(jan1));
        assertNotNull(TimeZone.getTimeZone("Europe/Berlin"));
    }

    @Test
    @DisplayName("a malformed field names itself")
    void cronRefusals() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> CronSchedule.parse("0 0 25 * * *", "UTC"));
        assertTrue(e.getMessage().contains("hour"), e.getMessage());
        e = assertThrows(IllegalArgumentException.class,
                () -> CronSchedule.parse("0 0 * * *", "UTC"));
        assertTrue(e.getMessage().contains("leading 0"), e.getMessage());
    }

    // -------------------------------------------------------------- scheduler

    @Test
    @DisplayName("a manual trigger after the scheduler stopped starts nothing")
    void noTriggerAfterStop() throws Exception {
        Scheduler scheduler = new Scheduler(null);
        final AtomicInteger runs = new AtomicInteger();
        scheduler.fixedDelay("job", 1000000, 1000000, null, Tasks.PLATFORM, null, -1,
                new Runnable() {
                    public void run() {
                        runs.incrementAndGet();
                    }
                });
        scheduler.start();
        scheduler.stop(1000);
        assertFalse(scheduler.trigger("job"), "a stopped scheduler started a run");
        Thread.sleep(100);
        assertEquals(0, runs.get());
    }

    @Test
    @DisplayName("an abandoned @Async call fails its Future instead of never finishing")
    void abandonedAsyncFails() throws Exception {
        AsyncTask task = new AsyncTask("test.abandoned", false) {
            protected Object call() {
                return "never";
            }
        };
        task.abandon("stopped");
        assertTrue(task.isDone());
        assertThrows(java.util.concurrent.ExecutionException.class, () -> task.get());
    }

    @Test
    @DisplayName("fixed-delay jobs run, never overlap, and stop with the scheduler")
    void schedulerRunsAndStops() throws Exception {
        final AtomicInteger runs = new AtomicInteger();
        final AtomicInteger concurrent = new AtomicInteger();
        final AtomicInteger overlap = new AtomicInteger();
        Scheduler scheduler = new Scheduler(null);
        scheduler.fixedRate("rate", 0, 5, null, Tasks.PLATFORM, null, -1, new Runnable() {
            public void run() {
                if(concurrent.incrementAndGet() > 1) {
                    overlap.incrementAndGet();
                }
                runs.incrementAndGet();
                try {
                    Thread.sleep(15);
                } catch (InterruptedException ignored) {
                    // test
                }
                concurrent.decrementAndGet();
            }
        });
        scheduler.start();
        long deadline = System.currentTimeMillis() + 5000;
        while(runs.get() < 5 && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        scheduler.stop(1000);
        int after = runs.get();
        Thread.sleep(100);
        assertTrue(after >= 5, "only " + after + " runs");
        assertEquals(0, overlap.get(), "a run overlapped the previous one");
        assertEquals(after, runs.get(), "a run started after stop");
        List jobs = scheduler.describe();
        assertEquals("rate", ((Map)jobs.get(0)).get("name"));
        assertTrue(((Number)((Map)jobs.get(0)).get("skipped")).longValue() > 0,
                "a 5ms rate with 15ms runs must skip fires");
        Tasks.shutdown(1000);
    }

    @Test
    @DisplayName("a lock is refused without a database")
    void lockNeedsADatabase() {
        Scheduler scheduler = new Scheduler(null);
        assertThrows(IllegalStateException.class, () -> scheduler.cron("x",
                CronSchedule.parse("@hourly", null), null, Tasks.PLATFORM, "x", -1,
                new Runnable() {
                    public void run() {
                    }
                }));
    }

    @Test
    @DisplayName("an async task completes its future, and a failure reaches the caller")
    void asyncTask() throws Exception {
        AsyncTask ok = new AsyncTask("ok", false) {
            protected Object call() {
                return AsyncResult.of("done");
            }
        };
        Tasks.platform(ok);
        assertEquals("done", ok.get(5, TimeUnit.SECONDS));
        AsyncTask bad = new AsyncTask("bad", false) {
            protected Object call() {
                throw new IllegalStateException("no");
            }
        };
        Tasks.platform(bad);
        java.util.concurrent.ExecutionException err = assertThrows(
                java.util.concurrent.ExecutionException.class, () -> bad.get(5, TimeUnit.SECONDS));
        assertTrue(err.getCause() instanceof IllegalStateException);
        Tasks.shutdown(1000);
    }

    @Test
    @DisplayName("a virtual task falls back to a platform thread where there are none")
    void virtualFallsBack() throws Exception {
        final CountDownLatch ran = new CountDownLatch(1);
        Tasks.virtual(new Runnable() {
            public void run() {
                ran.countDown();
            }
        });
        assertTrue(ran.await(5, TimeUnit.SECONDS));
        Tasks.shutdown(1000);
    }

    // --------------------------------------------------------------- sessions

    @Test
    @DisplayName("the database session store: no resurrection, and no early expiry for a short timeout")
    void jdbcSessionStore(@org.junit.jupiter.api.io.TempDir java.io.File dir) throws Exception {
        DataSource pool = DataSource.open(new java.io.File(dir, "s.db").getAbsolutePath(),
                2, 5000, 10000);
        try {
            Sessions.Jdbc store = new Sessions.Jdbc(pool);
            long now = System.currentTimeMillis();
            HttpSession created = new HttpSession("sid", now, now, 1800);
            created.markNew();
            created.setAttribute("user", "ada");
            store.save(created, null);
            // Two requests hold their own copies; one logs out.
            HttpSession a = store.load("sid");
            HttpSession b = store.load("sid");
            assertNotNull(a);
            store.delete(a.getId());
            b.setAttribute("seen", "yes");
            store.save(b, null);
            assertNull(store.load("sid"), "a stale copy brought an invalidated session back");

            // A ten-second session last written eleven seconds ago may be a
            // read-only one used every few seconds whose touches were never
            // written; it is not expired until the touch interval has passed too.
            HttpSession short10 = new HttpSession("short", now, now - 11000, 10);
            short10.markNew();
            store.save(short10, null);
            HttpSession back = store.load("short");
            assertFalse(back.isExpired(now), "expired before the touch interval allowed");
            assertTrue(back.isExpired(now + 2000));
            assertEquals(2500L, Sessions.Jdbc.touchInterval(10));
            assertEquals(60000L, Sessions.Jdbc.touchInterval(1800));
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("every loaded copy of a session shares one set of session-scoped beans")
    void sessionBeansAreSharedAcrossCopies() {
        Sessions sessions = new Sessions();
        long now = System.currentTimeMillis();
        HttpSession first = new HttpSession("same", now, now, 1800);
        HttpSession second = new HttpSession("same", now, now, 1800);
        first.owner = sessions;
        second.owner = sessions;
        Object[] beansA = first.scopedBeans(2);
        beansA[0] = "cart";
        assertTrue(first.beanLock() == second.beanLock(),
                "two copies of one session locked different objects");
        assertEquals("cart", second.scopedBeans(2)[0]);
    }

    @Test
    @DisplayName("the built-in server gauges add up every running server, and drop a stopped one")
    void serverGaugesAggregate() throws Exception {
        Backend[] servers = new Backend[2];
        int[] ports = {freePort(), freePort()};
        for(int i = 0 ; i < 2 ; i++) {
            Properties settings = new Properties();
            settings.setProperty(Config.SERVER_PORT, String.valueOf(ports[i]));
            servers[i] = Backend.builder(Config.of(settings, "dev")).quiet()
                    .handler(new HttpServer.Handler() {
                        public HttpServer.Response handle(HttpServer.Request request)
                                throws Exception {
                            return request.respond(200, "text/plain", "ok".getBytes("UTF-8"));
                        }
                    }).start();
        }
        try {
            read(open(ports[0], "/x"));
            read(open(ports[1], "/x"));
            read(open(ports[1], "/x"));
            double both = served();
            double expected = number(servers[0]) + number(servers[1]);
            assertEquals(expected, both, 0.0, "the gauge reported one server, not the sum");
            servers[1].stop();
            assertEquals(number(servers[0]), served(), 0.0,
                    "a stopped server was still counted");
        } finally {
            servers[0].stop();
            servers[1].stop();
        }
    }

    private static double served() {
        Map point = (Map)Metrics.get("cn1.server.requests_served").points().get(0);
        return ((Number)point.get("value")).doubleValue();
    }

    private static double number(Backend b) {
        return ((Number)b.getServer().getMetrics().get("requestsServed")).doubleValue();
    }

    @Test
    @DisplayName("a server without generated beans still applies the session settings")
    void handlerOnlySessionSettings() throws Exception {
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        settings.setProperty("cn1.session.cookie", "APPSESSION");
        settings.setProperty("cn1.session.same-site", "Strict");
        Backend backend = Backend.builder(Config.of(settings, "test")).quiet()
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request)
                            throws Exception {
                        request.getSession(true).setAttribute("k", "v");
                        return request.respond(200, "text/plain", "ok".getBytes("UTF-8"));
                    }
                })
                .start();
        try {
            HttpURLConnection c = open(port, "/");
            assertEquals("ok", read(c));
            String cookie = c.getHeaderField("Set-Cookie");
            assertTrue(cookie != null && cookie.startsWith("APPSESSION=")
                    && cookie.contains("SameSite=Strict"), String.valueOf(cookie));
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("each server has its own executors, and stopping one leaves the other's running")
    void executorsArePerServer() throws Exception {
        final TaskExecutor[] seen = new TaskExecutor[2];
        Backend[] servers = new Backend[2];
        int[] ports = {freePort(), freePort()};
        for(int i = 0 ; i < 2 ; i++) {
            final int index = i;
            Properties settings = new Properties();
            settings.setProperty(Config.SERVER_PORT, String.valueOf(ports[i]));
            servers[i] = Backend.builder(Config.of(settings, "test")).quiet()
                    .handler(new HttpServer.Handler() {
                        public HttpServer.Response handle(HttpServer.Request request)
                                throws Exception {
                            seen[index] = Tasks.executor("jobs", Tasks.PLATFORM);
                            return request.respond(200, "text/plain", "ok".getBytes("UTF-8"));
                        }
                    }).start();
        }
        try {
            assertEquals("ok", read(open(ports[0], "/")));
            assertEquals("ok", read(open(ports[1], "/")));
            assertTrue(seen[0] != null && seen[1] != null && seen[0] != seen[1],
                    "two servers shared one executor");
            servers[1].stop();
            assertTrue(seen[1].isShutdown());
            assertFalse(seen[0].isShutdown(), "stopping one server shut the other's executor");
            final CountDownLatch ran = new CountDownLatch(1);
            seen[0].execute(new Runnable() {
                public void run() {
                    ran.countDown();
                }
            });
            assertTrue(ran.await(5, TimeUnit.SECONDS));
        } finally {
            servers[0].stop();
            servers[1].stop();
        }
    }

    @Test
    @DisplayName("a task still queued when the shutdown deadline passes never starts")
    void shutdownDropsQueuedWorkAtTheDeadline() throws Exception {
        Tasks.Registry registry = Tasks.open(null);
        TaskExecutor one = new TaskExecutor("one", false, 1, registry);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch started = new CountDownLatch(1);
        final AtomicInteger late = new AtomicInteger();
        one.execute(new Runnable() {
            public void run() {
                started.countDown();
                try {
                    release.await();
                } catch (InterruptedException err) {
                    // the shutdown interrupts it; fine
                }
            }
        });
        assertTrue(started.await(5, TimeUnit.SECONDS));
        one.execute(new Runnable() {
            public void run() {
                late.incrementAndGet();
            }
        });
        one.shutdown(50);
        assertEquals(1, one.getDroppedCount());
        release.countDown();
        Thread.sleep(200);
        assertEquals(0, late.get(), "a queued task ran after the shutdown deadline");
        Tasks.shutdown(registry, 0);
    }

    @Test
    @DisplayName("a session is created on demand, found again by its cookie, and invalidated")
    void sessions() throws Exception {
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend backend = Backend.builder(Config.of(settings, "test")).quiet()
                .application(new EmptyApplication())
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request)
                            throws Exception {
                        String t = request.getTarget();
                        if(t.startsWith("/login")) {
                            HttpSession s = request.getSession(true);
                            s.changeSessionId();
                            s.setAttribute("user", "ada");
                            return request.respond(200, "text/plain", "in".getBytes("UTF-8"));
                        }
                        if(t.startsWith("/me")) {
                            HttpSession s = request.getSession(false);
                            String who = s == null ? "nobody" : String.valueOf(s.getAttribute("user"));
                            return request.respond(200, "text/plain", who.getBytes("UTF-8"));
                        }
                        if(t.startsWith("/logout")) {
                            request.getSession(true).invalidate();
                            return request.respond(200, "text/plain", "out".getBytes("UTF-8"));
                        }
                        return null;
                    }
                })
                .start();
        try {
            HttpURLConnection login = open(port, "/login");
            assertEquals(200, login.getResponseCode());
            String cookie = login.getHeaderField("Set-Cookie");
            assertNotNull(cookie, "no session cookie was sent");
            assertTrue(cookie.contains("HttpOnly") && cookie.contains("SameSite=Lax"), cookie);
            String pair = cookie.substring(0, cookie.indexOf(';'));
            HttpURLConnection me = open(port, "/me");
            me.setRequestProperty("Cookie", pair);
            assertEquals("ada", read(me));
            HttpURLConnection anonymous = open(port, "/me");
            assertEquals("nobody", read(anonymous));
            assertNull(anonymous.getHeaderField("Set-Cookie"),
                    "a request that never asked for a session got one");
            HttpURLConnection logout = open(port, "/logout");
            logout.setRequestProperty("Cookie", pair);
            assertEquals("out", read(logout));
            assertTrue(logout.getHeaderField("Set-Cookie").contains("Max-Age=0"));
            HttpURLConnection after = open(port, "/me");
            after.setRequestProperty("Cookie", pair);
            assertEquals("nobody", read(after));
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("a handler's own Set-Cookie survives beside the session's, and its Response is not touched")
    void cookiesCoexist() {
        HttpServer.Response r = new HttpServer.Response(200, "text/plain", new byte[0],
                new LinkedHashMap(java.util.Collections.singletonMap("Set-Cookie", "a=1")));
        HttpServer.Response sent = Sessions.withHeader(r, "Set-Cookie", "b=2");
        Object both = sent.extraHeaders.get("Set-Cookie");
        assertTrue(both instanceof List && ((List)both).size() == 2, String.valueOf(both));
        // The handler's Response may be a shared constant: a cookie written into
        // it would reach the next request that returns it.
        assertTrue(sent != r, "the handler's Response was modified in place");
        assertEquals("a=1", r.extraHeaders.get("Set-Cookie"));
        HttpServer.Response bare = new HttpServer.Response(200, "text/plain", new byte[0], null);
        assertTrue(Sessions.withHeader(bare, "Set-Cookie", "c=3") != bare);
        assertTrue(bare.extraHeaders == null, "a header-less shared Response gained a cookie");
    }

    @Test
    @DisplayName("cn1.session.secure accepts auto, true or false and refuses anything else")
    void secureSettingIsValidated() throws Exception {
        Properties p = new Properties();
        p.setProperty("cn1.session.secure", "tru");
        try {
            IOException refused = assertThrows(IOException.class,
                    () -> Sessions.configure(Config.of(p, "test"), true, null, null));
            assertTrue(refused.getMessage().contains("auto, true or false"),
                    refused.getMessage());
            p.setProperty("cn1.session.secure", "FALSE");
            Sessions.configure(Config.of(p, "test"), true, null, null);
            p.setProperty("cn1.session.timeout", "-1800");
            IOException negative = assertThrows(IOException.class,
                    () -> Sessions.configure(Config.of(p, "test"), true, null, null));
            assertTrue(negative.getMessage().contains("cn1.session.timeout"),
                    negative.getMessage());
        } finally {
            p.clear();
        }
    }

    @Test
    @DisplayName("a histogram keeps its own copy of its boundaries and label keys")
    void histogramCopiesItsArrays() {
        double[] bounds = {1, 2, 3};
        String[] labels = {"route"};
        Histogram h = Metrics.histogram("test.copied", "", "ms", bounds, labels);
        bounds[0] = 100;
        labels[0] = "renamed";
        h.getLabelKeys()[0] = "again";
        assertEquals("route", h.getLabelKeys()[0]);
        h.record(0.5);
        Map point = (Map)h.points().get(0);
        assertEquals(1.0, ((Number)((List)point.get("bounds")).get(0)).doubleValue());
        assertThrows(IllegalArgumentException.class, () -> Metrics.histogram("test.unsorted",
                "", "ms", new double[] {2, 1}, null));
    }

    @Test
    @DisplayName("an MCP byte or short argument out of its range is refused, not wrapped")
    void narrowArgumentsAreRangeChecked() {
        Map args = new LinkedHashMap();
        args.put("s", new Long(40000));
        args.put("b", new Long(-129));
        args.put("ok", new Long(-128));
        assertThrows(IllegalArgumentException.class,
                () -> com.codename1.backend.mcp.McpArgs.shortValue(args, "s", true));
        assertThrows(IllegalArgumentException.class,
                () -> com.codename1.backend.mcp.McpArgs.shortObject(args, "s", true));
        assertThrows(IllegalArgumentException.class,
                () -> com.codename1.backend.mcp.McpArgs.byteValue(args, "b", true));
        assertThrows(IllegalArgumentException.class,
                () -> com.codename1.backend.mcp.McpArgs.byteObject(args, "b", true));
        assertEquals(-128, com.codename1.backend.mcp.McpArgs.byteValue(args, "ok", true));
        assertEquals(-128, com.codename1.backend.mcp.McpArgs.shortValue(args, "ok", true));
        // A JSON number too big for a long must not saturate to Long.MAX_VALUE.
        args.put("huge", new Double(1e20));
        args.put("big", new Double(9.007199254740992E15));
        assertThrows(IllegalArgumentException.class,
                () -> com.codename1.backend.mcp.McpArgs.longValue(args, "huge", true));
        assertEquals(9007199254740992L,
                com.codename1.backend.mcp.McpArgs.longValue(args, "big", true));
    }

    // ---------------------------------------------------------------- metrics

    @Test
    @DisplayName("instruments are shared by name, and render as Prometheus text")
    void metrics() {
        Counter c = Metrics.counter("test.orders", "Orders", "{order}");
        assertTrue(c == Metrics.counter("test.orders", "", ""));
        c.add(3);
        Histogram h = Metrics.histogram("test.latency", "Latency", "ms");
        h.record(7);
        h.record(700);
        String text = Metrics.prometheus();
        assertTrue(text.contains("test_orders_total 3"), text);
        assertTrue(text.contains("test_latency_bucket{le=\"10\"} 1"), text);
        assertTrue(text.contains("test_latency_count 2"), text);
        assertThrows(IllegalArgumentException.class, () -> c.add(-1));
        assertThrows(IllegalArgumentException.class,
                () -> Metrics.histogram("test.orders", "", ""));
    }

    // -------------------------------------------------------------------- MCP

    @Test
    @DisplayName("the MCP endpoint lists and calls a tool, and reports a bad call as a tool error")
    void mcp() throws Exception {
        McpTool echo = new McpTool() {
            public String name() {
                return "echo";
            }

            public String description() {
                return "Echoes";
            }

            public Map inputSchema() {
                Map m = new LinkedHashMap();
                m.put("type", "object");
                return m;
            }

            public Object call(Map arguments) {
                if(!arguments.containsKey("text")) {
                    throw new IllegalArgumentException("text is required");
                }
                return arguments.get("text");
            }
        };
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend backend = Backend.builder(Config.of(settings, "dev")).quiet()
                .mcp(null).mcpTool(echo).handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) {
                        return null;
                    }
                }).start();
        try {
            String init = post(port, "/mcp", "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":"
                    + "\"initialize\",\"params\":{\"protocolVersion\":\"2025-03-26\"}}", null);
            assertTrue(init.contains("\"protocolVersion\":\"2025-03-26\""), init);
            String call = post(port, "/mcp", "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":"
                    + "\"tools/call\",\"params\":{\"name\":\"echo\",\"arguments\":{\"text\":"
                    + "\"hi\"}}}", null);
            assertTrue(call.contains("\"text\":\"hi\"") && call.contains("\"isError\":false"),
                    call);
            String bad = post(port, "/mcp", "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":"
                    + "\"tools/call\",\"params\":{\"name\":\"echo\",\"arguments\":{}}}", null);
            assertTrue(bad.contains("\"isError\":true") && bad.contains("text is required"), bad);
            String answer = rawMcp(port, "127.0.0.1:" + port, "http://evil.example");
            assertTrue(answer.startsWith("HTTP/1.1 403"), "a foreign Origin was served: "
                    + answer);
            // DNS rebinding: the hostile page's name now resolves to 127.0.0.1,
            // so its Origin and the Host header agree -- and must still be refused.
            answer = rawMcp(port, "evil.example:" + port, "http://evil.example:" + port);
            assertTrue(answer.startsWith("HTTP/1.1 403"), "a rebound Origin was served: "
                    + answer);
            answer = rawMcp(port, "127.0.0.1:" + port, "http://localhost:" + port);
            assertTrue(answer.startsWith("HTTP/1.1 200"), "a loopback Origin was refused: "
                    + answer);
        } finally {
            backend.stop();
        }
        // A server started again in the same process has only its own tools: the
        // stopped server's echo is gone, so with none there is no endpoint.
        Backend again = Backend.builder(Config.of(settings, "dev")).quiet()
                .mcp(null).handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) {
                        return null;
                    }
                }).start();
        try {
            HttpURLConnection c = open(port, "/mcp");
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            OutputStream out = c.getOutputStream();
            out.write("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}"
                    .getBytes("UTF-8"));
            out.close();
            assertEquals(404, c.getResponseCode(),
                    "a restarted server served the previous server's MCP tools");
        } finally {
            again.stop();
        }
    }

    @Test
    @DisplayName("outside development the MCP endpoint refuses to start without a token")
    void mcpNeedsATokenInProduction() {
        Properties settings = new Properties();
        settings.setProperty(McpServer.ENABLED, "true");
        IOException e = assertThrows(IOException.class,
                () -> McpServer.fromConfig(Config.of(settings, "prod"), null, null, null));
        assertTrue(e.getMessage().contains(McpServer.TOKEN), e.getMessage());
    }

    // ----------------------------------------------------------------- helpers

    /** An application with no beans, for tests that only need the hooks. */
    static final class EmptyApplication implements Backend.Application {
        public HttpServer.Handler[] create(Backend.Environment environment) {
            return new HttpServer.Handler[0];
        }

        public void registerWebSockets(HttpServer.WebSocketRegistry registry) {
        }

        public void started(Backend backend) {
        }

        public void stopping() {
        }

        public void stopped() {
        }

        public boolean tracksCurrentRequest() {
            return false;
        }

        public void requestEnded(Object[] beans) {
        }

        public void sessionEnded(Object[] beans) {
        }

        public Scheduler getScheduler() {
            return null;
        }

        public List describeBeans() {
            return new ArrayList();
        }

        public List describeRoutes() {
            return new ArrayList();
        }
    }

    private static HttpURLConnection open(int port, String path) throws IOException {
        return (HttpURLConnection)new URL("http://127.0.0.1:" + port + path).openConnection();
    }

    private static String read(HttpURLConnection c) throws IOException {
        int status = c.getResponseCode();
        InputStream in = status >= 400 ? c.getErrorStream() : c.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if(in != null) {
            byte[] buffer = new byte[4096];
            int n;
            while((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
        }
        String body = new String(out.toByteArray(), "UTF-8");
        return status >= 400 ? "HTTP " + status + ": " + body : body;
    }

    /**
     * A raw POST of a ping to /mcp, because HttpURLConnection silently drops
     * restricted headers like Origin and Host -- the request would arrive
     * without them and pass.
     */
    private static String rawMcp(int port, String host, String origin) throws IOException {
        String body = "{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"ping\"}";
        java.net.Socket socket = new java.net.Socket("127.0.0.1", port);
        try {
            socket.getOutputStream().write(("POST /mcp HTTP/1.1\r\nHost: " + host
                    + "\r\nOrigin: " + origin + "\r\nContent-Type: application/json\r\n"
                    + "Content-Length: " + body.length() + "\r\nConnection: close\r\n\r\n"
                    + body).getBytes("UTF-8"));
            ByteArrayOutputStream raw = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while((n = socket.getInputStream().read(buffer)) > 0) {
                raw.write(buffer, 0, n);
            }
            return new String(raw.toByteArray(), "UTF-8");
        } finally {
            socket.close();
        }
    }

    private static String post(int port, String path, String json, String origin)
            throws IOException {
        HttpURLConnection c = open(port, path);
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json");
        if(origin != null) {
            c.setRequestProperty("Origin", origin);
        }
        OutputStream out = c.getOutputStream();
        out.write(json.getBytes("UTF-8"));
        out.close();
        return read(c);
    }

    private static int freePort() throws IOException {
        ServerSocket s = new ServerSocket(0);
        try {
            return s.getLocalPort();
        } finally {
            s.close();
        }
    }
}
