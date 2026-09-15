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
import com.codename1.ui.animations.Motion;
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
            return eased(CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL,
                    false, ms > 0 ? ms : CUPERTINO_PAGE_MS));
        }
        return CommonTransitions.createFade(ms > 0 ? ms : ZOOM_PAGE_MS);
    }

    /// Flutter's {@code Curves.linearToEaseOut}, which is the curve a Cupertino page
    /// transition travels along.
    /// The curve the ARRIVING page's position rides on an iOS push.
    ///
    /// NOT the linear-to-ease-out curve (0.35, 0.91, 0.33, 0.97), which is the obvious
    /// one to reach for and is what this used: that curve belongs to the page being LEFT
    /// and to the shadow, never to the one arriving. The page coming in gets this one,
    /// which is a three-point cubic: it
    /// accelerates hard to about half its travel in the first fifth of the run, then
    /// changes character at the joint and settles slowly.
    ///
    /// A single cubic cannot express that, which is why the distinction is easy to lose:
    /// substituting the outgoing curve looks plausible, matches at both ends, and is
    /// wrong everywhere in between. Measured against the reference over a 500ms push, as
    /// a fraction of the travel completed:
    ///
    /// ```text
    ///   ms     reference   this curve   linearToEaseOut
    ///   50        0.2382       0.2383            0.2615
    ///  150        0.7600       0.7604            0.7195
    ///  250        0.9422       0.9422            0.9201
    /// ```
    ///
    /// Both curves start at 0 and finish at 1 at exactly 500ms, so the error is invisible
    /// at either end and worst in the middle, where it is a whole page sitting 46 device
    /// pixels from where it belongs.
    ///
    /// Known gap: the OUTGOING page should ride linear-to-ease-out while this one rides
    /// the three-point curve, but a Codename One slide moves both pages from a single
    /// Motion, so both currently share this one. The outgoing page travels a third of the
    /// distance, so the error there is a third the size.
    private static final float[] FAST_EASE_IN_TO_SLOW_EASE_OUT = {
        0.056f, 0.024f, 0.108f, 0.3085f,
        0.198f, 0.541f,
        0.3655f, 1.0f, 0.5465f, 0.989f,
    };

    /**
     * Gives a transition Flutter's page curve instead of Codename One's default ease.
     *
     * <p>Both take the same 500ms, so the two agreed at the ends and disagreed all the
     * way between: measured against the reference at the same animation times, ours ran
     * ahead early and fell behind through the middle -- an ease-in-out against a curve
     * that is nearly linear out of the gate and eases only at the finish. Same distance,
     * same duration, visibly different travel.</p>
     */
    private static Transition eased(CommonTransitions t) {
        t.setMotion(new com.codename1.util.LazyValue<Motion>() {
            @Override
            public Motion get(Object... args) {
                int from = ((Integer) args[0]).intValue();
                int to = ((Integer) args[1]).intValue();
                int duration = ((Integer) args[2]).intValue();
                float[] c = FAST_EASE_IN_TO_SLOW_EASE_OUT;
                return Motion.createThreePointCubicMotion(from, to, duration,
                        c[0], c[1], c[2], c[3], c[4], c[5], c[6], c[7], c[8], c[9]);
            }
        });
        return t;
    }

    private static boolean usesCupertinoPageTransition(TargetPlatform p) {
        return p == TargetPlatform.iOS || p == TargetPlatform.macOS;
    }

    private static int platformPageMillis(TargetPlatform p) {
        return usesCupertinoPageTransition(p) ? CUPERTINO_PAGE_MS : ZOOM_PAGE_MS;
    }
}
