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
package com.codenameone.examples.bastion.net;

import com.codename1.io.rest.TestResponses;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Answers from an earlier session are dropped.
class NetTest {
    /// A call made before the lock answers after it: neither its result nor
    /// its failure reaches the screen that made it.
    @Test
    void anAnswerAfterTheLockIsDropped() {
        final int[] calls = new int[1];
        com.codename1.util.OnComplete<com.codename1.io.rest.Response<String>> callback =
                Net.to(value -> calls[0]++, (status, message) -> calls[0]++);
        Session.suspend();
        callback.completed(TestResponses.ok("balance"));
        assertEquals(0, calls[0]);

        Net.to((String value) -> calls[0]++, (status, message) -> calls[0]++)
                .completed(TestResponses.ok("balance"));
        assertEquals(1, calls[0], "a call made after it still answers");
    }
}
