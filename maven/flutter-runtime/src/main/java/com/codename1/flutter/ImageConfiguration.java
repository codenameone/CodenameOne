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

import com.codename1.flutter.rendering.Size;

/**
 * The context handed to a custom {@code Decoration}'s box painter
 * ({@code ImageConfiguration} in Flutter): the target size, device pixel ratio,
 * text direction and locale. new_gallery's tab-indicator and pie-chart painters
 * read {@link #size()} to lay out their geometry.
 */
public class ImageConfiguration {

    /** The empty configuration (Dart's {@code ImageConfiguration.empty}). */
    public static final ImageConfiguration empty = new ImageConfiguration();

    private Size size;
    private Double devicePixelRatio;
    private TextDirection textDirection;
    private Locale locale;

    public ImageConfiguration() {
    }

    // Named-parameter setters.
    public void size(Size v) {
        this.size = v;
    }

    public void devicePixelRatio(double v) {
        this.devicePixelRatio = v;
    }

    public void textDirection(TextDirection v) {
        this.textDirection = v;
    }

    public void locale(Locale v) {
        this.locale = v;
    }

    public Size size() {
        return size;
    }

    public Double devicePixelRatio() {
        return devicePixelRatio;
    }
}
