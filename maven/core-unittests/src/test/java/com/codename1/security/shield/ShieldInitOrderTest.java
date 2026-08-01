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

import com.codename1.io.ConnectionRequest;
import com.codename1.junit.UITestBase;
import com.codename1.io.NetworkGuard;
import com.codename1.io.NetworkGuardTestAccess;
import com.codename1.io.NetworkManager;
import com.codename1.security.shield.spi.EngineContext;
import com.codename1.security.shield.spi.ShieldEngine;
import com.codename1.security.shield.spi.ShieldEngineRegistry;
import com.codename1.security.shield.spi.ShieldEngineTestAccess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a request that starts while {@code AppShield.init()} is still running gets.
 *
 * <p>The answer has to be "the shield, a moment later" and never "no shield at all". A
 * cold start is the one moment when an app fires its first protected request and when
 * initialization takes longest, so the window is not theoretical -- it is the common
 * case. Both bugs this covers were invisible in normal use: everything succeeded, the
 * request simply left without a token and without a pin check.</p>
 */
class ShieldInitOrderTest extends UITestBase {

    /** Long enough that a real regression cannot pass by being fast. */
    private static final long GENEROUS_TIMEOUT_MS = 10000L;

    /** Short: it bounds how long a *correct* implementation is observed blocking. */
    private static final long BLOCKED_OBSERVATION_MS = 300L;

    private SlowEngine engine;

    @BeforeEach
    void setUpShield() {
        NetworkGuardTestAccess.reset();
        ShieldEngineTestAccess.reset();
        AppShield.resetForTesting();
        engine = new SlowEngine();
    }

    @AfterEach
    void tearDownShield() {
        engine.release();
        NetworkGuardTestAccess.reset();
        ShieldEngineTestAccess.reset();
        AppShield.resetForTesting();
    }

    /**
     * The guard exists before the engine is given a chance to run.
     *
     * <p>Publishing an "initialization in progress" flag is not enough on its own. A
     * concurrent request only reaches the code that waits on that flag if something routes
     * it there, and the only thing that does is the network guard. Installed after
     * {@code engine.initialize()}, the guard did not exist yet, so
     * {@code ConnectionRequest.performOperationComplete()} found none and opened the
     * connection directly -- no token, no pin check, for as long as the engine took to
     * start.</p>
     */
    @Test
    void aRequestStartingDuringInitializationFindsAGuardRatherThanNone() throws Exception {
        Thread init = initOnAnotherThread();
        assertTrue(engine.entered.await(GENEROUS_TIMEOUT_MS, TimeUnit.MILLISECONDS),
                "the engine should have been asked to initialize");

        assertNotNull(NetworkManager.getNetworkGuard(),
                "a request starting now would find no guard and skip the shield entirely");

        engine.release();
        init.join(GENEROUS_TIMEOUT_MS);
        assertFalse(init.isAlive(), "init() should have finished");
    }

    /**
     * And going through that guard waits for initialization rather than sailing past it.
     *
     * <p>Installing the guard early is only safe because every path through it waits: the
     * alternative -- a guard that answers while the engine is half-built -- would attach
     * nothing and report success, which is the same hole one layer down.</p>
     */
    @Test
    void thatGuardBlocksTheRequestUntilInitializationFinishes() throws Exception {
        Thread init = initOnAnotherThread();
        assertTrue(engine.entered.await(GENEROUS_TIMEOUT_MS, TimeUnit.MILLISECONDS),
                "the engine should have been asked to initialize");

        NetworkGuard guard = NetworkManager.getNetworkGuard();
        assertNotNull(guard);

        final RecordingRequest request = new RecordingRequest();
        request.setUrl("https://api.example.com/secure");
        final CountDownLatch done = new CountDownLatch(1);
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        final NetworkGuard target = guard;
        Thread caller = new Thread(new Runnable() {
            public void run() {
                try {
                    target.beforeRequest(request);
                } catch (Throwable t) {
                    failure.set(t);
                }
                done.countDown();
            }
        }, "shield-init-order-request");
        caller.setDaemon(true);
        caller.start();

        assertFalse(done.await(BLOCKED_OBSERVATION_MS, TimeUnit.MILLISECONDS),
                "the request must wait for initialization, not proceed without a token");
        assertNull(request.attached(),
                "and nothing should have been attached while the engine was still starting");

        engine.release();
        assertTrue(done.await(GENEROUS_TIMEOUT_MS, TimeUnit.MILLISECONDS),
                "the wait has to end when initialization does");
        assertNull(failure.get(), "the request should have succeeded: " + failure.get());
        assertEquals("token-from-a-fully-initialized-engine", request.attached(),
                "once initialization finished the token belongs on the request");

        init.join(GENEROUS_TIMEOUT_MS);
        assertFalse(init.isAlive(), "init() should have finished");
    }

    private Thread initOnAnotherThread() {
        ShieldEngineRegistry.setEngine(engine);
        Thread t = new Thread(new Runnable() {
            public void run() {
                AppShield.init(new ShieldConfig()
                        .protect("api.example.com", HostPolicy.ENFORCED));
            }
        }, "shield-init-order-init");
        t.setDaemon(true);
        t.start();
        return t;
    }

    /**
     * Records what the shield attached. {@code ConnectionRequest} has no header getter, and
     * adding one to the public API to serve a test would be the wrong direction.
     */
    private static final class RecordingRequest extends ConnectionRequest {

        private String token;

        @Override
        public void addRequestHeader(String key, String value) {
            super.addRequestHeader(key, value);
            if ("X-CN1-Attest".equals(key)) {
                token = value;
            }
        }

        @Override
        public void removeRequestHeader(String key) {
            super.removeRequestHeader(key);
            if ("X-CN1-Attest".equals(key)) {
                token = null;
            }
        }

        String attached() {
            return token;
        }
    }

    /** An engine whose {@code initialize()} is held open, which is the whole window. */
    private static final class SlowEngine implements ShieldEngine {

        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch proceed = new CountDownLatch(1);

        void release() {
            proceed.countDown();
        }

        public String getName() {
            return "slow-test-engine";
        }

        public boolean isAvailable() {
            return true;
        }

        public void initialize(EngineContext ctx, ShieldConfig config) {
            entered.countDown();
            try {
                proceed.await(GENEROUS_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public ShieldToken fetchToken(String bindingData) {
            return new ShieldToken("token-from-a-fully-initialized-engine",
                    ShieldStatus.OK, System.currentTimeMillis(), 300000L, bindingData);
        }

        public ShieldToken getCachedToken() {
            return null;
        }

        public boolean verifyPins(String host, String[] spkiDigests, String[] certDigests) {
            return true;
        }

        public PinSet getPinSet() {
            return PinSet.EMPTY;
        }

        public ShieldSignal[] collectSignals() {
            return new ShieldSignal[0];
        }

        public void invalidate() {
        }

        public void shutdown() {
        }
    }
}
