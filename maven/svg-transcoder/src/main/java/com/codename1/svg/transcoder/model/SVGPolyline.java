package com.codename1.svg.transcoder.model;

/** &lt;polyline&gt; — an open polyline. */
public class SVGPolyline extends SVGShape {
    private float[] points = new float[0];

    public float[] getPoints() { return points; }
    public void setPoints(float[] points) { this.points = points == null ? new float[0] : points; }

    /** True when the figure should be closed (polygon). */
    public boolean isClosed() { return false; }
}
