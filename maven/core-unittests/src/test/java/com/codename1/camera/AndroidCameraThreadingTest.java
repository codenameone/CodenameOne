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
package com.codename1.camera;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Runs the port's startup/preview/teardown code against thread-checking CameraX doubles. */
class AndroidCameraThreadingTest {
    @TempDir
    static Path temporary;
    private static URLClassLoader loader;
    private static Class<?> harness;

    @BeforeAll
    static void compileProductionMethods() throws Exception {
        String android = read(Paths.get("../../Ports/Android/src/com/codename1/impl/android/AndroidCameraImpl.java"));
        StringBuilder methods = new StringBuilder();
        for (String signature : new String[] {"public void open(", "public PeerComponent createPreviewPeer(",
                "public void close(", "private static Executor mainExecutor("}) {
            methods.append(method(android, signature));
        }
        // Optional so this harness can also demonstrate the pre-fix failure.
        if (android.contains("private void preparePreview(")) {
            methods.append(method(android, "private void preparePreview("));
        }
        String fields = android.substring(android.indexOf("private static final String TAG"),
                android.indexOf("public AndroidCameraImpl("));
        String impl = read(Paths.get("../../Ports/Android/src/com/codename1/impl/android/AndroidImplementation.java"));
        String fixture = read(Paths.get("src/test/resources/AndroidCameraThreadingHarness.java.template"))
                .replace("/* PRODUCTION_FIELDS */", fields)
                .replace("/* PRODUCTION_METHODS */", methods)
                .replace("/* UI_SYNC_METHOD */", method(impl, "public static void runOnUiThreadSync(")
                        .replace("com.codename1.io.Log.e(t)", "Log.e(null, null, t)"));
        Path source = temporary.resolve("AndroidCameraThreadingHarness.java");
        Files.write(source, fixture.getBytes(StandardCharsets.UTF_8));
        Path preview = temporary.resolve("androidx/camera/core/Preview.java");
        Files.createDirectories(preview.getParent());
        Files.write(preview, ("package androidx.camera.core; public class Preview { "
                + "public interface SurfaceProvider {} }").getBytes(StandardCharsets.UTF_8));
        Path size = temporary.resolve("android/util/Size.java");
        Files.createDirectories(size.getParent());
        Files.write(size, ("package android.util; public class Size { "
                + "public Size(int w, int h) {} }").getBytes(StandardCharsets.UTF_8));
        assertNotNull(ToolProvider.getSystemJavaCompiler(), "A JDK is required");
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-d", temporary.toString(), source.toString(), preview.toString(), size.toString()));
        loader = new URLClassLoader(new URL[] {temporary.toUri().toURL()}, null);
        harness = loader.loadClass("AndroidCameraThreadingHarness");
    }

    private static String read(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static String method(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "Missing production method: " + signature);
        int depth = 1;
        int end = source.indexOf('{', start) + 1;
        while (depth > 0 && end < source.length()) {
            char c = source.charAt(end++);
            if (c == '{') depth++;
            if (c == '}') depth--;
        }
        assertEquals(0, depth);
        return source.substring(start, end) + "\n";
    }

    @AfterAll
    static void closeLoader() throws Exception {
        if (harness != null) harness.getMethod("shutdown").invoke(null);
        if (loader != null) loader.close();
    }

    @Test
    void previewIsReadyBeforeBindAndPeerStaysOnCallerThread() throws Exception {
        harness.getMethod("openAndClose").invoke(null);
    }

    @Test
    void previewFailureReleasesExecutorAndAllowsRetry() throws Exception {
        harness.getMethod("failedPreview").invoke(null);
    }

    @Test
    void failedBindUnbindsAndAllowsRetry() throws Exception {
        harness.getMethod("failedBind").invoke(null);
    }

    @Test
    void lateProviderCannotReopenTimedOutSession() throws Exception {
        harness.getMethod("timedOutOpen").invoke(null);
    }

    @Test
    void closingAfterGlobalActivityIsClearedStillReleasesCamera() throws Exception {
        harness.getMethod("closeWithoutRegisteredActivity").invoke(null);
    }

    @Test
    void dispatchFailureStillReleasesExecutorAndNativeReferences() throws Exception {
        harness.getMethod("failedCloseDispatch").invoke(null);
    }

    @Test
    void interruptedCloseFinishesCleanupAndRestoresInterruptFlag() throws Exception {
        harness.getMethod("interruptedClose").invoke(null);
    }

    @Test
    void closeFromAndroidMainThreadDoesNotDeadlock() throws Exception {
        harness.getMethod("closeFromMain").invoke(null);
    }
}
