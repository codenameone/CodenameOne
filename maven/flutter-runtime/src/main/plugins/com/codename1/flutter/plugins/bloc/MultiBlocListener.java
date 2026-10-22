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

import java.util.List;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/// flutter_bloc's `MultiBlocListener`: nests its listeners around `child`.
public class MultiBlocListener extends StatelessWidget {
    private List<? extends Widget> listeners;
    private Widget child;

    public void listeners(List<? extends Widget> v) {
        listeners = v;
    }

    public void child(Widget v) {
        child = v;
    }

    @Override
    public Widget build(BuildContext context) {
        Widget acc = child;
        if (listeners != null) {
            for (int i = listeners.size() - 1; i >= 0; i--) {
                Widget l = listeners.get(i);
                if (l instanceof BlocListener) {
                    ((BlocListener<?, ?>) l).child(acc);
                    acc = l;
                }
            }
        }
        return acc;
    }
}
