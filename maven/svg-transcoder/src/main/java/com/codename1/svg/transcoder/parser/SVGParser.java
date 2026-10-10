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
package com.codename1.svg.transcoder.parser;

import com.codename1.svg.transcoder.animation.SMILParser;
import com.codename1.svg.transcoder.model.*;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Walks an SVG XML document with StAX and builds a {@link SVGDocument} tree.
 *
 * Element coverage:
 *   svg, g, defs
 *   rect, circle, ellipse, line, polyline, polygon, path
 *   linearGradient, radialGradient, stop
 *   animate, animateTransform, set, title, desc (last two ignored).
 *
 * Anything else is skipped silently so an unfamiliar element won't fail the
 * whole build -- the transcoder errs on the side of "render what we can".
 */
public final class SVGParser {

    /**
     * What the document's {@code <style>} elements give each element, keyed
     * by the element's position among the start tags; null without any.
     */
    private Map<Integer, String[]> sheetStyles;

    /** The reader of the pass that builds the tree. */
    private CountingReader reader;

    public SVGDocument parse(InputStream in) throws IOException {
        // Read twice: a <style> element may follow the shapes it colours,
        // so its rules are collected before the tree is built.
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int len;
        while ((len = in.read(chunk)) != -1) {
            buf.write(chunk, 0, len);
        }
        byte[] data = buf.toByteArray();
        XMLInputFactory f = XMLInputFactory.newInstance();
        // harden against XXE
        f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        f.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        try {
            XMLStreamReader first = f.createXMLStreamReader(new ByteArrayInputStream(data));
            try {
                sheetStyles = readSheetStyles(first);
            } finally {
                first.close();
            }
            reader = new CountingReader(f.createXMLStreamReader(new ByteArrayInputStream(data)));
            try {
                return parseDocument(reader);
            } finally {
                reader.close();
            }
        } catch (XMLStreamException e) {
            throw new IOException(e);
        } finally {
            sheetStyles = null;
            reader = null;
        }
    }

    /** Counts the start tags passed, which is how an element is told apart between the two passes. */
    private static final class CountingReader extends StreamReaderDelegate {
        int started;

        CountingReader(XMLStreamReader r) {
            super(r);
        }

        @Override
        public int next() throws XMLStreamException {
            int ev = super.next();
            if (ev == XMLStreamConstants.START_ELEMENT) started++;
            return ev;
        }

        @Override
        public int nextTag() throws XMLStreamException {
            int ev = super.nextTag();
            if (ev == XMLStreamConstants.START_ELEMENT) started++;
            return ev;
        }
    }

    /**
     * Collects the embedded stylesheets and matches their rules against
     * every element of the document.
     */
    private static Map<Integer, String[]> readSheetStyles(XMLStreamReader r) throws XMLStreamException {
        StyleSheet sheet = new StyleSheet();
        List<StyleSheet.Element> elements = new ArrayList<StyleSheet.Element>();
        List<StyleSheet.Element> open = new ArrayList<StyleSheet.Element>();
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.END_ELEMENT) {
                if (!open.isEmpty()) open.remove(open.size() - 1);
                continue;
            }
            if (ev != XMLStreamConstants.START_ELEMENT) continue;
            StyleSheet.Element parent = open.isEmpty() ? null : open.get(open.size() - 1);
            StyleSheet.Element el = new StyleSheet.Element(r.getLocalName(),
                    r.getAttributeValue(null, "id"), r.getAttributeValue(null, "class"), parent);
            elements.add(el);
            if (!"style".equals(el.name)) {
                open.add(el);
                continue;
            }
            String type = r.getAttributeValue(null, "type");
            boolean css = type == null || type.trim().isEmpty() || "text/css".equalsIgnoreCase(type.trim());
            StringBuilder text = new StringBuilder();
            int depth = 1;
            while (r.hasNext() && depth > 0) {
                int inner = r.next();
                if (inner == XMLStreamConstants.START_ELEMENT) {
                    depth++;
                    // Keeps the positions in step with the second pass.
                    elements.add(new StyleSheet.Element(r.getLocalName(), null, null, el));
                } else if (inner == XMLStreamConstants.END_ELEMENT) {
                    depth--;
                } else if (inner == XMLStreamConstants.CHARACTERS || inner == XMLStreamConstants.CDATA) {
                    text.append(r.getText());
                }
            }
            if (css) sheet.add(text.toString());
        }
        if (sheet.isEmpty()) return null;
        Map<Integer, String[]> out = new HashMap<Integer, String[]>();
        for (int i = 0; i < elements.size(); i++) {
            String[] d = sheet.declarationsFor(elements.get(i));
            if (d != null) out.put(Integer.valueOf(i), d);
        }
        return out;
    }

    private SVGDocument parseDocument(XMLStreamReader r) throws XMLStreamException {
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT && "svg".equals(r.getLocalName())) {
                SVGDocument doc = new SVGDocument();
                readSVGRoot(r, doc);
                readChildren(r, doc, doc);
                indexById(doc, doc);
                return doc;
            }
        }
        throw new XMLStreamException("No <svg> root found");
    }

    private void readSVGRoot(XMLStreamReader r, SVGDocument doc) {
        Map<String, String> a = attrs(r);
        applyCommon(doc, a);
        doc.setWidth(NumberParser.parseFloat(a.get("width")));
        doc.setHeight(NumberParser.parseFloat(a.get("height")));
        doc.setPreserveAspectRatio(a.get("preserveAspectRatio"));
        String vb = a.get("viewBox");
        if (vb != null) {
            NumberParser np = new NumberParser(vb);
            try {
                doc.setViewBoxX(np.nextFloat());
                doc.setViewBoxY(np.nextFloat());
                doc.setViewBoxWidth(np.nextFloat());
                doc.setViewBoxHeight(np.nextFloat());
                doc.setViewBoxDeclared(doc.getViewBoxWidth() > 0 && doc.getViewBoxHeight() > 0);
            } catch (RuntimeException e) {
                // leave defaults
            }
        }
        if (doc.getViewBoxWidth() == 0) doc.setViewBoxWidth(doc.getWidth());
        if (doc.getViewBoxHeight() == 0) doc.setViewBoxHeight(doc.getHeight());
        if (doc.getWidth() == 0) doc.setWidth(doc.getViewBoxWidth());
        if (doc.getHeight() == 0) doc.setHeight(doc.getViewBoxHeight());
    }

    private void readChildren(XMLStreamReader r, SVGGroup parent, SVGDocument doc) throws XMLStreamException {
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.END_ELEMENT) return;
            if (ev != XMLStreamConstants.START_ELEMENT) continue;

            String name = r.getLocalName();
            if ("g".equals(name)) {
                SVGGroup g = new SVGGroup();
                applyCommon(g, attrs(r));
                parent.addChild(g);
                readChildren(r, g, doc);
            } else if ("defs".equals(name)) {
                readDefs(r, doc);
            } else if ("use".equals(name)) {
                // Read for a renderer that follows references. The class the
                // transcoder generates does not draw one.
                parent.addChild(readUse(r));
                consumeUntilEnd(r);
            } else if ("symbol".equals(name)) {
                // Drawn only where a <use> names it, so it is registered and
                // kept out of the drawing.
                SVGSymbol symbol = new SVGSymbol();
                Map<String, String> sa = attrs(r);
                applyCommon(symbol, sa);
                String svb = sa.get("viewBox");
                if (svb != null) {
                    NumberParser np = new NumberParser(svb);
                    try {
                        symbol.setViewBox(np.nextFloat(), np.nextFloat(), np.nextFloat(), np.nextFloat());
                    } catch (RuntimeException e) {
                        // no view box
                    }
                }
                readChildren(r, symbol, doc);
                if (symbol.getId() != null) doc.getDefinitions().put(symbol.getId(), symbol);
                indexById(symbol, doc);
            } else if ("rect".equals(name)) {
                SVGRect rect = readRect(r);
                parent.addChild(rect);
                readNestedAnimations(r, rect);
            } else if ("circle".equals(name)) {
                SVGCircle circle = readCircle(r);
                parent.addChild(circle);
                readNestedAnimations(r, circle);
            } else if ("ellipse".equals(name)) {
                SVGEllipse el = readEllipse(r);
                parent.addChild(el);
                readNestedAnimations(r, el);
            } else if ("line".equals(name)) {
                SVGLine ln = readLine(r);
                parent.addChild(ln);
                readNestedAnimations(r, ln);
            } else if ("polyline".equals(name)) {
                SVGPolyline pl = readPolyline(r, false);
                parent.addChild(pl);
                readNestedAnimations(r, pl);
            } else if ("polygon".equals(name)) {
                SVGPolyline pg = readPolyline(r, true);
                parent.addChild(pg);
                readNestedAnimations(r, pg);
            } else if ("path".equals(name)) {
                SVGPath path = readPath(r);
                parent.addChild(path);
                readNestedAnimations(r, path);
            } else if ("text".equals(name)) {
                SVGText text = readText(r);
                parent.addChild(text);
            } else if ("linearGradient".equals(name)) {
                SVGLinearGradient lg = readLinearGradient(r);
                if (lg.getId() != null) doc.getDefinitions().put(lg.getId(), lg);
            } else if ("radialGradient".equals(name)) {
                SVGRadialGradient rg = readRadialGradient(r);
                if (rg.getId() != null) doc.getDefinitions().put(rg.getId(), rg);
            } else if ("clipPath".equals(name) || "mask".equals(name)) {
                // Many SVGs declare clipPath outside <defs>. Accept both --
                // we don't render the clipPath inline, just register it as
                // a definition for later clip-path="url(#id)" lookup.
                SVGClipPath cp = readClipPath(r, doc);
                if (cp.getId() != null) doc.getDefinitions().put(cp.getId(), cp);
            } else if ("animate".equals(name) || "animateTransform".equals(name) || "set".equals(name)) {
                SVGAnimation an = readAnimation(r, name);
                // SVG semantics: <animate*> as a sibling of shapes inside a <g>
                // animates the group itself (typically its transform). The
                // shape-nested case (<animate> inside <rect>, <circle>, etc.)
                // is handled separately by readNestedAnimations.
                parent.addAnimation(an);
                consumeUntilEnd(r);
            } else {
                skip(r);
            }
        }
    }

    /**
     * Reads a {@code <defs>} block. Gradients and clip paths are registered
     * as they are read. Everything else in it -- the shapes, groups and
     * symbols a {@code <use>} draws -- is read like any other content into a
     * group that is never part of the drawing, and registered by id.
     */
    private void readDefs(XMLStreamReader r, SVGDocument doc) throws XMLStreamException {
        SVGGroup held = new SVGGroup();
        readChildren(r, held, doc);
        indexById(held, doc);
    }

    /** Registers every element under {@code node} that has an id and is not registered yet. */
    private static void indexById(SVGNode node, SVGDocument doc) {
        if (node != doc && node.getId() != null && !doc.getDefinitions().containsKey(node.getId())) {
            doc.getDefinitions().put(node.getId(), node);
        }
        if (node instanceof SVGGroup) {
            for (SVGNode child : ((SVGGroup) node).getChildren()) {
                indexById(child, doc);
            }
        }
    }

    private SVGUse readUse(XMLStreamReader r) {
        SVGUse use = new SVGUse();
        Map<String, String> a = attrs(r);
        applyCommon(use, a);
        String href = a.get("href");
        if (href != null) {
            href = href.trim();
            use.setHref(href.startsWith("#") ? href.substring(1) : href);
        }
        use.setX(NumberParser.parseFloat(a.get("x")));
        use.setY(NumberParser.parseFloat(a.get("y")));
        use.setWidth(NumberParser.parseFloat(a.get("width")));
        use.setHeight(NumberParser.parseFloat(a.get("height")));
        return use;
    }

    private SVGClipPath readClipPath(XMLStreamReader r, SVGDocument doc) throws XMLStreamException {
        SVGClipPath cp = new SVGClipPath();
        Map<String, String> a = attrs(r);
        cp.setId(a.get("id"));
        readChildren(r, cp, doc);
        return cp;
    }

    private SVGRect readRect(XMLStreamReader r) {
        SVGRect s = new SVGRect();
        Map<String, String> a = attrs(r);
        applyCommon(s, a);
        s.setX(NumberParser.parseFloat(a.get("x")));
        s.setY(NumberParser.parseFloat(a.get("y")));
        s.setWidth(NumberParser.parseFloat(a.get("width")));
        s.setHeight(NumberParser.parseFloat(a.get("height")));
        s.setRx(NumberParser.parseFloat(a.get("rx")));
        s.setRy(NumberParser.parseFloat(a.get("ry")));
        return s;
    }

    private SVGCircle readCircle(XMLStreamReader r) {
        SVGCircle s = new SVGCircle();
        Map<String, String> a = attrs(r);
        applyCommon(s, a);
        s.setCx(NumberParser.parseFloat(a.get("cx")));
        s.setCy(NumberParser.parseFloat(a.get("cy")));
        s.setR(NumberParser.parseFloat(a.get("r")));
        return s;
    }

    private SVGEllipse readEllipse(XMLStreamReader r) {
        SVGEllipse s = new SVGEllipse();
        Map<String, String> a = attrs(r);
        applyCommon(s, a);
        s.setCx(NumberParser.parseFloat(a.get("cx")));
        s.setCy(NumberParser.parseFloat(a.get("cy")));
        s.setRx(NumberParser.parseFloat(a.get("rx")));
        s.setRy(NumberParser.parseFloat(a.get("ry")));
        return s;
    }

    private SVGLine readLine(XMLStreamReader r) {
        SVGLine s = new SVGLine();
        Map<String, String> a = attrs(r);
        applyCommon(s, a);
        s.setX1(NumberParser.parseFloat(a.get("x1")));
        s.setY1(NumberParser.parseFloat(a.get("y1")));
        s.setX2(NumberParser.parseFloat(a.get("x2")));
        s.setY2(NumberParser.parseFloat(a.get("y2")));
        return s;
    }

    private SVGPolyline readPolyline(XMLStreamReader r, boolean closed) {
        SVGPolyline s = closed ? new SVGPolygon() : new SVGPolyline();
        Map<String, String> a = attrs(r);
        applyCommon(s, a);
        String pts = a.get("points");
        if (pts != null) {
            NumberParser np = new NumberParser(pts);
            List<Float> list = new ArrayList<Float>();
            while (np.hasMore()) list.add(np.nextFloat());
            float[] arr = new float[list.size()];
            for (int i = 0; i < arr.length; i++) arr[i] = list.get(i);
            s.setPoints(arr);
        }
        return s;
    }

    private SVGText readText(XMLStreamReader r) throws XMLStreamException {
        SVGText t = new SVGText();
        Map<String, String> a = attrs(r);
        applyCommon(t, a);
        t.setX(NumberParser.parseFloat(a.get("x")));
        t.setY(NumberParser.parseFloat(a.get("y")));
        String anchor = mergedValue(a, "text-anchor");
        if (anchor != null) {
            String norm = anchor.trim().toLowerCase();
            if ("middle".equals(norm)) t.setAnchor(SVGText.Anchor.MIDDLE);
            else if ("end".equals(norm)) t.setAnchor(SVGText.Anchor.END);
        }
        String family = mergedValue(a, "font-family");
        if (family != null) {
            t.setFontFamily(family.trim());
        }
        String size = mergedValue(a, "font-size");
        if (size != null) {
            try { t.setFontSize(NumberParser.parseFloat(size)); } catch (RuntimeException ignored) { /* default 0 */ }
        }
        String weight = mergedValue(a, "font-weight");
        if (weight != null) {
            String w = weight.trim().toLowerCase();
            t.setBold("bold".equals(w) || "bolder".equals(w) || isNumericGte(w, 600));
        }
        String style = mergedValue(a, "font-style");
        if (style != null) {
            String s = style.trim().toLowerCase();
            t.setItalic("italic".equals(s) || "oblique".equals(s));
        }
        // Collect character data; flatten any <tspan> by recursing into its text.
        StringBuilder content = new StringBuilder();
        readTextContent(r, content);
        t.setContent(content.toString());
        return t;
    }

    private void readTextContent(XMLStreamReader r, StringBuilder out) throws XMLStreamException {
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.END_ELEMENT) {
                return;
            }
            if (ev == XMLStreamConstants.CHARACTERS || ev == XMLStreamConstants.CDATA) {
                out.append(r.getText());
            } else if (ev == XMLStreamConstants.START_ELEMENT) {
                // tspan / textPath / etc. -- flatten the textual content.
                readTextContent(r, out);
            }
        }
    }

    private static boolean isNumericGte(String s, int threshold) {
        try {
            return Integer.parseInt(s) >= threshold;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    private SVGPath readPath(XMLStreamReader r) {
        SVGPath p = new SVGPath();
        Map<String, String> a = attrs(r);
        applyCommon(p, a);
        p.setCommands(PathDataParser.parse(a.get("d")));
        return p;
    }

    private SVGLinearGradient readLinearGradient(XMLStreamReader r) throws XMLStreamException {
        SVGLinearGradient g = new SVGLinearGradient();
        Map<String, String> a = attrs(r);
        g.setId(a.get("id"));
        if (a.containsKey("x1")) g.setX1(parseGradCoord(a.get("x1")));
        if (a.containsKey("y1")) g.setY1(parseGradCoord(a.get("y1")));
        if (a.containsKey("x2")) g.setX2(parseGradCoord(a.get("x2")));
        if (a.containsKey("y2")) g.setY2(parseGradCoord(a.get("y2")));
        if ("userSpaceOnUse".equals(a.get("gradientUnits"))) g.setUserSpace(true);
        String href = a.get("href");
        if (href == null) href = a.get("xlink:href");
        if (href != null && href.startsWith("#")) g.setHref(href.substring(1));
        readGradientStops(r, g.getStops());
        return g;
    }

    private SVGRadialGradient readRadialGradient(XMLStreamReader r) throws XMLStreamException {
        SVGRadialGradient g = new SVGRadialGradient();
        Map<String, String> a = attrs(r);
        g.setId(a.get("id"));
        if (a.containsKey("cx")) g.setCx(parseGradCoord(a.get("cx")));
        if (a.containsKey("cy")) g.setCy(parseGradCoord(a.get("cy")));
        if (a.containsKey("r")) g.setR(parseGradCoord(a.get("r")));
        if ("userSpaceOnUse".equals(a.get("gradientUnits"))) g.setUserSpace(true);
        String href = a.get("href");
        if (href == null) href = a.get("xlink:href");
        if (href != null && href.startsWith("#")) g.setHref(href.substring(1));
        readGradientStops(r, g.getStops());
        return g;
    }

    private float parseGradCoord(String s) {
        if (s == null) return 0f;
        String v = s.trim();
        if (v.endsWith("%")) {
            return Float.parseFloat(v.substring(0, v.length() - 1)) / 100f;
        }
        return NumberParser.parseFloat(v);
    }

    private void readGradientStops(XMLStreamReader r, List<SVGGradientStop> stops) throws XMLStreamException {
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.END_ELEMENT) return;
            if (ev != XMLStreamConstants.START_ELEMENT) continue;
            if (!"stop".equals(r.getLocalName())) { skip(r); continue; }
            Map<String, String> a = attrs(r);
            SVGGradientStop stop = new SVGGradientStop();
            stop.setOffset(parseGradCoord(a.get("offset")));
            SVGStyle s = StyleParser.parse(presentationFor(a, "stop-color", "stop-opacity"), a.get("style"));
            // stop-color is held as fill in our merged map. Use directly:
            String sc = mergedValue(a, "stop-color");
            if (sc != null && !ColorParser.isNone(sc)) {
                try {
                    stop.setColor(ColorParser.parse(sc));
                } catch (RuntimeException e) {
                    stop.setColor(ColorParser.BLACK);
                }
            } else if (s.getFill() != null && !s.getFill().isNone() && !s.getFill().isReference()) {
                stop.setColor(s.getFill().getColor());
            } else {
                stop.setColor(ColorParser.BLACK);
            }
            String so = mergedValue(a, "stop-opacity");
            if (so != null) {
                try { stop.setOpacity(NumberParser.parseFloat(so)); } catch (RuntimeException e) { /* keep default */ }
            } else if (s.getFillOpacity() != null) {
                stop.setOpacity(s.getFillOpacity());
            }
            stops.add(stop);
            consumeUntilEnd(r);
        }
    }

    /** A property from the element's style, where the last declaration wins, or else from its attribute. */
    private String mergedValue(Map<String, String> attrs, String key) {
        String style = attrs.get("style");
        String found = null;
        if (style != null) {
            for (String decl : style.split(";")) {
                int colon = decl.indexOf(':');
                if (colon <= 0) continue;
                if (decl.substring(0, colon).trim().equals(key)) {
                    found = decl.substring(colon + 1).trim();
                }
            }
        }
        return found != null ? found : attrs.get(key);
    }

    private Map<String, String> presentationFor(Map<String, String> attrs, String... keys) {
        Map<String, String> out = new HashMap<String, String>();
        for (String k : keys) {
            if (attrs.containsKey(k)) out.put(k.startsWith("stop-") ? "fill" : k, attrs.get(k));
        }
        // map stop-color -> fill, stop-opacity -> fill-opacity for reuse with StyleParser
        if (attrs.containsKey("stop-color")) out.put("fill", attrs.get("stop-color"));
        if (attrs.containsKey("stop-opacity")) out.put("fill-opacity", attrs.get("stop-opacity"));
        return out;
    }

    private SVGAnimation readAnimation(XMLStreamReader r, String elementName) {
        SVGAnimation an = new SVGAnimation();
        Map<String, String> a = attrs(r);
        if ("animateTransform".equals(elementName)) {
            an.setKind(SVGAnimation.Kind.ANIMATE_TRANSFORM);
            an.setTransformType(SMILParser.parseTransformType(a.get("type")));
        } else if ("set".equals(elementName)) {
            an.setKind(SVGAnimation.Kind.SET);
        } else {
            an.setKind(SVGAnimation.Kind.ANIMATE);
        }
        an.setAttributeName(a.get("attributeName"));
        an.setFrom(a.get("from"));
        an.setTo(a.get("to"));
        an.setBy(a.get("by"));
        an.setValues(SMILParser.parseValues(a.get("values")));
        an.setBeginMs(SMILParser.parseClock(a.get("begin"), 0));
        an.setDurMs(SMILParser.parseClock(a.get("dur"), 0));
        an.setRepeatCount(SMILParser.parseRepeatCount(a.get("repeatCount")));
        an.setCalcMode(SMILParser.parseCalcMode(a.get("calcMode")));
        an.setFreeze("freeze".equalsIgnoreCase(a.get("fill")));
        return an;
    }

    private void applyCommon(SVGNode n, Map<String, String> a) {
        n.setId(a.get("id"));
        String tr = a.get("transform");
        if (tr != null) {
            SVGTransform t = TransformParser.parse(tr);
            if (t != null) n.setTransform(t);
        }
        Map<String, String> pres = new HashMap<String, String>();
        for (Map.Entry<String, String> e : a.entrySet()) {
            String k = e.getKey();
            // Whitelist of SVG presentation attributes we forward to
            // StyleParser. Missing `clip-path` here was why
            // clipped_badge.svg's outer rect lost its rounded clip --
            // StyleParser only sees the keys that land in `pres`, so any
            // attribute *not* listed is silently dropped even if it is a
            // well-formed presentation attribute on the element.
            if ("fill".equals(k) || "stroke".equals(k) || "fill-opacity".equals(k) || "stroke-opacity".equals(k)
                    || "opacity".equals(k) || "stroke-width".equals(k) || "stroke-linecap".equals(k)
                    || "stroke-linejoin".equals(k) || "stroke-miterlimit".equals(k)
                    || "clip-path".equals(k) || "fill-rule".equals(k)) {
                pres.put(k, e.getValue());
            }
        }
        n.setStyle(StyleParser.parse(pres, a.get("style")));
    }

    private Map<String, String> attrs(XMLStreamReader r) {
        Map<String, String> m = new HashMap<String, String>();
        int n = r.getAttributeCount();
        for (int i = 0; i < n; i++) {
            String prefix = r.getAttributePrefix(i);
            String name = r.getAttributeLocalName(i);
            String key = (prefix == null || prefix.isEmpty()) ? name : prefix + ":" + name;
            m.put(key, r.getAttributeValue(i));
            // also stash bare local name so callers can ignore namespace prefixes
            m.put(name, r.getAttributeValue(i));
        }
        // Stylesheet rules are folded into the style attribute, around the
        // element's own: every reader of a style then sees them, and a later
        // declaration overrides an earlier one.
        String[] sheet = sheetStyles == null || reader == null ? null
                : sheetStyles.get(Integer.valueOf(reader.started - 1));
        if (sheet != null) {
            String own = m.get("style");
            m.put("style", sheet[0] + (own == null ? "" : own + ";") + sheet[1]);
        }
        return m;
    }

    /** Read child elements of a shape -- currently only animation children matter. */
    private void readNestedAnimations(XMLStreamReader r, SVGNode shape) throws XMLStreamException {
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.END_ELEMENT) return;
            if (ev != XMLStreamConstants.START_ELEMENT) continue;
            String name = r.getLocalName();
            if ("animate".equals(name) || "animateTransform".equals(name) || "set".equals(name)) {
                shape.addAnimation(readAnimation(r, name));
                consumeUntilEnd(r);
            } else {
                skip(r);
            }
        }
    }

    private void consumeUntilEnd(XMLStreamReader r) throws XMLStreamException {
        int depth = 1;
        while (r.hasNext() && depth > 0) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT) depth++;
            else if (ev == XMLStreamConstants.END_ELEMENT) depth--;
        }
    }

    private void skip(XMLStreamReader r) throws XMLStreamException {
        consumeUntilEnd(r);
    }
}
