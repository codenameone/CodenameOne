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
/// Reporting calls to the operating system, and hearing what the user does
/// with them.
///
/// [Calls] is the entry point. A call reported here appears wherever the
/// system shows calls -- the lock screen, the car, the watch, the call log --
/// and the answer, hold, mute and keypad the user reaches for there arrive
/// back as [CallActionListener] events.
///
/// Referencing this package makes the build ask for the machinery: CallKit on
/// iOS, a self-managed `ConnectionService` and `MANAGE_OWN_CALLS` on Android.
/// It does **not** ask for the VoIP background mode -- that is
/// [com.codename1.call.voip], and Apple rejects an app that carries it
/// without cause.
///
/// #### This package carries no audio
///
/// Nothing here encodes, transports or plays anything. The system hands the
/// call its audio session and the app starts its own media then -- see
/// [CallAudioSession], which is the single most common thing to get wrong.
package com.codename1.call.session;
