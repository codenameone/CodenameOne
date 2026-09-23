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

import com.codename1.flutter.foundation.Listenable;

import java.util.ArrayList;
import java.util.List;

import dart.runtime.Funcs;

/**
 * Controls the visible page of a {@link PageView} — Flutter's
 * {@code PageController}.
 *
 * <p>This is a LIVE scroll model, not a stored page number. Flutter's carousels
 * are built on the controller being a {@code Listenable} whose {@link #page()}
 * is fractional while a drag is in flight: the gallery's home carousel wraps
 * every card in an {@code AnimatedBuilder(animation: controller)} and scales it
 * by {@code controller.page - index}, so a controller that never notifies and
 * always reports its initial page freezes every card at the scale it happened
 * to have on the first frame. The attached {@link PageViewRenderElement} feeds
 * {@link #applyMetrics} from the pane's scroll offset, which is what makes
 * {@code position.haveDimensions} answer true and drives the rebuilds.</p>
 */
public class PageController implements Listenable {

    private long initialPage;
    private boolean keepPage = true;
    private double viewportFraction = 1.0;
    private final ScrollPosition position = new ScrollPosition();
    private final List<Funcs.VoidFunc0> listeners = new ArrayList<Funcs.VoidFunc0>();

    /** The view currently driving this controller, or null when unattached. */
    private PageViewRenderElement view;

    /** The live fractional page; only meaningful once {@link #livePage} is set. */
    private double pageValue;
    private boolean livePage;

    public PageController() {
    }

    public void initialPage(long v) {
        this.initialPage = v;
    }

    public void keepPage(boolean v) {
        this.keepPage = v;
    }

    public void viewportFraction(double v) {
        this.viewportFraction = v;
    }

    /**
     * The current page, fractional while scrolling. Falls back to the initial
     * page until a view has reported its metrics, matching Flutter, where
     * reading {@code page} before attachment yields the initial page.
     */
    public Double page() {
        return livePage ? pageValue : (double) initialPage;
    }

    public long initialPage() {
        return initialPage;
    }

    public boolean keepPage() {
        return keepPage;
    }

    public double viewportFraction() {
        return viewportFraction;
    }

    public ScrollPosition position() {
        return position;
    }

    public boolean hasClients() {
        return view != null;
    }

    public Object animateToPage(long page, Object duration, Object curve) {
        if (view != null) {
            view.scrollToPage(page, true);
        }
        return null;
    }

    public void jumpToPage(long page) {
        if (view != null) {
            view.scrollToPage(page, false);
        }
    }

    public Object nextPage(Object duration, Object curve) {
        return animateToPage(Math.round(page().doubleValue()) + 1, duration, curve);
    }

    public Object previousPage(Object duration, Object curve) {
        return animateToPage(Math.round(page().doubleValue()) - 1, duration, curve);
    }

    public void addListener(Funcs.VoidFunc0 listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(Funcs.VoidFunc0 listener) {
        listeners.remove(listener);
    }

    public void dispose() {
        listeners.clear();
        view = null;
        livePage = false;
    }

    // ------------------------------------------------------------------
    // Framework plumbing — driven by the attached PageViewRenderElement
    // ------------------------------------------------------------------

    void attach(PageViewRenderElement v) {
        this.view = v;
    }

    void detach(PageViewRenderElement v) {
        if (this.view == v) {
            this.view = null;
            this.livePage = false;
        }
    }

    /**
     * Publishes the pane's geometry and scroll offset, notifying listeners only
     * when the fractional page actually moved.
     *
     * <p>The guard matters: this runs from both layout and every scroll event,
     * and an unconditional notify would mark the carousel's builders dirty on
     * frames where nothing moved.</p>
     */
    void applyMetrics(double pixels, double pageExtent, double viewportDimension,
                      double minExtent, double maxExtent) {
        position.applyViewportDimension(viewportDimension);
        position.applyContentDimensions(minExtent, maxExtent);
        position.setPixels(pixels);
        double p = pageExtent > 0 ? pixels / pageExtent : 0;
        if (livePage && p == pageValue) {
            return;
        }
        livePage = true;
        pageValue = p;
        notifyListeners();
    }

    private void notifyListeners() {
        com.codename1.flutter.foundation.Listeners.notify(listeners);
    }
}
