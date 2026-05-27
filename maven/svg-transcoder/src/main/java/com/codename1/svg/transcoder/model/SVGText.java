package com.codename1.svg.transcoder.model;

/**
 * &lt;text&gt; element. Treated as a single styled run -- &lt;tspan&gt;
 * children inherit the parent text's style but are flattened into the
 * combined content string. Supports SVG's start / middle / end anchor
 * positioning and a single (x, y) baseline; per-character coordinate
 * lists are not implemented.
 */
public final class SVGText extends SVGShape {

    public enum Anchor { START, MIDDLE, END }

    private float x;
    private float y;
    private String content = "";
    private String fontFamily;
    private float fontSize;
    private boolean bold;
    private boolean italic;
    private Anchor anchor = Anchor.START;

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public float getY() { return y; }
    public void setY(float y) { this.y = y; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content == null ? "" : content; }

    public String getFontFamily() { return fontFamily; }
    public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }

    public float getFontSize() { return fontSize; }
    public void setFontSize(float fontSize) { this.fontSize = fontSize; }

    public boolean isBold() { return bold; }
    public void setBold(boolean bold) { this.bold = bold; }

    public boolean isItalic() { return italic; }
    public void setItalic(boolean italic) { this.italic = italic; }

    public Anchor getAnchor() { return anchor; }
    public void setAnchor(Anchor anchor) { this.anchor = anchor == null ? Anchor.START : anchor; }
}
