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
package com.codename1.impl.android;

import com.codename1.home.HomeAuthorizationStatus;
import com.codename1.home.HomeAvailability;
import com.codename1.home.SmartHome;
import com.codename1.home.commissioning.CommissioningStyle;
import com.codename1.home.spi.HomeBridge;

/// Android `HomeBridge`, backing `com.codename1.home` with whatever the build
/// injected: Google Play services Matter commissioning, and the Google Home
/// APIs when the app enabled them.
///
/// #### It is a forwarder, and that is the whole design
///
/// Nothing here talks to Google. The port compiles against a fixed, old
/// `android.jar` with no Play services, no AndroidX and no Kotlin, so it
/// cannot -- see [SmartHomeDelegate] for why that constraint exists and how
/// the build works around it. This class turns the SPI's request ids into
/// callbacks, decodes nothing, and reports "unsupported" whenever no delegate
/// was injected, which is the state every app that never referenced
/// `com.codename1.home` is in.
///
/// #### Android's default answer is not "available"
///
/// A registered delegate commonly reports
/// [HomeAvailability#COMMISSIONING_ONLY]: Play services can add a Matter
/// accessory to the user's Google Home with no setup at all, while reading or
/// controlling one needs the Google Home APIs, a Google Cloud project and a
/// Home Developer Console registration that only the app's developer can
/// create. That distinction is reported rather than papered over, because an
/// app written against the iOS meaning of `AVAILABLE` would otherwise show an
/// empty house with no explanation.
final class AndroidHomeBridge implements HomeBridge {

    private static final String[] NOTHING = new String[0];

    AndroidHomeBridge() {
        SmartHomeDelegate delegate = AndroidSmartHomeSupport.getDelegate();
        if (delegate != null) {
            delegate.setEventSink(new Events());
        }
    }

    private static SmartHomeDelegate delegate() {
        return AndroidSmartHomeSupport.getDelegate();
    }

    private static String[] orEmpty(String[] value) {
        return value == null ? NOTHING : value;
    }

    // ------------------------------------------------------------------
    // capability
    // ------------------------------------------------------------------

    @Override
    public boolean isSupported() {
        return delegate() != null;
    }

    @Override
    public int getAvailability() {
        SmartHomeDelegate d = delegate();
        return d == null ? HomeAvailability.NOT_SUPPORTED.ordinal()
                : d.availability();
    }

    @Override
    public String getBackendId() {
        SmartHomeDelegate d = delegate();
        if (d == null) {
            return "none";
        }
        // Which backend this is follows from what it can actually do, not
        // from a flag the bridge sets separately -- the two could disagree,
        // and the availability is the one an app already branches on.
        return d.availability()
                == HomeAvailability.COMMISSIONING_ONLY.ordinal()
                ? "matter_only" : "google_home";
    }

    @Override
    public String[] getConfigurationProblems() {
        SmartHomeDelegate d = delegate();
        return d == null ? NOTHING : orEmpty(d.configurationProblems());
    }

    @Override
    public boolean areIdsPersistent() {
        // Google Home device and structure ids are stable across launches.
        return true;
    }

    @Override
    public void start(int requestId) {
        SmartHomeDelegate d = delegate();
        if (d == null) {
            SmartHome.deliverStarted(requestId,
                    HomeAvailability.NOT_SUPPORTED.ordinal(), unsupported());
            return;
        }
        d.start(new Started(requestId));
    }

    @Override
    public void stop() {
        SmartHomeDelegate d = delegate();
        if (d != null) {
            d.stop();
        }
    }

    @Override
    public int getAuthorizationStatus() {
        SmartHomeDelegate d = delegate();
        return d == null ? HomeAuthorizationStatus.UNKNOWN.ordinal()
                : d.authorizationStatus();
    }

    @Override
    public void requestAuthorization(int requestId) {
        SmartHomeDelegate d = delegate();
        if (d == null) {
            SmartHome.deliverAuthorization(requestId,
                    HomeAuthorizationStatus.UNKNOWN.ordinal(), unsupported());
            return;
        }
        d.requestAuthorization(new Authorized(requestId));
    }

    @Override
    public boolean openHomeSettings() {
        SmartHomeDelegate d = delegate();
        return d != null && d.openHomeSettings();
    }

    @Override
    public boolean openEcosystemApp() {
        SmartHomeDelegate d = delegate();
        return d != null && d.openEcosystemApp();
    }

    @Override
    public boolean openProviderSetup() {
        SmartHomeDelegate d = delegate();
        return d != null && d.openProviderSetup();
    }

    // ------------------------------------------------------------------
    // graph
    // ------------------------------------------------------------------

    @Override
    public String[] getStructures() {
        SmartHomeDelegate d = delegate();
        return d == null ? NOTHING : orEmpty(d.structures());
    }

    @Override
    public String[] getRooms(String structureId) {
        SmartHomeDelegate d = delegate();
        return d == null ? NOTHING : orEmpty(d.rooms(structureId));
    }

    @Override
    public String[] getZones(String structureId) {
        // Zones are a HomeKit concept. Empty rather than synthesized: a
        // guess about which rooms a user thinks of as upstairs is not
        // information, and an app laid out around invented zones would look
        // broken on the platform that has real ones.
        return NOTHING;
    }

    @Override
    public String[] getAccessories(String structureId) {
        SmartHomeDelegate d = delegate();
        return d == null ? NOTHING : orEmpty(d.accessories(structureId));
    }

    @Override
    public String[] getServices(String accessoryId) {
        SmartHomeDelegate d = delegate();
        return d == null ? NOTHING : orEmpty(d.services(accessoryId));
    }

    @Override
    public String[] getTraits(String accessoryId, String serviceId) {
        SmartHomeDelegate d = delegate();
        return d == null ? NOTHING : orEmpty(d.traits(accessoryId, serviceId));
    }

    @Override
    public void refresh(int requestId) {
        SmartHomeDelegate d = delegate();
        if (d == null) {
            SmartHome.deliverRefreshed(requestId, unsupported());
            return;
        }
        d.refresh(new Refreshed(requestId));
    }

    // ------------------------------------------------------------------
    // reads and writes
    // ------------------------------------------------------------------

    @Override
    public int getMaxReadBatchSize() {
        return 0;
    }

    @Override
    public void readTraits(int requestId, String[] accessoryIds,
            String[] serviceIds, String[] traitIds, boolean allowCached) {
        SmartHomeDelegate d = delegate();
        if (d == null) {
            SmartHome.deliverReadings(requestId, NOTHING, unsupported());
            return;
        }
        d.readTraits(accessoryIds, serviceIds, traitIds, allowCached,
                new Readings(requestId));
    }

    @Override
    public int getMaxWriteBatchSize() {
        return 0;
    }

    @Override
    public void writeTraits(int requestId, String[] accessoryIds,
            String[] serviceIds, String[] traitIds, int[] kinds,
            double[] numericValues, String[] stringValues, int[] unitWireIds,
            String authorizationData) {
        SmartHomeDelegate d = delegate();
        if (d == null) {
            SmartHome.deliverWriteResults(requestId, NOTHING, unsupported());
            return;
        }
        d.writeTraits(accessoryIds, serviceIds, traitIds, kinds, numericValues,
                stringValues, unitWireIds, authorizationData,
                new WriteResults(requestId));
    }

    // ------------------------------------------------------------------
    // subscriptions
    // ------------------------------------------------------------------

    @Override
    public boolean isPushDelivery() {
        SmartHomeDelegate d = delegate();
        return d != null && d.isPushDelivery();
    }

    @Override
    public void subscribe(int requestId, String subscriptionId,
            String[] accessoryIds, String[] serviceIds, String[] traitIds) {
        SmartHomeDelegate d = delegate();
        if (d == null) {
            return;
        }
        d.subscribe(subscriptionId, accessoryIds, serviceIds, traitIds,
                new Subscribed());
    }

    @Override
    public void unsubscribe(String subscriptionId) {
        SmartHomeDelegate d = delegate();
        if (d != null) {
            d.unsubscribe(subscriptionId);
        }
    }

    @Override
    public void drainChanges(int requestId) {
        SmartHomeDelegate d = delegate();
        if (d == null) {
            SmartHome.deliverDrained(requestId, 0, null);
            return;
        }
        d.drainChanges(new Drained(requestId));
    }

    // ------------------------------------------------------------------
    // scenes
    // ------------------------------------------------------------------

    @Override
    public String[] getScenes(String structureId) {
        SmartHomeDelegate d = delegate();
        return d == null ? NOTHING : orEmpty(d.scenes(structureId));
    }

    @Override
    public String[] getSceneActions(String structureId, String sceneId) {
        SmartHomeDelegate d = delegate();
        return d == null ? NOTHING
                : orEmpty(d.sceneActions(structureId, sceneId));
    }

    @Override
    public void executeScene(int requestId, String structureId,
            String sceneId) {
        SmartHomeDelegate d = delegate();
        if (d == null) {
            SmartHome.deliverSceneResult(requestId, null, structureId,
                    unsupported());
            return;
        }
        d.executeScene(structureId, sceneId,
                new SceneOutcome(requestId, structureId));
    }

    @Override
    public void createScene(int requestId, String structureId, String name,
            String[] accessoryIds, String[] serviceIds, String[] traitIds,
            int[] kinds, double[] numericValues, String[] stringValues,
            int[] unitWireIds) {
        SmartHomeDelegate d = delegate();
        if (d == null) {
            SmartHome.deliverSceneResult(requestId, null, structureId,
                    unsupported());
            return;
        }
        d.createScene(structureId, name, accessoryIds, serviceIds, traitIds,
                kinds, numericValues, stringValues, unitWireIds,
                new SceneOutcome(requestId, structureId));
    }

    @Override
    public void deleteScene(int requestId, String structureId,
            String sceneId) {
        SmartHomeDelegate d = delegate();
        if (d == null) {
            SmartHome.deliverSceneResult(requestId, null, structureId,
                    unsupported());
            return;
        }
        d.deleteScene(structureId, sceneId,
                new SceneOutcome(requestId, structureId));
    }

    // ------------------------------------------------------------------
    // commissioning
    // ------------------------------------------------------------------

    @Override
    public int getCommissioningStyle() {
        SmartHomeDelegate d = delegate();
        return d == null ? CommissioningStyle.NONE.ordinal()
                : d.commissioningStyle();
    }

    @Override
    public void commission(int requestId, String setupPayload,
            String structureId, String roomId, String suggestedName,
            int timeoutMillis) {
        SmartHomeDelegate d = delegate();
        if (d == null) {
            SmartHome.deliverCommissioningResult(requestId, null, null, null, 0,
                    "COMMISSIONING_UNAVAILABLE\tthis build did not include "
                            + "Matter commissioning");
            return;
        }
        d.commission(setupPayload, structureId, roomId, suggestedName,
                timeoutMillis, new Commissioned(requestId));
    }

    @Override
    public void identify(int requestId, String accessoryId) {
        SmartHomeDelegate d = delegate();
        if (d == null) {
            SmartHome.deliverIdentifyResult(requestId, unsupported());
            return;
        }
        d.identify(accessoryId, new Identified(requestId));
    }

    // ------------------------------------------------------------------
    // internals
    // ------------------------------------------------------------------

    /// The wire form of a failure: the HomeError NAME and the platform's own
    /// text, tab separated. The name and never the ordinal, so a delegate
    /// built against a different version of the enum cannot map a failure onto
    /// the wrong one.
    private static String encode(String errorName, String message) {
        return (errorName == null ? "UNKNOWN" : errorName) + "\t"
                + (message == null ? "" : message);
    }

    private static String unsupported() {
        return "NOT_SUPPORTED\tthis build did not include smart-home support";
    }

    private static final class Started implements SmartHomeDelegate.Callback {

        private final int requestId;

        Started(int requestId) {
            this.requestId = requestId;
        }

        @Override
        public void onSuccess(String[] payload) {
            SmartHomeDelegate d = AndroidSmartHomeSupport.getDelegate();
            SmartHome.deliverStarted(requestId,
                    d == null ? HomeAvailability.NOT_SUPPORTED.ordinal()
                              : d.availability(),
                    null);
        }

        @Override
        public void onError(String errorName, String message) {
            SmartHome.deliverStarted(requestId,
                    HomeAvailability.NOT_SUPPORTED.ordinal(),
                    encode(errorName, message));
        }
    }

    /// Every callback here is a named static class rather than an anonymous
    /// one. Anonymous classes capture the enclosing bridge, which pins it for
    /// as long as the platform holds the callback and is what SpotBugs
    /// reports as SIC_INNER_SHOULD_BE_STATIC_ANON -- a zero-findings gate in
    /// this repo, so the choice is made for us. The names also read better in
    /// a stack trace than AndroidHomeBridge$7.
    private static final class Authorized implements SmartHomeDelegate.Callback {

        private final int requestId;

        Authorized(int requestId) {
            this.requestId = requestId;
        }

        @Override
        public void onSuccess(String[] payload) {
            // Re-read rather than trusting the payload: the delegate answers
            // when the flow finishes, and what the user actually chose is the
            // status afterwards.
            SmartHomeDelegate d = AndroidSmartHomeSupport.getDelegate();
            SmartHome.deliverAuthorization(requestId,
                    d == null ? HomeAuthorizationStatus.UNKNOWN.ordinal()
                              : d.authorizationStatus(),
                    null);
        }

        @Override
        public void onError(String errorName, String message) {
            SmartHome.deliverAuthorization(requestId,
                    HomeAuthorizationStatus.UNKNOWN.ordinal(),
                    encode(errorName, message));
        }
    }

    private static final class Refreshed implements SmartHomeDelegate.Callback {

        private final int requestId;

        Refreshed(int requestId) {
            this.requestId = requestId;
        }

        @Override
        public void onSuccess(String[] payload) {
            SmartHome.deliverRefreshed(requestId, null);
        }

        @Override
        public void onError(String errorName, String message) {
            SmartHome.deliverRefreshed(requestId, encode(errorName, message));
        }
    }

    private static final class Readings implements SmartHomeDelegate.Callback {

        private final int requestId;

        Readings(int requestId) {
            this.requestId = requestId;
        }

        @Override
        public void onSuccess(String[] payload) {
            SmartHome.deliverReadings(requestId, orEmpty(payload), null);
        }

        @Override
        public void onError(String errorName, String message) {
            SmartHome.deliverReadings(requestId, NOTHING,
                    encode(errorName, message));
        }
    }

    private static final class WriteResults
            implements SmartHomeDelegate.Callback {

        private final int requestId;

        WriteResults(int requestId) {
            this.requestId = requestId;
        }

        @Override
        public void onSuccess(String[] payload) {
            SmartHome.deliverWriteResults(requestId, orEmpty(payload), null);
        }

        @Override
        public void onError(String errorName, String message) {
            SmartHome.deliverWriteResults(requestId, NOTHING,
                    encode(errorName, message));
        }
    }

    /// Registration is not something the caller waits on -- the handle came
    /// back synchronously and is already live -- and a failure is deliberately
    /// not propagated: a subscription covering twenty traits where one
    /// accessory refused notifications is still worth having, and
    /// TraitConstraint.notifiesOnChange already told the caller which ones
    /// report.
    private static final class Subscribed
            implements SmartHomeDelegate.Callback {

        @Override
        public void onSuccess(String[] payload) {
        }

        @Override
        public void onError(String errorName, String message) {
        }
    }

    private static final class Drained implements SmartHomeDelegate.Callback {

        private final int requestId;

        Drained(int requestId) {
            this.requestId = requestId;
        }

        @Override
        public void onSuccess(String[] payload) {
            int delivered = 0;
            if (payload != null && payload.length > 0 && payload[0] != null) {
                try {
                    delivered = Integer.parseInt(payload[0].trim());
                } catch (NumberFormatException notANumber) {
                    // The count is informational -- the changes themselves
                    // already went through the event sink -- so an unreadable
                    // one is worth zero rather than a failed drain.
                    delivered = 0;
                }
            }
            SmartHome.deliverDrained(requestId, delivered, null);
        }

        @Override
        public void onError(String errorName, String message) {
            SmartHome.deliverDrained(requestId, 0, encode(errorName, message));
        }
    }

    private static final class Commissioned
            implements SmartHomeDelegate.Callback {

        private final int requestId;

        Commissioned(int requestId) {
            this.requestId = requestId;
        }

        @Override
        public void onSuccess(String[] payload) {
            String accessoryId = null;
            String accessoryName = null;
            String home = null;
            int mine = 0;
            if (payload != null && payload.length > 0 && payload[0] != null) {
                String[] f = payload[0].split("\t", -1);
                accessoryId = f.length > 0 && f[0].length() > 0 ? f[0] : null;
                accessoryName = f.length > 1 ? f[1] : null;
                home = f.length > 2 && f[2].length() > 0 ? f[2] : null;
                mine = f.length > 3 && "1".equals(f[3]) ? 1 : 0;
            }
            SmartHome.deliverCommissioningResult(requestId, accessoryId,
                    accessoryName, home, mine, null);
        }

        @Override
        public void onError(String errorName, String message) {
            SmartHome.deliverCommissioningResult(requestId, null, null, null, 0,
                    encode(errorName, message));
        }
    }

    private static final class Identified
            implements SmartHomeDelegate.Callback {

        private final int requestId;

        Identified(int requestId) {
            this.requestId = requestId;
        }

        @Override
        public void onSuccess(String[] payload) {
            SmartHome.deliverIdentifyResult(requestId, null);
        }

        @Override
        public void onError(String errorName, String message) {
            SmartHome.deliverIdentifyResult(requestId,
                    encode(errorName, message));
        }
    }

    private static final class SceneOutcome
            implements SmartHomeDelegate.Callback {

        private final int requestId;
        private final String structureId;

        SceneOutcome(int requestId, String structureId) {
            this.requestId = requestId;
            this.structureId = structureId;
        }

        @Override
        public void onSuccess(String[] payload) {
            SmartHome.deliverSceneResult(requestId,
                    payload == null || payload.length == 0 ? null : payload[0],
                    structureId, null);
        }

        @Override
        public void onError(String errorName, String message) {
            SmartHome.deliverSceneResult(requestId, null, structureId,
                    encode(errorName, message));
        }
    }

    /// Where the injected bridge pushes what nobody asked for.
    private static final class Events implements SmartHomeDelegate.EventSink {

        @Override
        public void changes(String subscriptionId, String[] records) {
            SmartHome.deliverChanges(subscriptionId, orEmpty(records));
        }

        @Override
        public void resyncRequired(String subscriptionId) {
            SmartHome.deliverResyncRequired(subscriptionId);
        }

        @Override
        public void structureChanged(int changeKindOrdinal, String structureId,
                String accessoryId) {
            SmartHome.notifyStructureChanged(changeKindOrdinal, structureId,
                    accessoryId);
        }
    }
}
