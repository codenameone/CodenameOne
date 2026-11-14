/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.luau;

import java.util.ArrayList;

/// A Luau thread. Its call stack is a list of [Frame]s rather than a Java
/// thread, so suspending one costs nothing and every coroutine runs on the
/// thread that resumes it.
public final class Coroutine {
    /// Created or yielded, waiting for `resume`.
    public static final int SUSPENDED = 0;
    /// Currently executing.
    public static final int RUNNING = 1;
    /// Resumed another coroutine and waits for it.
    public static final int NORMAL = 2;
    /// Finished or failed.
    public static final int DEAD = 3;

    final ArrayList<Frame> frames = new ArrayList<Frame>();
    final LuaFunction fn;
    int status = SUSPENDED;
    boolean started;
    /// Results expected at the call that yielded, -1 for all.
    int pendingNret;
    Object[] yieldValues;
    /// Depth of Java code (metamethods, sort comparators) running Luau on
    /// this coroutine; a yield is only legal at depth 0.
    int javaNest;
    /// Host data, e.g. the script that owns this thread.
    private Object owner;

    Coroutine(LuaFunction fn) {
        this.fn = fn;
    }

    /// The status constant.
    public int getStatus() {
        return status;
    }

    /// The status name `coroutine.status` reports.
    public String statusName() {
        switch (status) {
            case SUSPENDED:
                return "suspended";
            case RUNNING:
                return "running";
            case NORMAL:
                return "normal";
            default:
                return "dead";
        }
    }

    /// Host data attached to the thread.
    public Object getOwner() {
        return owner;
    }

    /// Attaches host data to the thread.
    public void setOwner(Object o) {
        owner = o;
    }

    @Override
    public String toString() {
        return "thread: 0x" + Integer.toHexString(System.identityHashCode(this));
    }
}
