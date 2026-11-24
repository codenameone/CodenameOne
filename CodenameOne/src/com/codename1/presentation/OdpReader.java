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
package com.codename1.presentation;

import com.codename1.impl.office.OdfText;
import com.codename1.impl.office.ZipParts;
import com.codename1.impl.xml.XmlScanner;
import com.codename1.wordprocessing.Block;
import com.codename1.wordprocessing.Paragraph;
import com.codename1.wordprocessing.Picture;
import com.codename1.wordprocessing.RichText;
import com.codename1.wordprocessing.Table;
import com.codename1.wordprocessing.TextStyle;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// OpenDocument presentation (`.odp`) reader. Styles cascade from `styles.xml` (common and
/// automatic styles, the graphic default style) through `content.xml`'s automatic styles
/// along `style:parent-style-name`; master pages supply the slide size, the background and
/// the bounds and formatting of placeholders a slide's `presentation:class` shape leaves
/// out. Whatever has no model equivalent is reported through `Presentation.getWarnings()`.
final class OdpReader {
    private static final int MAX_DEPTH = 32;
    private static final int MAX_WARNINGS = 200;
    private static final int MAX_REPEAT = 256;
    private static final float MAX_COORD = 100000;

    /// Every property attribute the reader understands, from any `style:*-properties`.
    private static final String[] PROPS = {"draw:fill", "draw:fill-color",
        "draw:fill-gradient-name", "draw:fill-image-name", "draw:opacity", "draw:stroke",
        "draw:stroke-dash", "svg:stroke-width", "svg:stroke-color", "svg:stroke-opacity",
        "draw:marker-start", "draw:marker-end", "draw:textarea-vertical-align",
        "fo:padding", "fo:padding-left", "fo:padding-top", "fo:padding-right",
        "fo:padding-bottom", "fo:wrap-option", "draw:auto-grow-height", "style:shrink-to-fit",
        "fo:clip", "presentation:background-visible",
        "presentation:background-objects-visible","presentation:transition-type",
        "presentation:duration", "presentation:transition-speed", "smil:type",
        "smil:subtype", "smil:direction", "smil:dur", "presentation:visibility",
        "fo:text-align", "fo:margin-left", "fo:margin-right", "fo:text-indent",
        "fo:margin-top", "fo:margin-bottom", "fo:line-height", "fo:font-family",
        "style:font-name", "fo:font-size", "fo:color", "fo:font-weight", "fo:font-style",
        "style:text-underline-style", "style:text-line-through-style",
        "fo:background-color", "style:text-position", "style:column-width",
        "chart:vertical", "chart:stacked", "fo:page-width", "fo:page-height"};

    /// One named style: its parent and its own property attributes.
    private static final class Style {
        String parent;
        final Map<String, String> props = new HashMap<String, String>();
    }

    /// A master page: page layout, background and placeholder templates.
    private static final class Master {
        String layout;
        String pageStyle;
        final Map<String, float[]> boxes = new HashMap<String, float[]>();
        final Map<String, String> boxStyles = new HashMap<String, String>();
        final List<SlideElement> decoration = new ArrayList<SlideElement>();
    }

    /// A build as read, before its target is mapped to a model id.
    private static final class RawBuild {
        String target;
        String kind;
        String effect;
        String trigger;
        String direction;
        int duration;
        int delay;
    }

    private final Map<String, byte[]> parts;
    private final long[] budget = {XmlScanner.MAX_DOCUMENT};
    private final List<String> warnings = new ArrayList<String>();
    private final Map<String, Style> styles = new HashMap<String, Style>();
    private final Map<String, Map<String, String>> resolved =
            new HashMap<String, Map<String, String>>();
    private final Set<String> used = new HashSet<String>();
    private final Map<String, String> defaultGraphic = new HashMap<String, String>();
    private final Map<String, float[]> layouts = new HashMap<String, float[]>();
    private final Map<String, String[]> gradients = new HashMap<String, String[]>();
    private final Map<String, String> fillImages = new HashMap<String, String>();
    private final Map<String, String> markers = new HashMap<String, String>();
    private final Map<String, String> dashes = new HashMap<String, String>();
    private final Map<String, Boolean> listStyles = new HashMap<String, Boolean>();
    private final Map<String, Master> masters = new HashMap<String, Master>();
    private final Map<String, String> xmlIds = new HashMap<String, String>();
    private String firstMaster;
    private Master currentMaster;
    private boolean inMaster;
    private int slideNumber;
    private long next = 1;

    private OdpReader(Map<String, byte[]> parts) {
        this.parts = parts;
    }

    static Presentation read(InputStream in) throws IOException {
        return new OdpReader(ZipParts.read(in, "OpenDocument presentation")).run();
    }

    // ------------------------------------------------------------------ helpers

    private void warn(String w) {
        if (warnings.size() < MAX_WARNINGS && !warnings.contains(w)) {
            warnings.add(w);
        }
    }

    private String where() {
        return inMaster ? "Master page: " : "Slide " + slideNumber + ": ";
    }

    private String newId(char prefix) {
        String id = String.valueOf(prefix) + next++;
        while (used.contains(id)) {
            id = String.valueOf(prefix) + next++;
        }
        used.add(id);
        return id;
    }

    /// Takes an id from the file when it is unused, keeping `newId` past its number.
    private String adopt(String id, char prefix) {
        if (id == null || id.length() == 0 || id.length() > 64 || used.contains(id)) {
            return newId(prefix);
        }
        used.add(id);
        int i = id.length();
        while (i > 0 && id.charAt(i - 1) >= '0' && id.charAt(i - 1) <= '9') {
            i--;
        }
        if (i > 0 && i < id.length() && id.length() - i < 12) {
            next = Math.max(next, Long.parseLong(id.substring(i)) + 1);
        }
        return id;
    }

    private float coord(XmlScanner x, String attr, float fallback) {
        float f = OdfText.length(x.attr(attr), fallback);
        if (f > MAX_COORD || f < -MAX_COORD) {
            warn(where() + "an out of range " + attr + " was clamped");
        }
        return len(x.attr(attr), fallback);
    }

    private XmlScanner scanner(String part) throws IOException {
        byte[] b = parts.get(part);
        return b == null ? null : new XmlScanner(new ByteArrayInputStream(b), budget);
    }

    private static float len(String v, float fallback) {
        float f = OdfText.length(v, fallback);
        return Math.max(-MAX_COORD, Math.min(MAX_COORD, f));
    }

    private static float percent(String v, float fallback) {
        if (v == null || !v.endsWith("%")) {
            return fallback;
        }
        try {
            float f = Float.parseFloat(v.substring(0, v.length() - 1).trim()) / 100f;
            return Float.isNaN(f) ? fallback : Math.max(0, Math.min(1, f));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /// Milliseconds of a SMIL clock value (`0.5s`, `500ms`) or an ISO duration (`PT2S`).
    private static int millis(String v) {
        if (v == null) {
            return -1;
        }
        String s = v.trim();
        double scale = 1000;
        if (s.startsWith("PT") && s.endsWith("S")) {
            s = s.substring(2, s.length() - 1);
        } else if (s.endsWith("ms")) {
            s = s.substring(0, s.length() - 2);
            scale = 1;
        } else if (s.endsWith("s")) {
            s = s.substring(0, s.length() - 1);
        }
        try {
            double d = Double.parseDouble(s) * scale;
            if (Double.isNaN(d) || d < 0) {
                return -1;
            }
            return (int) Math.min(3600000, Math.round(d));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /// A whole-number attribute clamped to `[min, max]`, warning when the file asks for
    /// more; `XmlScanner.count` throws on that, which would abandon the element mid-parse.
    private int count(XmlScanner x, String attribute, int fallback, int min, int max) {
        String v = x.attr(attribute);
        if (v == null) {
            return fallback;
        }
        String t = v.trim();
        long n = 0;
        for (int i = 0; i < t.length(); i++) {
            char d = t.charAt(i);
            if (d < '0' || d > '9') {
                warn(where() + "the malformed " + attribute + " \"" + v + "\" was ignored");
                return fallback;
            }
            if (n <= Integer.MAX_VALUE) {
                n = n * 10 + (d - '0');
            }
        }
        if (t.length() == 0) {
            return fallback;
        }
        if (n > max) {
            warn(where() + "a " + attribute + " of " + v + " was capped at " + max);
            return max;
        }
        return (int) Math.max(min, n);
    }

    private static boolean contains(String s, String part) {
        return s != null && s.indexOf(part) >= 0;
    }

    // ------------------------------------------------------------------ styles

    private Map<String, String> resolve(String name) {
        if (name == null) {
            return new HashMap<String, String>();
        }
        Map<String, String> r = resolved.get(name);
        if (r == null) {
            r = new HashMap<String, String>();
            List<Style> chain = new ArrayList<Style>();
            String n = name;
            while (n != null && chain.size() < 16) {
                Style s = styles.get(n);
                if (s == null || chain.contains(s)) {
                    break;
                }
                chain.add(s);
                n = s.parent;
            }
            for (int i = chain.size() - 1; i >= 0; i--) {
                r.putAll(chain.get(i).props);
            }
            resolved.put(name, r);
        }
        return new HashMap<String, String>(r);
    }

    private static void capture(XmlScanner x, Map<String, String> into) {
        for (String a : PROPS) {
            String v = x.attr(a);
            if (v != null) {
                into.put(a, v);
            }
        }
    }

    /// Reads the children of a styles container (`office:styles`, automatic styles).
    private void styleSection(XmlScanner x) throws IOException {
        int depth = 1;
        while (depth > 0) {
            int e = x.next();
            if (e == XmlScanner.EOF) {
                return;
            }
            if (e == XmlScanner.END) {
                depth--;
                continue;
            }
            if (e != XmlScanner.START) {
                continue;
            }
            String n = x.name;
            if ("style".equals(n) || "default-style".equals(n)
                    || "page-layout".equals(n)) {
                styleElement(x, n);
            } else if ("gradient".equals(n)) {
                gradients.put(x.attr("draw:name"), new String[] {x.attr("draw:start-color"),
                    x.attr("draw:end-color"), x.attr("draw:angle"), x.attr("draw:style")});
                x.skipElement();
            } else if ("fill-image".equals(n)) {
                fillImages.put(x.attr("draw:name"), x.attr("xlink:href"));
                x.skipElement();
            } else if ("marker".equals(n)) {
                String name = x.attr("draw:name");
                String kind = LineStyle.ARROW_TRIANGLE;
                if (contains(name, "Circle") || contains(name, "circle")
                        || contains(name, "Oval")) {
                    kind = LineStyle.ARROW_OVAL;
                } else if (contains(name, "Line")) {
                    kind = LineStyle.ARROW_OPEN;
                }
                markers.put(name, kind);
                x.skipElement();
            } else if ("stroke-dash".equals(n)) {
                String name = x.attr("draw:name");
                String dl = x.attr("draw:dots1-length");
                float dots = dl != null && dl.endsWith("%") ? len(dl.substring(0, dl.length() - 1),
                        0) : 0;
                boolean dot = contains(name, "Dot") || contains(name, "dot")
                        || dots > 0 && dots <= 100 && x.attr("draw:dots2") == null;
                dashes.put(name, dot ? LineStyle.DOT : LineStyle.DASH);
                x.skipElement();
            } else if ("list-style".equals(n)) {
                listStyle(x);
            } else {
                x.skipElement();
            }
        }
    }

    private void listStyle(XmlScanner x) throws IOException {
        String name = x.attr("style:name");
        Boolean bullet = null;
        int depth = 1;
        while (depth > 0) {
            int e = x.next();
            if (e == XmlScanner.EOF) {
                break;
            }
            if (e == XmlScanner.START) {
                depth++;
                if (bullet == null && x.name.startsWith("list-level-style-")) {
                    bullet = Boolean.valueOf(!x.name.endsWith("number"));
                }
            } else if (e == XmlScanner.END) {
                depth--;
            }
        }
        if (name != null) {
            listStyles.put(name, bullet == null ? Boolean.TRUE : bullet);
        }
    }

    private void styleElement(XmlScanner x, String kind) throws IOException {
        Style s = new Style();
        String name = x.attr("style:name");
        String family = x.attr("style:family");
        s.parent = x.attr("style:parent-style-name");
        int depth = 1;
        while (depth > 0) {
            int e = x.next();
            if (e == XmlScanner.EOF) {
                break;
            }
            if (e == XmlScanner.START) {
                depth++;
                if (x.name.endsWith("-properties")) {
                    capture(x, s.props);
                }
            } else if (e == XmlScanner.END) {
                depth--;
            }
        }
        if ("default-style".equals(kind)) {
            if ("graphic".equals(family)) {
                defaultGraphic.putAll(s.props);
            }
        } else if ("page-layout".equals(kind)) {
            if (name != null) {
                layouts.put(name, new float[] {len(s.props.get("fo:page-width"), 0),
                    len(s.props.get("fo:page-height"), 0)});
            }
        } else if (name != null) {
            styles.put(name, s);
            resolved.clear();
        }
    }

    private void masterStyles(XmlScanner x) throws IOException {
        int depth = 1;
        while (depth > 0) {
            int e = x.next();
            if (e == XmlScanner.EOF) {
                return;
            }
            if (e == XmlScanner.END) {
                depth--;
            } else if (e == XmlScanner.START) {
                if ("master-page".equals(x.name)) {
                    masterPage(x);
                } else {
                    x.skipElement();
                }
            }
        }
    }

    private void masterPage(XmlScanner x) throws IOException {
        Master m = new Master();
        String name = x.attr("style:name");
        m.layout = x.attr("style:page-layout-name");
        m.pageStyle = x.attr("draw:style-name");
        if (name == null) {
            name = "";
        }
        if (firstMaster == null) {
            firstMaster = name;
        }
        masters.put(name, m);
        inMaster = true;
        currentMaster = m;
        while (true) {
            int e = x.next();
            if (e == XmlScanner.EOF || e == XmlScanner.END) {
                break;
            }
            if (e != XmlScanner.START) {
                continue;
            }
            String cls = x.attr("presentation:class");
            if ("notes".equals(x.name)) {
                x.skipElement();
            } else if (cls != null) {
                m.boxes.put(cls, new float[] {len(x.attr("svg:x"), 0), len(x.attr("svg:y"), 0),
                    len(x.attr("svg:width"), 0), len(x.attr("svg:height"), 0)});
                String ps = x.attr("presentation:style-name");
                if (ps != null) {
                    m.boxStyles.put(cls, ps);
                }
                x.skipElement();
            } else {
                SlideElement el = element(x, 0);
                if (el != null) {
                    m.decoration.add(el);
                }
            }
        }
        inMaster = false;
        currentMaster = null;
    }

    // ------------------------------------------------------------------ fills and lines

    private Picture picture(String href, String description) {
        if (href == null || href.length() == 0) {
            return null;
        }
        String path = href.startsWith("./") ? href.substring(2) : href;
        byte[] data = parts.get(path);
        if (data == null) {
            if (contains(href, "://")) {
                return Picture.linked(newId('i'), href, 100, 100, description);
            }
            warn(where() + "the image " + href + " is missing from the package");
            return null;
        }
        float[] size = OdfText.imageSize(data);
        if (size == null) {
            size = new float[] {100, 100};
        }
        return new Picture(newId('i'), data, mimeOf(path), size[0], size[1], description);
    }

    private static String mimeOf(String path) {
        int dot = path.lastIndexOf('.');
        String ext = dot < 0 ? "" : path.substring(dot + 1);
        if ("png".equalsIgnoreCase(ext)) {
            return "image/png";
        }
        if ("jpg".equalsIgnoreCase(ext) || "jpeg".equalsIgnoreCase(ext)) {
            return "image/jpeg";
        }
        if ("gif".equalsIgnoreCase(ext)) {
            return "image/gif";
        }
        if ("svg".equalsIgnoreCase(ext)) {
            return "image/svg+xml";
        }
        if ("bmp".equalsIgnoreCase(ext)) {
            return "image/bmp";
        }
        return "tif".equalsIgnoreCase(ext) || "tiff".equalsIgnoreCase(ext) ? "image/tiff"
                : "application/octet-stream";
    }

    private Fill fill(Map<String, String> p) {
        String kind = p.get("draw:fill");
        Fill f;
        if ("none".equals(kind)) {
            return Fill.NO_FILL;
        } else if ("gradient".equals(kind)) {
            String[] g = gradients.get(p.get("draw:fill-gradient-name"));
            if (g == null) {
                warn(where() + "a gradient fill names an undefined gradient");
                f = Fill.solid(OdfText.color(p.get("draw:fill-color"), 0x729fcf));
            } else {
                if (g[3] != null && !"linear".equals(g[3]) && !"axial".equals(g[3])) {
                    warn(where() + "a " + g[3] + " gradient was read as a linear one");
                }
                float angle = 0;
                String a = g[2];
                if (a != null) {
                    a = a.trim();
                    if (a.endsWith("deg")) {
                        angle = len(a.substring(0, a.length() - 3), 0);
                    } else if (a.endsWith("rad")) {
                        angle = (float) Math.toDegrees(len(a.substring(0, a.length() - 3), 0));
                    } else {
                        angle = len(a, 0) / 10f;
                    }
                }
                float model = (90 - angle) % 360;
                if (model < 0) {
                    model += 360;
                }
                f = Fill.gradient(OdfText.color(g[0], 0), OdfText.color(g[1], 0xffffff), model);
            }
        } else if ("bitmap".equals(kind)) {
            Picture pic = picture(fillImages.get(p.get("draw:fill-image-name")), null);
            if (pic == null) {
                return Fill.NO_FILL;
            }
            f = Fill.picture(pic);
        } else if ("hatch".equals(kind)) {
            warn(where() + "a hatch fill was read as a solid fill");
            f = Fill.solid(OdfText.color(p.get("draw:fill-color"), 0x729fcf));
        } else if ("solid".equals(kind) || p.get("draw:fill-color") != null && kind == null) {
            f = Fill.solid(OdfText.color(p.get("draw:fill-color"), 0x729fcf));
        } else {
            return Fill.NO_FILL;
        }
        String opacity = p.get("draw:opacity");
        if (opacity != null) {
            f = f.withAlpha(percent(opacity, 1));
        }
        return f;
    }

    private LineStyle stroke(Map<String, String> p) {
        String kind = p.get("draw:stroke");
        if ("none".equals(kind) || kind == null && p.get("svg:stroke-color") == null) {
            return LineStyle.NONE;
        }
        String dash = LineStyle.SOLID;
        if ("dash".equals(kind)) {
            dash = dashes.get(p.get("draw:stroke-dash"));
            if (dash == null) {
                dash = LineStyle.DASH;
            }
        }
        float width = len(p.get("svg:stroke-width"), 0);
        if (width <= 0) {
            width = 0.75f;
        }
        String head = arrow(p.get("draw:marker-start"));
        String tail = arrow(p.get("draw:marker-end"));
        return new LineStyle(OdfText.color(p.get("svg:stroke-color"), 0x3465a4), width, dash,
                percent(p.get("svg:stroke-opacity"), 1), head, tail);
    }

    private String arrow(String marker) {
        if (marker == null || marker.length() == 0) {
            return LineStyle.ARROW_NONE;
        }
        String k = markers.get(marker);
        return k == null ? LineStyle.ARROW_TRIANGLE : k;
    }

    private static TextFrame frame(List<Block> blocks, Map<String, String> p) {
        String va = p.get("draw:textarea-vertical-align");
        String anchor = "middle".equals(va) ? TextFrame.MIDDLE
                : "bottom".equals(va) ? TextFrame.BOTTOM : TextFrame.TOP;
        float pad = len(p.get("fo:padding"), -1);
        float l = len(p.get("fo:padding-left"), pad < 0 ? 7.2f : pad);
        float t = len(p.get("fo:padding-top"), pad < 0 ? 3.6f : pad);
        float r = len(p.get("fo:padding-right"), pad < 0 ? 7.2f : pad);
        float b = len(p.get("fo:padding-bottom"), pad < 0 ? 3.6f : pad);
        String fit = TextFrame.FIT_NONE;
        if ("true".equals(p.get("style:shrink-to-fit"))) {
            fit = TextFrame.FIT_SHRINK;
        } else if ("true".equals(p.get("draw:auto-grow-height"))) {
            fit = TextFrame.FIT_RESIZE;
        }
        return new TextFrame(blocks, l, t, r, b, anchor, fit,
                !"no-wrap".equals(p.get("fo:wrap-option")));
    }

    // ------------------------------------------------------------------ geometry

    /// x, y, width, height and rotation, from svg attributes, a transform and a placeholder.
    private float[] bounds(XmlScanner x, float[] box) {
        float[] r = new float[5];
        r[0] = coord(x, "svg:x", box == null ? 0 : box[0]);
        r[1] = coord(x, "svg:y", box == null ? 0 : box[1]);
        r[2] = Math.max(0, coord(x, "svg:width", box == null ? 0 : box[2]));
        r[3] = Math.max(0, coord(x, "svg:height", box == null ? 0 : box[3]));
        String t = x.attr("draw:transform");
        if (t != null) {
            transform(t, r);
        }
        return r;
    }

    private static String args(String t, String op) {
        int i = t.indexOf(op);
        if (i < 0) {
            return null;
        }
        int open = t.indexOf('(', i);
        int close = t.indexOf(')', open + 1);
        return open < 0 || close < 0 ? null : t.substring(open + 1, close).trim();
    }

    private void transform(String t, float[] r) {
        if (t.indexOf("skew") >= 0 || t.indexOf("scale") >= 0 || t.indexOf("matrix") >= 0) {
            warn(where() + "a skew, scale or matrix transform was ignored");
        }
        double rad = 0;
        String rot = args(t, "rotate");
        if (rot != null) {
            try {
                rad = Double.parseDouble(rot);
            } catch (NumberFormatException e) {
                rad = 0;
            }
        }
        String tr = args(t, "translate");
        float tx = r[0];
        float ty = r[1];
        if (tr != null) {
            String a = tr.replace(',', ' ').trim();
            int sp = a.indexOf(' ');
            tx = len(sp < 0 ? a : a.substring(0, sp), 0);
            ty = sp < 0 ? 0 : len(a.substring(sp + 1), 0);
        }
        double deg = Math.toDegrees(rad) % 360;
        double model = (360 - deg) % 360;
        if (model < 0) {
            model += 360;
        }
        model = Math.round(model * 1000) / 1000.0;
        if (model >= 360) {
            model = 0;
        }
        double m = Math.toRadians(model);
        double cos = Math.cos(m);
        double sin = Math.sin(m);
        float w = r[2];
        float h = r[3];
        r[0] = (float) (tx - w / 2.0 + w / 2.0 * cos - h / 2.0 * sin);
        r[1] = (float) (ty - h / 2.0 + w / 2.0 * sin + h / 2.0 * cos);
        r[4] = (float) model;
    }

    // ------------------------------------------------------------------ package

    private Presentation run() throws IOException {
        if (!parts.containsKey("content.xml")) {
            throw new IOException("Not an OpenDocument presentation: content.xml is missing");
        }
        XmlScanner st = scanner("styles.xml");
        if (st != null) {
            topLevel(st, null);
        }
        List<Slide> slides = new ArrayList<Slide>();
        topLevel(scanner("content.xml"), slides);
        float w = Presentation.WIDE_WIDTH;
        float h = Presentation.WIDE_HEIGHT;
        Master m = firstMaster == null ? null : masters.get(firstMaster);
        float[] size = m == null ? null : layouts.get(m.layout);
        if (size != null && size[0] >= 1 && size[1] >= 1 && size[0] < MAX_COORD
                && size[1] < MAX_COORD) {
            w = size[0];
            h = size[1];
        } else if (size != null) {
            warn("The page size is out of range; the default size was used");
        }
        if (slides.isEmpty()) {
            warn("The presentation has no slides");
        }
        return new Presentation(w, h, slides, Theme.DEFAULT, title(), next, warnings);
    }

    private String title() throws IOException {
        XmlScanner x = scanner("meta.xml");
        if (x == null) {
            return null;
        }
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e == XmlScanner.START && "title".equals(x.name)) {
                return x.elementText().trim();
            }
        }
        return null;
    }

    /// Walks `styles.xml` or `content.xml`, collecting slides from the latter.
    private void topLevel(XmlScanner x, List<Slide> slides) throws IOException {
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e != XmlScanner.START) {
                continue;
            }
            String n = x.name;
            if ("styles".equals(n) || "automatic-styles".equals(n)) {
                styleSection(x);
            } else if ("master-styles".equals(n)) {
                masterStyles(x);
            } else if ("page".equals(n) && slides != null) {
                slideNumber = slides.size() + 1;
                slides.add(page(x));
            } else if ("font-face-decls".equals(n) || "scripts".equals(n)
                    || "settings".equals(n)) {
                x.skipElement();
            }
        }
    }

    // ------------------------------------------------------------------ pages

    private Slide page(XmlScanner x) throws IOException {
        String name = x.attr("draw:name");
        String masterName = x.attr("draw:master-page-name");
        Map<String, String> props = resolve(x.attr("draw:style-name"));
        Master m = masterName == null ? null : masters.get(masterName);
        if (m == null && firstMaster != null) {
            m = masters.get(firstMaster);
        }
        currentMaster = m;
        xmlIds.clear();
        String id = adopt(name, 's');
        List<SlideElement> elements = new ArrayList<SlideElement>();
        if (m != null && !"false".equals(props.get("presentation:background-objects-visible"))) {
            for (SlideElement d : m.decoration) {
                elements.add(renumber(d));
            }
        }
        List<Block> notes = null;
        List<RawBuild> raw = new ArrayList<RawBuild>();
        while (true) {
            int e = x.next();
            if (e == XmlScanner.EOF || e == XmlScanner.END) {
                break;
            }
            if (e != XmlScanner.START) {
                continue;
            }
            String n = x.name;
            if ("notes".equals(n)) {
                notes = notes(x);
            } else if ("par".equals(n) || "seq".equals(n)) {
                timing(x, raw);
            } else if ("forms".equals(n)) {
                x.skipElement();
            } else {
                SlideElement el = element(x, 0);
                if (el != null) {
                    elements.add(el);
                }
            }
        }
        currentMaster = null;
        Fill background = null;
        if (props.get("draw:fill") != null
                && !"false".equals(props.get("presentation:background-visible"))) {
            background = fill(props);
        }
        if (background == null && m != null && m.pageStyle != null) {
            Map<String, String> mp = resolve(m.pageStyle);
            if (mp.get("draw:fill") != null) {
                background = fill(mp);
            }
        }
        if (background == null) {
            background = Fill.solid(0xffffff);
        }
        List<Build> builds = new ArrayList<Build>();
        for (RawBuild r : raw) {
            String target = xmlIds.get(r.target);
            if (target == null) {
                warn(where() + "an animation targets a shape that was not read");
                continue;
            }
            builds.add(new Build(target, r.kind, r.effect, r.trigger, r.duration, r.delay,
                    r.direction));
        }
        return new Slide(id, null, background, elements, notes, transition(props), builds,
                "hidden".equals(props.get("presentation:visibility")));
    }

    private SlideElement renumber(SlideElement e) {
        if (e instanceof GroupElement) {
            List<SlideElement> kids = new ArrayList<SlideElement>();
            for (SlideElement c : ((GroupElement) e).children) {
                kids.add(renumber(c));
            }
            return ((GroupElement) e.withId(newId('e'))).withChildren(kids);
        }
        return e.withId(newId('e'));
    }

    private Transition transition(Map<String, String> p) {
        String type = p.get("presentation:transition-type");
        String smil = p.get("smil:type");
        if (type == null && smil == null) {
            return Transition.DEFAULT;
        }
        int after = -1;
        if ("automatic".equals(type) || "semi-automatic".equals(type)) {
            after = Math.max(0, millis(p.get("presentation:duration")));
        }
        boolean click = !"automatic".equals(type);
        String sub = p.get("smil:subtype");
        boolean reverse = "reverse".equals(p.get("smil:direction"));
        String dir = Transition.FROM_RIGHT;
        if ("fromLeft".equals(sub)) {
            dir = Transition.FROM_LEFT;
        } else if ("fromTop".equals(sub)) {
            dir = Transition.FROM_TOP;
        } else if ("fromBottom".equals(sub)) {
            dir = Transition.FROM_BOTTOM;
        }
        String kind = Transition.NONE;
        if (smil != null) {
            if ("fade".equals(smil)) {
                kind = Transition.FADE;
            } else if ("pushWipe".equals(smil)) {
                kind = Transition.PUSH;
            } else if ("slideWipe".equals(smil)) {
                kind = reverse ? Transition.UNCOVER : Transition.COVER;
            } else if ("barWipe".equals(smil)) {
                kind = Transition.WIPE;
                boolean vertical = "topToBottom".equals(sub);
                if (vertical) {
                    dir = reverse ? Transition.FROM_BOTTOM : Transition.FROM_TOP;
                } else {
                    dir = reverse ? Transition.FROM_RIGHT : Transition.FROM_LEFT;
                }
            } else if ("irisWipe".equals(smil) || "zoom".equals(smil)) {
                kind = Transition.ZOOM;
            } else {
                kind = Transition.FADE;
                warn(where() + "the " + smil + " transition was read as a fade");
            }
        }
        int dur = millis(p.get("smil:dur"));
        if (dur < 0) {
            String speed = p.get("presentation:transition-speed");
            dur = "fast".equals(speed) ? 500 : "slow".equals(speed) ? 1000 : 750;
        }
        return new Transition(kind, dur, dir, click, after);
    }

    private List<Block> notes(XmlScanner x) throws IOException {
        List<Block> out = null;
        int depth = 1;
        while (depth > 0) {
            int e = x.next();
            if (e == XmlScanner.EOF) {
                break;
            }
            if (e == XmlScanner.END) {
                depth--;
            } else if (e == XmlScanner.START) {
                if ("text-box".equals(x.name)) {
                    if (out == null) {
                        out = new ArrayList<Block>();
                    }
                    text(x, out, new HashMap<String, String>(), 1);
                } else if (!"frame".equals(x.name)) {
                    x.skipElement();
                } else {
                    depth++;
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ builds

    private void timing(XmlScanner x, List<RawBuild> out) throws IOException {
        int depth = 1;
        while (depth > 0) {
            int e = x.next();
            if (e == XmlScanner.EOF) {
                return;
            }
            if (e == XmlScanner.END) {
                depth--;
            } else if (e == XmlScanner.START) {
                if (x.attr("presentation:preset-class") != null) {
                    effect(x, out);
                } else {
                    depth++;
                }
            }
        }
    }

    private void effect(XmlScanner x, List<RawBuild> out) throws IOException {
        RawBuild b = new RawBuild();
        String cls = x.attr("presentation:preset-class");
        String preset = x.attr("presentation:preset-id");
        String node = x.attr("presentation:node-type");
        b.kind = "exit".equals(cls) ? Build.EXIT : "emphasis".equals(cls) ? Build.EMPHASIS
                : Build.ENTRANCE;
        if (!"exit".equals(cls) && !"entrance".equals(cls) && !"emphasis".equals(cls)) {
            warn(where() + "a " + cls + " animation was read as an entrance");
        }
        b.trigger = "with-previous".equals(node) ? Build.WITH_PREVIOUS
                : "after-previous".equals(node) ? Build.AFTER_PREVIOUS : Build.ON_CLICK;
        b.effect = Build.APPEAR;
        if (contains(preset, "fade") || contains(preset, "dissolve")) {
            b.effect = Build.FADE;
        } else if (contains(preset, "fly")) {
            b.effect = Build.FLY;
        } else if (contains(preset, "zoom")) {
            b.effect = Build.ZOOM;
        } else if (contains(preset, "wipe")) {
            b.effect = Build.WIPE;
        } else if (preset != null && !contains(preset, "appear") && !contains(preset, "disappear")
                && !Build.EMPHASIS.equals(b.kind)) {
            warn(where() + "the " + preset + " animation was read as an appear build");
        }
        b.delay = Math.max(0, millis(x.attr("smil:begin")));
        b.duration = 0;
        b.direction = Transition.FROM_BOTTOM;
        int depth = 1;
        while (depth > 0) {
            int e = x.next();
            if (e == XmlScanner.EOF) {
                break;
            }
            if (e == XmlScanner.END) {
                depth--;
            } else if (e == XmlScanner.START) {
                depth++;
                String target = x.attr("smil:targetElement");
                if (target != null && b.target == null) {
                    b.target = target;
                }
                int d = millis(x.attr("smil:dur"));
                if (d > 1) {
                    b.duration = Math.max(b.duration, d);
                }
                String values = x.attr("smil:values");
                if (values != null) {
                    if (values.indexOf("0-width") >= 0) {
                        b.direction = Transition.FROM_LEFT;
                    } else if (values.indexOf("1+width") >= 0) {
                        b.direction = Transition.FROM_RIGHT;
                    } else if (values.indexOf("0-height") >= 0) {
                        b.direction = Transition.FROM_TOP;
                    }
                }
            }
        }
        if (b.target == null) {
            warn(where() + "an animation without a target was skipped");
            return;
        }
        if (b.duration <= 0) {
            b.duration = 500;
        }
        out.add(b);
    }

    // ------------------------------------------------------------------ elements

    /// The element id: its `xml:id` (or `draw:id`) when unused, otherwise a new one.
    private String elementId(XmlScanner x) {
        String a = x.attr("xml:id");
        if (a == null) {
            a = x.attr("draw:id");
        }
        String id = inMaster ? newId('e') : adopt(a, 'e');
        register(x, id);
        return id;
    }

    private void register(XmlScanner x, String id) {
        String a = x.attr("xml:id");
        String b = x.attr("draw:id");
        if (a != null) {
            xmlIds.put(a, id);
        }
        if (b != null) {
            xmlIds.put(b, id);
        }
    }

    private Map<String, String> shapeProps(XmlScanner x, String cls) {
        Map<String, String> p = new HashMap<String, String>(defaultGraphic);
        String ps = x.attr("presentation:style-name");
        if (ps == null && cls != null && currentMaster != null) {
            ps = currentMaster.boxStyles.get(cls);
        }
        p.putAll(resolve(ps));
        p.putAll(resolve(x.attr("draw:style-name")));
        return p;
    }

    private static String placeholder(String cls) {
        if ("title".equals(cls)) {
            return ShapeElement.TITLE;
        }
        if ("subtitle".equals(cls)) {
            return ShapeElement.SUBTITLE;
        }
        return "outline".equals(cls) || "text".equals(cls) || "body".equals(cls)
                ? ShapeElement.BODY : null;
    }

    /// Reads the drawing element `x` is at, to its end tag; null when it has no equivalent.
    private SlideElement element(XmlScanner x, int depth) throws IOException {
        if (depth > MAX_DEPTH) {
            warn(where() + "shapes nested deeper than " + MAX_DEPTH + " levels were skipped");
            x.skipElement();
            return null;
        }
        String n = x.name;
        try {
            if ("frame".equals(n)) {
                return frameElement(x);
            }
            if ("custom-shape".equals(n) || "rect".equals(n)
                    || "ellipse".equals(n) || "circle".equals(n)) {
                return shape(x, n);
            }
            if ("line".equals(n) || "connector".equals(n)) {
                return line(x);
            }
            if ("g".equals(n)) {
                return group(x, depth);
            }
        } catch (IllegalArgumentException e) {
            warn(where() + "a " + n + " with invalid values was skipped");
            return null;
        }
        if ("polygon".equals(n) || "polyline".equals(n) || "path".equals(n)) {
            warn(where() + "a " + n + " was read as a rectangle");
            return shape(x, n);
        }
        warn(where() + "a " + n + " element was not converted");
        x.skipElement();
        return null;
    }

    private SlideElement group(XmlScanner x, int depth) throws IOException {
        String id = elementId(x);
        String name = x.attr("draw:name");
        List<SlideElement> kids = new ArrayList<SlideElement>();
        while (true) {
            int e = x.next();
            if (e == XmlScanner.EOF || e == XmlScanner.END) {
                break;
            }
            if (e == XmlScanner.START) {
                SlideElement c = element(x, depth + 1);
                if (c != null) {
                    kids.add(c);
                }
            }
        }
        if (kids.isEmpty()) {
            return null;
        }
        GroupElement g = GroupElement.of(id, kids);
        return new GroupElement(id, name, kids, g.x, g.y, g.width, g.height, 0, false, false);
    }

    private SlideElement line(XmlScanner x) throws IOException {
        String id = elementId(x);
        Map<String, String> p = shapeProps(x, null);
        float x1 = len(x.attr("svg:x1"), 0);
        float y1 = len(x.attr("svg:y1"), 0);
        float x2 = len(x.attr("svg:x2"), 0);
        float y2 = len(x.attr("svg:y2"), 0);
        String name = x.attr("draw:name");
        List<Block> blocks = new ArrayList<Block>();
        text(x, blocks, p, 1);
        ShapeElement s = ShapeElement.line(id, x1, y1, x2, y2, stroke(p));
        if (!blocks.isEmpty() && hasText(blocks)) {
            s = s.withText(frame(blocks, p));
        }
        if (name != null) {
            s = new ShapeElement(id, name, s.geometry, s.x, s.y, s.width, s.height, s.rotation,
                    s.flipH, s.flipV, s.fill, s.line, s.text, null, null);
        }
        return s;
    }

    private static boolean hasText(List<Block> blocks) {
        for (Block b : blocks) {
            if (b instanceof Paragraph && ((Paragraph) b).text != null
                    && !((Paragraph) b).text.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static String geometryOf(String odf) {
        for (String[] p : OdpWriter.PRESETS) {
            if (p[1].equals(odf)) {
                return p[0];
            }
        }
        if ("circle".equals(odf)) {
            return "ellipse";
        }
        if ("cross".equals(odf)) {
            return "plus";
        }
        return odf.startsWith("ooxml-") && odf.length() > 6 ? odf.substring(6) : null;
    }

    private SlideElement shape(XmlScanner x, String n) throws IOException {
        String id = elementId(x);
        String cls = x.attr("presentation:class");
        Map<String, String> p = shapeProps(x, cls);
        float[] box = cls != null && currentMaster != null ? currentMaster.boxes.get(cls) : null;
        float[] b = bounds(x, box);
        String name = x.attr("draw:name");
        String geometry = "ellipse".equals(n) || "circle".equals(n) ? "ellipse"
                : "rect";
        if ("rect".equals(n) && len(x.attr("draw:corner-radius"), 0) > 0) {
            geometry = "roundRect";
        }
        boolean flipH = false;
        boolean flipV = false;
        float[] adjust = null;
        List<Block> blocks = new ArrayList<Block>();
        while (true) {
            int e = x.next();
            if (e == XmlScanner.EOF || e == XmlScanner.END) {
                break;
            }
            if (e != XmlScanner.START) {
                continue;
            }
            String c = x.name;
            if ("enhanced-geometry".equals(c)) {
                String type = x.attr("draw:type");
                String g = type == null ? null : geometryOf(type);
                if (g == null) {
                    warn(where() + "the custom shape " + (type == null ? "path" : type)
                            + " was read as a rectangle");
                    g = "rect";
                } else if (type.startsWith("ooxml-")) {
                    adjust = modifiers(x.attr("draw:modifiers"));
                }
                geometry = g;
                flipH = "true".equals(x.attr("draw:mirror-horizontal"));
                flipV = "true".equals(x.attr("draw:mirror-vertical"));
                x.skipElement();
            } else if ("p".equals(c) || "h".equals(c)) {
                paragraph(x, blocks, p, Paragraph.NO_LIST, 0, 1, 1);
            } else if ("list".equals(c)) {
                list(x, blocks, p, null, 0, 1);
            } else {
                x.skipElement();
            }
        }
        TextFrame t = blocks.isEmpty() ? null : frame(blocks, p);
        return new ShapeElement(id, name, geometry, b[0], b[1], b[2], b[3], b[4], flipH, flipV,
                fill(p), stroke(p), t, placeholder(cls), adjust);
    }

    private static float[] modifiers(String m) {
        if (m == null || m.trim().length() == 0) {
            return null;
        }
        List<Float> out = new ArrayList<Float>();
        int i = 0;
        String s = m.trim();
        while (i < s.length() && out.size() < 16) {
            int sp = s.indexOf(' ', i);
            String tok = sp < 0 ? s.substring(i) : s.substring(i, sp);
            i = sp < 0 ? s.length() : sp + 1;
            if (tok.length() == 0) {
                continue;
            }
            try {
                out.add(Float.valueOf(Float.parseFloat(tok) / 100000f));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        float[] r = new float[out.size()];
        for (int k = 0; k < r.length; k++) {
            r[k] = out.get(k).floatValue();
        }
        return r;
    }

    private SlideElement frameElement(XmlScanner x) throws IOException {
        String id = elementId(x);
        String cls = x.attr("presentation:class");
        Map<String, String> p = shapeProps(x, cls);
        float[] box = cls != null && currentMaster != null ? currentMaster.boxes.get(cls) : null;
        float[] b = bounds(x, box);
        String name = x.attr("draw:name");
        List<Block> blocks = null;
        String image = null;
        String object = null;
        Table table = null;
        while (true) {
            int e = x.next();
            if (e == XmlScanner.EOF || e == XmlScanner.END) {
                break;
            }
            if (e != XmlScanner.START) {
                continue;
            }
            String c = x.name;
            if ("text-box".equals(c)) {
                blocks = new ArrayList<Block>();
                text(x, blocks, p, 1);
            } else if ("image".equals(c)) {
                if (image == null) {
                    image = x.attr("xlink:href");
                }
                x.skipElement();
            } else if ("object".equals(c) || "object-ole".equals(c)) {
                object = x.attr("xlink:href");
                if (object == null) {
                    object = "";
                }
                x.skipElement();
            } else if ("table".equals(c)) {
                table = table(x);
            } else {
                x.skipElement();
            }
        }
        if (object != null) {
            ChartElement chart = chart(object, id, name, b);
            if (chart != null) {
                return chart;
            }
            if (image != null) {
                warn(where() + "an embedded object was read as its replacement image");
            } else {
                warn(where() + "an embedded object without a chart or image was skipped");
                return null;
            }
        }
        if (table != null) {
            return new TableElement(id, name, table, b[0], b[1], b[2], b[3], b[4]);
        }
        if (image != null) {
            Picture pic = picture(image, name);
            if (pic == null) {
                return null;
            }
            float[] crop = crop(p.get("fo:clip"), pic);
            return new PictureElement(id, name, pic, b[0], b[1], b[2], b[3], b[4], false, false,
                    crop[3], crop[0], crop[1], crop[2], stroke(p));
        }
        if (blocks == null && cls == null) {
            warn(where() + "an empty frame was skipped");
            return null;
        }
        return new ShapeElement(id, name, "rect", b[0], b[1], b[2], b[3], b[4], false, false,
                fill(p), stroke(p), frame(blocks, p), placeholder(cls), null);
    }

    /// Top, right, bottom and left crop fractions from `fo:clip="rect(t, r, b, l)"`.
    private float[] crop(String clip, Picture pic) {
        float[] r = new float[4];
        if (clip == null || pic.width <= 0 || pic.height <= 0) {
            return r;
        }
        String c = args(clip, "rect");
        if (c == null) {
            return r;
        }
        String[] v = new String[4];
        int k = 0;
        int start = 0;
        String s = c.replace(',', ' ');
        for (int i = 0; i <= s.length() && k < 4; i++) {
            if (i == s.length() || s.charAt(i) == ' ') {
                if (i > start) {
                    v[k++] = s.substring(start, i);
                }
                start = i + 1;
            }
        }
        if (k < 4) {
            return r;
        }
        r[0] = len(v[0], 0) / pic.height;
        r[1] = len(v[1], 0) / pic.width;
        r[2] = len(v[2], 0) / pic.height;
        r[3] = len(v[3], 0) / pic.width;
        for (int i = 0; i < 4; i++) {
            r[i] = Math.max(0, r[i]);
        }
        if (r[0] + r[2] >= 1 || r[1] + r[3] >= 1) {
            warn(where() + "an image crop larger than the image was ignored");
            return new float[4];
        }
        return r;
    }

    // ------------------------------------------------------------------ tables

    private Table table(XmlScanner x) throws IOException {
        List<List<List<Block>>> rows = new ArrayList<List<List<Block>>>();
        List<Table.Merge> merges = new ArrayList<Table.Merge>();
        List<Float> widths = new ArrayList<Float>();
        int header = 0;
        boolean inHeader = false;
        int depth = 1;
        while (depth > 0) {
            int e = x.next();
            if (e == XmlScanner.EOF) {
                break;
            }
            if (e == XmlScanner.END) {
                depth--;
                if ("table-header-rows".equals(x.name)) {
                    inHeader = false;
                }
                continue;
            }
            if (e != XmlScanner.START) {
                continue;
            }
            String n = x.name;
            if ("table-column".equals(n)) {
                int rep = count(x, "table:number-columns-repeated", 1, 1, MAX_REPEAT);
                float w = len(resolve(x.attr("table:style-name")).get("style:column-width"), 0);
                for (int i = 0; i < rep && widths.size() < MAX_REPEAT; i++) {
                    widths.add(Float.valueOf(w));
                }
                x.skipElement();
            } else if ("table-header-rows".equals(n)) {
                inHeader = true;
                depth++;
            } else if ("table-row".equals(n)) {
                if (rows.size() >= MAX_REPEAT) {
                    warn(where() + "table rows past " + MAX_REPEAT + " were dropped");
                    x.skipElement();
                    continue;
                }
                List<List<Block>> row = row(x, rows.size(), merges);
                rows.add(row);
                if (inHeader) {
                    header++;
                }
            } else if ("table-rows".equals(n) || "table-columns".equals(n)
                    || "table-header-columns".equals(n)) {
                depth++;
            } else {
                x.skipElement();
            }
        }
        int cols = 1;
        for (List<List<Block>> r : rows) {
            cols = Math.max(cols, r.size());
        }
        if (rows.isEmpty()) {
            rows.add(new ArrayList<List<Block>>());
        }
        for (List<List<Block>> r : rows) {
            while (r.size() < cols) {
                r.add(emptyCell());
            }
        }
        List<Table.Merge> valid = new ArrayList<Table.Merge>();
        for (Table.Merge m : merges) {
            int rr = Math.min(m.rows, rows.size() - m.row);
            int cc = Math.min(m.columns, cols - m.column);
            if (rr * cc >= 2) {
                valid.add(new Table.Merge(m.row, m.column, rr, cc));
            }
        }
        Table t;
        try {
            t = new Table(newId('t'), rows, valid, header);
        } catch (IllegalArgumentException e) {
            warn(where() + "a table with overlapping or invalid merges was read without them");
            t = new Table(newId('t'), rows, new ArrayList<Table.Merge>(), header);
        }
        if (widths.size() == cols) {
            float[] w = new float[cols];
            boolean ok = true;
            for (int i = 0; i < cols; i++) {
                w[i] = widths.get(i).floatValue();
                ok &= w[i] > 0;
            }
            if (ok) {
                t = t.withColumnWidths(w);
            }
        }
        return t;
    }

    private List<Block> emptyCell() {
        List<Block> l = new ArrayList<Block>();
        l.add(new Paragraph(newId('p'), RichText.EMPTY));
        return l;
    }

    private List<List<Block>> row(XmlScanner x, int r, List<Table.Merge> merges)
            throws IOException {
        List<List<Block>> row = new ArrayList<List<Block>>();
        while (true) {
            int e = x.next();
            if (e == XmlScanner.EOF || e == XmlScanner.END) {
                break;
            }
            if (e != XmlScanner.START) {
                continue;
            }
            String n = x.name;
            if ("table-cell".equals(n) || "covered-table-cell".equals(n)) {
                int rep = count(x, "table:number-columns-repeated", 1, 1, MAX_REPEAT);
                if (rep >= MAX_REPEAT) {
                    warn(where() + "repeated table cells past " + MAX_REPEAT + " were dropped");
                }
                int cs = count(x, "table:number-columns-spanned", 1, 1, MAX_REPEAT);
                int rs = count(x, "table:number-rows-spanned", 1, 1, MAX_REPEAT);
                if (cs * rs >= 2 && "table-cell".equals(n)) {
                    merges.add(new Table.Merge(r, row.size(), rs, cs));
                }
                List<Block> cell = new ArrayList<Block>();
                text(x, cell, resolve(x.attr("table:style-name")), 1);
                if (cell.isEmpty()) {
                    cell = emptyCell();
                }
                row.add(cell);
                for (int i = 1; i < rep && row.size() < MAX_REPEAT; i++) {
                    row.add(emptyCell());
                }
            } else {
                x.skipElement();
            }
        }
        return row;
    }

    // ------------------------------------------------------------------ charts

    private ChartElement chart(String href, String id, String name, float[] b)
            throws IOException {
        String dir = href.startsWith("./") ? href.substring(2) : href;
        while (dir.endsWith("/")) {
            dir = dir.substring(0, dir.length() - 1);
        }
        XmlScanner x = dir.length() == 0 ? null : scanner(dir + "/content.xml");
        if (x == null) {
            return null;
        }
        Map<String, Map<String, String>> chartStyles = new HashMap<String, Map<String, String>>();
        String cls = null;
        String title = null;
        boolean legend = false;
        String plotStyle = null;
        List<String> seriesStyles = new ArrayList<String>();
        List<List<String[]>> rows = new ArrayList<List<String[]>>();
        int headerRows = -1;
        String styleName = null;
        for (int e = x.next(); e != XmlScanner.EOF; e = x.next()) {
            if (e == XmlScanner.END) {
                if ("table-header-rows".equals(x.name)) {
                    headerRows = rows.size();
                }
                continue;
            }
            if (e != XmlScanner.START) {
                continue;
            }
            String n = x.name;
            if ("style".equals(n)) {
                styleName = x.attr("style:name");
                if (styleName != null) {
                    chartStyles.put(styleName, new HashMap<String, String>());
                }
            } else if (n.endsWith("-properties") && styleName != null) {
                capture(x, chartStyles.get(styleName));
            } else if ("chart".equals(n)) {
                cls = x.attr("chart:class");
            } else if ("title".equals(n)) {
                title = x.elementText().trim();
            } else if ("legend".equals(n)) {
                legend = true;
            } else if ("plot-area".equals(n)) {
                plotStyle = x.attr("chart:style-name");
            } else if ("series".equals(n)) {
                seriesStyles.add(x.attr("chart:style-name"));
            } else if ("table-row".equals(n)) {
                if (rows.size() < MAX_REPEAT) {
                    rows.add(chartRow(x));
                } else {
                    x.skipElement();
                }
            } else if ("subtitle".equals(n) || "footer".equals(n)) {
                x.skipElement();
            }
        }
        if (cls == null || rows.isEmpty()) {
            return null;
        }
        Map<String, String> plot = plotStyle == null ? null : chartStyles.get(plotStyle);
        boolean vertical = plot != null && "true".equals(plot.get("chart:vertical"));
        boolean stacked = plot != null && "true".equals(plot.get("chart:stacked"));
        String c = cls.startsWith("chart:") ? cls.substring(6) : cls;
        String kind = ChartElement.COLUMN;
        if ("bar".equals(c)) {
            kind = vertical ? ChartElement.BAR : ChartElement.COLUMN;
        } else if ("line".equals(c)) {
            kind = ChartElement.LINE;
        } else if ("area".equals(c)) {
            kind = ChartElement.AREA;
        } else if ("circle".equals(c)) {
            kind = ChartElement.PIE;
        } else if ("ring".equals(c)) {
            kind = ChartElement.DOUGHNUT;
        } else if ("scatter".equals(c) || "bubble".equals(c)) {
            kind = ChartElement.SCATTER;
        } else if ("radar".equals(c) || "filled-radar".equals(c)) {
            kind = ChartElement.RADAR;
        } else {
            warn(where() + "a " + c + " chart was read as a column chart");
        }
        int head = headerRows > 0 ? headerRows : 1;
        int cols = 0;
        for (List<String[]> r : rows) {
            cols = Math.max(cols, r.size());
        }
        int series = Math.max(0, cols - 1);
        int cats = Math.max(0, rows.size() - head);
        if (series == 0 || cats == 0) {
            return null;
        }
        String[] names = new String[series];
        List<String[]> hr = rows.get(head - 1);
        for (int i = 0; i < series; i++) {
            names[i] = i + 1 < hr.size() ? hr.get(i + 1)[0] : "Series " + (i + 1);
        }
        String[] categories = new String[cats];
        double[][] values = new double[series][cats];
        for (int k = 0; k < cats; k++) {
            List<String[]> r = rows.get(head + k);
            categories[k] = r.isEmpty() ? "" : r.get(0)[0];
            for (int i = 0; i < series; i++) {
                values[i][k] = i + 1 < r.size() ? number(r.get(i + 1)) : 0;
            }
        }
        int[] colors = new int[series];
        boolean anyColor = false;
        for (int i = 0; i < series; i++) {
            Map<String, String> s = i < seriesStyles.size() && seriesStyles.get(i) != null
                    ? chartStyles.get(seriesStyles.get(i)) : null;
            colors[i] = s == null ? -1 : OdfText.color(s.get("draw:fill-color"), -1);
            anyColor |= colors[i] >= 0;
        }
        return new ChartElement(id, name, kind, b[0], b[1], b[2], b[3], b[4],
                title == null || title.length() == 0 ? null : title, stacked, legend, categories,
                names, values, anyColor ? colors : null);
    }

    private static double number(String[] cell) {
        String v = cell[1] != null ? cell[1] : cell[0];
        try {
            double d = Double.parseDouble(v.trim());
            return Double.isNaN(d) || Double.isInfinite(d) ? 0 : d;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private List<String[]> chartRow(XmlScanner x) throws IOException {
        List<String[]> row = new ArrayList<String[]>();
        while (true) {
            int e = x.next();
            if (e == XmlScanner.EOF || e == XmlScanner.END) {
                break;
            }
            if (e != XmlScanner.START) {
                continue;
            }
            if ("table-cell".equals(x.name) || "covered-table-cell".equals(x.name)) {
                int rep = count(x, "table:number-columns-repeated", 1, 1, MAX_REPEAT);
                String value = x.attr("office:value");
                String text = x.elementText().trim();
                for (int i = 0; i < rep && row.size() < MAX_REPEAT; i++) {
                    row.add(new String[] {text, value});
                }
            } else {
                x.skipElement();
            }
        }
        return row;
    }

    // ------------------------------------------------------------------ text

    /// Reads the paragraphs and lists inside the element `x` is at, to its end tag.
    private void text(XmlScanner x, List<Block> out, Map<String, String> base, int depth)
            throws IOException {
        while (true) {
            int e = x.next();
            if (e == XmlScanner.EOF || e == XmlScanner.END) {
                return;
            }
            if (e != XmlScanner.START) {
                continue;
            }
            String n = x.name;
            if ("p".equals(n) || "h".equals(n)) {
                paragraph(x, out, base, Paragraph.NO_LIST, 0, 1, depth);
            } else if ("list".equals(n)) {
                list(x, out, base, null, 0, depth);
            } else if ("frame".equals(n) || "custom-shape".equals(n)) {
                warn(where() + "a shape inside text was not converted");
                x.skipElement();
            } else {
                x.skipElement();
            }
        }
    }

    private void list(XmlScanner x, List<Block> out, Map<String, String> base, String kind,
            int level, int depth) throws IOException {
        if (depth > MAX_DEPTH) {
            warn(where() + "lists nested deeper than " + MAX_DEPTH + " levels were flattened");
            x.skipElement();
            return;
        }
        String k = kind;
        if (k == null) {
            Boolean bullet = listStyles.get(x.attr("text:style-name"));
            k = bullet == null || bullet.booleanValue() ? Paragraph.BULLET : Paragraph.NUMBERED;
        }
        while (true) {
            int e = x.next();
            if (e == XmlScanner.EOF || e == XmlScanner.END) {
                return;
            }
            if (e != XmlScanner.START) {
                continue;
            }
            boolean headerItem = "list-header".equals(x.name);
            if (!"list-item".equals(x.name) && !headerItem) {
                x.skipElement();
                continue;
            }
            int start = count(x, "text:start-value", 1, 1, 1000000);
            while (true) {
                int c = x.next();
                if (c == XmlScanner.EOF || c == XmlScanner.END) {
                    break;
                }
                if (c != XmlScanner.START) {
                    continue;
                }
                if ("p".equals(x.name) || "h".equals(x.name)) {
                    paragraph(x, out, base, headerItem ? Paragraph.NO_LIST : k,
                            Math.min(8, level), start, depth + 1);
                    start = 1;
                } else if ("list".equals(x.name)) {
                    list(x, out, base, k, level + 1, depth + 1);
                } else {
                    x.skipElement();
                }
            }
        }
    }

    private static String align(String a) {
        if ("center".equals(a)) {
            return Paragraph.CENTER;
        }
        if ("end".equals(a) || "right".equals(a)) {
            return Paragraph.RIGHT;
        }
        return "justify".equals(a) ? Paragraph.JUSTIFY : Paragraph.LEFT;
    }

    private void paragraph(XmlScanner x, List<Block> out, Map<String, String> base,
            String list, int level, int start, int depth) throws IOException {
        Map<String, String> p = new HashMap<String, String>(base);
        p.putAll(resolve(x.attr("text:style-name")));
        TextStyle style = OdfText.TextProps.of(p).apply(TextStyle.DEFAULT);
        RichText.Builder b = new RichText.Builder();
        boolean[] space = {true};
        spans(x, b, style, null, depth, space);
        RichText t = b.build();
        float spacing = 0;
        String lh = p.get("fo:line-height");
        if (lh != null && lh.endsWith("%")) {
            try {
                spacing = Float.parseFloat(lh.substring(0, lh.length() - 1)) / 100f;
            } catch (NumberFormatException e) {
                spacing = 0;
            }
        }
        float indent = len(p.get("fo:margin-left"), 0);
        Paragraph para = new Paragraph(newId('p'), null, align(p.get("fo:text-align")), list,
                level, start, len(p.get("fo:margin-top"), 0), len(p.get("fo:margin-bottom"), 0),
                indent, spacing > 0 && spacing < 10 ? spacing : 0, false, false, t)
                .withIndents(indent, len(p.get("fo:text-indent"), 0),
                        len(p.get("fo:margin-right"), 0));
        out.add(para);
    }

    private void spans(XmlScanner x, RichText.Builder b, TextStyle style, String link,
            int depth, boolean[] space) throws IOException {
        TextStyle s = link == null ? style : style.withLink(link);
        while (true) {
            int e = x.next();
            if (e == XmlScanner.EOF || e == XmlScanner.END) {
                return;
            }
            if (e == XmlScanner.TEXT) {
                appendCollapsed(b, x.text, s, space);
                continue;
            }
            if (e != XmlScanner.START) {
                continue;
            }
            String n = x.name;
            if ("s".equals(n)) {
                int c = count(x, "text:c", 1, 1, 1000);
                if (c >= 1000) {
                    warn(where() + "a run of more than 1000 spaces was shortened");
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < c; i++) {
                    sb.append(' ');
                }
                b.append(sb.toString(), s);
                space[0] = false;
                x.skipElement();
            } else if ("tab".equals(n)) {
                b.append("\t", s);
                space[0] = false;
                x.skipElement();
            } else if ("line-break".equals(n)) {
                b.append("\n", s);
                space[0] = true;
                x.skipElement();
            } else if (depth > MAX_DEPTH) {
                warn(where() + "text nested deeper than " + MAX_DEPTH + " levels was dropped");
                x.skipElement();
            } else if ("span".equals(n)) {
                Map<String, String> sp = resolve(x.attr("text:style-name"));
                spans(x, b, OdfText.TextProps.of(sp).apply(style), link, depth + 1, space);
            } else if ("a".equals(n)) {
                spans(x, b, style, x.attr("xlink:href"), depth + 1, space);
            } else if ("note".equals(n) || "annotation".equals(n)
                    || "bookmark".equals(n) || "soft-page-break".equals(n)
                    || "frame".equals(n)) {
                x.skipElement();
            } else {
                appendCollapsed(b, x.elementText(), s, space);
            }
        }
    }

    /// Appends text with runs of XML white space collapsed to one space, as ODF reads it.
    private static void appendCollapsed(RichText.Builder b, String raw, TextStyle s,
            boolean[] space) {
        if (raw == null || raw.length() == 0) {
            return;
        }
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                if (!space[0]) {
                    sb.append(' ');
                    space[0] = true;
                }
            } else {
                sb.append(c);
                space[0] = false;
            }
        }
        b.append(sb.toString(), s);
    }
}
