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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Clip;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.SizedBox;

import dart.runtime.Funcs;

/**
 * A material bottom sheet surface — Flutter's {@code BottomSheet}. Renders the
 * widget produced by {@code builder(context)}; the drag-to-dismiss gesture that
 * fires {@code onClosing} is deferred.
 */
public class BottomSheet extends StatelessWidget {

    private Funcs.Func1<BuildContext, Widget> builder;
    private Funcs.VoidFunc0 onClosing;
    private boolean enableDrag = true;
    private Color backgroundColor;

    public void animationController(Object v) {
    }

    public void enableDrag(boolean v) {
        this.enableDrag = v;
    }

    public void onClosing(Funcs.VoidFunc0 v) {
        this.onClosing = v;
    }

    public void builder(Funcs.Func1<BuildContext, Widget> v) {
        this.builder = v;
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void elevation(double v) {
    }

    public void shape(Object v) {
    }

    public void clipBehavior(Clip v) {
    }

    public void constraints(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        if (builder != null) {
            Widget w = builder.call(context);
            if (w != null) {
                return w;
            }
        }
        return new SizedBox();
    }
}
