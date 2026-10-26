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

import com.codename1.backend.metrics.Counter;
import com.codename1.backend.metrics.Metrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheTest {

    /** Time moves only when the test says so. */
    static final class TestClock implements Cache.Clock {
        long now = 1000;

        @Override
        public long millis() {
            return now;
        }
    }

    /** Records every removal as "key=value:cause". */
    static final class Recorder implements Cache.RemovalListener<String, String> {
        final List<String> seen = new ArrayList<String>();

        @Override
        public synchronized void removed(String key, String value, int cause) {
            seen.add(key + "=" + value + ":" + cause);
        }
    }

    private static int names;

    private static String name() {
        return "test" + (names++);
    }

    private static Cache.Loader<String, String> upper(final AtomicInteger calls) {
        return new Cache.Loader<String, String>() {
            @Override
            public String load(String key) {
                calls.incrementAndGet();
                return key.toUpperCase(java.util.Locale.ROOT);
            }
        };
    }

    @Test
    @DisplayName("a value is loaded once and then answered from the cache")
    void loadsOnce() {
        Cache<String, String> c = Cache.<String, String>builder(name()).build();
        AtomicInteger calls = new AtomicInteger();
        assertEquals("A", c.get("a", upper(calls)));
        assertEquals("A", c.get("a", upper(calls)));
        assertEquals(1, calls.get());
        assertEquals(1, c.hitCount());
        assertEquals(1, c.missCount());
        assertEquals(1, c.loadCount());
    }

    @Test
    @DisplayName("concurrent callers for one key share a single load")
    void concurrentLoadsAreDeduplicated() throws Exception {
        final Cache<String, String> c = Cache.<String, String>builder(name()).build();
        final AtomicInteger calls = new AtomicInteger();
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final Cache.Loader<String, String> slow = new Cache.Loader<String, String>() {
            @Override
            public String load(String key) throws Exception {
                calls.incrementAndGet();
                entered.countDown();
                release.await(10, TimeUnit.SECONDS);
                return "v";
            }
        };
        final String[] results = new String[8];
        Thread[] threads = new Thread[results.length];
        for (int i = 0; i < threads.length; i++) {
            final int at = i;
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    results[at] = c.get("k", slow);
                }
            });
            threads[i].start();
        }
        assertTrue(entered.await(10, TimeUnit.SECONDS));
        // Every caller has had time to find the load in flight before it ends.
        Thread.sleep(200);
        release.countDown();
        for (Thread t : threads) {
            t.join(10000);
        }
        assertEquals(1, calls.get());
        for (String r : results) {
            assertEquals("v", r);
        }
    }

    @Test
    @DisplayName("a failed load reaches every waiter, stores nothing, and the next call loads again")
    void failuresAreSharedAndNotCached() throws Exception {
        final Cache<String, String> c = Cache.<String, String>builder(name()).build();
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final Cache.Loader<String, String> failing = new Cache.Loader<String, String>() {
            @Override
            public String load(String key) throws Exception {
                entered.countDown();
                release.await(10, TimeUnit.SECONDS);
                throw new IOException("down");
            }
        };
        final Throwable[] waiterSaw = new Throwable[1];
        Thread first = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    c.get("k", failing);
                } catch (Throwable t) {
                    // The owner's failure; asserted through the waiter below.
                }
            }
        });
        first.start();
        assertTrue(entered.await(10, TimeUnit.SECONDS));
        Thread waiter = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    c.get("k", failing);
                } catch (Throwable t) {
                    waiterSaw[0] = t;
                }
            }
        });
        waiter.start();
        Thread.sleep(200);
        release.countDown();
        first.join(10000);
        waiter.join(10000);
        assertTrue(waiterSaw[0] instanceof Cache.LoadException, String.valueOf(waiterSaw[0]));
        assertTrue(waiterSaw[0].getCause() instanceof IOException);
        assertEquals(1, c.loadFailureCount());
        assertEquals(0, c.size());
        AtomicInteger calls = new AtomicInteger();
        assertEquals("K", c.get("k", upper(calls)));
        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("an unchecked loader exception is rethrown as it is")
    void runtimeExceptionsPassThrough() {
        Cache<String, String> c = Cache.<String, String>builder(name()).build();
        assertThrows(IllegalStateException.class, () -> c.get("k", key -> {
            throw new IllegalStateException("bad");
        }));
    }

    @Test
    @DisplayName("a null load is answered and not stored")
    void nullIsNotStored() {
        Cache<String, String> c = Cache.<String, String>builder(name()).build();
        assertNull(c.get("k", key -> null));
        assertEquals(0, c.size());
        assertEquals("x", c.get("k", key -> "x"));
    }

    @Test
    @DisplayName("a loader asking for its own key is refused instead of waiting for itself")
    void recursiveLoadIsRefused() {
        final Cache<String, String> c = Cache.<String, String>builder(name()).build();
        assertThrows(IllegalStateException.class, () -> c.get("k", key -> c.get("k", k -> "x")));
        // And the cache is usable afterwards: the refused load did not stay in flight.
        assertEquals("y", c.get("k", key -> "y"));
    }

    @Test
    @DisplayName("expire-after-write removes the entry at its deadline and notifies EXPIRED")
    void expireAfterWrite() {
        TestClock clock = new TestClock();
        Recorder r = new Recorder();
        Cache<String, String> c = Cache.<String, String>builder(name())
                .expireAfterWriteMillis(100).clock(clock).removalListener(r).build();
        c.put("a", "1");
        clock.now += 99;
        assertEquals("1", c.getIfPresent("a"));
        clock.now += 1;
        assertNull(c.getIfPresent("a"));
        assertEquals("[a=1:" + Cache.EXPIRED + "]", r.seen.toString());
    }

    @Test
    @DisplayName("expire-after-access is pushed back by every read")
    void expireAfterAccess() {
        TestClock clock = new TestClock();
        Cache<String, String> c = Cache.<String, String>builder(name())
                .expireAfterAccessMillis(100).clock(clock).build();
        c.put("a", "1");
        for (int i = 0; i < 5; i++) {
            clock.now += 90;
            assertEquals("1", c.getIfPresent("a"));
        }
        clock.now += 100;
        assertNull(c.getIfPresent("a"));
    }

    @Test
    @DisplayName("a per-entry expiry overrides the default lifetime")
    void perEntryExpiry() {
        TestClock clock = new TestClock();
        Cache<String, String> c = Cache.<String, String>builder(name())
                .expireAfterWriteMillis(1000).clock(clock)
                .expiry((key, value) -> key.startsWith("short") ? 10 : Long.MAX_VALUE).build();
        c.put("short", "1");
        c.put("long", "2");
        clock.now += 10;
        assertNull(c.getIfPresent("short"));
        clock.now += 100000;
        assertEquals("2", c.getIfPresent("long"));
    }

    @Test
    @DisplayName("cleanUp and size remove every expired entry, not just the ones read")
    void cleanUpSweeps() {
        TestClock clock = new TestClock();
        Recorder r = new Recorder();
        Cache<String, String> c = Cache.<String, String>builder(name())
                .expireAfterWriteMillis(100).clock(clock).removalListener(r).build();
        c.put("a", "1");
        c.put("b", "2");
        clock.now += 100;
        assertEquals(0, c.size());
        assertEquals(2, r.seen.size());
    }

    @Test
    @DisplayName("above the size bound the least recently used entry goes, notified SIZE")
    void sizeEvictionIsLru() {
        Recorder r = new Recorder();
        Cache<String, String> c = Cache.<String, String>builder(name())
                .maximumSize(2).removalListener(r).build();
        c.put("a", "1");
        c.put("b", "2");
        c.getIfPresent("a");
        c.put("c", "3");
        assertNull(c.getIfPresent("b"));
        assertEquals("1", c.getIfPresent("a"));
        assertEquals("3", c.getIfPresent("c"));
        assertEquals("[b=2:" + Cache.SIZE + "]", r.seen.toString());
        assertEquals(1, c.evictionCount());
    }

    @Test
    @DisplayName("a weight bound evicts until the total fits, and an entry heavier than it is not kept")
    void weightEviction() {
        Cache<String, String> c = Cache.<String, String>builder(name())
                .maximumWeight(10, (key, value) -> value.length()).build();
        c.put("a", "12345");
        c.put("b", "12345");
        assertEquals(10, c.weight());
        c.put("c", "123");
        assertNull(c.getIfPresent("a"));
        assertEquals(8, c.weight());
        long before = c.evictionCount();
        c.put("huge", "12345678901");
        assertEquals(before + 1, c.evictionCount());
        assertNull(c.getIfPresent("huge"));
        assertEquals(8, c.weight());
        assertEquals(2, c.size());
    }

    @Test
    @DisplayName("put over an entry notifies REPLACED; invalidate and invalidateAll notify EXPLICIT")
    void replacementAndInvalidation() {
        Recorder r = new Recorder();
        Cache<String, String> c = Cache.<String, String>builder(name()).removalListener(r).build();
        c.put("a", "1");
        c.put("a", "2");
        c.put("b", "3");
        c.invalidate("a");
        c.invalidate("missing");
        c.invalidateAll();
        assertEquals("[a=1:" + Cache.REPLACED + ", a=2:" + Cache.EXPLICIT + ", b=3:"
                + Cache.EXPLICIT + "]", r.seen.toString());
        assertEquals(0, c.size());
    }

    @Test
    @DisplayName("a key invalidated during its load keeps the loaded value out of the cache")
    void invalidateDuringLoad() {
        final Cache<String, String> c = Cache.<String, String>builder(name()).build();
        assertEquals("stale", c.get("k", key -> {
            c.invalidate("k");
            return "stale";
        }));
        assertNull(c.getIfPresent("k"));
        // The load's own caller gets what it loaded; the cache keeps the put.
        assertEquals("stale", c.get("k", key -> {
            c.put("k", "fresh");
            return "stale";
        }));
        assertEquals("fresh", c.getIfPresent("k"));
    }

    @Test
    @DisplayName("a get after invalidate does not join the load the invalidation superseded")
    @Timeout(30)
    void getAfterInvalidateLoadsAfresh() throws Exception {
        final Cache<String, String> c = Cache.<String, String>builder(name()).build();
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        Thread first = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    c.get("k", key -> {
                        entered.countDown();
                        release.await(10, TimeUnit.SECONDS);
                        return "old";
                    });
                } catch (Throwable t) {
                    // Not under test.
                }
            }
        });
        first.start();
        assertTrue(entered.await(10, TimeUnit.SECONDS));
        c.invalidate("k");
        assertEquals("new", c.get("k", key -> "new"));
        release.countDown();
        first.join(10000);
        assertEquals("new", c.getIfPresent("k"));
    }

    @Test
    @DisplayName("an entry whose per-entry expiry is zero is never stored and replaces the old one")
    void zeroExpiryIsNotStored() {
        Cache<String, String> c = Cache.<String, String>builder(name()).maximumSize(1)
                .expiry((key, value) -> value.equals("dead") ? 0 : Long.MAX_VALUE).build();
        c.put("live", "live");
        c.put("other", "dead");
        assertEquals("live", c.getIfPresent("live"));
        assertEquals(1, c.size());
        assertEquals(0, c.evictionCount());
        c.put("live", "dead");
        assertNull(c.getIfPresent("live"));
        assertEquals(0, c.size());
    }

    @Test
    @DisplayName("a listener that throws affects neither the cache nor the caller")
    void throwingListener() {
        Cache<String, String> c = Cache.<String, String>builder(name()).maximumSize(1)
                .removalListener((key, value, cause) -> {
                    throw new RuntimeException("listener");
                }).build();
        c.put("a", "1");
        c.put("b", "2");
        c.invalidate("b");
        assertEquals(0, c.size());
        c.put("c", "3");
        assertEquals("3", c.getIfPresent("c"));
    }

    @Test
    @DisplayName("a weigher that throws during a load releases its waiters and stores nothing")
    void throwingWeigher() {
        Cache<String, String> c = Cache.<String, String>builder(name())
                .maximumWeight(10, (key, value) -> {
                    throw new IllegalArgumentException("weigher");
                }).build();
        assertThrows(IllegalArgumentException.class, () -> c.get("k", key -> "v"));
        assertEquals(0, c.size());
        assertEquals(1, c.loadFailureCount());
    }

    @Test
    @DisplayName("hits, misses, loads and evictions are published as metrics")
    void metrics() {
        String n = name();
        Cache<String, String> c = Cache.<String, String>builder(n).maximumSize(1).build();
        c.get("a", key -> "1");
        c.get("a", key -> "1");
        c.put("b", "2");
        Counter hits = Metrics.counter("cache." + n + ".hits", "", "{lookup}");
        assertSame(hits, Metrics.counter("cache." + n + ".hits", "", "{lookup}"));
        assertEquals(1, hits.get());
        assertEquals(1, Metrics.counter("cache." + n + ".misses", "", "{lookup}").get());
        assertEquals(1, Metrics.counter("cache." + n + ".loads", "", "{load}").get());
        assertEquals(1, Metrics.counter("cache." + n + ".evictions", "", "{entry}").get());
    }

    @Test
    @DisplayName("invalid settings are refused")
    void invalidSettings() {
        assertThrows(IllegalArgumentException.class, () -> Cache.builder(""));
        assertThrows(IllegalStateException.class, () -> Cache.<String, String>builder(name())
                .maximumSize(1).maximumWeight(1, (k, v) -> 1).build());
        assertThrows(IllegalArgumentException.class, () -> Cache.builder(name())
                .expireAfterWriteMillis(0));
        Cache<String, String> c = Cache.<String, String>builder(name()).build();
        assertThrows(NullPointerException.class, () -> c.put("a", null));
    }
}
