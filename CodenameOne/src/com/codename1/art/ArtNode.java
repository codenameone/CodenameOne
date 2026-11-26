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
package com.codename1.art;

import com.codename1.art.effects.Effect;
import com.codename1.ui.geom.GeneralPath;

import java.util.ArrayList;
import java.util.List;

/// One element of an artwork: a group, shape, text, image or adjustment layer.
///
/// Every node has its own transform (from its space to its parent's), opacity, blend mode,
/// an optional clip path and mask, and a list of effects applied in order.
public abstract class ArtNode {
    private String id;
    private String name;
    private boolean visible = true;
    private boolean locked;
    private float opacity = 1;
    private BlendMode blendMode = BlendMode.NORMAL;
    private Matrix transform = Matrix.IDENTITY;
    private GeneralPath clipPath;
    private ArtNode mask;
    private boolean luminanceMask;
    private boolean knockout;
    private final List<Effect> effects = new ArrayList<Effect>();

    /// The node's id, unique within a document when set.
    public String getId() {
        return id;
    }

    /// Sets the id.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id
    public void setId(String id) {
        this.id = id;
    }

    /// The display name, as a layers panel shows it.
    public String getName() {
        return name;
    }

    /// Sets the name.
    ///
    /// #### Parameters
    ///
    /// - `name`: the name
    public void setName(String name) {
        this.name = name;
    }

    /// False hides the node and everything in it.
    public boolean isVisible() {
        return visible;
    }

    /// Shows or hides the node.
    ///
    /// #### Parameters
    ///
    /// - `visible`: true to show it
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /// True keeps the editor from selecting the node.
    public boolean isLocked() {
        return locked;
    }

    /// Locks or unlocks the node.
    ///
    /// #### Parameters
    ///
    /// - `locked`: true to lock
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    /// Opacity, 0 to 1.
    public float getOpacity() {
        return opacity;
    }

    /// Sets the opacity.
    ///
    /// #### Parameters
    ///
    /// - `opacity`: 0 to 1
    public void setOpacity(float opacity) {
        this.opacity = Math.max(0, Math.min(1, opacity));
    }

    /// The blend mode.
    public BlendMode getBlendMode() {
        return blendMode;
    }

    /// Sets the blend mode.
    ///
    /// #### Parameters
    ///
    /// - `blendMode`: the mode
    public void setBlendMode(BlendMode blendMode) {
        this.blendMode = blendMode == null ? BlendMode.NORMAL : blendMode;
    }

    /// The transform from this node's space to its parent's.
    public Matrix getTransform() {
        return transform;
    }

    /// Sets the transform.
    ///
    /// #### Parameters
    ///
    /// - `transform`: the new transform
    public void setTransform(Matrix transform) {
        this.transform = transform == null ? Matrix.IDENTITY : transform;
    }

    /// A clip path in this node's space, or null.
    public GeneralPath getClipPath() {
        return clipPath;
    }

    /// Sets the clip path.
    ///
    /// #### Parameters
    ///
    /// - `clipPath`: the clip; null for none
    public void setClipPath(GeneralPath clipPath) {
        this.clipPath = clipPath;
    }

    /// A node whose alpha (or luminance) masks this one, in this node's space; or null.
    public ArtNode getMask() {
        return mask;
    }

    /// Sets the mask.
    ///
    /// #### Parameters
    ///
    /// - `mask`: the mask node; null for none
    /// - `luminance`: true to mask by luminance (SVG), false by alpha
    public void setMask(ArtNode mask, boolean luminance) {
        this.mask = mask;
        this.luminanceMask = luminance;
    }

    /// True when the node knocks out what is below it in its group, Photoshop's shallow
    /// knockout: where the node is opaque, the layers under it in the same group are erased
    /// before it is drawn, so the group's backdrop shows through.
    public boolean isKnockout() {
        return knockout;
    }

    /// Sets the knockout.
    ///
    /// #### Parameters
    ///
    /// - `knockout`: true to knock out the layers below in the group
    public void setKnockout(boolean knockout) {
        this.knockout = knockout;
    }

    /// True when the mask works by luminance.
    public boolean isLuminanceMask() {
        return luminanceMask;
    }

    /// The effects, applied in order. The list is live.
    public List<Effect> getEffects() {
        return effects;
    }

    /// The bounds of this node's own content in its own space as `x, y, w, h`, without
    /// effects; null when it has none.
    public abstract double[] getLocalBounds();

    /// A deep copy.
    public abstract ArtNode copy();

    /// A copy for undo: like `copy`, but pixel data is shared rather than duplicated. Code that
    /// draws into a raster replaces it with a copy first, so a snapshot never changes.
    ArtNode snapshot() {
        return copy();
    }

    /// Copies the shared properties into `n` and returns it.
    protected ArtNode copyBase(ArtNode n) {
        n.id = id;
        n.name = name;
        n.visible = visible;
        n.locked = locked;
        n.opacity = opacity;
        n.blendMode = blendMode;
        n.transform = transform;
        n.clipPath = clipPath == null ? null : new GeneralPath(clipPath);
        n.mask = mask == null ? null : mask.copy();
        n.luminanceMask = luminanceMask;
        n.knockout = knockout;
        for (int i = 0; i < effects.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            n.effects.add(effects.get(i).copy());
        }
        return n;
    }

    /// Bounds in the parent's space, as `x, y, w, h`; null when empty.
    public double[] getBounds() {
        return transformBounds(getLocalBounds(), transform);
    }

    static double[] transformBounds(double[] b, Matrix m) {
        if (b == null) {
            return null;
        }
        double[] xs = {b[0], b[0] + b[2], b[0], b[0] + b[2]};
        double[] ys = {b[1], b[1], b[1] + b[3], b[1] + b[3]};
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            double x = m.mapX(xs[i], ys[i]);
            double y = m.mapY(xs[i], ys[i]);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        return new double[]{minX, minY, maxX - minX, maxY - minY};
    }

    static double[] union(double[] a, double[] b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        double x = Math.min(a[0], b[0]);
        double y = Math.min(a[1], b[1]);
        double r = Math.max(a[0] + a[2], b[0] + b[2]);
        double btm = Math.max(a[1] + a[3], b[1] + b[3]);
        return new double[]{x, y, r - x, btm - y};
    }
}
