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

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Properties the canonical trait table has to hold whatever is added to it.
 *
 * <p>Written as a sweep over {@link Trait#all()} rather than as a test per
 * constant, because the failure mode is a new trait being added with a
 * copy-pasted id or a unit that does not match its kind, and a test per
 * constant only covers the constants somebody remembered to write one for.</p>
 */
class TraitTableTest {

    @Test
    void everyTraitHasAUniqueNonEmptyId() {
        Set<String> seen = new HashSet<String>();
        for (Trait t : Trait.all()) {
            assertNotNull(t.getId(), "a trait with no id");
            assertTrue(t.getId().length() > 0, "a trait with an empty id");
            assertTrue(seen.add(t.getId()),
                    "two traits share the id " + t.getId()
                            + "; ids are the wire format and a persisted"
                            + " favourite would resolve to the wrong one");
        }
    }

    /**
     * The id crosses the native boundary and appears in build hints, so it has
     * to survive both. Anything outside lowercase, digits and underscore is a
     * character some encoder along the way will treat as structure.
     */
    @Test
    void everyIdIsSafeOnTheWireAndInABuildHint() {
        for (Trait t : Trait.all()) {
            String id = t.getId();
            for (int i = 0; i < id.length(); i++) {
                char c = id.charAt(i);
                boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                        || c == '_';
                assertTrue(ok, "the id " + id + " contains '" + c
                        + "', which is not safe in a tab-delimited record or"
                        + " a build hint");
            }
        }
    }

    @Test
    void forIdIsTotalAndRoundTripsEveryTrait() {
        for (Trait t : Trait.all()) {
            assertSame(t, Trait.forId(t.getId()));
        }
        assertNull(Trait.forId("no_such_trait"),
                "an unknown id must answer null rather than throwing: ids"
                        + " arrive from persisted state and from newer ports");
        assertNull(Trait.forId(null));
    }

    /**
     * A measured trait needs a real unit and a non-measured one must not
     * carry one, or a caller reading through getDouble(unit) gets a
     * dimension check against something meaningless.
     */
    @Test
    void unitsMatchKinds() {
        for (Trait t : Trait.all()) {
            if (t.getValueKind() == TraitValueKind.DOUBLE) {
                assertTrue(t.getUnit() != TraitUnit.NONE,
                        t.getId() + " is measured and has no unit");
            } else {
                assertSame(TraitUnit.NONE, t.getUnit(),
                        t.getId() + " is not measured but carries the unit "
                                + t.getUnit());
            }
        }
    }

    @Test
    void nominalRangesAreOrderedAndOnlyPresentWhereClaimed() {
        for (Trait t : Trait.all()) {
            if (t.hasNominalRange()) {
                assertTrue(t.getNominalMinimum() < t.getNominalMaximum(),
                        t.getId() + " has an inverted nominal range");
            } else {
                assertEquals(0.0, t.getNominalMinimum(), 0.0);
                assertEquals(0.0, t.getNominalMaximum(), 0.0);
            }
        }
    }

    /**
     * A "target" trait tells an accessory what to do, so writing one has to be
     * possible; a trait that only reports cannot be a target. Both halves are
     * checked because getting either backwards makes an app's controls
     * silently inert.
     */
    @Test
    void targetTraitsAreWritableAndCurrentReadingsAreNot() {
        for (Trait t : Trait.all()) {
            if (t.getId().startsWith("target_")) {
                assertFalse(t.isReadOnly(),
                        t.getId() + " names a target but cannot be written");
            }
        }
        assertTrue(Trait.CURRENT_TEMPERATURE.isReadOnly());
        assertTrue(Trait.CURRENT_HEATING_COOLING.isReadOnly());
        assertTrue(Trait.LOCK_STATE.isReadOnly());
        assertTrue(Trait.COVERING_POSITION.isReadOnly());
        assertTrue(Trait.BATTERY_LEVEL.isReadOnly());
        assertTrue(Trait.MOTION_DETECTED.isReadOnly());
        assertTrue(Trait.OUTLET_IN_USE.isReadOnly());
    }

    /**
     * Every proportional trait normalizes to percent, which is the promise
     * that keeps Matter's 0..254 level control and 0..10000 covering position
     * out of application code.
     */
    @Test
    void everyProportionIsInPercent() {
        assertSame(TraitUnit.PERCENT, Trait.BRIGHTNESS.getUnit());
        assertSame(TraitUnit.PERCENT, Trait.SATURATION.getUnit());
        assertSame(TraitUnit.PERCENT, Trait.COVERING_POSITION.getUnit());
        assertSame(TraitUnit.PERCENT, Trait.TARGET_COVERING_POSITION.getUnit());
        assertSame(TraitUnit.PERCENT, Trait.COVERING_TILT.getUnit());
        assertSame(TraitUnit.PERCENT, Trait.FAN_SPEED.getUnit());
        assertSame(TraitUnit.PERCENT, Trait.BATTERY_LEVEL.getUnit());
        assertSame(TraitUnit.PERCENT, Trait.CURRENT_HUMIDITY.getUnit());
        assertSame(TraitUnit.PERCENT, Trait.VOLUME.getUnit());
    }

    /**
     * Mireds rather than Kelvin, because both platforms use mireds natively.
     * Making Kelvin canonical would put a reciprocal on every read and write
     * in both ports for no gain.
     */
    @Test
    void colourTemperatureIsInMireds() {
        assertSame(TraitUnit.MIRED, Trait.COLOR_TEMPERATURE.getUnit());
        assertSame(TraitUnitDimension.COLOR_TEMPERATURE,
                Trait.COLOR_TEMPERATURE.getUnit().getDimension());
    }

    /**
     * Temperature setpoints and readings share one dimension, so a UI can
     * convert them all together, and a colour temperature is deliberately not
     * in it.
     */
    @Test
    void temperaturesShareADimensionThatColourDoesNot() {
        assertSame(TraitUnitDimension.TEMPERATURE,
                Trait.CURRENT_TEMPERATURE.getUnit().getDimension());
        assertSame(TraitUnitDimension.TEMPERATURE,
                Trait.TARGET_HEATING_TEMPERATURE.getUnit().getDimension());
        assertSame(TraitUnitDimension.TEMPERATURE,
                Trait.TARGET_COOLING_TEMPERATURE.getUnit().getDimension());
        assertFalse(Trait.COLOR_TEMPERATURE.getUnit()
                .isCompatibleWith(Trait.CURRENT_TEMPERATURE.getUnit()),
                "a colour temperature must not convert into a room"
                        + " temperature");
    }

    /**
     * The trait pairs a thermostat UI needs when the mode is AUTO, where
     * TARGET_TEMPERATURE reports no value at all.
     */
    @Test
    void theThermostatHasBothThresholdsAndNotOnlyOneSetpoint() {
        List<Trait> all = Trait.all();
        assertTrue(all.contains(Trait.TARGET_TEMPERATURE));
        assertTrue(all.contains(Trait.TARGET_HEATING_TEMPERATURE));
        assertTrue(all.contains(Trait.TARGET_COOLING_TEMPERATURE));
    }

    /**
     * Motion and occupancy are separate constants even though Matter reads
     * them off the same bit, so an app written against HomeKit's motion sensor
     * does not lose the feature on Android.
     */
    @Test
    void motionAndOccupancyAreBothPresent() {
        assertNotNull(Trait.forId("motion_detected"));
        assertNotNull(Trait.forId("occupancy_detected"));
    }

    @Test
    void theTableIsImmutable() {
        List<Trait> all = Trait.all();
        try {
            all.add(Trait.ON_OFF);
            org.junit.jupiter.api.Assertions.fail(
                    "the trait table must not be mutable from outside");
        } catch (UnsupportedOperationException expected) {
            // The point of the test.
            assertTrue(true);
        }
    }
}
