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
import javafx.scene.control.Control;

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

    /// Every style a native component is drawn with, as one: what is set
    /// on the answer is set on each.
    ///
    /// `Component.getAllStyles()` is the styles at rest, selected, pressed
    /// and disabled, and leaves out the one a theme may declare for a
    /// component under the pointer. A font or a colour set through it was
    /// therefore not the font or the colour of a hovered control: its
    /// text changed size where it was sized for the font at rest, and the
    /// last letter was cut off for as long as the pointer stayed.
    public static Style allStyles(Component c) {
        Style hover = c.getHoverStyle();
        if (hover == null) {
            return c.getAllStyles();
        }
        return Style.createProxyStyle(c.getUnselectedStyle(), c.getSelectedStyle(), c.getPressedStyle(),
                c.getDisabledStyle(), hover);
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

    /// The opacity JavaFX draws a disabled control with.
    public static final double DISABLED_OPACITY = 0.4;

    /// Returns the opacity a parent is painted with: its own, and for a
    /// disabled control the dimming JavaFX gives one. The outermost
    /// disabled control dims everything in it, the controls inside
    /// included, so those do not dim a second time.
    public static double opacity(Node node) {
        double o = node.getOpacity();
        if (node instanceof Control && node.isDisabled()) {
            for (Node up = node.getParent(); up != null; up = up.getParent()) {
                if (up instanceof Control && up.isDisabled()) {
                    return o;
                }
            }
            return o * DISABLED_OPACITY;
        }
        return o;
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

    /// Whether the port keeps its clip in the coordinates of the screen
    /// while a matrix is installed, asked right after one was: `before` is
    /// the transform that was replaced and the rectangle is the clip read
    /// under it.
    ///
    /// A port either answers the clip in the coordinates drawing is given
    /// in, so that the numbers change with the matrix, or in those of the
    /// screen, so that they do not. Only the second kind needs
    /// [#includeChildren]; widening on the first would let what is inside a
    /// viewport paint outside it. Unchanged numbers under a matrix that
    /// moves the rectangle are what tells the two apart, and a matrix that
    /// leaves the rectangle where it was needs nothing on either.
    static boolean clipStaysOnScreen(Graphics g, Transform before, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0 || g.getClipX() != x || g.getClipY() != y || g.getClipWidth() != w
                || g.getClipHeight() != h) {
            return false;
        }
        Transform now = g.getTransform();
        int ox = g.getTranslateX() + x;
        int oy = g.getTranslateY() + y;
        float[] in = new float[2];
        float[] a = new float[2];
        float[] b = new float[2];
        for (int i = 0; i < 4; i++) {
            in[0] = ox + ((i & 1) == 0 ? 0 : w);
            in[1] = oy + ((i & 2) == 0 ? 0 : h);
            before.transformPoint(in, a);
            now.transformPoint(in, b);
            if (Math.abs(a[0] - b[0]) > 1 || Math.abs(a[1] - b[1]) > 1) {
                return true;
            }
        }
        return false;
    }

    /// How many parents are painting their children under a matrix on a
    /// port that keeps its clip in the coordinates of the screen. Painting
    /// is on one thread.
    private static int screenClipDepth;

    /// Whether a parent further up is painting under such a matrix: the
    /// parents below it widen their clip as well, since theirs is in the
    /// same two spaces.
    static boolean underScreenClip() {
        return screenClipDepth > 0;
    }

    /// Whether this graphics is one that such a parent is painting on, with
    /// a matrix installed: a clip read from it is not in the coordinates
    /// drawing is given in. An image painted on in the meantime, with no
    /// matrix of its own, is not.
    static boolean screenClipUnderMatrix(Graphics g) {
        return screenClipDepth > 0 && g.isTransformSupported() && !g.getTransform().isIdentity();
    }

    /// Enters (1) or leaves (-1) a parent that widened its clip.
    static void screenClip(int by) {
        screenClipDepth += by;
    }

    /// Widens the clip to hold every child of a peer about to paint them;
    /// the caller restores the clip afterwards. See [#clipStaysOnScreen].
    static void includeChildren(Graphics g, com.codename1.ui.Container peer) {
        int x1 = g.getClipX();
        int y1 = g.getClipY();
        int x2 = x1 + g.getClipWidth();
        int y2 = y1 + g.getClipHeight();
        if (x2 <= x1 || y2 <= y1) {
            return;
        }
        int ox = peer.getX() - peer.getScrollX();
        int oy = peer.getY() - peer.getScrollY();
        int n = peer.getComponentCount();
        for (int i = 0; i < n; i++) {
            Component c = peer.getComponentAt(i);
            x1 = Math.min(x1, ox + c.getX());
            y1 = Math.min(y1, oy + c.getY());
            x2 = Math.max(x2, ox + c.getX() + c.getWidth());
            y2 = Math.max(y2, oy + c.getY() + c.getHeight());
        }
        g.setClip(x1, y1, x2 - x1, y2 - y1);
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
