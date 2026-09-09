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
import com.codename1.flutter.Key;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * A list whose items can be reordered by dragging — Flutter's
 * {@code ReorderableListView}. This milestone renders the items as a scrollable
 * {@link ListView} (children mode or {@code .builder} mode); the drag-handle
 * reordering that fires {@code onReorder(oldIndex, newIndex)} is deferred.
 */
public class ReorderableListView extends StatelessWidget {

    private DartList<Widget> children;
    private Widget header;
    private Funcs.VoidFunc2<Long, Long> onReorder;
    private Funcs.Func2<BuildContext, Long, Widget> itemBuilder;
    private Long itemCount;
    private boolean shrinkWrap;

    public ReorderableListView() {
    }

    /** Dart's {@code ReorderableListView.builder} named constructor. */
    public static ReorderableListView builder(Key key,
                                              Funcs.Func2<BuildContext, Long, Widget> itemBuilder,
                                              long itemCount,
                                              Funcs.VoidFunc2<Long, Long> onReorder,
                                              Object padding,
                                              Object scrollDirection,
                                              Boolean shrinkWrap) {
        ReorderableListView r = new ReorderableListView();
        r.key(key);
        r.itemBuilder = itemBuilder;
        r.itemCount = itemCount;
        r.onReorder = onReorder;
        r.shrinkWrap = shrinkWrap != null && shrinkWrap;
        return r;
    }

    public void children(DartList<Widget> v) {
        this.children = v;
    }

    public void header(Widget v) {
        this.header = v;
    }

    public void onReorder(Funcs.VoidFunc2<Long, Long> v) {
        this.onReorder = v;
    }

    public void padding(Object v) {
    }

    public void scrollDirection(Object v) {
    }

    public void shrinkWrap(boolean v) {
        this.shrinkWrap = v;
    }

    public void physics(Object v) {
    }

    public void buildDefaultDragHandles(boolean v) {
    }

    @Override
    public Widget build(BuildContext context) {
        DartList<Widget> kids = new DartList<Widget>();
        if (header != null) {
            kids.add(header);
        }
        if (itemBuilder != null && itemCount != null) {
            for (long i = 0; i < itemCount; i++) {
                Widget w = itemBuilder.call(context, i);
                if (w != null) {
                    kids.add(w);
                }
            }
        } else if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                kids.add(children.get(i));
            }
        }
        ListView list = new ListView();
        list.children(kids);
        if (shrinkWrap) {
            list.shrinkWrap(true);
        }
        return list;
    }
}
