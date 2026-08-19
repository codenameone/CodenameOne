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

import com.codename1.home.commissioning.CommissioningRequest;
import com.codename1.home.commissioning.CommissioningStyle;
import com.codename1.home.commissioning.Commissioner;
import com.codename1.util.AsyncResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How the API behaves on a port with no smart-home support, which is the state
 * this test class runs in: no Display is initialized, so no bridge resolves.
 *
 * <p>This is the contract that lets application code drop the platform
 * branch. Every graph accessor answers empty, every operation fails fast with
 * a typed reason, and nothing returns null where an object was promised. If
 * any of these started throwing or returning null instead, an app written on a
 * phone would crash on the desktop.</p>
 */
class HomeDegradationTest {

    /**
     * The facade is a process-wide singleton, so a test that ran earlier and
     * installed a bridge would otherwise leak into this one and the failure
     * would look like a bug in whichever test happened to run second.
     */
    @BeforeEach
    void withNoBackend() {
        SmartHome.resetForTest(null);
    }

    @Test
    void getInstanceNeverReturnsNull() {
        assertNotNull(SmartHome.getInstance());
        assertSame(SmartHome.getInstance(), SmartHome.getInstance(),
                "the instance owns subscriptions and listeners, so replacing"
                        + " it would silently drop them");
    }

    @Test
    void anUnsupportedPortSaysSoRatherThanPretending() {
        SmartHome home = SmartHome.getInstance();
        assertFalse(home.isSupported());
        assertSame(HomeAvailability.NOT_SUPPORTED, home.getAvailability());
        assertSame(HomeBackend.NONE, home.getBackend());
        assertSame(HomeAuthorizationStatus.UNKNOWN,
                home.getAuthorizationStatus());
        assertFalse(home.areIdsPersistent());
    }

    @Test
    void graphAccessorsAnswerEmptyRatherThanNull() {
        SmartHome home = SmartHome.getInstance();
        assertNotNull(home.getStructures());
        assertTrue(home.getStructures().isEmpty());
        assertNull(home.getPrimaryStructure());
        assertNull(home.findAccessory("anything"));
        assertNotNull(home.getConfigurationProblems());
        assertTrue(home.getConfigurationProblems().isEmpty());
    }

    @Test
    void theOpenersAnswerFalseRatherThanThrowing() {
        SmartHome home = SmartHome.getInstance();
        assertFalse(home.openHomeSettings());
        assertFalse(home.openEcosystemApp());
        assertFalse(home.openProviderSetup());
    }

    /**
     * Not "not implemented yet" -- the three ecosystems model triggers in
     * three incompatible ways and Matter has none, so there is no honest
     * common shape. The method exists so an app can say why the feature is
     * absent.
     */
    @Test
    void automationIsNotClaimedAnywhere() {
        assertFalse(SmartHome.getInstance().isAutomationSupported());
    }

    @Test
    void operationsFailFastWithATypedReason() {
        SmartHome home = SmartHome.getInstance();
        assertNotSupported(home.refresh());
        assertNotSupported(home.requestAuthorization());
        assertNotSupported(
                home.read(new TraitReadRequest().add("a", "1", Trait.ON_OFF)));
        List<TraitWrite> writes = new ArrayList<TraitWrite>();
        writes.add(new TraitWrite("a", "1", Trait.ON_OFF,
                TraitValue.of(true)));
        assertNotSupported(home.write(writes));
    }

    /**
     * An empty batch is a successful nothing, not a failure. A caller that
     * built a request from a filtered list and filtered everything out has
     * not done anything wrong.
     */
    @Test
    void anEmptyBatchSucceedsWithNothing() {
        SmartHome home = SmartHome.getInstance();
        AsyncResource<List<TraitReading>> read =
                home.read(new TraitReadRequest());
        assertTrue(read.isDone());
        assertTrue(read.isReady(),
                "reading nothing is not an error even on a port with no"
                        + " smart-home support");

        AsyncResource<List<TraitWriteResult>> write =
                home.write(new ArrayList<TraitWrite>());
        assertTrue(write.isDone());
        assertTrue(write.isReady());
    }

    /**
     * The handle is live and inert, so a screen's teardown code runs
     * unchanged rather than needing a null check that only matters on one
     * platform.
     */
    @Test
    void subscribingWithNoBackendStillReturnsAStoppableHandle() {
        SubscriptionRequest request = new SubscriptionRequest()
                .add("a", "1", Trait.ON_OFF);
        TraitSubscription sub = SmartHome.getInstance()
                .subscribe(request, batch -> { });
        assertNotNull(sub);
        assertTrue(sub.isActive());
        assertFalse(sub.isPushDelivery());
        sub.stop();
        assertFalse(sub.isActive());
        sub.stop();
        assertFalse(sub.isActive(), "stop has to be idempotent");
    }

    /**
     * A subscription that watches nothing would never fire, so it is refused
     * at the call site rather than becoming a listener that looks attached and
     * is not.
     */
    @Test
    void anEmptySubscriptionIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> SmartHome.getInstance()
                        .subscribe(new SubscriptionRequest(), batch -> { }));
    }

    @Test
    void drainingWithNoBackendAnswersZero() {
        AsyncResource<Integer> drained =
                SmartHome.getInstance().drainChanges();
        assertTrue(drained.isDone());
        assertEquals(Integer.valueOf(0), drained.get());
    }

    @Test
    void theCommissionerIsPresentAndInert() {
        Commissioner c = SmartHome.getInstance().getCommissioner();
        assertNotNull(c);
        assertSame(CommissioningStyle.NONE, c.getStyle());
        assertFalse(c.isSupported());
        assertFalse(c.openEcosystemApp());
        AsyncResource<?> result = c.commission(new CommissioningRequest());
        assertTrue(result.isDone());
        HomeAwait.assertFailedWith(HomeError.COMMISSIONING_UNAVAILABLE,
                result);
    }

    @Test
    void structureListenersCanBeAddedAndRemovedWithNoBackend() {
        HomeStructureListener listener = event -> { };
        SmartHome home = SmartHome.getInstance();
        home.addStructureListener(listener);
        home.addStructureListener(listener);
        home.addStructureListener(null);
        home.removeStructureListener(listener);
        home.removeStructureListener(null);
    }

    /**
     * A trait that reports what an accessory is doing cannot be a command, and
     * saying so at construction beats letting the write travel to a port and
     * come back refused.
     */
    @Test
    void writingAReadOnlyTraitIsRefusedAtTheCallSite() {
        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class,
                        () -> new TraitWrite("a", "1", Trait.LOCK_STATE,
                                TraitValue.ofEnum(LockState.SECURED)));
        assertTrue(e.getMessage().indexOf("read-only") >= 0, e.getMessage());
    }

    @Test
    void writingTheWrongKindIsRefusedAtTheCallSite() {
        assertThrows(IllegalArgumentException.class,
                () -> new TraitWrite("a", "1", Trait.BRIGHTNESS,
                        TraitValue.of(true)));
    }

    private static void assertNotSupported(AsyncResource<?> resource) {
        assertTrue(resource.isDone(),
                "an unsupported operation must fail immediately rather than"
                        + " leaving the caller waiting for an answer that"
                        + " will never come");
        HomeAwait.assertFailedWith(HomeError.NOT_SUPPORTED, resource);
    }
}
