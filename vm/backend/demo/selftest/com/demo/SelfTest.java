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
import com.codename1.backend.ByteSink;
import com.codename1.backend.Crypto;
import com.codename1.backend.Database;
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
import com.codename1.backend.aws.Credentials;
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

    private static void json() throws Exception {
        bothJsonWritersAgree();
        patchIsASendableVerb();
        repeatedOutboundHeadersSurvive();
        negativeConnectTimeoutsAreRefused();
        malformedPortsAreRefused();
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
