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
package com.codename1.art;

import com.codename1.art.effects.Effect;
import com.codename1.art.effects.Effects;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.PathIterator;
import com.codename1.util.Base64;
import com.codename1.util.MathUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Reads SVG 1.1 and SVG 2 static documents into an `ArtDocument`.
///
/// Covers shapes and paths, groups, nested `svg`, `use` with `symbol`, `switch`,
/// presentation attributes, inline styles and `<style>` sheets (type, class, id and
/// compound selectors), gradients with `href` inheritance, patterns, `clipPath`, `mask`,
/// every filter primitive, `text` with `tspan`, `image` with data URLs and resolved links,
/// `mix-blend-mode` and `viewBox` with `preserveAspectRatio`. Markers are expanded into
/// shapes. Animation and scripts have no static meaning and are reported as notes.
public final class SvgReader {
    /// Loads what an `href` names. Return null for anything that should not be loaded.
    public interface ResourceResolver {
        /// The bytes of a linked resource.
        ///
        /// #### Parameters
        ///
        /// - `href`: the link as written
        byte[] load(String href);
    }

    private static final String[] INHERITED = {
        "fill", "fill-opacity", "fill-rule", "stroke", "stroke-width", "stroke-opacity", "stroke-linecap",
        "stroke-linejoin", "stroke-miterlimit", "stroke-dasharray", "stroke-dashoffset", "visibility",
        "font-family", "font-size", "font-weight", "font-style", "text-anchor", "color", "letter-spacing",
        "clip-rule", "text-decoration", "marker-start", "marker-mid", "marker-end", "marker",
        "color-interpolation-filters", "paint-order"
    };
    private static final String[] PRESENTATION = {
        "fill", "fill-opacity", "fill-rule", "stroke", "stroke-width", "stroke-opacity", "stroke-linecap",
        "stroke-linejoin", "stroke-miterlimit", "stroke-dasharray", "stroke-dashoffset", "visibility",
        "display", "opacity", "font-family", "font-size", "font-weight", "font-style", "text-anchor", "color",
        "letter-spacing", "clip-path", "clip-rule", "mask", "filter", "stop-color", "stop-opacity",
        "mix-blend-mode", "text-decoration", "flood-color", "flood-opacity", "lighting-color",
        "marker-start", "marker-mid", "marker-end", "marker", "color-interpolation-filters", "paint-order",
        "isolation"
    };

    private final Map<String, SvgElement> ids = new HashMap<String, SvgElement>();
    private final List<String[]> rules = new ArrayList<String[]>();
    private final List<ImportNote> notes = new ArrayList<ImportNote>();
    private final Set<String> noted = new HashSet<String>();
    private ResourceResolver resolver;
    private int useDepth;
    /// How many `use` references have been expanded. Depth alone does not bound the work:
    /// ten references per level, 32 levels deep, is 10^32 copies from a few hundred elements.
    private int useExpansions;
    private static final int MAX_USE_EXPANSIONS = 20000;
    private double viewportW = 100;
    private double viewportH = 100;

    /// Sets what loads linked images and documents; without one only data URLs are read.
    ///
    /// #### Parameters
    ///
    /// - `resolver`: the resolver, or null
    public SvgReader setResolver(ResourceResolver resolver) {
        this.resolver = resolver;
        return this;
    }

    /// Reads an SVG document.
    ///
    /// #### Parameters
    ///
    /// - `in`: the document; not closed
    public static ArtDocument read(InputStream in) throws IOException {
        try {
            return new SvgReader().parse(in);
        } catch (RuntimeException e) {
            throw ArtFormats.damaged(e);
        }
    }

    /// Reads an SVG document from bytes.
    ///
    /// #### Parameters
    ///
    /// - `data`: the document
    public static ArtDocument read(byte[] data) throws IOException {
        try {
            return new SvgReader().parse(new ByteArrayInputStream(data));
        } catch (RuntimeException e) {
            throw ArtFormats.damaged(e);
        }
    }

    /// Reads an SVG document with this reader's settings.
    ///
    /// #### Parameters
    ///
    /// - `in`: the document; not closed
    public ArtDocument parse(InputStream in) throws IOException {
        SvgElement root = SvgElement.parse(in);
        if (!"svg".equals(root.name)) {
            throw new IOException("Root element is not svg");
        }
        index(root);
        double[] vb = numbers(root.attr("viewBox"));
        double w = length(root.attr("width"), vb.length == 4 ? vb[2] : 300, 0);
        double h = length(root.attr("height"), vb.length == 4 ? vb[3] : 150, 0);
        if (w <= 0 || h <= 0 || Double.isNaN(w) || Double.isNaN(h)) {
            w = vb.length == 4 ? vb[2] : 300;
            h = vb.length == 4 ? vb[3] : 150;
        }
        ArtDocument doc = new ArtDocument(w, h);
        viewportW = vb.length == 4 ? vb[2] : w;
        viewportH = vb.length == 4 ? vb[3] : h;
        GroupNode top = new GroupNode();
        top.setName("svg");
        top.setTransform(viewBoxTransform(vb, root.attr("preserveAspectRatio"), 0, 0, w, h));
        Map<String, String> style = computeStyle(root, new HashMap<String, String>());
        children(root, top, style);
        GroupNode docRoot = new GroupNode();
        docRoot.add(top);
        applyCommon(root, top, style);
        top.setTransform(viewBoxTransform(vb, root.attr("preserveAspectRatio"), 0, 0, w, h));
        doc.setRoot(docRoot);
        SvgElement title = first(root, "title");
        if (title != null) {
            doc.setTitle(title.text.toString().trim());
        }
        doc.getNotes().addAll(notes);
        return doc;
    }

    private void note(String where, String msg) {
        if (noted.add(where + msg) && notes.size() < 500) {
            notes.add(new ImportNote(where, msg));
        }
    }

    private static SvgElement first(SvgElement e, String name) {
        for (int i = 0; i < e.children.size(); i++) {
            if (name.equals(e.children.get(i).name)) {
                return e.children.get(i);
            }
        }
        return null;
    }

    private void index(SvgElement e) {
        String id = e.attr("id");
        if (id != null && !ids.containsKey(id)) {
            ids.put(id, e);
        }
        if ("style".equals(e.name)) {
            parseSheet(e.text.toString());
        }
        for (int i = 0; i < e.children.size(); i++) {
            index(e.children.get(i));
        }
    }

    private void parseSheet(String css) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < css.length()) {
            if (css.startsWith("/*", i)) {
                int e = css.indexOf("*/", i + 2);
                i = e < 0 ? css.length() : e + 2;
                continue;
            }
            sb.append(css.charAt(i++));
        }
        String s = sb.toString();
        int pos = 0;
        while (pos < s.length()) {
            int open = s.indexOf('{', pos);
            if (open < 0) {
                break;
            }
            int close = s.indexOf('}', open);
            if (close < 0) {
                break;
            }
            String sel = s.substring(pos, open).trim();
            String body = s.substring(open + 1, close);
            if (sel.startsWith("@")) {
                note("style", "at-rule ignored: " + sel);
            } else {
                String[] sels = split(sel, ',');
                for (int k = 0; k < sels.length; k++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                    String one = sels[k].trim();
                    if (one.length() > 0) {
                        rules.add(new String[]{one, body});
                    }
                }
            }
            pos = close + 1;
        }
    }

    private static String[] split(String s, char c) {
        List<String> out = new ArrayList<String>();
        int st = 0;
        for (int i = 0; i <= s.length(); i++) {
            if (i == s.length() || s.charAt(i) == c) {
                out.add(s.substring(st, i));
                st = i + 1;
            }
        }
        return out.toArray(new String[out.size()]);
    }

    private static boolean matchesSimple(SvgElement e, String sel) {
        if ("*".equals(sel)) {
            return true;
        }
        String tag = null;
        List<String> classes = new ArrayList<String>();
        String id = null;
        int i = 0;
        int n = sel.length();
        StringBuilder cur = new StringBuilder();
        char kind = 't';
        while (i <= n) {
            char c = i < n ? sel.charAt(i) : 0;
            if (c == '.' || c == '#' || c == 0 || c == '[' || c == ':') {
                String v = cur.toString();
                if (v.length() > 0) {
                    if (kind == 't') {
                        tag = v;
                    } else if (kind == '.') {
                        classes.add(v);
                    } else if (kind == '#') {
                        id = v;
                    }
                }
                cur.setLength(0);
                if (c == '[' || c == ':') {
                    return false;
                }
                kind = c;
            } else {
                cur.append(c);
            }
            i++;
        }
        if (tag != null && !"*".equals(tag) && !tag.equals(e.name)) {
            return false;
        }
        if (id != null && !id.equals(e.attr("id"))) {
            return false;
        }
        if (!classes.isEmpty()) {
            String cls = e.attr("class");
            if (cls == null) {
                return false;
            }
            String[] have = split(cls.replace('\t', ' ').replace('\n', ' '), ' ');
            for (int k = 0; k < classes.size(); k++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                boolean found = false;
                for (int j = 0; j < have.length; j++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                    if (classes.get(k).equals(have[j])) {
                        found = true;
                    }
                }
                if (!found) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean matches(SvgElement e, String selector) {
        String[] parts = split(selector.replace('>', ' ').trim(), ' ');
        List<String> p = new ArrayList<String>();
        for (int i = 0; i < parts.length; i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            if (parts[i].trim().length() > 0) {
                p.add(parts[i].trim());
            }
        }
        if (p.isEmpty() || !matchesSimple(e, p.get(p.size() - 1))) {
            return false;
        }
        SvgElement anc = e.parent;
        for (int i = p.size() - 2; i >= 0; i--) {
            while (anc != null && !matchesSimple(anc, p.get(i))) {
                anc = anc.parent;
            }
            if (anc == null) {
                return false;
            }
            anc = anc.parent;
        }
        return true;
    }

    private static int specificity(String sel) {
        int s = 0;
        for (int i = 0; i < sel.length(); i++) {
            char c = sel.charAt(i);
            if (c == '#') {
                s += 100;
            } else if (c == '.') {
                s += 10;
            }
        }
        return s + 1;
    }

    private Map<String, String> computeStyle(SvgElement e, Map<String, String> parent) {
        Map<String, String> st = new HashMap<String, String>();
        for (int i = 0; i < INHERITED.length; i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            String v = parent.get(INHERITED[i]);
            if (v != null) {
                st.put(INHERITED[i], v);
            }
        }
        for (int i = 0; i < PRESENTATION.length; i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            String v = e.attr(PRESENTATION[i]);
            if (v != null) {
                put(st, PRESENTATION[i], v.trim(), parent);
            }
        }
        List<String[]> matched = new ArrayList<String[]>();
        for (String[] r : rules) {
            if (matches(e, r[0])) {
                matched.add(r);
            }
        }
        for (int pass = 0; pass < 1000 && !matched.isEmpty(); pass++) {
            int best = 0;
            for (int i = 1; i < matched.size(); i++) {
                if (specificity(matched.get(i)[0]) < specificity(matched.get(best)[0])) {
                    best = i;
                }
            }
            declarations(st, matched.remove(best)[1], parent);
        }
        String inline = e.attr("style");
        if (inline != null) {
            declarations(st, inline, parent);
        }
        return st;
    }

    private static void put(Map<String, String> st, String k, String v, Map<String, String> parent) {
        if ("inherit".equals(v)) {
            String pv = parent.get(k);
            if (pv != null) {
                st.put(k, pv);
            }
            return;
        }
        st.put(k, v);
    }

    private static void declarations(Map<String, String> st, String body, Map<String, String> parent) {
        String[] decls = split(body, ';');
        for (String d : decls) {
            int c = d.indexOf(':');
            if (c <= 0) {
                continue;
            }
            String k = d.substring(0, c).trim();
            String v = d.substring(c + 1).trim();
            if (v.endsWith("!important")) {
                v = v.substring(0, v.length() - 10).trim();
            }
            put(st, k, v, parent);
        }
    }

    private void children(SvgElement e, GroupNode into, Map<String, String> style) {
        for (int i = 0; i < e.children.size(); i++) {
            SvgElement c = e.children.get(i);
            ArtNode n = element(c, style);
            if (n != null) {
                into.add(n);
            }
            if ("switch".equals(e.name) && n != null) {
                return;
            }
        }
    }

    private ArtNode element(SvgElement e, Map<String, String> parentStyle) {
        String n = e.name;
        if ("defs".equals(n) || "symbol".equals(n) || "clipPath".equals(n) || "mask".equals(n)
                || "linearGradient".equals(n) || "radialGradient".equals(n) || "pattern".equals(n)
                || "filter".equals(n) || "marker".equals(n) || "style".equals(n) || "title".equals(n)
                || "desc".equals(n) || "metadata".equals(n)) {
            return null;
        }
        if (n.startsWith("animate") || "set".equals(n)) {
            note(n, "animation is not part of the static image and was not applied");
            return null;
        }
        if ("script".equals(n)) {
            note(n, "scripts are not run");
            return null;
        }
        if ("foreignObject".equals(n)) {
            note(n, "foreign content (HTML) is not rendered");
            return null;
        }
        Map<String, String> st = computeStyle(e, parentStyle);
        if ("none".equals(st.get("display"))) {
            return null;
        }
        String rf = e.attr("requiredFeatures");
        if (rf != null && rf.trim().length() == 0) {
            return null;
        }
        ArtNode node;
        if ("g".equals(n) || "a".equals(n) || "switch".equals(n)) {
            GroupNode g = new GroupNode();
            children(e, g, st);
            node = g;
        } else if ("svg".equals(n)) {
            node = nestedSvg(e, st);
        } else if ("use".equals(n)) {
            node = use(e, st);
        } else if ("text".equals(n)) {
            node = text(e, st);
        } else if ("image".equals(n)) {
            node = image(e);
        } else {
            GeneralPath p = shapePath(e);
            if (p == null) {
                return null;
            }
            ShapeNode s = new ShapeNode(p);
            paints(s, st, p);
            node = s;
            GroupNode markers = markers(st, p);
            if (markers != null) {
                GroupNode wrap = new GroupNode();
                wrap.add(s);
                wrap.add(markers);
                applyCommon(e, wrap, st);
                return wrap;
            }
        }
        if (node == null) {
            return null;
        }
        if ("hidden".equals(st.get("visibility")) || "collapse".equals(st.get("visibility"))) {
            if (!(node instanceof GroupNode)) {
                node.setVisible(false);
            }
        }
        applyCommon(e, node, st);
        return node;
    }

    private void applyCommon(SvgElement e, ArtNode node, Map<String, String> st) {
        node.setId(e.attr("id"));
        String label = e.attr("inkscape:label");
        if (label == null) {
            label = e.attr("label");
        }
        node.setName(label != null ? label : (e.attr("id") != null ? e.attr("id") : e.name));
        Matrix t = transform(e.attr("transform"));
        if ("use".equals(e.name)) {
            t = t.multiply(Matrix.translate(length(e.attr("x"), 0, 1), length(e.attr("y"), 0, 2)));
        }
        node.setTransform(node.getTransform().isIdentity() ? t : t.multiply(node.getTransform()));
        String op = st.get("opacity");
        if (op != null) {
            node.setOpacity((float) fraction(op, 1));
        }
        String blend = st.get("mix-blend-mode");
        if (blend != null) {
            node.setBlendMode(blendMode(blend));
        }
        if (node instanceof GroupNode && "isolate".equals(st.get("isolation"))) {
            ((GroupNode) node).setPassThrough(false);
        }
        String clip = ref(st.get("clip-path"));
        if (clip != null) {
            SvgElement ce = ids.get(clip);
            if (ce != null) {
                clipText.clear();
                GeneralPath cp = clipPath(ce, node);
                if (clipText.isEmpty()) {
                    node.setClipPath(cp);
                } else {
                    // Glyphs have no outline here, so a clip path with text becomes an alpha mask of
                    // the shapes and the text drawn opaque, which clips exactly the same pixels.
                    GroupNode cm = new GroupNode();
                    ShapeNode shapes = new ShapeNode(cp);
                    shapes.fill(new SolidPaint(0xff000000));
                    cm.getChildren().add(shapes);
                    cm.getChildren().addAll(clipText);
                    clipText.clear();
                    node.setMask(cm, false);
                }
            }
        }
        String mask = ref(st.get("mask"));
        if (mask != null) {
            SvgElement me = ids.get(mask);
            if (me != null) {
                node.setMask(maskNode(me, node, st), true);
            }
        }
        String filter = st.get("filter");
        if (filter != null && !"none".equals(filter)) {
            String fid = ref(filter);
            if (fid != null && ids.get(fid) != null) {
                node.getEffects().add(filterEffect(ids.get(fid), node));
            } else {
                cssFilters(filter, node);
            }
        }
    }

    private void cssFilters(String f, ArtNode node) {
        int i = 0;
        while (i < f.length()) {
            int p = f.indexOf('(', i);
            int q = f.indexOf(')', p + 1);
            if (p < 0 || q < 0) {
                return;
            }
            String fn = f.substring(i, p).trim();
            String arg = f.substring(p + 1, q).trim();
            double v = length(arg.endsWith("%") ? null : arg, 0, 0);
            double frac = fraction(arg, 1);
            if ("blur".equals(fn)) {
                node.getEffects().add(Effects.gaussianBlur((float) (v * 2)));
            } else if ("brightness".equals(fn)) {
                float b = (float) frac;
                float[] bright = {b, 0, 0, 0, 0, 0, b, 0, 0, 0, 0, 0, b, 0, 0, 0, 0, 0, 1, 0};
                node.getEffects().add(Effects.colorMatrix(bright));
            } else if ("grayscale".equals(fn)) {
                node.getEffects().add(Effects.hueSaturation(0, (float) -frac, 0));
            } else if ("invert".equals(fn)) {
                node.getEffects().add(Effects.invert().set("opacity", (float) frac));
            } else if ("saturate".equals(fn)) {
                node.getEffects().add(Effects.hueSaturation(0, (float) (frac - 1), 0));
            } else if ("hue-rotate".equals(fn)) {
                node.getEffects().add(Effects.hueSaturation((float) number(arg.replace("deg", ""), 0), 0, 0));
            } else if ("contrast".equals(fn)) {
                node.getEffects().add(Effects.brightnessContrast(0, (float) (frac - 1)));
            } else if ("opacity".equals(fn)) {
                node.setOpacity(node.getOpacity() * (float) frac);
            } else if ("drop-shadow".equals(fn)) {
                String[] parts = tokens(arg);
                double dx = parts.length > 0 ? length(parts[0], 0, 0) : 0;
                double dy = parts.length > 1 ? length(parts[1], 0, 0) : 0;
                double r = parts.length > 2 && Character.isDigit(parts[2].charAt(0)) ? length(parts[2], 0, 0) : 0;
                int c = 0xff000000;
                for (int k = 0; k < parts.length; k++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                    if (!Character.isDigit(parts[k].charAt(0)) && parts[k].charAt(0) != '-' && parts[k].charAt(0) != '.') {
                        c = Colors.parse(parts[k], c);
                    }
                }
                node.getEffects().add(Effects.dropShadow(c, (float) dx, (float) dy, (float) r, 0));
            } else if ("sepia".equals(fn)) {
                float a = (float) frac;
                float[] sepia = {1 - 0.607f * a, 0.769f * a, 0.189f * a, 0, 0, 0.349f * a, 1 - 0.314f * a,
                    0.168f * a, 0, 0, 0.272f * a, 0.534f * a, 1 - 0.869f * a, 0, 0, 0, 0, 0, 1, 0};
                node.getEffects().add(Effects.colorMatrix(sepia));
            }
            i = q + 1;
        }
    }

    private static String[] tokens(String s) {
        List<String> out = new ArrayList<String>();
        int depth = 0;
        int st = 0;
        for (int i = 0; i <= s.length(); i++) {
            char c = i < s.length() ? s.charAt(i) : ' ';
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }
            if (depth == 0 && c == ' ') {
                String t = s.substring(st, i).trim();
                if (t.length() > 0) {
                    out.add(t);
                }
                st = i + 1;
            }
        }
        return out.toArray(new String[out.size()]);
    }

    private ArtNode nestedSvg(SvgElement e, Map<String, String> st) {
        double x = length(e.attr("x"), 0, 1);
        double y = length(e.attr("y"), 0, 2);
        double[] vb = numbers(e.attr("viewBox"));
        double w = length(e.attr("width"), viewportW, 1);
        double h = length(e.attr("height"), viewportH, 2);
        GroupNode g = new GroupNode();
        double pw = viewportW;
        double ph = viewportH;
        if (vb.length == 4) {
            viewportW = vb[2];
            viewportH = vb[3];
        }
        children(e, g, st);
        viewportW = pw;
        viewportH = ph;
        g.setTransform(viewBoxTransform(vb, e.attr("preserveAspectRatio"), x, y, w, h));
        if (!"visible".equals(e.attr("overflow"))) {
            Matrix inv = g.getTransform().invert();
            if (inv != null) {
                g.setClipPath(Shapes.transform(Shapes.roundRect(x, y, w, h, 0, 0), inv));
            }
        }
        return g;
    }

    private ArtNode use(SvgElement e, Map<String, String> st) {
        String href = e.attr("href");
        if (href == null || !href.startsWith("#")) {
            if (href != null) {
                note("use", "external reference not loaded: " + href);
            }
            return null;
        }
        SvgElement t = ids.get(href.substring(1));
        if (t == null || useDepth > 32) {
            return null;
        }
        if (++useExpansions > MAX_USE_EXPANSIONS) {
            if (useExpansions == MAX_USE_EXPANSIONS + 1) {
                note("use", "more than " + MAX_USE_EXPANSIONS + " references expanded; the rest were dropped");
            }
            return null;
        }
        for (SvgElement a = e; a != null; a = a.parent) {
            if (a == t) { // NOPMD CompareObjectsWithEquals - node identity
                return null;
            }
        }
        useDepth++;
        try {
            GroupNode g = new GroupNode();
            if ("symbol".equals(t.name)) {
                Map<String, String> sst = computeStyle(t, st);
                children(t, g, sst);
                double[] vb = numbers(t.attr("viewBox"));
                double w = length(e.attr("width") != null ? e.attr("width") : t.attr("width"), vb.length == 4 ? vb[2] : viewportW, 1);
                double h = length(e.attr("height") != null ? e.attr("height") : t.attr("height"), vb.length == 4 ? vb[3] : viewportH, 2);
                GroupNode inner = new GroupNode();
                inner.getChildren().addAll(g.getChildren());
                inner.setTransform(viewBoxTransform(vb, t.attr("preserveAspectRatio"), 0, 0, w, h));
                GroupNode outer = new GroupNode();
                outer.add(inner);
                outer.setSymbolId(t.attr("id"));
                return outer;
            }
            SvgElement saved = t.parent;
            t.parent = e;
            ArtNode n = element(t, st);
            t.parent = saved;
            if (n != null) {
                g.add(n);
            }
            g.setSymbolId(t.attr("id"));
            return g;
        } finally {
            useDepth--;
        }
    }

    private GeneralPath shapePath(SvgElement e) {
        String n = e.name;
        if ("path".equals(n)) {
            return PathData.parse(e.attr("d"));
        }
        if ("rect".equals(n)) {
            double w = length(e.attr("width"), 0, 1);
            double h = length(e.attr("height"), 0, 2);
            if (w <= 0 || h <= 0) {
                return null;
            }
            String rxs = e.attr("rx");
            String rys = e.attr("ry");
            double rx = length(rxs, -1, 1);
            double ry = length(rys, -1, 2);
            if (rx < 0 && ry >= 0) {
                rx = ry;
            }
            if (ry < 0 && rx >= 0) {
                ry = rx;
            }
            return Shapes.roundRect(length(e.attr("x"), 0, 1), length(e.attr("y"), 0, 2), w, h,
                    Math.max(0, rx), Math.max(0, ry));
        }
        if ("circle".equals(n)) {
            double r = length(e.attr("r"), 0, 0);
            if (r <= 0) {
                return null;
            }
            return Shapes.ellipse(length(e.attr("cx"), 0, 1), length(e.attr("cy"), 0, 2), r, r);
        }
        if ("ellipse".equals(n)) {
            double rx = length(e.attr("rx"), -1, 1);
            double ry = length(e.attr("ry"), -1, 2);
            if (rx < 0) {
                rx = ry;
            }
            if (ry < 0) {
                ry = rx;
            }
            if (rx <= 0 || ry <= 0) {
                return null;
            }
            return Shapes.ellipse(length(e.attr("cx"), 0, 1), length(e.attr("cy"), 0, 2), rx, ry);
        }
        if ("line".equals(n)) {
            GeneralPath p = new GeneralPath();
            p.moveTo(length(e.attr("x1"), 0, 1), length(e.attr("y1"), 0, 2));
            p.lineTo(length(e.attr("x2"), 0, 1), length(e.attr("y2"), 0, 2));
            return p;
        }
        if ("polyline".equals(n) || "polygon".equals(n)) {
            double[] pts = numbers(e.attr("points"));
            if (pts.length < 4) {
                return null;
            }
            if (pts.length % 2 == 1) {
                double[] t = new double[pts.length - 1];
                System.arraycopy(pts, 0, t, 0, t.length);
                pts = t;
            }
            return Shapes.polygon(pts, "polygon".equals(n));
        }
        return null;
    }

    private void paints(ShapeNode s, Map<String, String> st, GeneralPath p) {
        String rule = st.get("fill-rule");
        p.setWindingRule("evenodd".equals(rule) ? PathIterator.WIND_EVEN_ODD : PathIterator.WIND_NON_ZERO);
        String fill = st.get("fill");
        if (fill == null) {
            fill = "black";
        }
        ArtPaint fp = paint(fill, st, fraction(st.get("fill-opacity"), 1));
        String stroke = st.get("stroke");
        ArtPaint sp = stroke == null ? null : paint(stroke, st, fraction(st.get("stroke-opacity"), 1));
        StrokeStyle ss = null;
        if (sp != null) {
            ss = new StrokeStyle(sp, (float) length(st.get("stroke-width"), 1, 0));
            String cap = st.get("stroke-linecap");
            ss.setCap("round".equals(cap) ? StrokeStyle.CAP_ROUND : "square".equals(cap) ? StrokeStyle.CAP_SQUARE : StrokeStyle.CAP_BUTT);
            String join = st.get("stroke-linejoin");
            ss.setJoin("round".equals(join) ? StrokeStyle.JOIN_ROUND : "bevel".equals(join) ? StrokeStyle.JOIN_BEVEL : StrokeStyle.JOIN_MITER);
            ss.setMiterLimit((float) number(st.get("stroke-miterlimit"), 4));
            String da = st.get("stroke-dasharray");
            if (da != null && !"none".equals(da)) {
                double[] d = numbers(da.replace("px", ""));
                boolean valid = d.length > 0;
                double sum = 0;
                for (int i = 0; i < d.length; i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                    if (d[i] < 0) {
                        valid = false;
                    }
                    sum += d[i];
                }
                if (valid && sum > 0) {
                    int len = d.length % 2 == 1 ? d.length * 2 : d.length;
                    float[] f = new float[len];
                    for (int i = 0; i < len; i++) {
                        f[i] = (float) d[i % d.length];
                    }
                    ss.setDash(f);
                }
            }
            ss.setDashOffset((float) length(st.get("stroke-dashoffset"), 0, 0));
        }
        String order = st.get("paint-order");
        if (fp != null) {
            s.fill(fp);
        }
        if (ss != null) {
            if (fp != null && order != null && order.trim().startsWith("stroke")) {
                // A stroke painted under its fill shows only the half outside the shape.
                ss.setWidth(ss.getWidth() / 2);
                ss.setAlignment(StrokeStyle.ALIGN_OUTSIDE);
            }
            s.stroke(ss);
        }
    }

    private ArtPaint paint(String v, Map<String, String> st, double opacity) {
        v = v.trim();
        if ("none".equals(v) || v.length() == 0) {
            return null;
        }
        ArtPaint p;
        if (v.startsWith("url(")) {
            String id = ref(v);
            SvgElement e = id == null ? null : ids.get(id);
            p = e == null ? null : server(e);
            if (p == null) {
                int close = v.indexOf(')');
                String fb = close > 0 ? v.substring(close + 1).trim() : "";
                return fb.length() > 0 ? paint(fb, st, opacity) : null;
            }
        } else {
            if ("currentColor".equals(v)) {
                v = st.get("color") == null ? "black" : st.get("color");
            }
            p = new SolidPaint(Colors.parse(v, 0xff000000));
        }
        p.setOpacity((float) (p.getOpacity() * opacity));
        return p;
    }

    private ArtPaint server(SvgElement e) {
        if ("linearGradient".equals(e.name) || "radialGradient".equals(e.name)) {
            return gradient(e);
        }
        if ("pattern".equals(e.name)) {
            return pattern(e);
        }
        if ("solidColor".equals(e.name) || "solidcolor".equals(e.name)) {
            Map<String, String> st = computeStyle(e, new HashMap<String, String>());
            String c = e.attr("solid-color");
            if (c == null) {
                c = st.get("solid-color");
            }
            SolidPaint sp = new SolidPaint(Colors.parse(c, 0xff000000));
            sp.setOpacity((float) fraction(e.attr("solid-opacity"), 1));
            return sp;
        }
        return null;
    }

    private String inheritAttr(SvgElement e, String name) {
        for (int i = 0; e != null && i < 16; i++) {
            String v = e.attr(name);
            if (v != null) {
                return v;
            }
            String h = e.attr("href");
            e = h != null && h.startsWith("#") ? ids.get(h.substring(1)) : null;
        }
        return null;
    }

    private GradientPaint gradient(SvgElement e) {
        SvgElement stops = e;
        for (int i = 0; i < 16 && stops != null && first(stops, "stop") == null; i++) {
            String h = stops.attr("href");
            stops = h != null && h.startsWith("#") ? ids.get(h.substring(1)) : null;
        }
        List<Float> pos = new ArrayList<Float>();
        List<Integer> cols = new ArrayList<Integer>();
        if (stops != null) {
            float last = 0;
            for (int i = 0; i < stops.children.size(); i++) {
                SvgElement s = stops.children.get(i);
                if (!"stop".equals(s.name)) {
                    continue;
                }
                Map<String, String> st = computeStyle(s, new HashMap<String, String>());
                float off = (float) fraction(s.attr("offset"), 0);
                off = Math.max(last, Math.min(1, Math.max(0, off)));
                last = off;
                String sc = st.get("stop-color");
                if ("currentColor".equals(sc)) {
                    sc = st.get("color");
                }
                int c = Colors.parse(sc == null ? "black" : sc, 0xff000000);
                double so = fraction(st.get("stop-opacity"), 1);
                int a = (int) ((c >>> 24) * so + 0.5);
                pos.add(Float.valueOf(off));
                cols.add(Integer.valueOf((a << 24) | (c & 0xffffff)));
            }
        }
        if (pos.isEmpty()) {
            return null;
        }
        float[] p = new float[pos.size()];
        int[] c = new int[cols.size()];
        for (int i = 0; i < p.length; i++) {
            p[i] = pos.get(i).floatValue();
            c[i] = cols.get(i).intValue();
        }
        boolean bbox = !"userSpaceOnUse".equals(inheritAttr(e, "gradientUnits"));
        GradientPaint g;
        int ax = bbox ? -1 : 1;
        int ay = bbox ? -1 : 2;
        if ("linearGradient".equals(e.name)) {
            g = GradientPaint.linear(len(inheritAttr(e, "x1"), 0, ax), len(inheritAttr(e, "y1"), 0, ay),
                    len(inheritAttr(e, "x2"), bbox ? 1 : viewportW, ax), len(inheritAttr(e, "y2"), 0, ay), p, c);
        } else {
            double cx = len(inheritAttr(e, "cx"), bbox ? 0.5 : viewportW / 2, ax);
            double cy = len(inheritAttr(e, "cy"), bbox ? 0.5 : viewportH / 2, ay);
            double r = len(inheritAttr(e, "r"), bbox ? 0.5 : Math.sqrt(viewportW * viewportW + viewportH * viewportH) / Math.sqrt(8), bbox ? -1 : 0);
            g = GradientPaint.radial(cx, cy, r, p, c);
            String fxs = inheritAttr(e, "fx");
            String fys = inheritAttr(e, "fy");
            g.setRadial(r, fxs == null ? cx : len(fxs, cx, ax), fys == null ? cy : len(fys, cy, ay));
        }
        g.setObjectBoundingBox(bbox);
        g.setTransform(transform(inheritAttr(e, "gradientTransform")));
        String sm = inheritAttr(e, "spreadMethod");
        g.setSpread("reflect".equals(sm) ? GradientPaint.SPREAD_REFLECT : "repeat".equals(sm) ? GradientPaint.SPREAD_REPEAT : GradientPaint.SPREAD_PAD);
        return g;
    }

    private double len(String v, double def, int axis) {
        if (axis < 0) {
            return fraction(v, def);
        }
        return length(v, def, axis);
    }

    private ArtPaint pattern(SvgElement e) {
        if (useDepth > 16) {
            return null;
        }
        boolean userUnits = "userSpaceOnUse".equals(inheritAttr(e, "patternUnits"));
        double w = len(inheritAttr(e, "width"), 0, userUnits ? 1 : -1);
        double h = len(inheritAttr(e, "height"), 0, userUnits ? 2 : -1);
        double x = len(inheritAttr(e, "x"), 0, userUnits ? 1 : -1);
        double y = len(inheritAttr(e, "y"), 0, userUnits ? 2 : -1);
        if (w <= 0 || h <= 0) {
            return null;
        }
        SvgElement content = e;
        for (int i = 0; i < 16 && content != null && content.children.isEmpty(); i++) {
            String hr = content.attr("href");
            content = hr != null && hr.startsWith("#") ? ids.get(hr.substring(1)) : null;
        }
        if (content == null) {
            return null;
        }
        useDepth++;
        try {
            GroupNode g = new GroupNode();
            children(content, g, computeStyle(content, new HashMap<String, String>()));
            double[] vb = numbers(inheritAttr(e, "viewBox"));
            boolean contentBox = "objectBoundingBox".equals(inheritAttr(e, "patternContentUnits"));
            double tileW = userUnits ? w : w * 100;
            double tileH = userUnits ? h : h * 100;
            if (vb.length == 4) {
                g.setTransform(viewBoxTransform(vb, inheritAttr(e, "preserveAspectRatio"), 0, 0, tileW, tileH));
            } else if (contentBox || !userUnits) {
                g.setTransform(Matrix.scale(userUnits ? 1 : 100, userUnits ? 1 : 100));
                if (!contentBox && !userUnits) {
                    note("pattern", "objectBoundingBox pattern tile with user-space content approximated");
                }
            }
            double res = 2;
            int pw = Math.max(1, Math.min(2048, (int) Math.ceil(tileW * res)));
            int ph = Math.max(1, Math.min(2048, (int) Math.ceil(tileH * res)));
            Raster tile = new Raster(0, 0, pw, ph);
            new ArtRenderer().renderNode(g, Matrix.scale(pw / tileW, ph / tileH), tile);
            ImagePaint ip = new ImagePaint(tile);
            ip.setTiled(true);
            Matrix pt = transform(inheritAttr(e, "patternTransform"));
            if (userUnits) {
                ip.setTransform(pt.multiply(Matrix.translate(x, y)).multiply(Matrix.scale(tileW / pw, tileH / ph)));
            } else {
                ip.setObjectBoundingBox(true);
                ip.setTransform(pt.multiply(Matrix.translate(x, y)).multiply(Matrix.scale(w / pw, h / ph)));
            }
            return ip;
        } finally {
            useDepth--;
        }
    }

    /// Text found by the last `clipPath` call, drawn opaque and placed in the target's space.
    private final List<ArtNode> clipText = new ArrayList<ArtNode>();

    private static void opaque(ArtNode n) {
        n.setOpacity(1);
        n.getEffects().clear();
        if (n instanceof TextNode) {
            List<TextRun> runs = ((TextNode) n).getRuns();
            for (int i = 0; i < runs.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                runs.get(i).setFill(new SolidPaint(0xff000000));
            }
        } else if (n instanceof GroupNode) {
            List<ArtNode> ch = ((GroupNode) n).getChildren();
            for (int i = 0; i < ch.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                opaque(ch.get(i));
            }
        }
    }

    private GeneralPath clipPath(SvgElement ce, ArtNode target) {
        GeneralPath out = new GeneralPath();
        boolean bbox = "objectBoundingBox".equals(ce.attr("clipPathUnits"));
        Matrix base = transform(ce.attr("transform"));
        if (bbox) {
            double[] b = target.getLocalBounds();
            if (b == null) {
                return out;
            }
            base = new Matrix(b[2], 0, 0, b[3], b[0], b[1]).multiply(base);
        }
        Map<String, String> cst = computeStyle(ce, new HashMap<String, String>());
        for (int i = 0; i < ce.children.size(); i++) {
            SvgElement c = ce.children.get(i);
            SvgElement shape = c;
            Matrix m = base.multiply(transform(c.attr("transform")));
            if ("use".equals(c.name) && c.attr("href") != null && c.attr("href").startsWith("#")) {
                shape = ids.get(c.attr("href").substring(1));
                m = m.multiply(Matrix.translate(length(c.attr("x"), 0, 1), length(c.attr("y"), 0, 2)));
                if (shape == null) {
                    continue;
                }
                m = m.multiply(transform(shape.attr("transform")));
            }
            if ("text".equals(shape.name)) {
                ArtNode t = text(shape, cst);
                opaque(t);
                t.setTransform(m.multiply(t.getTransform()));
                clipText.add(t);
                continue;
            }
            GeneralPath p = shapePath(shape);
            if (p != null) {
                Map<String, String> st = computeStyle(shape, cst);
                GeneralPath tp = Shapes.transform(p, m);
                if ("evenodd".equals(st.get("clip-rule"))) {
                    note("clipPath", "evenodd clip rule applied per path");
                }
                out.append(tp, false);
            }
        }
        out.setWindingRule(PathIterator.WIND_NON_ZERO);
        return out;
    }

    private ArtNode maskNode(SvgElement me, ArtNode target, Map<String, String> st) {
        GroupNode g = new GroupNode();
        children(me, g, computeStyle(me, st));
        if ("objectBoundingBox".equals(me.attr("maskContentUnits"))) {
            double[] b = target.getLocalBounds();
            if (b != null) {
                g.setTransform(new Matrix(b[2], 0, 0, b[3], b[0], b[1]));
            }
        }
        boolean userUnits = "userSpaceOnUse".equals(me.attr("maskUnits"));
        double[] b = target.getLocalBounds();
        if (userUnits) {
            double x = length(me.attr("x"), -viewportW * 0.1, 1);
            double y = length(me.attr("y"), -viewportH * 0.1, 2);
            double w = length(me.attr("width"), viewportW * 1.2, 1);
            double h = length(me.attr("height"), viewportH * 1.2, 2);
            g.setClipPath(Shapes.roundRect(x, y, w, h, 0, 0));
        } else if (b != null) {
            double x = b[0] + fraction(me.attr("x"), -0.1) * b[2];
            double y = b[1] + fraction(me.attr("y"), -0.1) * b[3];
            double w = fraction(me.attr("width"), 1.2) * b[2];
            double h = fraction(me.attr("height"), 1.2) * b[3];
            Matrix inv = g.getTransform().invert();
            if (inv != null) {
                g.setClipPath(Shapes.transform(Shapes.roundRect(x, y, w, h, 0, 0), inv));
            }
        }
        g.setPassThrough(false);
        return g;
    }

    private Effect filterEffect(SvgElement fe, ArtNode target) {
        Effect e = Effects.svgFilter();
        List<Map<String, Object>> prims = e.getPrimitives();
        SvgElement src = fe;
        for (int i = 0; i < 16 && src != null && src.children.isEmpty(); i++) {
            String h = src.attr("href");
            src = h != null && h.startsWith("#") ? ids.get(h.substring(1)) : null;
        }
        if (src == null) {
            return e;
        }
        boolean primBbox = "objectBoundingBox".equals(fe.attr("primitiveUnits"));
        double[] b = target.getLocalBounds();
        double unit = primBbox && b != null ? Math.max(b[2], b[3]) : 1;
        Map<String, String> fst = computeStyle(src, new HashMap<String, String>());
        for (int i = 0; i < src.children.size(); i++) {
            SvgElement p = src.children.get(i);
            if (!p.name.startsWith("fe")) {
                continue;
            }
            prims.add(primitive(p, fst, unit));
        }
        if (b != null) {
            double margin = Math.max(b[2], b[3]) * 0.1;
            String fw = fe.attr("width");
            if (fw != null) {
                margin = Math.max(margin, (fraction(fw, 1.2) - 1) / 2 * Math.max(b[2], b[3]));
            }
            e.set("margin", (float) Math.max(4, margin));
        }
        String ci = fst.get("color-interpolation-filters");
        if ("sRGB".equals(ci)) {
            e.set("colorInterpolation", "sRGB");
        }
        return e;
    }

    private Map<String, Object> primitive(SvgElement p, Map<String, String> parent, double unit) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("type", p.name);
        Map<String, String> st = computeStyle(p, parent);
        Iterator<Map.Entry<String, String>> it = p.attrs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> en = it.next();
            if (!"type".equals(en.getKey())) {
                m.put(en.getKey(), en.getValue());
            }
        }
        String t = p.attr("type");
        if ("feColorMatrix".equals(p.name)) {
            m.put("matrixType", t == null ? "matrix" : t);
        } else if ("feTurbulence".equals(p.name)) {
            m.put("noiseType", t == null ? "turbulence" : t);
        } else if (t != null) {
            m.put("funcType", t);
        }
        String[] styled = {"flood-color", "flood-opacity", "lighting-color"};
        for (int i = 0; i < styled.length; i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            if (st.get(styled[i]) != null) {
                m.put(styled[i], st.get(styled[i]));
            }
        }
        if (unit != 1) {
            String[] lengths = {"dx", "dy", "stdDeviation", "radius", "scale"};
            for (int i = 0; i < lengths.length; i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                Object v = m.get(lengths[i]);
                if (v != null) {
                    double[] n = numbers(v.toString());
                    StringBuilder sb = new StringBuilder();
                    for (int k = 0; k < n.length; k++) {
                        if (k > 0) {
                            sb.append(' ');
                        }
                        sb.append(n[k] * unit);
                    }
                    m.put(lengths[i], sb.toString());
                }
            }
        }
        if ("feImage".equals(p.name)) {
            String href = p.attr("href");
            Raster r = loadImage(href);
            if (r != null) {
                m.put("image", r);
            }
        }
        if (!p.children.isEmpty()) {
            List<Map<String, Object>> ch = new ArrayList<Map<String, Object>>();
            for (int i = 0; i < p.children.size(); i++) {
                Map<String, Object> cm = primitive(p.children.get(i), st, unit);
                ch.add(cm);
            }
            m.put("children", ch);
        }
        return m;
    }

    private ArtNode text(SvgElement e, Map<String, String> st) {
        TextNode t = new TextNode();
        double x = length(firstOf(e.attr("x")), 0, 1);
        double y = length(firstOf(e.attr("y")), 0, 2);
        x += length(firstOf(e.attr("dx")), 0, 1);
        y += length(firstOf(e.attr("dy")), 0, 2);
        t.setPosition(x, y);
        String anchor = st.get("text-anchor");
        t.setAlign("middle".equals(anchor) ? TextNode.ALIGN_CENTER : "end".equals(anchor) ? TextNode.ALIGN_RIGHT : TextNode.ALIGN_LEFT);
        GroupNode extra = new GroupNode();
        textRuns(e, st, t, extra);
        trimRuns(t);
        if (extra.getChildren().isEmpty()) {
            return t;
        }
        extra.getChildren().add(0, t);
        return extra;
    }

    private static String firstOf(String v) {
        if (v == null) {
            return null;
        }
        String[] p = split(v.trim().replace(',', ' '), ' ');
        return p.length > 0 ? p[0] : null;
    }

    private void textRuns(SvgElement e, Map<String, String> st, TextNode t, GroupNode extra) {
        for (int i = 0; i < e.content.size(); i++) {
            Object o = e.content.get(i);
            if (o instanceof String) {
                String s = collapse((String) o);
                if (s.length() > 0) {
                    t.getRuns().add(run(s, st));
                }
            } else if (o instanceof SvgElement) {
                SvgElement c = (SvgElement) o;
                if ("textPath".equals(c.name)) {
                    textPath(c, computeStyle(c, st), extra);
                } else if ("tspan".equals(c.name) || "a".equals(c.name)) {
                    Map<String, String> cst = computeStyle(c, st);
                    if (c.attr("x") != null || c.attr("y") != null) {
                        TextNode sub = new TextNode();
                        sub.setPosition(length(firstOf(c.attr("x")), t.getX(), 1),
                                length(firstOf(c.attr("y")), t.getY(), 2) + length(firstOf(c.attr("dy")), 0, 2));
                        sub.setAlign(t.getAlign());
                        textRuns(c, cst, sub, extra);
                        trimRuns(sub);
                        extra.add(sub);
                    } else {
                        textRuns(c, cst, t, extra);
                    }
                }
            }
        }
    }

    /// Lays the text of a `textPath` out along the path it names: each character is placed at
    /// the point its middle reaches along the path and turned to the path's direction there.
    private void textPath(SvgElement c, Map<String, String> st, GroupNode extra) {
        String href = c.attr("href");
        SvgElement pe = href != null && href.startsWith("#") ? ids.get(href.substring(1)) : null;
        String d = pe == null ? null : pe.attr("d");
        if (d == null) {
            note("textPath", "path not found: " + (href == null ? "(none)" : href));
            return;
        }
        Matrix pm = pe.attr("transform") == null ? Matrix.IDENTITY : transform(pe.attr("transform"));
        Polylines pl = Polylines.flatten(PathData.parse(d), pm, 0.25);
        if (pl.lines.isEmpty()) {
            return;
        }
        double[] pts = pl.lines.get(0);
        int np = pts.length / 2;
        double[] acc = new double[np];
        for (int i = 1; i < np; i++) {
            double dx = pts[i * 2] - pts[i * 2 - 2];
            double dy = pts[i * 2 + 1] - pts[i * 2 - 1];
            acc[i] = acc[i - 1] + Math.sqrt(dx * dx + dy * dy);
        }
        double total = acc[np - 1];
        StringBuilder text = new StringBuilder();
        collectText(c, text);
        String str = collapse(text.toString()).trim();
        TextRun proto = run(str, st);
        String so = c.attr("startOffset");
        double pos = 0;
        if (so != null) {
            so = so.trim();
            pos = so.endsWith("%") ? total * number(so.substring(0, so.length() - 1), 0) / 100
                    : length(so, 0, 1);
        }
        String anchor = st.get("text-anchor");
        double width = TextLayout.measure(proto, str, 1);
        if ("middle".equals(anchor)) {
            pos -= width / 2;
        } else if ("end".equals(anchor)) {
            pos -= width;
        }
        int seg = 1;
        for (int i = 0; i < str.length(); i++) {
            String ch = str.substring(i, i + 1);
            double adv = TextLayout.measure(proto, ch, 1);
            double mid = pos + adv / 2;
            pos += adv;
            if (mid < 0 || mid > total || ch.charAt(0) == ' ') {
                // Characters past either end of the path are not drawn, as the specification says.
                continue;
            }
            while (seg < np - 1 && acc[seg] < mid) {
                seg++;
            }
            double sl = acc[seg] - acc[seg - 1];
            double f = sl <= 0 ? 0 : (mid - acc[seg - 1]) / sl;
            double x0 = pts[seg * 2 - 2];
            double y0 = pts[seg * 2 - 1];
            double x1 = pts[seg * 2];
            double y1 = pts[seg * 2 + 1];
            double ang = MathUtil.atan2(y1 - y0, x1 - x0);
            TextNode g = new TextNode();
            g.getRuns().add(run(ch, st));
            g.setPosition(-adv / 2, 0);
            g.setTransform(Matrix.translate(x0 + (x1 - x0) * f, y0 + (y1 - y0) * f).multiply(Matrix.rotate(ang)));
            extra.add(g);
        }
    }

    private static void collectText(SvgElement e, StringBuilder out) {
        for (Object o : e.content) {
            if (o instanceof String) {
                out.append((String) o);
            } else if (o instanceof SvgElement) {
                collectText((SvgElement) o, out);
            }
        }
    }

    private static void trimRuns(TextNode t) {
        List<TextRun> r = t.getRuns();
        if (!r.isEmpty()) {
            TextRun f = r.get(0);
            String s = f.getText();
            int i = 0;
            while (i < s.length() && s.charAt(i) == ' ') {
                i++;
            }
            f.setText(s.substring(i));
            TextRun l = r.get(r.size() - 1);
            s = l.getText();
            int j = s.length();
            while (j > 0 && s.charAt(j - 1) == ' ') {
                j--;
            }
            l.setText(s.substring(0, j));
        }
    }

    private static String collapse(String s) {
        StringBuilder sb = new StringBuilder();
        boolean space = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\n' || c == '\t' || c == '\r') {
                if (!space) {
                    sb.append(' ');
                }
                space = true;
            } else {
                sb.append(c);
                space = false;
            }
        }
        return sb.toString();
    }

    private TextRun run(String s, Map<String, String> st) {
        String fam = st.get("font-family");
        if (fam != null) {
            int c = fam.indexOf(',');
            fam = (c > 0 ? fam.substring(0, c) : fam).trim();
            if (fam.length() > 1 && (fam.charAt(0) == '\'' || fam.charAt(0) == '"')) {
                fam = fam.substring(1, fam.length() - 1);
            }
        }
        float size = (float) length(st.get("font-size"), 16, 0);
        String fill = st.get("fill");
        ArtPaint p = paint(fill == null ? "black" : fill, st, fraction(st.get("fill-opacity"), 1));
        TextRun r = new TextRun(s, fam, size, p == null ? new SolidPaint(0) : p);
        String fw = st.get("font-weight");
        r.setBold(fw != null && ("bold".equals(fw) || "bolder".equals(fw) || number(fw, 400) >= 600));
        String fs = st.get("font-style");
        r.setItalic("italic".equals(fs) || "oblique".equals(fs));
        String td = st.get("text-decoration");
        if (td != null) {
            r.setUnderline(td.indexOf("underline") >= 0);
            r.setStrikethrough(td.indexOf("line-through") >= 0);
        }
        String ls = st.get("letter-spacing");
        if (ls != null && !"normal".equals(ls)) {
            r.setLetterSpacing((float) length(ls, 0, 0));
        }
        return r;
    }

    private ArtNode image(SvgElement e) {
        String href = e.attr("href");
        double x = length(e.attr("x"), 0, 1);
        double y = length(e.attr("y"), 0, 2);
        byte[] data = load(href);
        if (data == null) {
            note("image", "image not loaded: " + (href == null ? "(none)" : abbreviate(href)));
            return null;
        }
        if (looksLikeSvg(data)) {
            try {
                ArtDocument sub = new SvgReader().setResolver(resolver).parse(new ByteArrayInputStream(data));
                double w = length(e.attr("width"), sub.getWidth(), 1);
                double h = length(e.attr("height"), sub.getHeight(), 2);
                GroupNode g = sub.getRoot();
                g.setTransform(viewBoxTransform(new double[]{0, 0, sub.getWidth(), sub.getHeight()},
                        e.attr("preserveAspectRatio"), x, y, w, h));
                return g;
            } catch (IOException ex) {
                note("image", "embedded SVG could not be read: " + ex.getMessage());
                return null;
            }
        }
        Raster r = ArtImages.decode(data);
        if (r == null) {
            note("image", "image format could not be decoded here");
            return null;
        }
        double w = length(e.attr("width"), r.getWidth(), 1);
        double h = length(e.attr("height"), r.getHeight(), 2);
        RasterNode n = new RasterNode(r);
        n.setTransform(viewBoxTransform(new double[]{0, 0, r.getWidth(), r.getHeight()},
                e.attr("preserveAspectRatio"), x, y, w, h));
        return n;
    }

    private Raster loadImage(String href) {
        byte[] d = load(href);
        return d == null ? null : ArtImages.decode(d);
    }

    private static boolean looksLikeSvg(byte[] d) {
        int n = Math.min(d.length, 512);
        for (int i = 0; i + 4 < n; i++) {
            if (d[i] == '<' && d[i + 1] == 's' && d[i + 2] == 'v' && d[i + 3] == 'g') {
                return true;
            }
        }
        return false;
    }

    private static String abbreviate(String s) {
        return s.length() > 60 ? s.substring(0, 60) + "..." : s;
    }

    private byte[] load(String href) {
        if (href == null) {
            return null;
        }
        href = href.trim();
        if (href.startsWith("data:")) {
            int comma = href.indexOf(',');
            if (comma < 0) {
                return null;
            }
            String meta = href.substring(5, comma);
            String payload = href.substring(comma + 1);
            if (meta.endsWith(";base64")) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < payload.length(); i++) {
                    char c = payload.charAt(i);
                    if (c > ' ') {
                        sb.append(c);
                    }
                }
                try {
                    return Base64.decode(sb.toString().getBytes("UTF-8"));
                } catch (IOException ex) {
                    return null;
                }
            }
            try {
                return com.codename1.io.Util.decode(payload, "UTF-8", false).getBytes("UTF-8");
            } catch (IOException ex) {
                return null;
            }
        }
        return resolver == null ? null : resolver.load(href);
    }

    private GroupNode markers(Map<String, String> st, GeneralPath p) {
        String all = st.get("marker");
        String ms = ref(st.get("marker-start") != null ? st.get("marker-start") : all);
        String mm = ref(st.get("marker-mid") != null ? st.get("marker-mid") : all);
        String me = ref(st.get("marker-end") != null ? st.get("marker-end") : all);
        if (ms == null && mm == null && me == null) {
            return null;
        }
        List<double[]> verts = vertices(p);
        if (verts.isEmpty()) {
            return null;
        }
        GroupNode g = new GroupNode();
        double sw = length(st.get("stroke-width"), 1, 0);
        for (int i = 0; i < verts.size(); i++) {
            String id = i == 0 ? ms : (i == verts.size() - 1 ? me : mm);
            if (id == null || ids.get(id) == null || useDepth > 16) {
                continue;
            }
            SvgElement mk = ids.get(id);
            double[] v = verts.get(i);
            String orient = mk.attr("orient");
            double ang = 0;
            if ("auto".equals(orient) || "auto-start-reverse".equals(orient)) {
                ang = v[2];
                if (i == 0 && "auto-start-reverse".equals(orient)) {
                    ang += Math.PI;
                }
            } else if (orient != null) {
                ang = Math.toRadians(number(orient.replace("deg", ""), 0));
            }
            double scale = "userSpaceOnUse".equals(mk.attr("markerUnits")) ? 1 : sw;
            double mw = length(mk.attr("markerWidth"), 3, 1);
            double mh = length(mk.attr("markerHeight"), 3, 2);
            double[] vb = numbers(mk.attr("viewBox"));
            Matrix vbm = viewBoxTransform(vb, mk.attr("preserveAspectRatio"), 0, 0, mw, mh);
            double rx = length(mk.attr("refX"), 0, 1);
            double ry = length(mk.attr("refY"), 0, 2);
            double refX = vbm.mapX(rx, ry);
            double refY = vbm.mapY(rx, ry);
            useDepth++;
            GroupNode inner = new GroupNode();
            children(mk, inner, computeStyle(mk, new HashMap<String, String>()));
            useDepth--;
            inner.setTransform(Matrix.translate(v[0], v[1]).multiply(Matrix.rotate(ang))
                    .multiply(Matrix.scale(scale, scale)).multiply(Matrix.translate(-refX, -refY)).multiply(vbm));
            if (!"visible".equals(mk.attr("overflow"))) {
                Matrix inv = vbm.invert();
                if (inv != null) {
                    inner.setClipPath(Shapes.transform(Shapes.roundRect(0, 0, mw, mh, 0, 0), inv));
                }
            }
            g.add(inner);
        }
        return g;
    }

    private static List<double[]> vertices(GeneralPath p) {
        List<double[]> out = new ArrayList<double[]>();
        PathIterator it = p.getPathIterator();
        float[] c = new float[6];
        double lx = 0;
        double ly = 0;
        double sx = 0;
        double sy = 0;
        while (!it.isDone()) {
            int t = it.currentSegment(c);
            double x;
            double y;
            double inX;
            double inY;
            if (t == PathIterator.SEG_MOVETO) {
                x = c[0];
                y = c[1];
                sx = x;
                sy = y;
                out.add(new double[]{x, y, 0});
                lx = x;
                ly = y;
                it.next();
                continue;
            } else if (t == PathIterator.SEG_LINETO) {
                x = c[0];
                y = c[1];
                inX = lx;
                inY = ly;
            } else if (t == PathIterator.SEG_QUADTO) {
                x = c[2];
                y = c[3];
                inX = c[0];
                inY = c[1];
            } else if (t == PathIterator.SEG_CUBICTO) {
                x = c[4];
                y = c[5];
                inX = c[2];
                inY = c[3];
            } else {
                x = sx;
                y = sy;
                inX = lx;
                inY = ly;
            }
            double ang = MathUtil.atan2(y - inY, x - inX);
            if (out.size() == 1) {
                out.get(0)[2] = MathUtil.atan2((t == PathIterator.SEG_CUBICTO || t == PathIterator.SEG_QUADTO ? c[1] : y) - ly,
                        (t == PathIterator.SEG_CUBICTO || t == PathIterator.SEG_QUADTO ? c[0] : x) - lx);
            } else if (out.size() > 1) {
                double[] prev = out.get(out.size() - 1);
                prev[2] = bisect(prev[2], MathUtil.atan2(y - prev[1], x - prev[0]));
            }
            out.add(new double[]{x, y, ang});
            lx = x;
            ly = y;
            it.next();
        }
        return out;
    }

    private static double bisect(double a, double b) {
        double d = b - a;
        d -= Math.PI * 2 * Math.floor((d + Math.PI) / (Math.PI * 2));
        return a + d / 2;
    }

    static BlendMode blendMode(String m) {
        String[] names = {"multiply", "screen", "overlay", "darken", "lighten", "color-dodge", "color-burn",
            "hard-light", "soft-light", "difference", "exclusion", "hue", "saturation", "color", "luminosity"};
        BlendMode[] modes = {BlendMode.MULTIPLY, BlendMode.SCREEN, BlendMode.OVERLAY, BlendMode.DARKEN,
            BlendMode.LIGHTEN, BlendMode.COLOR_DODGE, BlendMode.COLOR_BURN, BlendMode.HARD_LIGHT,
            BlendMode.SOFT_LIGHT, BlendMode.DIFFERENCE, BlendMode.EXCLUSION, BlendMode.HUE,
            BlendMode.SATURATION, BlendMode.COLOR, BlendMode.LUMINOSITY};
        for (int i = 0; i < names.length; i++) {
            if (names[i].equalsIgnoreCase(m.trim())) {
                return modes[i];
            }
        }
        return BlendMode.NORMAL;
    }

    static String ref(String v) {
        if (v == null) {
            return null;
        }
        v = v.trim();
        if (!v.startsWith("url(")) {
            return null;
        }
        int close = v.indexOf(')');
        if (close < 0) {
            return null;
        }
        String in = v.substring(4, close).trim();
        if (in.length() > 1 && (in.charAt(0) == '\'' || in.charAt(0) == '"')) {
            in = in.substring(1, in.length() - 1);
        }
        int hash = in.indexOf('#');
        return hash < 0 ? null : in.substring(hash + 1);
    }

    static double number(String v, double def) {
        if (v == null) {
            return def;
        }
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    static double fraction(String v, double def) {
        if (v == null) {
            return def;
        }
        v = v.trim();
        if (v.endsWith("%")) {
            return number(v.substring(0, v.length() - 1), def * 100) / 100;
        }
        return number(v, def);
    }

    /// A length in user units; axis 1 is x, 2 is y, 0 is the normalized diagonal, for
    /// percentages.
    double length(String v, double def, int axis) {
        if (v == null) {
            return def;
        }
        v = v.trim();
        if (v.length() == 0 || "auto".equals(v)) {
            return def;
        }
        double mult = 1;
        String num = v;
        if (v.endsWith("%")) {
            double ref = axis == 1 ? viewportW : axis == 2 ? viewportH
                    : Math.sqrt((viewportW * viewportW + viewportH * viewportH) / 2);
            return number(v.substring(0, v.length() - 1), def) * ref / 100;
        }
        String[] units = {"px", "pt", "pc", "mm", "cm", "in", "em", "ex", "Q"};
        double[] f = {1, 4.0 / 3, 16, 96 / 25.4, 96 / 2.54, 96, 16, 8, 96 / 101.6};
        for (int i = 0; i < units.length; i++) {
            if (v.endsWith(units[i])) {
                num = v.substring(0, v.length() - units[i].length());
                mult = f[i];
                break;
            }
        }
        return number(num, def / mult) * mult;
    }

    static double[] numbers(String v) {
        if (v == null) {
            return new double[0];
        }
        List<Double> out = new ArrayList<Double>();
        int i = 0;
        int n = v.length();
        while (i < n) {
            char c = v.charAt(i);
            if (c == ' ' || c == ',' || c == '\t' || c == '\n' || c == '\r') {
                i++;
                continue;
            }
            int st = i;
            if (c == '-' || c == '+') {
                i++;
            }
            boolean dot = false;
            while (i < n) {
                char d = v.charAt(i);
                if (d >= '0' && d <= '9') {
                    i++;
                } else if (d == '.' && !dot) {
                    dot = true;
                    i++;
                } else if ((d == 'e' || d == 'E') && i + 1 < n) {
                    i++;
                    if (v.charAt(i) == '-' || v.charAt(i) == '+') {
                        i++;
                    }
                } else {
                    break;
                }
            }
            if (i == st) {
                i++;
                continue;
            }
            try {
                out.add(Double.valueOf(v.substring(st, i)));
            } catch (NumberFormatException e) {
                return new double[0];
            }
        }
        double[] r = new double[out.size()];
        for (int k = 0; k < r.length; k++) {
            r[k] = out.get(k).doubleValue();
        }
        return r;
    }

    /// Parses an SVG `transform` attribute.
    ///
    /// #### Parameters
    ///
    /// - `v`: the attribute; null gives the identity
    public static Matrix transform(String v) {
        Matrix m = Matrix.IDENTITY;
        if (v == null) {
            return m;
        }
        int i = 0;
        while (i < v.length()) {
            int p = v.indexOf('(', i);
            if (p < 0) {
                break;
            }
            int q = v.indexOf(')', p);
            if (q < 0) {
                break;
            }
            String fn = v.substring(i, p).replace(',', ' ').trim();
            double[] a = numbers(v.substring(p + 1, q));
            Matrix t = null;
            if ("matrix".equals(fn) && a.length == 6) {
                t = new Matrix(a[0], a[1], a[2], a[3], a[4], a[5]);
            } else if ("translate".equals(fn) && a.length >= 1) {
                t = Matrix.translate(a[0], a.length > 1 ? a[1] : 0);
            } else if ("scale".equals(fn) && a.length >= 1) {
                t = Matrix.scale(a[0], a.length > 1 ? a[1] : a[0]);
            } else if ("rotate".equals(fn) && a.length >= 1) {
                t = Matrix.rotate(Math.toRadians(a[0]));
                if (a.length == 3) {
                    t = Matrix.translate(a[1], a[2]).multiply(t).multiply(Matrix.translate(-a[1], -a[2]));
                }
            } else if ("skewX".equals(fn) && a.length == 1) {
                t = Matrix.skew(Math.toRadians(a[0]), 0);
            } else if ("skewY".equals(fn) && a.length == 1) {
                t = Matrix.skew(0, Math.toRadians(a[0]));
            }
            if (t != null) {
                m = m.multiply(t);
            }
            i = q + 1;
        }
        return m;
    }

    static Matrix viewBoxTransform(double[] vb, String par, double x, double y, double w, double h) {
        if (vb == null || vb.length != 4 || vb[2] <= 0 || vb[3] <= 0) {
            return Matrix.translate(x, y);
        }
        double sx = w / vb[2];
        double sy = h / vb[3];
        String align = "xMidYMid";
        boolean slice = false;
        if (par != null) {
            String[] p = split(par.trim(), ' ');
            int k = 0;
            if (p.length > 0 && "defer".equals(p[0])) {
                k = 1;
            }
            if (p.length > k) {
                align = p[k];
            }
            if (p.length > k + 1) {
                slice = "slice".equals(p[k + 1]);
            }
        }
        if ("none".equals(align)) {
            return new Matrix(sx, 0, 0, sy, x - vb[0] * sx, y - vb[1] * sy);
        }
        double s = slice ? Math.max(sx, sy) : Math.min(sx, sy);
        double tx = x - vb[0] * s;
        double ty = y - vb[1] * s;
        double ex = w - vb[2] * s;
        double ey = h - vb[3] * s;
        if (align.indexOf("xMid") >= 0) {
            tx += ex / 2;
        } else if (align.indexOf("xMax") >= 0) {
            tx += ex;
        }
        if (align.indexOf("YMid") >= 0) {
            ty += ey / 2;
        } else if (align.indexOf("YMax") >= 0) {
            ty += ey;
        }
        return new Matrix(s, 0, 0, s, tx, ty);
    }
}
