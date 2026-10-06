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
public class JsFinallyRethrowApp {
    public static int result;
    static boolean enabled = true;
    static Object field = new Object();
    static int finallyRuns;
    static int sum;

    static void boom() {
        throw new RuntimeException("boom");
    }

    static boolean check() {
        return sum >= 0;
    }

    static void use(Object a, int x, int y) {
        sum += x + y;
    }

    // The shape of Form.pointerReleased: several returns inside the try and a
    // conditional finally. The exception passing through the finally is caught by
    // a catch-any entry, which is the one that must be rethrown to the caller.
    static void f(int x, int y) {
        final boolean h = check();
        try {
            if (x == 1) {
                boom();
                return;
            }
            if (x == 2) {
                sum++;
                return;
            }
            boom();
        } finally {
            finallyRuns++;
            field = null;
            if (h && enabled && check()) {
                use(field, x, y);
            }
        }
    }

    public static void main(String[] args) {
        int mask = 0;
        for (int x = 1; x <= 3; x++) {
            try {
                f(x, 7);
                if (x == 2) {
                    mask |= 2;
                }
            } catch (RuntimeException e) {
                if (x == 1 && "boom".equals(e.getMessage())) {
                    mask |= 1;
                }
                if (x == 3 && "boom".equals(e.getMessage())) {
                    mask |= 4;
                }
            }
        }
        if (finallyRuns == 3) {
            mask |= 8;
        }
        if (sum == 1 + (1 + 7) + (2 + 7) + (3 + 7)) {
            mask |= 16;
        }
        result = mask;
    }
}
