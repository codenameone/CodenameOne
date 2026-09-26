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
import com.codename1.flutter.Element;
import com.codename1.flutter.Key;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A staggered, Pinterest-style grid from the {@code flutter_staggered_grid_view}
 * package — {@code MasonryGridView}. Crane's backdrop builds one via the
 * {@code .count} constructor to lay out its destination cards.
 */
public class MasonryGridView extends Widget {

    private String restorationId;
    private long crossAxisCount = 1;
    private Double mainAxisSpacing;
    private Double crossAxisSpacing;
    private Long itemCount;
    private Funcs.Func2<BuildContext, Long, Widget> itemBuilder;

    public MasonryGridView() {
    }

    /** Dart's {@code MasonryGridView.count} named constructor in positional form. */
    public static MasonryGridView count(Key key, String restorationId, long crossAxisCount,
                                        Double mainAxisSpacing, Double crossAxisSpacing,
                                        Long itemCount, Funcs.Func2<BuildContext, Long, Widget> itemBuilder, Object scrollDirection,
                                        Boolean shrinkWrap, Object physics, Object padding,
                                        Object controller) {
        MasonryGridView g = new MasonryGridView();
        g.key(key);
        g.restorationId = restorationId;
        g.crossAxisCount = Math.max(1, crossAxisCount);
        g.mainAxisSpacing = mainAxisSpacing;
        g.crossAxisSpacing = crossAxisSpacing;
        g.itemCount = itemCount;
        g.itemBuilder = itemBuilder;
        return g;
    }

    public long getCrossAxisCount() {
        return crossAxisCount;
    }

    public Long getItemCount() {
        return itemCount;
    }

    public Funcs.Func2<BuildContext, Long, Widget> getItemBuilder() {
        return itemBuilder;
    }

    public String getRestorationId() {
        return restorationId;
    }

    /** The gap between items down a column, in logical pixels. */
    public double getMainAxisSpacing() {
        return mainAxisSpacing == null ? 0 : mainAxisSpacing.doubleValue();
    }

    /** The gap between columns, in logical pixels. */
    public double getCrossAxisSpacing() {
        return crossAxisSpacing == null ? 0 : crossAxisSpacing.doubleValue();
    }

    @Override
    public Element createElement() {
        return new MasonryGridViewRenderElement(this);
    }
}
