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
package com.codenameone.examples.bazaar;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/// Every literal the code hands to `Lang.tr` has a line in the English
/// bundle, so a new string cannot ship untranslated. Reads the sources and
/// the bundle as files; loads no UI class.
class BundleCoverageTest {
    private static final Pattern TR =
            Pattern.compile("Lang\\.tr\\(\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*[,)]");

    @Test
    void everyTranslatedLiteralIsInTheEnglishBundle() throws IOException {
        File root = new File("src/main");
        Properties en = new Properties();
        try (InputStream in = new FileInputStream(new File(root, "l10n/Bundle_en.properties"))) {
            en.load(in);
        }
        Set<String> missing = new TreeSet<String>();
        scan(new File(root, "java"), en, missing);
        assertTrue(missing.isEmpty(), "Missing from Bundle_en.properties: " + missing);
    }

    private static void scan(File dir, Properties en, Set<String> missing) throws IOException {
        File[] files = dir.listFiles();
        assertTrue(files != null, "No sources under " + dir.getAbsolutePath());
        for (File f : files) {
            if (f.isDirectory()) {
                scan(f, en, missing);
            } else if (f.getName().endsWith(".java")) {
                String src = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                Matcher m = TR.matcher(src);
                while (m.find()) {
                    String key = m.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
                    if (en.getProperty(key) == null) {
                        missing.add(key + " (" + f.getName() + ")");
                    }
                }
            }
        }
    }
}
