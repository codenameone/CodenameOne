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

import com.codename1.io.Log;
import com.codename1.surfaces.spi.SurfaceBridge;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The static entry point for external surfaces: home-screen widgets and live activities -- the
/// two faces of one concept, a live source of information that resides outside your app. Declare
/// your widget kinds and register an action handler in `init()`, then publish content whenever
/// your data changes:
///
/// ```java
/// Surfaces.registerWidgetKind(new WidgetKind("delivery_status")
///         .setDisplayName("Delivery").setDescription("Track your order"));
/// Surfaces.setActionHandler(evt -> showOrder(evt.getParams()));
/// ...
/// Surfaces.publish("delivery_status", new WidgetTimeline()
///         .setContent(layout).addEntry(new Date(), state));
/// ```
///
/// #### How surfaces render
///
/// Surfaces render while your app process may be dead: the published timeline is serialized (a
/// JSON descriptor plus PNG blobs) and persisted where the platform renderer can reach it -- the
/// iOS widget extension, the Android widget provider or a desktop surface window. Layouts embed
/// `${key}` placeholders resolved from each timeline entry's state map, and `SurfaceDynamicText`
/// countdowns tick natively on the OS clock with no app wakeups. To refresh content periodically
/// implement `com.codename1.background.BackgroundFetch` and re-publish there.
///
/// Widget kinds must also be declared at build time in the project's `surfaces.json` resource --
/// the platform widget galleries are compiled into the native app. See the package documentation.
///
/// #### Zero cost when unused
///
/// Merely referencing this package makes the build inject the native plumbing (the WidgetKit
/// extension and app group on iOS, the widget receivers on Android). Apps that never touch
/// `com.codename1.surfaces` get none of it. On the simulator the Widgets preview window renders
/// published surfaces; on unsupported ports the API is an inert no-op.
public final class Surfaces {
    private static SurfaceBridge bridge;
    private static boolean bridgeOverridden;
    private static SurfaceActionHandler actionHandler;
    private static final List<SurfaceActionEvent> pendingActions =
            new ArrayList<SurfaceActionEvent>();
    private static final List<WidgetKind> registeredKinds = new ArrayList<WidgetKind>();

    private Surfaces() {
    }

    /// Returns true when this platform can render home-screen (or desktop) widgets.
    ///
    /// #### Returns
    ///
    /// true when widgets are supported
    public static boolean areWidgetsSupported() {
        SurfaceBridge b = bridgeInternal();
        return b != null && b.areWidgetsSupported();
    }

    /// Declares a widget kind at runtime. Call once per kind, typically from `init()`. The id must
    /// match a kind declared in the project's `surfaces.json` build-time manifest; a mismatch logs
    /// a prominent warning on supporting platforms.
    ///
    /// #### Parameters
    ///
    /// - `kind`: the kind declaration
    public static void registerWidgetKind(WidgetKind kind) {
        if (kind == null) {
            return;
        }
        for (WidgetKind k : registeredKinds) {
            if (k.getId().equals(kind.getId())) {
                registeredKinds.remove(k);
                break;
            }
        }
        registeredKinds.add(kind);
        SurfaceBridge b = bridgeInternal();
        if (b != null) {
            b.registerWidgetKind(SurfaceSerializer.serializeKind(kind));
        }
    }

    /// Returns the widget kinds registered so far.
    public static List<WidgetKind> getRegisteredKinds() {
        return new ArrayList<WidgetKind>(registeredKinds);
    }

    /// Publishes a widget kind's content, atomically replacing any previously published timeline
    /// and asking the platform to re-render the kind's widget instances. A no-op on platforms
    /// without widget support.
    ///
    /// #### Threading
    ///
    /// Callable from any thread -- including
    /// `com.codename1.background.BackgroundFetch#performBackgroundFetch(long, com.codename1.util.Callback)`
    /// callbacks while the app UI is not running (on Android the fetch runs in a background
    /// service with no Activity at all). Publishing is data-only: the timeline is serialized,
    /// persisted where the platform renderer can reach it and the renderer is poked
    /// asynchronously; no step blocks on the EDT or the platform UI thread. Implementing
    /// background fetch and re-publishing there is the intended way to keep widgets fresh; see
    /// the `com.codename1.surfaces.spi` package documentation for the per-platform background
    /// update story.
    ///
    /// #### Parameters
    ///
    /// - `kindId`: the widget kind id
    /// - `timeline`: the content to publish
    public static void publish(String kindId, WidgetTimeline timeline) {
        SurfaceBridge b = bridgeInternal();
        if (b == null || !b.areWidgetsSupported()) {
            return;
        }
        Map<String, byte[]> images = new LinkedHashMap<String, byte[]>();
        String json = SurfaceSerializer.serializeTimeline(kindId, timeline, images);
        b.publishWidgetTimeline(kindId, json, images);
    }

    /// Asks the platform to re-render widgets from their already-published timelines.
    ///
    /// #### Parameters
    ///
    /// - `kindId`: the kind to reload, or null for all kinds
    public static void reloadWidgets(String kindId) {
        SurfaceBridge b = bridgeInternal();
        if (b != null) {
            b.reloadWidgets(kindId);
        }
    }

    /// Returns the number of widget instances of a kind the user placed on the platform surface,
    /// or 0 when none exist or the platform cannot tell. Useful to skip publishing work when no
    /// widget is installed.
    ///
    /// #### Parameters
    ///
    /// - `kindId`: the widget kind id
    ///
    /// #### Returns
    ///
    /// the installed instance count, or 0
    public static int getInstalledWidgetCount(String kindId) {
        SurfaceBridge b = bridgeInternal();
        return b == null ? 0 : b.getInstalledWidgetCount(kindId);
    }

    /// Registers the single handler receiving surface action events on the EDT. Registration
    /// flushes any actions queued before it (e.g. the tap that cold-started the app), in arrival
    /// order, with their cold-start flag set.
    ///
    /// #### Parameters
    ///
    /// - `handler`: the handler, or null to clear
    public static void setActionHandler(SurfaceActionHandler handler) {
        actionHandler = handler;
        if (handler == null) {
            return;
        }
        List<SurfaceActionEvent> queued;
        synchronized (pendingActions) {
            if (pendingActions.isEmpty()) {
                return;
            }
            queued = new ArrayList<SurfaceActionEvent>(pendingActions);
            pendingActions.clear();
        }
        for (SurfaceActionEvent evt : queued) {
            deliver(evt);
        }
    }

    // --- framework/port entry points -----------------------------------------

    /// Framework/port entry point: delivers a surface action to the app. Ports call this after
    /// decoding their platform payload (deep link, intent extras, window click). Handles EDT
    /// marshaling; when no handler is registered yet the event is queued and flagged cold start.
    ///
    /// #### Parameters
    ///
    /// - `source`: the widget kind id or live activity type
    /// - `actionId`: the action id of the tapped node
    /// - `params`: the action parameters, may be null
    public static void dispatchAction(String source, String actionId, Map<String, Object> params) {
        SurfaceActionEvent evt = new SurfaceActionEvent(source, actionId, params);
        if (actionHandler == null) {
            evt.setColdStart(true);
            synchronized (pendingActions) {
                pendingActions.add(evt);
            }
            return;
        }
        deliver(evt);
    }

    /// Framework/port/test entry point: overrides the bridge resolved from the platform port.
    /// Passing null restores platform resolution.
    ///
    /// #### Parameters
    ///
    /// - `b`: the bridge, or null to resolve from the platform again
    public static void setBridge(SurfaceBridge b) {
        bridge = b;
        bridgeOverridden = b != null;
    }

    static SurfaceBridge bridgeInternal() {
        if (bridgeOverridden) {
            return bridge;
        }
        if (!Display.isInitialized()) {
            return null;
        }
        try {
            return Display.getInstance().getSurfaceBridge();
        } catch (Throwable t) {
            Log.e(t);
            return null;
        }
    }

    /// Test seam: clears the bridge override, handler, queued actions and registered kinds.
    static void reset() {
        bridge = null;
        bridgeOverridden = false;
        actionHandler = null;
        synchronized (pendingActions) {
            pendingActions.clear();
        }
        registeredKinds.clear();
    }

    private static void deliver(final SurfaceActionEvent evt) {
        final SurfaceActionHandler h = actionHandler;
        if (h == null) {
            return;
        }
        if (Display.isInitialized()) {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    h.onSurfaceAction(evt);
                }
            });
        } else {
            h.onSurfaceAction(evt);
        }
    }
}
