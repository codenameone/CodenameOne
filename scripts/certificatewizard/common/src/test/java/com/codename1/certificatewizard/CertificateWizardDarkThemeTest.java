package com.codename1.certificatewizard;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CertificateWizardDarkThemeTest {
    private static final String DARK_BG_APP = "#0F1626";
    private static final String DARK_BG_PANEL = "#1A2233";
    private static final String DARK_BG_SUBTLE = "#26314A";
    private static final String DARK_BG_INPUT = "#141B2B";
    private static final String DARK_BORDER = "#3A465E";
    private static final String DARK_TEXT = "#EEF2F8";
    private static final String DARK_TEXT_MUTED = "#A9B6CC";
    private static final String DARK_ACCENT = "#5C93FF";
    private static final String DARK_LIME = "#B8D532";
    private static final String DARK_DANGER = "#FF7A7A";

    @Test
    void darkThemeMatchesDarkWizardMockPalette() throws IOException {
        String css = themeCss();

        assertProperty(css, "DarkCWForm", "background-color", DARK_BG_APP);
        assertProperty(css, "DarkCWPage", "background-color", DARK_BG_APP);
        assertProperty(css, "DarkCWChrome", "background-color", DARK_BG_PANEL);
        assertProperty(css, "DarkCWSidebar", "background-color", DARK_BG_PANEL);
        assertProperty(css, "DarkCWCard", "background-color", DARK_BG_PANEL);
        assertProperty(css, "DarkCWCardRow", "background-color", DARK_BG_PANEL);
        assertProperty(css, "DarkCWField", "background-color", DARK_BG_INPUT);
        assertProperty(css, "DarkCWField", "color", DARK_TEXT);
        assertProperty(css, "DarkCWFieldHint", "color", DARK_TEXT_MUTED);
        assertProperty(css, "DarkCWNavSelected", "color", DARK_ACCENT);
        assertProperty(css, "DarkCWPrimary", "background-color", DARK_LIME);
        assertProperty(css, "DarkCWDanger", "color", DARK_DANGER);
        assertProperty(css, "DarkCWToolbarButton", "border", "0.25mm solid " + DARK_BORDER);
    }

    @Test
    void darkThemeCoversEveryWizardUiid() throws IOException {
        String css = themeCss();
        String[] uiids = {
                "CWAccent",
                "CWActionGrid", "CWActionRow", "CWBanner", "CWBannerWarn", "CWCard", "CWCardMeta", "CWCardRow", "CWCardTitle",
                "CWCellMain", "CWCellSub", "CWChoice", "CWChoiceSelected", "CWChrome",
                "CWDanger", "CWDarkToggle", "CWDialogActions", "CWDialogContent", "CWDisabled", "CWEmail",
                "CWField", "CWFieldLabel", "CWFieldHint", "CWFilterClear", "CWFilterField", "CWFilterRow",
                "CWFilterWrap", "CWForm", "CWLogo", "CWMetric",
                "CWMetricLabel", "CWMetricNumber",
                "CWModal", "CWModalTitle", "CWNav", "CWNavLabel",
                "CWOutline", "CWPage", "CWPageTitle", "CWPillBad", "CWPillMuted",
                "CWPillOk", "CWPillWarn", "CWPrimary", "CWRow", "CWSegment", "CWSegmentSelected", "CWSidebar",
                "CWTableBody", "CWTableCell",
                "CWStatus", "CWStatusOff", "CWSub", "CWTableHeader", "CWTitle",
                "CWToolbarActions", "CWToolbarButton"
        };
        for (String uiid : uiids) {
            assertTrue(css.contains("Dark" + uiid),
                    "Missing dark UIID coverage for " + uiid);
        }
    }

    @Test
    void darkThemePreservesLightThemeStructure() throws IOException {
        String css = themeCss();
        String[] uiids = {
                "CWAccent", "CWActionGrid", "CWActionRow", "CWBanner", "CWBannerWarn", "CWCard", "CWCardMeta",
                "CWCardRow", "CWCardTitle", "CWCellMain", "CWCellSub", "CWChoice", "CWChoiceSelected",
                "CWChrome", "CWDanger", "CWDarkToggle", "CWDialogActions", "CWDialogContent", "CWDisabled",
                "CWEmail", "CWField", "CWFieldHint", "CWFieldLabel", "CWFilterClear", "CWFilterField",
                "CWFilterRow", "CWFilterWrap", "CWForm", "CWLogo",
                "CWMetric", "CWMetricLabel",
                "CWMetricNumber", "CWModal", "CWModalTitle", "CWNav", "CWNavLabel", "CWNavSelected",
                "CWOutline", "CWPage", "CWPageTitle", "CWPillBad", "CWPillMuted",
                "CWPillOk", "CWPillWarn", "CWPrimary", "CWRow", "CWSegment", "CWSegmentSelected", "CWSidebar",
                "CWTableBody", "CWTableCell",
                "CWStatus", "CWStatusOff", "CWSub", "CWTableHeader", "CWTitle",
                "CWToolbarActions", "CWToolbarButton"
        };
        String[] structuralProperties = {
                "background", "border-radius", "font-family", "font-size", "margin", "padding"
        };
        for (String uiid : uiids) {
            for (String structuralProperty : structuralProperties) {
                String lightValue = propertyOrNull(css, uiid, structuralProperty);
                if (lightValue != null) {
                    assertEquals(lightValue, property(css, "Dark" + uiid, structuralProperty),
                            "Dark" + uiid + " " + structuralProperty);
                }
            }
        }
    }

    /// The look and feel asks for these UIIDs by name, so the app's "Dark" prefix scheme cannot
    /// reach them. Left undeclared they came from the blank-theme defaults -- a white track with a
    /// black thumb, which is the inverted scrollbar of issue #5636 -- so both schemes have to
    /// declare them, dark through the prefers-color-scheme block the compiler turns into $Dark
    /// entries.
    @Test
    void fixedLookAndFeelUiidsAreThemedInBothSchemes() throws IOException {
        String css = themeCss();
        String light = css.substring(0, darkBlockStart(css));
        // Past the "@media (...) {" itself: the block scanner reads brace pairs flat, so leaving
        // the wrapper in would pair the media brace with the first rule's closing one and swallow
        // that rule whole.
        String dark = css.substring(css.indexOf('{', darkBlockStart(css)) + 1);
        for (String scheme : new String[] {"light", "dark"}) {
            String part = "light".equals(scheme) ? light : dark;
            for (String uiid : new String[] {"Scroll", "HorizontalScroll", "DesktopScroll",
                    "DesktopHorizontalScroll"}) {
                assertEquals("transparent", property(part, uiid, "background-color"),
                        scheme + " " + uiid + " track must not paint over the page");
            }
            for (String uiid : new String[] {"ScrollThumb", "HorizontalScrollThumb", "DesktopScrollThumb",
                    "DesktopHorizontalScrollThumb"}) {
                String thumb = property(part, uiid, "background-color");
                assertTrue(thumb.startsWith("#"), scheme + " " + uiid + " needs an explicit colour");
                assertTrue(contrast(thumb, "light".equals(scheme) ? "#FFFFFF" : DARK_BG_PANEL) >= 1.3,
                        scheme + " " + uiid + " must be visible against the surface it scrolls");
            }
            assertTrue(property(part, "CheckBox", "color").startsWith("#"),
                    scheme + " unchecked check box colour");
            assertTrue(property(part, "CheckBox.selected", "color").startsWith("#"),
                    scheme + " checked check box colour");
            assertNotEquals(property(part, "CheckBox", "color"), property(part, "CheckBox.selected", "color"),
                    scheme + " checked and unchecked check boxes must not be the same colour");
        }
        assertTrue(css.contains("checkBoxIconSizeMM"),
                "the check box is sized off the label font without this constant, so it ends up "
                        + "smaller than the word beside it");
    }

    /// The primary action is a lime pill. White on it lands near 2:1, which is a label you have to
    /// hunt for rather than read.
    @Test
    void primaryButtonLabelIsReadableOnLime() throws IOException {
        String css = themeCss();
        for (String uiid : new String[] {"CWPrimary", "DarkCWPrimary"}) {
            assertTrue(contrast(property(css, uiid, "color"), property(css, uiid, "background-color")) >= 4.5,
                    uiid + " label contrast");
        }
    }

    /// A segmented control that resizes when you select it slides its neighbours out from under
    /// the pointer, which is why selecting a profile type took a dozen clicks.
    @Test
    void segmentKeepsItsBoxWhenSelected() throws IOException {
        String css = themeCss();
        for (String prefix : new String[] {"", "Dark"}) {
            assertEquals(property(css, prefix + "CWSegment", "border").replaceAll("#[0-9A-Fa-f]{6}", ""),
                    property(css, prefix + "CWSegmentSelected", "border").replaceAll("#[0-9A-Fa-f]{6}", ""),
                    prefix + "CWSegmentSelected must occupy the same box as " + prefix + "CWSegment");
        }
    }

    private static int darkBlockStart(String css) {
        int i = css.indexOf("@media");
        assertTrue(i > 0, "prefers-color-scheme block missing");
        return i;
    }

    @Test
    void darkThemeTextContrastStaysUsable() {
        assertTrue(contrast(DARK_TEXT, DARK_BG_PANEL) >= 7.0, "main text contrast");
        assertTrue(contrast(DARK_TEXT_MUTED, DARK_BG_PANEL) >= 4.5, "muted text contrast");
        assertTrue(contrast(DARK_ACCENT, DARK_BG_APP) >= 4.5, "accent contrast");
        assertTrue(contrast(DARK_DANGER, DARK_BG_PANEL) >= 4.5, "danger contrast");
    }

    private static void assertProperty(String css, String selector, String property, String expected) {
        assertEquals(expected, property(css, selector, property), selector + " " + property);
    }

    private static String property(String css, String selector, String property) {
        String value = propertyOrNull(css, selector, property);
        if (value != null) {
            return value;
        }
        assertTrue(hasAnySelector(css, selector), "selector not found: " + selector);
        throw new AssertionError("property not found: " + selector + " " + property);
    }

    private static String propertyOrNull(String css, String selector, String property) {
        int blockStart = css.indexOf('{');
        while (blockStart >= 0) {
            int selectorStart = css.lastIndexOf('}', blockStart);
            selectorStart = selectorStart < 0 ? 0 : selectorStart + 1;
            String selectorList = css.substring(selectorStart, blockStart).trim();
            int blockEnd = css.indexOf('}', blockStart);
            assertTrue(blockEnd > blockStart, "unterminated block after " + selectorList);
            if (hasSelector(selectorList, selector)) {
                String block = css.substring(blockStart + 1, blockEnd);
                String[] declarations = block.split(";");
                for (String declaration : declarations) {
                    int colon = declaration.indexOf(':');
                    if (colon > 0 && property.equals(declaration.substring(0, colon).trim())) {
                        return declaration.substring(colon + 1).trim();
                    }
                }
            }
            blockStart = css.indexOf('{', blockEnd);
        }
        return null;
    }

    private static boolean hasAnySelector(String css, String selector) {
        int blockStart = css.indexOf('{');
        while (blockStart >= 0) {
            int selectorStart = css.lastIndexOf('}', blockStart);
            selectorStart = selectorStart < 0 ? 0 : selectorStart + 1;
            if (hasSelector(css.substring(selectorStart, blockStart).trim(), selector)) {
                return true;
            }
            int blockEnd = css.indexOf('}', blockStart);
            assertTrue(blockEnd > blockStart, "unterminated block");
            blockStart = css.indexOf('{', blockEnd);
        }
        return false;
    }

    private static boolean hasSelector(String selectorList, String selector) {
        String[] selectors = selectorList.split(",");
        for (String candidate : selectors) {
            if (selector.equals(candidate.trim())) {
                return true;
            }
        }
        return false;
    }

    private static String themeCss() throws IOException {
        Path p = Paths.get(System.getProperty("user.dir"), "../common/src/main/css/theme.css").normalize();
        if (!Files.exists(p)) {
            p = Paths.get(System.getProperty("user.dir"), "src/main/css/theme.css").normalize();
        }
        return stripComments(new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
    }

    /// The block scanner below reads a rule's selectors as everything between the previous "}" and
    /// the next "{", so a comment sitting in that gap becomes part of the first selector and the
    /// rule stops being found at all.
    private static String stripComments(String css) {
        StringBuilder out = new StringBuilder();
        int pos = 0;
        while (pos < css.length()) {
            int start = css.indexOf("/*", pos);
            if (start < 0) {
                out.append(css.substring(pos));
                break;
            }
            out.append(css, pos, start);
            int end = css.indexOf("*/", start + 2);
            if (end < 0) {
                break;
            }
            pos = end + 2;
        }
        return out.toString();
    }

    private static double contrast(String foreground, String background) {
        double a = luminance(foreground);
        double b = luminance(background);
        double lighter = Math.max(a, b);
        double darker = Math.min(a, b);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double luminance(String hex) {
        int rgb = Integer.parseInt(hex.substring(1), 16);
        double r = channel((rgb >> 16) & 0xff);
        double g = channel((rgb >> 8) & 0xff);
        double b = channel(rgb & 0xff);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double channel(int value) {
        double v = value / 255.0;
        return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }
}
