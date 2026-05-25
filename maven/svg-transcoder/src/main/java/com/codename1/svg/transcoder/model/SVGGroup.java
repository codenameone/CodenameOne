package com.codename1.svg.transcoder.model;

import java.util.ArrayList;
import java.util.List;

/** &lt;g&gt; or &lt;svg&gt; container. */
public class SVGGroup extends SVGNode {
    private final List<SVGNode> children = new ArrayList<SVGNode>();

    public List<SVGNode> getChildren() { return children; }
    public void addChild(SVGNode n) { children.add(n); }
}
