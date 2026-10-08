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
import com.codename1.ui.Transform;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;

import javafx.geometry.Bounds;
import javafx.scene.Node;

/// What the peers share: stripping a component of its Codename One look,
/// and painting a node with its opacity, scale and rotation.
///
/// A leaf draws through a [Renderer], which carries the node's matrix
/// itself. A parent's children are Codename One components that know
/// nothing of the matrix, so it is installed on the graphics as a native
/// transform around them, where the port supports one.
public final class PeerPaint {

    private PeerPaint() {
    }

    /// Removes everything Codename One would draw or reserve for a
    /// component of its own: background, border, padding and margin.
    public static void strip(Component c) {
        c.setUIID("FxNode");
        Style s = c.getAllStyles();
        s.setBgTransparency(0);
        s.setBgImage(null);
        s.setBorder(Border.createEmpty());
        s.setPadding(0, 0, 0, 0);
        s.setMargin(0, 0, 0, 0);
    }

    /// Returns the graphics alpha for a node drawn with an opacity.
    static int alpha(int base, double opacity) {
        double o = opacity < 0 ? 0 : (opacity > 1 ? 1 : opacity);
        return (int) Math.round(base * o);
    }

    /// Lets a transformed node paint anywhere its parent may: a rotated or
    /// scaled node is clipped by its parent, not by its own bounds.
    static void widenClip(Graphics g, Component peer) {
        Component parent = peer.getParent();
        if (parent instanceof ParentPeer) {
            int[] c = new int[4];
            if (((ParentPeer) parent).childClip(c)) {
                g.setClip(c[0], c[1], c[2], c[3]);
            }
        }
    }

    /// Installs a node's matrix on the graphics for a peer at a position;
    /// answers the transform to restore, or `null` when nothing was done.
    static Transform push(Graphics g, Node node, double[] m, int x, int y) {
        if (m == null || !g.isTransformSupported()) {
            return null;
        }
        double s = Units.scale();
        Bounds lb = node.getLayoutBounds();
        double lx = lb.getMinX();
        double ly = lb.getMinY();
        double px = -x / s + lx;
        double py = -y / s + ly;
        double tx = (m[0] * px + m[2] * py + m[4] - lx) * s + x;
        double ty = (m[1] * px + m[3] * py + m[5] - ly) * s + y;
        Transform saved = Transform.makeIdentity();
        g.getTransform(saved);
        Transform t = saved.copy();
        t.concatenate(Transform.makeAffine(m[0], m[1], m[2], m[3], tx, ty));
        g.setTransform(t);
        return saved;
    }

    /// Draws what a node draws itself, for a peer at a position.
    static void paintNode(Graphics g, Node node, int x, int y, boolean matrixInstalled) {
        Renderer r = new Renderer(g, x, y);
        Bounds lb = node.getLayoutBounds();
        r.translate(-lb.getMinX(), -lb.getMinY());
        if (!matrixInstalled) {
            double[] m = node.cn1PaintMatrix();
            if (m != null) {
                r.concat(m[0], m[1], m[2], m[3], m[4], m[5]);
            }
        }
        node.cn1Paint(r);
    }
}
