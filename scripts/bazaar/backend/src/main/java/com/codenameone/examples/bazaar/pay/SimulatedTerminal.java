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
package com.codenameone.examples.bazaar.pay;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/// A card reader that is not there. Any registration code that starts with
/// `simulated-` pairs a reader; an intent handed to it is still PENDING the
/// first time it is asked about -- the cardholder is tapping -- and decided the
/// time after, the way a real reader answers a poll a moment later.
///
/// As Stripe's simulated readers do, the amount decides the outcome: an
/// amount whose last two digits are `01` is declined, and every other amount
/// is approved on a test Visa ending 4242. No money moves.
public class SimulatedTerminal implements CardTerminal {
    /// The prefix of a code that pairs a simulated reader.
    public static final String CODE_PREFIX = "simulated-";
    /// Cents that make a payment declined.
    public static final long DECLINED_CENTS = 1;

    private static final class Intent {
        long amountCents;
        String reader = "";
        int asked;
        boolean cancelled;
    }

    private final Map<String, Intent> intents = new HashMap<String, Intent>();
    private int next;

    @Override
    public String name() {
        return "simulated";
    }

    @Override
    public synchronized Reader register(String registrationCode, String label)
            throws IOException {
        if (registrationCode == null || !registrationCode.startsWith(CODE_PREFIX)
                || registrationCode.length() <= CODE_PREFIX.length()) {
            throw new IOException("That code is not a reader's: a simulated reader's code starts "
                    + "with " + CODE_PREFIX);
        }
        next++;
        return new Reader("tmr_sim_" + registrationCode.substring(CODE_PREFIX.length()),
                "SIM-" + (1000 + next));
    }

    @Override
    public void remove(String readerRef) {
        // Nothing is kept of a simulated reader.
    }

    @Override
    public synchronized String createIntent(String key, long amountCents, String currency,
            String description) throws IOException {
        if (amountCents <= 0) {
            throw new IOException("That is not an amount");
        }
        String ref = "sim_tpi_" + key;
        if (!intents.containsKey(ref)) {
            Intent i = new Intent();
            i.amountCents = amountCents;
            intents.put(ref, i);
        }
        return ref;
    }

    @Override
    public synchronized void process(String readerRef, String intentRef) throws IOException {
        Intent i = intents.get(intentRef);
        if (i == null) {
            throw new IOException("No such intent");
        }
        i.reader = readerRef;
        i.asked = 0;
        i.cancelled = false;
    }

    @Override
    public synchronized Outcome status(String readerRef, String intentRef) throws IOException {
        Intent i = intents.get(intentRef);
        if (i == null) {
            // Forgotten by a restart, as a simulated card is.
            return new Outcome(FAILED, "The reader lost the payment", "");
        }
        if (i.cancelled) {
            return new Outcome(CANCELED, "Cancelled at the register", "");
        }
        if (i.asked++ == 0) {
            return new Outcome(PENDING, "", "");
        }
        if (i.amountCents % 100 == DECLINED_CENTS) {
            return new Outcome(FAILED, "Your card was declined", "");
        }
        return new Outcome(SUCCEEDED, "", "Visa 4242");
    }

    @Override
    public synchronized void cancel(String readerRef, String intentRef) {
        Intent i = intents.get(intentRef);
        if (i != null) {
            i.cancelled = true;
        }
    }
}
