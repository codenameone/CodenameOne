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

import dart.core.DartList;
import dart.runtime.Funcs;
import dart.runtime.RefLong;

/**
 * A scrollable list showing one page at a time — Flutter's {@code PageView} (and
 * its {@code .builder} named constructor). Two modes mirror {@link ListView}:
 * <ul>
 *   <li><b>Children mode</b> — a fixed list of page widgets.</li>
 *   <li><b>Builder mode</b> ({@link #builder}) — pages materialized on demand
 *       from {@code itemBuilder(context, index)} for {@code 0..itemCount-1}.</li>
 * </ul>
 * This pass lays the pages out in a scroll boundary; true one-page snapping and
 * the {@link PageController} coupling land with the paging renderer, so
 * {@code controller} / {@code onPageChanged} are captured.
 */
public class PageView extends Widget {

    private PageController controller;
    private Object scrollDirection;
    private Boolean reverse;
    private Object physics;
    private Boolean pageSnapping;
    private Funcs.VoidFunc1<RefLong> onPageChanged;
    private DartList<Widget> children;
    private Boolean allowImplicitScrolling;
    private String restorationId;
    private Object clipBehavior;

    private Funcs.Func2<BuildContext, Long, Widget> itemBuilder;
    private Long itemCount;

    public PageView() {
    }

    public void controller(PageController v) { this.controller = v; }
    public void scrollDirection(Object v) { this.scrollDirection = v; }
    public void reverse(Boolean v) { this.reverse = v; }
    public void physics(Object v) { this.physics = v; }
    public void pageSnapping(Boolean v) { this.pageSnapping = v; }
    public void onPageChanged(Funcs.VoidFunc1<RefLong> v) { this.onPageChanged = v; }
    public void children(DartList<Widget> v) { this.children = v; }
    public void allowImplicitScrolling(Boolean v) { this.allowImplicitScrolling = v; }
    public void restorationId(String v) { this.restorationId = v; }
    public void clipBehavior(Object v) { this.clipBehavior = v; }

    /** Dart's {@code PageView.builder} named constructor in positional form. */
    public static PageView builder(Key key, PageController controller, Object scrollDirection,
                                   Boolean reverse, Object physics, Boolean pageSnapping,
                                   Funcs.VoidFunc1<RefLong> onPageChanged,
                                   Funcs.Func2<BuildContext, Long, Widget> itemBuilder,
                                   Long itemCount, Boolean allowImplicitScrolling,
                                   String restorationId, Object clipBehavior) {
        PageView p = new PageView();
        p.key(key);
        p.controller = controller;
        p.scrollDirection = scrollDirection;
        p.reverse = reverse;
        p.physics = physics;
        p.pageSnapping = pageSnapping;
        p.onPageChanged = onPageChanged;
        p.itemBuilder = itemBuilder;
        p.itemCount = itemCount;
        p.allowImplicitScrolling = allowImplicitScrolling;
        p.restorationId = restorationId;
        p.clipBehavior = clipBehavior;
        return p;
    }

    public PageController getController() {
        return controller;
    }

    /** The scroll axis; null means Flutter's default, {@code Axis.horizontal}. */
    public Object getScrollDirection() {
        return scrollDirection;
    }

    public DartList<Widget> getChildren() {
        return children;
    }

    public Funcs.Func2<BuildContext, Long, Widget> getItemBuilder() {
        return itemBuilder;
    }

    /**
     * Whether a released drag settles on a page. Flutter's default is true, but
     * it is genuinely opt-out — the gallery's home carousel passes false and
     * scrolls freely, so snapping it would be a fidelity bug, not a nicety.
     */
    public boolean isPageSnapping() {
        return pageSnapping == null || pageSnapping.booleanValue();
    }

    public Funcs.VoidFunc1<RefLong> getOnPageChanged() {
        return onPageChanged;
    }

    public Long getItemCount() {
        return itemCount;
    }

    public boolean isBuilderMode() {
        return itemBuilder != null;
    }

    @Override
    public Element createElement() {
        return new PageViewRenderElement(this);
    }
}
