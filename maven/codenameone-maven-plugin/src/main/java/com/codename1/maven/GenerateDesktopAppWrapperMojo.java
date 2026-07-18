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

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static com.codename1.maven.PathUtil.path;

/**
 * Generates the desktop SE wrapper (the {@code <MainName>Stub} class containing
 * the {@code main(String[])} that boots the Swing JFrame and instantiates the
 * Codename One {@code Lifecycle} subclass). This Mojo is the JavaSE analogue of
 * the stub generation that {@link com.codename1.builders.AndroidGradleBuilder}
 * and {@link com.codename1.builders.IPhoneBuilder} do for their respective
 * targets - taking values from {@code codenameone_settings.properties} (and
 * {@code codename1.arg.desktop.*} build hints) and emitting the platform's
 * entry-point class so the app-level project does not have to ship a
 * hand-maintained stub.
 *
 * <p>If the developer drops {@code javase/src/desktop/java/<pkg>/<MainName>Stub.java}
 * by hand it wins: this Mojo will skip generation, and the existing source-root
 * registration below picks it up. That makes the static file a complete
 * override - useful when an app needs custom Swing setup that's outside what
 * the build-hint surface covers.
 */
@Mojo(name="generate-desktop-app-wrapper")
public class GenerateDesktopAppWrapperMojo extends AbstractCN1Mojo {
    private static final String GENERATED_SOURCES_DIR = "cn1-desktop";

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        generateIcons();
        generateStub();
        registerCustomStubSourceRoot();
    }

    private void generateIcons() throws MojoExecutionException {
        String iconPath = properties.getProperty("codename1.icon");
        if (iconPath == null) {
            getLog().warn("codename1.icon not set in codenameone_settings.properties.  Skipping desktop app icon generation.");
            return;
        }
        File iconFile = new File(iconPath);
        if (!iconFile.isAbsolute()) {
            iconFile = new File(getCN1ProjectDir(), iconFile.getPath());
        }
        if (!iconFile.exists()) {
            getLog().warn("Icon file "+iconFile+" not found.  Skipping desktop app icon generation.");
            return;
        }
        File outputDir = new File(project.getBuild().getOutputDirectory());
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        try (FileInputStream fis = new FileInputStream(iconFile)) {
            getLog().debug("Creating Application Icons");
            BufferedImage iconImage = ImageIO.read(fis);
            createIconFile(new File(outputDir, "applicationIconImage_16x16.png"), iconImage, 16, 16);
            createIconFile(new File(outputDir, "applicationIconImage_20x20.png"), iconImage, 20, 20);
            createIconFile(new File(outputDir, "applicationIconImage_32x32.png"), iconImage, 32, 32);
            createIconFile(new File(outputDir, "applicationIconImage_40x40.png"), iconImage, 40, 40);
            createIconFile(new File(outputDir, "applicationIconImage_64x64.png"), iconImage, 64, 64);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to generate icons", ex);
        }
    }

    private void registerCustomStubSourceRoot() {
        File wrapperSources = new File(project.getBasedir(), path("src", "desktop", "java"));
        if (wrapperSources.exists()) {
            project.addCompileSourceRoot(wrapperSources.getAbsolutePath());
        }
    }

    private void generateStub() throws MojoExecutionException {
        String packageName = properties.getProperty("codename1.packageName");
        String mainName = properties.getProperty("codename1.mainName");
        if (packageName == null || mainName == null) {
            getLog().warn("codename1.packageName or codename1.mainName not set in codenameone_settings.properties. Skipping desktop stub generation.");
            return;
        }

        // If the developer hand-rolls a stub under src/desktop/java/<pkg>/<MainName>Stub.java
        // treat it as a full override and skip generation - the source root that
        // registerCustomStubSourceRoot adds will pick it up.
        String packagePath = packageName.replace('.', File.separatorChar);
        File customStub = new File(project.getBasedir(),
                path("src", "desktop", "java", packagePath, mainName + "Stub.java"));
        if (customStub.exists()) {
            getLog().info("Custom desktop stub found at " + customStub.getAbsolutePath() + " - skipping generation.");
            return;
        }

        File generatedRoot = new File(project.getBuild().getDirectory(),
                path("generated-sources", GENERATED_SOURCES_DIR));
        File generatedPkgDir = new File(generatedRoot, packagePath);
        generatedPkgDir.mkdirs();
        File generatedStub = new File(generatedPkgDir, mainName + "Stub.java");

        String template;
        try (InputStream in = getClass().getResourceAsStream("desktop-app-stub-template.java")) {
            if (in == null) {
                throw new MojoExecutionException("Could not load desktop-app-stub-template.java resource from plugin jar.");
            }
            template = IOUtils.toString(in, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to read desktop stub template", ex);
        }

        String stubSource = applyTemplate(template, packageName, mainName);

        try {
            Files.write(generatedStub.toPath(), stubSource.getBytes(StandardCharsets.UTF_8));
            getLog().info("Generated desktop stub: " + generatedStub.getAbsolutePath());
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to write generated desktop stub " + generatedStub, ex);
        }

        project.addCompileSourceRoot(generatedRoot.getAbsolutePath());
    }

    // package-private for unit testing the build-hint -> generated-stub substitution
    String applyTemplate(String template, String packageName, String mainName) {
        // Default APP_TITLE to displayName; fall back to mainName so we never inject an empty literal.
        String displayName = properties.getProperty("codename1.displayName", mainName);
        String appTitle = arg("desktop.title", displayName);
        String appVersion = properties.getProperty("codename1.version", "1.0");

        String width = arg("desktop.width", "800");
        String height = arg("desktop.height", "600");
        String resizable = arg("desktop.resizable", "true");
        String fullscreen = arg("desktop.fullscreen", "false");
        String adaptToRetina = arg("desktop.adaptToRetina", "true");
        String titleBar = arg("desktop.titleBar", "native");
        String interactiveScrollbars = arg("desktop.interactiveScrollbars", "true");

        String result = template;
        result = result.replace("__PACKAGE__", packageName);
        result = result.replace("__MAIN_NAME__", mainName);
        result = result.replace("__APP_TITLE__", escapeJavaStringLiteral(appTitle));
        result = result.replace("__APP_VERSION__", escapeJavaStringLiteral(appVersion));
        result = result.replace("__APP_WIDTH__", sanitizeInt(width, "800"));
        result = result.replace("__APP_HEIGHT__", sanitizeInt(height, "600"));
        result = result.replace("__APP_RESIZEABLE__", sanitizeBoolean(resizable, true));
        result = result.replace("__APP_FULLSCREEN__", sanitizeBoolean(fullscreen, false));
        result = result.replace("__APP_ADAPT_TO_RETINA__", sanitizeBoolean(adaptToRetina, true));
        result = result.replace("__APP_DESKTOP_TITLEBAR__", sanitizeTitleBarMode(titleBar));
        result = result.replace("__APP_DESKTOP_INTERACTIVE_SCROLLBARS__", sanitizeBoolean(interactiveScrollbars, true));
        return result;
    }

    private String sanitizeTitleBarMode(String value) {
        if (value != null) {
            String trimmed = value.trim().toLowerCase();
            if ("native".equals(trimmed) || "custom".equals(trimmed) || "toolbar".equals(trimmed)) {
                return trimmed;
            }
        }
        getLog().warn("Invalid desktop.titleBar build hint: '" + value + "'. Using 'native'.");
        return "native";
    }

    private String arg(String name, String defaultValue) {
        String v = properties.getProperty("codename1.arg." + name);
        return (v == null || v.isEmpty()) ? defaultValue : v;
    }

    private String sanitizeInt(String value, String fallback) {
        try {
            return String.valueOf(Integer.parseInt(value.trim()));
        } catch (NumberFormatException ex) {
            getLog().warn("Invalid integer in desktop build hint: '" + value + "'. Using " + fallback + ".");
            return fallback;
        }
    }

    private String sanitizeBoolean(String value, boolean fallback) {
        if (value == null) return String.valueOf(fallback);
        String trimmed = value.trim().toLowerCase();
        if ("true".equals(trimmed) || "false".equals(trimmed)) {
            return trimmed;
        }
        getLog().warn("Invalid boolean in desktop build hint: '" + value + "'. Using " + fallback + ".");
        return String.valueOf(fallback);
    }

    private String escapeJavaStringLiteral(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': out.append("\\\\"); break;
                case '"':  out.append("\\\""); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    out.append(c);
            }
        }
        return out.toString();
    }

    private void createIconFile(File f, BufferedImage icon, int w, int h) throws IOException {
        ImageIO.write(getScaledInstance(icon, w, h), "png", f);
    }

    private BufferedImage getScaledInstance(BufferedImage img,
                                              int targetWidth,
                                              int targetHeight) {
        BufferedImage ret = (BufferedImage) img;
        int w, h;
        // Use multi-step technique: start with original size, then
        // scale down in multiple passes with drawImage()
        // until the target size is reached
        w = img.getWidth();
        h = img.getHeight();

        if (w < targetWidth && h < targetHeight) {
            BufferedImage b = new BufferedImage(targetWidth, targetHeight, img.getType());
            Graphics2D g2d = b.createGraphics();
            g2d.drawImage(img, 0, 0, targetWidth, targetHeight, null);
            g2d.dispose();
            return b;
        }

        do {
            if (w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }

}
