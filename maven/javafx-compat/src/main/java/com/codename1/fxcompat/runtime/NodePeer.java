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

import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;

import javafx.scene.Node;

/// The Codename One component behind a node that draws itself: a shape, a
/// canvas, text. It has no look of its own; painting is the node's
/// `cn1Paint`. Pointer events pass through it to the form, which is where
/// the scene picks its own target.
public class NodePeer extends Component implements FxPeer {

    private final Node node;

    /// Creates the peer of a node.
    public NodePeer(Node node) {
        this.node = node;
        PeerPaint.strip(this);
        setFocusable(false);
    }

    @Override
    public Node node() {
        return node;
    }

    @Override
    public boolean isIgnorePointerEvents() {
        return true;
    }

    @Override
    public void paint(Graphics g) {
        int old = g.getAlpha();
        double opacity = node.getOpacity();
        if (opacity < 1) {
            g.setAlpha(PeerPaint.alpha(old, opacity));
        }
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        boolean widened = node.cn1PaintMatrix() != null || node.cn1PaintsOutsideBounds();
        if (widened) {
            PeerPaint.widenClip(g, this);
        }
        PeerPaint.paintNode(g, node, getX(), getY(), false);
        if (widened) {
            g.setClip(cx, cy, cw, ch);
        }
        g.setAlpha(old);
    }

    @Override
    protected void paintBackground(Graphics g) {
    }

    @Override
    protected Dimension calcPreferredSize() {
        return new Dimension(Units.sizeToPixels(node.prefWidth(-1)), Units.sizeToPixels(node.prefHeight(-1)));
    }
}
