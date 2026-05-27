package com.codename1.svg.transcoder.model;

/// Definition node holding the children of a &lt;clipPath&gt; element. The
/// transcoder takes the first shape child (rect, circle, ellipse, path, ...)
/// and emits it as the clip outline. Multi-shape and nested
/// `clip-path-on-clipPath-child` forms are not supported in this
/// implementation -- they are silently flattened to the first child shape.
public final class SVGClipPath extends SVGGroup {
}
