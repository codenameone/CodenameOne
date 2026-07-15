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
package com.codename1.maven.help;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ToolingHelpReportTest {

    @Test
    public void truncatesShortFieldsTo64() {
        String longStep = repeat('s', 200);
        ToolingHelpReport report = ToolingHelpReport.builder().step(longStep).build();
        assertEquals(ToolingHelpReport.SHORT_FIELD_MAX, report.getStep().length());
    }

    @Test
    public void truncatesMediumFieldsTo512() {
        ToolingHelpReport report = ToolingHelpReport.builder()
                .errorSummary(repeat('e', 1000))
                .email(repeat('a', 1000))
                .message(repeat('m', 1000))
                .build();
        assertEquals(ToolingHelpReport.MEDIUM_FIELD_MAX, report.getErrorSummary().length());
        assertEquals(ToolingHelpReport.MEDIUM_FIELD_MAX, report.getEmail().length());
        assertEquals(ToolingHelpReport.MEDIUM_FIELD_MAX, report.getMessage().length());
    }

    @Test
    public void truncatesErrorDetailTo16Kb() {
        ToolingHelpReport report = ToolingHelpReport.builder()
                .errorDetail(repeat('x', 20000))
                .build();
        assertEquals(ToolingHelpReport.ERROR_DETAIL_MAX, report.getErrorDetail().length());
    }

    @Test
    public void errorSummaryCollapsesToSingleLine() {
        ToolingHelpReport report = ToolingHelpReport.builder()
                .errorSummary("line one\n   line two\r\nline three")
                .build();
        assertEquals("line one line two line three", report.getErrorSummary());
    }

    @Test
    public void omitsEmailWhenUnknown() {
        ToolingHelpReport report = ToolingHelpReport.builder()
                .component("maven-plugin")
                .step("install")
                .build();
        assertFalse(report.hasEmail());
        String json = report.toJson();
        assertFalse("email must be omitted when unknown", json.contains("\"email\""));
        assertTrue(json.contains("\"component\":\"maven-plugin\""));
        assertTrue(json.contains("\"step\":\"install\""));
    }

    @Test
    public void omitsBlankEmail() {
        ToolingHelpReport report = ToolingHelpReport.builder().email("   ").build();
        assertFalse(report.hasEmail());
        assertFalse(report.toJson().contains("\"email\""));
    }

    @Test
    public void includesEmailWhenPresent() {
        ToolingHelpReport report = ToolingHelpReport.builder().email("dev@example.com").build();
        assertTrue(report.hasEmail());
        assertTrue(report.toJson().contains("\"email\":\"dev@example.com\""));
    }

    @Test
    public void escapesJsonSpecialCharacters() {
        ToolingHelpReport report = ToolingHelpReport.builder()
                .errorDetail("path \"C:\\Users\"\nline2\ttabbed")
                .build();
        String json = report.toJson();
        assertTrue(json.contains("\\\"C:\\\\Users\\\""));
        assertTrue(json.contains("\\n"));
        assertTrue(json.contains("\\t"));
    }

    @Test
    public void builderPrefillsEnvironmentFromSystemProperties() {
        ToolingHelpReport report = ToolingHelpReport.builder().build();
        assertEquals(System.getProperty("os.name"), report.getOs());
        assertEquals(System.getProperty("java.version"), report.getJavaVersion());
    }

    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
