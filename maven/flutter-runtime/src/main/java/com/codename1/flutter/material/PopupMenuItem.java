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
package com.codename1.flutter.material;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.HasChild;
import com.codename1.flutter.widgets.PassThroughRenderElement;

import dart.runtime.Funcs;

/**
 * An item in a {@link PopupMenuButton}'s menu — Flutter's
 * {@code PopupMenuItem}. Carries the selection {@code value} and a child
 * widget. Rendered as its child when materialized (the menu presentation is
 * deferred). See {@link PassThroughRenderElement}.
 */
public class PopupMenuItem<T> extends PopupMenuEntry<T> implements HasChild {

    private Object value;
    private boolean enabled = true;
    private double height = 48;
    private Object padding;
    private Object textStyle;
    private Object mouseCursor;
    private Funcs.VoidFunc0 onTap;
    private Widget child;

    public void value(Object v) {
        this.value = v;
    }

    public void enabled(boolean v) {
        this.enabled = v;
    }

    public void height(double v) {
        this.height = v;
    }

    public void padding(Object v) {
        this.padding = v;
    }

    public void textStyle(Object v) {
        this.textStyle = v;
    }

    public void mouseCursor(Object v) {
        this.mouseCursor = v;
    }

    public void onTap(Funcs.VoidFunc0 v) {
        this.onTap = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Object getValue() {
        return value;
    }

    public Funcs.VoidFunc0 getOnTap() {
        return onTap;
    }

    public boolean isEnabled() {
        return enabled;
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
