package com.codename1.flutter;

/**
 * A glyph in the CN1 material design icon font, identified by its codepoint
 * (one of the {@code FontImage.MATERIAL_*} char constants).
 */
public final class IconData {

    private final char codePoint;

    public IconData(char codePoint) {
        this.codePoint = codePoint;
    }

    public char codePoint() {
        return codePoint;
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
