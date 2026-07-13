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

/// The platform seam of the external surfaces framework. Application code never touches this
/// package -- ports implement `SurfaceBridge` and return it from
/// `CodenameOneImplementation.getSurfaceBridge()`.
///
/// #### Port implementer notes
///
/// - **Persist everything.** Surfaces render while the app process is dead. Published timeline
///   JSON and image blobs must be written where the platform renderer can reach them without the
///   VM: the app group container on iOS (`timeline.json` plus `<name>.png` files under
///   `cn1surfaces/<kindId>/`), the files directory on Android, storage on desktop. Writes should
///   be atomic (write-rename) because the renderer may read concurrently.
/// - **Actions flow through the framework.** Decode your platform payload (deep link, intent
///   extras, window click) and call `com.codename1.surfaces.Surfaces.dispatchAction(source,
///   actionId, params)`; it handles EDT marshaling and cold-start queuing. The canonical deep
///   link form on URL-based platforms is
///   `cn1surface://a?src=<source>&id=<actionId>&p=<url-encoded JSON params>`.
/// - **State-only updates.** `updateLiveActivity` ships just a state map; re-interpolate the
///   `${key}` placeholders of the descriptor persisted at start time and re-render locally.
/// - **Degrade honestly.** Return false from the `is...Supported` queries rather than presenting
///   a broken surface; the public API turns into a documented no-op.
///
/// #### Updating surfaces from the background
///
/// "A background process fetches data and updates the widget without the app UI running" is a
/// first-class flow: the app implements `com.codename1.background.BackgroundFetch`, registers by
/// calling `com.codename1.ui.Display.setPreferredBackgroundFetchInterval(int)` and re-publishes
/// inside `performBackgroundFetch`. To support it,
/// `com.codename1.surfaces.Surfaces.publish(String, com.codename1.surfaces.WidgetTimeline)` is
/// callable from any thread and no bridge may block on the EDT or the platform UI thread in its
/// publish path -- publishing is data-only (serialize, persist, poke the renderer
/// asynchronously). Rendering may still happen on the EDT later; that is a separate, async step.
///
/// Per platform:
///
/// - **Android**: the fetch runs in a background `IntentService` while no Activity exists; the
///   bridge resolves the service/application context, so publish works from a UI-less process.
///   The widget also pulls the app: when a provider renders an exhausted `reload=atEnd` timeline
///   (or no timeline at all) it starts the fetch service directly, throttled to once per 15
///   minutes per kind, and only when the app declares background fetch.
/// - **iOS**: the widget extension cannot wake the app arbitrarily. A `reload=atEnd` timeline
///   makes WidgetKit re-request entries from the extension, which re-reads the persisted
///   document -- so publish timelines with enough future entries to bridge the gap between
///   fetches. Refresh the data itself from `performBackgroundFetch` (add `fetch` to the
///   `ios.background_modes` build hint); the publish path is file IO plus a
///   `WidgetCenter.reloadTimelines` call, neither of which requires the UIKit main thread.
/// - **Desktop / simulator**: the app process is running, so a publish simply re-renders the
///   surface windows. The simulator additionally simulates background fetch with a timer that
///   fires `performBackgroundFetch` while the app is paused, and the Widgets preview window
///   re-renders on every publish.
package com.codename1.surfaces.spi;
