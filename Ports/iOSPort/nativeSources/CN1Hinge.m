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
//  CN1Hinge.m
//  The iOS side of com.codename1.ui.DevicePosture.
//
//  Apple's foldable API arrived in the iOS **27.1** SDK, not 27.0: UIHinge,
//  UIHingeInteraction and -[UIView reservedRegionsOfKind:] are all
//  API_AVAILABLE(ios(27.1)). Measured on the two SDKs installed when this was
//  written, __IPHONE_OS_VERSION_MAX_ALLOWED is 260200 for Xcode 26.2 and 270100
//  for Xcode 27.1, and <UIKit/UIHingeInteraction.h> exists only in the latter.
//
//  The tree still builds with Xcode 26 by default (scripts/lib/xcode.sh pins
//  CN1_XCODE_MAJOR=26), so this file has to COMPILE against an SDK that has none
//  of it. __has_include on the header is the guard rather than a version
//  comparison, because it answers the question actually being asked -- does this
//  SDK declare the API -- and keeps working when Apple renumbers.
//
//  watchOS needs no special case: UIHinge is API_UNAVAILABLE(watchos) and the
//  header is absent from the watch SDK, so the same guard compiles the whole
//  implementation out there and leaves the "no hinge" answers behind.
//

#import "CodenameOne_GLViewController.h"
#import "CN1Hinge.h"

#if __has_include(<UIKit/UIHingeInteraction.h>)
#define CN1_HAS_HINGE_SDK 1
#import <UIKit/UIKit.h>
#import <UIKit/UIHinge.h>
#import <UIKit/UIHingeInteraction.h>
#endif

#include "com_codename1_impl_ios_IOSFoldablePosture.h"

extern void cn1RunSyncOnMainQueue(void (^block)(void));

// ---------------------------------------------------------------------
// Cached fold state.
//
// Everything here is written on the MAIN thread -- from the interaction's
// update handler, which UIKit declares NS_SWIFT_UI_ACTOR, and from the one-shot
// seeding in cn1HingeStart. Readers are the EDT.
//
// There is no lock, and that is deliberate rather than an oversight: Codename
// One is single threaded on the EDT and the framework adds no locks, so the
// marshalling belongs at the native boundary. These are separately-written ints,
// so the worst a reader can observe is one update's status next to the previous
// update's angle -- a frame of skew in a value that is already a live physical
// measurement, not a torn value. Bundling them behind a lock would buy a
// consistency the hardware does not offer anyway.
// ---------------------------------------------------------------------

static int cn1HingeStatusValue = -1;        // -1 = no hinge observed yet
static int cn1HingeAngleValue = -1;         // whole degrees, -1 = unknown
static int cn1HingeRegionKind = 0;          // 0 none, 2 division (see below)
static int cn1HingeRegionPx[4] = {0, 0, 0, 0};
static bool cn1HingeDisplayIsFoldable = false;

#ifdef CN1_HAS_HINGE_SDK

// These two are inside the guard because only the guarded code reads them; at
// file scope on the no-hinge build they would be unused statics.

/// Set once cn1HingeIsFoldableDisplay has waited for a first hinge update, so a
/// device that has none pays that wait once rather than on every query.
static bool cn1HingeProbed = false;
/// True once the interaction is actually on a view. Kept at file scope rather
/// than inside cn1HingeStart as a function static, because the probe latch has
/// to distinguish "waited and no hinge exists" from "waited while there was
/// still no root view to observe through" -- latching the second would answer
/// "not foldable" for the rest of the process on a device that folds.
static bool cn1HingeInstalled = false;

/// The view the interaction is attached to and the regions are read from. The
/// root view's coordinate space is the one DevicePosture#getFoldBounds is
/// documented in ("display coordinates"), which is why the region is read here
/// rather than from whatever view happens to be asking.
static UIView *cn1HingeRootView(void) {
    CodenameOne_GLViewController *vc = [CodenameOne_GLViewController instance];
    return vc == nil ? nil : vc.view;
}

static CGFloat cn1HingeScale(UIView *view) {
    if (view != nil && view.window != nil && view.window.screen != nil) {
        return view.window.screen.scale;
    }
    return [UIScreen mainScreen].scale;
}

/// Reads the active fold region into the cache. Main thread only.
///
/// DIVISION regions only, and that is measured rather than chosen.
///
/// The two kinds sound interchangeable -- an occlusion region is display the
/// application cannot draw into, a division region is a seam that splits it --
/// and the first cut here read both, preferring occlusion on the reasoning that
/// a hinge which eats pixels is an occlusion. Running it on an iPhone Duo
/// (iOS 27.1) said otherwise:
///
///     bounds = x=1199 y=88 width=111 height=111
///
/// A 111x111 square near the top of the display is the camera housing, not a
/// hinge. `occlusionRegionKind` is not fold-specific: it reports every occluded
/// area, so any iPhone with a camera cutout would have answered
/// DevicePosture#isSeparating with true and DevicePosture#getFoldBounds with
/// the Dynamic Island. That is not a near miss, it is the wrong feature.
///
/// So the fold is the DIVISION region, which is the kind whose meaning is
/// actually "the display is divided here". If a future device turns out to
/// report its fold as an occlusion instead, the fix is to measure that device
/// and widen this deliberately -- not to guess now with a shape heuristic,
/// which would be a rule about camera cutouts rather than about folds.
static void cn1HingeRefreshRegionOnMain(void) API_AVAILABLE(ios(27.1)) {
    UIView *view = cn1HingeRootView();
    cn1HingeRegionKind = 0;
    if (view == nil) {
        return;
    }
    CGFloat scale = cn1HingeScale(view);
    UIViewReservedRegionKind *division = [UIViewReservedRegionKind divisionRegionKind];
    // Including inactive regions answers "does this display divide at all",
    // which is a different question from "is it divided right now" -- the first
    // is DevicePosture#isFoldable, the second DevicePosture#isSeparating.
    NSArray<UIViewReservedRegion *> *all =
        [view reservedRegionsOfKind:division
                            options:UIViewReservedRegionQueryOptionsIncludeInactive];
    if (all.count > 0) {
        cn1HingeDisplayIsFoldable = true;
    }
    for (UIViewReservedRegion *region in all) {
        if (!region.isActive) {
            continue;
        }
        CGRect f = region.frame;
        cn1HingeRegionPx[0] = (int)lround(f.origin.x * scale);
        cn1HingeRegionPx[1] = (int)lround(f.origin.y * scale);
        cn1HingeRegionPx[2] = (int)lround(f.size.width * scale);
        cn1HingeRegionPx[3] = (int)lround(f.size.height * scale);
        cn1HingeRegionKind = 2;
        break;
    }
}

#endif /* CN1_HAS_HINGE_SDK */

void cn1HingeStart(void) {
#ifdef CN1_HAS_HINGE_SDK
    // Deliberately NOT dispatch_once. The root view does not exist yet when the
    // implementation initialises, and a dispatch_once that found nil would have
    // spent the only attempt -- leaving an app that adds a posture listener at
    // startup, and never queries, with an observer that was never installed.
    // The flag is set on success only, so this stays idempotent while remaining
    // retryable, and every call after the first is a load and a branch.
    if (cn1HingeInstalled) {
        return;
    }
    cn1RunSyncOnMainQueue(^{
        if (cn1HingeInstalled) {
            return;
        }
        // 27.1 and not 27 -- see the file header. An @available(iOS 27, *)
        // fence here would pass on 27.0 and then message a class that does
        // not exist there.
        if (@available(iOS 27.1, *)) {
            UIView *view = cn1HingeRootView();
            if (view == nil) {
                return;
            }
            cn1HingeRefreshRegionOnMain();
            UIHingeInteraction *interaction = [[UIHingeInteraction alloc]
                initWithUpdateHandler:^(UIHingeInteraction *sender,
                                        UIHingeInteractionUpdate *update) {
                    UIHinge *hinge = update.hinge;
                    if (hinge == nil) {
                        // The interaction left a hierarchy that supplies hinge
                        // updates. That is not "the device stopped folding", so
                        // the display's foldability is left alone and only the
                        // live reading is dropped.
                        cn1HingeStatusValue = -1;
                        cn1HingeAngleValue = -1;
                    } else {
                        cn1HingeStatusValue = (int)hinge.status;
                        // UIHinge reports RADIANS; DevicePosture documents whole
                        // degrees from 0 (closed) to 180 (flat). Converting here
                        // keeps one unit on each side of the boundary instead of
                        // two in play.
                        cn1HingeAngleValue =
                            (int)lround(hinge.angle * 180.0 / M_PI);
                        cn1HingeDisplayIsFoldable = true;
                    }
                    cn1HingeRefreshRegionOnMain();
                    com_codename1_impl_ios_IOSFoldablePosture_postureChangedFromNative__(
                        getThreadLocalData());
                }];
            // The block escapes (UIKit stores it), and it captures nothing but C
            // statics, so there is no retain cycle to break. The port compiles
            // without ARC -- only CN1Vision.m, CN1Language.m and CN1Inference.m
            // get -fobjc-arc -- so the interaction is owned by the view from
            // here on and is never released: it lives as long as the
            // application does, which is the whole point.
            [view addInteraction:interaction];
            cn1HingeInstalled = true;
        }
    });
#endif
}

bool cn1HingeIsFoldableDisplay(void) {
#ifdef CN1_HAS_HINGE_SDK
    cn1HingeStart();
    if (cn1HingeDisplayIsFoldable) {
        return true;
    }
    if (cn1HingeProbed) {
        // Asked once, answered no. A device with no hinge never produces an
        // update, so without this latch every query would pay the wait below.
        return false;
    }

    // Reserved regions are readable synchronously, so ask them first -- but
    // they are NOT sufficient, and that is measured rather than assumed.
    //
    // On an iPhone Duo simulator running iOS 27.1, folded shut:
    //
    //     division total=0 active=0      (inactive included)
    //     hinge    status=1 (UIHingeStatusClosed) angle=0.0
    //
    // A closed foldable reports no division region at all, not even an inactive
    // one -- there is nothing for the fold to divide while the cover display is
    // the one in use. Keying foldability on the regions alone therefore answers
    // "not foldable" for a foldable device that happens to be shut, which is
    // the worst of the three possible wrong answers: it is wrong exactly when
    // an application most wants to know.
    //
    // The hinge itself is the authority, and it is push-only. So when the
    // regions say nothing, wait briefly for the first update: UIKit documents
    // the handler as being "invoked with the initial hinge state", and the
    // interaction is installed during IOSImplementation#init, so in practice
    // it has already fired long before application code asks.
    cn1RunSyncOnMainQueue(^{
        if (@available(iOS 27.1, *)) {
            cn1HingeRefreshRegionOnMain();
        }
    });
    if (cn1HingeDisplayIsFoldable) {
        cn1HingeProbed = true;
        return true;
    }
    if (!cn1HingeInstalled) {
        // No root view yet, so there is nothing observing and nothing to wait
        // for. Answer with what is known and do NOT latch: the interaction gets
        // installed as soon as a view exists, and a later call must be free to
        // ask again. IOSImplementation#init calls startHingeMonitoring, so this
        // window is short and usually empty.
        return false;
    }
    if ([NSThread isMainThread]) {
        // The handler is main-actor isolated, so spinning HERE would starve the
        // very runloop that has to deliver it. Answer with what is known and
        // leave the latch unset, so a later call from the EDT can still ask
        // properly. Codename One calls this from the EDT; the main thread path
        // exists only for a native caller.
        return false;
    }
    // Bounded, and only ever once. 300ms is far longer than the observed
    // delivery (the handler fires within the first runloop turns after
    // addInteraction:) and short enough that a non-foldable device pays it once
    // and never again.
    for (int waited = 0; waited < 300 && !cn1HingeDisplayIsFoldable; waited += 10) {
        [NSThread sleepForTimeInterval:0.01];
    }
    cn1HingeProbed = true;
    return cn1HingeDisplayIsFoldable;
#else
    return false;
#endif
}

int cn1HingeStatus(void) {
    return cn1HingeStatusValue;
}

int cn1HingeAngleDegrees(void) {
    return cn1HingeAngleValue;
}

int cn1HingeFoldRegion(int *out) {
    if (cn1HingeRegionKind == 0 || out == NULL) {
        return 0;
    }
    out[0] = cn1HingeRegionPx[0];
    out[1] = cn1HingeRegionPx[1];
    out[2] = cn1HingeRegionPx[2];
    out[3] = cn1HingeRegionPx[3];
    return cn1HingeRegionKind;
}

// ---------------------------------------------------------------------
// ParparVM entry points.
//
// The C name encodes the WHOLE Java signature and neither the compiler nor the
// linker checks it: a misspelling compiles, links, and leaves the Java method
// with no symbol -- at which point the dead-code pass reads it as unused and
// drops it, so the feature is inert on a green build. scripts/check-native-
// signatures.sh is the gate; run it after touching any name here.
// ---------------------------------------------------------------------

void com_codename1_impl_ios_IOSNative_startHingeMonitoring__(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    POOL_BEGIN();
    cn1HingeStart();
    POOL_END();
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isFoldableDisplay___R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    POOL_BEGIN();
    JAVA_BOOLEAN result = cn1HingeIsFoldableDisplay() ? JAVA_TRUE : JAVA_FALSE;
    POOL_END();
    return result;
}

JAVA_INT com_codename1_impl_ios_IOSNative_getHingeStatus___R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return (JAVA_INT)cn1HingeStatus();
}

JAVA_INT com_codename1_impl_ios_IOSNative_getHingeAngleDegrees___R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    return (JAVA_INT)cn1HingeAngleDegrees();
}

JAVA_INT com_codename1_impl_ios_IOSNative_getFoldRegion___int_1ARRAY_R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT out) {
    if (out == JAVA_NULL) {
        return 0;
    }
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* intArray = out;
    JAVA_ARRAY_INT* data =
        (JAVA_ARRAY_INT*)intArray->fields.org_xmlvm_runtime_XMLVMArray.array_;
#else
    JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)((JAVA_ARRAY)out)->data;
#endif
    int region[4];
    int kind = cn1HingeFoldRegion(region);
    if (kind != 0) {
        data[0] = (JAVA_ARRAY_INT)region[0];
        data[1] = (JAVA_ARRAY_INT)region[1];
        data[2] = (JAVA_ARRAY_INT)region[2];
        data[3] = (JAVA_ARRAY_INT)region[3];
    }
    return (JAVA_INT)kind;
}
