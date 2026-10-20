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
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.codename1.backend.Backend;
import com.codename1.backend.ClientCertificate;
import com.codename1.backend.Config;
import com.codename1.backend.HttpServer;
import com.codename1.backend.Tasks;
import com.codename1.backend.VirtualThread;
import com.codename1.backend.Web;
import com.codename1.backend.WebClient;
import java.util.Properties;

/**
 * Drives WebClient's mutual TLS, HTTP/2 requirement, timeouts and connection
 * reuse against the local fixture servers of tests/web-mtls-h2.sh, on whichever
 * runtime it was built for, and reports what came back.
 *
 * The servers answer "cn=NAME conn=PORT proto=VERSION": the client certificate's
 * common name as the SERVER verified it, the client's source port (a reused
 * connection keeps it) and the protocol the server spoke. So every check reads
 * what arrived, not what the client believes it sent.
 *
 * Environment: CN1_MTLS_H2 (https base of the HTTP/2 server), CN1_MTLS_H1 (the
 * HTTP/1.1-only one), CN1_MTLS_DIR (the credentials), CN1_MTLS_ARM ("native" or
 * "javase", which decides what an HTTP/2 requirement must do).
 */
public class MtlsCheck {
    private static int passed;
    private static final List failures = new ArrayList();
    private static String dir;

    public static void main(String[] args) throws Exception {
        String h2 = System.getenv("CN1_MTLS_H2");
        String h1 = System.getenv("CN1_MTLS_H1");
        dir = System.getenv("CN1_MTLS_DIR");
        boolean nativeArm = !"javase".equals(System.getenv("CN1_MTLS_ARM"));
        if (h2 == null || h1 == null || dir == null) {
            System.out.println("CN1_MTLS_H2, CN1_MTLS_H1 and CN1_MTLS_DIR must be set");
            System.out.println("MTLSCHECK FAILED");
            System.exit(1);
        }
        ClientCertificate alice = ClientCertificate.pem(read("alice.crt"), read("alice.key"), null);
        ClientCertificate bob = ClientCertificate.pem(read("bob.crt"), read("bob.key"), null);

        // --- mutual TLS, HTTP/1.1 ---
        WebClient a1 = new WebClient().setClientCertificate(alice);
        check("PEM credential is accepted", "cn=alice", field(body(a1, h1 + "/who"), "cn"));
        check("HTTP/1.1 server reports HTTP/1.1", "HTTP/1.1", protocol(a1, h1 + "/who"));
        WebClient p12 = new WebClient().setClientCertificate(
                ClientCertificate.pkcs12(read("alice.p12"), "p12-secret"));
        check("PKCS#12 credential is accepted", "cn=alice", field(body(p12, h1 + "/who"), "cn"));
        // An encrypted PEM key: OpenSSL reads it on the packaged arm; the Java SE
        // arm refuses it with the conversion to make.
        WebClient encrypted = new WebClient().setClientCertificate(
                ClientCertificate.pem(read("alice.crt"), read("alice-enc.key"), "pem-secret"));
        String enc = body(encrypted, h1 + "/who");
        if (nativeArm) {
            check("an encrypted PEM key with its passphrase is accepted", "cn=alice", field(enc, "cn"));
        } else {
            check("an encrypted PEM key names the conversion", "true",
                    String.valueOf(enc.indexOf("openssl pkcs8") >= 0));
        }

        check("no credential is rejected", "failed", outcome(new WebClient(), h1 + "/who"));
        check("a key that is not the certificate's is rejected", "failed",
                outcome(new WebClient().setClientCertificate(
                        ClientCertificate.pem(read("alice.crt"), read("bob.key"), null)), h1 + "/who"));
        check("an expired certificate is rejected", "failed",
                outcome(new WebClient().setClientCertificate(
                        ClientCertificate.pem(read("expired.crt"), read("expired.key"), null)), h1 + "/who"));
        check("a certificate from an untrusted CA is rejected", "failed",
                outcome(new WebClient().setClientCertificate(
                        ClientCertificate.pem(read("rogue.crt"), read("rogue.key"), null)), h1 + "/who"));
        check("a wrong PKCS#12 password is rejected", "failed",
                outcome(new WebClient().setClientCertificate(
                        ClientCertificate.pkcs12(read("alice.p12"), "not-the-password")), h1 + "/who"));
        check("the plain Web API presents no credential", "failed", outcome(null, h1 + "/who"));

        // --- verification of the SERVER stays on ---
        // The server certificate names localhost only, so 127.0.0.1 -- the same server -- must fail verification.
        check("a server certificate for another name is rejected", "failed",
                outcome(new WebClient().setClientCertificate(alice), swapHost(h1, "127.0.0.1") + "/who"));

        // --- two identities never share a connection ---
        WebClient b1 = new WebClient().setClientCertificate(bob);
        String isolation = "ok";
        for (int iter = 0 ; iter < 4 ; iter++) {
            String seenA = field(body(a1, h1 + "/who"), "cn");
            String seenB = field(body(b1, h1 + "/who"), "cn");
            if (!"cn=alice".equals(seenA) || !"cn=bob".equals(seenB)) {
                isolation = "round " + iter + ": " + seenA + " / " + seenB;
            }
        }
        check("interleaved clients keep their own identity", "ok", isolation);

        // --- connection reuse ---
        WebClient reuse = new WebClient().setClientCertificate(alice);
        String first = field(body(reuse, h1 + "/who"), "conn");
        String second = field(body(reuse, h1 + "/who"), "conn");
        check("a client reuses its connection", "true", String.valueOf(first != null && first.equals(second)));
        WebClient fresh = new WebClient().setClientCertificate(alice).setReuseConnections(false);
        first = field(body(fresh, h1 + "/who"), "conn");
        second = field(body(fresh, h1 + "/who"), "conn");
        check("setReuseConnections(false) opens a new connection", "true",
                String.valueOf(first != null && second != null && !first.equals(second)));
        check("a client's configuration is fixed by its first request", "IllegalStateException",
                setterAfterUse(reuse));
        reuse.close();
        check("a closed client refuses requests", "failed", outcome(reuse, h1 + "/who"));

        // --- timeouts ---
        WebClient quick = new WebClient().setClientCertificate(alice).setRequestTimeoutMillis(1000);
        long started = System.currentTimeMillis();
        String slow = outcome(quick, h1 + "/slow");
        long took = System.currentTimeMillis() - started;
        check("the request timeout ends a slow request", "failed", slow);
        check("the request timeout ends it promptly", "true", String.valueOf(took < 2500));

        // --- HTTP/2 ---
        WebClient strict = new WebClient().setClientCertificate(alice).setRequireHttp2(true);
        WebClient preferred = new WebClient().setClientCertificate(alice);
        int before = count(a1, h1);
        String refused = body(strict, h1 + "/who");
        // What makes the refusal safe: the HTTP/1.1 peer never saw the request.
        check("the refused request never reached the HTTP/1.1 peer", String.valueOf(before),
                String.valueOf(count(a1, h1)));
        if (nativeArm) {
            check("required HTTP/2 succeeds over HTTP/2", "proto=HTTP/2",
                    field(body(strict, h2 + "/who"), "proto"));
            check("required HTTP/2 reports HTTP/2", "HTTP/2", protocol(strict, h2 + "/who"));
            check("required HTTP/2 presents the credential", "cn=alice",
                    field(body(strict, h2 + "/who"), "cn"));
            check("preferred HTTP/2 is negotiated", "HTTP/2", protocol(preferred, h2 + "/who"));
            check("an HTTP/1.1-only peer is refused when HTTP/2 is required", "true",
                    String.valueOf(refused.indexOf("HTTP/2 is required") >= 0));
        } else {
            check("required HTTP/2 is refused by the Java SE runtime", "true",
                    String.valueOf(refused.indexOf("cannot make an HTTP/2") >= 0));
            check("preferred HTTP/2 falls back to HTTP/1.1 on Java SE", "HTTP/1.1",
                    protocol(preferred, h1 + "/who"));
        }

        onVirtualThreads(h1, h2, alice, bob, nativeArm);

        System.out.println("passed=" + passed + " failed=" + failures.size());
        for (int iter = 0 ; iter < failures.size() ; iter++) {
            System.out.println("FAIL " + failures.get(iter));
        }
        System.out.println(failures.isEmpty() ? "MTLSCHECK OK" : "MTLSCHECK FAILED");
        if (!failures.isEmpty()) {
            System.exit(1);
        }
    }

    /**
     * The same checks from inside virtual threads, which is where a request
     * handler makes them: on the packaged arm a WebClient request there PARKS
     * instead of blocking, and its pooled connections are driven through the
     * parked wait rather than poll(). Everything above ran on the main thread,
     * which cannot park. Two tasks on ONE host, so alice's and bob's requests
     * interleave on the same pthread while each holds its own pool.
     */
    private static void onVirtualThreads(final String h1, final String h2,
                                         ClientCertificate alice, ClientCertificate bob,
                                         final boolean nativeArm) throws Exception {
        Backend backend = Backend.builder(Config.of(new Properties(), "test"))
                .quiet().host("127.0.0.1").port(0).workers(1)
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) {
                        return HttpServer.Response.text(200, "ok");
                    }
                }).start();
        try {
            final WebClient a = new WebClient().setClientCertificate(alice);
            final WebClient b = new WebClient().setClientCertificate(bob)
                    .setRequireHttp2(nativeArm);
            final Results results = new Results();
            Tasks.virtual(new Runnable() {
                public void run() {
                    StringBuilder out = new StringBuilder();
                    String conn = null;
                    boolean sameConn = true;
                    for (int iter = 0 ; iter < 4 ; iter++) {
                        String seen = body(a, h1 + "/who");
                        out.append(field(seen, "cn")).append(' ');
                        String c = field(seen, "conn");
                        if (conn != null && !conn.equals(c)) {
                            sameConn = false;
                        }
                        conn = c;
                    }
                    results.put("alice", out.toString().trim() + (sameConn ? " reused" : " new")
                            + (nativeArm && !VirtualThread.isVirtual() ? " NOT-VIRTUAL" : ""));
                }
            });
            Tasks.virtual(new Runnable() {
                public void run() {
                    StringBuilder out = new StringBuilder();
                    for (int iter = 0 ; iter < 4 ; iter++) {
                        String seen = body(b, (nativeArm ? h2 : h1) + "/who");
                        out.append(field(seen, "cn")).append(' ');
                        if (nativeArm) {
                            out.append(field(seen, "proto")).append(' ');
                        }
                    }
                    results.put("bob", out.toString().trim());
                }
            });
            results.await(2, 30000);
            check("virtual thread: alice keeps identity and connection",
                    "cn=alice cn=alice cn=alice cn=alice reused", results.get("alice"));
            check("virtual thread: bob keeps identity" + (nativeArm ? " over HTTP/2" : ""),
                    nativeArm ? "cn=bob proto=HTTP/2 cn=bob proto=HTTP/2 cn=bob proto=HTTP/2 cn=bob proto=HTTP/2"
                            : "cn=bob cn=bob cn=bob cn=bob", results.get("bob"));
            a.close();
            b.close();
        } finally {
            backend.stop();
        }
    }

    private static final class Results {
        private final java.util.Map values = new java.util.HashMap();

        synchronized void put(String key, String value) {
            values.put(key, value);
        }

        synchronized String get(String key) {
            Object value = values.get(key);
            return value == null ? "no result" : (String) value;
        }

        void await(int count, long millis) throws InterruptedException {
            long deadline = System.currentTimeMillis() + millis;
            while (System.currentTimeMillis() < deadline) {
                synchronized (this) {
                    if (values.size() >= count) {
                        return;
                    }
                }
                Thread.sleep(10);
            }
        }
    }

    private static byte[] read(String name) throws Exception {
        InputStream in = new FileInputStream(dir + "/" + name);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    /** The body on a 200, otherwise the status or the exception message. */
    private static String body(WebClient client, String url) {
        try {
            Web.Result r = client == null ? Web.get(url) : client.get(url);
            if (r.getStatus() != 200) {
                return "status " + r.getStatus();
            }
            return r.getBodyAsString();
        } catch (Exception err) {
            return "threw " + err.getMessage();
        }
    }

    private static String protocol(WebClient client, String url) {
        try {
            return client.get(url).getProtocol();
        } catch (Exception err) {
            return "threw " + err.getMessage();
        }
    }

    /** "ok" for a 200, "failed" for anything that did not authenticate. */
    private static String outcome(WebClient client, String url) {
        String seen = body(client, url);
        return seen.startsWith("cn=") ? "ok: " + seen : "failed";
    }

    /** One "name=value" field of the fixture's answer, or the whole answer. */
    private static String field(String answer, String name) {
        int at = answer.indexOf(name + "=");
        if (at < 0) {
            return answer;
        }
        int end = answer.indexOf(' ', at);
        return answer.substring(at, end < 0 ? answer.length() : end).trim();
    }

    /** How many requests the HTTP/1.1 server has handled, /count itself excluded. */
    private static int count(WebClient client, String h1) {
        String seen = field(body(client, h1 + "/count"), "count");
        try {
            return Integer.parseInt(seen.substring("count=".length()));
        } catch (Exception err) {
            return -1000;
        }
    }

    private static String setterAfterUse(WebClient client) {
        try {
            client.setRequestTimeoutMillis(5);
            return "accepted";
        } catch (IllegalStateException expected) {
            return "IllegalStateException";
        }
    }

    private static String swapHost(String base, String host) {
        int start = base.indexOf("://") + 3;
        int colon = base.indexOf(':', start);
        return base.substring(0, start) + host + base.substring(colon);
    }

    private static void check(String name, String expected, String actual) {
        if (expected.equals(actual)) {
            passed++;
        } else {
            failures.add(name + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
