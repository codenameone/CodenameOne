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

/// How a slide comes on screen in a slide show, and when the show moves on from it.
public final class Transition {
    /// The slide simply replaces the previous one.
    public static final String NONE = "none";

    /// The slide fades in over the previous one.
    public static final String FADE = "fade";

    /// The slide pushes the previous one off screen.
    public static final String PUSH = "push";

    /// The slide slides in over the previous one.
    public static final String COVER = "cover";

    /// The previous slide slides off, uncovering this one.
    public static final String UNCOVER = "uncover";

    /// The slide is revealed by an edge sweeping across.
    public static final String WIPE = "wipe";

    /// The slide grows from the centre.
    public static final String ZOOM = "zoom";

    /// From the left.
    public static final String FROM_LEFT = "left";

    /// From the right.
    public static final String FROM_RIGHT = "right";

    /// From the top.
    public static final String FROM_TOP = "up";

    /// From the bottom.
    public static final String FROM_BOTTOM = "down";

    /// No transition, advanced by a click.
    public static final Transition DEFAULT = new Transition(NONE, 0, FROM_RIGHT, true, -1);

    /// The effect, one of the effect constants.
    public final String type;

    /// The duration in milliseconds.
    public final int duration;

    /// The direction the new slide comes from, one of the `FROM_` constants.
    public final String direction;

    /// True when a click, tap or key moves to the next slide.
    public final boolean advanceOnClick;

    /// Milliseconds after which the show moves on by itself, or -1 for never.
    public final int advanceAfter;

    /// A transition.
    ///
    /// #### Parameters
    ///
    /// - `type`: the effect
    ///
    /// - `duration`: milliseconds
    ///
    /// - `direction`: a `FROM_` constant
    ///
    /// - `advanceOnClick`: whether a click advances
    ///
    /// - `advanceAfter`: milliseconds before advancing by itself, or -1
    public Transition(String type, int duration, String direction, boolean advanceOnClick,
            int advanceAfter) {
        this.type = FADE.equals(type) || PUSH.equals(type) || COVER.equals(type)
                || UNCOVER.equals(type) || WIPE.equals(type) || ZOOM.equals(type) ? type : NONE;
        this.duration = Math.max(0, Math.min(60000, duration));
        this.direction = FROM_LEFT.equals(direction) || FROM_TOP.equals(direction)
                || FROM_BOTTOM.equals(direction) ? direction : FROM_RIGHT;
        this.advanceOnClick = advanceOnClick;
        this.advanceAfter = advanceAfter < 0 ? -1 : advanceAfter;
    }

    /// A transition advanced by a click.
    ///
    /// #### Parameters
    ///
    /// - `type`: the effect
    ///
    /// - `duration`: milliseconds
    ///
    /// #### Returns
    ///
    /// the transition
    public static Transition of(String type, int duration) {
        return new Transition(type, duration, FROM_RIGHT, true, -1);
    }

    /// This transition from another direction.
    ///
    /// #### Parameters
    ///
    /// - `value`: a `FROM_` constant
    ///
    /// #### Returns
    ///
    /// the transition
    public Transition withDirection(String value) {
        return new Transition(type, duration, value, advanceOnClick, advanceAfter);
    }

    /// This transition advancing by itself.
    ///
    /// #### Parameters
    ///
    /// - `ms`: milliseconds, or -1 for never
    ///
    /// #### Returns
    ///
    /// the transition
    public Transition withAdvanceAfter(int ms) {
        return new Transition(type, duration, direction, advanceOnClick, ms);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Transition)) {
            return false;
        }
        Transition t = (Transition) o;
        return type.equals(t.type) && duration == t.duration && direction.equals(t.direction)
                && advanceOnClick == t.advanceOnClick && advanceAfter == t.advanceAfter;
    }

    @Override
    public int hashCode() {
        return type.hashCode() * 31 + duration + direction.hashCode() * 7;
    }
}
