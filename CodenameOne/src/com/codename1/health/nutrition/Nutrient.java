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
package com.codename1.health.nutrition;

import com.codename1.health.HealthUnit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A single nutrient that a [NutritionSample] can carry -- a macronutrient,
/// a vitamin, a mineral.
///
/// Interned constants, like [com.codename1.health.HealthDataType], so `==`
/// is a valid identity test and [#forId(String)] stays total across
/// framework versions.
///
/// Each nutrient declares the unit its amounts are expressed in. That is
/// not cosmetic: sodium is conventionally logged in milligrams and protein
/// in grams, and a food database that mixes them up is off by a thousand.
public final class Nutrient {

    private static final Map<String, Nutrient> BY_ID =
            new HashMap<String, Nutrient>();
    private static final List<Nutrient> ALL = new ArrayList<Nutrient>();

    private final String id;
    private final HealthUnit unit;

    private Nutrient(String id, HealthUnit unit) {
        this.id = id;
        this.unit = unit;
    }

    private static Nutrient define(String id, HealthUnit unit) {
        Nutrient n = new Nutrient(id, unit);
        BY_ID.put(id, n);
        ALL.add(n);
        return n;
    }

    // --- energy and macronutrients ---

    /// Food energy. Note that this is also available as a standalone type,
    /// [com.codename1.health.HealthDataType#DIETARY_ENERGY]; writing a
    /// [NutritionSample] that carries it makes it part of a logged meal
    /// rather than a bare total.
    public static final Nutrient ENERGY =
            define("energy", HealthUnit.KILOCALORIE);
    public static final Nutrient PROTEIN = define("protein", HealthUnit.GRAM);
    public static final Nutrient TOTAL_FAT =
            define("total_fat", HealthUnit.GRAM);
    public static final Nutrient SATURATED_FAT =
            define("saturated_fat", HealthUnit.GRAM);
    public static final Nutrient MONOUNSATURATED_FAT =
            define("monounsaturated_fat", HealthUnit.GRAM);
    public static final Nutrient POLYUNSATURATED_FAT =
            define("polyunsaturated_fat", HealthUnit.GRAM);
    public static final Nutrient TRANS_FAT =
            define("trans_fat", HealthUnit.GRAM);
    public static final Nutrient CHOLESTEROL =
            define("cholesterol", HealthUnit.MILLIGRAM);
    public static final Nutrient TOTAL_CARBOHYDRATE =
            define("total_carbohydrate", HealthUnit.GRAM);
    public static final Nutrient DIETARY_FIBER =
            define("dietary_fiber", HealthUnit.GRAM);
    public static final Nutrient SUGAR = define("sugar", HealthUnit.GRAM);
    public static final Nutrient WATER = define("water", HealthUnit.LITER);

    // --- minerals ---

    public static final Nutrient SODIUM =
            define("sodium", HealthUnit.MILLIGRAM);
    public static final Nutrient POTASSIUM =
            define("potassium", HealthUnit.MILLIGRAM);
    public static final Nutrient CALCIUM =
            define("calcium", HealthUnit.MILLIGRAM);
    public static final Nutrient IRON = define("iron", HealthUnit.MILLIGRAM);
    public static final Nutrient MAGNESIUM =
            define("magnesium", HealthUnit.MILLIGRAM);
    public static final Nutrient ZINC = define("zinc", HealthUnit.MILLIGRAM);
    public static final Nutrient PHOSPHORUS =
            define("phosphorus", HealthUnit.MILLIGRAM);
    public static final Nutrient IODINE =
            define("iodine", HealthUnit.MICROGRAM);
    public static final Nutrient SELENIUM =
            define("selenium", HealthUnit.MICROGRAM);
    public static final Nutrient COPPER =
            define("copper", HealthUnit.MILLIGRAM);
    public static final Nutrient MANGANESE =
            define("manganese", HealthUnit.MILLIGRAM);
    public static final Nutrient CHROMIUM =
            define("chromium", HealthUnit.MICROGRAM);
    public static final Nutrient MOLYBDENUM =
            define("molybdenum", HealthUnit.MICROGRAM);
    public static final Nutrient CHLORIDE =
            define("chloride", HealthUnit.MILLIGRAM);

    // --- vitamins ---

    public static final Nutrient VITAMIN_A =
            define("vitamin_a", HealthUnit.MICROGRAM);
    public static final Nutrient THIAMIN =
            define("thiamin", HealthUnit.MILLIGRAM);
    public static final Nutrient RIBOFLAVIN =
            define("riboflavin", HealthUnit.MILLIGRAM);
    public static final Nutrient NIACIN =
            define("niacin", HealthUnit.MILLIGRAM);
    public static final Nutrient PANTOTHENIC_ACID =
            define("pantothenic_acid", HealthUnit.MILLIGRAM);
    public static final Nutrient VITAMIN_B6 =
            define("vitamin_b6", HealthUnit.MILLIGRAM);
    public static final Nutrient BIOTIN =
            define("biotin", HealthUnit.MICROGRAM);
    public static final Nutrient VITAMIN_B12 =
            define("vitamin_b12", HealthUnit.MICROGRAM);
    public static final Nutrient VITAMIN_C =
            define("vitamin_c", HealthUnit.MILLIGRAM);
    public static final Nutrient VITAMIN_D =
            define("vitamin_d", HealthUnit.MICROGRAM);
    public static final Nutrient VITAMIN_E =
            define("vitamin_e", HealthUnit.MILLIGRAM);
    public static final Nutrient VITAMIN_K =
            define("vitamin_k", HealthUnit.MICROGRAM);
    public static final Nutrient FOLATE =
            define("folate", HealthUnit.MICROGRAM);

    // --- other ---

    public static final Nutrient CAFFEINE =
            define("caffeine", HealthUnit.MILLIGRAM);

    /// The stable, portable identifier for this nutrient.
    public String getId() {
        return id;
    }

    /// The unit amounts of this nutrient are expressed in.
    public HealthUnit getUnit() {
        return unit;
    }

    /// Looks a nutrient up by [#getId()], or `null` when unknown to this
    /// version of the framework.
    public static Nutrient forId(String id) {
        return id == null ? null : BY_ID.get(id);
    }

    /// Every nutrient this framework understands.
    public static List<Nutrient> values() {
        return Collections.unmodifiableList(ALL);
    }

    @Override
    public String toString() {
        return id;
    }
}
