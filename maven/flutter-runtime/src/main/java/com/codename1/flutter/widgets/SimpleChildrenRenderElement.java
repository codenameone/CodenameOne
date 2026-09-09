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

import com.codename1.flutter.Element;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;

import dart.core.DartList;
import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * A minimal multi-child render element: it lays every (non-null) child out with
 * the loosened incoming constraints, stacks them at the origin and sizes itself
 * to the biggest child. Shared by the structural multi-child widgets that this
 * milestone renders without their full paint/positioning semantics
 * ({@link IndexedStack}, {@link Overlay}, ...). The child list is supplied lazily
 * through {@link Children} so each widget can compute (or materialize) its
 * children at build time. Owns no CN1 component.
 */
public class SimpleChildrenRenderElement extends RenderElement {

    /** Supplies the current child widgets of the owning widget. */
    public interface Children {
        DartList<Widget> get();
    }

    private final Children provider;
    private List<Element> kids = new ArrayList<Element>();

    public SimpleChildrenRenderElement(Widget widget, Children provider) {
        super(widget);
        this.provider = provider;
    }

    @Override
    protected void syncChildren() {
        List<Widget> newWidgets = new ArrayList<Widget>();
        DartList<Widget> src = provider == null ? null : provider.get();
        if (src != null) {
            for (Widget w : src) {
                if (w != null) {
                    newWidgets.add(w);
                }
            }
        }
        kids = updateChildren(kids, newWidgets);
    }

    @Override
    public void visitChildren(Funcs.VoidFunc1<Element> visitor) {
        for (Element c : kids) {
            if (c != null) {
                visitor.call(c);
            }
        }
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        List<RenderElement> rc = renderChildren();
        BoxConstraints loose = constraints.loosen();
        double maxW = 0;
        double maxH = 0;
        for (RenderElement k : rc) {
            Size cs = k.layout(loose);
            maxW = Math.max(maxW, cs.width());
            maxH = Math.max(maxH, cs.height());
            setChildOffset(k, 0, 0);
        }
        return constraints.constrain(new Size(maxW, maxH));
    }
}
