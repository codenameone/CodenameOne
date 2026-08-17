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
package com.codename1.impl.home;

import com.codename1.home.HomeAuthorizationStatus;
import com.codename1.home.HomeAvailability;
import com.codename1.home.HomeError;
import com.codename1.home.SmartHome;
import com.codename1.home.StructureChangeKind;
import com.codename1.home.Trait;
import com.codename1.home.TraitConstraint;
import com.codename1.home.TraitReading;
import com.codename1.home.TraitValue;
import com.codename1.home.TraitValueKind;
import com.codename1.home.commissioning.CommissioningStyle;
import com.codename1.home.spi.HomeBridge;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// An app-private simulated home, used by the simulator, the desktop ports and
/// the JavaScript port.
///
/// Reads and writes work, they are durable, and no physical device is
/// involved -- which is why the availability it reports is
/// [HomeAvailability#LOCAL_ONLY] and not `AVAILABLE`. Nothing outside this app
/// can see these accessories.
///
/// #### Why this exists rather than "unsupported on the desktop"
///
/// Almost all of a smart-home feature is code that has nothing to do with
/// accessories: laying out a room, wiring a switch to a write, handling a
/// failure, deciding what to show while a covering is moving. A desktop port
/// that answered `NOT_SUPPORTED` would make all of that untestable outside a
/// phone with real hardware attached, and testable-only-on-hardware is
/// testable-rarely.
///
/// #### Two rules it follows on purpose
///
/// **It never completes inline.** Every answer goes through the same
/// `EdtResult` marshalling the mobile ports use, and every one is delayed by a
/// few milliseconds. It could answer synchronously and it deliberately does
/// not: code written against a store that answers instantly races the moment
/// it meets one that does not, and that asymmetry has already shipped once in
/// this codebase.
///
/// **It does not push changes.** [#isPushDelivery()] answers `false` and
/// changes wait for [#drainChanges(int)], matching every backend except
/// HomeKit in the foreground. A simulator that pushed would let an app be
/// written against the one behaviour it cannot rely on.
///
/// #### Scripting it
///
/// The mutators -- [#setValue(java.lang.String, java.lang.String,
/// com.codename1.home.Trait, com.codename1.home.TraitValue)],
/// [#setReachable(java.lang.String, boolean)],
/// [#setAvailability(com.codename1.home.HomeAvailability)] -- are what the
/// simulator's Smart Home panel drives. `setAvailability` in particular means
/// an app's `PERMISSION_REQUIRED` and `COMMISSIONING_ONLY` branches are
/// reachable on a desktop, which is the only place most people will ever
/// exercise them.
///
/// A new instance is empty. Furnish it with
/// [#addStructure(java.lang.String, java.lang.String, boolean)] and its
/// friends, or with [SyntheticHome#populate(LocalHomeBridge)] for the
/// deliberately awkward house the simulator uses.
public class LocalHomeBridge implements HomeBridge {

    /// How long an operation takes to answer, in milliseconds.
    ///
    /// Small, and deliberately not zero. See the class note.
    private static final int LATENCY_MILLIS = 4;

    private final Map<String, Structure> structures =
            new LinkedHashMap<String, Structure>();
    private final Map<String, Accessory> accessories =
            new LinkedHashMap<String, Accessory>();
    private final Map<String, TraitValue> values =
            new HashMap<String, TraitValue>();
    private final Map<String, Watch> watches =
            new LinkedHashMap<String, Watch>();
    private final Map<String, List<TraitReading>> undelivered =
            new LinkedHashMap<String, List<TraitReading>>();

    private HomeAvailability availability = HomeAvailability.LOCAL_ONLY;
    private HomeAuthorizationStatus authorization =
            HomeAuthorizationStatus.AUTHORIZED;
    private int nextCommissionedIndex = 1;

    // ------------------------------------------------------------------
    // model building, for the simulator and for tests
    // ------------------------------------------------------------------

    /// Adds a home.
    ///
    /// #### Parameters
    ///
    /// - `id`: the identifier
    ///
    /// - `name`: the user-visible name
    ///
    /// - `primary`: whether this is the default home
    public synchronized void addStructure(String id, String name,
            boolean primary) {
        structures.put(id, new Structure(id, name, primary));
    }

    /// Adds a room to a home.
    ///
    /// #### Parameters
    ///
    /// - `structureId`: the home
    ///
    /// - `roomId`: the identifier
    ///
    /// - `name`: the user-visible name
    public synchronized void addRoom(String structureId, String roomId,
            String name) {
        Structure s = structures.get(structureId);
        if (s != null) {
            s.rooms.put(roomId, name);
        }
    }

    /// Adds an accessory to a home.
    ///
    /// #### Parameters
    ///
    /// - `structureId`: the home
    ///
    /// - `accessoryId`: the identifier
    ///
    /// - `name`: the user-visible name
    ///
    /// - `roomId`: the room, or `null`
    ///
    /// - `categoryOrdinal`: the `com.codename1.home.AccessoryCategory`
    ///   ordinal
    public synchronized void addAccessory(String structureId,
            String accessoryId, String name, String roomId,
            int categoryOrdinal) {
        Structure s = structures.get(structureId);
        if (s == null) {
            return;
        }
        Accessory a = new Accessory(accessoryId, name, roomId, categoryOrdinal,
                structureId);
        accessories.put(accessoryId, a);
        s.accessoryIds.add(accessoryId);
    }

    /// Puts an accessory behind a bridge.
    ///
    /// #### Parameters
    ///
    /// - `accessoryId`: the accessory
    ///
    /// - `bridgeAccessoryId`: the bridge it sits behind, or `null` to detach
    ///   it
    public synchronized void setBridge(String accessoryId,
            String bridgeAccessoryId) {
        Accessory a = accessories.get(accessoryId);
        if (a != null) {
            a.bridgeAccessoryId = bridgeAccessoryId;
        }
    }

    /// Adds a service to an accessory.
    ///
    /// #### Parameters
    ///
    /// - `accessoryId`: the accessory
    ///
    /// - `serviceId`: the identifier
    ///
    /// - `name`: the user-visible name
    ///
    /// - `serviceTypeOrdinal`: the `com.codename1.home.ServiceType` ordinal
    ///
    /// - `primary`: whether this is the accessory's main service
    public synchronized void addService(String accessoryId, String serviceId,
            String name, int serviceTypeOrdinal, boolean primary) {
        Accessory a = accessories.get(accessoryId);
        if (a != null) {
            a.services.put(serviceId,
                    new Service(serviceId, name, serviceTypeOrdinal, primary));
        }
    }

    /// Gives a service a trait, with its starting value.
    ///
    /// #### Parameters
    ///
    /// - `accessoryId`: the accessory
    ///
    /// - `serviceId`: the service on it
    ///
    /// - `constraint`: what the service will accept for the trait
    ///
    /// - `initialValue`: the value to start at, or `null` for no value
    public synchronized void addTrait(String accessoryId, String serviceId,
            TraitConstraint constraint, TraitValue initialValue) {
        Accessory a = accessories.get(accessoryId);
        if (a == null) {
            return;
        }
        Service s = a.services.get(serviceId);
        if (s == null) {
            return;
        }
        s.constraints.add(constraint);
        if (initialValue != null) {
            values.put(key(accessoryId, serviceId,
                    constraint.getTrait().getId()), initialValue);
        }
    }

    /// Adds a scene to a home.
    ///
    /// #### Parameters
    ///
    /// - `structureId`: the home
    ///
    /// - `sceneId`: the identifier
    ///
    /// - `name`: the user-visible name
    ///
    /// - `sceneTypeOrdinal`: the `com.codename1.home.SceneType` ordinal
    public synchronized void addScene(String structureId, String sceneId,
            String name, int sceneTypeOrdinal) {
        Structure s = structures.get(structureId);
        if (s != null) {
            s.scenes.put(sceneId,
                    new SceneRecord(sceneId, name, sceneTypeOrdinal));
        }
    }

    /// Adds one action to a scene.
    ///
    /// #### Parameters
    ///
    /// - `structureId`: the home
    ///
    /// - `sceneId`: the scene
    ///
    /// - `accessoryId`: the accessory to act on
    ///
    /// - `serviceId`: the service on it
    ///
    /// - `trait`: the trait to set
    ///
    /// - `value`: the value to set it to
    public synchronized void addSceneAction(String structureId, String sceneId,
            String accessoryId, String serviceId, Trait trait,
            TraitValue value) {
        Structure s = structures.get(structureId);
        if (s == null) {
            return;
        }
        SceneRecord scene = s.scenes.get(sceneId);
        if (scene != null) {
            scene.actions.add(new ActionRecord(accessoryId, serviceId, trait,
                    value));
        }
    }

    // ------------------------------------------------------------------
    // scripting
    // ------------------------------------------------------------------

    /// Changes a trait's value as though the accessory had done it, so
    /// watchers see a change.
    ///
    /// This is the difference between the simulator and a mock: an app can be
    /// driven through a motion sensor firing or a covering finishing its
    /// travel without any of it being written into the app's own test code.
    ///
    /// #### Parameters
    ///
    /// - `accessoryId`: the accessory
    ///
    /// - `serviceId`: the service on it
    ///
    /// - `trait`: the trait to change
    ///
    /// - `value`: the new value, or `null` to make the trait report no value
    public void setValue(String accessoryId, String serviceId, Trait trait,
            TraitValue value) {
        applyValue(accessoryId, serviceId, trait, value);
    }

    /// Makes an accessory reachable or not, firing the matching structure
    /// change.
    ///
    /// #### Parameters
    ///
    /// - `accessoryId`: the accessory
    ///
    /// - `reachable`: whether the platform can talk to it
    public void setReachable(String accessoryId, boolean reachable) {
        String structureId;
        synchronized (this) {
            Accessory a = accessories.get(accessoryId);
            if (a == null || a.reachable == reachable) {
                return;
            }
            a.reachable = reachable;
            structureId = a.structureId;
        }
        SmartHome.notifyStructureChanged(
                StructureChangeKind.REACHABILITY_CHANGED.ordinal(),
                structureId, accessoryId);
    }

    /// Makes this home report a different availability.
    ///
    /// Every state in [HomeAvailability] is reachable this way, which is the
    /// point: an app's `PERMISSION_REQUIRED`, `COMMISSIONING_ONLY` and
    /// `NOT_CONFIGURED` branches are otherwise only exercisable on a device in
    /// exactly the wrong state.
    ///
    /// #### Parameters
    ///
    /// - `availability`: what to report; `null` is ignored
    public void setAvailability(HomeAvailability availability) {
        if (availability == null) {
            return;
        }
        synchronized (this) {
            if (this.availability == availability) {
                return;
            }
            this.availability = availability;
        }
        SmartHome.notifyStructureChanged(
                StructureChangeKind.AVAILABILITY_CHANGED.ordinal(), null,
                null);
    }

    /// Makes this home report a different authorization status.
    ///
    /// #### Parameters
    ///
    /// - `authorization`: what to report; `null` is ignored
    public synchronized void setAuthorizationStatus(
            HomeAuthorizationStatus authorization) {
        if (authorization != null) {
            this.authorization = authorization;
        }
    }

    /// Tells watchers of a subscription that their values are stale, so an
    /// app's resync path can be exercised.
    ///
    /// #### Parameters
    ///
    /// - `subscriptionId`: the subscription to mark
    public void forceResync(String subscriptionId) {
        SmartHome.deliverResyncRequired(subscriptionId);
    }

    /// This home's current value for one trait.
    ///
    /// #### Parameters
    ///
    /// - `accessoryId`: the accessory
    ///
    /// - `serviceId`: the service on it
    ///
    /// - `trait`: the trait
    ///
    /// #### Returns
    ///
    /// the value, or `null` when the trait has none
    public synchronized TraitValue getValue(String accessoryId,
            String serviceId, Trait trait) {
        return values.get(key(accessoryId, serviceId, trait.getId()));
    }

    // ------------------------------------------------------------------
    // HomeBridge -- capability
    // ------------------------------------------------------------------

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public synchronized int getAvailability() {
        return availability.ordinal();
    }

    @Override
    public String getBackendId() {
        return "local";
    }

    @Override
    public String[] getConfigurationProblems() {
        return new String[0];
    }

    @Override
    public boolean areIdsPersistent() {
        return true;
    }

    @Override
    public void start(int requestId) {
        answer(new Started(requestId, getAvailability()));
    }

    @Override
    public void stop() {
        synchronized (this) {
            watches.clear();
            undelivered.clear();
        }
    }

    @Override
    public synchronized int getAuthorizationStatus() {
        return authorization.ordinal();
    }

    @Override
    public void requestAuthorization(final int requestId) {
        synchronized (this) {
            if (authorization == HomeAuthorizationStatus.NOT_DETERMINED) {
                authorization = HomeAuthorizationStatus.AUTHORIZED;
            }
        }
        answer(new Authorized(requestId, getAuthorizationStatus()));
    }

    @Override
    public boolean openHomeSettings() {
        return false;
    }

    @Override
    public boolean openEcosystemApp() {
        return false;
    }

    @Override
    public boolean openProviderSetup() {
        return false;
    }

    // ------------------------------------------------------------------
    // HomeBridge -- graph
    // ------------------------------------------------------------------

    @Override
    public synchronized String[] getStructures() {
        List<String> out = new ArrayList<String>();
        for (Structure s : structures.values()) {
            out.add(HomeWire.join(new String[] {s.id, s.name,
                    HomeWire.flag(s.primary), HomeWire.flag(true),
                    HomeWire.flag(true)}));
        }
        return toArray(out);
    }

    @Override
    public synchronized String[] getRooms(String structureId) {
        Structure s = structures.get(structureId);
        List<String> out = new ArrayList<String>();
        if (s != null) {
            for (Map.Entry<String, String> e : s.rooms.entrySet()) {
                out.add(HomeWire.join(new String[] {e.getKey(),
                        e.getValue()}));
            }
        }
        return toArray(out);
    }

    @Override
    public String[] getZones(String structureId) {
        // Zones are a HomeKit concept and this home is not HomeKit. Empty
        // rather than invented: a synthesized grouping would let an app be
        // built around zones that do not exist anywhere it will actually run.
        return new String[0];
    }

    @Override
    public synchronized String[] getAccessories(String structureId) {
        Structure s = structures.get(structureId);
        List<String> out = new ArrayList<String>();
        if (s == null) {
            return toArray(out);
        }
        for (int i = 0; i < s.accessoryIds.size(); i++) {
            Accessory a = accessories.get(s.accessoryIds.get(i));
            if (a == null) {
                continue;
            }
            out.add(HomeWire.join(new String[] {a.id, a.name,
                    a.roomId == null ? "" : a.roomId,
                    Integer.toString(a.categoryOrdinal), "Codename One",
                    "Simulated", "1.0", HomeWire.flag(a.reachable),
                    a.bridgeAccessoryId == null ? "" : a.bridgeAccessoryId}));
        }
        return toArray(out);
    }

    @Override
    public synchronized String[] getServices(String accessoryId) {
        Accessory a = accessories.get(accessoryId);
        List<String> out = new ArrayList<String>();
        if (a != null) {
            for (Service s : a.services.values()) {
                out.add(HomeWire.join(new String[] {s.id, s.name,
                        Integer.toString(s.typeOrdinal),
                        HomeWire.flag(s.primary)}));
            }
        }
        return toArray(out);
    }

    @Override
    public synchronized String[] getTraits(String accessoryId,
            String serviceId) {
        List<String> out = new ArrayList<String>();
        Service s = serviceOf(accessoryId, serviceId);
        if (s == null) {
            return toArray(out);
        }
        for (int i = 0; i < s.constraints.size(); i++) {
            TraitConstraint c = s.constraints.get(i);
            out.add(HomeWire.join(new String[] {c.getTrait().getId(),
                    HomeWire.flag(c.isReadable()),
                    HomeWire.flag(c.isWritable()),
                    HomeWire.flag(c.notifiesOnChange()),
                    HomeWire.flag(c.hasRange()),
                    Double.toString(c.getMinimum()),
                    Double.toString(c.getMaximum()),
                    Double.toString(c.getStep()), ""}));
        }
        return toArray(out);
    }

    @Override
    public void refresh(int requestId) {
        answer(new Refreshed(requestId));
    }

    // ------------------------------------------------------------------
    // HomeBridge -- reads and writes
    // ------------------------------------------------------------------

    @Override
    public int getMaxReadBatchSize() {
        return 0;
    }

    @Override
    public void readTraits(int requestId, String[] accessoryIds,
            String[] serviceIds, String[] traitIds, boolean allowCached) {
        String[] lines = new String[traitIds.length];
        synchronized (this) {
            for (int i = 0; i < traitIds.length; i++) {
                TraitReading r = readOne(accessoryIds[i], serviceIds[i],
                        traitIds[i]);
                // An unknown trait cannot be encoded, because the encoding is
                // keyed by the trait. An empty record is emitted instead,
                // which the decoder skips -- the caller gets a short list
                // rather than a corrupted one.
                lines[i] = r == null ? "" : HomeWire.encodeReading(r);
            }
        }
        answer(new Readings(requestId, lines));
    }

    private TraitReading readOne(String accessoryId, String serviceId,
            String traitId) {
        Trait trait = Trait.forId(traitId);
        if (trait == null) {
            return null;
        }
        Accessory a = accessories.get(accessoryId);
        if (a == null) {
            return TraitReading.failed(accessoryId, serviceId, trait,
                    HomeError.ACCESSORY_NOT_FOUND,
                    "no such accessory in the simulated home");
        }
        if (!a.reachable) {
            return TraitReading.failed(accessoryId, serviceId, trait,
                    HomeError.ACCESSORY_UNREACHABLE,
                    "this accessory is simulated as unreachable");
        }
        Service s = a.services.get(serviceId);
        if (s == null || s.constraintFor(trait) == null) {
            return TraitReading.failed(accessoryId, serviceId, trait,
                    HomeError.TRAIT_NOT_SUPPORTED,
                    "this service does not have that trait");
        }
        TraitValue v = values.get(key(accessoryId, serviceId, traitId));
        if (v == null) {
            return TraitReading.absent(accessoryId, serviceId, trait);
        }
        return TraitReading.of(accessoryId, serviceId, trait, v,
                System.currentTimeMillis());
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
        String[] lines = new String[traitIds.length];
        List<TraitReading> changed = new ArrayList<TraitReading>();
        synchronized (this) {
            for (int i = 0; i < traitIds.length; i++) {
                lines[i] = writeOne(accessoryIds[i], serviceIds[i],
                        traitIds[i], kinds[i], numericValues[i],
                        stringValues[i], unitWireIds[i], changed);
            }
        }
        recordChanges(changed);
        answer(new WriteResults(requestId, lines));
    }

    private String writeOne(String accessoryId, String serviceId,
            String traitId, int kind, double numeric, String text,
            int unitWireId, List<TraitReading> changed) {
        String[] f = new String[6];
        f[0] = accessoryId;
        f[1] = serviceId;
        f[2] = traitId;
        Trait trait = Trait.forId(traitId);
        if (trait == null) {
            return refusal(f, HomeError.TRAIT_NOT_SUPPORTED,
                    "this build does not know that trait");
        }
        Accessory a = accessories.get(accessoryId);
        if (a == null) {
            return refusal(f, HomeError.ACCESSORY_NOT_FOUND,
                    "no such accessory in the simulated home");
        }
        if (!a.reachable) {
            return refusal(f, HomeError.ACCESSORY_UNREACHABLE,
                    "this accessory is simulated as unreachable");
        }
        Service s = a.services.get(serviceId);
        TraitConstraint c = s == null ? null : s.constraintFor(trait);
        if (c == null) {
            return refusal(f, HomeError.TRAIT_NOT_SUPPORTED,
                    "this service does not have that trait");
        }
        if (!c.isWritable()) {
            return refusal(f, HomeError.READ_ONLY_TRAIT,
                    "this trait reports what the accessory is doing");
        }
        TraitValue value = HomeWire.decodeValue(trait, kind, numeric, text,
                unitWireId, 0, false);
        if (value == null) {
            return refusal(f, HomeError.INVALID_ARGUMENT,
                    "that value does not fit this trait");
        }
        if (!c.accepts(value)) {
            return refusal(f, HomeError.VALUE_OUT_OF_RANGE,
                    "this accessory accepts " + c.getMinimum() + " to "
                            + c.getMaximum());
        }
        values.put(key(accessoryId, serviceId, traitId), value);
        changed.add(TraitReading.of(accessoryId, serviceId, trait, value,
                System.currentTimeMillis()));
        f[3] = HomeWire.flag(true);
        f[4] = "";
        f[5] = "";
        return HomeWire.join(f);
    }

    private static String refusal(String[] f, HomeError error, String message) {
        f[3] = HomeWire.flag(false);
        f[4] = error.name();
        f[5] = message;
        return HomeWire.join(f);
    }

    // ------------------------------------------------------------------
    // HomeBridge -- subscriptions
    // ------------------------------------------------------------------

    @Override
    public boolean isPushDelivery() {
        // Matching every backend except HomeKit in the foreground. A simulator
        // that pushed would let an app be written against the one delivery
        // behaviour it cannot rely on anywhere else.
        return false;
    }

    @Override
    public synchronized void subscribe(int requestId, String subscriptionId,
            String[] accessoryIds, String[] serviceIds, String[] traitIds) {
        Watch w = new Watch();
        for (int i = 0; i < traitIds.length; i++) {
            w.keys.add(key(accessoryIds[i], serviceIds[i], traitIds[i]));
        }
        watches.put(subscriptionId, w);
    }

    @Override
    public synchronized void unsubscribe(String subscriptionId) {
        watches.remove(subscriptionId);
        undelivered.remove(subscriptionId);
    }

    @Override
    public void drainChanges(int requestId) {
        Map<String, List<TraitReading>> batch;
        synchronized (this) {
            if (undelivered.isEmpty()) {
                answer(new Drained(requestId, 0));
                return;
            }
            batch = new LinkedHashMap<String, List<TraitReading>>(undelivered);
            undelivered.clear();
        }
        int delivered = 0;
        for (Map.Entry<String, List<TraitReading>> e : batch.entrySet()) {
            List<TraitReading> readings = e.getValue();
            delivered += readings.size();
            String[] lines = new String[readings.size()];
            for (int i = 0; i < readings.size(); i++) {
                lines[i] = HomeWire.encodeReading(readings.get(i));
            }
            SmartHome.deliverChanges(e.getKey(), lines);
        }
        answer(new Drained(requestId, delivered));
    }

    // ------------------------------------------------------------------
    // HomeBridge -- scenes
    // ------------------------------------------------------------------

    @Override
    public synchronized String[] getScenes(String structureId) {
        Structure s = structures.get(structureId);
        List<String> out = new ArrayList<String>();
        if (s != null) {
            for (SceneRecord scene : s.scenes.values()) {
                out.add(HomeWire.join(new String[] {scene.id, scene.name,
                        Integer.toString(scene.typeOrdinal),
                        HomeWire.flag(true)}));
            }
        }
        return toArray(out);
    }

    @Override
    public synchronized String[] getSceneActions(String structureId,
            String sceneId) {
        List<String> out = new ArrayList<String>();
        Structure s = structures.get(structureId);
        SceneRecord scene = s == null ? null : s.scenes.get(sceneId);
        if (scene == null) {
            return toArray(out);
        }
        for (int i = 0; i < scene.actions.size(); i++) {
            ActionRecord a = scene.actions.get(i);
            out.add(HomeWire.join(new String[] {a.accessoryId, a.serviceId,
                    a.trait.getId(),
                    Integer.toString(a.value.getKind().ordinal()),
                    Double.toString(HomeWire.numericOf(a.value)),
                    a.value.getKind() == TraitValueKind.STRING
                            ? a.value.getString() : "",
                    Integer.toString(a.value.getUnit().getWireId())}));
        }
        return toArray(out);
    }

    @Override
    public void executeScene(int requestId, String structureId,
            String sceneId) {
        List<TraitReading> changed = new ArrayList<TraitReading>();
        String line;
        synchronized (this) {
            Structure s = structures.get(structureId);
            SceneRecord scene = s == null ? null : s.scenes.get(sceneId);
            if (scene == null) {
                answer(new SceneResult(requestId, null, structureId,
                        HomeError.INVALID_ARGUMENT.name() + "\tno such scene"));
                return;
            }
            for (int i = 0; i < scene.actions.size(); i++) {
                ActionRecord a = scene.actions.get(i);
                values.put(key(a.accessoryId, a.serviceId, a.trait.getId()),
                        a.value);
                changed.add(TraitReading.of(a.accessoryId, a.serviceId,
                        a.trait, a.value, System.currentTimeMillis()));
            }
            line = HomeWire.join(new String[] {scene.id, scene.name,
                Integer.toString(scene.typeOrdinal), HomeWire.flag(true)});
        }
        recordChanges(changed);
        answer(new SceneResult(requestId, line, structureId, null));
    }

    @Override
    public void createScene(int requestId, String structureId, String name,
            String[] accessoryIds, String[] serviceIds, String[] traitIds,
            int[] kinds, double[] numericValues, String[] stringValues,
            int[] unitWireIds) {
        String line;
        synchronized (this) {
            Structure s = structures.get(structureId);
            if (s == null) {
                answer(new SceneResult(requestId, null, structureId,
                        HomeError.INVALID_ARGUMENT.name() + "\tno such home"));
                return;
            }
            String sceneId = "scene-" + (s.scenes.size() + 1);
            SceneRecord scene = new SceneRecord(sceneId, name,
                    com.codename1.home.SceneType.USER_DEFINED.ordinal());
            for (int i = 0; i < traitIds.length; i++) {
                Trait trait = Trait.forId(traitIds[i]);
                if (trait == null) {
                    continue;
                }
                TraitValue v = HomeWire.decodeValue(trait, kinds[i],
                        numericValues[i], stringValues[i], unitWireIds[i], 0,
                        false);
                if (v != null) {
                    scene.actions.add(new ActionRecord(accessoryIds[i],
                            serviceIds[i], trait, v));
                }
            }
            s.scenes.put(sceneId, scene);
            line = HomeWire.join(new String[] {sceneId, name,
                Integer.toString(scene.typeOrdinal), HomeWire.flag(true)});
        }
        answer(new SceneResult(requestId, line, structureId, null));
    }

    @Override
    public void deleteScene(int requestId, String structureId,
            String sceneId) {
        synchronized (this) {
            Structure s = structures.get(structureId);
            if (s != null) {
                s.scenes.remove(sceneId);
            }
        }
        answer(new SceneResult(requestId, null, structureId, null));
    }

    // ------------------------------------------------------------------
    // HomeBridge -- commissioning
    // ------------------------------------------------------------------

    @Override
    public int getCommissioningStyle() {
        return CommissioningStyle.OS_OWNED_UI.ordinal();
    }

    @Override
    public void commission(int requestId, String setupPayload,
            String structureId, String roomId, String suggestedName,
            int timeoutMillis) {
        String targetStructure = structureId;
        String accessoryId;
        String name;
        synchronized (this) {
            if (targetStructure == null || targetStructure.length() == 0
                    || !structures.containsKey(targetStructure)) {
                targetStructure = structures.isEmpty() ? null
                        : structures.keySet().iterator().next();
            }
            if (targetStructure == null) {
                answer(new Commissioned(requestId, null, null, null, false,
                        HomeError.COMMISSIONING_FAILED.name()
                                + "\tthe simulated home has no structure to"
                                + " add an accessory to"));
                return;
            }
            accessoryId = "commissioned-" + (nextCommissionedIndex++);
            name = suggestedName == null || suggestedName.length() == 0
                    ? "New Accessory" : suggestedName;
            addAccessory(targetStructure, accessoryId, name,
                    roomId == null || roomId.length() == 0 ? null : roomId,
                    com.codename1.home.AccessoryCategory.SWITCH.ordinal());
            addService(accessoryId, "1", name,
                    com.codename1.home.ServiceType.SWITCH.ordinal(), true);
            addTrait(accessoryId, "1",
                    TraitConstraint.of(Trait.ON_OFF, true, true, true),
                    TraitValue.of(false));
        }
        SmartHome.notifyStructureChanged(
                StructureChangeKind.ACCESSORY_ADDED.ordinal(), targetStructure,
                accessoryId);
        answer(new Commissioned(requestId, accessoryId, name, targetStructure,
                true, null));
    }

    @Override
    public void identify(int requestId, String accessoryId) {
        answer(new Identified(requestId));
    }

    // ------------------------------------------------------------------
    // internals
    // ------------------------------------------------------------------

    private void applyValue(String accessoryId, String serviceId, Trait trait,
            TraitValue value) {
        List<TraitReading> changed = new ArrayList<TraitReading>();
        synchronized (this) {
            String k = key(accessoryId, serviceId, trait.getId());
            if (value == null) {
                values.remove(k);
                changed.add(TraitReading.absent(accessoryId, serviceId,
                        trait));
            } else {
                values.put(k, value);
                changed.add(TraitReading.of(accessoryId, serviceId, trait,
                        value, System.currentTimeMillis()));
            }
        }
        recordChanges(changed);
    }

    /// Files changed readings against the subscriptions that asked for them,
    /// to be handed over on the next drain.
    ///
    /// Held rather than delivered, because [#isPushDelivery()] answers
    /// `false` and delivering anyway would make this simulator the one place
    /// an app's polling path is never exercised.
    private void recordChanges(List<TraitReading> changed) {
        if (changed.isEmpty()) {
            return;
        }
        synchronized (this) {
            for (Map.Entry<String, Watch> e : watches.entrySet()) {
                List<TraitReading> forThisWatch = null;
                for (TraitReading r : changed) {
                    if (!e.getValue().keys.contains(key(r.getAccessoryId(),
                            r.getServiceId(), r.getTrait().getId()))) {
                        continue;
                    }
                    if (forThisWatch == null) {
                        forThisWatch = undelivered.get(e.getKey());
                        if (forThisWatch == null) {
                            forThisWatch = new ArrayList<TraitReading>();
                            undelivered.put(e.getKey(), forThisWatch);
                        }
                    }
                    forThisWatch.add(r);
                }
            }
        }
    }

    private Service serviceOf(String accessoryId, String serviceId) {
        Accessory a = accessories.get(accessoryId);
        return a == null ? null : a.services.get(serviceId);
    }

    private static String key(String accessoryId, String serviceId,
            String traitId) {
        // "\0" rather than a literal NUL byte: the character is the right
        // separator -- it cannot occur in a platform identifier -- but written
        // raw it makes this file binary to grep and diff.
        return accessoryId + "\0" + serviceId + "\0" + traitId;
    }

    private static String[] toArray(List<String> list) {
        String[] out = new String[list.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = list.get(i);
        }
        return out;
    }

    /// Answers after a short delay, never inline.
    ///
    /// The delay is the point: code written against a store that answers
    /// synchronously races the moment it meets one that does not, and a
    /// simulator that completed inline would let that code look correct right
    /// up until it reached a phone.
    private void answer(Runnable delivery) {
        if (Display.isInitialized()) {
            Display.getInstance().setTimeout(LATENCY_MILLIS, delivery);
            return;
        }
        // No Display, so this is a unit test driving the bridge directly.
        // Inline is the only option and is safe there: the EDT contract this
        // delay protects is about a running application.
        delivery.run();
    }

    // ------------------------------------------------------------------
    // model records
    // ------------------------------------------------------------------

    private static final class Structure {
        private final String id;
        private final String name;
        private final boolean primary;
        private final Map<String, String> rooms =
                new LinkedHashMap<String, String>();
        private final List<String> accessoryIds = new ArrayList<String>();
        private final Map<String, SceneRecord> scenes =
                new LinkedHashMap<String, SceneRecord>();

        Structure(String id, String name, boolean primary) {
            this.id = id;
            this.name = name;
            this.primary = primary;
        }
    }

    private static final class Accessory {
        private final String id;
        private final String name;
        private final String roomId;
        private final int categoryOrdinal;
        private final String structureId;
        private final Map<String, Service> services =
                new LinkedHashMap<String, Service>();
        private boolean reachable = true;
        private String bridgeAccessoryId;

        Accessory(String id, String name, String roomId, int categoryOrdinal,
                String structureId) {
            this.id = id;
            this.name = name;
            this.roomId = roomId;
            this.categoryOrdinal = categoryOrdinal;
            this.structureId = structureId;
        }
    }

    private static final class Service {
        private final String id;
        private final String name;
        private final int typeOrdinal;
        private final boolean primary;
        private final List<TraitConstraint> constraints =
                new ArrayList<TraitConstraint>();

        Service(String id, String name, int typeOrdinal, boolean primary) {
            this.id = id;
            this.name = name;
            this.typeOrdinal = typeOrdinal;
            this.primary = primary;
        }

        TraitConstraint constraintFor(Trait trait) {
            for (TraitConstraint c : constraints) {
                // Reference equality on purpose: Trait instances are
                // interned constants, so == is the identity test the class
                // documents. Trait does not override equals, so .equals()
                // would be the same comparison spelled longer.
                if (c.getTrait() == trait) { //NOPMD CompareObjectsWithEquals
                    return c;
                }
            }
            return null;
        }
    }

    private static final class SceneRecord {
        private final String id;
        private final String name;
        private final int typeOrdinal;
        private final List<ActionRecord> actions =
                new ArrayList<ActionRecord>();

        SceneRecord(String id, String name, int typeOrdinal) {
            this.id = id;
            this.name = name;
            this.typeOrdinal = typeOrdinal;
        }
    }

    private static final class ActionRecord {
        private final String accessoryId;
        private final String serviceId;
        private final Trait trait;
        private final TraitValue value;

        ActionRecord(String accessoryId, String serviceId, Trait trait,
                TraitValue value) {
            this.accessoryId = accessoryId;
            this.serviceId = serviceId;
            this.trait = trait;
            this.value = value;
        }
    }

    private static final class Watch {
        private final List<String> keys = new ArrayList<String>();
    }

    // ------------------------------------------------------------------
    // deliveries, named so they carry no synthetic outer reference
    // ------------------------------------------------------------------

    private static final class Started implements Runnable {
        private final int requestId;
        private final int availability;

        Started(int requestId, int availability) {
            this.requestId = requestId;
            this.availability = availability;
        }

        @Override
        public void run() {
            SmartHome.deliverStarted(requestId, availability, null);
        }
    }

    private static final class Refreshed implements Runnable {
        private final int requestId;

        Refreshed(int requestId) {
            this.requestId = requestId;
        }

        @Override
        public void run() {
            SmartHome.deliverRefreshed(requestId, null);
        }
    }

    private static final class Authorized implements Runnable {
        private final int requestId;
        private final int status;

        Authorized(int requestId, int status) {
            this.requestId = requestId;
            this.status = status;
        }

        @Override
        public void run() {
            SmartHome.deliverAuthorization(requestId, status, null);
        }
    }

    private static final class Readings implements Runnable {
        private final int requestId;
        private final String[] lines;

        Readings(int requestId, String[] lines) {
            this.requestId = requestId;
            this.lines = lines;
        }

        @Override
        public void run() {
            SmartHome.deliverReadings(requestId, lines, null);
        }
    }

    private static final class WriteResults implements Runnable {
        private final int requestId;
        private final String[] lines;

        WriteResults(int requestId, String[] lines) {
            this.requestId = requestId;
            this.lines = lines;
        }

        @Override
        public void run() {
            SmartHome.deliverWriteResults(requestId, lines, null);
        }
    }

    private static final class SceneResult implements Runnable {
        private final int requestId;
        private final String sceneLine;
        private final String structureId;
        private final String error;

        SceneResult(int requestId, String sceneLine, String structureId,
                String error) {
            this.requestId = requestId;
            this.sceneLine = sceneLine;
            this.structureId = structureId;
            this.error = error;
        }

        @Override
        public void run() {
            SmartHome.deliverSceneResult(requestId, sceneLine, structureId,
                    error);
        }
    }

    private static final class Commissioned implements Runnable {
        private final int requestId;
        private final String accessoryId;
        private final String accessoryName;
        private final String structureId;
        private final boolean mine;
        private final String error;

        Commissioned(int requestId, String accessoryId, String accessoryName,
                String structureId, boolean mine, String error) {
            this.requestId = requestId;
            this.accessoryId = accessoryId;
            this.accessoryName = accessoryName;
            this.structureId = structureId;
            this.mine = mine;
            this.error = error;
        }

        @Override
        public void run() {
            SmartHome.deliverCommissioningResult(requestId, accessoryId,
                    accessoryName, structureId, mine ? 1 : 0, error);
        }
    }

    private static final class Drained implements Runnable {
        private final int requestId;
        private final int count;

        Drained(int requestId, int count) {
            this.requestId = requestId;
            this.count = count;
        }

        @Override
        public void run() {
            SmartHome.deliverDrained(requestId, count, null);
        }
    }

    private static final class Identified implements Runnable {
        private final int requestId;

        Identified(int requestId) {
            this.requestId = requestId;
        }

        @Override
        public void run() {
            SmartHome.deliverIdentifyResult(requestId, null);
        }
    }
}
