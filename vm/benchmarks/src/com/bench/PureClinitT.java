/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.bench;

/**
 * Classes whose static initializer is PURE are initialized at startup, before main,
 * and lose their init guards (ByteCodeClass.isEagerInitEligible). This checks the
 * two halves of that promise.
 *
 * <p>{@code Broken}'s initializer always throws -- pure code has no inputs, so it
 * either always completes or always fails -- and it is reachable but never used in
 * this run. On the JVM that costs nothing. Run eagerly it must cost only that class:
 * the program has to reach "end" (the runtime reports the failure on a
 * {@code [CN1]} line, which the gauntlet's comparison drops). {@code Squares} and
 * {@code Color} are the ordinary case: a table built in a loop and an enum, both
 * initialized eagerly and both expected to read back exactly as the JVM does.</p>
 */
public class PureClinitT {
    static final class Broken {
        static final int[] TABLE = new int[2];
        static {
            TABLE[3] = 7;
        }

        static int first() {
            return TABLE[0];
        }
    }

    static final class Squares {
        static final int[] VALUES = new int[16];
        static {
            for (int i = 0; i < VALUES.length; i++) {
                VALUES[i] = i * i;
            }
        }
    }

    enum Color { RED, GREEN, BLUE }

    public static void main(String[] args) {
        System.out.println("start");
        if (args.length > 100) {
            System.out.println(Broken.first());
        }
        int sum = 0;
        for (int v : Squares.VALUES) {
            sum += v;
        }
        System.out.println("squares=" + sum);
        Color[] colors = Color.values();
        System.out.println("colors=" + colors.length + " " + Color.GREEN.ordinal() + " " + colors[2].name());
        System.out.println("end");
    }
}
