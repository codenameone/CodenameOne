package com.codename1.svg.transcoder.model;

import com.codename1.svg.transcoder.parser.SVGStyle;
import com.codename1.svg.transcoder.parser.SVGTransform;

import java.util.ArrayList;
import java.util.List;

/** Base class for every parsed SVG element. */
public abstract class SVGNode {
    private String id;
    private SVGStyle style = new SVGStyle();
    private SVGTransform transform;
    private final List<SVGAnimation> animations = new ArrayList<SVGAnimation>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public SVGStyle getStyle() { return style; }
    public void setStyle(SVGStyle style) { this.style = style; }

    public SVGTransform getTransform() { return transform; }
    public void setTransform(SVGTransform transform) { this.transform = transform; }

    public List<SVGAnimation> getAnimations() { return animations; }
    public void addAnimation(SVGAnimation a) { animations.add(a); }
}
