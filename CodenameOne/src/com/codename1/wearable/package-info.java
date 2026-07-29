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

/// Talking between a phone app and its watch app.
///
/// A watch app and a phone app are two apps on two devices with two sandboxes. Nothing is shared
/// between them automatically: `Storage`, `Preferences` and the SQLite database are per-device, and
/// there is no cross-device container. This package is the channel between them, and it is the same
/// channel on Apple Watch (`WCSession`) and Wear OS (the Wearable Data Layer).
///
/// #### Three ways to move information, and how to choose
///
/// The platforms offer three transports because they answer three different questions. Picking the
/// wrong one is the usual source of "my watch app didn't get the update":
///
/// | You need | Use | Delivered |
/// |---|---|---|
/// | An answer, now, while both apps are awake | [WearableConnection#sendMessage(WearableMessage,WearableReplyHandler)] | Immediately, or it fails |
/// | The peer to end up with the latest state, whenever it next looks | [WearableConnection#putData(WearableMessage)] | Eventually, survives sleep and relaunch |
/// | To move a file or a large blob | [WearableConnection#transferFile(String,String,byte[])] | In the background, possibly much later |
///
/// A message is a phone call: it only works if someone picks up ([WearableConnection#isReachable()]
/// is true). Data is a shared noticeboard: you pin the current value at a path and the peer reads it
/// whenever it wakes, so it is what you want for "the watch should show my latest step count". Data
/// replaces the value at a path rather than queueing, so do not use it as a message queue.
///
/// #### The dead-process rule
///
/// The peer app may not be running when something arrives for it. The platform starts it, which
/// means your listener may not be registered yet. Callbacks that arrive before you register are
/// therefore queued and replayed to your first listener, on the EDT. Register listeners from your
/// `init()` rather than from a form, or you will race the platform and lose the callback that
/// launched you.
///
/// #### Degrades instead of failing
///
/// On a device with no counterpart -- a phone with no paired watch, a desktop build, the
/// simulator with no watch window open -- there is no bridge, [WearableConnection#isSupported()]
/// returns false and every call is an inert no-op. Application code needs no platform conditionals.
///
/// Merely referencing this package makes the build wire the native plumbing (`WatchConnectivity` on
/// Apple, the `play-services-wearable` dependency and a `WearableListenerService` on Android); apps
/// that never use it pay nothing. See the "Wearables" chapter of the developer guide.
package com.codename1.wearable;
