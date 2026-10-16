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
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A builder that owns a scrap of local state, rebuilt via a {@code setState}
 * handed to its {@code builder} — Flutter's {@code StatefulBuilder}. The
 * {@code builder} receives the {@link BuildContext} and a state-setter
 * ({@code void Function(VoidCallback)}); calling it runs the mutation. This pass
 * builds once (the setter runs the mutation but the localized rebuild is
 * deferred to the element machinery).
 */
public class StatefulBuilder extends StatelessWidget {

    private Funcs.Func2<BuildContext, Funcs.VoidFunc1<Funcs.VoidFunc0>, Widget> builder;

    public void builder(Funcs.Func2<BuildContext, Funcs.VoidFunc1<Funcs.VoidFunc0>, Widget> v) {
        this.builder = v;
    }

    @Override
    public Widget build(final BuildContext context) {
        if (builder == null) {
            return null;
        }
        Funcs.VoidFunc1<Funcs.VoidFunc0> setState = new Funcs.VoidFunc1<Funcs.VoidFunc0>() {
            @Override
            public void call(Funcs.VoidFunc0 fn) {
                if (fn != null) {
                    fn.call();
                }
                // Then rebuild this builder, as State.setState does. The mutation ran and
                // nothing was marked dirty, so the subtree showed the old value until an
                // unrelated ancestor rebuilt -- a StatefulBuilder in a dialog never updated.
                if (context instanceof com.codename1.flutter.Element) {
                    com.codename1.flutter.Element e = (com.codename1.flutter.Element) context;
                    if (e.isMounted()) {
                        e.markNeedsBuild();
                    }
                }
            }
        };
        return builder.call(context, setState);
    }
}
