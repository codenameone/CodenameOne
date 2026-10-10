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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A widget that can be dismissed by dragging — Flutter's {@code Dismissible}.
 * This milestone renders the {@code child}; the swipe-to-dismiss gesture, the
 * reveal of {@code background}/{@code secondaryBackground} and the resize
 * animation are deferred. {@code onDismissed} carries a {@link DismissDirection}.
 */
public class Dismissible extends Widget {

    private Widget child;
    private Widget background;
    private Widget secondaryBackground;
    private Funcs.VoidFunc1<DismissDirection> onDismissed;
    private Funcs.Func1<DismissDirection, Object> confirmDismiss;
    private Funcs.VoidFunc0 onResize;
    private Object direction;
    private Object dismissThresholds;

    public void child(Widget v) {
        this.child = v;
    }

    public void background(Widget v) {
        this.background = v;
    }

    public void secondaryBackground(Widget v) {
        this.secondaryBackground = v;
    }

    public void confirmDismiss(Funcs.Func1<DismissDirection, Object> v) {
        this.confirmDismiss = v;
    }

    public void onResize(Funcs.VoidFunc0 v) {
        this.onResize = v;
    }

    public void onUpdate(Object v) {
    }

    public void onDismissed(Funcs.VoidFunc1<DismissDirection> v) {
        this.onDismissed = v;
    }

    public void direction(Object v) {
        this.direction = v;
    }

    public void resizeDuration(Object v) {
    }

    public void dismissThresholds(Object v) {
        this.dismissThresholds = v;
    }

    public void movementDuration(Object v) {
    }

    public void crossAxisEndOffset(double v) {
    }

    public void dragStartBehavior(Object v) {
    }

    public void behavior(Object v) {
    }

    public Widget getChild() {
        return child;
    }

    public Widget getBackground() {
        return background;
    }

    public Widget getSecondaryBackground() {
        return secondaryBackground;
    }

    public Funcs.VoidFunc1<DismissDirection> getOnDismissed() {
        return onDismissed;
    }

    public Funcs.Func1<DismissDirection, Object> getConfirmDismiss() {
        return confirmDismiss;
    }

    public Object getDismissThresholds() {
        return dismissThresholds;
    }

    public Object getDirection() {
        return direction;
    }

    @Override
    public com.codename1.flutter.Element createElement() {
        return new DismissibleRenderElement(this);
    }
}
