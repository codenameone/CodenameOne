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
package com.codename1.impl.ios;

import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.DevicePosture;
import com.codename1.ui.geom.Rectangle;

/**
 * Reads the device fold posture from UIKit's hinge API and answers the
 * {@link com.codename1.ui.DevicePosture} SPI.
 *
 * <p>The Android counterpart, {@code AndroidFoldablePosture}, reaches
 * {@code androidx.window} through reflection because the dependency is optional
 * there. Nothing equivalent is needed here: the hinge API is UIKit, which every
 * Codename One iOS binary already links, so the conditional compilation lives in
 * {@code nativeSources/CN1Hinge.m} -- against the SDK, where it belongs -- and
 * this class is unconditional. On a device or SDK without the API every native
 * below answers "no hinge" and every query degrades to "not foldable".</p>
 *
 * <p>Note the split in where the answers come from. The hinge STATUS and ANGLE
 * are push-only: UIKit delivers them through a {@code UIHingeInteraction}
 * handler, so they are whatever the last update said. The fold GEOMETRY is pull:
 * {@code -[UIView reservedRegionsOfKind:]} answers synchronously.</p>
 *
 * <p>Foldability needs both, and the reason is measured. On an iPhone Duo
 * running iOS 27.1 and folded shut, the view reports no division region --
 * inactive ones included -- while the hinge reports
 * {@code UIHingeStatusClosed}. A closed foldable has nothing for the fold to
 * divide. So {@link #isFoldable()} consults the region, and when it says
 * nothing it falls back to the hinge, which is the only thing that knows.</p>
 *
 * <p>Only the DIVISION region counts. The occlusion region is not
 * fold-specific: on the same device it is the camera housing, a 111x111 square,
 * and an earlier cut of this class reported it as the fold -- which would have
 * made {@link #isSeparating()} true and {@link #getFoldBounds} the Dynamic
 * Island on every modern iPhone.</p>
 */
class IOSFoldablePosture {
    /// Matches UIHingeStatus. -1 is ours: "no hinge has been observed", which
    /// UIKit has no value for because it only ever hands out a real hinge.
    private static final int HINGE_STATUS_NONE = -1;
    private static final int HINGE_STATUS_UNKNOWN = 0;
    private static final int HINGE_STATUS_CLOSED = 1;
    private static final int HINGE_STATUS_PARTIALLY_OPEN = 2;
    private static final int HINGE_STATUS_FULLY_OPEN = 3;

    /// Reused across calls. This is EDT-only state, like the rest of the
    /// framework: Codename One is single threaded and the native layer has
    /// already marshalled, so a fresh array per query would be allocation for
    /// nothing.
    private static final int[] REGION = new int[4];

    private IOSFoldablePosture() {
    }

    private static IOSNative nativeInstance() {
        return IOSImplementation.nativeInstance;
    }

    static boolean isFoldable() {
        return nativeInstance().isFoldableDisplay();
    }

    static int getPosture() {
        if (!isFoldable()) {
            return DevicePosture.POSTURE_UNKNOWN;
        }
        switch (nativeInstance().getHingeStatus()) {
            case HINGE_STATUS_CLOSED:
                return DevicePosture.POSTURE_CLOSED;
            case HINGE_STATUS_PARTIALLY_OPEN:
                return DevicePosture.POSTURE_HALF_OPENED;
            case HINGE_STATUS_FULLY_OPEN:
                return DevicePosture.POSTURE_FLAT;
            case HINGE_STATUS_UNKNOWN:
            case HINGE_STATUS_NONE:
            default:
                // A foldable whose hinge has not reported yet, or has reported
                // UIHingeStatusUnknown. POSTURE_UNKNOWN is the honest answer for
                // both; guessing POSTURE_FLAT would have a layout treat a closed
                // device as an open one.
                return DevicePosture.POSTURE_UNKNOWN;
        }
    }

    static int getHingeAngle() {
        if (!isFoldable()) {
            return -1;
        }
        return nativeInstance().getHingeAngleDegrees();
    }

    static int getFoldOrientation() {
        if (nativeInstance().getFoldRegion(REGION) == 0) {
            return DevicePosture.FOLD_ORIENTATION_NONE;
        }
        // A VERTICAL fold runs top to bottom and splits the display into a left
        // and a right half, so its region is TALLER than it is wide. The
        // comparison is deliberately not >= : a square region says nothing, and
        // FOLD_ORIENTATION_NONE is a better answer than a coin toss. That guard
        // earned its place -- it is what stopped the camera housing, a 111x111
        // square, from being reported as a horizontal fold while the region
        // kind was still wrong.
        int width = REGION[2];
        int height = REGION[3];
        if (height > width) {
            return DevicePosture.FOLD_ORIENTATION_VERTICAL;
        }
        if (width > height) {
            return DevicePosture.FOLD_ORIENTATION_HORIZONTAL;
        }
        return DevicePosture.FOLD_ORIENTATION_NONE;
    }

    static boolean isSeparating() {
        // An ACTIVE division region is exactly "the fold currently separates
        // the display": getFoldRegion only reports active ones, and an inactive
        // region is a display that folds but is not folded.
        return nativeInstance().getFoldRegion(REGION) != 0;
    }

    static Rectangle getFoldBounds(Rectangle rect) {
        if (nativeInstance().getFoldRegion(REGION) == 0) {
            return null;
        }
        if (rect == null) {
            rect = new Rectangle();
        }
        rect.setBounds(REGION[0], REGION[1], REGION[2], REGION[3]);
        return rect;
    }

    /**
     * Invoked from native code (do not rename) when the hinge state changes.
     *
     * <p>The name is compiled into {@code CN1Hinge.m} as
     * {@code com_codename1_impl_ios_IOSFoldablePosture_postureChangedFromNative__},
     * and ParparVM keeps a Java method alive precisely because that symbol
     * appears in the native sources. Renaming this without renaming the call
     * site does not fail to link -- it silently drops the method and the
     * posture event with it.</p>
     *
     * <p>It carries no values on purpose. The native side caches status, angle
     * and geometry and every getter above reads that cache, so a notification
     * that also carried a snapshot would be a second source of truth able to
     * disagree with the first.</p>
     */
    static void postureChangedFromNative() {
        if (!CN.isEdt()) {
            CN.callSerially(new Runnable() {
                public void run() {
                    postureChangedFromNative();
                }
            });
            return;
        }
        Display.getInstance().postureChanged();
    }
}
