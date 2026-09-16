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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The generated entry point has to install a native theme.
 *
 * <p>IOSImplementation.installNativeTheme() picks its resource from a static
 * iosMode that defaults to {@code auto}, and therefore to iOS7Theme.res. That
 * file declares no {@code @darkModeBool}, which is the constant
 * UIManager:654 gates the whole dark palette on -- only iOSModernTheme.res
 * declares it. So a stub that never calls setIosMode leaves the application
 * with no dark mode at all, however carefully it asks for one, and every
 * dark-appearance screenshot comes out light.</p>
 *
 * <p>Asserted on the generated SOURCE rather than through a build: the symptom
 * only appears when a screenshot suite runs on a macOS runner an hour later,
 * and four attempts to reproduce it locally failed on environment rather than
 * on the thing being measured.</p>
 */
class MacOSStubThemeTest {

    @Test
    void theGeneratedStubInstallsTheModernThemeByDefault(@TempDir Path tmp) throws Exception {
        String stub = generateStub(tmp, new HashMap<String, String>());
        assertTrue(stub.contains("setIosMode(\"modern\")"),
                "the stub must select the only theme that declares @darkModeBool; got:\n" + stub);
    }

    @Test
    void theHintStillChoosesTheLegacyTheme(@TempDir Path tmp) throws Exception {
        Map<String, String> hints = new HashMap<String, String>();
        hints.put("macos.themeMode", "ios7");
        assertTrue(generateStub(tmp, hints).contains("setIosMode(\"ios7\")"),
                "an application that asks for the legacy theme has to get it");
    }

    /// Selecting the theme is only half of it: the resource has to reach the
    /// application's own resource directory, because installNativeTheme() asks
    /// for it by name at run time and falls back to the legacy theme WITHOUT
    /// SAYING SO when the lookup returns null. The iOS builder gets this for
    /// free by unzipping its native jar with buildinRes as the destination; this
    /// one stages the same jar for clang alone, so the copy is explicit.
    @Test
    void theThemeResourceReachesTheApplicationResources(@TempDir Path tmp) throws Exception {
        File nativeSources = new File(tmp.toFile(), "nativeSources");
        File buildinRes = new File(tmp.toFile(), "btres");
        assertTrue(nativeSources.mkdirs() && buildinRes.mkdirs());
        Files.write(new File(nativeSources, "iOSModernTheme.res").toPath(),
                "theme".getBytes(StandardCharsets.UTF_8));
        Files.write(new File(nativeSources, "METALView.m").toPath(),
                "native".getBytes(StandardCharsets.UTF_8));

        MacOSNativeBuilder.stageThemeResources(nativeSources, buildinRes);

        assertTrue(new File(buildinRes, "iOSModernTheme.res").isFile(),
                "the theme has to be an application resource, not only a clang input");
        assertFalse(new File(buildinRes, "METALView.m").exists(),
                "native sources are not application resources");
        assertTrue(new File(nativeSources, "iOSModernTheme.res").isFile(),
                "and it stays staged for the signature gate, which reads that set");
    }

    /// An application shipping its own theme of the same name keeps it.
    @Test
    void anApplicationsOwnResourceIsNotOverwritten(@TempDir Path tmp) throws Exception {
        File nativeSources = new File(tmp.toFile(), "nativeSources");
        File buildinRes = new File(tmp.toFile(), "btres");
        assertTrue(nativeSources.mkdirs() && buildinRes.mkdirs());
        Files.write(new File(nativeSources, "iOSModernTheme.res").toPath(),
                "ours".getBytes(StandardCharsets.UTF_8));
        Files.write(new File(buildinRes, "iOSModernTheme.res").toPath(),
                "theirs".getBytes(StandardCharsets.UTF_8));

        MacOSNativeBuilder.stageThemeResources(nativeSources, buildinRes);

        assertEquals("theirs", new String(Files.readAllBytes(
                        new File(buildinRes, "iOSModernTheme.res").toPath()),
                StandardCharsets.UTF_8));
    }

    /// Drives the real writeStub, so this cannot pass against a builder that
    /// stopped emitting the call.
    private static String generateStub(Path tmp, final Map<String, String> raw) throws Exception {
        File stubSource = new File(tmp.toFile(), "stubSource");
        File classesDir = new File(tmp.toFile(), "classes");
        assertTrue(stubSource.mkdirs() && classesDir.mkdirs());

        MacOSBuildHints hints = new MacOSBuildHints();
        hints.parse(new MacOSBuildHints.HintSource() {
            @Override
            public String get(String key, String defaultValue) {
                String v = raw.get(key);
                return v != null ? v : defaultValue;
            }
        }, "com.example.app");

        BuildRequest request = new BuildRequest();
        request.setPackageName("com.example.app");
        request.setMainClass("MyApp");
        request.setVersion("1.0");

        MacOSNativeBuilder builder = new MacOSNativeBuilder();
        builder.setCodenameOneJar(new File("../core/target/classes"));
        builder.writeStub(request, stubSource, classesDir, hints);

        File written = new File(stubSource, "com/example/app/MyAppStub.java");
        assertTrue(written.isFile(), "the stub was not written to " + written);
        return new String(Files.readAllBytes(written.toPath()), StandardCharsets.UTF_8);
    }
}
