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
package com.codename1.home;

/// What moved in the home, for [HomeStructureEvent].
///
/// These say the graph snapshot you are holding is out of date. Call
/// [SmartHome#refresh()] and wait for it to complete before reading the graph
/// again: [SmartHome#getStructures()] answers from the last refresh, so on its
/// own it hands back the same stale snapshot the event just told you about.
/// They never carry trait values -- those arrive through a
/// [TraitSubscription], which is a separate mechanism with separate delivery
/// guarantees.
public enum StructureChangeKind {

    /// A home was added or removed, or the set of homes the user has granted
    /// access to changed. Everything you were holding is suspect.
    STRUCTURES_CHANGED,

    /// An accessory joined a home.
    ACCESSORY_ADDED,

    /// An accessory left a home.
    ///
    /// Anything you had persisted about it -- a favourite, a widget -- now
    /// points at nothing, and a write to it will fail with
    /// [HomeError#ACCESSORY_NOT_FOUND].
    ACCESSORY_REMOVED,

    /// An accessory was renamed by the user in their ecosystem app.
    ACCESSORY_RENAMED,

    /// An accessory moved to a different room.
    ACCESSORY_MOVED,

    /// An accessory became reachable or unreachable.
    ///
    /// The most frequent kind by a wide margin, and the one worth handling
    /// separately: a whole graph re-fetch on every reachability flap is
    /// wasteful, and updating one row's availability indicator is usually all
    /// that is wanted. [HomeStructureEvent#getAccessoryId()] names the
    /// accessory. When a bridge drops, expect one of these per accessory
    /// behind it.
    REACHABILITY_CHANGED,

    /// A scene was created, changed or deleted.
    SCENES_CHANGED,

    /// [SmartHome#getAvailability()] now answers something different -- the
    /// user granted or revoked access, signed in or out, or installed the
    /// provider the backend was waiting for. Ask again.
    AVAILABILITY_CHANGED
}
