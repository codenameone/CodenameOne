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

/// Every text the screens name has a line in `Bundle_en.properties`, so the
/// file is a complete list for a translator to work from. A string added to
/// the code without its line fails here, named.
///
/// What is checked is the plain case: a single literal handed to `Lang.tr` or
/// to one of the builders that translate their argument. A sentence built of
/// several literals is the writer's to add by hand.
class BundleTest {
    private static final Pattern CALL = Pattern.compile(
            "(?:Lang\\.tr|Ui\\.(?:form|title|primary|secondary|section|empty|field|toast)|Nav\\.form)"
            + "\\(\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*[,)]");

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
}
