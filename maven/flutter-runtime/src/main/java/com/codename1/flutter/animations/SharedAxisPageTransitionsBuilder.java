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
package com.codename1.flutter.animations;

import com.codename1.flutter.Color;

/**
 * The {@code PageTransitionsBuilder} for the shared-axis (X/Y/Z) motion pattern
 * — the {@code animations} package's {@code SharedAxisPageTransitionsBuilder}.
 * Installed into a {@code PageTransitionsTheme} so route pushes animate along
 * the configured {@link SharedAxisTransitionType}. The optional {@code fillColor}
 * paints behind the transitioning pages. Configuration only in this pass; the
 * builder emits a {@link SharedAxisTransition} when the navigation renderer
 * lands.
 */
public class SharedAxisPageTransitionsBuilder
        extends com.codename1.flutter.material.PageTransitionsBuilder {

    private SharedAxisTransitionType transitionType;
    private Color fillColor;

    public void transitionType(SharedAxisTransitionType v) {
        this.transitionType = v;
    }

    public void fillColor(Color v) {
        this.fillColor = v;
    }

    public SharedAxisTransitionType getTransitionType() {
        return transitionType;
    }

    public Color getFillColor() {
        return fillColor;
    }
}
