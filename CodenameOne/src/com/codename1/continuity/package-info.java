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
/// Saves what the user was doing and brings it back -- after the operating system kills the app,
/// and on the other devices that person owns.
///
/// Start at `Continuity`. The framework already knows the `com.codename1.router.Navigation` stack,
/// so an app whose screens carry `@Route` gets them restored with no code; a `StateProvider` adds
/// whatever else matters. `AppState` is the snapshot the two halves make, and it is the same value
/// that is written to storage, advertised to a nearby device and sent through a `StateRelay`.
///
/// Referencing this package is what makes the build declare the activity type and compile the
/// native continuation handling in. Its sibling `com.codename1.continuity.sync` is separate
/// because it costs an entitlement on iOS.
///
/// See the State Restoration and Continuity chapter of the developer guide for the platform
/// capability table, the build hints and the relay contract.
package com.codename1.continuity;
