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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// NativeTokenIndex is ReachabilityCull's cheaper copy of NativeSymbolIndex, and the cull's
/// output depends on the two answering every question the same way. Checked against the
/// runtime's real native sources -- the text the perf gate's translation indexes, without
/// the SQLite amalgamation, whose 13 MB would only make the reference index slow -- with
/// hits, near misses and substrings, and with queries both above and below the length
/// hint, whose shorter tokens never enter the automaton.
class NativeTokenIndexTest {

    @Test
    void answersExactlyAsNativeSymbolIndexOverTheRuntimeNatives() throws IOException {
        String[] texts = runtimeNatives();
        NativeSymbolIndex reference = new NativeSymbolIndex(texts);
        List<String> queries = queries(texts);
        for (int minQuery : new int[] {1, 12, 40}) {
            NativeTokenIndex index = new NativeTokenIndex(texts, minQuery);
            int hits = 0;
            int below = 0;
            for (String q : queries) {
                boolean expected = reference.contains(q);
                assertEquals(expected, index.contains(q), "minQuery " + minQuery + ": " + q);
                if (expected) {
                    hits++;
                }
                if (q.length() < minQuery) {
                    below++;
                }
            }
            // Both answers have to be exercised, and so does the short-token path.
            assertTrue(hits > 1000 && hits < queries.size() - 1000, "hits " + hits + " of " + queries.size());
            if (minQuery > 1) {
                assertTrue(below > 1000, "queries below the hint: " + below);
            }
        }
    }

    @Test
    void aQueryNeverSpansTwoTokens() {
        NativeTokenIndex index = new NativeTokenIndex(new String[] {"alpha beta(gamma)", "delta"}, 1);
        assertTrue(index.contains("pha"));
        assertTrue(index.contains("gamma"));
        assertTrue(index.contains("delta"));
        assertFalse(index.contains("alphabeta"));
        assertFalse(index.contains("betagamma"));
        assertFalse(index.contains("gammadelta"));
        // Below the hint: the same answers from the short-token scan.
        NativeTokenIndex hinted = new NativeTokenIndex(new String[] {"alpha beta(gamma)", "delta"}, 10);
        assertTrue(hinted.contains("pha"));
        assertTrue(hinted.contains("et"));
        assertFalse(hinted.contains("abeta"));
    }

    private static String[] runtimeNatives() throws IOException {
        Path src = Paths.get("..", "ByteCodeTranslator", "src").toAbsolutePath().normalize();
        List<String> out = new ArrayList<String>();
        try (Stream<Path> files = Files.list(src)) {
            for (Object o : files.sorted().toArray()) {
                Path p = (Path) o;
                String n = p.getFileName().toString();
                if ((n.endsWith(".m") || n.endsWith(".c") || n.endsWith(".h")) && Files.size(p) < 2000000) {
                    out.add(new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
                }
            }
        }
        assertTrue(out.size() > 5, "runtime natives under " + src);
        return out.toArray(new String[0]);
    }

    /// A sample of the distinct tokens, a substring of each, and a near miss of each.
    private static List<String> queries(String[] texts) {
        Set<String> tokens = new LinkedHashSet<String>();
        Matcher m = Pattern.compile("[A-Za-z0-9_]+").matcher("");
        for (String t : texts) {
            m.reset(t);
            while (m.find()) {
                tokens.add(m.group());
            }
        }
        Random r = new Random(5);
        List<String> out = new ArrayList<String>();
        int n = 0;
        for (String t : tokens) {
            if (n++ % 3 != 0) {
                continue;
            }
            out.add(t);
            int a = r.nextInt(t.length());
            int b = a + 1 + r.nextInt(t.length() - a);
            out.add(t.substring(a, b));
            char c = t.charAt(r.nextInt(t.length()));
            out.add(t.replace(c, c == 'Q' ? 'Z' : 'Q') + "_");
        }
        return out;
    }
}
