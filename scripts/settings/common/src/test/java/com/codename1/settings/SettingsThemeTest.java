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
    public void everyProgrammaticUiidHasLightAndDarkCssRules() throws Exception {
        String source = Files.readString(APP_SOURCE, StandardCharsets.UTF_8);
        String css = Files.readString(THEME_CSS, StandardCharsets.UTF_8);
        Set<String> uiids = referencedUiids(source);
        assertFalse(uiids.isEmpty(), "Expected to find uiid(\"...\") references in CodenameOneSettings.java");
        Set<String> selectors = cssSelectors(css);
        for (String uiid : uiids) {
            assertTrue(selectors.contains(uiid),
                    "Missing light CSS selector for UIID '" + uiid + "'. "
                            + "CN1 CSS selectors target component UIIDs, per Initializr skill guidance.");
            assertTrue(selectors.contains(uiid + "Dark"),
                    "Missing dark CSS selector for UIID '" + uiid + "Dark'. "
                            + "The Settings app toggles by appending 'Dark' at runtime, so every styled UIID needs both.");
        }
    }

    @Test
    public void darkThemeMatchesReferencePalette() throws Exception {
        String css = Files.readString(THEME_CSS, StandardCharsets.UTF_8);
        String[] colors = {
                "#071B4D",
                "#102B66",
                "#163575",
                "#0E2A61",
                "#4C6EA8",
                "#7390C0",
                "#F5F8FF",
                "#A8B8DA",
                "#7E93BC",
                "#4D86FF",
                "#B8D532"
        };
        for (String color : colors) {
            assertTrue(css.contains(color), "Dark theme should include reference color " + color);
        }
    }

    @Test
    public void focusedInputsRemainVisibleInDarkMode() throws Exception {
        String css = Files.readString(THEME_CSS, StandardCharsets.UTF_8);
        assertTrue(css.contains("SettingsSearchBoxFocusedDark"));
        assertTrue(css.contains("SettingsSearchFieldDark, SettingsSearchFieldDark.selected"));
        assertTrue(css.contains("border-color: #4D86FF"));
        assertTrue(css.contains("color: #F5F8FF"));
    }

    @Test
    public void activeBuildHintControlsStayAlignedToTheRight() throws Exception {
        String source = Files.readString(APP_SOURCE, StandardCharsets.UTF_8);
        assertTrue(source.contains("widthPercentage(72), new Container()"));
        assertTrue(source.contains("widthPercentage(28), controls"));
        assertTrue(source.contains("controls.add(BorderLayout.EAST, remove)"));
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
        assertTrue(source.contains("new GridLayout(3, 2)"),
                "The Basic form should use a responsive two-column GridLayout.");
        assertTrue(source.contains("private Container configureToolbar()"),
                "Native desktop chrome should use a stable top-bar container, not a second Toolbar instance.");
        assertTrue(source.contains("section == Section.EXTENSIONS ? 100 : 72"),
                "Extensions should use the full content width while forms retain a readable measure.");
    }

    @Test
    public void retinaTypographyHasReadablePhysicalMinimums() throws Exception {
        String css = Files.readString(THEME_CSS, StandardCharsets.UTF_8);
        assertMmFontAtLeast(css, "SettingsForm", 3.2);
        assertMmFontAtLeast(css, "SettingsToolbarBrand", 3.4);
        assertMmFontAtLeast(css, "SettingsPageTitle", 5.5);
        assertMmFontAtLeast(css, "SettingsField", 3.0);
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

    private static Set<String> referencedUiids(String source) {
        LinkedHashSet<String> uiids = new LinkedHashSet<String>();
        Matcher matcher = Pattern.compile("uiid\\(\"([A-Za-z0-9_]+)\"\\)").matcher(source);
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
