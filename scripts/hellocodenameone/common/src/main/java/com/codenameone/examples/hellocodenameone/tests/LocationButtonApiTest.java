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

import com.codename1.location.LocationButton;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.UITimer;

/// Puts a real [com.codename1.location.LocationButton] on screen and checks what
/// the device made of it.
///
/// The interesting assertion is the one that needs a device: on Android 17 the
/// button is drawn by the *system*, in another process, into a surface this view
/// adopts. What is checked is `isSystemRendered()`, which asks the platform
/// whether it is actually drawing -- provider created, host token obtained,
/// session opened, surface package adopted. A peer being in place is NOT that
/// check and deliberately is not used as one: the component builds its peer
/// synchronously, before any session exists, so a handshake that hangs with no
/// error to report would leave a control that is present, blank, and passing.
/// Nothing short of a real Android 17 device can tell those apart.
///
/// Below API 37, and on every other port, the component is that ordinary button
/// and this checks it is present and labelled.
///
/// The one thing not covered is the tap itself, which needs a person: the grant
/// only means anything when the *user* presses a button the system drew, and
/// synthesised input is exactly what that design refuses to honour.
///
/// No screenshot -- the system's surface belongs to another process and does not
/// belong in a baseline that has to match across ports.
public class LocationButtonApiTest extends BaseTest {

    private LocationButton button;

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        // The PLATFORM question, asked before anything is built.
        // LocationButton.isSystemRendered() answers for a particular button and
        // is false until one has been shown, so the two are not interchangeable.
        final boolean platformSupported =
                com.codename1.ui.Display.getInstance().isLocationButtonSupported();
        com.codename1.io.Log.p("LocationButtonApiTest: platformSupported="
                + platformSupported + " androidSeventeen=" + androidSeventeenOrNewer());

        // Support detection is part of what this test covers, so it cannot also
        // BE the expectation: a regressed class name, SDK floor or reflective
        // lookup would make isLocationButtonSupported() false, and a test that
        // derived its expectation from it would quietly accept the fallback and
        // pass. The device's own OS release is an independent answer -- it comes
        // from Build.VERSION.RELEASE, while the detection reads SDK_INT and
        // looks up android.app.permissionui classes.
        if (androidSeventeenOrNewer() && !platformSupported) {
            fail("Android 17 reports no location button support; detection regressed");
            return true;
        }

        button = new LocationButton(LocationButton.TEXT_USE_PRECISE_LOCATION);
        if (button.getTextType() != LocationButton.TEXT_USE_PRECISE_LOCATION) {
            fail("the button did not keep the text type it was built with");
            return true;
        }

        Form f = new Form("LocationButton", BoxLayout.y());
        f.add(button);
        f.show();

        // The component builds its child when it is initialized, and the
        // platform's session opens a beat later -- a failure arrives as a
        // callback, not as a return value, so the check has to wait for the
        // form to settle rather than run straight after show().
        //
        // Bounded polling rather than one long sleep: the session took under a
        // second on an Android 17 emulator, but that is one machine's answer
        // and a fixed deadline tuned to it is a flake waiting for a slower one.
        // A control that never opens still fails, just later.
        scheduleCheck(f, platformSupported, 0);
        return true;
    }

    /// How many times to look again before calling a session that has not
    /// opened a session that never will.
    private static final int MAX_ATTEMPTS = 8;

    private void scheduleCheck(final Form f, final boolean platformSupported,
            final int attempt) {
        UITimer.timer(attempt == 0 ? 2000 : 1000, false, f, new Runnable() {
            public void run() {
                if (platformSupported && !button.isSystemRendered()
                        && attempt + 1 < MAX_ATTEMPTS
                        && childOf(button) instanceof PeerComponent) {
                    // Still the platform's control and still not drawing:
                    // give the session more time before calling it dead. Once
                    // the component has swapped in the fallback there is
                    // nothing left to wait for.
                    scheduleCheck(f, platformSupported, attempt + 1);
                    return;
                }
                check(platformSupported);
            }
        });
    }

    private void check(boolean platformSupported) {
        Component child = childOf(button);
        if (child == null) {
            fail("the location button produced no child at all");
            return;
        }
        com.codename1.io.Log.p("LocationButtonApiTest: child=" + child.getClass().getName()
                + " systemRendered=" + button.isSystemRendered()
                + " " + button.getWidth() + "x" + button.getHeight());

        // Reporting the system's control while showing an ordinary button is
        // the failure mode the instance method exists to prevent. The converse
        // is legal for a beat -- the peer is built before its session opens --
        // so only this direction is an error.
        if (button.isSystemRendered() && !(child instanceof PeerComponent)) {
            fail("isSystemRendered() is true while an ordinary button is showing");
            return;
        }

        if (platformSupported) {
            if (!button.isSystemRendered()) {
                // Covers both shapes of a broken handshake: a session that
                // reported an error, which the component swapped for the
                // fallback button, and one that simply never opened, which
                // leaves the peer in place and blank.
                fail("the platform claims a location button but nothing is drawing it");
                return;
            }
            if (button.getWidth() <= 0 || button.getHeight() <= 0) {
                fail("the system location button was laid out with no size");
                return;
            }
            done();
            return;
        }

        if (!(child instanceof Button)) {
            fail("without a platform control the component must be an ordinary button");
            return;
        }
        if (((Button) child).getText().length() == 0) {
            fail("the fallback button should carry the label of its text type");
            return;
        }
        done();
    }

    /// Whether this device is Android 17 or newer, decided without asking the
    /// code under test.
    ///
    /// `OSVer` is documented as a user-readable string rather than a number, so
    /// only the leading integer is read and anything unparseable answers false
    /// -- this may only ever ADD an assertion, never remove one.
    private static boolean androidSeventeenOrNewer() {
        if (!"and".equals(com.codename1.ui.Display.getInstance().getPlatformName())) {
            return false;
        }
        String version =
                com.codename1.ui.Display.getInstance().getProperty("OSVer", "");
        int end = 0;
        while (end < version.length() && Character.isDigit(version.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return false;
        }
        try {
            return Integer.parseInt(version.substring(0, end)) >= 17;
        } catch (NumberFormatException notANumber) {
            return false;
        }
    }

    private static Component childOf(LocationButton b) {
        if (b.getComponentCount() == 0) {
            return null;
        }
        return b.getComponentAt(0);
    }
}
