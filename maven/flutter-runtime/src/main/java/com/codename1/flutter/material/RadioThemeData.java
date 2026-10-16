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
 * Material {@code RadioThemeData}: write-once radio-button styling. The values
 * are {@link MaterialStateProperty}/density/cursor objects owned by other
 * runtime areas, so they are held opaquely as {@code Object} in this pass.
 * Named Dart constructor parameters map to setter methods.
 */
public class RadioThemeData {

    private Object fillColor;
    private Object overlayColor;
    private Object splashRadius;
    private Object materialTapTargetSize;
    private Object visualDensity;
    private Object mouseCursor;

    public void fillColor(Object v) {
        this.fillColor = v;
    }

    public void overlayColor(Object v) {
        this.overlayColor = v;
    }

    public void splashRadius(Object v) {
        this.splashRadius = v;
    }

    public void materialTapTargetSize(Object v) {
        this.materialTapTargetSize = v;
    }

    public void visualDensity(Object v) {
        this.visualDensity = v;
    }

    public void mouseCursor(Object v) {
        this.mouseCursor = v;
    }

    public Object getFillColor() {
        return fillColor;
    }
}
