package com.codename1.flutter;

/**
 * A glyph in the CN1 material design icon font, identified by its codepoint
 * (one of the {@code FontImage.MATERIAL_*} char constants).
 */
public final class IconData {

    private final char codePoint;
    private String fontFamily;

    public IconData(char codePoint) {
        this.codePoint = codePoint;
    }

    /**
     * Convenience overload for the transpiler, which emits every Dart {@code int}
     * codepoint literal as a Java {@code long}. Gallery-font codepoints live in the
     * BMP private-use area, so the narrowing to {@code char} is lossless in practice.
     */
    public IconData(long codePoint) {
        this((char) codePoint);
    }

    public char codePoint() {
        return codePoint;
    }

    /** The named icon font this glyph belongs to (Dart's {@code IconData.fontFamily}). */
    public String fontFamily() {
        return fontFamily;
    }

    public void fontFamily(String v) {
        this.fontFamily = v;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof IconData && ((IconData) o).codePoint == codePoint;
    }

    @Override
    public int hashCode() {
        return codePoint;
    }
}
