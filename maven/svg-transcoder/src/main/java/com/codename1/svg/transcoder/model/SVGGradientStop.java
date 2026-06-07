package com.codename1.svg.transcoder.model;

public final class SVGGradientStop {
    private float offset;
    private int color;
    private float opacity = 1f;

    public float getOffset() { return offset; }
    public void setOffset(float offset) { this.offset = offset; }
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
    public float getOpacity() { return opacity; }
    public void setOpacity(float opacity) { this.opacity = opacity; }
}
