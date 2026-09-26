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
package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Column;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * The iOS spinning-wheel picker — Flutter's {@code CupertinoPicker}. The 3D
 * wheel is not modeled this pass; the item widgets are laid out as a vertical
 * {@link Column} (approximate). The selection callback is captured.
 */
public class CupertinoPicker extends Widget {

    private DartList<Widget> children;
    private Funcs.VoidFunc1<Long> onSelectedItemChanged;

    public void backgroundColor(Color v) {
    }

    public void itemExtent(double v) {
    }

    public void diameterRatio(double v) {
    }

    public void magnification(double v) {
    }

    public void squeeze(double v) {
    }

    public void useMagnifier(boolean v) {
    }

    public void scrollController(Object v) {
    }

    public void onSelectedItemChanged(Funcs.VoidFunc1<Long> v) {
        this.onSelectedItemChanged = v;
    }

    public void children(DartList<Widget> v) {
        this.children = v;
    }

    DartList<Widget> getChildren() {
        return children;
    }

    Funcs.VoidFunc1<Long> getOnSelectedItemChanged() {
        return onSelectedItemChanged;
    }

    @Override
    public com.codename1.flutter.Element createElement() {
        return new CupertinoPickerRenderElement(this);
    }
}
