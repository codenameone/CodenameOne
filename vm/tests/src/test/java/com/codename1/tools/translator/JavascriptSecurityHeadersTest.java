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
package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// The generated Content-Security-Policy, checked against the page it is generated for.
///
/// A CSP is a policy that fails by breaking the application rather than by reporting anything, so
/// the failure mode to guard is "the hash no longer matches the page". These tests pin the
/// relationship: every inline block in the shipped `index.html` template has a hash in the policy,
/// and the hash is the one a browser computes.
class JavascriptSecurityHeadersTest {

    /// One directive of a policy, so an assertion about `worker-src` is not accidentally
    /// satisfied by something `img-src` allows.
    private static String clause(String csp, String directive) {
        String[] parts = csp.split(";");
        for (int iter = 0; iter < parts.length; iter++) {
            String part = parts[iter].trim();
            if (part.startsWith(directive + " ") || part.equals(directive)) {
                return part;
            }
        }
        return null;
    }

    private static String indexTemplate() throws Exception {
        Path template = Paths.get("..", "ByteCodeTranslator", "src", "javascript", "index.html")
                .toAbsolutePath().normalize();
        assertTrue(Files.exists(template), "index.html template not found at " + template);
        return new String(Files.readAllBytes(template), StandardCharsets.UTF_8);
    }

    @Test
    void everyInlineBlockInTheShippedPageIsHashed() throws Exception {
        String html = indexTemplate();
        String csp = JavascriptSecurityHeaders.contentSecurityPolicy(html);

        List<String> scripts = JavascriptSecurityHeaders.hashesOf(html, "script");
        List<String> styles = JavascriptSecurityHeaders.hashesOf(html, "style");
        // The page really does have inline blocks -- if it stopped having them this test would
        // otherwise pass over an empty list and prove nothing.
        assertFalse(scripts.isEmpty() && styles.isEmpty(),
                "the shipped page has no inline script or style, so this test checks nothing");

        for (String hash : scripts) {
            assertTrue(csp.indexOf(hash) >= 0, "script hash missing from the policy: " + hash);
        }
        for (String hash : styles) {
            assertTrue(csp.indexOf(hash) >= 0, "style hash missing from the policy: " + hash);
        }
    }

    @Test
    void theHashIsTheOneABrowserComputes() throws Exception {
        // Pinned against an independently computable value: SHA-256 of the exact bytes between
        // the tags, base64. A browser does not trim, does not normalise line endings and does not
        // skip comments, and neither does this.
        String html = "<html><head><script>alert(1)</script></head></html>";
        List<String> hashes = JavascriptSecurityHeaders.hashesOf(html, "script");
        assertEquals(1, hashes.size());
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        String expected = "sha256-" + java.util.Base64.getEncoder().encodeToString(
                digest.digest("alert(1)".getBytes(StandardCharsets.UTF_8)));
        assertEquals(expected, hashes.get(0));
    }

    @Test
    void anExternalScriptIsNotHashed() {
        // An element with src has no inline body. Hashing its empty content would put the hash of
        // the empty string into the policy, which matches any empty inline script an attacker
        // injects -- a source expression that grants something rather than restricting it.
        String html = "<script src=\"port.js\"></script><script>x=1</script>";
        List<String> hashes = JavascriptSecurityHeaders.hashesOf(html, "script");
        assertEquals(1, hashes.size());
        assertEquals(JavascriptSecurityHeaders.sha256("x=1"), hashes.get(0));
    }

    @Test
    void theEmptyStringIsNeverHashed() {
        assertTrue(JavascriptSecurityHeaders.hashesOf("<script></script>", "script").isEmpty());
    }

    @Test
    void thePolicyCoversOnlyTheDocumentItsHashesCameFrom() throws Exception {
        java.io.File out = java.nio.file.Files.createTempDirectory("cn1-csp").toFile();
        java.nio.file.Files.write(new java.io.File(out, "index.html").toPath(),
                indexTemplate().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        JavascriptSecurityHeaders.write(out);

        java.io.File guidance = new java.io.File(out, "cn1-security");
        String netlify = new String(java.nio.file.Files.readAllBytes(
                new java.io.File(guidance, "_headers").toPath()),
                java.nio.charset.StandardCharsets.UTF_8);

        // The hashes are computed from index.html and from nothing else, so a site-wide policy
        // governs documents whose inline code was never hashed -- a BrowserComponent page on the
        // same origin -- and blocks script the build itself emitted.
        int siteWide = netlify.indexOf("/*");
        int policy = netlify.indexOf("Content-Security-Policy");
        assertTrue(siteWide >= 0, "the transport headers are still site wide");
        assertTrue(policy > siteWide,
                "the policy must come after the site-wide block, not inside it: " + netlify);
        assertTrue(netlify.indexOf("/index.html") >= 0,
                "the policy must be scoped to the document it was computed from: " + netlify);

        // And the headers that describe the ORIGIN rather than a document stay everywhere.
        String siteWideBlock = netlify.substring(siteWide, policy);
        assertTrue(siteWideBlock.indexOf("Strict-Transport-Security") >= 0, siteWideBlock);
        assertTrue(siteWideBlock.indexOf("X-Frame-Options") >= 0, siteWideBlock);
        assertTrue(siteWideBlock.indexOf("Content-Security-Policy") < 0,
                "the policy must not be in the site-wide block: " + siteWideBlock);
    }

    @Test
    void thePolicyDeniesTheThingsThatMatter() throws Exception {
        String csp = JavascriptSecurityHeaders.contentSecurityPolicy(indexTemplate());
        assertTrue(csp.indexOf("object-src 'none'") >= 0);
        assertTrue(csp.indexOf("base-uri 'none'") >= 0);
        assertTrue(csp.indexOf("frame-ancestors 'none'") >= 0);
        assertTrue(csp.indexOf("form-action 'none'") >= 0);
        assertTrue(csp.indexOf("connect-src 'self'") >= 0);
        // Workers must come from the origin, and specifically not from blob: -- a blob worker is
        // the easiest way for injected script to run code the script-src hashes would otherwise
        // have stopped. Checked on the worker-src clause itself rather than on the whole policy,
        // which legitimately allows blob: for images and media.
        assertTrue(csp.indexOf("worker-src 'self'") >= 0);
        assertEquals("worker-src 'self'", clause(csp, "worker-src"));
        assertEquals("child-src 'self'", clause(csp, "child-src"));
    }

    @Test
    void theWasmConcessionIsNarrowAndEvalIsNotGranted() throws Exception {
        String csp = JavascriptSecurityHeaders.contentSecurityPolicy(indexTemplate());
        // The port's SQLite is WebAssembly, so this one is unavoidable.
        assertTrue(csp.indexOf("'wasm-unsafe-eval'") >= 0);
        // And this one is not: Display.execute("javascript:") is an opt-in that most applications
        // do not use, and granting the origin eval by default to serve them would be the largest
        // concession the policy could make. The quote in the needle is what keeps this from
        // matching the 'wasm-unsafe-eval' above.
        assertTrue(csp.indexOf("'unsafe-eval'") < 0,
                "the policy must not grant unsafe-eval: " + csp);
        assertTrue(csp.indexOf("'unsafe-inline'") < 0,
                "inline script is covered by hashes, never by unsafe-inline: " + csp);
    }
}
