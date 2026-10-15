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

import com.codename1.io.JSONParser;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class JavaScriptPwaTest {
    @TempDir Path temp;

    private File bundle() throws Exception {
        Path app = temp.resolve("app");
        Files.createDirectories(app.resolve("assets"));
        put(app.resolve("index.html"), "<!doctype html><html><head><title>App</title></head><body>app</body></html>");
        put(app.resolve("worker.js"), "importScripts('translated_app.js', 'translated_app_1.js');");
        put(app.resolve("translated_app.js"), "self.appVersion = 'v1';");
        put(app.resolve("translated_app_1.js"), "self.chunkLoaded = true;");
        put(app.resolve("assets/font with spaces.ttf"), "font");
        put(app.resolve("assets/theme.res"), "theme-v1");
        Path portWorker = Paths.get("../../Ports/JavaScriptPort/src/main/webapp/sw.js");
        Files.copy(portWorker, app.resolve("sw.js"));
        return app.toFile();
    }

    private BuildRequest request() throws Exception {
        BuildRequest request = new BuildRequest();
        request.setDisplayName("Tom & Jerry's App");
        BufferedImage icon = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        icon.setRGB(0, 0, 0xff123456);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(icon, "png", bytes);
        request.setIcon(bytes.toByteArray());
        return request;
    }

    @Test void generatesInstallableMetadataAndActualIcons() throws Exception {
        File app = bundle();
        JavaScriptPwa.write(app, request());
        Map manifest = parse(new File(app, "manifest.json"));
        assertEquals("Tom & Jerry's App", manifest.get("name"));
        assertEquals("standalone", manifest.get("display"));
        assertEquals("./index.html", manifest.get("start_url"));
        assertEquals("./", manifest.get("scope"));
        assertFalse(manifest.containsKey("orientation"), "responsive apps must not be locked to portrait");
        Set<String> sizes = new HashSet<String>();
        for (Object item : (List) manifest.get("icons")) {
            Map icon = (Map) item;
            BufferedImage image = ImageIO.read(new File(app, (String) icon.get("src")));
            assertEquals(image.getWidth() + "x" + image.getHeight(), icon.get("sizes"));
            sizes.add((String) icon.get("sizes"));
        }
        assertTrue(sizes.containsAll(Arrays.asList("180x180", "192x192", "512x512")));
        String html = read(new File(app, "index.html"));
        assertTrue(html.contains("rel=\"manifest\" href=\"manifest.json\""));
        assertTrue(html.contains("apple-touch-icon"));
        assertTrue(html.contains("src=\"cn1-pwa.js\""));
    }

    @Test void orientationHintsAreExplicitAndManifestOverridesLegacy() throws Exception {
        File app = bundle();
        BuildRequest request = request();
        request.putArgument("javascript.orientation", "portrait-primary");
        JavaScriptPwa.write(app, request);
        assertEquals("portrait-primary", parse(new File(app, "manifest.json")).get("orientation"));
        request.putArgument("javascript.manifest.orientation", "landscape");
        JavaScriptPwa.write(app, request);
        assertEquals("landscape", parse(new File(app, "manifest.json")).get("orientation"));
    }

    @Test void preservesHintsAndStructuredManifestValues() throws Exception {
        File app = bundle();
        BuildRequest request = request();
        request.putArgument("javascript.manifest.name", "false");
        request.putArgument("javascript.manifest.description", "[A description]");
        request.putArgument("javascript.manifest.start_url", "./index.html?installed=1");
        request.putArgument("javascript.manifest.prefer_related_applications", "false");
        request.putArgument("javascript.manifest.related_applications", "[{\"platform\":\"play\",\"id\":\"com.example.app\"}]");
        request.putArgument("javascript.manifest.shortcuts", "[{\"name\":\"Open\",\"url\":\"./index.html?open=1\"}]");
        JavaScriptPwa.write(app, request);
        Map manifest = parse(new File(app, "manifest.json"));
        assertEquals("false", manifest.get("name"));
        assertEquals("[A description]", manifest.get("description"));
        assertEquals(Boolean.FALSE, manifest.get("prefer_related_applications"));
        assertEquals("com.example.app", ((Map)((List)manifest.get("related_applications")).get(0)).get("id"));
        assertTrue(manifest.get("shortcuts") instanceof List);
    }

    @Test void cachesFinalResourcesPreservesPushAndVersionsContentRatherThanAppVersion() throws Exception {
        File app = bundle();
        BuildRequest request = request();
        JavaScriptPwa.write(app, request);
        String first = read(new File(app, "sw.js"));
        String html = read(new File(app, "index.html"));
        assertTrue(first.contains("addEventListener('push'"));
        assertTrue(first.contains("addEventListener('notificationclick'"));
        assertFalse(first.contains("upup-cache"));
        assertFalse(first.contains("teavm/classes.js"));
        for (String name : Arrays.asList("worker.js", "translated_app_1.js", "assets/theme.res", "assets/font%20with%20spaces.ttf")) {
            assertTrue(first.contains(name), name);
        }
        JavaScriptPwa.write(app, request);
        assertEquals(first, read(new File(app, "sw.js")), "identical builds must not force another update");
        assertEquals(html, read(new File(app, "index.html")), "no duplicate manifest or registration tags");
        put(new File(app, "assets/theme.res").toPath(), "theme-v2");
        JavaScriptPwa.write(app, request);
        assertNotEquals(first, read(new File(app, "sw.js")), "resource-only changes must invalidate the cache");
    }

    @Test void proxyDisabledAndPackagedBuildsBothContainPwa() throws Exception {
        File app = bundle();
        BuildRequest request = request();
        request.putArgument("javascript.manifest.start_url", "./dashboard?installed=1");
        request.putArgument("javascript.inject_proxy", "false");
        assertNull(JavaScriptProxyPackager.packageProxy(app, temp.toFile(), "App", request, message -> {}));
        assertEquals("standalone", parse(new File(app, "manifest.json")).get("display"));
        request.putArgument("javascript.inject_proxy", "true");
        request.putArgument("javascript.proxy.target", "node");
        File zip = JavaScriptProxyPackager.packageProxy(app, temp.toFile(), "App", request, message -> {});
        try (java.util.zip.ZipFile archive = new java.util.zip.ZipFile(zip)) {
            assertNotNull(archive.getEntry("public/manifest.json"));
            assertNotNull(archive.getEntry("public/sw.js"));
            assertNotNull(archive.getEntry("public/_icons/icon-512.png"));
        }
        assertTrue(read(new File(app, "sw.js")).contains("./dashboard?installed=1"));
        // Optional local browser proof uses the real packaged output from this test.
        String fixture = System.getProperty("cn1.pwa.fixture");
        if (fixture != null) {
            Path dest = Paths.get(fixture);
            Files.createDirectories(dest);
            try (java.util.stream.Stream<Path> paths = Files.walk(app.toPath())) {
                for (Path path : (Iterable<Path>) paths::iterator) {
                    Path target = dest.resolve(app.toPath().relativize(path));
                    if (Files.isDirectory(path)) Files.createDirectories(target);
                    else Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static Map parse(File file) throws Exception {
        JSONParser parser = new JSONParser();
        parser.setUseBooleanInstance(true);
        return parser.parseJSON(new StringReader(read(file)));
    }
    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
    private static void put(Path file, String value) throws Exception {
        Files.write(file, value.getBytes(StandardCharsets.UTF_8));
    }
}
