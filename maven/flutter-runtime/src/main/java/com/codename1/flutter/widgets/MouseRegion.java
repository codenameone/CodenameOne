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
 * Tracks the pointer as it enters/exits/moves over its child. Pointer hover is
 * a no-op on touch targets; the child renders unchanged. See
 * {@link PassThroughRenderElement}.
 */
public class MouseRegion extends Widget implements HasChild {

    private Object cursor;
    private boolean opaque = true;
    private Object onEnter;
    private Object onExit;
    private Object onHover;
    private Object hitTestBehavior;
    private Widget child;

    public void cursor(Object v) {
        this.cursor = v;
    }

    public void opaque(boolean v) {
        this.opaque = v;
    }

    public void onEnter(Object v) {
        this.onEnter = v;
    }

    public void onExit(Object v) {
        this.onExit = v;
    }

    public void onHover(Object v) {
        this.onHover = v;
    }

    public void hitTestBehavior(Object v) {
        this.hitTestBehavior = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new PassThroughRenderElement(this);
    }
}
