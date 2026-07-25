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
package com.codename1.settings;

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.swing.JFrame;

/**
 * Dumps the desktop render state of the Settings tool to a text file.
 *
 * The Control Center is a Codename One app hosted in a Swing frame, so "it
 * opened but painted nothing" (issue #5443) can come from the window layer,
 * the HiDPI scale, the theme, or font loading - and a screenshot alone cannot
 * tell those apart. This dump records all four in one place so a user report
 * or a CI artifact is actionable without a round trip:
 *
 * <pre>mvn cn1:settings -Dsettings.diagnostics=cn1-settings-diag.txt</pre>
 *
 * Everything here is best effort: a diagnostic dump must never be the reason
 * the tool fails to start, so every probe degrades to a marker string.
 */
final class SettingsDiagnostics {
    /**
     * UIIDs sampled from the loaded theme. If the CSS theme did not load these
     * all fall back to the built-in {@link Style} defaults (white background,
     * black text), which is itself the diagnosis.
     */
    static final String[] SAMPLED_UIIDS = {
        "SettingsForm", "SettingsFormDark",
        "SettingsChrome", "SettingsChromeDark",
        "SettingsPage", "SettingsPageDark",
        "SettingsPageTitle", "SettingsPageTitleDark",
        "SettingsField", "SettingsFieldDark"
    };

    private SettingsDiagnostics() {
    }

    /**
     * Renders the report. Call on the Codename One EDT: the UIID and current
     * form probes read live component state.
     */
    static String describe(JFrame frame) {
        StringBuilder out = new StringBuilder();
        out.append("# Codename One Settings diagnostics\n");
        out.append("edt.responsive=true\n");
        appendEnvironment(out);
        appendDisplayScale(out);
        appendWindow(out, frame);
        appendCodenameOne(out);
        appendTheme(out);
        return out.toString();
    }

    /**
     * Fallback report for when the Codename One EDT never ran the probe. The
     * EDT-independent sections still describe the window and the scale, and
     * the thread dump names whatever the EDT is stuck on.
     */
    static void writeUnresponsiveEdt(File target, JFrame frame) {
        StringBuilder out = new StringBuilder();
        out.append("# Codename One Settings diagnostics\n");
        out.append("edt.responsive=false\n");
        try {
            appendEnvironment(out);
            appendDisplayScale(out);
            appendWindow(out, frame);
        } catch (Throwable err) {
            out.append("diagnostics.failed=").append(err).append('\n');
        }
        appendEdtStack(out);
        writeReport(target, out.toString());
    }

    private static void appendEdtStack(StringBuilder out) {
        out.append("\n[edt-stack]\n");
        try {
            for (java.util.Map.Entry<Thread, StackTraceElement[]> entry
                    : Thread.getAllStackTraces().entrySet()) {
                String name = entry.getKey().getName();
                if (!name.contains("EDT") && !name.startsWith("AWT-EventQueue")) {
                    continue;
                }
                out.append("thread=").append(name)
                        .append(" state=").append(entry.getKey().getState()).append('\n');
                for (StackTraceElement frame : entry.getValue()) {
                    out.append("    at ").append(frame).append('\n');
                }
            }
        } catch (Throwable err) {
            out.append("edt-stack.failed=").append(err).append('\n');
        }
    }

    static void write(File target, JFrame frame) {
        String report;
        try {
            report = describe(frame);
        } catch (Throwable err) {
            report = "# Codename One Settings diagnostics\ndiagnostics.failed=" + err + "\n";
        }
        writeReport(target, report);
    }

    private static void writeReport(File target, String report) {
        try {
            File parent = target.getAbsoluteFile().getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            Files.write(target.toPath(), report.getBytes(StandardCharsets.UTF_8));
        } catch (IOException err) {
            System.err.println("Failed to write Settings diagnostics to " + target + ": " + err);
        }
    }

    private static void appendEnvironment(StringBuilder out) {
        out.append("\n[environment]\n");
        prop(out, "os.name");
        prop(out, "os.version");
        prop(out, "os.arch");
        prop(out, "java.version");
        prop(out, "java.vm.name");
        prop(out, "java.home");
        prop(out, "user.language");
        prop(out, "user.country");
        prop(out, "file.encoding");
        prop(out, "sun.java2d.uiScale");
        prop(out, "sun.java2d.d3d");
        prop(out, "sun.java2d.opengl");
        prop(out, "cn1.retinaScale");
        kv(out, "settings.version", System.getProperty("settings.version", "?"));
        kv(out, "headless", String.valueOf(GraphicsEnvironment.isHeadless()));
    }

    private static void appendDisplayScale(StringBuilder out) {
        out.append("\n[display]\n");
        try {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            kv(out, "toolkit.screenResolutionDpi", String.valueOf(toolkit.getScreenResolution()));
            GraphicsConfiguration config = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration();
            AffineTransform tx = config.getDefaultTransform();
            kv(out, "graphicsConfig.scaleX", String.valueOf(tx.getScaleX()));
            kv(out, "graphicsConfig.scaleY", String.valueOf(tx.getScaleY()));
            kv(out, "graphicsConfig.bounds", String.valueOf(config.getBounds()));
        } catch (Throwable err) {
            kv(out, "display.failed", String.valueOf(err));
        }
        kv(out, "javase.retinaScale", String.valueOf(JavaSEPort.getRetinaScale()));
        kv(out, "javase.defaultPixelMilliRatio", String.valueOf(JavaSEPort.getDefaultPixelMilliRatio()));
    }

    private static void appendWindow(StringBuilder out, JFrame frame) {
        out.append("\n[window]\n");
        if (frame == null) {
            kv(out, "frame", "null");
            return;
        }
        kv(out, "frame.bounds", String.valueOf(frame.getBounds()));
        kv(out, "frame.visible", String.valueOf(frame.isVisible()));
        kv(out, "frame.contentPaneSize", String.valueOf(frame.getContentPane().getSize()));
        kv(out, "frame.contentPaneComponents", String.valueOf(frame.getContentPane().getComponentCount()));
        try {
            java.awt.Component canvas = frame.getContentPane().getComponentCount() > 0
                    ? frame.getContentPane().getComponent(0) : null;
            kv(out, "canvas.class", canvas == null ? "null" : canvas.getClass().getName());
            kv(out, "canvas.bounds", canvas == null ? "null" : String.valueOf(canvas.getBounds()));
        } catch (Throwable err) {
            kv(out, "canvas.failed", String.valueOf(err));
        }
    }

    private static void appendCodenameOne(StringBuilder out) {
        out.append("\n[codenameone]\n");
        if (!Display.isInitialized()) {
            kv(out, "display.initialized", "false");
            return;
        }
        Display display = Display.getInstance();
        kv(out, "display.initialized", "true");
        kv(out, "display.isEdt", String.valueOf(display.isEdt()));
        kv(out, "display.size", display.getDisplayWidth() + "x" + display.getDisplayHeight());
        kv(out, "display.density", String.valueOf(display.getDeviceDensity()));
        kv(out, "display.convertToPixels12mm", String.valueOf(display.convertToPixels(12f)));
        kv(out, "display.darkMode", String.valueOf(display.isDarkMode()));
        Form current = display.getCurrent();
        kv(out, "form", current == null ? "null" : current.getClass().getName());
        if (current != null) {
            kv(out, "form.uiid", current.getUIID());
            kv(out, "form.size", current.getWidth() + "x" + current.getHeight());
            kv(out, "form.componentCount", String.valueOf(current.getContentPane().getComponentCount()));
            describeStyle(out, "form.style", current.getStyle());
            kv(out, "form.paintedLeafCount", String.valueOf(countVisibleLeaves(current.getContentPane())));
        }
        Font system = Font.getDefaultFont();
        kv(out, "font.default.height", system == null ? "null" : String.valueOf(system.getHeight()));
        kv(out, "font.default.width.M", system == null ? "null" : String.valueOf(system.stringWidth("M")));
        Font main = safeNativeFont();
        kv(out, "font.nativeMainRegular.height", main == null ? "null" : String.valueOf(main.getHeight()));
        kv(out, "font.nativeMainRegular.width.M", main == null ? "null" : String.valueOf(main.stringWidth("M")));
    }

    private static void appendTheme(StringBuilder out) {
        out.append("\n[theme]\n");
        kv(out, "theme.resourcePresent",
                String.valueOf(SettingsDiagnostics.class.getResource("/theme.res") != null));
        kv(out, "nativeTheme.resourcePresent",
                String.valueOf(SettingsDiagnostics.class.getResource("/NativeTheme.res") != null));
        if (!Display.isInitialized()) {
            return;
        }
        for (String uiid : SAMPLED_UIIDS) {
            try {
                describeStyle(out, "uiid." + uiid, UIManager.getInstance().getComponentStyle(uiid));
            } catch (Throwable err) {
                kv(out, "uiid." + uiid, "failed: " + err);
            }
        }
    }

    private static void describeStyle(StringBuilder out, String prefix, Style style) {
        if (style == null) {
            kv(out, prefix, "null");
            return;
        }
        Font font = style.getFont();
        kv(out, prefix, "bg=#" + hex(style.getBgColor())
                + " fg=#" + hex(style.getFgColor())
                + " bgTransparency=" + (style.getBgTransparency() & 0xff)
                + " opacity=" + style.getOpacity()
                + " fontHeight=" + (font == null ? "null" : String.valueOf(font.getHeight()))
                + " fontWidthM=" + (font == null ? "null" : String.valueOf(font.stringWidth("M"))));
    }

    /**
     * Counts components that would actually put ink on the screen. A collapsed
     * layout (every text component measuring zero) and a healthy one are hard
     * to tell apart from a colour histogram but differ sharply here.
     */
    private static int countVisibleLeaves(Container root) {
        int count = 0;
        for (int iter = 0; iter < root.getComponentCount(); iter++) {
            Component child = root.getComponentAt(iter);
            if (child instanceof Container) {
                count += countVisibleLeaves((Container) child);
            } else if (child.getWidth() > 0 && child.getHeight() > 0) {
                count++;
            }
        }
        return count;
    }

    private static Font safeNativeFont() {
        try {
            return Font.createTrueTypeFont(CN.NATIVE_MAIN_REGULAR, CN.NATIVE_MAIN_REGULAR);
        } catch (Throwable err) {
            return null;
        }
    }

    private static String hex(int color) {
        String value = Integer.toHexString(color & 0xffffff);
        while (value.length() < 6) {
            value = "0" + value;
        }
        return value;
    }

    private static void prop(StringBuilder out, String key) {
        kv(out, key, System.getProperty(key, "<unset>"));
    }

    private static void kv(StringBuilder out, String key, String value) {
        out.append(key).append('=').append(value == null ? "null" : value).append('\n');
    }
}
