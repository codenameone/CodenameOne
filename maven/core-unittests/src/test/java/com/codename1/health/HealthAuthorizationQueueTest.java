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

import com.codename1.junit.UITestBase;
import com.codename1.util.AsyncResource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Only one authorization flow reaches a port at a time.
 *
 * <p>Overlapping flows are not a race a port can win. On Android the
 * permission screen is an activity result, and
 * {@code CodenameOneActivity.setIntentResultListener} returns without
 * installing the second listener once {@code waitingForResult} is set --
 * silently, while {@code AndroidNativeUtil.startActivityForResult} launches
 * the activity anyway. The first flow's listener then took whichever result
 * arrived first, and the second request sat unresolved until its
 * authorization timeout minutes later, having shown the user a sheet whose
 * answer went nowhere.</p>
 */
class HealthAuthorizationQueueTest extends UITestBase {

    private static HealthAccess read(HealthDataType type) {
        return HealthAccess.read(type);
    }

    /**
     * A second request waits for the first rather than launching beside it.
     */
    @Test
    void aSecondAuthorizationWaitsForTheFirstToFinish() {
        FakeHealthStore store = new FakeHealthStore();
        store.holdAuthorization = true;

        AsyncResource<Boolean> first =
                store.requestAuthorization(read(HealthDataType.STEPS));
        AsyncResource<Boolean> second =
                store.requestAuthorization(read(HealthDataType.HEART_RATE));

        assertEquals(1, store.authorizationsSeen.size(),
                "the port must be asked for one flow at a time");
        assertFalse(second.isDone(), "and the second is still waiting");

        store.heldAuthorizations.get(0).complete(Boolean.TRUE);

        assertEquals(2, store.authorizationsSeen.size(),
                "the queued flow runs once the first ends");
        assertSame(HealthDataType.HEART_RATE,
                store.authorizationsSeen.get(1).get(0).getType(),
                "and it asks for its own access, not the first request's");
        assertTrue(first.isDone());

        store.heldAuthorizations.get(1).complete(Boolean.TRUE);
        assertTrue(second.isDone(), "both callers are answered");
    }

    /**
     * A failed flow still hands the screen on.
     *
     * <p>Otherwise one denied sheet strands every request behind it for the
     * life of the process, which is worse than the bug being fixed.</p>
     */
    @Test
    void aRejectedFlowStillReleasesTheQueue() {
        FakeHealthStore store = new FakeHealthStore();
        store.holdAuthorization = true;

        store.requestAuthorization(read(HealthDataType.STEPS));
        AsyncResource<Boolean> second =
                store.requestAuthorization(read(HealthDataType.HEART_RATE));

        store.heldAuthorizations.get(0).error(new HealthException(
                HealthError.USER_CANCELED, "dismissed"));

        assertEquals(2, store.authorizationsSeen.size(),
                "a dismissal must not strand the queue");
        store.heldAuthorizations.get(1).complete(Boolean.TRUE);
        assertTrue(second.isDone());
    }

    /**
     * A caller that gave up while queued never gets a sheet, and does not
     * strand the ones behind it either.
     */
    @Test
    void aCancelledQueuedRequestIsSkipped() {
        FakeHealthStore store = new FakeHealthStore();
        store.holdAuthorization = true;

        store.requestAuthorization(read(HealthDataType.STEPS));
        AsyncResource<Boolean> abandoned =
                store.requestAuthorization(read(HealthDataType.HEART_RATE));
        AsyncResource<Boolean> third =
                store.requestAuthorization(read(HealthDataType.BODY_MASS));

        assertTrue(abandoned.cancel(true));
        store.heldAuthorizations.get(0).complete(Boolean.TRUE);

        assertEquals(2, store.authorizationsSeen.size(),
                "the cancelled request must not be shown");
        assertSame(HealthDataType.BODY_MASS,
                store.authorizationsSeen.get(1).get(0).getType(),
                "the queue skips to the one still waiting");
        store.heldAuthorizations.get(1).complete(Boolean.TRUE);
        assertTrue(third.isDone());
    }

    /** With nothing queued, the next request runs immediately. */
    @Test
    void aLaterAuthorizationRunsOnceTheQueueIsEmpty() {
        FakeHealthStore store = new FakeHealthStore();
        store.holdAuthorization = true;

        store.requestAuthorization(read(HealthDataType.STEPS));
        store.heldAuthorizations.get(0).complete(Boolean.TRUE);

        store.requestAuthorization(read(HealthDataType.HEART_RATE));
        assertEquals(2, store.authorizationsSeen.size(),
                "the flag must not be left set behind a finished flow");
    }
}
