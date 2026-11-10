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

import com.codename1.backend.Config;
import com.codename1.backend.DataSource;
import com.codename1.backend.messaging.Message;
import com.codename1.backend.messaging.MessageHandler;
import com.codename1.backend.messaging.MessageTransport;
import com.codename1.backend.messaging.Messaging;
import com.codename1.backend.messaging.amqp.AmqpTransport;
import com.codename1.backend.messaging.kafka.KafkaTransport;
import com.codename1.backend.messaging.redis.RedisStreamTransport;
import com.codename1.backend.messaging.Subscription;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/// Exercises the messaging transports: the in-process queue, the database
/// queue over SQLite, and -- when `CN1_MSGCHECK_DB` names one -- PostgreSQL or
/// MySQL, then whatever broker `CN1_MSGCHECK_URL` names. The same checks run on
/// every transport, on the JVM and on the native build alike.
public class MsgCheck {
    private static int passed;
    private static final List<String> failures = new ArrayList<String>();

    public static void main(String[] args) throws Exception {
        // What the generated wiring does for a scheme application.properties names.
        Messaging.register("redis", new RedisStreamTransport.Factory());
        Messaging.register("amqp", new AmqpTransport.Factory());
        Messaging.register("kafka", new KafkaTransport.Factory());
        run("memory:", null);
        String file = "msgcheck-" + Long.toString(System.nanoTime(), 36) + ".db";
        run("db:", file);
        String db = System.getenv("CN1_MSGCHECK_DB");
        if (db != null && db.length() > 0) {
            run("db:", db);
        }
        String url = args.length > 0 ? args[0] : System.getenv("CN1_MSGCHECK_URL");
        if (url != null && url.length() > 0) {
            run(url, null);
        }
        new java.io.File(file).delete();
        System.out.println("passed=" + passed + " failed=" + failures.size());
        for (String f : failures) {
            System.out.println("FAIL " + f);
        }
        System.out.println(failures.isEmpty() ? "MSGCHECK OK" : "MSGCHECK FAILED");
        System.exit(failures.isEmpty() ? 0 : 1);
    }

    private static void run(String url, String database) throws Exception {
        Properties p = new Properties();
        p.setProperty("cn1.messaging.url", url);
        p.setProperty("cn1.messaging.retry.backoff", "20");
        p.setProperty("cn1.messaging.db.poll", "20");
        DataSource ds = database == null ? null : DataSource.open(database, 8);
        String w = url + (database == null ? "" : " " + (database.indexOf("://") > 0
                ? database.substring(0, database.indexOf(':')) : "sqlite"));
        MessageTransport t = Messaging.open(Config.of(p, "dev"), ds);
        String q = "mc" + Long.toString(System.nanoTime(), 36);
        try {
            if (ds != null && database.indexOf("://") > 0) {
                ds.execute("DELETE FROM cn1_messages", new Object[0]);
            }
        } catch (java.io.IOException noTableYet) {
            // created by the first send
        }
        try {
            order(w, t, q + "o");
            headers(w, t, q + "h");
            binary(w, t, q + "b");
            competing(w, t, q + "c");
            retry(w, t, q + "r");
            deadLetter(w, t, q + "d");
            closed(w, t, q + "x");
        } finally {
            t.close();
            if (ds != null) {
                ds.close();
            }
        }
    }

    private static void order(String w, MessageTransport t, String q) throws Exception {
        // One key: Kafka orders a partition, and spreads unkeyed records over all.
        for (int i = 0; i < 50; i++) {
            t.send(new Message(q, "k", ("m" + i).getBytes("UTF-8"),
                    new LinkedHashMap<String, String>()));
        }
        final List<String> got = Collections.synchronizedList(new ArrayList<String>());
        Subscription s = t.subscribe(q, "g", 1, new MessageHandler() {
            public void handle(Message m) {
                got.add(m.getText());
            }
        });
        waitFor(got, 50);
        s.close();
        List<String> expected = new ArrayList<String>();
        for (int i = 0; i < 50; i++) {
            expected.add("m" + i);
        }
        check(w + " order", expected, new ArrayList<String>(got));
    }

    private static void headers(String w, MessageTransport t, String q) throws Exception {
        Map<String, String> h = new LinkedHashMap<String, String>();
        h.put("trace", "abc");
        h.put("content-type", "text/plain");
        t.send(new Message(q, "k1", "body".getBytes("UTF-8"), h));
        final List<Message> got = Collections.synchronizedList(new ArrayList<Message>());
        Subscription s = t.subscribe(q, "g", 1, new MessageHandler() {
            public void handle(Message m) {
                got.add(m);
            }
        });
        waitFor(got, 1);
        s.close();
        Message m = got.isEmpty() ? null : got.get(0);
        check(w + " header", "abc", m == null ? null : m.getHeader("trace"));
        check(w + " content type", "text/plain", m == null ? null : m.getHeader("content-type"));
        check(w + " key", "k1", m == null ? null : m.getKey());
        check(w + " text", "body", m == null ? null : m.getText());
        check(w + " first attempt", Integer.valueOf(1), m == null ? null
                : Integer.valueOf(m.getAttempt()));
        check(w + " destination", q, m == null ? null : m.getDestination());
    }

    private static void binary(String w, MessageTransport t, String q) throws Exception {
        byte[] all = new byte[256];
        for (int i = 0; i < 256; i++) {
            all[i] = (byte) i;
        }
        t.send(new Message(q, all));
        t.send(new Message(q, new byte[0]));
        final List<byte[]> got = Collections.synchronizedList(new ArrayList<byte[]>());
        Subscription s = t.subscribe(q, "g", 1, new MessageHandler() {
            public void handle(Message m) {
                got.add(m.getPayload());
            }
        });
        waitFor(got, 2);
        s.close();
        // Two partitions need not deliver in send order: tell them apart by size.
        byte[] big = got.size() < 2 ? null : got.get(0).length > 0 ? got.get(0) : got.get(1);
        byte[] small = got.size() < 2 ? null : got.get(0).length > 0 ? got.get(1) : got.get(0);
        check(w + " binary", Boolean.TRUE, Boolean.valueOf(java.util.Arrays.equals(all, big)));
        check(w + " empty", Integer.valueOf(0), small == null ? null
                : Integer.valueOf(small.length));
    }

    private static void competing(String w, MessageTransport t, String q) throws Exception {
        final int n = 200;
        for (int i = 0; i < n; i++) {
            t.send(new Message(q, Integer.toString(i).getBytes("UTF-8")));
        }
        final List<String> got = Collections.synchronizedList(new ArrayList<String>());
        final Set<String> threads = Collections.synchronizedSet(new HashSet<String>());
        Subscription s = t.subscribe(q, "g", 4, new MessageHandler() {
            public void handle(Message m) throws Exception {
                got.add(m.getText());
                threads.add(Thread.currentThread().getName());
                Thread.sleep(1);
            }
        });
        waitFor(got, n);
        Thread.sleep(200);
        s.close();
        check(w + " competing count", Integer.valueOf(n), Integer.valueOf(got.size()));
        check(w + " competing distinct", Integer.valueOf(n),
                Integer.valueOf(new HashSet<String>(got).size()));
        check(w + " competing threads", Boolean.TRUE, Boolean.valueOf(threads.size() > 1));
    }

    private static void retry(String w, MessageTransport t, String q) throws Exception {
        t.send(new Message(q, "r".getBytes("UTF-8")));
        final List<Integer> attempts = Collections.synchronizedList(new ArrayList<Integer>());
        Subscription s = t.subscribe(q, "g", 1, new MessageHandler() {
            public void handle(Message m) {
                attempts.add(Integer.valueOf(m.getAttempt()));
                if (m.getAttempt() < 3) {
                    throw new IllegalStateException("not yet");
                }
            }
        });
        waitFor(attempts, 3);
        Thread.sleep(300);
        s.close();
        List<Integer> expected = new ArrayList<Integer>();
        expected.add(Integer.valueOf(1));
        expected.add(Integer.valueOf(2));
        expected.add(Integer.valueOf(3));
        check(w + " retry attempts", expected, new ArrayList<Integer>(attempts));
    }

    private static void deadLetter(String w, MessageTransport t, String q) throws Exception {
        t.send(new Message(q, "poison".getBytes("UTF-8")));
        final AtomicInteger tries = new AtomicInteger();
        Subscription s = t.subscribe(q, "g", 1, new MessageHandler() {
            public void handle(Message m) {
                tries.incrementAndGet();
                throw new IllegalArgumentException("bad payload");
            }
        });
        final List<Message> dead = Collections.synchronizedList(new ArrayList<Message>());
        Subscription d = t.subscribe(q + ".dlq", "g", 1, new MessageHandler() {
            public void handle(Message m) {
                dead.add(m);
            }
        });
        waitFor(dead, 1);
        Thread.sleep(300);
        s.close();
        d.close();
        Message m = dead.isEmpty() ? null : dead.get(0);
        check(w + " dlq tries", Integer.valueOf(3), Integer.valueOf(tries.get()));
        check(w + " dlq count", Integer.valueOf(1), Integer.valueOf(dead.size()));
        check(w + " dlq origin", q, m == null ? null : m.getHeader(Message.ORIGIN));
        check(w + " dlq error", "java.lang.IllegalArgumentException: bad payload",
                m == null ? null : m.getHeader(Message.ERROR));
        check(w + " dlq payload", "poison", m == null ? null : m.getText());
    }

    private static void closed(String w, MessageTransport t, String q) throws Exception {
        final List<String> got = Collections.synchronizedList(new ArrayList<String>());
        MessageHandler h = new MessageHandler() {
            public void handle(Message m) {
                got.add(m.getText());
            }
        };
        Subscription s = t.subscribe(q, "g", 2, h);
        s.close();
        t.send(new Message(q, "later".getBytes("UTF-8")));
        Thread.sleep(300);
        check(w + " closed takes nothing", Integer.valueOf(0), Integer.valueOf(got.size()));
        Subscription again = t.subscribe(q, "g", 1, h);
        waitFor(got, 1);
        again.close();
        check(w + " kept for the next", "later", got.isEmpty() ? null : got.get(0));
    }

    private static void waitFor(List<?> list, int size) throws InterruptedException {
        long end = System.currentTimeMillis() + 20000;
        while (list.size() < size && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
    }

    private static void check(String what, Object expected, Object actual) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            passed++;
        } else {
            failures.add(what + ": expected " + expected + " but was " + actual);
        }
    }
}
