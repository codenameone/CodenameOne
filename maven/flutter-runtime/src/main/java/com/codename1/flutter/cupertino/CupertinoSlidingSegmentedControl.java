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
import com.codename1.flutter.widgets.Container;

import dart.runtime.Funcs;

/**
 * The iOS-13 sliding segmented control — Flutter's
 * {@code CupertinoSlidingSegmentedControl}. Like {@link CupertinoSegmentedControl},
 * captures the segments and selection callback but composes an empty
 * {@link Container} placeholder this pass.
 *
 * @param <T> the segment key type
 */
public class CupertinoSlidingSegmentedControl<T> extends StatelessWidget {

    private Object children;
    private Funcs.VoidFunc1<Long> onValueChanged;
    private Object groupValue;

    public void children(Object v) {
        this.children = v;
    }

    public void onValueChanged(Funcs.VoidFunc1<Long> v) {
        this.onValueChanged = v;
    }

    public void groupValue(Object v) {
        this.groupValue = v;
    }

    public void thumbColor(Color v) {
    }

    public void backgroundColor(Color v) {
    }

    public void padding(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        return new Container();
    }
}
