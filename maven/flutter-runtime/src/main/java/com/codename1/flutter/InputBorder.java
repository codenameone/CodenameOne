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
 * The border drawn around a Material text field — Flutter's {@code InputBorder}.
 */
public abstract class InputBorder extends ShapeBorder {

    /** {@code InputBorder.none}: the "no border" sentinel. */
    public static final InputBorder none = new NoInputBorder();

    // Flutter's default is an ordinary BorderSide -- opaque black, one logical
    // pixel -- not "none". Defaulting to none meant an OutlineInputBorder that
    // states no side of its own drew a rounded background with an INVISIBLE
    // outline, so the text-field demo's Life story and Salary boxes, whose
    // whole decoration is that outline, rendered as bare labels.
    BorderSide borderSide = BorderSide.solidBlack();

    public void borderSide(BorderSide v) {
        this.borderSide = v == null ? BorderSide.none : v;
    }

    public BorderSide getBorderSide() {
        return borderSide;
    }
}
