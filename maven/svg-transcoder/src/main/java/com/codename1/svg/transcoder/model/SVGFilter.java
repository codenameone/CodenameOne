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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// A `<filter>`: a chain of effects an element is drawn through.
///
/// The effects are kept as they are written, each with its attributes and
/// the elements inside it, for a renderer to work out. It is registered as
/// a definition. The class the transcoder generates applies no filters.
public final class SVGFilter extends SVGNode {
    /// One effect of a filter (`feGaussianBlur`, say), or an element inside
    /// one (`feMergeNode`, `feFuncR`, a light).
    public static final class Primitive {
        private final String name;
        private final Map<String, String> attributes;
        private final List<Primitive> children = new ArrayList<Primitive>();

        public Primitive(String name, Map<String, String> attributes) {
            this.name = name;
            this.attributes = attributes;
        }

        /// The element's name, without a prefix.
        public String getName() { return name; }
        /// The value of an attribute or, where the element has both, of the
        /// style property of that name; null when it has neither.
        public String get(String attribute) { return attributes.get(attribute); }
        /// Replaces what an attribute says, for a reader that has worked
        /// out what it stands for.
        public void set(String attribute, String value) { attributes.put(attribute, value); }
        public List<Primitive> getChildren() { return children; }
    }

    private final List<Primitive> primitives = new ArrayList<Primitive>();
    private boolean regionUserSpace;
    private float regionX = -0.1f;
    private float regionY = -0.1f;
    private float regionWidth = 1.2f;
    private float regionHeight = 1.2f;
    private boolean primitiveObjectBoundingBox;
    private boolean srgb;

    public List<Primitive> getPrimitives() { return primitives; }

    /// Whether the region is in the coordinates of the filtered element
    /// (`filterUnits="userSpaceOnUse"`) and not in fractions of its
    /// bounding box.
    public boolean isRegionUserSpace() { return regionUserSpace; }
    public void setRegionUserSpace(boolean v) { this.regionUserSpace = v; }

    /// The region the filter works in, outside which nothing is drawn.
    /// Left alone it is the bounding box and a tenth more each way.
    public float getRegionX() { return regionX; }
    public float getRegionY() { return regionY; }
    public float getRegionWidth() { return regionWidth; }
    public float getRegionHeight() { return regionHeight; }
    public void setRegion(float x, float y, float width, float height) {
        this.regionX = x;
        this.regionY = y;
        this.regionWidth = width;
        this.regionHeight = height;
    }

    /// Whether the lengths of the effects are fractions of the filtered
    /// element's bounding box (`primitiveUnits="objectBoundingBox"`).
    public boolean isPrimitiveObjectBoundingBox() { return primitiveObjectBoundingBox; }
    public void setPrimitiveObjectBoundingBox(boolean v) { this.primitiveObjectBoundingBox = v; }

    /// Whether the effects work on colours as they are written
    /// (`color-interpolation-filters="sRGB"`) and not on linear light,
    /// which is what they work on when nothing says.
    public boolean isSrgb() { return srgb; }
    public void setSrgb(boolean v) { this.srgb = v; }
}
