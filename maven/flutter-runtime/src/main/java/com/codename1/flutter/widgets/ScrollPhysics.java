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
}
