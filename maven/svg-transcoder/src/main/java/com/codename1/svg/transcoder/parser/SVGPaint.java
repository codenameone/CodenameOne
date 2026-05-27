package com.codename1.svg.transcoder.parser;

/**
 * A paint value: either a solid ARGB color, a reference to a gradient
 * definition by id (set via {@link #setReference}), or "none".
 */
public final class SVGPaint {

    public static final SVGPaint NONE = new SVGPaint(true, 0, null);
    public static final SVGPaint BLACK = new SVGPaint(false, 0xFF000000, null);

    private final boolean none;
    private final int color;
    private final String reference;

    private SVGPaint(boolean none, int color, String reference) {
        this.none = none;
        this.color = color;
        this.reference = reference;
    }

    public static SVGPaint ofColor(int argb) {
        return new SVGPaint(false, argb, null);
    }

    public static SVGPaint ofReference(String id) {
        return new SVGPaint(false, 0, id);
    }

    public boolean isNone() { return none; }
    public int getColor() { return color; }
    public String getReference() { return reference; }
    public boolean isReference() { return reference != null; }

    public static SVGPaint setReference(String value) {
        String r = stripUrl(value);
        return r == null ? null : ofReference(r);
    }

    /** "url(#foo)" -> "foo"; returns null otherwise. */
    public static String stripUrl(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (!t.startsWith("url(")) return null;
        int close = t.indexOf(')', 4);
        if (close < 0) return null;
        String inside = t.substring(4, close).trim();
        if (inside.startsWith("#")) inside = inside.substring(1);
        return inside;
    }
}
