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
package com.codename1.maven;

import com.codename1.build.BuildFailureException;
import com.codename1.build.SystemStreamLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackendWebAppTest {
    @TempDir
    Path tmp;

    private final BackendWebApp webApp = new BackendWebApp(new SystemStreamLog());

    /// Text long enough, and repetitive enough, to be worth a compressed copy.
    private static String script() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            out.append("function f").append(i).append("(){return ").append(i).append(";}\n");
        }
        return out.toString();
    }

    private static File zip(File file, String... namesAndContents) throws IOException {
        file.getParentFile().mkdirs();
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
        try {
            for (int i = 0; i < namesAndContents.length; i += 2) {
                out.putNextEntry(new ZipEntry(namesAndContents[i]));
                out.write(namesAndContents[i + 1].getBytes("UTF-8"));
                out.closeEntry();
            }
        } finally {
            out.close();
        }
        return file;
    }

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), "UTF-8");
    }

    private static String gunzip(File file) throws IOException {
        InputStream in = new GZIPInputStream(new FileInputStream(file));
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int n = in.read(chunk);
            while (n >= 0) {
                out.write(chunk, 0, n);
                n = in.read(chunk);
            }
            return new String(out.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }

    @Test
    void stagesABundleAndCompressesItsText() throws Exception {
        File bundle = zip(tmp.resolve("app.zip").toFile(),
                "index.html", "<html></html>",
                "translated_app.js", script(),
                "js/", "",
                "js/fontmetrics.js", "m();",
                "assets/logo.png", script());
        File out = tmp.resolve("server/target/webapp").toFile();

        assertEquals(4, webApp.stage(bundle, out));

        assertEquals("<html></html>", read(new File(out, "index.html")));
        assertEquals("m();", read(new File(out, "js/fontmetrics.js")));
        assertEquals(script(), read(new File(out, "translated_app.js")));
        // The original stays: a browser that refuses gzip, and a byte range,
        // are both answered from it.
        assertEquals(script(), gunzip(new File(out, "translated_app.js.gz")));
        assertTrue(new File(out, "translated_app.js.gz").length() < new File(out, "translated_app.js").length());
        // Too small to be worth it, and not text at all.
        assertFalse(new File(out, "index.html.gz").exists());
        assertFalse(new File(out, "js/fontmetrics.js.gz").exists());
        assertFalse(new File(out, "assets/logo.png.gz").exists());
    }

    @Test
    void replacesWhatWasStagedBefore() throws Exception {
        File out = tmp.resolve("webapp").toFile();
        webApp.stage(zip(tmp.resolve("one.zip").toFile(),
                "index.html", "one", "gone.js", "x"), out);
        webApp.stage(zip(tmp.resolve("two.zip").toFile(), "index.html", "two"), out);
        assertEquals("two", read(new File(out, "index.html")));
        // A file the new build no longer has must not go on being served.
        assertFalse(new File(out, "gone.js").exists());
    }

    @Test
    void unwrapsABundleThatSitsInOneDirectory() throws Exception {
        File bundle = zip(tmp.resolve("wrapped.zip").toFile(),
                "MyApp-1.0/index.html", "wrapped", "MyApp-1.0/js/a.js", "a");
        File out = tmp.resolve("webapp").toFile();
        assertEquals(2, webApp.stage(bundle, out));
        assertEquals("wrapped", read(new File(out, "index.html")));
        assertEquals("a", read(new File(out, "js/a.js")));
    }

    @Test
    void stagesADirectory() throws Exception {
        File dir = tmp.resolve("unpacked").toFile();
        new File(dir, "css").mkdirs();
        Files.write(new File(dir, "index.html").toPath(), "dir".getBytes("UTF-8"));
        Files.write(new File(dir, "css/a.css").toPath(), "a{}".getBytes("UTF-8"));
        File out = tmp.resolve("webapp").toFile();
        assertEquals(2, webApp.stage(dir, out));
        assertEquals("a{}", read(new File(out, "css/a.css")));
    }

    @Test
    void refusesAnEntryThatClimbsOutOfTheDirectory() throws Exception {
        File bundle = zip(tmp.resolve("slip.zip").toFile(),
                "index.html", "x", "../../escaped.txt", "escaped");
        File out = tmp.resolve("a/b/webapp").toFile();
        BuildFailureException refused = assertThrows(BuildFailureException.class,
                () -> webApp.stage(bundle, out));
        assertTrue(refused.getMessage().contains("escaped.txt"), refused.getMessage());
        assertFalse(tmp.resolve("a/escaped.txt").toFile().exists());
        assertFalse(tmp.resolve("escaped.txt").toFile().exists());
    }

    @Test
    void refusesABundleThatIsNotAnApplication() throws Exception {
        File bundle = zip(tmp.resolve("sources.zip").toFile(), "pom.xml", "<project/>");
        File out = tmp.resolve("webapp").toFile();
        BuildFailureException refused = assertThrows(BuildFailureException.class,
                () -> webApp.stage(bundle, out));
        assertTrue(refused.getMessage().contains("index.html"), refused.getMessage());
        // Nothing half-staged is left for a server to find.
        assertFalse(out.exists());
        assertThrows(BuildFailureException.class,
                () -> webApp.stage(tmp.resolve("absent.zip").toFile(), out));
    }

    @Test
    void refusesToStageADirectoryIntoItself() throws Exception {
        File dir = tmp.resolve("unpacked").toFile();
        dir.mkdirs();
        Files.write(new File(dir, "index.html").toPath(), "dir".getBytes("UTF-8"));
        assertThrows(BuildFailureException.class, () -> webApp.stage(dir, new File(dir, "webapp")));
        assertThrows(BuildFailureException.class, () -> webApp.stage(dir, dir));
        assertEquals("dir", read(new File(dir, "index.html")));
    }

    @Test
    void findsTheBundleOfWhicheverModuleBuildsForTheBrowser() throws Exception {
        File root = tmp.resolve("app").toFile();
        assertNull(BackendWebApp.locateBundle(root));
        // The archive of sources a cloud build uploads lives in the same directory.
        zip(new File(root, "common/target/app-common-1.0-src.zip"), "pom.xml", "<project/>");
        assertNull(BackendWebApp.locateBundle(root));
        File common = zip(new File(root, "common/target/app-common-1.0.zip"), "index.html", "c");
        assertEquals(common, BackendWebApp.locateBundle(root));
        File javascript = zip(new File(root, "javascript/target/app-javascript-1.0.ZIP"), "index.html", "j");
        assertEquals(javascript, BackendWebApp.locateBundle(root));
        File newer = zip(new File(root, "javascript/target/app-javascript-1.1.zip"), "index.html", "j2");
        assertTrue(newer.setLastModified(javascript.lastModified() + 5000));
        assertEquals(newer, BackendWebApp.locateBundle(root));
    }

    /// A project with one build directory for every platform, as a Gradle
    /// project has: the bundle is the newest archive there that is an app.
    @Test
    void findsTheBundleInABuildDirectory() throws Exception {
        File build = new File(tmp.toFile(), "build");
        assertNull(BackendWebApp.locateBundleIn(build));
        assertNull(BackendWebApp.locateBundleIn(null));
        zip(new File(build, "sources.zip"), "src/Main.java", "class Main {}");
        assertNull(BackendWebApp.locateBundleIn(build));
        File app = zip(new File(build, "MyApp.zip"), "index.html", "<html></html>");
        assertEquals(app, BackendWebApp.locateBundleIn(build));
    }

    @Test
    void leavesAProjectsOwnProxyChoiceAlone() throws Exception {
        File settings = tmp.resolve("codenameone_settings.properties").toFile();
        assertFalse(BackendWebApp.choosesProxy(settings));
        Files.write(settings.toPath(), "codename1.arg.java.version=17\n".getBytes("UTF-8"));
        assertFalse(BackendWebApp.choosesProxy(settings));
        Files.write(settings.toPath(), "codename1.arg.javascript.inject_proxy=true\n".getBytes("UTF-8"));
        assertTrue(BackendWebApp.choosesProxy(settings));
        Files.write(settings.toPath(),
                "codename1.arg.javascript.proxy.url=https://p.example/?u=\n".getBytes("UTF-8"));
        assertTrue(BackendWebApp.choosesProxy(settings));
    }

    @Test
    void doesNotAnswerForAServerThatSaysWhereItsAppIs() throws Exception {
        File module = tmp.resolve("backend").toFile();
        module.mkdirs();
        Map<String, String> none = Collections.emptyMap();
        assertFalse(BackendWebApp.rootIsConfigured(module, none, null));
        assertFalse(BackendWebApp.rootIsConfigured(module, none, "-Xmx1g -Dcn1.webapp.path=/app"));
        assertTrue(BackendWebApp.rootIsConfigured(module, none, "-Xmx1g -Dcn1.webapp.root=/srv/app"));
        Map<String, String> env = new HashMap<String, String>();
        env.put("CN1_WEBAPP_ROOT", "/srv/app");
        assertTrue(BackendWebApp.rootIsConfigured(module, env, null));
        Files.write(new File(module, "application.properties").toPath(),
                "server.port=8081\n".getBytes("UTF-8"));
        assertFalse(BackendWebApp.rootIsConfigured(module, none, null));
        Files.write(new File(module, "application-dev.properties").toPath(),
                "cn1.webapp.root=../site\n".getBytes("UTF-8"));
        assertTrue(BackendWebApp.rootIsConfigured(module, none, null));
    }

    @Test
    void copiesAStagedAppBesideABinaryPackagedElsewhere() throws Exception {
        File out = tmp.resolve("webapp").toFile();
        webApp.stage(zip(tmp.resolve("app.zip").toFile(),
                "index.html", "x", "translated_app.js", script()), out);
        File beside = tmp.resolve("dist/webapp").toFile();
        assertEquals(2, webApp.copy(out, beside));
        assertEquals(script(), gunzip(new File(beside, "translated_app.js.gz")));
        assertEquals(new File(out, "translated_app.js").lastModified(),
                new File(beside, "translated_app.js").lastModified());
        assertEquals(0, webApp.copy(out, out));
        assertEquals("x", read(new File(out, "index.html")));
    }
}
