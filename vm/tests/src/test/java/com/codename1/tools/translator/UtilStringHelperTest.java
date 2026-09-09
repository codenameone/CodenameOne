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
package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Holds {@link Util}'s hand-written string helpers to the JDK regex behaviour they
 * replaced.
 *
 * The translator has to compile against ParparVM's JavaAPI in order to translate
 * itself, and String.split/replaceAll are not declared there -- they are among the
 * methods BytecodeComplianceMojo rewrites onto com.codename1.util.regex precisely
 * because JavaAPI lacks them. The call sites lost the regex rather than JavaAPI
 * gaining a second engine, so the risk is that a replacement quietly disagrees and
 * changes generated C. These tests compare against the originals directly, so the
 * JDK is the oracle rather than a hand-written expectation.
 */
class UtilStringHelperTest {

    private static final String LOCALS_REGEX = "locals\\[(\\d+)\\]\\.data\\.o";
    private static final String LOCALS_REPLACEMENT = "olocals_$1_";

    @Test
    void rewriteLocalObjectRefsMatchesReplaceAll() {
        for (String s : localsCases()) {
            assertEquals(s.replaceAll(LOCALS_REGEX, LOCALS_REPLACEMENT),
                    Util.rewriteLocalObjectRefs(s),
                    "rewriteLocalObjectRefs diverged on: " + s);
        }
    }

    @Test
    void collapseWhitespaceMatchesReplaceAll() {
        for (String s : whitespaceCases()) {
            assertEquals(s.replaceAll("\\s+", " "), Util.collapseWhitespace(s),
                    "collapseWhitespace diverged on: " + escape(s));
        }
    }

    @Test
    void splitWhitespaceMatchesSplit() {
        for (String s : whitespaceCases()) {
            assertArrayEquals(s.split("\\s+"), Util.splitWhitespace(s),
                    "splitWhitespace diverged on: " + escape(s));
        }
    }

    @Test
    void splitLiteralMatchesSplit() {
        String[] cases = {
            "", ";", ";;", "a", "a;b", "a;b;c", ";a", "a;", "a;;b", ";;a;;b;;",
            "a;b;", "a;b;;", "  a ; b  ", "one"
        };
        for (String s : cases) {
            assertArrayEquals(s.split(";"), Util.splitLiteral(s, ';'),
                    "splitLiteral diverged on: " + escape(s));
        }
    }

    /**
     * The generated-code shapes plus the near misses: a bracket with no digits, a
     * digit run that is not followed by ".data.o", and a nested occurrence. These are
     * where a hand-written scanner and a regex are most likely to part company.
     */
    private List<String> localsCases() {
        List<String> cases = new ArrayList<String>();
        for (String s : new String[]{
            "",
            "locals[0].data.o",
            "locals[12].data.o",
            "locals[0].data.o + locals[1].data.o",
            "f(locals[3].data.o, locals[44].data.o)",
            "locals[].data.o",
            "locals[x].data.o",
            "locals[0].data.i",
            "locals[0].data",
            "locals[",
            "locals[0",
            "locals[0]",
            "prefix locals[7].data.o suffix",
            "locals[locals[1].data.o].data.o",
            "no match here at all",
            "LOCALS[0].DATA.O"
        }) {
            cases.add(s);
        }
        // Randomised fuzz over the alphabet the pattern cares about, so the oracle
        // sees inputs nobody thought to enumerate.
        Random r = new Random(20260909L);
        char[] alphabet = {'l', 'o', 'c', 'a', 's', '[', ']', '.', 'd', 't', '0', '1', '9', ' ', 'x'};
        for (int i = 0; i < 3000; i++) {
            StringBuilder b = new StringBuilder();
            int len = r.nextInt(24);
            for (int j = 0; j < len; j++) {
                b.append(alphabet[r.nextInt(alphabet.length)]);
            }
            if (r.nextBoolean()) {
                b.append("locals[").append(r.nextInt(200)).append("].data.o");
            }
            cases.add(b.toString());
        }
        return cases;
    }

    private List<String> whitespaceCases() {
        List<String> cases = new ArrayList<String>();
        // 0x0B is the vertical tab: Java's \s includes it and Character.isWhitespace
        // does not, which is the difference most likely to be got wrong.
        String vt = String.valueOf((char) 0x0B);
        for (String s : new String[]{
            "", " ", "  ", "a", "a b", "a  b", " a b ", "\ta\tb\t", "a\nb",
            "a" + vt + "b", "a\fb", "a\r\nb", "JAVA_OBJECT  me", " leading", "trailing ",
            "  both  ", "a \t\n b"
        }) {
            cases.add(s);
        }
        Random r = new Random(20260910L);
        char[] alphabet = {' ', '\t', '\n', 0x0B, '\f', '\r', 'a', 'b', '*'};
        for (int i = 0; i < 3000; i++) {
            StringBuilder b = new StringBuilder();
            int len = r.nextInt(16);
            for (int j = 0; j < len; j++) {
                b.append(alphabet[r.nextInt(alphabet.length)]);
            }
            cases.add(b.toString());
        }
        return cases;
    }

    private String escape(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 0x20) {
                b.append("\\x").append(Integer.toHexString(c));
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }
}
