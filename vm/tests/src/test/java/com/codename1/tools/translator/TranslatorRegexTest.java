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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The translator's text passes run on its own regex engine
 * ({@code com.codename1.tools.translator.regex}), which has to agree with
 * {@code java.util.regex} exactly: a disagreement is a different peephole rewrite
 * and therefore different generated JavaScript. Every operation the translator
 * uses is compared against the JDK, over the syntax the engine supports and over
 * random inputs drawn from the alphabet those patterns care about.
 */
class TranslatorRegexTest {

    private static final String[] PATTERNS = {
        // The shapes of the translator's own peepholes.
        "stack\\.p\\(([a-zA-Z_\\$][\\w\\$]*(?:\\[\\d+\\])*(?:\\[\"[\\w\\$]+\"\\])*)\\);?\\s*stack\\.p\\(stack\\.q\\(\\)\\[\"([\\w\\$]+)\"\\]\\)",
        "stack\\.p\\(([^;(){},]+)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*\\{\\s*let b = stack\\.q\\(\\);",
        "stack\\.p\\(((?:(?!stack\\.q\\()[^;{}()]|\\([^()]*\\))+)\\);",
        "(\\s+s(\\d+) = )((?:(?!yield)[^;])+);\\s+s\\2 = s\\2(\\[\"[\\w\\$]+\"\\]);",
        "\\s+s(\\d+) = ([^;]+);\\s+return s\\1;",
        "(\\s)([\\w\\$]+) = ([\\w\\$]+);\\s+\\3 = \\2;",
        "\\s*let\\s+S\\s*=\\s*\\[\\s*\\]\\s*;|\\s*let\\s+pc\\s*=\\s*0\\s*;|\\s+",
        "function\\*?\\s+(cn1_[A-Za-z0-9_]+)\\s*\\(",
        "[\"'](cn1_[A-Za-z0-9_]+)[\"']",
        "\\d+",
        // General syntax.
        "a*", "a+b?", "(a|ab)(c|bcd)(d*)", "(?:x|y)+z", "^ab", "ab$", "[^a-c]+", "[\\s\\d]+",
        "(a*)+b", "((a)|b)+", "(a?)+", "a{2}", "a{1,3}b", "a{2,}", "\\W+", "\\S\\D", ".+", "x.y",
        "(?=ab)a", "(?!ab)a.", "(a)(b)?\\2", "\\(\\)\\[\\]\\{\\}\\.\\*\\+\\?\\|\\^\\$",
    };

    private static final String ALPHABET = "abcdxyz \t\n;(){}[]\".$_0129sSp=lt*";

    private static String random(Random r, int max) {
        int n = r.nextInt(max);
        StringBuilder b = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            b.append(ALPHABET.charAt(r.nextInt(ALPHABET.length())));
        }
        return b.toString();
    }

    private static final String[] FIXED_INPUTS = {
        "", "a", "aaa", "ab", "abc", "abcd", "xyxyz", "ab\n", "ab\r\n", "ab\r", "\r\n",
        "stack.p(locals[1]); stack.p(stack.q()[\"cn1_x\"])",
        "stack.p(a); stack.p(b); { let b = stack.q();",
        "stack.p(foo(bar)); stack.p(stack.q());",
        "  s3 = a.b(c);\n  s3 = s3[\"f\"];",
        "\n  s12 = x + 1;\n  return s12;",
        " a = b;\n b = a;",
        "  let S = [];  let pc = 0;   ",
        "function* cn1_foo_bar(x) function cn1_q (",
        "'cn1_a' \"cn1_b\" `cn1_c`",
    };

    private static List<String> jdk(String regex, String input) {
        List<String> out = new ArrayList<String>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(input);
        while (m.find()) {
            StringBuilder b = new StringBuilder(m.start() + "-" + m.end());
            for (int g = 1; g <= m.groupCount(); g++) {
                b.append('|').append(m.group(g)).append('@').append(m.start(g));
            }
            out.add(b.toString());
        }
        out.add("matches=" + java.util.regex.Pattern.compile(regex).matcher(input).matches());
        out.add("lookingAt=" + java.util.regex.Pattern.compile(regex).matcher(input).lookingAt());
        out.add("split=" + Arrays.toString(input.split(regex)));
        StringBuilder repl = new StringBuilder("<$0>");
        for (int g = 1; g <= java.util.regex.Pattern.compile(regex).matcher("").groupCount(); g++) {
            repl.append('$').append(g).append("\\$");
        }
        out.add("replaceAll=" + input.replaceAll(regex, repl.toString()));
        return out;
    }

    private static List<String> ours(String regex, String input) {
        List<String> out = new ArrayList<String>();
        com.codename1.tools.translator.regex.Matcher m =
                com.codename1.tools.translator.regex.Pattern.compile(regex).matcher(input);
        while (m.find()) {
            StringBuilder b = new StringBuilder(m.start() + "-" + m.end());
            for (int g = 1; g <= m.groupCount(); g++) {
                b.append('|').append(m.group(g)).append('@').append(m.start(g));
            }
            out.add(b.toString());
        }
        out.add("matches=" + com.codename1.tools.translator.regex.Pattern.compile(regex).matcher(input).matches());
        out.add("lookingAt=" + com.codename1.tools.translator.regex.Pattern.compile(regex).matcher(input).lookingAt());
        out.add("split=" + Arrays.toString(com.codename1.tools.translator.regex.Pattern.compile(regex).split(input)));
        StringBuilder repl = new StringBuilder("<$0>");
        for (int g = 1; g <= com.codename1.tools.translator.regex.Pattern.compile(regex).matcher("").groupCount(); g++) {
            repl.append('$').append(g).append("\\$");
        }
        out.add("replaceAll=" + com.codename1.tools.translator.regex.Pattern.replaceAll(input, regex, repl.toString()));
        return out;
    }

    @Test
    void agreesWithTheJdkOnFixedInputs() {
        for (String regex : PATTERNS) {
            for (String input : FIXED_INPUTS) {
                assertEquals(jdk(regex, input), ours(regex, input), "/" + regex + "/ on <" + input + ">");
            }
        }
    }

    @Test
    void agreesWithTheJdkOnRandomInputs() {
        Random r = new Random(5);
        for (String regex : PATTERNS) {
            for (int i = 0; i < 300; i++) {
                String input = random(r, 40);
                assertEquals(jdk(regex, input), ours(regex, input), "/" + regex + "/ on <" + input + ">");
            }
        }
    }

    @Test
    void regionsAnchorAndBoundLikeTheJdk() {
        String text = "xx  let S = [];  let pc = 0;  L[3] = foo;";
        for (String regex : new String[] {"\\s*let\\s+S\\s*=\\s*\\[\\s*\\]\\s*;", "L\\[(\\d+)\\]\\s*=\\s*([^;]+);", "^\\s+", "\\w+$"}) {
            for (int start = 0; start < text.length(); start += 3) {
                java.util.regex.Matcher j = java.util.regex.Pattern.compile(regex).matcher(text).region(start, text.length());
                com.codename1.tools.translator.regex.Matcher o =
                        com.codename1.tools.translator.regex.Pattern.compile(regex).matcher(text).region(start, text.length());
                boolean jl = j.lookingAt();
                assertEquals(jl, o.lookingAt(), "/" + regex + "/ lookingAt from " + start);
                if (jl) {
                    assertEquals(j.end(), o.end(), "/" + regex + "/ end from " + start);
                }
            }
        }
    }

    @Test
    void quoteReplacementAndAppendMatchTheJdk() {
        String q = "a$1\\b";
        assertEquals(java.util.regex.Matcher.quoteReplacement(q),
                com.codename1.tools.translator.regex.Matcher.quoteReplacement(q));
        StringBuffer jb = new StringBuffer();
        java.util.regex.Matcher j = java.util.regex.Pattern.compile("\"(cn1_[a-z]+)\"").matcher("x \"cn1_a\" y \"cn1_b\" z");
        StringBuffer ob = new StringBuffer();
        com.codename1.tools.translator.regex.Matcher o =
                com.codename1.tools.translator.regex.Pattern.compile("\"(cn1_[a-z]+)\"").matcher("x \"cn1_a\" y \"cn1_b\" z");
        while (j.find()) {
            assertTrue(o.find());
            j.appendReplacement(jb, java.util.regex.Matcher.quoteReplacement("$" + j.group(1)));
            o.appendReplacement(ob, com.codename1.tools.translator.regex.Matcher.quoteReplacement("$" + o.group(1)));
        }
        assertFalse(o.find());
        j.appendTail(jb);
        o.appendTail(ob);
        assertEquals(jb.toString(), ob.toString());
    }

    @Test
    void rejectsSyntaxItDoesNotImplement() {
        for (String regex : new String[] {"a*?", "a++", "(?<n>a)", "[a[b]]", "\\p{L}", "(?<=a)b"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> com.codename1.tools.translator.regex.Pattern.compile(regex), regex);
        }
    }
}
