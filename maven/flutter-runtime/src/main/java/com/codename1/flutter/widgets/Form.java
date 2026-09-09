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
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Groups form fields that validate/save together — Flutter's {@code Form}. A
 * {@link com.codename1.flutter.GlobalKey}{@code <FormState>} attached to the
 * form gives access to the {@link FormState} that drives
 * validate/save/reset across the fields. Structural pass-through for this
 * milestone: the {@code child} renders unchanged.
 */
public class Form extends Widget implements HasChild {

    private Widget child;
    private Object onChanged;
    private Object onWillPop;
    private Object canPop;
    private Object onPopInvoked;
    private Object autovalidateMode;

    public void child(Widget v) {
        this.child = v;
    }

    public void onChanged(Object v) {
        this.onChanged = v;
    }

    public void onWillPop(Object v) {
        this.onWillPop = v;
    }

    public void canPop(Object v) {
        this.canPop = v;
    }

    public void onPopInvoked(Object v) {
        this.onPopInvoked = v;
    }

    public void autovalidateMode(Object v) {
        this.autovalidateMode = v;
    }

    @Override
    public Widget getChild() {
        return child;
    }

    /** Flutter's {@code Form.of} — the nearest enclosing {@link FormState}. */
    public static FormState of(BuildContext context) {
        return null;
    }

    /** Flutter's {@code Form.maybeOf}. */
    public static FormState maybeOf(BuildContext context) {
        return null;
    }

    @Override
    public Element createElement() {
        return new PassThroughRenderElement(this);
    }
}
