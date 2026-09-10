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
package com.codename1.flutter.animation;

import com.codename1.flutter.Widget;

import dart.core.Duration;
import dart.runtime.Funcs;

/**
 * Holds the run a {@link PageTransitionSwitcher} plays when its child is replaced.
 *
 * <p>The state has to live here rather than on the widget: a widget is rebuilt every time
 * anything above it changes, so a controller kept there would be a fresh one on every
 * frame and the transition would restart forever. The element survives those rebuilds,
 * which is what lets it notice that THIS child is not the one it had.</p>
 */
public class PageTransitionSwitcherElement extends AnimatedWidgetElement {

    /// The animations package's own default, and what the gallery relies on.
    private static final int DEFAULT_MS = 300;

    private final AnimationController controller = new AnimationController();
    private final AlwaysStoppedAnimation<Double> still =
            new AlwaysStoppedAnimation<Double>(Double.valueOf(0));
    private Widget shown;
    private boolean subscribed;

    public PageTransitionSwitcherElement(PageTransitionSwitcher widget) {
        super(widget);
    }

    /**
     * Notes the child being built, starting a run when it is a different one.
     *
     * <p>"Different" is Flutter's own {@code canUpdate}: same type and same key updates in
     * place and is not a transition, anything else replaces the subtree and is.</p>
     */
    void noteChild(Widget child, Duration duration) {
        if (!subscribed) {
            subscribed = true;
            // Settled, not starting. The first child was not switched TO -- it was always
            // there -- and a transition that reads its animation at zero draws the page
            // fully transparent and scaled away. Every screen built through a switcher
            // would come up blank and stay blank, which is what a first run of this
            // measured: the bottom-navigation demo went from 4.6% wrong to 39%.
            controller.value(1.0);
            controller.addListener(new Funcs.VoidFunc0() {
                @Override
                public void call() {
                    markNeedsBuild();
                }
            });
        }
        controller.duration(duration != null ? duration
                : Duration.of(0, 0, 0, 0, DEFAULT_MS, 0));
        if (shown != null && !Widget.canUpdate(shown, child)) {
            controller.forward(Double.valueOf(0));
        }
        shown = child;
    }

    /** The incoming child's animation: 0 when it arrives, 1 when it has settled. */
    Animation<Double> primary() {
        return controller;
    }

    /**
     * The outgoing child's animation.
     *
     * <p>Held at zero. Running the two halves at once needs both children mounted at once,
     * and a route's subtree here is a whole page -- the mail navigator or the search page
     * -- so keeping the old one alive to fade it out would double the tree for the length
     * of the run. The incoming half is the half that reads as the transition.</p>
     */
    Animation<Double> secondary() {
        return still;
    }
}
