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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.codename1.backend.HttpProxy;
import com.codename1.backend.HttpServer;
import com.codename1.backend.Web;

/// Streaming bodies on whichever runtime this is built for: a spooled upload,
/// a multipart file parsed in place, an outbound exchange in both directions,
/// cancellation and the proxy.
///
/// With no argument it serves itself on an ephemeral port and runs every check
/// as its own client, then prints STREAMCHECK OK. With `serve <port>` it only
/// serves, so tests/stream-check.sh can send the same uploads over HTTP/1.1 and
/// cleartext HTTP/2 and compare the answers.
public class StreamCheck {
    private static final int BIG = 3 * 1024 * 1024;
    private static int passed;
    private static final List failures = new ArrayList();
    private static volatile boolean writerFailed;
    private static volatile boolean writerDone;
    private static volatile String seenDrop = "<unset>";
    private static volatile String seenKeep = "<unset>";
    private static HttpProxy proxy;
    private static HttpProxy elsewhere;

    public static void main(String[] args) throws Exception {
        if (args != null && args.length == 2 && "serve".equals(args[0])) {
            HttpServer.start("127.0.0.1", Integer.parseInt(args[1]), 64, 4, handler());
            System.out.println("STREAMCHECK SERVING");
            while (true) {
                Thread.sleep(60000);
            }
        }
        HttpServer server = HttpServer.start("127.0.0.1", 0, 64, 4, handler());
        String base = "http://127.0.0.1:" + server.getPort();
        proxy = new HttpProxy(base);
        elsewhere = new HttpProxy(base);
        try {
            upload(base);
            download(base);
            cancellation(base);
            proxied(base);
        } finally {
            server.stop(2000);
        }
        System.out.println("passed=" + passed + " failed=" + failures.size());
        for (int iter = 0 ; iter < failures.size() ; iter++) {
            System.out.println("FAIL " + failures.get(iter));
        }
        System.out.println(failures.isEmpty() ? "STREAMCHECK OK" : "STREAMCHECK FAILED");
        if (!failures.isEmpty()) {
            System.exit(1);
        }
    }

    private static void upload(String base) throws IOException {
        Web.Exchange up = Web.open("POST", base + "/sum", header(
                "Content-Type: application/octet-stream"), BIG);
        try {
            OutputStream out = up.getRequestBody();
            byte[] block = pattern(64 * 1024);
            for (int iter = 0 ; iter < BIG / block.length ; iter++) {
                out.write(block);
            }
            out.close();
            check("a large upload reaches the handler as a stream", "200 " + BIG + " "
                    + sum(pattern(64 * 1024)) * (BIG / (64 * 1024)),
                    up.getStatus() + " " + text(up.getResponseBody()));
        } finally {
            up.close();
        }
    }

    private static void download(String base) throws IOException {
        Web.Exchange down = Web.open("GET", base + "/big", null, Web.NO_BODY);
        try {
            InputStream in = down.getResponseBody();
            byte[] buffer = new byte[16384];
            long total = 0;
            long s = 0;
            int n;
            while ((n = in.read(buffer)) > 0) {
                total += n;
                s += sum(buffer, n);
            }
            check("a streamed response arrives whole", "200 " + BIG + " "
                    + sum(pattern(64 * 1024)) * (BIG / (64 * 1024)),
                    down.getStatus() + " " + total + " " + s);
        } finally {
            down.close();
        }
    }

    private static void cancellation(String base) throws Exception {
        Web.Exchange down = Web.open("GET", base + "/endless", null, Web.NO_BODY);
        byte[] some = new byte[1024];
        int got = down.getResponseBody().read(some);
        down.close();
        long deadline = System.currentTimeMillis() + 20000;
        while (!writerDone && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        check("closing an exchange early stops the producer", "true true true",
                (got > 0) + " " + writerDone + " " + writerFailed);
    }

    private static void proxied(String base) throws IOException {
        List headers = header("Content-Type: application/octet-stream");
        headers.add("X-Keep: kept");
        Web.Exchange up = Web.open("POST", base + "/proxy/sum", headers, BIG);
        try {
            OutputStream out = up.getRequestBody();
            byte[] block = pattern(64 * 1024);
            for (int iter = 0 ; iter < BIG / block.length ; iter++) {
                out.write(block);
            }
            out.close();
            check("the proxy streams a body through", "200 " + BIG + " "
                    + sum(pattern(64 * 1024)) * (BIG / (64 * 1024)),
                    up.getStatus() + " " + text(up.getResponseBody()));
            check("and forwards end-to-end headers", "kept", seenKeep);
        } finally {
            up.close();
        }
        Web.Result refused = Web.request("GET", base + "/proxy-elsewhere", null, null);
        check("the proxy refuses a destination it was not given", "502",
                String.valueOf(refused.getStatus()));
        // An exchange never follows a redirect itself, so this sees what the
        // proxy answered.
        Web.Exchange redirect = Web.open("GET", base + "/proxy/redirect", null, Web.NO_BODY);
        try {
            check("and passes a redirect through rather than following it", "302 /sum",
                    redirect.getStatus() + " " + redirect.getHeader("Location"));
        } finally {
            redirect.close();
        }
    }

    private static HttpServer.Handler handler() {
        return new HttpServer.Handler() {
            public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                String path = request.getTarget();
                if (path.startsWith("/proxy-elsewhere")) {
                    return elsewhere.forward(request, "http://example.com/");
                }
                if (path.startsWith("/proxy/")) {
                    return proxy.forward(request, path.substring("/proxy".length()));
                }
                if (path.startsWith("/redirect")) {
                    HttpServer.Response r = HttpServer.Response.text(302, "moved");
                    r.header("Location", "/sum");
                    return r;
                }
                if (path.startsWith("/sum")) {
                    seenKeep = String.valueOf(request.getHeader("X-Keep"));
                    seenDrop = String.valueOf(request.getHeader("X-Drop"));
                    InputStream in = request.getBodyStream();
                    byte[] buffer = new byte[16384];
                    long total = 0;
                    long s = 0;
                    int n;
                    while ((n = in.read(buffer)) > 0) {
                        total += n;
                        s += sum(buffer, n);
                    }
                    return HttpServer.Response.text(200, total + " " + s);
                }
                if (path.startsWith("/parts")) {
                    List parts = request.getParts();
                    StringBuilder out = new StringBuilder();
                    for (int iter = 0 ; iter < parts.size() ; iter++) {
                        HttpServer.Part part = (HttpServer.Part) parts.get(iter);
                        InputStream in = part.getInputStream();
                        byte[] buffer = new byte[16384];
                        long s = 0;
                        int n;
                        while ((n = in.read(buffer)) > 0) {
                            s += sum(buffer, n);
                        }
                        out.append(part.getName()).append('=').append(part.getLength())
                                .append('/').append(s).append(' ');
                    }
                    return HttpServer.Response.text(200, out.toString().trim());
                }
                if (path.startsWith("/headers")) {
                    return HttpServer.Response.text(200, "drop=" + seenDrop + " keep=" + seenKeep);
                }
                if (path.startsWith("/big")) {
                    return HttpServer.Response.stream(200, "application/octet-stream",
                            new HttpServer.BodyWriter() {
                                public void writeTo(OutputStream output) throws IOException {
                                    byte[] block = pattern(64 * 1024);
                                    for (int iter = 0 ; iter < BIG / block.length ; iter++) {
                                        output.write(block);
                                    }
                                }
                            });
                }
                if (path.startsWith("/endless")) {
                    writerDone = false;
                    writerFailed = false;
                    return HttpServer.Response.stream(200, "application/octet-stream",
                            new HttpServer.BodyWriter() {
                                public void writeTo(OutputStream output) throws IOException {
                                    byte[] block = pattern(64 * 1024);
                                    try {
                                        for (int iter = 0 ; iter < 16384 ; iter++) {
                                            output.write(block);
                                        }
                                    } catch (IOException err) {
                                        writerFailed = true;
                                        throw err;
                                    } finally {
                                        writerDone = true;
                                    }
                                }
                            });
                }
                return HttpServer.Response.text(404, "no");
            }
        };
    }

    private static byte[] pattern(int length) {
        byte[] out = new byte[length];
        for (int iter = 0 ; iter < length ; iter++) {
            out[iter] = (byte) (iter * 31 + 7);
        }
        return out;
    }

    private static long sum(byte[] data) {
        return sum(data, data.length);
    }

    private static long sum(byte[] data, int length) {
        long s = 0;
        for (int iter = 0 ; iter < length ; iter++) {
            s += data[iter] & 0xff;
        }
        return s;
    }

    private static String text(InputStream in) throws IOException {
        StringBuilder out = new StringBuilder();
        int c;
        while ((c = in.read()) >= 0) {
            out.append((char) c);
        }
        return out.toString();
    }

    private static List header(String line) {
        List out = new ArrayList();
        out.add(line);
        return out;
    }

    private static void check(String name, String expected, String actual) {
        if (expected.equals(actual)) {
            passed++;
            System.out.println("ok   " + name);
        } else {
            failures.add(name + ": expected <" + expected + "> but was <" + actual + ">");
            System.out.println("FAIL " + name + ": expected <" + expected + "> but was <"
                    + actual + ">");
        }
    }
}
