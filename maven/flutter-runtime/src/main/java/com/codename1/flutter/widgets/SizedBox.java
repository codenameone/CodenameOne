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
import com.codename1.flutter.rendering.Size;

/**
 * A box with a fixed width and/or height (logical pixels). Without a child
 * it is a fixed-size spacer; with a child it tightens the child to the given
 * dimensions.
 */
/// Implements {@link HasChild} so the widget walkers can see THROUGH it.
/// A button consumes its content rather than mounting it, and the walk that finds that
/// content stops at any wrapper it cannot open: Shrine's login buttons wrap their label
/// in a Padding, and both rendered with no label at all -- the row collapsed to a blob
/// where the reference reads CANCEL and NEXT.
public class SizedBox extends Widget implements HasChild {

    private Double width;
    private Double height;
    private Widget child;

    public void width(double v) {
        this.width = v;
    }

    public void height(double v) {
        this.height = v;
    }

    public void child(Widget v) {
        this.child = v;
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
     * {@code SizedBox.shrink}: a zero-size box (a minimal spacer / placeholder).
     */
    public static SizedBox shrink(Key key, Widget child) {
        SizedBox b = new SizedBox();
        b.key(key);
        b.width(0);
        b.height(0);
        if (child != null) {
            b.child(child);
        }
        return b;
    }

    /**
     * {@code SizedBox.expand}: a box that expands to fill its parent (infinite
     * width and height).
     */
    public static SizedBox expand(Key key, Widget child) {
        SizedBox b = new SizedBox();
        b.key(key);
        b.width(Double.POSITIVE_INFINITY);
        b.height(Double.POSITIVE_INFINITY);
        if (child != null) {
            b.child(child);
        }
        return b;
    }

    /**
     * {@code SizedBox.fromSize}: a box tightened to the given {@link Size}.
     */
    public static SizedBox fromSize(Key key, Size size, Widget child) {
        SizedBox b = new SizedBox();
        b.key(key);
        if (size != null) {
            b.width(size.width());
            b.height(size.height());
        }
        if (child != null) {
            b.child(child);
        }
        return b;
    }

    @Override
    public Element createElement() {
        return new SizedBoxRenderElement(this);
    }
}
