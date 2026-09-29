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
package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/// The page-level settings `JavascriptBundleWriter` writes into the shipped `index.html`:
/// the mobile viewport (issue #5909), the application's title (#5911), and the browser
/// translation and Dark Reader opt-outs with the build hints that lift them.
class JavascriptPageSettingsTest {

    private static String template() throws Exception {
        Path template = Paths.get("..", "ByteCodeTranslator", "src", "javascript", "index.html")
                .toAbsolutePath().normalize();
        assertTrue(Files.exists(template), "index.html template not found at " + template);
        return new String(Files.readAllBytes(template), StandardCharsets.UTF_8);
    }

    @Test
    void defaultsDeclareAViewportAndOptOutOfTranslationAndDarkReader() throws Exception {
        String html = JavascriptBundleWriter.applyPageSettings(template(), null, false, true);
        assertTrue(html.contains("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"), html);
        assertTrue(html.contains("<meta name=\"google\" content=\"notranslate\">"), html);
        assertTrue(html.contains("<html translate=\"no\">"), html);
        assertTrue(html.contains("<meta name=\"darkreader-lock\">"), html);
        assertTrue(html.contains("<title>" + JavascriptBundleWriter.DEFAULT_PAGE_TITLE + "</title>"), html);
        assertNoPlaceholderLeft(html);
    }

    @Test
    void viewportMustNotDisableZoom() throws Exception {
        String html = JavascriptBundleWriter.applyPageSettings(template(), "App", false, true);
        assertFalse(html.contains("user-scalable"), html);
        assertFalse(html.contains("maximum-scale"), html);
    }

    @Test
    void hintsLiftTheOptOuts() throws Exception {
        String html = JavascriptBundleWriter.applyPageSettings(template(), "App", true, false);
        assertFalse(html.contains("notranslate"), html);
        assertFalse(html.contains("translate=\"no\""), html);
        assertTrue(html.contains("<html>"), html);
        assertFalse(html.contains("darkreader-lock"), html);
        // The viewport is not a policy and has no switch.
        assertTrue(html.contains("name=\"viewport\""), html);
        assertNoPlaceholderLeft(html);
    }

    @Test
    void titleIsTheDisplayNameEscaped() throws Exception {
        String html = JavascriptBundleWriter.applyPageSettings(template(), "  Tom & Jerry's <Game>  ", false, true);
        assertTrue(html.contains("<title>Tom &amp; Jerry&#39;s &lt;Game&gt;</title>"), html);
        assertFalse(html.contains(JavascriptBundleWriter.DEFAULT_PAGE_TITLE), html);
    }

    @Test
    void settingsLeaveTheContentSecurityPolicyUnchanged() throws Exception {
        // Only meta tags and an attribute are added. An inline script or style would need a new
        // hash, and a page whose policy was computed before the change would block it.
        String raw = template();
        String filled = JavascriptBundleWriter.applyPageSettings(raw, "App", false, true);
        assertEquals(JavascriptSecurityHeaders.hashesOf(raw, "script"), JavascriptSecurityHeaders.hashesOf(filled, "script"));
        assertEquals(JavascriptSecurityHeaders.hashesOf(raw, "style"), JavascriptSecurityHeaders.hashesOf(filled, "style"));
    }

    private static void assertNoPlaceholderLeft(String html) {
        assertFalse(html.contains("__HTML_ATTRS__"), html);
        assertFalse(html.contains("__HEAD_META__"), html);
        assertFalse(html.contains("__APP_TITLE__"), html);
    }
}
