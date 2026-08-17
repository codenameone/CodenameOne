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
package com.codename1.impl.ios;

import com.codename1.home.spi.HomeBridge;
import com.codename1.util.StringUtil;

import java.util.List;

/// Apple `HomeBridge`, backing `com.codename1.home` with HomeKit and, for
/// commissioning, `MatterSupport`.
///
/// #### Arrays cross as one newline-joined string
///
/// The SPI is written in terms of `String[]`, and every native here takes and
/// returns a single `String` instead, joined with newlines. That is not an
/// oversight: building a Java `String[]` from Objective-C means allocating a
/// Java array and a Java string per element on the ParparVM heap, from
/// whatever thread HomeKit called back on. The wearable bridge reached the
/// same conclusion for the same reason, and this class does the splitting on
/// the Java side where it is free.
///
/// A record can never contain a newline or a tab, because the native side
/// replaces both with a space before joining -- the only fields carrying
/// arbitrary text are accessory, room and scene names, which are the user's
/// own and are worth less than a decodable record.
///
/// #### It is dead code unless the build asked for it
///
/// The natives below are compiled only when the builder flipped
/// `CN1_INCLUDE_HOMEKIT`, which it does when the app references
/// `com.codename1.home`. Without it every native answers unsupported,
/// `isSupported()` is `false`, and `SmartHome` reports `NOT_SUPPORTED`.
final class IOSHomeBridge implements HomeBridge {

    private final IOSNative nativeInstance;

    IOSHomeBridge(IOSNative nativeInstance) {
        this.nativeInstance = nativeInstance;
        // Touch the callback surface so the optimizer cannot decide the
        // static entry points are unreachable and replace them with empty
        // stubs -- the same guard IOSDeviceIntegrity documents. Without a
        // reachable reference the native side calls into nothing and every
        // operation hangs waiting for an answer that was compiled away.
        IOSHomeCallbacks.keepAlive();
    }

    // ------------------------------------------------------------------
    // capability
    // ------------------------------------------------------------------

    @Override
    public boolean isSupported() {
        return nativeInstance.homeSupported();
    }

    @Override
    public int getAvailability() {
        return nativeInstance.homeAvailability();
    }

    @Override
    public String getBackendId() {
        return "homekit";
    }

    @Override
    public String[] getConfigurationProblems() {
        return split(nativeInstance.homeConfigurationProblems());
    }

    @Override
    public boolean areIdsPersistent() {
        // HMHome.uniqueIdentifier and HMAccessory.uniqueIdentifier are stable
        // for the life of the accessory's membership of the home, so a
        // favourite can be persisted by id.
        return true;
    }

    @Override
    public void start(int requestId) {
        nativeInstance.homeStart(requestId);
    }

    @Override
    public void stop() {
        nativeInstance.homeStop();
    }

    @Override
    public int getAuthorizationStatus() {
        return nativeInstance.homeAuthorizationStatus();
    }

    @Override
    public void requestAuthorization(int requestId) {
        nativeInstance.homeRequestAuthorization(requestId);
    }

    @Override
    public boolean openHomeSettings() {
        return nativeInstance.homeOpenSettings();
    }

    @Override
    public boolean openEcosystemApp() {
        return nativeInstance.homeOpenEcosystemApp();
    }

    @Override
    public boolean openProviderSetup() {
        // HomeKit ships with the OS; there is no provider to install or
        // update, so there is nowhere to send the user. Answering false is
        // the honest result rather than opening the App Store at something
        // irrelevant.
        return false;
    }

    // ------------------------------------------------------------------
    // graph
    // ------------------------------------------------------------------

    @Override
    public String[] getStructures() {
        return split(nativeInstance.homeStructures());
    }

    @Override
    public String[] getRooms(String structureId) {
        return split(nativeInstance.homeRooms(structureId));
    }

    @Override
    public String[] getZones(String structureId) {
        return split(nativeInstance.homeZones(structureId));
    }

    @Override
    public String[] getAccessories(String structureId) {
        return split(nativeInstance.homeAccessories(structureId));
    }

    @Override
    public String[] getServices(String accessoryId) {
        return split(nativeInstance.homeServices(accessoryId));
    }

    @Override
    public String[] getTraits(String accessoryId, String serviceId) {
        return split(nativeInstance.homeTraits(accessoryId, serviceId));
    }

    @Override
    public void refresh(int requestId) {
        nativeInstance.homeRefresh(requestId);
    }

    // ------------------------------------------------------------------
    // reads and writes
    // ------------------------------------------------------------------

    @Override
    public int getMaxReadBatchSize() {
        // HomeKit reads one characteristic at a time anyway -- there is no
        // batch API -- so there is no size the framework needs to split at.
        // The native side issues them concurrently and answers once.
        return 0;
    }

    @Override
    public void readTraits(int requestId, String[] accessoryIds,
            String[] serviceIds, String[] traitIds, boolean allowCached) {
        nativeInstance.homeReadTraits(requestId, join(accessoryIds),
                join(serviceIds), join(traitIds), allowCached);
    }

    @Override
    public int getMaxWriteBatchSize() {
        return 0;
    }

    @Override
    public void writeTraits(int requestId, String[] accessoryIds,
            String[] serviceIds, String[] traitIds, int[] kinds,
            double[] numericValues, String[] stringValues, int[] unitWireIds,
            String[] authorizationData) {
        nativeInstance.homeWriteTraits(requestId, join(accessoryIds),
                join(serviceIds), join(traitIds), joinInts(kinds),
                joinDoubles(numericValues), join(stringValues),
                joinInts(unitWireIds), join(authorizationData));
    }

    // ------------------------------------------------------------------
    // subscriptions
    // ------------------------------------------------------------------

    @Override
    public boolean isPushDelivery() {
        // True, and narrower than it sounds: HMAccessoryDelegate fires only
        // while the app is running in the foreground. The home hub, not this
        // app, is what reacts to an accessory while the phone is asleep.
        // TraitSubscription's javadoc says so; this is the one backend that
        // can answer true at all.
        return nativeInstance.homeSupported();
    }

    @Override
    public void subscribe(int requestId, String subscriptionId,
            String[] accessoryIds, String[] serviceIds, String[] traitIds) {
        nativeInstance.homeSubscribe(requestId, subscriptionId,
                join(accessoryIds), join(serviceIds), join(traitIds));
    }

    @Override
    public void unsubscribe(String subscriptionId) {
        nativeInstance.homeUnsubscribe(subscriptionId);
    }

    @Override
    public void drainChanges(int requestId) {
        nativeInstance.homeDrainChanges(requestId);
    }

    // ------------------------------------------------------------------
    // scenes
    // ------------------------------------------------------------------

    @Override
    public String[] getScenes(String structureId) {
        return split(nativeInstance.homeScenes(structureId));
    }

    @Override
    public String[] getSceneActions(String structureId, String sceneId) {
        return split(nativeInstance.homeSceneActions(structureId, sceneId));
    }

    @Override
    public void executeScene(int requestId, String structureId,
            String sceneId) {
        nativeInstance.homeExecuteScene(requestId, structureId, sceneId);
    }

    @Override
    public void createScene(int requestId, String structureId, String name,
            String[] accessoryIds, String[] serviceIds, String[] traitIds,
            int[] kinds, double[] numericValues, String[] stringValues,
            int[] unitWireIds) {
        nativeInstance.homeCreateScene(requestId, structureId, name,
                join(accessoryIds), join(serviceIds), join(traitIds),
                joinInts(kinds), joinDoubles(numericValues),
                join(stringValues), joinInts(unitWireIds));
    }

    @Override
    public void deleteScene(int requestId, String structureId,
            String sceneId) {
        nativeInstance.homeDeleteScene(requestId, structureId, sceneId);
    }

    // ------------------------------------------------------------------
    // commissioning
    // ------------------------------------------------------------------

    @Override
    public int getCommissioningStyle() {
        return nativeInstance.homeCommissioningStyle();
    }

    @Override
    public void commission(int requestId, String setupPayload,
            String structureId, String roomId, String suggestedName,
            int timeoutMillis) {
        nativeInstance.homeCommission(requestId, setupPayload, structureId,
                roomId, suggestedName, timeoutMillis);
    }

    @Override
    public void identify(int requestId, String accessoryId) {
        nativeInstance.homeIdentify(requestId, accessoryId);
    }

    // ------------------------------------------------------------------
    // wire helpers
    // ------------------------------------------------------------------

    /// Splits the native side's newline-joined records.
    ///
    /// No unescaping, unlike the wearable bridge's paths: the native side
    /// replaces a newline inside a field with a space rather than escaping it,
    /// because the only fields that can carry one are user-chosen names and a
    /// name that loses a line break is worth less than a record that survives.
    private static String[] split(String joined) {
        if (joined == null || joined.length() == 0) {
            return new String[0];
        }
        List<String> parts = StringUtil.tokenize(joined, '\n');
        String[] out = new String[parts.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = parts.get(i);
        }
        return out;
    }

    private static String join(String[] values) {
        if (values == null || values.length == 0) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                b.append('\n');
            }
            b.append(values[i] == null ? "" : values[i]);
        }
        return b.toString();
    }

    private static String joinInts(int[] values) {
        if (values == null || values.length == 0) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                b.append('\n');
            }
            b.append(values[i]);
        }
        return b.toString();
    }

    private static String joinDoubles(double[] values) {
        if (values == null || values.length == 0) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                b.append('\n');
            }
            b.append(values[i]);
        }
        return b.toString();
    }
}
