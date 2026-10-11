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

    /** The size of the viewport being read, which a percentage in it is a share of. */
    private float viewportWidth;
    private float viewportHeight;

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
        /**
         * Whether white space is kept, for each element that is open: its
         * own {@code xml:space} or, without one, that of the element it is in.
         */
        private final List<Boolean> spaces = new ArrayList<Boolean>();

        boolean preservesSpace() {
            return !spaces.isEmpty() && spaces.get(spaces.size() - 1).booleanValue();
        }

        private void track(int ev) {
            if (ev == XMLStreamConstants.START_ELEMENT) {
                started++;
                String own = getAttributeValue("http://www.w3.org/XML/1998/namespace", "space");
                spaces.add(own == null ? Boolean.valueOf(preservesSpace())
                        : Boolean.valueOf("preserve".equals(own.trim())));
            } else if (ev == XMLStreamConstants.END_ELEMENT && !spaces.isEmpty()) {
                spaces.remove(spaces.size() - 1);
            }
        }

        CountingReader(XMLStreamReader r) {
            super(r);
        }

        @Override
        public int next() throws XMLStreamException {
            int ev = super.next();
            track(ev);
            return ev;
        }

        @Override
        public int nextTag() throws XMLStreamException {
            int ev = super.nextTag();
            track(ev);
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
            // Every attribute, for a selector that tests one: [data-role="accent"].
            Map<String, String> given = new HashMap<String, String>();
            for (int i = 0; i < r.getAttributeCount(); i++) {
                given.put(r.getAttributeLocalName(i), r.getAttributeValue(i));
            }
            StyleSheet.Element el = new StyleSheet.Element(r.getLocalName(),
                    r.getAttributeValue(null, "id"), r.getAttributeValue(null, "class"), parent, given);
            elements.add(el);
            if (!"style".equals(el.name)) {
                open.add(el);
                continue;
            }
            String type = r.getAttributeValue(null, "type");
            // A sheet for another medium, print say, is not one of an image's.
            boolean css = (type == null || type.trim().isEmpty() || "text/css".equalsIgnoreCase(type.trim()))
                    && StyleSheet.mediaMatches(r.getAttributeValue(null, "media"));
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
        doc.setWidth(lengthOrZero(a.get("width")));
        doc.setHeight(lengthOrZero(a.get("height")));
        // Whether each was written as a length, for a caller that sizes the
        // drawing: one filled in below from the view box is its shape and
        // not a size anyone asked for.
        if (doc.getWidth() > 0) doc.markGiven("width");
        if (doc.getHeight() > 0) doc.markGiven("height");
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
        if (doc.isViewBoxDeclared()) {
            // One side given and the other not: the missing one follows the
            // shape of the view box.
            float ratio = doc.getViewBoxWidth() / doc.getViewBoxHeight();
            if (doc.getWidth() > 0 && doc.getHeight() == 0) doc.setHeight(doc.getWidth() / ratio);
            else if (doc.getHeight() > 0 && doc.getWidth() == 0) doc.setWidth(doc.getHeight() * ratio);
        }
        if (doc.getViewBoxWidth() == 0) doc.setViewBoxWidth(doc.getWidth());
        if (doc.getViewBoxHeight() == 0) doc.setViewBoxHeight(doc.getHeight());
        if (doc.getWidth() == 0) doc.setWidth(doc.getViewBoxWidth());
        if (doc.getHeight() == 0) doc.setHeight(doc.getViewBoxHeight());
        viewportWidth = doc.getViewBoxWidth();
        viewportHeight = doc.getViewBoxHeight();
        fontFamily = null;
        fontSize = null;
        fontBold = null;
        fontItalic = null;
        inheritedColor = null;
        enterFont(a);
    }

    /** A horizontal length: a percentage is that share of the viewport's width. */
    private float lengthX(String value) {
        return length(value, viewportWidth);
    }

    /** A vertical length: a percentage is that share of the viewport's height. */
    private float lengthY(String value) {
        return length(value, viewportHeight);
    }

    /** A length with no direction, a radius for one: a percentage is a share of the viewport's diagonal, evened out. */
    private float lengthAcross(String value) {
        return length(value, (float) Math.sqrt((viewportWidth * viewportWidth + viewportHeight * viewportHeight) / 2.0));
    }

    private static float length(String value, float whole) {
        if (value == null) return 0f;
        float v = NumberParser.parseFloat(value);
        return value.trim().endsWith("%") ? v / 100f * whole : v;
    }

    /**
     * A width or height of the root element; zero for one that is missing,
     * unreadable, or a percentage, which is a share of a viewport the file
     * does not know and so no size of its own.
     */
    private static float lengthOrZero(String value) {
        if (value == null || value.trim().endsWith("%")) return 0f;
        try {
            return NumberParser.parseFloat(value);
        } catch (RuntimeException e) {
            return 0f;
        }
    }

    /**
     * Whether the element just opened is one to draw: it asks for no
     * extension, there being none, and when it names languages one of them
     * is English, which is the language an image is taken to be drawn in.
     */
    private static boolean conditionsHold(XMLStreamReader r) {
        String extensions = r.getAttributeValue(null, "requiredExtensions");
        if (extensions != null && !extensions.trim().isEmpty()) return false;
        String languages = r.getAttributeValue(null, "systemLanguage");
        if (languages == null) return true;
        for (String language : languages.split(",")) {
            String l = language.trim();
            if (l.equalsIgnoreCase("en") || l.regionMatches(true, 0, "en-", 0, 3)) return true;
        }
        return false;
    }

    /** The font an element with none of its own takes from what it is in; null where nothing sets one. */
    private String fontFamily;
    private Float fontSize;
    private Boolean fontBold;
    private Boolean fontItalic;

    /** Takes in the font properties of a container; what is returned undoes it with {@link #leaveFont}. */
    private Object[] enterFont(Map<String, String> a) {
        Object[] saved = {fontFamily, fontSize, fontBold, fontItalic, inheritedColor};
        Integer color = colorOf(a);
        if (color != null) inheritedColor = color;
        String family = mergedValue(a, "font-family");
        if (family != null) fontFamily = family.trim();
        Float size = fontSizeOf(mergedValue(a, "font-size"));
        if (size != null) fontSize = size;
        Boolean bold = boldOf(mergedValue(a, "font-weight"));
        if (bold != null) fontBold = bold;
        Boolean italic = italicOf(mergedValue(a, "font-style"));
        if (italic != null) fontItalic = italic;
        return saved;
    }

    private void leaveFont(Object[] saved) {
        fontFamily = (String) saved[0];
        fontSize = (Float) saved[1];
        fontBold = (Boolean) saved[2];
        fontItalic = (Boolean) saved[3];
        inheritedColor = (Integer) saved[4];
    }

    /** A font size, a relative one taken against the size around it; null when there is none to read. */
    private Float fontSizeOf(String size) {
        if (size == null) return null;
        try {
            float given = NumberParser.parseFloat(size);
            String unit = size.trim();
            float around = fontSize != null ? fontSize.floatValue() : 12f;
            if (unit.endsWith("%")) return Float.valueOf(given / 100 * around);
            if (unit.endsWith("rem")) return Float.valueOf(given * 16f);
            if (unit.endsWith("em")) return Float.valueOf(given * around);
            if (unit.endsWith("ex")) return Float.valueOf(given * around / 2);
            return Float.valueOf(given);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Boolean boldOf(String weight) {
        if (weight == null) return null;
        String w = weight.trim();
        if ("inherit".equals(w)) return null;
        return Boolean.valueOf("bold".equals(w) || "bolder".equals(w) || isNumericGte(w, 600));
    }

    private static Boolean italicOf(String style) {
        if (style == null) return null;
        String s = style.trim();
        if ("inherit".equals(s)) return null;
        return Boolean.valueOf("italic".equals(s) || "oblique".equals(s));
    }

    private void readChildren(XMLStreamReader r, SVGGroup parent, SVGDocument doc) throws XMLStreamException {
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.END_ELEMENT) return;
            if (ev != XMLStreamConstants.START_ELEMENT) continue;

            String name = r.getLocalName();
            if (!conditionsHold(r)) {
                // Asks for something this is not: an extension, or another
                // language. It is as if it were not written.
                skip(r);
                continue;
            }
            // A link is a group as far as drawing goes: what is inside it
            // is drawn, and only following it is left out. So is a
            // `<switch>`, of which only the first child that can be drawn
            // is: the ones that cannot have been passed over above, or are
            // of a kind that is not read at all.
            if ("g".equals(name) || "a".equals(name) || "switch".equals(name)) {
                SVGGroup g = new SVGGroup();
                Map<String, String> ga = attrs(r);
                applyCommon(g, ga);
                parent.addChild(g);
                Object[] font = enterFont(ga);
                try {
                    readChildren(r, g, doc);
                } finally {
                    leaveFont(font);
                }
                if ("switch".equals(name)) {
                    while (g.getChildren().size() > 1) {
                        g.getChildren().remove(g.getChildren().size() - 1);
                    }
                }
            } else if ("svg".equals(name)) {
                // Read for a renderer that can lay one out. The class the
                // transcoder generates does not draw it.
                parent.addChild(readViewport(r, doc));
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
                symbol.setPreserveAspectRatio(sa.get("preserveAspectRatio"));
                String svb = sa.get("viewBox");
                if (svb != null) {
                    NumberParser np = new NumberParser(svb);
                    try {
                        symbol.setViewBox(np.nextFloat(), np.nextFloat(), np.nextFloat(), np.nextFloat());
                    } catch (RuntimeException e) {
                        // no view box
                    }
                }
                // What is inside measures its percentages against the
                // symbol's own view box, as inside a nested `<svg>`.
                float outerWidth = viewportWidth;
                float outerHeight = viewportHeight;
                if (symbol.getViewBoxWidth() > 0 && symbol.getViewBoxHeight() > 0) {
                    viewportWidth = symbol.getViewBoxWidth();
                    viewportHeight = symbol.getViewBoxHeight();
                }
                Object[] font = enterFont(sa);
                try {
                    readChildren(r, symbol, doc);
                } finally {
                    leaveFont(font);
                    viewportWidth = outerWidth;
                    viewportHeight = outerHeight;
                }
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
            } else if ("image".equals(name)) {
                // Read for a renderer that can load one. The class the
                // transcoder generates does not draw it.
                parent.addChild(readImage(r));
                consumeUntilEnd(r);
            } else if ("text".equals(name)) {
                SVGText text = readText(r);
                parent.addChild(text);
            } else if ("linearGradient".equals(name)) {
                SVGLinearGradient lg = readLinearGradient(r);
                if (lg.getId() != null) doc.getDefinitions().put(lg.getId(), lg);
            } else if ("radialGradient".equals(name)) {
                SVGRadialGradient rg = readRadialGradient(r);
                if (rg.getId() != null) doc.getDefinitions().put(rg.getId(), rg);
            } else if ("marker".equals(name)) {
                // Read for a renderer that draws them. The class the
                // transcoder generates does not.
                SVGMarker marker = readMarker(r, doc);
                if (marker.getId() != null) doc.getDefinitions().put(marker.getId(), marker);
                indexById(marker, doc);
            } else if ("filter".equals(name)) {
                SVGFilter filter = readFilter(r, doc);
                if (filter.getId() != null) doc.getDefinitions().put(filter.getId(), filter);
            } else if ("pattern".equals(name)) {
                // Read for a renderer that can tile one. The class the
                // transcoder generates does not paint with it.
                SVGPattern pattern = readPattern(r, doc);
                if (pattern.getId() != null) doc.getDefinitions().put(pattern.getId(), pattern);
                indexById(pattern, doc);
            } else if ("clipPath".equals(name) || "mask".equals(name)) {
                // Many SVGs declare clipPath outside <defs>. Accept both --
                // we don't render the clipPath inline, just register it as
                // a definition for later clip-path="url(#id)" lookup.
                SVGClipPath cp = readClipPath(r, doc, "mask".equals(name));
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
    // What `<defs>` sets for itself, a fill say, is not kept for the
    // elements in it. They are only ever drawn through a `<use>`, and an
    // element drawn that way inherits from the `<use>` and what that is in,
    // never from where it was defined: `<defs fill="red">` does not make a
    // shape under it red when it is used, in a browser either.
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
        if (node instanceof SVGViewport) {
            indexById(((SVGViewport) node).getContent(), doc);
        }
        if (node instanceof SVGGroup) {
            for (SVGNode child : ((SVGGroup) node).getChildren()) {
                indexById(child, doc);
            }
        }
    }

    private SVGViewport readViewport(XMLStreamReader r, SVGDocument doc) throws XMLStreamException {
        SVGViewport v = new SVGViewport();
        Map<String, String> a = attrs(r);
        applyCommon(v, a);
        v.setX(viewportSide(a.get("x"), 0f, viewportWidth));
        v.setY(viewportSide(a.get("y"), 0f, viewportHeight));
        // Left out, a side is the whole of the viewport it is in.
        v.setWidth(viewportSide(a.get("width"), 1f, viewportWidth));
        v.setHeight(viewportSide(a.get("height"), 1f, viewportHeight));
        v.setPreserveAspectRatio(a.get("preserveAspectRatio"));
        String overflow = mergedValue(a, "overflow");
        v.setOverflowVisible(overflow != null && ("visible".equals(overflow.trim()) || "auto".equals(overflow.trim())));
        String vb = a.get("viewBox");
        if (vb != null) {
            NumberParser np = new NumberParser(vb);
            try {
                v.setViewBox(np.nextFloat(), np.nextFloat(), np.nextFloat(), np.nextFloat());
            } catch (RuntimeException e) {
                // no view box
            }
        }
        // What is inside measures its percentages against this one.
        float outerWidth = viewportWidth;
        float outerHeight = viewportHeight;
        viewportWidth = v.getViewBoxWidth() > 0 ? v.getViewBoxWidth() : v.getWidth();
        viewportHeight = v.getViewBoxHeight() > 0 ? v.getViewBoxHeight() : v.getHeight();
        Object[] font = enterFont(a);
        try {
            readChildren(r, v.getContent(), doc);
        } finally {
            leaveFont(font);
            viewportWidth = outerWidth;
            viewportHeight = outerHeight;
        }
        return v;
    }

    /** A side of a nested viewport: a length, or a share of the enclosing viewport's when it is a percentage or left out. */
    private float viewportSide(String value, float share, float whole) {
        if (value == null) return share * whole;
        try {
            float n = parseGradCoord(value);
            return value.trim().endsWith("%") ? n * whole : n;
        } catch (RuntimeException e) {
            return share * whole;
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
        use.setX(lengthX(a.get("x")));
        use.setY(lengthY(a.get("y")));
        use.setWidth(lengthX(a.get("width")));
        use.setHeight(lengthY(a.get("height")));
        return use;
    }

    /** A side of a picture's box; zero, for the picture's own size, when it is left out or cannot be read. */
    private static float imageSide(String value, float whole) {
        if (value == null) return 0f;
        try {
            return length(value, whole);
        } catch (RuntimeException e) {
            return 0f;
        }
    }

    private SVGImage readImage(XMLStreamReader r) {
        SVGImage image = new SVGImage();
        Map<String, String> a = attrs(r);
        applyCommon(image, a);
        String href = a.get("href");
        if (href != null) image.setHref(href.trim());
        image.setX(lengthX(a.get("x")));
        image.setY(lengthY(a.get("y")));
        image.setWidth(imageSide(a.get("width"), viewportWidth));
        image.setHeight(imageSide(a.get("height"), viewportHeight));
        image.setPreserveAspectRatio(a.get("preserveAspectRatio"));
        return image;
    }

    // A percentage in the region of a mask, a filter or a pattern is a share
    // of the viewport the definition is written in: the nested `<svg>` it is
    // inside, when it is inside one, and not the whole drawing.
    private SVGClipPath readClipPath(XMLStreamReader r, SVGDocument doc, boolean mask) throws XMLStreamException {
        SVGClipPath cp = new SVGClipPath();
        Map<String, String> a = attrs(r);
        // What it sets is what its children inherit: a clip-rule for a
        // clip path, a fill for a mask. A clip path may also be transformed
        // as a whole, which moves the region it cuts; a mask takes no
        // transform.
        applyCommon(cp, a);
        if (mask) cp.setTransform(null);
        cp.setMask(mask);
        cp.setAlphaMask(mask && "alpha".equals(mergedValue(a, "mask-type")));
        cp.setObjectBoundingBox("objectBoundingBox".equals(a.get(mask ? "maskContentUnits" : "clipPathUnits")));
        if (mask) {
            boolean user = "userSpaceOnUse".equals(a.get("maskUnits"));
            cp.setRegionUserSpace(user);
            cp.setRegion(regionSide(a.get("x"), -0.1f, user, viewportWidth),
                    regionSide(a.get("y"), -0.1f, user, viewportHeight),
                    regionSide(a.get("width"), 1.2f, user, viewportWidth),
                    regionSide(a.get("height"), 1.2f, user, viewportHeight));
        }
        // What is drawn in fractions of the box around the element measures
        // its percentages against that box, which is one by one: 50% is a
        // half, to be scaled with the rest, not half the drawing's width.
        float outerWidth = viewportWidth;
        float outerHeight = viewportHeight;
        if (cp.isObjectBoundingBox()) {
            viewportWidth = 1f;
            viewportHeight = 1f;
        }
        try {
            readChildren(r, cp, doc);
        } finally {
            viewportWidth = outerWidth;
            viewportHeight = outerHeight;
        }
        return cp;
    }

    /**
     * One side of a mask's region. Left out it is the fraction given, which
     * in the masked element's own coordinates is that share of the drawing;
     * a percentage is a share of the drawing there too, and of the bounding
     * box otherwise.
     */
    private float regionSide(String value, float fraction, boolean userSpace, float whole) {
        if (value == null) return userSpace ? fraction * whole : fraction;
        try {
            float v = parseGradCoord(value);
            return userSpace && value.trim().endsWith("%") ? v * whole : v;
        } catch (RuntimeException e) {
            return userSpace ? fraction * whole : fraction;
        }
    }

    private SVGMarker readMarker(XMLStreamReader r, SVGDocument doc) throws XMLStreamException {
        SVGMarker m = new SVGMarker();
        Map<String, String> a = attrs(r);
        applyCommon(m, a);
        m.setTransform(null);
        if (a.containsKey("markerWidth")) m.setMarkerWidth(lengthX(a.get("markerWidth")));
        if (a.containsKey("markerHeight")) m.setMarkerHeight(lengthY(a.get("markerHeight")));
        m.setRefX(markerRef(a.get("refX")));
        m.setRefY(markerRef(a.get("refY")));
        m.setOrient(a.get("orient") == null ? null : a.get("orient").trim());
        m.setUserSpace("userSpaceOnUse".equals(a.get("markerUnits")));
        m.setPreserveAspectRatio(a.get("preserveAspectRatio"));
        String overflow = mergedValue(a, "overflow");
        m.setOverflowVisible(overflow != null && ("visible".equals(overflow.trim()) || "auto".equals(overflow.trim())));
        String vb = a.get("viewBox");
        if (vb != null) {
            NumberParser np = new NumberParser(vb);
            try {
                m.setViewBox(np.nextFloat(), np.nextFloat(), np.nextFloat(), np.nextFloat());
            } catch (RuntimeException e) {
                // no view box
            }
        }
        readChildren(r, m, doc);
        return m;
    }

    private static float markerRef(String value) {
        try {
            return NumberParser.parseFloat(value);
        } catch (RuntimeException e) {
            // A keyword (left, center, ...): the corner, as when left out.
            return 0f;
        }
    }

    private SVGFilter readFilter(XMLStreamReader r, SVGDocument doc) throws XMLStreamException {
        SVGFilter f = new SVGFilter();
        Map<String, String> a = attrs(r);
        f.setId(a.get("id"));
        boolean user = "userSpaceOnUse".equals(a.get("filterUnits"));
        f.setRegionUserSpace(user);
        f.setRegion(regionSide(a.get("x"), -0.1f, user, viewportWidth),
                regionSide(a.get("y"), -0.1f, user, viewportHeight),
                regionSide(a.get("width"), 1.2f, user, viewportWidth),
                regionSide(a.get("height"), 1.2f, user, viewportHeight));
        f.setPrimitiveObjectBoundingBox("objectBoundingBox".equals(a.get("primitiveUnits")));
        String space = mergedValue(a, "color-interpolation-filters");
        f.setSrgb(space != null && "sRGB".equalsIgnoreCase(space.trim()));
        readPrimitives(r, f.getPrimitives());
        // A percentage in the area of an effect is a share of the viewport
        // the filter is written in, or of the element's box when the
        // effects are measured in that. It is worked out here, where the
        // viewport is known, into the number it stands for.
        for (SVGFilter.Primitive p : f.getPrimitives()) {
            resolveShare(p, "x", viewportWidth, f.isPrimitiveObjectBoundingBox());
            resolveShare(p, "width", viewportWidth, f.isPrimitiveObjectBoundingBox());
            resolveShare(p, "y", viewportHeight, f.isPrimitiveObjectBoundingBox());
            resolveShare(p, "height", viewportHeight, f.isPrimitiveObjectBoundingBox());
        }
        return f;
    }

    private static void resolveShare(SVGFilter.Primitive p, String attribute, float whole, boolean box) {
        String value = p.get(attribute);
        if (value == null || !value.trim().endsWith("%")) return;
        try {
            float share = Float.parseFloat(value.trim().substring(0, value.trim().length() - 1)) / 100f;
            p.set(attribute, String.valueOf(box ? share : share * whole));
        } catch (RuntimeException e) {
            // Not a number: left for the reader of it to pass over.
        }
    }

    private void readPrimitives(XMLStreamReader r, List<SVGFilter.Primitive> into) throws XMLStreamException {
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.END_ELEMENT) return;
            if (ev != XMLStreamConstants.START_ELEMENT) continue;
            String name = r.getLocalName();
            Map<String, String> a = attrs(r);
            // A property written in the style is the property: it is
            // looked up like an attribute and wins over one.
            String style = a.get("style");
            if (style != null) {
                for (String decl : style.split(";")) {
                    int colon = decl.indexOf(':');
                    if (colon > 0) a.put(decl.substring(0, colon).trim(), decl.substring(colon + 1).trim());
                }
            }
            SVGFilter.Primitive p = new SVGFilter.Primitive(name, a);
            into.add(p);
            readPrimitives(r, p.getChildren());
        }
    }

    private SVGPattern readPattern(XMLStreamReader r, SVGDocument doc) throws XMLStreamException {
        SVGPattern p = new SVGPattern();
        Map<String, String> a = attrs(r);
        applyCommon(p, a);
        // The pattern's transform is its patternTransform, not one to draw
        // its content through a second time.
        p.setTransform(null);
        boolean user = "userSpaceOnUse".equals(a.get("patternUnits"));
        if (a.containsKey("patternUnits")) { p.markGiven("patternUnits"); p.setUserSpace(user); }
        if (a.containsKey("x")) { p.markGiven("x"); p.setX(patternSide(a.get("x"), user, viewportWidth)); }
        if (a.containsKey("y")) { p.markGiven("y"); p.setY(patternSide(a.get("y"), user, viewportHeight)); }
        if (a.containsKey("width")) {
            p.markGiven("width");
            p.setWidth(patternSide(a.get("width"), user, viewportWidth));
        }
        if (a.containsKey("height")) {
            p.markGiven("height");
            p.setHeight(patternSide(a.get("height"), user, viewportHeight));
        }
        if (a.containsKey("patternContentUnits")) {
            p.markGiven("patternContentUnits");
            p.setContentObjectBoundingBox("objectBoundingBox".equals(a.get("patternContentUnits")));
        }
        if (a.get("patternTransform") != null) {
            p.markGiven("patternTransform");
            p.setPatternTransform(TransformParser.parse(a.get("patternTransform")));
        }
        String vb = a.get("viewBox");
        if (vb != null) {
            NumberParser np = new NumberParser(vb);
            try {
                p.setViewBox(np.nextFloat(), np.nextFloat(), np.nextFloat(), np.nextFloat());
                p.markGiven("viewBox");
            } catch (RuntimeException e) {
                // no view box
            }
        }
        if (a.containsKey("preserveAspectRatio")) {
            p.markGiven("preserveAspectRatio");
            p.setPreserveAspectRatio(a.get("preserveAspectRatio"));
        }
        String href = a.get("href");
        if (href != null && href.trim().startsWith("#")) p.setHref(href.trim().substring(1));
        // As for a clip path: content in fractions of the box, or in a
        // view box of its own, measures its percentages against that.
        float outerWidth = viewportWidth;
        float outerHeight = viewportHeight;
        if (p.getViewBoxWidth() > 0 && p.getViewBoxHeight() > 0) {
            viewportWidth = p.getViewBoxWidth();
            viewportHeight = p.getViewBoxHeight();
        } else if (p.isContentObjectBoundingBox()) {
            viewportWidth = 1f;
            viewportHeight = 1f;
        }
        try {
            readChildren(r, p, doc);
        } finally {
            viewportWidth = outerWidth;
            viewportHeight = outerHeight;
        }
        return p;
    }

    private float patternSide(String value, boolean userSpace, float whole) {
        try {
            float v = parseGradCoord(value);
            return userSpace && value.trim().endsWith("%") ? v * whole : v;
        } catch (RuntimeException e) {
            return 0f;
        }
    }

    private SVGRect readRect(XMLStreamReader r) {
        SVGRect s = new SVGRect();
        Map<String, String> a = attrs(r);
        applyCommon(s, a);
        s.setX(lengthX(a.get("x")));
        s.setY(lengthY(a.get("y")));
        s.setWidth(lengthX(a.get("width")));
        s.setHeight(lengthY(a.get("height")));
        s.setRx(lengthX(a.get("rx")));
        s.setRy(lengthY(a.get("ry")));
        return s;
    }

    private SVGCircle readCircle(XMLStreamReader r) {
        SVGCircle s = new SVGCircle();
        Map<String, String> a = attrs(r);
        applyCommon(s, a);
        s.setCx(lengthX(a.get("cx")));
        s.setCy(lengthY(a.get("cy")));
        s.setR(lengthAcross(a.get("r")));
        return s;
    }

    private SVGEllipse readEllipse(XMLStreamReader r) {
        SVGEllipse s = new SVGEllipse();
        Map<String, String> a = attrs(r);
        applyCommon(s, a);
        s.setCx(lengthX(a.get("cx")));
        s.setCy(lengthY(a.get("cy")));
        s.setRx(lengthX(a.get("rx")));
        s.setRy(lengthY(a.get("ry")));
        return s;
    }

    private SVGLine readLine(XMLStreamReader r) {
        SVGLine s = new SVGLine();
        Map<String, String> a = attrs(r);
        applyCommon(s, a);
        s.setX1(lengthX(a.get("x1")));
        s.setY1(lengthY(a.get("y1")));
        s.setX2(lengthX(a.get("x2")));
        s.setY2(lengthY(a.get("y2")));
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
        // Each may be a list, one for each character: the first is where
        // the text starts and the rest are kept for the ones after it.
        glyphLists = new float[][] {lengths(a.get("x"), true), lengths(a.get("y"), false),
            lengths(a.get("dx"), true), lengths(a.get("dy"), false)};
        glyphIndex = 0;
        t.setX(glyphLists[0].length > 0 ? glyphLists[0][0] : 0f);
        t.setY(glyphLists[1].length > 0 ? glyphLists[1][0] : 0f);
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
        // What the text does not say of its font, it has from the group,
        // the nested drawing or the root it is in: `<g font-size="24">`
        // sizes every text inside it.
        if (family == null && fontFamily != null) {
            t.setFontFamily(fontFamily);
        }
        Float sized = fontSizeOf(mergedValue(a, "font-size"));
        if (sized == null) sized = fontSize;
        if (sized != null) {
            t.setFontSize(sized.floatValue());
        }
        if (mergedValue(a, "font-weight") == null && fontBold != null) {
            t.setBold(fontBold.booleanValue());
        }
        if (mergedValue(a, "font-style") == null && fontItalic != null) {
            t.setItalic(fontItalic.booleanValue());
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
        // Collect character data. The content is every <tspan> flattened
        // into one string; the runs keep them apart, each with what it sets.
        StringBuilder content = new StringBuilder();
        textSpaceBefore = true;
        readTextContent(r, content, t, null);
        t.setContent(content.toString());
        List<SVGText.Run> runs = t.getRuns();
        if (!runs.isEmpty() && !textSpaceKept) {
            SVGText.Run last = runs.get(runs.size() - 1);
            if (last.getText().endsWith(" ")) {
                last.setText(last.getText().substring(0, last.getText().length() - 1));
            }
        }
        return t;
    }

    /** Whether the text read so far ends in white space, or has not begun: more of it is then dropped. */
    private boolean textSpaceBefore;
    /** Whether the text last read was under {@code xml:space="preserve"}, which keeps a space at its end. */
    private boolean textSpaceKept;
    /**
     * The x, y, dx and dy lists of the text or span being read, which place
     * its characters one by one, and how many of its characters have been
     * read. Only what comes after the first of each list is taken from
     * here: the first is where the text or the span itself starts.
     */
    private float[][] glyphLists = new float[4][0];
    private int glyphIndex;

    /** A list of lengths; empty when there is none or it cannot be read. */
    private float[] lengths(String value, boolean horizontal) {
        if (value == null || value.trim().isEmpty()) return new float[0];
        String[] parts = value.trim().split("[\\s,]+");
        float[] out = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = horizontal ? lengthX(parts[i]) : lengthY(parts[i]);
            } catch (RuntimeException e) {
                return java.util.Arrays.copyOf(out, i);
            }
        }
        return out;
    }

    /** Whether the span being read has still to hand its position to a run. */
    private boolean spanUnplaced;

    private void readTextContent(XMLStreamReader r, StringBuilder out, SVGText text, SVGText.Run span)
            throws XMLStreamException {
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.END_ELEMENT) {
                return;
            }
            if (ev == XMLStreamConstants.CHARACTERS || ev == XMLStreamConstants.CDATA) {
                String raw = r.getText();
                out.append(raw);
                // White space as a browser shows it: any run of it one
                // space, and none at the start of the text. Under
                // xml:space="preserve", its own or an ancestor's, every
                // character of it is kept instead, as a space each: text
                // lined up with spaces stays lined up.
                boolean kept = reader != null && reader.preservesSpace();
                textSpaceKept = kept;
                StringBuilder shown = new StringBuilder();
                for (int i = 0; i < raw.length(); i++) {
                    char c = raw.charAt(i);
                    boolean space = c == ' ' || c == '\t' || c == '\n' || c == '\r';
                    if (space && textSpaceBefore && !kept) continue;
                    shown.append(space ? ' ' : c);
                    textSpaceBefore = space;
                }
                if (shown.length() == 0) continue;
                // Characters a list places are each a run of their own,
                // begun where the list says; the rest follow the last. The
                // first character is not one of them: it is where the text
                // or the span starts, which is placed already.
                int listed = 0;
                for (float[] list : glyphLists) listed = Math.max(listed, list.length);
                int from = glyphIndex;
                glyphIndex += shown.length();
                int head = 0;
                while (head < shown.length() && (from + head < 1 || from + head >= listed)) head++;
                String rest = shown.substring(head);
                shown.setLength(head);
                SVGText.Run run = new SVGText.Run();
                run.setText(shown.toString());
                if (span != null) {
                    run.setStyle(span.getStyle());
                    run.setFontFamily(span.getFontFamily());
                    run.setFontSize(span.getFontSize());
                    run.setBold(span.getBold());
                    run.setItalic(span.getItalic());
                    run.setPathRef(span.getPathRef());
                    run.setStartOffset(span.getStartOffset(), span.isStartOffsetShare());
                    if (spanUnplaced) {
                        // A span is moved once, where its text begins.
                        run.setX(span.getX());
                        run.setY(span.getY());
                        run.setDx(span.getDx());
                        run.setDy(span.getDy());
                        spanUnplaced = false;
                    }
                }
                if (shown.length() > 0) text.getRuns().add(run);
                SVGText.Run placed = null;
                for (int i = 0; i < rest.length(); i++) {
                    int at = from + head + i;
                    if (at < listed || placed == null) {
                        placed = new SVGText.Run();
                        placed.setStyle(run.getStyle());
                        placed.setFontFamily(run.getFontFamily());
                        placed.setFontSize(run.getFontSize());
                        placed.setBold(run.getBold());
                        placed.setItalic(run.getItalic());
                        placed.setPathRef(run.getPathRef());
                        placed.setStartOffset(run.getStartOffset(), run.isStartOffsetShare());
                        if (at < glyphLists[0].length) placed.setX(Float.valueOf(glyphLists[0][at]));
                        if (at < glyphLists[1].length) placed.setY(Float.valueOf(glyphLists[1][at]));
                        if (at < glyphLists[2].length) placed.setDx(glyphLists[2][at]);
                        if (at < glyphLists[3].length) placed.setDy(glyphLists[3][at]);
                        text.getRuns().add(placed);
                    }
                    placed.setText(placed.getText() + rest.charAt(i));
                }
            } else if (ev == XMLStreamConstants.START_ELEMENT) {
                // tspan / textPath / etc.
                boolean onPath = "textPath".equals(r.getLocalName());
                Map<String, String> a = attrs(r);
                SVGText.Run child = new SVGText.Run();
                if (span != null) {
                    child.setPathRef(span.getPathRef());
                    child.setStartOffset(span.getStartOffset(), span.isStartOffsetShare());
                }
                if (onPath) {
                    // Laid along the path it names, from as far along it as
                    // it says.
                    String href = a.get("href");
                    child.setPathRef(href != null && href.trim().startsWith("#") ? href.trim().substring(1) : "");
                    String offset = a.get("startOffset");
                    float from = 0f;
                    boolean share = offset != null && offset.trim().endsWith("%");
                    if (offset != null) {
                        try {
                            from = NumberParser.parseFloat(offset);
                            if (share) from /= 100f;
                        } catch (RuntimeException e) {
                            from = 0f;
                        }
                    }
                    child.setStartOffset(from, share);
                }
                SVGNode styled = new SVGGroup();
                applyCommon(styled, a);
                child.setStyle(styled.getStyle());
                if (span != null) {
                    if (span.getStyle() != null) child.getStyle().inherit(span.getStyle());
                    child.setFontFamily(span.getFontFamily());
                    child.setFontSize(span.getFontSize());
                    child.setBold(span.getBold());
                    child.setItalic(span.getItalic());
                    if (spanUnplaced) {
                        // The enclosing span has shown no text yet, so what
                        // it was moved by moves this one.
                        child.setX(span.getX());
                        child.setY(span.getY());
                        child.setDx(span.getDx());
                        child.setDy(span.getDy());
                    }
                }
                if (a.get("x") != null) child.setX(firstLength(a.get("x"), true));
                if (a.get("y") != null) child.setY(firstLength(a.get("y"), false));
                if (a.get("x") != null || a.get("y") != null || a.get("dx") != null || a.get("dy") != null) {
                    // A span that places itself places its characters by
                    // its own lists from here on.
                    glyphLists = new float[][] {lengths(a.get("x"), true), lengths(a.get("y"), false),
                        lengths(a.get("dx"), true), lengths(a.get("dy"), false)};
                    glyphIndex = 0;
                }
                Float dx = a.get("dx") == null ? null : firstLength(a.get("dx"), true);
                Float dy = a.get("dy") == null ? null : firstLength(a.get("dy"), false);
                if (dx != null) child.setDx(child.getDx() + dx.floatValue());
                if (dy != null) child.setDy(child.getDy() + dy.floatValue());
                String family = mergedValue(a, "font-family");
                if (family != null) child.setFontFamily(family.trim());
                String size = mergedValue(a, "font-size");
                if (size != null) {
                    try {
                        float given = NumberParser.parseFloat(size);
                        // A share of the size the span is in, or so many
                        // times it: half of 20 is 10, not 50.
                        String unit = size.trim();
                        float around = child.getFontSize() != null ? child.getFontSize().floatValue()
                                : text.getFontSize() > 0 ? text.getFontSize() : 12f;
                        if (unit.endsWith("%")) {
                            given = given / 100 * around;
                        } else if (unit.endsWith("rem")) {
                            given = given * 16f;
                        } else if (unit.endsWith("em")) {
                            given = given * around;
                        } else if (unit.endsWith("ex")) {
                            given = given * around / 2;
                        }
                        child.setFontSize(Float.valueOf(given));
                    } catch (RuntimeException ignored) { /* inherited */ }
                }
                String weight = mergedValue(a, "font-weight");
                if (weight != null) {
                    String w = weight.trim();
                    child.setBold(Boolean.valueOf("bold".equals(w) || "bolder".equals(w) || isNumericGte(w, 600)));
                }
                String slant = mergedValue(a, "font-style");
                if (slant != null) {
                    String s = slant.trim();
                    child.setItalic(Boolean.valueOf("italic".equals(s) || "oblique".equals(s)));
                }
                spanUnplaced = true;
                readTextContent(r, out, text, child);
                // Back in the enclosing span, which has been placed by now
                // or handed its position on.
                spanUnplaced = false;
            }
        }
    }

    /** The first of a list of lengths, which is where a span starts; null when it cannot be read. */
    private Float firstLength(String value, boolean horizontal) {
        String v = value.trim();
        int end = 0;
        while (end < v.length() && v.charAt(end) != ' ' && v.charAt(end) != ',') end++;
        try {
            return Float.valueOf(horizontal ? lengthX(v.substring(0, end)) : lengthY(v.substring(0, end)));
        } catch (RuntimeException e) {
            return null;
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
        boolean user = "userSpaceOnUse".equals(a.get("gradientUnits"));
        if (a.containsKey("x1")) g.markGiven("x1");
        if (a.containsKey("x1")) g.setX1(gradCoord(a.get("x1"), user, viewportWidth));
        if (a.containsKey("y1")) g.markGiven("y1");
        if (a.containsKey("y1")) g.setY1(gradCoord(a.get("y1"), user, viewportHeight));
        if (a.containsKey("x2")) g.markGiven("x2");
        if (a.containsKey("x2")) g.setX2(gradCoord(a.get("x2"), user, viewportWidth));
        if (a.containsKey("y2")) g.markGiven("y2");
        if (a.containsKey("y2")) g.setY2(gradCoord(a.get("y2"), user, viewportHeight));
        if (a.containsKey("spreadMethod")) {
            g.markGiven("spreadMethod");
            g.setSpreadMethod(a.get("spreadMethod").trim());
        }
        if (a.containsKey("gradientUnits")) g.markGiven("gradientUnits");
        if ("userSpaceOnUse".equals(a.get("gradientUnits"))) g.setUserSpace(true);
        if (a.get("gradientTransform") != null) g.setGradientTransform(TransformParser.parse(a.get("gradientTransform")));
        String href = a.get("href");
        if (href == null) href = a.get("xlink:href");
        if (href != null && href.startsWith("#")) g.setHref(href.substring(1));
        readGradientStops(r, g.getStops(), colorOf(a) != null ? colorOf(a) : inheritedColor);
        return g;
    }

    private SVGRadialGradient readRadialGradient(XMLStreamReader r) throws XMLStreamException {
        SVGRadialGradient g = new SVGRadialGradient();
        Map<String, String> a = attrs(r);
        g.setId(a.get("id"));
        boolean user = "userSpaceOnUse".equals(a.get("gradientUnits"));
        if (a.containsKey("cx")) g.markGiven("cx");
        if (a.containsKey("cx")) g.setCx(gradCoord(a.get("cx"), user, viewportWidth));
        if (a.containsKey("cy")) g.markGiven("cy");
        if (a.containsKey("cy")) g.setCy(gradCoord(a.get("cy"), user, viewportHeight));
        if (a.containsKey("r")) g.markGiven("r");
        if (a.containsKey("r")) g.setR(gradCoord(a.get("r"), user, (float) diagonal()));
        if (a.containsKey("fx")) g.markGiven("fx");
        if (a.containsKey("fx")) g.setFx(Float.valueOf(gradCoord(a.get("fx"), user, viewportWidth)));
        if (a.containsKey("fy")) g.markGiven("fy");
        if (a.containsKey("fy")) g.setFy(Float.valueOf(gradCoord(a.get("fy"), user, viewportHeight)));
        if (a.get("gradientTransform") != null) g.setGradientTransform(TransformParser.parse(a.get("gradientTransform")));
        if (a.containsKey("spreadMethod")) {
            g.markGiven("spreadMethod");
            g.setSpreadMethod(a.get("spreadMethod").trim());
        }
        if (a.containsKey("gradientUnits")) g.markGiven("gradientUnits");
        if ("userSpaceOnUse".equals(a.get("gradientUnits"))) g.setUserSpace(true);
        String href = a.get("href");
        if (href == null) href = a.get("xlink:href");
        if (href != null && href.startsWith("#")) g.setHref(href.substring(1));
        readGradientStops(r, g.getStops(), colorOf(a) != null ? colorOf(a) : inheritedColor);
        return g;
    }

    /**
     * A coordinate of a gradient. A percentage is a fraction of the box
     * around what is painted, and in a gradient laid out in user space that
     * share of the viewport: {@code x2="100%"} is its far side, not 1.
     */
    private float gradCoord(String value, boolean userSpace, float whole) {
        float v = parseGradCoord(value);
        return userSpace && value != null && value.trim().endsWith("%") ? v * whole : v;
    }

    private float parseGradCoord(String s) {
        if (s == null) return 0f;
        String v = s.trim();
        if (v.endsWith("%")) {
            return Float.parseFloat(v.substring(0, v.length() - 1)) / 100f;
        }
        return NumberParser.parseFloat(v);
    }

    /** The {@code color} the element being read has from what it is in; null where nothing sets one. */
    private Integer inheritedColor;

    /** A {@code color} property as a colour, or null when it is not written or not one. */
    private Integer colorOf(Map<String, String> a) {
        String color = mergedValue(a, "color");
        if (color == null || ColorParser.isCurrentColor(color)) return null;
        try {
            return Integer.valueOf(ColorParser.parse(color));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void readGradientStops(XMLStreamReader r, List<SVGGradientStop> stops, Integer around)
            throws XMLStreamException {
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
            if (sc != null && ColorParser.isCurrentColor(sc)) {
                // The stop's own `color`, or the one it has from the
                // gradient and what that is in; black when nobody gives one.
                Integer own = colorOf(a);
                stop.setColor(own != null ? own.intValue() : around != null ? around.intValue() : ColorParser.BLACK);
            } else if (sc != null && !ColorParser.isNone(sc)) {
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
                Float alpha = StyleParser.toAlpha(so);
                if (alpha != null) stop.setOpacity(alpha.floatValue());
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
        // A transform declared as a property, in the style attribute or by
        // a rule of the stylesheet, which is folded into it. It replaces
        // the attribute, as any property does the attribute of its name.
        String declared = declaredValue(a, "transform");
        if (declared != null) {
            try {
                n.setTransform(TransformParser.parseCss(declared));
            } catch (RuntimeException e) {
                // Not a transform this reads: the attribute stands.
            }
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
                    || "clip-path".equals(k) || "fill-rule".equals(k)
                    || "stroke-dasharray".equals(k) || "stroke-dashoffset".equals(k)
                    || "display".equals(k) || "visibility".equals(k) || "mask".equals(k)
                    || "color".equals(k) || "filter".equals(k) || "marker".equals(k) || "clip-rule".equals(k)
                    || "vector-effect".equals(k)
                    || "marker-start".equals(k) || "marker-mid".equals(k) || "marker-end".equals(k)) {
                pres.put(k, e.getValue());
            }
        }
        n.setStyle(StyleParser.parse(pres, a.get("style")));
        // A percentage stroke width is that share of the viewport's
        // diagonal, evened out between its sides, and not that many units.
        String strokeWidth = mergedValue(a, "stroke-width");
        if (strokeWidth != null && strokeWidth.trim().endsWith("%") && n.getStyle() != null
                && n.getStyle().getStrokeWidth() != null) {
            double diagonal = Math.sqrt((viewportWidth * (double) viewportWidth
                    + viewportHeight * (double) viewportHeight) / 2);
            n.getStyle().setStrokeWidth(Float.valueOf((float) (n.getStyle().getStrokeWidth().floatValue() / 100 * diagonal)));
        }
        // So are the dashes and where they start.
        String dashes = mergedValue(a, "stroke-dasharray");
        float[] lengths = n.getStyle() == null ? null : n.getStyle().getStrokeDashArray();
        if (dashes != null && dashes.indexOf('%') >= 0 && lengths != null && lengths.length > 0) {
            String[] parts = dashes.trim().split("[\\s,]+");
            // An odd list is stored written out twice.
            float[] scaled = lengths.clone();
            for (int i = 0; i < scaled.length && parts.length > 0; i++) {
                if (parts[i % parts.length].endsWith("%")) {
                    scaled[i] = (float) (scaled[i] / 100 * diagonal());
                }
            }
            n.getStyle().setStrokeDashArray(scaled);
        }
        String dashOffset = mergedValue(a, "stroke-dashoffset");
        if (dashOffset != null && dashOffset.trim().endsWith("%") && n.getStyle() != null
                && n.getStyle().getStrokeDashOffset() != null) {
            n.getStyle().setStrokeDashOffset(Float.valueOf(
                    (float) (n.getStyle().getStrokeDashOffset().floatValue() / 100 * diagonal())));
        }
    }

    /** What a percentage of a length that runs neither across nor down is a share of. */
    private double diagonal() {
        return Math.sqrt((viewportWidth * (double) viewportWidth + viewportHeight * (double) viewportHeight) / 2);
    }

    /** What the style attribute, with the stylesheet's rules folded in, declares for a property; null when it does not. */
    private static String declaredValue(Map<String, String> attrs, String key) {
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
        return found;
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
        String own = m.get("style");
        if (sheet != null || (own != null && own.indexOf('!') >= 0)) {
            // The element's own declarations come after the stylesheet's,
            // and its important ones after the stylesheet's important ones:
            // `!important` in a style attribute beats it in a rule. The
            // word itself is taken off, which no reader of a value expects.
            StringBuilder normal = new StringBuilder();
            StringBuilder important = new StringBuilder();
            if (own != null) {
                for (String decl : own.split(";")) {
                    String d = decl.trim();
                    int bang = d.lastIndexOf('!');
                    if (bang > 0 && "important".equalsIgnoreCase(d.substring(bang + 1).trim())) {
                        important.append(d, 0, bang).append(';');
                    } else if (d.length() > 0) {
                        normal.append(d).append(';');
                    }
                }
            }
            m.put("style", (sheet == null ? "" : sheet[0]) + normal + (sheet == null ? "" : sheet[1]) + important);
        }
        String shorthand = declaredValue(m, "font");
        if (shorthand != null) {
            // `font: italic bold 24px serif` is the four properties this
            // reads, written as one. They are put ahead of the rest of the
            // style, where a longhand written beside it still has the last
            // word.
            m.put("style", fontLonghands(shorthand) + m.get("style"));
        }
        return m;
    }

    /** The font shorthand as the declarations it stands for; empty when it cannot be read as one. */
    private static String fontLonghands(String value) {
        String[] words = value.trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        int i = 0;
        // Before the size: the style, the variant and the weight, in any order.
        for (; i < words.length; i++) {
            String w = words[i];
            if ("italic".equals(w) || "oblique".equals(w)) {
                out.append("font-style:").append(w).append(';');
            } else if ("bold".equals(w) || "bolder".equals(w) || "lighter".equals(w)
                    || (w.length() == 3 && w.charAt(0) >= '1' && w.charAt(0) <= '9' && w.endsWith("00"))) {
                out.append("font-weight:").append(w).append(';');
            } else if (!"normal".equals(w) && !"small-caps".equals(w)) {
                break;
            }
        }
        if (i >= words.length - 1) {
            // A size and a family are both required.
            return "";
        }
        String size = words[i];
        int slash = size.indexOf('/');
        if (slash >= 0) size = size.substring(0, slash);
        if (size.isEmpty() || !(Character.isDigit(size.charAt(0)) || size.charAt(0) == '.')) {
            return "";
        }
        StringBuilder family = new StringBuilder();
        for (int k = i + 1; k < words.length; k++) {
            // A line height written apart from the size.
            if (k == i + 1 && words[k].startsWith("/")) {
                if (words[k].length() == 1) k++;
                continue;
            }
            if (family.length() > 0) family.append(' ');
            family.append(words[k]);
        }
        if (family.length() == 0) return "";
        return out.append("font-size:").append(size).append(";font-family:").append(family).append(';').toString();
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
