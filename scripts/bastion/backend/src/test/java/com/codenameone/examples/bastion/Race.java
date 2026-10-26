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
package com.codenameone.examples.bastion;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/// Starts calls at the same moment, as near as threads allow. A count and a
/// flag, polled: the class library a compiled server links against has the
/// atomics and no latch.
final class Race {
    private Race() {
    }

    static void together(Runnable[] calls) throws Exception {
        final AtomicInteger ready = new AtomicInteger();
        final AtomicBoolean go = new AtomicBoolean();
        Thread[] threads = new Thread[calls.length];
        for (int iter = 0; iter < calls.length; iter++) {
            final Runnable call = calls[iter];
            threads[iter] = new Thread(new Runnable() {
                @Override
                public void run() {
                    ready.incrementAndGet();
                    long giveUp = System.currentTimeMillis() + 30000L;
                    while (!go.get()) {
                        if (System.currentTimeMillis() > giveUp) {
                            return;
                        }
                        Thread.yield();
                    }
                    call.run();
                }
            }, "race-" + iter);
            threads[iter].start();
        }
        while (ready.get() < calls.length) {
            Thread.yield();
        }
        go.set(true);
        for (Thread t : threads) {
            t.join(60000L);
        }
    }
}
