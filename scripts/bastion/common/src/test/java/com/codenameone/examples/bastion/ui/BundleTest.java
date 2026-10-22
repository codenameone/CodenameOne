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
/// What is checked is the plain case: a single literal handed to `Lang.tr`,
/// to one of the builders that translate their argument, or to a failure
/// callback (`onSucess`, `failed`) whose text a screen shows translated. A sentence built of
/// several literals is the writer's to add by hand.
class BundleTest {
    private static final Pattern CALL = Pattern.compile(
            "(?:(?:Lang\\.tr|Ui\\.(?:form|title|primary|secondary|section|empty|field|toast)|Nav\\.form)"
            + "\\(\\s*"
            // A reason handed to a failure callback, which the screen shows
            // through Lang.tr: `failed.onSucess("...")`, `result.failed("...")`
            // and `failed.failed(401, "...")`.
            + "|onSucess\\(\\s*|\\.failed\\(\\s*(?:-?\\d+\\s*,\\s*)?)"
            + "\"((?:[^\"\\\\]|\\\\.)*)\"\\s*[,)]");

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
            Matcher m = CALL.matcher(new String(Files.readAllBytes(source), StandardCharsets.UTF_8));
            while (m.find()) {
                String key = m.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
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
