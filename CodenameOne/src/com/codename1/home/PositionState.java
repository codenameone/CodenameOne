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

/// Which way a window covering is moving, for [Trait#COVERING_MOTION].
/// Read-only on every backend.
///
/// Phrased in terms of opening and closing rather than of the position number
/// going up or down, because the two backends disagree about which direction
/// that number runs -- see [Trait#COVERING_POSITION]. "Opening" here always
/// means moving toward daylight.
public enum PositionState {

    /// Not moving.
    STOPPED,

    /// Moving toward fully open.
    OPENING,

    /// Moving toward fully closed.
    CLOSING,

    /// The accessory could not say.
    UNKNOWN;

    /// Reads a motion state out of a trait value, total: a value that is not
    /// an enum, or whose ordinal is outside this set, answers [#UNKNOWN].
    ///
    /// #### Parameters
    ///
    /// - `value`: a value read from [Trait#COVERING_MOTION], or `null`
    ///
    /// #### Returns
    ///
    /// the state, never `null`
    public static PositionState of(TraitValue value) {
        if (value == null || value.getKind() != TraitValueKind.ENUM) {
            return UNKNOWN;
        }
        int ordinal = value.getEnumOrdinal();
        PositionState[] all = values();
        if (ordinal < 0 || ordinal >= all.length) {
            return UNKNOWN;
        }
        return all[ordinal];
    }
}
