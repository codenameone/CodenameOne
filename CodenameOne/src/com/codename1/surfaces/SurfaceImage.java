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

import com.codename1.ui.Image;

import java.util.Map;

/// An image node. Because surfaces render while the app process may be dead, images are shipped as
/// named PNG blobs beside the descriptor rather than passed by reference: constructing the node
/// from a `com.codename1.ui.Image` encodes it to PNG at publish time and names it by content hash
/// (identical art re-published later ships no new bytes). A node can also reference a name that
/// was already shipped with a previous publish.
///
/// Keep surface art small -- widget renderers run under tight memory budgets (about 30mb for the
/// whole iOS widget extension) and Android parcels the rendered widget over a 1mb binder
/// transaction.
public class SurfaceImage extends SurfaceNode {
    /// Scales the image to fit inside the node bounds, preserving aspect ratio.
    public static final int SCALE_FIT = 0;

    /// Scales the image to fill the node bounds, preserving aspect ratio and cropping overflow.
    public static final int SCALE_FILL = 1;

    /// Centers the image without scaling.
    public static final int SCALE_CENTER = 2;

    private final Image image;
    private final String registeredName;
    private int scaleMode = SCALE_FIT;
    private SurfaceColor tint;

    /// Creates an image node from an image. The image is encoded to PNG when the descriptor is
    /// published.
    ///
    /// #### Parameters
    ///
    /// - `image`: the image to ship with the descriptor
    public SurfaceImage(Image image) {
        this.image = image;
        this.registeredName = null;
    }

    /// Creates an image node referencing an image name shipped with an earlier publish to the same
    /// surface.
    ///
    /// #### Parameters
    ///
    /// - `registeredName`: the previously registered image name
    public SurfaceImage(String registeredName) {
        this.image = null;
        this.registeredName = registeredName;
    }

    /// Sets how the image scales within the node bounds.
    ///
    /// #### Parameters
    ///
    /// - `scaleMode`: one of `SCALE_FIT`, `SCALE_FILL`, `SCALE_CENTER`
    ///
    /// #### Returns
    ///
    /// this image node, for chaining
    public SurfaceImage setScaleMode(int scaleMode) {
        this.scaleMode = scaleMode;
        return this;
    }

    /// Tints the image with the supplied color, template-image style: the image's alpha channel is
    /// kept and its color replaced.
    ///
    /// #### Parameters
    ///
    /// - `tint`: the tint color
    ///
    /// #### Returns
    ///
    /// this image node, for chaining
    public SurfaceImage setTint(SurfaceColor tint) {
        this.tint = tint;
        return this;
    }

    /// Returns the source image, or null when this node references a registered name.
    public Image getImage() {
        return image;
    }

    /// Returns the referenced registered name, or null when this node ships its own image.
    public String getRegisteredName() {
        return registeredName;
    }

    /// Returns the scale mode, one of `SCALE_FIT`, `SCALE_FILL`, `SCALE_CENTER`.
    public int getScaleMode() {
        return scaleMode;
    }

    /// Returns the tint color, or null.
    public SurfaceColor getTint() {
        return tint;
    }

    @Override
    public SurfaceImage setPadding(int all) {
        super.setPadding(all);
        return this;
    }

    @Override
    public SurfaceImage setPadding(int top, int right, int bottom, int left) {
        super.setPadding(top, right, bottom, left);
        return this;
    }

    @Override
    public SurfaceImage setBackground(SurfaceColor background) {
        super.setBackground(background);
        return this;
    }

    @Override
    public SurfaceImage setCornerRadius(int radius) {
        super.setCornerRadius(radius);
        return this;
    }

    @Override
    public SurfaceImage setAlignment(SurfaceAlignment alignment) {
        super.setAlignment(alignment);
        return this;
    }

    @Override
    public SurfaceImage setWeight(int weight) {
        super.setWeight(weight);
        return this;
    }

    @Override
    public SurfaceImage setSize(int widthDips, int heightDips) {
        super.setSize(widthDips, heightDips);
        return this;
    }

    @Override
    public SurfaceImage setAction(String actionId) {
        super.setAction(actionId);
        return this;
    }

    @Override
    public SurfaceImage setAction(String actionId, Map<String, Object> params) {
        super.setAction(actionId, params);
        return this;
    }

    @Override
    String getType() {
        return "img";
    }

    @Override
    void serializeContent(Map<String, Object> out, Map<String, byte[]> images, int depth) {
        String name = registeredName;
        if (name == null) {
            name = SurfaceSerializer.registerImage(image, images);
        }
        out.put("name", name == null ? "" : name);
        if (scaleMode == SCALE_FILL) {
            out.put("scale", "fill");
        } else if (scaleMode == SCALE_CENTER) {
            out.put("scale", "center");
        }
        if (tint != null) {
            out.put("tint", SurfaceSerializer.colorMap(tint));
        }
    }
}
