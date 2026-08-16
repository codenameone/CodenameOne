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

/// The state of a door lock, for [Trait#LOCK_STATE] and
/// [Trait#TARGET_LOCK_STATE].
///
/// Only [#SECURED] and [#UNSECURED] may be written; the rest describe
/// situations an accessory reports and a caller cannot ask for.
public enum LockState {

    /// Locked.
    ///
    /// Maps to HomeKit's `secured` and Matter's `Locked`.
    SECURED,

    /// Unlocked.
    ///
    /// Maps to HomeKit's `unsecured` and to both of Matter's `Unlocked` and
    /// `Unlatched` -- the latter is a 1.4 addition meaning the latch has been
    /// pulled back as well as the bolt, which HomeKit has no way to say.
    UNSECURED,

    /// The bolt is partly thrown: not locked, and not simply open either.
    ///
    /// Matter's `NotFullyLocked`. Its own constant rather than being folded
    /// into a neighbour, because both alternatives lose real information --
    /// calling it [#JAMMED] asserts a fault that may not exist, and calling it
    /// [#UNKNOWN] throws away a state the accessory was specific about.
    ///
    /// **HomeKit never produces this.**
    PARTIALLY_LOCKED,

    /// The mechanism is stuck.
    ///
    /// **Unreachable outside HomeKit.** HomeKit publishes a jam as a value of
    /// the lock characteristic, so it arrives here. Matter reports it as a
    /// `LockOperationError` or `DoorLockAlarm` *event*, and this release does
    /// not claim Matter events -- so a jammed Matter lock reports
    /// [#PARTIALLY_LOCKED] or [#UNKNOWN] instead. Do not build a safety
    /// feature on this constant.
    JAMMED,

    /// The accessory could not say. Also what a nullable Matter `LockState`
    /// resolves to when it is null.
    UNKNOWN;

    /// Reads a lock state out of a trait value, total: a value that is not an
    /// enum, or whose ordinal is outside this set, answers [#UNKNOWN].
    ///
    /// #### Parameters
    ///
    /// - `value`: a value read from [Trait#LOCK_STATE] or
    ///   [Trait#TARGET_LOCK_STATE], or `null`
    ///
    /// #### Returns
    ///
    /// the state, never `null`
    public static LockState of(TraitValue value) {
        if (value == null || value.getKind() != TraitValueKind.ENUM) {
            return UNKNOWN;
        }
        int ordinal = value.getEnumOrdinal();
        LockState[] all = values();
        if (ordinal < 0 || ordinal >= all.length) {
            return UNKNOWN;
        }
        return all[ordinal];
    }

    /// Whether this state may be written to [Trait#TARGET_LOCK_STATE].
    ///
    /// #### Returns
    ///
    /// `true` for [#SECURED] and [#UNSECURED] only
    public boolean isWritable() {
        return this == SECURED || this == UNSECURED;
    }
}
