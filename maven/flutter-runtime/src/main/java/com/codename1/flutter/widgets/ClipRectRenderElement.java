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
package com.codename1.flutter.widgets;

import com.codename1.flutter.Clip;
import com.codename1.flutter.Widget;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;

/**
 * Clips its subtree to its own bounds, which is what makes an expand/collapse animation
 * read as one: {@code ClipRect(child: Align(heightFactor: t, child: ...))} shrinks the box
 * while the child keeps its full size, and everything past the box has to disappear.
 *
 * <p>The clip itself costs nothing to apply. Codename One already confines a component's
 * paint - its own and its children's - to its bounds before calling
 * {@code paint} (see {@code Component.internalPaintImpl}). The work is getting the subtree
 * to be that component's children at all: the render tree is otherwise flat, every element
 * absolutely positioned as a sibling in one host. {@link EffectRenderElement} supplies the
 * nested pane that makes the subtree genuinely nested, so this element only has to exist,
 * not to paint anything special.</p>
 */
public class ClipRectRenderElement extends EffectRenderElement {


    public ClipRectRenderElement(Widget widget) {
        super(widget);
    }

    @Override
    protected Widget effectChild() {
        return ((HasChild) widget()).getChild();
    }

    /** The clip mode this widget asks for, defaulting to a hard edge as Flutter's ClipRect does. */
    private Clip behavior() {
        Widget w = widget();
        Clip c = w instanceof ClipRect ? ((ClipRect) w).getClipBehavior() : null;
        return c == null ? Clip.hardEdge : c;
    }

    @Override
    protected void paintWithEffect(Graphics g, Container pane, Subtree paintChildren) {
        // Clip.none never reaches here: ClipRect gives it a pass-through element instead,
        // since this element's pane clips whatever the behaviour asks for.
        paintChildren.paint(g);
    }
}
