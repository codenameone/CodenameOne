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

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Lets its child overflow its own constraints — Flutter's {@code OverflowBox}.
 *
 * <p>Structural pass-through for this milestone: the single {@code child}
 * renders unchanged; the imposed min/max constraints and alignment are held
 * for a later render pass.</p>
 */
public class OverflowBox extends Widget implements HasChild {

    private Object alignment;
    private Double minWidth;
    private Double maxWidth;
    private Double minHeight;
    private Double maxHeight;
    private Widget child;

    public void alignment(Object v) { this.alignment = v; }
    public void minWidth(double v) { this.minWidth = v; }
    public void maxWidth(double v) { this.maxWidth = v; }
    public void minHeight(double v) { this.minHeight = v; }
    public void maxHeight(double v) { this.maxHeight = v; }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget getChild() {
        return child;
    }

    public Object getAlignment() {
        return alignment;
    }

    public Double getMinWidth() {
        return minWidth;
    }

    public Double getMaxWidth() {
        return maxWidth;
    }

    public Double getMinHeight() {
        return minHeight;
    }

    public Double getMaxHeight() {
        return maxHeight;
    }

    @Override
    public Element createElement() {
        return new OverflowBoxRenderElement(this);
    }
}
