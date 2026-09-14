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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates the response headers a Codename One JavaScript application should be served with,
 * computed from the bundle that was actually emitted rather than copied from a template.
 *
 * <p>The Content-Security-Policy is the part that has to be generated. Every inline
 * {@code <script>} and {@code <style>} in the page needs its own {@code 'sha256-...'} source
 * expression, and those change whenever the page template does -- a hand-written policy goes
 * stale silently, and a stale CSP does not fail loudly: the page simply stops working in a way
 * that looks like a browser bug. Hashing what was written closes that.</p>
 *
 * <h2>What cannot be done from here</h2>
 *
 * <p>Three of the protections below <b>only</b> exist as response headers and cannot be expressed
 * in a {@code <meta>} tag: {@code frame-ancestors}, {@code Strict-Transport-Security} and
 * {@code X-Frame-Options}. A build that emitted a meta tag and called the job done would be
 * shipping a policy with a hole in exactly the place clickjacking lives. So this writes
 * configuration files for the host instead, and the generated README says which host has to serve
 * them.</p>
 *
 * <h2>Why the policy is not stricter</h2>
 *
 * <p>{@code 'wasm-unsafe-eval'} is required: the port's SQLite is a WebAssembly module, and
 * instantiating one is a CSP-relevant operation. It is much narrower than {@code 'unsafe-eval'}
 * -- it permits WebAssembly compilation and nothing else, no {@code eval} and no
 * {@code new Function}.</p>
 *
 * <p>{@code 'unsafe-eval'} is <b>not</b> included, which means
 * {@code Display.execute("javascript:...")} does not work under this policy. That is deliberate:
 * the feature is an application opt-in, most applications do not use it, and granting the whole
 * origin {@code eval} to support the ones that do is the single largest concession a CSP can
 * make. The generated README explains how to add it for an application that needs it, and what it
 * costs.</p>
 */
final class JavascriptSecurityHeaders {

    /// Where everything except `_headers` goes. Named so it is obvious in a file listing that it
    /// is for the person deploying the application and not for the application.
    static final String DEPLOYMENT_DIRECTORY = "cn1-security";

    private JavascriptSecurityHeaders() {
    }

    /**
     * Writes the deployment configuration next to the generated application.
     *
     * @param outputDirectory the directory the bundle was written to
     * @throws IOException if the files cannot be written
     */
    static void write(File outputDirectory) throws IOException {
        File index = new File(outputDirectory, "index.html");
        String html = index.isFile()
                ? new String(Files.readAllBytes(index.toPath()), StandardCharsets.UTF_8) : "";
        File bridge = new File(outputDirectory, "browser_bridge.js");
        String bridgeSource = bridge.isFile()
                ? new String(Files.readAllBytes(bridge.toPath()), StandardCharsets.UTF_8) : "";
        String csp = contentSecurityPolicy(html, bridgeSource);

        // Everything goes in a subdirectory, including `_headers`, and that last part is the
        // important one.
        //
        // `_headers` at the publish root is the name Netlify and Cloudflare Pages read, and they
        // read it *automatically*. Writing it there would mean an application that had never
        // asked for a Content-Security-Policy acquired one on its next deploy -- and this policy
        // sets `connect-src 'self'`, which blocks every ConnectionRequest, fetch and WebSocket
        // aimed at a backend on another origin. Most applications have one. Rebuilding would have
        // taken their networking away with no diagnostic beyond a console error in a browser
        // nobody was watching.
        //
        // So the build generates the policy and the developer activates it, by copying one file.
        // The README beside it says which, and says what to edit first.
        File guidance = new File(outputDirectory, DEPLOYMENT_DIRECTORY);
        if (!guidance.isDirectory() && !guidance.mkdirs()) {
            throw new IOException("could not create " + guidance);
        }
        Files.write(new File(guidance, "_headers").toPath(),
                netlifyHeaders(csp).getBytes(StandardCharsets.UTF_8));
        Files.write(new File(guidance, "cn1-security.nginx.conf").toPath(),
                nginxHeaders(csp).getBytes(StandardCharsets.UTF_8));
        Files.write(new File(guidance, "cn1-security.htaccess").toPath(),
                apacheHeaders(csp).getBytes(StandardCharsets.UTF_8));
        Files.write(new File(guidance, "README.md").toPath(),
                readme(csp).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Builds the policy, hashing every inline block the page actually contains.
     *
     * @param html the generated index.html
     * @return the policy value, without the header name
     */
    static String contentSecurityPolicy(String html, String bridgeSource) {
        StringBuilder scriptSources = new StringBuilder("'self' 'wasm-unsafe-eval'");
        for (String hash : hashesOf(html, "script")) {
            scriptSources.append(" '").append(hash).append('\'');
        }
        StringBuilder styleSources = new StringBuilder("'self'");
        for (String hash : hashesOf(html, "style")) {
            styleSources.append(" '").append(hash).append('\'');
        }
        // The page is not the only thing this policy governs. The bridge builds a srcdoc
        // document to print an image through, and a srcdoc document inherits the embedder's
        // CSP -- so the <style> inside it is checked against style-src just like one in
        // index.html, and without its hash the printout loses its layout. Hashing the literal
        // out of the bridge as emitted keeps the two from drifting: there is one copy of the
        // text and the policy is derived from it.
        for (String css : runtimeStyles(bridgeSource)) {
            styleSources.append(" '").append(sha256(css)).append('\'');
        }
        return "default-src 'self'; "
                + "script-src " + scriptSources + "; "
                + "style-src " + styleSources + "; "
                // Workers are same-origin files; blob: is not permitted, because a blob worker is
                // how injected script most easily escapes a script-src that would otherwise hold.
                + "worker-src 'self'; "
                + "child-src 'self'; "
                // data: for the canvas and for generated images; the port creates blob URLs for
                // media it decodes itself.
                + "img-src 'self' data: blob:; "
                + "media-src 'self' data: blob:; "
                + "font-src 'self' data:; "
                + "connect-src 'self'; "
                + "object-src 'none'; "
                + "base-uri 'none'; "
                + "form-action 'none'; "
                + "frame-ancestors 'none'; "
                + "upgrade-insecure-requests";
    }

    /// The exact text of every `<style>` body the bridge injects at runtime.
    ///
    /// Each is marked in `browser_bridge.js` with a `cn1-csp-style` block comment followed by a
    /// single-quoted literal on the same line. An escape inside that literal would mean the text
    /// the browser sees differs from the bytes read here, and a hash computed from the wrong text
    /// is worse than no hash -- it looks like the case is covered. So this refuses rather than
    /// guesses.
    static List<String> runtimeStyles(String bridgeSource) {
        List<String> out = new ArrayList<String>();
        String marker = "/* cn1-csp-style */";
        int at = 0;
        while (true) {
            int found = bridgeSource.indexOf(marker, at);
            if (found < 0) {
                return out;
            }
            int open = bridgeSource.indexOf('\'', found + marker.length());
            int lineEnd = bridgeSource.indexOf('\n', found);
            if (open < 0 || (lineEnd >= 0 && open > lineEnd)) {
                throw new IllegalStateException(
                        "a cn1-csp-style marker in browser_bridge.js is not followed by a "
                        + "single-quoted literal on the same line");
            }
            int close = bridgeSource.indexOf('\'', open + 1);
            if (close < 0 || (lineEnd >= 0 && close > lineEnd)) {
                throw new IllegalStateException(
                        "an unterminated cn1-csp-style literal in browser_bridge.js");
            }
            String css = bridgeSource.substring(open + 1, close);
            if (css.indexOf('\\') >= 0) {
                throw new IllegalStateException(
                        "a cn1-csp-style literal in browser_bridge.js contains an escape, so the "
                        + "text the browser sees is not the text being hashed: " + css);
            }
            out.add(css);
            at = close + 1;
        }
    }

    /**
     * The {@code 'sha256-...'} source expressions for every inline block of one element type.
     *
     * <p>An element with a {@code src} attribute is skipped: it has no inline body to hash, and
     * hashing its empty content would add a source expression that matches any empty script --
     * which is a source expression an attacker can satisfy.</p>
     */
    static List<String> hashesOf(String html, String tag) {
        List<String> out = new ArrayList<String>();
        String open = "<" + tag;
        String close = "</" + tag + ">";
        int at = 0;
        while (true) {
            int start = indexOfIgnoreCase(html, open, at);
            if (start < 0) {
                return out;
            }
            int openEnd = html.indexOf('>', start);
            if (openEnd < 0) {
                return out;
            }
            String attributes = html.substring(start, openEnd);
            int bodyEnd = indexOfIgnoreCase(html, close, openEnd);
            if (bodyEnd < 0) {
                return out;
            }
            at = bodyEnd + close.length();
            if (indexOfIgnoreCase(attributes, " src=", 0) >= 0
                    || indexOfIgnoreCase(attributes, " href=", 0) >= 0) {
                continue;
            }
            String body = html.substring(openEnd + 1, bodyEnd);
            if (body.length() == 0) {
                continue;
            }
            out.add(sha256(body));
        }
    }

    private static int indexOfIgnoreCase(String haystack, String needle, int from) {
        int limit = haystack.length() - needle.length();
        for (int iter = Math.max(0, from); iter <= limit; iter++) {
            if (haystack.regionMatches(true, iter, needle, 0, needle.length())) {
                return iter;
            }
        }
        return -1;
    }

    /**
     * The CSP hash of one inline body: SHA-256 of the exact bytes between the tags, base64.
     *
     * <p>"Exact" is the whole difficulty. The browser hashes the element's text content byte for
     * byte, so a build step that reformats the page, changes its line endings or strips a comment
     * invalidates the hash -- which is why this runs over the file as written rather than over the
     * template it came from.</p>
     */
    static String sha256(String body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(body.getBytes(StandardCharsets.UTF_8));
            return "sha256-" + base64(hash);
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static String base64(byte[] data) {
        final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        StringBuilder out = new StringBuilder(((data.length + 2) / 3) * 4);
        for (int iter = 0; iter < data.length; iter += 3) {
            int remaining = data.length - iter;
            int block = (data[iter] & 0xff) << 16;
            if (remaining > 1) {
                block |= (data[iter + 1] & 0xff) << 8;
            }
            if (remaining > 2) {
                block |= data[iter + 2] & 0xff;
            }
            out.append(alphabet.charAt((block >> 18) & 0x3f));
            out.append(alphabet.charAt((block >> 12) & 0x3f));
            out.append(remaining > 1 ? alphabet.charAt((block >> 6) & 0x3f) : '=');
            out.append(remaining > 2 ? alphabet.charAt(block & 0x3f) : '=');
        }
        return out.toString();
    }

    /// The headers that describe the ORIGIN rather than a document, so every path gets them.
    ///
    /// The Content-Security-Policy is deliberately not among them. Its hashes are computed from
    /// `index.html` and from nothing else, so applying it site-wide governed documents whose
    /// inline code was never hashed -- a BrowserComponent page served from the same origin, for
    /// instance -- and blocked script the build itself emitted. The policy is attached to the
    /// document it was computed from; these are not document specific and stay everywhere.
    private static String commonHeaders() {
        return "Strict-Transport-Security: max-age=63072000; includeSubDomains\n"
                + "X-Content-Type-Options: nosniff\n"
                + "X-Frame-Options: DENY\n"
                + "Referrer-Policy: no-referrer\n"
                + "Permissions-Policy: geolocation=(self), camera=(self), microphone=(self), "
                + "payment=(), usb=(), interest-cohort=()\n"
                + "Cross-Origin-Resource-Policy: same-origin\n";
    }

    private static String netlifyHeaders(String csp) {
        StringBuilder b = new StringBuilder();
        b.append("# Codename One JavaScript port -- security headers.\n");
        b.append("# Netlify and Cloudflare Pages read this file verbatim; see "
                + "cn1-security/README.md.\n");
        b.append("/*\n");
        String[] lines = commonHeaders().split("\n");
        for (int iter = 0; iter < lines.length; iter++) {
            b.append("  ").append(lines[iter]).append('\n');
        }
        b.append("\n");
        b.append("# The policy is scoped to the document its hashes came from. Another HTML\n");
        b.append("# document on this origin -- a BrowserComponent page, say -- carries inline\n");
        b.append("# code this build never hashed, and a site-wide policy would block it.\n");
        b.append("/\n");
        b.append("  Content-Security-Policy: ").append(csp).append('\n');
        b.append("/index.html\n");
        b.append("  Content-Security-Policy: ").append(csp).append('\n');
        b.append("\n");
        b.append("# The service worker must never be served from a stale cache: a pinned old\n");
        b.append("# worker keeps serving old application code, and old application code is how a\n");
        b.append("# storage format that has been migrated away from gets written again.\n");
        b.append("/sw.js\n");
        b.append("  Cache-Control: no-cache\n");
        return b.toString();
    }

    private static String nginxHeaders(String csp) {
        return "# Codename One JavaScript port -- security headers for nginx.\n"
                + "# Include inside the server{} or location{} block that serves the app.\n"
                + "# 'always' matters: without it nginx omits the header on error responses,\n"
                + "# and an error page in the app's origin is a page in the app's origin.\n"
                + "# The policy is scoped to the document its hashes came from; another HTML\n"
                + "# document on this origin carries inline code this build never hashed.\n"
                + "#\n"
                + "# The common headers are REPEATED inside each location, and that is not\n"
                + "# redundancy. nginx inherits add_header from an outer level only when the\n"
                + "# current level defines none of its own, so a location that adds the policy\n"
                + "# silently drops HSTS, nosniff and the rest -- and it would be the main\n"
                + "# application document that lost them.\n"
                + "location = /index.html {\n"
                + "    add_header Content-Security-Policy \"" + csp + "\" always;\n"
                + nginxCommon("    ")
                + "}\n"
                + "location = / {\n"
                + "    add_header Content-Security-Policy \"" + csp + "\" always;\n"
                + nginxCommon("    ")
                + "}\n"
                + "add_header Strict-Transport-Security \"max-age=63072000; includeSubDomains\" always;\n"
                + "add_header X-Content-Type-Options \"nosniff\" always;\n"
                + "add_header X-Frame-Options \"DENY\" always;\n"
                + "add_header Referrer-Policy \"no-referrer\" always;\n"
                + "add_header Permissions-Policy \"geolocation=(self), camera=(self), "
                + "microphone=(self), payment=(), usb=(), interest-cohort=()\" always;\n"
                + "add_header Cross-Origin-Resource-Policy \"same-origin\" always;\n"
                + "\n"
                + "location = /sw.js {\n"
                + "    add_header Cache-Control \"no-cache\" always;\n"
                + nginxCommon("    ")
                + "}\n";
    }

    /// The origin-wide headers, for a level that has to restate them.
    ///
    /// nginx's add_header inheritance is all-or-nothing per level: define one here and every one
    /// from the enclosing level is dropped. A location that sets only the policy, or only a
    /// cache directive, therefore serves a document with none of the transport headers.
    private static String nginxCommon(String indent) {
        return indent + "add_header Strict-Transport-Security \"max-age=63072000; includeSubDomains\" always;\n"
                + indent + "add_header X-Content-Type-Options \"nosniff\" always;\n"
                + indent + "add_header X-Frame-Options \"DENY\" always;\n"
                + indent + "add_header Referrer-Policy \"no-referrer\" always;\n"
                + indent + "add_header Permissions-Policy \"geolocation=(self), camera=(self), "
                + "microphone=(self), payment=(), usb=(), interest-cohort=()\" always;\n"
                + indent + "add_header Cross-Origin-Resource-Policy \"same-origin\" always;\n";
    }

    private static String apacheHeaders(String csp) {
        return "# Codename One JavaScript port -- security headers for Apache.\n"
                + "# Rename to .htaccess, or paste into the matching <Directory> block.\n"
                + "<IfModule mod_headers.c>\n"
                + "  # The policy is scoped to the document its hashes came from; another HTML\n"
                + "  # document on this origin carries inline code this build never hashed.\n"
                + "  <Files \"index.html\">\n"
                + "    Header always set Content-Security-Policy \"" + csp + "\"\n"
                + "  </Files>\n"
                + "  Header always set Strict-Transport-Security \"max-age=63072000; includeSubDomains\"\n"
                + "  Header always set X-Content-Type-Options \"nosniff\"\n"
                + "  Header always set X-Frame-Options \"DENY\"\n"
                + "  Header always set Referrer-Policy \"no-referrer\"\n"
                + "  Header always set Permissions-Policy \"geolocation=(self), camera=(self), "
                + "microphone=(self), payment=(), usb=(), interest-cohort=()\"\n"
                + "  Header always set Cross-Origin-Resource-Policy \"same-origin\"\n"
                + "  <Files \"sw.js\">\n"
                + "    Header always set Cache-Control \"no-cache\"\n"
                + "  </Files>\n"
                + "</IfModule>\n";
    }

    private static String readme(String csp) {
        return "# Deploying this application securely\n"
                + "\n"
                + "## Nothing here is active until you activate it\n"
                + "\n"
                + "The build generates this policy; it does not apply it. Copy the file your\n"
                + "host reads to where it reads it:\n"
                + "\n"
                + "- `_headers` -- Netlify, Cloudflare Pages. Copy it to the **publish root**,\n"
                + "  beside index.html. Those platforms read it automatically, which is exactly\n"
                + "  why the build does not put it there for you.\n"
                + "- `cn1-security.nginx.conf` -- nginx, included from the serving block\n"
                + "- `cn1-security.htaccess` -- Apache, renamed to `.htaccess`\n"
                + "\n"
                + "**Do not upload this `cn1-security/` directory itself.** It is for you; the\n"
                + "application reads none of it, and a web root is not the place for a host\'s\n"
                + "configuration.\n"
                + "\n"
                + "## Read this before you activate it: connect-src\n"
                + "\n"
                + "The policy sets `connect-src \'self\'`, which permits network calls back to\n"
                + "this origin and **blocks every other one** -- every `ConnectionRequest`,\n"
                + "`fetch` and WebSocket aimed at an API on a different host. If your application\n"
                + "talks to a backend anywhere else, and most do, add its origin before you\n"
                + "deploy:\n"
                + "\n"
                + "```\n"
                + "connect-src \'self\' https://api.example.com wss://api.example.com;\n"
                + "```\n"
                + "\n"
                + "The build cannot fill that in, because nothing in the application declares\n"
                + "which origins it talks to. A policy that guessed would be either wrong or\n"
                + "meaningless, so it states the restrictive default and tells you to widen it.\n"
                + "\n"
                + "The same applies to `frame-src` for an embedded `BrowserComponent` pointing at\n"
                + "another site, and to `img-src` / `media-src` for assets loaded from a CDN.\n"
                + "\n"
                + "## HTTPS is not optional\n"
                + "\n"
                + "`com.codename1.security.vault.Vault` and the encrypted\n"
                + "`com.codename1.security.SecureStorage` both need Web Crypto, and Web Crypto\n"
                + "does not exist outside a secure context. Served over plain `http:` on anything\n"
                + "but `localhost`, storage writes are **refused** rather than quietly written in\n"
                + "the clear, so an insecure deployment fails loudly at the first write.\n"
                + "\n"
                + "Give the application its own origin where you can. Everything a browser\n"
                + "isolates -- storage, IndexedDB, the vault's non-extractable key, cookies -- is\n"
                + "keyed by origin and not by path, so a second application on the same host under\n"
                + "a different path shares all of it.\n"
                + "\n"
                + "## What a meta tag cannot do\n"
                + "\n"
                + "`frame-ancestors`, `X-Frame-Options` and `Strict-Transport-Security` are\n"
                + "ignored in a `<meta http-equiv>` tag. They exist only as response headers.\n"
                + "A deployment that embeds the policy in the page and skips the host\n"
                + "configuration has no clickjacking protection and no HSTS, whatever the rest of\n"
                + "the policy says.\n"
                + "\n"
                + "## The policy\n"
                + "\n"
                + "```\n"
                + csp + "\n"
                + "```\n"
                + "\n"
                + "The `'sha256-...'` entries are hashes of the inline blocks in the `index.html`\n"
                + "generated beside them. **Editing that file invalidates them** and the page\n"
                + "stops running, with a console error rather than an obvious failure. Regenerate\n"
                + "the build rather than hand-editing either file.\n"
                + "\n"
                + "`'wasm-unsafe-eval'` is required: the port's SQLite is a WebAssembly module.\n"
                + "It permits WebAssembly compilation and nothing else -- not `eval`, not\n"
                + "`new Function`.\n"
                + "\n"
                + "## If your application calls Display.execute(\"javascript:...\")\n"
                + "\n"
                + "That API evaluates a string, so it needs `'unsafe-eval'` in `script-src`. Adding\n"
                + "it grants the whole origin the ability to turn any string into code, which is\n"
                + "the concession an injected-script attack most wants. Prefer a\n"
                + "`BrowserComponent` bridge or a native interface, and add `'unsafe-eval'` only\n"
                + "if neither fits.\n"
                + "\n"
                + "## COOP and COEP are deliberately absent\n"
                + "\n"
                + "Cross-origin isolation would let the port use the default OPFS backend instead\n"
                + "of `opfs-sahpool`, and it breaks any cross-origin image, font, media or iframe\n"
                + "the application loads unless every one of them opts in with CORP or CORS. The\n"
                + "port is built not to need it. Add it only if you have measured that you do, and\n"
                + "expect to audit every external resource afterwards.\n"
                + "\n"
                + "## The service worker\n"
                + "\n"
                + "`sw.js` is served with `Cache-Control: no-cache` above. A pinned stale worker\n"
                + "keeps serving old application code indefinitely, and old application code can\n"
                + "still write the plaintext storage entries a newer version has migrated away\n"
                + "from. Do not cache authenticated API responses or decrypted data in the\n"
                + "worker's caches; the Cache API is readable by anything in the origin and is not\n"
                + "cleared when the vault locks.\n";
    }
}
