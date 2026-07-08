package com.codename1.certificatewizard;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CertificateWizardDarkThemeTest {
    private static final String DARK_BG_APP = "#071B4D";
    private static final String DARK_BG_PANEL = "#102B66";
    private static final String DARK_BG_SUBTLE = "#163575";
    private static final String DARK_BG_INPUT = "#0E2A61";
    private static final String DARK_BORDER = "#33518C";
    private static final String DARK_TEXT = "#F5F8FF";
    private static final String DARK_TEXT_MUTED = "#A8B8DA";
    private static final String DARK_ACCENT = "#4D86FF";
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
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
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
