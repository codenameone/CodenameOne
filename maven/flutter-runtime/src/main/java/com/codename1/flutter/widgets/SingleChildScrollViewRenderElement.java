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

import com.codename1.flutter.Axis;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.Dp;
import com.codename1.ui.Component;
import com.codename1.ui.events.ScrollListener;

/**
 * Scroll boundary for {@link SingleChildScrollView}: the content is the
 * child, optionally inset by the padding (synthesized {@link Padding}).
 */
public class SingleChildScrollViewRenderElement extends ScrollRenderElement {

    public SingleChildScrollViewRenderElement(SingleChildScrollView widget) {
        super(widget);
    }

    private Component pane;
    private ScrollController controller;
    /** A controller offset asked for before the pane had a size; applied on layout. */
    private Double pendingOffsetDp;
    private final ScrollController.Client client = new ScrollController.Client() {
        @Override
        public void scrollToOffset(double offset) {
            pendingOffsetDp = Double.valueOf(offset);
            applyPendingScroll();
        }
    };

    @Override
    public void mount(com.codename1.flutter.Element parent, int slot) {
        super.mount(parent, slot);
        attachController(((SingleChildScrollView) widget()).controller());
    }

    @Override
    public void update(Widget newWidget) {
        ScrollController next = ((SingleChildScrollView) newWidget).controller();
        if (next != controller) {
            attachController(next);
        }
        super.update(newWidget);
    }

    @Override
    public void unmount() {
        attachController(null);
        super.unmount();
    }

    /** Same attach/detach contract as {@code ListViewRenderElement}. */
    private void attachController(ScrollController next) {
        if (controller != null) {
            controller.detach(client);
        }
        controller = next;
        if (controller != null) {
            controller.attach(client);
        }
    }

    @Override
    protected Component createComponent() {
        Component c = super.createComponent();
        pane = c;
        if (c != null) {
            c.addScrollListener(new ScrollListener() {
                @Override
                public void scrollChanged(int scrollX, int scrollY, int oldX, int oldY) {
                    reportScroll(horizontal() ? scrollX : scrollY);
                }
            });
        }
        return c;
    }

    @Override
    protected com.codename1.ui.Container createPane(com.codename1.ui.layouts.Layout layout) {
        return new ScrollPane(layout) {
            @Override
            public void layoutContainer() {
                super.layoutContainer();
                applyPendingScroll();
            }
        };
    }

    private void applyPendingScroll() {
        if (pendingOffsetDp == null || !(pane instanceof ScrollPane)) {
            return;
        }
        boolean h = horizontal();
        if ((h ? pane.getWidth() : pane.getHeight()) <= 0) {
            return;
        }
        double offsetDp = pendingOffsetDp.doubleValue();
        pendingOffsetDp = null;
        ((ScrollPane) pane).scrollToPosition((int) Math.round(Dp.px(offsetDp)), h);
    }

    /** Tells the attached controller where the pane scrolled to, in logical pixels. */
    private void reportScroll(int scrollPx) {
        if (controller == null || pane == null) {
            return;
        }
        boolean h = horizontal();
        double scale = Dp.scale();
        double s = scale > 0 ? scale : 1;
        double content = h ? pane.getScrollDimension().getWidth() : pane.getScrollDimension().getHeight();
        double viewport = h ? pane.getWidth() : pane.getHeight();
        double maxExtent = Math.max(0, content - viewport);
        controller.userScrolled(client, scrollPx / s, maxExtent / s, viewport / s);
    }

    @Override
    protected boolean horizontal() {
        return ((SingleChildScrollView) widget()).getScrollDirection() == Axis.horizontal;
    }

    @Override
    protected Widget buildContent() {
        SingleChildScrollView w = (SingleChildScrollView) widget();
        return padForScrollAxis(w.getChild(), w.getPadding());
    }
}
