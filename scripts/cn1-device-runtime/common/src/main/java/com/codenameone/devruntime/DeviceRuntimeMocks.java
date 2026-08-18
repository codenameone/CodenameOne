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
package com.codenameone.devruntime;

import com.codename1.impl.interp.InterpHostInterceptor;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;

import java.util.Hashtable;

/**
 * Stands in for the subsystems this runtime cannot really provide.
 *
 * <p>Three of them, and each for a different reason. A purchase needs a store
 * account and products in a console, and shipping a real billing flow inside a
 * host that runs other people's code would be indefensible. A social login
 * needs client ids bound to this app's bundle id and signing certificate, which
 * a pushed program cannot have. Both are things developers debug constantly and
 * would otherwise have to debug somewhere else.</p>
 *
 * <p>The seam is the static factory. {@code Purchase.getInAppPurchase()} and
 * {@code FacebookConnect.getInstance()} are how an application reaches these,
 * so answering those calls hands pushed code a mock and every later call lands
 * on it by ordinary dispatch. The runtime app itself is untouched.</p>
 *
 * <h2>Saying so</h2>
 *
 * <p>A mock that looks like the real thing is worse than no mock: somebody
 * ships code that only ever succeeded because nothing was real. So the first
 * time a pushed program touches one, the runtime says which subsystem it was,
 * on screen and in the status line, and the mock objects say it again in the
 * data they return -- a price of "$0.00 (mock)", a token that reads
 * {@code mock-...-not-valid-anywhere}.</p>
 *
 * @author Shai Almog
 */
public final class DeviceRuntimeMocks implements InterpHostInterceptor {
    /// Subsystems already announced, so the warning appears once per program
    /// rather than once per call.
    private static final Hashtable ANNOUNCED = new Hashtable();

    /// One purchase object for the life of a pushed program, so what it bought
    /// is still bought on the next call.
    private final MockPurchase purchase = new MockPurchase();

    public Object interceptStatic(String owner, String name, String descriptor, Object[] args) {
        if ("com/codename1/payment/Purchase".equals(owner)
                && "getInAppPurchase".equals(name)) {
            return purchase;
        }
        return NOT_INTERCEPTED;
    }

    /// Forgets what has been announced, so the next pushed program is told too.
    public static void reset() {
        ANNOUNCED.clear();
    }

    /**
     * Says once, per subsystem, that what the program just used is a mock.
     *
     * <p>Called from the mocks rather than from the interception, because
     * fetching {@code Purchase.getInAppPurchase()} is not the interesting
     * moment -- completing a purchase is.</p>
     */
    public static void warnOnce(final String subsystem) {
        if (ANNOUNCED.get(subsystem) != null) {
            return;
        }
        ANNOUNCED.put(subsystem, Boolean.TRUE);
        DeviceRuntimeService.getInstance().noteMockUsed(subsystem);
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                Dialog.show("This is a mock",
                        "This program just used " + subsystem + ", which this runtime "
                        + "mocks. It always succeeds, no money moves and no real account "
                        + "is involved. Test the real thing in a build of your own app.",
                        "OK", null);
            }
        });
    }
}
