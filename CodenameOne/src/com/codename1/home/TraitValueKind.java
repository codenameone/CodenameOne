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

/// What sort of value a [Trait] carries, and therefore which
/// [TraitValue] getter is the one that works.
///
/// #### There is deliberately no TLV
///
/// HomeKit characteristics can carry TLV8 blobs, which is how accessory
/// vendors ship extensions outside the published characteristic set. Those
/// have no portable meaning -- decoding one requires the vendor's own schema,
/// and there is nothing on the Matter side to map them to. Exposing a byte
/// array here would look like a capability and be an invitation to write
/// iOS-only code inside a cross-platform API. A trait whose platform value is
/// TLV8 is simply not in the [Trait] table.
public enum TraitValueKind {

    /// True or false. Read with [TraitValue#getBoolean()].
    BOOLEAN,

    /// A whole number with no unit. Read with [TraitValue#getInt()].
    INT,

    /// A measured quantity with a [TraitUnit]. Read with
    /// [TraitValue#getDouble(TraitUnit)], which makes you name the unit you
    /// expect.
    DOUBLE,

    /// Free text. Read with [TraitValue#getString()].
    STRING,

    /// One of a fixed set, carried as an ordinal into one of this package's
    /// domain enums -- [LockState], [HeatingCoolingMode], [AirQualityLevel]
    /// and the rest. Read it through that enum's own lookup rather than
    /// through [TraitValue#getEnumOrdinal()], which exists for the codec.
    ENUM
}
