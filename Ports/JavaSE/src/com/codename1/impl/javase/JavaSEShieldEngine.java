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

import com.codename1.io.Log;
import com.codename1.security.shield.AppShield;
import com.codename1.security.shield.FailureMode;
import com.codename1.security.shield.HostPolicy;
import com.codename1.security.shield.PinSet;
import com.codename1.security.shield.ShieldConfig;
import com.codename1.security.shield.ShieldException;
import com.codename1.security.shield.ShieldSignal;
import com.codename1.security.shield.ShieldStatus;
import com.codename1.security.shield.ShieldToken;
import com.codename1.security.shield.spi.EngineContext;
import com.codename1.security.shield.spi.ShieldEngine;
import com.codename1.security.shield.spi.ShieldEngineRegistry;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The engine behind {@code Simulate &gt; App Shield}.
 *
 * <p>The toggles in that menu described outcomes nothing produced: a developer could
 * switch on <b>Force Pin Mismatch</b>, watch the status dialog agree that pinning was
 * "forcing mismatch", and see every request succeed -- because no engine was registered
 * in the simulator, so the inert default answered every call and the fail-closed branch
 * the switch exists to reach was unreachable. A switch that reports a state it does not
 * cause is worse than no switch: it is a test that passes for the wrong reason.</p>
 *
 * <p>Registered on demand, the first time a developer touches that menu, and never
 * automatically -- a simulator that reported {@code isProtected()} true out of the box
 * would change what every existing app does when run there. Tokens are stamped
 * {@link #SIMULATED_MARKER} so nothing can mistake one for the real thing, and every
 * one of them says so in its own value.</p>
 */
public final class JavaSEShieldEngine implements ShieldEngine {

    /**
     * Present in every simulated token.
     *
     * <p>These are minted locally with no attestation behind them at all, so a backend
     * that ever sees one is talking to a simulator. It is in the token value rather than
     * only in a header because the value is the part that gets copied into a curl command
     * and pasted into a bug report.</p>
     */
    public static final String SIMULATED_MARKER = "cn1-simulated";

    private ShieldConfig config;
    private ShieldToken cached;

    /**
     * Installs the simulator engine, once, if nothing has claimed the slot.
     *
     * <p>Called when a shield simulation is switched on -- including one restored from
     * preferences at startup, because a developer who left "Force Pin Mismatch" armed
     * expects it to still be armed. Not called merely because the menu exists: the
     * registry seals on first registration and a simulator that always reported
     * {@code isProtected()} true would change what every app does when run there.</p>
     *
     * <p>The engine is initialized here as well as by
     * {@code AppShield.init(ShieldConfig)}, because the two can happen in either order.
     * An app that called {@code init()} at startup and a developer who opens the menu
     * afterwards would otherwise leave this holding a null config -- and a null config
     * means no pinned hosts, which means {@code verifyPins()} is never reached and the
     * force switch quietly does nothing. That is the exact failure this class exists to
     * remove, so it must not be reintroduced by ordering.</p>
     */
    public static void ensureRegistered() {
        if (ShieldEngineRegistry.isEngineRegistered()) {
            return;
        }
        JavaSEShieldEngine engine = new JavaSEShieldEngine();
        try {
            ShieldEngineRegistry.setEngine(engine);
        } catch (IllegalStateException alreadySealed) {
            // A real engine got there first -- a build-server bootstrap, or a test. The
            // simulator does not displace it.
            return;
        } catch (RuntimeException other) {
            Log.e(other);
            return;
        }
        try {
            engine.initialize(ShieldEngineRegistry.getDefaultContext(),
                    AppShield.getConfig());
        } catch (RuntimeException e) {
            Log.e(e);
        }
    }

    public String getName() {
        return "simulator";
    }

    public boolean isAvailable() {
        return JavaSEShield.attestationSupported;
    }

    public void initialize(EngineContext ctx, ShieldConfig cfg) {
        this.config = cfg;
    }

    public ShieldToken fetchToken(String bindingData) throws ShieldException {
        if (!JavaSEShield.attestationSupported) {
            throw new ShieldException(ShieldStatus.UNPROTECTED,
                    "The simulated platform reports no attestation support");
        }
        switch (JavaSEShield.attestOutcome) {
            case FAIL_REJECTED:
                throw new ShieldException(ShieldStatus.REJECTED,
                        "Simulated: the service rejected this device");
            case FAIL_NO_NETWORK:
                throw new ShieldException(ShieldStatus.NO_NETWORK,
                        "Simulated: no network");
            case FAIL_SERVICE_DOWN:
                throw new ShieldException(ShieldStatus.SERVICE_DOWN,
                        "Simulated: the attestation service is down");
            case FAIL_RATE_LIMITED:
                throw new ShieldException(ShieldStatus.RATE_LIMITED,
                        "Simulated: rate limited");
            case UNSUPPORTED:
                throw new ShieldException(ShieldStatus.UNPROTECTED,
                        "Simulated: this platform has no attestation");
            default:
                break;
        }
        long ttl = (long) Math.max(1, JavaSEShield.tokenTtlSeconds) * 1000L;
        long fetchedAt = System.currentTimeMillis();
        if (JavaSEShield.serveExpiredToken) {
            // Handed out already lapsed rather than with a short lifetime, so a test does
            // not have to wait for it. isValid() is answered from a monotonic reading
            // taken at construction, so backdating fetchedAt alone would not do it.
            ttl = 0L;
        }
        cached = new ShieldToken(SIMULATED_MARKER + "." + Long.toHexString(fetchedAt)
                + (bindingData == null ? "" : "." + Integer.toHexString(bindingData.hashCode())),
                ShieldStatus.OK, fetchedAt, ttl, bindingData);
        return cached;
    }

    public ShieldToken getCachedToken() {
        return cached;
    }

    public boolean verifyPins(String host, String[] spkiDigests, String[] certDigests) {
        if (!JavaSEShield.forcePinMismatch) {
            return true;
        }
        // One shot, because the switch is labelled "on next request". Leaving it armed
        // would fail every subsequent request too, and a developer testing a recovery
        // path would be testing a permanently broken app instead.
        JavaSEShield.forcePinMismatch = false;
        return false;
    }

    public PinSet getPinSet() {
        if (JavaSEShield.failPinFetch) {
            // An unavailable pin set is not a mismatch. Pinning fails OPEN on
            // unavailability everywhere in this design, and the simulator has to be able
            // to demonstrate that rather than assert it.
            return new PinSet(new Hashtable(), 0, 0L, 0L);
        }
        // Every host the app registered, pinned to a digest no real chain can produce.
        // Enforcement is what makes verifyPins() run at all, so without this the force
        // switch would still have nothing to act on: PinSet.isEnforcedFor() is false for
        // a host with no pins, and ShieldNetworkGuard checks that before asking.
        Hashtable hostToPins = new Hashtable();
        if (config != null) {
            java.util.Enumeration hosts = config.protectedHosts();
            while (hosts.hasMoreElements()) {
                String host = (String) hosts.nextElement();
                if (host == null) {
                    continue;
                }
                // Wildcards included. PinSet.isEnforcedFor() resolves "api.example.com"
                // against a "*.example.com" entry, so skipping them left every app that
                // registers its hosts by pattern -- which is the common way to do it --
                // with nothing enforced and the force switch doing nothing, which is the
                // whole failure this engine exists to remove.
                Vector pins = new Vector();
                pins.addElement(SIMULATED_PIN);
                hostToPins.put(host, pins);
            }
        }
        long now = System.currentTimeMillis();
        return new PinSet(hostToPins, 1, now + DAY_MILLIS, now + 30L * DAY_MILLIS);
    }

    public ShieldSignal[] collectSignals() {
        String[] reasons = JavaSEShield.simReasons();
        Vector out = new Vector();
        for (int i = 0; i < reasons.length; i++) {
            out.addElement(new ShieldSignal(reasons[i], severityFor(reasons[i]),
                    "simulated"));
        }
        String[] accessibility = JavaSEShield.simAccessibility();
        for (int i = 0; i < accessibility.length; i++) {
            out.addElement(new ShieldSignal(ShieldSignal.ACCESSIBILITY, 60,
                    accessibility[i]));
        }
        ShieldSignal[] arr = new ShieldSignal[out.size()];
        out.copyInto(arr);
        return arr;
    }

    public void invalidate() {
        cached = null;
    }

    public void shutdown() {
        cached = null;
    }

    private static int severityFor(String reason) {
        if (ShieldSignal.HOOK.equals(reason) || "frida".equals(reason)) {
            return 90;
        }
        if (ShieldSignal.ROOT.equals(reason) || ShieldSignal.JAILBREAK.equals(reason)) {
            return 70;
        }
        if (ShieldSignal.REPACKAGED.equals(reason)) {
            return 80;
        }
        if (ShieldSignal.DEBUGGER.equals(reason)) {
            return 50;
        }
        return 30;
    }

    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    /**
     * A digest no live chain can match, so an enforced host fails when the switch is on.
     *
     * <p>Deliberately not a real digest: if it ever collided with a host's actual key the
     * force switch would silently stop working, which is the failure mode this whole
     * class exists to remove.</p>
     */
    private static final String SIMULATED_PIN =
            "c2ltdWxhdGVkLXBpbi1uby1yZWFsLWNoYWluLW1hdGNoZXMtdGhpcw==";

    /**
     * The policy a simulated host gets when the app registered none, so the menu's
     * pinning switches have something to act on even in an app that only called
     * {@code AppShield.init()}.
     */
    static HostPolicy simulatedPolicy() {
        return new HostPolicy(true, true, FailureMode.CLOSED);
    }
}
