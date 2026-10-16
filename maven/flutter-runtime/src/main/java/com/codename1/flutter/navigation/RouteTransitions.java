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
package com.codename1.flutter.navigation;

import com.codename1.flutter.TargetPlatform;
import com.codename1.flutter.foundation.FoundationLib;
import com.codename1.ui.Form;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.Transition;

/**
 * Gives a pushed route the motion Flutter would give it.
 *
 * <p>A route here is a Form of its own, and a Form shown without a transition simply
 * replaces what was on screen. That is what every page push did: the new page was fully
 * painted on the first frame after the tap, with nothing in between. Flutter never does
 * that -- {@code MaterialPageRoute} always has a transition, chosen by
 * {@code PageTransitionsTheme} from the target platform.</p>
 *
 * <p>The mapping below follows Flutter's own default table
 * ({@code PageTransitionsTheme._defaultBuilders}) and its durations:</p>
 *
 * <ul>
 * <li><b>iOS and macOS</b> use {@code CupertinoPageTransitionsBuilder}, 500ms, the page
 *     entering from the trailing edge. Codename One's horizontal slide is the same
 *     gesture; what it does not reproduce is the outgoing page's parallax, since it
 *     moves both pages at one rate.</li>
 * <li><b>Android, Windows and Linux</b> use {@code ZoomPageTransitionsBuilder}, 300ms, a
 *     fade with a slight scale. A cross-fade keeps the fade and drops the scale.</li>
 * <li>A <b>full-screen dialog</b> enters from the bottom edge on every platform.</li>
 * <li>A <b>container transform</b> ({@code OpenContainer}) grows the tapped card into the
 *     page. Across two Forms the honest approximation is a cross-fade over the route's own
 *     duration -- it reads as one surface becoming another rather than as a page arriving
 *     from off-screen, which is the part that matters.</li>
 * </ul>
 *
 * <p>Only the entering transition is set. Codename One plays it in reverse for
 * {@code showBack()}, which is what {@link Navigator#pop} uses, so the way back out of a
 * route mirrors the way in without a second mapping to keep in step.</p>
 */
final class RouteTransitions {

    /** {@code CupertinoRouteTransitionMixin.kTransitionDuration}. */
    private static final int CUPERTINO_PAGE_MS = 500;

    /** {@code ZoomPageTransitionsBuilder.transitionDuration}. */
    private static final int ZOOM_PAGE_MS = 300;

    private RouteTransitions() {
    }

    /**
     * Sets the transition the route asks for on the Form that carries it. Called before
     * the Form is shown; a Form with no transition set replaces the screen outright.
     */
    static void apply(Form form, Route<?> route) {
        if (form == null || route == null) {
            return;
        }
        Transition t = forRoute(route);
        if (t != null) {
            form.setTransitionInAnimator(t);
        }
    }

    static Transition forRoute(Route<?> route) {
        return forRoute(route, FoundationLib.defaultTargetPlatform);
    }

    /** The platform is a parameter so both branches of the table can be pinned by a test. */
    static Transition forRoute(Route<?> route, TargetPlatform platform) {
        int ms = route.transitionMillis();
        if (route.isContainerTransform()) {
            String source = route.containerTransformSource();
            if (source != null) {
                // The page grows out of the bounds of what was tapped and folds back into
                // it on the way out, which is what makes a card feel like it BECAME the
                // page rather than being replaced by one.
                //
                // Not BubbleTransition, which was the closest thing to hand and is a
                // circular reveal: a hole opening in the screen, always round, always from
                // the centre of the destination. Material's container transform is a
                // rounded RECTANGLE travelling from the tapped bounds with its corners
                // straightening and the two contents crossing over inside it, which is a
                // different shape and a different anchor.
                return com.codename1.ui.animations.ContainerTransformTransition.create(
                        source, ms > 0 ? ms : ZOOM_PAGE_MS);
            }
            // Nothing to grow from -- the tapped surface has no component of its own.
            // A cross-fade at least reads as one surface becoming another.
            return CommonTransitions.createFade(ms > 0 ? ms : ZOOM_PAGE_MS);
        }
        if (route.isFullscreenDialog()) {
            // Up from the bottom edge: SLIDE_VERTICAL with forward false, since forward
            // moves the incoming page down rather than up.
            return CommonTransitions.createSlide(CommonTransitions.SLIDE_VERTICAL, false,
                    ms > 0 ? ms : platformPageMillis(platform));
        }
        if (usesCupertinoPageTransition(platform)) {
            // forward=FALSE is the push. CommonTransitions names the direction after the
            // OUTGOING page -- paintSlideAtPosition moves the source by +position when
            // forward is true, so the destination comes in from the leading edge, which is
            // the way BACK. A push brings the new page in from the trailing edge, and
            // showBack() plays this in reverse for the pop.
            // Not a slide: a slide holds the two pages a screen apart and moves the pair,
            // and this platform's push moves them different distances on different
            // curves -- the arriving page crosses the whole screen, the one it covers
            // drifts a third of it.
            return com.codename1.ui.animations.CupertinoPageTransition.create(
                    ms > 0 ? ms : CUPERTINO_PAGE_MS);
        }
        return CommonTransitions.createFade(ms > 0 ? ms : ZOOM_PAGE_MS);
    }

    private static boolean usesCupertinoPageTransition(TargetPlatform p) {
        return p == TargetPlatform.iOS || p == TargetPlatform.macOS;
    }

    private static int platformPageMillis(TargetPlatform p) {
        return usesCupertinoPageTransition(p) ? CUPERTINO_PAGE_MS : ZOOM_PAGE_MS;
    }
}
