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

import com.codename1.flutter.Widget;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;

/**
 * Paints {@link Opacity}'s subtree at its opacity, by compositing the whole nested pane
 * through the Graphics alpha rather than tinting components individually — so overlapping
 * children fade as one layer, the way Flutter's Opacity behaves.
 */
public class OpacityRenderElement extends EffectRenderElement {

    public OpacityRenderElement(Opacity widget) {
        super(widget);
    }

    private Opacity opacity() {
        return (Opacity) widget();
    }

    @Override
    protected Widget effectChild() {
        return opacity().getChild();
    }

    @Override
    protected void paintWithEffect(Graphics g, Container pane, Subtree paintChildren) {
        double o = opacity().getOpacity();
        if (o >= 1.0) {
            paintChildren.paint(g);
            return;
        }
        if (o <= 0.0) {
            return;   // fully transparent: painting anything would be wrong
        }
        int previous = g.getAlpha();
        // Compose with the alpha already in effect, so nested Opacity multiplies.
        g.setAlpha((int) Math.round(previous * o));
        try {
            paintChildren.paint(g);
        } finally {
            g.setAlpha(previous);
        }
    }
}
