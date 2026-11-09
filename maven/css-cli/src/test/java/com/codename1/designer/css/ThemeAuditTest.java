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
package com.codename1.designer.css;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeAuditTest {

    private static final File CSS = new File("theme.css");

    private static String checks(List<ThemeAudit.Finding> findings) {
        StringBuilder sb = new StringBuilder();
        for (ThemeAudit.Finding f : findings) {
            sb.append(f.check).append('@').append(f.line).append(' ');
        }
        return sb.toString().trim();
    }

    @Test
    void literalThatAVariableNamesIsReportedAtItsLine() {
        String css = ":root {\n  --primary: #3366FF;\n}\n/* #3366ff in a comment is not a use */\n"
                + "Button {\n  color: var(--primary);\n  background-color: rgb(51, 102, 255);\n}\n";
        List<ThemeAudit.Finding> f = ThemeAudit.auditText(CSS, css);
        assertEquals("low-contrast@6 literal-matches-variable@7", checks(f));
        assertTrue(f.get(1).message.contains("var(--primary)"), f.get(1).message);
    }

    @Test
    void undefinedAndUnusedVariables() {
        String css = ":root { --unused: 4px; }\nLabel { padding: var(--missing); margin: var(--other, 1mm); }";
        List<ThemeAudit.Finding> f = ThemeAudit.auditText(CSS, css);
        assertEquals("unused-variable@1 undefined-variable@2", checks(f));
        assertEquals(ThemeAudit.Severity.ERROR, f.get(1).severity);
    }

    @Test
    void literalRepeatedInThreeRulesIsSuggestedOnce() {
        String css = "A { color: #123456; }\nB { border-color: #123456; }\n@media (min-width: 1px) {\n C { color: #123456; }\n}";
        assertEquals("repeated-literal@1", checks(ThemeAudit.auditText(CSS, css)));
    }

    @Test
    void contrastResolvesVariablesAndSkipsTranslucentColours() {
        String css = ":root { --fg: #777; --bg: #888; }\nA { color: var(--fg); background-color: var(--bg); }\n"
                + "B { color: #000; background-color: #fff; }\nC { color: rgba(0,0,0,0.5); background-color: #fff; }";
        assertEquals("low-contrast@2", checks(ThemeAudit.auditText(CSS, css)));
        assertEquals(21.0, ThemeAudit.contrast(new int[] {0, 0, 0, 255}, new int[] {255, 255, 255, 255}), 0.01);
    }

    @Test
    void unusedUiidIgnoresStatesConstantsAndRoot() {
        String css = ":root { --x: 1px; }\n#Constants { a: b; }\nUsed, Unused { color: red; }\nUsed.pressed { color: blue; }\n"
                + "Unused.pressed { color: blue; }";
        List<ThemeAudit.Finding> f = ThemeAudit.auditText(CSS, css, new HashSet<String>(Arrays.asList("Used")));
        assertEquals("unused-uiid@3", checks(f));
        assertTrue(f.get(0).message.startsWith("Unused "), f.get(0).message);
    }

    @Test
    void cliAuditFindsUiidsInSourcesAndTheFramework(@TempDir Path dir) throws Exception {
        Path css = dir.resolve("theme.css");
        Files.write(css, ("Button { color: #000; }\nMyCard { color: #000; }\nMyGhost { color: #000; }\nDeskRow { color: red; }\n")
                .getBytes(StandardCharsets.UTF_8));
        Path src = dir.resolve("src");
        Files.createDirectories(src);
        Files.write(src.resolve("Main.java"), "class Main { void f(c) { c.setUIID(\"MyCard\"); c.setUIID(\"Desk\" + n); } }"
                .getBytes(StandardCharsets.UTF_8));
        PrintStream old = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int status;
        try {
            System.setOut(new PrintStream(out, true, "UTF-8"));
            status = CN1CSSCLI.run(new String[] {"-audit", "-input", css.toString(), "-sources", src.toString(),
                "-format", "json", "-strict"});
        } finally {
            System.setOut(old);
        }
        String json = new String(out.toByteArray(), StandardCharsets.UTF_8);
        // Button is named by the framework, MyCard by the application.
        assertTrue(json.contains("MyGhost is styled"), json);
        assertTrue(!json.contains("MyCard is styled") && !json.contains("Button is styled")
                && !json.contains("DeskRow"), json);
        assertTrue(json.contains("repeated-literal"), json);
        assertEquals(CN1CSSCLI.EXIT_COMPILE_FAILED, status);
    }
}
