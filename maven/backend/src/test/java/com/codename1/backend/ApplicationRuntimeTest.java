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
    @DisplayName("a metric registered again in another unit is kept, as Micrometer keeps it, and warned about")
    void metricUnitMismatch() throws Exception {
        Counter first = Metrics.counter("test.unit.counter", "", "ms");
        java.io.PrintStream err = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setErr(new java.io.PrintStream(captured, true, "UTF-8"));
        try {
            assertTrue(first == Metrics.counter("test.unit.counter", "", "s"));
            assertTrue(first == Metrics.counter("test.unit.counter", "", "s"));
            assertTrue(first == Metrics.counter("test.unit.counter", "", ""));
        } finally {
            System.setErr(err);
        }
        String warned = captured.toString("UTF-8");
        assertTrue(warned.contains("test.unit.counter is registered again with unit \"s\""),
                warned);
        assertEquals(warned.indexOf("test.unit.counter"), warned.lastIndexOf("test.unit.counter"),
                "warned more than once: " + warned);
        assertEquals("ms", first.getUnit());
    }

    @Test
    @DisplayName("a gauge replacing a shared one survives the old source's removal")
    void replacedSharedGaugeSurvives() {
        com.codename1.backend.metrics.Gauge.Source old =
                new com.codename1.backend.metrics.Gauge.Source() {
                    public double read() {
                        return 1;
                    }
                };
        Metrics.addSource("test.shared.replace", "", "", old);
        com.codename1.backend.metrics.Gauge replacement = Metrics.gauge("test.shared.replace",
                "", "", new com.codename1.backend.metrics.Gauge.Source() {
                    public double read() {
                        return 2;
                    }
                });
        Metrics.removeSource("test.shared.replace", old);    // the old server stops
        assertTrue(Metrics.get("test.shared.replace") == replacement,
                "removing the replaced source deleted the application's gauge");
    }

    @Test
    @DisplayName("label values that print alike are one histogram series, as Prometheus sees them")
    void labelValuesThatPrintAlikeShareASeries() {
        Histogram h = Metrics.histogram("test.labels.text", "", "ms", null,
                new String[] {"flag", "code"});
        h.record(1, Boolean.TRUE, Long.valueOf(1), null);
        h.record(2, "true", "1", null);
        List points = h.points();
        assertEquals(1, points.size(), "two series print as one label set: " + points);
        assertEquals(Long.valueOf(2), ((Map) points.get(0)).get("count"));
        String text = Metrics.prometheus();
        assertEquals(text.indexOf("test_labels_text_count{"),
                text.lastIndexOf("test_labels_text_count{"), text);
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
    @DisplayName("a string argument refuses an array or object, and takes a scalar's text")
    void stringArgumentRefusesStructures() {
        Map args = new LinkedHashMap();
        List list = new ArrayList();
        list.add("42");
        Map object = new LinkedHashMap();
        object.put("id", "42");
        args.put("list", list);
        args.put("object", object);
        args.put("number", Long.valueOf(42));
        args.put("text", "42");
        assertThrows(IllegalArgumentException.class,
                () -> com.codename1.backend.mcp.McpArgs.string(args, "list", true));
        assertThrows(IllegalArgumentException.class,
                () -> com.codename1.backend.mcp.McpArgs.string(args, "object", true));
        assertEquals("42", com.codename1.backend.mcp.McpArgs.string(args, "number", true));
        assertEquals("42", com.codename1.backend.mcp.McpArgs.string(args, "text", true));
        assertNull(com.codename1.backend.mcp.McpArgs.string(args, "absent", false));
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
    @DisplayName("the longest fixed delay or rate runs once, not back to back")
    @org.junit.jupiter.api.Timeout(value = 30, threadMode =
            org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD)
    void hugePeriodsDoNotWrap() throws Exception {
        final AtomicInteger delayRuns = new AtomicInteger();
        final AtomicInteger rateRuns = new AtomicInteger();
        Scheduler scheduler = new Scheduler(null);
        scheduler.fixedDelay("delay", 0, Long.MAX_VALUE, null, Tasks.PLATFORM, null, -1,
                new Runnable() {
                    public void run() {
                        delayRuns.incrementAndGet();
                    }
                });
        scheduler.fixedRate("rate", 0, Long.MAX_VALUE, null, Tasks.PLATFORM, null, -1,
                new Runnable() {
                    public void run() {
                        rateRuns.incrementAndGet();
                    }
                });
        scheduler.start();
        try {
            long deadline = System.currentTimeMillis() + 5000;
            while((delayRuns.get() < 1 || rateRuns.get() < 1)
                    && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
            Thread.sleep(300);
        } finally {
            scheduler.stop(1000);
        }
        assertEquals(1, delayRuns.get(), "the delay wrapped into the past");
        assertEquals(1, rateRuns.get(), "the rate wrapped into the past");
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
    @DisplayName("an async call returning another's pending future does not hold its worker")
    void chainedFutureDoesNotStarveItsExecutor() throws Exception {
        final TaskExecutor solo = new TaskExecutor("solo", false, 1, null);
        AsyncTask outer = new AsyncTask("outer", false) {
            protected Object call() {
                AsyncTask inner = new AsyncTask("inner", false) {
                    protected Object call() {
                        return AsyncResult.of("inner-done");
                    }
                };
                solo.execute(inner);           // queued behind this, on the only thread
                return inner;
            }
        };
        solo.execute(outer);
        assertEquals("inner-done", outer.get(10, TimeUnit.SECONDS),
                "the outer call held the only worker waiting for the inner one");
        AsyncTask failing = new AsyncTask("failing", false) {
            protected Object call() {
                AsyncTask inner = new AsyncTask("broken", false) {
                    protected Object call() {
                        throw new IllegalStateException("inner failed");
                    }
                };
                solo.execute(inner);
                return inner;
            }
        };
        solo.execute(failing);
        java.util.concurrent.ExecutionException err = assertThrows(
                java.util.concurrent.ExecutionException.class,
                () -> failing.get(10, TimeUnit.SECONDS));
        assertEquals("inner failed", err.getCause().getMessage());
        solo.shutdown(1000);
    }

    @Test
    @DisplayName("an executor shut down with the longest wait waits for its tasks")
    void hugeShutdownWaitWaits() throws Exception {
        // A guard, not a reproduction: shutdown() only takes deadline - now,
        // which wraparound already kept right. It holds whichever way the wait
        // is written.
        TaskExecutor e = new TaskExecutor("patient", false, 1, null);
        final AtomicInteger ran = new AtomicInteger();
        e.execute(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException err) {
                    Thread.currentThread().interrupt();
                }
                ran.incrementAndGet();
            }
        });
        e.execute(new Runnable() {
            public void run() {
                ran.incrementAndGet();
            }
        });
        e.shutdown(Long.MAX_VALUE);
        assertEquals(0, e.getDroppedCount(), "the wait overflowed and gave up at once");
        assertEquals(2, ran.get());
    }

    @Test
    @DisplayName("the longest timed get waits instead of timing out at once")
    void hugeTimeoutWaits() throws Exception {
        assertEquals(Long.MAX_VALUE, AsyncTask.deadline(System.currentTimeMillis(),
                TimeUnit.DAYS.toMillis(Long.MAX_VALUE)));
        final CountDownLatch go = new CountDownLatch(1);
        AsyncTask slow = new AsyncTask("slow", false) {
            protected Object call() throws Exception {
                go.await(5, TimeUnit.SECONDS);
                return AsyncResult.of("done");
            }
        };
        Tasks.platform(slow);
        Thread opener = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException err) {
                    Thread.currentThread().interrupt();
                }
                go.countDown();
            }
        });
        opener.start();
        assertEquals("done", slow.get(Long.MAX_VALUE, TimeUnit.DAYS));
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
    @DisplayName("two servers' database session stores never read each other's sessions")
    void jdbcSessionNamespaces(@org.junit.jupiter.api.io.TempDir java.io.File dir)
            throws Exception {
        DataSource pool = DataSource.open(new java.io.File(dir, "ns.db").getAbsolutePath(),
                2, 5000, 10000);
        try {
            Sessions.Jdbc orders = new Sessions.Jdbc(pool, "com.example.orders");
            Sessions.Jdbc admin = new Sessions.Jdbc(pool, "com.example.admin");
            long now = System.currentTimeMillis();
            HttpSession signedIn = new HttpSession("shared-cookie", now, now, 1800);
            signedIn.markNew();
            signedIn.setAttribute("user", "ada");
            orders.save(signedIn, null);
            assertNull(admin.load("shared-cookie"),
                    "another server accepted this server's session cookie");
            admin.delete("shared-cookie");
            assertEquals(0, admin.purgeExpired(Long.MAX_VALUE / 4));
            assertEquals("ada", orders.load("shared-cookie").getAttribute("user"));
            // A replica of the same server shares them.
            assertEquals("ada", new Sessions.Jdbc(pool, "com.example.orders")
                    .load("shared-cookie").getAttribute("user"));
        } finally {
            pool.close();
        }
    }

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
            // Answered 500, as Spring Boot's Tomcat answers a handler's Error.
            assertEquals(500, out.getResponseCode());
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
    @DisplayName("one process runs one backend: a second start is refused, a restart is not")
    void oneBackendPerProcess() throws Exception {
        final HttpServer.Handler none = new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) {
                return null;
            }
        };
        Properties first = new Properties();
        first.setProperty(Config.SERVER_PORT, String.valueOf(freePort()));
        Backend running = Backend.builder(Config.of(first, "test")).quiet().handler(none).start();
        try {
            final Properties second = new Properties();
            second.setProperty(Config.SERVER_PORT, String.valueOf(freePort()));
            IllegalStateException refused = assertThrows(IllegalStateException.class,
                    () -> Backend.builder(Config.of(second, "test")).quiet().handler(none)
                            .start());
            assertTrue(refused.getMessage().contains("one process runs one backend"),
                    refused.getMessage());
        } finally {
            running.stop();
        }
        Properties again = new Properties();
        again.setProperty(Config.SERVER_PORT, String.valueOf(freePort()));
        Backend restarted = Backend.builder(Config.of(again, "test")).quiet().handler(none)
                .start();
        restarted.stop();
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
    @DisplayName("a failed start waits for its start-up tasks before destroying their beans")
    void failedStartDrainsItsTasks() throws Exception {
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(freePort()));
        final java.util.concurrent.atomic.AtomicBoolean finished =
                new java.util.concurrent.atomic.AtomicBoolean();
        final java.util.concurrent.atomic.AtomicBoolean seenAtDestroy =
                new java.util.concurrent.atomic.AtomicBoolean();
        assertThrows(IllegalStateException.class, () -> Backend.builder(
                Config.of(settings, "test")).quiet().shutdownTimeoutMillis(5000)
                .application(new EmptyApplication() {
                    public HttpServer.Handler[] create(Backend.Environment environment) {
                        // A @PostConstruct that starts work, then a later bean
                        // that refuses its configuration.
                        Tasks.executor("boot", Tasks.PLATFORM).execute(new Runnable() {
                            public void run() {
                                long until = System.currentTimeMillis() + 300;
                                while(System.currentTimeMillis() < until) {
                                    try {
                                        Thread.sleep(20);
                                    } catch (InterruptedException ignored) {
                                        // keeps going, as a task may
                                    }
                                }
                                finished.set(true);
                            }
                        });
                        throw new IllegalStateException("a later bean refuses to start");
                    }

                    public void stopped() {
                        seenAtDestroy.set(finished.get());
                    }
                }).start());
        assertTrue(seenAtDestroy.get(),
                "the beans were destroyed while a start-up task was still using them");
    }

    @Test
    @DisplayName("a metric reader that declines to open is never shut down")
    void declinedMetricReaderIsNotStopped() throws Exception {
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        final AtomicInteger shutdowns = new AtomicInteger();
        // The dev profile turns the management endpoints on, so the server
        // measures whatever the reader answers.
        Backend backend = Backend.builder(Config.of(settings, "dev")).quiet()
                .metrics(new com.codename1.backend.metrics.MetricReader() {
                    public boolean open(Config config) {
                        return false;
                    }

                    public void shutdown(int timeoutMillis) {
                        shutdowns.incrementAndGet();
                    }
                })
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) {
                        return null;
                    }
                }).start();
        backend.stop();
        assertEquals(0, shutdowns.get(), "stop() shut down a reader that never opened");
    }

    @Test
    @DisplayName("an AUTO executor is pinned to the kind a later caller asks for by name")
    void autoExecutorIsPinnedByAnExplicitKind() {
        Tasks.Registry mine = Tasks.open(null);
        try {
            TaskExecutor first = Tasks.executor(mine, "shared", Tasks.AUTO);
            assertFalse(first.isVirtual());          // no virtual hosts on this runtime
            TaskExecutor again = Tasks.executor(mine, "shared", Tasks.VIRTUAL);
            assertTrue(again == first);
            assertTrue(again.isVirtual(),
                    "the explicit kind lost to the AUTO call that happened to come first");
            // The explicit kind is now the executor's, so the other one conflicts.
            assertThrows(IllegalStateException.class,
                    () -> Tasks.executor(mine, "shared", Tasks.PLATFORM));
        } finally {
            Tasks.shutdown(mine, 0);
        }
    }

    @Test
    @DisplayName("the websocket wrapper forwards the endpoint's subprotocols")
    void wrappedEndpointKeepsItsSubprotocols() {
        WebSocket endpoint = new WebSocket() {
            public void onOpen(WebSocketSession session) {
            }

            public void onText(WebSocketSession session, String message) {
            }

            public void onBinary(WebSocketSession session, byte[] m, int o, int l) {
            }

            public String[] getSubprotocols() {
                return new String[] {"chat.v2"};
            }
        };
        Tasks.Registry mine = Tasks.open(null);
        try {
            String[] offered = new Backend.TaskBound(endpoint, mine).getSubprotocols();
            assertNotNull(offered, "the wrapper hid the endpoint's subprotocols");
            assertEquals("chat.v2", offered[0]);
        } finally {
            Tasks.shutdown(mine, 0);
        }
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
    @DisplayName("a time the clocks pass twice fires once, at the first, from either side of the change")
    void cronInAFallBackOverlap() {
        CronSchedule c = CronSchedule.parse("0 30 1 * * *", "America/New_York");
        long hour = 3600000L;
        long minute = 60000L;
        // 2026-11-01: 01:30 EDT is 05:30Z, the clocks go back at 06:00Z, and 01:30
        // EST is 06:30Z. The next day's 01:30 EST is 30.5 hours after midnight UTC.
        long midnightUtc = 1793491200000L;                  // 2026-11-01T00:00:00Z
        long tomorrow = midnightUtc + 30 * hour + 30 * minute;
        assertEquals(midnightUtc + 5 * hour + 30 * minute, c.next(midnightUtc + 4 * hour));
        // Once the first has passed, that day's is spent -- before the change
        // (just after a run at the first) and after it alike.
        assertEquals(tomorrow, c.next(midnightUtc + 5 * hour + 30 * minute));
        assertEquals(tomorrow, c.next(midnightUtc + 5 * hour + 45 * minute));
        assertEquals(tomorrow, c.next(midnightUtc + 6 * hour));
        // A job that fires through the night still gets the times after the fold.
        CronSchedule half = CronSchedule.parse("0 */30 * * * *", "America/New_York");
        assertEquals(midnightUtc + 7 * hour, half.next(midnightUtc + 6 * hour + 10 * minute),
                "the 02:00 EST after the repeated hour was skipped");
    }

    @Test
    @DisplayName("a session rotated by a running request is spared under its old id too")
    void aRotatingRequestKeepsItsOldIdBusy(@org.junit.jupiter.api.io.TempDir java.io.File dir)
            throws Exception {
        DataSource pool = DataSource.open(new java.io.File(dir, "busy.db").getAbsolutePath(),
                2, 5000, 10000);
        try {
            Sessions sessions = new Sessions();
            sessions.setStore(new Sessions.Jdbc(pool));
            long now = System.currentTimeMillis();
            HttpSession stored = new HttpSession("old", now - 3600000, now - 3600000, 60);
            stored.markNew();
            sessions.getStore().save(stored, null);
            HttpSession copy = sessions.getStore().load("old");
            copy.owner = sessions;
            HttpServer.Request running = new HttpServer.Request("GET", "/", "HTTP/1.1",
                    new LinkedHashMap(), null);
            sessions.enter(running, copy);
            copy.changeSessionId();              // the row is still under "old"
            assertNotNull(sessions.find("old", false),
                    "another request expired the row a rotating request still owns");
            assertNotNull(sessions.getStore().load("old"));
            sessions.leave(running);
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("an id-less message with no method is invalid, and answered; a response is ignored")
    void malformedIdlessMessagesAreAnswered() throws Exception {
        Properties settings = new Properties();
        settings.setProperty(McpServer.ENABLED, "true");
        McpServer server = McpServer.fromConfig(Config.of(settings, "dev"), null, null, null);
        HttpServer.Response r = server.handle(new HttpServer.Request("POST", "/mcp",
                "HTTP/1.1", new LinkedHashMap(), "{\"jsonrpc\":\"2.0\"}"));
        assertEquals(200, r.getStatus());
        String body = r.body != null && r.body.length > 0 ? new String(r.body, "UTF-8")
                : Json.write(r.deferredJson);
        assertTrue(body.contains("-32600") && body.contains("\"id\":null"), body);
        r = server.handle(new HttpServer.Request("POST", "/mcp", "HTTP/1.1",
                new LinkedHashMap(), "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}"));
        assertEquals(202, r.getStatus(), "a response the client sent was answered");
    }

    @Test
    @DisplayName("a float argument too small for a float is refused, not made zero")
    void floatUnderflowIsRefused() {
        Map args = new LinkedHashMap();
        args.put("tiny", Double.valueOf(1e-100));
        args.put("zero", Double.valueOf(0));
        assertThrows(IllegalArgumentException.class,
                () -> com.codename1.backend.mcp.McpArgs.floatValue(args, "tiny", true));
        assertEquals(0f, com.codename1.backend.mcp.McpArgs.floatValue(args, "zero", true));
    }

    @Test
    @DisplayName("a handler that loses a race to stop the server does not hold up the drain")
    void aSecondStoppingHandlerReturns() throws Exception {
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        settings.setProperty(Config.SERVER_SHUTDOWN_MILLIS, "20000");
        final Backend[] self = new Backend[1];
        final CountDownLatch both = new CountDownLatch(2);
        Backend backend = Backend.builder(Config.of(settings, "test")).quiet()
                .application(new EmptyApplication())
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request)
                            throws Exception {
                        both.countDown();
                        both.await(5, TimeUnit.SECONDS);
                        self[0].stop();
                        return request.respond(200, "text/plain", "ok".getBytes("UTF-8"));
                    }
                }).start();
        self[0] = backend;
        final List errors = new ArrayList();
        Thread[] callers = new Thread[2];
        for(int i = 0 ; i < 2 ; i++) {
            callers[i] = new Thread(new Runnable() {
                public void run() {
                    try {
                        HttpURLConnection c = open(port, "/");
                        c.getResponseCode();
                    } catch (IOException err) {
                        // the server is stopping under it; fine
                    }
                }
            });
            callers[i].start();
        }
        long started = System.currentTimeMillis();
        for(Thread t : callers) {
            t.join(30000);
        }
        backend.stop();
        long took = System.currentTimeMillis() - started;
        assertTrue(took < 15000, "the drain waited " + took + "ms for the losing stop caller");
    }

    @Test
    @DisplayName("a scheduled run that ends during the drain still records its metrics")
    void aDrainedRunIsMeasured() throws Exception {
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        final CountDownLatch running = new CountDownLatch(1);
        final Scheduler[] scheduler = new Scheduler[1];
        Backend backend = Backend.builder(Config.of(settings, "dev")).quiet()
                .application(new EmptyApplication() {
                    public void started(Backend b) {
                        scheduler[0] = new Scheduler(null);
                        scheduler[0].fixedDelay("drainedJob", 0, 1000000, null, Tasks.PLATFORM,
                                null, -1, new Runnable() {
                                    public void run() {
                                        running.countDown();
                                        try {
                                            Thread.sleep(400);
                                        } catch (InterruptedException err) {
                                            // the drain may interrupt at its deadline
                                        }
                                    }
                                });
                        scheduler[0].bind(b);
                        scheduler[0].start();
                    }

                    public void stopping() {
                        scheduler[0].stop(0);            // stop launching; the run drains
                    }
                }).start();
        assertTrue(running.await(5, TimeUnit.SECONDS));
        backend.stop();
        assertTrue(Json.write(Metrics.snapshot()).contains("drainedJob"),
                "the run that finished in the drain was not recorded");
    }

    @Test
    @DisplayName("loopback is 127/8 as a numeric literal, ::1 or localhost -- not a name that starts 127.")
    void loopbackIsCheckedStrictly() {
        assertTrue(Backend.isLoopback("127.0.0.1"));
        assertTrue(Backend.isLoopback("127.1.2.3"));
        assertTrue(Backend.isLoopback("::1"));
        assertTrue(Backend.isLoopback("localhost"));
        assertFalse(Backend.isLoopback("127.backend.example"));
        assertFalse(Backend.isLoopback("127.0.0.1.evil.example"));
        assertFalse(Backend.isLoopback("127.0.0.256"));
        assertFalse(Backend.isLoopback("127.0.0"));
        assertFalse(Backend.isLoopback("10.0.0.1"));
        assertEquals("127.0.0.1", Backend.advertised(null));
        assertEquals("127.0.0.1", Backend.advertised("0.0.0.0"));
        assertEquals("[::1]", Backend.advertised("::1"));
        assertEquals("192.0.2.10", Backend.advertised("192.0.2.10"));
    }

    @Test
    @DisplayName("a JSON-RPC id that is an object, array or boolean is refused before the method runs")
    void unsupportedIdTypesAreRefused() throws Exception {
        final int[] calls = {0};
        com.codename1.backend.mcp.McpTool tool = new com.codename1.backend.mcp.McpTool() {
            public String name() {
                return "touch";
            }

            public String description() {
                return "counts calls";
            }

            public Map inputSchema() {
                Map schema = new LinkedHashMap();
                schema.put("type", "object");
                return schema;
            }

            public Object call(Map arguments) {
                calls[0]++;
                return "ok";
            }
        };
        Properties settings = new Properties();
        settings.setProperty(McpServer.ENABLED, "true");
        McpServer server = McpServer.fromConfig(Config.of(settings, "dev"), null, null,
                java.util.Collections.singletonList(tool));
        HttpServer.Response r = server.handle(new HttpServer.Request("POST", "/mcp",
                "HTTP/1.1", new LinkedHashMap(), "{\"jsonrpc\":\"2.0\",\"id\":{\"x\":1},"
                + "\"method\":\"tools/call\",\"params\":{\"name\":\"touch\","
                + "\"arguments\":{}}}"));
        String body = r.body != null && r.body.length > 0 ? new String(r.body, "UTF-8")
                : Json.write(r.deferredJson);
        assertTrue(body.contains("-32600") && body.contains("\"id\":null"), body);
        assertEquals(0, calls[0], "a request with an object id ran its tool");
    }

    @Test
    @DisplayName("backend_call reaches a listener bound to one address, IPv6 loopback included")
    void backendCallUsesTheBoundAddress() throws Exception {
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend backend;
        try {
            backend = Backend.builder(Config.of(settings, "dev")).quiet()
                    .application(new EmptyApplication())
                    .mcp(new com.codename1.backend.mcp.DevTools())
                    .handler(new HttpServer.Handler() {
                        public HttpServer.Response handle(HttpServer.Request request)
                                throws Exception {
                            return request.respond(200, "text/plain", "reached".getBytes("UTF-8"));
                        }
                    }).host("::1").start();
        } catch (IOException noIpv6) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "no IPv6 loopback here");
            return;
        }
        try {
            assertEquals("[::1]", backend.getListenAddress());
            HttpURLConnection c = (HttpURLConnection) new URL("http://[::1]:" + port + "/mcp")
                    .openConnection();
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("Accept", "application/json, text/event-stream");
            c.getOutputStream().write(("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":"
                    + "\"tools/call\",\"params\":{\"name\":\"backend_call\",\"arguments\":"
                    + "{\"method\":\"GET\",\"path\":\"/x\"}}}").getBytes("UTF-8"));
            String answer = read(c);
            assertTrue(answer.contains("reached") && answer.contains("\"isError\":false"),
                    answer);
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("request beans a @PreDestroy builds, however deep, are destroyed too")
    void requestBeansBuiltDuringDestroyAreDrained() throws Exception {
        final List ended = java.util.Collections.synchronizedList(new ArrayList());
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend backend = Backend.builder(Config.of(settings, "test")).quiet()
                .application(new EmptyApplication() {
                    public HttpServer.Handler[] create(Backend.Environment environment) {
                        return new HttpServer.Handler[] {new HttpServer.Handler() {
                            public HttpServer.Response handle(HttpServer.Request request) {
                                request.scopedBeans(1)[0] = "a";
                                return HttpServer.Response.text(200, "ok");
                            }
                        }};
                    }

                    public boolean tracksCurrentRequest() {
                        return true;
                    }

                    public void requestEnded(Object[] beans) {
                        String name = (String) beans[0];
                        ended.add(name);
                        // Its @PreDestroy uses a request bean nobody built yet.
                        String next = "a".equals(name) ? "b" : "b".equals(name) ? "c"
                                : "c".equals(name) ? "d" : null;
                        if (next != null) {
                            Backend.currentRequest().scopedBeans(1)[0] = next;
                        }
                    }
                }).start();
        try {
            assertEquals("ok", read(open(port, "/x")));
            long deadline = System.currentTimeMillis() + 5000;
            while(ended.size() < 4 && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
            assertEquals("[a, b, c, d]", String.valueOf(ended),
                    "a bean built while destroying another was never destroyed");
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("a JSON body a request bean owns is written before the bean is destroyed")
    void deferredJsonIsWrittenBeforeRequestBeansEnd() throws Exception {
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend backend = Backend.builder(Config.of(settings, "test")).quiet()
                .application(new EmptyApplication() {
                    public HttpServer.Handler[] create(Backend.Environment environment) {
                        return new HttpServer.Handler[] {new HttpServer.Handler() {
                            public HttpServer.Response handle(HttpServer.Request request) {
                                List owned = new ArrayList();
                                owned.add("kept");
                                request.scopedBeans(1)[0] = owned;
                                return request.respondJson(200, owned);
                            }
                        }};
                    }

                    public void requestEnded(Object[] beans) {
                        ((List) beans[0]).clear();      // a @PreDestroy that clears
                    }
                }).start();
        try {
            assertEquals("[\"kept\"]", read(open(port, "/x")));
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("a request finishing during a memory-store login does not undo its rotation")
    void concurrentRequestKeepsTheLoginsRotation() throws Exception {
        final CountDownLatch rotated = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend backend = Backend.builder(Config.of(settings, "test")).quiet()
                .application(new EmptyApplication() {
                    public HttpServer.Handler[] create(Backend.Environment environment) {
                        return new HttpServer.Handler[] {new HttpServer.Handler() {
                            public HttpServer.Response handle(HttpServer.Request request)
                                    throws Exception {
                                String t = request.getTarget();
                                if (t.startsWith("/in")) {
                                    request.getSession(true).setAttribute("visits", "1");
                                    return HttpServer.Response.text(200, "in");
                                }
                                if (t.startsWith("/login")) {
                                    HttpSession s = request.getSession(false);
                                    s.changeSessionId();          // fixation defence
                                    s.setAttribute("user", "ada");
                                    rotated.countDown();
                                    release.await(10, TimeUnit.SECONDS);
                                    return HttpServer.Response.text(200, "login");
                                }
                                HttpSession s = request.getSession(false);
                                return HttpServer.Response.text(200, s == null ? "none"
                                        : String.valueOf(s.getAttribute("user")));
                            }
                        }};
                    }
                }).start();
        try {
            HttpURLConnection in = open(port, "/in");
            assertEquals("in", read(in));
            String cookie = in.getHeaderField("Set-Cookie");
            final String old = cookie.substring(0, cookie.indexOf(';'));
            final int p = port;
            final String[] loginCookie = new String[1];
            Thread login = new Thread(new Runnable() {
                public void run() {
                    try {
                        HttpURLConnection c = open(p, "/login");
                        c.setRequestProperty("Cookie", old);
                        read(c);
                        loginCookie[0] = c.getHeaderField("Set-Cookie");
                    } catch (IOException err) {
                        loginCookie[0] = String.valueOf(err);
                    }
                }
            });
            login.start();
            assertTrue(rotated.await(10, TimeUnit.SECONDS));
            HttpURLConnection meanwhile = open(port, "/peek");   // the old cookie, mid-login
            meanwhile.setRequestProperty("Cookie", old);
            assertFalse("ada".equals(read(meanwhile)),
                    "the old id reached the session the login was authenticating");
            release.countDown();
            login.join(10000);
            assertNotNull(loginCookie[0], "the login's rotation was undone: no new cookie");
            String rotatedPair = loginCookie[0].substring(0, loginCookie[0].indexOf(';'));
            assertFalse(rotatedPair.equals(old), "the login announced the old id");
            HttpURLConnection attacker = open(port, "/me");
            attacker.setRequestProperty("Cookie", old);
            assertFalse("ada".equals(read(attacker)),
                    "the old, pre-login id carries the signed-in session");
            HttpURLConnection user = open(port, "/me");
            user.setRequestProperty("Cookie", rotatedPair);
            assertEquals("ada", read(user));
        } finally {
            release.countDown();
            backend.stop();
        }
    }

    @Test
    @DisplayName("a rotation whose save fails leaves the session and its beans under the old id")
    void failedRotationSaveIsUndone() throws Exception {
        final SessionStore inner = new Sessions().getStore();
        final boolean[] failRotation = new boolean[1];
        SessionStore store = new SessionStore() {
            public HttpSession load(String id) throws IOException {
                return inner.load(id);
            }

            public void save(HttpSession session, String previousId) throws IOException {
                if (previousId != null && failRotation[0]) {
                    throw new IOException("the store is down");
                }
                inner.save(session, previousId);
            }

            public void delete(String id) throws IOException {
                inner.delete(id);
            }

            public int purgeExpired(long now) throws IOException {
                return inner.purgeExpired(now);
            }

            public int size() {
                return inner.size();
            }
        };
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend backend = Backend.builder(Config.of(settings, "test")).quiet()
                .sessionStore(store)
                .application(new EmptyApplication() {
                    public HttpServer.Handler[] create(Backend.Environment environment) {
                        return new HttpServer.Handler[] {new HttpServer.Handler() {
                            public HttpServer.Response handle(HttpServer.Request request)
                                    throws Exception {
                                String t = request.getTarget();
                                if (t.startsWith("/in")) {
                                    request.getSession(true).scopedBeans(1)[0] = "cart";
                                    return HttpServer.Response.text(200, "in");
                                }
                                HttpSession s = request.getSession(false);
                                if (t.startsWith("/rotate")) {
                                    s.changeSessionId();
                                    s.scopedBeans(1);       // after the rotation: moves them
                                    return HttpServer.Response.text(200, "rotated");
                                }
                                return HttpServer.Response.text(200, s == null ? "none"
                                        : s.getId() + " " + s.scopedBeans(1)[0]);
                            }
                        }};
                    }
                }).start();
        try {
            HttpURLConnection in = open(port, "/in");
            assertEquals("in", read(in));
            String cookie = in.getHeaderField("Set-Cookie");
            String pair = cookie.substring(0, cookie.indexOf(';'));
            String id = pair.substring(pair.indexOf('=') + 1);
            failRotation[0] = true;
            HttpURLConnection rotate = open(port, "/rotate");
            rotate.setRequestProperty("Cookie", pair);
            assertEquals(500, rotate.getResponseCode());
            assertNull(rotate.getHeaderField("Set-Cookie"));
            failRotation[0] = false;
            HttpURLConnection me = open(port, "/me");
            me.setRequestProperty("Cookie", pair);
            assertEquals(id + " cart", read(me),
                    "the failed rotation left the session or its beans under an id nobody has");
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("an invalidated session's beans outlive the invalidating request while another uses them")
    void invalidatedBeansWaitForOtherUsers(@org.junit.jupiter.api.io.TempDir java.io.File dir)
            throws Exception {
        DataSource pool = DataSource.open(new java.io.File(dir, "inv.db").getAbsolutePath(),
                2, 5000, 10000);
        final List ended = new ArrayList();
        try {
            Sessions sessions = new Sessions(new EmptyApplication() {
                public void sessionEnded(Object[] beans) {
                    ended.add(beans);
                }
            });
            sessions.setStore(new Sessions.Jdbc(pool));
            long now = System.currentTimeMillis();
            HttpSession stored = new HttpSession("shared", now, now, 1800);
            stored.markNew();
            sessions.getStore().save(stored, null);
            HttpSession a = sessions.getStore().load("shared");
            HttpSession b = sessions.getStore().load("shared");
            a.owner = sessions;
            b.owner = sessions;
            HttpServer.Request ra = new HttpServer.Request("GET", "/", "HTTP/1.1",
                    new LinkedHashMap(), null);
            HttpServer.Request rb = new HttpServer.Request("GET", "/", "HTTP/1.1",
                    new LinkedHashMap(), null);
            sessions.enter(ra, a);
            sessions.enter(rb, b);
            Object[] beans = b.scopedBeans(1);
            beans[0] = "cart";
            a.invalidate();                              // a logout in request A
            sessions.finish(ra, a, HttpServer.Response.text(200, "a"));
            sessions.leave(ra);
            assertTrue(ended.isEmpty(), "request B's beans were destroyed under it");
            assertTrue(b.scopedBeans(1) == beans,
                    "request B was handed fresh beans under the deleted id");
            sessions.leave(rb);
            assertEquals(1, ended.size(), "the retired beans were never destroyed");
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("removing a shared gauge source waits for a read of it already running")
    void removingASourceWaitsForItsRead() throws Exception {
        final CountDownLatch reading = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        com.codename1.backend.metrics.Gauge.Source slow =
                new com.codename1.backend.metrics.Gauge.Source() {
                    public double read() {
                        reading.countDown();
                        try {
                            release.await(5, TimeUnit.SECONDS);
                        } catch (InterruptedException err) {
                            // test
                        }
                        return 1;
                    }
                };
        Metrics.addSource("test.drain.gauge", "", "", slow);
        Thread reader = new Thread(new Runnable() {
            public void run() {
                Metrics.snapshot();
            }
        });
        reader.start();
        assertTrue(reading.await(5, TimeUnit.SECONDS));
        final boolean[] removed = {false};
        Thread remover = new Thread(new Runnable() {
            public void run() {
                Metrics.removeSource("test.drain.gauge", slow);
                removed[0] = true;
            }
        });
        remover.start();
        Thread.sleep(300);
        assertFalse(removed[0], "the source was removed while its read was still running");
        release.countDown();
        remover.join(5000);
        assertTrue(removed[0]);
        reader.join(5000);
    }

    @Test
    @DisplayName("a gauge stuck in its callback delays only its own removal")
    void aStuckGaugeDelaysOnlyItself() throws Exception {
        final CountDownLatch reading = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        com.codename1.backend.metrics.Gauge.Source stuck =
                new com.codename1.backend.metrics.Gauge.Source() {
                    public double read() {
                        reading.countDown();
                        try {
                            release.await(10, TimeUnit.SECONDS);
                        } catch (InterruptedException err) {
                            // test
                        }
                        return 1;
                    }
                };
        com.codename1.backend.metrics.Gauge.Source quick =
                new com.codename1.backend.metrics.Gauge.Source() {
                    public double read() {
                        return 2;
                    }
                };
        Metrics.addSource("test.stuck.gauge", "", "", stuck);
        Metrics.addSource("test.quick.gauge", "", "", quick);
        Thread reader = new Thread(new Runnable() {
            public void run() {
                Metrics.snapshot();
            }
        });
        reader.start();
        try {
            assertTrue(reading.await(5, TimeUnit.SECONDS));
            long start = System.currentTimeMillis();
            Metrics.removeSource("test.quick.gauge", quick);
            assertTrue(System.currentTimeMillis() - start < 1000,
                    "another gauge's stuck read delayed this removal");
        } finally {
            release.countDown();
            reader.join(5000);
            Metrics.removeSource("test.stuck.gauge", stuck);
        }
    }

    @Test
    @DisplayName("a gauge without a name is refused, like every other instrument")
    void aNamelessGaugeIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Metrics.gauge("", "", "",
                new com.codename1.backend.metrics.Gauge.Source() {
                    public double read() {
                        return 1;
                    }
                }));
    }

    @Test
    @DisplayName("one named executor asked for two kinds of thread is refused, AUTO agrees with either")
    void anExecutorHasOneThreadKind() {
        Tasks.Registry registry = Tasks.open(null);
        try {
            Tasks.executor(registry, "reports", Tasks.PLATFORM);
            Tasks.executor(registry, "reports", Tasks.AUTO);
            assertThrows(IllegalStateException.class,
                    () -> Tasks.executor(registry, "reports", Tasks.VIRTUAL));
        } finally {
            Tasks.shutdown(registry, 0);
        }
    }

    @Test
    @DisplayName("a rotation a failed request cannot announce is undone, and its other changes kept")
    void anUnannouncedRotationKeepsTheChanges(@org.junit.jupiter.api.io.TempDir java.io.File dir)
            throws Exception {
        DataSource pool = DataSource.open(new java.io.File(dir, "undo.db").getAbsolutePath(),
                2, 5000, 10000);
        try {
            Sessions sessions = new Sessions();
            sessions.setStore(new Sessions.Jdbc(pool));
            long now = System.currentTimeMillis();
            HttpSession stored = new HttpSession("kept", now, now, 1800);
            stored.markNew();
            sessions.getStore().save(stored, null);
            HttpSession copy = sessions.getStore().load("kept");
            copy.owner = sessions;
            copy.setAttribute("cart", "3 items");
            String next = copy.changeSessionId();
            assertNull(sessions.finish(copy, null));     // the handler threw
            assertEquals("kept", copy.getId(), "the session kept an id the client never got");
            assertNull(sessions.getStore().load(next));
            assertEquals("3 items", sessions.getStore().load("kept").getAttribute("cart"),
                    "the failed request's other changes were lost with the rotation");
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("params that are not an object are Invalid params, not an empty call")
    void nonObjectParamsAreRefused() throws Exception {
        Properties settings = new Properties();
        settings.setProperty(McpServer.ENABLED, "true");
        McpServer server = McpServer.fromConfig(Config.of(settings, "dev"), null, null, null);
        HttpServer.Response r = server.handle(new HttpServer.Request("POST", "/mcp",
                "HTTP/1.1", new LinkedHashMap(), "{\"jsonrpc\":\"2.0\",\"id\":3,"
                + "\"method\":\"initialize\",\"params\":1}"));
        String body = r.body != null && r.body.length > 0 ? new String(r.body, "UTF-8")
                : Json.write(r.deferredJson);
        assertTrue(body.contains("-32602") && !body.contains("protocolVersion"), body);
    }

    @Test
    @DisplayName("tools/call with arguments that are not an object is refused, and the tool never runs")
    void nonObjectArgumentsAreRefused() throws Exception {
        final int[] calls = {0};
        com.codename1.backend.mcp.McpTool tool = new com.codename1.backend.mcp.McpTool() {
            public String name() {
                return "touch";
            }

            public String description() {
                return "counts calls";
            }

            public Map inputSchema() {
                Map schema = new LinkedHashMap();
                schema.put("type", "object");
                return schema;
            }

            public Object call(Map arguments) {
                calls[0]++;
                return "ok";
            }
        };
        Properties settings = new Properties();
        settings.setProperty(McpServer.ENABLED, "true");
        McpServer server = McpServer.fromConfig(Config.of(settings, "dev"), null, null,
                java.util.Collections.singletonList(tool));
        HttpServer.Response r = server.handle(new HttpServer.Request("POST", "/mcp",
                "HTTP/1.1", new LinkedHashMap(), "{\"jsonrpc\":\"2.0\",\"id\":6,"
                + "\"method\":\"tools/call\",\"params\":{\"name\":\"touch\","
                + "\"arguments\":[1]}}"));
        String body = r.body != null && r.body.length > 0 ? new String(r.body, "UTF-8")
                : Json.write(r.deferredJson);
        assertTrue(body.contains("-32602"), body);
        assertEquals(0, calls[0], "a call with array arguments ran its tool");
    }

    @Test
    @DisplayName("a configured MCP or management path is compared in its canonical form")
    void configuredPathsAreCanonical() throws Exception {
        Properties settings = new Properties();
        settings.setProperty(McpServer.ENABLED, "true");
        settings.setProperty(McpServer.PATH, "/%6dcp");
        assertEquals("/mcp", McpServer.fromConfig(Config.of(settings, "dev"), null, null,
                null).getPath());
        Properties manage = new Properties();
        manage.setProperty(Management.PATH, "/%6danage");
        assertEquals("/manage", Config.of(manage, "dev").getRoutePath(Management.PATH,
                "/manage"));
    }

    private static Backend startAnswering(int port, final String text,
            com.codename1.backend.mcp.DevTools tools) throws Exception {
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        return Backend.builder(Config.of(settings, "dev")).quiet()
                .application(new EmptyApplication()).mcp(tools)
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request)
                            throws Exception {
                        return request.respond(200, "text/plain", text.getBytes("UTF-8"));
                    }
                }).start();
    }

    @Test
    @DisplayName("a stop() a handler makes tears the beans down only after that handler returns")
    void aHandlerStoppingTheServerFinishesFirst() throws Exception {
        final boolean[] handlerDone = {false};
        final boolean[] destroyedEarly = {false};
        final CountDownLatch destroyed = new CountDownLatch(1);
        final Backend[] self = new Backend[1];
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend backend = Backend.builder(Config.of(settings, "test")).quiet()
                .application(new EmptyApplication() {
                    public void stopped() {
                        synchronized (handlerDone) {
                            destroyedEarly[0] = !handlerDone[0];
                        }
                        destroyed.countDown();
                    }
                })
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request)
                            throws Exception {
                        self[0].stop();
                        Thread.sleep(200);                 // still using the beans
                        synchronized (handlerDone) {
                            handlerDone[0] = true;
                        }
                        return request.respond(200, "text/plain", "bye".getBytes("UTF-8"));
                    }
                }).start();
        self[0] = backend;
        assertEquals("bye", read(open(port, "/")));
        assertTrue(destroyed.await(10, TimeUnit.SECONDS), "the beans were never destroyed");
        assertFalse(destroyedEarly[0], "the beans were destroyed while the handler still ran");
        long deadline = System.currentTimeMillis() + 5000;
        while(!backend.isStopped() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(backend.isStopped());
    }

    @Test
    @DisplayName("values past a histogram's label keys do not make series of their own")
    void extraLabelValuesAreIgnored() {
        Histogram h = Metrics.histogram("test.extra.labels", "", "ms", null,
                new String[] {"route", null, null});
        h.record(1, "/a", "x", null);
        h.record(2, "/a", "y", null);
        int labelled = 0;
        for(Object p : h.points()) {
            Map attributes = (Map) ((Map) p).get("attributes");
            if(attributes != null && "/a".equals(attributes.get("route"))) {
                labelled++;
            }
        }
        assertEquals(1, labelled, "two series exported under one label set");
    }

    @Test
    @DisplayName("a new session whose first save fails has its beans destroyed and its row removed")
    void anUnsavedNewSessionIsDiscarded() throws Exception {
        final List ended = new ArrayList();
        final List deleted = new ArrayList();
        int port = freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        final SessionStore memory = new Sessions.Memory();
        final SessionStore failing = new SessionStore() {
            public HttpSession load(String id) throws IOException {
                return memory.load(id);
            }

            public void save(HttpSession session, String previousId) throws IOException {
                throw new IOException("the store refused");
            }

            public void delete(String id) throws IOException {
                deleted.add(id);
                memory.delete(id);
            }

            public int purgeExpired(long now) throws IOException {
                return memory.purgeExpired(now);
            }

            public int size() {
                return memory.size();
            }
        };
        Backend backend = Backend.builder(Config.of(settings, "test")).quiet()
                .sessionStore(failing)
                .application(new EmptyApplication() {
                    public void sessionEnded(Object[] beans) {
                        ended.add(beans);
                    }
                })
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request)
                            throws Exception {
                        HttpSession s = request.getSession(true);
                        s.scopedBeans(1)[0] = "cart";
                        s.setAttribute("n", "x");
                        return request.respond(200, "text/plain", "ok".getBytes("UTF-8"));
                    }
                }).start();
        try {
            assertEquals(500, open(port, "/").getResponseCode());
            assertEquals(1, ended.size(), "the unsaved session's beans were kept");
            assertEquals(1, deleted.size(), "a half-written row was left behind");
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("a session store is set before the server listens; replacing it later is refused")
    void sessionStoresAreSetBeforeUse() throws Exception {
        Sessions sessions = new Sessions();
        sessions.setStore(new Sessions.Memory());         // nothing used yet: fine
        sessions.find(null, true);
        assertThrows(IllegalStateException.class,
                () -> sessions.setStore(new Sessions.Memory()));
    }

    @Test
    @DisplayName("a builder runs one server at a time, and may start again once it stopped")
    void aBuilderRunsOneServerAtATime() throws Exception {
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(freePort()));
        Backend.Builder builder = Backend.builder(Config.of(settings, "test")).quiet()
                .application(new EmptyApplication());
        Backend first = builder.start();
        try {
            assertThrows(IllegalStateException.class, () -> builder.start());
        } finally {
            first.stop();
        }
        Backend again = builder.start();
        again.stop();
    }

    @Test
    @DisplayName("a copy that rotates after another request invalidated its session keeps the retired beans")
    void aStaleRotationUsesTheRetiredBeans(@org.junit.jupiter.api.io.TempDir java.io.File dir)
            throws Exception {
        DataSource pool = DataSource.open(new java.io.File(dir, "retired.db").getAbsolutePath(),
                2, 5000, 10000);
        try {
            Sessions sessions = new Sessions(new EmptyApplication());
            sessions.setStore(new Sessions.Jdbc(pool));
            long now = System.currentTimeMillis();
            HttpSession stored = new HttpSession("shared", now, now, 1800);
            stored.markNew();
            sessions.getStore().save(stored, null);
            HttpSession a = sessions.getStore().load("shared");
            HttpSession b = sessions.getStore().load("shared");
            a.owner = sessions;
            b.owner = sessions;
            HttpServer.Request ra = new HttpServer.Request("GET", "/", "HTTP/1.1",
                    new LinkedHashMap(), null);
            HttpServer.Request rb = new HttpServer.Request("GET", "/", "HTTP/1.1",
                    new LinkedHashMap(), null);
            sessions.enter(ra, a);
            sessions.enter(rb, b);
            Object[] beans = b.scopedBeans(1);
            a.invalidate();
            sessions.finish(ra, a, HttpServer.Response.text(200, "a"));
            sessions.leave(ra);
            b.changeSessionId();                        // B rotates its stale copy
            assertTrue(b.scopedBeans(1) == beans,
                    "a fresh set of beans was built under the rotated id");
            sessions.leave(rb);
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("recording with no label values is the unlabelled series, not a second empty one")
    void noLabelValuesIsThePlainSeries() {
        Histogram h = Metrics.histogram("test.plain.labels", "", "ms", null,
                new String[] {"route", null, null});
        h.record(1);
        h.record(2, null, null, null);
        assertEquals(1, h.points().size(), "two series exported with the same empty labels");
    }

    @Test
    @DisplayName("a database session's beans get the same expiry grace as its row")
    void jdbcBeansGetTheRowsGrace(@org.junit.jupiter.api.io.TempDir java.io.File dir)
            throws Exception {
        DataSource pool = DataSource.open(new java.io.File(dir, "grace.db").getAbsolutePath(),
                2, 5000, 10000);
        final List ended = new ArrayList();
        try {
            Sessions sessions = new Sessions(new EmptyApplication() {
                public void sessionEnded(Object[] beans) {
                    ended.add(beans);
                }
            });
            sessions.setStore(new Sessions.Jdbc(pool));
            long now = System.currentTimeMillis();
            // A minute's timeout, last used 65 seconds ago: past the timeout, inside
            // the 15-second touch interval the store still accepts the row for.
            HttpSession s = new HttpSession("graced", now - 65000, now - 65000, 60);
            s.owner = sessions;
            s.scopedBeans(1)[0] = "cart";
            sessions.purgeIfDue(sessions.getStore(), now);
            assertTrue(ended.isEmpty(), "the beans expired while the row was still accepted");
            sessions.purgeIfDue(sessions.getStore(), now + 61000);
            assertEquals(1, ended.size(), "the beans never expired");
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("label values that export alike are one series; reserved and empty keys are refused")
    void labelValuesAndKeysAreChecked() {
        Histogram h = Metrics.histogram("test.canonical.labels", "", "ms", null,
                new String[] {"code", null, null});
        h.record(1, Integer.valueOf(200), null, null);
        h.record(2, Long.valueOf(200), null, null);
        assertEquals(1, h.points().size(), "an Integer and a Long label made two series");
        assertThrows(IllegalArgumentException.class, () -> Metrics.histogram(
                "test.reserved.label", "", "ms", null,
                new String[] {"otel.metric.overflow", null, null}));
        assertThrows(IllegalArgumentException.class, () -> Metrics.histogram(
                "test.reserved.folded", "", "ms", null,
                new String[] {"otel_metric_overflow", null, null}));
        assertThrows(IllegalArgumentException.class,
                () -> com.codename1.backend.metrics.Gauge.point("", "x", 1));
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
