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
package com.codename1.settings;

import com.codename1.ui.css.CSSThemeCompiler;
import com.codename1.ui.util.MutableResource;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SettingsThemeTest {
    private static final Path COMMON_DIR = Paths.get("../common").normalize();
    private static final Path SETTINGS = COMMON_DIR.resolve("codenameone_settings.properties");
    private static final Path THEME_CSS = COMMON_DIR.resolve("src/main/css/theme.css");
    private static final Path APP_SOURCE = COMMON_DIR.resolve("src/main/java/com/codename1/settings/CodenameOneSettings.java");
    private static final Path COMPILED_THEME = COMMON_DIR.resolve("target/classes/theme.res");

    @Test
    public void settingsAppEnablesCssThemeCompilation() throws Exception {
        String settings = Files.readString(SETTINGS, StandardCharsets.UTF_8);
        assertTrue(settings.contains("codename1.cssTheme=true"),
                "Codename One CSS is only baked into theme.res when codename1.cssTheme=true is present. "
                        + "This follows the Initializr skill convention for common/src/main/css/theme.css.");
    }

    @Test
    public void themeCssCompilesToThemeResource() throws Exception {
        String css = Files.readString(THEME_CSS, StandardCharsets.UTF_8);
        assertTrue(css.contains("includeNativeBool: true"),
                "theme.css should include the modern native theme base before Settings overrides. "
                        + "This keeps modern native components such as Switch styled correctly.");
        MutableResource resource = new MutableResource();
        new CSSThemeCompiler().compile(css, resource, "SettingsThemeCompileCheck");
        assertNotNull(resource.getTheme("SettingsThemeCompileCheck"),
                "theme.css should compile with the Codename One CSS compiler used by Initializr projects.");
        assertFalse(Pattern.compile("(?m)^\\s*border\\s*:\\s*0\\s*;").matcher(css).find(),
                "Codename One CSS rejects unitless 'border: 0'; use border-width: 0 instead.");
    }

    @Test
    public void packagedThemeResExistsAndContainsThemeData() throws Exception {
        assertTrue(Files.isRegularFile(COMPILED_THEME),
                "common/target/classes/theme.res should exist after Maven package/test. "
                        + "If this is missing, the JavaSE app starts with '/theme.res not found'.");
        byte[] data = Files.readAllBytes(COMPILED_THEME);
        String resourceText = new String(data, StandardCharsets.ISO_8859_1);
        assertTrue(data.length > 1024, "theme.res should contain compiled theme data, not an empty placeholder.");
        assertTrue(resourceText.contains("Theme"), "theme.res should include the default compiled theme entry.");
        assertTrue(resourceText.contains("SettingsForm"),
                "theme.res should include Settings UIID data from common/src/main/css/theme.css.");
    }

    @Test
    public void everyProgrammaticUiidHasACssRuleAndNoDarkTwin() throws Exception {
        String source = Files.readString(APP_SOURCE, StandardCharsets.UTF_8);
        String css = Files.readString(THEME_CSS, StandardCharsets.UTF_8);
        Set<String> uiids = referencedUiids(source);
        assertFalse(uiids.isEmpty(), "Expected to find \"Settings...\" UIIDs in CodenameOneSettings.java");
        Set<String> selectors = cssSelectors(css);
        for (String uiid : uiids) {
            assertTrue(selectors.contains(uiid),
                    "Missing CSS selector for UIID '" + uiid + "'. "
                            + "CN1 CSS selectors target component UIIDs, per Initializr skill guidance.");
        }
        for (String selector : selectors) {
            assertFalse(selector.startsWith("Settings") && selector.endsWith("Dark"),
                    "'" + selector + "' is a dark twin UIID. Dark mode is the native one now: the "
                            + "@media (prefers-color-scheme: dark) block compiles to $Dark styles the "
                            + "framework picks per style, so the app never switches UIIDs.");
        }
        assertFalse(source.contains("+ \"Dark\""),
                "The app must not append 'Dark' to a UIID; CN.setDarkMode drives the $Dark styles.");
    }

    /// Settings has no palette of its own: surfaces and text bind to the variables every
    /// desktop native theme declares, so the same stylesheet reads as Fluent, Aqua or
    /// Adwaita depending on which one is underneath. Only the brand is fixed.
    @Test
    public void colorsComeFromTheNativePaletteExceptTheBrand() throws Exception {
        String css = Files.readString(THEME_CSS, StandardCharsets.UTF_8);
        String source = Files.readString(APP_SOURCE, StandardCharsets.UTF_8);
        assertTrue(source.contains("themeMode = \"native\""),
                "Settings must opt into the platform's desktop theme; unset means legacy.");
        for (String variable : new String[]{"--window-bg-color", "--control-bg-color", "--text-color",
                "--text-secondary-color", "--window-bg-color-dark", "--text-color-dark"}) {
            assertTrue(css.contains("var(" + variable + ")"), "theme.css should bind to " + variable);
        }
        assertTrue(css.contains("@media (prefers-color-scheme: dark)"));
        assertTrue(css.contains("--accent-color: #2F6BFF;"), "the brand accent is Codename One blue");
        assertTrue(css.contains("--accent-color-dark: #4D86FF;"));
        assertTrue(css.contains("background-color: #B8D532;"), "Save keeps the brand lime");
        assertFalse(css.contains("#071B4D") || css.contains("#102B66"),
                "The old navy page palette must not come back; it hid the native theme entirely.");
    }

    @Test
    public void controlsDeriveFromTheirNativeUiids() throws Exception {
        String css = Files.readString(THEME_CSS, StandardCharsets.UTF_8);
        assertDerives(css, "SettingsField", "TextField");
        assertDerives(css, "SettingsSearchBox", "TextField");
        assertDerives(css, "SettingsPrimary", "RaisedButton");
        assertDerives(css, "SettingsOutline", "Button");
        assertDerives(css, "SettingsCard", "GroupBox");
        assertDerives(css, "SettingsRow", "GroupBox");
        assertDerives(css, "SettingsRailItem", "ListRenderer");
        assertDerives(css, "SettingsPopupMenu", "PopupContentPane");
        String source = Files.readString(APP_SOURCE, StandardCharsets.UTF_8);
        assertFalse(source.contains("SettingsSwitch"),
                "A Switch reads its track and thumb constants by UIID, so a renamed switch "
                        + "loses the native theme's switch geometry. Use the plain Switch UIID.");
    }

    @Test
    public void focusedInputsRemainVisibleInDarkMode() throws Exception {
        String css = Files.readString(THEME_CSS, StandardCharsets.UTF_8);
        String dark = css.substring(css.indexOf("@media (prefers-color-scheme: dark)"));
        assertTrue(dark.contains("SettingsSearchBoxFocused { background-color: var(--view-bg-color-dark)"));
        assertTrue(dark.contains("border: 2px solid #4D86FF"));
        assertTrue(dark.contains("SettingsSearchField, SettingsSearchField.selected { color: var(--text-color-dark)"));
    }

    @Test
    public void activeBuildHintControlsStayAlignedToTheRight() throws Exception {
        String source = Files.readString(APP_SOURCE, StandardCharsets.UTF_8);
        // The controls sit EAST of the name, on the name's own line, which is
        // where the Add button of an inactive row sits. They used to be a band of
        // their own below the name, held right by a TableLayout whose left 72%
        // was an empty spacer; that band cost a full row of height per active
        // row, which is height multiplied by the length of the catalog.
        assertFalse(source.contains("widthPercentage(72), new Container()"));
        assertTrue(source.contains("header.add(BorderLayout.EAST, activeHintEditor(row, meta, value, effectiveType))"));
        // The delete control is EAST of the editor. The button itself moved into
        // removeHintButton so the conflict row can reuse it -- what this asserts
        // is where it sits, which is unchanged.
        assertTrue(source.contains("controls.add(BorderLayout.EAST, removeHintButton(row, meta))"));
    }

    /// The list scrolls; the page around it does not (issue #5602). Two nested
    /// scrollable containers both claim the drag, which is the arrangement that
    /// produces scrolling artifacts -- so the search box and the custom hint form
    /// are pinned and the rows scroll under them.
    @Test
    public void buildHintsListScrollsAndThePageAroundItDoesNot() throws Exception {
        String source = Files.readString(APP_SOURCE, StandardCharsets.UTF_8);
        assertTrue(source.contains("boolean fillsViewport = section == Section.BUILD_HINTS;"));
        assertTrue(source.contains("pageViewport.setScrollableY(!fillsViewport);"));
        assertTrue(source.contains("private final class HintList extends Container"),
                "The rows must come from a plain scrollable Container holding the whole "
                        + "result set. A paging container draws its scrollbar from the rows "
                        + "it has fetched, so the thumb is sized against a fraction of the "
                        + "catalog and resizes every time a batch lands -- which is the "
                        + "bouncing thumb issue #5602 came back about.");
        assertFalse(source.contains("extends InfiniteContainer"),
                "InfiniteContainer is for data of unknown length arriving over a network. "
                        + "The catalog is 615 known rows already in memory, and building all "
                        + "of them is not measurably slower than building 20.");
        assertTrue(source.contains("hintList.reload()"),
                "A new result set must reload the list rather than being appended to it.");
    }

    /// Two ways the list could show something that is not the result set.
    ///
    /// A search run from halfway down the scrolled catalog has to come back to
    /// the top: leaving the old offset in place puts it past the end of a short
    /// result, and the list comes up blank under a header counting two matches.
    ///
    /// And a hint the catalog has never heard of is in the list only because
    /// the project declares it, so rebuilding its row after the declaration was
    /// removed left a hint that exists nowhere offering an Add button, with the
    /// header still counting it.
    @Test
    public void theListCannotShowResultsThatAreNoLongerThere() throws Exception {
        String source = Files.readString(APP_SOURCE, StandardCharsets.UTF_8);
        assertTrue(source.contains("void reload() {"));
        assertTrue(Pattern.compile(
                        "void reload\\(\\) \\{\\s*removeAll\\(\\);.*?setScrollY\\(0\\);",
                        Pattern.DOTALL).matcher(source).find(),
                "Reloading has to replace every row and put the reader back at the first "
                        + "result; keeping the old offset leaves a short result set scrolled "
                        + "past its own end, so the header counts matches over a blank list.");
        assertTrue(source.contains("if (isBrowsableHint(meta)) {"),
                "Removing a hint the browse list does not offer has to take the result set "
                        + "again -- rebuilding its row in place leaves a row for a hint that "
                        + "nothing offers.");
        assertTrue(source.contains("return meta.aliasOf() == null && buildHints.contains(meta.name());"),
                "A deprecated alias is in the catalog so an existing declaration can be "
                        + "described, and search() skips it deliberately -- so membership of the "
                        + "catalog alone does not mean a row survives losing its declaration.");
    }

    /// The desktop scrollbar is drawn from UIIDs the look and feel names itself. The
    /// native desktop themes style them for both appearances, so the app no longer
    /// paints a fixed thumb color over the platform's.
    @Test
    public void desktopScrollbarIsLeftToTheNativeTheme() throws Exception {
        String css = Files.readString(THEME_CSS, StandardCharsets.UTF_8);
        Set<String> selectors = cssSelectors(css);
        assertFalse(selectors.contains("DesktopScroll"));
        assertFalse(selectors.contains("DesktopScrollThumb"));
    }

    @Test
    public void uiUsesDensityAwareThemeSizingInsteadOfFixedComponentDimensions() throws Exception {
        String source = Files.readString(APP_SOURCE, StandardCharsets.UTF_8);
        String css = Files.readString(THEME_CSS, StandardCharsets.UTF_8);
        assertFalse(Pattern.compile("\\.setPreferred(?:W|H|Size)\\s*\\(").matcher(source).find(),
                "CN1 component sizes must come from density-aware fonts, padding, margins, and layouts, "
                        + "not setPreferredWidth/Height/Size calls that break on Retina displays.");
        assertFalse(Pattern.compile("font-size\\s*:\\s*[0-9.]+px", Pattern.CASE_INSENSITIVE).matcher(css).find(),
                "Theme font sizes must use physical mm units so Retina density does not create miniature text.");
        assertTrue(source.contains("new TableLayout(1, 2)"),
                "The main content width should be responsive through TableLayout percentages.");
        // Match the column count, not the row count. This asserted GridLayout(3, 2)
        // and the Basic form has grown to five rows since; because this module's
        // tests are skipped by default nothing reported the drift. The two columns
        // are what makes the form responsive -- the row count is just how many
        // fields there happen to be.
        assertTrue(Pattern.compile("new GridLayout\\(\\d+, 2\\)").matcher(source).find(),
                "The Basic form should use a responsive two-column GridLayout.");
        assertTrue(source.contains("private Container configureToolbar()"),
                "Native desktop chrome should use a stable top-bar container, not a second Toolbar instance.");
        assertTrue(source.contains("|| section == Section.EXTENSIONS || section == Section.BUILD_HINTS ? 100 : 72"),
                "Extensions and Build Hints should use the full content width while forms retain a readable measure.");
    }

    @Test
    public void retinaTypographyHasReadablePhysicalMinimums() throws Exception {
        String css = Files.readString(THEME_CSS, StandardCharsets.UTF_8);
        assertMmFontAtLeast(css, "SettingsForm", 3.2);
        assertMmFontAtLeast(css, "SettingsToolbarBrand", 3.4);
        assertMmFontAtLeast(css, "SettingsPageTitle", 5.5);
        // SettingsField takes the native TextField's size (3.4mm to 3.9mm across the
        // desktop themes) so its placeholder lines up; it declares none of its own.
        assertMmFontAtLeast(css, "SettingsPopupLabel", 3.0);
        assertMmFontAtLeast(css, "SettingsExtensionTitle", 3.6);
        assertMmFontAtLeast(css, "SettingsExtensionText", 2.8);
    }

    private static void assertMmFontAtLeast(String css, String selector, double minimum) {
        Matcher rule = Pattern.compile("(?m)^\\s*" + Pattern.quote(selector)
                        + "\\s*(?:,[^\\{]+)?\\{([^}]*)}",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(css);
        assertTrue(rule.find(), "Missing CSS rule for " + selector);
        Matcher size = Pattern.compile("font-size\\s*:\\s*([0-9.]+)mm",
                Pattern.CASE_INSENSITIVE).matcher(rule.group(1));
        assertTrue(size.find(), selector + " should declare a physical mm font size.");
        assertTrue(Double.parseDouble(size.group(1)) >= minimum,
                selector + " font size must remain at least " + minimum + "mm for Retina readability.");
    }

    private static void assertDerives(String css, String selector, String parent) {
        Matcher rule = Pattern.compile("(?m)^\\s*" + Pattern.quote(selector)
                        + "\\s*(?:,[^\\{]+)?\\{([^}]*)}", Pattern.DOTALL).matcher(css);
        assertTrue(rule.find(), "Missing CSS rule for " + selector);
        assertTrue(rule.group(1).contains("cn1-derive: " + parent + ";"),
                selector + " should derive from the native " + parent);
    }

    private static Set<String> referencedUiids(String source) {
        LinkedHashSet<String> uiids = new LinkedHashSet<String>();
        // Every "Settings..." literal is a UIID except thread names (passed to a Thread
        // that is then started) and the string compared against in font sizing.
        Matcher matcher = Pattern.compile("\"(Settings[A-Za-z0-9_]+)\"(?!\\)\\.start\\(|\\.equals\\()")
                .matcher(source);
        while (matcher.find()) {
            uiids.add(matcher.group(1));
        }
        return uiids;
    }

    private static Set<String> cssSelectors(String css) {
        LinkedHashSet<String> selectors = new LinkedHashSet<String>();
        Matcher matcher = Pattern.compile("(?m)^\\s*([A-Za-z][A-Za-z0-9_]*(?:\\s*,\\s*[A-Za-z][A-Za-z0-9_]*)*)\\s*\\{").matcher(css);
        while (matcher.find()) {
            String[] parts = matcher.group(1).split(",");
            for (String part : parts) {
                selectors.add(part.trim());
            }
        }
        return selectors;
    }
}
