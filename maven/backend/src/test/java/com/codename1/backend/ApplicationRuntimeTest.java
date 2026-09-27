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
    @DisplayName("a schedule decades between matches is still found")
    void cronFindsARareMatch() {
        // February 29th on a Monday: 2016, then 2044.
        CronSchedule s = CronSchedule.parse("0 0 0 29 2 MON", "UTC");
        long jan2020 = 1577836800000L;
        long feb29of2044 = 2340316800000L;
        assertEquals(feb29of2044, s.next(jan2020));
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
    @DisplayName("an @Async call dropped at the shutdown deadline fails its Future")
    void droppedAsyncFails() throws Exception {
        Tasks.Registry registry = Tasks.open(null);
        TaskExecutor one = new TaskExecutor("one", false, 1, registry);
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        one.execute(new Runnable() {
            public void run() {
                started.countDown();
                try {
                    release.await();
                } catch (InterruptedException err) {
                    // interrupted by the shutdown
                }
            }
        });
        assertTrue(started.await(5, TimeUnit.SECONDS));
        AsyncTask queued = new AsyncTask("test.dropped", false) {
            protected Object call() {
                return "ran";
            }
        };
        one.execute(queued);
        one.shutdown(50);
        release.countDown();
        assertTrue(queued.isDone(), "a dropped call's Future never finished");
        assertThrows(java.util.concurrent.ExecutionException.class, () -> queued.get());
        Tasks.shutdown(registry, 0);
    }

    @Test
    @DisplayName("two metrics that render as one Prometheus name are refused")
    void prometheusNameClash() {
        Metrics.gauge("test.clash.total", "", "", new com.codename1.backend.metrics.Gauge.Source() {
            public double read() {
                return 1;
            }
        });
        assertThrows(IllegalArgumentException.class,
                () -> Metrics.counter("test_clash", "", ""),
                "a counter test_clash exports as test_clash_total, the gauge's name");
        assertThrows(IllegalArgumentException.class,
                () -> Metrics.gauge("test_clash_total", "", "",
                        new com.codename1.backend.metrics.Gauge.Source() {
                    public double read() {
                        return 2;
                    }
                }));
    }

    @Test
    @DisplayName("an enum argument is matched by its constant name, not its toString")
    void enumArgumentsByName() {
        Map args = new LinkedHashMap();
        args.put("state", "PENDING");
        assertEquals(Labelled.PENDING, com.codename1.backend.mcp.McpArgs.enumValue(
                Labelled.values(), args, "state", true));
        args.put("state", "Waiting for payment");
        assertThrows(IllegalArgumentException.class,
                () -> com.codename1.backend.mcp.McpArgs.enumValue(Labelled.values(), args,
                        "state", true));
    }

    enum Labelled {
        PENDING;

        public String toString() {
            return "Waiting for payment";
        }
    }

    @Test
    @DisplayName("a counter refuses to wrap past the 64-bit range")
    void counterOverflow() {
        Counter c = Metrics.counter("test.overflow", "", "");
        c.add(Long.MAX_VALUE - c.get());
        assertThrows(IllegalStateException.class, () -> c.add(1));
        assertEquals(Long.MAX_VALUE, c.get());
    }

    @Test
    @DisplayName("a float argument out of the float range is refused, not made infinite")
    void floatArgumentRange() {
        Map args = new LinkedHashMap();
        args.put("f", new Double(1e100));
        args.put("ok", new Double(1.5));
        assertThrows(IllegalArgumentException.class,
                () -> com.codename1.backend.mcp.McpArgs.floatValue(args, "f", true));
        assertThrows(IllegalArgumentException.class,
                () -> com.codename1.backend.mcp.McpArgs.floatObject(args, "f", true));
        assertEquals(1.5f, com.codename1.backend.mcp.McpArgs.floatValue(args, "ok", true));
    }

    @Test
    @DisplayName("a session cookie name that is not an HTTP token is refused")
    void cookieNameValidated() throws Exception {
        String[] bad = {"", "my session", "a;b", "x\u0001"};
        for(int iter = 0 ; iter < bad.length ; iter++) {
            Properties p = new Properties();
            p.setProperty("cn1.session.cookie", bad[iter]);
            final Config c = Config.of(p, "test");
            assertThrows(IOException.class, () -> Sessions.configure(c, false, null, null),
                    "accepted cookie name [" + bad[iter] + "]");
        }
    }

    @Test
    @DisplayName("a refused shared gauge leaves nothing behind for a retry to skip past")
    void refusedSharedGaugeLeavesNoEntry() {
        Metrics.gauge("test.shared.x", "", "", new com.codename1.backend.metrics.Gauge.Source() {
            public double read() {
                return 1;
            }
        });
        final com.codename1.backend.metrics.Gauge.Source src =
                new com.codename1.backend.metrics.Gauge.Source() {
            public double read() {
                return 2;
            }
        };
        assertThrows(IllegalArgumentException.class,
                () -> Metrics.addSource("test_shared_x", "", "", src));
        assertThrows(IllegalArgumentException.class,
                () -> Metrics.addSource("test_shared_x", "", "", src),
                "a retry found the failed entry and registered no gauge");
    }

    @Test
    @DisplayName("an Error while the application is built still undoes the start")
    void errorDuringCreateCleansUp() throws Exception {
        final boolean[] destroyed = new boolean[1];
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        assertThrows(NoClassDefFoundError.class, () -> Backend.builder(
                Config.of(settings, "test")).quiet()
                .application(new EmptyApplication() {
                    public HttpServer.Handler[] create(Backend.Environment environment) {
                        throw new NoClassDefFoundError("com/example/Missing");
                    }

                    public void stopped() {
                        destroyed[0] = true;
                    }
                }).start());
        assertTrue(destroyed[0], "the beans of a start that failed with an Error were kept");
    }

    @Test
    @DisplayName("a null and an empty first label are two histogram series")
    void histogramNullAndEmptyLabels() {
        Histogram h = Metrics.histogram("test.labels.nullempty", "", "ms", null,
                new String[] {"route", "method", null});
        h.record(1, null, "GET", null);
        h.record(2, "", "GET", null);
        assertEquals(2, h.points().size(), "null and \"\" were merged into one series");
    }

    @Test
    @DisplayName("an MCP extension that fails to install stops the server it was starting")
    void failedMcpAttachStopsTheServer() throws Exception {
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        assertThrows(IllegalStateException.class, () -> Backend.builder(
                Config.of(settings, "dev")).quiet()
                .mcp(new McpServer.Extension() {
                    public void install(McpServer server, Backend backend) {
                        throw new IllegalStateException("refused");
                    }
                })
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) {
                        return null;
                    }
                }).start());
        // The port is free again: the listener did not outlive the failed start.
        ServerSocket again = new ServerSocket(port);
        again.close();
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

            // Two copies each changing their own attribute: merged, not replaced,
            // and the slower one's older last use does not move it back.
            HttpSession fresh = new HttpSession("merge", now, now, 1800);
            fresh.markNew();
            store.save(fresh, null);
            HttpSession one = store.load("merge");
            HttpSession two = store.load("merge");
            one.touch(now + 5000);
            one.setAttribute("theme", "dark");
            two.setAttribute("lang", "en");
            store.save(one, null);
            store.save(two, null);
            HttpSession merged = store.load("merge");
            assertEquals("dark", merged.getAttribute("theme"), "a stale copy discarded a change");
            assertEquals("en", merged.getAttribute("lang"));
            assertEquals(now + 5000, merged.getLastAccessedTime(),
                    "a slower request moved the last use back");
            // A stale copy rotating after another request invalidated the session
            // must not bring it back under the new id.
            HttpSession live = new HttpSession("rot", now, now, 1800);
            live.markNew();
            store.save(live, null);
            HttpSession stale = store.load("rot");
            store.delete("rot");                               // a logout elsewhere
            String rotated = stale.changeSessionId();
            store.save(stale, "rot");
            assertNull(store.load(rotated), "a stale rotation recreated a logged-out session");
            two.removeAttribute("lang");
            store.save(two, null);
            assertNull(store.load("merge").getAttribute("lang"));
            assertEquals("dark", store.load("merge").getAttribute("theme"));

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
    @DisplayName("an expired session's beans are not destroyed while a request is still using them")
    void inUseSessionBeansSurviveThePurge() throws Exception {
        final List ended = new ArrayList();
        Sessions sessions = new Sessions(new EmptyApplication() {
            public void sessionEnded(Object[] beans) {
                ended.add(beans);
            }
        });
        long now = System.currentTimeMillis();
        HttpSession session = new HttpSession("busy", now, now, 1);   // a 1-second timeout
        session.owner = sessions;
        session.scopedBeans(1)[0] = "cart";
        HttpServer.Request request = new HttpServer.Request("GET", "/", "HTTP/1.1",
                new LinkedHashMap(), null);
        session.markNew();
        sessions.getStore().save(session, null);
        sessions.enter(request, session);
        // Two minutes on -- long past the timeout and the purge interval -- and
        // the request is still running: its beans, and the session itself in the
        // store, must survive.
        sessions.purgeIfDue(sessions.getStore(), now + 120000);
        assertTrue(ended.isEmpty(), "a running request's session beans were destroyed");
        assertNotNull(sessions.getStore().load("busy"),
                "a running request's session was purged from the store");
        sessions.leave(request);
        sessions.purgeIfDue(sessions.getStore(), now + 240000);
        assertEquals(1, ended.size(), "the idle session's beans were never destroyed");

        // A request that ends its session and starts another: the replacement's
        // beans are in use too.
        HttpSession first = new HttpSession("first", now, now, 1);
        first.owner = sessions;
        HttpServer.Request switching = new HttpServer.Request("GET", "/", "HTTP/1.1",
                new LinkedHashMap(), null);
        sessions.enter(switching, first);
        HttpSession replacement = new HttpSession("second", now, now, 1);
        replacement.owner = sessions;
        sessions.enter(switching, replacement);
        replacement.scopedBeans(1)[0] = "new cart";
        sessions.purgeIfDue(sessions.getStore(), now + 360000);
        assertEquals(1, ended.size(), "the replacement session's beans were destroyed in use");
        sessions.leave(switching);
    }

    @Test
    @DisplayName("histogram label keys may not collide in Prometheus, nor be its le")
    void histogramLabelKeysChecked() {
        assertThrows(IllegalArgumentException.class, () -> Metrics.histogram("test.lbl.a",
                "", "ms", null, new String[] {"a.b", "a_b", null}));
        assertThrows(IllegalArgumentException.class, () -> Metrics.histogram("test.lbl.b",
                "", "ms", null, new String[] {"le", null, null}));
    }

    @Test
    @DisplayName("a scheduler refuses two jobs of one name")
    void duplicateJobNames() {
        Scheduler scheduler = new Scheduler(null);
        Runnable body = new Runnable() {
            public void run() {
            }
        };
        scheduler.fixedDelay("same", 1000, 1000, null, Tasks.PLATFORM, null, -1, body);
        assertThrows(IllegalArgumentException.class, () -> scheduler.fixedDelay("same", 1000,
                1000, null, Tasks.PLATFORM, null, -1, body));
    }

    @Test
    @DisplayName("a handler that invalidates its session and then throws an Error still logs out")
    void errorAfterInvalidateStillEndsTheSession() throws Exception {
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend backend = Backend.builder(Config.of(settings, "test")).quiet()
                .application(new EmptyApplication())
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request)
                            throws Exception {
                        if(request.getTarget().startsWith("/in")) {
                            request.getSession(true).setAttribute("user", "ada");
                            return request.respond(200, "text/plain", "in".getBytes("UTF-8"));
                        }
                        if(request.getTarget().startsWith("/out")) {
                            request.getSession(true).invalidate();
                            throw new AssertionError("after the logout");
                        }
                        HttpSession s = request.getSession(false);
                        return request.respond(200, "text/plain", String.valueOf(
                                s == null ? null : s.getAttribute("user")).getBytes("UTF-8"));
                    }
                }).start();
        try {
            HttpURLConnection in = open(port, "/in");
            assertEquals("in", read(in));
            String pair = in.getHeaderField("Set-Cookie");
            pair = pair.substring(0, pair.indexOf(';'));
            HttpURLConnection out = open(port, "/out");
            out.setRequestProperty("Cookie", pair);
            try {
                // An Error drops the connection rather than answering 500.
                out.getResponseCode();
            } catch (IOException dropped) {
                // expected either way
            }
            HttpURLConnection me = open(port, "/me");
            me.setRequestProperty("Cookie", pair);
            assertEquals("null", read(me), "the logout was lost when the handler threw an Error");
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("a bare HttpServer refuses sessions it could never store")
    void standaloneSessionsRefused() {
        HttpServer.Request request = new HttpServer.Request("GET", "/", "HTTP/1.1",
                new LinkedHashMap(), null);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> request.getSession(true));
        assertTrue(e.getMessage().contains("Backend.builder()"), e.getMessage());
    }

    @Test
    @DisplayName("a cron zone the runtime does not know is refused, not read as UTC")
    void unknownCronZone() {
        assertThrows(IllegalArgumentException.class,
                () -> CronSchedule.parse("0 0 0 * * *", "Europe/Berli"));
        CronSchedule.parse("0 0 0 * * *", "Europe/Berlin");
    }

    @Test
    @DisplayName("an Error from the application's stopping hook does not abandon the shutdown")
    void stopSurvivesAnError() throws Exception {
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend backend = Backend.builder(Config.of(settings, "test")).quiet()
                .application(new EmptyApplication() {
                    public void stopping() {
                        throw new AssertionError("broken hook");
                    }
                })
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) {
                        return null;
                    }
                }).start();
        backend.stop();
        ServerSocket again = new ServerSocket(port);          // the listener is gone
        again.close();
    }

    @Test
    @DisplayName("a websocket callback carries its own server's executors")
    void websocketCallbacksCarryTheirTasks() throws Exception {
        final Tasks.Registry mine = Tasks.open(null);
        final TaskExecutor[] seen = new TaskExecutor[1];
        WebSocket endpoint = new WebSocket() {
            public void onOpen(WebSocketSession session) {
            }

            public void onText(WebSocketSession session, String message) {
                seen[0] = Tasks.executor("ws", Tasks.PLATFORM);
            }

            public void onBinary(WebSocketSession session, byte[] m, int o, int l) {
            }
        };
        Tasks.Registry other = Tasks.open(null);           // started later: the fallback
        try {
            new Backend.TaskBound(endpoint, mine).onText(null, "hi");
            assertTrue(seen[0] == Tasks.executor(mine, "ws", Tasks.PLATFORM),
                    "the callback used another server's executors");
        } finally {
            Tasks.shutdown(mine, 0);
            Tasks.shutdown(other, 0);
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
    @DisplayName("a managed operation answers 404 only for what does not exist, 400 for bad input")
    void managedOperationStatuses() throws Exception {
        final ManagedBean cache = new ManagedBean() {
            public String getObjectName() { return "cache"; }
            public String getDescription() { return ""; }
            public String[] attributeNames() { return new String[0]; }
            public String[] attributeDescriptions() { return new String[0]; }
            public Object readAttribute(int index) { return null; }
            public String[] operationNames() { return new String[] {"evict"}; }
            public String[] operationDescriptions() { return new String[] {""}; }
            public String[][] operationParameters() { return new String[][] {{"key"}}; }
            public Object invoke(int index, Map arguments) {
                if(!arguments.containsKey("key")) {
                    throw new IllegalArgumentException("key is required");
                }
                return "evicted";
            }
        };
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        settings.setProperty(Management.TOKEN, "t0k");
        Backend backend = Backend.builder(Config.of(settings, "dev")).quiet()
                .application(new EmptyApplication() {
                    public HttpServer.Handler[] create(Backend.Environment environment) {
                        environment.registerManaged(cache);
                        return new HttpServer.Handler[0];
                    }
                })
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) {
                        return null;
                    }
                }).start();
        try {
            assertEquals(200, manage(port, "/manage/managed/cache/evict", "{\"key\":\"a\"}"));
            assertEquals(400, manage(port, "/manage/managed/cache/evict", "{}"),
                    "an argument the operation refused was reported as a missing endpoint");
            assertEquals(400, manage(port, "/manage/managed/cache/evict", "{not json"),
                    "a malformed body was an internal error");
            assertEquals(404, manage(port, "/manage/managed/cache/nope", "{}"));
            assertEquals(404, manage(port, "/manage/managed/none/evict", "{}"));
        } finally {
            backend.stop();
        }
    }

    private static int manage(int port, String path, String body) throws IOException {
        HttpURLConnection c = open(port, path);
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Authorization", "Bearer t0k");
        c.setRequestProperty("Content-Type", "application/json");
        OutputStream out = c.getOutputStream();
        out.write(body.getBytes("UTF-8"));
        out.close();
        return c.getResponseCode();
    }

    @Test
    @DisplayName("a server that does not measure keeps its requests out of another's histogram")
    void uninstrumentedServerIsNotMeasured() throws Exception {
        int[] ports = {freePort(), freePort()};
        Properties measured = new Properties();
        measured.setProperty(Config.SERVER_PORT, String.valueOf(ports[0]));
        Properties plain = new Properties();
        plain.setProperty(Config.SERVER_PORT, String.valueOf(ports[1]));
        HttpServer.Handler ok = new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                return request.respond(200, "text/plain", "ok".getBytes("UTF-8"));
            }
        };
        Backend a = Backend.builder(Config.of(measured, "dev")).quiet().handler(ok).start();
        Backend b = Backend.builder(Config.of(plain, "prod")).quiet().handler(ok).start();
        try {
            read(open(ports[0], "/a"));
            long before = requestCount();
            read(open(ports[1], "/b"));
            read(open(ports[1], "/b"));
            assertEquals(before, requestCount(),
                    "requests to a server without metrics were recorded");
        } finally {
            a.stop();
            b.stop();
        }
    }

    private static long requestCount() {
        long n = 0;
        List points = Metrics.get("http.server.request.duration").points();
        for(int iter = 0 ; iter < points.size() ; iter++) {
            n += ((Number)((Map)points.get(iter)).get("count")).longValue();
        }
        return n;
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
                        if(t.startsWith("/twice")) {
                            // Ends two sessions in one request: both must go.
                            request.getSession(true).invalidate();
                            request.getSession(true).invalidate();
                            return request.respond(200, "text/plain", "gone".getBytes("UTF-8"));
                        }
                        if(t.startsWith("/nothing")) {
                            // A new session and no response to send its cookie on.
                            request.getSession(true).setAttribute("k", "v");
                            return null;
                        }
                        if(t.startsWith("/switch")) {
                            // Ends the session and starts another in one request.
                            request.getSession(true).invalidate();
                            String none = String.valueOf(request.getSession(false));
                            HttpSession next = request.getSession(true);
                            next.setAttribute("user", "grace");
                            return request.respond(200, "text/plain",
                                    (none + "|" + next.isValid()).getBytes("UTF-8"));
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
            // Invalidated then replaced in one request: a lookup in between finds
            // none, and the new session is what the client ends up with.
            HttpURLConnection login2 = open(port, "/login");
            assertEquals("in", read(login2));
            String pair2 = login2.getHeaderField("Set-Cookie");
            pair2 = pair2.substring(0, pair2.indexOf(';'));
            HttpURLConnection sw = open(port, "/switch");
            sw.setRequestProperty("Cookie", pair2);
            assertEquals("null|true", read(sw));
            String replaced = null;
            List cookies = sw.getHeaderFields().get("Set-Cookie");
            for(int iter = 0 ; iter < cookies.size() ; iter++) {
                String c = (String)cookies.get(iter);
                if(!c.contains("Max-Age=0")) {
                    replaced = c.substring(0, c.indexOf(';'));
                }
            }
            assertNotNull(replaced, "the replacement session was never sent");
            HttpURLConnection whoNow = open(port, "/me");
            whoNow.setRequestProperty("Cookie", replaced);
            assertEquals("grace", read(whoNow));
            HttpURLConnection oldOne = open(port, "/me");
            oldOne.setRequestProperty("Cookie", pair2);
            assertEquals("nobody", read(oldOne));
            HttpURLConnection twice = open(port, "/twice");
            twice.setRequestProperty("Cookie", replaced);
            assertEquals("gone", read(twice));
            HttpURLConnection afterTwice = open(port, "/me");
            afterTwice.setRequestProperty("Cookie", replaced);
            assertEquals("nobody", read(afterTwice),
                    "the first of two sessions ended in one request survived");
            int kept = backend.getSessions().getStore().size();
            HttpURLConnection nothing = open(port, "/nothing");
            assertEquals(404, nothing.getResponseCode());
            assertEquals(kept, backend.getSessions().getStore().size(),
                    "a session whose cookie could never be sent was stored");
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

    @Test
    @DisplayName("of two requests rotating one database session, the loser sends no cookie")
    void aLostRotationIsNotAnnounced(@org.junit.jupiter.api.io.TempDir java.io.File dir)
            throws Exception {
        DataSource pool = DataSource.open(new java.io.File(dir, "r.db").getAbsolutePath(),
                2, 5000, 10000);
        try {
            Sessions sessions = new Sessions();
            Sessions.Jdbc store = new Sessions.Jdbc(pool);
            sessions.setStore(store);
            long now = System.currentTimeMillis();
            HttpSession created = new HttpSession("twice", now, now, 1800);
            created.markNew();
            store.save(created, null);
            HttpSession first = store.load("twice");
            HttpSession second = store.load("twice");
            first.owner = sessions;
            second.owner = sessions;
            String won = first.changeSessionId();
            String lost = second.changeSessionId();
            HttpServer.Response a = sessions.finish(first, HttpServer.Response.text(200, "a"));
            HttpServer.Response b = sessions.finish(second, HttpServer.Response.text(200, "b"));
            assertTrue(String.valueOf(a.extraHeaders).contains(won),
                    "the rotation that moved the row did not announce its id");
            assertTrue(b.extraHeaders == null || !String.valueOf(b.extraHeaders).contains(lost),
                    "the losing rotation sent an id nothing stores: " + b.extraHeaders);
            assertNotNull(store.load(won));
            assertNull(store.load(lost));
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("an expired run releases only its own lease, not a later run's")
    void anExpiredRunDoesNotReleaseItsSuccessor(@org.junit.jupiter.api.io.TempDir java.io.File dir)
            throws Exception {
        DataSource pool = DataSource.open(new java.io.File(dir, "l.db").getAbsolutePath(),
                2, 5000, 10000);
        try {
            Scheduler scheduler = new Scheduler(pool);
            Runnable nothing = new Runnable() {
                public void run() {
                }
            };
            Scheduler.Job slow = new Scheduler.Job("slow", Scheduler.FIXED_DELAY, null, 1000, 0,
                    null, Tasks.PLATFORM, "shared", 1, nothing);
            Scheduler.Job next = new Scheduler.Job("next", Scheduler.FIXED_DELAY, null, 1000, 0,
                    null, Tasks.PLATFORM, "shared", 60000, nothing);
            String expired = scheduler.claim(slow);
            assertNotNull(expired);
            Thread.sleep(20);                        // the 1ms lease runs out
            String current = scheduler.claim(next);
            assertNotNull(current, "an expired lease was not taken over");
            scheduler.release(slow, expired);        // the overrun finally ends
            assertNull(scheduler.claim(slow),
                    "the overrun released the lease a later run now holds");
            scheduler.release(next, current);
            assertNotNull(scheduler.claim(slow), "releasing its own lease freed nothing");
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("a colon is legal in a Prometheus metric name but not in a label name")
    void prometheusLabelNamesHaveNoColon() {
        Histogram h = Metrics.histogram("test.lbl.render", "", "ms", null,
                new String[] {"tenant:id", null, null});
        h.record(1, "acme", null, null);
        String text = Metrics.prometheus();
        assertTrue(text.contains("tenant_id=\"acme\""), text);
        assertFalse(text.contains("tenant:id="), "a colon reached a label name: " + text);
        assertThrows(IllegalArgumentException.class, () -> Metrics.histogram("test.lbl.colon",
                "", "ms", null, new String[] {"tenant:id", "tenant_id", null}));
    }

    @Test
    @DisplayName("a task that shuts its own executor down is not waited for")
    void aTaskStoppingItsExecutorIsNotAwaited() throws Exception {
        Tasks.Registry registry = Tasks.open(null);
        final TaskExecutor one = new TaskExecutor("self", false, 1, registry);
        final long[] took = {-1};
        final boolean[] interrupted = {false};
        final CountDownLatch done = new CountDownLatch(1);
        one.execute(new Runnable() {
            public void run() {
                long start = System.currentTimeMillis();
                one.shutdown(5000);
                took[0] = System.currentTimeMillis() - start;
                interrupted[0] = Thread.currentThread().isInterrupted();
                done.countDown();
            }
        });
        assertTrue(done.await(10, TimeUnit.SECONDS));
        assertTrue(took[0] < 2000, "the stopping task waited " + took[0] + "ms for itself");
        assertFalse(interrupted[0], "the stopping task was interrupted by its own shutdown");
        Tasks.shutdown(registry, 0);
    }

    @Test
    @DisplayName("a double that rounds to -2^63 is refused as a long, the exact Long is not")
    void theNegativeLongBoundaryIsChecked() {
        Map args = new LinkedHashMap();
        args.put("rounded", Double.valueOf(-9223372036854775809.0));
        args.put("exact", Long.valueOf(Long.MIN_VALUE));
        assertThrows(IllegalArgumentException.class,
                () -> com.codename1.backend.mcp.McpArgs.longValue(args, "rounded", true));
        assertEquals(Long.MIN_VALUE,
                com.codename1.backend.mcp.McpArgs.longValue(args, "exact", true));
    }

    @Test
    @DisplayName("a bearer token no request could present is refused at start, unechoed")
    void unsendableBearerTokensAreRefused() {
        String[] bad = {"s3cret\n", " s3cret", "s3\u0001cret"};
        for(String token : bad) {
            Properties mcp = new Properties();
            mcp.setProperty(McpServer.ENABLED, "true");
            mcp.setProperty(McpServer.TOKEN, token);
            IOException e = assertThrows(IOException.class,
                    () -> McpServer.fromConfig(Config.of(mcp, "prod"), null, null, null));
            assertTrue(e.getMessage().contains(McpServer.TOKEN), e.getMessage());
            assertFalse(e.getMessage().contains("s3"), "the secret was echoed");
            Properties manage = new Properties();
            manage.setProperty(Management.ENABLED, "true");
            manage.setProperty(Management.TOKEN, token);
            e = assertThrows(IOException.class,
                    () -> Management.fromConfig(Config.of(manage, "prod")));
            assertTrue(e.getMessage().contains(Management.TOKEN), e.getMessage());
        }
    }

    @Test
    @DisplayName("a session a running request holds is not expired by the next lookup")
    void aLookupSparesASessionInUse() throws Exception {
        Sessions sessions = new Sessions();
        long now = System.currentTimeMillis();
        // Stored an hour ago with a minute's timeout: expired by the stored time,
        // but a request that began then is still running and holding it.
        HttpSession held = new HttpSession("long", now - 3600000, now - 3600000, 60);
        held.owner = sessions;
        held.markNew();
        sessions.getStore().save(held, null);
        HttpServer.Request running = new HttpServer.Request("GET", "/", "HTTP/1.1",
                new LinkedHashMap(), null);
        sessions.enter(running, held);
        assertNotNull(sessions.find("long", false),
                "a second request expired the session another request is using");
        assertNotNull(sessions.getStore().load("long"));
        sessions.leave(running);
        // One nobody is using still expires on lookup.
        HttpSession idle = new HttpSession("idle", now - 3600000, now - 3600000, 60);
        idle.owner = sessions;
        idle.markNew();
        sessions.getStore().save(idle, null);
        assertNull(sessions.find("idle", false), "an idle expired session was found");
    }

    @Test
    @DisplayName("a websocket callback reports to its own server's tracer")
    void websocketCallbacksCarryTheirTracer() throws Exception {
        final Tracer[] seen = new Tracer[1];
        WebSocket endpoint = new WebSocket() {
            public void onOpen(WebSocketSession session) {
            }

            public void onText(WebSocketSession session, String message) {
                seen[0] = Tracing.active();
            }

            public void onBinary(WebSocketSession session, byte[] m, int o, int l) {
            }
        };
        Tracer mine = new QuietTracer();
        Tracer later = new QuietTracer();
        Tasks.Registry tasks = Tasks.open(null);
        Tracing.install(later);                       // a second server, started after
        try {
            new Backend.TaskBound(endpoint, tasks, mine).onText(null, "hi");
            assertTrue(seen[0] == mine, "the callback reported to another server's tracer");
            assertTrue(Tracing.active() == later, "the binding outlived the callback");
        } finally {
            Tracing.install(null);
            Tasks.shutdown(tasks, 0);
        }
    }

    @Test
    @DisplayName("NaN and Infinity written as strings are refused as number arguments")
    void nonFiniteNumberStringsAreRefused() {
        Map args = new LinkedHashMap();
        args.put("nan", "NaN");
        args.put("inf", "-Infinity");
        args.put("ok", "2.5");
        assertThrows(IllegalArgumentException.class,
                () -> com.codename1.backend.mcp.McpArgs.doubleValue(args, "nan", true));
        assertThrows(IllegalArgumentException.class,
                () -> com.codename1.backend.mcp.McpArgs.doubleValue(args, "inf", true));
        assertEquals(2.5, com.codename1.backend.mcp.McpArgs.doubleValue(args, "ok", true), 0.0);
    }

    @Test
    @DisplayName("two requests rotating one memory session: the later rotation is undone, not announced")
    void aSecondRotationOfASharedSessionIsUndone() throws Exception {
        Sessions sessions = new Sessions();
        long now = System.currentTimeMillis();
        HttpSession shared = new HttpSession("start", now, now, 1800);
        shared.owner = sessions;
        shared.markNew();
        sessions.getStore().save(shared, null);
        shared.clean();
        HttpServer.Request a = new HttpServer.Request("GET", "/", "HTTP/1.1",
                new LinkedHashMap(), null);
        HttpServer.Request b = new HttpServer.Request("GET", "/", "HTTP/1.1",
                new LinkedHashMap(), null);
        sessions.enter(a, shared);                  // both found it under "start"
        sessions.enter(b, shared);
        String first = shared.changeSessionId();
        HttpServer.Response ra = sessions.finish(a, shared, HttpServer.Response.text(200, "a"));
        String second = shared.changeSessionId();   // the memory store shares the object
        HttpServer.Response rb = sessions.finish(b, shared, HttpServer.Response.text(200, "b"));
        assertTrue(String.valueOf(ra.extraHeaders).contains(first));
        assertTrue(rb.extraHeaders == null || !String.valueOf(rb.extraHeaders).contains(second),
                "the second rotation announced an id: " + rb.extraHeaders);
        assertTrue(sessions.getStore().load(first) == shared,
                "the id the first response carries was removed");
        assertNull(sessions.getStore().load(second));
        assertEquals(first, shared.getId());
        // One request rotating twice is still its own rotation.
        HttpServer.Request c = new HttpServer.Request("GET", "/", "HTTP/1.1",
                new LinkedHashMap(), null);
        sessions.enter(c, shared);
        shared.changeSessionId();
        String last = shared.changeSessionId();
        HttpServer.Response rc = sessions.finish(c, shared, HttpServer.Response.text(200, "c"));
        assertTrue(String.valueOf(rc.extraHeaders).contains(last));
        assertNull(sessions.getStore().load(first), "a request's own rotation left the old id");
    }

    @Test
    @DisplayName("one session the store cannot end does not stop the request's others ending")
    void everyEndedSessionIsFinishedWhenOneFails() throws Exception {
        final List deleted = new ArrayList();
        final String[] ids = new String[2];
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend backend = Backend.builder(Config.of(settings, "test")).quiet()
                .application(new EmptyApplication())
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request)
                            throws Exception {
                        for(int i = 0 ; i < 2 ; i++) {
                            HttpSession s = request.getSession(true);
                            ids[i] = s.getId();
                            s.setAttribute("n", "x");
                            s.invalidate();
                        }
                        return request.respond(200, "text/plain", "ok".getBytes("UTF-8"));
                    }
                }).start();
        final SessionStore memory = backend.getSessions().getStore();
        backend.getSessions().setStore(new SessionStore() {
            public HttpSession load(String id) throws IOException {
                return memory.load(id);
            }

            public void save(HttpSession session, String previousId) throws IOException {
                memory.save(session, previousId);
            }

            public void delete(String id) throws IOException {
                if(id.equals(ids[0])) {
                    throw new IOException("the store refused");
                }
                deleted.add(id);
                memory.delete(id);
            }

            public int purgeExpired(long now) throws IOException {
                return memory.purgeExpired(now);
            }

            public int size() {
                return memory.size();
            }
        });
        try {
            HttpURLConnection c = open(port, "/");
            int status = c.getResponseCode();
            assertEquals(500, status, "the store's failure should fail the request");
            assertTrue(ids[0] != null && ids[1] != null && !ids[0].equals(ids[1]),
                    ids[0] + " / " + ids[1]);
            assertTrue(deleted.contains(ids[1]),
                    "the second ended session was skipped after the first failed: " + deleted
                    + " ids " + ids[0] + " / " + ids[1]);
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("a request's end forgets the ids of the sessions it found, so a pooled one holds none")
    void leavingForgetsFoundSessionIds() {
        Sessions sessions = new Sessions();
        long now = System.currentTimeMillis();
        HttpSession s = new HttpSession("pooled", now, now, 1800);
        HttpServer.Request request = new HttpServer.Request("GET", "/", "HTTP/1.1",
                new LinkedHashMap(), null);
        sessions.enter(request, s);
        assertNotNull(request.sessionIdsFound);
        sessions.leave(request);
        assertNull(request.sessionIdsFound, "a finished request still holds its sessions");
    }

    @Test
    @DisplayName("an executor kind that is neither platform nor virtual stops the start")
    void unknownExecutorKindsAreRefused() {
        Properties settings = new Properties();
        settings.setProperty("cn1.task.executor.reports.kind", "platfrom");
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> Tasks.open(Config.of(settings, "test")));
        assertTrue(e.getMessage().contains("cn1.task.executor.reports.kind"), e.getMessage());
        assertEquals("platform", Tasks.checkedKind("k", " platform "));
        assertNull(Tasks.checkedKind("k", "  "));
    }

    @Test
    @DisplayName("a histogram registered again must have the same buckets and labels")
    void histogramShapesMustAgree() {
        Histogram h = Metrics.histogram("test.shape", "", "ms", new double[] {1, 2}, null);
        assertTrue(h == Metrics.histogram("test.shape", "", "ms", new double[] {1, 2}, null));
        assertThrows(IllegalArgumentException.class, () -> Metrics.histogram("test.shape", "",
                "ms", new double[] {1, 3}, null));
        assertThrows(IllegalArgumentException.class, () -> Metrics.histogram("test.shape", "",
                "ms", new double[] {1, 2}, new String[] {"route", null, null}));
    }

    @Test
    @DisplayName("an empty JSON-RPC batch is answered with Invalid Request")
    void anEmptyBatchIsInvalid() throws Exception {
        Properties settings = new Properties();
        settings.setProperty(McpServer.ENABLED, "true");
        McpServer server = McpServer.fromConfig(Config.of(settings, "dev"), null, null, null);
        HttpServer.Response r = server.handle(new HttpServer.Request("POST", "/mcp",
                "HTTP/1.1", new LinkedHashMap(), "[]"));
        assertEquals(200, r.getStatus());
        // respondJson defers the encoding to the write; render the value here.
        String body = r.body != null && r.body.length > 0 ? new String(r.body, "UTF-8")
                : Json.write(r.deferredJson);
        assertTrue(body.contains("-32600") && body.contains("\"id\":null"), body);
    }

    @Test
    @DisplayName("a server with tracing off stays untraced when another server installs a tracer")
    void anUntracedServerIsNotClaimed() throws Exception {
        final List started = new ArrayList();
        Tracer other = new QuietTracer() {
            public Span startSpan(String name, int kind, Span parent, String traceparent,
                                  String tracestate) {
                started.add(name);
                return null;
            }
        };
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend backend = Backend.builder(Config.of(settings, "test")).quiet()
                .application(new EmptyApplication())
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request)
                            throws Exception {
                        // A span of the application's own, inside the request.
                        Tracing.startSpan("custom").end();
                        return request.respond(200, "text/plain", "ok".getBytes("UTF-8"));
                    }
                }).start();
        Tracing.install(other);                       // another server, started after
        try {
            assertEquals("ok", read(open(port, "/")));
            assertTrue(started.isEmpty(),
                    "the untraced server's work went to another server's tracer: " + started);
            // The fallback itself is unchanged: with no owner, the installed one.
            Tracing.startServer(new HttpServer.Request("GET", "/", "HTTP/1.1",
                    new LinkedHashMap(), null), false, null);
            assertEquals(1, started.size());
        } finally {
            Tracing.install(null);
            backend.stop();
        }
    }

    @Test
    @DisplayName("a tokenless MCP endpoint binds loopback, and refuses an explicit public address")
    void tokenlessMcpStaysOnLoopback() throws Exception {
        Properties settings = new Properties();
        settings.setProperty(McpServer.ENABLED, "true");
        int port = freePort();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        IOException refused = assertThrows(IOException.class, () -> Backend.builder(
                Config.of(settings, "dev")).quiet().application(new EmptyApplication())
                .mcp(null).host("0.0.0.0").start());
        assertTrue(refused.getMessage().contains(McpServer.TOKEN), refused.getMessage());
        Backend backend = Backend.builder(Config.of(settings, "dev")).quiet()
                .application(new EmptyApplication()).mcp(null).start();
        try {
            java.net.InetAddress external = null;
            java.util.Enumeration nics = java.net.NetworkInterface.getNetworkInterfaces();
            while(external == null && nics != null && nics.hasMoreElements()) {
                java.net.NetworkInterface nic = (java.net.NetworkInterface) nics.nextElement();
                java.util.Enumeration addresses = nic.getInetAddresses();
                while(nic.isUp() && addresses.hasMoreElements()) {
                    java.net.InetAddress a = (java.net.InetAddress) addresses.nextElement();
                    if(a instanceof java.net.Inet4Address && !a.isLoopbackAddress()) {
                        external = a;
                        break;
                    }
                }
            }
            if(external != null) {
                java.net.Socket probe = new java.net.Socket();
                try {
                    probe.connect(new java.net.InetSocketAddress(external, port), 2000);
                    org.junit.jupiter.api.Assertions.fail("the tokenless MCP server answered on "
                            + external.getHostAddress());
                } catch (IOException expected) {
                    // refused: loopback only
                } finally {
                    probe.close();
                }
            }
            java.net.Socket local = new java.net.Socket("127.0.0.1", port);
            local.close();
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("a request that is not JSON-RPC 2.0 is refused before its method runs")
    void onlyJsonRpc2IsDispatched() throws Exception {
        Properties settings = new Properties();
        settings.setProperty(McpServer.ENABLED, "true");
        McpServer server = McpServer.fromConfig(Config.of(settings, "dev"), null, null, null);
        HttpServer.Response r = server.handle(new HttpServer.Request("POST", "/mcp",
                "HTTP/1.1", new LinkedHashMap(), "{\"jsonrpc\":\"1.0\",\"id\":4,"
                + "\"method\":\"ping\"}"));
        String body = r.body != null && r.body.length > 0 ? new String(r.body, "UTF-8")
                : Json.write(r.deferredJson);
        assertTrue(body.contains("-32600") && !body.contains("\"result\""), body);
        r = server.handle(new HttpServer.Request("POST", "/mcp", "HTTP/1.1",
                new LinkedHashMap(), "{\"id\":5,\"method\":\"ping\"}"));
        body = r.body != null && r.body.length > 0 ? new String(r.body, "UTF-8")
                : Json.write(r.deferredJson);
        assertTrue(body.contains("-32600"), body);
    }

    @Test
    @DisplayName("a time the clocks pass twice fires at the first, or the second when started between")
    void cronInAFallBackOverlap() {
        CronSchedule c = CronSchedule.parse("0 30 1 * * *", "America/New_York");
        long hour = 3600000L;
        // 2026-11-01: 01:30 EDT is 05:30Z and 01:30 EST is 06:30Z.
        long midnightUtc = 1793491200000L;                  // 2026-11-01T00:00:00Z
        assertEquals(midnightUtc + 5 * hour + 30 * 60000L, c.next(midnightUtc + 4 * hour));
        assertEquals(midnightUtc + 6 * hour + 30 * 60000L, c.next(midnightUtc + 6 * hour));
        // After the first, the next is the following day's, not the repeat.
        assertEquals(midnightUtc + 30 * hour + 30 * 60000L,
                c.next(midnightUtc + 5 * hour + 30 * 60000L));
    }

    /** A tracer that records nothing, for tests that only check which one is used. */
    static class QuietTracer implements Tracer {
        public boolean open(Config config) {
            return true;
        }

        public Span startSpan(String name, int kind, Span parent, String traceparent,
                              String tracestate) {
            return null;
        }

        public void flush(int timeoutMillis) {
        }

        public void shutdown(int timeoutMillis) {
        }

        public HttpServer.Handler relay() {
            return null;
        }

        public void metrics(Map out) {
        }
    }

    // ----------------------------------------------------------------- helpers

    /** An application with no beans, for tests that only need the hooks. */
    static class EmptyApplication implements Backend.Application {
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
