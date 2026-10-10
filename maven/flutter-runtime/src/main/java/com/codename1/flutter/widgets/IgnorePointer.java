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
import com.codename1.flutter.Widget;

/**
 * Prevents its subtree from receiving pointer events — Flutter's {@code IgnorePointer}.
 *
 * <p>The child renders unchanged and its subtree is made deaf to touch, so a handler
 * ABOVE this widget receives a press that lands on top of the child -- which is the whole
 * point of it. See {@link IgnorePointerRenderElement}.</p>
 */
public class IgnorePointer extends Widget implements HasChild {

    private Boolean ignoring;
    private Boolean ignoringSemantics;
    private Widget child;

    public void ignoring(Boolean v) { this.ignoring = v; }

    /// Whether the subtree is deaf to touch. Null means the default, which is true.
    ///
    /// #### Returns
    ///
    /// the ignoring flag as given, or null when it was never set
    public Boolean getIgnoring() {
        return ignoring;
    }
    public void ignoringSemantics(Boolean v) { this.ignoringSemantics = v; }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new IgnorePointerRenderElement(this);
    }
}
