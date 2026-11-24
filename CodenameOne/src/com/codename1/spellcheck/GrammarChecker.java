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
package com.codename1.spellcheck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/// Checks text against a list of grammar and style rules. `english()` holds the rules English
/// text gets by default -- a repeated word, "a" against "an", a sentence that starts in lower
/// case, a space before punctuation, a doubled space -- and an application adds its own
/// `Rule`s or replaces the list.
///
/// ```java
/// GrammarChecker g = GrammarChecker.english();
/// for (ProofIssue i : g.check("This is is an test.")) {
///     // "Repeated word" over "is is", "Use a before a consonant sound" over "an"
/// }
/// ```
public final class GrammarChecker {
    /// One check over a text.
    public interface Rule {
        /// Adds the problems this rule finds in a text.
        ///
        /// #### Parameters
        ///
        /// - `text`: the text, typically a paragraph
        ///
        /// - `words`: its word ranges, from `SpellChecker.words`
        ///
        /// - `out`: where issues go
        void check(String text, List<int[]> words, List<ProofIssue> out);
    }

    /// A word written twice in a row.
    public static final Rule REPEATED_WORD = new RepeatedWord();

    /// "a" before a vowel sound and "an" before a consonant one.
    public static final Rule ARTICLE = new Article();

    /// A sentence that starts in lower case.
    public static final Rule CAPITALIZATION = new Capitalization();

    /// A space before a comma, period or semicolon, and two spaces in a row.
    public static final Rule SPACING = new Spacing();

    private final List<Rule> rules = new ArrayList<Rule>();

    /// A checker with no rules.
    public GrammarChecker() {
    }

    /// A checker with the given rules.
    ///
    /// #### Parameters
    ///
    /// - `rules`: the rules, in order
    public GrammarChecker(Rule... rules) {
        this.rules.addAll(Arrays.asList(rules));
    }

    /// A checker with the English rules.
    ///
    /// #### Returns
    ///
    /// a new checker
    public static GrammarChecker english() {
        return new GrammarChecker(REPEATED_WORD, ARTICLE, CAPITALIZATION, SPACING);
    }

    /// Adds a rule.
    ///
    /// #### Parameters
    ///
    /// - `r`: the rule
    public void addRule(Rule r) {
        rules.add(r);
    }

    /// Removes a rule.
    ///
    /// #### Parameters
    ///
    /// - `r`: the rule
    public void removeRule(Rule r) {
        rules.remove(r);
    }

    /// The rules, in order.
    ///
    /// #### Returns
    ///
    /// a copy of the list
    public List<Rule> getRules() {
        return new ArrayList<Rule>(rules);
    }

    /// The problems every rule finds in a text.
    ///
    /// #### Parameters
    ///
    /// - `text`: the text, typically a paragraph
    ///
    /// #### Returns
    ///
    /// the issues, rule by rule
    public List<ProofIssue> check(String text) {
        List<ProofIssue> out = new ArrayList<ProofIssue>();
        List<int[]> words = SpellChecker.words(text);
        for (Rule r : rules) {
            r.check(text, words, out);
        }
        return out;
    }

    static ProofIssue issue(int a, int b, String message, String fix) {
        List<String> s = new ArrayList<String>(1);
        s.add(fix);
        return new ProofIssue(a, b, ProofIssue.GRAMMAR, message, s);
    }

    static boolean alpha(String w) {
        for (int i = 0; i < w.length(); i++) {
            if (!SpellChecker.letter(w.charAt(i))) {
                return false;
            }
        }
        return w.length() > 0;
    }

    private static final class RepeatedWord implements Rule {
        @Override
        public void check(String text, List<int[]> ws, List<ProofIssue> out) {
            for (int k = 0; k + 1 < ws.size(); k++) {
                int[] x = ws.get(k);
                int[] y = ws.get(k + 1);
                String w0 = text.substring(x[0], x[1]);
                String w1 = text.substring(y[0], y[1]);
                String l0 = HunspellDictionary.lower(w0);
                if (text.substring(x[1], y[0]).trim().length() == 0 && w0.equalsIgnoreCase(w1)
                        && alpha(w0) && !"had".equals(l0) && !"that".equals(l0)
                        && !"bye".equals(l0) && !"so".equals(l0)) {
                    out.add(issue(x[0], y[1], "Repeated word: \"" + w1 + "\"", w0));
                }
            }
        }
    }

    private static final class Article implements Rule {
        private static final String[] CONSONANT_SOUND = {"uni", "use", "usu", "eu", "one", "once",
            "ubiq", "uti", "ure", "uro"};

        private static boolean consonantSound(String w) {
            String l = HunspellDictionary.lower(w);
            for (String s : CONSONANT_SOUND) {
                if (l.startsWith(s)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void check(String text, List<int[]> ws, List<ProofIssue> out) {
            for (int k = 0; k + 1 < ws.size(); k++) {
                int[] x = ws.get(k);
                int[] y = ws.get(k + 1);
                if (!" ".equals(text.substring(x[1], y[0]))) {
                    continue;
                }
                String w0 = text.substring(x[0], x[1]);
                String w1 = text.substring(y[0], y[1]);
                char c1 = w1.charAt(0);
                boolean vowel = "aeiouAEIOU".indexOf(c1) >= 0;
                if (("a".equals(w0) || "A".equals(w0)) && vowel && !consonantSound(w1)) {
                    out.add(issue(x[0], x[1], "Use \"an\" before a vowel sound",
                            "A".equals(w0) ? "An" : "an"));
                } else if (("an".equals(w0) || "An".equals(w0)) && SpellChecker.letter(c1)
                        && "aeiouAEIOUhH".indexOf(c1) < 0
                        && !HunspellDictionary.upper(w1).equals(w1)) {
                    out.add(issue(x[0], x[1], "Use \"a\" before a consonant sound",
                            "An".equals(w0) ? "A" : "a"));
                }
            }
        }
    }

    private static final class Capitalization implements Rule {
        @Override
        public void check(String text, List<int[]> ws, List<ProofIssue> out) {
            boolean afterEnd = true;
            for (int[] w : ws) {
                String word = text.substring(w[0], w[1]);
                if (afterEnd && alpha(word) && Character.isLowerCase(word.charAt(0))) {
                    out.add(issue(w[0], w[1], "Capitalize the first word of a sentence",
                            HunspellDictionary.capitalize(word)));
                }
                int t = w[1];
                while (t < text.length() && "\"\u201d)'".indexOf(text.charAt(t)) >= 0) {
                    t++;
                }
                afterEnd = false;
                if (t < text.length() && ".!?".indexOf(text.charAt(t)) >= 0
                        && !text.startsWith("..", t)) {
                    int u = t;
                    while (u < text.length() && ".!?\"\u201d)".indexOf(text.charAt(u)) >= 0) {
                        u++;
                    }
                    afterEnd = u < text.length() && text.charAt(u) == ' ';
                }
            }
        }
    }

    private static final class Spacing implements Rule {
        @Override
        public void check(String text, List<int[]> ws, List<ProofIssue> out) {
            for (int i = text.indexOf(' '); i >= 0 && i + 1 < text.length();
                    i = text.indexOf(' ', i + 1)) {
                char next = text.charAt(i + 1);
                if (",.;".indexOf(next) >= 0 && i > 0 && text.charAt(i - 1) != ' ') {
                    if (i + 2 < text.length() && ".,".indexOf(text.charAt(i + 2)) >= 0) {
                        continue;
                    }
                    out.add(issue(i, i + 2, "Remove the space before the punctuation",
                            String.valueOf(next)));
                } else if (next == ' ' && (i == 0 || text.charAt(i - 1) != ' ')) {
                    out.add(issue(i, i + 2, "Extra space", " "));
                }
            }
        }
    }
}
