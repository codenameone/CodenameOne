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
package com.codenameone.examples.hellocodenameone.backend;

import com.codename1.backend.Base64;
import com.codename1.backend.HttpServer;
import com.codename1.backend.HttpSession;
import com.codename1.backend.Json;
import com.codename1.backend.annotations.DeleteMapping;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PatchMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.PutMapping;
import com.codename1.backend.annotations.RequestHeader;
import com.codename1.backend.annotations.RequestMapping;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.annotations.RequestPart;
import com.codename1.backend.annotations.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// What the app's networking tests probe that a typed API does not: every verb,
/// status codes, redirects, cookies and sessions, authentication, large and gzip
/// bodies, downloads, uploads and timeouts. Each answer is deterministic, so a
/// client test can assert on it exactly.
@RestController
@RequestMapping("/api")
public class ProbeApi {
    /// The basic credentials /api/auth/basic accepts: user and pass.
    static final String BASIC = "Basic " + Base64.encode(ascii("user:pass"));
    /// The bearer token /api/auth/bearer accepts.
    static final String TOKEN = "Bearer token-123";

    /// The server is up, and what it is.
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("status", "up");
        out.put("server", "hellocodenameone-backend");
        return out;
    }

    @GetMapping("/hello/{name}")
    public String hello(@PathVariable("name") String name) {
        return "Hello, " + name;
    }

    @GetMapping("/echo")
    public HttpServer.Response echoGet(HttpServer.Request request) {
        return echo(request);
    }

    @PostMapping("/echo")
    public HttpServer.Response echoPost(HttpServer.Request request) {
        return echo(request);
    }

    @PutMapping("/echo")
    public HttpServer.Response echoPut(HttpServer.Request request) {
        return echo(request);
    }

    @PatchMapping("/echo")
    public HttpServer.Response echoPatch(HttpServer.Request request) {
        return echo(request);
    }

    @DeleteMapping("/echo")
    public HttpServer.Response echoDelete(HttpServer.Request request) {
        return echo(request);
    }

    /// The request as the server saw it: method, path, query parameters, the
    /// headers a test sets, and the body.
    private static HttpServer.Response echo(HttpServer.Request request) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("method", request.getMethod());
        out.put("target", request.getTarget());
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        String[] names = {"a", "b", "q", "page", "name"};
        for (String n : names) {
            String v = request.param(n);
            if (v != null) {
                params.put(n, v);
            }
        }
        out.put("params", params);
        Map<String, Object> headers = new LinkedHashMap<String, Object>();
        String[] interesting = {"X-Test", "X-Other", "Accept", "Content-Type", "User-Agent",
                "Authorization", "Accept-Encoding", "Host"};
        for (String h : interesting) {
            String v = request.getHeader(h);
            if (v != null) {
                headers.put(h, v);
            }
        }
        out.put("headers", headers);
        byte[] body = request.getBodyBytes();
        out.put("bodyLength", Integer.valueOf(body == null ? 0 : body.length));
        if (body != null) {
            try {
                out.put("body", request.getBody());
            } catch (IllegalStateException binary) {
                out.put("body", null);
            }
        }
        return HttpServer.Response.json(200, Json.write(out)).header("X-Probe", "echo");
    }

    /// Answers with the status asked for.
    @GetMapping("/status/{code}")
    public HttpServer.Response status(@PathVariable("code") int code) {
        if (code < 200 || code > 599) {
            return HttpServer.Response.text(400, "a status is 200 to 599");
        }
        if (code == 204 || code == 304) {
            return HttpServer.Response.empty(code, null, null);
        }
        return HttpServer.Response.text(code, "status " + code);
    }

    /// Redirects `n` times, then answers "landed".
    @GetMapping("/redirect/{n}")
    public HttpServer.Response redirect(@PathVariable("n") int n) {
        if (n <= 0) {
            return HttpServer.Response.text(200, "landed");
        }
        return HttpServer.Response.empty(302, null, null)
                .header("Location", "/api/redirect/" + (n - 1));
    }

    /// Sets a cookie.
    @GetMapping("/cookie/set")
    public HttpServer.Response setCookie(@RequestParam("name") String name,
                                         @RequestParam("value") String value) {
        return HttpServer.Response.text(200, "set " + name)
                .header("Set-Cookie", name + "=" + value + "; Path=/");
    }

    /// The value of a cookie the client sent, or "none".
    @GetMapping("/cookie/read")
    public String readCookie(HttpServer.Request request, @RequestParam("name") String name) {
        String value = request.getCookie(name);
        return value == null ? "none" : value;
    }

    /// A per-session counter, which needs the session cookie to round-trip.
    @GetMapping("/session/count")
    public String sessionCount(HttpServer.Request request) {
        HttpSession session = request.getSession(true);
        Object previous = session.getAttribute("count");
        int count = previous instanceof Integer ? ((Integer) previous).intValue() + 1 : 1;
        session.setAttribute("count", Integer.valueOf(count));
        return String.valueOf(count);
    }

    @GetMapping("/auth/basic")
    public HttpServer.Response basic(@RequestHeader(value = "Authorization", required = false)
                                     String authorization) {
        if (BASIC.equals(authorization)) {
            return HttpServer.Response.text(200, "user");
        }
        return HttpServer.Response.text(401, "who are you?")
                .header("WWW-Authenticate", "Basic realm=\"probe\"");
    }

    @GetMapping("/auth/bearer")
    public HttpServer.Response bearer(@RequestHeader(value = "Authorization", required = false)
                                      String authorization) {
        if (TOKEN.equals(authorization)) {
            return HttpServer.Response.text(200, "token accepted");
        }
        return HttpServer.Response.text(401, "missing or wrong token");
    }

    /// `size` characters of a repeating, compressible pattern, as JSON-free text;
    /// over a kilobyte it is gzipped for a client that accepts it.
    @GetMapping("/big")
    public String big(@RequestParam(value = "size", defaultValue = "4096") int size) {
        StringBuilder sb = new StringBuilder(size);
        String pattern = "Codename One backend probe. ";
        while (sb.length() < size) {
            sb.append(pattern);
        }
        sb.setLength(Math.max(0, size));
        return sb.toString();
    }

    /// `size` bytes whose i-th byte is `i & 0xff`, as a file to download.
    @GetMapping("/download/{size}")
    public HttpServer.Response download(@PathVariable("size") int size) {
        int n = Math.max(0, Math.min(size, 8 * 1024 * 1024));
        byte[] data = new byte[n];
        for (int iter = 0 ; iter < n ; iter++) {
            data[iter] = (byte) iter;
        }
        return new HttpServer.Response(200, "application/octet-stream", data)
                .header("Content-Disposition", "attachment; filename=\"probe.bin\"");
    }

    /// A multipart upload: the file's name, size and checksum, and the fields.
    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestPart("file") HttpServer.Part file,
                                      @RequestParam(value = "note", required = false) String note) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("name", file.getName());
        out.put("filename", file.getFilename());
        out.put("contentType", file.getContentType());
        out.put("size", Integer.valueOf(file.getSize()));
        out.put("sum", Long.valueOf(checksum(file.getBytes())));
        out.put("note", note);
        return out;
    }

    /// A raw binary body: its size and checksum.
    @PostMapping("/raw")
    public Map<String, Object> raw(HttpServer.Request request) {
        byte[] body = request.getBodyBytes();
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("size", Integer.valueOf(body == null ? 0 : body.length));
        out.put("sum", Long.valueOf(body == null ? 0 : checksum(body)));
        return out;
    }

    /// Answers after `ms` milliseconds, for timeout and cancellation tests.
    @GetMapping("/slow")
    public String slow(@RequestParam(value = "ms", defaultValue = "1000") int ms)
            throws InterruptedException {
        // A test fixture's delay, capped; nothing in the server waits on this.
        Thread.sleep(Math.max(0, Math.min(ms, 30000)));
        return "slow " + ms;
    }

    @GetMapping("/json/list")
    public List<Object> jsonList() {
        List<Object> out = new ArrayList<Object>();
        for (int iter = 1 ; iter <= 5 ; iter++) {
            out.add(Long.valueOf(iter));
        }
        out.add("six");
        out.add(Boolean.TRUE);
        return out;
    }

    @GetMapping("/json/map")
    public Map<String, Object> jsonMap() {
        Map<String, Object> inner = new LinkedHashMap<String, Object>();
        inner.put("depth", Long.valueOf(2));
        inner.put("tags", jsonList());
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("title", "probe");
        out.put("count", Long.valueOf(3));
        out.put("ratio", Double.valueOf(0.5));
        out.put("nested", inner);
        out.put("nothing", null);
        return out;
    }

    /// Custom response headers, including an ETag.
    @GetMapping("/headers")
    public HttpServer.Response headers() {
        return HttpServer.Response.text(200, "headers")
                .header("X-Probe", "one")
                .header("ETag", "\"v1\"")
                .header("Cache-Control", "no-store");
    }

    /// A deterministic checksum both ends can compute: a 31-based polynomial.
    static long checksum(byte[] data) {
        long sum = 0;
        for (int iter = 0 ; iter < data.length ; iter++) {
            sum = (sum * 31 + (data[iter] & 0xff)) & 0xffffffffL;
        }
        return sum;
    }

    private static byte[] ascii(String value) {
        byte[] out = new byte[value.length()];
        for (int iter = 0 ; iter < out.length ; iter++) {
            out[iter] = (byte) value.charAt(iter);
        }
        return out;
    }
}
