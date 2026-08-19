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

/// The state of a door or garage door, for [Trait#DOOR_STATE] and
/// [Trait#TARGET_DOOR_STATE].
///
/// #### Backend support
///
/// HomeKit publishes this directly. The Google Home APIs have an equivalent
/// through Google's own garage-door trait. **Matter has no standard garage
/// cluster** in the revisions shipping ecosystems support, so an Android build
/// running on Play services Matter commissioning alone -- see
/// [HomeAvailability#COMMISSIONING_ONLY] -- cannot see a garage door at all.
public enum DoorState {

    /// Fully open.
    OPEN,

    /// Fully closed.
    CLOSED,

    /// Moving toward [#OPEN]. A **report** only; not writable.
    OPENING,

    /// Moving toward [#CLOSED]. A **report** only; not writable.
    CLOSING,

    /// Halted part-way, usually because the user stopped it or an obstruction
    /// was hit. Check [Trait#OBSTRUCTION_DETECTED]. A **report** only.
    STOPPED,

    /// The accessory could not say.
    UNKNOWN;

    /// Reads a door state out of a trait value, total: a value that is not an
    /// enum, or whose ordinal is outside this set, answers [#UNKNOWN].
    ///
    /// #### Parameters
    ///
    /// - `value`: a value read from [Trait#DOOR_STATE] or
    ///   [Trait#TARGET_DOOR_STATE], or `null`
    ///
    /// #### Returns
    ///
    /// the state, never `null`
    public static DoorState of(TraitValue value) {
        if (value == null || value.getKind() != TraitValueKind.ENUM) {
            return UNKNOWN;
        }
        int ordinal = value.getEnumOrdinal();
        DoorState[] all = values();
        if (ordinal < 0 || ordinal >= all.length) {
            return UNKNOWN;
        }
        return all[ordinal];
    }

    /// Whether this state may be written to [Trait#TARGET_DOOR_STATE].
    ///
    /// #### Returns
    ///
    /// `true` for [#OPEN] and [#CLOSED] only
    public boolean isWritable() {
        return this == OPEN || this == CLOSED;
    }
}
