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
package com.codename1.surfaces;

import java.util.Map;

/// A flexible spacer that absorbs the leftover space of its parent row or column, pushing its
/// siblings apart. Maps to a SwiftUI `Spacer` and a weighted empty view on Android.
public class SurfaceSpacer extends SurfaceNode {
    private int minDips;

    /// Creates a spacer.
    public SurfaceSpacer() {
    }

    /// Creates a spacer with a minimum length along the parent's axis.
    ///
    /// #### Parameters
    ///
    /// - `minDips`: the minimum length in dips
    public SurfaceSpacer(int minDips) {
        this.minDips = minDips;
    }

    /// Returns the minimum length along the parent's axis in dips.
    public int getMinDips() {
        return minDips;
    }

    @Override
    String getType() {
        return "spacer";
    }

    @Override
    void serializeContent(Map<String, Object> out, Map<String, byte[]> images, int depth) {
        if (minDips != 0) {
            out.put("min", Integer.valueOf(minDips));
        }
    }
}
