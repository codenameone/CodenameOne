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
/// System call integration: making a call this app carries look, to the
/// operating system and to the user, like a call the phone itself placed.
///
/// That means the lock-screen call UI, the ringtone that plays while the app
/// is not running, the entry in the system call log, the audio session the
/// OS hands over and takes back, and the caller's name shown for an incoming
/// number the address book has never seen.
///
/// **What this is not.** Codename One does not carry the voice. There is no
/// codec, no signalling and no WebRTC here, and none is planned: those are
/// the app's, and there are good libraries for them. What was missing was
/// everything *around* the media -- and without it an app could not ring at
/// all while backgrounded, which is why a Codename One app could not
/// previously be a calling app no matter how good its audio was.
///
/// #### The sub-packages are the opt-in
///
/// They are separate packages rather than one because **referencing a
/// package is the only opt-in there is**. The build server decides what
/// native machinery an app gets by scanning bytecode for these prefixes, and
/// it has no way to express an exclusion.
///
/// - [com.codename1.call.session] -- report calls and receive the user's
///   answer, hold, mute and keypad actions. The place to start.
/// - [com.codename1.call.voip] -- ring when the app is not running, from a
///   VoIP push. Costs the `voip` background mode, which Apple rejects an app
///   for carrying without a working call implementation.
/// - [com.codename1.call.directory] -- name and block numbers the app knows
///   about, for calls that have nothing to do with this app. Deliberately
///   **not** a superset of the other two: a caller-ID app must not have to
///   carry telephony permissions it never uses.
///
/// This package itself holds only what they share: [CallError],
/// [CallException], [CallHandle], [CallHandleType], [CallId], [CallState],
/// [CallDirection], [CallEndReason] and [CallAvailability]. Referencing it
/// alone costs nothing.
///
/// #### How this relates to what was already here
///
/// `com.codename1.ui.Display#dial(String)` hands a number to the system
/// dialer and forgets about it; it places a *cellular* call and this app is
/// not part of it. Nothing here replaces that, and an app that only wants to
/// let the user phone somebody should keep using it.
///
/// `com.codename1.ui.Display#isInCall()` is a lifecycle heuristic and always
/// was -- on iOS it reports whether the app was interrupted, not whether a
/// call exists. [com.codename1.call.session.Calls] is the real answer where
/// it is available.
package com.codename1.call;
