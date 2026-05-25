package com.codename1.svg.transcoder.model;

import java.util.HashMap;
import java.util.Map;

/** Top-level &lt;svg&gt; document. */
public final class SVGDocument extends SVGGroup {
    private float viewBoxX;
    private float viewBoxY;
    private float viewBoxWidth;
    private float viewBoxHeight;
    private float width;
    private float height;
    private final Map<String, SVGNode> definitions = new HashMap<String, SVGNode>();

    public float getViewBoxX() { return viewBoxX; }
    public void setViewBoxX(float v) { this.viewBoxX = v; }

    public float getViewBoxY() { return viewBoxY; }
    public void setViewBoxY(float v) { this.viewBoxY = v; }

    public float getViewBoxWidth() { return viewBoxWidth; }
    public void setViewBoxWidth(float v) { this.viewBoxWidth = v; }

    public float getViewBoxHeight() { return viewBoxHeight; }
    public void setViewBoxHeight(float v) { this.viewBoxHeight = v; }

    public float getWidth() { return width; }
    public void setWidth(float w) { this.width = w; }

    public float getHeight() { return height; }
    public void setHeight(float h) { this.height = h; }

    public Map<String, SVGNode> getDefinitions() { return definitions; }
}
