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

import com.codename1.health.HealthDataType;
import com.codename1.health.HealthQuantity;
import com.codename1.health.HealthUnit;
import com.codename1.health.SessionSample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A logged food or meal, carrying whichever nutrients are known about it.
///
/// #### Sparse by design
///
/// A [NutritionSample] holds only the nutrients actually set. A logged
/// apple sets four fields; a packaged food scanned from a barcode might set
/// thirty. Modelling this as forty nullable fields -- which is roughly what
/// both platforms do -- would make every consumer write forty null checks,
/// so this exposes a map instead and [#getNutrients()] returns only what is
/// present.
///
/// ```java
/// NutritionSample lunch = NutritionSample.create(start, end);
/// lunch.setTitle("Chicken salad");
/// lunch.setNutrient(Nutrient.ENERGY, 420);
/// lunch.setNutrient(Nutrient.PROTEIN, 35);
/// lunch.setNutrient(Nutrient.SODIUM, 610);
/// ```
///
/// #### Meal type
///
/// Health Connect records which meal an entry belongs to and HealthKit
/// does not, so [#getMealType()] round-trips through the local store and
/// would ride in sample metadata on iOS once that mapping exists.
///
/// #### Local and simulator only in this release
///
/// Neither phone carries this shape yet -- see the package
/// documentation. A read or write of
/// [HealthDataType#NUTRITION] on iOS or Android is refused with
/// [HealthError#TYPE_NOT_SUPPORTED]; everything here works against the
/// local store.
public final class NutritionSample extends SessionSample {

    /// The entry is not attributed to a particular meal.
    public static final int MEAL_UNKNOWN = 0;
    /// Breakfast.
    public static final int MEAL_BREAKFAST = 1;
    /// Lunch.
    public static final int MEAL_LUNCH = 2;
    /// Dinner.
    public static final int MEAL_DINNER = 3;
    /// A snack between meals.
    public static final int MEAL_SNACK = 4;

    private final Map<String, HealthQuantity> nutrients =
            new HashMap<String, HealthQuantity>();
    private int mealType = MEAL_UNKNOWN;
    private String foodName;

    private NutritionSample(long startMillis, long endMillis) {
        super(HealthDataType.NUTRITION, startMillis, endMillis);
    }

    /// A food or meal consumed over `[startMillis, endMillis]`.
    public static NutritionSample create(long startMillis, long endMillis) {
        return new NutritionSample(startMillis, endMillis);
    }

    /// A food or meal logged at one moment. Nutrition is interval-only, so
    /// this records a one-second span rather than a zero-width one.
    public static NutritionSample create(long atMillis) {
        return new NutritionSample(atMillis, atMillis + 1000);
    }

    /// Sets a nutrient amount, expressed in that nutrient's own unit --
    /// see [Nutrient#getUnit()].
    public NutritionSample setNutrient(Nutrient nutrient, double amount) {
        if (nutrient == null) {
            return this;
        }
        nutrients.put(nutrient.getId(),
                new HealthQuantity(amount, nutrient.getUnit()));
        return this;
    }

    /// Sets a nutrient amount in an explicit unit, converting into the
    /// nutrient's own unit.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `unit` measures a different
    ///   dimension than the nutrient -- protein in millilitres, say.
    public NutritionSample setNutrient(Nutrient nutrient, double amount,
            HealthUnit unit) {
        if (nutrient == null) {
            return this;
        }
        double converted =
                HealthUnit.convert(amount, unit, nutrient.getUnit());
        nutrients.put(nutrient.getId(),
                new HealthQuantity(converted, nutrient.getUnit()));
        return this;
    }

    /// The amount of `nutrient`, or `null` when this entry does not record
    /// it.
    ///
    /// Null rather than zero, for the same reason aggregate buckets return
    /// null: "this food's sodium was never measured" and "this food
    /// contains no sodium" are different claims, and only one of them is
    /// safe to show someone managing their intake.
    public HealthQuantity getNutrient(Nutrient nutrient) {
        return nutrient == null ? null : nutrients.get(nutrient.getId());
    }

    /// Whether `nutrient` is recorded on this entry.
    public boolean hasNutrient(Nutrient nutrient) {
        return nutrient != null && nutrients.containsKey(nutrient.getId());
    }

    /// The nutrients actually recorded, in no particular order.
    public List<Nutrient> getNutrients() {
        List<Nutrient> out = new ArrayList<Nutrient>();
        for (String id : nutrients.keySet()) {
            Nutrient n = Nutrient.forId(id);
            if (n != null) {
                out.add(n);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /// How many nutrients this entry records.
    public int getNutrientCount() {
        return nutrients.size();
    }

    /// Which meal this belongs to, as a `MEAL_` constant.
    public int getMealType() {
        return mealType;
    }

    /// Attributes this entry to a meal, using a `MEAL_` constant.
    public NutritionSample setMealType(int mealType) {
        this.mealType = mealType;
        return this;
    }

    /// The food's name, or null.
    public String getFoodName() {
        return foodName;
    }

    /// Names the food.
    public NutritionSample setFoodName(String foodName) {
        this.foodName = foodName;
        return this;
    }

    @Override
    public String toString() {
        return "NutritionSample[" + (foodName == null ? "food" : foodName)
                + ", " + nutrients.size() + " nutrients]";
    }
}
