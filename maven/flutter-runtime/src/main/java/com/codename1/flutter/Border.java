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
 * A border drawn around a box, with an independent {@link BorderSide} on each
 * edge — Flutter's {@code Border}.
 */
public final class Border extends BoxBorder {

    private BorderSide top = BorderSide.none;
    private BorderSide right = BorderSide.none;
    private BorderSide bottom = BorderSide.none;
    private BorderSide left = BorderSide.none;

    public Border() {
    }

    /** {@code Border.all(color: ..., width: ..., style: ...)}. */
    public static Border all(Color color, double width, BorderStyle style,
            Double strokeAlign) {
        BorderSide side = new BorderSide();
        if (color != null) {
            side.color(color);
        }
        side.width(width);
        side.style(style == null ? BorderStyle.solid : style);
        Border b = new Border();
        b.top = side;
        b.right = side;
        b.bottom = side;
        b.left = side;
        return b;
    }

    /** {@code Border.symmetric(vertical: ..., horizontal: ...)}. */
    public static Border symmetric(BorderSide vertical, BorderSide horizontal) {
        Border b = new Border();
        BorderSide v = vertical == null ? BorderSide.none : vertical;
        BorderSide h = horizontal == null ? BorderSide.none : horizontal;
        b.top = v;
        b.bottom = v;
        b.left = h;
        b.right = h;
        return b;
    }

    public void top(BorderSide v) {
        this.top = v == null ? BorderSide.none : v;
    }

    public void right(BorderSide v) {
        this.right = v == null ? BorderSide.none : v;
    }

    public void bottom(BorderSide v) {
        this.bottom = v == null ? BorderSide.none : v;
    }

    public void left(BorderSide v) {
        this.left = v == null ? BorderSide.none : v;
    }

    public BorderSide top() {
        return top;
    }

    public BorderSide right() {
        return right;
    }

    public BorderSide bottom() {
        return bottom;
    }

    public BorderSide left() {
        return left;
    }
}
