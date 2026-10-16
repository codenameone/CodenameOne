/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Clip;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.Material;

/**
 * Fills, clips and elevates its {@code child} to an arbitrary shape — Flutter's
 * {@code PhysicalShape}.
 *
 * <p>Built on {@link Material}, which already paints a coloured surface, clips
 * its subtree to a rounded shape and draws an elevation shadow. The two widgets
 * describe the same thing; the only difference is that PhysicalShape names its
 * shape through a clipper.</p>
 *
 * <p>It was a pass-through, which is a quiet way to lose a whole surface: Crane
 * builds its front layer — the white rounded card the destination list sits on
 * — as a PhysicalShape, so the card simply did not exist and its contents
 * floated on the backdrop.</p>
 */
public class PhysicalShape extends StatelessWidget implements HasChild {

    private Object clipper;
    private Object clipBehavior;
    private double elevation;
    private Color color;
    private Color shadowColor;
    private Widget child;

    public void clipper(Object v) { this.clipper = v; }
    public void clipBehavior(Object v) { this.clipBehavior = v; }
    public void elevation(double v) { this.elevation = v; }
    public void color(Color v) { this.color = v; }
    public void shadowColor(Color v) { this.shadowColor = v; }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget getChild() {
        return child;
    }

    @Override
    public Widget build(BuildContext context) {
        if (child == null) {
            return null;
        }
        Material m = new Material();
        if (color != null) {
            m.color(color);
        }
        if (shadowColor != null) {
            m.shadowColor(shadowColor);
        }
        m.elevation(elevation);
        Object shape = clipper instanceof ShapeBorderClipper
                ? ((ShapeBorderClipper) clipper).getShape() : null;
        if (shape != null) {
            m.shape(shape);
            // A shape is only a shape if the subtree is held to it; Flutter's
            // PhysicalShape always clips.
            m.clipBehavior(Clip.antiAlias);
        }
        m.child(child);
        return m;
    }
}
