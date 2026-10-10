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
 * Adds a scrollbar to a scrollable child. Codename One draws its own
 * scrollbars, so this renders the child unchanged. See
 * {@link PassThroughRenderElement}.
 */
public class Scrollbar extends Widget implements HasChild {

    private Object controller;
    private boolean thumbVisibility;
    private boolean trackVisibility;
    private double thickness;
    private Object radius;
    private boolean interactive;
    private Object notificationPredicate;
    private Object scrollbarOrientation;
    private Widget child;

    public void controller(Object v) {
        this.controller = v;
    }

    public void thumbVisibility(boolean v) {
        this.thumbVisibility = v;
    }

    /** Whether the thumb stays on screen when nothing is scrolling. */
    public boolean isThumbVisible() {
        return thumbVisibility;
    }

    public void trackVisibility(boolean v) {
        this.trackVisibility = v;
    }

    public void thickness(double v) {
        this.thickness = v;
    }

    public void radius(Object v) {
        this.radius = v;
    }

    public void interactive(boolean v) {
        this.interactive = v;
    }

    public void notificationPredicate(Object v) {
        this.notificationPredicate = v;
    }

    public void scrollbarOrientation(Object v) {
        this.scrollbarOrientation = v;
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
