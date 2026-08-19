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

import com.codename1.impl.home.HomeWire;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The encoding the SPI speaks.
 *
 * <p>The theme running through these is <b>totality</b>: every decoder here is
 * fed something malformed and has to answer rather than throw. Records arrive
 * from native code in deliveries that carry good rows alongside bad ones, and
 * a parser that threw would discard the lot.</p>
 */
class HomeWireTest {

    @Test
    void splitPreservesTrailingEmptyFields() {
        // String.split drops these, and an accessory with no room and no
        // bridge ends in exactly this shape -- losing them shifts every
        // index after the gap.
        String[] f = HomeWire.split("a\tb\t\t");
        assertEquals(4, f.length);
        assertEquals("a", f[0]);
        assertEquals("b", f[1]);
        assertEquals("", f[2]);
        assertEquals("", f[3]);
    }

    @Test
    void fieldAccessorsAreTotal() {
        String[] f = HomeWire.split("a\tb");
        assertEquals("", HomeWire.field(f, 9));
        assertEquals("", HomeWire.field(null, 0));
        assertEquals("", HomeWire.field(f, -1));
        assertFalse(HomeWire.flag(f, 9));
        assertEquals(7, HomeWire.integer(f, 9, 7));
        assertEquals(7, HomeWire.integer(HomeWire.split("x"), 0, 7),
                "a field that is not a number must fall back rather than"
                        + " throw");
        assertEquals(1.5, HomeWire.real(HomeWire.split("nope"), 0, 1.5),
                0.0001);
    }

    /**
     * A user's accessory name is the one field that carries arbitrary text.
     * A tab in it would split one record into two and shift everything after
     * it, so the name loses its tab rather than the record losing its shape.
     */
    @Test
    void sanitizeKeepsUserTextFromCorruptingARecord() {
        assertEquals("a b", HomeWire.sanitize("a\tb"));
        assertEquals("a b", HomeWire.sanitize("a\nb"));
        assertEquals("a b", HomeWire.sanitize("a\rb"));
        assertEquals("", HomeWire.sanitize(null));
        assertEquals("plain", HomeWire.sanitize("plain"));
        String[] fields = HomeWire.split(
                HomeWire.join(new String[] {"id", "Kitchen\tLight"}));
        assertEquals(2, fields.length,
                "a tab inside a name must not create a third field");
        assertEquals("Kitchen Light", fields[1]);
    }

    @Test
    void listFieldsDropEmptiesAndTrim() {
        assertEquals(2, HomeWire.list("a, b").size());
        assertEquals("b", HomeWire.list("a, b").get(1));
        assertTrue(HomeWire.list("").isEmpty());
        assertTrue(HomeWire.list(null).isEmpty());
        assertTrue(HomeWire.list(",,").isEmpty());
    }

    /**
     * Errors cross by name, never by ordinal. A port built against a version
     * where a constant was inserted mid-enum would otherwise map every error
     * past it onto the wrong one, and a mis-mapped authorization failure looks
     * exactly like a mis-mapped timeout downstream.
     */
    @Test
    void errorsDecodeByNameAndDegradeRatherThanThrow() {
        assertNull(HomeWire.decodeError(null));
        assertNull(HomeWire.decodeError(""));
        HomeException e = HomeWire.decodeError("PIN_REQUIRED\tneeds a code");
        assertNotNull(e);
        assertSame(HomeError.PIN_REQUIRED, e.getError());
        assertEquals("needs a code", e.getMessage());
        HomeException unknown =
                HomeWire.decodeError("SOMETHING_A_NEWER_PORT_KNOWS\tdetail");
        assertSame(HomeError.UNKNOWN, unknown.getError());
        assertEquals("detail", unknown.getMessage(),
                "the platform's own text has to survive an unrecognized name,"
                        + " or the one useful part of the failure is lost");
    }

    @Test
    void anErrorWithNoMessageFallsBackToItsName() {
        assertEquals("TIMEOUT",
                HomeWire.decodeError("TIMEOUT").getMessage());
    }

    @Test
    void readingsRoundTripThroughTheWire() {
        TraitReading original = TraitReading.of("acc", "1", Trait.BRIGHTNESS,
                TraitValue.of(42, TraitUnit.PERCENT), 1234L);
        TraitReading decoded =
                HomeWire.decodeReading(HomeWire.encodeReading(original));
        assertNotNull(decoded);
        assertEquals("acc", decoded.getAccessoryId());
        assertEquals("1", decoded.getServiceId());
        assertSame(Trait.BRIGHTNESS, decoded.getTrait());
        assertTrue(decoded.hasValue());
        assertEquals(42.0, decoded.getValue().getDouble(TraitUnit.PERCENT),
                0.0001);
        assertEquals(1234L, decoded.getTimestampMillis());
    }

    /**
     * The third outcome. An accessory with nothing to report is not an error
     * and is not a zero, and the wire has to carry that distinction or a
     * thermostat that has never measured renders as freezing.
     */
    @Test
    void anAbsentValueSurvivesTheRoundTripAsAbsentRatherThanAsZero() {
        TraitReading absent =
                TraitReading.absent("acc", "1", Trait.CURRENT_TEMPERATURE);
        TraitReading decoded =
                HomeWire.decodeReading(HomeWire.encodeReading(absent));
        assertNotNull(decoded);
        assertFalse(decoded.hasValue());
        assertNull(decoded.getValue());
        assertFalse(decoded.isFailed(),
                "no value is not the same as a failure");
    }

    @Test
    void aFailedReadingCarriesItsErrorAcross() {
        TraitReading failed = TraitReading.failed("acc", "1", Trait.ON_OFF,
                HomeError.ACCESSORY_UNREACHABLE, "offline");
        TraitReading decoded =
                HomeWire.decodeReading(HomeWire.encodeReading(failed));
        assertNotNull(decoded);
        assertTrue(decoded.isFailed());
        assertSame(HomeError.ACCESSORY_UNREACHABLE, decoded.getError());
        assertEquals("offline", decoded.getErrorMessage());
        assertFalse(decoded.hasValue());
    }

    @Test
    void enumAndBooleanReadingsRoundTrip() {
        TraitReading lock = TraitReading.of("l", "1", Trait.LOCK_STATE,
                TraitValue.ofEnum(LockState.PARTIALLY_LOCKED), 0);
        TraitReading decodedLock =
                HomeWire.decodeReading(HomeWire.encodeReading(lock));
        assertSame(LockState.PARTIALLY_LOCKED,
                LockState.of(decodedLock.getValue()));

        TraitReading on = TraitReading.of("s", "1", Trait.ON_OFF,
                TraitValue.of(true), 0);
        TraitReading decodedOn =
                HomeWire.decodeReading(HomeWire.encodeReading(on));
        assertTrue(decodedOn.getValue().getBoolean());
    }

    /**
     * A decoded reading equals the value an app builds for the same constant.
     *
     * <p>The wire carries an ordinal and nothing else, so a decoded enum
     * value used to arrive with no constant name while the value an app
     * builds with {@code ofEnum} carries one -- and the two were therefore
     * unequal. Comparing a reading against a desired state is the most
     * ordinary thing anyone does with one, and it was false every time.</p>
     */
    @Test
    void aDecodedEnumEqualsTheValueAnAppBuilds() {
        TraitReading secured = TraitReading.of("l", "1", Trait.LOCK_STATE,
                TraitValue.ofEnum(LockState.SECURED), 0);
        TraitValue decoded =
                HomeWire.decodeReading(HomeWire.encodeReading(secured))
                        .getValue();

        assertEquals(TraitValue.ofEnum(LockState.SECURED), decoded,
                "a decoded SECURED must equal the SECURED an app builds");
        assertEquals(TraitValue.ofEnum(LockState.SECURED).hashCode(),
                decoded.hashCode(),
                "and hash with it, or a map keyed on values misses");
        assertEquals("SECURED", decoded.getEnumName(),
                "the trait names the ordinal the wire carried");
    }

    /**
     * A trait a newer port knows and this build does not is a row to skip, not
     * a delivery to abandon.
     */
    @Test
    void anUnknownTraitSkipsItsRowRatherThanFailingTheDelivery() {
        assertNull(HomeWire.decodeReading(
                "acc\t1\ta_trait_from_the_future\t0\t1\t\t0\t\t1\t0\t\t"));
        assertNull(HomeWire.decodeReading("garbage"));
        assertNull(HomeWire.decodeReading(""));
    }

    /**
     * The trait decides the kind, not the wire. A port and this build
     * disagreeing is a bug worth surfacing rather than papering over by
     * trusting whoever spoke last.
     */
    @Test
    void aKindThatContradictsTheTraitIsRefused() {
        assertNull(HomeWire.decodeValue(Trait.ON_OFF,
                TraitValueKind.DOUBLE.ordinal(), 1, "", 0, 0, false));
        assertNull(HomeWire.decodeValue(Trait.ON_OFF, -1, 1, "", 0, 0, false));
        assertNull(HomeWire.decodeValue(Trait.ON_OFF, 99, 1, "", 0, 0, false));
        assertNull(HomeWire.decodeValue(null, 0, 1, "", 0, 0, false));
    }

    /**
     * A unit wire id this build does not have is refused.
     *
     * <p>This used to fall back to the trait's canonical unit, on the
     * reasoning that the two sides agree about what the trait measures. They
     * do -- but not about the scale: a future temperature unit carries a
     * number that is not degrees Celsius, and relabelling it Celsius is a
     * guess presented as a fact. The decoder cannot read the value, which is
     * what INVALID_DATA means.</p>
     */
    @Test
    void anUnknownUnitIsRefusedRatherThanRelabelled() {
        assertNull(HomeWire.decodeValue(Trait.BRIGHTNESS,
                TraitValueKind.DOUBLE.ordinal(), 40, "", 9999, 0, false));
        // The known one still decodes, so this is not refusing everything.
        TraitValue ok = HomeWire.decodeValue(Trait.BRIGHTNESS,
                TraitValueKind.DOUBLE.ordinal(), 40, "",
                TraitUnit.PERCENT.getWireId(), 0, false);
        assertNotNull(ok);
        assertSame(TraitUnit.PERCENT, ok.getUnit());
    }

    @Test
    void traitConstraintsDecodeWithAndWithoutARange() {
        TraitConstraint ranged = HomeWire.decodeTraitConstraint(
                "brightness\t1\t1\t1\t1\t10\t100\t5\t");
        assertNotNull(ranged);
        assertTrue(ranged.hasRange());
        assertEquals(10.0, ranged.getMinimum(), 0.0001);
        assertEquals(100.0, ranged.getMaximum(), 0.0001);
        assertEquals(5.0, ranged.getStep(), 0.0001);
        assertTrue(ranged.isWritable());

        TraitConstraint plain =
                HomeWire.decodeTraitConstraint("on_off\t1\t1\t0\t0\t\t\t\t");
        assertNotNull(plain);
        assertFalse(plain.hasRange());
        assertFalse(plain.notifiesOnChange());

        assertNull(HomeWire.decodeTraitConstraint("not_a_trait\t1\t1\t1\t0"));
    }

    @Test
    void enumConstraintsCarryTheirValidOrdinals() {
        TraitConstraint c = HomeWire.decodeTraitConstraint(
                "target_lock_state\t1\t1\t1\t0\t\t\t\t0,1");
        assertNotNull(c);
        assertEquals(2, c.getValidOrdinals().size());
        assertTrue(c.accepts(TraitValue.ofEnum(LockState.SECURED)));
        assertFalse(c.accepts(TraitValue.ofEnum(LockState.JAMMED)),
                "a lock that lists only secured and unsecured must refuse a"
                        + " write of jammed");
    }

    /**
     * One unreadable ordinal widens what the accessory is believed to accept
     * rather than narrowing it, because an empty list already means "did not
     * say" and refusing writes on the strength of a parse failure would break
     * writes that work.
     */
    @Test
    void anUnreadableOrdinalIsDroppedRatherThanPoisoningTheList() {
        TraitConstraint c = HomeWire.decodeTraitConstraint(
                "target_lock_state\t1\t1\t1\t0\t\t\t\t0,x,1");
        assertNotNull(c);
        assertEquals(2, c.getValidOrdinals().size());

        TraitConstraint allBad = HomeWire.decodeTraitConstraint(
                "target_lock_state\t1\t1\t1\t0\t\t\t\tx,y");
        assertNotNull(allBad);
        assertTrue(allBad.getValidOrdinals().isEmpty());
        assertTrue(allBad.accepts(TraitValue.ofEnum(LockState.SECURED)),
                "with nothing readable the constraint must fall back to"
                        + " 'did not say' rather than 'accepts nothing'");
    }

    @Test
    void graphRecordsDecodeAndTolerateMissingIdentifiers() {
        assertNotNull(HomeWire.decodeRoom("r1\tKitchen", "h1"));
        assertEquals("h1", HomeWire.decodeRoom("r1\tKitchen", "h1")
                .getStructureId());
        assertNull(HomeWire.decodeRoom("\tKitchen", "h1"));
        assertNull(HomeWire.decodeZone("\tUpstairs\tr1"));
        assertEquals(2,
                HomeWire.decodeZone("z1\tUpstairs\tr1,r2").getRoomIds().size());
        assertNull(HomeWire.decodeAccessory("\tLamp", null));
        assertNull(HomeWire.decodeService("\tSwitch", null));
        assertNull(HomeWire.decodeScene("\tEvening", "h1", null));
    }

    @Test
    void anAccessoryWithNoRoomOrBridgeReportsNullRatherThanEmptyStrings() {
        Accessory a = HomeWire.decodeAccessory(
                "acc\tLamp\t\t0\tAcme\tX1\t1.2\t1\t", null);
        assertNotNull(a);
        assertNull(a.getRoomId());
        assertNull(a.getBridgeAccessoryId());
        assertFalse(a.isBridged());
        assertTrue(a.isReachable());
        assertEquals("Acme", a.getManufacturer());
    }

    @Test
    void ordinalLookupsAreTotal() {
        assertSame(AccessoryCategory.OTHER, HomeWire.categoryFor(-1));
        assertSame(AccessoryCategory.OTHER, HomeWire.categoryFor(9999));
        assertSame(AccessoryCategory.LIGHT,
                HomeWire.categoryFor(AccessoryCategory.LIGHT.ordinal()));
        assertSame(ServiceType.OTHER, HomeWire.serviceTypeFor(9999));
        assertSame(SceneType.USER_DEFINED, HomeWire.sceneTypeFor(-1));
    }
}
