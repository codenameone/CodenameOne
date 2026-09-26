/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.rendering;

import com.codename1.flutter.Offset;
import com.codename1.flutter.rendering.Dp;

/**
 * A render object laid out with the box protocol (a Cartesian size) — Flutter's
 * {@code RenderBox}.
 *
 * <p>Backed by the live layout when it is obtained from
 * {@code BuildContext.findRenderObject()}: the size and the global position
 * come from the Codename One component the element owns, converted to the
 * LOGICAL pixels a Flutter caller expects.
 *
 * <p>It used to answer zero and the identity mapping, which is worse than
 * unimplemented — a caller positioning something by these numbers puts it in
 * the top-left corner with no indication anything went wrong. The gallery's
 * feature-discovery highlight centres itself this way.
 */
public class RenderBox extends RenderObject {

    private Size size = Size.ZERO;
    /** The element this box reports for, when it was obtained from the live tree. */
    private com.codename1.flutter.RenderElement element;

    public RenderBox() {
    }

    public RenderBox(com.codename1.flutter.RenderElement element) {
        this.element = element;
    }

    /** The size of this box after layout, in logical pixels. */
    public Size size() {
        if (element == null) {
            return size;
        }
        double scale = scale();
        Size s = element.size();
        return s == null ? Size.ZERO : new Size(s.width() / scale, s.height() / scale);
    }

    /** Named setter used by the runtime once layout is known. */
    public void size(Size v) {
        this.size = v == null ? Size.ZERO : v;
    }

    /** Whether this box has been through layout and has a valid size. */
    public boolean hasSize() {
        return size() != null;
    }

    /**
     * Converts a point from this box's local coordinate space to the global
     * (screen) space, optionally relative to {@code ancestor}.
     */
    public Offset localToGlobal(Offset point, RenderObject ancestor) {
        Offset p = point == null ? Offset.zero : point;
        if (element == null) {
            return p;
        }
        // The layout pass writes every box's position absolutely within the
        // host, so the element already knows where it is; no component needed
        // (many elements own none).
        //
        // ...but "within the host" is not the screen when the subtree lives in a
        // host of its own. A root Scaffold's app bar is hosted in the Codename
        // One Toolbar, so its boxes are positioned from the toolbar's origin,
        // and anything asking where it is on SCREEN was told where it is inside
        // the bar. The gallery's feature discovery asks exactly that to place
        // its coach mark: the circle came out low enough to change which branch
        // of its own radius rule it took, and so the wrong size as well as the
        // wrong place. Adding the host container's absolute position makes the
        // answer global, which is what the name says.
        double scale = scale();
        // The host's own origin is NOT added here.
        //
        // It was, on the theory that a Toolbar-hosted subtree reports positions
        // from the bar's origin rather than the screen's. Measured on the
        // device, that double-counted: the feature discovery's centre came back
        // 162 logical pixels below the icon it was asking about, which then also
        // put its background circle in the wrong size bracket. The layout pass
        // already writes absolute positions within the form, so the element's
        // own x/y are the global answer.
        return new Offset(p.dx() + element.x() / scale,
                p.dy() + element.y() / scale);
    }

    /**
     * Converts a point from global (screen) space to this box's local space,
     * optionally relative to {@code ancestor}.
     */
    public Offset globalToLocal(Offset point, RenderObject ancestor) {
        Offset p = point == null ? Offset.zero : point;
        if (element == null) {
            return p;
        }
        double scale = scale();
        return new Offset(p.dx() - element.x() / scale, p.dy() - element.y() / scale);
    }

    private static double scale() {
        double s = Dp.scale();
        return s <= 0 ? 1 : s;
    }

    /**
     * The transform mapping this object's coordinate space to {@code ancestor}
     * (a 4x4 matrix in Flutter). Returned opaque for this milestone.
     */
    public Object getTransformTo(RenderObject ancestor) {
        return null;
    }
}
