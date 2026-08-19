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

/// What kind of [Scene] this is.
///
/// HomeKit gives its four built-in action sets distinct types and lets the
/// user create more; the built-ins are the ones an ecosystem app surfaces
/// specially, so an app that wants to match that presentation can.
public enum SceneType {

    /// The home's built-in "good morning" scene.
    WAKE_UP,

    /// The home's built-in "good night" scene.
    SLEEP,

    /// The home's built-in "I'm home" scene.
    ARRIVAL,

    /// The home's built-in "I'm leaving" scene.
    DEPARTURE,

    /// A scene the user created.
    USER_DEFINED,

    /// A scene owned by an automation rather than by the user.
    ///
    /// **Not executable.** It exists so a listing is complete and so a scene
    /// the user cannot run does not silently look like one they can; the
    /// automation that owns it decides when it fires, and this release does
    /// not expose automations. [Scene#isExecutable()] answers `false`.
    TRIGGER_OWNED
}
