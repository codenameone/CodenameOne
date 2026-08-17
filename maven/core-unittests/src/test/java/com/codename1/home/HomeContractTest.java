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

import com.codename1.impl.home.LocalHomeBridge;
import com.codename1.impl.home.SubscriptionState;
import com.codename1.impl.home.SyntheticHome;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The parts of the contract that are invisible from the happy path: what
 * reaches the bridge, and what stops reaching a listener once it has been
 * stopped.
 *
 * <p>Both of these shipped wrong once. They are the kind of defect that a
 * feature test cannot see, because the operation still succeeds -- it just
 * succeeds against the wrong accessory, or against a form that is gone.</p>
 */
class HomeContractTest {

    /**
     * A batch can hold two locks with different PINs.
     *
     * <p>The credential used to be a single slot for the whole batch, filled
     * by whichever write in the batch had one last. Two locks in one call
     * therefore sent the second lock's PIN to the first, which fails, and
     * discarded the credential the caller actually supplied for it. Nothing
     * in the result distinguishes that from a wrong PIN, so it would have
     * read as the user's fault.</p>
     */
    @Test
    void eachWriteInABatchKeepsItsOwnCredential() {
        CapturingBridge bridge = new CapturingBridge();
        SyntheticHome.populate(bridge);
        SmartHome.resetForTest(bridge);
        SmartHome home = SmartHome.getInstance();
        HomeAwait.settled(home.refresh());

        Accessory lock = home.findAccessory("lock-front");
        Accessory lamp = home.findAccessory("lamp-living");
        List<TraitWrite> writes = new ArrayList<TraitWrite>();
        writes.add(new TraitWrite(lock, lock.getPrimaryService(),
                Trait.TARGET_LOCK_STATE, TraitValue.ofEnum(LockState.SECURED))
                .setAuthorizationData("1234"));
        writes.add(new TraitWrite(lamp, lamp.getPrimaryService(),
                Trait.ON_OFF, TraitValue.of(true)));
        writes.add(new TraitWrite(lock, lock.getPrimaryService(),
                Trait.TARGET_LOCK_STATE, TraitValue.ofEnum(LockState.UNSECURED))
                .setAuthorizationData("9999"));
        HomeAwait.settled(home.write(writes));

        assertArrayEquals(new String[] {"1234", "", "9999"},
                bridge.authorization,
                "each write's credential must arrive in its own slot");
    }

    /**
     * The initial delivery is produced by a read that is in flight while the
     * caller is free to stop the subscription, so it has to be checked at the
     * point of delivery like every other batch.
     */
    @Test
    void aStoppedSubscriptionDeliversNothingEvenUpFront() {
        final AtomicReference<TraitChangeBatch> seen =
                new AtomicReference<TraitChangeBatch>();
        SubscriptionState state = new SubscriptionState("sub-1",
                new HomeChangeListener() {
                    @Override
                    public void traitsChanged(TraitChangeBatch batch) {
                        seen.set(batch);
                    }
                }, 0);
        List<TraitReading> readings = new ArrayList<TraitReading>();
        readings.add(TraitReading.of("lamp-living", "1", Trait.ON_OFF,
                TraitValue.of(true), 1L));

        state.dispose();
        state.offer(readings, true);
        assertNull(seen.get(),
                "an initial batch must not reach a stopped subscription");

        state.offer(readings, false);
        assertNull(seen.get(),
                "nor must an ordinary one");
    }

    /**
     * The same state before it is stopped, so the test above is proving the
     * dispose check rather than a listener that never fires.
     */
    @Test
    void aLiveSubscriptionStillDeliversUpFront() {
        final AtomicReference<TraitChangeBatch> seen =
                new AtomicReference<TraitChangeBatch>();
        SubscriptionState state = new SubscriptionState("sub-2",
                new HomeChangeListener() {
                    @Override
                    public void traitsChanged(TraitChangeBatch batch) {
                        seen.set(batch);
                    }
                }, 0);
        List<TraitReading> readings = new ArrayList<TraitReading>();
        readings.add(TraitReading.of("lamp-living", "1", Trait.ON_OFF,
                TraitValue.of(true), 1L));

        state.offer(readings, true);
        assertEquals(1, seen.get().getReadings().size());
        state.dispose();
    }

    /**
     * The local bridge, with the one array this test cares about kept.
     *
     * <p>A subclass rather than a hand-written fake: the point is what the
     * facade hands a bridge, and everything else about the call has to stay
     * real for the write to reach here at all.</p>
     */
    private static final class CapturingBridge extends LocalHomeBridge {

        private String[] authorization;

        @Override
        public void writeTraits(int requestId, String[] accessoryIds,
                String[] serviceIds, String[] traitIds, int[] kinds,
                double[] numericValues, String[] stringValues,
                int[] unitWireIds, String[] authorizationData) {
            authorization = authorizationData;
            super.writeTraits(requestId, accessoryIds, serviceIds, traitIds,
                    kinds, numericValues, stringValues, unitWireIds,
                    authorizationData);
        }
    }
}
