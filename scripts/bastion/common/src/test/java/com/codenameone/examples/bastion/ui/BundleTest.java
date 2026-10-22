/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.ui;

import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Every text the screens name has a line in `Bundle_en.properties`, so the
/// file is a complete list for a translator to work from. A string added to
/// the code without its line fails here, named.
///
/// What is checked: every literal operand of a call that translates its
/// argument -- `Lang.tr`, the builders that translate theirs (`Ui.form`,
/// `Ui.secondary`, `Ui.segments(new String[] {...})`, `Ui.failed`...), a
/// failure callback (`onSucess`, `failed`) whose text a screen shows
/// translated, and a Security Center `Item` -- including each side of a
/// `?:` among them; the Security Center's group names; and the `return`
/// literals of a method whose result is handed to `Lang.tr` (`stateName`).
/// A sentence built of several literals is the writer's to add by hand, and
/// is better written as one pattern with `{0}`.
class BundleTest {
    /// Where a translated argument list starts.
    private static final Pattern CALL = Pattern.compile(
            "(?:(?<![\\w.])(?:Lang\\.tr|Ui\\.(?:form|title|primary|secondary|section|empty|field|toast"
            + "|segments|failed)|Nav\\.form|primary|secondary|result|new Item)"
            + "|onSucess|\\.failed)\\(");

    /// A literal that is a whole operand: after `(`, `,`, `?`, `:` or `{`,
    /// and before `,`, `)`, `:` or `}`.
    private static final Pattern OPERAND = Pattern.compile(
            "[(,?:{]\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*(?=[,):}])");

    /// The text of the call's arguments, from the `(` at `open` to its match,
    /// skipping string and character literals.
    static String arguments(String text, int open) {
        int depth = 0;
        for (int at = open; at < text.length(); at++) {
            char c = text.charAt(at);
            if (c == '"' || c == '\'') {
                for (at++; at < text.length() && text.charAt(at) != c; at++) {
                    if (text.charAt(at) == '\\') {
                        at++;
                    }
                }
            } else if (c == '(') {
                depth++;
            } else if (c == ')' && --depth == 0) {
                return text.substring(open, at + 1);
            }
        }
        return text.substring(open);
    }

    /// `args` with the arguments of every call nested in it emptied: what
    /// `hint("cn1.hardenLevel")` is given is not translated, and a nested
    /// translating call is scanned by itself.
    static String withoutNestedCalls(String args) {
        StringBuilder out = new StringBuilder();
        int at = 0;
        while (at < args.length()) {
            char c = args.charAt(at);
            if (c == '"' || c == '\'') {
                int end = at + 1;
                while (end < args.length() && args.charAt(end) != c) {
                    end += args.charAt(end) == '\\' ? 2 : 1;
                }
                out.append(args, at, Math.min(end + 1, args.length()));
                at = end + 1;
            } else if (c == '(' && at > 0 && Character.isJavaIdentifierPart(args.charAt(at - 1))) {
                String nested = arguments(args, at);
                out.append("()");
                at += nested.length();
            } else {
                out.append(c);
                at++;
            }
        }
        return out.toString();
    }

    /// Every English text `source` hands to a translation.
    static List<String> translated(String text) {
        List<String> out = new ArrayList<>();
        Matcher call = CALL.matcher(text);
        while (call.find()) {
            Matcher m = OPERAND.matcher(withoutNestedCalls(arguments(text, call.end() - 1)));
            while (m.find()) {
                out.add(m.group(1));
            }
        }
        // The Security Center's groups: `g = "Network";`.
        Matcher group = Pattern.compile("(?<![\\w.])g = \"((?:[^\"\\\\]|\\\\.)*)\";").matcher(text);
        while (group.find()) {
            out.add(group.group(1));
        }
        // What a method returns when its result is translated:
        // `Lang.tr(stateName(...))` makes every `return "...";` of
        // `stateName` a key.
        Matcher wrapped = Pattern.compile("Lang\\.tr\\(\\s*(\\w+)\\(").matcher(text);
        while (wrapped.find()) {
            Matcher body = Pattern.compile("\\s" + wrapped.group(1) + "\\([^)]*\\)\\s*\\{").matcher(text);
            if (body.find()) {
                String method = arguments(text.replace('{', '(').replace('}', ')'),
                        body.end() - 1);
                Matcher ret = Pattern.compile("return \"((?:[^\"\\\\]|\\\\.)*)\";").matcher(
                        text.substring(body.end() - 1, body.end() - 1 + method.length()));
                while (ret.find()) {
                    out.add(ret.group(1));
                }
            }
        }
        return out;
    }

    @Test
    void theScanSeesEveryShape() {
        String code = "Ui.segments(new String[] {\"A\", \"B\"}, 0, i -> { });\n"
                + "Ui.secondary(x ? \"C\" : \"D\");\n"
                + "Ui.failed(content, \"E\", back);\n"
                + "secondary(\"F\");\n"
                + "new Item(g, \"G\", ACTIVE, y ? \"H {0}\" : \"I\", v);\n"
                + "g = \"J\";\n"
                + "Lang.tr(stateName(s));\n"
                + "static String stateName(int s) { if (s == 1) { return \"K\"; } return \"L\"; }\n"
                + "new Item(g, \"M\", OFF, \"N {0}\", hint(\"not.a.key\"));\n";
        assertEquals(java.util.Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H {0}", "I", "M", "N {0}",
                "J", "K", "L"), translated(code));
    }

    @Test
    void everyTranslatedLiteralHasAnEnglishLine() throws Exception {
        Properties bundle = new Properties();
        try (InputStreamReader in = new InputStreamReader(
                new FileInputStream("src/main/l10n/Bundle_en.properties"), StandardCharsets.UTF_8)) {
            bundle.load(in);
        }
        List<String> missing = new ArrayList<>();
        List<Path> sources;
        try (Stream<Path> walk = Files.walk(Paths.get("src/main/java"))) {
            sources = walk.filter(p -> p.toString().endsWith(".java")).sorted()
                    .collect(java.util.stream.Collectors.toList());
        }
        for (Path source : sources) {
            for (String literal : translated(new String(Files.readAllBytes(source),
                    StandardCharsets.UTF_8))) {
                String key = literal.replace("\\\"", "\"").replace("\\\\", "\\");
                if (key.length() > 0 && !bundle.containsKey(key)) {
                    missing.add(source.getFileName() + ": " + key);
                }
            }
        }
        assertEquals("", String.join("\n", missing));
    }

    /// Every refusal the server words (`Refused.with` in the backend) has its
    /// `error.<code>` line, with the server's own English.
    @Test
    void everyServerRefusalHasALine() throws Exception {
        Properties bundle = load("src/main/l10n/Bundle_en.properties");
        Pattern refused = Pattern.compile(
                "Refused\\.with\\(\\s*\\d+,\\s*\"([^\"]+)\",\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        List<String> missing = new ArrayList<>();
        int seen = 0;
        try (Stream<Path> walk = Files.walk(Paths.get("../backend/src/main/java"))) {
            for (Path source : (Iterable<Path>) walk.filter(p -> p.toString().endsWith(".java"))::iterator) {
                Matcher m = refused.matcher(new String(Files.readAllBytes(source), StandardCharsets.UTF_8));
                while (m.find()) {
                    seen++;
                    String line = bundle.getProperty("error." + m.group(1));
                    if (!m.group(2).equals(line)) {
                        missing.add(source.getFileName() + ": error." + m.group(1) + "=" + m.group(2));
                    }
                }
            }
        }
        assertTrue(seen > 50, "found the server's refusals");
        assertEquals("", String.join("\n", missing));
    }

    /// Every translation has every English key, the same `{n}` places, and
    /// no key English does not have.
    @Test
    void everyTranslationIsComplete() throws Exception {
        Properties en = load("src/main/l10n/Bundle_en.properties");
        assertEquals(Lang.CODES.length > 1, true, "there are translations");
        List<String> wrong = new ArrayList<>();
        for (String code : Lang.CODES) {
            Properties tr = load("src/main/l10n/Bundle_" + code + ".properties");
            for (String key : en.stringPropertyNames()) {
                String value = tr.getProperty(key);
                if (value == null || value.length() == 0) {
                    wrong.add(code + " lacks " + key);
                } else if (!places(value).equals(places(en.getProperty(key)))) {
                    wrong.add(code + " places differ in " + key);
                }
            }
            for (String key : tr.stringPropertyNames()) {
                if (!en.containsKey(key)) {
                    wrong.add(code + " has extra " + key);
                }
            }
        }
        assertEquals("", String.join("\n", wrong));
    }

    private static java.util.Set<String> places(String text) {
        java.util.Set<String> out = new java.util.TreeSet<>();
        Matcher m = Pattern.compile("\\{\\d\\}").matcher(text);
        while (m.find()) {
            out.add(m.group());
        }
        return out;
    }

    private static Properties load(String file) throws Exception {
        Properties bundle = new Properties();
        try (InputStreamReader in = new InputStreamReader(new FileInputStream(file),
                StandardCharsets.UTF_8)) {
            bundle.load(in);
        }
        return bundle;
    }
}
