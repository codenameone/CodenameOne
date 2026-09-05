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
package com.codename1.location;

import com.codename1.junit.UITestBase;
import com.codename1.location.spi.LocationButtonBridge;
import com.codename1.testing.TestUtils;
import com.codename1.ui.Display;
import com.codename1.ui.PeerComponent;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A setter that changes the label or the colours REPLACES the platform control,
 * because it takes those at construction. The native view that leaves the screen
 * keeps the listeners it was built with -- they point at the component, not at
 * the peer -- and nothing unregisters them.
 *
 * <p>So a session that died after its button had already been replaced still
 * retired the healthy control that took its place, and a permission result from
 * the retired view was served as though the visible button had produced it.</p>
 */
class LocationButtonRebuildTest extends UITestBase {

    /** Stands in for the platform's view: a peer, with nothing behind it. */
    private static final class FakePeer extends PeerComponent {
        private FakePeer() {
            super(new Object());
        }
    }

    /** One built control: the callbacks it was handed, kept for the test. */
    private static final class Session {
        private SuccessCallback<Boolean> onResult;
        private Runnable onUnavailable;
    }

    /** A bridge that builds peers and remembers what each one was given. */
    private static final class RecordingBridge implements LocationButtonBridge {
        private final List<Session> sessions = new ArrayList<Session>();

        public boolean isSupported() {
            return true;
        }

        public void setButtonEnabled(PeerComponent button, boolean enabled) {
        }

        /**
         * Null, which is what a platform that granted nothing usable answers.
         * These tests are about which control a callback belongs to, not about
         * what a served grant then produces, and a null manager keeps the
         * served path from reaching for a location this module has no way to
         * supply.
         */
        public LocationManager getGrantedLocationManager() {
            return null;
        }

        public PeerComponent createButton(int textType, int backgroundColor,
                int textColor, SuccessCallback<Boolean> onResult,
                Runnable onUnavailable) {
            Session session = new Session();
            session.onResult = onResult;
            session.onUnavailable = onUnavailable;
            sessions.add(session);
            // NOT PeerComponent.create: that asks the implementation to make a
            // native peer, this module's implementation does not support one,
            // and createSystemButton catches the throw and quietly falls back
            // to the ordinary button. The component then holds no peer at all
            // and the test measures the fallback path instead of the one it
            // names -- which is what it did, silently, until the generation
            // counter came out two higher than there were controls.
            return new FakePeer();
        }
    }

    private RecordingBridge install() {
        RecordingBridge bridge = new RecordingBridge();
        implementation.setLocationButtonBridge(bridge);
        return bridge;
    }

    @AfterEach
    void removeBridge() {
        // The implementation is a singleton shared with every other test class
        // in this module, so a bridge left installed would make an unrelated
        // component believe this device renders the button itself.
        implementation.setLocationButtonBridge(null);
    }

    /**
     * Lets everything already queued on the EDT run, and PROVES it did.
     *
     * <p>A sentinel of its own goes on the queue behind whatever the test just
     * posted, so FIFO ordering makes "the sentinel ran" mean "the callback ran".
     * Sleeping a fixed interval instead is how the negative assertion in these
     * tests passed while the EDT had not run a single one of them: nothing had
     * happened yet, and "nothing happened" is exactly what it was asserting.</p>
     */
    private static void drain() {
        final boolean[] ran = new boolean[1];
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                ran[0] = true;
            }
        });
        for (int guard = 0; !ran[0] && guard < 400; guard++) {
            TestUtils.waitFor(5);
        }
        assertTrue(ran[0],
                "the EDT never drained its queue, so this test asserted "
                + "nothing at all");
    }

    @Test
    void aFailedSessionDoesNotRetireTheButtonThatReplacedIt() {
        RecordingBridge bridge = install();
        LocationButton button = new LocationButton();
        assertEquals(1, bridge.sessions.size(), "the platform control");
        assertFalse(button.isUnavailable());

        // A setter has to build a new control, because the platform takes the
        // label at construction.
        button.setTextType(LocationButton.TEXT_USE_PRECISE_LOCATION);
        assertEquals(2, bridge.sessions.size(), "the replacement control");

        // Now the FIRST session dies, which is the whole shape of this bug: the
        // view is off screen but its error listener still points here.
        bridge.sessions.get(0).onUnavailable.run();
        drain();
        assertFalse(button.isUnavailable(),
                "a session that died after its button was replaced must not "
                + "retire the healthy control that took its place");

        // The one on screen failing is a real failure and must still land.
        bridge.sessions.get(1).onUnavailable.run();
        drain();
        assertTrue(button.isUnavailable(),
                "the CURRENT control failing is what unavailable means");
    }

    @Test
    void aGrantFromAReplacedControlIsNotServed() {
        RecordingBridge bridge = install();
        LocationButton button = new LocationButton();
        final int[] answers = new int[1];
        button.addLocationSharedListener(new LocationSharedListener() {
            public void locationShared(Location location) {
                answers[0]++;
            }
        });
        button.setTextType(LocationButton.TEXT_USE_PRECISE_LOCATION);
        assertEquals(2, bridge.sessions.size());

        // The retired view reports a grant. Serving it would fire the listeners
        // of a button the user has not touched.
        bridge.sessions.get(0).onResult.onSucess(Boolean.TRUE);
        drain();
        assertEquals(0, answers[0],
                "a permission result from a replaced control is not this "
                + "button's answer");
    }
}
