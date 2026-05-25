package com.codename1.svg.transcoder.model;

import java.util.ArrayList;
import java.util.List;

public final class SVGLinearGradient extends SVGNode {
    private float x1 = 0f, y1 = 0f, x2 = 1f, y2 = 0f;
    private boolean userSpace;
    private String href;
    private final List<SVGGradientStop> stops = new ArrayList<SVGGradientStop>();

    public float getX1() { return x1; }
    public void setX1(float v) { this.x1 = v; }
    public float getY1() { return y1; }
    public void setY1(float v) { this.y1 = v; }
    public float getX2() { return x2; }
    public void setX2(float v) { this.x2 = v; }
    public float getY2() { return y2; }
    public void setY2(float v) { this.y2 = v; }

    public boolean isUserSpace() { return userSpace; }
    public void setUserSpace(boolean userSpace) { this.userSpace = userSpace; }

    public String getHref() { return href; }
    public void setHref(String href) { this.href = href; }

    public List<SVGGradientStop> getStops() { return stops; }
}
