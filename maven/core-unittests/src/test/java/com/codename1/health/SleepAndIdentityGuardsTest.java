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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards on the value objects an app builds by hand: sleep sessions and
 * the identifier a subscription is persisted under.
 */
class SleepAndIdentityGuardsTest {

    private static final long START = 1767225600000L;

    private static SleepStageInterval stage(SleepStage s, long from,
            long to) {
        return new SleepStageInterval(s, START + from, START + to);
    }

    /**
     * An unclassified span is not time asleep.
     *
     * <p>The rollup asked "is this not one of the awake stages?", which swept
     * in {@code UNKNOWN} -- defined by the enum as a span the source did not
     * classify. A session carrying one unclassified ten-minute interval
     * therefore reported ten minutes of sleep, turning information the
     * platform declined to give into a concrete health measurement.</p>
     *
     * <p>It counts towards neither side now. {@code hasStageDetail()} is what
     * tells a caller the breakdown is unavailable.</p>
     */
    @Test
    void anUnclassifiedSpanIsNotCountedAsSleep() {
        List<SleepStageInterval> supplied =
                new ArrayList<SleepStageInterval>();
        supplied.add(stage(SleepStage.UNKNOWN, 0, 600000));

        SleepSample sleep = SleepSample.create(START, START + 600000,
                supplied);

        assertEquals(0, sleep.getAsleepDurationMillis(),
                "an unclassified span must not become a sleep measurement");
    }

    /** And a real asleep stage beside it still counts. */
    @Test
    void anUnclassifiedSpanDoesNotHideTheStagesAroundIt() {
        List<SleepStageInterval> supplied =
                new ArrayList<SleepStageInterval>();
        supplied.add(stage(SleepStage.UNKNOWN, 0, 1000));
        supplied.add(stage(SleepStage.DEEP, 1000, 3000));
        supplied.add(stage(SleepStage.AWAKE, 3000, 4000));

        SleepSample sleep = SleepSample.create(START, START + 4000,
                supplied);

        assertEquals(2000, sleep.getAsleepDurationMillis(),
                "only the classified asleep span counts");
    }

    /**
     * A null in the supplied list was skipped by validation and then
     * copied in anyway, so it sat in the session waiting for the first
     * accessor that walked the stages to dereference it.
     */
    @Test
    void nullStagesAreDroppedRatherThanStored() {
        List<SleepStageInterval> supplied =
                new ArrayList<SleepStageInterval>();
        supplied.add(stage(SleepStage.LIGHT, 0, 1000));
        supplied.add(null);
        supplied.add(stage(SleepStage.DEEP, 1000, 2000));

        SleepSample sleep = SleepSample.create(START, START + 2000,
                supplied);

        assertEquals(2, sleep.getStages().size());
        // These are the calls that used to throw.
        assertTrue(sleep.hasStageDetail());
        assertEquals(1000, sleep.getDurationMillis(SleepStage.LIGHT));
        assertEquals(2000, sleep.getAsleepDurationMillis());
    }

    /**
     * Platform sleep categories overlap, so summing them reported more
     * time asleep than the session lasted. LIGHT [0,10] plus DEEP [5,15]
     * covers 15, not 20.
     */
    @Test
    void overlappingStagesCountTheOverlapOnce() {
        List<SleepStageInterval> supplied =
                new ArrayList<SleepStageInterval>();
        supplied.add(stage(SleepStage.LIGHT, 0, 10000));
        supplied.add(stage(SleepStage.DEEP, 5000, 15000));

        SleepSample sleep = SleepSample.create(START, START + 15000,
                supplied);

        assertEquals(15000, sleep.getAsleepDurationMillis());
        assertTrue(sleep.getAsleepDurationMillis()
                <= sleep.getDurationMillis());
    }

    /** Two spans of one stage that overlap are the same minutes twice. */
    @Test
    void overlappingSpansOfOneStageCountOnce() {
        List<SleepStageInterval> supplied =
                new ArrayList<SleepStageInterval>();
        supplied.add(stage(SleepStage.REM, 0, 10000));
        supplied.add(stage(SleepStage.REM, 4000, 6000));

        SleepSample sleep = SleepSample.create(START, START + 10000,
                supplied);

        assertEquals(10000, sleep.getDurationMillis(SleepStage.REM));
    }

    /** Disjoint spans still add up, in any supplied order. */
    @Test
    void disjointStagesStillSum() {
        List<SleepStageInterval> supplied =
                new ArrayList<SleepStageInterval>();
        supplied.add(stage(SleepStage.DEEP, 12000, 15000));
        supplied.add(stage(SleepStage.LIGHT, 0, 5000));

        SleepSample sleep = SleepSample.create(START, START + 15000,
                supplied);

        assertEquals(8000, sleep.getAsleepDurationMillis());
    }

    /** Awake time is not time asleep, overlapping or not. */
    @Test
    void awakeSpansAreNotCounted() {
        List<SleepStageInterval> supplied =
                new ArrayList<SleepStageInterval>();
        supplied.add(stage(SleepStage.LIGHT, 0, 10000));
        supplied.add(stage(SleepStage.AWAKE, 3000, 6000));

        SleepSample sleep = SleepSample.create(START, START + 10000,
                supplied);

        assertEquals(10000, sleep.getAsleepDurationMillis());
        assertEquals(3000, sleep.getDurationMillis(SleepStage.AWAKE));
    }

    /**
     * The subscription registry is newline-delimited by record and
     * tab-delimited by field and is written unescaped, so an id carrying
     * either came back as a different id -- or as none -- after a
     * restart, and the subscription stopped delivering with nothing said.
     */
    @Test
    void subscriptionIdsRejectTheRegistryDelimiters() {
        assertThrows(IllegalArgumentException.class,
                () -> new SubscriptionRequest("steps\nv1"));
        assertThrows(IllegalArgumentException.class,
                () -> new SubscriptionRequest("steps\tv1"));
        assertThrows(IllegalArgumentException.class,
                () -> new SubscriptionRequest("steps\rv1"));
    }

    /** Ordinary ids, including punctuation and non-ASCII, still work. */
    @Test
    void ordinarySubscriptionIdsAreAccepted() {
        assertEquals("steps-v1",
                new SubscriptionRequest("  steps-v1  ").getId());
        assertEquals("pas quotidiens",
                new SubscriptionRequest("pas quotidiens").getId());
    }
}
