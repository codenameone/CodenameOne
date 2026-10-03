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
import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.Element;
import com.codename1.flutter.Key;
import com.codename1.flutter.Widget;

import dart.core.DartList;
import dart.core.UnsupportedError;
import dart.runtime.Funcs;

/**
 * A scrollable vertical list. Two modes:
 * <ul>
 *   <li><b>Children mode</b> ({@code new ListView()} + setters): the given
 *       children stacked in a scrollable column.</li>
 *   <li><b>Builder mode</b> ({@link #builder}): M2 materializes
 *       {@code itemBuilder(context, index)} EAGERLY for every index in
 *       {@code 0..itemCount-1}; windowed/lazy building is an M3 milestone,
 *       which is also why a null (infinite) {@code itemCount} is rejected
 *       with an {@link UnsupportedError}.</li>
 * </ul>
 */
public class ListView extends Widget {

    private DartList<Widget> children;
    private EdgeInsets padding;
    private boolean shrinkWrap;
    private Long itemCount;
    private Funcs.Func2<BuildContext, Long, Widget> itemBuilder;
    private String restorationId;
    private ScrollPhysics physics;
    private boolean reverse;
    private com.codename1.flutter.Axis scrollDirection = com.codename1.flutter.Axis.vertical;
    private ScrollController controller;

    public void restorationId(String v) {
        this.restorationId = v;
    }

    public void physics(ScrollPhysics v) {
        this.physics = v;
    }

    public void reverse(boolean v) {
        this.reverse = v;
    }

    public void scrollDirection(com.codename1.flutter.Axis v) {
        this.scrollDirection = v;
    }

    public void controller(ScrollController v) {
        this.controller = v;
    }

    /** The controller driving this list, or null. */
    public ScrollController getController() {
        return controller;
    }

    public ScrollPhysics getPhysics() {
        return physics;
    }

    public boolean getReverse() {
        return reverse;
    }

    public com.codename1.flutter.Axis getScrollDirection() {
        return scrollDirection;
    }

    public ListView() {
    }

    /**
     * Dart's {@code ListView.builder} named constructor, in canonical positional form.
     *
     * <p>The trailing parameters are not decoration. A named argument this factory does not
     * declare is dropped by the transpiler without a word, so {@code shrinkWrap: true} —
     * which is how a list inside a Column says "size to your content" — used to be
     * discarded, and the list took the whole height it was offered instead. The settings
     * page's expanding options list is exactly that shape.</p>
     */
    public static ListView builder(Key key, Long itemCount,
                                   Funcs.Func2<BuildContext, Long, Widget> itemBuilder,
                                   EdgeInsets padding, Boolean shrinkWrap, Object physics,
                                   com.codename1.flutter.Axis scrollDirection, Object controller,
                                   String restorationId, Boolean primary, Double itemExtent,
                                   Boolean reverse) {
        if (itemCount == null) {
            throw new UnsupportedError(
                    "ListView.builder without itemCount (an infinite list) is not supported in M2; "
                            + "items are materialized eagerly and windowed building lands in M3");
        }
        ListView l = new ListView();
        l.key(key);
        l.itemCount = itemCount;
        l.itemBuilder = itemBuilder;
        l.padding = padding;
        l.shrinkWrap = shrinkWrap != null && shrinkWrap.booleanValue();
        if (scrollDirection != null) {
            l.scrollDirection(scrollDirection);
        }
        l.restorationId(restorationId);
        if (itemExtent != null) {
            l.itemExtent(itemExtent.doubleValue());
        }
        if (reverse != null) {
            l.reverse(reverse.booleanValue());
        }
        if (primary != null) {
            l.primary(primary.booleanValue());
        }
        // Declared by the builder and dropped: a builder list's controller never
        // reached the list.
        if (controller instanceof ScrollController) {
            l.controller((ScrollController) controller);
        }
        return l;
    }

    public void children(DartList<Widget> v) {
        this.children = v;
    }

    public void padding(EdgeInsets v) {
        this.padding = v;
    }

    public void shrinkWrap(boolean v) {
        this.shrinkWrap = v;
    }

    /**
     * A fixed per-item extent along the scroll axis — Flutter's
     * {@code ListView.itemExtent}. Held for a later layout pass.
     */
    public void itemExtent(double v) {
    }

    /**
     * Whether this is the primary scroll view associated with the parent
     * {@code PrimaryScrollController} ({@code ListView.primary}). Accepted for
     * API compatibility; scroll-controller association is not modelled here.
     */
    public void primary(boolean v) {
    }

    public DartList<Widget> getChildren() {
        return children;
    }

    public EdgeInsets getPadding() {
        return padding;
    }

    public boolean getShrinkWrap() {
        return shrinkWrap;
    }

    public Long getItemCount() {
        return itemCount;
    }

    public Funcs.Func2<BuildContext, Long, Widget> getItemBuilder() {
        return itemBuilder;
    }

    public boolean isBuilderMode() {
        return itemBuilder != null;
    }

    /**
     * Dart's {@code ListView.separated} named constructor.
     *
     * <p>The separators are real: a separated list of n items is a builder list
     * of 2n-1 children where the odd ones come from
     * {@code separatorBuilder(context, i)}. Dropping them (what this used to do)
     * is not a cosmetic omission — the mail study's inbox separates its cards
     * with a 4dp gap through which the page's background shows, so without the
     * separators the whole list rendered as one continuous white slab.</p>
     *
     * <p>An {@code itemCount} of null means an unbounded list, where "2n-1" has
     * no meaning; such a list keeps building items alone.</p>
     */
    public static ListView separated(Key key, Boolean primary, Long itemCount,
                                     final Funcs.Func2<BuildContext, Long, Widget> itemBuilder,
                                     final Funcs.Func2<BuildContext, Long, Widget> separatorBuilder,
                                     EdgeInsets padding, Boolean shrinkWrap) {
        Long count = itemCount;
        Funcs.Func2<BuildContext, Long, Widget> children = itemBuilder;
        if (itemCount != null && separatorBuilder != null && itemCount.longValue() > 0) {
            count = Long.valueOf(itemCount.longValue() * 2 - 1);
            children = new Funcs.Func2<BuildContext, Long, Widget>() {
                @Override
                public Widget call(BuildContext context, Long index) {
                    long i = index == null ? 0 : index.longValue();
                    if ((i & 1L) == 0L) {
                        return itemBuilder == null ? null
                                : itemBuilder.call(context, Long.valueOf(i / 2));
                    }
                    return separatorBuilder.call(context, Long.valueOf(i / 2));
                }
            };
        }
        ListView l = builder(key, count, children, padding, shrinkWrap,
                null, null, null, null, null, null, null);
        if (shrinkWrap != null) {
            l.shrinkWrap(shrinkWrap);
        }
        return l;
    }

    @Override
    public Element createElement() {
        return new ListViewRenderElement(this);
    }
}
