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
package com.codename1.impl.ios.sim;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Child-first classloader giving the user's app its own Codename One
 * universe: com.codename1.* (and the app's own classes) load fresh in this
 * loader, so the app's Display singleton, EDT and UI state are fully isolated
 * from the simulator shell - the same isolation trick the Swing simulator's
 * Executor uses for hot reload.
 *
 * <p>The one exception is the bridge package
 * (com.codename1.impl.ios.sim.bridge), which must resolve to the parent's
 * classes so both universes share the RenderBridge/InputSink interfaces and
 * the BridgeRegistry statics.</p>
 */
public class ChildFirstClassLoader extends URLClassLoader {
    private final String[] childFirstPrefixes;

    public ChildFirstClassLoader(URL[] urls, ClassLoader parent, String[] extraChildFirstPrefixes) {
        super(urls, parent);
        int n = extraChildFirstPrefixes != null ? extraChildFirstPrefixes.length : 0;
        childFirstPrefixes = new String[n + 1];
        childFirstPrefixes[0] = "com.codename1.";
        for (int i = 0; i < n; i++) {
            childFirstPrefixes[i + 1] = extraChildFirstPrefixes[i];
        }
    }

    private boolean isChildFirst(String name) {
        if (name.startsWith("com.codename1.impl.ios.sim.bridge.")) {
            return false;
        }
        for (String prefix : childFirstPrefixes) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c == null && isChildFirst(name)) {
                try {
                    c = findClass(name);
                } catch (ClassNotFoundException fallThrough) {
                    // not on our urls - delegate upward
                }
            }
            if (c == null) {
                return super.loadClass(name, resolve);
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }
}
