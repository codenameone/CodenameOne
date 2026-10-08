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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/// The JavaScript port's networking, at the two points where a byte or a header used to be lost
/// without any error: response headers read through the JSO bridge, and response bytes copied from
/// a `Uint8Array` into a Java `byte[]`.
///
/// Both were found by the sample app's backend tests: a cross-origin POST whose exposed `X-Probe`
/// header read back null, and a 100000-byte download that read back with the right length and the
/// wrong contents. Each is checked here by running the real `parparvm_runtime.js` and `port.js`
/// under Node, so no browser and no translated application is needed.
class JavascriptNetworkBindingsTest {

    private static final Path RUNTIME = Paths.get("..", "ByteCodeTranslator", "src", "javascript",
            "parparvm_runtime.js").toAbsolutePath().normalize();
    private static final Path PORT_ROOT =
            Paths.get("..", "..", "Ports", "JavaScriptPort").toAbsolutePath().normalize();
    private static final Path PORT_JS = PORT_ROOT.resolve(Paths.get("src", "main", "webapp", "port.js"));
    private static final Path NETWORK_CONNECTION = PORT_ROOT.resolve(Paths.get("src", "main", "java",
            "com", "codename1", "impl", "html5", "NetworkConnection.java"));

    /// The bridge maps a no-argument `getXxx()` to a property read, which is why
    /// `XMLHttpRequest.getAllResponseHeaders()` cannot be called through it. If the bridge ever
    /// learns to call it as a method this fails, and the `@JSBody` workaround can go.
    @Test
    void bridgeReadsGetAllResponseHeadersAsAProperty() throws Exception {
        String out = runNode(""
                + "const b = jvm.parseJsoBridgeMethod('com_codename1_html5_js_ajax_XMLHttpRequest',"
                + " 'cn1_com_codename1_html5_js_ajax_XMLHttpRequest_getAllResponseHeaders_R_java_lang_String');\n"
                + "console.log('KIND=' + b.kind + ' MEMBER=' + b.member);\n");
        assertTrue(out.contains("KIND=getter MEMBER=allResponseHeaders"),
                "the bridge no longer maps getAllResponseHeaders() to a property read: " + out);
    }

    /// No JavaScript-port source calls `getAllResponseHeaders()` on a JSO, which answers null for
    /// every response (see above). The call inside a `@JSBody` script is a string and is allowed.
    @Test
    void noPortSourceCallsGetAllResponseHeadersThroughTheBridge() throws Exception {
        Pattern call = Pattern.compile("\\.getAllResponseHeaders\\s*\\(\\s*\\)");
        StringBuilder offenders = new StringBuilder();
        int scanned = 0;
        try (Stream<Path> paths = Files.walk(PORT_ROOT.resolve(Paths.get("src", "main", "java")))) {
            for (Path path : (Iterable<Path>) paths.filter(p -> p.toString().endsWith(".java"))::iterator) {
                scanned++;
                String code = stripCommentsAndStrings(read(path));
                if (call.matcher(code).find()) {
                    offenders.append(' ').append(PORT_ROOT.relativize(path));
                }
            }
        }
        assertTrue(scanned > 100, "the JavaScript port sources were not found under " + PORT_ROOT);
        assertEquals("", offenders.toString(),
                "these call XMLHttpRequest.getAllResponseHeaders() through the JSO bridge, which reads "
                + "a property that does not exist and answers null; use NetworkConnection.responseHeaders");
    }

    /// The `@JSBody` that replaces it really invokes the method, and tolerates a missing receiver.
    @Test
    void responseHeadersScriptInvokesTheMethod() throws Exception {
        String source = read(NETWORK_CONNECTION);
        Matcher m = Pattern.compile("@JSBody\\(params=\\{\"xhr\"\\}, script=((?:\\s*\\+?\\s*\"[^\"]*\")+)\\)"
                + "\\s*static native String responseHeaders\\(XMLHttpRequest xhr\\);").matcher(source);
        assertTrue(m.find(), "the responseHeaders @JSBody was not found in NetworkConnection.java");
        StringBuilder script = new StringBuilder();
        Matcher literal = Pattern.compile("\"([^\"]*)\"").matcher(m.group(1));
        while (literal.find()) {
            script.append(literal.group(1));
        }
        String out = runNode(""
                + "const f = new Function('xhr', " + quoteJs(script.toString()) + ");\n"
                + "const xhr = { getAllResponseHeaders: function() {"
                + " return 'content-type: application/json\\r\\nx-probe: echo\\r\\n'; } };\n"
                + "console.log('HEADERS=' + JSON.stringify(f(xhr)));\n"
                + "console.log('NULL=' + f(null));\n");
        assertTrue(out.contains("HEADERS=\"content-type: application/json\\r\\nx-probe: echo\\r\\n\""),
                "the @JSBody did not return the XHR's header block: " + out);
        assertTrue(out.contains("NULL=null"), "the @JSBody must answer null without a request: " + out);
    }

    /// The send `@JSBody` applies the request's timeout and reports a timeout -- which a
    /// synchronous XHR throws -- as a failure string instead of letting it escape.
    @Test
    void sendScriptAppliesTheTimeoutAndReportsTimingOut() throws Exception {
        String source = read(NETWORK_CONNECTION);
        Matcher m = Pattern.compile("@JSBody\\(params=\\{\"xhr\", \"body\", \"timeoutMillis\"\\}, "
                + "script=((?:\\s*\\+?\\s*\"[^\"]*\")+)\\)\\s*static native String send\\(").matcher(source);
        assertTrue(m.find(), "the send @JSBody was not found in NetworkConnection.java");
        StringBuilder script = new StringBuilder();
        Matcher literal = Pattern.compile("\"([^\"]*)\"").matcher(m.group(1));
        while (literal.find()) {
            script.append(literal.group(1));
        }
        String out = runNode(""
                + "const f = new Function('xhr', 'body', 'timeoutMillis', " + quoteJs(script.toString()) + ");\n"
                + "const slow = { timeout: 0, send: function() {"
                + " const e = new Error('took too long'); e.name = 'TimeoutError'; throw e; } };\n"
                + "console.log('SLOW=' + f(slow, null, 2000) + ' TIMEOUT=' + slow.timeout);\n"
                + "let sent = null;\n"
                + "const fast = { timeout: 0, send: function(b) { sent = b === undefined ? 'none' : b; } };\n"
                + "console.log('FAST=' + f(fast, 'payload', 0) + ' SENT=' + sent + ' TIMEOUT=' + fast.timeout);\n"
                + "const quiet = { timeout: 0, status: 0, response: null, send: function() {} };\n"
                + "console.log('QUIET=' + f(quiet, null, 2000));\n"
                + "const answered = { timeout: 0, status: 200, response: {}, send: function() {} };\n"
                + "console.log('ANSWERED=' + f(answered, null, 2000));\n");
        assertTrue(out.contains("SLOW=TimeoutError: took too long TIMEOUT=2000"),
                "a timeout must be applied and reported: " + out);
        assertTrue(out.contains("FAST=null SENT=payload TIMEOUT=0"),
                "a request without a timeout is sent as it was: " + out);
        assertTrue(out.contains("QUIET=NetworkError: no response"),
                "a request that came back with no response at all must be a failure: " + out);
        assertTrue(out.contains("ANSWERED=null"), "an answered request is not a failure: " + out);
    }

    /// `ArrayBufferInputStream.read(byte[], int, int)` stores signed Java bytes. A raw 0..255
    /// value is a byte no Java code can produce: `b == (byte) 200` is false for it.
    @Test
    void bulkReadStoresSignedJavaBytes() throws Exception {
        String out = runNode(""
                + "const id = 'cn1_com_codename1_teavm_io_ArrayBufferInputStream_readBulkImpl_"
                + "com_codename1_html5_js_typedarrays_Uint8Array_int_byte_1ARRAY_int_int';\n"
                + "if (typeof global[id] !== 'function') throw new Error('readBulkImpl is not bound');\n"
                + "const src = jvm.wrapJsObject(Uint8Array.from([9, 0, 127, 128, 200, 255]),"
                + " 'com_codename1_html5_js_typedarrays_Uint8Array');\n"
                + "const dst = jvm.newArray(7, 'JAVA_BYTE', 1);\n"
                + "const r = global[id](src, 1, dst, 2, 5);\n"
                + "if (r && typeof r.next === 'function') { r.next(); }\n"
                + "console.log('BYTES=' + JSON.stringify(Array.from(dst)));\n"
                // And back: the signed values become the original unsigned ones again.
                + "const back = global['cn1_com_codename1_teavm_io_BlobUtil_byteArrayToUint8Array_"
                + "byte_1ARRAY_R_com_codename1_html5_js_typedarrays_Uint8Array'](dst);\n"
                + "console.log('BACK=' + JSON.stringify(Array.from(back.__jsValue || back)));\n");
        assertTrue(out.contains("BYTES=[0,0,0,127,-128,-56,-1]"),
                "bulk read must sign-extend into the byte[] and honour both offsets: " + out);
        assertTrue(out.contains("BACK=[0,0,0,127,128,200,255]"),
                "byte[] to Uint8Array must restore the unsigned values: " + out);
    }

    @Test
    void getBytesStoresSignedJavaBytes() throws Exception {
        // The same rule for the runtime's String.getBytes: "\u00e9" is C3 A9 in UTF-8,
        // which a Java byte[] holds as -61 and -87.
        String out = runNode(""
                + "const id = 'cn1_java_lang_String_charsToBytes_char_1ARRAY_char_1ARRAY_R_byte_1ARRAY';\n"
                + "if (typeof global[id] !== 'function') throw new Error('charsToBytes is not bound');\n"
                + "const chars = jvm.newArray(2, 'JAVA_CHAR', 1);\n"
                + "chars[0] = 65; chars[1] = 233;\n"
                + "let r = global[id](chars, null);\n"
                + "if (r && typeof r.next === 'function') { r = r.next().value; }\n"
                + "console.log('BYTES=' + JSON.stringify(Array.from(r)));\n");
        assertTrue(out.contains("BYTES=[65,-61,-87]"),
                "getBytes must sign-extend into the byte[]: " + out);
    }

    static String runNode(String body) throws Exception {
        assertTrue(Files.exists(RUNTIME), "ParparVM JavaScript runtime not found at " + RUNTIME);
        assertTrue(Files.exists(PORT_JS), "port.js not found at " + PORT_JS);
        Path harness = Files.createTempFile("js-network-bindings", ".js");
        try {
            String source = ""
                    + "const fs = require('fs');\n"
                    + "const vm = require('vm');\n"
                    + "global.self = global; global.window = global; global.global = global;\n"
                    + "global.postMessage = function() {};\n"
                    + "vm.runInThisContext(fs.readFileSync(" + quoteJs(RUNTIME.toString()) + ", 'utf8'));\n"
                    + "vm.runInThisContext(fs.readFileSync(" + quoteJs(PORT_JS.toString()) + ", 'utf8'));\n"
                    + body;
            Files.write(harness, source.getBytes(StandardCharsets.UTF_8));
            Process process = new ProcessBuilder("node", harness.toString()).start();
            String output = readAll(process.getInputStream());
            String errors = readAll(process.getErrorStream());
            assertEquals(0, process.waitFor(), "node harness failed. stdout: " + output + " stderr: " + errors);
            return output;
        } finally {
            Files.deleteIfExists(harness);
        }
    }

    /// Blanks out comments and string literals so a mention of a call in either is not a call.
    private static String stripCommentsAndStrings(String code) {
        StringBuilder out = new StringBuilder(code.length());
        int i = 0;
        int n = code.length();
        while (i < n) {
            char c = code.charAt(i);
            if (c == '/' && i + 1 < n && code.charAt(i + 1) == '/') {
                while (i < n && code.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && i + 1 < n && code.charAt(i + 1) == '*') {
                int end = code.indexOf("*/", i + 2);
                i = end < 0 ? n : end + 2;
            } else if (c == '"' || c == '\'') {
                i++;
                while (i < n && code.charAt(i) != c) {
                    if (code.charAt(i) == '\\') {
                        i++;
                    }
                    i++;
                }
                i++;
                out.append("\"\"");
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private static String read(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static String readAll(InputStream input) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            out.write(buffer, 0, count);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String quoteJs(String value) {
        StringBuilder out = new StringBuilder("'");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' || c == '\'') {
                out.append('\\');
            }
            out.append(c);
        }
        return out.append('\'').toString();
    }
}
