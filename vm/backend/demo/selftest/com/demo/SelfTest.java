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
import com.codename1.backend.Http;
import com.codename1.backend.Http1Date;
import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.Jwt;
import com.codename1.backend.ServerSocket;
import com.codename1.backend.StaticFiles;
import com.codename1.backend.Tcp;
import com.codename1.backend.Web;
import com.codename1.backend.aws.Credentials;
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
            // AN EMBEDDED NUL, built the same way and for the same reason. It is a
            // legal character in a Java string -- JSON carries them -- and
            // getBytes("UTF-8") encodes it as one zero byte, so binding the value
            // by its C length stopped there and stored "a" while reporting success.
            // The Java SE arm goes through JDBC and stored all three characters,
            // which made it another divergence rather than a plain truncation.
            String nulText = new String(new char[]{'a', '\0', 'b'});
            String[] values = new String[] {
                "caf\u00e9",
                "\ud83d\ude00 smile",
                escapeText,
                nulText,
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
        outboundHeadersCannotCarryANewline();
        repeatedOutboundHeadersSurvive();
        anOversizedResponseIsRefusedNotAccumulated();
        onlyHttpUrlsAreFetched();
        aServerWithNoWorkersIsRefusedBeforeBinding();
        negativeConnectTimeoutsAreRefused();
        malformedPortsAreRefused();
        urlComponentsKeepTheirUnicode();
        boundParametersMustMatchThePlaceholders();
        storedTextComesBackUnchanged();
        scramIterationCountIsBounded();
        aTruncatedMySqlHeaderIsRefused();
        truncatedRowFramesAreRefused();
        aDeadSessionReportsItselfClosed();
        aSchemeIsRecognisedInAnyCase();
        aShortAuthFrameIsRefused();
        aTruncatedMySqlBodyIsRefused();
        aStaticFileComesBackWhole();
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
