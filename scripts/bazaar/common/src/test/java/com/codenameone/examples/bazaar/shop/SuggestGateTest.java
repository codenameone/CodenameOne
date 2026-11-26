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
package com.codenameone.examples.bazaar.shop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/// The app's predictive search drops answers that a newer request outran.
class SuggestGateTest {
    @Test
    void onlyTheNewestAnswerIsShown() {
        SuggestGate gate = new SuggestGate();
        int mu = gate.issue();
        int mug = gate.issue();
        assertFalse(gate.current(mu));
        assertTrue(gate.current(mug));
        gate.invalidate();
        assertFalse(gate.current(mug));
    }

    @Test
    void shortInputIsNotAskedAbout() {
        assertFalse(SuggestGate.worthAsking(null));
        assertFalse(SuggestGate.worthAsking(" m "));
        assertTrue(SuggestGate.worthAsking("mu"));
    }
}
