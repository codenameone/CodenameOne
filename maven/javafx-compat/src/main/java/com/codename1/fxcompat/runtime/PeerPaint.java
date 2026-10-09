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

    /// Whether nothing of a node can be seen: it is fully transparent, or
    /// its matrix leaves it no area. The peers then paint nothing at all.
    /// A native component under a transparent node is the reason this is
    /// asked rather than left to the alpha: a theme may draw its text with
    /// an alpha of its own, and a label faded out to nothing stayed.
    static boolean unseen(Node node) {
        return !(node.getOpacity() > 0) || collapsed(node.cn1PaintMatrix());
    }

    /// Whether a node's matrix maps everything onto a line or a point, as a
    /// scale of zero does -- the first frame of a node that grows into view.
    /// Nothing of such a node is visible, and a matrix that cannot be
    /// inverted leaves a port without a clip to answer, so the peers paint
    /// nothing rather than install it.
    static boolean collapsed(double[] m) {
        if (m == null) {
            return false;
        }
        double det = m[0] * m[3] - m[1] * m[2];
        return !(det > 1e-12 || det < -1e-12);
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
        Bounds lb = node.getLayoutBounds();
        double[] d = deviceMatrix(m, lb.getMinX(), lb.getMinY(), Units.scale(), g.getTranslateX() + x,
                g.getTranslateY() + y);
        Transform saved = Transform.makeIdentity();
        g.getTransform(saved);
        Transform t = saved.copy();
        t.concatenate(Transform.makeAffine(d[0], d[1], d[2], d[3], d[4], d[5]));
        setDeviceTransform(g, t);
        return saved;
    }

    /// A node's matrix, which is in logical units about the origin of its
    /// layout bounds `(lx, ly)`, as the matrix in pixels for a peer whose
    /// origin is drawn at `(ox, oy)`: a point of the peer stays where the
    /// node's matrix puts it, whatever the peer's position.
    public static double[] deviceMatrix(double[] m, double lx, double ly, double scale, double ox, double oy) {
        double px = -ox / scale + lx;
        double py = -oy / scale + ly;
        return new double[] {m[0], m[1], m[2], m[3],
            (m[0] * px + m[2] * py + m[4] - lx) * scale + ox,
            (m[1] * px + m[3] * py + m[5] - ly) * scale + oy};
    }

    /// Sets a transform that acts on what is drawn where it is drawn.
    ///
    /// `Graphics.setTransform` called under a translation keeps the matrix
    /// as one about the translated origin, and moves it along with every
    /// later `translate`. A matrix installed for a parent would then scale
    /// each child about the child's own corner, and a matrix read back and
    /// restored further down the tree would be re-based on that place: the
    /// siblings painted after a scaled node landed somewhere else. Set at
    /// no translation, the matrix is the port's own and stays put.
    static void setDeviceTransform(Graphics g, Transform t) {
        int tx = g.getTranslateX();
        int ty = g.getTranslateY();
        if (tx == 0 && ty == 0) {
            g.setTransform(t);
            return;
        }
        g.translate(-tx, -ty);
        g.setTransform(t);
        g.translate(tx, ty);
    }

    private static Renderer renderer(Graphics g, Node node, int x, int y, boolean matrixInstalled) {
        Renderer r = new Renderer(g, x, y);
        Bounds lb = node.getLayoutBounds();
        r.translate(-lb.getMinX(), -lb.getMinY());
        if (!matrixInstalled) {
            double[] m = node.cn1PaintMatrix();
            if (m != null) {
                r.concat(m[0], m[1], m[2], m[3], m[4], m[5]);
            }
        }
        return r;
    }

    /// Restricts the graphics to the clip of a node, for a peer at a
    /// position; answers whether a clip was pushed, which the caller
    /// then pops with `Graphics.popClip()`. The clip is pushed after any
    /// widening, so it is the node's clip that holds for its children.
    static boolean pushClip(Graphics g, Node node, int x, int y) {
        FxPath clip = node.cn1ClipPath();
        if (clip == null) {
            return false;
        }
        renderer(g, node, x, y, false).clip(clip);
        return true;
    }

    /// Draws what a node draws itself, for a peer at a position, inside
    /// the node's clip if it has one.
    static void paintNode(Graphics g, Node node, int x, int y, boolean matrixInstalled) {
        Renderer r = renderer(g, node, x, y, matrixInstalled);
        FxPath clip = node.cn1ClipPath();
        if (clip != null) {
            r.save();
            r.clip(clip);
        }
        node.cn1Paint(r);
        if (clip != null) {
            r.restore();
        }
    }
}
