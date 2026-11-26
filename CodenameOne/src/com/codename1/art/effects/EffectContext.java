/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.art.effects;

import com.codename1.art.Raster;

/// What an effect needs to know about where it is applied.
public final class EffectContext {
    private final double scale;
    private final Raster backdrop;

    /// Creates a context.
    ///
    /// #### Parameters
    ///
    /// - `scale`: device pixels per unit of the node's space
    /// - `backdrop`: what is painted below the layer, for background blur and
    ///   `BackgroundImage`; may be null
    public EffectContext(double scale, Raster backdrop) {
        this.scale = scale;
        this.backdrop = backdrop;
    }

    /// Device pixels per unit.
    public double getScale() {
        return scale;
    }

    /// What is painted below the layer, or null.
    public Raster getBackdrop() {
        return backdrop;
    }
}
