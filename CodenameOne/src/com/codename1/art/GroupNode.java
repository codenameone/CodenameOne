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

import java.util.ArrayList;
import java.util.List;

/// A node that holds other nodes, painted first to last.
///
/// An artboard (a Sketch artboard, a Figma frame, a PSD document) is a group with a frame:
/// `setFrame` gives it a rectangle it can fill with a background and clip its children to.
public class GroupNode extends ArtNode {
    private final List<ArtNode> children = new ArrayList<ArtNode>();
    private boolean passThrough = true;
    private double[] frame;
    private boolean clipToFrame;
    private ArtPaint background;
    private String symbolId;

    /// The children, painted first to last. The list is live.
    public List<ArtNode> getChildren() {
        return children;
    }

    /// Adds a child on top.
    ///
    /// #### Parameters
    ///
    /// - `child`: the node
    public GroupNode add(ArtNode child) {
        children.add(child);
        return this;
    }

    /// True (the default) lets children blend with what is below the group, as PSD's pass
    /// through mode does; false composites the group as one layer first. A group with
    /// opacity, effects, a mask or a blend mode other than normal is always isolated.
    public boolean isPassThrough() {
        return passThrough;
    }

    /// Sets pass-through.
    ///
    /// #### Parameters
    ///
    /// - `passThrough`: false to isolate the group
    public void setPassThrough(boolean passThrough) {
        this.passThrough = passThrough;
    }

    /// The frame as `x, y, w, h`, or null for a plain group.
    public double[] getFrame() {
        return frame;
    }

    /// Makes this group an artboard.
    ///
    /// #### Parameters
    ///
    /// - `x`: left
    /// - `y`: top
    /// - `w`: width
    /// - `h`: height
    /// - `clip`: true to clip children to the frame
    public void setFrame(double x, double y, double w, double h, boolean clip) {
        this.frame = new double[]{x, y, w, h};
        this.clipToFrame = clip;
    }

    /// Removes the frame.
    public void clearFrame() {
        frame = null;
        clipToFrame = false;
    }

    /// True when children are clipped to the frame.
    public boolean isClipToFrame() {
        return clipToFrame;
    }

    /// The frame's background, or null.
    public ArtPaint getBackground() {
        return background;
    }

    /// Sets the frame's background.
    ///
    /// #### Parameters
    ///
    /// - `background`: the paint; null for none
    public void setBackground(ArtPaint background) {
        this.background = background;
    }

    /// The symbol or component this group was expanded from, or null.
    public String getSymbolId() {
        return symbolId;
    }

    /// Records the symbol this group was expanded from.
    ///
    /// #### Parameters
    ///
    /// - `symbolId`: the source symbol's id
    public void setSymbolId(String symbolId) {
        this.symbolId = symbolId;
    }

    @Override
    public double[] getLocalBounds() {
        if (frame != null && clipToFrame) {
            return new double[]{frame[0], frame[1], frame[2], frame[3]};
        }
        double[] b = frame == null ? null : new double[]{frame[0], frame[1], frame[2], frame[3]};
        for (ArtNode c : children) {
            if (c.isVisible()) {
                b = union(b, c.getBounds());
            }
        }
        return b;
    }

    @Override
    public ArtNode copy() {
        GroupNode g = new GroupNode();
        copyGroup(g);
        return copyBase(g);
    }

    @Override
    ArtNode snapshot() {
        GroupNode g = new GroupNode();
        for (int i = 0; i < children.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            g.children.add(children.get(i).snapshot());
        }
        g.passThrough = passThrough;
        g.frame = frame == null ? null : new double[]{frame[0], frame[1], frame[2], frame[3]};
        g.clipToFrame = clipToFrame;
        g.background = background == null ? null : background.copy();
        g.symbolId = symbolId;
        return copyBase(g);
    }

    /// Copies the group's own state into `g`.
    protected void copyGroup(GroupNode g) {
        for (int i = 0; i < children.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            g.children.add(children.get(i).copy());
        }
        g.passThrough = passThrough;
        g.frame = frame == null ? null : new double[]{frame[0], frame[1], frame[2], frame[3]};
        g.clipToFrame = clipToFrame;
        g.background = background == null ? null : background.copy();
        g.symbolId = symbolId;
    }
}
