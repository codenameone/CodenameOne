/*
 * Copyright (c) 2025, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */
package com.codename1.lottie.transcoder.parser;

import com.codename1.svg.transcoder.model.SVGAnimation;
import com.codename1.svg.transcoder.model.SVGDocument;
import com.codename1.svg.transcoder.model.SVGEllipse;
import com.codename1.svg.transcoder.model.SVGGroup;
import com.codename1.svg.transcoder.model.SVGNode;
import com.codename1.svg.transcoder.model.SVGPath;
import com.codename1.svg.transcoder.model.SVGRect;
import com.codename1.svg.transcoder.model.SVGShape;
import com.codename1.svg.transcoder.parser.PathCommand;
import com.codename1.svg.transcoder.parser.SVGPaint;
import com.codename1.svg.transcoder.parser.SVGStyle;
import com.codename1.svg.transcoder.parser.SVGTransform;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.codename1.lottie.transcoder.parser.JsonParser.asBoolean;
import static com.codename1.lottie.transcoder.parser.JsonParser.asDouble;
import static com.codename1.lottie.transcoder.parser.JsonParser.asInt;
import static com.codename1.lottie.transcoder.parser.JsonParser.asList;
import static com.codename1.lottie.transcoder.parser.JsonParser.asMap;
import static com.codename1.lottie.transcoder.parser.JsonParser.asString;

/**
 * Reads a Lottie/Bodymovin JSON animation and produces an
 * {@link SVGDocument} the existing SVG transcoder's
 * {@code JavaCodeGenerator} can render. The pipeline is identical to the
 * SVG one from that point on -- no new Image base class, no new registry,
 * no per-port wiring.
 *
 * <p>Supported subset:</p>
 * <ul>
 *   <li>Shape layers (ty=4) with grouped {@code rc} / {@code el} / {@code sh}
 *       primitives, plus {@code fl} fills and {@code st} strokes.</li>
 *   <li>Solid color layers (ty=1) -- emitted as a filled rect.</li>
 *   <li>Layer transforms (anchor, position, scale, rotation, opacity).
 *       Constant values are baked into a static {@link SVGTransform};
 *       animated values are emitted as one or more {@link SVGAnimation}
 *       entries spanning the document's duration.</li>
 *   <li>Multi-keyframe properties are collapsed to first-vs-last linear
 *       interpolation -- the SVG codegen only honors that today, and the
 *       resulting motion still matches simple "spinner" / "pulse" cases.</li>
 * </ul>
 *
 * <p>Anything outside the subset (text layers, image layers, mattes,
 * expressions, repeaters, bezier easing) is silently dropped. The parser
 * still produces a renderable document so a partially-supported file
 * does not break the build.</p>
 */
public final class LottieParser {

    private LottieParser() { }

    public static SVGDocument parse(InputStream in) throws IOException {
        Object root = JsonParser.parse(in);
        if (!(root instanceof Map)) {
            throw new IllegalArgumentException("Lottie root must be a JSON object");
        }
        return parse(asMap(root));
    }

    public static SVGDocument parse(Map<String, Object> root) {
        SVGDocument doc = new SVGDocument();
        float w = (float) asDouble(root.get("w"), 100);
        float h = (float) asDouble(root.get("h"), 100);
        doc.setWidth(w);
        doc.setHeight(h);
        doc.setViewBoxX(0f);
        doc.setViewBoxY(0f);
        doc.setViewBoxWidth(w);
        doc.setViewBoxHeight(h);

        float frameRate = (float) asDouble(root.get("fr"), 30);
        float inFrame = (float) asDouble(root.get("ip"), 0);
        float outFrame = (float) asDouble(root.get("op"), 0);
        long durationMs = frameRate > 0f && outFrame > inFrame
                ? Math.round((outFrame - inFrame) * 1000.0 / frameRate)
                : 0L;
        float fpsOffset = inFrame; // subtract from raw frame values

        List<Object> layers = asList(root.get("layers"));
        if (layers == null) {
            return doc;
        }
        // Lottie paints layers in reverse: the last entry in the array is
        // drawn first, the first entry on top. SVG/CN1 paints in document
        // order, so iterate the layer list back-to-front.
        for (int i = layers.size() - 1; i >= 0; i--) {
            Map<String, Object> layer = asMap(layers.get(i));
            if (layer == null) continue;
            SVGNode emitted = emitLayer(layer, frameRate, fpsOffset, durationMs);
            if (emitted != null) {
                doc.addChild(emitted);
            }
        }
        return doc;
    }

    /** Build the SVG subtree for a single Lottie layer. */
    private static SVGNode emitLayer(Map<String, Object> layer, float frameRate,
                                     float fpsOffset, long durationMs) {
        int type = asInt(layer.get("ty"), -1);
        SVGGroup g = new SVGGroup();
        applyLayerTransform(g, asMap(layer.get("ks")), frameRate, fpsOffset, durationMs);

        switch (type) {
            case 1: // solid color layer
                emitSolidLayer(g, layer);
                break;
            case 4: // shape layer
                List<Object> shapes = asList(layer.get("shapes"));
                if (shapes != null) {
                    emitShapes(g, shapes, frameRate, fpsOffset, durationMs);
                }
                break;
            default:
                // Unsupported layer (text, image, null, precomp, etc.).
                // Return an empty group so the document still compiles.
                break;
        }
        return g;
    }

    private static void emitSolidLayer(SVGGroup g, Map<String, Object> layer) {
        SVGRect r = new SVGRect();
        r.setX(0);
        r.setY(0);
        r.setWidth((float) asDouble(layer.get("sw"), 0));
        r.setHeight((float) asDouble(layer.get("sh"), 0));
        int argb = parseHexColor(asString(layer.get("sc"), "#000000"));
        SVGStyle st = r.getStyle();
        st.setFill(SVGPaint.ofColor(argb));
        st.setStroke(SVGPaint.NONE);
        g.addChild(r);
    }

    /** Walk a "shapes" array within a single Lottie shape group and append
     *  the produced SVG nodes to {@code parent}. Lottie's paint convention:
     *  list entries earlier in the array are drawn on top of later ones,
     *  and fill/stroke items style the primitives that follow them in the
     *  list. We collect primitives in document order, scan once for the
     *  applicable fill/stroke (last wins -- matches AE's "Fill" effect),
     *  apply them to every primitive, then append in reverse so paint order
     *  matches Lottie. Nested {@code gr} groups recurse into a sub-{@code <g>}. */
    private static void emitShapes(SVGGroup parent, List<Object> shapes,
                                   float frameRate, float fpsOffset, long durationMs) {
        List<SVGNode> emitted = new ArrayList<SVGNode>();
        Integer fillArgb = null;
        Float fillOpacity = null;
        Integer strokeArgb = null;
        Float strokeOpacity = null;
        Float strokeWidth = null;
        SVGTransform groupTransform = null;
        for (Object o : shapes) {
            Map<String, Object> s = asMap(o);
            if (s == null) continue;
            String ty = asString(s.get("ty"), "");
            switch (ty) {
                case "rc": {
                    SVGRect r = emitRect(s);
                    if (r != null) emitted.add(r);
                    break;
                }
                case "el": {
                    SVGEllipse e = emitEllipse(s);
                    if (e != null) emitted.add(e);
                    break;
                }
                case "sh": {
                    SVGPath p = emitPath(s);
                    if (p != null) emitted.add(p);
                    break;
                }
                case "fl": {
                    fillArgb = Integer.valueOf(extractColor(s));
                    fillOpacity = extractScalar0to1(s.get("o"));
                    break;
                }
                case "st": {
                    strokeArgb = Integer.valueOf(extractColor(s));
                    strokeOpacity = extractScalar0to1(s.get("o"));
                    strokeWidth = Float.valueOf(extractScalar(s.get("w"), 1f));
                    break;
                }
                case "tr": {
                    // Shape group transform -- baked as a static matrix on a
                    // wrapping <g>. Animated shape-group transforms collapse
                    // to the first keyframe.
                    groupTransform = staticTransformFrom(s);
                    break;
                }
                case "gr": {
                    SVGGroup child = new SVGGroup();
                    List<Object> items = asList(s.get("it"));
                    if (items != null) {
                        emitShapes(child, items, frameRate, fpsOffset, durationMs);
                    }
                    emitted.add(child);
                    break;
                }
                default:
                    // "tm" (trim path), "rp" (repeater), "gf"/"gs" (gradient
                    // fill/stroke), "mm" (merge), "rd" (rounded corners),
                    // expressions -- silently ignored.
                    break;
            }
        }

        SVGGroup target = parent;
        if (groupTransform != null && !groupTransform.isIdentity()) {
            target = new SVGGroup();
            target.setTransform(groupTransform);
            parent.addChild(target);
        }

        // Lottie paints later-list-entries first, so reverse before appending
        // so the first item ends up on top.
        for (int i = emitted.size() - 1; i >= 0; i--) {
            SVGNode n = emitted.get(i);
            if (n instanceof SVGShape) {
                SVGShape s = (SVGShape) n;
                SVGStyle st = s.getStyle();
                if (fillArgb != null) {
                    int argb = applyOpacity(fillArgb.intValue(),
                            fillOpacity == null ? 1f : fillOpacity.floatValue());
                    st.setFill(SVGPaint.ofColor(argb));
                } else {
                    st.setFill(SVGPaint.NONE);
                }
                if (strokeArgb != null) {
                    int argb = applyOpacity(strokeArgb.intValue(),
                            strokeOpacity == null ? 1f : strokeOpacity.floatValue());
                    st.setStroke(SVGPaint.ofColor(argb));
                    if (strokeWidth != null) {
                        st.setStrokeWidth(strokeWidth);
                    }
                }
            }
            target.addChild(n);
        }
    }

    private static SVGRect emitRect(Map<String, Object> s) {
        float[] pos = extractVector2(s.get("p"), new float[]{0f, 0f});
        float[] size = extractVector2(s.get("s"), new float[]{0f, 0f});
        float r = extractScalar(s.get("r"), 0f);
        if (size[0] <= 0f || size[1] <= 0f) return null;
        SVGRect rect = new SVGRect();
        rect.setX(pos[0] - size[0] / 2f);
        rect.setY(pos[1] - size[1] / 2f);
        rect.setWidth(size[0]);
        rect.setHeight(size[1]);
        if (r > 0f) { rect.setRx(r); rect.setRy(r); }
        return rect;
    }

    private static SVGEllipse emitEllipse(Map<String, Object> s) {
        float[] pos = extractVector2(s.get("p"), new float[]{0f, 0f});
        float[] size = extractVector2(s.get("s"), new float[]{0f, 0f});
        if (size[0] <= 0f || size[1] <= 0f) return null;
        SVGEllipse e = new SVGEllipse();
        e.setCx(pos[0]);
        e.setCy(pos[1]);
        e.setRx(size[0] / 2f);
        e.setRy(size[1] / 2f);
        return e;
    }

    /** Lottie shape ("sh") encodes a path as vertices + per-vertex in/out
     *  tangents. Convert to cubic Beziers. */
    private static SVGPath emitPath(Map<String, Object> s) {
        Map<String, Object> ks = asMap(s.get("ks"));
        if (ks == null) return null;
        Map<String, Object> k = asMap(ks.get("k"));
        if (k == null) {
            // Animated shape -- take the first keyframe's "s" value.
            List<Object> kfs = asList(ks.get("k"));
            if (kfs == null || kfs.isEmpty()) return null;
            Map<String, Object> first = asMap(kfs.get(0));
            if (first == null) return null;
            List<Object> sList = asList(first.get("s"));
            if (sList == null || sList.isEmpty()) return null;
            k = asMap(sList.get(0));
            if (k == null) return null;
        }
        List<Object> vertices = asList(k.get("v"));
        List<Object> inTangents = asList(k.get("i"));
        List<Object> outTangents = asList(k.get("o"));
        boolean closed = asBoolean(k.get("c"), false);
        if (vertices == null || vertices.isEmpty()) return null;

        List<PathCommand> commands = new ArrayList<PathCommand>();
        float[] first = pair(vertices.get(0));
        commands.add(new PathCommand(PathCommand.Type.MOVE,
                new float[]{ first[0], first[1] }));
        int n = vertices.size();
        for (int i = 1; i <= n; i++) {
            int prev = i - 1;
            int curr = i % n;
            if (curr == 0 && !closed) break;
            float[] p0 = pair(vertices.get(prev));
            float[] p1 = pair(vertices.get(curr));
            // Lottie tangents are *relative* to the vertex they belong to.
            float[] out0 = outTangents != null && prev < outTangents.size()
                    ? pair(outTangents.get(prev)) : new float[]{0f, 0f};
            float[] in1 = inTangents != null && curr < inTangents.size()
                    ? pair(inTangents.get(curr)) : new float[]{0f, 0f};
            float c1x = p0[0] + out0[0];
            float c1y = p0[1] + out0[1];
            float c2x = p1[0] + in1[0];
            float c2y = p1[1] + in1[1];
            commands.add(new PathCommand(PathCommand.Type.CUBIC,
                    new float[]{ c1x, c1y, c2x, c2y, p1[0], p1[1] }));
        }
        if (closed) {
            commands.add(new PathCommand(PathCommand.Type.CLOSE, new float[0]));
        }
        SVGPath path = new SVGPath();
        path.setCommands(commands);
        return path;
    }

    /** Apply the layer "ks" block to the group: bake static parts into a
     *  matrix, emit animateTransform for any animated rotation/position/
     *  scale. */
    private static void applyLayerTransform(SVGGroup g, Map<String, Object> ks,
                                            float frameRate, float fpsOffset, long durationMs) {
        if (ks == null) return;

        // Decompose into anchor, position, scale, rotation, opacity.
        Map<String, Object> a = asMap(ks.get("a"));
        Map<String, Object> p = asMap(ks.get("p"));
        Map<String, Object> s = asMap(ks.get("s"));
        Map<String, Object> r = asMap(ks.get("r"));
        Map<String, Object> o = asMap(ks.get("o"));

        float[] anchor = extractInitial(a, new float[]{0f, 0f});
        float[] position = extractInitial(p, new float[]{0f, 0f});
        float[] scale = extractInitial(s, new float[]{100f, 100f});
        float rotation = extractInitial(r, new float[]{0f})[0];
        float opacity = extractInitial(o, new float[]{100f})[0];

        // Bake the constant transform first so the painter sees the correct
        // resting pose for non-animated values.
        SVGTransform mt = SVGTransform.identity()
                .multiply(SVGTransform.translate(position[0], position[1]))
                .multiply(SVGTransform.rotate(rotation, 0, 0))
                .multiply(SVGTransform.scale(scale[0] / 100f, scale[1] / 100f))
                .multiply(SVGTransform.translate(-anchor[0], -anchor[1]));
        if (!mt.isIdentity()) {
            g.setTransform(mt);
        }
        if (opacity != 100f) {
            g.getStyle().setOpacity(Float.valueOf(opacity / 100f));
        }

        // Animated rotation -- most common Lottie animation. The SVG codegen
        // already pre-applies the static transform, so we emit additional
        // animateTransform deltas relative to the resting pose.
        if (durationMs > 0L) {
            emitAnimatedRotation(g, r, durationMs, rotation);
            emitAnimatedTranslate(g, p, durationMs, position);
            emitAnimatedScale(g, s, durationMs, scale);
        }
    }

    private static void emitAnimatedRotation(SVGGroup g, Map<String, Object> r,
                                             long durationMs, float restingDeg) {
        if (r == null) return;
        if (asInt(r.get("a"), 0) != 1) return;
        List<Object> keyframes = asList(r.get("k"));
        if (keyframes == null || keyframes.size() < 2) return;
        float[] startEnd = firstAndLastScalar(keyframes);
        if (startEnd == null) return;
        SVGAnimation an = new SVGAnimation();
        an.setKind(SVGAnimation.Kind.ANIMATE_TRANSFORM);
        an.setTransformType(SVGAnimation.TransformType.ROTATE);
        an.setBeginMs(0L);
        an.setDurMs(durationMs);
        an.setRepeatCount(SVGAnimation.REPEAT_INDEFINITE);
        an.setFrom(formatRotateValue(startEnd[0] - restingDeg));
        an.setTo(formatRotateValue(startEnd[1] - restingDeg));
        g.addAnimation(an);
    }

    private static void emitAnimatedTranslate(SVGGroup g, Map<String, Object> p,
                                              long durationMs, float[] restingXY) {
        if (p == null) return;
        if (asInt(p.get("a"), 0) != 1) return;
        List<Object> keyframes = asList(p.get("k"));
        if (keyframes == null || keyframes.size() < 2) return;
        float[][] startEnd = firstAndLastVector(keyframes);
        if (startEnd == null) return;
        SVGAnimation an = new SVGAnimation();
        an.setKind(SVGAnimation.Kind.ANIMATE_TRANSFORM);
        an.setTransformType(SVGAnimation.TransformType.TRANSLATE);
        an.setBeginMs(0L);
        an.setDurMs(durationMs);
        an.setRepeatCount(SVGAnimation.REPEAT_INDEFINITE);
        an.setFrom((startEnd[0][0] - restingXY[0]) + " " + (startEnd[0][1] - restingXY[1]));
        an.setTo((startEnd[1][0] - restingXY[0]) + " " + (startEnd[1][1] - restingXY[1]));
        g.addAnimation(an);
    }

    private static void emitAnimatedScale(SVGGroup g, Map<String, Object> s,
                                          long durationMs, float[] restingScale) {
        if (s == null) return;
        if (asInt(s.get("a"), 0) != 1) return;
        List<Object> keyframes = asList(s.get("k"));
        if (keyframes == null || keyframes.size() < 2) return;
        float[][] startEnd = firstAndLastVector(keyframes);
        if (startEnd == null) return;
        SVGAnimation an = new SVGAnimation();
        an.setKind(SVGAnimation.Kind.ANIMATE_TRANSFORM);
        an.setTransformType(SVGAnimation.TransformType.SCALE);
        an.setBeginMs(0L);
        an.setDurMs(durationMs);
        an.setRepeatCount(SVGAnimation.REPEAT_INDEFINITE);
        // Lottie scale is in percent (100 = identity); convert to multiplier
        // relative to the resting scale baked into the static transform.
        float fx = (startEnd[0][0] / restingScale[0]);
        float fy = (startEnd[0][1] / restingScale[1]);
        float tx = (startEnd[1][0] / restingScale[0]);
        float ty = (startEnd[1][1] / restingScale[1]);
        an.setFrom(fx + " " + fy);
        an.setTo(tx + " " + ty);
        g.addAnimation(an);
    }

    private static String formatRotateValue(float deg) {
        // SVG rotate transform takes "angle [cx cy]" -- a single scalar is
        // sufficient here because the static transform already moved the
        // pivot to the anchor point.
        return Float.toString(deg);
    }

    // ---------------------------------------------------------------------
    // Lottie property readers.
    // ---------------------------------------------------------------------

    /** Read either a constant scalar/vector or the first keyframe's "s"
     *  value -- the "resting" value the static transform should use. */
    private static float[] extractInitial(Map<String, Object> prop, float[] fallback) {
        if (prop == null) return fallback;
        int animated = asInt(prop.get("a"), 0);
        Object k = prop.get("k");
        if (animated == 0) {
            return floatsFrom(k, fallback);
        }
        List<Object> keyframes = asList(k);
        if (keyframes == null || keyframes.isEmpty()) {
            return fallback;
        }
        Map<String, Object> first = asMap(keyframes.get(0));
        if (first == null) return fallback;
        Object sv = first.get("s");
        return floatsFrom(sv, fallback);
    }

    private static float[] floatsFrom(Object o, float[] fallback) {
        if (o instanceof Number) {
            return new float[]{ ((Number) o).floatValue() };
        }
        List<Object> list = asList(o);
        if (list == null) return fallback;
        float[] out = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            out[i] = (float) asDouble(list.get(i), 0);
        }
        return out;
    }

    private static float extractScalar(Object prop, float fallback) {
        if (prop == null) return fallback;
        Map<String, Object> p = asMap(prop);
        if (p == null) return fallback;
        Object k = p.get("k");
        if (k instanceof Number) return ((Number) k).floatValue();
        List<Object> kfs = asList(k);
        if (kfs != null && !kfs.isEmpty()) {
            Map<String, Object> first = asMap(kfs.get(0));
            if (first != null) {
                Object sv = first.get("s");
                if (sv instanceof Number) return ((Number) sv).floatValue();
                List<Object> sList = asList(sv);
                if (sList != null && !sList.isEmpty()) {
                    return (float) asDouble(sList.get(0), fallback);
                }
            }
        }
        return fallback;
    }

    private static Float extractScalar0to1(Object prop) {
        float v = extractScalar(prop, 100f) / 100f;
        if (v < 0f) v = 0f;
        if (v > 1f) v = 1f;
        return Float.valueOf(v);
    }

    private static float[] extractVector2(Object prop, float[] fallback) {
        if (prop == null) return fallback;
        Map<String, Object> p = asMap(prop);
        if (p == null) return fallback;
        Object k = p.get("k");
        List<Object> list = asList(k);
        if (list != null && !list.isEmpty()) {
            Object e0 = list.get(0);
            if (e0 instanceof Map) {
                // Animated -- take first keyframe's "s".
                Map<String, Object> first = asMap(e0);
                Object sv = first.get("s");
                List<Object> sList = asList(sv);
                if (sList != null && sList.size() >= 2) {
                    return new float[]{
                            (float) asDouble(sList.get(0), fallback[0]),
                            (float) asDouble(sList.get(1), fallback[1])
                    };
                }
                return fallback;
            }
            if (list.size() >= 2) {
                return new float[]{
                        (float) asDouble(list.get(0), fallback[0]),
                        (float) asDouble(list.get(1), fallback[1])
                };
            }
        }
        return fallback;
    }

    private static int extractColor(Map<String, Object> s) {
        Map<String, Object> c = asMap(s.get("c"));
        if (c == null) return 0xFF000000;
        Object k = c.get("k");
        List<Object> list = asList(k);
        if (list == null) return 0xFF000000;
        // Animated colors collapse to the first keyframe's "s".
        if (!list.isEmpty() && list.get(0) instanceof Map) {
            Map<String, Object> first = asMap(list.get(0));
            list = asList(first.get("s"));
            if (list == null) return 0xFF000000;
        }
        double r = list.size() > 0 ? asDouble(list.get(0), 0) : 0;
        double gC = list.size() > 1 ? asDouble(list.get(1), 0) : 0;
        double bC = list.size() > 2 ? asDouble(list.get(2), 0) : 0;
        double aC = list.size() > 3 ? asDouble(list.get(3), 1) : 1;
        int ri = clampByte((int) Math.round(r * 255));
        int gi = clampByte((int) Math.round(gC * 255));
        int bi = clampByte((int) Math.round(bC * 255));
        int ai = clampByte((int) Math.round(aC * 255));
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }

    private static int applyOpacity(int argb, float scale) {
        int a = (argb >>> 24) & 0xFF;
        a = clampByte(Math.round(a * scale));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private static SVGTransform staticTransformFrom(Map<String, Object> tr) {
        float[] anchor = extractInitial(asMap(tr.get("a")), new float[]{0f, 0f});
        float[] position = extractInitial(asMap(tr.get("p")), new float[]{0f, 0f});
        float[] scale = extractInitial(asMap(tr.get("s")), new float[]{100f, 100f});
        float rotation = extractInitial(asMap(tr.get("r")), new float[]{0f})[0];
        return SVGTransform.identity()
                .multiply(SVGTransform.translate(position[0], position[1]))
                .multiply(SVGTransform.rotate(rotation, 0, 0))
                .multiply(SVGTransform.scale(scale[0] / 100f, scale[1] / 100f))
                .multiply(SVGTransform.translate(-anchor[0], -anchor[1]));
    }

    private static float[] firstAndLastScalar(List<Object> keyframes) {
        Float s = null;
        Float e = null;
        for (int i = 0; i < keyframes.size(); i++) {
            Map<String, Object> kf = asMap(keyframes.get(i));
            if (kf == null) continue;
            Object sv = kf.get("s");
            float v;
            if (sv instanceof Number) v = ((Number) sv).floatValue();
            else {
                List<Object> sList = asList(sv);
                if (sList == null || sList.isEmpty()) continue;
                v = (float) asDouble(sList.get(0), 0);
            }
            if (s == null) s = Float.valueOf(v);
            e = Float.valueOf(v);
        }
        if (s == null || e == null) return null;
        return new float[]{ s.floatValue(), e.floatValue() };
    }

    private static float[][] firstAndLastVector(List<Object> keyframes) {
        float[] s = null;
        float[] e = null;
        for (int i = 0; i < keyframes.size(); i++) {
            Map<String, Object> kf = asMap(keyframes.get(i));
            if (kf == null) continue;
            List<Object> sList = asList(kf.get("s"));
            if (sList == null || sList.size() < 2) continue;
            float[] v = new float[]{
                    (float) asDouble(sList.get(0), 0),
                    (float) asDouble(sList.get(1), 0)
            };
            if (s == null) s = v;
            e = v;
        }
        if (s == null || e == null) return null;
        return new float[][]{ s, e };
    }

    private static float[] pair(Object o) {
        List<Object> list = asList(o);
        if (list == null || list.size() < 2) return new float[]{ 0f, 0f };
        return new float[]{
                (float) asDouble(list.get(0), 0),
                (float) asDouble(list.get(1), 0)
        };
    }

    private static int parseHexColor(String s) {
        if (s == null) return 0xFF000000;
        String t = s.trim();
        if (t.startsWith("#")) t = t.substring(1);
        try {
            if (t.length() == 6) {
                return 0xFF000000 | Integer.parseInt(t, 16);
            }
            if (t.length() == 8) {
                long l = Long.parseLong(t, 16);
                return (int) l;
            }
        } catch (NumberFormatException ignored) { /* fall through */ }
        return 0xFF000000;
    }

    private static int clampByte(int v) {
        if (v < 0) return 0;
        if (v > 255) return 255;
        return v;
    }
}
