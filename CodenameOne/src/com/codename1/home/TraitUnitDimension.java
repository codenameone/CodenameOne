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

/// What a [TraitUnit] measures. Two units convert into one another only when
/// they share a dimension; asking for anything else is a bug in the calling
/// code and throws.
public enum TraitUnitDimension {

    /// A bare number with no unit -- an ordinal, a count, a boolean carried
    /// as a number.
    DIMENSIONLESS,

    /// A proportion. Canonical unit [TraitUnit#PERCENT].
    RATIO,

    /// Temperature. Canonical unit [TraitUnit#CELSIUS]. The only dimension
    /// here that needs the affine offset.
    TEMPERATURE,

    /// A plane angle, used for colour hue. Canonical unit
    /// [TraitUnit#ARC_DEGREE].
    ANGLE,

    /// The correlated colour temperature of a light. Canonical unit
    /// [TraitUnit#MIRED].
    ///
    /// Its own dimension rather than a member of [#TEMPERATURE], which it
    /// resembles only by name: a light at 250 mireds is not 250 degrees of
    /// anything, and letting the two convert would turn a thermostat setpoint
    /// into a colour by arithmetic that raises no error.
    COLOR_TEMPERATURE,

    /// Illuminance. Canonical unit [TraitUnit#LUX].
    ILLUMINANCE,

    /// A concentration expressed as a fraction of the whole -- parts per
    /// million and its relatives. Canonical unit [TraitUnit#PPM].
    ///
    /// Deliberately does **not** convert to [#CONCENTRATION_MASS]. Going
    /// between parts-per and micrograms per cubic metre needs the molar mass
    /// of the gas and the ambient temperature and pressure, none of which
    /// this API has. Accessories report one or the other; where the platform
    /// gives a value in the dimension the trait does not use, the port
    /// reports no value rather than inventing the missing physics.
    CONCENTRATION_PARTS,

    /// A concentration expressed as mass per volume, used for particulate
    /// matter. Canonical unit [TraitUnit#MICROGRAM_PER_CUBIC_METER].
    CONCENTRATION_MASS
}
