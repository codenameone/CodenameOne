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
package com.codename1.intents;

/// Where an intent invocation came from.
///
/// Handlers should rarely branch on this -- an intent that behaves differently
/// depending on who asked is a bug waiting to happen -- but it is useful for
/// analytics, for tailoring the spoken line, and for deciding how much detail a
/// snippet should carry.
public enum IntentSource {
    /// A voice assistant (Siri on iOS). Only ever seen where
    /// [Intents#isVoiceInvocationSupported()] is true.
    VOICE,

    /// The user tapped an item this app published through [Intents#index].
    SPOTLIGHT,

    /// A shortcut the user placed or the system suggested: the Shortcuts app,
    /// an iOS App Shortcut, or an Android launcher shortcut.
    SHORTCUT,

    /// A button on a home-screen widget or live activity bound to this intent.
    WIDGET,

    /// An in-app call to [Intents#invoke], including the simulator's Intents
    /// window.
    IN_APP,

    /// A language model calling the intent as a tool, through the projection
    /// [Intents#asTools()] exposes.
    MODEL,

    /// The platform did not say, or the source is one this version does not
    /// model. Treat exactly as [#IN_APP].
    UNKNOWN
}
