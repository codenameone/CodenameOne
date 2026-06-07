package com.codename1.svg.transcoder.parser;

/**
 * Parses an SVG transform attribute: a sequence of translate / rotate / scale
 * / skewX / skewY / matrix functions applied left-to-right.
 */
public final class TransformParser {

    private TransformParser() { }

    public static SVGTransform parse(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;

        SVGTransform result = SVGTransform.identity();
        int i = 0;
        int len = t.length();
        while (i < len) {
            while (i < len && (t.charAt(i) == ' ' || t.charAt(i) == ',')) i++;
            if (i >= len) break;

            int nameStart = i;
            while (i < len && (Character.isLetter(t.charAt(i)))) i++;
            String name = t.substring(nameStart, i).trim();
            while (i < len && t.charAt(i) != '(') i++;
            if (i >= len) throw new IllegalArgumentException("Expected '(' after " + name);
            i++;
            int close = t.indexOf(')', i);
            if (close < 0) throw new IllegalArgumentException("Unclosed transform " + name);
            String inside = t.substring(i, close);
            i = close + 1;

            float[] args = parseArgs(inside);
            SVGTransform op = build(name, args);
            result = result.multiply(op);
        }
        return result.isIdentity() ? null : result;
    }

    private static float[] parseArgs(String s) {
        NumberParser np = new NumberParser(s);
        java.util.ArrayList<Float> list = new java.util.ArrayList<Float>();
        while (np.hasMore()) list.add(np.nextFloat());
        float[] r = new float[list.size()];
        for (int i = 0; i < r.length; i++) r[i] = list.get(i);
        return r;
    }

    private static SVGTransform build(String name, float[] args) {
        if ("translate".equals(name)) {
            float tx = args.length > 0 ? args[0] : 0;
            float ty = args.length > 1 ? args[1] : 0;
            return SVGTransform.translate(tx, ty);
        }
        if ("scale".equals(name)) {
            float sx = args.length > 0 ? args[0] : 1;
            float sy = args.length > 1 ? args[1] : sx;
            return SVGTransform.scale(sx, sy);
        }
        if ("rotate".equals(name)) {
            float ang = args.length > 0 ? args[0] : 0;
            float cx = args.length > 1 ? args[1] : 0;
            float cy = args.length > 2 ? args[2] : 0;
            return SVGTransform.rotate(ang, cx, cy);
        }
        if ("skewX".equals(name)) {
            return SVGTransform.skewX(args.length > 0 ? args[0] : 0);
        }
        if ("skewY".equals(name)) {
            return SVGTransform.skewY(args.length > 0 ? args[0] : 0);
        }
        if ("matrix".equals(name)) {
            if (args.length < 6) throw new IllegalArgumentException("matrix() needs 6 args");
            return new SVGTransform(args[0], args[1], args[2], args[3], args[4], args[5]);
        }
        throw new IllegalArgumentException("Unknown transform: " + name);
    }
}
