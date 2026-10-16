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

/**
 * Base type of a navigable route — Flutter's {@code Route<T>}.
 *
 * <p>{@link #buildPage} is the one thing every route has to answer, and having
 * it here is what stops the navigator caring which kind of route it holds. It
 * used to ask {@code instanceof MaterialPageRoute} and build nothing for
 * anything else, so a nested navigator handed a Cupertino route — as the
 * Cupertino navigation-bar demo does — rendered a blank screen with nothing
 * reported.
 *
 * @param <T> the value type the route completes with when popped
 */
public abstract class Route<T> {

    private RouteSettings settings;

    /**
     * Flutter's {@code Route.settings}. Accepts an {@code Object} because super-parameter
     * forwarding erases the argument type to {@code dynamic}; only a {@link RouteSettings} is
     * retained.
     */
    public void settings(Object v) {
        this.settings = (v instanceof RouteSettings) ? (RouteSettings) v : null;
    }

    /**
     * Never null, as Flutter's is not: a route built without settings gets an empty
     * one whose name is null.
     *
     * <p>Returning null here LOCKED THE APP. Every study is wrapped in a back-to-gallery
     * button whose tap is {@code popUntil(route.settings().name() == '/')}, so a route on
     * the stack with no settings threw out of the predicate, the pop never finished, and
     * the only way out of the study was gone. A route reached in a way that sets no
     * settings -- a showModalPopup, a route pushed by builder rather than by name -- was
     * enough to arm it.</p>
     */
    public RouteSettings settings() {
        if (settings == null) {
            settings = new RouteSettings();
        }
        return settings;
    }

    /**
     * The page this route displays, or null when it cannot build one.
     *
     * <p>Every concrete route overrides this; a route that does not is a gap
     * worth reporting rather than a blank screen, which is why the navigator
     * reports a null page.</p>
     */
    /**
     * How long this route's entrance runs, or -1 to take the platform's page duration.
     * Flutter reads this off the route ({@code Route.transitionDuration}); a route with
     * motion of its own -- a container transform, a custom PageRouteBuilder -- sets it.
     */
    public int transitionMillis() {
        return -1;
    }

    /**
     * Whether this route enters as a full-screen modal, which Flutter animates up from the
     * bottom edge rather than in from the side.
     */
    public boolean isFullscreenDialog() {
        return false;
    }

    /**
     * The name of the component this route grows out of, or null.
     *
     * <p>Set for a container transform: the transition grows the incoming page from the
     * bounds of the thing that was tapped, so it needs to be able to find that thing in
     * the outgoing Form.</p>
     */
    public String containerTransformSource() {
        return null;
    }

    /**
     * Whether this route is an expanding container transform rather than a page push. The
     * real effect grows the tapped card into the page; a route is a Form of its own here,
     * so the closest honest approximation is a cross-fade, which at least reads as the
     * same surface changing rather than a new page arriving from off-screen.
     */
    public boolean isContainerTransform() {
        return false;
    }

    public com.codename1.flutter.Widget buildPage(com.codename1.flutter.BuildContext context) {
        return null;
    }
}
