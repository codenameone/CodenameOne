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

/// The seam between the Android port and Google's smart-home APIs.
///
/// #### Why the port does not call them directly
///
/// Two libraries live behind this, and the port can reference neither. The
/// Google Home APIs are Kotlin with `suspend` functions; Play services' Matter
/// commissioning is Java but lives in `com.google.android.gms.home`, which is
/// not on the classpath the port compiles against -- a fixed, old
/// `android.jar` with no Play services, no AndroidX, no Kotlin and no
/// coroutines.
///
/// It does not need to. The port ships to app builds as **source**, which the
/// app's own Gradle compiles at a modern `compileSdkVersion`, Kotlin included.
/// So the build server drops an implementation of this interface into the app
/// and registers it through [AndroidSmartHomeSupport], exactly as the Health
/// Connect and Android Auto glue are injected.
///
/// Every method here therefore speaks only primitives, `String` and arrays of
/// them. No Play services type, no Kotlin type and no coroutine escapes into
/// the port, and the records use the same tab-delimited format the iOS bridge
/// and `com.codename1.impl.home.HomeWire` already agree on.
///
/// #### What "no delegate" means
///
/// When nothing is registered -- an app that never referenced
/// `com.codename1.home`, or a build predating the generator --
/// [AndroidSmartHomeSupport#getDelegate()] answers null and the smart-home API
/// reports itself unsupported. That is the default, and it is the state every
/// existing app is in.
///
/// A delegate that IS registered may still report
/// `HomeAvailability.COMMISSIONING_ONLY`, which is the ordinary Android
/// answer: Play services can commission a Matter accessory with no setup at
/// all, while reading or controlling one needs the Google Home APIs and the
/// Google Cloud project only the app's developer can create.
public interface SmartHomeDelegate {

    /// Receives the result of an asynchronous call. The delegate invokes
    /// exactly one of these, on an unspecified thread; the port marshals to
    /// the EDT.
    interface Callback {

        /// The call succeeded, carrying whatever records it produces --
        /// empty for an operation that returns nothing.
        ///
        /// #### Parameters
        ///
        /// - `payload`: the encoded records, never `null`
        void onSuccess(String[] payload);

        /// The call failed.
        ///
        /// #### Parameters
        ///
        /// - `errorName`: the `com.codename1.home.HomeError` constant name.
        ///   The name and not the ordinal, so a delegate built against a
        ///   different version of that enum cannot map a failure onto the
        ///   wrong one; an unrecognized name degrades to `UNKNOWN` with the
        ///   message intact.
        ///
        /// - `message`: the platform's own text, or `null`
        void onError(String errorName, String message);
    }

    /// Where the delegate pushes changes and graph events it was not asked
    /// for. Registered once by the port through
    /// [#setEventSink(SmartHomeDelegate.EventSink)].
    interface EventSink {

        /// Watched traits changed.
        ///
        /// #### Parameters
        ///
        /// - `subscriptionId`: the subscription these belong to
        ///
        /// - `records`: the encoded readings
        void changes(String subscriptionId, String[] records);

        /// The delegate lost its change stream, so the values a subscription
        /// is tracking can no longer be trusted.
        ///
        /// #### Parameters
        ///
        /// - `subscriptionId`: the subscription to mark
        void resyncRequired(String subscriptionId);

        /// The home graph moved.
        ///
        /// #### Parameters
        ///
        /// - `changeKindOrdinal`: the
        ///   `com.codename1.home.StructureChangeKind` ordinal
        ///
        /// - `structureId`: the home affected, or `null`
        ///
        /// - `accessoryId`: the accessory affected, or `null`
        void structureChanged(int changeKindOrdinal, String structureId,
                String accessoryId);
    }

    /// Registers where unsolicited events go. Called once during startup.
    ///
    /// #### Parameters
    ///
    /// - `sink`: the port's receiver
    void setEventSink(EventSink sink);

    /// The `com.codename1.home.HomeAvailability` ordinal for the current
    /// state.
    ///
    /// #### Returns
    ///
    /// the availability ordinal
    int availability();

    /// The `com.codename1.home.HomeAuthorizationStatus` ordinal.
    ///
    /// #### Returns
    ///
    /// the status ordinal
    int authorizationStatus();

    /// What the build is missing that this backend needs -- a Google Cloud
    /// project id, an OAuth client -- one sentence per entry, each naming the
    /// build hint that supplies it. Empty when nothing is missing.
    ///
    /// #### Returns
    ///
    /// the problems, never `null`
    String[] configurationProblems();

    /// Connects and loads the graph.
    ///
    /// #### Parameters
    ///
    /// - `callback`: where the outcome goes
    void start(Callback callback);

    /// Disconnects and releases every registration. Idempotent.
    void stop();

    /// Prompts the user for access, including the Google Home structure
    /// grant.
    ///
    /// #### Parameters
    ///
    /// - `callback`: where the outcome goes
    void requestAuthorization(Callback callback);

    /// Opens this app's page in the system settings.
    ///
    /// #### Returns
    ///
    /// `true` when something was opened
    boolean openHomeSettings();

    /// Opens the Google Home app.
    ///
    /// #### Returns
    ///
    /// `true` when the app was opened; `false` when it is not installed
    boolean openEcosystemApp();

    /// Opens the Play listing for Google Play services, the recovery from a
    /// missing or outdated provider.
    ///
    /// #### Returns
    ///
    /// `true` when something was opened
    boolean openProviderSetup();

    /// The homes, one encoded record per entry.
    ///
    /// Synchronous and must not block: the delegate caches the graph and this
    /// reads the cache, because the SPI's graph getters are called from the
    /// EDT.
    ///
    /// #### Returns
    ///
    /// the encoded homes, never `null`
    String[] structures();

    /// The rooms of one home.
    ///
    /// #### Parameters
    ///
    /// - `structureId`: the home
    ///
    /// #### Returns
    ///
    /// the encoded rooms, never `null`
    String[] rooms(String structureId);

    /// The accessories of one home.
    ///
    /// #### Parameters
    ///
    /// - `structureId`: the home
    ///
    /// #### Returns
    ///
    /// the encoded accessories, never `null`
    String[] accessories(String structureId);

    /// The services of one accessory.
    ///
    /// #### Parameters
    ///
    /// - `accessoryId`: the accessory
    ///
    /// #### Returns
    ///
    /// the encoded services, never `null`
    String[] services(String accessoryId);

    /// The traits of one service.
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
    String[] traits(String accessoryId, String serviceId);

    /// Reloads the graph.
    ///
    /// #### Parameters
    ///
    /// - `callback`: where the outcome goes
    void refresh(Callback callback);

    /// Reads traits. The three arrays are positionally aligned.
    ///
    /// #### Parameters
    ///
    /// - `accessoryIds`: the accessories to read
    ///
    /// - `serviceIds`: the services on them
    ///
    /// - `traitIds`: the traits to read
    ///
    /// - `allowCached`: whether a cached value is acceptable
    ///
    /// - `callback`: receives one encoded reading per requested trait
    void readTraits(String[] accessoryIds, String[] serviceIds,
            String[] traitIds, boolean allowCached, Callback callback);

    /// Writes traits. All the arrays are positionally aligned.
    ///
    /// #### Parameters
    ///
    /// - `accessoryIds`: the accessories to write
    ///
    /// - `serviceIds`: the services on them
    ///
    /// - `traitIds`: the traits to set
    ///
    /// - `kinds`: each value's `com.codename1.home.TraitValueKind` ordinal
    ///
    /// - `numericValues`: each value's numeric component
    ///
    /// - `stringValues`: each value's text component
    ///
    /// - `unitWireIds`: each value's `com.codename1.home.TraitUnit` wire id
    ///
    /// - `authorizationData`: the door-lock PIN for each write, empty where
    ///   none. Per write rather than per batch, because a batch can hold two
    ///   locks with different PINs. Must not be logged.
    ///
    /// - `callback`: receives one encoded outcome per write
    void writeTraits(String[] accessoryIds, String[] serviceIds,
            String[] traitIds, int[] kinds, double[] numericValues,
            String[] stringValues, int[] unitWireIds,
            String[] authorizationData, Callback callback);

    /// Whether this backend pushes trait changes without being asked.
    ///
    /// #### Returns
    ///
    /// `true` when changes arrive unsolicited
    boolean isPushDelivery();

    /// Starts watching traits.
    ///
    /// #### Parameters
    ///
    /// - `subscriptionId`: the identifier to tag deliveries with
    ///
    /// - `accessoryIds`: the accessories to watch
    ///
    /// - `serviceIds`: the services on them
    ///
    /// - `traitIds`: the traits to watch
    ///
    /// - `callback`: where registration failures go
    void subscribe(String subscriptionId, String[] accessoryIds,
            String[] serviceIds, String[] traitIds, Callback callback);

    /// Stops watching. Idempotent, and silent about an unknown identifier.
    ///
    /// #### Parameters
    ///
    /// - `subscriptionId`: the subscription to end
    void unsubscribe(String subscriptionId);

    /// Hands over changes gathered since the last drain, through the event
    /// sink, then answers with how many were delivered as a single record.
    ///
    /// #### Parameters
    ///
    /// - `callback`: receives one record holding the delivered count
    void drainChanges(Callback callback);

    /// The scenes of one home.
    ///
    /// #### Parameters
    ///
    /// - `structureId`: the home
    ///
    /// #### Returns
    ///
    /// the encoded scenes, never `null`
    String[] scenes(String structureId);

    /// What one scene does.
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
    String[] sceneActions(String structureId, String sceneId);

    /// Runs a scene.
    ///
    /// #### Parameters
    ///
    /// - `structureId`: the home
    ///
    /// - `sceneId`: the scene to run
    ///
    /// - `callback`: receives the affected scene's record
    void executeScene(String structureId, String sceneId, Callback callback);

    /// Creates a scene. The value arrays are encoded as in
    /// [#writeTraits(java.lang.String[], java.lang.String[],
    /// java.lang.String[], int[], double[], java.lang.String[], int[],
    /// java.lang.String, SmartHomeDelegate.Callback)].
    ///
    /// #### Parameters
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
    /// - `kinds`: each value's kind ordinal
    ///
    /// - `numericValues`: each value's numeric component
    ///
    /// - `stringValues`: each value's text component
    ///
    /// - `unitWireIds`: each value's unit wire id
    ///
    /// - `callback`: receives the new scene's record
    void createScene(String structureId, String name, String[] accessoryIds,
            String[] serviceIds, String[] traitIds, int[] kinds,
            double[] numericValues, String[] stringValues, int[] unitWireIds,
            Callback callback);

    /// Deletes a scene.
    ///
    /// #### Parameters
    ///
    /// - `structureId`: the home
    ///
    /// - `sceneId`: the scene to delete
    ///
    /// - `callback`: where the outcome goes
    void deleteScene(String structureId, String sceneId, Callback callback);

    /// The `com.codename1.home.commissioning.CommissioningStyle` ordinal.
    ///
    /// #### Returns
    ///
    /// the style ordinal
    int commissioningStyle();

    /// Runs the Play services add-device flow.
    ///
    /// #### Parameters
    ///
    /// - `setupPayload`: the Matter onboarding payload, or empty to let the
    ///   platform's UI scan one
    ///
    /// - `structureId`: the home to add it to, or empty
    ///
    /// - `roomId`: the room, or empty
    ///
    /// - `suggestedName`: a name to offer, or empty
    ///
    /// - `timeoutMillis`: the limit, or zero for the platform default
    ///
    /// - `callback`: receives one record of
    ///   `accessoryId \t accessoryName \t structureId \t
    ///   commissionedToThisApp`
    void commission(String setupPayload, String structureId, String roomId,
            String suggestedName, int timeoutMillis, Callback callback);

    /// Asks an accessory to identify itself.
    ///
    /// #### Parameters
    ///
    /// - `accessoryId`: the accessory
    ///
    /// - `callback`: where the outcome goes
    void identify(String accessoryId, Callback callback);
}
