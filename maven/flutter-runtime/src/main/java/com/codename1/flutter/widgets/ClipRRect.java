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
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Clips its child with a rounded rectangle. See
 * {@link ClipRRectRenderElement}.
 */
public class ClipRRect extends Widget implements HasChild {

    private Object borderRadius;
    private Object clipper;
    private Clip clipBehavior = Clip.antiAlias;
    private Widget child;

    public void borderRadius(Object v) {
        this.borderRadius = v;
    }

    public Object getBorderRadius() {
        return borderRadius;
    }

    public Clip getClipBehavior() {
        return clipBehavior;
    }

    public void clipper(Object v) {
        this.clipper = v;
    }

    public void clipBehavior(Clip v) {
        this.clipBehavior = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget getChild() {
        return child;
    }

    /** {@code Clip.none} means do not clip, so it must not get a clipping pane. */
    @Override
    public Element createElement() {
        if (clipBehavior == Clip.none) {
            return new PassThroughRenderElement(this);
        }
        return new ClipRRectRenderElement(this);
    }
}
