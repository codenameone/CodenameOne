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
package com.codename1.presentation;

/// One step of a slide's animation: an element appearing, leaving or drawing attention.
///
/// A slide's builds run in order. A build started `ON_CLICK` waits for the presenter; one
/// started `WITH_PREVIOUS` runs together with the build before it and one started
/// `AFTER_PREVIOUS` when that build ends.
public final class Build {
    /// The element appears.
    public static final String ENTRANCE = "entrance";

    /// The element leaves.
    public static final String EXIT = "exit";

    /// The element pulses where it is.
    public static final String EMPHASIS = "emphasis";

    /// Shows or hides at once.
    public static final String APPEAR = "appear";

    /// Fades.
    public static final String FADE = "fade";

    /// Moves in from, or out to, the slide's edge.
    public static final String FLY = "fly";

    /// Grows from, or shrinks to, its centre.
    public static final String ZOOM = "zoom";

    /// Is revealed by a sweeping edge.
    public static final String WIPE = "wipe";

    /// Waits for a click.
    public static final String ON_CLICK = "onClick";

    /// Starts with the previous build.
    public static final String WITH_PREVIOUS = "withPrevious";

    /// Starts when the previous build ends.
    public static final String AFTER_PREVIOUS = "afterPrevious";

    /// The id of the element animated.
    public final String elementId;

    /// `ENTRANCE`, `EXIT` or `EMPHASIS`.
    public final String kind;

    /// `APPEAR`, `FADE`, `FLY`, `ZOOM` or `WIPE`.
    public final String effect;

    /// `ON_CLICK`, `WITH_PREVIOUS` or `AFTER_PREVIOUS`.
    public final String trigger;

    /// Milliseconds the effect takes.
    public final int duration;

    /// Milliseconds before it starts once triggered.
    public final int delay;

    /// For a fly: the edge, one of the `Transition.FROM_` constants.
    public final String direction;

    /// A build.
    ///
    /// #### Parameters
    ///
    /// - `elementId`: the element
    ///
    /// - `kind`: `ENTRANCE`, `EXIT` or `EMPHASIS`
    ///
    /// - `effect`: `APPEAR`, `FADE`, `FLY`, `ZOOM` or `WIPE`
    ///
    /// - `trigger`: `ON_CLICK`, `WITH_PREVIOUS` or `AFTER_PREVIOUS`
    ///
    /// - `duration`: milliseconds
    ///
    /// - `delay`: milliseconds
    ///
    /// - `direction`: a `Transition.FROM_` constant for a fly
    public Build(String elementId, String kind, String effect, String trigger, int duration,
            int delay, String direction) {
        if (elementId == null || elementId.length() == 0) {
            throw new IllegalArgumentException("A build needs an element");
        }
        this.elementId = elementId;
        this.kind = EXIT.equals(kind) || EMPHASIS.equals(kind) ? kind : ENTRANCE;
        this.effect = FADE.equals(effect) || FLY.equals(effect) || ZOOM.equals(effect)
                || WIPE.equals(effect) ? effect : APPEAR;
        this.trigger = WITH_PREVIOUS.equals(trigger) || AFTER_PREVIOUS.equals(trigger) ? trigger
                : ON_CLICK;
        this.duration = Math.max(0, Math.min(60000, duration));
        this.delay = Math.max(0, Math.min(60000, delay));
        this.direction = Transition.FROM_LEFT.equals(direction)
                || Transition.FROM_TOP.equals(direction)
                || Transition.FROM_BOTTOM.equals(direction) ? direction : Transition.FROM_BOTTOM;
    }

    /// An entrance on click.
    ///
    /// #### Parameters
    ///
    /// - `elementId`: the element
    ///
    /// - `effect`: the effect
    ///
    /// #### Returns
    ///
    /// the build
    public static Build entrance(String elementId, String effect) {
        return new Build(elementId, ENTRANCE, effect, ON_CLICK, 500, 0, Transition.FROM_BOTTOM);
    }

    /// This build with another trigger.
    ///
    /// #### Parameters
    ///
    /// - `value`: the trigger
    ///
    /// #### Returns
    ///
    /// the build
    public Build withTrigger(String value) {
        return new Build(elementId, kind, effect, value, duration, delay, direction);
    }

    /// This build of another element, for copies.
    ///
    /// #### Parameters
    ///
    /// - `value`: the element id
    ///
    /// #### Returns
    ///
    /// the build
    public Build withElement(String value) {
        return new Build(value, kind, effect, trigger, duration, delay, direction);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Build)) {
            return false;
        }
        Build b = (Build) o;
        return elementId.equals(b.elementId) && kind.equals(b.kind) && effect.equals(b.effect)
                && trigger.equals(b.trigger) && duration == b.duration && delay == b.delay
                && direction.equals(b.direction);
    }

    @Override
    public int hashCode() {
        return elementId.hashCode() * 31 + kind.hashCode() + effect.hashCode() * 7;
    }
}
