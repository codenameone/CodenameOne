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
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class JavaScriptProxyPackagerTest {
    private File work;
    private File app;
    private final List<String> log = new ArrayList<String>();

    @Before
    public void setUp() throws Exception {
        work = Files.createTempDirectory("cn1-js-proxy-test").toFile();
        app = new File(work, "app");
        app.mkdirs();
        Files.write(new File(app, "index.html").toPath(),
                "<html><head></head><body>app</body></html>".getBytes(StandardCharsets.UTF_8));
        Files.write(new File(app, "worker.js").toPath(), "worker".getBytes(StandardCharsets.UTF_8));
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(work);
    }

    @Test
    public void defaultBuildProducesModernWar() throws Exception {
        BuildRequest request = new BuildRequest();
        File output = packageProxy(request);
        assertTrue(output.getName().endsWith("-jakarta-servlet.war"));
        assertZipContains(output, "WEB-INF/web.xml");
        assertZipContains(output, "WEB-INF/classes/com/codename1/corsproxy/CORSProxy.class");
        assertZipContains(output, "worker.js");
        assertTrue(log.toString().contains("arbitrary HTTP(S) targets"));
        assertTrue(readIndex().contains("/cn1-cors-proxy?_target="));
    }

    @Test
    public void nodeBuildContainsRuntimeAndAllowlist() throws Exception {
        BuildRequest request = new BuildRequest();
        request.putArgument("javascript.proxy.target", "node");
        request.putArgument("javascript.proxy.allowedTargets", "https://api.example.com,*.example.org");
        File output = packageProxy(request);
        assertZipContains(output, "server.mjs");
        assertZipContains(output, "proxy-core.mjs");
        assertZipContains(output, "public/index.html");
        assertZipTextContains(output, "proxy-core.mjs", "api.example.com");
        assertFalse(log.toString().contains("arbitrary HTTP(S) targets"));
    }

    @Test
    public void generatesEveryDocumentedDeploymentProfile() throws Exception {
        String[][] profiles = {
            {"javax-servlet", "WEB-INF/web.xml"},
            {"php", "cn1-cors-proxy.php"},
            {"aws-lambda", "template.yaml"},
            {"google-cloud-functions", "index.js"},
            {"cloudflare-workers", "wrangler.jsonc"}
        };
        for (String[] profile : profiles) {
            BuildRequest request = new BuildRequest();
            request.putArgument("javascript.proxy.target", profile[0]);
            File output = packageProxy(request);
            assertNotNull(profile[0], output);
            assertZipContains(output, profile[1]);
        }
    }

    @Test
    public void proxyGenerationCanBeDisabled() throws Exception {
        BuildRequest request = new BuildRequest();
        request.putArgument("javascript.inject_proxy", "false");

        assertNull(packageProxy(request));
        assertFalse(readIndex().contains("cn1CORSProxyURL"));
    }

    @Test
    public void externalProxySuppressesBundleUnlessTargetIsExplicit() throws Exception {
        BuildRequest request = new BuildRequest();
        request.putArgument("javascript.proxy.url", "https://proxy.example/cn1?_target=");
        assertNull(packageProxy(request));
        assertTrue(readIndex().contains("https://proxy.example/cn1?_target="));
    }

    @Test
    public void replacesThePortTemplatePlaceholder() throws Exception {
        Files.write(new File(app, "index.html").toPath(),
                "<html><head><script>//INJECT-DEFAULT-PROXY\n"
                        .concat("//window.cn1CORSProxyURL='/example?_target=';</script></head></html>")
                        .getBytes(StandardCharsets.UTF_8));

        packageProxy(new BuildRequest());

        String index = readIndex();
        assertTrue(index.contains("window.cn1CORSProxyURL='/cn1-cors-proxy?_target=';"));
        assertFalse(index.contains("//INJECT-DEFAULT-PROXY"));
    }

    @Test(expected = java.io.IOException.class)
    public void invalidTargetFails() throws Exception {
        BuildRequest request = new BuildRequest();
        request.putArgument("javascript.proxy.target", "unknown");
        packageProxy(request);
    }

    private File packageProxy(BuildRequest request) throws Exception {
        return JavaScriptProxyPackager.packageProxy(app, work, "TestApp", request,
                new JavaScriptProxyPackager.Logger() {
                    public void log(String message) {
                        log.add(message);
                    }
                });
    }

    private String readIndex() throws Exception {
        return new String(Files.readAllBytes(new File(app, "index.html").toPath()), StandardCharsets.UTF_8);
    }

    private static void assertZipContains(File zip, String name) throws Exception {
        assertNotNull("Missing " + name + " in " + zip, zipEntry(zip, name));
    }

    private static void assertZipTextContains(File zip, String name, String text) throws Exception {
        byte[] bytes = zipEntry(zip, name);
        assertNotNull(bytes);
        assertTrue(new String(bytes, StandardCharsets.UTF_8).contains(text));
    }

    private static byte[] zipEntry(File zip, String name) throws Exception {
        ZipInputStream in = new ZipInputStream(new FileInputStream(zip));
        try {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (!name.equals(entry.getName())) continue;
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                return out.toByteArray();
            }
            return null;
        } finally {
            in.close();
        }
    }
}
