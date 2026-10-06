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

import com.codename1.tools.javac.StubLibrary;

/**
 * JavaSE (simulator / desktop) side of {@link PlaygroundLoaderNative}: compiled
 * Playground code is defined by a fresh class loader per run, whose parent is the
 * application's, so it sees Codename One and the Playground.
 */
public class PlaygroundLoaderNativeImpl implements PlaygroundLoaderNative {
    public boolean registerDefiner() {
        PlaygroundClassDefiner.Registry.register(new JvmDefiner());
        return true;
    }

    public boolean isSupported() {
        return true;
    }

    /** Defines one run's classes in their own loader. */
    static final class JvmDefiner implements PlaygroundClassDefiner {
        public Object defineAndInstantiate(byte[] classes, String mainClass) throws Exception {
            final StubLibrary pack = new StubLibrary(classes);
            ClassLoader loader = new ClassLoader(PlaygroundLoaderNativeImpl.class.getClassLoader()) {
                @Override
                protected Class<?> findClass(String name) throws ClassNotFoundException {
                    byte[] b = pack.classBytes(name.replace('.', '/'));
                    if (b == null) {
                        throw new ClassNotFoundException(name);
                    }
                    return defineClass(name, b, 0, b.length);
                }
            };
            return loader.loadClass(mainClass).getDeclaredConstructor().newInstance();
        }
    }
}
