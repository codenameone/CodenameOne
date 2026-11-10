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

import com.codename1.build.shared.PlatformFeatureCatalog;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The whisper.cpp and llama.cpp natives: the published versions, how an
/// application comes to fetch them, and how the iOS builder fetches them.
class AiNativesCatalogTest {

    /// `scripts/ai-natives/versions.properties` decides what the workflow
    /// publishes and the catalog decides what builders fetch; a bump to one
    /// alone makes every build that uses the library fail on a missing artifact.
    @Test
    void publishedVersionsMatchTheCatalog() throws IOException {
        Properties p = new Properties();
        InputStream in = new FileInputStream(versionsFile());
        try {
            p.load(in);
        } finally {
            in.close();
        }
        assertEquals(version(p, "whisper"), PlatformFeatureCatalog.WHISPER_NATIVES_VERSION);
        assertEquals(version(p, "llama"), PlatformFeatureCatalog.LLAMA_NATIVES_VERSION);
    }

    @Test
    void untouchedAppFetchesNothing() {
        PlatformFeatureCatalog.Accumulator acc = new PlatformFeatureCatalog.Accumulator();
        acc.consume("com/example/Main");
        acc.consume("com/codename1/ai/LlmClient");
        acc.consumeMethod("com/codename1/ai/LlmClient", "openAi");
        assertTrue(acc.iosXcframeworks().isEmpty());
        assertTrue(android(acc).indexOf("com.codenameone.natives") < 0);
    }

    @Test
    void localLlmCallFetchesLlamaOnly() {
        PlatformFeatureCatalog.Accumulator acc = new PlatformFeatureCatalog.Accumulator();
        // What the scanner reports for `LlmClient.local(path)`: the class
        // reference and the call.
        acc.consume("com/codename1/ai/LlmClient");
        acc.consumeMethod("com/codename1/ai/LlmClient", "local");
        assertEquals(1, acc.iosXcframeworks().size());
        assertTrue(acc.iosXcframeworks().iterator().next().indexOf(":cn1-llama-ios:") > 0);
        assertTrue(android(acc).indexOf(":cn1-llama-android:") > 0);
        assertTrue(android(acc).indexOf("whisper") < 0);
    }

    @Test
    void whisperCn1libFetchesWhisperOnly() {
        PlatformFeatureCatalog.Accumulator acc = new PlatformFeatureCatalog.Accumulator();
        acc.consume("com/codename1/ai/whisper/WhisperRecognizer");
        assertEquals(1, acc.iosXcframeworks().size());
        assertTrue(acc.iosXcframeworks().iterator().next().indexOf(":cn1-whisper-ios:") > 0);
        assertTrue(android(acc).indexOf("llama") < 0);
    }

    @Test
    void zipUrlFollowsMavenLayout() {
        assertEquals("https://r/m2/com/codenameone/natives/cn1-llama-ios/0.9.0-1/cn1-llama-ios-0.9.0-1.zip",
                CatalogNatives.zipUrl("https://r/m2/", "com.codenameone.natives:cn1-llama-ios:0.9.0-1"));
        assertThrows(IllegalArgumentException.class,
                () -> CatalogNatives.zipUrl("https://r", "a:b"));
    }

    @Test
    void parsesBareAndSha256sumDigests() {
        String d = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        assertEquals(d, CatalogNatives.parseSha256(d + "\n"));
        assertEquals(d, CatalogNatives.parseSha256(d + "  cn1-llama-ios-0.9.0-1.zip\n"));
        assertNull(CatalogNatives.parseSha256("not a digest"));
    }

    @Test
    void fetchVerifiesTheChecksum() throws IOException {
        File repo = Files.createTempDirectory("cn1natives").toFile();
        File dir = new File(repo, "g/a/1");
        assertTrue(dir.mkdirs());
        byte[] zip = {'P', 'K', 3, 4};
        write(new File(dir, "a-1.zip"), zip);
        write(new File(dir, "a-1.zip.sha256"), CatalogNatives.sha256(zip).getBytes("UTF-8"));
        File out = Files.createTempDirectory("cn1out").toFile();
        File got = CatalogNatives.fetchZip(repo.toURI().toString(), "g:a:1", out);
        assertEquals(4, got.length());

        write(new File(dir, "a-1.zip.sha256"), CatalogNatives.sha256(new byte[1]).getBytes("UTF-8"));
        assertThrows(IOException.class,
                () -> CatalogNatives.fetchZip(repo.toURI().toString(), "g:a:1", out));
    }

    private static String android(PlatformFeatureCatalog.Accumulator acc) {
        StringBuilder sb = new StringBuilder();
        for (PlatformFeatureCatalog.Entry e : acc.hits()) {
            sb.append(e.androidGradleDeps()).append(' ');
        }
        return sb.toString();
    }

    private static String version(Properties p, String lib) {
        String tag = p.getProperty(lib + ".tag");
        return (tag.startsWith("v") ? tag.substring(1) : tag) + "-" + p.getProperty(lib + ".revision");
    }

    private static File versionsFile() {
        for (File d = new File("").getAbsoluteFile(); d != null; d = d.getParentFile()) {
            File f = new File(d, "scripts/ai-natives/versions.properties");
            if (f.isFile()) {
                return f;
            }
        }
        throw new IllegalStateException("scripts/ai-natives/versions.properties not found");
    }

    private static void write(File f, byte[] data) throws IOException {
        OutputStream out = new FileOutputStream(f);
        try {
            out.write(data);
        } finally {
            out.close();
        }
    }
}
