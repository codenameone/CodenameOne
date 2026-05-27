package com.codename1.svg.transcoder.model;

import java.util.ArrayList;
import java.util.List;

public final class SVGRadialGradient extends SVGNode {
    private float cx = 0.5f, cy = 0.5f, r = 0.5f;
    private boolean userSpace;
    private String href;
    private final List<SVGGradientStop> stops = new ArrayList<SVGGradientStop>();

    public float getCx() { return cx; }
    public void setCx(float cx) { this.cx = cx; }
    public float getCy() { return cy; }
    public void setCy(float cy) { this.cy = cy; }
    public float getR() { return r; }
    public void setR(float r) { this.r = r; }

    public boolean isUserSpace() { return userSpace; }
    public void setUserSpace(boolean userSpace) { this.userSpace = userSpace; }

    public String getHref() { return href; }
    public void setHref(String href) { this.href = href; }

    public List<SVGGradientStop> getStops() { return stops; }
}
