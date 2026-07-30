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
package com.codename1.health;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The API speaks java.time where an abstraction exists.
 *
 * <p>Raw {@code long} millis and {@code java.util.TimeZone} were the whole
 * vocabulary, next to a {@code com.codename1.calendar} that already used
 * {@code Instant} and {@code ZoneId}. The millis forms remain on the paths
 * where they earn their keep -- inside a series, across the port SPI, and in
 * the tab-separated wire format, where a record can hold tens of thousands of
 * measurements and an object per point is an allocation per point -- but the
 * scalar public surface hands out the types the rest of the framework uses.</p>
 */
class HealthAbstractionsTest {

    @Test
    void aRangeIsBuiltFromAndReportsInstants() {
        Instant from = Instant.ofEpochMilli(1_000L);
        Instant to = Instant.ofEpochMilli(61_000L);

        HealthTimeRange range = HealthTimeRange.between(from, to);

        assertEquals(from, range.getStart());
        assertEquals(to, range.getEnd());
        assertEquals(Duration.ofMinutes(1), range.getDuration());
    }

    /**
     * An open-ended range has no end to name, so it says so rather than
     * handing back {@code Long.MAX_VALUE} dressed as an instant.
     */
    @Test
    void anOpenEndedRangeHasNoEndInstantUntilItIsResolved() {
        HealthTimeRange open = HealthTimeRange.since(Instant.ofEpochMilli(5));

        assertTrue(open.isOpenEnded());
        assertNull(open.getEnd(), "there is no end instant yet");
        assertNull(open.getDuration(), "and so no duration either");

        HealthTimeRange closed = open.resolve(1_000L);
        assertEquals(Instant.ofEpochMilli(1_000L), closed.getEnd());
    }

    @Test
    void aRollingWindowIsBuiltFromADuration() {
        HealthTimeRange window = HealthTimeRange.last(Duration.ofHours(2));
        assertEquals(2 * 3600_000L,
                window.getEndMillis() - window.getStartMillis());
    }

    @Test
    void aSampleReportsItsSpanAsInstantsAndADuration() {
        QuantitySample s = QuantitySample.create(HealthDataType.STEPS,
                new HealthQuantity(10, HealthUnit.COUNT), 1_000L, 61_000L);

        assertEquals(Instant.ofEpochMilli(1_000L), s.getStart());
        assertEquals(Instant.ofEpochMilli(61_000L), s.getEnd());
        assertEquals(Duration.ofMinutes(1), s.getDuration());
    }

    @Test
    void anInstantaneousSampleHasAZeroDuration() {
        QuantitySample s = QuantitySample.create(HealthDataType.BODY_MASS,
                new HealthQuantity(70, HealthUnit.KILOGRAM), 5_000L);
        assertEquals(s.getStart(), s.getEnd());
        assertEquals(Duration.ZERO, s.getDuration());
    }

    /**
     * The zone is a {@link ZoneId}, and an immutable one, which is what
     * removed the defensive-copy problem the mutable type had rather than
     * merely documenting it.
     */
    @Test
    void calendarIntervalsTakeAZoneId() {
        HealthInterval day = HealthInterval.calendarDays(1, ZoneId.of("UTC"));
        assertEquals(ZoneId.of("UTC"), day.getZone());
        assertNull(day.getDuration(),
                "a calendar day has no fixed length, so it reports none");

        HealthInterval fixed = HealthInterval.of(Duration.ofMinutes(15));
        assertEquals(Duration.ofMinutes(15), fixed.getDuration());
        assertNull(fixed.getZone(), "a fixed interval needs no zone");
    }

    /**
     * A fixed-offset zone buckets at that offset, not at GMT.
     *
     * <p>{@code ZoneId.of("+02:00")} has the id {@code +02:00}, and
     * {@code TimeZone.getTimeZone("+02:00")} does not recognise that form --
     * it falls back to GMT silently. Every calendar bucket then aligned two
     * hours early and samples landed in the wrong local day, which is exactly
     * the drift an explicit zone is required to prevent.</p>
     */
    @Test
    void aFixedOffsetZoneAlignsBucketsAtThatOffset() {
        HealthInterval plusTwo =
                HealthInterval.calendarDays(1, ZoneId.of("+02:00"));
        HealthInterval utc = HealthInterval.calendarDays(1, ZoneId.of("UTC"));

        long noonUtc = 1767268800000L; // 2026-01-01T12:00:00Z
        long startPlusTwo = plusTwo.bucketStart(noonUtc, noonUtc);
        long startUtc = utc.bucketStart(noonUtc, noonUtc);

        assertNotEquals(startUtc, startPlusTwo,
                "a +02:00 day must not start where the UTC day starts");
        assertEquals(2 * 3600_000L, startUtc - startPlusTwo,
                "it starts two hours earlier in absolute time");
    }

    /** And the offset spelling survives the round trip through the zone id. */
    @Test
    void offsetZoneIdsAreTranslatedForTheCalendar() {
        assertEquals("GMT+02:00", HealthTimeRange.zoneIdToTz("+02:00"));
        assertEquals("GMT-05:30", HealthTimeRange.zoneIdToTz("-05:30"));
        assertEquals("GMT", HealthTimeRange.zoneIdToTz("Z"));
        assertEquals("Europe/Berlin",
                HealthTimeRange.zoneIdToTz("Europe/Berlin"));
    }

    /**
     * A duration too large for the millis field it feeds is refused.
     *
     * <p>Past {@code Integer.MAX_VALUE} milliseconds -- about 24.9 days --
     * the narrowing wrapped negative, and negative does not mean "very long"
     * to any of these: the scan timeout reads it as disabled and leaves an
     * expensive BLE scan running, and the batch and staleness windows read it
     * as zero.</p>
     */
    @Test
    void anOutOfRangeDurationIsRefusedRatherThanWrapped() {
        try {
            new com.codename1.health.sensors.SensorScanSettings()
                    .setTimeout(Duration.ofDays(30));
            fail("a timeout beyond the int range must be refused");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("milliseconds"));
        }
        // And a workable one still goes through.
        assertEquals(30_000,
                new com.codename1.health.sensors.SensorScanSettings()
                        .setTimeout(Duration.ofSeconds(30))
                        .getTimeoutMillis());
    }

    /**
     * A stage interval retained by the caller cannot rewrite a stored record.
     *
     * <p>The platform code is mutable, which exposed the shallow element
     * copies: the list was copied and the intervals were not, so setting a
     * code on an interval you had written changed the session that was
     * stored -- with no write and no persistence update.</p>
     */
    @Test
    void aRetainedStageIntervalCannotMutateTheStoredSession() {
        SleepStageInterval mine =
                new SleepStageInterval(SleepStage.DEEP, 0L, 1000L);
        java.util.List<SleepStageInterval> supplied =
                new java.util.ArrayList<SleepStageInterval>();
        supplied.add(mine);

        SleepSample sleep = SleepSample.create(0L, 1000L, supplied);
        mine.setPlatformCode(42);

        assertEquals(SleepStageInterval.NO_PLATFORM_CODE,
                sleep.getStages().get(0).getPlatformCode(),
                "the session must not see a change made after the write");
    }

    @Test
    void durationsAreAcceptedWhereMillisWere() {
        SampleQuery q = new SampleQuery()
                .setSleepSessionGap(Duration.ofMinutes(20));
        assertEquals(20 * 60_000L, q.getSleepSessionGapMillis());
        assertEquals(Duration.ofMinutes(20), q.getSleepSessionGap());

        WorkoutSample w = WorkoutSample.create(
                WorkoutActivityType.RUNNING, 0L, 60_000L);
        w.setActiveDuration(Duration.ofSeconds(45));
        assertEquals(45_000L, w.getActiveDurationMillis());
        assertEquals(Duration.ofSeconds(45), w.getActiveDuration());
    }

    /** A null duration is a programming error, not a silent zero. */
    @Test
    void aNullDurationIsRejected() {
        try {
            new SampleQuery().setSleepSessionGap(null);
            fail("a null duration must be refused");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }
}
