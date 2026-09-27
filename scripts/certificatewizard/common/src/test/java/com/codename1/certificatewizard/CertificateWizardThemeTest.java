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
package com.codename1.certificatewizard;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The wizard sits on the platform's desktop theme -- Fluent, Aqua or Adwaita -- the way the
/// Settings app does, and styles itself in that theme's terms. These hold the stylesheet to
/// that: no palette of its own, no hand-painted dark twins, controls that are the native ones.
class CertificateWizardThemeTest {

    @Test
    void wizardOptsIntoTheNativeDesktopTheme() throws IOException {
        String source = appSource();
        assertTrue(source.contains("themeMode = \"native\""),
                "the wizard must opt into the platform's desktop theme; unset means legacy");
        assertTrue(source.contains("interactiveScrollbars = Toggle.ON"),
                "a desktop tool with long tables needs the scrollbar you can grab");
        assertTrue(themeCss().contains("includeNativeBool: true"),
                "the app theme layers the native one; without it the native theme is replaced, "
                        + "and with it went the interactive scrollbar constant the port injects");
    }

    @Test
    void everyProgrammaticUiidHasACssRuleAndNoDarkTwin() throws IOException {
        String source = appSource();
        String css = themeCss();
        Set<String> uiids = new LinkedHashSet<String>();
        Matcher m = Pattern.compile("\"(CW[A-Za-z0-9]+)\"(?!\\.equals\\()").matcher(source);
        while (m.find()) {
            uiids.add(m.group(1));
        }
        assertTrue(uiids.size() > 30, "expected the wizard's CW* UIIDs, found " + uiids);
        Set<String> selectors = selectors(css);
        for (String uiid : uiids) {
            assertTrue(selectors.contains(uiid), "missing CSS rule for UIID " + uiid);
        }
        for (String selector : selectors) {
            assertFalse(selector.startsWith("Dark"),
                    selector + " is a dark twin. Dark mode is the native one: the @media "
                            + "(prefers-color-scheme: dark) block compiles to $Dark styles the "
                            + "framework picks per style, so the app never switches UIIDs.");
        }
        assertFalse(source.contains("\"Dark\" +"), "the app must not prefix a UIID with Dark");
    }

    @Test
    void colorsComeFromTheNativePaletteExceptTheBrand() throws IOException {
        String css = themeCss();
        for (String variable : new String[] {"--window-bg-color", "--headerbar-bg-color",
                "--control-bg-color", "--text-color", "--text-secondary-color",
                "--window-bg-color-dark", "--control-bg-color-dark", "--text-color-dark",
                "--text-secondary-color-dark"}) {
            assertTrue(css.contains("var(" + variable + ")"), "theme.css should bind to " + variable);
        }
        assertTrue(css.contains("--accent-color: #2F6BFF;"), "the brand accent is Codename One blue");
        assertTrue(css.contains("@media (prefers-color-scheme: dark)"));
        int constants = css.indexOf("#Constants");
        String constantsBlock = css.substring(constants, css.indexOf('}', constants));
        assertFalse(constantsBlock.contains("--window-bg-color"),
                "palette fallbacks in #Constants are exported as theme constants and pin every "
                        + "platform to the Fluent values; they belong in the Container rule");
        for (String old : new String[] {"#112247", "#0F1626", "#1A2233", "#F3F4F7", "#D9DEE8"}) {
            assertFalse(css.contains(old), "the wizard's old private palette (" + old + ") is back");
        }
    }

    @Test
    void controlsDeriveFromTheirNativeUiids() throws IOException {
        String css = themeCss();
        assertDerives(css, "CWForm", "Form");
        assertDerives(css, "CWField", "TextArea");
        assertDerives(css, "CWFieldHint", "TextHint");
        assertDerives(css, "CWFilterWrap", "TextField");
        assertDerives(css, "CWPrimary", "RaisedButton");
        assertDerives(css, "CWAccent", "RaisedButton");
        assertDerives(css, "CWOutline", "Button");
        assertDerives(css, "CWDanger", "Button");
        assertDerives(css, "CWToolbarButton", "Button");
        assertDerives(css, "CWSegment", "Button");
        assertDerives(css, "CWCard", "GroupBox");
        assertDerives(css, "CWMetric", "GroupBox");
        assertDerives(css, "CWChoice", "GroupBox");
        assertDerives(css, "CWNav", "ListRenderer");
        assertDerives(css, "CWModal", "Dialog");
    }

    /// Issue #5636: undeclared, the scrollbar came from the blank theme -- a white track and a
    /// black thumb, an inverted bar -- and the check box was one colour checked or not. The
    /// wizard now leaves those UIIDs to the native theme, so what keeps #5636 fixed is that
    /// every desktop theme declares them for both appearances.
    @Test
    void lookAndFeelUiidsAreLeftToADesktopThemeThatStylesThem() throws IOException {
        Set<String> own = selectors(themeCss());
        for (String uiid : new String[] {"Scroll", "ScrollThumb", "DesktopScroll", "DesktopScrollThumb",
                "CheckBox"}) {
            assertFalse(own.contains(uiid), uiid + " is the native theme's to style");
        }
        for (String theme : new String[] {"windows-fluent", "macos-aqua", "gnome-adwaita"}) {
            String css = stripComments(read(repoRoot().resolve("native-themes/" + theme + "/theme.css")));
            Set<String> declared = selectors(css);
            for (String uiid : new String[] {"DesktopScroll", "DesktopScrollThumb", "DesktopHorizontalScroll",
                    "DesktopHorizontalScrollThumb", "CheckBox"}) {
                assertTrue(declared.contains(uiid), theme + " must declare " + uiid);
            }
            assertTrue(css.contains("CheckBox.selected"), theme + " must style the checked box");
            assertTrue(css.contains("prefers-color-scheme: dark"), theme + " must carry a dark appearance");
        }
    }

    /// The primary action is a lime button. White on it lands near 2:1, a label you hunt for.
    @Test
    void primaryButtonLabelIsReadableOnLime() throws IOException {
        String css = themeCss();
        for (String part : new String[] {light(css), dark(css)}) {
            assertTrue(contrast(property(part, "CWPrimary", "color"),
                    property(part, "CWPrimary", "background-color")) >= 4.5, "CWPrimary label contrast");
        }
    }

    /// Status pills and banners keep fixed semantic tints, since no desktop theme declares one.
    /// Each has to stay readable in the appearance it is written for.
    @Test
    void semanticTintsAreReadableInBothAppearances() throws IOException {
        String css = themeCss();
        for (String part : new String[] {light(css), dark(css)}) {
            for (String uiid : new String[] {"CWPillOk", "CWPillWarn", "CWPillBad", "CWPillMuted",
                    "CWStatus", "CWStatusOff", "CWBannerWarn"}) {
                assertTrue(contrast(property(part, uiid, "color"), property(part, uiid, "background-color")) >= 4.5,
                        uiid + " text contrast");
            }
        }
    }

    /// cn1-pill-border bakes the fill into a generated border, so a dark rule that only changes
    /// background-color keeps the light pill -- light green behind light green text.
    @Test
    void darkPillsRestateTheirPillBorder() throws IOException {
        String dark = dark(themeCss());
        for (String uiid : new String[] {"CWStatus", "CWStatusOff"}) {
            assertTrue(property(dark, uiid, "background").contains("cn1-pill-border"), uiid);
        }
    }

    /// A segmented control that resizes when you select it slides its neighbours out from under
    /// the pointer, which is why selecting a profile type took a dozen clicks. Both states share
    /// one rule for the box; the selected one may only change the fill.
    @Test
    void segmentKeepsItsBoxWhenSelected() throws IOException {
        String css = themeCss();
        assertTrue(Pattern.compile("(?m)^CWSegment, CWSegmentSelected \\{").matcher(css).find());
        for (String part : new String[] {light(css), dark(css)}) {
            for (String box : new String[] {"border", "padding", "margin", "font-size", "cn1-derive"}) {
                assertNull(ownProperty(part, "CWSegmentSelected", box),
                        "CWSegmentSelected must not restate " + box);
            }
        }
    }

    @Test
    void fontSizesArePhysical() throws IOException {
        assertFalse(Pattern.compile("font-size\\s*:\\s*[0-9.]+px").matcher(themeCss()).find(),
                "font sizes must be mm so Retina density does not produce miniature text");
    }

    private static void assertDerives(String css, String selector, String parent) {
        String value = property(light(css), selector, "cn1-derive");
        assertTrue(parent.equals(value), selector + " should derive from the native " + parent + ", got " + value);
    }

    private static String light(String css) {
        return css.substring(0, css.indexOf("@media"));
    }

    /// Past the "@media (...) {" itself: the block scanner reads brace pairs flat.
    private static String dark(String css) {
        int media = css.indexOf("@media");
        return css.substring(css.indexOf('{', media) + 1);
    }

    /// The value the cascade ends on: the LAST rule naming `selector`, alone or in a list.
    private static String property(String css, String selector, String property) {
        String value = find(css, selector, property, false);
        assertTrue(value != null, "no " + property + " for " + selector);
        return value;
    }

    /// The value from a rule naming `selector` ALONE, or null.
    private static String ownProperty(String css, String selector, String property) {
        return find(css, selector, property, true);
    }

    private static String find(String css, String selector, String property, boolean alone) {
        String found = null;
        int blockStart = css.indexOf('{');
        while (blockStart >= 0) {
            int selectorStart = css.lastIndexOf('}', blockStart);
            selectorStart = selectorStart < 0 ? 0 : selectorStart + 1;
            String selectorList = css.substring(selectorStart, blockStart).trim();
            int blockEnd = css.indexOf('}', blockStart);
            if (blockEnd < 0) {
                return found;
            }
            boolean match = alone ? selectorList.equals(selector) : hasSelector(selectorList, selector);
            if (match) {
                for (String declaration : css.substring(blockStart + 1, blockEnd).split(";")) {
                    int colon = declaration.indexOf(':');
                    if (colon > 0 && property.equals(declaration.substring(0, colon).trim())) {
                        found = declaration.substring(colon + 1).trim();
                    }
                }
            }
            blockStart = css.indexOf('{', blockEnd);
        }
        return found;
    }

    private static boolean hasSelector(String selectorList, String selector) {
        for (String candidate : selectorList.split(",")) {
            if (selector.equals(candidate.trim())) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> selectors(String css) {
        Set<String> out = new LinkedHashSet<String>();
        Matcher m = Pattern.compile("(?m)^\\s*([A-Za-z][A-Za-z0-9_]*(?:\\s*,\\s*[A-Za-z][A-Za-z0-9_.]*)*)\\s*\\{")
                .matcher(css);
        while (m.find()) {
            for (String part : m.group(1).split(",")) {
                out.add(part.trim());
            }
        }
        return out;
    }

    private static Path commonDir() {
        Path p = Paths.get(System.getProperty("user.dir")).normalize();
        return Files.isDirectory(p.resolve("src/main/css")) ? p : p.resolve("../common").normalize();
    }

    private static Path repoRoot() {
        return commonDir().resolve("../../..").normalize();
    }

    private static String themeCss() throws IOException {
        return stripComments(read(commonDir().resolve("src/main/css/theme.css")));
    }

    private static String appSource() throws IOException {
        return read(commonDir().resolve("src/main/java/com/codename1/certificatewizard/CertificateWizard.java"));
    }

    private static String read(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    /// The block scanner reads a rule's selectors as everything between the previous "}" and the
    /// next "{", so a comment in that gap would become part of the first selector.
    private static String stripComments(String css) {
        return css.replaceAll("(?s)/\\*.*?\\*/", "");
    }

    private static double contrast(String foreground, String background) {
        double a = luminance(foreground);
        double b = luminance(background);
        return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
    }

    private static double luminance(String hex) {
        int rgb = Integer.parseInt(hex.substring(1), 16);
        return 0.2126 * channel((rgb >> 16) & 0xff) + 0.7152 * channel((rgb >> 8) & 0xff)
                + 0.0722 * channel(rgb & 0xff);
    }

    private static double channel(int value) {
        double v = value / 255.0;
        return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }
}
