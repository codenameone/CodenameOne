/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
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

    private java.util.Set<String> given;

    /**
     * Whether the element wrote this attribute itself. Kept for the elements
     * that take what they leave out from another one they refer to (a
     * gradient or a pattern with an href), where a default cannot be told
     * from a value.
     */
    public boolean isGiven(String attribute) { return given != null && given.contains(attribute); }
    public void markGiven(String attribute) {
        if (given == null) given = new java.util.HashSet<String>();
        given.add(attribute);
    }

    public List<SVGAnimation> getAnimations() { return animations; }
    public void addAnimation(SVGAnimation a) { animations.add(a); }
}
