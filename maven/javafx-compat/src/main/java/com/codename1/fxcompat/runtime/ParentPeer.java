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
package com.codename1.fxcompat.runtime;

import com.codename1.ui.Container;
import com.codename1.ui.Graphics;
import com.codename1.ui.Transform;
import com.codename1.ui.events.WheelEvent;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;

import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;

/// The Codename One container behind a parent node. It holds the peers of
/// the children, draws what the parent draws itself (a region's
/// background and border) below them, and applies the parent's opacity,
/// scale and rotation to all of it.
///
/// Its layout manager runs JavaFX layout, and only in the peer of a root:
/// that pass places the whole tree, and every node pushes its own bounds
/// into its peer as it is placed.
public class ParentPeer extends Container implements FxPeer {

    private final Parent node;
    private int childClipX;
    private int childClipY;
    private int childClipW;
    private int childClipH;
    private int painting;

    /// Creates the peer of a parent.
    public ParentPeer(Parent node) {
        super(new Bridge());
        this.node = node;
        PeerPaint.strip(this);
        setScrollable(false);
        setFocusable(false);
    }

    @Override
    public Parent node() {
        return node;
    }

    /// The area the children may paint in, in their coordinates; answers
    /// false when there is none to give.
    ///
    /// While this peer paints its children it is the clip it painted them
    /// with. That clip must not be kept for later, though. Codename One
    /// also paints a peer on its own -- the one that asked to be painted
    /// again -- and paints the peers lying over it clipped to that one's
    /// area; a clip recorded during such a pass is that small area, and a
    /// pane painted on its own afterwards took it for all it may draw in
    /// and drew nothing. So outside a paint of this peer the area is
    /// worked out from where the peers are.
    public boolean childClip(int[] out) {
        if (painting > 0) {
            out[0] = childClipX;
            out[1] = childClipY;
            out[2] = childClipW;
            out[3] = childClipH;
            return true;
        }
        return allowed(out);
    }

    /// The area the children of this peer may ever paint in: its own
    /// bounds where it cuts them off, and whatever its parent allows
    /// where it does not. Answers false under a transform or a clip of
    /// the application's, neither of which is a rectangle of the screen;
    /// what asks then keeps the clip it has.
    private boolean allowed(int[] out) {
        if (node.cn1PaintMatrix() != null || node.cn1ClipPath() != null) {
            return false;
        }
        Container above = getParent();
        if (!(above instanceof ParentPeer)) {
            out[0] = 0;
            out[1] = 0;
            out[2] = getWidth();
            out[3] = getHeight();
            return true;
        }
        if (!((ParentPeer) above).allowed(out)) {
            return false;
        }
        out[0] -= getX();
        out[1] -= getY();
        if (clipsChildren(node)) {
            int x1 = Math.max(0, out[0]);
            int y1 = Math.max(0, out[1]);
            int x2 = Math.min(getWidth(), out[0] + out[2]);
            int y2 = Math.min(getHeight(), out[1] + out[3]);
            out[0] = x1;
            out[1] = y1;
            out[2] = Math.max(0, x2 - x1);
            out[3] = Math.max(0, y2 - y1);
        }
        return true;
    }

    /// Returns whether what is in a parent is cut off at its bounds. A
    /// pane and a group draw their children whole wherever they are; a
    /// control, whose skin holds a viewport, does not.
    public static boolean clipsChildren(Parent parent) {
        return !(parent instanceof Pane || parent instanceof Group);
    }

    /// Returns whether a parent above this one is scaled, rotated or
    /// otherwise transformed.
    public static boolean underTransform(Parent parent) {
        for (Parent above = parent.getParent(); above != null; above = above.getParent()) {
            if (above.cn1PaintMatrix() != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void paint(Graphics g) {
        if (PeerPaint.unseen(node)) {
            return;
        }
        int old = g.getAlpha();
        double opacity = PeerPaint.opacity(node);
        if (opacity < 1) {
            g.setAlpha(PeerPaint.alpha(old, opacity));
        }
        double[] m = node.cn1PaintMatrix();
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        // A pane or a group does not clip what is in it: a child larger
        // than the pane, or placed beyond its edge, is drawn whole. Codename
        // One clips a container to its bounds, so the clip is the parent's
        // again. A control keeps its own, which is what holds the content
        // of a scroll pane or a list inside it.
        //
        // Not under a scale or a rotation, though: the clip recorded by
        // the parent is in the coordinates of the screen, and under a
        // transform the graphics reads it in the transformed ones.
        boolean widened = m != null || (!clipsChildren(node) && !underTransform(node));
        if (widened) {
            PeerPaint.widenClip(g, this);
        }
        boolean clipped = PeerPaint.pushClip(g, node, getX(), getY());
        // Codename One paints the children translated by this peer's origin.
        childClipX = g.getClipX() - getX();
        childClipY = g.getClipY() - getY();
        childClipW = g.getClipWidth();
        childClipH = g.getClipHeight();
        Transform saved = PeerPaint.push(g, node, m, getX(), getY());
        painting++;
        super.paint(g);
        painting--;
        if (saved != null) {
            PeerPaint.setDeviceTransform(g, saved);
        }
        if (clipped) {
            g.popClip();
        }
        if (widened) {
            g.setClip(cx, cy, cw, ch);
        }
        g.setAlpha(old);
        if (Effects.has(node)) {
            Effects.paintOver(g, node, this);
        }
    }

    /// Paints the node and everything in it into a picture of its own,
    /// for [Effects]: what Codename One paints for a container, without
    /// the backgrounds of what is above it.
    void capture(Graphics g) {
        paintBackground(g);
        paint(g);
    }

    @Override
    protected void paintBackground(Graphics g) {
        int old = g.getAlpha();
        Scene scene = node.getScene();
        if (node.getParent() == null && scene != null) {
            Paint fill = scene.getFill();
            if (fill != null) {
                new Renderer(g, getX(), getY()).fillRect(0, 0, Units.toLogical(getWidth()),
                        Units.toLogical(getHeight()), fill);
            }
        }
        if (PeerPaint.unseen(node)) {
            return;
        }
        if (Effects.has(node)) {
            Effects.paintBehind(g, node, this);
        }
        double opacity = PeerPaint.opacity(node);
        if (opacity < 1) {
            g.setAlpha(PeerPaint.alpha(old, opacity));
        }
        double[] m = node.cn1PaintMatrix();
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        if (m != null) {
            PeerPaint.widenClip(g, this);
        }
        PeerPaint.paintNode(g, node, getX(), getY(), false);
        if (m != null) {
            g.setClip(cx, cy, cw, ch);
        }
        g.setAlpha(old);
    }

    @Override
    protected Dimension calcPreferredSize() {
        return new Dimension(Units.sizeToPixels(node.prefWidth(-1)), Units.sizeToPixels(node.prefHeight(-1)));
    }

    @Override
    protected boolean mouseWheel(WheelEvent ev) {
        if (node.getParent() == null && node.getScene() != null && SceneInput.wheel(node.getScene(), this,
                ev.getX(), ev.getY(), ev.getDeltaX(), ev.getDeltaY())) {
            ev.consume();
            return true;
        }
        return super.mouseWheel(ev);
    }

    /// Runs JavaFX layout from the peer of a root.
    static final class Bridge extends Layout {

        @Override
        public void layoutContainer(Container parent) {
            if (!(parent instanceof ParentPeer)) {
                return;
            }
            Parent node = ((ParentPeer) parent).node;
            if (node.getParent() != null) {
                return;
            }
            int w = parent.getWidth();
            int h = parent.getHeight();
            if (w <= 0 && h <= 0) {
                return;
            }
            Scene scene = node.getScene();
            if (scene != null) {
                scene.cn1Layout(Units.toLogical(w), Units.toLogical(h));
            } else {
                node.resize(Units.toLogical(w), Units.toLogical(h));
                node.layout();
            }
        }

        @Override
        public Dimension getPreferredSize(Container parent) {
            if (!(parent instanceof ParentPeer)) {
                return new Dimension(0, 0);
            }
            Parent node = ((ParentPeer) parent).node;
            return new Dimension(Units.sizeToPixels(node.prefWidth(-1)), Units.sizeToPixels(node.prefHeight(-1)));
        }
    }
}
