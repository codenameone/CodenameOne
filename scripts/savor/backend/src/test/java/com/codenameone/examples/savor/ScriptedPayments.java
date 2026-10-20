/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.savor;

import com.codenameone.examples.savor.pay.SimulatedPayments;

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
