/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.metrics;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A count that only goes up -- requests served, jobs run, errors seen. Exported
 * as a cumulative monotonic sum.
 *
 * <p>Recording is one atomic add: no lock, no allocation.
 */
public final class Counter extends Instrument {
    private final AtomicLong total = new AtomicLong();

    Counter(String name, String description, String unit, boolean upDown) {
        super(name, description, unit, upDown ? UP_DOWN_COUNTER : COUNTER);
    }

    /** Adds one. */
    public void increment() {
        total.incrementAndGet();
    }

    /**
     * Adds {@code amount}. A counter refuses a negative amount, since a
     * cumulative monotonic sum that falls reads as a reset to every backend; an
     * up-down counter takes either sign.
     */
    public void add(long amount) {
        if(amount < 0 && getKind() == COUNTER) {
            throw new IllegalArgumentException("Counter " + getName()
                    + " only goes up; use an up-down counter");
        }
        total.addAndGet(amount);
    }

    public long get() {
        return total.get();
    }

    public List points() {
        return single(point(null, total.get()));
    }
}
