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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tagged-union value type: that each kind reads back through its own
 * getter, that the wrong getter is a loud failure rather than a plausible
 * number, and that unit handling refuses the conversions that would be
 * silently wrong.
 */
class TraitValueTest {

    @Test
    void eachKindReadsBackThroughItsOwnGetter() {
        assertTrue(TraitValue.of(true).getBoolean());
        assertFalse(TraitValue.of(false).getBoolean());
        assertEquals(7, TraitValue.of(7).getInt());
        assertEquals("hi", TraitValue.of("hi").getString());
        assertEquals(LockState.SECURED.ordinal(),
                TraitValue.ofEnum(LockState.SECURED).getEnumOrdinal());
        assertEquals(40.0,
                TraitValue.of(40, TraitUnit.PERCENT).getDouble(
                        TraitUnit.PERCENT),
                0.0001);
    }

    /**
     * The whole reason this is one class with kind-checked getters rather
     * than forty subclasses: under ParparVM a failed cast does not throw, it
     * hands the wrong object to the next instruction. An IllegalStateException
     * naming both kinds is the same failure on every platform.
     */
    @Test
    void theWrongGetterThrowsRatherThanReturningSomethingPlausible() {
        TraitValue percent = TraitValue.of(40, TraitUnit.PERCENT);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> percent.getBoolean());
        assertTrue(e.getMessage().indexOf("DOUBLE") >= 0,
                "the message must name the kind the value actually is: "
                        + e.getMessage());
        assertTrue(e.getMessage().indexOf("BOOLEAN") >= 0,
                "the message must name the kind that was asked for: "
                        + e.getMessage());
    }

    @Test
    void aBooleanIsNotReadableAsANumber() {
        TraitValue on = TraitValue.of(true);
        assertThrows(IllegalStateException.class,
                () -> on.getRawDouble());
    }

    @Test
    void aMeasuredValueNeedsAUnit() {
        assertThrows(IllegalArgumentException.class,
                () -> TraitValue.of(40, null));
    }

    /**
     * The Celsius/Fahrenheit round trip is the one conversion with an offset,
     * and getting the affine form wrong produces numbers that look reasonable
     * over a narrow range.
     */
    @Test
    void temperatureConvertsBothWays() {
        TraitValue c = TraitValue.of(20, TraitUnit.CELSIUS);
        assertEquals(68.0, c.getDouble(TraitUnit.FAHRENHEIT), 0.0001);
        TraitValue f = TraitValue.of(68, TraitUnit.FAHRENHEIT);
        assertEquals(20.0, f.getDouble(TraitUnit.CELSIUS), 0.0001);
        assertEquals(-40.0,
                TraitValue.of(-40, TraitUnit.CELSIUS).getDouble(
                        TraitUnit.FAHRENHEIT),
                0.0001);
    }

    /**
     * A cross-dimension read is a bug in the calling code, not a runtime
     * condition, so it throws rather than resolving through an AsyncResource.
     */
    @Test
    void readingAcrossDimensionsThrows() {
        TraitValue temperature = TraitValue.of(20, TraitUnit.CELSIUS);
        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class,
                        () -> temperature.getDouble(TraitUnit.PERCENT));
        assertTrue(e.getMessage().indexOf("different dimensions") >= 0,
                e.getMessage());
    }

    /**
     * Mireds and Kelvin are reciprocal, so they cannot be a unit pair. This
     * checks the named conversion rather than the table, and checks the
     * direction people get wrong: a HIGHER mired value is a WARMER light.
     */
    @Test
    void colorTemperatureConvertsToKelvinAndHigherMiredsAreWarmer() {
        TraitValue warm = TraitValue.of(370, TraitUnit.MIRED);
        TraitValue cold = TraitValue.of(153, TraitUnit.MIRED);
        assertEquals(1000000.0 / 370, warm.getColorTemperatureKelvin(),
                0.0001);
        assertTrue(warm.getColorTemperatureKelvin()
                        < cold.getColorTemperatureKelvin(),
                "more mireds must mean fewer kelvin, or the whole colour"
                        + " temperature story is backwards");
    }

    @Test
    void kelvinIsNotAUnitSoATemperatureIsNotAColour() {
        TraitValue celsius = TraitValue.of(20, TraitUnit.CELSIUS);
        assertThrows(IllegalArgumentException.class,
                () -> celsius.getColorTemperatureKelvin());
    }

    @Test
    void miredAndKelvinConversionsRefuseNonPositiveInput() {
        assertThrows(IllegalArgumentException.class,
                () -> TraitUnit.miredToKelvin(0));
        assertThrows(IllegalArgumentException.class,
                () -> TraitUnit.kelvinToMired(-1));
    }

    /**
     * The escape hatch for lossy mappings. Without it, every judgment call
     * made in the trait table would be a permanent lie to an app that needs
     * the platform's own answer.
     */
    @Test
    void theRawPlatformValueIsCarriedAlongsideTheCanonicalOne() {
        TraitValue plain = TraitValue.ofEnum(AirQualityLevel.MODERATE);
        assertFalse(plain.hasRawPlatformValue());
        assertEquals(0, plain.getRawPlatformValue());
        TraitValue withRaw = plain.withRawPlatformValue(3);
        assertTrue(withRaw.hasRawPlatformValue());
        assertEquals(3, withRaw.getRawPlatformValue());
        assertEquals(AirQualityLevel.MODERATE.ordinal(),
                withRaw.getEnumOrdinal());
        assertFalse(plain.hasRawPlatformValue(),
                "withRawPlatformValue must copy rather than mutate");
    }

    @Test
    void equalityCoversUnitAndRawValue() {
        assertEquals(TraitValue.of(40, TraitUnit.PERCENT),
                TraitValue.of(40, TraitUnit.PERCENT));
        assertEquals(TraitValue.of(40, TraitUnit.PERCENT).hashCode(),
                TraitValue.of(40, TraitUnit.PERCENT).hashCode());
        assertNotEquals(TraitValue.of(20, TraitUnit.CELSIUS),
                TraitValue.of(20, TraitUnit.FAHRENHEIT));
        assertNotEquals(TraitValue.of(1), TraitValue.of(true));
    }

    /**
     * A pairing secret must not reach a log through the obvious route, and the
     * same discipline applies here: toString is used in log lines, so it
     * renders the unit rather than a bare number that could be misread.
     */
    @Test
    void toStringNamesTheUnitForAMeasuredValue() {
        assertTrue(TraitValue.of(40, TraitUnit.PERCENT).toString()
                .indexOf("PERCENT") >= 0);
        assertEquals("true", TraitValue.of(true).toString());
    }
}
