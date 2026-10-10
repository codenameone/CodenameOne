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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    private static String reason(String message) {
        return FirstBuildTelemetry.reason(new RuntimeException(message));
    }

    private static String wrapped(Throwable cause) {
        return FirstBuildTelemetry.reason(new RuntimeException("Build failed", cause));
    }
}
