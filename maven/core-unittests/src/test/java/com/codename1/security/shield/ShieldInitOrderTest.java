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
import com.codename1.util.AsyncResource;
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

    /**
     * An interrupt while waiting for startup does not turn a fail-closed host into an
     * open one.
     *
     * <p>The wait used to return quietly on interrupt, which is indistinguishable from
     * "initialization finished" -- the caller then saw the shield as uninitialized, took
     * its early return, and the request went out with no token and no pin check.
     * {@code ConnectionRequest} does not consult the interrupt flag either, so nothing
     * further down stopped it. The one request that must never leave unprotected is
     * exactly this one.</p>
     */
    @Test
    void anInterruptedWaitStillRefusesAFailClosedRequest() throws Exception {
        Thread init = initOnAnotherThread();
        assertTrue(engine.entered.await(GENEROUS_TIMEOUT_MS, TimeUnit.MILLISECONDS),
                "the engine should have been asked to initialize");

        final NetworkGuard guard = NetworkManager.getNetworkGuard();
        final RecordingRequest request = new RecordingRequest();
        request.setUrl("https://api.example.com/secure");
        final CountDownLatch done = new CountDownLatch(1);
        final AtomicReference<Throwable> outcome = new AtomicReference<Throwable>();
        Thread caller = new Thread(new Runnable() {
            public void run() {
                try {
                    guard.beforeRequest(request);
                } catch (Throwable t) {
                    outcome.set(t);
                }
                done.countDown();
            }
        }, "shield-interrupted-request");
        caller.setDaemon(true);
        caller.start();

        // Let it park in the wait, then interrupt it there.
        assertFalse(done.await(BLOCKED_OBSERVATION_MS, TimeUnit.MILLISECONDS));
        caller.interrupt();

        assertTrue(done.await(GENEROUS_TIMEOUT_MS, TimeUnit.MILLISECONDS),
                "the interrupt has to end the wait");
        assertNull(request.attached(),
                "nothing can be attached without an initialized shield");
        assertTrue(outcome.get() instanceof ShieldException,
                "a fail-closed host must refuse the request rather than let it go out "
                + "unprotected, got: " + outcome.get());
        assertEquals(ShieldStatus.NOT_INITIALIZED,
                ((ShieldException) outcome.get()).getStatus());

        engine.release();
        init.join(GENEROUS_TIMEOUT_MS);
    }

    /**
     * Renaming the token header does not leave the previous one on a reused request.
     *
     * <p>{@code ShieldConfig} is mutable and {@code getConfig()} hands out the live
     * instance, so the name that has to be cleared is the one the header was SET under,
     * which may no longer be the configured one. Clearing only the current name left a
     * bearer token in the request under its old name -- and a redirect to an unprotected
     * host carried it there, which is precisely what the clearing exists to prevent.</p>
     */
    @Test
    void renamingTheTokenHeaderStillClearsTheOneAlreadyAttached() throws Exception {
        Thread init = initOnAnotherThread();
        engine.release();
        init.join(GENEROUS_TIMEOUT_MS);
        assertFalse(init.isAlive());

        RecordingRequest request = new RecordingRequest();
        request.setUrl("https://api.example.com/secure");
        AppShield.attach(request);
        assertEquals("token-from-a-fully-initialized-engine", request.attached(),
                "the fixture must actually attach something, or this proves nothing");

        // The app renames its header between attempts, then the request is redirected to
        // a host the shield does not protect.
        AppShield.getConfig().tokenHeader("X-Other-Attest");
        request.setUrl("https://unprotected.example.com/elsewhere");
        AppShield.attach(request);

        assertNull(request.headerValue("X-CN1-Attest"),
                "the token attached under the old name must not survive to another host");
        assertNull(request.headerValue("X-Other-Attest"));
    }

    /**
     * The asynchronous fetch does not become synchronous during startup.
     *
     * <p>{@code fetchToken()} is documented asynchronous, and it waited for
     * initialization before returning its {@code AsyncResource} -- so a caller on the EDT
     * froze for the length of a cold start, and an engine whose own initialization needs
     * anything dispatched to the EDT deadlocked outright: the EDT parked waiting for the
     * initialization that is waiting for the EDT.</p>
     */
    @Test
    void fetchTokenReturnsWhileInitializationIsStillRunning() throws Exception {
        Thread init = initOnAnotherThread();
        assertTrue(engine.entered.await(GENEROUS_TIMEOUT_MS, TimeUnit.MILLISECONDS),
                "the engine should have been asked to initialize");

        final AsyncResource<ShieldToken>[] handle = new AsyncResource[1];
        final CountDownLatch returned = new CountDownLatch(1);
        Thread caller = new Thread(new Runnable() {
            public void run() {
                handle[0] = AppShield.fetchToken();
                returned.countDown();
            }
        }, "shield-fetch-during-init");
        caller.setDaemon(true);
        caller.start();

        assertTrue(returned.await(GENEROUS_TIMEOUT_MS, TimeUnit.MILLISECONDS),
                "fetchToken must hand back its resource without waiting for startup");
        assertNotNull(handle[0]);
        assertFalse(handle[0].isDone(),
                "and it cannot be finished yet -- the engine has not started");

        engine.release();
        init.join(GENEROUS_TIMEOUT_MS);
        long deadline = System.currentTimeMillis() + GENEROUS_TIMEOUT_MS;
        while (!handle[0].isDone() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10L);
        }
        assertTrue(handle[0].isDone(), "the resource has to settle once startup finishes");
        assertEquals("token-from-a-fully-initialized-engine", handle[0].get().getValue());
    }

    /**
     * The cleanup only touches the request the shield actually decorated.
     *
     * <p>The remembered names were process-global, so every request lost them -- and an
     * app whose token header is also one an unprotected service legitimately expects
     * ({@code X-API-Key} being the obvious case) found the shield quietly deleting that
     * service's header on a request the shield has nothing to do with. Removing a header
     * an app set itself is a bug that presents as the other service rejecting the call.</p>
     */
    @Test
    void anotherRequestKeepsAHeaderTheAppSetItself() throws Exception {
        Thread init = initOnAnotherThread();
        engine.release();
        init.join(GENEROUS_TIMEOUT_MS);

        RecordingRequest protectedRequest = new RecordingRequest();
        protectedRequest.setUrl("https://api.example.com/secure");
        AppShield.attach(protectedRequest);
        assertEquals("token-from-a-fully-initialized-engine", protectedRequest.attached());

        // A different request, to a host the shield does not protect, carrying a header
        // of the app's own that happens to share the token header's name.
        RecordingRequest ownRequest = new RecordingRequest();
        ownRequest.setUrl("https://elsewhere.example.com/thing");
        ownRequest.addRequestHeader("X-CN1-Attest", "the-app-put-this-here");
        AppShield.attach(ownRequest);

        assertEquals("the-app-put-this-here", ownRequest.headerValue("X-CN1-Attest"),
                "the shield must not strip a header it did not attach to this request");
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

        private final java.util.Map<String, String> headers =
                new java.util.LinkedHashMap<String, String>();

        @Override
        public void addRequestHeader(String key, String value) {
            super.addRequestHeader(key, value);
            headers.put(key, value);
        }

        @Override
        public void removeRequestHeader(String key) {
            super.removeRequestHeader(key);
            headers.remove(key);
        }

        String attached() {
            return headers.get("X-CN1-Attest");
        }

        String headerValue(String name) {
            return headers.get(name);
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
