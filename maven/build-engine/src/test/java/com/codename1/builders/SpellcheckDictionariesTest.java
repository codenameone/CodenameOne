/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The build adds dictionaries exactly to applications that name SpellChecker.
class SpellcheckDictionariesTest {
    @TempDir
    File dir;

    private static final class TestExecutor extends Executor {
        @Override
        protected String getDeviceIdCode() {
            return "";
        }

        @Override
        protected String generatePeerComponentCreationCode(String methodCallString) {
            return "";
        }

        @Override
        protected String convertPeerComponentToNative(String param) {
            return "";
        }

        @Override
        public boolean build(File sourceZip, BuildRequest request) {
            return true;
        }
    }

    private File jar(String name, String... entries) throws Exception {
        File f = new File(dir, name);
        try (ZipOutputStream z = new ZipOutputStream(new FileOutputStream(f))) {
            for (int i = 0; i < entries.length; i += 2) {
                z.putNextEntry(new ZipEntry(entries[i]));
                z.write(entries[i + 1].getBytes(StandardCharsets.ISO_8859_1));
                z.closeEntry();
            }
        }
        return f;
    }

    private static String classWith(String name) {
        return "\u00ca\u00fe\u00ba\u00be\u0000\u0001" + name + "\u0007";
    }

    @Test
    void anAppThatNeverNamesSpellCheckerIsLeftAlone() throws Exception {
        File j = jar("a.jar", "app/Main.class", classWith("com/codename1/wordprocessing/DocumentView"),
                "com/codename1/spellcheck/SpellChecker.class", classWith("com/codename1/spellcheck/SpellChecker"),
                "app/Other.class", classWith("com/codename1/spellcheck/SpellCheckerish"));
        assertSame(j, SpellcheckDictionaries.inject(new TestExecutor(), j, new BuildRequest()));
    }

    @Test
    void addsTheHintedLanguagesAtTheRoot() throws Exception {
        File j = jar("b.jar", "app/Main.class", classWith("Lcom/codename1/spellcheck/SpellChecker;"),
                "cn1dict_en_CA.dic", "mine");
        BuildRequest r = new BuildRequest();
        r.putArgument("spellcheck.languages", "en-US, en_CA");
        File out = SpellcheckDictionaries.inject(new TestExecutor(), j, r);
        try (ZipFile z = new ZipFile(out)) {
            assertNotNull(z.getEntry("app/Main.class"));
            assertTrue(z.getEntry("cn1dict_en_US.dic").getSize() > 100000);
            assertNotNull(z.getEntry("cn1dict_en_US.aff"));
            assertNotNull(z.getEntry("cn1dict_en_CA.aff"));
            // the application's own file wins
            assertEquals(4, z.getEntry("cn1dict_en_CA.dic").getSize());
            assertEquals("en_US\nen_CA\n", new String(readAll(z, "cn1dict_languages.txt"),
                    StandardCharsets.UTF_8));
        }
    }

    @Test
    void anUnknownLanguageFailsTheBuildAndTheHintCanTurnItOff() throws Exception {
        File j = jar("c.jar", "app/Main.class", classWith("com/codename1/spellcheck/SpellChecker"));
        BuildRequest r = new BuildRequest();
        r.putArgument("spellcheck.languages", "xx_XX");
        BuildException e = assertThrows(BuildException.class,
                () -> SpellcheckDictionaries.inject(new TestExecutor(), j, r));
        assertTrue(e.getMessage().contains("en_US"), e.getMessage());
        r.putArgument("spellcheck.dictionaries", "false");
        assertSame(j, SpellcheckDictionaries.inject(new TestExecutor(), j, r));
    }

    @Test
    void languagesAreNormalized() {
        assertEquals(Arrays.asList("en_US", "en_AU"), SpellcheckDictionaries.languages(" en-US,,en_AU,en_US"));
        assertEquals(Arrays.asList("en_US"), SpellcheckDictionaries.languages(" "));
    }

    private static byte[] readAll(ZipFile z, String name) throws Exception {
        try (java.io.InputStream in = z.getInputStream(z.getEntry(name))) {
            java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            for (int n = in.read(buf); n >= 0; n = in.read(buf)) {
                b.write(buf, 0, n);
            }
            return b.toByteArray();
        }
    }
}
