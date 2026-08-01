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
        assertTrue(HealthAwait.settled(first).isDone());

        store.heldAuthorizations.get(1).complete(Boolean.TRUE);
        assertTrue(HealthAwait.settled(second).isDone(),
                "both callers are answered");
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
        assertTrue(HealthAwait.settled(second).isDone());
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
        assertTrue(HealthAwait.settled(third).isDone());
    }

    /**
     * Cancelling the active request does not hand the screen on.
     *
     * <p>The caller's resource settles on three different events -- the
     * port answering, the caller cancelling, and the timeout firing -- and
     * on the last two the permission sheet is still up with Android's
     * {@code waitingForResult} still set. Releasing the queue there
     * launched a second activity beside the first, which is the overlap
     * this queue exists to prevent, brought about by a caller who had
     * already stopped listening.</p>
     *
     * <p>So the queue is keyed to the native flow, not to the caller: the
     * next sheet waits for the port to answer even though nobody is
     * waiting for the answer any more.</p>
     */
    @Test
    void cancellingTheActiveFlowDoesNotLaunchTheNextOne() {
        FakeHealthStore store = new FakeHealthStore();
        store.holdAuthorization = true;

        AsyncResource<Boolean> first =
                store.requestAuthorization(read(HealthDataType.STEPS));
        AsyncResource<Boolean> second =
                store.requestAuthorization(read(HealthDataType.HEART_RATE));

        assertTrue(first.cancel(true), "the caller gives up on the sheet");
        assertEquals(1, store.authorizationsSeen.size(),
                "the sheet is still on screen, so nothing else may launch");

        // The platform answers the abandoned flow. Only now is the screen
        // free, and the queued request is the one that gets it.
        store.heldAuthorizations.get(0).complete(Boolean.TRUE);
        assertEquals(2, store.authorizationsSeen.size(),
                "the queue moves once the native flow closes");
        assertSame(HealthDataType.HEART_RATE,
                store.authorizationsSeen.get(1).get(0).getType());

        store.heldAuthorizations.get(1).complete(Boolean.TRUE);
        assertTrue(HealthAwait.settled(second).isDone());
    }

    /**
     * A cancelled caller is not handed the port's late answer.
     *
     * <p>The flow still resolves -- that is what releases the screen -- but
     * its outcome must not reach a resource the caller already cancelled.
     */
    @Test
    void aCancelledCallerGetsNoLateResult() {
        FakeHealthStore store = new FakeHealthStore();
        store.holdAuthorization = true;

        AsyncResource<Boolean> first =
                store.requestAuthorization(read(HealthDataType.STEPS));
        final int[] delivered = new int[1];
        first.ready(new com.codename1.util.SuccessCallback<Boolean>() {
            public void onSucess(Boolean value) {
                delivered[0]++;
            }
        });

        assertTrue(first.cancel(true));
        store.heldAuthorizations.get(0).complete(Boolean.TRUE);

        assertEquals(0, delivered[0],
                "a cancelled request must not be handed a grant");
        assertTrue(first.isCancelled());
    }

    /**
     * A caller that times out is answered; the screen stays held.
     *
     * <p>This asserted the opposite until the review caught it. Arming the
     * timeout on the flow made the timeout itself release the queue, which is
     * the original defect one step removed: {@code waitingForResult} is
     * cleared by the activity result and by nothing else, so the next sheet
     * launched beside a permission activity that was still open and had its
     * listener silently dropped. The old test called that "a lost platform
     * callback must not strand the screen" and was pinning the bug.</p>
     *
     * <p>The timeout now bounds the caller only. What releases the screen is
     * the port settling the flow, which every port does on every path:
     * Android always receives an activity result once the activity finishes
     * and settles from the catch if it could not be started, and an
     * unreadable grant list still resolves because the sheet did complete.
     * The remaining way to strand the queue is the process dying, which
     * resets it anyway.</p>
     */
    @Test
    void aTimedOutCallerIsAnsweredWhileTheScreenStaysHeld() throws Exception {
        FakeHealthStore store = new FakeHealthStore();
        store.setAuthorizationTimeoutForTest(120);
        store.holdAuthorization = true;

        AsyncResource<Boolean> first =
                store.requestAuthorization(read(HealthDataType.STEPS));
        AsyncResource<Boolean> second =
                store.requestAuthorization(read(HealthDataType.HEART_RATE));

        long deadline = System.currentTimeMillis() + 5000L;
        while (!first.isDone() && System.currentTimeMillis() < deadline) {
            Thread.sleep(20L);
        }
        assertTrue(first.isDone(), "the caller must not wait past its budget");
        assertEquals(1, store.authorizationsSeen.size(),
                "the sheet is still on screen, so nothing else may launch");
        assertFalse(second.isDone());

        // The platform answers the flow the caller gave up on. Only now is
        // the screen free.
        store.heldAuthorizations.get(0).complete(Boolean.TRUE);
        assertEquals(2, store.authorizationsSeen.size(),
                "the queue moves when the native flow closes, not before");
        store.heldAuthorizations.get(1).complete(Boolean.TRUE);
        assertTrue(HealthAwait.settled(second).isDone());
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
