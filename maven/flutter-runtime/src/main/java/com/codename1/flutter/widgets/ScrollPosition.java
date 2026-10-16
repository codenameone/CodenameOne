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

import com.codename1.flutter.animation.Curve;

import dart.async.Future;
import dart.core.Duration;

/**
 * The live scroll offset of a single scrollable ({@code ScrollPosition} in
 * Flutter), extending {@link ScrollMetrics} with mutation. new_gallery reads
 * {@code controller.position.maxScrollExtent} / {@code .haveDimensions}. Actual
 * animated scrolling is driven by the mounted scroll render element; here the
 * pixel offset is updated synchronously.
 */
public class ScrollPosition extends ScrollMetrics {

    /** Whether the viewport and content dimensions are known yet. */
    public boolean haveDimensions() {
        return hasContentDimensions && hasViewportDimension;
    }

    /** Animate to {@code to}; completes immediately in the M1 model. */
    public Future<Object> animateTo(double to, Duration duration, Curve curve) {
        jumpTo(to);
        return Future.value(null);
    }

    public void jumpTo(double value) {
        this.pixels = Math.max(minScrollExtent, Math.min(maxScrollExtent, value));
        this.hasPixels = true;
    }

    // ------------------------------------------------------------------
    // Framework plumbing
    // ------------------------------------------------------------------

    void applyContentDimensions(double min, double max) {
        this.minScrollExtent = min;
        this.maxScrollExtent = max;
        this.hasContentDimensions = true;
    }

    void applyViewportDimension(double dim) {
        this.viewportDimension = dim;
        this.hasViewportDimension = true;
    }

    void setPixels(double p) {
        this.pixels = p;
        this.hasPixels = true;
    }
}
