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
package com.codename1.backend.mcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/// Connects an MCP host that only speaks stdio -- Codex, older desktop hosts -- to
/// a backend's HTTP endpoint. Each line read from stdin is one JSON-RPC message,
/// POSTed as is; each answer is written to stdout as one line.
///
/// ```java
///   java -cp codenameone-backend.jar com.codename1.backend.mcp.StdioBridge \
///        http://127.0.0.1:8080/mcp [token]
/// ```
///
/// A host that speaks HTTP -- Claude Code among them -- needs none of this and
/// should be pointed at the URL directly.
public final class StdioBridge {
    /// How long to wait for the backend to accept the connection.
    static final int CONNECT_TIMEOUT_MILLIS = 5000;

    /// How long to wait for an answer, in milliseconds: `CN1_MCP_TIMEOUT_MS`,
    /// two minutes by default. Messages are forwarded one at a time, so without
    /// a bound a backend that accepted a request and then stalled would hold
    /// every later message too, and the host would wait forever instead of
    /// being told the backend is unreachable.
    static int readTimeoutMillis() {
        String configured = System.getenv("CN1_MCP_TIMEOUT_MS");
        int fallback = 120000;
        if (configured == null) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(configured.trim());
            return value > 0 ? value : fallback;
        } catch (NumberFormatException notANumber) {
            return fallback;
        }
    }

    private StdioBridge() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: StdioBridge <mcp-url> [bearer-token]");
            System.exit(2);
        }
        URL url = new URL(args[0]);
        String token = args.length > 1 ? args[1] : System.getenv("CN1_MCP_TOKEN");
        // The process's own streams: they live as long as it does, and closing
        // them would take stdin and stdout from anything else in the JVM.
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, //NOPMD CloseResource - the process stdin
                StandardCharsets.UTF_8));
        OutputStream out = System.out; //NOPMD CloseResource - the process stdout
        String line;
        while ((line = in.readLine()) != null) {
            if (line.trim().length() == 0) {
                continue;
            }
            String answer = post(url, token, line);
            if (answer != null && answer.trim().length() > 0) {
                out.write((answer.trim() + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        }
    }

    /// Forwards one line; package-private for the test of the unreachable path.
    static String post(URL url, String token, String body) {
        return post(url, token, body, readTimeoutMillis());
    }

    /// [#post(URL,String,String)] with an explicit bound on the answer's wait.
    static String post(URL url, String token, String body, int readTimeout) {
        try {
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            // A timeout is an IOException, so it reaches unreachable() below and
            // the host gets an error under its request's id.
            c.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            c.setReadTimeout(readTimeout);
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("Accept", "application/json, text/event-stream");
            if (token != null && token.length() > 0) {
                c.setRequestProperty("Authorization", "Bearer " + token);
            }
            c.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
            int status = c.getResponseCode();
            java.io.InputStream stream = status >= 400 ? c.getErrorStream() : c.getInputStream();
            if (stream == null) {
                return null;
            }
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int n;
            while ((n = stream.read(chunk)) > 0) {
                buffer.write(chunk, 0, n);
            }
            String answer = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
            if (status < 200 || status >= 300) {
                return refused(body, status, answer);
            }
            return answer;
        } catch (IOException err) {
            return unreachable(body, err);
        } catch (RuntimeException err) {
            return unreachable(body, err);
        }
    }

    /// The answer the host gets when the backend is not up: an error under the id
    /// it asked with, or nothing for a notification.
    static String unreachable(String body, Exception err) {
        return errorUnderRequestId(body, "backend unreachable: " + err.getMessage());
    }

    /// The answer the host gets when the backend refused the HTTP request -- a 401
    /// for a missing or wrong token, most often. That refusal comes before the
    /// server reads the JSON-RPC body, so its error carries a null id, and a host
    /// waiting on its own id would never see the request complete. So it is
    /// answered again under the id the host asked with; a notification gets
    /// nothing, and a body that is no single request passes the server's through.
    static String refused(String body, int status, String answer) {
        Object request = parse(body);
        if (!(request instanceof java.util.Map)) {
            return answer;
        }
        String detail = answer == null ? "" : answer.trim();
        Object parsed = parse(detail);
        if (parsed instanceof java.util.Map
                && ((java.util.Map) parsed).get("error") instanceof java.util.Map) {
            Object message = ((java.util.Map) ((java.util.Map) parsed).get("error")).get("message");
            detail = message == null ? "" : String.valueOf(message);
        }
        if (detail.length() > 200) {
            detail = detail.substring(0, 200);
        }
        return errorUnderRequestId(body, "backend answered HTTP " + status
                + (detail.length() > 0 ? ": " + detail : ""));
    }

    /// An error under the TOP-LEVEL id of `body`, or null when it has none --
    /// a notification, which is never answered. Parsed, not searched: a pattern
    /// took the first "id" anywhere, so a tool argument named id got the answer
    /// and the host waited forever.
    static String errorUnderRequestId(String body, String message) {
        Object request = parse(body);
        if (!(request instanceof java.util.Map)
                || !((java.util.Map) request).containsKey("id")) {
            return null;
        }
        java.util.Map error = new java.util.LinkedHashMap();
        error.put("code", Integer.valueOf(-32000));
        error.put("message", message);
        java.util.Map response = new java.util.LinkedHashMap();
        response.put("jsonrpc", "2.0");
        response.put("id", ((java.util.Map) request).get("id"));
        response.put("error", error);
        return com.codename1.backend.Json.write(response);
    }

    private static Object parse(String text) {
        try {
            return com.codename1.backend.Json.parse(text);
        } catch (IOException parseErr) {
            return null;
        } catch (RuntimeException parseErr) {
            return null;
        }
    }
}
