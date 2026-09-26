/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

package com.codename1.flutter.widgets;

import com.codename1.flutter.Widget;
import com.codename1.ui.Component;
import com.codename1.ui.Container;

/**
 * Renders {@link IgnorePointer}'s child and makes that subtree deaf to touch.
 *
 * <p>The widget used to be a pure structural pass-through, which renders the right thing
 * and is not what the widget is FOR: it exists so that something above it can receive a
 * touch that lands on top of its child. With the child still live, whatever it contains
 * answers first and the handler above never runs.</p>
 *
 * <p>That is how the gallery's splash screen became a trap. The splash slides the home
 * page down and leaves a strip of it showing; the strip is wrapped in an IgnorePointer
 * inside a gesture detector whose tap dismisses the splash, so the ONLY way back is a
 * touch that passes through the strip. Ignoring nothing meant the strip's own rows
 * answered the tap, the dismiss never fired, and the screen could be entered and not
 * left.</p>
 *
 * <p>Codename One's hit test walks UP from the component it lands on for as long as each
 * one ignores pointer events, so the flag has to be set across the whole subtree rather
 * than on its root: a touch landing on a deep child would otherwise stop there.</p>
 */
public class IgnorePointerRenderElement extends PassThroughRenderElement {

    public IgnorePointerRenderElement(Widget widget) {
        super(widget);
    }

    /** {@code IgnorePointer.ignoring} defaults to true, as Flutter's does. */
    private boolean ignoring() {
        Boolean v = ((IgnorePointer) widget()).getIgnoring();
        return v == null || v.booleanValue();
    }

    @Override
    protected void positionChildren(int x, int y) {
        super.positionChildren(x, y);
        // After the child is placed, so the components it owns exist. A wrapper owns no
        // component of its own, so the search descends until it finds the ones that do.
        applyToRenderSubtree(this, ignoring());
    }

    private static void applyToRenderSubtree(com.codename1.flutter.RenderElement e,
            boolean deaf) {
        for (com.codename1.flutter.RenderElement child : e.renderChildren()) {
            Component c = child.component();
            if (c != null) {
                apply(c, deaf);
            } else {
                applyToRenderSubtree(child, deaf);
            }
        }
    }

    private static void apply(Component c, boolean deaf) {
        c.setIgnorePointerEvents(deaf);
        if (c instanceof Container) {
            Container g = (Container) c;
            int n = g.getComponentCount();
            for (int i = 0; i < n; i++) {
                apply(g.getComponentAt(i), deaf);
            }
        }
    }
}
