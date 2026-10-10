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
package com.codename1.flutter.provider;

import com.codename1.flutter.StatelessElement;
import com.codename1.flutter.Widget;

/**
 * The element of a {@link Selector}: rebuilds its subtree only when the selected slice
 * changed, or when it was given a new Selector configuration -- provider's rule. The
 * last selection and the child built for it are kept here, per mount point.
 */
public class SelectorElement<A, S> extends StatelessElement {

    private boolean built;
    private Widget builtFor;
    private S lastSelected;
    private Widget lastChild;

    public SelectorElement(Selector<A, S> widget) {
        super(widget);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Widget build() {
        Selector<A, S> w = (Selector<A, S>) widget();
        S selected = w.select(this);
        if (built && builtFor == w && !w.changed(lastSelected, selected)) {
            // Handing back the same child instance lets reconciliation skip the subtree.
            return lastChild;
        }
        Widget out = w.buildFor(this, selected);
        built = true;
        builtFor = w;
        lastSelected = selected;
        lastChild = out;
        return out;
    }
}
