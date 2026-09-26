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

import com.codename1.flutter.physics.SpringDescription;

/**
 * Determines how scrollable widgets respond to user input — Flutter's {@code
 * ScrollPhysics}. Physics can be composed by chaining a {@code parent}. This
 * pass captures the physics selection (and any parent) for API shape; the
 * concrete fling/overscroll behaviour is applied by the scrolling layer.
 */
public class ScrollPhysics {

    /** Material's default scroll spring, matching Flutter's ScrollPhysics.spring. */
    private static final SpringDescription DEFAULT_SPRING =
            SpringDescription.withDampingRatio(0.5, 100.0, 1.1);

    private ScrollPhysics parent;

    public ScrollPhysics() {
    }

    public void parent(ScrollPhysics v) {
        this.parent = v;
    }

    public ScrollPhysics getParent() {
        return parent;
    }

    /**
     * {@code ScrollPhysics.spring}: the default spring an overriding
     * {@code createBallisticSimulation} feeds to a ScrollSpringSimulation.
     */
    public SpringDescription spring() {
        return DEFAULT_SPRING;
    }

    /**
     * Returns a copy of this physics with {@code ancestor} as the tail of the parent
     * chain — Flutter's {@code ScrollPhysics.applyTo}. Subclasses override to return
     * their own type; the base composes a plain ScrollPhysics.
     */
    public ScrollPhysics applyTo(ScrollPhysics ancestor) {
        ScrollPhysics p = new ScrollPhysics();
        p.parent = ancestor;
        return p;
    }

    /**
     * Helper for {@code applyTo}: applies this physics's parent onto {@code ancestor}
     * — Flutter's {@code ScrollPhysics.buildParent}.
     */
    public ScrollPhysics buildParent(ScrollPhysics ancestor) {
        return parent == null ? ancestor : parent.applyTo(ancestor);
    }

    /**
     * The ballistic (fling) simulation for a release at {@code velocity} — Flutter's
     * {@code ScrollPhysics.createBallisticSimulation}. Delegates to the parent, else none.
     */
    public com.codename1.flutter.physics.Simulation createBallisticSimulation(
            ScrollMetrics position, double velocity) {
        return parent == null ? null : parent.createBallisticSimulation(position, velocity);
    }

    /** The tolerance for this physics ({@code ScrollPhysics.toleranceFor}). */
    public com.codename1.flutter.physics.Tolerance toleranceFor(ScrollMetrics metrics) {
        return parent == null ? new com.codename1.flutter.physics.Tolerance()
                : parent.toleranceFor(metrics);
    }

    /** Whether implicit scrolling (e.g. for accessibility) is allowed. */
    public boolean allowImplicitScrolling() {
        return true;
    }

    /**
     * Converts a raw drag delta into the offset actually applied to the scroll position —
     * Flutter's {@code ScrollPhysics.applyPhysicsToUserOffset}.
     *
     * <p>The base passes the drag straight through: one pixel of finger is one pixel of
     * content. {@link BouncingScrollPhysics} is where that stops being true.</p>
     */
    public double applyPhysicsToUserOffset(ScrollMetrics position, double offset) {
        return parent == null ? offset : parent.applyPhysicsToUserOffset(position, offset);
    }

    /** Whether the scrollable should respond to a drag at all. */
    public boolean shouldAcceptUserOffset(ScrollMetrics position) {
        if (parent != null) {
            return parent.shouldAcceptUserOffset(position);
        }
        return position.pixels() != 0.0
                || position.minScrollExtent() != position.maxScrollExtent();
    }

    // The fling thresholds, in logical pixels (per second for the velocities). Values are
    // Flutter's, confirmed against the SDK by the flutter_scroll_physics behavioral case
    // rather than transcribed from its constants.
    public double minFlingVelocity() {
        return parent == null ? 100.0 : parent.minFlingVelocity();
    }

    public double maxFlingVelocity() {
        return parent == null ? 8000.0 : parent.maxFlingVelocity();
    }

    public double minFlingDistance() {
        return parent == null ? 18.0 : parent.minFlingDistance();
    }
}
