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

import com.codename1.home.commissioning.Commissioner;
import com.codename1.home.commissioning.CommissioningRequest;
import com.codename1.home.commissioning.CommissioningResult;
import com.codename1.home.spi.HomeBridge;
import com.codename1.impl.async.EdtResult;
import com.codename1.impl.home.CommissioningGateway;
import com.codename1.impl.home.HomeWire;
import com.codename1.impl.home.PendingMap;
import com.codename1.impl.home.SubscriptionState;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Entry point for the Codename One smart-home API -- reading the accessories
/// in a user's home, reading and writing what they can do, watching them for
/// change, running scenes, and adding new Matter accessories.
///
/// [#getInstance()] never returns `null`. On a port with no smart-home support
/// every operation fails fast with [HomeError#NOT_SUPPORTED] and every graph
/// accessor returns an empty list, so calling code needs no platform-specific
/// `if`.
///
/// #### Quick start
///
/// ```java
/// SmartHome home = SmartHome.getInstance();
/// if (home.getAvailability() == HomeAvailability.PERMISSION_REQUIRED) {
///     home.requestAuthorization();
///     return;
/// }
/// home.refresh().onResult((structures, err) -> {
///     if (err != null) {
///         return;
///     }
///     HomeStructure h = home.getPrimaryStructure();
///     for (Accessory a : h.getAccessoriesSupporting(Trait.ON_OFF)) {
///         addRow(a);
///     }
/// });
/// ```
///
/// Turning a light on:
///
/// ```java
/// AccessoryService svc = lamp.getPrimaryService();
/// home.write(new TraitWrite(lamp, svc, Trait.ON_OFF, TraitValue.of(true)));
/// ```
///
/// #### Three things that will surprise you
///
/// **Android's default answer is not "available".** With no extra setup an
/// Android app can commission a Matter accessory and do nothing else: the
/// graph is empty and no trait can be read or written. That is
/// [HomeAvailability#COMMISSIONING_ONLY], and it exists because reporting
/// `AVAILABLE` would make the word mean something entirely different there
/// than it does on iOS. The full graph needs the Google Home APIs, which need
/// a Google Cloud project and a Home Developer Console registration only you
/// can create; [#getConfigurationProblems()] names what is missing.
///
/// **Nothing wakes your app for an accessory change.** HomeKit delivers
/// changes only while your app is in the foreground, and the Google Home APIs
/// need a live signed-in client -- the home hub, not your app, is what reacts
/// to a sensor while the phone sleeps. Check
/// [TraitSubscription#isPushDelivery()] rather than assuming; where it answers
/// `false`, changes arrive when you call [#drainChanges()] and at no other
/// time.
///
/// **A missing value is not zero and not an error.** An accessory can
/// legitimately have nothing to report -- an unmeasured temperature, a hue on
/// a light that is currently in white mode. Ask [TraitReading#hasValue()]
/// before [TraitReading#getValue()]; nothing in this API substitutes a zero
/// for a measurement that was never taken.
///
/// #### Threading
///
/// Every method here may be called from the EDT and returns immediately.
/// Every `AsyncResource` this class hands back resolves on the EDT, and every
/// [HomeChangeListener] and [HomeStructureListener] delivery arrives on it --
/// on every platform, including the desktop, simulator and JavaScript ports,
/// which marshal rather than answering on whichever thread happened to ask. A
/// callback may touch components directly.
///
/// #### Platform support
///
/// - **iOS, iPadOS, watchOS, tvOS, macOS** -- HomeKit. Needs the
///   `com.apple.developer.homekit` entitlement and an
///   `ios.NSHomeKitUsageDescription` build hint; the build fails with an
///   actionable message if the description is missing. Commissioning is
///   iOS-only.
/// - **Android** -- Google Play services Matter commissioning always, and the
///   full Google Home APIs graph when the `android.googleHome.enabled` build
///   hint and its project configuration are present.
/// - **Simulator, desktop, JavaScript** -- a local, app-private simulated
///   home, reported as [HomeAvailability#LOCAL_ONLY].
///
/// #### Not claimed in this release
///
/// Automations and triggers -- scenes only, and [#isAutomationSupported()]
/// answers `false` everywhere. Topology writes: creating homes, renaming
/// rooms, moving accessories. Cameras and video. Security and alarm panels.
/// Matter events, which is why [LockState#JAMMED] is unreachable outside
/// HomeKit. Energy, appliance and diagnostic clusters. And Codename One is not
/// a Matter controller -- everything Matter goes through the OS ecosystem, so
/// the Apple Home or Google Home app has to be installed and set up.
public final class SmartHome {

    /// The one instance. Not re-created when the bridge appears, because it
    /// owns subscriptions and listeners that a replacement would silently
    /// drop.
    private static SmartHome instance;

    private HomeBridge bridge;
    private boolean bridgeResolved;
    private boolean started;
    private int nextRequestId = 1;
    private int nextSubscriptionId = 1;

    private final PendingMap<List<HomeStructure>> pendingGraph =
            new PendingMap<List<HomeStructure>>();
    private final PendingMap<HomeAuthorizationStatus> pendingAuthorization =
            new PendingMap<HomeAuthorizationStatus>();
    private final PendingMap<List<TraitReading>> pendingReads =
            new PendingMap<List<TraitReading>>();
    private final PendingMap<List<TraitWriteResult>> pendingWrites =
            new PendingMap<List<TraitWriteResult>>();
    private final PendingMap<Scene> pendingScenes =
            new PendingMap<Scene>();
    private final PendingMap<CommissioningResult> pendingCommissioning =
            new PendingMap<CommissioningResult>();
    private final PendingMap<Integer> pendingDrains =
            new PendingMap<Integer>();
    private final PendingMap<Object> pendingIdentify =
            new PendingMap<Object>();

    /// The write each read request was built from, so a reply can be matched
    /// back to what was asked for -- the wire carries only the answers.
    private final Map<Integer, TraitReadRequest> readRequests =
            new HashMap<Integer, TraitReadRequest>();
    private final Map<Integer, List<TraitWrite>> writeRequests =
            new HashMap<Integer, List<TraitWrite>>();

    private final Map<String, SubscriptionState> subscriptions =
            new HashMap<String, SubscriptionState>();
    private final List<HomeStructureListener> structureListeners =
            new ArrayList<HomeStructureListener>();

    private List<HomeStructure> structures = Collections.emptyList();
    private final Commissioner commissioner;

    private SmartHome() {
        commissioner = new Commissioner(new Gateway());
    }

    /// The smart-home API for this device.
    ///
    /// Never `null`. On a port with no support the returned instance answers
    /// [HomeAvailability#NOT_SUPPORTED] and every operation fails fast, so
    /// there is nothing to null-check and no platform branch to write.
    ///
    /// #### Returns
    ///
    /// the instance, never `null`
    public static synchronized SmartHome getInstance() {
        if (instance == null) {
            instance = new SmartHome();
        }
        return instance;
    }

    /// Replaces the singleton with one wired to the supplied bridge, for
    /// tests.
    ///
    /// Package-private, so only this package's tests can reach it -- the same
    /// arrangement `com.codename1.maps.spi.MapProviderRegistry` uses for the
    /// same reason. Global state that a test suite shares is otherwise
    /// order-dependent: a test that runs after one which resolved a bridge
    /// sees that bridge, and the failure looks like a bug in whichever test
    /// happened to run second.
    ///
    /// Passing `null` gives a bridgeless instance without waiting for
    /// `Display` to be absent, which is what the degradation tests need.
    ///
    /// #### Parameters
    ///
    /// - `testBridge`: the bridge to install, or `null` for none
    static synchronized void resetForTest(HomeBridge testBridge) {
        SmartHome replacement = new SmartHome();
        replacement.installBridge(testBridge);
        instance = replacement;
    }

    /// Holds the instance monitor, not the class one.
    ///
    /// `resetForTest` is `static synchronized`, which locks the class -- a
    /// different monitor from the one [#bridge()] uses, so assigning the
    /// fields there directly left them written under one lock and read under
    /// another.
    private synchronized void installBridge(HomeBridge testBridge) {
        this.bridge = testBridge;
        this.bridgeResolved = true;
    }

    /// Resolved on first use rather than in the constructor.
    ///
    /// A static initializer or a constructor lookup would run before
    /// `Display.init`, find no bridge, and cache that answer for the life of
    /// the process -- so an app that touched this class early would report
    /// no smart-home support on a phone that has it.
    private synchronized HomeBridge bridge() {
        if (!bridgeResolved && Display.isInitialized()) {
            bridge = Display.getInstance().getHomeBridge();
            bridgeResolved = true;
        }
        return bridge;
    }

    private synchronized int nextRequestId() {
        int id = nextRequestId++;
        if (nextRequestId <= 0) {
            // Zero is reserved for unsolicited deliveries, so the wrap goes
            // back to one rather than to zero.
            nextRequestId = 1;
        }
        return id;
    }

    // ------------------------------------------------------------------
    // capability
    // ------------------------------------------------------------------

    /// Whether this device has any smart-home support at all.
    ///
    /// A coarse question. [#getAvailability()] is the one worth asking,
    /// because "supported" covers a device whose user has never opened the
    /// Home app and one whose every light is ready to switch.
    ///
    /// #### Returns
    ///
    /// `true` when a backend is present
    public boolean isSupported() {
        HomeBridge b = bridge();
        return b != null && b.isSupported();
    }

    /// Whether a home graph is usable right now, and when it is not, why.
    ///
    /// Check this before anything else, and branch through it rather than
    /// through platform detection.
    ///
    /// #### Returns
    ///
    /// the availability, never `null`
    public HomeAvailability getAvailability() {
        HomeBridge b = bridge();
        if (b == null || !b.isSupported()) {
            return HomeAvailability.NOT_SUPPORTED;
        }
        HomeAvailability[] all = HomeAvailability.values();
        int ordinal = b.getAvailability();
        if (ordinal < 0 || ordinal >= all.length) {
            return HomeAvailability.NOT_SUPPORTED;
        }
        return all[ordinal];
    }

    /// Which platform service is behind this instance.
    ///
    /// For explaining a limitation to a user, not for branching on -- every
    /// real capability question has its own query. See [HomeBackend].
    ///
    /// #### Returns
    ///
    /// the backend, never `null`
    public HomeBackend getBackend() {
        HomeBridge b = bridge();
        if (b == null) {
            return HomeBackend.NONE;
        }
        String id = b.getBackendId();
        if ("homekit".equals(id)) {
            return HomeBackend.HOMEKIT;
        }
        if ("google_home".equals(id)) {
            return HomeBackend.GOOGLE_HOME;
        }
        if ("matter_only".equals(id)) {
            return HomeBackend.MATTER_COMMISSIONING_ONLY;
        }
        if ("local".equals(id)) {
            return HomeBackend.LOCAL;
        }
        return HomeBackend.NONE;
    }

    /// Build configuration this backend needs and does not have.
    ///
    /// One sentence per problem, each naming the build hint that fixes it --
    /// a missing `ios.NSHomeKitUsageDescription`, a missing
    /// `android.googleHome.projectId`. Empty when nothing is missing.
    ///
    /// **This text is for you, not for your user.** Nothing here can be fixed
    /// at runtime; it is a description of what the build left out. Log it,
    /// show it in a debug screen, and do not put it in front of someone
    /// holding a phone.
    ///
    /// #### Returns
    ///
    /// an immutable list, possibly empty
    public List<String> getConfigurationProblems() {
        HomeBridge b = bridge();
        if (b == null) {
            return Collections.emptyList();
        }
        String[] problems = b.getConfigurationProblems();
        if (problems == null || problems.length == 0) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<String>(problems.length);
        for (int i = 0; i < problems.length; i++) {
            if (problems[i] != null && problems[i].length() > 0) {
                out.add(problems[i]);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /// Whether accessory and structure identifiers survive an app restart.
    ///
    /// Both shipping backends answer `true`, so a favourite can be persisted
    /// by id. Ask before doing so anyway -- a local or test backend that
    /// regenerates its graph does not, and a favourites list that silently
    /// empties on every launch is a confusing bug to trace.
    ///
    /// #### Returns
    ///
    /// `true` when identifiers are stable across launches
    public boolean areIdsPersistent() {
        HomeBridge b = bridge();
        return b != null && b.areIdsPersistent();
    }

    /// Whether this release can create or run automations -- a scene plus a
    /// trigger.
    ///
    /// **Always `false`.** HomeKit, Google Home and Matter model triggers in
    /// three incompatible ways and Matter has none at all, so there is no
    /// honest common shape to expose. Scenes work everywhere; see
    /// [#executeScene(Scene)].
    ///
    /// The method exists rather than the capability being absent silently, so
    /// an app can say why the feature it wanted is not offered.
    ///
    /// #### Returns
    ///
    /// `false`
    public boolean isAutomationSupported() {
        return false;
    }

    /// Adding new Matter accessories.
    ///
    /// Never `null`; ask [Commissioner#isSupported()] before offering an "add
    /// a device" button.
    ///
    /// #### Returns
    ///
    /// the commissioner, never `null`
    public Commissioner getCommissioner() {
        return commissioner;
    }

    // ------------------------------------------------------------------
    // authorization
    // ------------------------------------------------------------------

    /// Whether the user has granted this app access to their home.
    ///
    /// #### Returns
    ///
    /// the status, never `null`
    public HomeAuthorizationStatus getAuthorizationStatus() {
        HomeBridge b = bridge();
        if (b == null) {
            return HomeAuthorizationStatus.UNKNOWN;
        }
        HomeAuthorizationStatus[] all = HomeAuthorizationStatus.values();
        int ordinal = b.getAuthorizationStatus();
        if (ordinal < 0 || ordinal >= all.length) {
            return HomeAuthorizationStatus.UNKNOWN;
        }
        return all[ordinal];
    }

    /// Prompts the user for access to their home.
    ///
    /// Resolves when the platform's flow finishes, **whatever the user
    /// chose** -- so a resolved result means they were asked, not that they
    /// agreed. Read the status it carries.
    ///
    /// #### Returns
    ///
    /// the resulting status, delivered on the EDT
    public AsyncResource<HomeAuthorizationStatus> requestAuthorization() {
        HomeBridge b = bridge();
        if (b == null) {
            return failed(HomeError.NOT_SUPPORTED,
                    "this platform has no smart-home support");
        }
        int id = nextRequestId();
        EdtResult<HomeAuthorizationStatus> result =
                pendingAuthorization.open(id);
        b.requestAuthorization(id);
        return result;
    }

    /// Opens the system settings page where the user can change this app's
    /// smart-home access.
    ///
    /// The only recovery from [HomeAuthorizationStatus#DENIED]: once the user
    /// has said no, the platform will not ask again from inside the app.
    ///
    /// #### Returns
    ///
    /// `true` when something was opened
    public boolean openHomeSettings() {
        HomeBridge b = bridge();
        return b != null && b.openHomeSettings();
    }

    /// Opens the platform's ecosystem app -- Apple Home, Google Home.
    ///
    /// The right answer to [HomeAvailability#NOT_CONFIGURED]: a user with no
    /// homes has to create one somewhere, and it is not here.
    ///
    /// #### Returns
    ///
    /// `true` when the app was opened; `false` when it is not installed
    public boolean openEcosystemApp() {
        HomeBridge b = bridge();
        return b != null && b.openEcosystemApp();
    }

    /// Opens wherever the user installs or updates the backend's provider.
    ///
    /// The recovery from [HomeAvailability#PROVIDER_NOT_INSTALLED] and
    /// [HomeAvailability#PROVIDER_UPDATE_REQUIRED].
    ///
    /// #### Returns
    ///
    /// `true` when something was opened
    public boolean openProviderSetup() {
        HomeBridge b = bridge();
        return b != null && b.openProviderSetup();
    }

    // ------------------------------------------------------------------
    // graph
    // ------------------------------------------------------------------

    /// Loads the home graph from the platform.
    ///
    /// Call this once before reading [#getStructures()], and again whenever a
    /// [HomeStructureListener] says the topology moved. It connects to the
    /// backend on first use, so it is also what triggers a permission prompt
    /// on a platform that defers one.
    ///
    /// #### Returns
    ///
    /// the homes, delivered on the EDT
    public AsyncResource<List<HomeStructure>> refresh() {
        HomeBridge b = bridge();
        if (b == null) {
            return failed(HomeError.NOT_SUPPORTED,
                    "this platform has no smart-home support");
        }
        int id = nextRequestId();
        EdtResult<List<HomeStructure>> result = pendingGraph.open(id);
        boolean needsStart;
        synchronized (this) {
            needsStart = !started;
            started = true;
        }
        if (needsStart) {
            b.start(id);
        } else {
            b.refresh(id);
        }
        return result;
    }

    /// The homes, as of the last [#refresh()].
    ///
    /// Empty until a refresh has completed -- this reads a cached snapshot and
    /// never calls into the platform, so it cannot block and cannot fail. See
    /// [Accessory] for why the graph is snapshots rather than live handles.
    ///
    /// #### Returns
    ///
    /// an immutable list, possibly empty
    public List<HomeStructure> getStructures() {
        synchronized (this) {
            return structures;
        }
    }

    /// The user's default home, as of the last [#refresh()].
    ///
    /// The one their ecosystem app opens. An app that only ever wants one home
    /// should use this rather than the first entry of [#getStructures()],
    /// which is in whatever order the platform gave them.
    ///
    /// #### Returns
    ///
    /// the primary home, the first home when none is marked primary, or
    /// `null` when there are none
    public HomeStructure getPrimaryStructure() {
        List<HomeStructure> all = getStructures();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).isPrimary()) {
                return all.get(i);
            }
        }
        return all.isEmpty() ? null : all.get(0);
    }

    /// One accessory by identifier, across every home.
    ///
    /// #### Parameters
    ///
    /// - `accessoryId`: the identifier to look up, or `null`
    ///
    /// #### Returns
    ///
    /// the accessory, or `null` when no home holds it
    public Accessory findAccessory(String accessoryId) {
        if (accessoryId == null) {
            return null;
        }
        List<HomeStructure> all = getStructures();
        for (int i = 0; i < all.size(); i++) {
            Accessory a = all.get(i).getAccessory(accessoryId);
            if (a != null) {
                return a;
            }
        }
        return null;
    }

    /// Asks an accessory to make itself known -- blink, beep, whatever it
    /// does.
    ///
    /// Best-effort: mandatory in Matter and present on HomeKit, but not
    /// reliably surfaced by the Google Home APIs, where it fails with
    /// [HomeError#TRAIT_NOT_SUPPORTED] rather than doing nothing quietly.
    ///
    /// #### Parameters
    ///
    /// - `accessory`: the accessory to identify
    ///
    /// #### Returns
    ///
    /// completion, delivered on the EDT
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `accessory` is `null`
    public AsyncResource<Object> identify(Accessory accessory) {
        if (accessory == null) {
            throw new IllegalArgumentException("accessory is required");
        }
        HomeBridge b = bridge();
        if (b == null) {
            return failed(HomeError.NOT_SUPPORTED,
                    "this platform has no smart-home support");
        }
        int id = nextRequestId();
        EdtResult<Object> result = pendingIdentify.open(id);
        b.identify(id, accessory.getId());
        return result;
    }

    // ------------------------------------------------------------------
    // reads
    // ------------------------------------------------------------------

    /// The largest number of traits this backend reads in one call, or zero
    /// for no limit.
    ///
    /// Informational: [#read(TraitReadRequest)] splits a larger request and
    /// recombines the answers, so there is no size a caller has to stay under.
    ///
    /// #### Returns
    ///
    /// the batch limit, or zero
    public int getMaxReadBatchSize() {
        HomeBridge b = bridge();
        return b == null ? 0 : b.getMaxReadBatchSize();
    }

    /// Reads trait values.
    ///
    /// One [TraitReading] per requested trait, in the order they were added.
    /// **A partial success is the normal case**: three values and one
    /// unreachable accessory resolve successfully, with the failure carried on
    /// that one reading. The resource itself fails only when the request never
    /// reached the platform.
    ///
    /// #### Parameters
    ///
    /// - `request`: what to read
    ///
    /// #### Returns
    ///
    /// the readings, delivered on the EDT
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `request` is `null`
    public AsyncResource<List<TraitReading>> read(TraitReadRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        // Before the bridge check, deliberately. A request that asks for
        // nothing has no platform component, so answering "nothing" is
        // correct everywhere -- and a caller who built a request from a list
        // that filtered down to empty should not get a different answer on
        // the desktop than on a phone.
        if (request.isEmpty()) {
            EdtResult<List<TraitReading>> empty =
                    new EdtResult<List<TraitReading>>();
            empty.complete(Collections.<TraitReading>emptyList());
            return empty;
        }
        HomeBridge b = bridge();
        if (b == null) {
            return failed(HomeError.NOT_SUPPORTED,
                    "this platform has no smart-home support");
        }
        int id = nextRequestId();
        EdtResult<List<TraitReading>> result = pendingReads.open(id);
        synchronized (this) {
            readRequests.put(Integer.valueOf(id), request);
        }
        List<String> accessoryIds = request.getAccessoryIds();
        List<String> serviceIds = request.getServiceIds();
        List<Trait> traits = request.getTraits();
        String[] traitIds = new String[traits.size()];
        for (int i = 0; i < traits.size(); i++) {
            traitIds[i] = traits.get(i).getId();
        }
        b.readTraits(id, toArray(accessoryIds), toArray(serviceIds), traitIds,
                request.isAllowCached());
        return result;
    }

    /// Reads one trait, for the common case where a request builder would be
    /// noise.
    ///
    /// Prefer [#read(TraitReadRequest)] when reading more than one: the cost
    /// of a read is the round trip to the platform, so four separate calls are
    /// four of them.
    ///
    /// #### Parameters
    ///
    /// - `accessory`: the accessory to read
    ///
    /// - `service`: the service on it
    ///
    /// - `trait`: the trait to read
    ///
    /// #### Returns
    ///
    /// the reading, delivered on the EDT
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when any argument is `null`
    public AsyncResource<TraitReading> read(Accessory accessory,
            AccessoryService service, Trait trait) {
        final EdtResult<TraitReading> single = new EdtResult<TraitReading>();
        read(new TraitReadRequest().add(accessory, service, trait))
                .onResult(new SingleReading(single, accessory, service,
                        trait));
        return single;
    }

    // ------------------------------------------------------------------
    // writes
    // ------------------------------------------------------------------

    /// The largest number of traits this backend writes in one call, or zero
    /// for no limit.
    ///
    /// #### Returns
    ///
    /// the batch limit, or zero
    public int getMaxWriteBatchSize() {
        HomeBridge b = bridge();
        return b == null ? 0 : b.getMaxWriteBatchSize();
    }

    /// Writes trait values.
    ///
    /// One [TraitWriteResult] per write, in order. **A partial success is the
    /// normal case** -- "turn off every light" against a home with a dead bulb
    /// mostly worked, and failing the whole operation would have the caller
    /// retry and flicker the house. The resource itself fails only when the
    /// request never reached the platform.
    ///
    /// #### Parameters
    ///
    /// - `writes`: what to change
    ///
    /// #### Returns
    ///
    /// the outcomes, delivered on the EDT
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `writes` is `null` or holds a
    ///   `null`
    public AsyncResource<List<TraitWriteResult>> write(
            List<TraitWrite> writes) {
        if (writes == null) {
            throw new IllegalArgumentException("writes are required");
        }
        // Before the bridge check, for the same reason as in read().
        if (writes.isEmpty()) {
            EdtResult<List<TraitWriteResult>> empty =
                    new EdtResult<List<TraitWriteResult>>();
            empty.complete(Collections.<TraitWriteResult>emptyList());
            return empty;
        }
        HomeBridge b = bridge();
        if (b == null) {
            return failed(HomeError.NOT_SUPPORTED,
                    "this platform has no smart-home support");
        }
        int count = writes.size();
        String[] accessoryIds = new String[count];
        String[] serviceIds = new String[count];
        String[] traitIds = new String[count];
        int[] kinds = new int[count];
        double[] numeric = new double[count];
        String[] text = new String[count];
        int[] units = new int[count];
        String authorization = null;
        List<TraitWrite> copy = new ArrayList<TraitWrite>(count);
        for (int i = 0; i < count; i++) {
            TraitWrite w = writes.get(i);
            if (w == null) {
                throw new IllegalArgumentException(
                        "writes cannot contain a null at index " + i);
            }
            copy.add(w);
            TraitValue v = w.getValue();
            accessoryIds[i] = w.getAccessoryId();
            serviceIds[i] = w.getServiceId();
            traitIds[i] = w.getTrait().getId();
            kinds[i] = v.getKind().ordinal();
            numeric[i] = HomeWire.numericOf(v);
            text[i] = v.getKind() == TraitValueKind.STRING ? v.getString() : "";
            units[i] = v.getUnit().getWireId();
            if (w.getAuthorizationData() != null) {
                authorization = w.getAuthorizationData();
            }
        }
        int id = nextRequestId();
        EdtResult<List<TraitWriteResult>> result = pendingWrites.open(id);
        synchronized (this) {
            writeRequests.put(Integer.valueOf(id), copy);
        }
        b.writeTraits(id, accessoryIds, serviceIds, traitIds, kinds, numeric,
                text, units, authorization);
        return result;
    }

    /// Writes one trait value.
    ///
    /// #### Parameters
    ///
    /// - `write`: what to change
    ///
    /// #### Returns
    ///
    /// the outcome, delivered on the EDT
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `write` is `null`
    public AsyncResource<TraitWriteResult> write(TraitWrite write) {
        if (write == null) {
            throw new IllegalArgumentException("write is required");
        }
        final EdtResult<TraitWriteResult> single =
                new EdtResult<TraitWriteResult>();
        List<TraitWrite> one = new ArrayList<TraitWrite>(1);
        one.add(write);
        write(one).onResult(new SingleWriteResult(single, write));
        return single;
    }

    // ------------------------------------------------------------------
    // subscriptions
    // ------------------------------------------------------------------

    /// Watches traits for change.
    ///
    /// The handle comes back immediately; the platform registration happens
    /// behind it. **Hold on to the handle and [TraitSubscription#stop()] it**
    /// -- a dropped subscription keeps its listener reachable and keeps the
    /// platform delivering.
    ///
    /// Check [TraitSubscription#isPushDelivery()] on the result. Where it
    /// answers `false`, which is everywhere except HomeKit in the foreground,
    /// this listener will not fire until you call [#drainChanges()].
    ///
    /// #### Parameters
    ///
    /// - `request`: what to watch
    ///
    /// - `listener`: where changes are delivered, on the EDT
    ///
    /// #### Returns
    ///
    /// the subscription handle, never `null`
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when either argument is `null`, or the
    ///   request is empty
    public TraitSubscription subscribe(SubscriptionRequest request,
            HomeChangeListener listener) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener is required");
        }
        if (request.isEmpty()) {
            throw new IllegalArgumentException(
                    "a subscription that watches nothing would never fire;"
                            + " add at least one trait");
        }
        HomeBridge b = bridge();
        String subscriptionId;
        synchronized (this) {
            subscriptionId = "s" + (nextSubscriptionId++);
        }
        boolean push = b != null && b.isPushDelivery();
        TraitSubscription handle =
                new TraitSubscription(subscriptionId, this, push);
        SubscriptionState state = new SubscriptionState(subscriptionId,
                listener, request.getMinIntervalMillis());
        synchronized (this) {
            subscriptions.put(subscriptionId, state);
        }
        if (b == null) {
            // Nothing to register against. The handle is live and inert, which
            // is the same shape as every other unsupported answer here: the
            // caller's teardown code still works and no null check is needed.
            return handle;
        }
        List<String> accessoryIds = request.getAccessoryIds();
        List<String> serviceIds = request.getServiceIds();
        List<Trait> traits = request.getTraits();
        String[] traitIds = new String[traits.size()];
        for (int i = 0; i < traits.size(); i++) {
            traitIds[i] = traits.get(i).getId();
        }
        b.subscribe(nextRequestId(), subscriptionId, toArray(accessoryIds),
                toArray(serviceIds), traitIds);
        if (request.isDeliverInitialValues()) {
            deliverInitialValues(state, request);
        }
        return handle;
    }

    private void deliverInitialValues(final SubscriptionState state,
            SubscriptionRequest request) {
        TraitReadRequest read = new TraitReadRequest();
        List<String> accessoryIds = request.getAccessoryIds();
        List<String> serviceIds = request.getServiceIds();
        List<Trait> traits = request.getTraits();
        for (int i = 0; i < traits.size(); i++) {
            read.add(accessoryIds.get(i), serviceIds.get(i), traits.get(i));
        }
        read(read).onResult(new InitialDelivery(state));
    }

    /// Called by [TraitSubscription#stop()].
    void unsubscribeInternal(TraitSubscription subscription) {
        SubscriptionState state;
        synchronized (this) {
            state = subscriptions.remove(subscription.getId());
        }
        if (state != null) {
            state.dispose();
        }
        HomeBridge b = bridge();
        if (b != null) {
            b.unsubscribe(subscription.getId());
        }
    }

    /// Collects changes the platform has been holding and delivers them to
    /// the subscriptions that asked for them.
    ///
    /// **The only way changes arrive** on every backend except HomeKit in the
    /// foreground; see [TraitSubscription#isPushDelivery()]. Wire this into
    /// the point where your app comes to the foreground, and into whatever
    /// polling cadence suits what you are showing.
    ///
    /// #### Returns
    ///
    /// how many changed readings were delivered, on the EDT
    public AsyncResource<Integer> drainChanges() {
        HomeBridge b = bridge();
        if (b == null) {
            EdtResult<Integer> none = new EdtResult<Integer>();
            none.complete(Integer.valueOf(0));
            return none;
        }
        int id = nextRequestId();
        EdtResult<Integer> result = pendingDrains.open(id);
        b.drainChanges(id);
        return result;
    }

    // ------------------------------------------------------------------
    // structure listeners
    // ------------------------------------------------------------------

    /// Watches the home graph for topology changes.
    ///
    /// Deliveries arrive on the EDT. This is about accessories appearing,
    /// disappearing, being renamed, moving room or changing reachability --
    /// not about their values, which is what [#subscribe(SubscriptionRequest,
    /// HomeChangeListener)] is for.
    ///
    /// #### Parameters
    ///
    /// - `listener`: the listener to add; `null` is ignored
    public void addStructureListener(HomeStructureListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (this) {
            if (!structureListeners.contains(listener)) {
                structureListeners.add(listener);
            }
        }
    }

    /// Stops watching the home graph.
    ///
    /// #### Parameters
    ///
    /// - `listener`: the listener to remove; `null` and unknown listeners are
    ///   ignored
    public void removeStructureListener(HomeStructureListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (this) {
            structureListeners.remove(listener);
        }
    }

    // ------------------------------------------------------------------
    // scenes
    // ------------------------------------------------------------------

    /// Runs a scene.
    ///
    /// #### Parameters
    ///
    /// - `scene`: the scene to run
    ///
    /// #### Returns
    ///
    /// completion, delivered on the EDT
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `scene` is `null`
    public AsyncResource<Scene> executeScene(Scene scene) {
        if (scene == null) {
            throw new IllegalArgumentException("scene is required");
        }
        HomeBridge b = bridge();
        if (b == null) {
            return failed(HomeError.NOT_SUPPORTED,
                    "this platform has no smart-home support");
        }
        if (!scene.isExecutable()) {
            return failed(HomeError.UNAUTHORIZED, "the scene \""
                    + scene.getName() + "\" cannot be run from here");
        }
        int id = nextRequestId();
        EdtResult<Scene> result = pendingScenes.open(id);
        b.executeScene(id, scene.getStructureId(), scene.getId());
        return result;
    }

    /// Creates a scene from a set of accessory states.
    ///
    /// Check [HomeStructure#isSceneAuthoringSupported()] before offering this;
    /// several backends will run a scene and not author one.
    ///
    /// #### Parameters
    ///
    /// - `structure`: the home to create it in
    ///
    /// - `name`: the scene's name
    ///
    /// - `actions`: what it should do; [TraitWrite#toSceneAction()] turns the
    ///   changes a user just made into these
    ///
    /// #### Returns
    ///
    /// the new scene, delivered on the EDT
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when any argument is `null`, when the
    ///   name is empty, or when there are no actions
    public AsyncResource<Scene> createScene(HomeStructure structure,
            String name, List<SceneAction> actions) {
        if (structure == null) {
            throw new IllegalArgumentException("structure is required");
        }
        if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("a scene needs a name");
        }
        if (actions == null || actions.isEmpty()) {
            throw new IllegalArgumentException(
                    "a scene with no actions would do nothing");
        }
        HomeBridge b = bridge();
        if (b == null) {
            return failed(HomeError.NOT_SUPPORTED,
                    "this platform has no smart-home support");
        }
        if (!structure.isSceneAuthoringSupported()) {
            return failed(HomeError.NOT_SUPPORTED,
                    "scenes cannot be created in \"" + structure.getName()
                            + "\"; check isSceneAuthoringSupported() first");
        }
        int count = actions.size();
        String[] accessoryIds = new String[count];
        String[] serviceIds = new String[count];
        String[] traitIds = new String[count];
        int[] kinds = new int[count];
        double[] numeric = new double[count];
        String[] text = new String[count];
        int[] units = new int[count];
        for (int i = 0; i < count; i++) {
            SceneAction a = actions.get(i);
            if (a == null) {
                throw new IllegalArgumentException(
                        "actions cannot contain a null at index " + i);
            }
            TraitValue v = a.getValue();
            accessoryIds[i] = a.getAccessoryId();
            serviceIds[i] = a.getServiceId();
            traitIds[i] = a.getTrait().getId();
            kinds[i] = v.getKind().ordinal();
            numeric[i] = HomeWire.numericOf(v);
            text[i] = v.getKind() == TraitValueKind.STRING ? v.getString() : "";
            units[i] = v.getUnit().getWireId();
        }
        int id = nextRequestId();
        EdtResult<Scene> result = pendingScenes.open(id);
        b.createScene(id, structure.getId(), name.trim(), accessoryIds,
                serviceIds, traitIds, kinds, numeric, text, units);
        return result;
    }

    /// Deletes a scene.
    ///
    /// #### Parameters
    ///
    /// - `scene`: the scene to delete
    ///
    /// #### Returns
    ///
    /// completion, delivered on the EDT
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `scene` is `null`
    public AsyncResource<Scene> deleteScene(Scene scene) {
        if (scene == null) {
            throw new IllegalArgumentException("scene is required");
        }
        HomeBridge b = bridge();
        if (b == null) {
            return failed(HomeError.NOT_SUPPORTED,
                    "this platform has no smart-home support");
        }
        int id = nextRequestId();
        EdtResult<Scene> result = pendingScenes.open(id);
        b.deleteScene(id, scene.getStructureId(), scene.getId());
        return result;
    }

    // ------------------------------------------------------------------
    // native -> Java delivery
    // ------------------------------------------------------------------

    /// Answers [#refresh()]'s first call. Called by the ports from any
    /// thread.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the request being answered
    ///
    /// - `availabilityOrdinal`: the availability now that the backend is
    ///   connected
    ///
    /// - `error`: the encoded failure, or `null` for success
    public static void deliverStarted(int requestId, int availabilityOrdinal,
            String error) {
        getInstance().onGraphAnswer(requestId, error);
    }

    /// Answers [#refresh()]. Called by the ports from any thread.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the request being answered
    ///
    /// - `error`: the encoded failure, or `null` for success
    public static void deliverRefreshed(int requestId, String error) {
        getInstance().onGraphAnswer(requestId, error);
    }

    /// Answers [#requestAuthorization()]. Called by the ports from any
    /// thread.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the request being answered
    ///
    /// - `statusOrdinal`: the resulting
    ///   [HomeAuthorizationStatus] ordinal
    ///
    /// - `error`: the encoded failure, or `null` for success
    public static void deliverAuthorization(int requestId, int statusOrdinal,
            String error) {
        SmartHome home = getInstance();
        EdtResult<HomeAuthorizationStatus> result =
                home.pendingAuthorization.take(requestId);
        if (result == null) {
            return;
        }
        HomeException failure = HomeWire.decodeError(error);
        if (failure != null) {
            result.error(failure);
            return;
        }
        HomeAuthorizationStatus[] all = HomeAuthorizationStatus.values();
        result.complete(statusOrdinal >= 0 && statusOrdinal < all.length
                ? all[statusOrdinal] : HomeAuthorizationStatus.UNKNOWN);
    }

    /// Answers [#read(TraitReadRequest)]. Called by the ports from any
    /// thread.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the request being answered
    ///
    /// - `lines`: one encoded reading per requested trait
    ///
    /// - `error`: the encoded failure, or `null` for success
    public static void deliverReadings(int requestId, String[] lines,
            String error) {
        SmartHome home = getInstance();
        EdtResult<List<TraitReading>> result = home.pendingReads.take(requestId);
        synchronized (home) {
            home.readRequests.remove(Integer.valueOf(requestId));
        }
        if (result == null) {
            return;
        }
        HomeException failure = HomeWire.decodeError(error);
        if (failure != null) {
            result.error(failure);
            return;
        }
        result.complete(decodeReadings(lines));
    }

    /// Answers [#write(java.util.List)]. Called by the ports from any thread.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the request being answered
    ///
    /// - `lines`: one outcome per write, as
    ///   `accessoryId \t serviceId \t traitId \t applied \t errorName \t
    ///   errorMessage`
    ///
    /// - `error`: the encoded failure, or `null` for success
    public static void deliverWriteResults(int requestId, String[] lines,
            String error) {
        SmartHome home = getInstance();
        EdtResult<List<TraitWriteResult>> result =
                home.pendingWrites.take(requestId);
        List<TraitWrite> writes;
        synchronized (home) {
            writes = home.writeRequests.remove(Integer.valueOf(requestId));
        }
        if (result == null) {
            return;
        }
        HomeException failure = HomeWire.decodeError(error);
        if (failure != null) {
            result.error(failure);
            return;
        }
        result.complete(decodeWriteResults(lines, writes));
    }

    /// Answers [#executeScene(Scene)], [#createScene(HomeStructure,
    /// java.lang.String, java.util.List)] and [#deleteScene(Scene)]. Called by
    /// the ports from any thread.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the request being answered
    ///
    /// - `sceneLine`: the affected scene, encoded as in
    ///   `HomeBridge#getScenes(java.lang.String)`, or `null`
    ///
    /// - `structureId`: the home the scene belongs to
    ///
    /// - `error`: the encoded failure, or `null` for success
    public static void deliverSceneResult(int requestId, String sceneLine,
            String structureId, String error) {
        SmartHome home = getInstance();
        EdtResult<Scene> result = home.pendingScenes.take(requestId);
        if (result == null) {
            return;
        }
        HomeException failure = HomeWire.decodeError(error);
        if (failure != null) {
            result.error(failure);
            return;
        }
        result.complete(sceneLine == null || sceneLine.length() == 0 ? null
                : HomeWire.decodeScene(sceneLine, structureId,
                        Collections.<SceneAction>emptyList()));
    }

    /// Answers [Commissioner#commission(CommissioningRequest)]. Called by the
    /// ports from any thread.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the request being answered
    ///
    /// - `accessoryId`: the new accessory, or `null`
    ///
    /// - `accessoryName`: the name it ended up with, or `null`
    ///
    /// - `structureId`: the home it joined, or `null`
    ///
    /// - `commissionedToThisApp`: `1` when this app can address it
    ///
    /// - `error`: the encoded failure, or `null` for success
    public static void deliverCommissioningResult(int requestId,
            String accessoryId, String accessoryName, String structureId,
            int commissionedToThisApp, String error) {
        SmartHome home = getInstance();
        EdtResult<CommissioningResult> result =
                home.pendingCommissioning.take(requestId);
        if (result == null) {
            return;
        }
        HomeException failure = HomeWire.decodeError(error);
        if (failure != null) {
            result.error(failure);
            return;
        }
        result.complete(new CommissioningResult(accessoryId, accessoryName,
                structureId, commissionedToThisApp != 0));
    }

    /// Answers [#identify(Accessory)]. Called by the ports from any thread.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the request being answered
    ///
    /// - `error`: the encoded failure, or `null` for success
    public static void deliverIdentifyResult(int requestId, String error) {
        SmartHome home = getInstance();
        EdtResult<Object> result = home.pendingIdentify.take(requestId);
        if (result == null) {
            return;
        }
        HomeException failure = HomeWire.decodeError(error);
        if (failure != null) {
            result.error(failure);
        } else {
            result.complete(null);
        }
    }

    /// Answers [#drainChanges()]. Called by the ports from any thread, after
    /// every [#deliverChanges(java.lang.String, java.lang.String[])] the drain
    /// produced.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the request being answered
    ///
    /// - `deliveredCount`: how many readings were handed over
    ///
    /// - `error`: the encoded failure, or `null` for success
    public static void deliverDrained(int requestId, int deliveredCount,
            String error) {
        SmartHome home = getInstance();
        EdtResult<Integer> result = home.pendingDrains.take(requestId);
        if (result == null) {
            return;
        }
        HomeException failure = HomeWire.decodeError(error);
        if (failure != null) {
            result.error(failure);
        } else {
            result.complete(Integer.valueOf(deliveredCount));
        }
    }

    /// Delivers watched trait changes. Called by the ports from any thread,
    /// unsolicited or in response to a drain.
    ///
    /// #### Parameters
    ///
    /// - `subscriptionId`: which subscription these belong to
    ///
    /// - `lines`: the encoded readings
    public static void deliverChanges(String subscriptionId, String[] lines) {
        SmartHome home = getInstance();
        SubscriptionState state;
        synchronized (home) {
            state = home.subscriptions.get(subscriptionId);
        }
        if (state == null) {
            return;
        }
        state.offer(decodeReadings(lines), false);
    }

    /// Tells subscribers that changes were missed and their values are stale.
    ///
    /// #### Parameters
    ///
    /// - `subscriptionId`: which subscription lost its stream
    public static void deliverResyncRequired(String subscriptionId) {
        SmartHome home = getInstance();
        SubscriptionState state;
        synchronized (home) {
            state = home.subscriptions.get(subscriptionId);
        }
        if (state != null) {
            state.markResyncRequired();
        }
    }

    /// Reports a change to the home graph. Called by the ports from any
    /// thread.
    ///
    /// #### Parameters
    ///
    /// - `changeKindOrdinal`: the [StructureChangeKind] ordinal
    ///
    /// - `structureId`: the home affected, or `null`
    ///
    /// - `accessoryId`: the accessory affected, or `null`
    public static void notifyStructureChanged(int changeKindOrdinal,
            String structureId, String accessoryId) {
        StructureChangeKind[] all = StructureChangeKind.values();
        StructureChangeKind kind = changeKindOrdinal >= 0
                && changeKindOrdinal < all.length ? all[changeKindOrdinal]
                : StructureChangeKind.STRUCTURES_CHANGED;
        getInstance().fireStructureChanged(
                new HomeStructureEvent(kind, structureId, accessoryId));
    }

    // ------------------------------------------------------------------
    // internals
    // ------------------------------------------------------------------

    private void onGraphAnswer(int requestId, String error) {
        EdtResult<List<HomeStructure>> result = pendingGraph.take(requestId);
        HomeException failure = HomeWire.decodeError(error);
        if (failure != null) {
            synchronized (this) {
                // A failed start must not leave the flag set, or the retry
                // would call refresh() on a bridge that never connected and
                // fail in a way that no longer names the real problem.
                started = false;
            }
            if (result != null) {
                result.error(failure);
            }
            return;
        }
        List<HomeStructure> rebuilt = buildGraph();
        synchronized (this) {
            structures = rebuilt;
        }
        if (result != null) {
            result.complete(rebuilt);
        }
    }

    private List<HomeStructure> buildGraph() {
        HomeBridge b = bridge();
        if (b == null) {
            return Collections.emptyList();
        }
        String[] structureLines = b.getStructures();
        if (structureLines == null || structureLines.length == 0) {
            return Collections.emptyList();
        }
        List<HomeStructure> out =
                new ArrayList<HomeStructure>(structureLines.length);
        for (int i = 0; i < structureLines.length; i++) {
            String[] f = HomeWire.split(structureLines[i]);
            String structureId = HomeWire.field(f, 0);
            if (structureId.length() == 0) {
                continue;
            }
            out.add(new HomeStructure(structureId, HomeWire.field(f, 1),
                    HomeWire.flag(f, 2), HomeWire.flag(f, 3),
                    HomeWire.flag(f, 4), buildRooms(b, structureId),
                    buildZones(b, structureId),
                    buildAccessories(b, structureId),
                    buildScenes(b, structureId)));
        }
        return Collections.unmodifiableList(out);
    }

    private static List<HomeRoom> buildRooms(HomeBridge b, String structureId) {
        String[] lines = b.getRooms(structureId);
        List<HomeRoom> out = new ArrayList<HomeRoom>();
        if (lines == null) {
            return out;
        }
        for (int i = 0; i < lines.length; i++) {
            HomeRoom room = HomeWire.decodeRoom(lines[i], structureId);
            if (room != null) {
                out.add(room);
            }
        }
        return out;
    }

    private static List<HomeZone> buildZones(HomeBridge b, String structureId) {
        String[] lines = b.getZones(structureId);
        List<HomeZone> out = new ArrayList<HomeZone>();
        if (lines == null) {
            return out;
        }
        for (int i = 0; i < lines.length; i++) {
            HomeZone zone = HomeWire.decodeZone(lines[i]);
            if (zone != null) {
                out.add(zone);
            }
        }
        return out;
    }

    private static List<Accessory> buildAccessories(HomeBridge b,
            String structureId) {
        String[] lines = b.getAccessories(structureId);
        List<Accessory> out = new ArrayList<Accessory>();
        if (lines == null) {
            return out;
        }
        for (int i = 0; i < lines.length; i++) {
            String accessoryId = HomeWire.field(HomeWire.split(lines[i]), 0);
            if (accessoryId.length() == 0) {
                continue;
            }
            Accessory accessory = HomeWire.decodeAccessory(lines[i],
                    buildServices(b, accessoryId));
            if (accessory != null) {
                out.add(accessory);
            }
        }
        return out;
    }

    private static List<AccessoryService> buildServices(HomeBridge b,
            String accessoryId) {
        String[] lines = b.getServices(accessoryId);
        List<AccessoryService> out = new ArrayList<AccessoryService>();
        if (lines == null) {
            return out;
        }
        for (int i = 0; i < lines.length; i++) {
            String serviceId = HomeWire.field(HomeWire.split(lines[i]), 0);
            if (serviceId.length() == 0) {
                continue;
            }
            AccessoryService service = HomeWire.decodeService(lines[i],
                    buildConstraints(b, accessoryId, serviceId));
            if (service != null) {
                out.add(service);
            }
        }
        return out;
    }

    private static List<TraitConstraint> buildConstraints(HomeBridge b,
            String accessoryId, String serviceId) {
        String[] lines = b.getTraits(accessoryId, serviceId);
        List<TraitConstraint> out = new ArrayList<TraitConstraint>();
        if (lines == null) {
            return out;
        }
        for (int i = 0; i < lines.length; i++) {
            TraitConstraint constraint =
                    HomeWire.decodeTraitConstraint(lines[i]);
            if (constraint != null) {
                out.add(constraint);
            }
        }
        return out;
    }

    private static List<Scene> buildScenes(HomeBridge b, String structureId) {
        String[] lines = b.getScenes(structureId);
        List<Scene> out = new ArrayList<Scene>();
        if (lines == null) {
            return out;
        }
        for (int i = 0; i < lines.length; i++) {
            String sceneId = HomeWire.field(HomeWire.split(lines[i]), 0);
            if (sceneId.length() == 0) {
                continue;
            }
            Scene scene = HomeWire.decodeScene(lines[i], structureId,
                    buildSceneActions(b, structureId, sceneId));
            if (scene != null) {
                out.add(scene);
            }
        }
        return out;
    }

    private static List<SceneAction> buildSceneActions(HomeBridge b,
            String structureId, String sceneId) {
        String[] lines = b.getSceneActions(structureId, sceneId);
        List<SceneAction> out = new ArrayList<SceneAction>();
        if (lines == null) {
            return out;
        }
        for (int i = 0; i < lines.length; i++) {
            SceneAction action = HomeWire.decodeSceneAction(lines[i]);
            if (action != null) {
                out.add(action);
            }
        }
        return out;
    }

    private static List<TraitReading> decodeReadings(String[] lines) {
        if (lines == null || lines.length == 0) {
            return Collections.emptyList();
        }
        List<TraitReading> out = new ArrayList<TraitReading>(lines.length);
        for (int i = 0; i < lines.length; i++) {
            TraitReading reading = HomeWire.decodeReading(lines[i]);
            if (reading != null) {
                out.add(reading);
            }
        }
        return Collections.unmodifiableList(out);
    }

    private static List<TraitWriteResult> decodeWriteResults(String[] lines,
            List<TraitWrite> writes) {
        if (writes == null || writes.isEmpty()) {
            return Collections.emptyList();
        }
        List<TraitWriteResult> out =
                new ArrayList<TraitWriteResult>(writes.size());
        for (int i = 0; i < writes.size(); i++) {
            TraitWrite w = writes.get(i);
            if (lines == null || i >= lines.length) {
                // The port answered with fewer outcomes than there were
                // writes. Reported as unknown rather than assumed applied:
                // a write silently counted as successful is how a light stays
                // on and nothing in the app ever says so.
                out.add(TraitWriteResult.failed(w, HomeError.UNKNOWN,
                        "the port did not report an outcome for this write"));
                continue;
            }
            String[] f = HomeWire.split(lines[i]);
            String errorName = HomeWire.field(f, 4);
            if (errorName.length() > 0) {
                out.add(TraitWriteResult.failed(w,
                        HomeError.forName(errorName), HomeWire.field(f, 5)));
            } else if (HomeWire.flag(f, 3)) {
                out.add(TraitWriteResult.applied(w));
            } else {
                out.add(TraitWriteResult.failed(w, HomeError.UNKNOWN,
                        "the accessory did not accept this write"));
            }
        }
        return Collections.unmodifiableList(out);
    }

    private void fireStructureChanged(final HomeStructureEvent event) {
        final HomeStructureListener[] snapshot;
        synchronized (this) {
            if (structureListeners.isEmpty()) {
                return;
            }
            snapshot = new HomeStructureListener[structureListeners.size()];
            for (int i = 0; i < snapshot.length; i++) {
                snapshot[i] = structureListeners.get(i);
            }
        }
        if (Display.isInitialized() && !Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(
                    new StructureDispatch(snapshot, event));
            return;
        }
        dispatchStructure(snapshot, event);
    }

    private static void dispatchStructure(HomeStructureListener[] listeners,
            HomeStructureEvent event) {
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].structureChanged(event);
        }
    }

    private static String[] toArray(List<String> list) {
        String[] out = new String[list.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = list.get(i);
        }
        return out;
    }

    private static <T> EdtResult<T> failed(HomeError error, String message) {
        EdtResult<T> result = new EdtResult<T>();
        result.error(error == HomeError.NOT_CONFIGURED
                ? new HomeConfigurationException(message)
                : new HomeException(error, message));
        return result;
    }

    /// Named rather than anonymous so the queued runnable carries no synthetic
    /// reference to the enclosing instance (SpotBugs
    /// `SIC_INNER_SHOULD_BE_STATIC_ANON`).
    private static final class StructureDispatch implements Runnable {

        private final HomeStructureListener[] listeners;
        private final HomeStructureEvent event;

        StructureDispatch(HomeStructureListener[] listeners,
                HomeStructureEvent event) {
            this.listeners = listeners;
            this.event = event;
        }

        @Override
        public void run() {
            dispatchStructure(listeners, event);
        }
    }

    /// Narrows a one-trait batch read back to the single reading the
    /// convenience overload promised.
    private static final class SingleReading
            implements com.codename1.util.AsyncResult<List<TraitReading>> {

        private final EdtResult<TraitReading> target;
        private final Accessory accessory;
        private final AccessoryService service;
        private final Trait trait;

        SingleReading(EdtResult<TraitReading> target, Accessory accessory,
                AccessoryService service, Trait trait) {
            this.target = target;
            this.accessory = accessory;
            this.service = service;
            this.trait = trait;
        }

        @Override
        public void onReady(List<TraitReading> readings, Throwable error) {
            if (error != null) {
                target.error(error);
                return;
            }
            if (readings == null || readings.isEmpty()) {
                // Answered with nothing rather than with a reading. Reported
                // as a failure of that trait rather than as an empty success,
                // because the caller asked for one value and there is none.
                target.complete(TraitReading.failed(accessory.getId(),
                        service.getId(), trait, HomeError.UNKNOWN,
                        "the port returned no reading for this trait"));
                return;
            }
            target.complete(readings.get(0));
        }
    }

    /// Narrows a one-write batch back to the single outcome the convenience
    /// overload promised.
    private static final class SingleWriteResult
            implements com.codename1.util.AsyncResult<List<TraitWriteResult>> {

        private final EdtResult<TraitWriteResult> target;
        private final TraitWrite write;

        SingleWriteResult(EdtResult<TraitWriteResult> target,
                TraitWrite write) {
            this.target = target;
            this.write = write;
        }

        @Override
        public void onReady(List<TraitWriteResult> results, Throwable error) {
            if (error != null) {
                target.error(error);
                return;
            }
            if (results == null || results.isEmpty()) {
                target.complete(TraitWriteResult.failed(write,
                        HomeError.UNKNOWN,
                        "the port did not report an outcome for this write"));
                return;
            }
            target.complete(results.get(0));
        }
    }

    /// Feeds a subscription's up-front read into it as its first batch, so a
    /// screen populated by a subscription has one code path rather than two.
    private static final class InitialDelivery
            implements com.codename1.util.AsyncResult<List<TraitReading>> {

        private final SubscriptionState state;

        InitialDelivery(SubscriptionState state) {
            this.state = state;
        }

        @Override
        public void onReady(List<TraitReading> readings, Throwable error) {
            if (error != null || readings == null || readings.isEmpty()) {
                // Nothing to seed the screen with. Deliberately silent: the
                // subscription itself is registered and live, and failing it
                // over an unavailable initial value would take the live
                // updates down with it.
                return;
            }
            state.offer(readings, true);
        }
    }

    /// The seam `com.codename1.home.commissioning.Commissioner` reaches this
    /// class through, so commissioning can live in its own package -- which it
    /// must, because the build server decides what native machinery to include
    /// by scanning for package prefixes and commissioning costs an entire
    /// extra Xcode target.
    private final class Gateway implements CommissioningGateway {

        @Override
        public int getCommissioningStyle() {
            HomeBridge b = bridge();
            if (b == null) {
                return com.codename1.home.commissioning.CommissioningStyle.NONE
                        .ordinal();
            }
            return b.getCommissioningStyle();
        }

        @Override
        public AsyncResource<CommissioningResult> commission(
                CommissioningRequest request) {
            HomeBridge b = bridge();
            if (b == null) {
                return SmartHome.<CommissioningResult>failed(
                        HomeError.COMMISSIONING_UNAVAILABLE,
                        "this platform cannot add smart-home accessories");
            }
            int id = nextRequestId();
            EdtResult<CommissioningResult> result =
                    pendingCommissioning.open(id);
            b.commission(id, orEmpty(request.getRawSetupPayload()),
                    orEmpty(request.getStructureId()),
                    orEmpty(request.getRoomId()),
                    orEmpty(request.getSuggestedName()),
                    request.getTimeoutMillis());
            return result;
        }

        @Override
        public boolean openEcosystemApp() {
            return SmartHome.this.openEcosystemApp();
        }

        private String orEmpty(String value) {
            return value == null ? "" : value;
        }
    }
}
