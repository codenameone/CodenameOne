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
package com.codenameone.examples.bastion.ui;

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.io.Util;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/// Cash in and out keeps one idempotency key across tries of one action.
class AccountFormKeyTest {
    @Test
    void aRetryOfTheSameActionReusesItsKey() {
        Util.setImplementation(new JavaSEPort());
        AccountForm.forgetKey();
        String first = AccountForm.keyFor("a1|out|2500");
        // The answer was lost; the user opens the dialog again, same amount.
        assertEquals(first, AccountForm.keyFor("a1|out|2500"));
        String other = AccountForm.keyFor("a1|out|3000");
        assertNotEquals(first, other, "another amount is another action");
        AccountForm.forgetKey();
        assertNotEquals(other, AccountForm.keyFor("a1|out|3000"), "a success forgets it");
        AccountForm.forgetKey();
    }
}
