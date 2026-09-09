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
import com.codename1.flutter.Widget;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * A lazily-built child delegate — Flutter's {@code SliverChildBuilderDelegate}.
 * Materializes {@code builder(context, index)} for {@code 0..childCount-1}, or,
 * when {@code childCount} is null (an infinite delegate), until the builder
 * returns null (Flutter's end-of-list convention), capped to keep the eager
 * materialization bounded.
 */
public class SliverChildBuilderDelegate extends SliverChildDelegate {

    private static final int UNBOUNDED_CAP = 10000;

    private final Funcs.Func2<BuildContext, Long, Widget> builder;
    private Long childCount;

    public SliverChildBuilderDelegate(Funcs.Func2<BuildContext, Long, Widget> builder) {
        this.builder = builder;
    }

    public void childCount(long v) {
        this.childCount = v;
    }

    public void addAutomaticKeepAlives(boolean v) {
    }

    public void addRepaintBoundaries(boolean v) {
    }

    public void addSemanticIndexes(boolean v) {
    }

    @Override
    public DartList<Widget> buildChildren(BuildContext context) {
        DartList<Widget> out = new DartList<Widget>();
        if (builder == null) {
            return out;
        }
        long count = childCount != null ? childCount : UNBOUNDED_CAP;
        for (long i = 0; i < count; i++) {
            Widget w = builder.call(context, i);
            if (w == null) {
                break;
            }
            out.add(w);
        }
        return out;
    }
}
