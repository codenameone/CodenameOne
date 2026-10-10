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
package com.codename1.gradle;

import com.codename1.build.SystemStreamLog;
import com.codename1.project.ProjectKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The wiring around `backendWebApp`: the browser build it stages, which
/// backend has an application to build, and the staged app beside a binary.
class BackendWebAppSupportTest {
    @TempDir
    Path tmp;

    private File write(String path, String text) throws IOException {
        File file = new File(tmp.toFile(), path);
        file.getParentFile().mkdirs();
        Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    /// A backend is not a servlet container, so the build it hosts is made
    /// without the proxy servlet the JavaScript port expects beside the page.
    @Test
    void theHostedBuildIsMadeWithoutAProxy() throws Exception {
        File settings = write("codenameone_settings.properties", "codename1.mainName=Main\n");
        Map<String, String> given = Collections.singletonMap("codename1.arg.ios.foo", "bar");

        Map<String, String> hosted = AppSupport.hostedProperties(given, settings);

        assertEquals("false", hosted.get("codename1.arg.javascript.inject_proxy"));
        assertEquals("bar", hosted.get("codename1.arg.ios.foo"));
        assertEquals(1, given.size(), "the project's own properties are not changed");
        assertEquals("false", AppSupport.hostedProperties(Collections.<String, String>emptyMap(),
                new File(tmp.toFile(), "absent.properties")).get("codename1.arg.javascript.inject_proxy"));
    }

    @Test
    void aProjectsOwnProxyChoiceStands() throws Exception {
        File plain = write("plain.properties", "codename1.mainName=Main\n");
        Map<String, String> onTheCommandLine = new LinkedHashMap<String, String>();
        onTheCommandLine.put("codename1.arg.javascript.inject_proxy", "true");
        assertEquals("true", AppSupport.hostedProperties(onTheCommandLine, plain)
                .get("codename1.arg.javascript.inject_proxy"));

        Map<String, String> aUrl = Collections.singletonMap("codename1.arg.javascript.proxy.url", "https://p/");
        assertFalse(AppSupport.hostedProperties(aUrl, plain).containsKey("codename1.arg.javascript.inject_proxy"));

        File chosen = write("chosen.properties", "codename1.arg.javascript.proxy.url=https://p/\n");
        assertFalse(AppSupport.hostedProperties(Collections.<String, String>emptyMap(), chosen)
                .containsKey("codename1.arg.javascript.inject_proxy"));
    }

    @Test
    void anEarlierBundleIsNotLeftForTheNextBuildToBeMistakenFor() throws Exception {
        final File stale = write("build/MyApp.zip", "old");
        org.gradle.api.provider.Provider<File> bundle = provider(stale);
        new AppSupport.DeleteStaleBundle(bundle).execute(null);
        assertFalse(stale.exists());
        // And nothing to remove is not a failure.
        new AppSupport.DeleteStaleBundle(bundle).execute(null);
    }

    @SuppressWarnings("unchecked")
    private static org.gradle.api.provider.Provider<File> provider(final File value) {
        return (org.gradle.api.provider.Provider<File>) java.lang.reflect.Proxy.newProxyInstance(
                BackendWebAppSupportTest.class.getClassLoader(),
                new Class<?>[] {org.gradle.api.provider.Provider.class}, (proxy, m, args) -> {
                    if (m.getName().equals("get")) {
                        return value;
                    }
                    throw new UnsupportedOperationException(m.getName());
                });
    }

    @Test
    void onlyAnApplicationsBackendHasAnAppToBuild() {
        assertTrue(BackendSupport.hostsApplication(true, ProjectKind.APP));
        assertFalse(BackendSupport.hostsApplication(false, null), "a backend at the root of its build");
        assertFalse(BackendSupport.hostsApplication(true, ProjectKind.LIB));
        assertFalse(BackendSupport.hostsApplication(true, ProjectKind.BACKEND));
    }

    /// The default layout: the binary is build/<name> and the app is staged in
    /// build/webapp, so they are neighbours and nothing is copied.
    @Test
    void theStagedAppIsAlreadyBesideABinaryInTheBuildDirectory() throws Exception {
        File staged = write("backend/build/webapp/index.html", "<html></html>").getParentFile();
        File binary = new File(tmp.toFile(), "backend/build/backend");

        assertEquals(staged.getAbsoluteFile(), BackendPackageSupport.webAppBeside(staged, binary));
        assertTrue(BackendPackageSupport.webAppIsBeside(staged, binary));
        assertEquals(staged.getAbsoluteFile(),
                BackendPackageSupport.webAppBesideTheBinary(new SystemStreamLog(), staged, binary));
        assertTrue(new File(staged, "index.html").isFile());
    }

    @Test
    void aBinaryPackagedElsewhereGetsTheAppCopiedToIt() throws Exception {
        File staged = write("backend/build/webapp/index.html", "<html>app</html>").getParentFile();
        write("backend/build/webapp/js/app.js", "run();");
        File binary = new File(tmp.toFile(), "dist/server");
        File beside = new File(tmp.toFile(), "dist/webapp").getAbsoluteFile();

        // Not there yet, so a package with unchanged inputs still has work to do.
        assertFalse(BackendPackageSupport.webAppIsBeside(staged, binary));
        assertEquals(beside, BackendPackageSupport.webAppBesideTheBinary(new SystemStreamLog(), staged, binary));
        assertEquals("run();", new String(Files.readAllBytes(new File(beside, "js/app.js").toPath()),
                StandardCharsets.UTF_8));
        assertTrue(BackendPackageSupport.webAppIsBeside(staged, binary));

        // A later browser build is newer than the copy.
        File index = new File(staged, "index.html");
        assertTrue(index.setLastModified(index.lastModified() + 60000));
        assertFalse(BackendPackageSupport.webAppIsBeside(staged, binary));
    }

    @Test
    void aBackendWithNoStagedAppShipsNone() throws Exception {
        File staged = new File(tmp.toFile(), "backend/build/webapp");
        File binary = new File(tmp.toFile(), "dist/server");

        assertNull(BackendPackageSupport.webAppBeside(staged, binary));
        assertTrue(BackendPackageSupport.webAppIsBeside(staged, binary));
        assertNull(BackendPackageSupport.webAppBesideTheBinary(new SystemStreamLog(), staged, binary));
        assertFalse(new File(tmp.toFile(), "dist/webapp").exists());
    }
}
