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
package com.codename1.maven;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.apache.maven.project.MavenProject;
import java.nio.file.Path;
import java.nio.file.Files;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertTrue(src.contains("CN1Bootstrap.isCEFSupported()"),
                "desktop apps must select the JCEF Maven runtime when supported");
        assertFalse(src.contains("cef.dir"),
                "desktop apps must not require a pre-extracted CEF directory");
    }

    @Test
    void invalidTitleBarFallsBackToNative() throws Exception {
        String src = render("bogus", "true");
        assertTrue(src.contains("private static final String APP_DESKTOP_TITLEBAR = \"native\";"),
                "an invalid titleBar hint must fall back to native");
    }
    @Test
    void packagesThemeHintEvenWithAnArchetypeStub(@TempDir Path root) throws Exception {
        GenerateDesktopAppWrapperMojo mojo = new GenerateDesktopAppWrapperMojo();
        mojo.project = new MavenProject();
        mojo.project.setFile(root.resolve("pom.xml").toFile());
        mojo.project.getBuild().setDirectory(root.resolve("target").toString());
        mojo.project.getBuild().setOutputDirectory(root.resolve("target/classes").toString());
        mojo.properties = new Properties();
        mojo.properties.setProperty("codename1.packageName", "com.example");
        mojo.properties.setProperty("codename1.mainName", "MyApp");
        Path customStub = root.resolve("src/desktop/java/com/example/MyAppStub.java");
        Files.createDirectories(customStub.getParent());
        Files.write(customStub, new byte[0]);
        for (String mode : new String[]{"auto", "fluent", "aqua", "adwaita", "legacy", "custom"}) {
            mojo.properties.setProperty("codename1.arg.desktop.themeMode", mode);
            mojo.executeImpl();
            Properties packaged = new Properties();
            try (InputStream in = Files.newInputStream(root.resolve("target/classes/codenameone-desktop.properties"))) {
                packaged.load(in);
            }
            assertEquals(mode, packaged.getProperty("desktop.themeMode"));
            assertEquals(1, packaged.size(), "do not package unrelated build hints or credentials");
        }
        assertFalse(Files.exists(root.resolve("target/generated-sources/cn1-desktop/com/example/MyAppStub.java")),
                "the custom stub remains the source override");
    }

    @Test
    void mobileThemeHintsDoNotChangeThePackagedDesktopDefault(@TempDir Path root) throws Exception {
        GenerateDesktopAppWrapperMojo mojo = new GenerateDesktopAppWrapperMojo();
        mojo.project = new MavenProject();
        mojo.project.getBuild().setOutputDirectory(root.toString());
        mojo.properties = new Properties();
        for (String hint : new String[]{"nativeTheme", "cn1.nativeTheme"}) {
            for (String mode : new String[]{"modern", "custom", "legacy"}) {
                mojo.properties.clear();
                mojo.properties.setProperty("codename1.arg." + hint, mode);
                mojo.generateThemeConfiguration();
                Properties packaged = new Properties();
                try (InputStream in = Files.newInputStream(root.resolve("codenameone-desktop.properties"))) {
                    packaged.load(in);
                }
                assertEquals("legacy", packaged.getProperty("desktop.themeMode"), hint + "=" + mode);
            }
        }
    }

    // The exception to the test above, and the only one: "native" means the platform's
    // own look on every OS, so the packaged desktop answer follows it. Resolved here
    // rather than at runtime because a packaged app has no settings file to read the
    // shared hint back out of -- this properties file IS the answer.
    @Test
    void theSharedNativeHintDoesChangeThePackagedDesktopDefault(@TempDir Path root) throws Exception {
        GenerateDesktopAppWrapperMojo mojo = new GenerateDesktopAppWrapperMojo();
        mojo.project = new MavenProject();
        mojo.project.getBuild().setOutputDirectory(root.toString());
        mojo.properties = new Properties();
        for (String hint : new String[]{"nativeTheme", "cn1.nativeTheme"}) {
            mojo.properties.clear();
            mojo.properties.setProperty("codename1.arg." + hint, "native");
            mojo.generateThemeConfiguration();
            Properties packaged = new Properties();
            try (InputStream in = Files.newInputStream(root.resolve("codenameone-desktop.properties"))) {
                packaged.load(in);
            }
            assertEquals("native", packaged.getProperty("desktop.themeMode"), hint);

            // An explicit desktop hint outranks it.
            mojo.properties.setProperty("codename1.arg.desktop.themeMode", "legacy");
            mojo.generateThemeConfiguration();
            try (InputStream in = Files.newInputStream(root.resolve("codenameone-desktop.properties"))) {
                packaged.clear();
                packaged.load(in);
            }
            assertEquals("legacy", packaged.getProperty("desktop.themeMode"), hint);
        }
    }

    /**
     * A packaged desktop app has no settings file at runtime, so what this goal writes is
     * all it ever reads. The device builds and cn1:run merge the hints declared as
     * annotations; this goal read only codenameone_settings.properties, so an app that
     * said {@code @DesktopBuild(themeMode = "aqua")} previewed as Aqua and shipped as
     * legacy. Drives the real merge against a real processor fingerprint.
     */
    @Test
    void annotationDeclaredHintsReachThePackagedApp(@TempDir Path root) throws Exception {
        final File common = root.resolve("common-classes").toFile();
        final File annotations = new File(Class.forName("com.codename1.annotations.buildhints.DesktopBuild")
                .getProtectionDomain().getCodeSource().getLocation().toURI());
        com.codename1.maven.annotations.JavaSourceCompiler.compile(
                com.codename1.maven.annotations.JavaSourceCompiler.singleSource(
                        "com.example.MyApp",
                        "package com.example;\n"
                                + "import com.codename1.annotations.buildhints.*;\n"
                                + "@DesktopBuild(themeMode = \"aqua\")\n"
                                + "public class MyApp {\n}\n"),
                common, java.util.Collections.singletonList(annotations));
        String digest = com.codename1.maven.processors.BuildHintAnnotationProcessor.sourceDigest(
                com.codename1.maven.annotations.ClassScanner.readClass(
                        new File(common, "com/example/MyApp.class")));
        Path manifest = common.toPath().resolve("META-INF/codenameone/build-hints.properties");
        Files.createDirectories(manifest.getParent());
        Files.write(manifest, ("cn1.buildHints.mainClass=com.example.MyApp\n"
                + "cn1.buildHints.sourceDigest=" + digest + "\n"
                + "codename1.arg.desktop.themeMode=aqua\n").getBytes(StandardCharsets.ISO_8859_1));

        GenerateDesktopAppWrapperMojo mojo = new GenerateDesktopAppWrapperMojo();
        // The javase module: its own output, then the common module it depends on.
        final File javaseClasses = root.resolve("javase-classes").toFile();
        mojo.project = new MavenProject() {
            @Override
            public java.util.List<String> getCompileClasspathElements() {
                return java.util.Arrays.asList(javaseClasses.getAbsolutePath(),
                        common.getAbsolutePath(), annotations.getAbsolutePath());
            }
        };
        mojo.project.getBuild().setOutputDirectory(javaseClasses.getAbsolutePath());
        mojo.properties = new Properties();
        mojo.properties.setProperty("codename1.packageName", "com.example");
        mojo.properties.setProperty("codename1.mainName", "MyApp");

        mojo.applyAnnotationBuildHints();
        mojo.generateThemeConfiguration();

        Properties packaged = new Properties();
        try (InputStream in = Files.newInputStream(javaseClasses.toPath().resolve("codenameone-desktop.properties"))) {
            packaged.load(in);
        }
        assertEquals("aqua", packaged.getProperty("desktop.themeMode"),
                "the annotation's value must be what the packaged app installs");
    }

    @Test
    void defaultWrapperPreservesPlatformFontsAndExplicitOverrides() throws Exception {
        String source = render(null, null);
        assertFalse(source.contains("setFontFaces(\"Arial"));
        assertTrue(source.contains("JavaSEPort.setFontFaces(fontFaces[0], fontFaces[1], fontFaces[2])"));
    }

}
