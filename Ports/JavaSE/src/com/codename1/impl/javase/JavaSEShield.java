/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.javase;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulator state behind the {@code Simulate > App Shield} menu.
 *
 * <p>Exists so a developer can exercise the branches an app takes when a device
 * looks compromised or attestation fails. Those are exactly the paths that are
 * otherwise impossible to test: rooting a phone, attaching a hooking framework
 * or deliberately mis-pinning a real host are all things nobody does casually,
 * so without simulation the error handling ships untested and is discovered by
 * users.</p>
 *
 * <p>Mirrors {@link JavaSEBiometrics}: plain static fields driven by menu
 * checkboxes and persisted to preferences, deliberately with no logic of its
 * own.</p>
 */
public final class JavaSEShield {

    private JavaSEShield() {
    }

    // --- attestation ------------------------------------------------------

    /** What the next attestation request should do. */
    public enum AttestOutcome {
        /** Return a simulated token. */
        PASS,
        /** The service evaluated the device and said no. Not retryable. */
        FAIL_REJECTED,
        /** No connectivity. Retryable. */
        FAIL_NO_NETWORK,
        /** The service returned an error. Retryable. */
        FAIL_SERVICE_DOWN,
        /** The device is being throttled. Retryable after backoff. */
        FAIL_RATE_LIMITED,
        /** The platform has no attestation at all. */
        UNSUPPORTED
    }

    public static AttestOutcome attestOutcome = AttestOutcome.PASS;

    /** Whether the simulated platform reports attestation support. */
    public static boolean attestationSupported = true;

    // --- device signals ---------------------------------------------------

    public static boolean simRooted;
    public static boolean simHooked;
    public static boolean simEmulator;
    public static boolean simDebugger;
    public static boolean simRepackaged;
    public static boolean simUntrustedAccessibility;

    // --- token ------------------------------------------------------------

    /** Simulated token lifetime. */
    public static int tokenTtlSeconds = 300;

    /** Hand out a token that has already lapsed, to exercise refresh handling. */
    public static boolean serveExpiredToken;

    // --- pinning ----------------------------------------------------------

    /**
     * Fail the next certificate check.
     *
     * <p>The most useful switch here by a distance: it is the only practical way
     * to test a fail-closed branch, short of deliberately mis-pinning a live
     * host and waiting for the request to break.</p>
     */
    public static boolean forcePinMismatch;

    /** Simulate being unable to fetch a pin set. Must never fail a request. */
    public static boolean failPinFetch;

    /** True when the window is displaying a screen marked secure. */
    public static boolean secureScreen;

    /** The compromise reasons the simulated device reports. */
    public static String[] simReasons() {
        List<String> out = new ArrayList<String>();
        if (simRooted) {
            out.add("root");
        }
        if (simHooked) {
            out.add("frida");
        }
        if (simEmulator) {
            out.add("emulator");
        }
        if (simDebugger) {
            out.add("debugger");
        }
        if (simRepackaged) {
            out.add("repackaged");
        }
        return out.toArray(new String[out.size()]);
    }

    /** The accessibility services the simulated device reports as enabled. */
    public static String[] simAccessibility() {
        if (!simUntrustedAccessibility) {
            return new String[0];
        }
        return new String[] {"com.example.malware/.OverlayService"};
    }

    /** Resets every toggle. Used by the menu's reset item and by tests. */
    public static void reset() {
        attestOutcome = AttestOutcome.PASS;
        attestationSupported = true;
        simRooted = false;
        simHooked = false;
        simEmulator = false;
        simDebugger = false;
        simRepackaged = false;
        simUntrustedAccessibility = false;
        tokenTtlSeconds = 300;
        serveExpiredToken = false;
        forcePinMismatch = false;
        failPinFetch = false;
    }

    /** A human-readable dump for the menu's status dialog. */
    public static String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append("Attestation outcome: ").append(attestOutcome).append('\n');
        sb.append("Attestation supported: ").append(attestationSupported).append('\n');
        String[] reasons = simReasons();
        sb.append("Device signals: ")
          .append(reasons.length == 0 ? "(none)" : String.join(", ", reasons)).append('\n');
        sb.append("Accessibility: ")
          .append(simUntrustedAccessibility ? "untrusted service enabled" : "clean").append('\n');
        sb.append("Token TTL: ").append(tokenTtlSeconds).append("s")
          .append(serveExpiredToken ? " (serving expired)" : "").append('\n');
        sb.append("Pinning: ")
          .append(forcePinMismatch ? "forcing mismatch" : "normal")
          .append(failPinFetch ? ", pin fetch failing" : "").append('\n');
        sb.append("Secure screen: ").append(secureScreen).append('\n');
        return sb.toString();
    }
}
