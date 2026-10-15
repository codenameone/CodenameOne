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
package com.codenameone.examples.wayline;

import com.codenameone.examples.wayline.pay.SimulatedPayments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The simulated processor with a test's hand on it: the test says whether
/// refunds and card detaches fail, and what happens while a refund is at the
/// processor -- which is how two requests that overlap are lined up one inside
/// the other, in the order a test wants, on one thread.
final class ScriptedPayments extends SimulatedPayments {
    /// Run inside the next refund, before it answers, and then forgotten.
    Runnable duringRefund;
    /// How the coming refunds answer, in the order they answer -- a refund
    /// run inside another answers before it. True fails one without saying
    /// whether it was made; once the list is empty every refund succeeds.
    final List<Boolean> refundFailures = new ArrayList<Boolean>();
    /// Whether detaching a card fails the same way.
    boolean detachFails;
    /// The tokens of the cards detached, in order.
    final List<String> detached = new ArrayList<String>();

    /// Back to a processor that does what the simulated one does.
    void reset() {
        duringRefund = null;
        refundFailures.clear();
        detachFails = false;
        detached.clear();
    }

    @Override
    public Refund refund(String chargeReference, long amountCents) throws IOException {
        Runnable during = duringRefund;
        duringRefund = null;
        if (during != null) {
            during.run();
        }
        if (!refundFailures.isEmpty() && refundFailures.remove(0).booleanValue()) {
            throw new IOException("The scripted processor did not answer");
        }
        return super.refund(chargeReference, amountCents);
    }

    @Override
    public void detachCard(String customer, String token) throws IOException {
        if (detachFails) {
            throw new IOException("The scripted processor did not answer");
        }
        detached.add(token);
    }
}
