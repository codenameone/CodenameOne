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
/// adopts -- and if that session fails the component replaces itself with an
/// ordinary Codename One button. So a peer that is still in place after the form
/// has settled means the whole platform handshake worked: provider created, host
/// token obtained, session opened, surface package adopted. Nothing short of a
/// real Android 17 device can say that.
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
                + platformSupported);

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
        UITimer.timer(2000, false, f, new Runnable() {
            public void run() {
                check(platformSupported);
            }
        });
        return true;
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

        // The component's own answer and what it actually put on screen have to
        // agree, on every port. A disagreement is the failure mode the instance
        // method exists to prevent.
        if (button.isSystemRendered() != (child instanceof PeerComponent)) {
            fail("isSystemRendered() disagrees with the child it produced");
            return;
        }

        if (platformSupported) {
            if (!button.isSystemRendered()) {
                // The component swaps a failed platform session for the
                // fallback button, so this is what a broken handshake looks
                // like from here.
                fail("the platform claims a location button but the session did not survive");
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

    private static Component childOf(LocationButton b) {
        if (b.getComponentCount() == 0) {
            return null;
        }
        return b.getComponentAt(0);
    }
}
