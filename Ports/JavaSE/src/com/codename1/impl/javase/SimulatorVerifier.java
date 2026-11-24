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
package com.codename1.impl.javase;

import com.codename1.impl.javase.record.RecordingOptions;
import com.codename1.io.JSONParser;
import com.codename1.mcp.MCPServer;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.accessibility.AccessibilityAction;
import com.codename1.ui.accessibility.AccessibilityManager;
import com.codename1.ui.accessibility.AccessibilityNodeSnapshot;
import com.codename1.ui.accessibility.AccessibilityTreeSnapshot;
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Checks the current screen of the simulator under several conditions -- light and
/// dark, portrait and landscape, the largest text size -- and writes a JSON and HTML
/// report with a screenshot per condition. Each condition is applied, the frozen agent
/// clock is settled, the screen is checked, and every setting is put back afterwards.
///
/// The checks are deliberately few and literal, so a finding is always worth reading:
/// an activatable control with no accessible label, a control with no size, text that
/// a label truncates or that runs past a parent that cannot scroll, and text whose
/// colour contrast with its solid background is below 3:1 (WCAG's floor for large
/// text). Backgrounds that are images or gradients are not judged.
final class SimulatorVerifier {
    /// Contrast below this is reported.
    private static final double MIN_CONTRAST = 3.0;
    private static final String ENV_DIR = "CN1_VERIFY_DIR";

    private final JavaSEPort port;
    private final MCPServer server;

    SimulatorVerifier(JavaSEPort port, MCPServer server) {
        this.port = port;
        this.server = server;
    }

    /// `cn1:verify` starts the simulator with `CN1_VERIFY_DIR` set: wait for the first
    /// form, verify it, write the report there and exit. The goal reads the report.
    static void startIfRequested(final JavaSEPort port, final MCPServer server) {
        final String dir = System.getenv(ENV_DIR);
        if (dir == null || dir.trim().length() == 0) {
            return;
        }
        Thread t = new Thread(new Runnable() {
            public void run() {
                int status = 0;
                try {
                    long deadline = System.currentTimeMillis() + 120000;
                    while (Display.getInstance().getCurrent() == null) {
                        if (System.currentTimeMillis() > deadline) {
                            throw new IllegalStateException("The application showed no form within two minutes");
                        }
                        Thread.sleep(200);
                    }
                    Map<String, Object> report = new SimulatorVerifier(port, server).verify(new File(dir.trim()));
                    System.out.println("[cn1.verify] " + report.get("errors") + " error(s), "
                            + report.get("warnings") + " warning(s): " + new File(dir.trim(), "report.html"));
                } catch (Throwable ex) {
                    status = 2;
                    System.err.println("[cn1.verify] failed: " + ex);
                    ex.printStackTrace();
                    writeFailure(new File(dir.trim()), String.valueOf(ex));
                }
                System.exit(status);
            }
        }, "cn1-verify");
        t.setDaemon(true);
        t.start();
    }

    private static void writeFailure(File dir, String message) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("failed", message);
        try {
            writeText(new File(dir, "report.json"), JSONParser.toJson(out));
        } catch (IOException ignored) {
            // Nothing more can be reported; the exit status says it failed.
        }
    }

    /// Runs every condition on the current form. Called from the MCP server's thread.
    Map<String, Object> verify(File dir) throws IOException {
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Cannot create " + dir);
        }
        boolean wasFrozen = AnimationTime.isOverridden();
        final Boolean[] darkBefore = new Boolean[1];
        onEdt(new Runnable() {
            public void run() {
                darkBefore[0] = Display.getInstance().isDarkMode();
            }
        });
        boolean portraitBefore = port.isSimulatorPortrait();
        boolean largerBefore = port.isLargerTextEnabled();
        float scaleBefore = port.getLargerTextScale();
        List<Object> conditions = new ArrayList<Object>();
        BufferedImage lightShot = null;
        int errors = 0;
        int warnings = 0;
        tool("ui_clock_freeze", "{}");
        try {
            String[][] plan = {
                {"light-portrait", "light", "portrait", "normal"},
                {"dark-portrait", "dark", "portrait", "normal"},
                {"light-landscape", "light", "landscape", "normal"},
                {"largest-text", "light", "portrait", "largest"},
            };
            for (String[] c : plan) {
                Map<String, Object> result = new LinkedHashMap<String, Object>();
                result.put("name", c[0]);
                if ("largest".equals(c[3]) && !UIManager.getInstance().isUseLargerTextScale()) {
                    // The app has not opted into font scaling, so a larger text setting
                    // changes nothing; reporting it clean would be a lie.
                    result.put("skipped", "The app does not opt into larger text (theme constant "
                            + "useLargerTextScaleBool, or UIManager.setUseLargerTextScale(true))");
                    conditions.add(result);
                    continue;
                }
                apply("dark".equals(c[1]), "portrait".equals(c[2]), "largest".equals(c[3]) ? 2.0f : 1.0f);
                List<Map<String, Object>> findings = check();
                for (Map<String, Object> f : findings) {
                    if ("error".equals(f.get("severity"))) {
                        errors++;
                    } else {
                        warnings++;
                    }
                }
                String png = c[0] + ".png";
                BufferedImage shot = port.captureSimulatorFrame(RecordingOptions.Frame.SCREEN);
                if ("light-portrait".equals(c[0])) {
                    lightShot = shot;
                } else if ("dark-portrait".equals(c[0]) && samePixels(lightShot, shot)) {
                    Map<String, Object> f = finding("warning", "no-dark-theme", null,
                            "Dark mode draws exactly what light mode draws",
                            "Add dark styles (CSS @media (prefers-color-scheme: dark)) or a theme with dark variants");
                    findings.add(f);
                    warnings++;
                }
                if (shot != null && javax.imageio.ImageIO.write(shot, "png", new File(dir, png))) {
                    result.put("screenshot", png);
                }
                result.put("findings", findings);
                conditions.add(result);
            }
        } finally {
            port.setSimulatorLargerTextScale(largerBefore ? scaleBefore : 1.0f);
            if (port.isSimulatorPortrait() != portraitBefore) {
                port.rotateSimulator(portraitBefore);
                waitForRotation(portraitBefore);
            }
            onEdt(new Runnable() {
                public void run() {
                    Display.getInstance().setDarkMode(darkBefore[0]);
                    refreshTheme();
                }
            });
            tool("ui_clock_settle", "{\"maxMs\":3000}");
            if (!wasFrozen) {
                tool("ui_clock_release", "{}");
            }
        }
        Map<String, Object> report = new LinkedHashMap<String, Object>();
        // The counts come first: cn1:verify reads the first "errors" in the file, and a
        // form title is app text.
        report.put("errors", Integer.valueOf(errors));
        report.put("warnings", Integer.valueOf(warnings));
        Form f = Display.getInstance().getCurrent();
        report.put("form", f == null ? null : f.getTitle());
        report.put("conditions", conditions);
        writeText(new File(dir, "report.json"), JSONParser.toJson(report));
        writeText(new File(dir, "report.html"), html(report));
        report.put("report", new File(dir, "report.html").getAbsolutePath());
        return report;
    }

    private void apply(final boolean dark, boolean portrait, float textScale) {
        if (port.isSimulatorPortrait() != portrait) {
            port.rotateSimulator(portrait);
            waitForRotation(portrait);
        }
        port.setSimulatorLargerTextScale(textScale);
        onEdt(new Runnable() {
            public void run() {
                Display.getInstance().setDarkMode(Boolean.valueOf(dark));
                refreshTheme();
            }
        });
        tool("ui_clock_settle", "{\"maxMs\":3000}");
    }

    /// Rotation is applied on the AWT thread and reaches the form as a size change; wait
    /// (wall time, bounded) until the display agrees.
    private void waitForRotation(boolean portrait) {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            Display d = Display.getInstance();
            boolean nowPortrait = d.getDisplayHeight() >= d.getDisplayWidth();
            if (nowPortrait == portrait) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static void refreshTheme() {
        UIManager.getInstance().refreshTheme();
        Form curr = Display.getInstance().getCurrent();
        if (curr != null) {
            curr.refreshTheme(true);
            curr.revalidate();
            curr.repaint();
        }
    }

    private List<Map<String, Object>> check() {
        final List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        onEdt(new Runnable() {
            public void run() {
                Form form = Display.getInstance().getCurrent();
                if (form == null) {
                    return;
                }
                checkAccessibility(out);
                walk(form, out);
            }
        });
        return out;
    }

    private static void checkAccessibility(List<Map<String, Object>> out) {
        AccessibilityTreeSnapshot snap = AccessibilityManager.getInstance().getCurrentSnapshot();
        if (snap == null) {
            return;
        }
        for (AccessibilityNodeSnapshot node : snap.getNodes().values()) {
            if (node.getAction(AccessibilityAction.ACTIVATE) == null) {
                continue;
            }
            Component c = node.getComponent();
            if (c != null && !c.isVisible()) {
                continue;
            }
            String label = node.getLabel();
            if (label == null || label.trim().length() == 0) {
                out.add(finding("error", "unlabeled-control", c,
                        "An activatable control has no accessible label; a screen reader announces nothing",
                        "Give it text, or call setAccessibilityText(...)"));
            }
            Rectangle b = node.getBounds();
            if (b != null && (b.getWidth() <= 0 || b.getHeight() <= 0)) {
                out.add(finding("error", "zero-size-control", c,
                        "An activatable control has no area and cannot be tapped",
                        "Check its layout constraint and preferred size"));
            }
        }
    }

    private static void walk(Container parent, List<Map<String, Object>> out) {
        for (int i = 0; i < parent.getComponentCount(); i++) {
            Component c = parent.getComponentAt(i);
            if (!c.isVisible() || c.getWidth() <= 0 || c.getHeight() <= 0) {
                continue;
            }
            if (!parent.isScrollableX() && c.getX() + c.getWidth() > parent.getWidth() - parent.getStyle().getPaddingRightNoRTL() + 1
                    && parent.getWidth() > 0) {
                out.add(finding("warning", "overflows-parent", c,
                        "Runs " + (c.getX() + c.getWidth() - parent.getWidth()) + "px past a parent that cannot scroll sideways",
                        "Let the text wrap, shrink the content, or make the parent scrollable"));
            }
            if (c instanceof Label) {
                Label l = (Label) c;
                String text = l.getText();
                if (text != null && text.length() > 0) {
                    if (l.getPreferredW() > l.getWidth() + 1) {
                        out.add(finding("error", "truncated-text", c,
                                "\"" + shorten(text) + "\" needs " + l.getPreferredW() + "px but has " + l.getWidth()
                                        + "px, so it is cut off",
                                "Use a SpanLabel/TextArea for text that can grow, or give the label room"));
                    }
                    Double ratio = contrast(c);
                    if (ratio != null && ratio.doubleValue() < MIN_CONTRAST) {
                        out.add(finding("error", "low-contrast", c,
                                "\"" + shorten(text) + "\" has a contrast of "
                                        + Math.round(ratio.doubleValue() * 100) / 100.0 + ":1 with its background",
                                "Use a text colour at least 3:1 against the background (4.5:1 for body text)"));
                    }
                }
            }
            if (c instanceof Container) {
                walk((Container) c, out);
            }
        }
    }

    /// The contrast between a component's text colour and the nearest solid background
    /// behind it, or null when that background is an image or gradient or is not found.
    static Double contrast(Component c) {
        int fg = c.getStyle().getFgColor();
        Component p = c;
        while (p != null) {
            Style s = p.getStyle();
            if (s.getBgImage() != null) {
                return null;
            }
            if ((s.getBgTransparency() & 0xff) == 0xff) {
                if (s.getBackgroundType() != Style.BACKGROUND_NONE
                        && s.getBackgroundType() != Style.BACKGROUND_IMAGE_SCALED) {
                    return null;
                }
                return Double.valueOf(ratio(fg, s.getBgColor()));
            }
            p = p.getParent();
        }
        return null;
    }

    static double ratio(int a, int b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(int rgb) {
        return 0.2126 * channel((rgb >> 16) & 0xff) + 0.7152 * channel((rgb >> 8) & 0xff)
                + 0.0722 * channel(rgb & 0xff);
    }

    private static double channel(int v) {
        double c = v / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static Map<String, Object> finding(String severity, String id, Component c, String message, String fix) {
        Map<String, Object> f = new LinkedHashMap<String, Object>();
        f.put("severity", severity);
        f.put("id", id);
        if (c != null) {
            f.put("component", c.getClass().getSimpleName()
                    + (c.getName() != null ? " '" + c.getName() + "'" : "")
                    + " at " + c.getAbsoluteX() + "," + c.getAbsoluteY() + " " + c.getWidth() + "x" + c.getHeight());
        }
        f.put("message", message);
        f.put("fix", fix);
        return f;
    }

    private static boolean samePixels(BufferedImage a, BufferedImage b) {
        if (a == null || b == null || a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return false;
        }
        int w = a.getWidth();
        int[] ra = new int[w];
        int[] rb = new int[w];
        for (int y = 0; y < a.getHeight(); y++) {
            a.getRGB(0, y, w, 1, ra, 0, w);
            b.getRGB(0, y, w, 1, rb, 0, w);
            if (!java.util.Arrays.equals(ra, rb)) {
                return false;
            }
        }
        return true;
    }

    private static String shorten(String s) {
        return s.length() > 40 ? s.substring(0, 37) + "..." : s;
    }

    private void tool(String name, String args) {
        String reply = server.handleMessage("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\""
                + name + "\",\"arguments\":" + args + "}}");
        if (reply != null && reply.indexOf("\"isError\":true") >= 0) {
            throw new IllegalStateException(name + " failed: " + reply);
        }
    }

    private static void onEdt(final Runnable r) {
        Display d = Display.getInstance();
        if (d.isEdt()) {
            r.run();
        } else {
            d.callSeriallyAndWait(r);
        }
    }

    @SuppressWarnings("unchecked")
    private static String html(Map<String, Object> report) {
        StringBuilder b = new StringBuilder();
        b.append("<!doctype html><html><head><meta charset=\"utf-8\"><title>cn1:verify</title><style>")
                .append("body{font-family:sans-serif;margin:16px}section{margin-bottom:24px}")
                .append("img{max-width:240px;border:1px solid #888;float:left;margin-right:16px}")
                .append(".error{color:#b00020}.warning{color:#8a5a00}ul{overflow:hidden}</style></head><body>");
        b.append("<h1>").append(esc(String.valueOf(report.get("form")))).append(": ")
                .append(report.get("errors")).append(" error(s), ").append(report.get("warnings"))
                .append(" warning(s)</h1>");
        for (Object o : (List<Object>) report.get("conditions")) {
            Map<String, Object> c = (Map<String, Object>) o;
            b.append("<section><h2>").append(esc(String.valueOf(c.get("name")))).append("</h2>");
            if (c.get("skipped") != null) {
                b.append("<p>Skipped: ").append(esc(String.valueOf(c.get("skipped")))).append("</p></section>");
                continue;
            }
            if (c.get("screenshot") != null) {
                b.append("<img alt=\"\" src=\"").append(esc(String.valueOf(c.get("screenshot")))).append("\">");
            }
            b.append("<ul>");
            List<Object> findings = (List<Object>) c.get("findings");
            if (findings.isEmpty()) {
                b.append("<li>Clean</li>");
            }
            for (Object fo : findings) {
                Map<String, Object> f = (Map<String, Object>) fo;
                b.append("<li class=\"").append(esc(String.valueOf(f.get("severity")))).append("\"><b>")
                        .append(esc(String.valueOf(f.get("id")))).append("</b> ")
                        .append(esc(String.valueOf(f.get("message")))).append(" &mdash; ")
                        .append(esc(String.valueOf(f.get("component")))).append(". <i>")
                        .append(esc(String.valueOf(f.get("fix")))).append("</i></li>");
            }
            b.append("</ul></section>");
        }
        return b.append("</body></html>").toString();
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static void writeText(File f, String text) throws IOException {
        Writer w = new OutputStreamWriter(new java.io.FileOutputStream(f), StandardCharsets.UTF_8);
        try {
            w.write(text);
        } finally {
            w.close();
        }
    }
}
