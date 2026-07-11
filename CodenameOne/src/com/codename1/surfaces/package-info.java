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

/// External surfaces: home-screen widgets and live activities from one declarative API.
///
/// Both features answer the same developer question -- *how do I keep a live source of
/// information outside my app?* -- so Codename One models them as one concept. A **widget** is
/// persistent, user-placed and content-driven (weather, next meeting, delivery status on the home
/// screen). A **live activity** is transient, app-started and progress-driven (the delivery that
/// is out RIGHT NOW, a running timer, a live score on the iOS lock screen / Dynamic Island, an
/// ongoing Android notification, a floating desktop pill). They share the layout model, the
/// serialization, the state mechanism and the action model.
///
/// #### The dead-process rule
///
/// Surfaces render while your app process may not be running. Everything you hand this API is
/// therefore turned into plain data at publish time: layouts serialize to a compact JSON
/// descriptor, images are encoded to named PNG blobs, and "callbacks" are string action ids that
/// open the app and are delivered to your `SurfaceActionHandler` on the EDT (queued across a cold
/// start). Layout text embeds `${key}` placeholders resolved from a per-entry state map, so
/// content changes are cheap re-publishes of data, not layouts.
///
/// #### The layout model
///
/// A small sealed catalog of nodes every platform can render natively -- `SurfaceColumn`,
/// `SurfaceRow`, `SurfaceBox`, `SurfaceText`, `SurfaceDynamicText`, `SurfaceImage`,
/// `SurfaceProgress`, `SurfaceSpacer` -- with shared styling (padding, background, corner radius,
/// alignment, weight, size, action). `SurfaceDynamicText` is the headline feature: countdowns and
/// elapsed-time text the OS animates on its own clock, so a delivery ETA ticks every second with
/// zero app wakeups.
///
/// #### The lowest common denominator contract
///
/// Android app widgets (`RemoteViews`) are the constrained platform; the catalog is designed to
/// its floor and degrades as follows:
///
/// | Node | iOS (SwiftUI) | Android (RemoteViews) | Caveat |
/// |---|---|---|---|
/// | Column/Row | VStack/HStack | LinearLayout | weight maps to layout_weight |
/// | Box | ZStack | FrameLayout | child alignment via 9-way enum |
/// | Text | Text | TextView | Android renders light/regular/medium as regular, semibold/bold as bold |
/// | DynamicText timer | Text(date, style:) | Chronometer | native on both |
/// | DynamicText time | Text(date, style: .time) | TextClock | native on both |
/// | DynamicText date/relative | native | static text | Android refreshes only on next update |
/// | Image | Image | ImageView | named PNG blobs, content-hash dedup |
/// | Progress linear | ProgressView | ProgressBar | value 0..1 or state key |
/// | Progress circular | Gauge | falls back to linear | Android widgets lack determinate circular |
/// | Progress date interval | native animation | frozen at refresh | iOS-only nicety |
/// | Spacer | Spacer | weighted View | |
/// | corner radius | clipShape | background drawable | may render square below Android 12 |
/// | node action | Link / widgetURL | setOnClickPendingIntent | small iOS widgets honor only the root action |
///
/// Descriptors are limited to 8 nesting levels; keep payloads (JSON + images) comfortably under
/// 200kb -- the iOS widget extension runs in about 30mb of memory and Android parcels rendered
/// widgets over a 1mb binder transaction.
///
/// #### Build-time manifest: `surfaces.json`
///
/// Platform widget galleries are compiled into the native app, so widget kinds must be known at
/// build time. Keep a `surfaces.json` in your project resources (next to your icons) mirroring
/// your runtime `WidgetKind` registrations:
///
/// ```json
/// {
///   "liveActivities": true,
///   "kinds": [
///     { "id": "delivery_status", "name": "Delivery", "description": "Track your order",
///       "iosFamilies": ["systemSmall", "systemMedium"] }
///   ]
/// }
/// ```
///
/// #### Refresh and updates
///
/// This version updates surfaces from the running app: publish a `WidgetTimeline` of dated
/// entries (the OS flips entries on schedule without waking you), implement
/// `com.codename1.background.BackgroundFetch` to re-publish periodically, and push live activity
/// state with `LiveActivity.update(...)`. Push-driven updates (server-sent widget content and
/// ActivityKit push tokens) are planned; the wire format already accommodates them.
///
/// #### Zero cost when unused
///
/// Referencing this package makes the build inject the native plumbing (WidgetKit extension and
/// app group on iOS, widget receivers on Android). Apps that never touch it get none of it. On
/// unsupported ports every entry point is an inert no-op.
package com.codename1.surfaces;
