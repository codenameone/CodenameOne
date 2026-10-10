/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package javafx.animation;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;

/// An animation that is told, on every frame, how far through its cycle
/// it is: a subclass sets its cycle duration and implements
/// [#interpolate(double)].
///
/// The fraction of the cycle that has passed goes through the
/// [interpolator][#interpolatorProperty()] first, so a transition eases
/// in and out unless it is told otherwise. The interpolator is read
/// when the transition starts.
///
/// A frame rate cannot be asked for; a transition follows the display.
public abstract class Transition extends Animation {

    private final ObjectProperty<Interpolator> interpolator =
            new SimpleObjectProperty<Interpolator>(this, "interpolator", Interpolator.EASE_BOTH);
    private Interpolator cachedInterpolator = Interpolator.EASE_BOTH;

    /// Creates a transition.
    public Transition() {
    }

    /// Sets the pacing of the transition.
    public final void setInterpolator(Interpolator value) {
        interpolator.set(value);
    }

    /// Returns the pacing of the transition.
    public final Interpolator getInterpolator() {
        return interpolator.get();
    }

    /// The pacing of the transition; `EASE_BOTH` unless set.
    public final ObjectProperty<Interpolator> interpolatorProperty() {
        return interpolator;
    }

    /// Returns the interpolator the transition was started with.
    protected Interpolator getCachedInterpolator() {
        return cachedInterpolator;
    }

    /// Returns the node of the nearest parent transition that names
    /// one, for a transition that names none itself; `null` when there
    /// is none.
    protected Node getParentTargetNode() {
        if (parent instanceof Transition) {
            return ((Transition) parent).getParentTargetNode();
        }
        return null;
    }

    /// Shows the transition a fraction of the way through its cycle: 0
    /// at the start, 1 at the end.
    protected abstract void interpolate(double frac);

    /// Reads what the transition starts from; called as it starts.
    void prepare() {
    }

    private double fraction(double time) {
        double length = cycleMillis();
        double f = length <= 0 || Double.isInfinite(length) ? 1.0 : time / length;
        f = f < 0 ? 0 : f > 1 ? 1 : f;
        Interpolator i = cachedInterpolator == null ? Interpolator.LINEAR : cachedInterpolator;
        return i.interpolate(0.0, 1.0, f);
    }

    @Override
    void doStart(boolean capture) {
        if (capture) {
            cachedInterpolator = getInterpolator();
            prepare();
        }
    }

    @Override
    void doPlayTo(double from, double to, boolean forward, boolean atStart) {
        interpolate(fraction(to));
    }

    @Override
    void doJumpTo(double time) {
        interpolate(fraction(time));
    }
}
