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
package com.codenameone.examples.bazaar.pos;

/// What the register does next while a card reader the server drives is
/// taking a payment: ask again, take the tender, or stop. Plain, so the
/// rules are tested without the UI.
public final class ReaderPoll {
    /// Keep asking.
    public static final int WAIT = 0;
    /// The reader took the card: record the tender.
    public static final int PAID = 1;
    /// Declined, cancelled or never answered: nothing was taken.
    public static final int STOP = 2;

    /// How often the register asks, in milliseconds.
    public static final int EVERY_MS = 1500;
    /// Two minutes of asking before the register gives up on a reader.
    public static final int MAX_ASKS = 80;
    /// Answers lost in a row before the register gives up.
    public static final int MAX_MISSES = 5;

    private ReaderPoll() {
    }

    /// @param status the payment's status, or null when the ask failed
    /// @param asks how many times the register has asked so far
    /// @param misses failed asks in a row so far, this one included
    public static int next(String status, int asks, int misses) {
        if ("SUCCEEDED".equals(status)) {
            return PAID;
        }
        if ("FAILED".equals(status) || "CANCELED".equals(status)) {
            return STOP;
        }
        if (status == null && misses >= MAX_MISSES) {
            return STOP;
        }
        return asks >= MAX_ASKS ? STOP : WAIT;
    }

    /// Whether a refused start means "use the standalone terminal instead":
    /// no reader at this register (409) or the server out of reach (0).
    public static boolean standalone(int status) {
        return status == 0 || status == 409;
    }
}
