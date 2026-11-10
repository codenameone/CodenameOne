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

/// Flutter's {@code RoundSliderThumbShape}: a round thumb of the given radius,
/// which the slider paints in the theme's thumb colour.
public class RoundSliderThumbShape {

    private double enabledThumbRadius = 10.0;
    private Double disabledThumbRadius;
    private double elevation = 1.0;

    public void enabledThumbRadius(double v) { this.enabledThumbRadius = v; }
    public void disabledThumbRadius(double v) { this.disabledThumbRadius = v; }
    public void elevation(double v) { this.elevation = v; }

    public double enabledThumbRadius() { return enabledThumbRadius; }
    public double disabledThumbRadius() {
        return disabledThumbRadius != null ? disabledThumbRadius : enabledThumbRadius;
    }
    public double elevation() { return elevation; }
}
