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

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;

import java.util.List;

/**
 * Lays out every child but shows only the one at {@code index} — Flutter's
 * {@code IndexedStack}.
 *
 * <p>All of them used to be laid out AND painted, at the same origin. Crane's
 * back layer is an {@code IndexedStack} over the Fly / Sleep / Eat forms, so
 * the study came up with all three printed on top of each other: "Travelers"
 * over "Diners", "Choose Origin" over "Select Location".</p>
 *
 * <p>The unselected children stay in the tree — that is the whole point of an
 * IndexedStack over a conditional child, and what keeps a hidden form's state
 * alive — but they are laid out against a zero-size constraint, which is how a
 * subtree renders nothing here without being torn down.</p>
 */
public class IndexedStackRenderElement extends SimpleChildrenRenderElement {

    public IndexedStackRenderElement(Widget widget, Children provider) {
        super(widget, provider);
    }

    /** The child to show; out-of-range indices show nothing, as in Flutter. */
    private int selected() {
        Widget w = widget();
        return w instanceof IndexedStack ? (int) ((IndexedStack) w).getIndex() : 0;
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        List<RenderElement> rc = renderChildren();
        int idx = selected();
        BoxConstraints loose = constraints.loosen();
        BoxConstraints none = BoxConstraints.tight(0, 0);
        Size shown = new Size(0, 0);
        for (int i = 0; i < rc.size(); i++) {
            RenderElement k = rc.get(i);
            Size cs = k.layout(i == idx ? loose : none);
            if (i == idx) {
                shown = cs;
            }
            setChildOffset(k, 0, 0);
        }
        return constraints.constrain(shown);
    }
}
