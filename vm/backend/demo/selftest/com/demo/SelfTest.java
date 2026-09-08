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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.codename1.backend.Base64Url;
import com.codename1.backend.Crypto;
import com.codename1.backend.Db;
import com.codename1.backend.DbPool;
import com.codename1.backend.Http;
import com.codename1.backend.Http1Date;
import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.Jwt;
import com.codename1.backend.ServerSocket;
import com.codename1.backend.Tcp;
import com.codename1.backend.Web;

/**
 * Unit tests for the backend runtime, run INSIDE a translated binary.
 *
 * They cannot be ordinary JUnit tests: every class here is backed by natives that
 * only exist in a translated program, so running them on a JVM would test nothing
 * that ships. The harness is deliberately tiny -- print a line per check, exit
 * non-zero if any failed -- and a JUnit test builds this, runs it, and reads the
 * result.
 */
public class SelfTest {
    private static int passed;
    private static final List failures = new ArrayList();

    /**
     * A saturated server must still answer a connection that arrives during the
     * saturation. This looks like an exotic property and is not: a worker that
     * keeps a busy keep-alive connection instead of handing it back makes the pool
     * size the hard limit on concurrent clients, and the failure is invisible in
     * every throughput number, because the connections that DO hold a worker are
     * served at full speed while the rest wait forever. The bug this pins was found
     * by a stray curl during a benchmark that was reporting 234k requests a second
     * at the time.
     *
     * Two workers and four connections that never stop sending, so there is no
     * arrangement in which a held connection is free to hold.
     */
    private static void fairness() throws Exception {
        HttpServer server = HttpServer.start("127.0.0.1", 0, 64, 2, new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) {
                return HttpServer.Response.text(200, "ok");
            }
        });
        final int port = server.getPort();
        final boolean[] stop = new boolean[1];
        Thread[] load = new Thread[4];
        try {
            for(int t = 0 ; t < load.length ; t++) {
                load[t] = new Thread(new Runnable() {
                    public void run() {
                        Tcp socket = null;
                        try {
                            socket = Tcp.connect("127.0.0.1", port, 2000);
                            byte[] req = ("GET /x HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n")
                                    .getBytes("UTF-8");
                            byte[] sink = new byte[1024];
                            while(!stop[0]) {
                                socket.write(req, 0, req.length);
                                if(socket.read(sink, 0, sink.length) <= 0) {
                                    return;
                                }
                            }
                        } catch (Exception ignored) {
                            // A loader that dies just reduces the pressure; the
                            // probe below is what decides the result.
                        } finally {
                            if(socket != null) {
                                socket.close();
                            }
                        }
                    }
                });
                load[t].start();
            }
            Thread.sleep(300);

            // The probe runs on its own thread so a server that never answers fails
            // the check instead of hanging the suite for the socket timeout.
            final String[] answer = new String[1];
            Thread probe = new Thread(new Runnable() {
                public void run() {
                    try {
                        answer[0] = new String(Http.get("127.0.0.1", port, "/probe").getBody(), "UTF-8");
                    } catch (Exception err) {
                        answer[0] = "failed: " + err;
                    }
                }
            });
            probe.start();
            probe.join(5000);
            check("a saturated server answers a new connection", "ok",
                    answer[0] == null ? "no response in 5s" : answer[0]);
        } finally {
            stop[0] = true;
            for(int t = 0 ; t < load.length ; t++) {
                load[t].join(2000);
            }
            server.stop(1000);
        }
    }

    /**
     * The foreign-backed read buffer: a byte[] whose storage on the translated
     * target is a C buffer the collector never allocated.
     *
     * The interesting assertion is the one after the GC cycles. An object outside
     * every heap page is not swept, and gcMarkObject rejects a pointer that does
     * not resolve, so it should come through untouched -- but "should" is the word
     * that makes this worth a test, because the failure mode is silent corruption
     * of a buffer every request reads through.
     */
    private static void foreignBuffer() throws Exception {
        byte[] buffer = ServerSocket.threadReadBuffer(4096);
        check("a read buffer is provided", "true", String.valueOf(buffer != null));
        check("it is at least the size asked for", "true",
                String.valueOf(buffer.length >= 4096));

        // Behaves as an ordinary array: bounds, element access, arraycopy.
        for(int iter = 0 ; iter < 4096 ; iter++) {
            buffer[iter] = (byte)(iter & 0x7f);
        }
        byte[] copy = new byte[16];
        System.arraycopy(buffer, 100, copy, 0, 16);
        check("arraycopy reads foreign storage", "100", String.valueOf(copy[0]));
        boolean threw = false;
        try {
            int ignored = buffer[buffer.length];
            threw = ignored == -1 && false;
        } catch (ArrayIndexOutOfBoundsException expected) {
            threw = true;
        }
        check("bounds are enforced on it", "true", String.valueOf(threw));

        // The same object, so a server reading through it allocates nothing.
        check("the same buffer comes back", "true",
                String.valueOf(ServerSocket.threadReadBuffer(4096) == buffer));

        // Survive collections. Allocate enough to force real cycles, then check
        // both the identity and every byte.
        for(int round = 0 ; round < 3 ; round++) {
            for(int iter = 0 ; iter < 20000 ; iter++) {
                byte[] garbage = new byte[256];
                garbage[0] = (byte)iter;
            }
            System.gc();
        }
        byte[] after = ServerSocket.threadReadBuffer(4096);
        check("identity survives collection", "true", String.valueOf(after == buffer));
        int damaged = -1;
        for(int iter = 0 ; iter < 4096 ; iter++) {
            if(buffer[iter] != (byte)(iter & 0x7f)) {
                damaged = iter;
                break;
            }
        }
        check("contents survive collection", "-1", String.valueOf(damaged));

        // A grow keeps one Java identity and moves only the storage.
        byte[] grown = ServerSocket.threadReadBuffer(65536);
        check("a grow still yields a usable buffer", "true",
                String.valueOf(grown != null && grown.length >= 65536));
        grown[65535] = 42;
        check("the grown tail is writable", "42", String.valueOf(grown[65535]));
    }

    public static void main(String[] args) throws Exception {
        crypto();
        jwt();
        base64Url();
        json();
        httpDate();
        database();
        pool();
        foreignBuffer();
        fairness();
        web();
        clientTls();

        System.out.println("passed=" + passed + " failed=" + failures.size());
        for(int iter = 0 ; iter < failures.size() ; iter++) {
            System.out.println("FAIL " + failures.get(iter));
        }
        System.out.println(failures.isEmpty() ? "SELFTEST OK" : "SELFTEST FAILED");
        if(!failures.isEmpty()) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------

    private static void crypto() throws Exception {
        // Known-answer tests, not round trips. A round trip passes just as happily
        // against a wrong-but-consistent implementation.
        check("sha256 known vector",
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                hex(Crypto.sha256(bytes("abc"))));

        // RFC 4231 test case 1.
        byte[] key = new byte[20];
        for(int iter = 0 ; iter < key.length ; iter++) {
            key[iter] = 0x0b;
        }
        check("hmac-sha256 RFC 4231 case 1",
                "b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7",
                hex(Crypto.hmacSha256(key, bytes("Hi There"))));

        byte[] a = Crypto.randomBytes(32);
        byte[] b = Crypto.randomBytes(32);
        check("randomBytes returns the requested length", "32", String.valueOf(a.length));
        check("randomBytes differ between calls", "true", String.valueOf(!hex(a).equals(hex(b))));

        check("constantTime equal", "true",
                String.valueOf(Crypto.equalsConstantTime(bytes("secret"), bytes("secret"))));
        check("constantTime differing", "false",
                String.valueOf(Crypto.equalsConstantTime(bytes("secret"), bytes("secret2"))));
        check("constantTime same length, one bit apart", "false",
                String.valueOf(Crypto.equalsConstantTime(bytes("secreta"), bytes("secretb"))));
        check("constantTime null", "false",
                String.valueOf(Crypto.equalsConstantTime(null, bytes("x"))));

        String stored = Crypto.hashPassword("hunter2");
        check("password hash is a pbkdf2 verifier", "true",
                String.valueOf(stored.startsWith("pbkdf2$")));
        check("password hash is not the password", "true",
                String.valueOf(stored.indexOf("hunter2") < 0));
        check("password verifies", "true", String.valueOf(Crypto.verifyPassword("hunter2", stored)));
        check("wrong password rejected", "false", String.valueOf(Crypto.verifyPassword("hunter3", stored)));
        check("empty password rejected", "false", String.valueOf(Crypto.verifyPassword("", stored)));
        // Two hashes of one password must differ, or the salt is not being used.
        check("hashes are salted", "true",
                String.valueOf(!stored.equals(Crypto.hashPassword("hunter2"))));
        check("malformed stored value rejected", "false",
                String.valueOf(Crypto.verifyPassword("hunter2", "not-a-hash")));
        check("truncated stored value rejected", "false",
                String.valueOf(Crypto.verifyPassword("hunter2", "pbkdf2$1000$abc")));
    }

    private static void jwt() throws Exception {
        byte[] secret = Crypto.randomBytes(32);
        Map claims = new LinkedHashMap();
        claims.put("sub", "shai");
        String token = Jwt.issue(claims, secret, 60);
        // JavaAPI's String has no split(); count the separators instead.
        check("token has two dots", "2", String.valueOf(countChar(token, '.')));

        Map verified = Jwt.verify(token, secret);
        check("subject survives", "shai", String.valueOf(verified.get("sub")));
        check("expiry is set", "true", String.valueOf(verified.get("exp") instanceof Number));

        checkThrows("a tampered signature is rejected", new Body() {
            public void run() throws Exception {
                byte[] s = Crypto.randomBytes(32);
                Map c = new LinkedHashMap();
                c.put("sub", "shai");
                String t = Jwt.issue(c, s, 60);
                // Flip a character in the MIDDLE of the signature. Flipping the
                // last one used to decode to identical bytes, because the trailing
                // bits of a base64 group are padding -- which is why the decoder
                // now rejects a non-canonical encoding.
                int at = t.length() - 10;
                char replacement = t.charAt(at) == 'A' ? 'B' : 'A';
                Jwt.verify(t.substring(0, at) + replacement + t.substring(at + 1), s);
            }
        });
        checkThrows("a token signed with another key is rejected", new Body() {
            public void run() throws Exception {
                Map c = new LinkedHashMap();
                c.put("sub", "shai");
                String t = Jwt.issue(c, Crypto.randomBytes(32), 60);
                Jwt.verify(t, Crypto.randomBytes(32));
            }
        });
        checkThrows("an expired token is rejected", new Body() {
            public void run() throws Exception {
                byte[] s = Crypto.randomBytes(32);
                Map c = new LinkedHashMap();
                c.put("sub", "shai");
                // Negative lifetime: issued already expired.
                Jwt.verify(Jwt.issue(c, s, -60), s);
            }
        });
        checkThrows("a short signing secret is refused", new Body() {
            public void run() throws Exception {
                Jwt.issue(new LinkedHashMap(), new byte[16], 60);
            }
        });
        checkThrows("alg=none is rejected", new Body() {
            public void run() throws Exception {
                // The classic JWT hole: a verifier that reads its algorithm out of
                // the token it is checking accepts this.
                String header = Base64Url.encode(bytes("{\"alg\":\"none\",\"typ\":\"JWT\"}"));
                String payload = Base64Url.encode(bytes("{\"sub\":\"attacker\",\"exp\":9999999999}"));
                Jwt.verify(header + "." + payload + ".", Crypto.randomBytes(32));
            }
        });
        checkThrows("a malformed token is rejected", new Body() {
            public void run() throws Exception {
                Jwt.verify("not.a.token", Crypto.randomBytes(32));
            }
        });

        check("bearer is extracted", "abc", String.valueOf(Jwt.bearer("Bearer abc")));
        check("bearer is case-insensitive", "abc", String.valueOf(Jwt.bearer("bearer abc")));
        check("a non-bearer header yields null", "null", String.valueOf(Jwt.bearer("Basic abc")));
        check("a null header yields null", "null", String.valueOf(Jwt.bearer(null)));
    }

    private static void base64Url() throws Exception {
        check("encodes without padding", "SGVsbG8", Base64Url.encode(bytes("Hello")));
        check("one leftover byte", "SGU", Base64Url.encode(bytes("He")));
        check("two leftover bytes", "SGVs", Base64Url.encode(bytes("Hel")));
        check("round trips", "Hello, world",
                new String(Base64Url.decode(Base64Url.encode(bytes("Hello, world"))), "UTF-8"));
        // The url alphabet: '-' and '_' where standard base64 has '+' and '/'.
        byte[] high = new byte[]{(byte)0xfb, (byte)0xff, (byte)0xbf};
        check("uses the url alphabet", "true",
                String.valueOf(Base64Url.encode(high).indexOf('+') < 0
                        && Base64Url.encode(high).indexOf('/') < 0));
        check("decodes the url alphabet", "fbffbf", hex(Base64Url.decode(Base64Url.encode(high))));
        // A base64 group is 2, 3 or 4 characters; a single leftover encodes nothing.
        check("rejects a lone trailing character", "null",
                String.valueOf(Base64Url.decode("SGVsbG8AA")));
        check("rejects a character outside the alphabet", "null",
                String.valueOf(Base64Url.decode("SGVs*G8")));
        check("empty round trips", "0", String.valueOf(Base64Url.decode("").length));
        // Non-canonical: "SGVsbG9" leaves bits set that a canonical encoder would
        // have left zero, so several strings would decode alike.
        check("rejects a non-canonical encoding", "null",
                String.valueOf(Base64Url.decode("SGVsbG9")));
        check("accepts the canonical form of the same bytes", "5",
                String.valueOf(Base64Url.decode("SGVsbG8").length));
    }

    private static void json() throws Exception {
        Map parsed = Json.parseObject("{\"a\":1,\"b\":\"two\",\"c\":true,\"d\":null,\"e\":1.5}");
        // Integers must stay integers: a long round-tripped through double loses
        // precision above 2^53, and ids are exactly the values that get large.
        check("integers parse as Long", "true", String.valueOf(parsed.get("a") instanceof Long));
        check("reals parse as Double", "true", String.valueOf(parsed.get("e") instanceof Double));
        check("strings parse", "two", String.valueOf(parsed.get("b")));
        check("booleans parse", "true", String.valueOf(parsed.get("c")));
        check("nulls parse", "null", String.valueOf(parsed.get("d")));

        check("large integers keep precision", "9007199254740993",
                String.valueOf(Json.parseObject("{\"n\":9007199254740993}").get("n")));

        Map nested = Json.parseObject("{\"o\":{\"p\":[1,2,{\"q\":\"r\"}]}}");
        Map inner = (Map)nested.get("o");
        List list = (List)inner.get("p");
        check("nesting survives", "3", String.valueOf(list.size()));
        check("objects inside arrays survive", "r",
                String.valueOf(((Map)list.get(2)).get("q")));

        check("escapes decode", "a\"b\\c\nd",
                String.valueOf(Json.parseObject("{\"s\":\"a\\\"b\\\\c\\nd\"}").get("s")));
        check("unicode escapes decode", "\u00e9",
                String.valueOf(Json.parseObject("{\"s\":\"\\u00e9\"}").get("s")));

        // RFC 8259 requires anything below U+0020 to arrive escaped. Accepting a
        // literal one let this parser and whatever validates upstream disagree
        // about where the string ended.
        boolean refusedControl = false;
        try {
            Json.parseObject("{\"s\":\"a\nb\"}");
        } catch (Exception expected) {
            refusedControl = true;
        }
        check("a raw control character in a string is refused", "true",
                String.valueOf(refusedControl));
        check("the same character escaped is accepted", "a\nb",
                String.valueOf(Json.parseObject("{\"s\":\"a\\nb\"}").get("s")));

        Map out = new LinkedHashMap();
        out.put("q", "a\"b");
        out.put("n", new Long(5));
        check("writing escapes quotes", "{\"q\":\"a\\\"b\",\"n\":5}", Json.write(out));
        check("writing a control character escapes it", "{\"q\":\"a\\nb\"}",
                Json.write(single("q", "a\nb")));
        // NaN and Infinity have no JSON form; emitting them produces a document no
        // parser will read back.
        check("NaN is written as null", "{\"q\":null}",
                Json.write(single("q", new Double(Double.NaN))));

        checkThrows("trailing content is rejected", new Body() {
            public void run() throws Exception {
                Json.parse("{\"a\":1} junk");
            }
        });
        checkThrows("an unterminated string is rejected", new Body() {
            public void run() throws Exception {
                Json.parse("{\"a\":\"oops}");
            }
        });
        checkThrows("a missing value is rejected", new Body() {
            public void run() throws Exception {
                Json.parse("{\"a\":}");
            }
        });
        checkThrows("an array asked for as an object is rejected", new Body() {
            public void run() throws Exception {
                Json.parseObject("[1,2]");
            }
        });
    }

    private static void httpDate() throws Exception {
        // The example from the HTTP specification itself.
        check("formats the RFC example", "Sun, 06 Nov 1994 08:49:37 GMT",
                Http1Date.format(784111777000L));
        check("parses the RFC example", "784111777000",
                String.valueOf(Http1Date.parse("Sun, 06 Nov 1994 08:49:37 GMT")));
        check("epoch formats", "Thu, 01 Jan 1970 00:00:00 GMT", Http1Date.format(0));
        check("a leap day survives a round trip", "Sat, 29 Feb 2020 12:00:00 GMT",
                Http1Date.format(Http1Date.parse("Sat, 29 Feb 2020 12:00:00 GMT")));
        check("garbage yields -1", "-1", String.valueOf(Http1Date.parse("not a date")));
        check("null yields -1", "-1", String.valueOf(Http1Date.parse(null)));
    }

    private static void database() throws Exception {
        Db db = Db.open(":memory:");
        try {
            db.execute("CREATE TABLE t (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name TEXT, weight REAL, data BLOB, maybe TEXT)", null);
            db.execute("INSERT INTO t (name, weight, data, maybe) VALUES (?, ?, ?, ?)",
                    new Object[]{"first", new Double(1.5), bytes("blob-bytes"), null});
            check("lastInsertId", "1", String.valueOf(db.lastInsertId()));

            List rows = db.query("SELECT id, name, weight, data, maybe FROM t", null);
            check("one row", "1", String.valueOf(rows.size()));
            Map row = (Map)rows.get(0);
            check("integer column is Long", "true", String.valueOf(row.get("id") instanceof Long));
            check("real column is Double", "true", String.valueOf(row.get("weight") instanceof Double));
            check("text column is String", "first", String.valueOf(row.get("name")));
            check("blob column is byte[]", "true", String.valueOf(row.get("data") instanceof byte[]));
            check("blob round trips", "blob-bytes", new String((byte[])row.get("data"), "UTF-8"));
            check("null column is null", "null", String.valueOf(row.get("maybe")));

            // The reason parameters are bound and never interpolated.
            db.execute("INSERT INTO t (name) VALUES (?)",
                    new Object[]{"bobby'); DROP TABLE t; --"});
            check("an injection attempt is stored as data",
                    "2", String.valueOf(db.query("SELECT id FROM t", null).size()));

            check("changes are counted", "2",
                    String.valueOf(db.execute("UPDATE t SET weight = 9.0", null)));

            // A transaction that throws must leave nothing behind.
            int before = db.query("SELECT id FROM t", null).size();
            boolean threw = false;
            try {
                db.transaction(new Db.Work() {
                    public Object run(Db conn) throws Exception {
                        conn.execute("INSERT INTO t (name) VALUES (?)", new Object[]{"doomed"});
                        throw new IllegalStateException("deliberate");
                    }
                });
            } catch (IllegalStateException err) {
                threw = true;
            }
            check("the transaction body's failure propagates", "true", String.valueOf(threw));
            check("the failed transaction rolled back", String.valueOf(before),
                    String.valueOf(db.query("SELECT id FROM t", null).size()));

            // And one that returns must commit.
            Object result = db.transaction(new Db.Work() {
                public Object run(Db conn) throws Exception {
                    conn.execute("INSERT INTO t (name) VALUES (?)", new Object[]{"kept"});
                    return "done";
                }
            });
            check("the transaction returns its value", "done", String.valueOf(result));
            check("the committed row is there", String.valueOf(before + 1),
                    String.valueOf(db.query("SELECT id FROM t", null).size()));

            checkThrows("bad SQL is reported", new Body() {
                public void run() throws Exception {
                    Db d = Db.open(":memory:");
                    try {
                        d.execute("SELECT * FROM no_such_table", null);
                    } finally {
                        d.close();
                    }
                }
            });
        } finally {
            db.close();
        }

        checkThrows("using a closed database is refused", new Body() {
            public void run() throws Exception {
                Db d = Db.open(":memory:");
                d.close();
                d.query("SELECT 1", null);
            }
        });
    }

    private static void pool() throws Exception {
        checkThrows("an in-memory database cannot be pooled", new Body() {
            public void run() throws Exception {
                // Each connection would get its own private database, so every
                // caller would see a different one.
                DbPool.open(":memory:", 2, 1000);
            }
        });

        String path = System.getenv("CN1_SELFTEST_DB");
        if(path == null) {
            note("pool concurrency skipped: set CN1_SELFTEST_DB to a writable path");
            return;
        }
        final DbPool pool = DbPool.open(path, 4, 5000);
        try {
            Db setup = pool.borrow();
            setup.execute("DROP TABLE IF EXISTS counter", null);
            setup.execute("CREATE TABLE counter (id INTEGER PRIMARY KEY AUTOINCREMENT, who TEXT)", null);
            pool.release(setup);

            final int threads = 8;
            final int each = 25;
            final int[] failed = new int[1];
            Thread[] workers = new Thread[threads];
            for(int t = 0 ; t < threads ; t++) {
                final String who = "worker-" + t;
                workers[t] = new Thread(new Runnable() {
                    public void run() {
                        for(int i = 0 ; i < each ; i++) {
                            try {
                                pool.inTransaction(new Db.Work() {
                                    public Object run(Db db) throws Exception {
                                        db.execute("INSERT INTO counter (who) VALUES (?)",
                                                new Object[]{who});
                                        return null;
                                    }
                                });
                            } catch (Exception err) {
                                synchronized(failed) {
                                    failed[0]++;
                                }
                            }
                        }
                    }
                });
                workers[t].start();
            }
            for(int t = 0 ; t < threads ; t++) {
                workers[t].join();
            }
            Db check = pool.borrow();
            List rows = check.query("SELECT COUNT(*) AS c FROM counter", null);
            long count = ((Number)((Map)rows.get(0)).get("c")).longValue();
            List distinct = check.query("SELECT COUNT(DISTINCT who) AS c FROM counter", null);
            long writers = ((Number)((Map)distinct.get(0)).get("c")).longValue();
            pool.release(check);
            check("every pooled write landed", String.valueOf(threads * each), String.valueOf(count));
            check("every worker got a connection", String.valueOf(threads), String.valueOf(writers));
            check("no pooled transaction failed", "0", String.valueOf(failed[0]));
        } finally {
            pool.close();
        }
    }

    private static void web() throws Exception {
        checkThrows("a null URL is refused", new Body() {
            public void run() throws Exception {
                Web.get(null);
            }
        });
        checkThrows("an unresolvable host fails rather than returning a status", new Body() {
            public void run() throws Exception {
                Web.get("https://this-host-does-not-exist.invalid/");
            }
        });
        if(System.getenv("CN1_SELFTEST_NETWORK") == null) {
            note("network checks skipped: set CN1_SELFTEST_NETWORK=1 to run them");
            return;
        }
        Web.Result ok = Web.get("https://api.github.com/zen");
        check("an https GET succeeds", "true", String.valueOf(ok.isSuccess()));
        check("the body arrives", "true", String.valueOf(ok.getBodyAsString().length() > 0));
        checkThrows("an expired certificate is rejected", new Body() {
            public void run() throws Exception {
                // Verification being ON is the whole reason to use a TLS library.
                Web.get("https://expired.badssl.com/");
            }
        });
    }

    // ------------------------------------------------------------------

    /**
     * Outbound TLS as an upgrade of a connected socket -- the shape the database
     * clients need, and the one Web.get (libcurl on the native target) does not
     * exercise.
     *
     * Both halves matter. The positive check proves the handshake completes and
     * bytes flow; the negative one proves the certificate is actually VERIFIED,
     * which is the part that fails open. An unverified TLS connection looks
     * exactly like a verified one until someone is in the middle.
     */
    private static void clientTls() throws Exception {
        if(System.getenv("CN1_SELFTEST_NETWORK") == null) {
            note("outbound TLS checks skipped: set CN1_SELFTEST_NETWORK=1 to run them");
            return;
        }
        Tcp plain = Tcp.connect("api.github.com", 443, 10000);
        try {
            check("a fresh socket is not secure", "false", String.valueOf(plain.isSecure()));
            plain.startTls("api.github.com");
            check("the socket is secure after the upgrade", "true",
                    String.valueOf(plain.isSecure()));
            byte[] request = bytes("GET /zen HTTP/1.0\r\nHost: api.github.com\r\n"
                    + "User-Agent: cn1-backend-selftest\r\nConnection: close\r\n\r\n");
            plain.write(request, 0, request.length);
            String response = readAll(plain);
            check("the encrypted response is an HTTP one", "true",
                    String.valueOf(response.startsWith("HTTP/1.")));
        } finally {
            plain.close();
        }

        // The name on the certificate has to be checked, not just its chain. This
        // connects to a host that HAS a valid certificate and asks for a different
        // name, so only the name check can reject it.
        final Tcp mismatched = Tcp.connect("api.github.com", 443, 10000);
        try {
            checkThrows("a certificate for another host is rejected", new Body() {
                public void run() throws Exception {
                    mismatched.startTls("example.invalid");
                }
            });
        } finally {
            mismatched.close();
        }

        final Tcp expired = Tcp.connect("expired.badssl.com", 443, 10000);
        try {
            checkThrows("an expired certificate is rejected on an upgraded socket", new Body() {
                public void run() throws Exception {
                    expired.startTls("expired.badssl.com");
                }
            });
        } finally {
            expired.close();
        }
    }

    /** Reads to end of stream. Only used on the small self-test responses. */
    private static String readAll(Tcp connection) throws Exception {
        StringBuilder out = new StringBuilder();
        byte[] buffer = new byte[4096];
        while(true) {
            int n = connection.read(buffer, 0, buffer.length);
            if(n <= 0) {
                return out.toString();
            }
            out.append(new String(buffer, 0, n, "UTF-8"));
        }
    }

    private interface Body {
        void run() throws Exception;
    }

    private static void check(String name, String expected, String actual) {
        if(expected.equals(actual)) {
            passed++;
        } else {
            failures.add(name + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void checkThrows(String name, Body body) {
        try {
            body.run();
            failures.add(name + ": expected an exception, none was thrown");
        } catch (Exception expected) {
            passed++;
        }
    }

    private static void note(String message) {
        System.out.println("NOTE " + message);
    }

    private static Map single(String key, Object value) {
        Map out = new LinkedHashMap();
        out.put(key, value);
        return out;
    }

    private static byte[] bytes(String value) {
        try {
            return value.getBytes("UTF-8");
        } catch (Exception err) {
            return new byte[0];
        }
    }

    private static int countChar(String value, char c) {
        int count = 0;
        for(int iter = 0 ; iter < value.length() ; iter++) {
            if(value.charAt(iter) == c) {
                count++;
            }
        }
        return count;
    }

    private static String hex(byte[] data) {
        if(data == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder();
        for(int iter = 0 ; iter < data.length ; iter++) {
            int v = data[iter] & 0xff;
            out.append("0123456789abcdef".charAt(v >>> 4));
            out.append("0123456789abcdef".charAt(v & 15));
        }
        return out.toString();
    }
}
