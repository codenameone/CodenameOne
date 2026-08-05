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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives {@code browser_bridge.js}'s native-interface dispatch directly in node, with the
 * page boot suppressed (a {@code document.readyState} of {@code "loading"} makes the bridge
 * wait for a DOMContentLoaded that never fires), so the registry lookup contract can be
 * exercised without a browser or a translated app.
 */
class JavaScriptNativeInterfaceBridgeTest {
    private static final Path BROWSER_BRIDGE =
            Paths.get("..", "ByteCodeTranslator", "src", "javascript", "browser_bridge.js");

    @Test
    void isSupportedAnswersFalseForAnInterfaceWithNoRegisteredJsImplementation() throws Exception {
        // NativeLookup.create() always resolves on this port -- the builder generates and
        // registers an <Iface>Impl for EVERY native interface in the app -- so isSupported()
        // is the developer's only "is this bound here?" signal. Rejecting the call turned the
        // standard "create(X.class) != null && x.isSupported()" guard into a thrown
        // RuntimeException for any app shipping no JS stub for the interface (issue #5512).
        String out = runBridgeProbe();

        assertTrue(out.contains("unregistered.isSupported=false"),
                "isSupported() on an unbound interface must resolve false, not reject. out=" + out);
        assertTrue(out.contains("partial.isSupported=false"),
                "isSupported() must resolve false when the registered stub omits it. out=" + out);
    }

    @Test
    void aRegisteredImplementationStillAnswersForItself() throws Exception {
        String out = runBridgeProbe();

        assertTrue(out.contains("registered.isSupported=true"),
                "A registered stub's own isSupported() answer must win. out=" + out);
    }

    @Test
    void everyOtherMethodOfAnUnboundInterfaceStillFails() throws Exception {
        // Silently resolving an unimplemented native would turn a real binding bug into a
        // no-op, so the fallback is scoped to isSupported() alone.
        String out = runBridgeProbe();

        assertTrue(out.contains("unregistered.other=rejected:No native interface implementation registered for"),
                "Calling an unimplemented native must still fail loudly. out=" + out);
    }

    private static String runBridgeProbe() throws Exception {
        Path harness = Files.createTempFile("js-native-interface-bridge", ".js");
        Files.write(harness, probeSource().getBytes(StandardCharsets.UTF_8));

        Process process = new ProcessBuilder("node", harness.toString()).start();
        String stdout = readAll(process.getInputStream());
        String stderr = readAll(process.getErrorStream());
        int rc = process.waitFor();
        assertEquals(0, rc, "Node bridge probe should exit cleanly. stdout: " + stdout + " stderr: " + stderr);
        return stdout;
    }

    private static String probeSource() throws Exception {
        String bridgePath = BROWSER_BRIDGE.toAbsolutePath().normalize().toString().replace("\\", "\\\\");
        return "const fs = require('fs');\n"
                + "const vm = require('vm');\n"
                + "const src = fs.readFileSync('" + bridgePath + "', 'utf8');\n"
                + "const documentStub = {\n"
                + "  readyState: 'loading',\n"
                + "  addEventListener: function() {},\n"
                + "  getElementById: function() { return null; },\n"
                + "  createElement: function() { return { style: {}, appendChild: function() {} }; },\n"
                + "  head: { appendChild: function() {} }\n"
                + "};\n"
                + "const selfStub = {\n"
                + "  console: { log: function() {}, warn: function() {}, error: function() {} },\n"
                + "  location: { search: '', href: 'http://localhost/' },\n"
                + "  devicePixelRatio: 1,\n"
                + "  addEventListener: function() {},\n"
                + "  setTimeout: setTimeout,\n"
                + "  clearTimeout: clearTimeout,\n"
                + "  Promise: Promise\n"
                + "};\n"
                + "selfStub.self = selfStub;\n"
                + "selfStub.window = selfStub;\n"
                + "selfStub.document = documentStub;\n"
                + "vm.runInContext(src, vm.createContext(selfStub), { filename: 'browser_bridge.js' });\n"
                + "const call = selfStub.cn1HostBridge.handlers['__cn1_native_interface_call__'];\n"
                + "async function probe(label, iface, method) {\n"
                + "  try {\n"
                + "    console.log(label + '=' + await call(iface, method, []));\n"
                + "  } catch (err) {\n"
                + "    console.log(label + '=rejected:' + String(err && err.message || err));\n"
                + "  }\n"
                + "}\n"
                + "async function run() {\n"
                + "  await probe('unregistered.isSupported', 'bridge_SystemTime', 'isSupported_');\n"
                + "  await probe('unregistered.other', 'bridge_SystemTime', 'currentTime_');\n"
                + "  selfStub.cn1_native_interfaces = {\n"
                + "    bridge_Partial: { other_: function(cb) { cb.complete(1); } },\n"
                + "    bridge_Real: { isSupported_: function(cb) { cb.complete(true); } }\n"
                + "  };\n"
                + "  await probe('partial.isSupported', 'bridge_Partial', 'isSupported_');\n"
                + "  await probe('registered.isSupported', 'bridge_Real', 'isSupported_');\n"
                + "}\n"
                + "run();\n";
    }

    private static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) > 0) {
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
