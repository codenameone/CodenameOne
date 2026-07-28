/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

/// Logged food and drink, as a sparse set of nutrient amounts.
///
/// Start at [com.codename1.health.nutrition.NutritionSample], written and
/// read through the ordinary [com.codename1.health.HealthStore] using
/// [com.codename1.health.HealthDataType#NUTRITION].
///
/// Both platforms model nutrition as a record with several dozen optional
/// nutrient fields. This package keeps that sparseness explicit rather than
/// exposing forty nullable getters: an entry carries only the nutrients
/// that were actually measured, and a nutrient that was never measured
/// reads back as null rather than zero.
///
/// #### Local and simulator only in this release
///
/// [com.codename1.health.HealthDataType#NUTRITION] is **not** carried to
/// HealthKit or Health Connect yet: neither the wire format nor either
/// port's type map knows the multi-nutrient record shape, so a read or a
/// write of it on a phone is refused with
/// [com.codename1.health.HealthError#TYPE_NOT_SUPPORTED] before it
/// reaches the platform. It works fully against the local store -- the
/// simulator, and the desktop, Windows, Linux and JavaScript ports.
///
/// Individual dietary quantities do reach the phones and are ordinary
/// [com.codename1.health.QuantitySample]s rather than entries here:
/// `HYDRATION` on both platforms, and `DIETARY_ENERGY` on iOS. Use those
/// when the number has to land in the user's real health store.
package com.codename1.health.nutrition;
