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
public class JsJsrFinallyRethrowApp {
    public static int result;
    static int unlocks;
    static int dones;
    static int closes;

    static void lock() {
    }

    // A finally that runs more often than once per call says so by throwing: the fault
    // this guards against repeats the finally without end, and a fixture that only
    // counted would hang the test instead of failing it.
    static void unlock() {
        if (++unlocks > 4) {
            throw new IllegalStateException("unlock ran again");
        }
    }

    static void done() {
        if (++dones > 4) {
            throw new IllegalStateException("done ran again");
        }
    }

    static void close() {
        if (++closes > 4) {
            throw new IllegalStateException("close ran again");
        }
    }

    static int locked(int x) {
        if (x == 1) {
            throw new IllegalArgumentException("refused");
        }
        return x * 10;
    }

    // The shape of MigrationEngine.migrate(): a return inside a finally inside a finally.
    // Compiled for Java 5, each finally is a subroutine entered with JSR.
    static int nested(int x) {
        try {
            lock();
            try {
                return locked(x);
            } finally {
                unlock();
            }
        } finally {
            done();
        }
    }

    // One finally is enough for the fault: the rethrow after it is what was caught again.
    static int single(int x) {
        try {
            return locked(x);
        } finally {
            close();
        }
    }

    public static void main(String[] args) {
        int mask = 0;
        if (nested(2) == 20 && single(3) == 30) {
            mask |= 1;
        }
        try {
            nested(1);
        } catch (RuntimeException e) {
            if ("refused".equals(e.getMessage())) {
                mask |= 2;
            }
        }
        try {
            single(1);
        } catch (RuntimeException e) {
            if ("refused".equals(e.getMessage())) {
                mask |= 4;
            }
        }
        if (unlocks == 2 && dones == 2) {
            mask |= 8;
        }
        if (closes == 2) {
            mask |= 16;
        }
        result = mask;
    }
}
