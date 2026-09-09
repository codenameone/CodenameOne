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
 * A material checkbox with CONTROLLED semantics: the widget's {@code value}
 * is authoritative. A user toggle fires {@code onChanged(newValue)} and the
 * component is snapped back to the configured value — the app's
 * setState/rebuild is what actually moves the checkbox. Backed by a CN1
 * {@link com.codename1.ui.CheckBox} (UIID "FlutterCheckbox").
 */
public class Checkbox extends Widget {

    private Boolean value;
    private Funcs.VoidFunc1<Boolean> onChanged;

    public void value(Boolean v) {
        this.value = v;
    }

    public void onChanged(Funcs.VoidFunc1<Boolean> v) {
        this.onChanged = v;
    }

    /**
     * Whether the checkbox has a third "indeterminate" state
     * ({@code Checkbox.tristate}). The CN1 checkbox is binary, so this flag is
     * accepted for API compatibility but not otherwise modelled.
     */
    public void tristate(boolean v) {
    }

    public boolean getValue() {
        return value != null && value.booleanValue();
    }

    public Funcs.VoidFunc1<Boolean> getOnChanged() {
        return onChanged;
    }

    @Override
    public Element createElement() {
        return new CheckboxRenderElement(this);
    }
}
