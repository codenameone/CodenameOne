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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.Log;
import com.codename1.ui.CN;
import com.codename1.ui.DevicePosture;
import com.codename1.ui.Display;
import com.codename1.ui.geom.Rectangle;

/// Exercises `com.codename1.ui.DevicePosture` end to end on the device.
///
/// Most CI devices do not fold, so the assertions are mostly about the
/// NOT-foldable contract -- which is the half that is easy to get wrong in a
/// port, because every value has a distinct "nothing to report" spelling and
/// they are not the same spelling. A port that answered `0` for the hinge angle
/// instead of `-1`, or returned a zero-sized rectangle instead of null, would
/// look plausible in a log and be wrong in every caller.
///
/// On a device that DOES fold -- an iPhone Duo on iOS 27.1, or an Android
/// foldable -- the same assertions pin the internal consistency of the reading
/// instead: a separating fold must have bounds, a non-separating one must not,
/// and a reported angle must be in the documented range.
///
/// The live values are logged either way, so a run on new hardware is readable
/// without a debugger.
///
/// No screenshot -- this is an assertion test.
public class DevicePostureApiTest extends BaseTest {

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        DevicePosture posture = DevicePosture.getInstance();
        if (posture == null) {
            fail("DevicePosture.getInstance() must never return null");
            return true;
        }
        if (posture != DevicePosture.getInstance()) {
            fail("DevicePosture.getInstance() must return the shared instance");
            return true;
        }

        boolean foldable = posture.isFoldable();
        int state = posture.getPosture();
        int angle = posture.getHingeAngle();
        int orientation = posture.getFoldOrientation();
        boolean separating = posture.isSeparating();
        Rectangle bounds = posture.getFoldBounds(null);

        Log.p("DevicePostureApiTest: foldable=" + foldable
                + " posture=" + state
                + " angle=" + angle
                + " orientation=" + orientation
                + " separating=" + separating
                + " tableTop=" + posture.isTableTop()
                + " bounds=" + (bounds == null ? "null" : bounds.toString())
                + " displays=" + CN.getDisplayCount());

        // Display and CN must agree with DevicePosture -- they are three doors
        // into one implementation method, and a port that overrode only one of
        // them would leave the other two answering the portable default.
        if (Display.getInstance().isFoldable() != foldable
                || CN.isFoldable() != foldable) {
            fail("Display.isFoldable/CN.isFoldable disagree with DevicePosture.isFoldable");
            return true;
        }

        if (!foldable) {
            // Every "nothing to report" value, each with its own spelling.
            if (state != DevicePosture.POSTURE_UNKNOWN) {
                fail("a non-foldable device must report POSTURE_UNKNOWN, got " + state);
                return true;
            }
            if (angle != -1) {
                fail("a non-foldable device must report a hinge angle of -1, got " + angle);
                return true;
            }
            if (orientation != DevicePosture.FOLD_ORIENTATION_NONE) {
                fail("a non-foldable device must report FOLD_ORIENTATION_NONE, got " + orientation);
                return true;
            }
            if (separating || posture.isTableTop()) {
                fail("a non-foldable device cannot be separating or table-top");
                return true;
            }
            if (bounds != null) {
                fail("a non-foldable device must report null fold bounds, got " + bounds);
                return true;
            }
            done();
            return true;
        }

        // A real foldable. Assert the reading is internally consistent rather
        // than asserting a particular posture, which depends on how the device
        // is being held.
        if (state != DevicePosture.POSTURE_UNKNOWN
                && state != DevicePosture.POSTURE_FLAT
                && state != DevicePosture.POSTURE_HALF_OPENED
                && state != DevicePosture.POSTURE_CLOSED) {
            fail("posture out of range: " + state);
            return true;
        }
        if (angle != -1 && (angle < 0 || angle > 180)) {
            fail("hinge angle must be -1 or 0..180 degrees, got " + angle);
            return true;
        }
        if (separating && bounds == null) {
            fail("a separating fold must report bounds");
            return true;
        }
        if (!separating && bounds != null) {
            fail("a fold that does not separate must report null bounds, got " + bounds);
            return true;
        }
        if (separating && orientation == DevicePosture.FOLD_ORIENTATION_NONE) {
            fail("a separating fold must report an orientation");
            return true;
        }
        if (posture.isTableTop() && state != DevicePosture.POSTURE_HALF_OPENED) {
            fail("isTableTop() implies POSTURE_HALF_OPENED");
            return true;
        }
        done();
        return true;
    }
}
