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

import dart.runtime.Funcs;

/// flutter_bloc's `BlocBuilder<B, S>`: rebuilds `builder` whenever the bloc's
/// state changes (and `buildWhen`, if given, agrees).
public class BlocBuilder<B, S> extends StatefulWidget {
    BlocBase<?> bloc;
    Funcs.Func2<BuildContext, S, Widget> builder;
    Funcs.Func2<S, S, Boolean> buildWhen;
    Class<?> providedType;

    public void bloc(BlocBase<?> v) {
        bloc = v;
    }

    public void builder(Funcs.Func2<BuildContext, S, Widget> v) {
        builder = v;
    }

    public void buildWhen(Funcs.Func2<S, S, Boolean> v) {
        buildWhen = v;
    }

    public void providedType(Class<?> v) {
        providedType = v;
    }

    @Override
    public State<? extends StatefulWidget> createState() {
        return new BlocWidgetState<BlocBuilder<B, S>>() {
            private Object shown;
            private boolean hasShown;

            @Override
            protected BlocBase<?> explicitBloc() {
                return widget().bloc;
            }

            @Override
            protected Class<?> blocType() {
                return widget().providedType;
            }

            @Override
            protected void onState(Object before, Object next) {
                if (test(widget().buildWhen, before, next)) {
                    shown = next;
                    hasShown = true;
                    rebuild();
                }
            }

            @Override
            @SuppressWarnings("unchecked")
            public Widget build(BuildContext context) {
                Object s = currentState();
                return widget().builder.call(context, (S) (hasShown ? shown : s));
            }
        };
    }
}
