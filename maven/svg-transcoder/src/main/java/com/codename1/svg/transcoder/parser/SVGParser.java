package com.codename1.svg.transcoder.parser;

import com.codename1.svg.transcoder.animation.SMILParser;
import com.codename1.svg.transcoder.model.*;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
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
 * whole build — the transcoder errs on the side of "render what we can".
 */
public final class SVGParser {

    public SVGDocument parse(InputStream in) throws IOException {
        XMLInputFactory f = XMLInputFactory.newInstance();
        // harden against XXE
        f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        f.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        try {
            XMLStreamReader r = f.createXMLStreamReader(in);
            try {
                return parseDocument(r);
            } finally {
                r.close();
            }
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    private SVGDocument parseDocument(XMLStreamReader r) throws XMLStreamException {
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT && "svg".equals(r.getLocalName())) {
                SVGDocument doc = new SVGDocument();
                readSVGRoot(r, doc);
                readChildren(r, doc, doc);
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
        String vb = a.get("viewBox");
        if (vb != null) {
            NumberParser np = new NumberParser(vb);
            try {
                doc.setViewBoxX(np.nextFloat());
                doc.setViewBoxY(np.nextFloat());
                doc.setViewBoxWidth(np.nextFloat());
                doc.setViewBoxHeight(np.nextFloat());
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
            } else if ("linearGradient".equals(name)) {
                SVGLinearGradient lg = readLinearGradient(r);
                if (lg.getId() != null) doc.getDefinitions().put(lg.getId(), lg);
            } else if ("radialGradient".equals(name)) {
                SVGRadialGradient rg = readRadialGradient(r);
                if (rg.getId() != null) doc.getDefinitions().put(rg.getId(), rg);
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

    private void readDefs(XMLStreamReader r, SVGDocument doc) throws XMLStreamException {
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.END_ELEMENT) return;
            if (ev != XMLStreamConstants.START_ELEMENT) continue;
            String name = r.getLocalName();
            if ("linearGradient".equals(name)) {
                SVGLinearGradient lg = readLinearGradient(r);
                if (lg.getId() != null) doc.getDefinitions().put(lg.getId(), lg);
            } else if ("radialGradient".equals(name)) {
                SVGRadialGradient rg = readRadialGradient(r);
                if (rg.getId() != null) doc.getDefinitions().put(rg.getId(), rg);
            } else {
                skip(r);
            }
        }
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

    private String mergedValue(Map<String, String> attrs, String key) {
        if (attrs.containsKey(key)) return attrs.get(key);
        String style = attrs.get("style");
        if (style == null) return null;
        for (String decl : style.split(";")) {
            int colon = decl.indexOf(':');
            if (colon <= 0) continue;
            if (decl.substring(0, colon).trim().equals(key)) {
                return decl.substring(colon + 1).trim();
            }
        }
        return null;
    }

    private Map<String, String> presentationFor(Map<String, String> attrs, String... keys) {
        Map<String, String> out = new HashMap<String, String>();
        for (String k : keys) {
            if (attrs.containsKey(k)) out.put(k.startsWith("stop-") ? "fill" : k, attrs.get(k));
        }
        // map stop-color → fill, stop-opacity → fill-opacity for reuse with StyleParser
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
            if ("fill".equals(k) || "stroke".equals(k) || "fill-opacity".equals(k) || "stroke-opacity".equals(k)
                    || "opacity".equals(k) || "stroke-width".equals(k) || "stroke-linecap".equals(k)
                    || "stroke-linejoin".equals(k) || "stroke-miterlimit".equals(k)) {
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
        return m;
    }

    /** Read child elements of a shape — currently only animation children matter. */
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
