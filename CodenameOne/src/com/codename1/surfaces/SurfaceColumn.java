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

/// A container that stacks its children vertically. Maps to a SwiftUI `VStack`, an Android
/// vertical `LinearLayout` and a vertical box on desktop surfaces.
public class SurfaceColumn extends SurfaceContainer {
    private int spacing;

    /// Sets the vertical gap between consecutive children.
    ///
    /// #### Parameters
    ///
    /// - `spacing`: the gap in dips
    ///
    /// #### Returns
    ///
    /// this column, for chaining
    public SurfaceColumn setSpacing(int spacing) {
        this.spacing = spacing;
        return this;
    }

    /// Returns the gap between consecutive children in dips.
    public int getSpacing() {
        return spacing;
    }

    @Override
    public SurfaceColumn add(SurfaceNode child) {
        super.add(child);
        return this;
    }

    @Override
    public SurfaceColumn setPadding(int all) {
        super.setPadding(all);
        return this;
    }

    @Override
    public SurfaceColumn setPadding(int top, int right, int bottom, int left) {
        super.setPadding(top, right, bottom, left);
        return this;
    }

    @Override
    public SurfaceColumn setBackground(SurfaceColor color) {
        super.setBackground(color);
        return this;
    }

    @Override
    public SurfaceColumn setCornerRadius(int radius) {
        super.setCornerRadius(radius);
        return this;
    }

    @Override
    public SurfaceColumn setAlignment(SurfaceAlignment alignment) {
        super.setAlignment(alignment);
        return this;
    }

    @Override
    public SurfaceColumn setWeight(int weight) {
        super.setWeight(weight);
        return this;
    }

    @Override
    public SurfaceColumn setSize(int widthDips, int heightDips) {
        super.setSize(widthDips, heightDips);
        return this;
    }

    @Override
    public SurfaceColumn setAction(String actionId) {
        super.setAction(actionId);
        return this;
    }

    @Override
    public SurfaceColumn setAction(String actionId, Map<String, Object> params) {
        super.setAction(actionId, params);
        return this;
    }

    @Override
    String getType() {
        return "col";
    }

    @Override
    void serializeContent(Map<String, Object> out, Map<String, byte[]> images, int depth) {
        if (spacing != 0) {
            out.put("spacing", Integer.valueOf(spacing));
        }
        super.serializeContent(out, images, depth);
    }
}
