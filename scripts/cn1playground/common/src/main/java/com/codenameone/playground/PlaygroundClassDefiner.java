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
package com.codenameone.playground;

/**
 * Turns compiled classes into a running object on platforms that can define
 * classes at run time (the JavaSE simulator, the test harnesses). Implementations
 * register themselves; the JavaScript build translates and evaluates instead
 * (see {@link PlaygroundJs}).
 */
public interface PlaygroundClassDefiner {
    /**
     * Defines {@code classes} (a {@link com.codename1.tools.javac.StubLibrary}-format
     * pack of class files) in a fresh loader whose parent sees the Playground, and
     * returns a new instance of {@code mainClass} (dotted name).
     */
    Object defineAndInstantiate(byte[] classes, String mainClass) throws Exception;

    /** The registered definer, if any. */
    final class Registry {
        private static PlaygroundClassDefiner definer;

        private Registry() {
        }

        public static void register(PlaygroundClassDefiner d) {
            definer = d;
        }

        public static PlaygroundClassDefiner get() {
            return definer;
        }
    }
}
