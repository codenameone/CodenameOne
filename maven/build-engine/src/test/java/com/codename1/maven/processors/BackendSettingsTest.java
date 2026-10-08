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
package com.codename1.maven.processors;

import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/// The settings the build reads to decide what is linked, held to the list the server
/// checks when it starts. A key the build acts on and the server knows nothing about is a
/// key that does nothing, silently, wherever the build does not read it.
public class BackendSettingsTest {

    private static File processors() {
        File dir = new File("src/main/java/com/codename1/maven/processors");
        assertTrue("run from the build-engine module: " + dir.getAbsolutePath(), dir.isDirectory());
        return dir;
    }

    private static String read(File f) throws Exception {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void theServerRefusesEveryKeyTheBuildLinksBy() throws Exception {
        Class<?> runtime = Class.forName("com.codename1.backend.BuildTimeSettings");
        Method keys = runtime.getDeclaredMethod("keys");
        keys.setAccessible(true);
        String[] known = (String[]) keys.invoke(null);

        assertEquals("the build links by a key the server has no rule for, or the server "
                + "checks one the build no longer reads",
                new TreeSet<String>(Arrays.asList(BackendSettings.BUILD_TIME_KEYS)),
                new TreeSet<String>(Arrays.asList(known)));
        assertEquals(BackendSettings.BUILD_TIME_KEYS.length,
                new TreeSet<String>(Arrays.asList(BackendSettings.BUILD_TIME_KEYS)).size());
    }

    @Test
    public void everyTruthTheBuildReadsIsOneOfTheListedKeys() throws Exception {
        // applicationPropertyTrue is how the build asks whether a properties file turns
        // something on. Each call has to name a listed key by its constant, so a new
        // one cannot be read without being listed -- and so checked by the server.
        List<String> allowed = Arrays.asList("MANAGEMENT_KEY", "SECURITY_SCHEMA_KEY", "MCP_KEY",
                "OTEL_ENABLED_PROPERTY");
        Pattern call = Pattern.compile("applicationPropertyTrue\\(\\s*ctx\\s*,\\s*([^)]*?)\\s*\\)");
        List<String> seen = new ArrayList<String>();
        File[] sources = processors().listFiles();
        assertNotNull(sources);
        for (File f : sources) {
            if (!f.getName().endsWith(".java")) {
                continue;
            }
            Matcher m = call.matcher(read(f));
            while (m.find()) {
                String argument = m.group(1);
                seen.add(argument);
                assertTrue(f.getName() + " reads applicationPropertyTrue(ctx, " + argument
                        + "): a key that links something must be one of BackendSettings."
                        + "BUILD_TIME_KEYS, named by its constant",
                        allowed.contains(argument));
            }
        }
        assertTrue("the scan found too few calls to mean anything: " + seen, seen.size() >= 4);
        assertTrue(seen.toString(), seen.containsAll(allowed));
        assertEquals("cn1.otel.enabled", RestControllerAnnotationProcessor.OTEL_ENABLED_PROPERTY);
    }

    @Test
    public void noOtherFileSpellsAKeyOut() throws Exception {
        File[] sources = processors().listFiles();
        assertNotNull(sources);
        int scanned = 0;
        for (File f : sources) {
            if (!f.getName().endsWith(".java") || "BackendSettings.java".equals(f.getName())) {
                continue;
            }
            scanned++;
            String text = read(f);
            for (String key : BackendSettings.BUILD_TIME_KEYS) {
                assertFalse(f.getName() + " spells out \"" + key + "\"; read it through "
                        + "BackendSettings so the list of build-time keys stays whole",
                        text.contains("\"" + key + "\""));
            }
            for (String key : BackendSettings.MCP_LINKING_KEYS) {
                assertFalse(f.getName() + " spells out \"" + key + "\"",
                        text.contains("\"" + key + "\""));
            }
        }
        assertTrue(scanned > 10);
    }
}
