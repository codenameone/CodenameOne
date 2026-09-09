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

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * A scroll view whose header slivers scroll with an inner scrollable —
 * Flutter's {@code NestedScrollView}. This milestone renders the {@code body};
 * the {@code headerSliverBuilder} slivers and the coordinated
 * outer/inner scroll linkage are deferred.
 */
public class NestedScrollView extends StatelessWidget {

    private Widget body;
    private Funcs.Func2<BuildContext, Boolean, DartList<Widget>> headerSliverBuilder;

    public void body(Widget v) {
        this.body = v;
    }

    public void headerSliverBuilder(Funcs.Func2<BuildContext, Boolean, DartList<Widget>> v) {
        this.headerSliverBuilder = v;
    }

    public void controller(Object v) {
    }

    public void scrollDirection(Object v) {
    }

    public void reverse(boolean v) {
    }

    public void physics(Object v) {
    }

    public void floatHeaderSlivers(boolean v) {
    }

    @Override
    public Widget build(BuildContext context) {
        return body != null ? body : new SizedBox();
    }
}
