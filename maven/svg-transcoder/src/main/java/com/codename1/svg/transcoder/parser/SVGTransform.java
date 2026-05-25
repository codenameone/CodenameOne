package com.codename1.svg.transcoder.parser;

/**
 * Holds a fully-resolved 2D affine matrix:
 * <pre>
 *   [ a c e ]
 *   [ b d f ]
 *   [ 0 0 1 ]
 * </pre>
 */
public final class SVGTransform {
    public final float a, b, c, d, e, f;

    public SVGTransform(float a, float b, float c, float d, float e, float f) {
        this.a = a; this.b = b; this.c = c; this.d = d; this.e = e; this.f = f;
    }

    public static SVGTransform identity() {
        return new SVGTransform(1, 0, 0, 1, 0, 0);
    }

    public static SVGTransform translate(float tx, float ty) {
        return new SVGTransform(1, 0, 0, 1, tx, ty);
    }

    public static SVGTransform scale(float sx, float sy) {
        return new SVGTransform(sx, 0, 0, sy, 0, 0);
    }

    public static SVGTransform rotate(float angleDeg, float cx, float cy) {
        double r = Math.toRadians(angleDeg);
        float cos = (float) Math.cos(r);
        float sin = (float) Math.sin(r);
        // Translate(cx,cy) * Rotate(angle) * Translate(-cx,-cy)
        float a = cos, b = sin, c = -sin, d = cos;
        float e = cx - cos * cx + sin * cy;
        float f = cy - sin * cx - cos * cy;
        return new SVGTransform(a, b, c, d, e, f);
    }

    public static SVGTransform skewX(float angleDeg) {
        float t = (float) Math.tan(Math.toRadians(angleDeg));
        return new SVGTransform(1, 0, t, 1, 0, 0);
    }

    public static SVGTransform skewY(float angleDeg) {
        float t = (float) Math.tan(Math.toRadians(angleDeg));
        return new SVGTransform(1, t, 0, 1, 0, 0);
    }

    /** Returns this * o (this applied first conceptually under SVG's column-vector convention). */
    public SVGTransform multiply(SVGTransform o) {
        return new SVGTransform(
                a * o.a + c * o.b,
                b * o.a + d * o.b,
                a * o.c + c * o.d,
                b * o.c + d * o.d,
                a * o.e + c * o.f + e,
                b * o.e + d * o.f + f);
    }

    public boolean isIdentity() {
        return a == 1f && b == 0f && c == 0f && d == 1f && e == 0f && f == 0f;
    }
}
