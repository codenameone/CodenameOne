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
//  CN1Hinge.h
//  Fold state for the iOS foldable, behind the com.codename1.ui.DevicePosture API.
//
//  There is deliberately NO //#define CN1_INCLUDE_HINGE gate here, unlike
//  CN1Health / CN1Bluetooth / CN1SmartHome. Every one of those gates buys
//  something: a privacy string the App Store reviews, an entitlement the
//  provisioning profile has to carry, or a framework on the link line. The hinge
//  API buys none of the three -- it is UIKit, which every Codename One iOS
//  binary already links, and it asks the user for nothing.
//
//  A gate would also have nothing to key on. The builder turns a gate on when
//  the application's bytecode references a package PlatformFeatureCatalog names,
//  and DevicePosture lives in com.codename1.ui -- a package every application
//  references. A prefix scan cannot tell an app that asks for the posture from
//  one that merely draws a Label, so the gate would be either always on or
//  always wrong.
//

#ifndef CN1Hinge_h
#define CN1Hinge_h

#include <stdbool.h>

/// Installs the hinge observer on the application's root view, once. Safe to
/// call from any thread and safe to call repeatedly; it hops to the main thread
/// because UIHingeInteraction is main-actor isolated.
///
/// A no-op on a build whose SDK predates the hinge API, and at runtime below
/// iOS 27.1, which is where UIHinge / UIHingeInteraction were introduced. Note
/// that is 27.1 and not 27.0: an @available(iOS 27, *) fence would compile and
/// then call a selector that does not exist on 27.0.
void cn1HingeStart(void);

/// True when this device has a hinge.
///
/// Division regions alone cannot answer this. Measured on an iPhone Duo
/// simulator running iOS 27.1 while folded shut: zero division regions, even
/// including inactive ones -- and a live hinge reporting UIHingeStatusClosed.
/// A closed foldable has nothing for the fold to divide, so the region is
/// absent and the hinge is the only witness.
///
/// The regions are still consulted first, because they answer synchronously.
/// When they say nothing, the call waits briefly (bounded, once per process)
/// for the hinge observer's first update, which UIKit documents as carrying the
/// initial state. Never call this from the main thread expecting the wait: the
/// update handler is main-actor isolated, so it is skipped there.
bool cn1HingeIsFoldableDisplay(void);

/// The most recent UIHingeStatus, or -1 when no hinge has been observed.
/// Values match UIHingeStatus: 0 unknown, 1 closed, 2 partially open, 3 fully
/// open.
int cn1HingeStatus(void);

/// The most recent hinge angle in whole DEGREES, or -1 when unknown. UIHinge
/// reports radians; degrees is what com.codename1.ui.DevicePosture documents, so
/// the conversion happens here rather than leaving two units in play across the
/// boundary.
int cn1HingeAngleDegrees(void);

/// Fills `out` with the active fold region in display PIXELS (x, y, width,
/// height) and returns its kind: 0 none, 2 division. `out` is untouched when
/// the return value is 0.
///
/// Only DIVISION regions count as a fold. Occlusion regions are not
/// fold-specific -- on an iPhone Duo the occlusion set is the camera housing
/// (a 111x111 square), so reading it here would report the Dynamic Island of
/// any iPhone as a hinge. See the note in CN1Hinge.m.
int cn1HingeFoldRegion(int *out);

#endif /* CN1Hinge_h */
