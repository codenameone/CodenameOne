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
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.Transition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A pushed route must be given motion. A Form shown with no transition replaces the screen
 * outright, which is what every page push used to do -- the new page was fully painted on
 * the first frame after the tap. Flutter always animates a route in, and which animation it
 * picks comes from {@code PageTransitionsTheme} and the target platform.
 */
class RouteTransitionsTest {

    private static CommonTransitions of(Route<?> r, TargetPlatform p) {
        Transition t = RouteTransitions.forRoute(r, p);
        assertNotNull(t, "every route must be given a transition");
        assertTrue(t instanceof CommonTransitions, "expected a CommonTransitions, got " + t);
        return (CommonTransitions) t;
    }

    private static MaterialPageRoute<Object> page() {
        return new MaterialPageRoute<Object>();
    }

    /// iOS and macOS take CupertinoPageTransitionsBuilder: the page enters from the
    /// trailing edge over kTransitionDuration.
    @Test
    void applePlatformsSlideThePageInFromTheSide() {
        for (TargetPlatform p : new TargetPlatform[] {TargetPlatform.iOS, TargetPlatform.macOS}) {
            CommonTransitions t = of(page(), p);
            assertTrue(t.isHorizontalSlide(), p + " should slide horizontally");
            // CommonTransitions names the direction after the OUTGOING page: forward
            // moves the source right, bringing the new page in from the LEADING edge,
            // which is the way back. A push comes from the trailing edge.
            assertFalse(t.isForwardSlide(),
                    p + " should bring the new page in from the trailing edge");
            assertEquals(500, t.getTransitionSpeed(), p + " uses kTransitionDuration");
        }
    }

    /// Everywhere else takes ZoomPageTransitionsBuilder, a fade with a slight scale. The
    /// scale is not reproduced; the fade and its duration are.
    @Test
    void theOtherPlatformsFadeThePageIn() {
        for (TargetPlatform p : new TargetPlatform[] {TargetPlatform.android,
                TargetPlatform.windows, TargetPlatform.linux}) {
            CommonTransitions t = of(page(), p);
            assertFalse(t.isHorizontalSlide(), p + " should not slide");
            assertFalse(t.isVerticalSlide(), p + " should not slide");
            assertEquals(300, t.getTransitionSpeed(),
                    p + " uses ZoomPageTransitionsBuilder's duration");
        }
    }

    @Test
    void aFullScreenDialogComesUpFromTheBottomOnEveryPlatform() {
        MaterialPageRoute<Object> r = page();
        r.fullscreenDialog(Boolean.TRUE);
        for (TargetPlatform p : new TargetPlatform[] {TargetPlatform.iOS, TargetPlatform.android}) {
            CommonTransitions t = of(r, p);
            assertTrue(t.isVerticalSlide(), p + " should slide a modal vertically");
            assertFalse(t.isForwardSlide(),
                    p + " should bring a modal UP from the bottom edge, not down");
        }
    }

    /// A container transform is one surface becoming another, so it must not read as a page
    /// arriving from off-screen -- on any platform, including the ones that slide.
    @Test
    void aContainerTransformCrossFadesAtItsOwnDuration() {
        Route<Object> r = new MaterialPageRoute<Object>() {
            @Override
            public boolean isContainerTransform() {
                return true;
            }

            @Override
            public int transitionMillis() {
                return 425;
            }
        };
        CommonTransitions t = of(r, TargetPlatform.iOS);
        assertFalse(t.isHorizontalSlide(), "a container transform is not a page push");
        assertEquals(425, t.getTransitionSpeed(), "the route's own duration is honoured");
    }

    /// A route that states no duration falls back to the platform's, rather than to zero --
    /// which would be no animation at all.
    @Test
    void aRouteWithNoStatedDurationTakesThePlatformDefault() {
        assertEquals(500, of(page(), TargetPlatform.iOS).getTransitionSpeed());
        assertEquals(300, of(page(), TargetPlatform.android).getTransitionSpeed());
    }
}
