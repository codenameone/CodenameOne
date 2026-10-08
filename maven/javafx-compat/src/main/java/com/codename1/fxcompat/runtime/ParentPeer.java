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

import javafx.scene.Parent;
import javafx.scene.Scene;
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
    private boolean hasChildClip;

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

    /// The area the children may paint in, in their coordinates, recorded
    /// when this peer last painted; answers false before the first paint.
    public boolean childClip(int[] out) {
        if (!hasChildClip) {
            return false;
        }
        out[0] = childClipX;
        out[1] = childClipY;
        out[2] = childClipW;
        out[3] = childClipH;
        return true;
    }

    @Override
    public void paint(Graphics g) {
        int old = g.getAlpha();
        double opacity = node.getOpacity();
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
        boolean clipped = PeerPaint.pushClip(g, node, getX(), getY());
        // Codename One paints the children translated by this peer's origin.
        childClipX = g.getClipX() - getX();
        childClipY = g.getClipY() - getY();
        childClipW = g.getClipWidth();
        childClipH = g.getClipHeight();
        hasChildClip = true;
        Transform saved = PeerPaint.push(g, node, m, getX(), getY());
        super.paint(g);
        if (saved != null) {
            g.setTransform(saved);
        }
        if (clipped) {
            g.popClip();
        }
        if (m != null) {
            g.setClip(cx, cy, cw, ch);
        }
        g.setAlpha(old);
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
        double opacity = node.getOpacity();
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
