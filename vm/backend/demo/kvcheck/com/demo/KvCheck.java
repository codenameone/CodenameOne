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
package com.demo;

import com.codename1.backend.kv.KeyValueStore;
import com.codename1.backend.kv.KeyValueStores;
import com.codename1.backend.kv.RedisKeyValueStore;
import com.codename1.backend.security.ratelimit.KeyValueRateLimiter;
import java.util.ArrayList;
import java.util.List;

/// Exercises the key/value store against the in-process store and, when
/// `CN1_KVCHECK_URL` (or the first argument) names one, a real Redis-protocol
/// server -- Redis, Valkey or KeyDB. The same checks run on both, so a wire
/// client that disagrees with the reference semantics fails here, on the JVM
/// and on the native build alike.
public class KvCheck {
    private static int passed;
    private static final List<String> failures = new ArrayList<String>();

    public static void main(String[] args) throws Exception {
        run("memory:");
        String url = args.length > 0 ? args[0] : System.getenv("CN1_KVCHECK_URL");
        if (url != null && url.length() > 0) {
            run(url);
        }
        System.out.println("passed=" + passed + " failed=" + failures.size());
        for (String f : failures) {
            System.out.println("FAIL " + f);
        }
        System.out.println(failures.isEmpty() ? "KVCHECK OK" : "KVCHECK FAILED");
        if (!failures.isEmpty()) {
            System.exit(1);
        }
    }

    private static void run(String url) throws Exception {
        KeyValueStore kv = KeyValueStores.open(url, 5000, 4);
        String p = "kvcheck:" + Long.toString(System.nanoTime(), 36) + ":";
        String where = url.startsWith("memory:") ? "memory" : "redis";
        try {
            check(where + " ping", true, kv.ping());
            check(where + " shared", !url.startsWith("memory:"), kv.isShared());
            basics(where, kv, p);
            binary(where, kv, p);
            prefix(where, kv, p);
            concurrency(where, kv, p);
            pubsub(where, kv, p);
            rateLimit(where, kv, p);
            if (kv instanceof RedisKeyValueStore) {
                errors(where, (RedisKeyValueStore) kv, p);
            }
        } finally {
            kv.deleteByPrefix(p);
            kv.close();
        }
    }

    private static void basics(String w, KeyValueStore kv, String p) throws Exception {
        check(w + " missing get", null, str(kv.get(p + "none")));
        check(w + " missing ttl", -2L, kv.ttl(p + "none"));
        kv.set(p + "a", b("one"), 0);
        check(w + " get", "one", str(kv.get(p + "a")));
        check(w + " no expiry", -1L, kv.ttl(p + "a"));
        kv.set(p + "t", b("x"), 60000);
        long ttl = kv.ttl(p + "t");
        check(w + " ttl in range", true, ttl > 55000 && ttl <= 60000);
        kv.set(p + "short", b("x"), 150);
        Thread.sleep(400);
        check(w + " expired", null, str(kv.get(p + "short")));
        check(w + " setIfAbsent first", true, kv.setIfAbsent(p + "n", b("1"), 0));
        check(w + " setIfAbsent second", false, kv.setIfAbsent(p + "n", b("2"), 0));
        check(w + " setIfAbsent kept", "1", str(kv.get(p + "n")));
        check(w + " cas wrong", false, kv.compareAndSet(p + "n", b("9"), b("3"), 0));
        check(w + " cas right", true, kv.compareAndSet(p + "n", b("1"), b("3"), 0));
        check(w + " cas value", "3", str(kv.get(p + "n")));
        check(w + " cas create", true, kv.compareAndSet(p + "c", null, b("new"), 0));
        check(w + " cas create twice", false, kv.compareAndSet(p + "c", null, b("x"), 0));
        check(w + " cas delete", true, kv.compareAndSet(p + "c", b("new"), null, 0));
        check(w + " cas deleted", null, str(kv.get(p + "c")));
        check(w + " incr new", 5L, kv.increment(p + "i", 5, 60000));
        check(w + " incr again", 3L, kv.increment(p + "i", -2, 60000));
        long ittl = kv.ttl(p + "i");
        check(w + " incr ttl set on create", true, ittl > 0 && ittl <= 60000);
        check(w + " delete", true, kv.delete(p + "i"));
        check(w + " delete again", false, kv.delete(p + "i"));
    }

    private static void binary(String w, KeyValueStore kv, String p) throws Exception {
        byte[] raw = new byte[256];
        for (int i = 0; i < raw.length; i++) {
            raw[i] = (byte) i;
        }
        kv.set(p + "bin", raw, 0);
        check(w + " binary round trip", true, same(raw, kv.get(p + "bin")));
        byte[] big = new byte[1 << 20];
        for (int i = 0; i < big.length; i++) {
            big[i] = (byte) (i * 31);
        }
        kv.set(p + "big", big, 0);
        check(w + " 1MB round trip", true, same(big, kv.get(p + "big")));
        kv.set(p + "utf\u00e9\u4e2d", b("\u00e9\r\n\u4e2d"), 0);
        check(w + " utf8 key and value", "\u00e9\r\n\u4e2d", str(kv.get(p + "utf\u00e9\u4e2d")));
        kv.set(p + "empty", new byte[0], 0);
        byte[] e = kv.get(p + "empty");
        check(w + " empty value", 0, e == null ? -1 : e.length);
    }

    private static void prefix(String w, KeyValueStore kv, String p) throws Exception {
        // More than one SCAN page, and a glob character in the prefix that must
        // match only itself.
        for (int i = 0; i < 1200; i++) {
            kv.set(p + "pre*:" + i, b("v"), 0);
        }
        kv.set(p + "preX:keep", b("v"), 0);
        kv.set(p + "pre*", b("v"), 0);
        check(w + " deleteByPrefix count", 1200, kv.deleteByPrefix(p + "pre*:"));
        check(w + " deleteByPrefix spared glob look-alike", "v", str(kv.get(p + "preX:keep")));
        check(w + " deleteByPrefix spared shorter", "v", str(kv.get(p + "pre*")));
        check(w + " deleteByPrefix gone", null, str(kv.get(p + "pre*:7")));
    }

    private static void concurrency(String w, final KeyValueStore kv, final String p)
            throws Exception {
        final int threads = 8;
        final int each = 250;
        final Throwable[] error = new Throwable[1];
        Thread[] t = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            t[i] = new Thread() {
                public void run() {
                    try {
                        for (int n = 0; n < each; n++) {
                            kv.increment(p + "ctr", 1, 0);
                        }
                    } catch (Throwable err) {
                        synchronized (error) {
                            error[0] = err;
                        }
                    }
                }
            };
            t[i].start();
        }
        for (int i = 0; i < threads; i++) {
            t[i].join();
        }
        check(w + " concurrent increments error", null, error[0] == null ? null
                : String.valueOf(error[0]));
        check(w + " concurrent increments", (long) threads * each,
                kv.increment(p + "ctr", 0, 0));
        // A CAS race: exactly one of the contenders wins each round.
        kv.set(p + "race", b("0"), 0);
        final int[] wins = new int[1];
        for (int i = 0; i < threads; i++) {
            t[i] = new Thread() {
                public void run() {
                    try {
                        if (kv.compareAndSet(p + "race", b("0"), b("1"), 0)) {
                            synchronized (wins) {
                                wins[0]++;
                            }
                        }
                    } catch (Throwable err) {
                        synchronized (error) {
                            error[0] = err;
                        }
                    }
                }
            };
            t[i].start();
        }
        for (int i = 0; i < threads; i++) {
            t[i].join();
        }
        check(w + " one CAS winner", 1, wins[0]);
    }

    private static void pubsub(String w, KeyValueStore kv, String p) throws Exception {
        final List<String> got = new ArrayList<String>();
        KeyValueStore.Subscription sub = kv.subscribe(p + "ch",
                new KeyValueStore.MessageListener() {
                    public void onMessage(String channel, byte[] message) {
                        synchronized (got) {
                            got.add(channel + "=" + str(message));
                            got.notifyAll();
                        }
                    }
                });
        // A subscription on a remote server is live once SUBSCRIBE is
        // acknowledged; publish until the first message arrives.
        long end = System.currentTimeMillis() + 5000;
        synchronized (got) {
            while (got.isEmpty() && System.currentTimeMillis() < end) {
                got.wait(100);
                if (got.isEmpty()) {
                    kv.publish(p + "ch", b("hello"));
                }
            }
        }
        synchronized (got) {
            got.clear();
        }
        kv.publish(p + "ch", b("second"));
        kv.publish(p + "other", b("not mine"));
        synchronized (got) {
            end = System.currentTimeMillis() + 5000;
            while (got.isEmpty() && System.currentTimeMillis() < end) {
                got.wait(100);
            }
        }
        Thread.sleep(200);
        synchronized (got) {
            check(w + " pubsub delivered once", "[" + p + "ch=second]", got.toString());
        }
        sub.close();
        Thread.sleep(200);
        kv.publish(p + "ch", b("after close"));
        Thread.sleep(300);
        synchronized (got) {
            check(w + " no delivery after close", 1, got.size());
        }
    }

    private static void rateLimit(String w, KeyValueStore kv, String p) throws Exception {
        KeyValueRateLimiter rl = new KeyValueRateLimiter(kv, p + "login", 3, 60);
        int allowed = 0;
        for (int i = 0; i < 5; i++) {
            if (rl.tryAcquire("1.2.3.4")) {
                allowed++;
            }
        }
        check(w + " rate limit permits", 3, allowed);
        check(w + " other key unaffected", true, rl.tryAcquire("5.6.7.8"));
        long retry = rl.retryAfterSeconds("1.2.3.4");
        check(w + " retry-after in window", true, retry >= 1 && retry <= 60);
        rl.reset("1.2.3.4");
        check(w + " reset", true, rl.tryAcquire("1.2.3.4"));
        kv.deleteByPrefix("cn1:rl:");
    }

    private static void errors(String w, RedisKeyValueStore kv, String p) throws Exception {
        kv.set(p + "s", b("text"), 0);
        String code = "none";
        try {
            kv.call("LPUSH", p + "s", "x");
        } catch (com.codename1.backend.kv.RedisException err) {
            code = err.getCode();
        }
        check(w + " error reply code", "WRONGTYPE", code);
        // The connection the error came back on is still in step.
        check(w + " usable after error", "text", str(kv.get(p + "s")));
    }

    // ------------------------------------------------------------------ helpers

    private static byte[] b(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException(err.toString());
        }
    }

    private static String str(byte[] v) {
        if (v == null) {
            return null;
        }
        try {
            return new String(v, "UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException(err.toString());
        }
    }

    private static boolean same(byte[] a, byte[] c) {
        if (a == null || c == null || a.length != c.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != c[i]) {
                return false;
            }
        }
        return true;
    }

    private static void check(String what, Object expected, Object actual) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        if (ok) {
            passed++;
        } else {
            failures.add(what + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
