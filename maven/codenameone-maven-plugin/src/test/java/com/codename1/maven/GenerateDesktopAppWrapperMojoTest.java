package com.codename1.maven;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the build-time desktop wrapper generation wires the {@code desktop.titleBar} and
 * {@code desktop.interactiveScrollbars} build hints into the generated {@code *Stub.java} (the
 * Stub used by the packaged / native desktop builds). The runtime behavior of those calls is
 * covered by the JavaSE {@code DesktopChromeUITest}; here we only assert the substitution.
 */
class GenerateDesktopAppWrapperMojoTest {

    private String template() throws Exception {
        try (InputStream in = GenerateDesktopAppWrapperMojo.class
                .getResourceAsStream("desktop-app-stub-template.java")) {
            assertTrue(in != null, "desktop-app-stub-template.java must be on the classpath");
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private String render(String titleBar, String interactiveScrollbars) throws Exception {
        GenerateDesktopAppWrapperMojo mojo = new GenerateDesktopAppWrapperMojo();
        Properties p = new Properties();
        if (titleBar != null) {
            p.setProperty("codename1.arg.desktop.titleBar", titleBar);
        }
        if (interactiveScrollbars != null) {
            p.setProperty("codename1.arg.desktop.interactiveScrollbars", interactiveScrollbars);
        }
        mojo.properties = p;
        return mojo.applyTemplate(template(), "com.example", "MyApp");
    }

    @Test
    void honorsExplicitHints() throws Exception {
        String src = render("custom", "false");
        assertTrue(src.contains("private static final String APP_DESKTOP_TITLEBAR = \"custom\";"),
                "titleBar hint must flow into the generated stub");
        assertTrue(src.contains("APP_DESKTOP_INTERACTIVE_SCROLLBARS = false;"),
                "interactiveScrollbars hint must flow into the generated stub");
        assertTrue(src.contains("JavaSEPort.setDesktopTitleBarMode(APP_DESKTOP_TITLEBAR)"),
                "the stub must call setDesktopTitleBarMode");
        assertTrue(src.contains("JavaSEPort.setDesktopInteractiveScrollbars(APP_DESKTOP_INTERACTIVE_SCROLLBARS)"),
                "the stub must call setDesktopInteractiveScrollbars");
        assertTrue(src.contains("if (\"custom\".equals(APP_DESKTOP_TITLEBAR))"),
                "custom mode must set the window undecorated in the generated stub");
    }

    @Test
    void defaultsToNativeAndInteractiveWhenHintsAbsent() throws Exception {
        String src = render(null, null);
        assertTrue(src.contains("private static final String APP_DESKTOP_TITLEBAR = \"native\";"),
                "default title bar mode must be native");
        assertTrue(src.contains("APP_DESKTOP_INTERACTIVE_SCROLLBARS = true;"),
                "interactive scrollbars must default on");
    }

    @Test
    void invalidTitleBarFallsBackToNative() throws Exception {
        String src = render("bogus", "true");
        assertTrue(src.contains("private static final String APP_DESKTOP_TITLEBAR = \"native\";"),
                "an invalid titleBar hint must fall back to native");
    }
}
