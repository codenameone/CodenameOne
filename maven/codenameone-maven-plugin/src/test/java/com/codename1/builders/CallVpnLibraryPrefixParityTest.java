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
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every prefix a builder folds into a flag is one it asked the library scan
 * to look for.
 *
 * <p>The two halves are written apart -- a {@code CALL_VPN_LIB_PREFIXES}
 * array near the top of the file, a run of {@code found.contains("...")}
 * lines in the fold below -- and adding one without the other is silent:
 * the key tested can never be present, so a cn1lib's usage goes unseen and
 * the feature is simply absent from the build. It happened on the daemon
 * twin the same day the tunnel fold was written there.</p>
 *
 * <p>Source text rather than reflection, as {@link HealthScannerParityTest}
 * explains for its own subject: one half is a private field and the other is
 * statements rather than state.</p>
 */
public class CallVpnLibraryPrefixParityTest {

    private static String source(String simpleName) throws Exception {
        File f = new File("src/main/java/com/codename1/builders/"
                + simpleName + ".java");
        assertTrue(f.exists(), "builder source must be readable: "
                + f.getAbsolutePath());
        return new String(Files.readAllBytes(f.toPath()),
                StandardCharsets.UTF_8);
    }

    @Test
    public void everyFoldedPrefixIsAlsoScannedFor() throws Exception {
        for (String builder : new String[] {"AndroidGradleBuilder",
                "IPhoneBuilder"}) {
            String src = source(builder);
            int listAt = src.indexOf("CALL_VPN_LIB_PREFIXES = {");
            assertTrue(listAt >= 0, builder + " declares the prefix list");
            String list = src.substring(listAt, src.indexOf("};", listAt));
            int foldAt = src.indexOf("foldInCallAndVpnLibraryUsage");
            assertTrue(foldAt >= 0, builder + " folds library usage");
            // To the METHOD's end. Slicing at the first "return found;"
            // stops at the empty-result early-out and cuts away the body
            // this is about -- a parse that then finds nothing and passes,
            // which is what the count assertion below exists to catch.
            String fold = src.substring(foldAt);
            fold = fold.substring(0, fold.indexOf("\n    }"));
            int checked = 0;
            int at = fold.indexOf("found.contains(\"");
            while (at >= 0) {
                int from = at + "found.contains(\"".length();
                String prefix = fold.substring(from, fold.indexOf('"', from));
                assertTrue(list.contains("\"" + prefix + "\""),
                        builder + " folds " + prefix
                        + " but never asks the scan to look for it");
                checked++;
                at = fold.indexOf("found.contains(\"", from);
            }
            assertTrue(checked > 0,
                    builder + " has folds to check; the parse found none");
        }
    }
}
