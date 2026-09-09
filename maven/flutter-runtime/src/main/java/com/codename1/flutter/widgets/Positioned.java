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
import com.codename1.flutter.Key;
import com.codename1.flutter.Widget;

/**
 * Positions a child of a {@link Stack} by insets from the stack's edges
 * and/or an explicit extent (all in logical pixels). Only has an effect when
 * its render element sits directly below a Stack.
 */
public class Positioned extends Widget {

    private Double left;
    private Double top;
    private Double right;
    private Double bottom;
    private Double width;
    private Double height;
    private Widget child;

    public void left(double v) {
        this.left = v;
    }

    public void top(double v) {
        this.top = v;
    }

    public void right(double v) {
        this.right = v;
    }

    public void bottom(double v) {
        this.bottom = v;
    }

    public void width(double v) {
        this.width = v;
    }

    public void height(double v) {
        this.height = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Double getLeft() {
        return left;
    }

    public Double getTop() {
        return top;
    }

    public Double getRight() {
        return right;
    }

    public Double getBottom() {
        return bottom;
    }

    public Double getWidth() {
        return width;
    }

    public Double getHeight() {
        return height;
    }

    public Widget getChild() {
        return child;
    }

    /**
     * {@code Positioned.fill}: pins the child to all four edges of the stack
     * (each unspecified inset defaults to 0), so it fills the stack.
     */
    public static Positioned fill(Key key, Double left, Double top, Double right, Double bottom,
            Widget child) {
        Positioned p = new Positioned();
        p.key(key);
        p.left(left == null ? 0 : left);
        p.top(top == null ? 0 : top);
        p.right(right == null ? 0 : right);
        p.bottom(bottom == null ? 0 : bottom);
        if (child != null) {
            p.child(child);
        }
        return p;
    }

    @Override
    public Element createElement() {
        return new PositionedRenderElement(this);
    }
}
