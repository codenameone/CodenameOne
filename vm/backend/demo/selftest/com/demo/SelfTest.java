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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.codename1.backend.Base64Url;
import com.codename1.backend.ByteSink;
import com.codename1.backend.Crypto;
import com.codename1.backend.Database;
import com.codename1.backend.Db;
import com.codename1.backend.DbPool;
import com.codename1.backend.FileIo;
import com.codename1.backend.Http;
import com.codename1.backend.Reactor;
import com.codename1.backend.Http1Date;
import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.Jwt;
import com.codename1.backend.ServerSocket;
import com.codename1.backend.StaticFiles;
import com.codename1.backend.Tcp;
import com.codename1.backend.Tls;
import com.codename1.backend.VirtualThread;
import com.codename1.backend.Web;
import com.codename1.backend.aws.Aws;
import com.codename1.backend.aws.CredentialEndpointProbe;
import com.codename1.backend.aws.Credentials;
import com.codename1.backend.FileCountProbe;
import com.codename1.backend.aws.ExpiryProbe;
import com.codename1.backend.aws.S3;

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
        threadLocalInitialization();
        crypto();
        jwt();
        base64Url();
        json();
        httpDate();
        database();
        sharedTransactionsDoNotInterleave();
        pool();
        foreignBuffer();
        fairness();
        web();
        clientTls();
        rotatedCaBundlesAreReRead();

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

    private static void threadLocalInitialization() {
        final int[] attempts = new int[1];
        ThreadLocal<String> retry = new ThreadLocal<String>() {
            protected String initialValue() {
                if(++attempts[0] == 1) {
                    throw new IllegalStateException("first initialization fails");
                }
                return "ready";
            }
        };
        boolean threw = false;
        try {
            retry.get();
        } catch (IllegalStateException expected) {
            threw = true;
        }
        check("ThreadLocal propagates initializer failure", "true", String.valueOf(threw));
        check("ThreadLocal retries initialization", "ready", retry.get());
        check("ThreadLocal caches successful initialization", "ready", retry.get());
        check("ThreadLocal initializes twice after one failure", "2", String.valueOf(attempts[0]));
        retry.remove();
        check("ThreadLocal initializes after remove", "ready", retry.get());
        check("ThreadLocal remove resets initialization", "3", String.valueOf(attempts[0]));
        retry.set(null);
        check("ThreadLocal preserves an explicit null", "null", String.valueOf(retry.get()));
        check("ThreadLocal null does not reinitialize", "3", String.valueOf(attempts[0]));

        final int[] removals = new int[1];
        ThreadLocal<String> removesDuringInitialization = new ThreadLocal<String>() {
            protected String initialValue() {
                removals[0]++;
                remove();
                set("temporary");
                return "final";
            }
        };
        check("ThreadLocal installs after initializer removes entry", "final",
                removesDuringInitialization.get());
        check("ThreadLocal retains initializer result", "final",
                removesDuringInitialization.get());
        check("ThreadLocal does not repeat a successful initializer", "1",
                String.valueOf(removals[0]));
        ThreadLocal<String> overridesSet = new ThreadLocal<String>() {
            protected String initialValue() { return "initialized"; }
            public void set(String value) {
                throw new IllegalStateException("get must not invoke an overridden set");
            }
        };
        check("ThreadLocal initialization does not invoke overridden set", "initialized",
                overridesSet.get());
        overridesSet.remove();
        retry.remove();
        removesDuringInitialization.remove();
    }

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
        // A null password must not become an empty-password account: utf8(null) is
        // an empty array, so hashing one produced a verifier that "" satisfies.
        // Asserted on BOTH runtimes, because the two have separate Crypto arms.
        String nullOutcome;
        try {
            Crypto.hashPassword(null);
            nullOutcome = "accepted";
        } catch (IllegalArgumentException refused) {
            nullOutcome = "refused";
        }
        check("a null password is refused", "refused", nullOutcome);
        // A stored row with empty salt and hash decoded to two EMPTY arrays, not
        // nulls, so the null check let it through, pbkdf2 derived zero bytes and
        // comparing empty with empty was true: that row accepted every password.
        check("a degenerate stored hash accepts nothing", "false",
                String.valueOf(Crypto.verifyPassword("anything", "pbkdf2$1$$")));
        check("a short salt is refused too", "false",
                String.valueOf(Crypto.verifyPassword("anything", "pbkdf2$1$AA$AA")));
        // Two hashes of one password must differ, or the salt is not being used.
        check("hashes are salted", "true",
                String.valueOf(!stored.equals(Crypto.hashPassword("hunter2"))));
        check("malformed stored value rejected", "false",
                String.valueOf(Crypto.verifyPassword("hunter2", "not-a-hash")));
        check("truncated stored value rejected", "false",
                String.valueOf(Crypto.verifyPassword("hunter2", "pbkdf2$1000$abc")));

        // AN IMPLAUSIBLE ITERATION COUNT, rejected rather than computed. The count
        // is read out of the stored value, so whoever influences a row -- or an
        // application that hands this a value from somewhere it does not control --
        // otherwise names how long the login takes. The salt and hash here are the
        // right length, so the count is the only thing left to reject it.
        String hostile = "pbkdf2$" + Integer.MAX_VALUE + "$"
                + stored.substring(stored.indexOf('$', 7) + 1);
        long started = System.currentTimeMillis();
        boolean accepted = Crypto.verifyPassword("hunter2", hostile);
        long elapsed = System.currentTimeMillis() - started;
        // BOTH HALVES, because the answer alone does not distinguish them. An
        // unbounded count still ends in a hash that does not match, so this returns
        // false either way and a check on the boolean passes with the bound removed
        // -- measured, not supposed: the whole self-test went on passing while one
        // call sat in PBKDF2 for 175 of its 178 seconds. What the bound changes is
        // WHEN the false arrives. Thirty seconds because the gap being measured is
        // five times that, not because answering is expected to need any of it.
        check("an implausible iteration count is rejected", "false without computing",
                (accepted ? "true" : "false")
                        + (elapsed < 30000 ? " without computing"
                                           : " only after " + elapsed + "ms"));
        // And the ordinary count this server writes still verifies, so the bound
        // did not simply refuse everything.
        check("the stored count still verifies", "true",
                String.valueOf(Crypto.verifyPassword("hunter2", stored)));

        // AND THE HASH LENGTH, which is the other factor in the same product.
        // PBKDF2 runs the iteration count once per output BLOCK, and the output
        // length comes out of the stored value too -- so bounding the count alone
        // left a few kilobytes of hash multiplying it by a couple of hundred.
        // Eight kilobytes at the highest count still allowed is 256 blocks of ten
        // million rounds.
        //
        // Timed, not just answered, for the same reason as the count above: an
        // oversized hash will not match either way, so the boolean is false in
        // both versions and only the clock tells them apart.
        byte[] wideSalt = new byte[16];
        byte[] wideHash = new byte[8192];
        String oversized = "pbkdf2$10000000$" + Base64Url.encode(wideSalt)
                + "$" + Base64Url.encode(wideHash);
        long wideStarted = System.currentTimeMillis();
        boolean wideAccepted = Crypto.verifyPassword("hunter2", oversized);
        long wideElapsed = System.currentTimeMillis() - wideStarted;
        check("an oversized stored hash is rejected", "false without computing",
                (wideAccepted ? "true" : "false")
                        + (wideElapsed < 30000 ? " without computing"
                                               : " only after " + wideElapsed + "ms"));
    }

    private static void jwt() throws Exception {
        byte[] secret = Crypto.randomBytes(32);
        Map claims = new LinkedHashMap();
        claims.put("sub", "shai");
        String token = Jwt.issue(claims, secret, 60);
        // JavaAPI's String has no split(); count the separators instead.
        check("token has two dots", "2", String.valueOf(countChar(token, '.')));

        Map verified = Jwt.verify(token, secret);
        // issue() has always refused a secret under 32 bytes as forgeable; verify()
        // took any. A deployment configured with an empty one would have accepted a
        // signature anybody could compute over claims of their choosing, so the
        // dangerous half was the one that failed open.
        boolean refusedShortSecret = false;
        try {
            Jwt.verify(token, new byte[0]);
        } catch (Exception expected) {
            refusedShortSecret = true;
        }
        check("verifying with a short secret is refused", "true",
                String.valueOf(refusedShortSecret));
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
        // Padding has to be contiguous and at the end, and the bits it stands for
        // have to be zero. "AA=A" satisfied "final group, '=' at index 2" and then
        // took the 'A' after it as data, returning three bytes for a string no
        // encoder can produce; "AB==" gave a second spelling of a byte "AA=="
        // already spells, which a strict decoder must not accept.
        check("padding followed by data is refused", "true",
                String.valueOf(com.codename1.backend.Base64.decode("AA=A") == null));
        check("nonzero padding bits are refused", "true",
                String.valueOf(com.codename1.backend.Base64.decode("AB==") == null));
        check("ordinary padding still decodes", "1",
                String.valueOf(com.codename1.backend.Base64.decode("AA==").length));
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

    /**
     * The two JSON writers have to answer the same bytes. HTTP/1.1 writes through
     * the ByteSink one and HTTP/2 through the String one, so a disagreement means
     * one handler returns two different documents depending on which protocol the
     * client negotiated -- and nothing in either path would ever notice. Float
     * was the second such defect (byte[] was the first), which is why this
     * compares the writers rather than either one's output.
     */
    private static void bothJsonWritersAgree() throws Exception {
        Object[] values = new Object[] {
            Float.valueOf(1.2f), Float.valueOf(-0.1f), Float.valueOf(3.4e38f),
            Double.valueOf(1.2d), Double.valueOf(1e300), Long.valueOf(9007199254740993L),
            Integer.valueOf(-7), Boolean.TRUE, "text", new byte[] {1, 2, 3},
        };
        for(int iter = 0 ; iter < values.length ; iter++) {
            ByteSink sink = new ByteSink(64);
            Json.write(values[iter], sink);
            String viaSink = new String(sink.bytes(), 0, sink.length(), "UTF-8");
            check("both JSON writers agree on " + values[iter].getClass().getName(),
                    Json.write(values[iter]), viaSink);
        }
        // And the float keeps its OWN spelling rather than the double it widens to.
        check("a float is not widened", "1.2", Json.write(Float.valueOf(1.2f)));
    }

    /**
     * A malformed HTTP date must be NO date. Every field is read at a fixed
     * offset and handed to a civil-date routine that normalises whatever it is
     * given, so "99 Nov 9999 99:99:99" became a date far in the future and a
     * conditional request read it as newer than the file -- answering 304, with
     * no content, to a client that had nothing cached.
     */
    private static void malformedDatesAreNotDates() throws Exception {
        String[] bad = new String[] {
            "Sun, 99 Nov 9999 99:99:99 BAD",   // out of range, wrong suffix
            "Sun, 06 Nov 1994 08:49:37 UTC",   // IMF-fixdate is GMT
            "Sun, 06 Nov 1994 08:49:37",       // truncated
            "Sun, 31 Feb 1994 08:49:37 GMT",   // a day that does not exist
            "Sun, 06 Nov 1994 25:00:00 GMT",   // hour out of range
            "Sun, 06 Nov 9999 -1:-1:-1 GMT",   // negative fields fit the fixed width
            "Sun, 06 Nov 9999 +1:+1:+1 GMT",   // and so does a signed one
            "Sun,  6 Nov 1994 08:49:37 GMT",   // space-padded day, an asctime habit
            "Xxx, 06 Nov 9999 08:49:37 GMT",   // not a weekday at all
            "Mon, 06 Nov 1994 08:49:37 GMT",   // a weekday, but not THAT date's
            "Sunday, 06-Nov-94 08:49:37 GMT",  // RFC 850, deliberately unsupported
        };
        for(int iter = 0 ; iter < bad.length ; iter++) {
            check("a malformed date is refused: " + bad[iter], "-1",
                    String.valueOf(Http1Date.parse(bad[iter])));
        }
        // And the one real form still parses, round-tripping through the writer.
        long when = Http1Date.parse("Sun, 06 Nov 1994 08:49:37 GMT");
        check("a valid IMF-fixdate parses", "784111777000", String.valueOf(when));
        check("and formats back", "Sun, 06 Nov 1994 08:49:37 GMT", Http1Date.format(when));
    }

    /**
     * Protocol tokens are folded by hand, never with String.toLowerCase(), which
     * is locale sensitive and has no root-locale overload here. The values below
     * all contain an I, which is the character a Turkish locale folds to a
     * dotless i -- so a lookup keyed on the folded form stops matching and the
     * header, the extension or the scheme reads as absent with nothing thrown.
     */
    private static void asciiFoldingIsLocaleIndependent() throws Exception {
        // Jwt.bearer is the one such fold reachable from here; StaticFiles'
        // content type and the Web arms' header index are package-private, and
        // BackendHttpIntegrationTest exercises those over the wire instead.
        check("an upper-case bearer scheme is still a bearer scheme",
                "abc.def.ghi", String.valueOf(Jwt.bearer("BEARER abc.def.ghi")));
        check("a mixed-case one too",
                "abc.def.ghi", String.valueOf(Jwt.bearer("Bearer abc.def.ghi")));
        check("and the lower-case spelling is unchanged",
                "abc.def.ghi", String.valueOf(Jwt.bearer("bearer abc.def.ghi")));
        check("something that is not a bearer header is still refused",
                "null", String.valueOf(Jwt.bearer("Basic abc")));
    }

    /**
     * PATCH is where the two runtimes genuinely differ, and this says so rather
     * than pretending otherwise: the packaged one sends it, while Java SE's
     * HttpURLConnection refuses the verb outright on every JDK measured -- 8, 21
     * and 25 -- and the reflection trick usually reached for works only on 8.
     *
     * What BOTH must satisfy is that the developer is never left holding an
     * unexplained failure. Packaged, the request is attempted; locally, it fails
     * with a message that names the limitation and says the packaged binary can
     * do it. The JDK's own "Invalid HTTP method: PATCH" says none of that, and
     * that opaque failure is what this asserts is gone.
     */
    /**
     * An outbound header value cannot smuggle a second header.
     *
     * <p>The packaged arm passes headers to its native as ONE string with '\n'
     * between them, and the native splits on that byte and gives each line to
     * libcurl. So a value derived from untrusted input -- the bearerToken
     * getJson() and postJson() accept is exactly that -- could add a header the
     * caller never wrote: "abc\nX-Admin: true" becomes a second header on a
     * request the upstream trusts.
     *
     * <p>Asserted on BOTH arms with one expectation, which is the point. Leaving
     * it to HttpURLConnection on one side and libcurl on the other is how the two
     * end up disagreeing about the same call; both now refuse before either stack
     * is reached. No request is made -- the refusal happens first, so the URL here
     * is never contacted.
     */
    /**
     * The transport's own fields cannot come from a caller.
     *
     * <p>libcurl documents that a header handed to CURLOPT_HTTPHEADER REPLACES
     * the one it would have generated, and the body is configured separately --
     * so "Content-Length: 0" alongside a real body tells the upstream the request
     * ends where it does not, and a keep-alive peer reads the rest of that body
     * as the next request on the connection. Host is the routing half: overriding
     * it picks a different virtual host on the destination this code chose.
     */
    private static void theTransportOwnsItsOwnFields() throws Exception {
        String[] owned = new String[] {
            "Content-Length: 0",
            "content-length: 0",            // the name is case insensitive
            "Transfer-Encoding: chunked",
            "Host: someone.else",
        };
        for(int iter = 0 ; iter < owned.length ; iter++) {
            List one = new ArrayList();
            one.add(owned[iter]);
            String outcome;
            try {
                Web.request("POST", "http://127.0.0.1:1/nothing", one,
                        "body".getBytes("UTF-8"));
                outcome = "sent";
            } catch (Exception refused) {
                String message = String.valueOf(refused.getMessage());
                outcome = message.indexOf("transport owns") >= 0 ? "refused"
                        : "other: " + message;
            }
            check("the transport owns " + owned[iter], "refused", outcome);
        }
        // AND AN ORDINARY HEADER IS STILL SENT, which is what says the rule is
        // about these four names and not about headers in general. Nothing is
        // listening, so a connection failure is the answer that means "allowed".
        List good = new ArrayList();
        good.add("X-Api-Key: abc");
        good.add("Content-Type: application/json");
        String allowed;
        try {
            Web.request("POST", "http://127.0.0.1:1/nothing", good,
                    "body".getBytes("UTF-8"));
            allowed = "allowed";
        } catch (Exception err) {
            String message = String.valueOf(err.getMessage());
            allowed = message.indexOf("transport owns") >= 0 ? "refused" : "allowed";
        }
        check("an ordinary header is still sent", "allowed", allowed);
    }

    /**
     * A response that repeats a field keeps every value of it.
     *
     * <p>Both arms kept one -- the translated one overwrote as it parsed, the
     * JavaSE one took the last of the JDK's list -- so a response setting three
     * cookies delivered one and nothing said the others had gone. They cannot be
     * joined back together either: an Expires attribute carries a comma of its
     * own, so a comma-joined Set-Cookie is not one cookie or three.
     *
     * <p>Answered by a stub that writes the response literally, because the
     * server in this process sends its extra headers from a Map and so cannot
     * repeat a name.
     */
    /**
     * A CA path carrying a NUL is refused before OpenSSL sees it.
     *
     * <p>The third string this runtime hands to a native, after the URL and the
     * host. stringToUTF8 keeps the zero byte and C stops there, so a value that
     * READS as an approved bundle loads whatever sits before the NUL -- and the
     * session then verifies against a trust root somebody else chose. A database
     * URL can carry it as %00.
     */
    private static void aCaPathCannotBeTruncated() throws Exception {
        ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        String outcome;
        try {
            // Connecting is enough -- the backlog accepts it -- because the check
            // runs before any handshake does.
            Tcp conn = Tcp.connect("127.0.0.1", listener.getPort(), 2000);
            try {
                conn.startTls("127.0.0.1", "/tmp/mine\u0000/etc/ssl/approved.pem");
                outcome = "accepted";
            } catch (Exception refused) {
                String message = String.valueOf(refused.getMessage());
                outcome = message.indexOf("cannot hold a NUL") >= 0
                        ? "refused" : "other: " + message;
            } finally {
                conn.close();
            }
        } finally {
            listener.close();
        }
        check("a CA path carrying a NUL is refused", "refused", outcome);
    }

    private static void aRepeatedResponseHeaderKeepsEveryValue() throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        final int port = listener.getPort();
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = listener.accept();
                    if(client < 0) {
                        return;
                    }
                    ServerSocket.setTimeout(client, 10000);
                    byte[] scratch = new byte[4096];
                    ServerSocket.read(client, scratch, 0, scratch.length);
                    String response = "HTTP/1.1 200 OK\r\n"
                            + "Content-Length: 2\r\n"
                            + "Set-Cookie: a=1; Expires=Wed, 21 Oct 2026 07:28:00 GMT\r\n"
                            + "Set-Cookie: b=2\r\n"
                            + "Set-Cookie: c=3\r\n"
                            + "Connection: close\r\n\r\nok";
                    byte[] bytes = response.getBytes("UTF-8");
                    ServerSocket.write(client, bytes, 0, bytes.length);
                } catch (Exception ignored) {
                    // The client hanging up is how this ends.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        String count;
        String first;
        try {
            Web.Result result = Web.request("GET", "http://127.0.0.1:" + port + "/x",
                    null, null);
            count = String.valueOf(result.getHeaderValues("set-cookie").size());
            first = String.valueOf(result.getHeader("Set-Cookie"));
        } finally {
            listener.close();
        }
        stub.join(10000);
        check("every Set-Cookie survives", "3", count);
        // AND THE SINGLE ACCESSOR still answers, with the first of them -- a
        // caller that wants one cookie does not have to learn a new method.
        check("and the single accessor answers with the first", "true",
                String.valueOf(first.startsWith("a=1")));
    }

    private static void outboundHeadersCannotCarryANewline() throws Exception {
        String[] bad = new String[] {
            "Authorization: Bearer abc\nX-Admin: true",  // the finding
            "Authorization: Bearer abc\rX-Admin: true",  // CR alone, same trick
            "Authorization: Bearer abc\u010a",           // narrows to 0x0A for the native
            "X Bad: value",                              // a name that is not a token
            "no-colon-at-all",                           // not a header line
        };
        for(int iter = 0 ; iter < bad.length ; iter++) {
            List one = new ArrayList();
            one.add(bad[iter]);
            String outcome;
            try {
                Web.request("GET", "http://127.0.0.1:1/nothing", one, null);
                outcome = "sent";
            } catch (Exception refused) {
                String message = String.valueOf(refused.getMessage());
                // The validator's wording. A connection failure would also throw
                // here -- nothing is listening on port 1 -- so matching on "it
                // threw" would pass even with the check removed.
                outcome = message.indexOf("request header") >= 0 ? "refused"
                        : "other: " + message;
            }
            check("an outbound header is validated: " + sanitize(bad[iter]),
                    "refused", outcome);
        }
        // A legal header is still sent. Nothing is listening, so the answer is a
        // connection failure -- what matters is that the VALIDATOR did not stop it.
        List good = new ArrayList();
        good.add("Authorization: Bearer abc.def");
        good.add("X-Trace: 1\tindented");
        String sent;
        try {
            Web.request("GET", "http://127.0.0.1:1/nothing", good, null);
            sent = "not refused";
        } catch (Exception err) {
            String message = String.valueOf(err.getMessage());
            sent = message.indexOf("request header") >= 0 ? "refused" : "not refused";
        }
        check("a legal header is still sent", "not refused", sent);
    }

    /** A header spelling with its control characters made visible, for a message. */
    private static String sanitize(String value) {
        StringBuilder out = new StringBuilder();
        for(int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            out.append(c < 0x20 || c > 0x7e ? '?' : c);
        }
        return out.toString();
    }

    /**
     * A method is one token, on both arms.
     *
     * <p>The packaged arm gives it to libcurl as CURLOPT_CUSTOMREQUEST, which
     * writes it into the request line as it stands: a method carrying a CRLF
     * puts a second request line and a header of the caller's choosing on the
     * wire. An application that forwards a caller's verb hands that over, and
     * HttpURLConnection refuses the same string -- so the arms disagreed about
     * whether it was a request at all.
     */
    private static void aMethodIsOneToken() throws Exception {
        check("a method carrying a request line is refused", "refused",
                methodRefused("GET /admin HTTP/1.1\r\nX-Evil: yes\r\nX:"));
        check("and one carrying a space", "refused", methodRefused("GET POST"));
        check("an ordinary verb is still sent", "sent", methodRefused("DELETE"));
    }

    /** Whether Web refuses `method` outright, rather than putting it on the wire. */
    private static String methodRefused(String method) {
        try {
            Web.request(method, "http://127.0.0.1:1/nothing", null, null);
            return "sent";
        } catch (Exception err) {
            String message = String.valueOf(err.getMessage());
            return message.indexOf("one token") >= 0 ? "refused" : "sent";
        }
    }

    /**
     * A GET with a body keeps its verb, or says why it cannot.
     *
     * <p>HttpURLConnection rewrites a GET into a POST as soon as anything is
     * written to it, silently, while the packaged arm sends the GET. A search
     * endpoint that takes a body would be exercised with one verb in development
     * and another in production. What both arms must satisfy is that the verb is
     * never quietly changed: the packaged one sends GET, the local one refuses
     * with a reason, exactly as PATCH does one direction over.
     */
    private static void aGetWithABodyKeepsItsVerb() throws Exception {
        final String[] seen = new String[] { "<none>" };
        HttpServer server = HttpServer.start("127.0.0.1", 0, 16, 1, new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                seen[0] = request.getMethod();
                return HttpServer.Response.text(200, "ok");
            }
        });
        String outcome;
        try {
            Web.request("GET", "http://127.0.0.1:" + server.getPort() + "/x", null,
                    "{\"q\":1}".getBytes("UTF-8"));
            outcome = seen[0];
        } catch (Exception refused) {
            String message = String.valueOf(refused.getMessage());
            outcome = message.indexOf("cannot send a GET with a body") >= 0
                    ? "explained" : "other: " + message;
        } finally {
            server.stop(2000);
        }
        check("a GET with a body is sent as GET, or explained", "true",
                String.valueOf("GET".equals(outcome) || "explained".equals(outcome)));
        check("and never quietly turned into something else", "false",
                String.valueOf("POST".equals(outcome)));
    }

    /**
     * Http is a public client of its own, and its request line is built by hand.
     *
     * <p>Web's verb is checked now; this one writes both fields of the request
     * line verbatim, so a method or a path carrying a CRLF adds whatever the
     * caller likes to a request the upstream trusts. Refused before the socket
     * is opened, which is what these assert: the failure is the token, not a
     * connection.
     */
    private static void theOtherClientChecksItsRequestLine() throws Exception {
        check("Http refuses a method carrying a request line", "refused",
                httpRefused("GET /admin HTTP/1.1\r\nX-Evil: yes\r\nX:", "/x"));
        check("and a path carrying a header", "refused",
                httpRefused("GET", "/x\r\nX-Evil: yes"));
        check("and a path that is not origin-form", "refused",
                httpRefused("GET", "http://elsewhere/x"));
        // AND AN ORDINARY REQUEST still gets as far as the connection, which is
        // what says the refusals above are about the request line.
        check("an ordinary request reaches the socket", "connected or refused",
                httpRefused("GET", "/x"));
    }

    /** Whether Http refuses the request line, or gets as far as connecting. */
    private static String httpRefused(String method, String path) {
        try {
            Http.request("127.0.0.1", 1, method, path, null);
            return "connected or refused";
        } catch (Exception err) {
            String message = String.valueOf(err.getMessage());
            return message.indexOf("one token") >= 0
                    || message.indexOf("origin-form") >= 0
                    || message.indexOf("control character") >= 0
                    ? "refused" : "connected or refused";
        }
    }

    /**
     * An IPv6 literal has to be bracketed in the Host field.
     *
     * <p>RFC 3986 gives the authority no other way to say where the address ends
     * and the port begins, and Tcp.connect accepts "::1" quite happily -- so an
     * otherwise valid request over IPv6 went out as "Host: ::1:8080" and a
     * conforming server refused it.
     */
    private static void anIpv6HostIsBracketed() throws Exception {
        check("an IPv6 literal is bracketed", "[::1]:8080",
                FileCountProbe.hostField("::1", 8080));
        check("one already bracketed is left alone", "[::1]:8080",
                FileCountProbe.hostField("[::1]", 8080));
        check("and a name is not bracketed", "example.com:80",
                FileCountProbe.hostField("example.com", 80));
        check("nor is an IPv4 literal", "127.0.0.1:8080",
                FileCountProbe.hostField("127.0.0.1", 8080));
    }

    private static void patchIsASendableVerb() throws Exception {
        String outcome;
        try {
            Web.Result r = Web.request("PATCH", "http://127.0.0.1:1/nothing", null, null);
            // Nothing is listening, so a failed CONNECTION is the expected answer
            // where the verb IS sendable. What matters is that the verb was not
            // what stopped it.
            outcome = r == null || r.getStatus() <= 0 ? "sent or explained" : "answered";
        } catch (Exception err) {
            String message = String.valueOf(err.getMessage());
            // Either it went out and the connection failed, or it was refused with
            // the explanation. Anything else is the opaque JDK error.
            outcome = message.indexOf("cannot send") >= 0
                    || message.indexOf("Connection refused") >= 0
                    || message.indexOf("failed") >= 0
                    ? "sent or explained" : "opaque: " + message;
        }
        check("PATCH is sent, or refused with a reason", "sent or explained", outcome);
    }

    /**
     * A repeated outbound header must survive on BOTH arms.
     *
     * libcurl appends every list entry it is given, so two Cookie lines both go
     * out of the packaged binary. HttpURLConnection's setRequestProperty REPLACES,
     * so the local arm sent only the last one -- an integration that depends on a
     * repeatable header worked once packaged and quietly sent half of what it
     * meant to under cn1:backend, which is the worst way round for a dev loop to
     * be wrong. Echoed back by a server here rather than inspected, because the
     * two arms have no shared way to ask what they sent.
     */
    /**
     * A server asked for no workers is refused, and does not keep the port.
     *
     * <p>The two arms failed this differently and both badly. Java SE's
     * Executors.newFixedThreadPool throws for a non-positive count, but only
     * after the listener and reactor are open, so the port stayed bound and the
     * caller's retry met "address already in use" rather than the argument error.
     * The packaged runtime's pool created no workers at all and returned a server
     * that accepts connections and queues them forever -- a server that is
     * listening and can never answer.
     *
     * <p>Rebinding the SAME port afterwards is the half that proves the listener
     * was not leaked; asserting only that it threw would pass either way.
     */
    private static void aServerWithNoWorkersIsRefusedBeforeBinding() throws Exception {
        // PORT 0 EVERY TIME. An earlier version of this check released a port and
        // rebound it to prove the listener was not leaked, and that races any
        // other process on the machine -- it failed under parallel test forks,
        // which is a flake I would have introduced to catch someone else's bug.
        //
        // It buys nothing either, because the MESSAGE already separates the three
        // outcomes. Validated before the bind: an IOException naming workerCount.
        // Validated by the Java SE executor after the bind: an
        // IllegalArgumentException that says nothing about workerCount, so this
        // reads "other". Not validated at all, which is what the packaged pool
        // did: a server comes back and this reads "accepted". Each arm's bug has
        // its own answer here, and none of them needs a fixed port.
        //
        // That the port is not retained follows from the check preceding the bind
        // rather than from an assertion here; measured once against the unfixed
        // build, the rebind failed with "Could not bind 127.0.0.1:53103".
        int[] counts = new int[]{0, -1};
        String[] refusals = new String[counts.length];
        for(int iter = 0 ; iter < counts.length ; iter++) {
            try {
                HttpServer bad = HttpServer.start("127.0.0.1", 0, 16, counts[iter],
                        new HttpServer.Handler() {
                            public HttpServer.Response handle(HttpServer.Request request) {
                                return HttpServer.Response.text(200, "ok");
                            }
                        });
                bad.stop(1000);
                refusals[iter] = "accepted";
            } catch (Exception refused) {
                String message = String.valueOf(refused.getMessage());
                refusals[iter] = message.indexOf("workerCount") >= 0
                        ? "refused" : "other: " + message;
            }
        }
        check("a server with no workers is refused", "refused", refusals[0]);
        check("and a negative worker count too", "refused", refusals[1]);

        // One worker is a legal server, which is the boundary the check sits on.
        HttpServer good = HttpServer.start("127.0.0.1", 0, 16, 1,
                new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) {
                        return HttpServer.Response.text(200, "ok");
                    }
                });
        try {
            check("one worker is accepted", "true", String.valueOf(good.getPort() > 0));
        } finally {
            good.stop(1000);
        }
    }

    /**
     * Web fetches http and https and NOTHING else.
     *
     * <p>The packaged arm hands the URL to libcurl with every protocol its build
     * enabled, and the shipped builds enable file://. So an application that
     * passes a caller-controlled URL to Web.request -- a webhook target, an avatar
     * URL, anything a user supplies -- could be asked for "file:///etc/passwd" and
     * would put the file in the handler-visible response body. The class documents
     * an HTTP client and the Java SE arm cannot do anything else, HttpURLConnection
     * being the only thing it opens.
     *
     * <p>Read of a file that certainly exists and certainly has content, so a
     * refusal cannot be mistaken for an empty read.
     */
    private static void onlyHttpUrlsAreFetched() throws Exception {
        String[] forbidden = new String[] {
            "file:///etc/hosts",
            "file://localhost/etc/hosts",
            "ftp://127.0.0.1/pub/x",
            "gopher://127.0.0.1/x",
            "dict://127.0.0.1/d:x",
        };
        for(int iter = 0 ; iter < forbidden.length ; iter++) {
            String outcome;
            try {
                Web.Result r = Web.request("GET", forbidden[iter], null, null);
                byte[] body = r == null ? null : r.getBody();
                outcome = "fetched " + (body == null ? 0 : body.length) + " bytes";
            } catch (Exception refused) {
                // THE SCHEME RULE, by name. A bare "it threw" is satisfied by
                // anything -- measured, the Java SE arm threw ClassCastException
                // out of the HttpURLConnection cast for file://, so this check
                // passed there before the rule existed and tested nothing. It is
                // also exactly the failure CLAUDE.md says never to rely on, since
                // ParparVM's CHECKCAST does not throw at all.
                String message = String.valueOf(refused.getMessage());
                outcome = message.indexOf("http and https") >= 0
                        ? "refused" : "other: " + refused.getClass().getName()
                                + ": " + message;
            }
            check("a non-HTTP url is refused: " + forbidden[iter], "refused", outcome);
        }

        // And http still works, which is the direction this breaks. Served
        // locally so the check needs no network.
        HttpServer server = HttpServer.start("127.0.0.1", 0, 16, 1, new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) {
                return HttpServer.Response.text(200, "reachable");
            }
        });
        try {
            Web.Result r = Web.request("GET",
                    "http://127.0.0.1:" + server.getPort() + "/", null, null);
            check("http is still fetched", "reachable",
                    r == null ? "null" : String.valueOf(r.getBodyAsString()).trim());
        } finally {
            server.stop(1000);
        }
    }

    /**
     * An outbound response bigger than the bound is refused, not accumulated.
     *
     * <p>Both arms buffer a response whole before the caller sees any of it, so an
     * endless or merely huge upstream reply grew memory until the process died --
     * the packaged arm up to a 2GB integer-safety ceiling, and the Java SE arm
     * with no ceiling at all. A read timeout is no help when the bytes are
     * arriving quickly; only a size bound is.
     *
     * <p>Served from a LOCAL server so the check needs no network and no patience:
     * the body is one byte over whatever CN1_WEB_MAX_RESPONSE_MB says. The test
     * harnesses set that to 1MB, and the check skips when it is unset or large,
     * because serving 64MB to prove the default would cost more than it is worth.
     */
    private static void anOversizedResponseIsRefusedNotAccumulated() throws Exception {
        String configured = System.getenv("CN1_WEB_MAX_RESPONSE_MB");
        int limitMb = 0;
        if(configured != null) {
            try {
                limitMb = Integer.parseInt(configured.trim());
            } catch (NumberFormatException ignored) {
                limitMb = 0;
            }
        }
        if(limitMb < 1 || limitMb > 4) {
            note("response-size bound skipped: set CN1_WEB_MAX_RESPONSE_MB to 1..4 to run it");
            return;
        }
        final int limitBytes = limitMb * 1024 * 1024;
        HttpServer server = HttpServer.start("127.0.0.1", 0, 16, 1, new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) {
                // "/over" is one byte past the bound; "/under" is comfortably inside
                // it, so the refusal below cannot be the server failing to serve.
                if(request.getTarget().indexOf("/headers") >= 0) {
                    // Past the native's fixed 1MB header ceiling, in LINES rather
                    // than in one enormous line: libcurl refuses a single line
                    // over its own cap, so only the count reaches the accumulator.
                    StringBuilder filler = new StringBuilder();
                    for(int c = 0 ; c < 600 ; c++) {
                        filler.append('x');
                    }
                    Map many = new LinkedHashMap();
                    for(int h = 0 ; h < 3000 ; h++) {
                        many.put("X-Filler-" + h, filler.toString());
                    }
                    return new HttpServer.Response(200, "text/plain",
                            "headers".getBytes(), many);
                }
                int size = request.getTarget().indexOf("/over") >= 0
                        ? limitBytes + 1 : 1024;
                return new HttpServer.Response(200, "application/octet-stream",
                        new byte[size]);
            }
        });
        try {
            String base = "http://127.0.0.1:" + server.getPort();
            String under;
            try {
                Web.Result r = Web.request("GET", base + "/under", null, null);
                under = r == null ? "null" : String.valueOf(r.getBody().length);
            } catch (Exception err) {
                under = "threw: " + err.getMessage();
            }
            check("a response inside the bound still arrives", "1024", under);

            String over;
            try {
                Web.Result r = Web.request("GET", base + "/over", null, null);
                over = "accepted " + (r == null ? "null" : String.valueOf(r.getBody().length));
            } catch (Exception refused) {
                over = "refused";
            }
            check("a response past the bound is refused", "refused", over);

            // THE HEADERS TOO, which the body bound does not cover. libcurl caps
            // one header LINE and nothing caps how many arrive, so an upstream
            // streaming legal header lines could exhaust this process before a
            // body existed at all.
            //
            // THE REASON, not just that it threw. Any failure would satisfy a bare
            // "refused" -- and measurement showed the refusal here comes from the
            // HTTP stack's own aggregate cap on both arms, libcurl's 300KB
            // ("Too large response headers") and the JDK's 384KB ("Header size too
            // big"), rather than from anything this project added. Matching the
            // reason is what keeps this a check on header SIZE and what would
            // notice if a stack ever stopped enforcing it.
            String headers;
            try {
                Web.Result r = Web.request("GET", base + "/headers", null, null);
                headers = "accepted " + (r == null ? "null"
                        : String.valueOf(r.getBodyAsString()).length());
            } catch (Exception refused) {
                String message = String.valueOf(refused.getMessage());
                boolean aboutHeaders = message.indexOf("eader") >= 0;
                headers = aboutHeaders ? "refused" : "other: " + message;
            }
            check("an oversized header block is refused", "refused", headers);
        } finally {
            server.stop(1000);
        }
    }

    private static void repeatedOutboundHeadersSurvive() throws Exception {
        HttpServer server = HttpServer.start("127.0.0.1", 0, 16, 1, new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) {
                String seen = request.getHeader("x-repeat");
                return HttpServer.Response.text(200, seen == null ? "absent" : seen);
            }
        });
        try {
            List headers = new ArrayList();
            headers.add("X-Repeat: one");
            headers.add("X-Repeat: two");
            Web.Result r = Web.request("GET",
                    "http://127.0.0.1:" + server.getPort() + "/", headers, null);
            String body = r == null ? "null" : r.getBodyAsString();
            // The server joins repeats with ", " (RFC 9110 5.3), so BOTH values
            // are present exactly when both lines were sent. Asserting on the
            // joined string rather than on a count keeps this true whichever
            // order the arms emit them in.
            check("a repeated request header keeps its first value", "true",
                    String.valueOf(body != null && body.indexOf("one") >= 0));
            check("a repeated request header keeps its second value", "true",
                    String.valueOf(body != null && body.indexOf("two") >= 0));
        } finally {
            server.stop();
        }
    }

    /**
     * A credential inside its refresh MARGIN is not an expired credential.
     *
     * <p>S3.credentials() refreshes early, at CREDENTIAL_REFRESH_MARGIN before
     * expiry, and now keeps the cached credential when that refresh fails -- a
     * metadata service that blinks must not become an outage of every S3 call
     * minutes before AWS would stop honouring what is in hand. That fallback is
     * decided by isExpiring(0), so what it means has to be exactly "already
     * expired" and nothing looser.
     *
     * <p>This pins the contract the fallback rests on. The fallback ITSELF is not
     * covered here: reaching it needs a credential inside its margin AND a
     * failing Credentials.resolve(), and there is no seam to inject either --
     * S3's constructor is private and resolve() reads the real environment.
     */
    /**
     * An expiry that does not exist is refused, rather than read as a later one.
     *
     * <p>Every field was taken by position and parsed as digits, and the
     * arithmetic that follows is a running total -- so February the 31st simply
     * carried into March and an hour of 99 added four days. Both read LATER than
     * the provider meant, which is the direction that matters: the credential
     * would be treated as live after AWS had retired it, and the failure arrives
     * as an authorization error with nothing pointing at the cause.
     *
     * <p>Zero is the answer for everything refused, because that is what
     * fromJson() turns into its "cannot read this Expiration" error.
     */
    private static void anImpossibleExpiryIsRefused() throws Exception {
        check("a real expiry still parses", "true",
                String.valueOf(ExpiryProbe.parse("2026-08-28T13:45:00Z") > 0));
        check("and so does one with fractional seconds", "true",
                String.valueOf(ExpiryProbe.parse("2026-08-28T13:45:00.123Z") > 0));
        check("a day that month does not have is refused", "0",
                String.valueOf(ExpiryProbe.parse("2026-02-31T00:00:00Z")));
        check("an hour of 99 is refused", "0",
                String.valueOf(ExpiryProbe.parse("2026-08-28T99:00:00Z")));
        check("a month of 13 is refused", "0",
                String.valueOf(ExpiryProbe.parse("2026-13-01T00:00:00Z")));
        // February the 29th exists in 2028 and not in 2026, and the check has to
        // know the difference rather than allowing 29 every year.
        check("the 29th of February is refused in a common year", "0",
                String.valueOf(ExpiryProbe.parse("2026-02-29T00:00:00Z")));
        check("and accepted in a leap year", "true",
                String.valueOf(ExpiryProbe.parse("2028-02-29T00:00:00Z") > 0));
        // The arithmetic has no notion of an offset, so one would be read as if it
        // were Zulu and the expiry would move by hours.
        check("an offset rather than Z is refused", "0",
                String.valueOf(ExpiryProbe.parse("2026-08-28T13:45:00-05:00")));
        check("a space where the T belongs is refused", "0",
                String.valueOf(ExpiryProbe.parse("2026-08-28 13:45:00Z")));

        // AND THE WHOLE RESPONSE. Both callers of fromJson are the metadata
        // providers, whose credentials are temporary and are signed with a session
        // token -- so a response carrying a key pair and an expiry but no token
        // describes something that cannot sign anything. Accepting it meant every
        // request came back rejected while S3 cached the pair until its refresh
        // margin, which may be hours.
        String complete = "{\"AccessKeyId\":\"AKIA\",\"SecretAccessKey\":\"s\","
                + "\"Token\":\"t\",\"Expiration\":\"2026-08-28T13:45:00Z\"}";
        check("a complete credential response is accepted", "accepted",
                ExpiryProbe.rejects(complete));
        String noToken = "{\"AccessKeyId\":\"AKIA\",\"SecretAccessKey\":\"s\","
                + "\"Expiration\":\"2026-08-28T13:45:00Z\"}";
        check("one with no session token is refused", "refused",
                ExpiryProbe.rejects(noToken));
        String emptyToken = "{\"AccessKeyId\":\"AKIA\",\"SecretAccessKey\":\"s\","
                + "\"Token\":\"\",\"Expiration\":\"2026-08-28T13:45:00Z\"}";
        check("and an empty one is not a token", "refused",
                ExpiryProbe.rejects(emptyToken));
        // SessionToken is the other spelling the providers use, and it still works.
        String sessionSpelling = "{\"AccessKeyId\":\"AKIA\",\"SecretAccessKey\":\"s\","
                + "\"SessionToken\":\"t\",\"Expiration\":\"2026-08-28T13:45:00Z\"}";
        check("SessionToken is accepted as the token", "accepted",
                ExpiryProbe.rejects(sessionSpelling));
        // THE KEY PAIR BY THE SAME RULE as the token beside it, which is where
        // this started: the pair was checked for null only, so a response naming
        // an empty key or secret was accepted and cached until the refresh margin
        // while every signed request came back rejected. Whitespace signs no
        // better than nothing.
        String emptyId = "{\"AccessKeyId\":\"\",\"SecretAccessKey\":\"s\","
                + "\"Token\":\"t\",\"Expiration\":\"2026-08-28T13:45:00Z\"}";
        check("an empty access key is refused", "refused",
                ExpiryProbe.rejects(emptyId));
        String emptySecret = "{\"AccessKeyId\":\"AKIA\",\"SecretAccessKey\":\"\","
                + "\"Token\":\"t\",\"Expiration\":\"2026-08-28T13:45:00Z\"}";
        check("an empty secret is refused", "refused",
                ExpiryProbe.rejects(emptySecret));
        String blankSecret = "{\"AccessKeyId\":\"AKIA\",\"SecretAccessKey\":\"   \","
                + "\"Token\":\"t\",\"Expiration\":\"2026-08-28T13:45:00Z\"}";
        check("a whitespace secret is refused", "refused",
                ExpiryProbe.rejects(blankSecret));
        // A REGION BECOMES PART OF A HOST: "s3." + region + ".amazonaws.com". One
        // with a separator in it moves the amazonaws.com suffix into the path and
        // leaves the authority to whoever owns the region string.
        check("a region with a path separator is refused", "refused",
                regionRefused("evil.example/ignored"));
        check("and one with a dot, which is the same trick a label shorter",
                "refused", regionRefused("evil.example"));
        check("and one with an at sign, which moves the host past userinfo",
                "refused", regionRefused("x@evil.example"));
        check("an ordinary region is accepted", "accepted",
                regionRefused("us-east-1"));
        // An empty Token now falls through to SessionToken instead of being read
        // as a token that is present but unusable.
        String emptyThenSession = "{\"AccessKeyId\":\"AKIA\",\"SecretAccessKey\":\"s\","
                + "\"Token\":\"\",\"SessionToken\":\"t\","
                + "\"Expiration\":\"2026-08-28T13:45:00Z\"}";
        check("an empty Token falls back to SessionToken", "accepted",
                ExpiryProbe.rejects(emptyThenSession));
    }

    private static void expiryMarginIsDistinctFromExpiry() throws Exception {
        long now = System.currentTimeMillis();
        // Two minutes of life left: inside a five-minute refresh margin, and not
        // expired. This is the state the fallback exists for.
        Credentials soon = new Credentials("id", "secret", "token", now + 120000);
        check("a credential inside the margin wants refreshing", "true",
                String.valueOf(soon.isExpiring(300000)));
        check("but it has NOT expired", "false", String.valueOf(soon.isExpiring(0)));

        Credentials gone = new Credentials("id", "secret", "token", now - 1000);
        check("a past expiry has expired", "true", String.valueOf(gone.isExpiring(0)));

        Credentials plenty = new Credentials("id", "secret", "token", now + 3600000);
        check("an hour of life needs no refresh", "false",
                String.valueOf(plenty.isExpiring(300000)));
    }

    /**
     * Text stored in the database comes back as the same text.
     *
     * <p>The packaged arm read SQLite values with newStringFromCString, which is
     * the GENERATED-LITERAL reader rather than a decoder: it widens each byte, so
     * a stored e-acute (UTF-8 C3 A9) came back as the two characters 0xC3 and
     * 0xA9, and it expands "~~uXXXX", so a value that merely CONTAINS that text
     * came back as whatever code unit it names. The Java SE arm goes through JDBC
     * and preserved both, so this was a divergence as well as a corruption -- the
     * value written by the packaged server could not be read back by it.
     *
     * <p>The escape case is the one that cannot be explained away as an encoding
     * detail: it is the database's own data choosing what character to become.
     */
    /**
     * A Date and a Character bound through raw SQL are stored the way the ORM
     * stores them.
     *
     * <p>Database.execute normalised only Boolean, so the other two scalars
     * whose Java form is not what the column holds reached the driver as objects
     * the engines render with String.valueOf. SQLite's integer affinity stores
     * that text without complaint, while PostgreSQL and a strict MySQL refuse it
     * as invalid integer input -- so the same statement wrote a row on one
     * engine and failed on the others, and what SQLite wrote did not compare
     * equal to what the generated dao writes for the same value.
     */
    private static void rawSqlEncodesDatesAndCharsLikeTheOrm() throws Exception {
        String path = "/tmp/cn1-selftest-scalars-" + System.currentTimeMillis() + ".db";
        Database db = Database.open(path);
        try {
            db.execute("DROP TABLE IF EXISTS scalars", null);
            db.execute("CREATE TABLE scalars (id INTEGER, whenMs INTEGER, ch INTEGER)", null);
            java.util.Date when = new java.util.Date(1234567890L);
            db.execute("INSERT INTO scalars (id, whenMs, ch) VALUES (?, ?, ?)",
                    new Object[] {Long.valueOf(1L), when, new Character('x')});
            List rows = db.query("SELECT whenMs, ch FROM scalars WHERE id = ?",
                    new Object[] {Long.valueOf(1L)});
            check("a Date binds as its millisecond value", "1234567890",
                    String.valueOf(com.codename1.impl.orm.Values.asLong(((Map)rows.get(0)).get("whenMs"), -1L)));
            // 'x' is 120. The ORM stores the code unit, so raw SQL has to as well
            // or a query written either way misses rows written the other.
            check("a Character binds as its code unit", "120",
                    String.valueOf(com.codename1.impl.orm.Values.asLong(((Map)rows.get(0)).get("ch"), -1L)));
        } finally {
            db.close();
            new java.io.File(path).delete();
        }
    }

    private static void storedTextComesBackUnchanged() throws Exception {
        String path = "/tmp/cn1-selftest-text-" + System.currentTimeMillis() + ".db";
        Database db = Database.open(path);
        try {
            db.execute("DROP TABLE IF EXISTS texts", null);
            db.execute("CREATE TABLE texts (id INTEGER, body TEXT)", null);

            // An accented letter, a character outside the basic plane, and the
            // literal escape the generated-literal reader would have expanded.
            // THE ESCAPE IS BUILT FROM CHARS, not written as a literal. A Java
            // string literal containing that sequence is expanded by the
            // TRANSLATOR on its way into the packaged binary, so the constant
            // there is already "A" and the value under test never reaches the
            // database -- the check would pass by testing nothing. Assembling it
            // at runtime is the only way the seven characters exist to be stored.
            String escapeText = new String(new char[]{'~', '~', 'u', '0', '0', '4', '1'});
            String[] values = new String[] {
                "caf\u00e9",
                "\ud83d\ude00 smile",
                escapeText,
                "plain ascii",
            };
            for(int iter = 0 ; iter < values.length ; iter++) {
                db.execute("INSERT INTO texts (id, body) VALUES (?, ?)",
                        new Object[]{Integer.valueOf(iter), values[iter]});
            }
            for(int iter = 0 ; iter < values.length ; iter++) {
                List rows = db.query("SELECT body FROM texts WHERE id = ?",
                        new Object[]{Integer.valueOf(iter)});
                String got = rows.isEmpty() ? "<missing>"
                        : String.valueOf(((Map)rows.get(0)).get("body"));
                check("stored text round trips: " + describeChars(values[iter]),
                        describeChars(values[iter]), describeChars(got));
            }
            // AN EMBEDDED NUL USED TO BE ONE OF THE VALUES ABOVE, and is now
            // refused before it reaches any engine. It is a legal character in a
            // Java string -- JSON carries them -- and this check existed because
            // getBytes("UTF-8") encodes it as one zero byte, so binding the
            // value by its C length stopped there and stored "a" while
            // reporting success, while the Java SE arm went through JDBC and
            // stored all three. That divergence is gone the only way it could
            // be: PostgreSQL cannot hold a zero byte in a text value at all, so
            // no encoding makes the three agree and the bind is refused. The
            // truncation it guarded is unreachable now rather than merely
            // tested -- a value that cannot be bound cannot be cut short.
            //
            // A NUL inside a byte[] is untouched, which is where one belongs.
            String nulText = new String(new char[]{'a', '\0', 'b'});
            String refusedNul;
            try {
                db.execute("INSERT INTO texts (id, body) VALUES (?, ?)",
                        new Object[]{Integer.valueOf(99), nulText});
                refusedNul = "accepted";
            } catch (Exception err) {
                refusedNul = "refused";
            }
            check("a NUL in bound text is refused, not truncated", "refused", refusedNul);
            check("and nothing was written", "0",
                    String.valueOf(db.query("SELECT body FROM texts WHERE id = ?",
                            new Object[]{Integer.valueOf(99)}).size()));
        } finally {
            db.close();
            new java.io.File(path).delete();
        }
    }

    /** A string as its code units, so a mismatch names the characters. */
    private static String describeChars(String value) {
        StringBuilder out = new StringBuilder();
        for(int iter = 0 ; iter < value.length() ; iter++) {
            if(iter > 0) {
                out.append(' ');
            }
            out.append(Integer.toHexString(value.charAt(iter)));
        }
        return out.toString();
    }

    /**
     * A statement runs with exactly as many parameters as it has placeholders.
     *
     * <p>SQLite leaves an UNBOUND parameter as NULL and says nothing, so passing
     * fewer values than the SQL has placeholders committed a row the caller never
     * wrote -- an insert or an update quietly storing NULL where a value belonged.
     * The Java SE arm throws for the same call, which is the worse half of it: the
     * dev loop refuses what production accepts.
     *
     * <p>Asserted on BOTH arms with one expectation. Too MANY parameters was
     * already refused (SQLITE_RANGE) and is checked here so it stays that way.
     */
    private static void boundParametersMustMatchThePlaceholders() throws Exception {
        String path = "/tmp/cn1-selftest-params-" + System.currentTimeMillis() + ".db";
        Database db = Database.open(path);
        try {
            db.execute("DROP TABLE IF EXISTS pairs", null);
            db.execute("CREATE TABLE pairs (a TEXT, b TEXT)", null);

            // The correct call first, so the rest cannot pass by the table being
            // broken or the statement never preparing.
            db.execute("INSERT INTO pairs (a, b) VALUES (?, ?)",
                    new Object[]{"one", "two"});
            check("a matched statement inserts", "1",
                    String.valueOf(db.query("SELECT a FROM pairs WHERE b = ?",
                            new Object[]{"two"}).size()));

            String tooFew;
            try {
                db.execute("INSERT INTO pairs (a, b) VALUES (?, ?)", new Object[]{"only"});
                tooFew = "accepted";
            } catch (Exception refused) {
                tooFew = "refused";
            }
            check("too few parameters are refused", "refused", tooFew);

            String noneAtAll;
            try {
                db.execute("INSERT INTO pairs (a, b) VALUES (?, ?)", null);
                noneAtAll = "accepted";
            } catch (Exception refused) {
                noneAtAll = "refused";
            }
            check("no parameters at all are refused", "refused", noneAtAll);

            String tooMany;
            try {
                db.execute("INSERT INTO pairs (a, b) VALUES (?, ?)",
                        new Object[]{"a", "b", "c"});
                tooMany = "accepted";
            } catch (Exception refused) {
                tooMany = "refused";
            }
            check("too many parameters are refused", "refused", tooMany);

            // And nothing was written by the refused calls: the row count is still
            // the one successful insert. A refusal that has already committed is
            // not a refusal.
            check("a refused statement wrote nothing", "1",
                    String.valueOf(db.query("SELECT a FROM pairs", null).size()));
        } finally {
            db.close();
            new java.io.File(path).delete();
        }
    }

    /**
     * A URL component keeps its non-ASCII characters when something ELSE in it
     * needed escaping.
     *
     * <p>The decoder wrote a literal char straight into a byte buffer that it then
     * read back as UTF-8, so "p\u00e4ss%40word" put byte 0xE4 -- a lead byte with
     * no continuation -- where two bytes belonged, and the password decoded to
     * U+FFFD in place of the a-umlaut. The credential, database name or CA path
     * silently became a DIFFERENT string, and the connection then failed, or
     * succeeded against something other than what was written down.
     *
     * <p>Only reachable when one component carries an escape AND a literal
     * non-ASCII character: with no escape anywhere the decoder is never entered,
     * which is why an accented password on its own always worked and this stayed
     * invisible until some other character needed encoding.
     *
     * <p>Read back through sslmode, which is the one decoded value this API
     * repeats verbatim -- its refusal quotes what it was given, so the decoder's
     * output is observable without a live server and without printing a password.
     */
    private static void urlComponentsKeepTheirUnicode() throws Exception {
        // "caf<a-umlaut>%2Dx" is a literal non-ASCII character and an escaped '-'
        // in one value. Correctly decoded it is "caf<a-umlaut>-x".
        String outcome;
        try {
            Database.open("postgres://u:p@127.0.0.1:1/db?sslmode=caf\u00e4%2Dx");
            outcome = "accepted";
        } catch (Exception refused) {
            outcome = String.valueOf(refused.getMessage());
        }
        check("the escape decoded", "true", String.valueOf(outcome.indexOf("-x") >= 0));
        check("and the literal character survived it", "true",
                String.valueOf(outcome.indexOf("caf\u00e4-x") >= 0));
        check("with no replacement character", "true",
                String.valueOf(outcome.indexOf("\uFFFD") < 0));

        // A character OUTSIDE the basic plane arrives as a surrogate PAIR, which a
        // hand-rolled encoder turns into two malformed halves. U+1F600 beside an
        // escape is the case that proves the pair is encoded as one sequence.
        String astral;
        try {
            Database.open("postgres://u:p@127.0.0.1:1/db?sslmode=\ud83d\ude00%2Dx");
            astral = "accepted";
        } catch (Exception refused) {
            astral = String.valueOf(refused.getMessage());
        }
        check("a surrogate pair survives the round trip", "true",
                String.valueOf(astral.indexOf("\ud83d\ude00-x") >= 0));
    }

    /**
     * An explicitly spelled port that is not a real port is refused by the
     * PARSER, rather than reaching an engine that reads it as "unset".
     *
     * <p>Both Postgres.connect and MySql.connect substitute their default for any
     * non-positive port, so "host:-1" -- which Integer.parseInt accepts, the sign
     * fitting inside no width limit -- connected to 5432 or 3306 without a word.
     * A mistyped deployment setting then reaches a live database nobody named, at
     * the address the URL did name, and the only evidence is that it worked.
     */
    private static void malformedPortsAreRefused() throws Exception {
        String[] bad = new String[] {
            "postgres://u:p@127.0.0.1:-1/db",      // the finding: a sign parses
            "postgres://u:p@127.0.0.1:+5432/db",   // the same leniency, other sign
            "postgres://u:p@127.0.0.1:0/db",       // port 0 is not connectable
            "postgres://u:p@127.0.0.1:65536/db",   // past the port space
            "postgres://u:p@127.0.0.1:/db",        // spelled, and empty
        };
        for(int iter = 0 ; iter < bad.length ; iter++) {
            String outcome;
            try {
                Database.open(bad[iter]);
                outcome = "accepted";
            } catch (Exception refused) {
                String message = String.valueOf(refused.getMessage());
                // The parser's wording, and the absence of the password. An
                // engine-side connect failure would also throw here, which is
                // why this matches the message rather than merely "it threw".
                outcome = message.indexOf("Not a port number") >= 0
                        && message.indexOf("p@") < 0 ? "refused" : "other: " + message;
            }
            check("a malformed port is refused: " + bad[iter], "refused", outcome);
        }
        // An ABSENT port still takes the engine default rather than being
        // refused, which is the behaviour this must not have broken. There is no
        // server on 5432 in this environment, so the check is that it got as far
        // as trying to connect -- any answer except the parser's refusal.
        String absent;
        try {
            Database.open("postgres://u:p@127.0.0.1/db?connectTimeout=1");
            absent = "connected";
        } catch (Exception err) {
            String message = String.valueOf(err.getMessage());
            absent = message.indexOf("Not a port number") >= 0 ? "refused" : "reached the engine";
        }
        check("an absent port still defaults", "reached the engine", absent);
    }

    /**
     * A negative connectTimeout is refused by the PARSER, so both arms fail the
     * same way. Left to the arms, Java SE threw IllegalArgumentException out of
     * Socket.connect while the packaged client read any non-positive value as
     * "block forever" and waited out the OS TCP timeout: the same URL, an error
     * on one side and a hang on the other.
     */
    private static void negativeConnectTimeoutsAreRefused() throws Exception {
        String outcome;
        try {
            // A NETWORK url: the query string is only parsed for the engines
            // that have a connection to time out, and sqlite has none.
            Database.open("postgres://u:p@127.0.0.1:1/db?connectTimeout=-1");
            outcome = "accepted";
        } catch (Exception refused) {
            String message = String.valueOf(refused.getMessage());
            // The PARSER's wording specifically. Matching on "negative" alone
            // also matches the JDK's own "timeout can't be negative" out of
            // Socket.connect -- which is the arm-specific failure this check
            // exists to replace, so it would have passed either way.
            outcome = message.indexOf("must not be negative") >= 0
                    ? "refused" : "other: " + message;
        }
        check("a negative connectTimeout is refused", "refused", outcome);

        // And at the API below the URL parser, where a caller can reach it
        // directly. The two arms disagreed: Java SE fails immediately out of
        // Socket.connect while the packaged native reads every non-positive value
        // as "block with no deadline", so the same call hangs for the OS TCP
        // timeout once packaged. Zero keeps its documented meaning.
        String tcpOutcome;
        try {
            Tcp.connect("127.0.0.1", 1, -1);
            tcpOutcome = "accepted";
        } catch (IllegalArgumentException refused) {
            tcpOutcome = "refused";
        } catch (Exception other) {
            tcpOutcome = "other: " + other;
        }
        check("a negative TCP connect timeout is refused", "refused", tcpOutcome);

        // A zero-length read is 0 on BOTH arms. InputStream says so and the Java SE
        // arm inherits it; the packaged one used to dispatch to recv(_, 0), whose
        // zero-byte result the native maps to END OF STREAM. Same call, "nothing
        // read" here and "the peer hung up" once packaged.
        HttpServer probe = HttpServer.start("127.0.0.1", 0, 16, 1, new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) {
                return HttpServer.Response.text(200, "ok");
            }
        });
        String emptyRead;
        try {
            // A GENEROUS connect timeout. This check is about what a zero-length
            // read answers, not about how fast a just-started server accepts --
            // and this self-test runs beside the HTTP suite, which has four
            // servers and several megabytes of traffic in flight. Two seconds
            // failed once here and could not be reproduced; the outcome string
            // below distinguishes "threw" from a wrong number, so if it ever does
            // fail again it will say which.
            Tcp conn = Tcp.connect("127.0.0.1", probe.getPort(), 15000);
            try {
                emptyRead = String.valueOf(conn.read(new byte[8], 0, 0));
            } finally {
                conn.close();
            }
        } catch (Exception err) {
            emptyRead = "threw: " + err;
        } finally {
            // TIMED, like fairness() above. The no-argument stop() drains without a
            // bound, and this probe deliberately leaves a connection that has just
            // been closed under it -- the run sat past ten minutes before I noticed
            // which of the two I had called.
            probe.stop(1000);
        }
        check("a zero-length read answers zero", "0", emptyRead);

        // And a non-positive iteration count is refused rather than quietly
        // deriving the one-round key. The native has always refused it.
        String weakKey;
        try {
            Crypto.pbkdf2Sha256("pw".getBytes("UTF-8"), "salt".getBytes("UTF-8"), 0, 32);
            weakKey = "accepted";
        } catch (Exception refused) {
            // IOException here, IllegalArgumentException on the other arm -- the
            // check is that it is REFUSED, not which exception says so.
            weakKey = "refused";
        }
        check("a zero iteration count is refused", "refused", weakKey);
    }

    /**
     * A static file comes back whole, on whichever path this arm serves it by.
     *
     * <p>Both arms run this and they do not share an implementation: the packaged
     * one splices with sendfile, the Java SE one copies through the deadline-aware
     * write. That second one used to be FileChannel.transferTo, which writes
     * straight at the channel and so goes around the deadline every other write
     * obeys -- on the pooled path the descriptor is blocking, so a client that
     * filled its buffer and stopped reading held a worker for as long as it liked.
     * The copy replacing it is the part with no coverage at all until here: a
     * static download is not something the integration suite reaches on this arm.
     *
     * <p>Bigger than one buffer on purpose. The loop that sends it advances by
     * whatever each call reports, so an implementation that miscounts shows up as
     * a short or repeated body rather than as an error.
     */
    /**
     * A file-backed response whose HEAD never reaches the client still closes its
     * descriptor.
     *
     * <p>The close used to sit beside the send, inside the branch that streams the
     * file, so anything that threw before that branch was reached left the
     * descriptor open and counted for the life of the process. The head write is
     * exactly such a thing, and aborted static requests are free to send.
     *
     * <p>Provoked rather than waited for: the response carries enough application
     * headers that its head cannot fit in any socket buffer, and the client closes
     * without reading a byte. The peer then resets the connection while the server
     * is still writing the head, which is the case being tested -- and the
     * descriptor is asserted to have been handed over in the first place, so the
     * check cannot pass by quietly getting a response with no file behind it.
     */
    private static void aFileBackedResponseClosesItsDescriptorWhenTheHeadFails()
            throws Exception {
        String dir = "/tmp/cn1-selftest-headfail-" + System.currentTimeMillis();
        new java.io.File(dir).mkdirs();
        String name = "payload.bin";
        StringBuilder content = new StringBuilder();
        for(int iter = 0 ; iter < 300 * 1024 ; iter++) {
            content.append((char)('a' + (iter % 26)));
        }
        java.io.FileOutputStream out = new java.io.FileOutputStream(dir + "/" + name);
        try {
            out.write(content.toString().getBytes("UTF-8"));
        } finally {
            out.close();
        }
        StringBuilder padding = new StringBuilder();
        for(int iter = 0 ; iter < 2048 ; iter++) {
            padding.append('p');
        }
        final String pad = padding.toString();
        final StaticFiles files = new StaticFiles(dir, "/static", null, null);
        final int[] handedOver = new int[] { -1 };
        HttpServer server = HttpServer.start("127.0.0.1", 0, 16, 1, new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                HttpServer.Response served = files.handle(request);
                if(served == null) {
                    return HttpServer.Response.text(404, "no");
                }
                handedOver[0] = FileCountProbe.fdOf(served);
                if(handedOver[0] < 0) {
                    return served;
                }
                // A head no socket buffer can take, so the write cannot finish
                // before the reset arrives.
                Map big = new LinkedHashMap();
                for(int iter = 0 ; iter < 4000 ; iter++) {
                    big.put("X-Pad-" + iter, pad);
                }
                return HttpServer.Response.file(200, "application/octet-stream",
                        handedOver[0], 0, FileCountProbe.lengthOf(served), big);
            }
        });
        String outcome;
        try {
            Tcp conn = Tcp.connect("127.0.0.1", server.getPort(), 15000);
            byte[] request = ("GET /static/" + name + " HTTP/1.1\r\nHost: x\r\n"
                    + "Connection: close\r\n\r\n").getBytes("UTF-8");
            conn.write(request, 0, request.length);
            // CLOSED AFTER A PAUSE, not immediately. Closing on the heels of the
            // write raced the server's read of it, and the request was dropped
            // before the handler ever saw it -- which the descriptor check below
            // reported rather than passing. By now the server is blocked writing a
            // head no buffer can take, so the close arrives in the middle of the
            // write, and the client's unread receive buffer is what makes the
            // close a reset rather than a polite half-close.
            Thread.sleep(100);
            conn.close();
            // The write fails on the server's own thread, so the descriptor comes
            // back a moment after the close rather than during it.
            long deadline = System.currentTimeMillis() + 10000;
            while(FileCountProbe.openFiles() > 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            outcome = handedOver[0] < 0 ? "no file was served"
                    : String.valueOf(FileCountProbe.openFiles());
        } finally {
            server.stop(2000);
            new java.io.File(dir + "/" + name).delete();
            new java.io.File(dir).delete();
        }
        check("a file-backed response closes its descriptor when the head fails",
                "0", outcome);
    }

    /**
     * A request whose body arrives late must not be served another connection's
     * bytes.
     *
     * The read buffer is __thread -- one per HOST thread, shared by every virtual
     * thread multiplexed onto it -- and a request is parsed in place inside it, so
     * its header slices name positions in storage the next connection will reuse.
     * A body that has not all arrived parks the virtual thread with those slices
     * live, the host goes and reads the next connection into that same buffer, and
     * what the handler then reads is whatever landed there.
     *
     * Both spellings of the same request are sent here: the head alone, then one
     * whole request on a second connection, then the withheld body. One worker, so
     * the two connections certainly share a host. Measured before the fix, every
     * getHeader on the first request answered null -- its own head sitting intact
     * in a copy 82 bytes away -- while getMethod, which is a String, was still
     * "POST". The same request with no second connection answered correctly, which
     * is the control this check keeps: an implementation that reads nothing at all
     * would pass the corrupted half and fail this one.
     */
    /**
     * A body this server will refuse is never invited.
     *
     * <p>100 Continue exists so a client can find out BEFORE it uploads. Sending
     * it and then answering 413 is the exact cost the mechanism avoids: a
     * conforming client transmits everything it declared -- up to the limit it is
     * about to be refused for -- and only then reads the answer. RFC 9110 10.1.1
     * says to send the final status instead when the request would be rejected.
     * A request with no Expect was refused correctly all along; one that politely
     * asked first was told to go ahead.
     */
    private static void aRefusedBodyIsNeverInvited() throws Exception {
        HttpServer server = HttpServer.start("127.0.0.1", 0, 16, 1, new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                return HttpServer.Response.text(200, "handled");
            }
        });
        String tooLarge;
        String malformed;
        try {
            tooLarge = expectationAnswer(server.getPort(), "999999999");
            malformed = expectationAnswer(server.getPort(), "not-a-number");
        } finally {
            server.stop(2000);
        }
        check("a body past the limit is refused instead of invited", "413", tooLarge);
        check("and so is one whose length is not a number", "400", malformed);
    }

    /**
     * The first status this server answers to an Expect, or "invited" when it
     * approved the body first.
     */
    private static String expectationAnswer(int port, String contentLength) throws Exception {
        Tcp conn = Tcp.connect("127.0.0.1", port, 15000);
        try {
            byte[] head = ("POST /x HTTP/1.1\r\nHost: x\r\nExpect: 100-continue\r\n"
                    + "Content-Length: " + contentLength + "\r\n"
                    + "Connection: close\r\n\r\n").getBytes("UTF-8");
            conn.write(head, 0, head.length);
            // NOT ONE BYTE OF BODY is sent, which is the client this is about: it
            // is waiting to be told. Whatever comes back came back without it.
            ByteArrayOutputStream reply = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            while(true) {
                int n = conn.read(chunk, 0, chunk.length);
                if(n <= 0) {
                    break;
                }
                reply.write(chunk, 0, n);
            }
            String text = new String(reply.toByteArray(), "UTF-8");
            if(text.indexOf("100 Continue") >= 0) {
                return "invited";
            }
            int space = text.indexOf(' ');
            return space < 0 || text.length() < space + 4 ? "unreadable: " + text
                    : text.substring(space + 1, space + 4);
        } finally {
            conn.close();
        }
    }

    /**
     * An interim response is not the answer.
     *
     * <p>RFC 9110 15.2: a 1xx is a complete response with its own status line and
     * header block and no body, and the final response follows it on the same
     * connection. Taking the first header terminator handed back "103 Early
     * Hints" as though it were the answer, with the real status line, headers and
     * body delivered to the caller as the 103's body -- so a client checking the
     * status read 103 for a request that had actually succeeded. This server
     * sends an interim 100 itself whenever a request carries Expect, so the two
     * halves of this codebase could not talk to each other about it.
     */
    private static void anInterimResponseIsSkipped() throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        final int port = listener.getPort();
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = listener.accept();
                    if(client < 0) {
                        return;
                    }
                    // DRAINED FIRST. Closing with the request still unread makes
                    // the peer send RST instead of FIN, and the client then loses
                    // the reply it had already been sent -- which turned this into
                    // a race between the reset and the last read rather than a
                    // check of what the client makes of an interim response.
                    byte[] request = new byte[4096];
                    ServerSocket.read(client, request, 0, request.length);
                    byte[] reply = ("HTTP/1.1 103 Early Hints\r\n"
                            + "Link: </style.css>; rel=preload\r\n\r\n"
                            + "HTTP/1.1 200 OK\r\n"
                            + "Content-Length: 2\r\n\r\n"
                            + "hi").getBytes("UTF-8");
                    ServerSocket.write(client, reply, 0, reply.length);
                } catch (Exception ignored) {
                    // The check below reports what the client made of it.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        String outcome;
        try {
            Http.Response response = Http.request("127.0.0.1", port, "GET", "/x", null);
            outcome = response.getStatus() + "/" + response.getBodyAsString();
        } catch (Exception err) {
            outcome = "failed: " + err.getMessage();
        } finally {
            listener.close();
        }
        stub.join(10000);
        check("an interim response is not read as the answer", "200/hi", outcome);
    }

    /**
     * A handler may shut its own server down and still answer.
     *
     * <p>The request that calls stop() is itself one of the things stop() drains,
     * and it cannot be released until stop() returns: the wait ran the whole
     * window out, and the sweep afterwards closed the very descriptor the reply
     * was owed on, so an admin endpoint that shuts the server down answered with
     * a dropped connection. The drain now discounts what the calling handler
     * holds and the sweep leaves its descriptor alone.
     */
    private static void aHandlerCanStopItsOwnServer() throws Exception {
        final HttpServer[] holder = new HttpServer[1];
        holder[0] = HttpServer.start("127.0.0.1", 0, 16, 1, new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                // The drain window is long on purpose: if the shutdown waits for
                // this request, the client below waits with it and the elapsed
                // time says so.
                holder[0].stop(20000);
                return HttpServer.Response.text(200, "stopping");
            }
        });
        long started = System.currentTimeMillis();
        String body;
        try {
            body = httpGetBody("127.0.0.1", holder[0].getPort(), "/shutdown");
        } catch (Exception err) {
            body = "failed: " + err.getMessage();
        }
        long elapsed = System.currentTimeMillis() - started;
        check("a handler that stops its server still answers", "stopping", body);
        check("and does not wait out the drain window for itself", "true",
                String.valueOf(elapsed < 10000));
    }

    /**
     * Two shutdowns are one teardown.
     *
     * <p>stop() had no claim, so two overlapping paths -- a signal handler and an
     * application's own cleanup is the ordinary pair -- both snapshotted the same
     * descriptors and ran the raw close that bypasses drop()'s ownership check.
     * The first close releases the number and the second closes whatever has
     * since been given it, and both also reach the unsynchronized Tls.close().
     *
     * <p>IF THIS REGRESSES, THE FAILURE WILL NOT LOOK LIKE THIS CHECK. The second
     * close lands on a descriptor some LATER connection has been given, so what
     * is seen is an unrelated check dying with "Socket write failed" -- measured,
     * three runs out of three, from oneLateBodyRequest several checks further on.
     * That is the defect's nature rather than a flaw in the check: a close that
     * hits somebody else's socket cannot be caught by the connection it belongs
     * to.
     */
    private static void twoShutdownsAreOneTeardown() throws Exception {
        final HttpServer server = HttpServer.start("127.0.0.1", 0, 16, 1,
                new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) {
                        return HttpServer.Response.text(200, "ok");
                    }
                });
        int port = server.getPort();
        // It serves before the shutdown, so the check below cannot pass against a
        // server that never worked.
        String before = httpGetBody("127.0.0.1", port, "/x");
        Thread other = new Thread(new Runnable() {
            public void run() {
                server.stop(2000);
            }
        });
        other.start();
        server.stop(2000);
        other.join(30000);
        String after;
        try {
            Tcp conn = Tcp.connect("127.0.0.1", port, 2000);
            conn.close();
            after = "still listening";
        } catch (Exception expected) {
            after = "stopped";
        }
        check("the server answered before the shutdown", "ok", before);
        check("two concurrent stops leave it stopped, and the process alive",
                "stopped", after);
    }

    /**
     * A server that names no auth plugin still gets the default one.
     *
     * <p>CLIENT_SECURE_CONNECTION without CLIENT_PLUGIN_AUTH is what the older
     * MySQL-compatible servers advertise. Their scramble ends with a NUL and no
     * plugin name follows it, so reading whatever was left found that terminator
     * and took it for an EMPTY plugin name -- which nothing implements, so the
     * connection was refused as unsupported rather than proceeding with
     * mysql_native_password.
     */
    private static void aServerThatNamesNoPluginStillConnects() throws Exception {
        check("a greeting without plugin auth still connects", "connected",
                mySqlGreetingOutcome(false));
        // The control: the same stub that DOES name a plugin still works, so this
        // cannot pass by ignoring the greeting.
        check("and one that names a plugin still connects", "connected",
                mySqlGreetingOutcome(true));
    }

    private static String mySqlGreetingOutcome(final boolean pluginAuth) throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = listener.accept();
                    if(client >= 0) {
                        mySqlStubSession(client, pluginAuth);
                    }
                } catch (Exception ignored) {
                    // The outcome below is what this check reports.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        String outcome;
        try {
            Database db = Database.open("mysql://u:pw@127.0.0.1:" + listener.getPort()
                    + "/db?sslmode=disable");
            db.close();
            outcome = "connected";
        } catch (Exception refused) {
            outcome = "refused: " + refused.getMessage();
        } finally {
            listener.close();
        }
        stub.join(10000);
        return outcome;
    }

    /**
     * A percent-escape in a database URL has to spell text.
     *
     * <p>Each triplet can be valid while the run they form is not UTF-8:
     * "%C3%28" is a lead byte followed by something that cannot continue it. new
     * String does not refuse that, it substitutes U+FFFD -- so the client
     * authenticates with a password the URL does not contain, and what the
     * operator sees is a remote authentication failure rather than a malformed
     * setting.
     */
    private static void aMalformedEscapeInADatabaseUrlIsRefused() throws Exception {
        String refusal;
        try {
            Database db = Database.open("postgres://u:p%C3%28ss@127.0.0.1:1/db?sslmode=disable");
            db.close();
            refusal = "accepted";
        } catch (Exception expected) {
            String message = String.valueOf(expected.getMessage());
            refusal = message.indexOf("not valid UTF-8") >= 0 ? "refused"
                    : "refused for another reason: " + message;
        }
        // THE CONTROL: a well-formed escape of the same shape still decodes, so
        // this cannot pass by refusing every escape. Port 1 refuses the
        // connection, which is a different failure and says the URL was read.
        String wellFormed;
        try {
            Database db = Database.open("postgres://u:p%C3%A9ss@127.0.0.1:1/db?sslmode=disable");
            db.close();
            wellFormed = "connected";
        } catch (Exception expected) {
            String message = String.valueOf(expected.getMessage());
            wellFormed = message.indexOf("not valid UTF-8") >= 0 ? "refused as malformed"
                    : "read the URL and failed to connect";
        }
        check("a malformed percent-escape in a database URL is refused",
                "refused", refusal);
        check("while a well-formed one is decoded",
                "read the URL and failed to connect", wellFormed);
        // AND A '%' THAT IS NOT AN ESCAPE AT ALL, which is the earlier half of the
        // same rule: a bare, truncated or non-hex one used to be appended as a
        // literal, so "p%ZZ" and "p%2" became passwords holding a percent sign and
        // the client authenticated with a value the URL does not contain. The
        // request target takes the opposite decision on purpose -- browsers do
        // send a bare '%' -- and a database URL is configuration somebody typed,
        // with no such traffic to keep working.
        check("a non-hex escape is refused", "refused",
                escapeVerdict("postgres://u:p%ZZss@127.0.0.1:1/db?sslmode=disable"));
        check("and a truncated one", "refused",
                escapeVerdict("postgres://u:pass%2@127.0.0.1:1/db?sslmode=disable"));
        check("and a bare percent", "refused",
                escapeVerdict("postgres://u:pass%@127.0.0.1:1/db?sslmode=disable"));
        // The control again, for the new rule: %41 is 'A' and must still decode.
        check("a well-formed escape is still not a complaint",
                "read the URL", escapeVerdict("postgres://u:p%41ss@127.0.0.1:1/db?sslmode=disable"));
    }

    /** "refused" when the URL's escapes are rejected, "read the URL" otherwise. */
    private static String escapeVerdict(String url) {
        try {
            Database db = Database.open(url);
            db.close();
            return "read the URL";
        } catch (Exception err) {
            String message = String.valueOf(err.getMessage());
            return message.indexOf("hex digits") >= 0 ? "refused" : "read the URL";
        }
    }

    /**
     * A connection field cannot smuggle a second field into the handshake.
     *
     * <p>Both wire protocols end each startup field with a NUL, so a NUL inside
     * a value does not truncate it -- it ENDS that field and the rest becomes the
     * next one. A database name of "app", a NUL, "application_name", a NUL and
     * "allowed.tenant" selects the app database and sets a startup parameter
     * nobody asked for, while the whole string still passes a suffix check on the
     * name the application thought it was connecting to.
     *
     * <p>The refusal happens while the packet is being built, before anything is
     * written, so a listener that only accepts is enough to reach it.
     */
    private static void aConnectionFieldCannotHoldASecondField() throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        // ACCEPTS AND HANGS UP. Without the guard the startup packet really is
        // written -- that is the defect -- and the client then waits for an auth
        // reply, so a silent listener would leave a REGRESSION hanging this suite
        // instead of failing it. Closing makes that path fail at once, and the
        // outcome below says which of the two happened.
        Thread hangUp = new Thread(new Runnable() {
            public void run() {
                try {
                    int client = listener.accept();
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                } catch (Exception ignored) {
                    // The check below reports what the client made of it.
                }
            }
        });
        hangUp.start();
        String postgres;
        try {
            Database db = Database.open("postgres://u:pw@127.0.0.1:" + listener.getPort()
                    + "/app\u0000application_name\u0000allowed.tenant?sslmode=disable");
            db.close();
            postgres = "accepted";
        } catch (Exception refused) {
            String message = String.valueOf(refused.getMessage());
            postgres = message.indexOf("NUL") >= 0 ? "refused"
                    : "refused for another reason: " + message;
        } finally {
            listener.close();
        }
        hangUp.join(10000);

        final ServerSocket mysqlListener = ServerSocket.bind("127.0.0.1", 0, 1);
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = mysqlListener.accept();
                    if(client >= 0) {
                        mySqlStubSession(client);
                    }
                } catch (Exception ignored) {
                    // The check below reports what the client made of it.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        String mysql;
        try {
            Database db = Database.open("mysql://u:pw@127.0.0.1:" + mysqlListener.getPort()
                    + "/app\u0000application_name\u0000allowed.tenant?sslmode=disable");
            db.close();
            mysql = "accepted";
        } catch (Exception refused) {
            String message = String.valueOf(refused.getMessage());
            mysql = message.indexOf("NUL") >= 0 ? "refused"
                    : "refused for another reason: " + message;
        } finally {
            mysqlListener.close();
        }
        stub.join(10000);
        check("a PostgreSQL connection field holding a NUL is refused", "refused", postgres);
        check("a MySQL connection field holding a NUL is refused", "refused", mysql);
    }

    /**
     * Every remaining string that crosses to a native refuses a NUL.
     *
     * <p>Six of these were reported one at a time, so this is the whole surface
     * rather than the next instance: the natives taking a String in the packaged
     * runtime are Web (method, url), FileIo (root, relative, path), Http2
     * (status), Tcp (host, caFile), ServerSocket (host), Db (path, sql, bound
     * value) and Tls (certPath, keyPath). Http2's status comes from an int and
     * Db's bound VALUE is passed with its encoded length, so those two cannot
     * truncate; the rest are checked, and these are the ones no other check in
     * this file already drives.
     */
    private static void everyNativeStringRefusesANul() throws Exception {
        String tlsCert;
        try {
            Tls.create("/tmp/cn1-cert\u0000.pem", "/tmp/cn1-key.pem");
            tlsCert = "accepted";
        } catch (java.io.IOException expected) {
            tlsCert = String.valueOf(expected.getMessage()).indexOf("NUL") >= 0
                    ? "refused" : "other: " + expected.getMessage();
        }
        String tlsKey;
        try {
            Tls.create("/tmp/cn1-cert.pem", "/tmp/cn1-key\u0000.pem");
            tlsKey = "accepted";
        } catch (java.io.IOException expected) {
            tlsKey = String.valueOf(expected.getMessage()).indexOf("NUL") >= 0
                    ? "refused" : "other: " + expected.getMessage();
        }
        String statement;
        String dir = "/tmp/cn1-selftest-sql-" + System.currentTimeMillis();
        new java.io.File(dir).mkdirs();
        Db db = Db.open(dir + "/t.db");
        try {
            db.execute("CREATE TABLE notes(id, owner)", null);
            // The shape that matters: the cut leaves a COMPLETE statement whose
            // authorization clause has gone.
            db.execute("DELETE FROM notes WHERE id='1'\u0000 AND owner='bob'", null);
            statement = "accepted";
        } catch (java.io.IOException expected) {
            statement = String.valueOf(expected.getMessage()).indexOf("NUL") >= 0
                    ? "refused" : "other: " + expected.getMessage();
        } finally {
            db.close();
            new java.io.File(dir + "/t.db").delete();
            new java.io.File(dir).delete();
        }
        // The Java SE arm has no TLS at all -- there is no native to protect and
        // create() refuses everything with one message -- so the two path checks
        // are the translated arm's. Reported rather than silently skipped, so a
        // run that does not exercise them says so.
        if(String.valueOf(tlsCert).indexOf("not available") >= 0) {
            note("TLS path checks skipped: this runtime has no TLS to configure");
        } else {
            check("a TLS certificate path holding a NUL is refused", "refused", tlsCert);
            check("a TLS private key path holding a NUL is refused", "refused", tlsKey);
        }
        check("an SQL statement holding a NUL is refused", "refused", statement);
    }

    /**
     * A database path that would name a different file as a C string opens
     * nothing.
     *
     * <p>sqlite3_open takes the path as a C string, so a NUL inside it ends the
     * path there: "allowed.db" followed by a NUL and "/ignored" opens allowed.db
     * while a caller that checked the directory it was handed approved something
     * else. That is the fifth string in this runtime that crosses to a native and
     * is not the string the native reads.
     */
    private static void aTruncatingDatabasePathOpensNothing() throws Exception {
        String dir = "/tmp/cn1-selftest-db-" + System.currentTimeMillis();
        new java.io.File(dir).mkdirs();
        String refusal;
        try {
            Db wrong = Db.open(dir + "/allowed.db\u0000/ignored");
            wrong.close();
            refusal = "opened";
        } catch (java.io.IOException expected) {
            refusal = "refused";
        }
        // THE CONTROL: the same path without the NUL still opens, so this cannot
        // pass by refusing everything.
        boolean honestOpened;
        Db honest = Db.open(dir + "/allowed.db");
        try {
            honest.execute("CREATE TABLE t(x)", null);
            honestOpened = true;
        } finally {
            honest.close();
        }
        new java.io.File(dir + "/allowed.db").delete();
        new java.io.File(dir).delete();
        check("a database path holding a NUL opens nothing", "refused", refusal);
        check("while the same path without one still opens", "true",
                String.valueOf(honestOpened));
    }

    /**
     * If-Range is an EXACT match, and a later date does not authorise a range.
     *
     * <p>Pinned because a review asked for the opposite, reasoning that a file
     * older than the client's validator cannot have changed since it -- which is
     * the rule for If-Unmodified-Since. RFC 7233 3.2 singles out the difference:
     * the If-Range comparison is by exact match "including when the validator is
     * an HTTP-date". The strong comparison is what stops a file whose timestamp
     * went backwards, restored from a backup, from having fresh bytes stapled
     * onto the prefix a client already holds.
     */
    private static void aLaterIfRangeDateDoesNotAuthoriseARange() throws Exception {
        String dir = "/tmp/cn1-selftest-ifrange-" + System.currentTimeMillis();
        new java.io.File(dir).mkdirs();
        java.io.FileOutputStream out = new java.io.FileOutputStream(dir + "/payload.txt");
        try {
            out.write("0123456789".getBytes("UTF-8"));
        } finally {
            out.close();
        }
        final StaticFiles files = new StaticFiles(dir, "", null, null);
        HttpServer server = HttpServer.start("127.0.0.1", 0, 16, 1, new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                HttpServer.Response served = files.handle(request);
                return served == null ? HttpServer.Response.text(404, "not ours") : served;
            }
        });
        String matching;
        String later;
        try {
            String head = httpHead(server.getPort(), "/payload.txt");
            String lastModified = headerOf(head, "Last-Modified");
            matching = statusOf(httpRangeRequest(server.getPort(), "/payload.txt",
                    "bytes=0-3", lastModified));
            // Later than the file by seventy years, so "not modified since" holds
            // and only the exact-match rule refuses it.
            later = statusOf(httpRangeRequest(server.getPort(), "/payload.txt",
                    "bytes=0-3", "Wed, 21 Oct 2099 07:28:00 GMT"));
        } finally {
            server.stop(2000);
            new java.io.File(dir + "/payload.txt").delete();
            new java.io.File(dir).delete();
        }
        check("the validator the client was given authorises a range", "206", matching);
        check("a later If-Range date does not", "200", later);
    }

    /**
     * A directory redirect names this origin, whatever the request spelled.
     *
     * <p>The Location was built from the request's own path, leading run and all,
     * so a target of "//name" produced "Location: //name/" -- which a browser
     * reads back as a scheme-relative URL whose AUTHORITY is "name". A static
     * file server's tidy-up redirect became an open redirect to somebody else's
     * site. Backslash counts as the same trap, because the URL parsers browsers
     * use treat it as a separator even though the path grammar does not.
     */
    private static void aDirectoryRedirectStaysOnThisOrigin() throws Exception {
        String dir = System.getProperty("java.io.tmpdir") + "/cn1-redirect-"
                + System.currentTimeMillis();
        new java.io.File(dir).mkdirs();
        new java.io.File(dir + "/evil.example").mkdirs();
        final StaticFiles files = new StaticFiles(dir, "", null, null);
        HttpServer server = HttpServer.start("127.0.0.1", 0, 16, 1, new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                HttpServer.Response served = files.handle(request);
                return served == null ? HttpServer.Response.text(404, "not ours") : served;
            }
        });
        String doubled;
        String single;
        try {
            doubled = headerOf(httpRaw(server.getPort(),
                    "GET //evil.example HTTP/1.1\r\nHost: x\r\n"
                    + "Connection: close\r\n\r\n"), "Location");
            // The control: an ordinary directory redirect is unchanged.
            single = headerOf(httpRaw(server.getPort(),
                    "GET /evil.example HTTP/1.1\r\nHost: x\r\n"
                    + "Connection: close\r\n\r\n"), "Location");
        } finally {
            server.stop(2000);
            new java.io.File(dir + "/evil.example").delete();
            new java.io.File(dir).delete();
        }
        check("a doubled leading slash does not become an authority",
                "/evil.example/", doubled);
        check("and an ordinary directory redirect is unchanged",
                "/evil.example/", single);
    }

    private static String httpHead(int port, String target) throws Exception {
        return httpRaw(port, "HEAD " + target + " HTTP/1.1\r\nHost: x\r\n"
                + "Connection: close\r\n\r\n");
    }

    private static String httpRangeRequest(int port, String target, String range,
            String ifRange) throws Exception {
        return httpRaw(port, "GET " + target + " HTTP/1.1\r\nHost: x\r\n"
                + "Range: " + range + "\r\nIf-Range: " + ifRange + "\r\n"
                + "Connection: close\r\n\r\n");
    }

    private static String httpRaw(int port, String request) throws Exception {
        Tcp conn = Tcp.connect("127.0.0.1", port, 15000);
        try {
            byte[] bytes = request.getBytes("UTF-8");
            conn.write(bytes, 0, bytes.length);
            ByteArrayOutputStream all = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            while(true) {
                int n = conn.read(chunk, 0, chunk.length);
                if(n <= 0) {
                    break;
                }
                all.write(chunk, 0, n);
            }
            return new String(all.toByteArray(), "UTF-8");
        } finally {
            conn.close();
        }
    }

    private static String statusOf(String reply) {
        int space = reply.indexOf(' ');
        return space < 0 || reply.length() < space + 4 ? "unreadable: " + reply
                : reply.substring(space + 1, space + 4);
    }

    /** Walks the head line by line: vm/JavaAPI has no String.split. */
    private static String headerOf(String reply, String name) {
        int at = 0;
        while(at < reply.length()) {
            int end = reply.indexOf("\r\n", at);
            if(end < 0) {
                end = reply.length();
            }
            String line = reply.substring(at, end);
            if(line.length() == 0) {
                return null;                    // the head ends at the blank line
            }
            int colon = line.indexOf(':');
            if(colon > 0 && line.substring(0, colon).equalsIgnoreCase(name)) {
                return line.substring(colon + 1).trim();
            }
            at = end + 2;
        }
        return null;
    }

    /**
     * A bind address that would name a different interface as a C string binds
     * nothing.
     *
     * <p>A NUL ends the name where it becomes a C string, so "127.0.0.1" plus a
     * NUL plus ".example" binds 127.0.0.1 -- and "0.0.0.0" spelled that way binds
     * every interface on the host -- while whoever approved the configured name
     * saw an .example one. The Java SE arm fails such a name at resolution, so
     * the two arms disagreed about which interfaces a configured host means, and
     * the one that binds wider is the packaged one.
     */
    private static void aTruncatingBindAddressBindsNothing() throws Exception {
        String refusal;
        try {
            ServerSocket wrong = ServerSocket.bind("127.0.0.1\u0000.example", 0, 1);
            wrong.close();
            refusal = "bound";
        } catch (Exception expected) {
            String message = String.valueOf(expected.getMessage());
            refusal = message.indexOf("control character") >= 0 ? "refused"
                    : "refused for another reason: " + message;
        }
        // THE CONTROL: the same address without the NUL still binds, and null
        // still means every interface, so this cannot pass by refusing everything.
        ServerSocket honest = ServerSocket.bind("127.0.0.1", 0, 1);
        boolean honestBound = honest.getPort() > 0;
        honest.close();
        ServerSocket wildcard = ServerSocket.bind(null, 0, 1);
        boolean wildcardBound = wildcard.getPort() > 0;
        wildcard.close();
        check("a bind address holding a NUL is refused", "refused", refusal);
        check("while the same address without one still binds", "true",
                String.valueOf(honestBound));
        check("and null still means every interface", "true",
                String.valueOf(wildcardBound));
    }

    /**
     * Closing a socket under a blocked plaintext read still wakes it.
     *
     * <p>close() is how a blocked read gets cancelled, and closing a descriptor is
     * also what hands its NUMBER back to the process -- so a reader still inside
     * recv() with it can be served by whatever socket is opened next. The
     * descriptor is therefore shut down rather than closed while an operation
     * holds it, and the last operation out closes it for real.
     *
     * <p>What that trades is the thing this checks: shutdown has to wake the
     * reader as reliably as close did, or cancelling a read would hang instead.
     * The reuse half is by construction -- the number is not released while
     * anyone is inside a native with it -- and is not observable from here.
     */
    private static void aClosedSocketWakesItsBlockedReader() throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        Thread silent = new Thread(new Runnable() {
            public void run() {
                try {
                    int client = listener.accept();
                    if(client >= 0) {
                        // Accepted and then SAYS NOTHING, which is what leaves the
                        // client's read with nothing to return.
                        Thread.sleep(4000);
                        ServerSocket.closeFd(client);
                    }
                } catch (Exception ignored) {
                    // The check below reports what the reader did.
                }
            }
        });
        silent.start();
        final Tcp conn = Tcp.connect("127.0.0.1", listener.getPort(), 5000);
        final boolean[] returned = new boolean[1];
        Thread reader = new Thread(new Runnable() {
            public void run() {
                byte[] buffer = new byte[64];
                try {
                    conn.read(buffer, 0, buffer.length);
                } catch (Exception expected) {
                    // Closed under it, which is the scenario.
                }
                returned[0] = true;
            }
        });
        reader.start();
        Thread.sleep(300);              // by now it is blocked in the native read
        long started = System.currentTimeMillis();
        conn.close();
        reader.join(10000);
        long woke = System.currentTimeMillis() - started;
        listener.close();
        silent.join(10000);
        check("a blocked read returns when the socket is closed under it", "true",
                String.valueOf(returned[0]));
        check("and it returns promptly rather than waiting for the peer", "true",
                String.valueOf(woke < 3000));
    }

    /**
     * A framed response ends where its framing says, not when the peer hangs up.
     *
     * <p>This client asks for "Connection: close" and used to read to end of
     * stream, so a peer that keeps the connection open anyway -- one that answers
     * keep-alive whatever was asked, or a control endpoint that never hangs up --
     * left it blocked in read() with the whole response already in the buffer.
     * There is no read deadline on the socket, so that is forever: in
     * LambdaRuntime it strands the invocation loop for the life of the instance.
     *
     * <p>The stub therefore answers and then holds the connection open, saying
     * nothing more. The request runs on its own thread so a regression FAILS here
     * instead of hanging the suite -- what is asserted is that the call finished
     * at all, and that what it returned is the whole body rather than a prefix of
     * it, which is the way a change like this breaks in the other direction.
     */
    private static void aFramedResponseEndsAtItsFraming() throws Exception {
        check("a response framed by Content-Length ends there", "answered: hello",
                heldOpenResponse("HTTP/1.1 200 OK\r\nContent-Length: 5\r\n\r\nhello"));
        // The other framing, which ends at its last chunk and terminated trailer
        // section rather than at a byte count.
        check("a chunked response ends at its last chunk", "answered: hello",
                heldOpenResponse("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n"
                        + "3\r\nhel\r\n2\r\nlo\r\n0\r\n\r\n"));
        // A bodiless response ends at the blank line, whatever it declares.
        check("a 204 ends at its header block", "answered: ",
                heldOpenResponse("HTTP/1.1 204 No Content\r\nContent-Length: 5\r\n\r\n"));
        // And an interim response frames nothing, so the one after it decides.
        check("an interim response does not end the message", "answered: hello",
                heldOpenResponse("HTTP/1.1 100 Continue\r\n\r\n"
                        + "HTTP/1.1 200 OK\r\nContent-Length: 5\r\n\r\nhello"));
    }

    /**
     * A null method is GET, and an empty array is not a body.
     *
     * <p>The packaged client copies the body into one byte even when there are
     * none, so that a failed allocation stays distinguishable from an empty one.
     * That made the copy non-null for request(null, url, null, new byte[0]), and
     * handing it to libcurl put the transfer into POST mode -- with no method
     * supplied, which means GET everywhere else including the Java SE twin. The
     * same public call reached a different route, or performed a state change,
     * only once packaged.
     */
    private static void aNullMethodWithAnEmptyBodyIsStillAGet() throws Exception {
        check("a null method and an empty body is a GET", "GET",
                methodSeenBy(null, new byte[0]));
        // The control: an explicit verb keeps its meaning, empty body and all.
        check("and an explicit POST is still a POST", "POST",
                methodSeenBy("POST", new byte[0]));
    }

    /** The method a listener actually sees for one Web.request call. */
    private static String methodSeenBy(String method, byte[] body) throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        final String[] seen = new String[1];
        seen[0] = "nothing";
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = listener.accept();
                    if(client < 0) {
                        return;
                    }
                    ServerSocket.setTimeout(client, 5000);
                    byte[] in = new byte[4096];
                    int n = ServerSocket.read(client, in, 0, in.length);
                    String text = n > 0 ? new String(in, 0, n, "UTF-8") : "";
                    int space = text.indexOf(' ');
                    seen[0] = space > 0 ? text.substring(0, space) : "unreadable";
                    byte[] ok = ("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n"
                            + "Connection: close\r\n\r\nok").getBytes("UTF-8");
                    ServerSocket.write(client, ok, 0, ok.length);
                } catch (Exception ignored) {
                    // Reported through seen[0], which stays "nothing".
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        try {
            Web.request(method, "http://127.0.0.1:" + listener.getPort() + "/probe",
                    null, body);
        } catch (Exception ignored) {
            // The method the peer saw is the answer, not whether the call
            // succeeded.
        } finally {
            listener.close();
        }
        stub.join(5000);
        return seen[0];
    }

    /**
     * A redirect does not carry the caller's BODY to another host.
     *
     * <p>Redirects are followed freely when the caller supplied no headers, on the
     * reasoning that there is then nothing to leak. A body is something to leak: a
     * 307 and a 308 preserve the method and the payload -- that is exactly what
     * separates them from a 301 or 302, which become a GET -- so a POST redirected
     * off-domain arrives at the new host complete. libcurl drops Authorization
     * when the host changes and has never done the equivalent for a body, because
     * nothing can know what is in one. A bodied request is also the
     * state-changing kind, so following one blindly can perform it twice, the
     * second time somewhere the caller never named.
     *
     * <p>The destination is named "localhost" while the origin is "127.0.0.1", so
     * this is a different host by name on either arm, whichever way each expresses
     * "same host only". The control is a bodiless GET through a 302, which both
     * arms follow: without it this check would pass just as well with redirects
     * switched off altogether.
     */
    private static void aRedirectDoesNotCarryTheBodyOffHost() throws Exception {
        check("a 307 with a body is not followed off-host", "307 nothing",
                redirectOutcome("POST", "307 Temporary Redirect",
                        "s3cret-payload".getBytes("UTF-8")));
        check("and a bodiless request still follows one", "200 a request arrived",
                redirectOutcome("GET", "302 Found", null));
        // AND THE METHOD COUNTS TOO. An empty DELETE leaks nothing, and a 307
        // preserves the method, so following it performs the delete on a host the
        // caller never named. RFC 9110 4.2.1: GET and HEAD are the safe ones.
        check("an empty DELETE is not followed off-host either", "307 nothing",
                redirectOutcome("DELETE", "307 Temporary Redirect", null));
        check("nor is an empty PUT", "307 nothing",
                redirectOutcome("PUT", "307 Temporary Redirect", null));
    }

    /**
     * A response past the ceiling is refused, and refusing it does not cost twice
     * the ceiling first.
     *
     * <p>The read buffer doubled whenever it filled, and the byte count that
     * enforces CN1_HTTP_MAX_RESPONSE_MB ran after: a response at the limit had
     * already bought an array of twice it, so the reply to 64 MB of unwanted body
     * was a 128 MB allocation and, on a small heap, an OutOfMemoryError instead of
     * the IOException the setting promises. At a large configured limit the
     * doubling overflows and the array size goes negative. The growth stops one
     * byte past the ceiling now -- enough for the count to still see the byte that
     * proves the response too long.
     *
     * <p>What this check can show is the refusal and its wording; the size of the
     * allocation behind it is not visible from here, so the cap on the growth is
     * argued by reading the code rather than by this. It runs only when the
     * ceiling is small, since the default is 64 MB.
     */
    private static void aResponsePastTheClientCeilingIsRefused() throws Exception {
        String configured = System.getenv("CN1_HTTP_MAX_RESPONSE_MB");
        int megabytes = 0;
        if(configured != null && configured.length() > 0) {
            try {
                megabytes = Integer.parseInt(configured.trim());
            } catch (NumberFormatException malformed) {
                megabytes = 0;
            }
        }
        if(megabytes <= 0 || megabytes > 4) {
            System.out.println("NOTE client response ceiling check skipped: set "
                    + "CN1_HTTP_MAX_RESPONSE_MB to 4 or less to run it");
            return;
        }
        final int ceiling = megabytes * 1024 * 1024;
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        final int port = listener.getPort();
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = listener.accept();
                    if(client < 0) {
                        return;
                    }
                    ServerSocket.setTimeout(client, 10000);
                    byte[] in = new byte[4096];
                    ServerSocket.read(client, in, 0, in.length);
                    // Twice the ceiling, declared honestly and then sent.
                    int total = ceiling * 2;
                    byte[] head = ("HTTP/1.1 200 OK\r\nContent-Length: " + total
                            + "\r\nConnection: close\r\n\r\n").getBytes("UTF-8");
                    ServerSocket.write(client, head, 0, head.length);
                    byte[] chunk = new byte[65536];
                    for(int iter = 0 ; iter < chunk.length ; iter++) {
                        chunk[iter] = (byte)'x';
                    }
                    int sent = 0;
                    while(sent < total) {
                        int step = total - sent < chunk.length ? total - sent : chunk.length;
                        ServerSocket.write(client, chunk, 0, step);
                        sent += step;
                    }
                } catch (Exception ignored) {
                    // The client refusing part way through is the expected ending.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        String outcome;
        try {
            Http.Response got = Http.get("127.0.0.1", port, "/big");
            outcome = "accepted " + got.getBodyAsString().length();
        } catch (Exception refused) {
            String message = String.valueOf(refused.getMessage());
            outcome = message.indexOf("will read") >= 0 ? "refused"
                    : "other: " + message;
        } finally {
            listener.close();
        }
        stub.join(10000);
        check("a response past the client's ceiling is refused", "refused", outcome);
    }

    /**
     * A database peer that answers the handshake and then stops does not hold the
     * caller for ever.
     *
     * <p>connectTimeout is spent by the time a query goes out. Past that, a read
     * on a blocking descriptor has no deadline of its own, so a peer that stalls
     * mid-conversation holds the calling thread -- a request worker, or a virtual
     * thread's carrier -- until the process ends. socketTimeout is the deadline
     * for the conversation, and it defaults to none because a legitimate query
     * can outlast any number picked here.
     *
     * <p>The stub accepts and says nothing at all, so the driver stalls waiting
     * for the server's first word. What is asserted is that the wait ENDS, and
     * roughly when: a failure that takes the stub's whole twenty seconds is the
     * thing being ruled out.
     */
    private static void aStalledDatabasePeerDoesNotHoldTheCaller() throws Exception {
        final ServerSocket silent = ServerSocket.bind("127.0.0.1", 0, 1);
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = silent.accept();
                    Thread.sleep(20000);
                } catch (Exception ignored) {
                    // The client giving up is the expected ending.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        long started = System.currentTimeMillis();
        String outcome;
        try {
            Database db = Database.open("postgres://u:pw@127.0.0.1:" + silent.getPort()
                    + "/db?sslmode=disable&connectTimeout=5000&socketTimeout=1500");
            db.close();
            outcome = "connected to a peer that said nothing";
        } catch (Exception expected) {
            long spent = System.currentTimeMillis() - started;
            outcome = spent < 8000 ? "gave up in time" : "gave up after " + spent + "ms";
        } finally {
            silent.close();
        }
        stub.join(5000);
        check("a stalled database peer does not hold the caller", "gave up in time",
                outcome);
    }

    /**
     * A peer that accepts and then says nothing does not hold a handler for ever.
     *
     * <p>connectTimeout is spent reaching the port. The handshake after it had no
     * deadline on either arm -- a bare SSL_connect on a blocking descriptor, and
     * a bare startHandshake() -- so a host that completes TCP and then stalls
     * parks the calling thread indefinitely. Every handler opening a connection
     * to a partly failed database does the same, and the pool is bounded, so the
     * server stops answering anything.
     *
     * <p>Driven by a stub that accepts and never speaks TLS. It runs only when
     * CN1_TLS_HANDSHAKE_MS names a short budget, because the default is fifteen
     * seconds and this suite should not spend them; the harness that runs it in
     * CI sets it. Asserted on the budget being SPENT rather than on any message:
     * the handshake failing is the point, and it has to happen in about the time
     * configured rather than not at all.
     */
    private static void aStalledPeerDoesNotHoldTheHandshake() throws Exception {
        String configured = System.getenv("CN1_TLS_HANDSHAKE_MS");
        int budget = 0;
        if(configured != null && configured.length() > 0) {
            try {
                budget = Integer.parseInt(configured.trim());
            } catch (NumberFormatException malformed) {
                budget = 0;
            }
        }
        if(budget <= 0 || budget > 5000) {
            System.out.println("NOTE TLS handshake budget check skipped: set "
                    + "CN1_TLS_HANDSHAKE_MS to 5000 or less to run it");
            return;
        }
        final ServerSocket silent = ServerSocket.bind("127.0.0.1", 0, 1);
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = silent.accept();
                    // AND NOTHING ELSE. The TCP connection is up and no TLS record
                    // will ever arrive, which is the shape being bounded.
                    Thread.sleep(20000);
                } catch (Exception ignored) {
                    // The client giving up is the expected ending.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        String outcome;
        long started = System.currentTimeMillis();
        Tcp socket = Tcp.connect("127.0.0.1", silent.getPort(), 5000);
        try {
            socket.startTls("127.0.0.1");
            outcome = "handshook with a peer that said nothing";
        } catch (Exception refused) {
            long spent = System.currentTimeMillis() - started;
            // Generous: the budget, plus room for a loaded machine. What is being
            // ruled out is "never", not a hundred milliseconds either way.
            outcome = spent < budget + 4000 ? "gave up in time"
                    : "gave up after " + spent + "ms";
        } finally {
            socket.close();
            silent.close();
        }
        stub.join(5000);
        check("a handshake with a silent peer is bounded", "gave up in time", outcome);
    }

    /**
     * A tunable below its floor takes the default, and the floor is not zero by
     * accident.
     *
     * <p>The connection ceiling reads 0 as "no ceiling", which is a real thing to
     * want, and the admission check applies the number only when it is positive.
     * A negative therefore removed the ceiling too -- silently, and from a typo
     * rather than a decision. The clamp is what separates the two, and its own
     * comment says it is package visible so that this check can reach it; until
     * now nothing did.
     */
    private static void aTunableBelowItsFloorTakesTheDefault() throws Exception {
        // Answers negative when the value is refused, and the value when it stands.
        check("a ceiling of zero is a deliberate no-ceiling", "0",
                String.valueOf(FileCountProbe.clamp("CN1_HTTP_MAX_CONNECTIONS", 0, 0)));
        check("a negative ceiling is refused", "true",
                String.valueOf(FileCountProbe.clamp("CN1_HTTP_MAX_CONNECTIONS", -1, 0) < 0));
        check("and an ordinary one stands", "4096",
                String.valueOf(FileCountProbe.clamp("CN1_HTTP_MAX_CONNECTIONS", 4096, 0)));
        // A floor above zero is the other use of the same clamp: a divisor of 0
        // throws on one arm and answers 0 on the other, so neither may pass.
        check("a divisor below its floor is refused", "true",
                String.valueOf(FileCountProbe.clamp("CN1_HTTP_READ_RATE", 0, 1) < 0));
    }

    /**
     * One ready descriptor is one slot, whatever a poller counts.
     *
     * <p>kqueue registers a filter at a time and reports one event per FILTER, so
     * a descriptor watched for READ and WRITE that becomes both at once is
     * reported twice -- while epoll reports it once with a combined mask, and a
     * Selector answers with one key. Copied straight through, that is one
     * connection handed to two workers, each believing it owns it.
     */
    private static void oneReadyDescriptorIsOneSlot() throws Exception {
        Reactor reactor = Reactor.create();
        ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        Tcp conn = Tcp.connect("127.0.0.1", listener.getPort(), 5000);
        int peer = listener.accept();
        int[] ready = new int[8];
        try {
            // Readable and writable at the same moment, watched for both.
            byte[] hello = "hi".getBytes("UTF-8");
            conn.write(hello, 0, hello.length);
            // WAIT FOR THE BYTES FIRST. Registering before they land leaves only
            // the WRITE filter ready, and then one event is the right answer for
            // the wrong reason -- the first version of this check passed just as
            // well with the coalescing removed, which is no check at all.
            ServerSocket.awaitReadable(peer, 2000);
            reactor.add(peer, Reactor.READ | Reactor.WRITE);
            int reported = reactor.await(ready, 1000);
            check("a descriptor ready both ways is reported once", "1",
                    String.valueOf(reported));
            check("and it is the descriptor that was watched", String.valueOf(peer),
                    String.valueOf(ready[0]));
        } finally {
            reactor.remove(peer);
            reactor.close();
            ServerSocket.closeFd(peer);
            conn.close();
            listener.close();
        }
    }

    /**
     * Runs one redirect scenario and answers "status what-the-second-host-saw".
     *
     * <p>Two listeners: the first answers the named 3xx pointing at the second,
     * and the second records whether anything reached it at all.
     */
    private static String redirectOutcome(String method, String status, byte[] body)
            throws Exception {
        final ServerSocket target = ServerSocket.bind("127.0.0.1", 0, 1);
        final ServerSocket origin = ServerSocket.bind("127.0.0.1", 0, 1);
        final int targetPort = target.getPort();
        final String[] atTarget = new String[1];
        atTarget[0] = "nothing";
        final String line = status;
        Thread targetStub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = target.accept();
                    if(client < 0) {
                        return;
                    }
                    ServerSocket.setTimeout(client, 5000);
                    byte[] in = new byte[8192];
                    int n = ServerSocket.read(client, in, 0, in.length);
                    String text = n > 0 ? new String(in, 0, n, "UTF-8") : "";
                    atTarget[0] = text.indexOf("s3cret-payload") >= 0
                            ? "the body arrived" : "a request arrived";
                    byte[] ok = ("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n"
                            + "Connection: close\r\n\r\nok").getBytes("UTF-8");
                    ServerSocket.write(client, ok, 0, ok.length);
                } catch (Exception ignored) {
                    // Never contacted is the answer this check is looking for.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        Thread originStub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = origin.accept();
                    if(client < 0) {
                        return;
                    }
                    ServerSocket.setTimeout(client, 5000);
                    byte[] in = new byte[8192];
                    ServerSocket.read(client, in, 0, in.length);
                    byte[] moved = ("HTTP/1.1 " + line + "\r\nLocation: http://localhost:"
                            + targetPort + "/moved\r\nContent-Length: 0\r\n"
                            + "Connection: close\r\n\r\n").getBytes("UTF-8");
                    ServerSocket.write(client, moved, 0, moved.length);
                } catch (Exception ignored) {
                    // Reported through the result the caller gets.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        targetStub.start();
        originStub.start();
        String outcome;
        try {
            Web.Result r = Web.request(method, "http://127.0.0.1:" + origin.getPort()
                    + "/start", null, body);
            outcome = (r == null ? "no result" : String.valueOf(r.getStatus()))
                    + " " + atTarget[0];
        } catch (Exception err) {
            outcome = "failed: " + err.getMessage();
        } finally {
            origin.close();
            target.close();
        }
        originStub.join(5000);
        targetStub.join(5000);
        return outcome;
    }

    /**
     * A chunk size the peer chose cannot be made to wrap this client's arithmetic.
     *
     * <p>parseChunkSize refuses only what overflows its own accumulator, so a peer
     * may legally name 7fffffff. The walk that decides where a chunked message
     * ends added that to the position of the chunk's first byte, wrapped negative,
     * and read the wrap as "the chunk has not arrived yet" -- then put the
     * negative back as its cursor. Measured with the fix removed, the next pass
     * indexes the buffer at -2147483590 and the request fails with that number as
     * its entire message; a review reading the same code predicted an endless
     * re-parse of the one size line, which is what it would be if the scan did not
     * clamp its start. Either way the peer chose a number that broke this client's
     * arithmetic, which is the part worth refusing.
     *
     * <p>The stub hangs up after the size line, so with the arithmetic fixed the
     * message is simply truncated and says so. The assertion is that sentence
     * rather than "it failed", because the failure it replaces IS a failure and
     * would satisfy a weaker check while proving nothing.
     */
    private static void aChunkSizeCannotWrapTheWalk() throws Exception {
        check("a chunk size near the int ceiling does not break the walk",
                "failed: Truncated chunked response: chunk runs past the body",
                heldOpenResponse("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n"
                        + "\r\n7fffffff\r\n", false));
        // The control: an ordinary size through the same path, truncated the same
        // way, so this cannot pass by refusing every chunked response.
        check("and an ordinary truncated chunk still says the same thing",
                "failed: Truncated chunked response: chunk runs past the body",
                heldOpenResponse("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n"
                        + "\r\n5\r\nhel", false));
    }

    /**
     * Sends exactly this response to one request and then holds the connection
     * open, and answers what the client made of it within a generous bound.
     */
    private static String heldOpenResponse(String response) throws Exception {
        return heldOpenResponse(response, true);
    }

    /**
     * The same, with a choice about the ending: hold the connection open, or hang
     * up once the bytes are out.
     */
    private static String heldOpenResponse(String response, boolean hold)
            throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        final int port = listener.getPort();
        final String payload = response;
        final boolean holdOpen = hold;
        final boolean[] holding = new boolean[1];
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = listener.accept();
                    if(client < 0) {
                        return;
                    }
                    ServerSocket.setTimeout(client, 10000);
                    // The request, far enough to know it arrived.
                    byte[] in = new byte[4096];
                    ServerSocket.read(client, in, 0, in.length);
                    byte[] out = payload.getBytes("UTF-8");
                    ServerSocket.write(client, out, 0, out.length);
                    // AND THEN NOTHING. No close, which is the whole point --
                    // except where the case under test is what happens at the end
                    // of a truncated message.
                    holding[0] = true;
                    if(holdOpen) {
                        Thread.sleep(4000);
                    }
                } catch (Exception ignored) {
                    // The client hanging up first is a perfectly good ending.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        final String[] answer = new String[1];
        Thread caller = new Thread(new Runnable() {
            public void run() {
                try {
                    Http.Response got = Http.get("127.0.0.1", port, "/x");
                    answer[0] = "answered: " + got.getBodyAsString();
                } catch (Exception err) {
                    answer[0] = "failed: " + err.getMessage();
                }
            }
        });
        caller.start();
        caller.join(3000);
        String outcome = answer[0] == null
                ? "still blocked in read after the whole response arrived"
                : answer[0];
        listener.close();
        stub.join(6000);
        caller.join(6000);
        return outcome;
    }

    /**
     * A removed descriptor is removed, even when only one filter was on it.
     *
     * <p>kqueue takes one change per filter, and the removal asked for both in a
     * single call. A descriptor registered for WRITE alone has no read filter, so
     * the first change answered ENOENT -- and with no eventlist to report
     * per-change errors into, kevent() stops there and never applies the second.
     * The write filter stayed, remove() reported success, and await() went on
     * handing the caller a descriptor it had deregistered.
     */
    private static void removingADescriptorRemovesEveryFilter() throws Exception {
        Reactor reactor = Reactor.create();
        ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        Tcp conn = Tcp.connect("127.0.0.1", listener.getPort(), 5000);
        int peer = listener.accept();
        int[] ready = new int[8];
        try {
            // WRITE only, which is what leaves the read filter absent.
            reactor.add(peer, Reactor.WRITE);
            int before = reactor.await(ready, 1000);
            reactor.remove(peer);
            int after = reactor.await(ready, 300);
            check("a writable descriptor is reported while it is watched", "1",
                    String.valueOf(before));
            check("and not after it is removed", "0", String.valueOf(after));
        } finally {
            reactor.close();
            ServerSocket.closeFd(peer);
            conn.close();
            listener.close();
        }
    }

    /**
     * A modify REPLACES a descriptor's interest; it does not add to it.
     *
     * <p>That is what EPOLL_CTL_MOD does with its mask and what interestOps() does
     * on a Selector, and kqueue has no equivalent: each filter is registered on
     * its own, so changing READ to WRITE left EVFILT_READ exactly where it was.
     * The descriptor then came back for readability the caller had explicitly
     * stopped asking about -- and since kevent reports one event per FILTER, the
     * same descriptor arrived twice in one await, which on a kqueue several hosts
     * share hands a connection to a host that was never told to expect it.
     *
     * <p>Counted rather than inspected because await() answers with descriptors
     * and not with what made them ready: one ready descriptor reported twice is
     * the shape of the defect, and the count is where it shows.
     */
    private static void modifyReplacesTheInterestRatherThanAddingToIt()
            throws Exception {
        Reactor reactor = Reactor.create();
        ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        Tcp conn = Tcp.connect("127.0.0.1", listener.getPort(), 5000);
        int peer = listener.accept();
        int[] ready = new int[8];
        try {
            // Readable AND writable at once, which is what makes the leak visible.
            byte[] hello = "hi".getBytes("UTF-8");
            conn.write(hello, 0, hello.length);
            reactor.add(peer, Reactor.READ);
            int readable = reactor.await(ready, 1000);
            reactor.modify(peer, Reactor.WRITE);
            int afterModify = reactor.await(ready, 1000);
            check("a descriptor watched for READ is reported", "1",
                    String.valueOf(readable));
            // Two would mean both filters fired: the WRITE that was asked for and
            // the READ that should have been replaced.
            check("and after modify it is reported once, for one interest", "1",
                    String.valueOf(afterModify));
        } finally {
            reactor.remove(peer);
            reactor.close();
            ServerSocket.closeFd(peer);
            conn.close();
            listener.close();
        }
    }

    /**
     * The other direction of the same rule: level-triggered, then modified to
     * one-shot, must actually stop reporting.
     *
     * <p>Kept apart from the check above because it needs a descriptor that was
     * never added with ONESHOT -- on the arm this was broken on, one added that
     * way stayed in the bookkeeping set forever, so modifying INTO one-shot
     * looked correct there for the wrong reason and proved nothing.
     */
    private static void modifyingIntoOneShotDisarms() throws Exception {
        Reactor reactor = Reactor.create();
        ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        Tcp conn = Tcp.connect("127.0.0.1", listener.getPort(), 5000);
        int peer = listener.accept();
        int[] ready = new int[8];
        try {
            // Level-triggered: a connected socket is writable, and stays reported.
            reactor.add(peer, Reactor.WRITE);
            int first = reactor.await(ready, 1000);
            int second = reactor.await(ready, 300);
            reactor.modify(peer, Reactor.WRITE | Reactor.ONESHOT);
            int third = reactor.await(ready, 1000);
            int fourth = reactor.await(ready, 300);
            check("a level-triggered write is reported", "1", String.valueOf(first));
            check("and reported again", "1", String.valueOf(second));
            check("modifying into one-shot still reports once", "1",
                    String.valueOf(third));
            check("and then stops", "0", String.valueOf(fourth));
        } finally {
            reactor.remove(peer);
            reactor.close();
            ServerSocket.closeFd(peer);
            conn.close();
            listener.close();
        }
    }

    /**
     * The reactor's zero timeout is a probe, and its one-shot means once.
     *
     * <p>Two rules the arms have to share. A zero timeout is epoll_wait(..., 0)
     * and a zero timespec on kevent -- both answer immediately -- while
     * Selector's zero is select(), which waits. And ONESHOT means the descriptor
     * is reported to exactly one waiter until it is re-armed; on kqueue the WRITE
     * filter was registered without EV_DISPATCH, so it stayed armed and a
     * writable descriptor came back every time, to every host waiting on that
     * kqueue.
     */
    private static void theReactorProbesAndFiresOnce() throws Exception {
        final Reactor reactor = Reactor.create();
        ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        Tcp conn = Tcp.connect("127.0.0.1", listener.getPort(), 5000);
        int peer = listener.accept();
        int[] ready = new int[8];
        String probe;
        try {
            // A connected socket with nothing to read: the only right answer to
            // "anything ready now?" is none, and the only right time is now.
            final boolean[] answered = new boolean[1];
            final int[] count = new int[1];
            Thread prober = new Thread(new Runnable() {
                public void run() {
                    try {
                        count[0] = reactor.await(new int[8], 0);
                    } catch (Exception ignored) {
                        return;
                    }
                    answered[0] = true;
                }
            });
            prober.start();
            prober.join(3000);
            probe = answered[0] ? "answered " + count[0] : "waited";
            prober.join(2000);

            // ONE SHOT: registered for WRITE, which a connected socket is
            // immediately, so the first await reports it. The second must not,
            // because nothing re-armed it.
            reactor.add(peer, Reactor.WRITE | Reactor.ONESHOT);
            int first = reactor.await(ready, 1000);
            int second = reactor.await(ready, 300);
            // AND MODIFY DECIDES IT TOO, in both directions. One-shot is a
            // property of the last call that set a descriptor's interest, which on
            // the translated arms falls out of the call itself -- EPOLL_CTL_MOD
            // carries EPOLLONESHOT, a kevent re-add carries EV_DISPATCH -- while
            // the Java SE arm kept a set of its own that only add() maintained.
            // Modifying back to level-triggered therefore left the descriptor
            // being disarmed after one delivery.
            reactor.modify(peer, Reactor.WRITE);
            int third = reactor.await(ready, 1000);
            int fourth = reactor.await(ready, 300);
            check("a reactor probe answers rather than waiting", "answered 0", probe);
            check("a one-shot write fires once", "1", String.valueOf(first));
            check("and not again until it is re-armed", "0", String.valueOf(second));
            check("modifying back to level-triggered re-arms it", "1",
                    String.valueOf(third));
            check("and level-triggered keeps reporting it", "1",
                    String.valueOf(fourth));
        } finally {
            reactor.remove(peer);
            reactor.close();
            ServerSocket.closeFd(peer);
            conn.close();
            listener.close();
        }
    }

    /**
     * Asking whether a socket is readable RIGHT NOW is not a wait.
     *
     * <p>The translated arm is a poll(), where a zero timeout returns at once and
     * says "nothing there". Selector's zero means the opposite -- select(0) is
     * select(), which waits until something arrives -- so the same call answered
     * one question on the device and hung on the simulator. The probe runs on its
     * own thread and is joined with a bound, so a regression FAILS here instead
     * of stopping the suite.
     */
    private static void aZeroTimeoutReadinessCheckDoesNotWait() throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        Tcp client = Tcp.connect("127.0.0.1", listener.getPort(), 5000);
        final int accepted = listener.accept();
        final boolean[] answered = new boolean[1];
        final boolean[] readable = new boolean[1];
        Thread probe = new Thread(new Runnable() {
            public void run() {
                try {
                    // Nothing has been sent on it, so the only right answer is
                    // "no", and the only right time to give it is now.
                    readable[0] = ServerSocket.awaitReadable(accepted, 0);
                } catch (Exception ignored) {
                    // answered stays false, which the check reports.
                    return;
                }
                answered[0] = true;
            }
        });
        probe.start();
        probe.join(3000);
        boolean returnedPromptly = answered[0];
        boolean saidNo = !readable[0];
        if(accepted >= 0) {
            ServerSocket.closeFd(accepted);
        }
        client.close();
        listener.close();
        probe.join(5000);
        check("a zero timeout answers rather than waiting", "true",
                String.valueOf(returnedPromptly));
        check("and the answer for a silent socket is no", "true",
                String.valueOf(saidNo));
    }

    /**
     * Half an environment credential pair fails the workload, it does not
     * quietly change its identity.
     *
     * <p>A misspelled key in a Kubernetes Secret, or one that failed to mount,
     * leaves one of the two variables set. Reading that as "no environment
     * credentials" sent resolve() on to the container endpoint and then to
     * instance metadata, so the workload ran under the NODE's role -- a
     * different identity with different grants, and no message anywhere saying
     * so. The pair is the unit; the session token is not part of it.
     *
     * <p>Driven through the probe because a process cannot set its own
     * environment, so the public method could only ever test the one case the
     * harness happened to launch it with.
     */
    private static void halfACredentialPairIsRefused() throws Exception {
        check("neither variable set is no environment credential", "none",
                CredentialEndpointProbe.pairVerdictFor(null, null));
        check("an empty pair is the same as an absent one", "none",
                CredentialEndpointProbe.pairVerdictFor("", ""));
        check("an id with no secret is refused", "refused",
                CredentialEndpointProbe.pairVerdictFor("AKIAEXAMPLE", null));
        check("a secret with no id is refused", "refused",
                CredentialEndpointProbe.pairVerdictFor(null, "s3cret"));
        // An empty string is how a Secret that mounted with no value arrives, and
        // it is exactly as broken as an unset one.
        check("and an empty half is a half", "refused",
                CredentialEndpointProbe.pairVerdictFor("AKIAEXAMPLE", ""));
        // The control: a whole pair still resolves, or this check would pass with
        // the environment provider deleted outright.
        check("a whole pair is credentials", "credentials",
                CredentialEndpointProbe.pairVerdictFor("AKIAEXAMPLE", "s3cret"));
    }

    /**
     * A partition whose endpoint cannot be named is refused, not guessed at.
     *
     * <p>endpointFor's comment says the ISO partitions are deliberately not
     * guessed at, because a wrong DNS suffix is a silent misdirection -- and then
     * the commercial suffix was returned for them anyway, which is that
     * misdirection. forRegion refuses them now; forEndpoint still takes a host
     * from its caller, so somebody on one of those networks can name it.
     */
    private static void anUnnameablePartitionIsRefused() throws Exception {
        // THROUGH forRegion, not through the rule on its own. A check that calls
        // the rule directly passes whether or not anything calls it -- measured:
        // removing the call from forRegion left such a check green -- so these go
        // in at the door a caller actually uses. The refusal happens before
        // credentials are resolved, which is what lets this run without any.
        check("an ISO region is refused", "refused", regionVerdict("us-iso-east-1"));
        check("and the other ISO partition too", "refused",
                regionVerdict("us-isob-east-1"));
        check("and the European one", "refused", regionVerdict("eu-isoe-west-1"));
        // The controls: the partitions this runtime CAN name are not refused here,
        // so the rule cannot pass by turning everything away.
        check("a commercial region is not", "not refused", regionVerdict("us-east-1"));
        check("nor is China", "not refused", regionVerdict("cn-north-1"));
        check("nor GovCloud", "not refused", regionVerdict("us-gov-west-1"));
        // And a region whose middle label merely begins with the same letters.
        check("nor one that only looks ISO", "not refused",
                regionVerdict("us-isle-east-1"));
    }

    /**
     * "refused" when forRegion turns the region away for its partition, and
     * "not refused" for anything else -- including the missing-credentials
     * failure that a region this runtime CAN name reaches next.
     */
    private static String regionVerdict(String region) {
        try {
            com.codename1.backend.aws.S3.forRegion(region);
            return "not refused";
        } catch (Exception err) {
            String message = String.valueOf(err.getMessage());
            return message.indexOf("ISO partition") >= 0 ? "refused" : "not refused";
        }
    }

    /**
     * Listing up to nothing does not list everything.
     *
     * <p>The page loop stopped at the limit only while it was positive, so a max
     * of zero or below followed every continuation token to the end of the
     * bucket: the opposite of what was asked, and on a large bucket many round
     * trips and a list of everything in the heap. The endpoint here is a port
     * nothing listens on, so a request that IS made fails -- which is what
     * separates "returned early" from "returned empty after asking".
     */
    private static void listingUpToNothingListsNothing() throws Exception {
        com.codename1.backend.aws.Credentials creds =
                new com.codename1.backend.aws.Credentials("AKIAEXAMPLE", "s3cret", null);
        com.codename1.backend.aws.S3 s3 = com.codename1.backend.aws.S3.forEndpoint(
                creds, "us-east-1", "http://127.0.0.1:1");
        String zero;
        try {
            zero = "listed " + s3.listObjects("bucket", null, 0).size();
        } catch (Exception err) {
            zero = "asked anyway: " + err.getMessage();
        }
        String negative;
        try {
            s3.listObjects("bucket", null, -1);
            negative = "listed";
        } catch (IllegalArgumentException refused) {
            negative = "refused";
        } catch (Exception other) {
            negative = "asked anyway: " + other.getMessage();
        }
        // The control: a positive max DOES go to the network, so the two answers
        // above are about the limit rather than about the method being inert.
        String positive;
        try {
            s3.listObjects("bucket", null, 5);
            positive = "listed";
        } catch (Exception expected) {
            positive = "asked";
        }
        check("a max of zero lists nothing without asking", "listed 0", zero);
        check("a negative max is refused", "refused", negative);
        check("and a positive max still asks", "asked", positive);
    }

    /**
     * A region is resolved in its own AWS partition.
     *
     * <p>AWS China is a separate partition whose S3 endpoints end in
     * amazonaws.com.cn. Building the commercial suffix for every region pointed a
     * cn-north-1 deployment at a hostname that does not serve it -- every request
     * and, worse, every presigned URL, which is handed to somebody else to fetch
     * and outlives the process that made it.
     */
    private static void aRegionResolvesInItsOwnPartition() throws Exception {
        check("a commercial region keeps the commercial suffix",
                "s3.us-east-1.amazonaws.com",
                CredentialEndpointProbe.s3EndpointFor("us-east-1"));
        check("a China region uses the China suffix",
                "s3.cn-north-1.amazonaws.com.cn",
                CredentialEndpointProbe.s3EndpointFor("cn-north-1"));
        check("and so does the other one",
                "s3.cn-northwest-1.amazonaws.com.cn",
                CredentialEndpointProbe.s3EndpointFor("cn-northwest-1"));
        // GovCloud is its own partition and keeps the commercial suffix, so it is
        // a control against matching too eagerly -- and so is a commercial region
        // whose name merely begins with the letters of one that does not.
        check("GovCloud keeps the commercial suffix",
                "s3.us-gov-west-1.amazonaws.com",
                CredentialEndpointProbe.s3EndpointFor("us-gov-west-1"));
        check("a region that merely starts with cn is not the China partition",
                "s3.cnorth-1.amazonaws.com",
                CredentialEndpointProbe.s3EndpointFor("cnorth-1"));
    }

    /**
     * A container credential endpoint off this host must be encrypted.
     *
     * <p>AWS_CONTAINER_CREDENTIALS_FULL_URI is used as the environment gives it.
     * Pointed at an http:// host that is not this container's, the request
     * carries the container authorization token TO that host and brings the
     * role's access key, secret and session token back from it in the clear. The
     * rule is the AWS SDKs': https anywhere, http only to loopback or to the ECS
     * and EKS link-local addresses that are the credential service itself.
     */
    private static void aRemoteCredentialEndpointMustBeEncrypted() throws Exception {
        check("an http endpoint off this host is refused", "refused",
                CredentialEndpointProbe.verdictFor("http://evil.example/creds"));
        check("the same host over https is allowed", "allowed",
                CredentialEndpointProbe.verdictFor("https://evil.example/creds"));
        check("the ECS address is allowed", "allowed",
                CredentialEndpointProbe.verdictFor("http://169.254.170.2/v2/credentials"));
        check("the EKS address is allowed", "allowed",
                CredentialEndpointProbe.verdictFor("http://169.254.170.23/v1/credentials"));
        check("loopback is allowed", "allowed",
                CredentialEndpointProbe.verdictFor("http://127.0.0.1:8080/creds"));
        check("and so is its IPv6 spelling", "allowed",
                CredentialEndpointProbe.verdictFor("http://[::1]:8080/creds"));
        // THE PREFIX TRAP: a NAME that begins with the loopback digits resolves
        // wherever its owner says, so matching "127." as text hands the keys over.
        check("a name that merely starts with 127 is refused", "refused",
                CredentialEndpointProbe.verdictFor("http://127.evil.example/creds"));
        check("and one that merely starts with the ECS address is refused", "refused",
                CredentialEndpointProbe.verdictFor("http://169.254.170.2.evil.example/x"));
        // Userinfo cannot be used to hide the real host either.
        check("a userinfo that looks like loopback is refused", "refused",
                CredentialEndpointProbe.verdictFor("http://127.0.0.1@evil.example/creds"));
        check("a scheme that is neither is refused", "refused",
                CredentialEndpointProbe.verdictFor("file:///etc/passwd"));
    }

    /**
     * A peer that never stops sending is refused rather than held.
     *
     * <p>The reader runs to EOF, so an endpoint that streams without closing grew
     * a buffer until the heap was gone -- and the failure lands on the whole
     * process rather than on the request that caused it. The endpoint does not
     * have to be hostile; a misconfigured one is enough. The sibling client
     * already bounded this, so the two now answer alike.
     */
    private static void anEndlessResponseIsRefused() throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        final int port = listener.getPort();
        final boolean[] stop = new boolean[1];
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = listener.accept();
                    if(client < 0) {
                        return;
                    }
                    byte[] request = new byte[4096];
                    ServerSocket.read(client, request, 0, request.length);
                    byte[] head = ("HTTP/1.1 200 OK\r\n"
                            + "Content-Type: text/plain\r\n\r\n").getBytes("UTF-8");
                    ServerSocket.write(client, head, 0, head.length);
                    // AND THEN IT NEVER STOPS. No length, no close: exactly the
                    // shape that has nothing to judge until EOF arrives.
                    byte[] filler = new byte[64 * 1024];
                    while(!stop[0]) {
                        ServerSocket.write(client, filler, 0, filler.length);
                    }
                } catch (Exception ignored) {
                    // The client refusing is what ends this write loop.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        String outcome;
        try {
            Http.request("127.0.0.1", port, "GET", "/x", null);
            outcome = "read it all";
        } catch (java.io.IOException refused) {
            String message = String.valueOf(refused.getMessage());
            outcome = message.indexOf("this client will read") >= 0 ? "refused"
                    : "refused for another reason: " + message;
        } finally {
            stop[0] = true;
            listener.close();
        }
        stub.join(10000);
        check("a response that never ends is refused", "refused", outcome);
    }

    /**
     * A response that declares its length twice, differently, is refused.
     *
     * <p>The first Content-Length was taken and the rest ignored, so the framing
     * depended on field ORDER: 5 before 10 with ten bytes was accepted and cut to
     * five, and the same two fields the other way round came back a transport
     * error. An unparseable one was treated as absent, which turns off the
     * truncation check entirely. RFC 9112 6.3: a message whose length is invalid
     * or contradictory is invalid.
     */
    private static void aContradictoryLengthIsRefused() throws Exception {
        check("a single length still frames the body", "hello",
                lengthReply("Content-Length: 5", "hello"));
        // The SAME pair both ways round: neither order may be believed.
        check("two different lengths are refused", "refused",
                lengthReply("Content-Length: 5\r\nContent-Length: 10", "helloworld"));
        check("and are refused in the other order too", "refused",
                lengthReply("Content-Length: 10\r\nContent-Length: 5", "helloworld"));
        check("a length that is not a number is refused", "refused",
                lengthReply("Content-Length: five", "hello"));
        check("a negative length is refused", "refused",
                lengthReply("Content-Length: -1", "hello"));
        // Repeating the SAME value is the one duplicate RFC 9112 lets a recipient
        // treat as one, so it must still work.
        check("the same length twice is one length", "hello",
                lengthReply("Content-Length: 5\r\nContent-Length: 5", "hello"));
    }

    /** Serves one canned response with the given length fields. */
    private static String lengthReply(final String lengthFields, final String body)
            throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        final int port = listener.getPort();
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = listener.accept();
                    if(client < 0) {
                        return;
                    }
                    byte[] request = new byte[4096];
                    ServerSocket.read(client, request, 0, request.length);
                    byte[] reply = ("HTTP/1.1 200 OK\r\n" + lengthFields + "\r\n\r\n"
                            + body).getBytes("UTF-8");
                    ServerSocket.write(client, reply, 0, reply.length);
                } catch (Exception ignored) {
                    // The verdict below is what this check reports.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        String outcome;
        try {
            outcome = Http.request("127.0.0.1", port, "GET", "/x", null).getBodyAsString();
        } catch (java.io.IOException refused) {
            String message = String.valueOf(refused.getMessage());
            outcome = message.indexOf("Content-Length") >= 0 ? "refused"
                    : "refused for another reason: " + message;
        } finally {
            listener.close();
        }
        stub.join(10000);
        return outcome;
    }

    /**
     * A transfer coding overrides a length that came with it.
     *
     * <p>RFC 9112 6.1: when a message carries both, the coding wins and a
     * response's Content-Length is ignored -- it describes the decoded body at
     * best, and at worst it is the smuggling attempt the rule exists for.
     * Applying it to the raw chunk framing measured the wrong bytes entirely: a
     * declared 5 cut the wire form to five bytes and the decoder then called a
     * complete response truncated.
     */
    private static void aTransferCodingOverridesTheLength() throws Exception {
        check("a chunked response with a Content-Length is read by its coding",
                "hello", codedReply("Content-Length: 5"));
        // Even a length that contradicts the framing outright is ignored rather
        // than believed, which is the half that would otherwise truncate.
        check("and a contradictory one is still ignored", "hello",
                codedReply("Content-Length: 99"));
    }

    /** Serves a chunked body that also declares a length. */
    private static String codedReply(final String lengthField) throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        final int port = listener.getPort();
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = listener.accept();
                    if(client < 0) {
                        return;
                    }
                    byte[] request = new byte[4096];
                    ServerSocket.read(client, request, 0, request.length);
                    byte[] reply = ("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n"
                            + lengthField + "\r\n\r\n"
                            + "5\r\nhello\r\n0\r\n\r\n").getBytes("UTF-8");
                    ServerSocket.write(client, reply, 0, reply.length);
                } catch (Exception ignored) {
                    // The verdict below is what this check reports.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        String outcome;
        try {
            outcome = Http.request("127.0.0.1", port, "GET", "/x", null).getBodyAsString();
        } catch (Exception err) {
            outcome = "failed: " + err.getMessage();
        } finally {
            listener.close();
        }
        stub.join(10000);
        return outcome;
    }

    /**
     * The static handler says whether it really splices, not whether it wishes
     * it did.
     *
     * <p>The Java SE arm reads into a ByteBuffer and writes it out; it answered
     * "yes" from when it used transferTo, so the shared code took its sendfile
     * branch and a large file made roughly its own size in short-lived arrays
     * instead of using the one reusable buffer -- and isZeroCopy() told callers
     * the kernel was doing the work.
     */
    private static void zeroCopyIsClaimedOnlyWhereItHappens() throws Exception {
        // The translated arm has sendfile(); the simulator does not. Whichever is
        // running, the claim has to match it.
        String expected = VirtualThread.supported() ? "true" : "false";
        check("zero copy is claimed only where it happens", expected,
                String.valueOf(StaticFiles.isZeroCopy()));
    }

    /**
     * A chunked response ends where its framing says, trailers included.
     *
     * <p>RFC 9112 7.1 ends the body with the last chunk, then the trailer
     * section, then a final CRLF. Returning as soon as the zero-size line arrived
     * accepted a peer that died right after "0" as a complete body, so a caller
     * acted on a message that had stopped part way through.
     */
    private static void aChunkedResponseEndsWhereItsFramingSays() throws Exception {
        check("a chunked response with its trailer section terminated is whole",
                "hello", chunkedReply("5\r\nhello\r\n0\r\n\r\n"));
        check("and one with real trailers is too", "hello",
                chunkedReply("5\r\nhello\r\n0\r\nX-Checksum: 7\r\n\r\n"));
        // THE CUT: everything above arrived, and then the peer stopped.
        check("a chunked response cut off after its last chunk is refused",
                "refused", chunkedReply("5\r\nhello\r\n0\r\n"));
        // A HEAD may describe the coding the GET would have used and still send
        // nothing, and so may a 304. The decoder must not be handed those zero
        // bytes and call them truncated: the same RFC 9110 6.4.1 rule that stops
        // the length check stops this.
        check("a HEAD that describes a chunked GET is not read as truncated",
                "200/", chunkedHeadReply("HEAD", 200));
        check("and neither is a 304 that carries the same metadata",
                "304/", chunkedHeadReply("GET", 304));
        check("and one whose trailer is not a header field is refused",
                "refused", chunkedReply("5\r\nhello\r\n0\r\nnot a field\r\n\r\n"));
    }

    /** A bodiless response that still declares a chunked transfer coding. */
    private static String chunkedHeadReply(final String verb, final int status)
            throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        final int port = listener.getPort();
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = listener.accept();
                    if(client < 0) {
                        return;
                    }
                    byte[] request = new byte[4096];
                    ServerSocket.read(client, request, 0, request.length);
                    byte[] reply = ("HTTP/1.1 " + status + " Here\r\n"
                            + "Transfer-Encoding: chunked\r\n\r\n").getBytes("UTF-8");
                    ServerSocket.write(client, reply, 0, reply.length);
                } catch (Exception ignored) {
                    // The verdict below is what this check reports.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        String outcome;
        try {
            Http.Response response = Http.request("127.0.0.1", port, verb, "/x", null);
            outcome = response.getStatus() + "/" + response.getBodyAsString();
        } catch (Exception err) {
            outcome = "failed: " + err.getMessage();
        } finally {
            listener.close();
        }
        stub.join(10000);
        return outcome;
    }

    /** Serves one canned chunked body and reports the client's verdict. */
    private static String chunkedReply(final String framing) throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        final int port = listener.getPort();
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = listener.accept();
                    if(client < 0) {
                        return;
                    }
                    byte[] request = new byte[4096];
                    ServerSocket.read(client, request, 0, request.length);
                    byte[] reply = ("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n"
                            + framing).getBytes("UTF-8");
                    ServerSocket.write(client, reply, 0, reply.length);
                } catch (Exception ignored) {
                    // The verdict below is what this check reports.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        String outcome;
        try {
            outcome = Http.request("127.0.0.1", port, "GET", "/x", null).getBodyAsString();
        } catch (java.io.IOException refused) {
            String message = String.valueOf(refused.getMessage());
            outcome = message.indexOf("chunked") >= 0 ? "refused"
                    : "refused for another reason: " + message;
        } finally {
            listener.close();
        }
        stub.join(10000);
        return outcome;
    }

    /**
     * A response ends where its Content-Length says it does.
     *
     * <p>Content-Length is the whole of the framing, so whatever follows the
     * declared bytes is not part of the response. Handing it back as body meant a
     * caller acted on the peer's answer with something else appended -- a second
     * response, or bytes an attacker put after a short one -- and a handler
     * invoked on that acts on input the peer never sent.
     */
    private static void aResponseEndsAtItsDeclaredLength() throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        final int port = listener.getPort();
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = listener.accept();
                    if(client < 0) {
                        return;
                    }
                    byte[] request = new byte[4096];
                    ServerSocket.read(client, request, 0, request.length);
                    byte[] reply = ("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\n"
                            + "hi" + "AND SOMETHING NOBODY ASKED FOR").getBytes("UTF-8");
                    ServerSocket.write(client, reply, 0, reply.length);
                } catch (Exception ignored) {
                    // The check below reports what the client made of it.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        String outcome;
        try {
            outcome = Http.request("127.0.0.1", port, "GET", "/x", null).getBodyAsString();
        } catch (Exception err) {
            outcome = "failed: " + err.getMessage();
        } finally {
            listener.close();
        }
        stub.join(10000);
        check("a response body is what its Content-Length framed", "hi", outcome);
    }

    /**
     * A path that would name a different file as a C string opens nothing.
     *
     * <p>A NUL inside it ENDS the path there, so "/etc/hosts\u0000.png" opens
     * /etc/hosts while an application that validated the name it was given saw a
     * .png and allowed it. The truncation happens only in the packaged runtime --
     * the Java SE arm's Paths.get refuses the NUL and answers -1 -- so the
     * simulator proves the extension check works and the device opens the other
     * file. This check runs on BOTH arms, which is the point: it is the two
     * answering alike that was missing.
     */
    /**
     * An open that failed for a reason OTHER than the file being absent.
     *
     * <p>-1 means "no such file", which a caller may treat as an optional file
     * nobody wrote; -2 means "there is something there and it could not be
     * opened", which nobody may quietly ignore -- Config refuses to start on it,
     * because a deployment whose TLS certificate and key are named in a file it
     * cannot read would otherwise come up in plaintext.
     *
     * <p>Provoked with an over-long name rather than a permission bit, so this
     * needs no chmod and answers the same whether or not the tests run as root.
     * Both arms have to agree, and they did not at first: the JVM's
     * FileChannel.open throws a plain FileSystemException for a path under a
     * non-directory while the native arm read that errno as "absent".
     */
    private static void anUnopenableFileIsNotAnAbsentOne() throws Exception {
        StringBuilder tooLong = new StringBuilder("/tmp/");
        for(int iter = 0 ; iter < 600 ; iter++) {
            tooLong.append('x');
        }
        int refused = FileIo.openRead(tooLong.toString());
        if(refused >= 0) {
            FileIo.close(refused);
        }
        check("an unopenable path is told apart from an absent one", "-2",
                String.valueOf(refused));
        // A path under a file rather than a directory, which is the case the two
        // arms disagreed about.
        int underAFile = FileIo.openRead("/etc/hosts/nope");
        if(underAFile >= 0) {
            FileIo.close(underAFile);
        }
        check("and so is a path under a non-directory", "-2", String.valueOf(underAFile));
        // THE CONTROL: a name that is merely absent still answers -1, so this
        // cannot pass by reporting -2 for everything.
        int absent = FileIo.openRead("/tmp/cn1-selftest-no-such-file");
        if(absent >= 0) {
            FileIo.close(absent);
        }
        check("while a file that is simply not there answers -1", "-1",
                String.valueOf(absent));
        // A DANGLING SYMLINK, which both arms used to collapse into "absent":
        // open() follows the link and reports the missing TARGET, giving the same
        // ENOENT a path that names nothing gives. A deployment whose
        // application.properties is a symlink into a volume that failed to mount
        // therefore read as "no configuration" -- every file-based setting fell
        // back to an environment default, and a server naming its TLS certificate
        // and key in that file came up in PLAINTEXT.
        //
        // The link is made by whoever runs this rather than here, because FileIo
        // has no symlink call and adding a native for a test is a worse trade
        // than skipping when it is absent. vm/backend/verify.sh makes it.
        String dangling = "/tmp/cn1-selftest-dangling-link";
        int danglingFd = FileIo.openRead(dangling);
        if(danglingFd >= 0) {
            FileIo.close(danglingFd);
        }
        if(danglingFd == -1) {
            System.out.println("NOTE dangling-symlink check skipped: create "
                    + dangling + " pointing at a missing target to run it");
        } else {
            check("a dangling symlink is told apart from an absent file", "-2",
                    String.valueOf(danglingFd));
        }
    }

    private static void aTruncatingPathOpensNothing() throws Exception {
        int truncated = FileIo.openRead("/etc/hosts\u0000.png");
        if(truncated >= 0) {
            FileIo.close(truncated);
        }
        String resolved = FileIo.realPath("/etc/hosts\u0000.png");
        // THE CONTROL: the same path without the NUL still opens, so this cannot
        // pass by refusing everything.
        int honest = FileIo.openRead("/etc/hosts");
        boolean honestOpened = honest >= 0;
        if(honest >= 0) {
            FileIo.close(honest);
        }
        check("a path holding a NUL opens nothing", "-1", String.valueOf(truncated));
        check("and resolves to nothing", "null", String.valueOf(resolved));
        check("while the same path without one still opens", "true",
                String.valueOf(honestOpened));
    }

    /**
     * The name TLS verifies against is checked before it becomes a C string.
     *
     * <p>A NUL inside it ends the name there, so OpenSSL verifies the certificate
     * against the prefix alone and a caller's own suffix check -- endsWith on the
     * trusted domain -- passes on a name that is never used. connect() validating
     * the address it dialled does not cover this: the verification name is a
     * separate argument and may come from somewhere else entirely.
     */
    private static void aTlsVerificationNameWithANulIsRefused() throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        // ACCEPTS AND HANGS UP, rather than accepting and going quiet. Without the
        // check below the name reaches OpenSSL, which blocks in sock_read waiting
        // for a server hello that a silent peer never sends -- so a regression
        // here would HANG this suite instead of failing it. Closing makes the
        // handshake fail at once, and the outcome says which of the two happened.
        Thread stub = new Thread(new Runnable() {
            public void run() {
                try {
                    int client = listener.accept();
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                } catch (Exception ignored) {
                    // The check below reports what the client made of it.
                }
            }
        });
        stub.start();
        String outcome;
        try {
            Tcp conn = Tcp.connect("127.0.0.1", listener.getPort(), 2000);
            try {
                conn.startTls("attacker.example\u0000.trusted.example");
                outcome = "accepted";
            } catch (java.io.IOException refused) {
                String message = String.valueOf(refused.getMessage());
                outcome = message.indexOf("host name") >= 0 || message.indexOf("NUL") >= 0
                        ? "refused"
                        : "reached the handshake instead: " + message;
            } finally {
                conn.close();
            }
        } finally {
            listener.close();
        }
        stub.join(10000);
        check("a TLS verification name holding a NUL is refused", "refused", outcome);
    }

    /**
     * A HEAD response declares the length of a body it correctly did not send.
     *
     * <p>RFC 9110 6.4.1: a response to HEAD ends at the blank line whatever the
     * header fields say, and a conforming server answers it with the
     * Content-Length the GET would have carried -- this server does exactly that.
     * The client compared that number against the zero bytes it received and
     * reported a truncated transfer, so it could not make a HEAD request against
     * a conforming server at all, which is every server including ours.
     */
    private static void aHeadResponseIsNotReadAsTruncated() throws Exception {
        HttpServer server = HttpServer.start("127.0.0.1", 0, 16, 1, new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                return HttpServer.Response.text(200, "a body with some length to it");
            }
        });
        String head;
        String get;
        try {
            try {
                head = String.valueOf(Http.request("127.0.0.1", server.getPort(),
                        "HEAD", "/x", null).getStatus());
            } catch (java.io.IOException err) {
                // Reported rather than thrown, so a regression here is one failing
                // check with the reason in it instead of a suite that stops.
                head = "refused: " + err.getMessage();
            }
            // The control: the SAME route over GET still has its length checked,
            // so this cannot pass by having stopped checking anything.
            get = String.valueOf(Http.request("127.0.0.1", server.getPort(),
                    "GET", "/x", null).getStatus());
        } finally {
            server.stop(2000);
        }
        check("a HEAD response is not reported as truncated", "200", head);
        check("and a GET of the same route still answers", "200", get);
    }

    /**
     * A null method is GET, on this client as well as the other one.
     *
     * <p>Web takes null and means GET -- one implementation normalises it, the
     * other leaves libcurl to its default -- while this client wrote the request
     * line itself and appended the four characters "null" as the verb.
     */
    private static void aNullMethodIsGet() throws Exception {
        HttpServer server = HttpServer.start("127.0.0.1", 0, 16, 1, new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                return HttpServer.Response.text(200, request.getMethod());
            }
        });
        String seen;
        try {
            seen = Http.request("127.0.0.1", server.getPort(), null, "/verb", null)
                    .getBodyAsString();
        } finally {
            server.stop(2000);
        }
        check("a null method reaches the server as GET", "GET", seen);
    }

    /**
     * A fragment never goes out in a request target.
     *
     * <p>The inbound parser refuses one; this is the same rule facing outward.
     */
    private static void anOutboundFragmentIsRefused() throws Exception {
        String refusal;
        try {
            Http.request("127.0.0.1", 1, "GET", "/users#private", null);
            refusal = "accepted";
        } catch (java.io.IOException expected) {
            refusal = String.valueOf(expected.getMessage()).indexOf("fragment") >= 0
                    ? "refused as a fragment" : "refused for another reason: "
                            + expected.getMessage();
        }
        check("a fragment in an outbound target is refused", "refused as a fragment", refusal);
    }

    /**
     * Two spellings of one header name are one field to SigV4.
     *
     * <p>The canonical request lower-cases every name, so X-Meta and x-meta
     * collide there while both go out on the wire. AWS combines what it receives,
     * with a comma, in arrival order; keeping only the last value signed a
     * different string than the service verified and the answer was a 403 that
     * named nothing. Asserted as the property itself: the duplicate pair must
     * sign exactly as the combined single field does.
     */
    private static void caseVariantHeadersSignAsOneField() throws Exception {
        Credentials credentials = new Credentials("AKIDEXAMPLE", "secret", null);
        Map duplicated = new LinkedHashMap();
        duplicated.put("X-Meta", "one");
        duplicated.put("x-meta", "two");
        Map combined = new LinkedHashMap();
        combined.put("x-meta", "one,two");
        Map query = new LinkedHashMap();
        String hash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        String duplicatedAuth = Aws.authorization(credentials, "us-east-1", "s3", "GET",
                "/", query, duplicated, hash, "20260101T000000Z");
        String combinedAuth = Aws.authorization(credentials, "us-east-1", "s3", "GET",
                "/", query, combined, hash, "20260101T000000Z");
        // And a control: a DIFFERENT combined value must not sign the same, or the
        // comparison above would hold however the duplicates were handled.
        Map wrong = new LinkedHashMap();
        wrong.put("x-meta", "two");
        String wrongAuth = Aws.authorization(credentials, "us-east-1", "s3", "GET",
                "/", query, wrong, hash, "20260101T000000Z");
        check("case-variant headers sign as one combined field", combinedAuth, duplicatedAuth);
        check("and not as the last value alone",
                "different", combinedAuth.equals(wrongAuth) ? "same" : "different");
    }

    private static void aLateBodyIsNotServedAnotherConnectionsBytes() throws Exception {
        check("a request with a late body reads its own headers",
                "aaaaaa|x|6|POST", oneLateBodyRequest(false));
        // The interleaved half needs the two connections to SHARE a thread, which
        // is what one worker buys under virtual threads and what makes the buffer
        // shared in the first place. Where they are real threads one worker cannot
        // serve the second connection while the first waits for its body -- the
        // two would simply wait for each other -- and the buffer is per thread
        // there, so there is nothing for the second connection to overwrite.
        if(!VirtualThread.supported()) {
            note("interleaved late-body check skipped: this runtime has no virtual threads,"
                    + " so the read buffer is not shared between connections");
            return;
        }
        check("a request with a late body reads its own headers with another served between",
                "aaaaaa|x|6|POST", oneLateBodyRequest(true));
    }

    private static String oneLateBodyRequest(boolean interleave) throws Exception {
        final String[] seen = new String[1];
        HttpServer server = HttpServer.start("127.0.0.1", 0, 16, 1, new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                String tag = request.getHeader("X-Tag");
                if("bbbbbb".equals(tag)) {
                    return HttpServer.Response.text(200, "second");
                }
                seen[0] = tag + "|" + request.getHeader("Host") + "|"
                        + request.getHeader("Content-Length") + "|" + request.getMethod();
                return HttpServer.Response.text(200, "first");
            }
        });
        try {
            Tcp first = Tcp.connect("127.0.0.1", server.getPort(), 15000);
            try {
                // The head alone: the body is promised and withheld, so the server
                // parses the headers and then parks waiting for the rest.
                byte[] head = ("POST /a HTTP/1.1\r\nHost: x\r\nX-Tag: aaaaaa\r\n"
                        + "Content-Length: 6\r\nConnection: close\r\n\r\n").getBytes("UTF-8");
                first.write(head, 0, head.length);
                Thread.sleep(300);
                if(interleave) {
                    Tcp second = Tcp.connect("127.0.0.1", server.getPort(), 15000);
                    try {
                        // Deliberately a different length from the first request, so
                        // a stale slice lands somewhere that cannot still read right.
                        byte[] whole = ("GET /bbbbbbbbbbbbbbbbbbbbbbb HTTP/1.1\r\n"
                                + "Host: x\r\nX-Tag: bbbbbb\r\n"
                                + "Connection: close\r\n\r\n").getBytes("UTF-8");
                        second.write(whole, 0, whole.length);
                        drainUntilClosed(second);
                    } finally {
                        second.close();
                    }
                    Thread.sleep(100);
                }
                byte[] body = "123456".getBytes("UTF-8");
                first.write(body, 0, body.length);
                drainUntilClosed(first);
            } finally {
                first.close();
            }
        } finally {
            server.stop(2000);
        }
        return seen[0];
    }

    /** Reads until the peer closes, so the server has finished with the request. */
    private static void drainUntilClosed(Tcp conn) throws Exception {
        byte[] buffer = new byte[4096];
        while(conn.read(buffer, 0, buffer.length) > 0) {
            // the bytes do not matter; finishing does
        }
    }

    private static void aStaticFileComesBackWhole() throws Exception {
        String dir = "/tmp/cn1-selftest-static-" + System.currentTimeMillis();
        new java.io.File(dir).mkdirs();
        String name = "payload.bin";
        int size = 300 * 1024;
        StringBuilder content = new StringBuilder();
        for(int iter = 0 ; iter < size ; iter++) {
            content.append((char)('a' + (iter % 26)));
        }
        String expected = content.toString();
        java.io.FileOutputStream out = new java.io.FileOutputStream(dir + "/" + name);
        try {
            out.write(expected.getBytes("UTF-8"));
        } finally {
            out.close();
        }
        final StaticFiles files = new StaticFiles(dir, "/static", null, null);
        HttpServer server = HttpServer.start("127.0.0.1", 0, 16, 1, new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                HttpServer.Response served = files.handle(request);
                return served == null ? HttpServer.Response.text(404, "no") : served;
            }
        });
        String outcome;
        try {
            String body = httpGetBody("127.0.0.1", server.getPort(), "/static/" + name);
            outcome = body == null ? "<none>" : String.valueOf(body.length());
        } finally {
            server.stop(2000);
            new java.io.File(dir + "/" + name).delete();
            new java.io.File(dir).delete();
        }
        check("a static file comes back whole", String.valueOf(size), outcome);
    }

    /**
     * The mount prefix is matched on the canonical path, so an equivalent
     * spelling of it is the same mount.
     *
     * <p>%61 is 'a'. The handler used to test request.getTarget(), which is the
     * target as it arrived, while a generated router matching the same URI
     * compares it with the unreserved escapes already resolved -- so /assets was
     * this handler's and /%61ssets was nobody's, and in a chain the second
     * spelling fell through to whatever came next.
     */
    private static void anEncodedMountPrefixIsTheSameMount() throws Exception {
        String dir = "/tmp/cn1-selftest-mount-" + System.currentTimeMillis();
        new java.io.File(dir).mkdirs();
        java.io.FileOutputStream out = new java.io.FileOutputStream(dir + "/logo.txt");
        try {
            out.write("logo".getBytes("UTF-8"));
        } finally {
            out.close();
        }
        final StaticFiles files = new StaticFiles(dir, "/assets", null, null);
        HttpServer server = HttpServer.start("127.0.0.1", 0, 16, 1, new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                HttpServer.Response served = files.handle(request);
                // What a chain does with a path the file handler says is not its
                // own -- and the answer that tells the two spellings apart.
                return served == null ? HttpServer.Response.text(404, "not ours") : served;
            }
        });
        String plain;
        String encoded;
        String encodedSlash;
        try {
            plain = httpGetBody("127.0.0.1", server.getPort(), "/assets/logo.txt");
            encoded = httpGetBody("127.0.0.1", server.getPort(), "/%61ssets/logo.txt");
            // AND NOT THIS ONE: %2F is not unreserved, so it stays encoded and
            // cannot pass for the separator that ends the mount.
            encodedSlash = httpGetBody("127.0.0.1", server.getPort(), "/assets%2Flogo.txt");
        } finally {
            server.stop(2000);
            new java.io.File(dir + "/logo.txt").delete();
            new java.io.File(dir).delete();
        }
        check("a static file is served from its mount", "logo", plain);
        check("and from an encoded spelling of the same mount", "logo", encoded);
        check("an encoded slash does not end the mount", "not ours", encodedSlash);
    }

    /**
     * Two spellings of one escape are one mount.
     *
     * <p>%2F and %2f are the same octet, and RFC 3986 6.2.2.1 normalises the
     * digits to upper case for exactly that reason. A retained escape was left as
     * it arrived on both sides, so a mount or a literal route declared with one
     * spelling was missed by the other -- and a request that changed nothing but
     * the case of a hex digit fell past a protected literal into whatever dynamic
     * route followed it.
     */
    private static void theCaseOfAnEscapeDoesNotChangeTheRoute() throws Exception {
        String dir = "/tmp/cn1-selftest-hex-" + System.currentTimeMillis();
        new java.io.File(dir).mkdirs();
        java.io.FileOutputStream out = new java.io.FileOutputStream(dir + "/logo.txt");
        try {
            out.write("logo".getBytes("UTF-8"));
        } finally {
            out.close();
        }
        // Declared in UPPER case; both spellings of the request must reach it.
        final StaticFiles upper = new StaticFiles(dir, "/a%2Fb", null, null);
        HttpServer server = HttpServer.start("127.0.0.1", 0, 16, 1, new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                HttpServer.Response served = upper.handle(request);
                return served == null ? HttpServer.Response.text(404, "not ours") : served;
            }
        });
        String asDeclared;
        String otherCase;
        try {
            asDeclared = httpGetBody("127.0.0.1", server.getPort(), "/a%2Fb/logo.txt");
            otherCase = httpGetBody("127.0.0.1", server.getPort(), "/a%2fb/logo.txt");
        } finally {
            server.stop(2000);
        }
        // And declared in LOWER case, which must behave identically.
        final StaticFiles lower = new StaticFiles(dir, "/a%2fb", null, null);
        HttpServer second = HttpServer.start("127.0.0.1", 0, 16, 1, new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                HttpServer.Response served = lower.handle(request);
                return served == null ? HttpServer.Response.text(404, "not ours") : served;
            }
        });
        String declaredLower;
        try {
            declaredLower = httpGetBody("127.0.0.1", second.getPort(), "/a%2Fb/logo.txt");
        } finally {
            second.stop(2000);
            new java.io.File(dir + "/logo.txt").delete();
            new java.io.File(dir).delete();
        }
        check("the spelling the mount was declared with reaches it", "logo", asDeclared);
        check("and so does the other case of the same escape", "logo", otherCase);
        check("declaring it in the other case is the same mount", "logo", declaredLower);
    }

    /**
     * The other half of the same rule: a mount DECLARED with an escape in it.
     *
     * <p>%7E is '~'. The request side resolves it before anything compares, so a
     * mount configured as /assets%7E was matched against a path that had already
     * become /assets~ -- and neither spelling of the URL reached it. Not the
     * encoded one, which no longer looks like that by then, and not the decoded
     * one, which never did. The mount served nothing at all, with nothing at
     * startup or request time to say so.
     */
    private static void aMountDeclaredWithAnEscapeIsStillReachable() throws Exception {
        String dir = "/tmp/cn1-selftest-mount-declared-" + System.currentTimeMillis();
        new java.io.File(dir).mkdirs();
        java.io.FileOutputStream out = new java.io.FileOutputStream(dir + "/logo.txt");
        try {
            out.write("logo".getBytes("UTF-8"));
        } finally {
            out.close();
        }
        final StaticFiles files = new StaticFiles(dir, "/assets%7E", null, null);
        HttpServer server = HttpServer.start("127.0.0.1", 0, 16, 1, new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                HttpServer.Response served = files.handle(request);
                return served == null ? HttpServer.Response.text(404, "not ours") : served;
            }
        });
        String asDeclared;
        String asResolved;
        try {
            asDeclared = httpGetBody("127.0.0.1", server.getPort(), "/assets%7E/logo.txt");
            asResolved = httpGetBody("127.0.0.1", server.getPort(), "/assets~/logo.txt");
        } finally {
            server.stop(2000);
            new java.io.File(dir + "/logo.txt").delete();
            new java.io.File(dir).delete();
        }
        check("a mount declared with an escape serves the spelling it was declared with",
                "logo", asDeclared);
        check("and the spelling that escape resolves to", "logo", asResolved);
    }

    /**
     * A field value goes out as one byte per character, on every protocol.
     *
     * <p>The server's own validation accepts 0x20 to 0xff except 0x7f -- obs-text
     * included -- and the HTTP/1.1 writer narrows each character with a cast. The
     * h2 and libcurl paths handed the native a String instead, which
     * stringToUTF8 encoded, so a handler returning U+00E9 sent one byte over
     * HTTP/1.1 and two over h2: the same response, different octets, chosen by
     * whichever protocol the client negotiated.
     */
    private static void aFieldValueIsOctetsOnEveryProtocol() throws Exception {
        byte[] bytes = FileCountProbe.headerBytes("x-label: caf\u00e9");
        check("an obs-text character is one byte", "1",
                String.valueOf(bytes.length - "x-label: caf".length()));
        check("and it is the byte the validation checked", "-23",
                String.valueOf(bytes[bytes.length - 1]));
        // ASCII is unaffected, which is every other header.
        byte[] plain = FileCountProbe.headerBytes("x-label: plain");
        check("an ascii header is unchanged", "14", String.valueOf(plain.length));
        check("and reads back as itself", "x-label: plain",
                new String(plain, 0, plain.length, "UTF-8"));
    }

    /**
     * An outbound failure must not put the URL's secrets in its message.
     *
     * <p>A presigned S3 URL carries its signature in the query, and userinfo
     * carries a password outright. The message goes wherever the caller logs it,
     * which is the whole reason Urls.requireHttp prints only the scheme and
     * Database.Url.describe omits the password -- Web's own errors were printing
     * the lot. Port 1 is not listening, so the transfer fails without needing a
     * server, and this runs on both arms: the JavaSE Web and the translated one
     * have their own copies of these messages.
     */
    private static void anOutboundFailureDoesNotLogTheSecrets() throws Exception {
        String url = "http://someone:hunter2@127.0.0.1:1/path?X-Amz-Signature=deadbeef";
        String message;
        try {
            Web.request("GET", url, null, null);
            message = "<the request unexpectedly succeeded>";
        } catch (Exception err) {
            message = String.valueOf(err.getMessage());
        }
        check("the password is not in the message", "false",
                String.valueOf(message.indexOf("hunter2") >= 0));
        check("nor is the signature", "false",
                String.valueOf(message.indexOf("deadbeef") >= 0));
        // Still worth logging: the endpoint that failed is named.
        check("the host is still named", "true",
                String.valueOf(message.indexOf("127.0.0.1") >= 0));
        // A NUL TRUNCATES THE URL AT THE NATIVE BOUNDARY: stringToUTF8 encodes
        // through String.getBytes("UTF-8"), so libcurl would be handed
        // "http://127.0.0.1" and an application that approved the .example.com
        // suffix would have approved a request to loopback.
        check("a NUL in a URL is refused", "refused",
                requestRefused("http://127.0.0.1\u0000.example.com/"));
        check("and a newline, which means two things to whatever parses it next",
                "refused", requestRefused("http://127.0.0.1/a\nb"));
        check("and a raw space", "refused",
                requestRefused("http://127.0.0.1/a b"));
        // AND THE SAME TRUNCATION ONE LAYER DOWN. Tcp.connect is public and hands
        // the name to getaddrinfo, so closing it in Web alone left the hole
        // reachable without a URL at all.
        check("a NUL in a host name is refused", "refused",
                connectRefused("127.0.0.1\u0000.example.com"));
        check("and an ordinary host is still attempted", "attempted",
                connectRefused("127.0.0.1"));
        // AND THE FRAGMENT, which is where an implicit-flow OAuth token arrives
        // and which never reaches the server at all.
        String fragment = "http://127.0.0.1:1/path#access_token=fragmentsecret";
        String second;
        try {
            Web.request("GET", fragment, null, null);
            second = "<the request unexpectedly succeeded>";
        } catch (Exception err) {
            second = String.valueOf(err.getMessage());
        }
        check("a fragment secret is not in the message", "false",
                String.valueOf(second.indexOf("fragmentsecret") >= 0));
        check("and the host is still named there too", "true",
                String.valueOf(second.indexOf("127.0.0.1") >= 0));
    }

    /**
     * Whether Tcp refuses a host outright, rather than trying to resolve it.
     *
     * Port 1 is not listening, so an accepted name fails at the connect: that is
     * "attempted", and it is what says the refusal above is about the name rather
     * than about nothing happening at all.
     */
    private static String connectRefused(String host) {
        try {
            Tcp.connect(host, 1, 1000).close();
            return "attempted";
        } catch (Exception err) {
            String message = String.valueOf(err.getMessage());
            return message.indexOf("control character") >= 0 ? "refused" : "attempted";
        }
    }

    /** Whether Web refuses a URL outright, rather than trying to fetch it. */
    private static String requestRefused(String url) {
        try {
            Web.request("GET", url, null, null);
            return "fetched";
        } catch (Exception err) {
            String message = String.valueOf(err.getMessage());
            return message.indexOf("control character") >= 0
                    || message.indexOf("http and https") >= 0 ? "refused" : "fetched";
        }
    }

    /**
     * Whether S3 refuses `region` as a hostname label.
     *
     * forRegion resolves credentials before it returns, which needs a metadata
     * service, so what is asked here is only whether the region got past the
     * check: anything else that fails says "accepted" and the check below reads
     * as a pass only when the refusal is the region's.
     */
    private static String regionRefused(String region) {
        try {
            S3.forRegion(region);
            return "accepted";
        } catch (Exception err) {
            String message = err.getMessage();
            return message != null && message.indexOf("Not an AWS region") >= 0
                    ? "refused" : "accepted";
        }
    }

    /** One GET, reading to the end of the body by Content-Length. */
    private static String httpGetBody(String host, int port, String target) throws Exception {
        Tcp conn = Tcp.connect(host, port, 15000);
        try {
            byte[] request = ("GET " + target + " HTTP/1.1\r\nHost: x\r\n"
                    + "Connection: close\r\n\r\n").getBytes("UTF-8");
            conn.write(request, 0, request.length);
            ByteArrayOutputStream all = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            while(true) {
                int n = conn.read(chunk, 0, chunk.length);
                if(n <= 0) {
                    break;
                }
                all.write(chunk, 0, n);
            }
            String text = new String(all.toByteArray(), "UTF-8");
            int at = text.indexOf("\r\n\r\n");
            return at < 0 ? null : text.substring(at + 4);
        } finally {
            conn.close();
        }
    }

    /**
     * A SCRAM server does not get to choose how long this client computes.
     *
     * <p>The iteration count arrives on the wire and multiplies straight into
     * PBKDF2. The default sslmode=prefer falls back to plaintext, so whoever answers
     * the connection can complete the exchange with a valid extended nonce and name
     * a count near Integer.MAX_VALUE. Measured with the bound removed and this stub
     * in place: one connection attempt held the thread for about 175 seconds, before
     * anything was authenticated and repeatable at the peer's choosing.
     *
     * <p>Driven by a stub, because a real server will not send a hostile count. The
     * assertion is the bound's own wording rather than "it threw": a stub that got
     * the protocol wrong would throw too, and would pass a weaker check while
     * proving nothing. That this returns at all is the other half of the proof --
     * without the bound this check fails, and takes those 175 seconds to fail.
     */
    private static void scramIterationCountIsBounded() throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        final int port = listener.getPort();
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = listener.accept();
                    if(client < 0) {
                        return;
                    }
                    ServerSocket.setTimeout(client, 10000);
                    if(pgRead(client) == null) {          // StartupMessage
                        return;
                    }
                    // AuthenticationSASL: mechanism 10, one mechanism, list ended
                    // by its own empty string.
                    pgSend(client, 'R', join(int32(10), ascii("SCRAM-SHA-256\0\0")));
                    byte[] initial = pgRead(client);      // SASLInitialResponse
                    if(initial == null) {
                        return;
                    }
                    // The client refuses a nonce that does not extend its own, so
                    // the stub has to echo it back. It is the last field of the
                    // message, which is why the tail is all of it.
                    String text = new String(initial, "UTF-8");
                    int at = text.lastIndexOf("r=");
                    String clientNonce = at < 0 ? "" : text.substring(at + 2);
                    // AuthenticationSASLContinue, with the count this check exists
                    // for. The salt is real base64 so that decoding it is not what
                    // fails.
                    pgSend(client, 'R', join(int32(11), ascii("r=" + clientNonce
                            + "stub,s=AAAAAAAAAAAAAAAA,i=" + Integer.MAX_VALUE)));
                } catch (Exception ignored) {
                    // The client hanging up mid-exchange is the expected ending.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        String outcome;
        try {
            Database db = Database.open("postgres://u:pw@127.0.0.1:" + port
                    + "/db?sslmode=disable");
            db.close();
            outcome = "connected";
        } catch (Exception refused) {
            String message = String.valueOf(refused.getMessage());
            outcome = message.indexOf("SCRAM iterations") >= 0
                    ? "refused" : "other: " + message;
        } finally {
            listener.close();
        }
        stub.join(10000);
        check("a hostile SCRAM iteration count is refused", "refused", outcome);
    }

    /**
     * A password SASLprep would rewrite is refused, not silently mismatched.
     *
     * <p>PostgreSQL builds its SCRAM verifier from the SASLprepped password, and
     * this runtime has no Normalizer to prep with, so a character the profile
     * MAPS -- a no-break space is the everyday one, pasted out of a browser --
     * makes a proof that cannot match a verifier that is otherwise correct. The
     * comment at that code claimed such a password was rejected while nothing
     * rejected it, so the symptom was "password authentication failed" and the
     * reader went to look at the role's grants.
     *
     * <p>Driven by the same stub as the iteration count, because a real server
     * cannot be made to disagree on demand. The assertion is the refusal's own
     * wording: "it threw" would pass on the authentication failure this exists to
     * replace. The ASCII half is the control -- the same stub, the same exchange,
     * a password with nothing to prepare -- so a check that refused everything
     * would fail it.
     */
    private static void scramRefusesAPasswordItCannotPrepare() throws Exception {
        // %C2%A0 is U+00A0, RFC 3454 table C.1.2, which SASLprep maps to a space.
        check("a SCRAM password SASLprep would rewrite is refused", "refused",
                scramOutcomeFor("p%C2%A0w", "SASLprep"));
        // And the same exchange with an ASCII password must get past this check
        // and fail on the proof instead, which is the stub hanging up.
        check("and an ASCII password is still computed", "reached the proof",
                scramOutcomeFor("pw", "SASLprep"));
    }

    /**
     * Opens a SCRAM connection against a stub that gets as far as the challenge,
     * and says whether the named refusal or the exchange itself ended it.
     */
    private static String scramOutcomeFor(String password, String refusal)
            throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        final int port = listener.getPort();
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = listener.accept();
                    if(client < 0) {
                        return;
                    }
                    ServerSocket.setTimeout(client, 10000);
                    if(pgRead(client) == null) {          // StartupMessage
                        return;
                    }
                    pgSend(client, 'R', join(int32(10), ascii("SCRAM-SHA-256\0\0")));
                    byte[] initial = pgRead(client);      // SASLInitialResponse
                    if(initial == null) {
                        return;
                    }
                    String text = new String(initial, "UTF-8");
                    int at = text.lastIndexOf("r=");
                    String clientNonce = at < 0 ? "" : text.substring(at + 2);
                    // A COUNT THE CLIENT WILL ACTUALLY COMPUTE, unlike the bound
                    // check's stub: the point here is to reach PBKDF2, so that a
                    // password refused before it is refused by the new rule rather
                    // than by the old ceiling.
                    pgSend(client, 'R', join(int32(11), ascii("r=" + clientNonce
                            + "stub,s=AAAAAAAAAAAAAAAA,i=4096")));
                } catch (Exception ignored) {
                    // The client hanging up mid-exchange is the expected ending.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        String outcome;
        try {
            Database db = Database.open("postgres://u:" + password + "@127.0.0.1:"
                    + port + "/db?sslmode=disable");
            db.close();
            outcome = "connected";
        } catch (Exception refused) {
            String message = String.valueOf(refused.getMessage());
            outcome = message.indexOf(refusal) >= 0 ? "refused" : "reached the proof";
        } finally {
            listener.close();
        }
        stub.join(10000);
        return outcome;
    }

    /**
     * A MySQL header torn in half is an IOException, not a wild allocation.
     *
     * <p>Only the first of the four header bytes was checked. wire.read() answers -1
     * at end of stream, so a peer that hung up after two bytes had that -1 shifted
     * into the length and made it NEGATIVE -- under the size ceiling rather than
     * over it, because a ceiling only rejects what is too large -- and readFully
     * asked for an array of that size.
     *
     * <p>The two arms then part company, and the packaged one is the worse. On Java
     * SE that is NegativeArraySizeException: not an IOException, so it walks past
     * connect()'s cleanup and leaves the socket open, one leaked descriptor per
     * retry. MEASURED on the packaged binary, there is no exception at all -- the
     * self-test died on SIGSEGV, exit 139, with no output after the check before
     * it. A negative array size is simply not checked there, which puts this in the
     * family of unchecked CHECKCAST and division by zero: a JVM guarantee the
     * translated target does not honour. The greeting is read through this path
     * before TLS, so the peer doing it need not have authenticated or be a database.
     *
     * <p>The assertion is the wording, not "it threw". Both the old behaviour and
     * the new one end in an exception out of Database.open, so a check that only
     * caught something would have passed before the fix.
     */
    /**
     * An sslmode nobody implements is refused, not read as "prefer".
     *
     * <p>Both engines compared the mode against "disable" and "require" and took
     * everything else as prefer, so "Require" or "required" asked for TLS and
     * accepted plaintext when the server declined -- the one outcome the word was
     * written to prevent, reached by spelling it slightly wrong.
     */
    private static void anUnknownSslModeIsRefused() throws Exception {
        String[] typos = new String[] { "Require", "required", "requrie", "verify-full" };
        for(int iter = 0 ; iter < typos.length ; iter++) {
            check("sslmode=" + typos[iter] + " is refused", "refused",
                    sslModeRefused(typos[iter]));
        }
        // THE THREE THIS RUNTIME IMPLEMENTS still open a connection -- there is
        // nothing listening on port 1, so a connection failure is what "accepted"
        // looks like here, and it is what says the refusals are about the mode.
        String[] known = new String[] { "require", "prefer", "disable" };
        for(int iter = 0 ; iter < known.length ; iter++) {
            check("sslmode=" + known[iter] + " is accepted", "accepted",
                    sslModeRefused(known[iter]));
        }
    }

    /**
     * Whether the ENGINE refuses the mode before it opens a socket.
     *
     * <p>Through Postgres.connect rather than Database.open: the URL parser has
     * always refused an unknown sslmode, so the hole was only ever reachable by a
     * caller of the public engine API -- which is a supported way in, and the one
     * a pool or a framework would use. Testing the URL would have passed without
     * the fix and said nothing.
     */
    private static String sslModeRefused(String mode) {
        try {
            com.codename1.backend.sql.Postgres.connect("127.0.0.1", 1, "db", "u", "pw",
                    mode, null, 1000, 0).close();
            return "accepted";
        } catch (Exception err) {
            String message = String.valueOf(err.getMessage());
            return message.indexOf("not one this runtime implements") >= 0
                    ? "refused" : "accepted";
        }
    }

    /**
     * A row this client cannot decode leaves the session unusable.
     *
     * <p>The decode throws after readPacket has already taken a whole packet off
     * the wire, so the rest of the result set is still unread. columnCount closes
     * for exactly this reason and the decoders did not, so the statement's finally
     * sent COM_STMT_CLOSE over the unread rows and isOpen() went on calling the
     * session reusable -- the pool then handed it out and the next statement read
     * a previous one's row as its own answer.
     *
     * <p>Driven by a stub speaking just enough of the protocol to get there: a
     * greeting, an OK for whatever is sent, a prepare response describing one
     * column, and then a row whose value claims sixteen megabytes that are not
     * there. A server ERROR packet is deliberately NOT this case -- that is a
     * clean stream, and closing on one would destroy a connection over an
     * ordinary duplicate key.
     */
    private static void aMalformedRowClosesTheMySqlSession() throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        final int port = listener.getPort();
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = listener.accept();
                    if(client >= 0) {
                        mySqlStubSession(client);
                    }
                } catch (Exception ignored) {
                    // The check below reports what the client made of it.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        String outcome;
        Database db = null;
        try {
            db = Database.open("mysql://u:pw@127.0.0.1:" + port + "/db?sslmode=disable");
            try {
                db.query("SELECT x FROM t", null);
                outcome = "decoded a row that cannot be decoded";
            } catch (Exception failed) {
                outcome = db.isOpen()
                        ? "left the session open: " + failed.getMessage()
                        : "closed";
            }
        } catch (Exception opening) {
            outcome = "never reached the query: " + opening.getMessage();
        } finally {
            if(db != null) {
                db.close();
            }
            listener.close();
        }
        stub.join(10000);
        check("a MySQL row that will not decode closes the session", "closed", outcome);
    }

    private static void mySqlStubSession(int client) throws Exception {
        mySqlStubSession(client, true);
    }

    /**
     * Enough of the server side to carry one prepared statement to its rows.
     *
     * @param pluginAuth whether the greeting advertises CLIENT_PLUGIN_AUTH and
     *                   names a plugin after its scramble. An older
     *                   MySQL-compatible server does neither, and ends the
     *                   scramble with a NUL that is not a plugin name.
     */
    private static void mySqlStubSession(int client, boolean pluginAuth) throws Exception {
        ByteArrayOutputStream greeting = new ByteArrayOutputStream();
        greeting.write(10);                                  // protocol version
        greeting.write("8.0.0-cn1stub".getBytes("UTF-8"));
        greeting.write(0);
        for(int iter = 0 ; iter < 4 ; iter++) {
            greeting.write(0);                               // connection id
        }
        for(int iter = 0 ; iter < 8 ; iter++) {
            greeting.write('a');                             // scramble, first part
        }
        greeting.write(0);                                   // filler
        greeting.write(0x00);
        greeting.write(0x82);            // PROTOCOL_41 | SECURE_CONNECTION, no SSL
        greeting.write(45);                                  // character set
        greeting.write(0);
        greeting.write(0);                                   // status flags
        greeting.write(pluginAuth ? 0x08 : 0x00);
        greeting.write(0x00);                                // PLUGIN_AUTH, or not
        greeting.write(pluginAuth ? 21 : 0);                 // scramble length
        for(int iter = 0 ; iter < 10 ; iter++) {
            greeting.write(0);                               // reserved
        }
        for(int iter = 0 ; iter < 12 ; iter++) {
            greeting.write('b');                             // scramble, second part
        }
        greeting.write(0);                                   // ends the scramble
        if(pluginAuth) {
            greeting.write("mysql_native_password".getBytes("UTF-8"));
            greeting.write(0);
        }
        mySqlStubSend(client, 0, greeting.toByteArray());

        while(true) {
            byte[] packet = mySqlStubReceive(client);
            if(packet == null) {
                return;
            }
            int sequence = packet[0] & 0xff;
            int command = packet.length > 1 ? packet[1] & 0xff : -1;
            if(command == 0x16) {                            // COM_STMT_PREPARE
                byte[] prepared = new byte[12];
                prepared[1] = 1;                             // statement id
                prepared[5] = 1;                             // one column
                mySqlStubSend(client, sequence + 1, prepared);
                mySqlStubSend(client, sequence + 2, mySqlStubColumn());
                mySqlStubSend(client, sequence + 3, mySqlStubEof());
            } else if(command == 0x17) {                     // COM_STMT_EXECUTE
                mySqlStubSend(client, sequence + 1, new byte[]{1});
                mySqlStubSend(client, sequence + 2, mySqlStubColumn());
                mySqlStubSend(client, sequence + 3, mySqlStubEof());
                // THE ROW: not null, and a value that says it is 16777215 bytes
                // long with none of them present.
                mySqlStubSend(client, sequence + 4, new byte[]{0, 0,
                        (byte) 0xfd, (byte) 0xff, (byte) 0xff, (byte) 0xff});
                mySqlStubSend(client, sequence + 5, mySqlStubEof());
            } else {
                mySqlStubSend(client, sequence + 1,
                        new byte[]{0, 0, 0, 2, 0, 0, 0});    // OK
            }
        }
    }

    private static byte[] mySqlStubColumn() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String[] strings = {"def", "db", "t", "t", "x", "x"};
        for(int iter = 0 ; iter < strings.length ; iter++) {
            byte[] bytes = strings[iter].getBytes("UTF-8");
            out.write(bytes.length);
            out.write(bytes, 0, bytes.length);
        }
        out.write(0x0c);                 // length of the fixed fields
        out.write(33);
        out.write(0);                    // character set, not 63, so not binary
        for(int iter = 0 ; iter < 4 ; iter++) {
            out.write(0);                // column length
        }
        out.write(0xfd);                 // VAR_STRING, read length-encoded
        out.write(0);
        out.write(0);                    // flags
        out.write(0);                    // decimals
        out.write(0);
        out.write(0);                    // filler
        return out.toByteArray();
    }

    private static byte[] mySqlStubEof() {
        return new byte[]{(byte) 0xfe, 0, 0, 2, 0};
    }

    private static void mySqlStubSend(int client, int sequence, byte[] body) throws Exception {
        byte[] packet = new byte[4 + body.length];
        packet[0] = (byte)(body.length & 0xff);
        packet[1] = (byte)((body.length >> 8) & 0xff);
        packet[2] = (byte)((body.length >> 16) & 0xff);
        packet[3] = (byte)(sequence & 0xff);
        System.arraycopy(body, 0, packet, 4, body.length);
        ServerSocket.write(client, packet, 0, packet.length);
    }

    /** The sequence number in element 0, the payload after it. Null at EOF. */
    private static byte[] mySqlStubReceive(int client) throws Exception {
        byte[] header = mySqlStubReadFully(client, 4);
        if(header == null) {
            return null;
        }
        int length = (header[0] & 0xff) | ((header[1] & 0xff) << 8)
                | ((header[2] & 0xff) << 16);
        byte[] body = length == 0 ? new byte[0] : mySqlStubReadFully(client, length);
        if(body == null) {
            return null;
        }
        byte[] out = new byte[body.length + 1];
        out[0] = header[3];
        System.arraycopy(body, 0, out, 1, body.length);
        return out;
    }

    private static byte[] mySqlStubReadFully(int client, int count) throws Exception {
        byte[] buffer = new byte[count];
        int at = 0;
        while(at < count) {
            int n = ServerSocket.read(client, buffer, at, count - at);
            if(n <= 0) {
                return null;
            }
            at += n;
        }
        return buffer;
    }

    private static void aTruncatedMySqlHeaderIsRefused() throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        final int port = listener.getPort();
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = listener.accept();
                    if(client < 0) {
                        return;
                    }
                    // Two bytes of a four byte header, then nothing. Enough to get
                    // past the one check there used to be.
                    ServerSocket.write(client, new byte[]{(byte) 0xff, (byte) 0xff}, 0, 2);
                } catch (Exception ignored) {
                    // Hanging up IS the scenario.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        String outcome;
        try {
            Database db = Database.open("mysql://u:pw@127.0.0.1:" + port
                    + "/db?sslmode=disable");
            db.close();
            outcome = "connected";
        } catch (Exception refused) {
            String message = String.valueOf(refused.getMessage());
            outcome = message.indexOf("closed mid-header") >= 0
                    ? "refused" : "other: " + refused.getClass().getName() + ": " + message;
        } finally {
            listener.close();
        }
        stub.join(10000);
        check("a truncated MySQL header is refused", "refused", outcome);
    }

    /**
     * An outer catch still catches when the try holds a nested try/finally.
     *
     * <p>Not a backend concern -- a VM one, found by a backend test. The row-frame
     * check below was first written as "open, then try/finally around the query to
     * close it", with catch(Exception) on the outer try. The packaged binary
     * printed "Uncaught exception java.io.IOException" and died, with a stack that
     * ran out through this method: the handler that encloses the throw did not
     * run. Flattening the method made the same assertion pass, which is why these
     * two shapes are pinned here rather than left as a puzzle in one test's
     * history.
     *
     * <p>Both arms run this. On Java SE it is trivially true, and that is the
     * point: if the arms disagree, this names the disagreement.
     */
    private static void aNestedFinallyDoesNotDefeatTheOuterCatch() throws Exception {
        // ORDER MATTERS: the cheap shapes first, because the one under suspicion
        // does not fail an assertion when it is wrong -- it takes the process with
        // it, and nothing after it would run to report anything.

        // 1. The finally is in a CALLEE. This is the ordinary shape -- a resource
        // closed where it was opened, guarded by a caller -- and it works.
        String belowUs;
        try {
            finallyInACallee();
            belowUs = "returned";
        } catch (Exception caught) {
            belowUs = "caught";
        }
        check("a callee's finally still reaches this catch", "caught", belowUs);

        // 2. No finally at all, same method. Works.
        String plain;
        try {
            throw new IOException("no finally anywhere");
        } catch (Exception caught) {
            plain = "caught";
        }
        check("a plain throw is caught in the same method", "caught", plain);

        // 3. BOTH IN ONE METHOD: the catch encloses a try/finally. THIS ONE IS
        // BROKEN on the packaged binary, which is why it does not run by default.
        //
        // Measured: it does not reach the catch at all. The binary prints
        // "Uncaught exception java.io.IOException: thrown in the inner try" and
        // exits 1, so it does not fail an assertion -- it takes the process with
        // it, and every check after it in this file with it. A finally compiles to
        // a catch-any that rethrows, and that rethrow is evidently not matched
        // against the handlers enclosing it in the SAME method. Case 1 above is
        // the same construct with the finally one frame down, and it is fine,
        // which is why this is not visible in ordinary code: the usual shape puts
        // the cleanup in the method that owns the resource.
        //
        // It is left here, runnable, because the reduced case is the whole bug
        // report: set CN1_SELFTEST_VM_PROBE=1 to watch it die. Fixing it belongs
        // in the translator's handler ranges, not here.
        if("1".equals(System.getenv("CN1_SELFTEST_VM_PROBE"))) {
            String sameMethod;
            try {
                try {
                    throw new IOException("thrown in the inner try");
                } finally {
                    touched++;      // the finally must run, and must not swallow
                }
            } catch (Exception caught) {
                sameMethod = "caught";
            }
            check("an enclosing catch sees a throw from a nested finally", "caught",
                    sameMethod);
        } else {
            note("nested-finally VM probe skipped: set CN1_SELFTEST_VM_PROBE=1 "
                    + "to run the case that kills the process");
        }
    }

    /**
     * A short authentication frame is an IOException, not a wild index.
     *
     * <p>The method code is four bytes the peer sends, and the md5 salt is four
     * more after it. Neither was checked, so a body shorter than that indexed past
     * the end -- and ArrayIndexOutOfBoundsException is not an IOException, so
     * connect()'s cleanup did not run and the socket leaked instead of being
     * refused. sslmode=prefer lets whoever answers decline TLS, so the peer doing
     * this need not have authenticated or be a server.
     *
     * <p>The assertion is the exception TYPE, because that is exactly what the bug
     * was: both versions throw, and only one of them throws something connect()
     * can clean up after.
     */
    private static void aShortAuthFrameIsRefused() throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        final int port = listener.getPort();
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = listener.accept();
                    if(client < 0) {
                        return;
                    }
                    ServerSocket.setTimeout(client, 10000);
                    if(pgRead(client) == null) {          // StartupMessage
                        return;
                    }
                    // 'R' with two bytes where the method code needs four.
                    pgSend(client, 'R', new byte[]{0, 0});
                } catch (Exception ignored) {
                    // The client hanging up is how this ends.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        String outcome;
        try {
            Database db = Database.open("postgres://u:pw@127.0.0.1:" + port
                    + "/db?sslmode=disable");
            db.close();
            outcome = "connected";
        } catch (Exception refused) {
            outcome = refused instanceof IOException
                    ? "refused" : "other: " + refused.getClass().getName();
        } finally {
            listener.close();
        }
        stub.join(10000);
        check("a short PostgreSQL auth frame is refused", "refused", outcome);
    }

    /**
     * A MySQL packet whose HEADER is complete and whose body is short.
     *
     * <p>Distinct from the truncated-header check above: the length arrives intact
     * and the body does not, so every accessor that walks the packet runs off the
     * end of it. The greeting is parsed this way before TLS, so a one-byte body is
     * enough and the peer need not be a database.
     *
     * <p>Asserted on the exception TYPE for the same reason as the PostgreSQL one:
     * the old behaviour threw too, and what mattered was that it threw something
     * connect() does not catch, so the socket leaked.
     */
    private static void aTruncatedMySqlBodyIsRefused() throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        final int port = listener.getPort();
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = listener.accept();
                    if(client < 0) {
                        return;
                    }
                    // A complete four byte header promising one byte, then that one
                    // byte: protocol 10, and nothing of the server version that has
                    // to follow it.
                    ServerSocket.write(client,
                            new byte[]{1, 0, 0, 0, (byte) 10}, 0, 5);
                } catch (Exception ignored) {
                    // Expected: the client gives up on us.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        String outcome;
        try {
            Database db = Database.open("mysql://u:pw@127.0.0.1:" + port
                    + "/db?sslmode=disable");
            db.close();
            outcome = "connected";
        } catch (Exception refused) {
            outcome = refused instanceof IOException
                    ? "refused" : "other: " + refused.getClass().getName();
        } finally {
            listener.close();
        }
        stub.join(10000);
        check("a truncated MySQL packet body is refused", "refused", outcome);
    }

    /**
     * A MySQL packet whose sequence byte is not the one due is refused.
     *
     * <p>The counter is this side's: the writer advances it packet by packet and a
     * read has to advance it the same way. Adopting the peer's byte instead meant
     * a stale packet -- one left over from an exchange this side thinks is
     * finished -- was parsed as the current response, and the session stayed
     * reusable, so desynchronisation turned into wrong data rather than an error.
     *
     * <p>The server's first handshake packet is due with sequence 0, so a stub
     * that sends 7 is the smallest form of the fault.
     */
    private static void aMySqlPacketOutOfSequenceIsRefused() throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        final int port = listener.getPort();
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = listener.accept();
                    if(client < 0) {
                        return;
                    }
                    // One byte of payload, announced with sequence 7 where 0 is due.
                    ServerSocket.write(client,
                            new byte[]{1, 0, 0, 7, (byte) 10}, 0, 5);
                } catch (Exception ignored) {
                    // Expected: the client gives up on us.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        String outcome;
        try {
            Database db = Database.open("mysql://u:pw@127.0.0.1:" + port
                    + "/db?sslmode=disable");
            db.close();
            outcome = "connected";
        } catch (Exception refused) {
            String message = String.valueOf(refused.getMessage());
            outcome = message.indexOf("no longer in step") >= 0
                    ? "refused" : "other: " + message;
        } finally {
            listener.close();
        }
        stub.join(10000);
        check("a MySQL packet out of sequence is refused", "refused", outcome);
    }

    /**
     * A PostgreSQL column length of -2 is a malformed frame, not a NULL.
     *
     * <p>The wire protocol gives -1 one meaning and leaves every other negative
     * undefined. Reading them all as NULL answered the query with a column the
     * caller sees as absent -- silently altered data -- and kept the session for
     * the next one.
     */
    private static void aNegativePostgresLengthThatIsNotNullIsRefused() throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        final int port = listener.getPort();
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = listener.accept();
                    if(client < 0) {
                        return;
                    }
                    ServerSocket.setTimeout(client, 10000);
                    if(pgRead(client) == null) {              // StartupMessage
                        return;
                    }
                    pgSend(client, 'R', int32(0));            // AuthenticationOk
                    pgSend(client, 'Z', new byte[]{(byte)'I'}); // ReadyForQuery, idle
                    while(pgRead(client) != null) {
                        // One column called "a", described in full, and then a row
                        // whose single column claims a length of -2.
                        pgSend(client, 'T', join(join(new byte[]{0, 1}, ascii("a\0")),
                                new byte[18]));
                        pgSend(client, 'D', join(new byte[]{0, 1}, int32(-2)));
                    }
                } catch (Exception ignored) {
                    // The client hanging up is how this ends.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        String outcome;
        try {
            Database db = Database.open("postgres://u:pw@127.0.0.1:" + port
                    + "/db?sslmode=disable");
            db.query("SELECT a", null);
            db.close();
            outcome = "answered";
        } catch (Exception refused) {
            String message = String.valueOf(refused.getMessage());
            outcome = message.indexOf("only -1 is NULL") >= 0
                    ? "refused" : "other: " + message;
        } finally {
            listener.close();
        }
        stub.join(10000);
        check("a PostgreSQL column length of -2 is refused", "refused", outcome);
    }

    /** try/finally here, the catch one frame up: the shape that works. */
    private static void finallyInACallee() throws IOException {
        try {
            throw new IOException("thrown below the try");
        } finally {
            touched++;
        }
    }

    /** Written by the finally blocks above so they cannot be optimised away. */
    private static int touched;

    /**
     * A row frame shorter than it claims is an IOException, not a wild index.
     *
     * <p>RowDescription and DataRow are walked at fixed offsets the PEER chose --
     * a name terminator, then eighteen bytes of descriptor per column. Nothing
     * checked that the body held them, so a truncated frame ran off the end and
     * raised ArrayIndexOutOfBoundsException. That is not an IOException: it left
     * the collect loop without draining ReadyForQuery and without closing, so the
     * session went back to the pool with the rest of the exchange still unread and
     * the next borrower would have read it as its own answer.
     *
     * <p>The stub authenticates with AuthenticationOk -- no password, no TLS --
     * and answers the first statement with a RowDescription that promises one
     * column and then stops. Asserting the wording rather than "it threw" matters
     * here more than usual: the unfixed code threw too.
     */
    private static void truncatedRowFramesAreRefused() throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        final int port = listener.getPort();
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = listener.accept();
                    if(client < 0) {
                        return;
                    }
                    ServerSocket.setTimeout(client, 10000);
                    if(pgRead(client) == null) {              // StartupMessage
                        return;
                    }
                    pgSend(client, 'R', int32(0));            // AuthenticationOk
                    pgSend(client, 'Z', new byte[]{(byte)'I'}); // ReadyForQuery, idle
                    // Whatever it asks, answer with a frame that promises one
                    // column and then ends: the count, a name, and four bytes
                    // where eighteen of descriptor belong.
                    while(pgRead(client) != null) {
                        pgSend(client, 'T', new byte[]{0, 1, (byte)'a', 0, 0, 0, 0, 0});
                    }
                } catch (Exception ignored) {
                    // The client hanging up is how this ends.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        String outcome;
        try {
            Database db = Database.open("postgres://u:pw@127.0.0.1:" + port
                    + "/db?sslmode=disable");
            db.query("SELECT 1", null);
            db.close();
            outcome = "answered";
        } catch (Exception refused) {
            String message = String.valueOf(refused.getMessage());
            outcome = message.indexOf("shorter than it claims") >= 0
                    ? "refused" : "other: " + refused.getClass().getName() + ": " + message;
        } finally {
            listener.close();
        }
        stub.join(10000);
        check("a truncated PostgreSQL row frame is refused", "refused", outcome);
    }

    /**
     * A URL scheme is recognised whatever case it is written in.
     *
     * <p>RFC 3986 makes a scheme case-insensitive, and anything unrecognised here
     * is taken as a SQLite PATH -- so "PostgreSQL://host/db" was not a mismatch
     * that got reported, it was a file name. The caller opened or created a local
     * database, or failed on a pathname with "//" in it, and nothing said the
     * server had never been contacted.
     *
     * <p>The stub only has to authenticate: reaching "connected" at all means the
     * URL was parsed as PostgreSQL rather than handed to SQLite.
     */
    private static void aSchemeIsRecognisedInAnyCase() throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        final int port = listener.getPort();
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = listener.accept();
                    if(client < 0) {
                        return;
                    }
                    ServerSocket.setTimeout(client, 10000);
                    if(pgRead(client) == null) {
                        return;
                    }
                    pgSend(client, 'R', int32(0));              // AuthenticationOk
                    pgSend(client, 'Z', new byte[]{(byte)'I'}); // ReadyForQuery
                    pgRead(client);                             // whatever follows
                } catch (Exception ignored) {
                    // The client closing is the end of it.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        String outcome;
        try {
            Database db = Database.open("PostgreSQL://u:pw@127.0.0.1:" + port
                    + "/db?sslmode=disable");
            outcome = db.isOpen() ? "connected" : "opened closed";
            db.close();
        } catch (Exception failed) {
            outcome = "failed: " + failed.getMessage();
        } finally {
            listener.close();
        }
        stub.join(10000);
        check("a mixed-case scheme still names PostgreSQL", "connected", outcome);
    }

    /**
     * A session whose peer hangs up is closed, not merely broken.
     *
     * <p>EOF between messages threw with the session still marked open, so
     * isOpen() described a dead connection as usable: DbPool handed it back out
     * and every disconnect left another descriptor behind. The stub authenticates,
     * says it is ready, and then simply goes away -- which is what a server
     * restart looks like from here.
     *
     * <p>The assertion is isOpen(), not that the query threw. The query threw
     * before this fix too; what it did not do was leave the session takeable.
     */
    private static void aDeadSessionReportsItselfClosed() throws Exception {
        final ServerSocket listener = ServerSocket.bind("127.0.0.1", 0, 1);
        final int port = listener.getPort();
        Thread stub = new Thread(new Runnable() {
            public void run() {
                int client = -1;
                try {
                    client = listener.accept();
                    if(client < 0) {
                        return;
                    }
                    ServerSocket.setTimeout(client, 10000);
                    if(pgRead(client) == null) {              // StartupMessage
                        return;
                    }
                    pgSend(client, 'R', int32(0));            // AuthenticationOk
                    pgSend(client, 'Z', new byte[]{(byte)'I'}); // ReadyForQuery
                    pgRead(client);                           // the first statement
                    // And then nothing: hang up mid-conversation.
                } catch (Exception ignored) {
                    // Going away IS the scenario.
                } finally {
                    if(client >= 0) {
                        ServerSocket.closeFd(client);
                    }
                }
            }
        });
        stub.start();
        String outcome;
        try {
            Database db = Database.open("postgres://u:pw@127.0.0.1:" + port
                    + "/db?sslmode=disable");
            try {
                db.query("SELECT 1", null);
                outcome = "answered";
            } catch (Exception expected) {
                outcome = db.isOpen() ? "still open" : "closed";
            }
        } catch (Exception refused) {
            outcome = "open failed: " + refused.getMessage();
        } finally {
            listener.close();
        }
        stub.join(10000);
        check("a session whose peer hung up reports itself closed", "closed", outcome);
    }

    /** One PostgreSQL message: type byte, length that counts itself, payload. */
    private static void pgSend(int fd, char type, byte[] payload) throws IOException {
        byte[] out = new byte[5 + payload.length];
        out[0] = (byte) type;
        int length = payload.length + 4;
        out[1] = (byte) (length >> 24);
        out[2] = (byte) (length >> 16);
        out[3] = (byte) (length >> 8);
        out[4] = (byte) length;
        System.arraycopy(payload, 0, out, 5, payload.length);
        ServerSocket.write(fd, out, 0, out.length);
    }

    /**
     * Whatever one read answers. Enough here because the client waits for a reply
     * before sending the next message, so each read is one whole message.
     */
    private static byte[] pgRead(int fd) throws IOException {
        byte[] buffer = new byte[4096];
        int got = ServerSocket.read(fd, buffer, 0, buffer.length);
        if(got <= 0) {
            return null;
        }
        byte[] out = new byte[got];
        System.arraycopy(buffer, 0, out, 0, got);
        return out;
    }

    private static byte[] int32(int value) {
        return new byte[]{(byte) (value >> 24), (byte) (value >> 16),
                (byte) (value >> 8), (byte) value};
    }

    private static byte[] ascii(String value) throws IOException {
        return value.getBytes("UTF-8");
    }

    private static byte[] join(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static void json() throws Exception {
        bothJsonWritersAgree();
        patchIsASendableVerb();
        aMethodIsOneToken();
        aGetWithABodyKeepsItsVerb();
        theOtherClientChecksItsRequestLine();
        anIpv6HostIsBracketed();
        outboundHeadersCannotCarryANewline();
        aRepeatedResponseHeaderKeepsEveryValue();
        aCaPathCannotBeTruncated();
        theTransportOwnsItsOwnFields();
        repeatedOutboundHeadersSurvive();
        anOversizedResponseIsRefusedNotAccumulated();
        onlyHttpUrlsAreFetched();
        aServerWithNoWorkersIsRefusedBeforeBinding();
        negativeConnectTimeoutsAreRefused();
        malformedPortsAreRefused();
        urlComponentsKeepTheirUnicode();
        boundParametersMustMatchThePlaceholders();
        storedTextComesBackUnchanged();
        rawSqlEncodesDatesAndCharsLikeTheOrm();
        scramIterationCountIsBounded();
        scramRefusesAPasswordItCannotPrepare();
        aTruncatedMySqlHeaderIsRefused();
        aMalformedRowClosesTheMySqlSession();
        anUnknownSslModeIsRefused();
        truncatedRowFramesAreRefused();
        aDeadSessionReportsItselfClosed();
        aSchemeIsRecognisedInAnyCase();
        aShortAuthFrameIsRefused();
        aTruncatedMySqlBodyIsRefused();
        aMySqlPacketOutOfSequenceIsRefused();
        aNegativePostgresLengthThatIsNotNullIsRefused();
        aStaticFileComesBackWhole();
        aRefusedBodyIsNeverInvited();
        anInterimResponseIsSkipped();
        aHandlerCanStopItsOwnServer();
        twoShutdownsAreOneTeardown();
        aServerThatNamesNoPluginStillConnects();
        aMalformedEscapeInADatabaseUrlIsRefused();
        aConnectionFieldCannotHoldASecondField();
        everyNativeStringRefusesANul();
        aTruncatingDatabasePathOpensNothing();
        aLaterIfRangeDateDoesNotAuthoriseARange();
        aTruncatingBindAddressBindsNothing();
        aClosedSocketWakesItsBlockedReader();
        theReactorProbesAndFiresOnce();
        modifyingIntoOneShotDisarms();
        modifyReplacesTheInterestRatherThanAddingToIt();
        removingADescriptorRemovesEveryFilter();
        aDirectoryRedirectStaysOnThisOrigin();
        aFramedResponseEndsAtItsFraming();
        aChunkSizeCannotWrapTheWalk();
        aRedirectDoesNotCarryTheBodyOffHost();
        aNullMethodWithAnEmptyBodyIsStillAGet();
        oneReadyDescriptorIsOneSlot();
        aTunableBelowItsFloorTakesTheDefault();
        aStalledPeerDoesNotHoldTheHandshake();
        aStalledDatabasePeerDoesNotHoldTheCaller();
        aResponsePastTheClientCeilingIsRefused();
        aZeroTimeoutReadinessCheckDoesNotWait();
        aRegionResolvesInItsOwnPartition();
        anUnnameablePartitionIsRefused();
        listingUpToNothingListsNothing();
        halfACredentialPairIsRefused();
        aRemoteCredentialEndpointMustBeEncrypted();
        anEndlessResponseIsRefused();
        aContradictoryLengthIsRefused();
        aTransferCodingOverridesTheLength();
        zeroCopyIsClaimedOnlyWhereItHappens();
        aChunkedResponseEndsWhereItsFramingSays();
        aResponseEndsAtItsDeclaredLength();
        aTruncatingPathOpensNothing();
        anUnopenableFileIsNotAnAbsentOne();
        aTlsVerificationNameWithANulIsRefused();
        aHeadResponseIsNotReadAsTruncated();
        aNullMethodIsGet();
        anOutboundFragmentIsRefused();
        caseVariantHeadersSignAsOneField();
        aLateBodyIsNotServedAnotherConnectionsBytes();
        aFileBackedResponseClosesItsDescriptorWhenTheHeadFails();
        anEncodedMountPrefixIsTheSameMount();
        aMountDeclaredWithAnEscapeIsStillReachable();
        theCaseOfAnEscapeDoesNotChangeTheRoute();
        anOutboundFailureDoesNotLogTheSecrets();
        aFieldValueIsOctetsOnEveryProtocol();
        aNestedFinallyDoesNotDefeatTheOuterCatch();
        expiryMarginIsDistinctFromExpiry();
        anImpossibleExpiryIsRefused();
        malformedDatesAreNotDates();
        asciiFoldingIsLocaleIndependent();
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

        // A bucket name is interpolated into the request URL, so one carrying a
        // slash steers it: into the path here, and into the HOST -- taking the
        // signed access key id and session token to a server of the caller's
        // choosing -- under virtual-hosted addressing. presign computes locally
        // and sends nothing, which is what makes this checkable here.
        //
        // forEndpoint is path style, so this covers that guard. The virtual-hosted
        // one needs forRegion, which resolves real credentials, so it is not
        // reachable from a self-test that must run with none.
        S3 s3 = S3.forEndpoint(new Credentials("AKIDEXAMPLE", "secret", null),
                "us-east-1", "s3.us-east-1.amazonaws.com");
        boolean refusedBucket = false;
        try {
            s3.presignGet("attacker.example/ignored", "k", 60);
        } catch (Exception expected) {
            refusedBucket = true;
        }
        // Both arms must refuse the same configuration. The translated one used to
        // cast this to an unsigned short, so 65536 became 0 and the server came up
        // on an arbitrary port while the Java SE loop rejected it.
        boolean refusedPort = false;
        try {
            ServerSocket.bind(null, 65536, 16);
        } catch (Exception expected) {
            refusedPort = true;
        }
        check("a port above 65535 is refused", "true", String.valueOf(refusedPort));
        check("a bucket name that rewrites the host is refused", "true",
                String.valueOf(refusedBucket));
        boolean signedOrdinary = false;
        try {
            signedOrdinary = s3.presignGet("ordinary-bucket", "k", 60)
                    .indexOf("/ordinary-bucket/k") > 0;
        } catch (Exception err) {
            signedOrdinary = false;
        }
        check("an ordinary bucket still signs", "true", String.valueOf(signedOrdinary));
        // A lifetime outside SigV4's 1 second to 7 days produces a URL that looks
        // right and is refused when the device tries to use it, which is a failure
        // a long way from the call that caused it.
        boolean refusedLifetime = false;
        try {
            s3.presignGet("ordinary-bucket", "k", 0);
        } catch (Exception expected) {
            refusedLifetime = true;
        }
        check("a presigned URL with no lifetime is refused", "true",
                String.valueOf(refusedLifetime));
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

    /**
     * Two threads, one shared Db, transactions that must not interleave.
     *
     * SQLite serializes each API CALL on a connection, which is what made "one
     * shared connection is correct, SQLite serializes it" look true. It does not
     * serialize a BEGIN/body/COMMIT sequence: without the lock, one thread's
     * BEGIN IMMEDIATE lands while another transaction is open and fails with
     * "cannot start a transaction within a transaction", or worse, a write from
     * one request is committed -- or rolled back -- by another that knows nothing
     * about it. Both are silent data corruption in the second case.
     */
    private static void sharedTransactionsDoNotInterleave() throws Exception {
        final Db db = Db.open(":memory:");
        try {
            db.execute("CREATE TABLE pair (tag TEXT)", null);
            final List failures = new ArrayList();
            Thread[] threads = new Thread[4];
            for(int t = 0 ; t < threads.length ; t++) {
                final String tag = "t" + t;
                threads[t] = new Thread(new Runnable() {
                    public void run() {
                        for(int round = 0 ; round < 15 ; round++) {
                            try {
                                db.transaction(new Db.Work() {
                                    public Object run(Db inner) throws Exception {
                                        // TWO writes, so an interleaving is visible
                                        // as an odd count for this tag.
                                        inner.execute("INSERT INTO pair (tag) VALUES (?)",
                                                new Object[]{tag});
                                        inner.execute("INSERT INTO pair (tag) VALUES (?)",
                                                new Object[]{tag});
                                        return null;
                                    }
                                });
                            } catch (Exception err) {
                                synchronized(failures) {
                                    failures.add(String.valueOf(err));
                                }
                                return;
                            }
                        }
                    }
                });
                threads[t].start();
            }
            for(int t = 0 ; t < threads.length ; t++) {
                threads[t].join(30000);
            }
            check("concurrent transactions on one connection all succeed", "0",
                    String.valueOf(failures.size())
                            + (failures.isEmpty() ? "" : " -> " + failures.get(0)));
            List rows = db.query("SELECT COUNT(*) AS n FROM pair", null);
            check("every transaction wrote both of its rows", "120",
                    String.valueOf(((Map)rows.get(0)).get("n")));
        } finally {
            db.close();
        }
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

        // CLOSED WHILE A READ IS INSIDE OPENSSL. The session handle is the SSL*
        // itself and SSL_read yields the VM thread while it is in there, so a
        // close on another thread -- which is what DbPool.close() does to a
        // connection a borrower still holds -- used to call SSL_free underneath
        // it. Nothing here can observe the free directly; what it asserts is the
        // behaviour that depends on the ordering being right: the blocked read
        // comes back rather than waiting for a peer that will never speak, and
        // the process is still alive to answer for it.
        int cameBack = 0;
        for(int attempt = 0 ; attempt < 5 ; attempt++) {
            final Tcp busy = Tcp.connect("api.github.com", 443, 10000);
            busy.startTls("api.github.com");
            final boolean[] returned = new boolean[1];
            Thread reader = new Thread(new Runnable() {
                public void run() {
                    byte[] buffer = new byte[256];
                    try {
                        busy.read(buffer, 0, buffer.length);
                    } catch (Exception expected) {
                        // Closed under it, which is the scenario.
                    }
                    returned[0] = true;
                }
            });
            reader.start();
            // Nothing has been sent, so by now it is blocked inside SSL_read.
            Thread.sleep(300);
            busy.close();
            reader.join(10000);
            if(returned[0]) {
                cameBack++;
            }
        }
        check("a read inside OpenSSL comes back when the socket is closed under it",
                "5", String.valueOf(cameBack));

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
    /**
     * A CA bundle replaced at the SAME PATH is re-read, rather than served from
     * the process's context cache.
     *
     * <p>The packaged arm caches one OpenSSL SSL_CTX per CA path, because building
     * one per connection would parse the whole bundle on every handshake. Keying
     * that cache on the path alone was wrong: a bundle is a mount, and mounts are
     * rotated under a stable name -- a Kubernetes secret, cert-manager, an RDS
     * refresh. The path never changes, so a long-lived backend went on trusting
     * the roots it read at startup and every connection failed the moment the
     * service presented a certificate signed by the new one, with a restart as
     * the only remedy. The Java SE arm reads the file per upgrade and so never
     * had the bug, which is the kind of divergence this whole file exists to find.
     *
     * <p>Rotating to a bundle that CANNOT load is what makes the check decisive.
     * A failed build is not cached, so rotating good-to-good would pass with or
     * without the fix; rotating good-to-broken can only be refused by a process
     * that went back to the file. The restore afterwards proves the cache
     * recovers, and that the context in use was never corrupted by the attempt.
     */
    private static void rotatedCaBundlesAreReRead() throws Exception {
        if(System.getenv("CN1_SELFTEST_NETWORK") == null) {
            note("CA rotation check skipped: set CN1_SELFTEST_NETWORK=1 to run it");
            return;
        }
        String source = System.getenv("CN1_SELFTEST_CA_BUNDLE");
        if(source == null) {
            note("CA rotation check skipped: set CN1_SELFTEST_CA_BUNDLE to a PEM bundle "
                    + "that verifies api.github.com");
            return;
        }
        byte[] good = readFile(source);
        if(good.length == 0) {
            note("CA rotation check skipped: " + source + " is empty");
            return;
        }
        String bundle = "/tmp/cn1-selftest-ca-" + System.currentTimeMillis() + ".pem";
        writeFile(bundle, good);
        check("the copied bundle verifies", "verified", tlsOutcome(bundle));

        // Rotate IN PLACE. A PEM that parses to no usable root is refused by
        // SSL_CTX_load_verify_locations, so a process that re-read the file
        // cannot complete this handshake and one serving its cached context can.
        writeFile(bundle, bytes("-----BEGIN CERTIFICATE-----\n"
                + "bm90IGEgY2VydGlmaWNhdGU=\n"
                + "-----END CERTIFICATE-----\n"));
        check("a rotated CA bundle is re-read, not served from the cache",
                "refused", tlsOutcome(bundle));

        // And back. The trailing comment keeps the SIZE different from the first
        // write, so the two are distinguishable even where the filesystem's
        // timestamps are too coarse to separate writes this close together.
        byte[] restored = new byte[good.length + 32];
        System.arraycopy(good, 0, restored, 0, good.length);
        System.arraycopy(bytes("\n# cn1-selftest restored bundle\n"), 0,
                restored, good.length, 32);
        writeFile(bundle, restored);
        check("and a restored bundle verifies again", "verified", tlsOutcome(bundle));
        new java.io.File(bundle).delete();
    }

    /** "verified" if api.github.com validates against the bundle at `path`. */
    private static String tlsOutcome(String path) {
        Tcp connection = null;
        try {
            connection = Tcp.connect("api.github.com", 443, 15000);
            connection.startTls("api.github.com", path);
            return "verified";
        } catch (Exception refused) {
            return "refused";
        } finally {
            if(connection != null) {
                try {
                    connection.close();
                } catch (Exception ignored) {
                    // Closing a socket whose handshake failed has nothing to report.
                }
            }
        }
    }

    private static byte[] readFile(String path) throws Exception {
        java.io.File file = new java.io.File(path);
        byte[] out = new byte[(int) file.length()];
        java.io.FileInputStream in = new java.io.FileInputStream(file);
        try {
            int at = 0;
            while(at < out.length) {
                int read = in.read(out, at, out.length - at);
                if(read < 0) {
                    break;
                }
                at += read;
            }
            return out;
        } finally {
            in.close();
        }
    }

    private static void writeFile(String path, byte[] data) throws Exception {
        java.io.FileOutputStream out = new java.io.FileOutputStream(path);
        try {
            out.write(data, 0, data.length);
        } finally {
            out.close();
        }
    }

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
