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
package com.codename1.surfaces.spi;

import java.util.Map;

/// The platform seam of the external surfaces framework, implemented by ports and returned from
/// `CodenameOneImplementation.getSurfaceBridge()` (null on unsupported ports, making the whole
/// public API an inert no-op).
///
/// Everything crosses this boundary as data -- JSON strings produced by the core serializer plus
/// named PNG blobs -- never as live model objects, because surfaces render while the app process
/// may be dead. Implementations MUST persist published payloads (shared container, files dir,
/// preferences) so the platform renderer can re-render them without the app running.
///
/// Action events travel in the opposite direction: the port decodes its platform payload (deep
/// link, intent extras, window click) and calls `com.codename1.surfaces.Surfaces.dispatchAction`,
/// which handles EDT marshaling and cold-start queuing.
public interface SurfaceBridge {
    /// Returns true when this port can render home-screen (or desktop) widgets.
    boolean areWidgetsSupported();

    /// Returns true when this port can present live activities (ongoing-state surfaces).
    boolean isLiveActivitySupported();

    /// Notifies the port of a runtime widget kind registration, letting it validate the kind
    /// against what was compiled into the native app and prepare storage.
    ///
    /// #### Parameters
    ///
    /// - `kindJson`: the serialized kind declaration
    void registerWidgetKind(String kindJson);

    /// Atomically replaces the persisted timeline of a widget kind and asks the platform to
    /// re-render its widget instances.
    ///
    /// #### Parameters
    ///
    /// - `kindId`: the widget kind id
    /// - `timelineJson`: the serialized timeline (layouts, entries, image names)
    /// - `images`: PNG blobs keyed by registered name; may be empty, never null
    void publishWidgetTimeline(String kindId, String timelineJson, Map<String, byte[]> images);

    /// Asks the platform to re-render widget instances from their persisted timelines.
    ///
    /// #### Parameters
    ///
    /// - `kindId`: the widget kind to reload, or null for all kinds
    void reloadWidgets(String kindId);

    /// Returns the number of widget instances of a kind the user placed on the platform surface,
    /// or 0 when none exist or the platform cannot tell.
    ///
    /// #### Parameters
    ///
    /// - `kindId`: the widget kind id
    int getInstalledWidgetCount(String kindId);

    /// Starts a live activity.
    ///
    /// #### Parameters
    ///
    /// - `descriptorJson`: the serialized descriptor including the initial state
    /// - `images`: PNG blobs keyed by registered name; may be empty, never null
    ///
    /// #### Returns
    ///
    /// a platform id for the running activity, or null when starting failed
    String startLiveActivity(String descriptorJson, Map<String, byte[]> images);

    /// Updates a running live activity with a fresh state map; the platform re-interpolates the
    /// existing layout locally.
    ///
    /// #### Parameters
    ///
    /// - `activityId`: the id returned from `startLiveActivity`
    /// - `stateJson`: the serialized state map
    void updateLiveActivity(String activityId, String stateJson);

    /// Ends a live activity.
    ///
    /// #### Parameters
    ///
    /// - `activityId`: the id returned from `startLiveActivity`
    /// - `finalStateJson`: an optional final state shown before dismissal, or null
    /// - `dismissImmediately`: true to remove the surface right away instead of letting the
    ///   platform linger on the final state
    void endLiveActivity(String activityId, String finalStateJson, boolean dismissImmediately);
}
