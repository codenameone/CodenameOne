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
package com.codename1.security;

import com.codename1.junit.UITestBase;
import com.codename1.security.shield.ShieldSignal;
import com.codename1.security.shield.ShieldSignals;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage for the tapjacking / screen-overlay family of {@link DeviceIntegrity}.
 *
 * <p>Two things are being pinned down here. The first is the blocking truth table on
 * {@link TapjackingPolicy}, which is pure logic precisely so that every port makes the
 * same decision and it can be checked without a device. The second is the reporting
 * contract: ports call {@code notifyScreenObscured} for every touch they handle, so the
 * notification has to fire on a *change* of state rather than per event -- otherwise a
 * user resting a finger on an obscured screen would queue a runnable per touch onto the
 * EDT and re-raise the same signal forever.</p>
 */
class TapjackingTest extends UITestBase {

    private final List<Object> observed = new ArrayList<Object>();
    private ActionListener listener;

    @BeforeEach
    void armListener() {
        ShieldSignals.clear();
        DeviceIntegrity.setTapjackingProtection(TapjackingPolicy.OFF);
        // The implementation is shared across test classes, so the obscured state has to be
        // wound back explicitly; nothing else resets it.
        implementation.notifyScreenObscured(false, null);
        flushSerialCalls();
        observed.clear();
        listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                observed.add(evt.getSource());
            }
        };
        DeviceIntegrity.addTapjackingListener(listener);
    }

    @AfterEach
    void disarmListener() {
        DeviceIntegrity.removeTapjackingListener(listener);
        implementation.notifyScreenObscured(false, null);
        DeviceIntegrity.setTapjackingProtection(TapjackingPolicy.OFF);
        ShieldSignals.clear();
    }

    // --- policy plumbing --------------------------------------------------

    @Test
    void defaultsToOffAndAClearScreen() {
        assertSame(TapjackingPolicy.OFF, DeviceIntegrity.getTapjackingPolicy());
        assertFalse(DeviceIntegrity.isScreenObscured());
    }

    @Test
    void policyRoundTrips() {
        DeviceIntegrity.setTapjackingProtection(TapjackingPolicy.STRICT);
        assertSame(TapjackingPolicy.STRICT, DeviceIntegrity.getTapjackingPolicy());
    }

    @Test
    void aNullPolicyIsTreatedAsOffRatherThanStored() {
        // Ports read the policy on the touch path and must never have to null check it.
        DeviceIntegrity.setTapjackingProtection(TapjackingPolicy.BLOCK);
        DeviceIntegrity.setTapjackingProtection(null);
        assertSame(TapjackingPolicy.OFF, DeviceIntegrity.getTapjackingPolicy());
    }

    // --- the blocking truth table ----------------------------------------

    @Test
    void onlyOffSkipsDetection() {
        assertFalse(TapjackingPolicy.OFF.isDetecting());
        assertTrue(TapjackingPolicy.REPORT.isDetecting());
        assertTrue(TapjackingPolicy.BLOCK.isDetecting());
        assertTrue(TapjackingPolicy.STRICT.isDetecting());
    }

    @Test
    void offAndReportNeverBlock() {
        // REPORT exists to measure how often obscuring happens without changing behaviour,
        // so it must stay indistinguishable from OFF at the delivery point.
        for (TapjackingPolicy p : new TapjackingPolicy[] {
                TapjackingPolicy.OFF, TapjackingPolicy.REPORT }) {
            assertFalse(p.blocks(false, false));
            assertFalse(p.blocks(true, false));
            assertFalse(p.blocks(false, true));
            assertFalse(p.blocks(true, true));
        }
    }

    @Test
    void blockStopsAtFullyObscured() {
        // Partial obscuring is set by ordinary system UI. Blocking on it would discard taps
        // the user meant, which is why it is STRICT's problem and not BLOCK's.
        assertFalse(TapjackingPolicy.BLOCK.blocks(false, false));
        assertTrue(TapjackingPolicy.BLOCK.blocks(true, false));
        assertFalse(TapjackingPolicy.BLOCK.blocks(false, true));
        assertTrue(TapjackingPolicy.BLOCK.blocks(true, true));
    }

    @Test
    void strictAlsoBlocksPartiallyObscured() {
        assertFalse(TapjackingPolicy.STRICT.blocks(false, false));
        assertTrue(TapjackingPolicy.STRICT.blocks(true, false));
        assertTrue(TapjackingPolicy.STRICT.blocks(false, true));
        assertTrue(TapjackingPolicy.STRICT.blocks(true, true));
    }

    // --- reporting contract ----------------------------------------------

    @Test
    void anObscuredTouchFlipsTheStateNotifiesOnceAndRaisesTheSignal() {
        implementation.notifyScreenObscured(true, "obscured");
        flushSerialCalls();

        assertTrue(DeviceIntegrity.isScreenObscured());
        assertEquals(1, observed.size());
        assertEquals(Boolean.TRUE, observed.get(0));

        ShieldSignal raised = findTapjackSignal();
        assertNotNull(raised, "an observed overlay has to reach the shield signal bus");
        assertEquals(80, raised.getSeverity());
        assertEquals("obscured", raised.getDetail());
    }

    @Test
    void repeatedObscuredTouchesDoNotNotifyAgain() {
        // A port calls this per touch. Re-notifying would put a runnable on the EDT for
        // every one of them.
        implementation.notifyScreenObscured(true, "obscured");
        implementation.notifyScreenObscured(true, "obscured");
        implementation.notifyScreenObscured(true, "obscured");
        flushSerialCalls();

        assertEquals(1, observed.size());
        assertTrue(DeviceIntegrity.isScreenObscured());
    }

    @Test
    void clearingTheOverlayNotifiesAgainSoAWarningCanBeDismissed() {
        implementation.notifyScreenObscured(true, "obscured");
        flushSerialCalls();
        implementation.notifyScreenObscured(false, null);
        flushSerialCalls();

        assertFalse(DeviceIntegrity.isScreenObscured());
        assertEquals(2, observed.size());
        assertEquals(Boolean.TRUE, observed.get(0));
        assertEquals(Boolean.FALSE, observed.get(1));
    }

    @Test
    void aCleanTouchOnACleanScreenNotifiesNothing() {
        implementation.notifyScreenObscured(false, null);
        flushSerialCalls();
        assertEquals(0, observed.size());
        assertNull(findTapjackSignal());
    }

    @Test
    void aRemovedListenerStopsReceiving() {
        DeviceIntegrity.removeTapjackingListener(listener);
        implementation.notifyScreenObscured(true, "obscured");
        flushSerialCalls();

        assertEquals(0, observed.size());
        // The state and the signal are independent of the listener and must still update.
        assertTrue(DeviceIntegrity.isScreenObscured());
        assertNotNull(findTapjackSignal());
    }

    @Test
    void hideOverlayWindowsIsUnsupportedAndHarmlessOnAPortThatLacksIt() {
        // The contract every non-Android port relies on: calling it is always safe.
        assertFalse(DeviceIntegrity.isHideOverlayWindowsSupported());
        DeviceIntegrity.setHideOverlayWindows(true);
        DeviceIntegrity.setHideOverlayWindows(false);
    }

    private ShieldSignal findTapjackSignal() {
        for (ShieldSignal s : ShieldSignals.snapshot()) {
            if (ShieldSignal.TAPJACK.equals(s.getId())) {
                return s;
            }
        }
        return null;
    }
}
