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
package com.codename1.compat.jdk;

import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// A corpus of patterns, each run over a set of inputs through this module's
/// `Pattern` and through `java.util.regex`: whether it compiles, whether it
/// matches, every match `find` reports with every group, what a replacement
/// and a split produce. The two have to agree on all of it.
public class RegexDifferentialTest {

    private static final String[] INPUTS = {"", "a", "abc", "ABC", "aaa", "abab", "abcabc", "hello world",
        "Hello, World!", "foo123bar", "2024-02-29", "12:34:56", "a,b,,c,", ",a,,b", "  lead and trail  ",
        "line1\nline2\r\nline3\rline4", "\n", "\r\n", "a\nb", "tab\tsep\tvalues", "x=1;y=22;z=333",
        "user@example.com", "The quick brown fox", "aAbBcC", "foo.bar.baz", "a|b|c", "(paren) [bracket] {brace}",
        "1+1=2", "$100.50", "back\\slash", "C:\\dir\\file.txt", "/usr/local/bin", "key: value", "  ", "aXbXc",
        "abba", "catcat dogdog", "<b>bold</b><i>it</i>", "0x1F 0xff 077", "camelCaseName", "snake_case_name",
        "commit 1a2b3c4d\nAuthor: someone\nDate: today", "\u001b[31mred\u001b[0m", "caf\u00e9 na\u00efve",
        "\u00c9COLE \u00e9cole", "a.b", "aab", "xyz", "ab12", "--flag=value", "feature/branch-name",
        "refs/heads/main", "v1.2.3-rc1", "#comment", "a  b   c", "end.", "foo\n", "foo\r\n", "\nfoo", "aa\n\nbb\n"};

    private static final String[] PATTERNS = {"a", "abc", "a*", "a+", "a?", "a{2}", "a{2,}", "a{1,2}", "a*?", "a+?",
        "a??", "a{1,2}?", "a*+", "a++", "a?+", "a*+a", ".", ".*", ".+", ".*?", "..", "^", "$", "^$", "^a", "c$",
        "^abc$", "\\Aa", "c\\z", "c\\Z", "\\z", "\\Z", "\\bfoo\\b", "\\b", "\\B", "\\b\\w+\\b", "\\d", "\\d+",
        "\\D+", "\\w+", "\\W+", "\\s+", "\\S+", "\\s*", "[abc]", "[^abc]", "[a-c]+", "[^a-c]+", "[a-zA-Z]+",
        "[a-z0-9_]+", "[\\d.]+", "[\\w.-]+@[\\w.-]+", "[a-z&&[^aeiou]]+", "[a-z&&[def]]", "[a-c[x-z]]+",
        "[^\\s]+", "[\\[\\]]", "[]a]+", "[a\\-z]+", "[a-]+", "[-a]+", "[.]", "[$^]", "[\\\\/]", "a|b", "a|b|c",
        "ab|cd", "abc|ab|a", "a|ab|abc", "(a|b)+", "(a|ab)(c|bcd)", "(?:a|b)+c", "x|", "|x", "(a)", "(a)(b)(c)",
        "(a(b(c)))", "(a)|(b)", "(a)?b", "(a*)*", "(a*)+", "(a|b)*", "(a+)+", "(?:a+)+b", "(ab)+", "(ab)*",
        "(ab){2}", "(ab){1,2}?", "(ab)?+ab", "(\\w)\\1", "(\\w+) \\1", "(a)(b)\\2\\1", "(?<first>\\w)\\k<first>",
        "(?<year>\\d{4})-(?<month>\\d{2})-(?<day>\\d{2})", "(\\d+):(\\d+):(\\d+)", "(\\w+)=(\\d+);?",
        "(?i)abc", "(?i)a(?-i)bc", "(?i:a)bc", "a(?i)b|c", "(?i)[a-c]+", "(?i)[^a-c]+", "(?i)(\\w)\\1",
        "(?m)^", "(?m)$", "(?m)^\\w+$", "(?m)^line\\d", "(?s).+", "(?s)a.b", "a.b", "(?d).+", "(?d)^.*$",
        "(?md)^\\w+$", "(?x) a b c # comment", "(?x)a\\ b", "(?ms)^l.*4$", "(?=a)", "(?=a)a", "a(?=b)", "a(?!b)",
        "(?<=a)b", "(?<!a)b", "(?<=a|bc)b", "(?<=\\d{1,3})\\D", "\\w+(?=,)", "(?<![a-z])\\d+", "(?!foo)\\w+",
        "(?=(\\w))\\1", "(?>a+)a", "(?>a|ab)c", "(?>a*)", "\\Qa.b\\E", "\\Q.\\E+", "\\Q(paren)\\E", "a\\.b",
        "\\$\\d+\\.\\d+", "\\(.*?\\)", "\\[.*\\]", "\\{.*\\}", "\\\\", "\\|", "\\t", "\\n", "\\r\\n", "\\r?\\n",
        "\\x41", "\\u0041", "\\x{41}", "\\0101", "\\011", "\\e\\[[0-9;]*m", "\\u001B\\[[@-Z\\\\-_]|\\[[0-?]*[ -/]*[@-~]",
        "\\cA", "\\a", "\\f", "\\h+", "\\H+", "\\v+", "\\V+", "\\p{Alpha}+", "\\p{Digit}+", "\\p{Upper}\\p{Lower}+",
        "\\p{Punct}+", "\\p{Space}+", "\\p{Alnum}+", "\\P{Alpha}+", "\\p{XDigit}+", "\\p{ASCII}+", "\\p{Blank}+",
        "\\p{Graph}+", "\\p{Print}+", "\\p{Cntrl}", "\\p{Lu}", "\\p{Ll}+", "\\p{L}+", "\\pL+", "\\PL+",
        "\\p{IsAlphabetic}+", "\\p{javaLowerCase}+", "\\p{javaUpperCase}+", "\\p{javaWhitespace}+", "\\p{Nd}+",
        "[\\p{Alpha}\\d]+", "[^\\p{Alpha}]+", "[\\P{Alpha}&&\\S]+", "\\G\\w", "\\Ga", "\\G\\d\\d:?",
        "^(\\w+)://([^/:]+)(:\\d+)?(/.*)?$", "^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$", "^\\s+|\\s+$", "\\s{2,}",
        "[,;]\\s*", "\\s*,\\s*", "(?<=[a-z])(?=[A-Z])", "_+", "\\.", "/", "0[xX][0-9a-fA-F]+", "^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:-(\\w+))?$",
        "<([a-z]+)>(.*?)</\\1>", "<[^>]+>", "^#.*", "^(?:refs/heads/)?(.+)$", "^commit ([0-9a-f]{7,40})$",
        "(?m)^commit ([0-9a-f]+)$", "(?m)^(\\w+): (.*)$", "^-{1,2}(\\w+)(?:=(.*))?$", "(?i)\\bthe\\b", "(?iu)\\u00e9cole",
        "(?i)\\u00e9cole", "caf.", "\\w+\\s\\w+", "[^,]*", "[^,]+", ",", "a{0}", "a{0,0}b", "(a{0,2}){2}", "(a|)+",
        "(|a)+", "(a?)*?b", "(a*)*b", "(?:a?){3}", "(?:){2}", "()", "()*", "(a)*\\1", "\\1(a)", "(?:(a)|b)\\1",
        "(a)|\\1", "[a-z]+(?:\\.[a-z]+)*", "(?:[a-z]+\\.)*[a-z]+", "\\d{4}-\\d{2}-\\d{2}", "^\\d+$", "^-?\\d+(\\.\\d+)?$",
        "X", "aXb", "(.)\\1", "(.+)\\1", "(?s)(.)(?=.*\\1)", "^(?!.*\\.\\.)[\\w./-]+$", "[\\x00-\\x1f]", "[\\u0000-\\u007f]+",
        "[^\\x00-\\x7f]+", "\\x1b", "[\\t ]+", "[ \\t\\n\\x0B\\f\\r]+", "\\S+\\s*$", "(?m)$\\n?", "(?m)^$",
        // Not patterns at all.
        "(", ")", "a)", "(a", "[", "[a", "[a-", "[z-a]", "*", "+", "?", "a**", "a{", "a{1", "a{2,1}", "a{x}", "\\",
        "\\q", "\\k<none>", "(?<n>a)(?<n>b)", "(?<1n>a)", "(?<n", "(?", "(?z)",
        "\\x4", "\\u004", "\\p{NoSuchThing}", "\\p{Alpha", "[[a]", "\\c", "\\0", "\\08", "x{2}{3}", "(?i", "[a&&]",
        "\\x{110000}"};

    /// Patterns for which a JDK older than the ones applications are
    /// written against answers differently; they are checked against
    /// expected values instead.
    private static final String[] VERSION_DEPENDENT = {"\\R", "\\R+", "a\\Rb", "\\X", "\\N{LATIN SMALL LETTER A}",
        "\\b{g}", "(?U)\\w", "\\p{IsGreek}", "\\p{InArrows}", "\\p{Sc}", "\\p{IsLatin}"};

    private static String sign(Throwable t) {
        return t.getClass().getSimpleName();
    }

    private static String jdk(String regex, int flags, String input) {
        java.util.regex.Pattern p;
        try {
            p = java.util.regex.Pattern.compile(regex, flags);
        } catch (java.util.regex.PatternSyntaxException e) {
            return "PatternSyntaxException";
        }
        StringBuilder sb = new StringBuilder();
        try {
            java.util.regex.Matcher m = p.matcher(input);
            sb.append(m.matches());
            m.reset();
            sb.append(m.lookingAt()).append('|');
            m.reset();
            int n = 0;
            while (m.find() && n++ < 200) {
                sb.append(m.start()).append('-').append(m.end());
                for (int g = 1; g <= m.groupCount(); g++) {
                    sb.append('(').append(m.start(g)).append(',').append(m.end(g)).append(',').append(m.group(g))
                            .append(')');
                }
                sb.append(';');
            }
            sb.append('|').append(m.replaceAll("<$0>"));
            sb.append('|').append(m.replaceFirst("!"));
            sb.append('|').append(Arrays.toString(p.split(input)));
            sb.append('|').append(Arrays.toString(p.split(input, -1)));
            sb.append('|').append(Arrays.toString(p.split(input, 2)));
            sb.append('|').append(m.find(input.length() / 2) ? m.start() : -1);
            m.region(input.length() / 3, input.length() - input.length() / 4);
            sb.append('|').append(m.find() ? m.start() + "-" + m.end() : "none");
            sb.append(m.matches()).append(m.lookingAt());
        } catch (StackOverflowError e) {
            return "overflow";
        } catch (RuntimeException e) {
            sb.append(sign(e));
        }
        return sb.toString();
    }

    private static String shim(String regex, int flags, String input) {
        Pattern p;
        try {
            p = Pattern.compile(regex, flags);
        } catch (PatternSyntaxException e) {
            return "PatternSyntaxException";
        }
        StringBuilder sb = new StringBuilder();
        try {
            Matcher m = p.matcher(input);
            sb.append(m.matches());
            m.reset();
            sb.append(m.lookingAt()).append('|');
            m.reset();
            int n = 0;
            while (m.find() && n++ < 200) {
                sb.append(m.start()).append('-').append(m.end());
                for (int g = 1; g <= m.groupCount(); g++) {
                    sb.append('(').append(m.start(g)).append(',').append(m.end(g)).append(',').append(m.group(g))
                            .append(')');
                }
                sb.append(';');
            }
            sb.append('|').append(m.replaceAll("<$0>"));
            sb.append('|').append(m.replaceFirst("!"));
            sb.append('|').append(Arrays.toString(p.split(input)));
            sb.append('|').append(Arrays.toString(p.split(input, -1)));
            sb.append('|').append(Arrays.toString(p.split(input, 2)));
            sb.append('|').append(m.find(input.length() / 2) ? m.start() : -1);
            m.region(input.length() / 3, input.length() - input.length() / 4);
            sb.append('|').append(m.find() ? m.start() + "-" + m.end() : "none");
            sb.append(m.matches()).append(m.lookingAt());
        } catch (StackOverflowError e) {
            return "overflow";
        } catch (RuntimeException e) {
            sb.append(sign(e));
        }
        return sb.toString();
    }

    private static String show(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < ' ' || c > '~') {
                sb.append("\\x").append(Integer.toHexString(c)).append(';');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static int compare(int flags) {
        int compared = 0;
        StringBuilder failures = new StringBuilder();
        int failed = 0;
        for (String regex : PATTERNS) {
            boolean reported = false;
            for (String input : INPUTS) {
                String expected = jdk(regex, flags, input);
                String actual = shim(regex, flags, input);
                compared++;
                if (!expected.equals(actual)) {
                    failed++;
                }
                if (!expected.equals(actual) && !reported && failures.length() < 6000) {
                    reported = true;
                    failures.append("\n/").append(show(regex)).append("/ flags ").append(flags).append(" on \"")
                            .append(show(input)).append("\"\n  jdk  ").append(show(expected)).append("\n  shim ")
                            .append(show(actual));
                }
                if (expected.equals("PatternSyntaxException")) {
                    break;
                }
            }
        }
        if (failed > 0) {
            fail(failed + " of " + compared + " differ:" + failures);
        }
        return compared;
    }

    @Test
    public void theCorpusAnswersAsTheJdkDoes() {
        assertTrue(compare(0) > 10000);
    }

    @Test
    public void theCorpusAnswersAsTheJdkDoesUnderEachFlag() {
        int compared = compare(java.util.regex.Pattern.CASE_INSENSITIVE);
        compared += compare(java.util.regex.Pattern.MULTILINE);
        compared += compare(java.util.regex.Pattern.DOTALL);
        compared += compare(java.util.regex.Pattern.UNIX_LINES | java.util.regex.Pattern.MULTILINE);
        compared += compare(java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE);
        compared += compare(java.util.regex.Pattern.LITERAL);
        compared += compare(java.util.regex.Pattern.LITERAL | java.util.regex.Pattern.CASE_INSENSITIVE);
        assertTrue(compared > 70000);
    }

    @Test
    public void theFlagConstantsAreTheJdkValues() {
        assertEquals(java.util.regex.Pattern.UNIX_LINES, Pattern.UNIX_LINES);
        assertEquals(java.util.regex.Pattern.CASE_INSENSITIVE, Pattern.CASE_INSENSITIVE);
        assertEquals(java.util.regex.Pattern.COMMENTS, Pattern.COMMENTS);
        assertEquals(java.util.regex.Pattern.MULTILINE, Pattern.MULTILINE);
        assertEquals(java.util.regex.Pattern.LITERAL, Pattern.LITERAL);
        assertEquals(java.util.regex.Pattern.DOTALL, Pattern.DOTALL);
        assertEquals(java.util.regex.Pattern.UNICODE_CASE, Pattern.UNICODE_CASE);
        assertEquals(java.util.regex.Pattern.CANON_EQ, Pattern.CANON_EQ);
        assertEquals(java.util.regex.Pattern.UNICODE_CHARACTER_CLASS, Pattern.UNICODE_CHARACTER_CLASS);
        assertEquals(Pattern.CASE_INSENSITIVE | Pattern.DOTALL, Pattern.compile("a", 34).flags());
        assertEquals("a+", Pattern.compile("a+").pattern());
        assertEquals("a+", Pattern.compile("a+").toString());
    }

    @Test
    public void quotingAndReplacementSyntaxAreTheJdks() {
        String[] texts = {"", "a.b", "\\E", "a\\Eb\\E", "$1", "\\Q", "a\\Qb\\Ec", "1+1", "\\"};
        for (String t : texts) {
            assertEquals(java.util.regex.Pattern.quote(t), Pattern.quote(t));
            assertTrue(Pattern.compile(Pattern.quote(t)).matcher(t).matches());
            assertEquals(java.util.regex.Matcher.quoteReplacement(t), Matcher.quoteReplacement(t));
            assertEquals(t, Pattern.compile("x").matcher("x").replaceAll(Matcher.quoteReplacement(t)));
        }
        String[] replacements = {"$1", "$2$1", "${word}", "\\$1", "$0$0", "[$1]", "$10", "$3", "$", "\\", "${none}",
            "${", "${word", "$x", "a\\\\b", "${1}", "$1 0"};
        for (String r : replacements) {
            String expected;
            try {
                expected = java.util.regex.Pattern.compile("(?<word>\\w+) (\\d+)").matcher("abc 123, de 45")
                        .replaceAll(r);
            } catch (RuntimeException e) {
                expected = sign(e);
            }
            String actual;
            try {
                actual = Pattern.compile("(?<word>\\w+) (\\d+)").matcher("abc 123, de 45").replaceAll(r);
            } catch (RuntimeException e) {
                actual = sign(e);
            }
            assertEquals("replacement " + r, expected, actual);
        }
    }

    @Test
    public void groupsAreReadByNameAndAfterTheMatcherMovedOn() {
        Matcher m = Pattern.compile("(?<key>\\w+)=(?<value>\\d+)").matcher("a=1 bb=22");
        java.util.regex.Matcher j = java.util.regex.Pattern.compile("(?<key>\\w+)=(?<value>\\d+)")
                .matcher("a=1 bb=22");
        assertTrue(m.find());
        assertTrue(j.find());
        MatchResult kept = m.toMatchResult();
        assertEquals(j.group("key"), m.group("key"));
        assertEquals(j.start("value"), m.start("value"));
        assertEquals(j.end("value"), m.end("value"));
        assertTrue(m.find());
        assertEquals("a=1", kept.group());
        assertEquals("1", kept.group(2));
        assertEquals(2, kept.groupCount());
        assertEquals("bb", m.group("key"));
        try {
            m.group("none");
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("none"));
        }
        try {
            Pattern.compile("a").matcher("b").group();
            fail();
        } catch (IllegalStateException expected) {
            assertEquals("No match found", expected.getMessage());
        }
        StringBuffer sb = new StringBuffer();
        Matcher r = Pattern.compile("\\d").matcher("a1b2c");
        while (r.find()) {
            r.appendReplacement(sb, "#");
        }
        r.appendTail(sb);
        assertEquals("a#b#c", sb.toString());
        assertEquals("A1B2C", Pattern.compile("[a-z]").matcher("a1b2c").replaceAll(
                new java.util.function.Function<MatchResult, String>() {
                    @Override
                    public String apply(MatchResult x) {
                        return x.group().toUpperCase();
                    }
                }));
        assertEquals(2L, Pattern.compile("\\d").matcher("a1b2c").results().count());
        assertEquals(3L, Pattern.compile(",").splitAsStream("a,b,c").count());
        assertEquals(0L, Pattern.compile(",").splitAsStream("").count());
        assertTrue(Pattern.compile("b").asPredicate().test("abc"));
        assertTrue(!Pattern.compile("b").asMatchPredicate().test("abc"));
        assertTrue(Pattern.matches("a+", "aaa"));
    }

    @Test
    public void aLineBreakIsOneMatchWhateverItsCharacters() {
        assertEquals("a|b|c|d|e", Pattern.compile("\\R").matcher("a\nb\r\nc\rd\u2028e").replaceAll("|"));
        assertTrue(Pattern.compile("a\\Rb").matcher("a\r\nb").matches());
        assertTrue(!Pattern.compile("a\\R\\nb").matcher("a\r\nb").matches());
        assertEquals(2, Pattern.compile("\\R").split("x\r\ny").length);
    }

    /// What this runtime does not have says so when the pattern is compiled,
    /// naming the construct, and never matches something else instead.
    @Test
    public void whatIsNotSupportedIsRefusedByName() {
        String[][] refused = {{"\\X", "\\X"}, {"\\N{LATIN SMALL LETTER A}", "\\N"}, {"\\b{g}", "\\b{"},
            {"(?U)\\w", "(?U)"}, {"\\p{IsGreek}", "IsGreek"}, {"\\p{InArrows}", "InArrows"}, {"\\p{Sc}", "Sc"},
            {"\\p{IsLatin}", "IsLatin"}, {"[\\x{1F600}]", "U+FFFF"}};
        // A look-behind with no upper bound is refused as current JDKs do.
        for (String unbounded : new String[] {"(?<=a*)b", "(?<=a+)b", "(?<!\\w+)x"}) {
            try {
                Pattern.compile(unbounded);
                fail(unbounded + " compiled");
            } catch (PatternSyntaxException e) {
                assertTrue(e.getDescription().contains("maximum length"));
            }
        }
        for (String[] r : refused) {
            try {
                Pattern.compile(r[0]);
                fail(r[0] + " compiled");
            } catch (PatternSyntaxException e) {
                assertTrue(e.getMessage(), e.getDescription().contains(r[1]));
                assertTrue(e.getMessage(), e.getDescription().contains("not supported by the Codename One runtime"));
                assertEquals(r[0], e.getPattern());
            }
        }
        for (int flag : new int[] {Pattern.CANON_EQ, Pattern.UNICODE_CHARACTER_CLASS}) {
            try {
                Pattern.compile("a", flag);
                fail();
            } catch (UnsupportedOperationException e) {
                assertTrue(e.getMessage().contains("not supported"));
            }
        }
        assertEquals(11, VERSION_DEPENDENT.length);
    }

    @Test
    public void aCodePointAboveTheBasicPlaneIsMatchedAsWritten() {
        String smile = new String(Character.toChars(0x1F600));
        assertTrue(Pattern.compile("a\\x{1F600}b").matcher("a" + smile + "b").matches());
        assertTrue(Pattern.compile("a" + smile + "+b").matcher("a" + smile + smile + "b").matches());
        assertTrue(Pattern.compile(smile).matcher("x" + smile).find());
    }

    @Test
    public void aSingleCharacterRepeatsOverALongTextWithoutRecursing() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200000; i++) {
            sb.append(i % 7 == 0 ? 'b' : 'a');
        }
        sb.append('!');
        String text = sb.toString();
        assertTrue(Pattern.compile(".*!").matcher(text).matches());
        assertTrue(Pattern.compile("[ab]+!").matcher(text).matches());
        assertTrue(Pattern.compile("(?:a|b)*!").matcher(text).matches());
        assertTrue(Pattern.compile("\\w*?!").matcher(text).matches());
        assertEquals(text.length() - 1, Pattern.compile("!").matcher(text).replaceAll("").length());
    }

    @Test
    public void theStringMethodsTakeTheJdkSyntax() {
        String[] texts = {"a,b,,c,,", "", "  two  spaces ", "a1b22c333", "x", ",", "a.b.c", "a|b", "tab\there"};
        String[] regexes = {",", "\\s+", "\\d+", "\\.", ".", "|", "\\|", "[,.]", "x", "", "(?<=\\d)(?=[a-z])", "\\t",
            "a", ",+"};
        int compared = 0;
        for (String t : texts) {
            for (String r : regexes) {
                for (int limit : new int[] {0, -1, 1, 2, 5}) {
                    assertEquals(t + " split " + r + " " + limit, Arrays.toString(t.split(r, limit)),
                            Arrays.toString(JdkRegex.split(t, r, limit)));
                    compared++;
                }
                assertEquals(Arrays.toString(t.split(r)), Arrays.toString(JdkRegex.split(t, r)));
                assertEquals(t.matches(r), JdkRegex.matches(t, r));
                assertEquals(t.replaceAll(r, "-"), JdkRegex.replaceAll(t, r, "-"));
                assertEquals(t.replaceFirst(r, "<$0>"), JdkRegex.replaceFirst(t, r, "<$0>"));
            }
        }
        assertTrue(compared > 500);
    }
}
