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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Connects an MCP host that only speaks stdio -- Codex, older desktop hosts -- to
 * a backend's HTTP endpoint. Each line read from stdin is one JSON-RPC message,
 * POSTed as is; each answer is written to stdout as one line.
 *
 * <pre>
 *   java -cp codenameone-backend.jar com.codename1.backend.mcp.StdioBridge \
 *        http://127.0.0.1:8080/mcp [token]
 * </pre>
 *
 * <p>A host that speaks HTTP -- Claude Code among them -- needs none of this and
 * should be pointed at the URL directly.
 */
public final class StdioBridge {
    private StdioBridge() {
    }

    public static void main(String[] args) throws Exception {
        if(args.length < 1) {
            System.err.println("usage: StdioBridge <mcp-url> [bearer-token]");
            System.exit(2);
        }
        URL url = new URL(args[0]);
        String token = args.length > 1 ? args[1] : System.getenv("CN1_MCP_TOKEN");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in,
                StandardCharsets.UTF_8));
        OutputStream out = System.out;
        String line;
        while((line = in.readLine()) != null) {
            if(line.trim().length() == 0) {
                continue;
            }
            String answer = post(url, token, line);
            if(answer != null && answer.trim().length() > 0) {
                out.write((answer.trim() + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        }
    }

    private static String post(URL url, String token, String body) {
        try {
            HttpURLConnection c = (HttpURLConnection)url.openConnection();
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("Accept", "application/json, text/event-stream");
            if(token != null && token.length() > 0) {
                c.setRequestProperty("Authorization", "Bearer " + token);
            }
            c.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
            int status = c.getResponseCode();
            java.io.InputStream stream = status >= 400 ? c.getErrorStream() : c.getInputStream();
            if(stream == null) {
                return null;
            }
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int n;
            while((n = stream.read(chunk)) > 0) {
                buffer.write(chunk, 0, n);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception err) {
            // The backend is not up: answer the host rather than hang it, under
            // the id it asked with -- and not at all for a notification.
            java.util.regex.Matcher id = java.util.regex.Pattern
                    .compile("\"id\"\\s*:\\s*(\"[^\"]*\"|-?[0-9]+)").matcher(body);
            if(!id.find()) {
                return null;
            }
            return "{\"jsonrpc\":\"2.0\",\"id\":" + id.group(1) + ",\"error\":{\"code\":-32000,"
                    + "\"message\":\"backend unreachable: "
                    + String.valueOf(err.getMessage()).replace('"', '\'') + "\"}}";
        }
    }
}
