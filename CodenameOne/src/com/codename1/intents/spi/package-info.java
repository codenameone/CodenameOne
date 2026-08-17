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

/// The platform seam of the app intents framework. Ports implement
/// `IntentBridge`; applications use `com.codename1.intents` and never touch this
/// package.
///
/// #### Everything crosses as data
///
/// The bridge trades JSON strings and named PNG blobs, never live objects. Three
/// forces push it that way and they all point the same direction: the peer is
/// Swift or Kotlin rather than Java; an invocation can arrive in a process that
/// was started only to answer it, with no UI and no application state warmed up;
/// and keeping the wire format to strings is what would let intents be hosted in
/// a separate process later without changing a line of Java. It is the same rule
/// the `com.codename1.surfaces` package states, for the same reasons.
///
/// #### The two directions
///
/// **Outward**, the core calls the bridge: publishing the intent catalogue at
/// startup, donating a completed action, writing entities into the device search
/// index, and handing back the result of an invocation.
///
/// **Inward**, the port calls `com.codename1.intents.Intents.dispatchInvocation`
/// after decoding its own platform payload. The port does not marshal threads,
/// enforce the deadline, or handle a cold start; the core owns all three so every
/// platform behaves identically. In particular the core guarantees
/// `completeInvocation` fires exactly once per token -- a handler that finishes
/// after the deadline has already been reported as failed and its late answer is
/// dropped, because the iOS side of this boundary crashes when a continuation is
/// resumed twice.
///
/// #### Returning null is a complete implementation
///
/// `CodenameOneImplementation.getIntentBridge()` returns null by default and a
/// port that cannot support intents leaves it that way. The public API then
/// degrades to inert no-ops -- except `Intents.invoke`, which keeps working,
/// because in-process dispatch runs through build-time generated code rather
/// than through this bridge.
package com.codename1.intents.spi;
