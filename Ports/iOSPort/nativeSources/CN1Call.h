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

//
//  CN1Call.h
//  The CallKit, PushKit and Call Directory bridge behind com.codename1.call.
//
//  Everything here is gated on CN1_INCLUDE_CALL, which IPhoneBuilder
//  uncomments in CodenameOne_GLViewController.h only for apps that reference
//  com.codename1.call. The two expensive halves are gated again, separately,
//  on CN1_CALL_VOIP and CN1_CALL_DIRECTORY -- an app that owns calls must not
//  link PushKit, because linking it obliges the voip background mode and
//  Apple rejects an app that carries that without a working call
//  implementation.
//
//  The provider, the call registry and the pending-push queue are all
//  file-static in CN1Call.m -- nothing is exported -- so this header exists
//  only to carry the shared ordinal constants and the one entry point the
//  app delegate needs.
//

#ifndef CN1Call_h
#define CN1Call_h

#import <Foundation/Foundation.h>

// com.codename1.call.CallError ordinals. Kept in step with the enum by hand;
// the enum's own documentation says existing constants must not be reordered.
#define CN1_CALL_ERR_NOT_SUPPORTED    0
#define CN1_CALL_ERR_UNAUTHORIZED     1
#define CN1_CALL_ERR_CALL_REFUSED     2
#define CN1_CALL_ERR_CALL_FILTERED    3
#define CN1_CALL_ERR_DUPLICATE_CALL   4
#define CN1_CALL_ERR_INVALID_ID       5
#define CN1_CALL_ERR_ACTION_TIMEOUT   6
#define CN1_CALL_ERR_AUDIO_FAILED     7
#define CN1_CALL_ERR_PUSH_UNAVAILABLE 8
#define CN1_CALL_ERR_DIRECTORY_FAILED 9
#define CN1_CALL_ERR_TIMEOUT          10
#define CN1_CALL_ERR_BUSY             11
#define CN1_CALL_ERR_UNKNOWN          12

// com.codename1.call.CallEndReason ordinals.
#define CN1_CALL_END_REMOTE     0
#define CN1_CALL_END_LOCAL      1
#define CN1_CALL_END_UNANSWERED 2
#define CN1_CALL_END_BUSY       3
#define CN1_CALL_END_FAILED     4
#define CN1_CALL_END_FILTERED   5

// com.codename1.call.CallAvailability ordinals.
#define CN1_CALL_AVAIL_AVAILABLE 0
#define CN1_CALL_AVAIL_EMERGENCY 1
#define CN1_CALL_AVAIL_OTHER_APP 2
#define CN1_CALL_AVAIL_NOT_PERMITTED 3
#define CN1_CALL_AVAIL_UNSUPPORTED 4

// com.codename1.call.session.CallAudioRoute ordinals.
#define CN1_CALL_ROUTE_EARPIECE 0
#define CN1_CALL_ROUTE_SPEAKER  1
#define CN1_CALL_ROUTE_WIRED    2
#define CN1_CALL_ROUTE_BLUETOOTH 3
#define CN1_CALL_ROUTE_UNKNOWN  4

// com.codename1.call.CallHandleType ordinals.
#define CN1_CALL_HANDLE_GENERIC 0
#define CN1_CALL_HANDLE_PHONE   1
#define CN1_CALL_HANDLE_EMAIL   2

// CallBridge.CAPABILITY_* bits.
#define CN1_CALL_CAP_SYSTEM_UI    1
#define CN1_CALL_CAP_OUTGOING     2
#define CN1_CALL_CAP_HOLD         4
#define CN1_CALL_CAP_MUTE         8
#define CN1_CALL_CAP_DTMF         16
#define CN1_CALL_CAP_GROUPING     32
#define CN1_CALL_CAP_VIDEO        64
#define CN1_CALL_CAP_VOIP_PUSH    128
#define CN1_CALL_CAP_DIRECTORY    256
#define CN1_CALL_CAP_SCREENING    512
#define CN1_CALL_CAP_ROUTE_PICKER 1024

// CallBridge.PERMISSION_* bits.
#define CN1_CALL_PERM_MANAGE_CALLS  1
#define CN1_CALL_PERM_MICROPHONE    2
#define CN1_CALL_PERM_CAMERA        4
#define CN1_CALL_PERM_NOTIFICATIONS 8
#define CN1_CALL_PERM_SCREENING     16

/// Installs the PushKit registry, from the app delegate's
/// willFinishLaunchingWithOptions:.
///
/// **Not didFinishLaunching.** A VoIP push can be delivered during launch, and
/// a registry that does not exist yet loses it -- which on iOS is the case
/// that terminates the process.
///
/// A no-op in builds without CN1_CALL_VOIP.
void cn1CallInstallPushRegistry(void);

#endif /* CN1Call_h */
