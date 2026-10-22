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
package com.codename1.flutter.plugins.bloc;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.State;
import com.codename1.flutter.StatefulWidget;
import com.codename1.flutter.Widget;

import dart.runtime.DartRuntime;
import dart.runtime.Funcs;

/// flutter_bloc's `BlocSelector<B, S, T>`: rebuilds only when `selector(state)`
/// gives a different value.
public class BlocSelector<B, S, T> extends StatefulWidget {
    BlocBase<?> bloc;
    Funcs.Func1<S, T> selector;
    Funcs.Func2<BuildContext, T, Widget> builder;
    Class<?> providedType;

    public void bloc(BlocBase<?> v) {
        bloc = v;
    }

    public void selector(Funcs.Func1<S, T> v) {
        selector = v;
    }

    public void builder(Funcs.Func2<BuildContext, T, Widget> v) {
        builder = v;
    }

    public void providedType(Class<?> v) {
        providedType = v;
    }

    @Override
    public State<? extends StatefulWidget> createState() {
        return new BlocWidgetState<BlocSelector<B, S, T>>() {
            private T selected;
            private boolean hasSelected;

            @Override
            protected BlocBase<?> explicitBloc() {
                return widget().bloc;
            }

            @Override
            protected Class<?> blocType() {
                return widget().providedType;
            }

            @Override
            protected void onBlocChanged() {
                hasSelected = false;
                selected = null;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void onState(Object before, Object next) {
                T v = widget().selector.call((S) next);
                if (!hasSelected || !DartRuntime.eq(v, selected)) {
                    selected = v;
                    hasSelected = true;
                    rebuild();
                }
            }

            @Override
            @SuppressWarnings("unchecked")
            public Widget build(BuildContext context) {
                // currentState() first: it is what notices a replaced bloc and clears
                // the cached selection.
                Object s = currentState();
                if (!hasSelected) {
                    selected = widget().selector.call((S) s);
                    hasSelected = true;
                }
                return widget().builder.call(context, selected);
            }
        };
    }
}
