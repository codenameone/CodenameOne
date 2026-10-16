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

import dart.runtime.Funcs;

/**
 * A material switch with CONTROLLED semantics (see {@link Checkbox}): a user
 * toggle fires {@code onChanged(newValue)} and the component snaps back to
 * the widget's configured value until a rebuild moves it. Backed by a CN1
 * {@link com.codename1.components.Switch} (UIID "FlutterSwitch").
 */
public class Switch extends Widget {

    private boolean value;
    private Funcs.VoidFunc1<Boolean> onChanged;

    public void value(boolean v) {
        this.value = v;
    }

    public void onChanged(Funcs.VoidFunc1<Boolean> v) {
        this.onChanged = v;
    }

    /** The color of the track/thumb when the switch is on — Flutter's {@code activeColor}. */
    public void activeColor(com.codename1.flutter.Color v) {
    }

    public boolean getValue() {
        return value;
    }

    public Funcs.VoidFunc1<Boolean> getOnChanged() {
        return onChanged;
    }

    @Override
    public Element createElement() {
        return new SwitchRenderElement(this);
    }
}
