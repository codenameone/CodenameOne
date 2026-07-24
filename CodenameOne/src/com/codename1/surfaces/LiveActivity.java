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
package com.codename1.surfaces;

import com.codename1.surfaces.spi.SurfaceBridge;

import java.util.LinkedHashMap;
import java.util.Map;

/// A running live activity: an ongoing-state surface (delivery, timer, ride, score) presented on
/// the iOS lock screen and Dynamic Island, as an ongoing Android notification, or as a floating
/// pill window on desktop. Start it with a descriptor and an initial state, then push fresh state
/// maps as the situation evolves -- updates ship only the state, the layout is re-interpolated on
/// the surface:
///
/// ```java
/// LiveActivity delivery = LiveActivity.start(descriptor, initialState);
/// ...
/// delivery.update(stateMap("Arriving now", eta, 1.0f));
/// delivery.end(null);
/// ```
///
/// On platforms without live activity support `start(...)` returns an inert handle whose methods
/// are safe no-ops ([#isActive()] returns false), so app code needs no platform checks.
public final class LiveActivity {
    private final String id;
    private boolean active;

    private LiveActivity(String id) {
        this.id = id;
        this.active = id != null;
    }

    /// Returns true when this platform can present live activities.
    ///
    /// #### Returns
    ///
    /// true when live activities are supported
    public static boolean isSupported() {
        SurfaceBridge b = Surfaces.bridgeInternal();
        return b != null && b.isLiveActivitySupported();
    }

    /// Starts a live activity. On unsupported platforms (or when the platform refuses, e.g. the
    /// user disabled live activities) this returns an inert handle rather than throwing.
    ///
    /// #### Parameters
    ///
    /// - `descriptor`: the activity layout and regions
    /// - `initialState`: the initial state map, may be null
    ///
    /// #### Returns
    ///
    /// a handle to the running activity; check [#isActive()] to know whether it is live
    public static LiveActivity start(LiveActivityDescriptor descriptor,
            Map<String, Object> initialState) {
        SurfaceBridge b = Surfaces.bridgeInternal();
        if (b == null || !b.isLiveActivitySupported()) {
            return new LiveActivity(null);
        }
        Map<String, byte[]> images = new LinkedHashMap<String, byte[]>();
        String json = SurfaceSerializer.serializeLiveActivity(descriptor, initialState, images);
        return new LiveActivity(b.startLiveActivity(json, images));
    }

    /// Push-framework entry point that updates an already-running native activity by id.
    public static void updateRemote(String id, String stateJson) {
        SurfaceBridge b = Surfaces.bridgeInternal();
        if (b != null && id != null && stateJson != null) {
            b.updateLiveActivity(id, stateJson);
        }
    }

    /// Push-framework entry point that ends an already-running native activity by id.
    /// `finalStateJson` may be null to keep the last published state, matching the
    /// `SurfaceBridge` contract.
    public static void endRemote(String id, String finalStateJson, boolean dismissImmediately) {
        SurfaceBridge b = Surfaces.bridgeInternal();
        if (b != null && id != null) {
            b.endLiveActivity(id, finalStateJson, dismissImmediately);
        }
    }

    /// Pushes a fresh state map to the running activity. A no-op on an inert or ended handle.
    ///
    /// #### Parameters
    ///
    /// - `state`: the new state map
    public void update(Map<String, Object> state) {
        if (!active) {
            return;
        }
        SurfaceBridge b = Surfaces.bridgeInternal();
        if (b != null) {
            b.updateLiveActivity(id, SurfaceSerializer.serializeState(state));
        }
    }

    /// Ends the activity, optionally showing a final state before the platform dismisses the
    /// surface. A no-op on an inert or already-ended handle.
    ///
    /// #### Parameters
    ///
    /// - `finalState`: the final state to show, or null to keep the last state
    public void end(Map<String, Object> finalState) {
        end(finalState, false);
    }

    /// Ends the activity.
    ///
    /// #### Parameters
    ///
    /// - `finalState`: the final state to show, or null to keep the last state
    /// - `dismissImmediately`: true to remove the surface right away instead of letting the
    ///   platform linger on the final state
    public void end(Map<String, Object> finalState, boolean dismissImmediately) {
        if (!active) {
            return;
        }
        active = false;
        SurfaceBridge b = Surfaces.bridgeInternal();
        if (b != null) {
            b.endLiveActivity(id,
                    finalState == null ? null : SurfaceSerializer.serializeState(finalState),
                    dismissImmediately);
        }
    }

    /// Returns true while the activity is running (false for inert handles and after `end`).
    public boolean isActive() {
        return active;
    }

    /// Returns the platform id of the activity, or null for inert handles. Action events from
    /// this activity carry the descriptor's activity type as their source.
    public String getId() {
        return id;
    }
}
