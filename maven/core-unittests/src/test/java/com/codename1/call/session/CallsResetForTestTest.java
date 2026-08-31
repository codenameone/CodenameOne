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
package com.codename1.call.session;

import com.codename1.call.CallHandle;
import com.codename1.impl.call.CallRequests;
import com.codename1.impl.call.LocalCallBridge;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reset between tests settles what it strands.
 *
 * <p>{@code resetForTest} cleared the listeners and the sessions and left
 * {@code PENDING_STARTS} alone. A test that deferred a {@code START} action
 * and reset without answering it therefore handed the next test a live action
 * and a running safety timer. Both address a call by id or by token, and each
 * test gets a fresh {@code LocalCallBridge} whose tokens restart at 1 -- so a
 * stale report or a stale timer could claim or fail an action belonging to a
 * test that had not begun when it was created.</p>
 *
 * <p>That makes the suite order-dependent, and an order-dependent suite is
 * where "flaky" comes from: the failure lands on whichever test happens to
 * run next, which is never the one that caused it.</p>
 *
 * <p>In this package because {@code CallAction.isAnswered()} is
 * package-private, and asserting on the action itself is the point -- the
 * observable alternatives all go through the very machinery under test.</p>
 */
class CallsResetForTestTest {

    private LocalCallBridge bridge;

    @BeforeEach
    void install() {
        bridge = new LocalCallBridge();
        CallRequests.resetForTest(bridge);
    }

    @AfterEach
    void clear() {
        CallRequests.resetForTest(null);
    }

    @Test
    void aDeferredStartIsAnsweredRatherThanLeftForTheNextTest() {
        final List<CallAction> starts = new ArrayList<CallAction>();
        Calls.addActionListener(new CallActionAdapter() {
            @Override
            public void startCallRequested(String callId, CallHandle handle,
                    boolean video, CallAction action) {
                // Deferred, which the listener contract explicitly allows and
                // which is how an application takes its time over a start.
                action.defer();
                starts.add(action);
            }
        });
        bridge.simulateStartCallRequest(com.codename1.call.CallId.random(),
                CallHandle.phone("+14155551212"), false);
        long limit = System.currentTimeMillis() + 5000;
        while (starts.isEmpty() && System.currentTimeMillis() < limit) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        assertFalse(starts.isEmpty(), "the start request has to arrive");
        assertFalse(starts.get(0).isAnswered(),
                "the listener deferred it, so nothing has answered it yet");

        Calls.resetForTest();
        assertTrue(starts.get(0).isAnswered(),
                "the reset has to settle what it strands, or the next test"
                + " inherits a live action and a running timer");
    }
}
