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
package com.codename1.builders;

import com.codename1.build.shared.PlatformFeatureCatalog;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The four places that must name the same non-Latin OCR scripts agree.
 *
 * <p>Nothing else catches a disagreement. The Android AI adapters are
 * excluded from the port's own compilation and ship as sources compiled
 * inside the generated app, so a class name typo is not a compile error
 * here; and the builder deletes any adapter the scan did not ask for, so
 * an adapter missing from the prune list is deleted from every build and
 * the feature reports itself unsupported with nothing in the log. The
 * chain is: the selector method the app calls -> the adapter source the
 * builder keeps -> the class the port loads -> the ML Kit artifact the
 * catalog adds.</p>
 *
 * <p>Matching on source text is crude, but the mappings live in private
 * static methods on a builder that cannot be constructed in a unit test.
 * Crude and load-bearing beats absent.</p>
 */
class AndroidTextScriptParityTest {
    /** The non-Latin scripts. Latin needs no separate model anywhere. */
    private static final String[] SCRIPTS = {
        "chinese", "devanagari", "japanese", "korean"
    };

    private static String read(File file) throws Exception {
        assertTrue(file.exists(),
                "source must be readable: " + file.getAbsolutePath());
        return new String(Files.readAllBytes(file.toPath()),
                StandardCharsets.UTF_8);
    }

    @Test
    void everyScriptAdapterSourceExistsAndSurvivesPruning() throws Exception {
        String builder = read(new File(
                "src/main/java/com/codename1/builders/AndroidGradleBuilder.java"));
        for (String script : SCRIPTS) {
            String source = AndroidGradleBuilder.androidTextScriptAdapterSource(
                    "com/codename1/ai/vision/TextScript", script);
            assertTrue(source != null, script + " has no adapter source");
            assertTrue(new File("../../Ports/Android/src/com/codename1/impl/"
                    + "android/ai/" + source).exists(),
                    source + " is selected by the scanner but does not exist");
            assertTrue(builder.contains("\"" + source + "\""),
                    source + " is missing from pruneOptionalAiSources, so the"
                            + " builder deletes it from every build");
        }
    }

    @Test
    void portLoadsTheAdapterClassesTheBuilderKeeps() throws Exception {
        String impl = read(new File("../../Ports/Android/src/com/codename1/"
                + "impl/android/ai/AndroidVisionImpl.java"));
        for (String script : SCRIPTS) {
            String source = AndroidGradleBuilder.androidTextScriptAdapterSource(
                    "com/codename1/ai/vision/TextScript", script);
            String simpleName = source.substring(0,
                    source.length() - ".java".length());
            assertTrue(impl.contains("\"" + script + "\""),
                    "AndroidVisionImpl must recognize the " + script
                            + " script id");
            assertTrue(impl.contains(simpleName.substring(
                            "AndroidTextRecognition".length())),
                    "AndroidVisionImpl must resolve " + simpleName);
        }
    }

    @Test
    void everyScriptSelectorAddsItsMlKitArtifact() {
        for (String script : SCRIPTS) {
            PlatformFeatureCatalog.Accumulator acc =
                    new PlatformFeatureCatalog.Accumulator();
            acc.consume("com/codename1/ai/vision/TextRecognizer");
            acc.consumeMethod("com/codename1/ai/vision/TextScript", script);
            boolean found = false;
            for (PlatformFeatureCatalog.Entry entry : acc.hits()) {
                for (String dependency : entry.androidGradleDeps()) {
                    found |= dependency.startsWith(
                            "com.google.mlkit:text-recognition-" + script + ":");
                }
            }
            assertTrue(found, "TextScript." + script + "() must add the "
                    + script + " ML Kit bundle, or the adapter it keeps will "
                    + "not compile in the generated app");
        }
    }
}
