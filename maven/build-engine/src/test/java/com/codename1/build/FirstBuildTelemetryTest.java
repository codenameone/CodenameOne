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
package com.codename1.build;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FirstBuildTelemetryTest {
    private static final String URL = "https://cloud.example/api/v2/funnel/initializr-event";
    private static final String ID = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private static Map<String, String> env(String... kv) {
        Map<String, String> m = new HashMap<String, String>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    private static Properties props(String... kv) {
        Properties p = new Properties();
        for (int i = 0; i < kv.length; i += 2) {
            p.setProperty(kv[i], kv[i + 1]);
        }
        return p;
    }

    @Test
    void reportsOnlyForAProjectThatOptedIn() {
        assertNull(FirstBuildTelemetry.start(null, ID, "run", env(), props()));
        assertNull(FirstBuildTelemetry.start(URL, null, "run", env(), props()));
        assertNull(FirstBuildTelemetry.start(URL, " ", "run", env(), props()));
        assertNull(FirstBuildTelemetry.start("file:///etc/passwd", ID, "run", env(), props()));
        assertNotNull(FirstBuildTelemetry.start(URL, ID, "run", env(), props()));
    }

    @Test
    void theLaunchersSwitchTurnsItOff() {
        assertNull(FirstBuildTelemetry.start(URL, ID, "run", env("CN1_TELEMETRY", "0"), props()));
        assertNull(FirstBuildTelemetry.start(URL, ID, "run", env(), props("cn1.telemetry", "false")));
        assertNotNull(FirstBuildTelemetry.start(URL, ID, "run", env("CN1_TELEMETRY", "1"), props()));
    }

    @Test
    void aBuildTheLauncherStartedIsLeftToTheLauncher() {
        assertNull(FirstBuildTelemetry.start(URL, ID, "run", env("CN1_LAUNCHER", "1"), props()));
    }

    @Test
    void namesTheToolThatRanTheBuild() {
        assertEquals("intellij", FirstBuildTelemetry.client(env(), props("idea.version", "2026.2")));
        assertEquals("intellij", FirstBuildTelemetry.client(env(), props("idea.active", "true")));
        assertEquals("netbeans", FirstBuildTelemetry.client(env(), props("netbeans.execution", "true")));
        assertEquals("eclipse", FirstBuildTelemetry.client(env(), props("osgi.framework", "x")));
        assertEquals("vscode", FirstBuildTelemetry.client(env("TERM_PROGRAM", "vscode"), props()));
        assertEquals("cli", FirstBuildTelemetry.client(env("TERM_PROGRAM", "Apple_Terminal"), props()));
    }

    @Test
    void theTargetIsTheBuildTargetOrTheFirstGoal() {
        assertEquals("android-device", FirstBuildTelemetry.target("android-device", Arrays.asList("package")));
        assertEquals("run", FirstBuildTelemetry.target(null, Arrays.asList("cn1:run", "package")));
        assertEquals("package", FirstBuildTelemetry.target("", Arrays.asList("package")));
        assertNull(FirstBuildTelemetry.target(null, null));
        assertEquals("bad_target_", FirstBuildTelemetry.token("Bad Target!"));
    }

    @Test
    void oneWordForWhyTheBuildStopped() {
        assertEquals("no_jdk", reason("Fatal error compiling: No compiler is provided in this environment."));
        assertEquals("java_too_old", reason("Fatal error compiling: error: release version 17 not supported"));
        assertEquals("java_too_old", reason("Fatal error compiling: invalid target release: 17"));
        assertEquals("java_too_old", reason("Rule 0: org.apache.maven.enforcer.rules.version.RequireJavaVersion failed"));
        assertEquals("login_timeout", reason("Browser login timed out after 5 minutes"));
        assertEquals("login_failed", reason("Cannot authenticate a non-interactive build"));
        assertEquals("compile", reason("Compilation failure: Main.java:[12,5] cannot find symbol"));
        assertEquals("tls", reason("Could not transfer artifact x: PKIX path building failed"));
        assertEquals("maven_download", reason("Could not resolve dependencies for project com.example:app"));
        assertEquals("maven_download", wrapped(new ConnectException("Connection refused")));
        assertEquals("size_limit", reason("Jar size limit reached"));
        assertEquals("no_certificate", reason("A certificate from Apple with the appropriate password is required"));
        assertEquals("upload_failed", reason("Failed to upload to server"));
        assertEquals("server_refused", reason("Error! Server response code 500"));
        assertEquals("build_failed", reason("Something nobody anticipated"));
    }

    @Test
    void readsTheWholeCauseChain() {
        Exception root = new IOException("Compilation failure");
        assertEquals("compile", FirstBuildTelemetry.reason(new RuntimeException("Build failed", root)));
    }

    @Test
    void encodesTheFormLikeTheLaunchers() throws Exception {
        Map<String, String> f = new LinkedHashMap<String, String>();
        f.put("pkg", ID);
        f.put("step", "launch");
        f.put("target", null);
        f.put("client", "intellij");
        assertEquals("pkg=" + ID + "&step=launch&client=intellij", FirstBuildTelemetry.form(f));
    }

    private HttpServer server;
    private final CopyOnWriteArrayList<String> bodies = new CopyOnWriteArrayList<String>();

    private String recorder(final long delayMs) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            InputStream in = exchange.getRequestBody();
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int n;
            while ((n = in.read(buf)) > 0) {
                b.write(buf, 0, n);
            }
            bodies.add(b.toString("UTF-8"));
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/e";
    }

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
        System.clearProperty(FirstBuildTelemetry.OUTCOME_PROPERTY);
    }

    private String exitReport(String url, String clientOutput, boolean clientSucceeded, Throwable failure) {
        FirstBuildTelemetry t = FirstBuildTelemetry.start(url, ID, "javascript", env(), props());
        FirstBuildTelemetry.clearNotedOutcome();
        if (clientOutput != null) {
            FirstBuildTelemetry.noteBuildClientOutput(clientOutput, clientSucceeded);
        }
        bodies.clear();
        t.finished(failure);
        return bodies.get(bodies.size() - 1);
    }

    @Test
    void aCleanExitIsNotASubmittedBuildUnlessTheClientSaysSo() throws Exception {
        String url = recorder(0);
        // The build client returns normally from the free plan's size limit and from an
        // upload it never confirmed; only its output tells them apart from a submission.
        assertTrue(exitReport(url, "Jar size limit reached\n", true, null).contains("exit=0&reason=size_limit"));
        assertTrue(exitReport(url, "Sending build request to the server\n", true, null)
                .contains("exit=0&reason=not_submitted"));
        assertTrue(exitReport(url, "Sending build request to the server\nYour build was submitted\n", true, null)
                .contains("exit=0&reason=ok"));
        // No cloud build ran (compile, the simulator): nothing was noted.
        assertTrue(exitReport(url, null, true, null).contains("exit=0&reason=ok"));
    }

    @Test
    void theClientsOwnWordsNameAGenericFailure() throws Exception {
        String url = recorder(0);
        assertTrue(exitReport(url, "Browser login timed out\n", false, new RuntimeException("Ant task failed"))
                .contains("exit=1&reason=login_timeout"));
        // A specific failure from the exception is kept.
        assertTrue(exitReport(url, "Browser login timed out\n", false, new RuntimeException("Compilation failure"))
                .contains("exit=1&reason=compile"));
    }

    @Test
    void anOutcomeIsReportedForTheBuildThatNotedItOnly() throws Exception {
        String url = recorder(0);
        FirstBuildTelemetry.noteBuildClientOutput("Jar size limit reached", true);
        FirstBuildTelemetry t = FirstBuildTelemetry.start(url, ID, "javascript", env(), props());
        t.launched(); // a new build forgets what the previous one noted
        t.finished(null);
        String exit = null;
        for (String b : bodies) {
            if (b.contains("step=exit")) {
                exit = b;
            }
        }
        assertNotNull(exit);
        assertTrue(exit.contains("reason=ok"), exit);
    }

    @Test
    void aStalledServerHoldsTheBuildUpForThreeSecondsAtMost() throws Exception {
        String url = recorder(6_000);
        FirstBuildTelemetry t = FirstBuildTelemetry.start(url, ID, "run", env(), props());
        long began = System.currentTimeMillis();
        t.finished(null);
        long took = System.currentTimeMillis() - began;
        assertTrue(took < 3_500, "took " + took + "ms");
    }

    private static String reason(String message) {
        return FirstBuildTelemetry.reason(new RuntimeException(message));
    }

    private static String wrapped(Throwable cause) {
        return FirstBuildTelemetry.reason(new RuntimeException("Build failed", cause));
    }
}
