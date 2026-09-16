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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Hashtable;

/**
 * The `.hover` state selector, which the desktop native themes are built on.
 *
 * <p>The behaviour worth defending is that hover is emitted <em>only</em> for a UIID that
 * actually declares it. Every other state writes its padding, margin, font and the rest for
 * every UIID unconditionally; doing that for hover would add a full key set to every theme
 * recompiled after this change, and - far worse - would defeat the runtime guard, because
 * {@code Component.getHoverStyle()} decides whether a component has a hover style by asking
 * the theme whether it declares one. Emit hover everywhere and every component answers yes,
 * then renders a style assembled from blank defaults the moment the pointer touches it.</p>
 */
public class CSSHoverStateTest {

    @BeforeAll
    static void installHeadlessImplementation() throws Exception {
        HeadlessTestSupport.installHeadlessImplementation();
    }

    @Test
    void testHoverSelectorCompilesToHoverPrefixedKeys() throws Exception {
        Hashtable theme = compile("Button { color: #111111; background-color: #ffffff; }"
                + "Button.hover { background-color: #f0f0f0; }");
        assertEquals("F0F0F0", theme.get("Button.hover#bgColor"), "hover bgColor");
        assertEquals("111111", theme.get("Button.fgColor"), "the base is untouched");
    }

    /** A UIID that says nothing about hover must contribute no hover keys at all. */
    @Test
    void testUiidWithoutHoverRuleEmitsNoHoverKeys() throws Exception {
        Hashtable theme = compile("Button { color: #111111; }"
                + "Button.hover { background-color: #f0f0f0; }"
                + "Label { color: #222222; }");
        for (Object key : theme.keySet()) {
            String k = (String) key;
            if (k.startsWith("Label") && k.indexOf("hover#") > -1) {
                throw new AssertionError("Label declares no hover rule but emitted " + k);
            }
        }
    }

    /** Nothing anywhere in a sheet without a single hover rule. */
    @Test
    void testSheetWithoutAnyHoverRuleEmitsNoHoverKeys() throws Exception {
        Hashtable theme = compile("Button { color: #111111; }"
                + "Button.pressed { color: #222222; }"
                + "Label { color: #333333; }");
        for (Object key : theme.keySet()) {
            if (((String) key).indexOf("hover#") > -1) {
                throw new AssertionError("no hover rule was written, yet " + key + " was emitted");
            }
        }
    }

    /** Dark-mode hover lands on the $Dark spelling the runtime looks for. */
    @Test
    void testDarkHoverCompilesToDarkPrefixedKeys() throws Exception {
        Hashtable theme = compile("Button { background-color: #ffffff; }"
                + "Button.hover { background-color: #f0f0f0; }"
                + "@media (prefers-color-scheme: dark) {"
                + "  Button.hover { background-color: #303030; }"
                + "}");
        assertEquals("F0F0F0", theme.get("Button.hover#bgColor"), "light hover");
        assertEquals("303030", theme.get("$DarkButton.hover#bgColor"), "dark hover");
    }

    @Test
    void testHoverInheritanceVisitsDirectAndTransitiveParents() throws Exception {
        Hashtable theme = compile("Parent { color: #111111; }"
                + "Parent.hover { background-color: #abcdef; }"
                + "Child { cn1-derive: Parent; }"
                + "Grandchild { cn1-derive: Child; }"
                + "Unrelated { color: #222222; }");
        assertEquals("ABCDEF", theme.get("Child.hover#bgColor"), "direct parent hover");
        assertEquals("ABCDEF", theme.get("Grandchild.hover#bgColor"), "transitive parent hover");
        assertEquals(null, theme.get("Unrelated.hover#bgColor"), "unrelated UIID stays opt-in");
    }

    @Test
    void testDeriveDeclaredOnHoverPreservesStateInheritanceAndOverrides() throws Exception {
        Hashtable theme = compile("Base { background-color: #111111; color: #222222; }"
                + "Base.hover { background-color: #abcdef; color: #123456; }"
                + "Child { background-color: #ffffff; }"
                + "Child.hover { cn1-derive: Base; color: #334455; }"
                + "Grandchild.hover { cn1-derive: Child; }");
        assertEquals("FFFFFF", theme.get("Child.bgColor"), "own normal style remains intact");
        assertEquals("ABCDEF", theme.get("Child.hover#bgColor"), "hover derives the parent's hover background");
        assertEquals("334455", theme.get("Child.hover#fgColor"), "local hover overrides remain intact");
        assertEquals("ABCDEF", theme.get("Grandchild.hover#bgColor"), "transitive state-level derivation");
        assertEquals("334455", theme.get("Grandchild.hover#fgColor"), "transitive local override");
    }

    @Test
    void testHoverOnlyRasterEffectTriggersCaptureAndHasAnHtmlElement() throws Exception {
        assertHoverCapture("Button { background-color: #ffffff; }"
                + "Button.hover { box-shadow: inset 0 2px 4px black; }", false);
    }

    @Test
    void testHoverCaptureCoexistsWithOtherStatesAndInheritance() throws Exception {
        assertHoverCapture("Button { background-color: #ffffff; }"
                + "Button.hover { box-shadow: 0 2px 4px rgba(255,0,0,0.5); }"
                + "Child { cn1-derive: Button; }"
                + "Other { box-shadow: inset 0 1px 3px black; }", true);
    }

    private static void assertHoverCapture(String css, boolean inherited) throws Exception {
        Path cssFile = Files.createTempFile("cn1-hover-capture", ".css");
        try {
            Files.write(cssFile, css.getBytes(StandardCharsets.UTF_8));
            CSSTheme theme = CSSTheme.load(cssFile.toUri().toURL());
            assertEquals(true, theme.requiresCaptureHtml(), "hover raster effects need capture");
            String html = theme.generateCaptureHtml();
            assertEquals(true, html.contains("id=\"Button.hover\""), "hover processor needs matching HTML");
            assertEquals(false, html.contains("id=\"Button\""), "native base style needs no capture");
            if (!inherited) {
                assertEquals(true, html.contains("data-box-shadow-padding=\"0.0,0.0,0.0,0.0\""),
                        "inset shadows do not reserve outer capture padding");
            }
            if (inherited) {
                assertEquals(true, html.contains("id=\"Child.hover\""), "inherited hover capture");
                assertEquals(true, html.contains("id=\"Other\""), "existing state capture remains present");
                assertEquals(false, html.contains("id=\"Other.hover\""), "no hover capture without a declaration");
            }
        } finally {
            deleteIfExists(cssFile);
        }
    }

    private static Hashtable compile(String css) throws Exception {
        Path cssFile = Files.createTempFile("cn1-hover", ".css");
        Path resFile = Files.createTempFile("cn1-hover", ".res");
        try {
            Files.write(cssFile, css.getBytes(StandardCharsets.UTF_8));
            CSSTheme theme = CSSTheme.load(cssFile.toUri().toURL());
            theme.resourceFile = resFile.toFile();
            theme.res = new com.codename1.ui.util.EditableResourcesForCSS(resFile.toFile());
            theme.res.setTheme("Theme", new Hashtable());
            theme.updateResources();
            return theme.res.getTheme("Theme");
        } finally {
            deleteIfExists(cssFile);
            deleteIfExists(resFile);
        }
    }

    private static void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }
}
