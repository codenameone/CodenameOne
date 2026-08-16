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
package com.codename1.home;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// What one particular accessory will actually accept for one particular
/// [Trait]: whether it can be read or written, the range it works over, the
/// step it moves in, and for an enum trait, which constants it can produce.
///
/// [Trait] documents the *nominal* range -- brightness is a percentage, so it
/// is 0 to 100. This carries the real one, from HomeKit's
/// `HMCharacteristicMetadata` or the Matter attribute's own min and max
/// attributes. A dimmer whose floor is 10 percent and that steps in fives says
/// so here, and a slider built from these numbers will not offer the user a
/// value the accessory is going to reject.
///
/// #### Writes are refused, not clamped
///
/// A write outside [#getMinimum()] to [#getMaximum()] fails with
/// [HomeError#VALUE_OUT_OF_RANGE]. Clamping was the obvious alternative and it
/// is worse: an app that asked for 40 degrees and silently got 38 never learns
/// it was wrong, so the bug reaches the user as a thermostat that "does not go
/// high enough" rather than as a failure at the call site where it can be
/// fixed.
public final class TraitConstraint {

    private final Trait trait;
    private final boolean readable;
    private final boolean writable;
    private final boolean notifies;
    private final double minimum;
    private final double maximum;
    private final double step;
    private final boolean ranged;
    private final List<Integer> validOrdinals;

    private TraitConstraint(Trait trait, boolean readable, boolean writable,
            boolean notifies, double minimum, double maximum, double step,
            boolean ranged, List<Integer> validOrdinals) {
        this.trait = trait;
        this.readable = readable;
        this.writable = writable;
        this.notifies = notifies;
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = step;
        this.ranged = ranged;
        this.validOrdinals = validOrdinals;
    }

    /// A constraint for a trait whose accessory declared no range -- a
    /// boolean, or a numeric trait the platform did not describe.
    ///
    /// #### Parameters
    ///
    /// - `trait`: the trait this describes
    ///
    /// - `readable`: whether the accessory will answer a read
    ///
    /// - `writable`: whether the accessory will accept a write
    ///
    /// - `notifies`: whether the accessory pushes changes, so a subscription
    ///   on it can do better than polling
    ///
    /// #### Returns
    ///
    /// the constraint
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `trait` is `null`
    public static TraitConstraint of(Trait trait, boolean readable,
            boolean writable, boolean notifies) {
        if (trait == null) {
            throw new IllegalArgumentException("trait is required");
        }
        return new TraitConstraint(trait, readable, writable, notifies, 0, 0,
                0, false, Collections.<Integer>emptyList());
    }

    /// A constraint for a numeric trait whose accessory declared its range.
    ///
    /// #### Parameters
    ///
    /// - `trait`: the trait this describes
    ///
    /// - `readable`: whether the accessory will answer a read
    ///
    /// - `writable`: whether the accessory will accept a write
    ///
    /// - `notifies`: whether the accessory pushes changes
    ///
    /// - `minimum`: the smallest accepted value, in [Trait#getUnit()]
    ///
    /// - `maximum`: the largest accepted value, in [Trait#getUnit()]
    ///
    /// - `step`: the increment the accessory moves in, or zero when it did
    ///   not say
    ///
    /// #### Returns
    ///
    /// the constraint
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `trait` is `null`, or `maximum` is
    ///   below `minimum`
    public static TraitConstraint ranged(Trait trait, boolean readable,
            boolean writable, boolean notifies, double minimum, double maximum,
            double step) {
        if (trait == null) {
            throw new IllegalArgumentException("trait is required");
        }
        if (maximum < minimum) {
            throw new IllegalArgumentException("maximum " + maximum
                    + " is below minimum " + minimum + " for " + trait.getId());
        }
        return new TraitConstraint(trait, readable, writable, notifies,
                minimum, maximum, step, true, Collections.<Integer>emptyList());
    }

    /// A constraint for an enum trait whose accessory declared which values it
    /// can produce.
    ///
    /// #### Parameters
    ///
    /// - `trait`: the trait this describes
    ///
    /// - `readable`: whether the accessory will answer a read
    ///
    /// - `writable`: whether the accessory will accept a write
    ///
    /// - `notifies`: whether the accessory pushes changes
    ///
    /// - `validOrdinals`: the ordinals of the constants this accessory can
    ///   produce or accept, in the domain enum for this trait; `null` or empty
    ///   means it did not say, which is not the same as "none"
    ///
    /// #### Returns
    ///
    /// the constraint
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `trait` is `null`
    public static TraitConstraint choices(Trait trait, boolean readable,
            boolean writable, boolean notifies, int[] validOrdinals) {
        if (trait == null) {
            throw new IllegalArgumentException("trait is required");
        }
        List<Integer> ordinals;
        if (validOrdinals == null || validOrdinals.length == 0) {
            ordinals = Collections.<Integer>emptyList();
        } else {
            List<Integer> copy = new ArrayList<Integer>(validOrdinals.length);
            for (int i = 0; i < validOrdinals.length; i++) {
                copy.add(Integer.valueOf(validOrdinals[i]));
            }
            ordinals = Collections.unmodifiableList(copy);
        }
        return new TraitConstraint(trait, readable, writable, notifies, 0, 0, 0,
                false, ordinals);
    }

    /// The trait this describes.
    ///
    /// #### Returns
    ///
    /// the trait, never `null`
    public Trait getTrait() {
        return trait;
    }

    /// Whether this accessory will answer a read of this trait.
    ///
    /// #### Returns
    ///
    /// `true` when the trait is readable here
    public boolean isReadable() {
        return readable;
    }

    /// Whether this accessory will accept a write of this trait.
    ///
    /// Narrower than [Trait#isReadOnly()], which says whether writing the
    /// trait could ever mean anything at all. A writable trait can still be
    /// read-only on a particular accessory -- a thermostat you have been given
    /// view-only access to, a covering with its motor disabled.
    ///
    /// #### Returns
    ///
    /// `true` when the trait is writable here
    public boolean isWritable() {
        return writable;
    }

    /// Whether this accessory pushes changes to this trait.
    ///
    /// Where this is `false`, a subscription still works but has nothing
    /// better than polling behind it, so changes arrive late or only when you
    /// call [SmartHome#drainChanges()]. Independent of
    /// [TraitSubscription#isPushDelivery()], which is about whether the
    /// *platform* can deliver at all; this is about whether the *accessory*
    /// bothers to say.
    ///
    /// #### Returns
    ///
    /// `true` when the accessory reports changes on its own
    public boolean notifiesOnChange() {
        return notifies;
    }

    /// Whether [#getMinimum()], [#getMaximum()] and [#getStep()] mean
    /// anything.
    ///
    /// #### Returns
    ///
    /// `true` when the accessory declared a range
    public boolean hasRange() {
        return ranged;
    }

    /// The smallest value this accessory accepts, in [Trait#getUnit()].
    ///
    /// #### Returns
    ///
    /// the minimum, or zero when [#hasRange()] is `false`
    public double getMinimum() {
        return minimum;
    }

    /// The largest value this accessory accepts, in [Trait#getUnit()].
    ///
    /// #### Returns
    ///
    /// the maximum, or zero when [#hasRange()] is `false`
    public double getMaximum() {
        return maximum;
    }

    /// The increment this accessory moves in, in [Trait#getUnit()].
    ///
    /// Zero means it did not say, which is not the same as continuous --
    /// treat it as unknown rather than as a step of nothing.
    ///
    /// #### Returns
    ///
    /// the step, or zero
    public double getStep() {
        return step;
    }

    /// The ordinals of the domain-enum constants this accessory can produce or
    /// accept.
    ///
    /// An empty list means the accessory did not say, **not** that it accepts
    /// nothing. Most do not say.
    ///
    /// #### Returns
    ///
    /// an immutable list of ordinals, possibly empty
    public List<Integer> getValidOrdinals() {
        return validOrdinals;
    }

    /// Whether a value falls inside what this accessory will accept.
    ///
    /// Checks the range for a numeric trait and the ordinal list for an enum
    /// one, and answers `true` for anything the accessory did not constrain --
    /// this is what the write path tests, and refusing values on the strength
    /// of information an accessory never gave would fail writes that work.
    ///
    /// The step is deliberately **not** enforced. Accessories declare steps
    /// they then round to happily, and refusing 33 percent on a dimmer that
    /// declares fives would reject a value the hardware accepts.
    ///
    /// #### Parameters
    ///
    /// - `value`: the value to test, or `null`
    ///
    /// #### Returns
    ///
    /// `true` when the value is acceptable, or when the accessory declared no
    /// constraint to test it against; `false` for a `null` value or one whose
    /// kind does not match the trait
    public boolean accepts(TraitValue value) {
        if (value == null || value.getKind() != trait.getValueKind()) {
            return false;
        }
        if (value.getKind() == TraitValueKind.ENUM) {
            if (validOrdinals.isEmpty()) {
                return true;
            }
            Integer ordinal = Integer.valueOf(value.getEnumOrdinal());
            return validOrdinals.contains(ordinal);
        }
        if (!ranged || value.getKind() != TraitValueKind.DOUBLE) {
            return true;
        }
        double v = value.getDouble(trait.getUnit());
        return v >= minimum && v <= maximum;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(trait.getId());
        b.append(readable ? " r" : " -");
        b.append(writable ? "w" : "-");
        b.append(notifies ? "n" : "-");
        if (ranged) {
            b.append(' ').append(minimum).append("..").append(maximum);
            if (step != 0) {
                b.append(" step ").append(step);
            }
        }
        return b.toString();
    }
}
