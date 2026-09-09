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
import com.codename1.flutter.Clip;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.core.DartList;

/**
 * A scroll view built from a list of slivers — Flutter's
 * {@code CustomScrollView}. Modeled as a {@link ListView} whose children are
 * the {@code slivers} (each sliver composes into a box widget); the fine-grained
 * sliver scroll protocol is deferred.
 */
public class CustomScrollView extends StatelessWidget {

    private DartList<Widget> slivers;
    private boolean shrinkWrap;

    public void slivers(DartList<Widget> v) {
        this.slivers = v;
    }

    public void controller(Object v) {
    }

    public void scrollDirection(Object v) {
    }

    public void reverse(boolean v) {
    }

    public void shrinkWrap(boolean v) {
        this.shrinkWrap = v;
    }

    public void physics(Object v) {
    }

    public void cacheExtent(double v) {
    }

    public void primary(Object v) {
    }

    public void clipBehavior(Clip v) {
    }

    @Override
    public Widget build(BuildContext context) {
        ListView list = new ListView();
        list.children(slivers != null ? slivers : new DartList<Widget>());
        if (shrinkWrap) {
            list.shrinkWrap(true);
        }
        return list;
    }
}
