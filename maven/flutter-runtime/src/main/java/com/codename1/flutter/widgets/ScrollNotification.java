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
import com.codename1.flutter.rendering.ScrollDirection;

/**
 * A notification that bubbles up the widget tree as a scrollable scrolls —
 * Flutter's {@code ScrollNotification}. The reply adaptive-nav reads
 * {@link #direction()} and the nesting {@code depth} to drive the bottom app
 * bar. Instances are produced by the scroll machinery; this pass captures the
 * inspected shape.
 */
public class ScrollNotification {

    private ScrollMetrics metrics;
    private long depth;
    private BuildContext context;
    private ScrollDirection direction;

    public ScrollMetrics metrics() {
        return metrics;
    }

    public void metrics(ScrollMetrics v) {
        this.metrics = v;
    }

    /** The number of scrollables this notification has bubbled through. */
    public long get$depth() {
        return depth;
    }

    public void depth(long v) {
        this.depth = v;
    }

    public BuildContext context() {
        return context;
    }

    public void context(BuildContext v) {
        this.context = v;
    }

    public ScrollDirection direction() {
        return direction;
    }

    public void direction(ScrollDirection v) {
        this.direction = v;
    }

    /**
     * Delivers this notification to every {@link NotificationListener} above {@code target},
     * innermost first, stopping at the first one that returns true -- Flutter's
     * {@code Notification.dispatch}.
     *
     * <p>{@link #get$depth()} counts the scroll views the notification has bubbled THROUGH,
     * not the listeners. That is the distinction the gallery relies on: Reply hides its
     * bottom bar on {@code UserScrollNotification(depth: 0)}, so that dragging the image
     * carousel inside a mail card -- a scroll view of its own, whose notifications reach
     * the same listener at depth 1 -- does not move the bar.</p>
     */
    public boolean dispatch(BuildContext target) {
        if (!(target instanceof com.codename1.flutter.Element)) {
            return false;
        }
        context(target);
        long depth = 0;
        com.codename1.flutter.Element from = (com.codename1.flutter.Element) target;
        for (com.codename1.flutter.Element a = from.parent(); a != null; a = a.parent()) {
            if (a instanceof ScrollRenderElement) {
                depth++;
                continue;
            }
            if (!(a.widget() instanceof NotificationListener)) {
                continue;
            }
            dart.runtime.Funcs.Func1<ScrollNotification, Boolean> cb =
                    ((NotificationListener<?>) a.widget()).getOnNotification();
            if (cb == null) {
                continue;
            }
            depth(depth);
            if (Boolean.TRUE.equals(cb.call(this))) {
                return true;
            }
        }
        return false;
    }
}
