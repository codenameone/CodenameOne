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
package com.codename1.flutter;

/**
 * An immutable description of how to paint a box — Flutter's
 * {@code BoxDecoration}. Only the {@link #getColor() background color} and
 * {@link #getShape() shape} are honored by the Codename One runtime for this
 * milestone; border/borderRadius/boxShadow/gradient/image are retained as
 * opaque values (owned by other API categories) but not yet painted.
 */
public class BoxDecoration extends Decoration {

    private Color color;
    private Object image;
    private Object border;
    private Object borderRadius;
    private Object boxShadow;
    private Object gradient;
    private Object backgroundBlendMode;
    private BoxShape shape = BoxShape.rectangle;

    public void color(Color v) {
        this.color = v;
    }

    public void image(Object v) {
        this.image = v;
    }

    public void border(Object v) {
        this.border = v;
    }

    public void borderRadius(Object v) {
        this.borderRadius = v;
    }

    public void boxShadow(Object v) {
        this.boxShadow = v;
    }

    public void gradient(Object v) {
        this.gradient = v;
    }

    public void backgroundBlendMode(Object v) {
        this.backgroundBlendMode = v;
    }

    public void shape(BoxShape v) {
        this.shape = v == null ? BoxShape.rectangle : v;
    }

    public Color getColor() {
        return color;
    }

    public Object getImage() {
        return image;
    }

    public Object getBorder() {
        return border;
    }

    public Object getBorderRadius() {
        return borderRadius;
    }

    public Object getBoxShadow() {
        return boxShadow;
    }

    public Object getGradient() {
        return gradient;
    }

    public Object getBackgroundBlendMode() {
        return backgroundBlendMode;
    }

    public BoxShape getShape() {
        return shape;
    }
}
