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
package com.codename1.flutter.material;

/**
 * Material {@code CheckboxThemeData}: write-once checkbox styling. All values
 * here are MaterialStateProperty / border / density objects owned by other
 * runtime areas, so they are held as opaque {@code Object}s in this pass.
 */
public class CheckboxThemeData {

    private Object fillColor;
    private Object checkColor;
    private Object overlayColor;
    private Object materialTapTargetSize;
    private Object shape;
    private Object side;
    private Object visualDensity;
    private Object mouseCursor;
    private Object splashRadius;

    public void fillColor(Object v) {
        this.fillColor = v;
    }

    public void checkColor(Object v) {
        this.checkColor = v;
    }

    public void overlayColor(Object v) {
        this.overlayColor = v;
    }

    public void materialTapTargetSize(Object v) {
        this.materialTapTargetSize = v;
    }

    public void shape(Object v) {
        this.shape = v;
    }

    public void side(Object v) {
        this.side = v;
    }

    public void visualDensity(Object v) {
        this.visualDensity = v;
    }

    public void mouseCursor(Object v) {
        this.mouseCursor = v;
    }

    public void splashRadius(Object v) {
        this.splashRadius = v;
    }
}
