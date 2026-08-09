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
package com.codename1.hardening;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** The dictionary is prefixed (no short names) and seed-dependent (reproducible renaming). */
public class Cn1NameFactoryTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void everyGeneratedNameIsPrefixedAndLongEnough() {
        for (int i = 0; i < 5000; i += 137) {
            String w = Cn1NameFactory.word(i);
            assertTrue(w, w.startsWith(Cn1NameFactory.PREFIX));
            assertTrue(w, w.length() >= 6);
            assertFalse("must not contain '_'", w.indexOf('_') >= 0);
            assertEquals("lower-case only", w.toLowerCase(), w);
        }
    }

    @Test
    public void dictionarySizeCoversTheLargestOfClassAndMemberScopes() {
        // Small app, tiny class scope: the 50000 floor applies.
        assertEquals(50000, Cn1NameFactory.dictionarySizeFor(100, 200));
        // Class-heavy jar: sized to the class scope with headroom.
        assertEquals(80000, Cn1NameFactory.dictionarySizeFor(20000, 200));
        // A single member-heavy class (e.g. a generated interface with 55000 same-descriptor methods)
        // must size the dictionary to the MEMBER scope, even though the class count is tiny -- otherwise
        // ProGuard exhausts it and falls back to short names, reintroducing the ParparVM cull pathology.
        assertTrue("dictionary must exceed the biggest class's member count",
                Cn1NameFactory.dictionarySizeFor(50, 55000) >= 55000);
        assertEquals(220000, Cn1NameFactory.dictionarySizeFor(50, 55000));
    }

    @Test
    public void differentSeedsProduceDifferentDictionariesButSameSeedReproduces() throws Exception {
        File a = tmp.newFile("a.txt");
        File b = tmp.newFile("b.txt");
        File a2 = tmp.newFile("a2.txt");
        Cn1NameFactory.writeDictionary(a, 100, 111);
        Cn1NameFactory.writeDictionary(b, 100, 222);
        Cn1NameFactory.writeDictionary(a2, 100, 111);
        String sa = new String(Files.readAllBytes(a.toPath()), Charset.forName("UTF-8"));
        String sb = new String(Files.readAllBytes(b.toPath()), Charset.forName("UTF-8"));
        String sa2 = new String(Files.readAllBytes(a2.toPath()), Charset.forName("UTF-8"));
        assertFalse("different seeds must produce different name assignments", sa.equals(sb));
        assertEquals("the same seed must reproduce the dictionary exactly", sa, sa2);
    }
}
