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
import javafx.util.Duration;

/// Does nothing for a length of time and then runs its finished
/// handler: a timer, and the pause between the children of a
/// [SequentialTransition].
public final class PauseTransition extends Transition {

    private final ObjectProperty<Duration> duration =
            new SimpleObjectProperty<Duration>(this, "duration", Duration.millis(400));

    /// Creates a pause of a length.
    public PauseTransition(Duration duration) {
        setDuration(duration);
    }

    /// Creates a pause of 400 milliseconds.
    public PauseTransition() {
        this(Duration.millis(400));
    }

    /// Sets the length of the transition. It is read when the
    /// transition starts.
    public final void setDuration(Duration value) {
        if (value == null) {
            throw new NullPointerException("Duration must not be null");
        }
        if (value.lessThan(Duration.ZERO) || value.isUnknown()) {
            throw new IllegalArgumentException("Cannot set duration to negative value.");
        }
        duration.set(value);
    }

    /// Returns the length of the transition.
    public final Duration getDuration() {
        return duration.get();
    }

    /// The length of the transition; 400 milliseconds unless set.
    public final ObjectProperty<Duration> durationProperty() {
        return duration;
    }

    @Override
    void syncDuration() {
        Duration d = duration.get();
        setCycleDuration(d == null || d.lessThan(Duration.ZERO) || d.isUnknown() ? Duration.ZERO : d);
    }

    /// Does nothing: a pause changes nothing.
    @Override
    protected void interpolate(double frac) {
    }
}
