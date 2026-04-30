package com.codename1.designer.css;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Hashtable;

/**
 * Regression tests for dark-mode media query compilation into $Dark UIIDs.
 */
public class CSSDarkModeMediaQueryTest {

    public static void main(String[] args) throws Exception {
        testDarkMediaCompilesToDarkUiids();
        testAtMediaInsideHeaderCommentIsIgnored();
    }

    private static void testDarkMediaCompilesToDarkUiids() throws Exception {
        Path cssFile = Files.createTempFile("cn1-dark-media", ".css");
        Path resFile = Files.createTempFile("cn1-dark-media", ".res");
        try {
            String css = "Button { color: #111111; }"
                    + "@media (prefers-color-scheme: dark) {"
                    + "  Button { color: #eeeeee; background-color: #000000; }"
                    + "  Button.selected { color: #ff0000; }"
                    + "}";
            Files.write(cssFile, css.getBytes(StandardCharsets.UTF_8));

            CSSTheme theme = CSSTheme.load(cssFile.toUri().toURL());
            theme.resourceFile = resFile.toFile();
            theme.updateResources();

            Hashtable themeProps = theme.res.getTheme("Theme");
            assertEquals("111111", themeProps.get("Button.fgColor"), "Base style fgColor");
            assertEquals("eeeeee", themeProps.get("$DarkButton.fgColor"), "Dark style fgColor");
            assertEquals("000000", themeProps.get("$DarkButton.bgColor"), "Dark style bgColor");
            assertEquals("255", themeProps.get("$DarkButton.transparency"), "Dark style transparency");
            assertEquals("ff0000", themeProps.get("$DarkButton.sel#fgColor"), "Dark selected fgColor");
        } finally {
            deleteIfExists(cssFile);
            deleteIfExists(resFile);
        }
    }

    /**
     * Regression: the dark-mode rewriter must not trigger on the literal
     * "@media (prefers-color-scheme:" string sitting inside a header
     * comment. Before the fix it swallowed everything up to the next {,
     * treated the subsequent block's properties as dark selectors, and
     * ran the tokenizer off EOF later on.
     */
    private static void testAtMediaInsideHeaderCommentIsIgnored() throws Exception {
        Path cssFile = Files.createTempFile("cn1-dark-comment", ".css");
        Path resFile = Files.createTempFile("cn1-dark-comment", ".res");
        try {
            String css = "/* header doc mentions @media (prefers-color-scheme: dark) for reference */\n"
                    + "#Constants { tabsGridBool: true; }\n"
                    + "Button { color: #111111; }\n"
                    + "@media (prefers-color-scheme: dark) {\n"
                    + "  Button { color: #eeeeee; }\n"
                    + "}\n";
            Files.write(cssFile, css.getBytes(StandardCharsets.UTF_8));

            CSSTheme theme = CSSTheme.load(cssFile.toUri().toURL());
            theme.resourceFile = resFile.toFile();
            theme.updateResources();

            Hashtable themeProps = theme.res.getTheme("Theme");
            assertEquals("111111", themeProps.get("Button.fgColor"), "Light Button fgColor survives comment");
            assertEquals("eeeeee", themeProps.get("$DarkButton.fgColor"), "Real dark block still compiles");
            assertEquals("true", themeProps.get("@tabsGridBool"), "#Constants block isn't mangled");
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
