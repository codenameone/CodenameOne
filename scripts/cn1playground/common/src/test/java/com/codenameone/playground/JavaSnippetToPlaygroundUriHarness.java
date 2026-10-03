/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.playground;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class JavaSnippetToPlaygroundUriHarness {
    private static final String PREFIX = "/playground/?code=";

    private JavaSnippetToPlaygroundUriHarness() {
    }

    /// Validates a snippet with the compiler the Playground uses, against the
    /// Playground's API, and prints its URI unless the snippet is not Java at all: a
    /// syntax error, such as generics mangled by an HTML conversion (`ComboBox> c`),
    /// an escaped `&lt;`, a CSS block in a `java` fence or a `{ ... }` placeholder.
    ///
    /// Semantic errors pass. Reference-documentation snippets are fragments: they use
    /// the surrounding text's variables (`form`, `url`) and classes the Playground does
    /// not import by default, which a reader supplies. Nothing is run -- whether the
    /// code works at run time is the live Playground's business.
    public static void main(String[] args) {
        try {
            String source = loadSource(args);
            if (source == null) {
                emitError("UNEXPECTED_ERROR", "No snippet source provided", 1, 1);
                return;
            }
            try {
                PlaygroundRunner.compile(source);
            } catch (PlaygroundRunner.CompileFailure f) {
                if (!f.wellFormed) {
                    PlaygroundRunner.Diagnostic d = f.diagnostics.isEmpty() ? null : f.diagnostics.get(0);
                    emitError("SYNTAX_ERROR", d == null ? "syntax error" : d.message,
                            d == null ? 1 : d.line, d == null ? 1 : d.column);
                    return;
                }
            }
            System.out.println(PREFIX + encodeLikePlayground(source));
        } catch (Throwable ex) {
            String message = ex.getMessage();
            if (message == null || message.length() == 0) {
                message = ex.getClass().getName();
            }
            emitError("UNEXPECTED_ERROR", message, 1, 1);
        }
    }

    private static String loadSource(String[] args) throws IOException {
        if (args.length == 0) {
            return slurp(System.in);
        }
        if (args.length == 2 && "--file".equals(args[0])) {
            InputStream input = new FileInputStream(args[1]);
            try {
                return slurp(input);
            } finally {
                input.close();
            }
        }
        throw new IOException("Unsupported arguments. Expected --file <path> or stdin.");
    }

    private static String slurp(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        String line;
        boolean first = true;
        while ((line = reader.readLine()) != null) {
            if (!first) {
                out.append('\n');
            }
            out.append(line);
            first = false;
        }
        return out.toString();
    }

    // Mirrors CN1Playground.encodeSharedScript: URL-safe Base64 without padding.
    // Implemented with java.util.Base64 so the headless harness does not require
    // an initialized CN1 Display (CN1's Base64 transitively touches Display.impl).
    private static String encodeLikePlayground(String source) {
        if (source == null || source.isEmpty()) {
            return "";
        }
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(source.getBytes(StandardCharsets.UTF_8));
    }

    private static void emitError(String errorType, String message, int line, int column) {
        System.out.println("{\"ok\":false,\"errorType\":\"" + jsonEscape(errorType)
                + "\",\"message\":\"" + jsonEscape(message)
                + "\",\"line\":" + Math.max(1, line)
                + ",\"column\":" + Math.max(1, column) + "}");
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\b':
                    out.append("\\b");
                    break;
                case '\f':
                    out.append("\\f");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (ch < 32) {
                        out.append("\\u");
                        String hex = Integer.toHexString(ch);
                        for (int j = hex.length(); j < 4; j++) {
                            out.append('0');
                        }
                        out.append(hex);
                    } else {
                        out.append(ch);
                    }
            }
        }
        return out.toString();
    }
}
