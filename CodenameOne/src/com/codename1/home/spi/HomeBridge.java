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
package com.codename1.home.spi;

/// Internal service-provider interface implemented by each platform port to
/// back `com.codename1.home.SmartHome` with HomeKit, the Google Home APIs, or
/// a local simulated home.
///
/// Application code never touches this; see the package documentation.
///
/// #### Primitives and flat strings only
///
/// Nothing here takes or returns an object from `com.codename1.home`. The
/// graph crosses as arrays of tab-delimited strings and values cross as
/// parallel primitive arrays, so an Objective-C implementation never has to
/// construct a Java object -- which under ParparVM means no allocation, no
/// class lookup, and no question about which thread built it. The same
/// discipline `com.codename1.wearable.spi.WearableBridge` follows, for the
/// same reasons.
///
/// The cost is that the encoding is a contract, written out below and shared
/// with `com.codename1.impl.home.HomeWire`, which does the decoding.
///
/// #### Identifiers
///
/// Three separate opaque strings, never an index and never a composite the
/// other side has to parse:
///
/// - **structureId** -- unique within the bridge. HomeKit:
///   `HMHome.uniqueIdentifier`. Google: the structure id.
/// - **accessoryId** -- unique within the bridge, **not merely within its
///   structure**, so a read or a write needs only two of the three. HomeKit:
///   `HMAccessory.uniqueIdentifier`. Matter: the fabric device id.
/// - **serviceId** -- unique within its accessory. HomeKit:
///   `HMService.uniqueIdentifier`. Matter: the endpoint number as decimal.
///
/// A **traitId** is always the canonical token from
/// `com.codename1.home.Trait#getId()`. A port maps it to its own platform
/// identifier on its own side; no `HMCharacteristicType` string and no Matter
/// cluster id ever crosses into Java.
///
/// #### Asynchrony
///
/// Every method taking a `requestId` returns immediately and answers later
/// through the matching static on `com.codename1.home.SmartHome`. Those
/// statics accept calls from any thread and marshal onto the EDT themselves,
/// which matters here specifically because `HMHomeManagerDelegate` and
/// `HMAccessoryDelegate` callbacks arrive on the Objective-C main queue and
/// that is not the Codename One EDT.
///
/// Request ids are allocated by the framework, are positive, and are never
/// reused while in flight. Zero is reserved for unsolicited deliveries.
///
/// #### Error encoding
///
/// Wherever a method's answer can fail, the error crosses as
/// `<HomeError name>\t<platform message>`, or `null` or empty for success.
/// The **name**, never the ordinal: a port built against a different version
/// of the enum would otherwise map every error past an inserted constant onto
/// the wrong one, and a mis-mapped authorization failure is indistinguishable
/// from a mis-mapped timeout to everything downstream.
public interface HomeBridge {

    // ------------------------------------------------------------------
    // lifecycle and capability
    // ------------------------------------------------------------------

    /// Whether this bridge can do anything at all. A port that compiles the
    /// smart-home code but finds the platform missing at runtime answers
    /// `false` here rather than failing every later call.
    ///
    /// #### Returns
    ///
    /// `true` when the backend is present
    boolean isSupported();

    /// The current availability, as the ordinal of a
    /// `com.codename1.home.HomeAvailability` constant.
    ///
    /// May be called before [#start(int)] and must answer without blocking.
    ///
    /// #### Returns
    ///
    /// the availability ordinal
    int getAvailability();

    /// Which backend this is: `"homekit"`, `"google_home"`, `"matter_only"` or
    /// `"local"`.
    ///
    /// #### Returns
    ///
    /// the backend token, never `null`
    String getBackendId();

    /// Build configuration this backend needs and does not have -- a missing
    /// entitlement, a missing Google Cloud project id -- one human-readable
    /// sentence per problem, each naming the build hint that fixes it.
    ///
    /// Empty when nothing is missing. This is what
    /// `com.codename1.home.HomeConfigurationException` carries, so the text
    /// is read by a developer, not by a user.
    ///
    /// #### Returns
    ///
    /// the problems, never `null`
    String[] getConfigurationProblems();

    /// Whether accessory and structure identifiers survive an app restart.
    ///
    /// Both shipping backends answer `true`; a local or test bridge that
    /// regenerates its graph does not, and an app persisting a favourite
    /// should ask.
    ///
    /// #### Returns
    ///
    /// `true` when identifiers are stable across launches
    boolean areIdsPersistent();

    /// Connects to the backend and loads the graph. Answers through
    /// `SmartHome.deliverStarted`.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the request to answer
    void start(int requestId);

    /// Disconnects, releases every platform registration, and drops any
    /// pending request without answering it. Idempotent.
    void stop();

    // ------------------------------------------------------------------
    // authorization
    // ------------------------------------------------------------------

    /// The current authorization, as the ordinal of a
    /// `com.codename1.home.HomeAuthorizationStatus` constant.
    ///
    /// #### Returns
    ///
    /// the status ordinal
    int getAuthorizationStatus();

    /// Prompts the user for access. Answers through
    /// `SmartHome.deliverAuthorization` when the flow finishes, whatever the
    /// user chose.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the request to answer
    void requestAuthorization(int requestId);

    /// Opens the system settings page where the user can change this app's
    /// smart-home access.
    ///
    /// #### Returns
    ///
    /// `true` when something was opened
    boolean openHomeSettings();

    /// Opens the platform's ecosystem app -- Apple Home, Google Home -- so the
    /// user can set up a home or add an accessory there.
    ///
    /// #### Returns
    ///
    /// `true` when the app was opened; `false` when it is not installed
    boolean openEcosystemApp();

    /// Opens wherever the user installs or updates the backend's provider --
    /// Google Play services on Android.
    ///
    /// #### Returns
    ///
    /// `true` when something was opened
    boolean openProviderSetup();

    // ------------------------------------------------------------------
    // graph
    // ------------------------------------------------------------------

    /// The homes, one per entry:
    /// `id \t name \t primary \t owner \t sceneAuthoring`, where the three
    /// flags are `1` or `0`.
    ///
    /// Synchronous, and must not block: the bridge caches the platform's model
    /// and this reads the cache. [#refresh(int)] is what reloads it.
    ///
    /// #### Returns
    ///
    /// the encoded homes, never `null`
    String[] getStructures();

    /// The rooms of one home, one per entry: `id \t name`.
    ///
    /// #### Parameters
    ///
    /// - `structureId`: the home
    ///
    /// #### Returns
    ///
    /// the encoded rooms, never `null`
    String[] getRooms(String structureId);

    /// The zones of one home, one per entry:
    /// `id \t name \t roomId,roomId,...`.
    ///
    /// Empty on every backend but HomeKit, which is the only one with zones.
    ///
    /// #### Parameters
    ///
    /// - `structureId`: the home
    ///
    /// #### Returns
    ///
    /// the encoded zones, never `null`
    String[] getZones(String structureId);

    /// The accessories of one home, one per entry:
    /// `id \t name \t roomId \t categoryOrdinal \t manufacturer \t model \t
    /// firmware \t reachable \t bridgeAccessoryId`.
    ///
    /// `roomId` and `bridgeAccessoryId` are empty when absent; `reachable` is
    /// `1` or `0`; `categoryOrdinal` indexes
    /// `com.codename1.home.AccessoryCategory`.
    ///
    /// #### Parameters
    ///
    /// - `structureId`: the home
    ///
    /// #### Returns
    ///
    /// the encoded accessories, never `null`
    String[] getAccessories(String structureId);

    /// The services of one accessory, one per entry:
    /// `id \t name \t serviceTypeOrdinal \t primary`.
    ///
    /// `serviceTypeOrdinal` indexes `com.codename1.home.ServiceType`;
    /// `primary` is `1` or `0`.
    ///
    /// #### Parameters
    ///
    /// - `accessoryId`: the accessory
    ///
    /// #### Returns
    ///
    /// the encoded services, never `null`
    String[] getServices(String accessoryId);

    /// The traits of one service, one per entry:
    /// `traitId \t readable \t writable \t notifies \t hasRange \t min \t max
    /// \t step \t validOrdinalsCsv`.
    ///
    /// The four flags are `1` or `0`; the three numbers are decimal and are
    /// ignored when `hasRange` is `0`; `validOrdinalsCsv` is empty when the
    /// accessory did not enumerate its values.
    ///
    /// A `traitId` this build does not know is skipped by the decoder rather
    /// than failing the row, so a newer port degrades gracefully.
    ///
    /// #### Parameters
    ///
    /// - `accessoryId`: the accessory
    ///
    /// - `serviceId`: the service on it
    ///
    /// #### Returns
    ///
    /// the encoded traits, never `null`
    String[] getTraits(String accessoryId, String serviceId);

    /// Reloads the graph from the platform. Answers through
    /// `SmartHome.deliverRefreshed`; the synchronous getters above reflect the
    /// new graph once it has.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the request to answer
    void refresh(int requestId);

    // ------------------------------------------------------------------
    // reads and writes
    // ------------------------------------------------------------------

    /// The largest number of traits this backend will read in one call, or
    /// zero for no limit. The framework splits larger requests and recombines
    /// the answers.
    ///
    /// #### Returns
    ///
    /// the batch limit, or zero
    int getMaxReadBatchSize();

    /// Reads traits. Answers through `SmartHome.deliverReadings`.
    ///
    /// The three arrays are positionally aligned and of equal length.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the request to answer
    ///
    /// - `accessoryIds`: the accessories to read
    ///
    /// - `serviceIds`: the services on them
    ///
    /// - `traitIds`: the traits to read
    ///
    /// - `allowCached`: whether the platform may answer from its own cache
    void readTraits(int requestId, String[] accessoryIds, String[] serviceIds,
            String[] traitIds, boolean allowCached);

    /// The largest number of traits this backend will write in one call, or
    /// zero for no limit.
    ///
    /// #### Returns
    ///
    /// the batch limit, or zero
    int getMaxWriteBatchSize();

    /// Writes traits. Answers through `SmartHome.deliverWriteResults`.
    ///
    /// All the arrays are positionally aligned and of equal length. Each
    /// value is carried in whichever of `numericValues` or `stringValues`
    /// suits its kind: a boolean as `1` or `0`, an int and an enum ordinal as
    /// themselves, a double with its unit in `unitWireIds`, a string in
    /// `stringValues` with the numeric slot ignored.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the request to answer
    ///
    /// - `accessoryIds`: the accessories to write
    ///
    /// - `serviceIds`: the services on them
    ///
    /// - `traitIds`: the traits to set
    ///
    /// - `kinds`: the ordinal of each value's
    ///   `com.codename1.home.TraitValueKind`
    ///
    /// - `numericValues`: the numeric component of each value
    ///
    /// - `stringValues`: the text component of each value, empty where none
    ///
    /// - `unitWireIds`: `com.codename1.home.TraitUnit#getWireId()` for each
    ///   value
    ///
    /// - `authorizationData`: the credential each write needs -- a door-lock
    ///   PIN -- empty where none. Positionally aligned like every other array
    ///   rather than one value for the batch: a batch can hold two locks with
    ///   different PINs, and a single slot would silently send one lock the
    ///   other's credential. Must not be logged.
    void writeTraits(int requestId, String[] accessoryIds, String[] serviceIds,
            String[] traitIds, int[] kinds, double[] numericValues,
            String[] stringValues, int[] unitWireIds,
            String[] authorizationData);

    // ------------------------------------------------------------------
    // subscriptions
    // ------------------------------------------------------------------

    /// Whether this backend pushes trait changes without being asked.
    ///
    /// `true` only where the platform genuinely delivers while the app runs.
    /// A backend that answers `false` must still accept [#subscribe] and
    /// gather changes for [#drainChanges(int)].
    ///
    /// #### Returns
    ///
    /// `true` when changes arrive unsolicited
    boolean isPushDelivery();

    /// Starts watching traits. Changes arrive through
    /// `SmartHome.deliverChanges` carrying the same `subscriptionId`.
    ///
    /// Coalescing is **not** the bridge's job: the framework applies the
    /// caller's window before anything reaches the EDT, so a port should
    /// deliver what the platform gives it.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the request to answer through
    ///   `SmartHome.deliverSubscribed`
    ///
    /// - `subscriptionId`: the identifier to tag deliveries with
    ///
    /// - `accessoryIds`: the accessories to watch
    ///
    /// - `serviceIds`: the services on them
    ///
    /// - `traitIds`: the traits to watch
    void subscribe(int requestId, String subscriptionId, String[] accessoryIds,
            String[] serviceIds, String[] traitIds);

    /// Stops watching and releases the platform registration. Idempotent, and
    /// silent about an identifier it does not know.
    ///
    /// #### Parameters
    ///
    /// - `subscriptionId`: the subscription to end
    void unsubscribe(String subscriptionId);

    /// Hands over changes gathered since the last drain, through
    /// `SmartHome.deliverChanges` for each affected subscription, then answers
    /// `SmartHome.deliverDrained`.
    ///
    /// The only way changes arrive at all where [#isPushDelivery()] is
    /// `false`.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the request to answer
    void drainChanges(int requestId);

    // ------------------------------------------------------------------
    // scenes
    // ------------------------------------------------------------------

    /// The scenes of one home, one per entry:
    /// `id \t name \t typeOrdinal \t executable`.
    ///
    /// `typeOrdinal` indexes `com.codename1.home.SceneType`; `executable` is
    /// `1` or `0`.
    ///
    /// #### Parameters
    ///
    /// - `structureId`: the home
    ///
    /// #### Returns
    ///
    /// the encoded scenes, never `null`
    String[] getScenes(String structureId);

    /// What one scene does, one action per entry:
    /// `accessoryId \t serviceId \t traitId \t kindOrdinal \t numericValue \t
    /// stringValue \t unitWireId`.
    ///
    /// Empty where the backend will run a scene but not enumerate it, which is
    /// a real answer rather than an empty scene.
    ///
    /// #### Parameters
    ///
    /// - `structureId`: the home
    ///
    /// - `sceneId`: the scene
    ///
    /// #### Returns
    ///
    /// the encoded actions, never `null`
    String[] getSceneActions(String structureId, String sceneId);

    /// Runs a scene. Answers through `SmartHome.deliverSceneResult`.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the request to answer
    ///
    /// - `structureId`: the home
    ///
    /// - `sceneId`: the scene to run
    void executeScene(int requestId, String structureId, String sceneId);

    /// Creates a scene. Answers through `SmartHome.deliverSceneResult`, whose
    /// scene id is the new scene's.
    ///
    /// The value arrays are encoded exactly as in [#writeTraits].
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the request to answer
    ///
    /// - `structureId`: the home to create it in
    ///
    /// - `name`: the scene's name
    ///
    /// - `accessoryIds`: the accessories the scene acts on
    ///
    /// - `serviceIds`: the services on them
    ///
    /// - `traitIds`: the traits to set
    ///
    /// - `kinds`: the ordinal of each value's kind
    ///
    /// - `numericValues`: the numeric component of each value
    ///
    /// - `stringValues`: the text component of each value
    ///
    /// - `unitWireIds`: the unit wire id of each value
    void createScene(int requestId, String structureId, String name,
            String[] accessoryIds, String[] serviceIds, String[] traitIds,
            int[] kinds, double[] numericValues, String[] stringValues,
            int[] unitWireIds);

    /// Deletes a scene. Answers through `SmartHome.deliverSceneResult`.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the request to answer
    ///
    /// - `structureId`: the home
    ///
    /// - `sceneId`: the scene to delete
    void deleteScene(int requestId, String structureId, String sceneId);

    // ------------------------------------------------------------------
    // commissioning
    // ------------------------------------------------------------------

    /// How this backend adds a new accessory, as the ordinal of a
    /// `com.codename1.home.commissioning.CommissioningStyle` constant.
    ///
    /// #### Returns
    ///
    /// the style ordinal
    int getCommissioningStyle();

    /// Adds a new Matter accessory. Answers through
    /// `SmartHome.deliverCommissioningResult`.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the request to answer
    ///
    /// - `setupPayload`: the Matter onboarding payload, or empty to let the
    ///   platform's own UI scan one
    ///
    /// - `structureId`: the home to add it to, or empty for the default
    ///
    /// - `roomId`: the room to add it to, or empty for none
    ///
    /// - `suggestedName`: a name to offer the user, or empty
    ///
    /// - `timeoutMillis`: how long to allow, or zero for the platform default
    void commission(int requestId, String setupPayload, String structureId,
            String roomId, String suggestedName, int timeoutMillis);

    // ------------------------------------------------------------------
    // miscellaneous
    // ------------------------------------------------------------------

    /// Asks an accessory to make itself known -- blink, beep. Answers through
    /// `SmartHome.deliverIdentifyResult`.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the request to answer
    ///
    /// - `accessoryId`: the accessory to identify
    void identify(int requestId, String accessoryId);
}
