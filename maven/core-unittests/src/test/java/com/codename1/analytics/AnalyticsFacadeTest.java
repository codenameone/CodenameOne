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
package com.codename1.analytics;

import com.codename1.io.Preferences;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for the {@link Analytics} facade: provider management, multi-provider
/// fan-out, the pseudonymous client id, and the personalization consent gate.
class AnalyticsFacadeTest extends UITestBase {

    @FormTest
    void addRemoveAndClearProviders() {
        Analytics.clearProviders();
        LoggingAnalyticsProvider a = new LoggingAnalyticsProvider();
        LoggingAnalyticsProvider b = new LoggingAnalyticsProvider();
        Analytics.addProvider(a);
        Analytics.addProvider(b);
        assertEquals(2, Analytics.getProviders().size());
        Analytics.removeProvider(a);
        assertEquals(1, Analytics.getProviders().size());
        Analytics.clearProviders();
        assertEquals(0, Analytics.getProviders().size());
        Analytics.setConsent(null);
    }

    @FormTest
    void screenFansOutToEveryProvider() {
        Analytics.clearProviders();
        Analytics.setConsentMode(ConsentMode.OPT_OUT);
        Analytics.setConsent(null);
        LoggingAnalyticsProvider a = new LoggingAnalyticsProvider();
        LoggingAnalyticsProvider b = new LoggingAnalyticsProvider();
        Analytics.addProvider(a);
        Analytics.addProvider(b);
        a.clearLog();
        b.clearLog();

        Analytics.screen("Home", null);
        assertEquals(1, a.getLog().size());
        assertEquals(1, b.getLog().size());

        Analytics.clearProviders();
        Analytics.setConsent(null);
    }

    @FormTest
    void clientIdIsStableAndResettable() {
        String id = Analytics.clientId();
        assertNotNull(id);
        assertEquals(32, id.length());
        assertEquals(id, Analytics.clientId(), "client id must be stable across calls");

        String reset = Analytics.resetClientId();
        assertNotNull(reset);
        assertNotEquals(id, reset, "reset must produce a new client id");
        assertEquals(reset, Analytics.clientId());
    }

    @FormTest
    void reservedDimensionsThatOutlivedAnErasureAreDroppedOnTheNextLaunch() {
        // Preferences.set discards its write-failure boolean, so an erasure
        // that could not reach the disk removed the reserved dimensions from
        // memory and left them in the file. The next launch loaded them back
        // and attached the referral identity the user asked to be rid of to
        // their NEW client id -- one launch later, with nothing in memory left
        // to notice.
        Analytics.clearProviders();
        Analytics.clearDimensions();
        String current = Analytics.clientId();

        // The file as a failed erasure leaves it: framework dimensions and an
        // application dimension, stamped with the identity that has gone.
        Analytics.simulateSurvivingDimensionsForTest(
                "cn1_campaign\tspring\ncn1_invite\tinstall_confirmed\nplan\tpro",
                "an-erased-client-id");

        Map<String, String> loaded = Analytics.getDimensions();
        assertNull(loaded.get("cn1_campaign"),
                "an erased referral came back and attached itself to the new client id");
        assertNull(loaded.get("cn1_invite"));
        // The APPLICATION's own dimension is not what an erasure asked about,
        // and losing it would be a second bug in the name of fixing the first.
        assertEquals("pro", loaded.get("plan"),
                "the application's own dimension was destroyed by someone else's erasure");
        assertEquals(current, Analytics.clientId(), "the fixture changed the identity");
    }

    @FormTest
    void dimensionsFromTheCurrentIdentityAreKept() {
        // The drop is keyed on the STAMP, not on the prefix, or an ordinary
        // launch would throw away the referral dimensions every time.
        Analytics.clearProviders();
        Analytics.clearDimensions();
        Analytics.simulateSurvivingDimensionsForTest(
                "cn1_campaign\tspring\nplan\tpro", Analytics.clientId());

        Map<String, String> loaded = Analytics.getDimensions();
        assertEquals("spring", loaded.get("cn1_campaign"),
                "a live referral was discarded on an ordinary launch");
        assertEquals("pro", loaded.get("plan"));
    }

    @FormTest
    void anUnstampedFileIsNotTrustedWithFrameworkDimensions() {
        // The case a read-back check cannot reach and the stamp only covers if
        // it is treated strictly: a file whose stamp was never written --
        // because it predates the stamp, or because the same storage failure
        // that broke the erasure also stopped the stamp landing. Treating an
        // absent stamp as current is exactly the state being defended against.
        Analytics.clearProviders();
        Analytics.clearDimensions();
        Analytics.simulateSurvivingDimensionsForTest(
                "cn1_campaign\tspring\nplan\tpro", null);

        Map<String, String> loaded = Analytics.getDimensions();
        assertNull(loaded.get("cn1_campaign"),
                "an unstamped referral was trusted and reloaded");
        assertEquals("pro", loaded.get("plan"),
                "the application's own dimension was destroyed with it");
    }

    @FormTest
    void setUserIdRequiresPersonalizationConsent() {
        Analytics.clearProviders();
        Analytics.setConsentMode(ConsentMode.OPT_IN);
        Analytics.setConsent(AnalyticsConsent.builder().analytics(true).personalization(false).build());
        LoggingAnalyticsProvider p = new LoggingAnalyticsProvider();
        Analytics.addProvider(p);
        p.clearLog();

        Analytics.setUserId("user-1");
        assertTrue(p.getLog().isEmpty(), "user id must be dropped without personalization consent");

        Analytics.setConsent(AnalyticsConsent.granted());
        p.clearLog();
        Analytics.setUserId("user-1");
        assertEquals(1, p.getLog().size());
        assertEquals("userId:user-1", p.getLog().get(0));

        Analytics.clearProviders();
        Analytics.setConsent(null);
    }

    @FormTest
    void dimensionsRoundTripAndPersist() {
        Analytics.clearDimensions();
        assertTrue(Analytics.getDimensions().isEmpty());

        Analytics.setDimension("plan", "pro");
        Analytics.setDimension("role", "admin");
        Map<String, String> dims = Analytics.getDimensions();
        assertEquals(2, dims.size());
        assertEquals("pro", dims.get("plan"));
        assertEquals("admin", dims.get("role"));

        // Defensive copy: mutating the returned map must not affect storage.
        dims.put("hacked", "x");
        assertFalse(Analytics.getDimensions().containsKey("hacked"));

        // Null value removes the key; null/empty key is ignored.
        Analytics.setDimension("plan", null);
        assertFalse(Analytics.getDimensions().containsKey("plan"));
        Analytics.setDimension(null, "y");
        Analytics.setDimension("", "y");
        assertEquals(1, Analytics.getDimensions().size());

        // Persistence: the raw preference must hold the surviving dimension.
        String stored = Preferences.get("cn1$analyticsDimensions", "");
        assertTrue(stored.contains("role"), "dimensions not persisted: " + stored);
        assertTrue(stored.contains("admin"), "dimension value not persisted: " + stored);

        Analytics.clearDimension("role");
        assertTrue(Analytics.getDimensions().isEmpty());
        Analytics.clearDimensions();
    }

    @FormTest
    void consentPersistsAcrossReload() {
        Analytics.clearProviders();
        Analytics.setConsentMode(ConsentMode.OPT_IN);
        Analytics.setConsent(AnalyticsConsent.granted());
        AnalyticsConsent loaded = Analytics.getConsent();
        assertNotNull(loaded);
        assertTrue(loaded.isAnalytics());
        Analytics.setConsent(null);
    }
}
