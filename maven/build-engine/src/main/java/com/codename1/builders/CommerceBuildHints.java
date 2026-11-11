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
package com.codename1.builders;

/**
 * The Codename One Commerce build hints, validated and rendered as the lines
 * of generated startup code that hand them to the client.
 *
 * <p>{@code CommerceManager} reads its configuration with
 * {@code Display.getProperty}, and a build hint only becomes a display
 * property when a builder writes a {@code setProperty} call into the
 * generated stub. Until this class existed nothing did, so
 * {@code commerce.cloud.enabled=false} and a self-hosted
 * {@code commerce.cloud.endpoint} were accepted by every build and read by
 * none.</p>
 *
 * <p>The values are spliced into a Java string literal, so each one is held
 * to the alphabet its meaning allows rather than escaped: an endpoint with a
 * quote in it is not a url the client could have used anyway, and refusing
 * the build names the hint, where a compile error in generated code would
 * not.</p>
 *
 * <p>The two build pipelines are separate codebases that must generate the
 * same stub, so <b>keep this file in sync with
 * {@code com.codename1.build.daemon.CommerceBuildHints} in the BuildDaemon
 * repository</b>. Its unit tests live in both repos for the same reason.</p>
 */
class CommerceBuildHints {

    static final String ENABLED = "commerce.cloud.enabled";
    static final String ENDPOINT = "commerce.cloud.endpoint";
    static final String PUBLIC_KEY = "commerce.cloud.publicKey";

    private CommerceBuildHints() {
    }

    /**
     * The generated startup lines for every commerce hint the build sets.
     *
     * <p>An absent hint emits nothing, which leaves the client on its own
     * default. That matters for the endpoint and the key: stamping the
     * default here would freeze it into the binary of an application that
     * never asked to be pinned to it.</p>
     *
     * @param request the build request
     * @return zero or more {@code setProperty} lines, each newline terminated
     * @throws IllegalArgumentException when a hint cannot be used as written
     */
    static String stubProperties(BuildRequest request) {
        StringBuilder out = new StringBuilder();
        String enabled = trimmed(request, ENABLED);
        if (enabled.length() > 0) {
            if (!"true".equalsIgnoreCase(enabled) && !"false".equalsIgnoreCase(enabled)) {
                throw new IllegalArgumentException(ENABLED + "=\"" + enabled
                        + "\" must be true or false.");
            }
            line(out, ENABLED, "true".equalsIgnoreCase(enabled) ? "true" : "false");
        }
        String endpoint = trimmed(request, ENDPOINT);
        if (endpoint.length() > 0) {
            if (!isUsableEndpoint(endpoint)) {
                throw new IllegalArgumentException(ENDPOINT + "=\"" + endpoint
                        + "\" is not a usable endpoint. It must be an http or https url with no "
                        + "spaces, quotes or backslashes, for example "
                        + "https://commerce.example.com/api/v2/commerce");
            }
            // The client appends "/validate", "/customer" and so on, so a
            // trailing slash would produce "//customer", which some proxies
            // route differently from the path the service maps.
            while (endpoint.endsWith("/")) {
                endpoint = endpoint.substring(0, endpoint.length() - 1);
            }
            line(out, ENDPOINT, endpoint);
        }
        String key = trimmed(request, PUBLIC_KEY);
        if (key.length() > 0) {
            if (!isBase64(key)) {
                throw new IllegalArgumentException(PUBLIC_KEY + " must be the base64 encoding "
                        + "of the service's 32 byte Ed25519 public key.");
            }
            line(out, PUBLIC_KEY, key);
        }
        return out.toString();
    }

    private static String trimmed(BuildRequest request, String name) {
        String value = request.getArg(name, "");
        return value == null ? "" : value.trim();
    }

    private static void line(StringBuilder out, String name, String value) {
        out.append("        Display.getInstance().setProperty(\"").append(name)
                .append("\", \"").append(value).append("\");\n");
    }

    /**
     * Whether the value is an http(s) url that is safe inside a string literal.
     *
     * @param value the trimmed hint, never empty
     * @return true when the client can use it as written
     */
    static boolean isUsableEndpoint(String value) {
        // regionMatches, not toLowerCase: a scheme is ASCII by specification
        // and a locale-sensitive fold breaks "HTTPS" on a Turkish build host.
        boolean http = value.regionMatches(true, 0, "http://", 0, 7) && value.length() > 7;
        boolean https = value.regionMatches(true, 0, "https://", 0, 8) && value.length() > 8;
        if (!http && !https) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c <= ' ' || c >= 127 || c == '"' || c == '\\' || c == '\'') {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether the value is plain or url-safe base64 text.
     *
     * <p>The length is not checked against 32 bytes here: the client refuses
     * a key of the wrong size by failing every signature, and a self-hosting
     * developer who pasted the wrong value learns that from the first
     * refresh rather than from a builder that has to track key formats.</p>
     *
     * @param value the trimmed hint, never empty
     * @return true when every character belongs to a base64 alphabet
     */
    static boolean isBase64(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '+' || c == '/' || c == '=' || c == '-' || c == '_';
            if (!ok) {
                return false;
            }
        }
        return true;
    }
}
