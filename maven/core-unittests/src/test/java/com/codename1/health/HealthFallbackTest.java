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

import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The no-op contract of the fallback classes returned on ports without a
 * health store. {@code TestCodenameOneImplementation.getHealth()} returns
 * {@code null} by default, so {@link Health#getInstance()} must substitute a
 * stable non-null instance whose capability queries are all {@code false} and
 * whose operations fail fast with {@link HealthError#NOT_SUPPORTED} rather
 * than throwing or hanging.
 */
class HealthFallbackTest extends UITestBase {

    @Test
    void getInstanceIsNeverNullAndStableWhenPortHasNoHealth() {
        assertNull(display.getHealth(),
                "test impl should report no port health by default");
        Health a = Health.getInstance();
        Health b = Health.getInstance();
        assertNotNull(a);
        assertSame(a, b);
    }

    @Test
    void fallbackCapabilityQueriesAreAllFalse() {
        Health h = Health.getInstance();
        assertFalse(h.isSupported());
        assertEquals(HealthAvailability.NOT_SUPPORTED, h.getAvailability());
        assertTrue(h.getConfigurationProblems().isEmpty());
    }

    @Test
    void subFacadesAreNeverNullAndStable() {
        Health h = Health.getInstance();
        assertNotNull(h.getStore());
        assertSame(h.getStore(), h.getStore());
        assertNotNull(h.getWorkouts());
        assertSame(h.getWorkouts(), h.getWorkouts());
        assertNotNull(h.getSensors());
        assertSame(h.getSensors(), h.getSensors());
    }

    @Test
    void fallbackStoreReportsNoSupportAndNoTypes() {
        HealthStore store = Health.getInstance().getStore();
        assertFalse(store.isSupported());
        assertFalse(store.isTypeSupported(HealthDataType.STEPS));
        assertFalse(store.isWritable(HealthDataType.STEPS));
        assertFalse(store.isDeletable(HealthDataType.STEPS));
        assertFalse(store.isBackgroundDeliverySupported());
        assertFalse(store.isPushDelivery());
        assertTrue(store.getSupportedTypes().isEmpty());
        assertTrue(store.getSupportedMetrics(HealthDataType.STEPS).isEmpty());
        assertTrue(store.getSubscriptions().isEmpty());
    }

    @Test
    void fallbackAuthorizationStatusIsNotSupportedRatherThanAGuess() {
        HealthStore store = Health.getInstance().getStore();
        assertEquals(HealthAuthorizationStatus.NOT_SUPPORTED,
                store.getReadAuthorizationStatus(HealthDataType.STEPS));
        assertEquals(HealthAuthorizationStatus.NOT_SUPPORTED,
                store.getWriteAuthorizationStatus(HealthDataType.STEPS));
    }

    @Test
    void fallbackReadFailsWithNotSupported() {
        HealthStore store = Health.getInstance().getStore();
        SampleQuery q = new SampleQuery()
                .addType(HealthDataType.STEPS)
                .setTimeRange(HealthTimeRange.lastHours(1));
        assertFailedWith(HealthError.NOT_SUPPORTED, store.readSamples(q));
        assertFailedWith(HealthError.NOT_SUPPORTED, store.readSamplePage(q));
    }

    @Test
    void fallbackAggregateFailsWithNotSupported() {
        HealthStore store = Health.getInstance().getStore();
        AggregateQuery q = new AggregateQuery()
                .addType(HealthDataType.STEPS)
                .addMetric(AggregateMetric.TOTAL)
                .setTimeRange(HealthTimeRange.calendarDays(7,
                        TimeZone.getTimeZone("UTC")))
                .setBucket(HealthInterval.calendarDays(1,
                        TimeZone.getTimeZone("UTC")));
        assertFailedWith(HealthError.NOT_SUPPORTED, store.aggregate(q));
    }

    @Test
    void fallbackWriteAndDeleteFailWithNotSupported() {
        HealthStore store = Health.getInstance().getStore();
        QuantitySample s = QuantitySample.create(HealthDataType.BODY_MASS,
                new HealthQuantity(70, HealthUnit.KILOGRAM), 1000L);
        assertFailedWith(HealthError.NOT_SUPPORTED, store.write(s));
        assertFailedWith(HealthError.NOT_SUPPORTED,
                store.delete(HealthDeleteRequest.byId(HealthDataType.STEPS, "x")));
    }

    @Test
    void fallbackAuthorizationRequestFailsWithNotSupported() {
        HealthStore store = Health.getInstance().getStore();
        assertFailedWith(HealthError.NOT_SUPPORTED,
                store.requestAuthorization(
                        HealthAccess.read(HealthDataType.STEPS)));
    }

    /**
     * Unlike the failing operations, the request-status probe resolves rather
     * than erroring: it answers a question about the UI ("would a sheet show
     * anything?") that has a sensible answer on a port with no health store.
     */
    @Test
    void fallbackRequestStatusResolvesUnknownRatherThanFailing() {
        HealthStore store = Health.getInstance().getStore();
        AsyncResource<HealthRequestStatus> r =
                store.getAuthorizationRequestStatus(
                        HealthAccess.read(HealthDataType.STEPS));
        HealthAwait.settled(r);
        assertEquals(HealthRequestStatus.UNKNOWN, r.get());
    }

    /**
     * {@code drainChanges} is called by the framework on every foreground
     * transition, so on a port without health it must be a cheap no-op rather
     * than an error the app would have to filter out on every resume.
     */
    @Test
    void fallbackDrainChangesResolvesZero() {
        AsyncResource<Integer> r =
                Health.getInstance().getStore().drainChanges();
        HealthAwait.settled(r);
        assertEquals(Integer.valueOf(0), r.get());
    }

    @Test
    void fallbackWorkoutManagerReportsNoLiveSupportButStillRecords() {
        assertFalse(Health.getInstance().getWorkouts()
                .isLiveSessionSupported());
        assertFalse(Health.getInstance().getWorkouts()
                .isSensorCollectionSupported());
        assertNull(Health.getInstance().getWorkouts().getActiveSession());
    }

    @Test
    void fallbackOpenersResolveFalseRatherThanFailing() {
        AsyncResource<Boolean> settings =
                Health.getInstance().openHealthSettings();
        assertTrue(settings.isDone());
        assertEquals(Boolean.FALSE, settings.get());

        AsyncResource<Boolean> setup =
                Health.getInstance().openProviderSetup();
        assertTrue(setup.isDone());
        assertEquals(Boolean.FALSE, setup.get());
    }

    private static void assertFailedWith(HealthError expected,
            AsyncResource<?> result) {
        // Settled rather than "already done": results are delivered on the
        // EDT on every backend now, so an off-EDT caller gets the failure
        // queued rather than inline. The point of the assertion is that a
        // fallback fails instead of hanging, which this still checks.
        HealthAwait.settled(result);
        Throwable err = errorOf(result);
        assertNotNull(err, "expected " + expected + " but the call succeeded");
        assertTrue(err instanceof HealthException,
                "expected a HealthException but got " + err);
        assertEquals(expected, ((HealthException) err).getError());
    }

    /**
     * An {@code except} callback registered on an already-failed resource
     * fires synchronously, so the error can be read out without waiting --
     * the same trick {@code BtTestUtil} uses.
     */
    private static Throwable errorOf(AsyncResource<?> r) {
        final Throwable[] err = new Throwable[1];
        r.except(new com.codename1.util.SuccessCallback<Throwable>() {
            public void onSucess(Throwable t) {
                err[0] = t;
            }
        });
        return err[0];
    }
}
