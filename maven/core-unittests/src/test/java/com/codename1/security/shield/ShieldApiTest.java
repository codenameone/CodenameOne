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
package com.codename1.security.shield;

import org.junit.jupiter.api.Test;

import java.util.Hashtable;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the pure value types of the shield API. The behaviours asserted here are
 * the ones an app relies on to decide whether to block a user, so a regression in
 * any of them is a production incident rather than a cosmetic bug.
 */
class ShieldApiTest {

    // --- ShieldStatus ---------------------------------------------------

    @Test
    void onlyOkIsSuccess() {
        assertTrue(ShieldStatus.OK.isSuccess());
        assertFalse(ShieldStatus.UNPROTECTED.isSuccess());
        assertFalse(ShieldStatus.REJECTED.isSuccess());
        assertFalse(ShieldStatus.PIN_MISMATCH.isSuccess());
    }

    /**
     * The distinction the whole API is built around: a service we could not reach
     * is retryable, a device the service refused is not.
     */
    @Test
    void reachabilityFailuresAreTransientButRejectionIsNot() {
        assertTrue(ShieldStatus.NO_NETWORK.isTransient());
        assertTrue(ShieldStatus.POOR_NETWORK.isTransient());
        assertTrue(ShieldStatus.SERVICE_DOWN.isTransient());
        assertTrue(ShieldStatus.RATE_LIMITED.isTransient());

        assertFalse(ShieldStatus.REJECTED.isTransient());
        assertFalse(ShieldStatus.PIN_MISMATCH.isTransient());
        assertFalse(ShieldStatus.UNPROTECTED.isTransient());
        assertFalse(ShieldStatus.OK.isTransient());
    }

    @Test
    void unknownStatusIdRoundTripsAsANonSuccess() {
        ShieldStatus s = ShieldStatus.forId("somethingThisBuildPredates");
        assertEquals("somethingThisBuildPredates", s.getId());
        assertFalse(s.isSuccess());
        assertSame(ShieldStatus.RATE_LIMITED, ShieldStatus.forId("rateLimited"));
        assertSame(ShieldStatus.NOT_INITIALIZED, ShieldStatus.forId(null));
    }

    /**
     * ShieldException carries the status id rather than the object, because
     * IOException is serializable. getStatus() must still hand back the
     * canonical constant, or identity checks like isTransient() silently stop
     * working.
     */
    @Test
    void exceptionStatusResolvesBackToTheCanonicalConstant() {
        ShieldException e = new ShieldException(ShieldStatus.RATE_LIMITED, "slow down");
        assertSame(ShieldStatus.RATE_LIMITED, e.getStatus());
        assertTrue(e.getStatus().isTransient());

        ShieldException pin = new ShieldException(ShieldStatus.PIN_MISMATCH, "bad chain");
        assertSame(ShieldStatus.PIN_MISMATCH, pin.getStatus());
        assertFalse(pin.getStatus().isTransient());
    }

    @Test
    void exceptionStatusIsNeverNull() {
        assertSame(ShieldStatus.NOT_INITIALIZED, new ShieldException(null, "x").getStatus());
    }

    // --- ShieldToken ----------------------------------------------------

    @Test
    void tokenIsValidOnlyWhileItHasValueSuccessAndTime() {
        long now = System.currentTimeMillis();
        assertTrue(new ShieldToken("t", ShieldStatus.OK, now, 60000, null).isValid());
        assertFalse(new ShieldToken(null, ShieldStatus.OK, now, 60000, null).isValid());
        assertFalse(new ShieldToken("t", ShieldStatus.REJECTED, now, 60000, null).isValid());
        assertFalse(new ShieldToken("t", ShieldStatus.OK, now - 120000, 60000, null).isValid());
    }

    @Test
    void expiredTokenReportsZeroRatherThanNegativeRemainingTime() {
        ShieldToken t = new ShieldToken("t", ShieldStatus.OK,
                System.currentTimeMillis() - 120000, 60000, null);
        assertEquals(0, t.getMillisUntilExpiry());
    }

    @Test
    void refreshTriggersOnceThresholdShareOfLifetimeIsUsed() {
        long now = System.currentTimeMillis();
        // 10% used, 50% threshold -> no refresh yet.
        assertFalse(new ShieldToken("t", ShieldStatus.OK, now - 1000, 10000, null)
                .shouldRefresh(50));
        // 60% used -> refresh.
        assertTrue(new ShieldToken("t", ShieldStatus.OK, now - 6000, 10000, null)
                .shouldRefresh(50));
    }

    @Test
    void aBoundTokenIsNotReusableForOtherRequests() {
        ShieldToken bound = new ShieldToken("t", ShieldStatus.OK,
                System.currentTimeMillis(), 60000, "digest-a");
        assertTrue(bound.isBoundTo("digest-a"));
        assertFalse(bound.isBoundTo("digest-b"));
        assertFalse(bound.isBoundTo(null));

        ShieldToken unbound = new ShieldToken("t", ShieldStatus.OK,
                System.currentTimeMillis(), 60000, null);
        assertTrue(unbound.isBoundTo(null));
        assertFalse(unbound.isBoundTo("digest-a"));
    }

    @Test
    void tokenToStringNeverRendersTheTokenValue() {
        String s = new ShieldToken("super-secret-token-value", ShieldStatus.OK,
                System.currentTimeMillis(), 60000, null).toString();
        assertFalse(s.contains("super-secret-token-value"),
                "token values must not be loggable via toString");
    }

    // --- HostPolicy / ShieldConfig --------------------------------------

    @Test
    void unregisteredHostsAreLeftCompletelyAlone() {
        ShieldConfig c = new ShieldConfig().protect("api.example.com");
        assertSame(HostPolicy.UNPROTECTED, c.policyFor("cdn.other.com"));
        assertTrue(HostPolicy.UNPROTECTED.isNoOp());
        assertSame(HostPolicy.UNPROTECTED, c.policyFor(null));
    }

    @Test
    void wildcardCoversSubdomainsButNotTheApexOrSiblings() {
        ShieldConfig c = new ShieldConfig().protect("*.example.com", HostPolicy.ENFORCED);
        assertSame(HostPolicy.ENFORCED, c.policyFor("api.example.com"));
        assertSame(HostPolicy.ENFORCED, c.policyFor("a.b.example.com"));
        assertSame(HostPolicy.UNPROTECTED, c.policyFor("example.com"));
        assertSame(HostPolicy.UNPROTECTED, c.policyFor("example.com.evil.test"));
    }

    @Test
    void exactHostWinsOverWildcard() {
        ShieldConfig c = new ShieldConfig()
                .protect("*.example.com", HostPolicy.PROTECTED)
                .protect("secure.example.com", HostPolicy.ENFORCED);
        assertSame(HostPolicy.ENFORCED, c.policyFor("secure.example.com"));
        assertSame(HostPolicy.PROTECTED, c.policyFor("other.example.com"));
    }

    @Test
    void hostMatchingIsCaseInsensitive() {
        ShieldConfig c = new ShieldConfig().protect("API.Example.COM");
        assertSame(HostPolicy.PROTECTED, c.policyFor("api.example.com"));
    }

    @Test
    void defaultsAreOpenAndNonBlocking() {
        ShieldConfig c = new ShieldConfig();
        assertEquals(FailureMode.OPEN, c.getDefaultFailureMode());
        assertEquals(ShieldConfig.DEFAULT_TOKEN_HEADER, c.getTokenHeader());
        assertFalse(c.hasProtectedHosts());
        assertTrue(c.isCollectSignals());
        assertEquals(FailureMode.OPEN, HostPolicy.PROTECTED.getFailureMode());
    }

    // --- PinSet ---------------------------------------------------------

    private static PinSet pinSet(String host, String pin, long soft, long hard) {
        Hashtable t = new Hashtable();
        Vector v = new Vector();
        v.addElement(pin);
        t.put(host, v);
        return new PinSet(t, 1, soft, hard);
    }

    /**
     * The never-brick rule. An unpinned host must report a match, or every request
     * to it would start failing the moment pinning was switched on anywhere.
     */
    @Test
    void unpinnedHostAlwaysMatches() {
        PinSet set = pinSet("api.example.com", "AAA", 0, 0);
        assertTrue(set.matches("other.example.com", new String[] {"ZZZ"}));
        assertTrue(PinSet.EMPTY.matches("api.example.com", new String[] {"ZZZ"}));
        assertFalse(PinSet.EMPTY.isEnforcedFor("api.example.com"));
    }

    @Test
    void pinnedHostMatchesAnywhereInTheChain() {
        PinSet set = pinSet("api.example.com", "INTERMEDIATE", 0, 0);
        assertTrue(set.matches("api.example.com",
                new String[] {"LEAF", "INTERMEDIATE", "ROOT"}));
    }

    @Test
    void pinnedHostWithNoMatchingChainEntryFails() {
        PinSet set = pinSet("api.example.com", "AAA", 0, 0);
        assertFalse(set.matches("api.example.com", new String[] {"BBB", "CCC"}));
        assertFalse(set.matches("api.example.com", new String[0]));
        assertFalse(set.matches("api.example.com", null));
    }

    /**
     * A device that cannot reach the service for a long time must lose pinning
     * rather than lose the app.
     */
    @Test
    void hardExpiredPinSetStopsEnforcingEntirely() {
        long past = System.currentTimeMillis() - 1000;
        PinSet expired = pinSet("api.example.com", "AAA", past, past);
        assertTrue(expired.isExpired());
        assertFalse(expired.isEnforcedFor("api.example.com"));
        assertTrue(expired.matches("api.example.com", new String[] {"WRONG"}));
    }

    @Test
    void staleButNotExpiredPinSetKeepsEnforcing() {
        long now = System.currentTimeMillis();
        PinSet stale = pinSet("api.example.com", "AAA", now - 1000, now + 600000);
        assertTrue(stale.isStale());
        assertFalse(stale.isExpired());
        assertTrue(stale.isEnforcedFor("api.example.com"));
        assertFalse(stale.matches("api.example.com", new String[] {"WRONG"}));
    }

    @Test
    void pinWildcardsFollowTheSameRulesAsHostPolicies() {
        PinSet set = pinSet("*.example.com", "AAA", 0, 0);
        assertTrue(set.isEnforcedFor("api.example.com"));
        assertFalse(set.isEnforcedFor("example.com"));
        assertNull(set.pinsFor("other.test"));
    }

    // --- ShieldSignals --------------------------------------------------

    @Test
    void repeatedSignalsOfTheSameKindCollapseInsteadOfAccumulating() {
        ShieldSignals.clear();
        for (int i = 0; i < 500; i++) {
            ShieldSignals.add(ShieldSignal.HOOK, 90, "attempt " + i);
        }
        ShieldSignal[] snapshot = ShieldSignals.snapshot();
        assertEquals(1, snapshot.length,
                "a detector firing every frame must not be able to grow the bus");
        assertEquals("attempt 499", snapshot[0].getDetail());
        ShieldSignals.clear();
    }

    @Test
    void signalBusIsBoundedAcrossDistinctIds() {
        ShieldSignals.clear();
        for (int i = 0; i < 200; i++) {
            ShieldSignals.add("signal-" + i, 10, null);
        }
        assertTrue(ShieldSignals.snapshot().length <= 32);
        ShieldSignals.clear();
    }

    @Test
    void severityIsClampedToTheDocumentedRange() {
        assertEquals(100, new ShieldSignal("x", 5000, null).getSeverity());
        assertEquals(0, new ShieldSignal("x", -5, null).getSeverity());
    }

    @Test
    void hasSignalAtLeastReflectsRecordedSeverities() {
        ShieldSignals.clear();
        assertFalse(ShieldSignals.hasSignalAtLeast(1));
        ShieldSignals.add(ShieldSignal.EMULATOR, 30, null);
        assertTrue(ShieldSignals.hasSignalAtLeast(30));
        assertFalse(ShieldSignals.hasSignalAtLeast(31));
        ShieldSignals.clear();
    }
}
