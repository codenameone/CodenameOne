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
 * Scales and positions its {@code child} within itself — Flutter's {@code FittedBox}.
 *
 * <p>Structural pass-through for this milestone: the single {@code child}
 * renders unchanged (see {@link PassThroughRenderElement}); the captured
 * parameters are held for a later render pass.</p>
 */
public class FittedBox extends Widget implements HasChild {

    private Object fit;
    private Object alignment;
    private Object clipBehavior;
    private Widget child;

    public void fit(Object v) { this.fit = v; }
    public void alignment(Object v) { this.alignment = v; }
    public void clipBehavior(Object v) { this.clipBehavior = v; }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget getChild() {
        return child;
    }

    /** How the child is scaled into the box; Flutter defaults to {@code contain}. */
    public Object getFit() {
        return fit;
    }

    /** Where the scaled child sits in the box; Flutter defaults to the centre. */
    public Object getAlignment() {
        return alignment;
    }

    @Override
    public Element createElement() {
        return new FittedBoxRenderElement(this);
    }
}
