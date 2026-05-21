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
package com.codename1.impl.javase.simulator;

/**
 * Static methods used by {@link SimulatorHookLoaderTest} as action targets.
 * Kept on a separate class so the test can verify reflective resolution works
 * for typical cn1lib-style entry points (a public class with public static
 * void no-arg methods).
 */
public class SimulatorHookLoaderTestFixture {

    public static volatile int alphaCount;
    public static volatile int betaCount;

    public static void alpha() {
        alphaCount++;
    }

    public static void beta() {
        betaCount++;
    }

    /** Not static — used to verify the loader rejects non-static methods. */
    public void instanceOnly() {
        // intentionally empty
    }

    static void resetCounters() {
        alphaCount = 0;
        betaCount = 0;
    }
}
