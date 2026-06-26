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
package com.codename1.impl.windows.sim;

import com.codename1.impl.ImplementationFactory;
import com.codename1.impl.windows.WindowsImplementation;
import com.codename1.ui.Display;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Pure Codename One simulator launcher for Windows: the Windows port's Java
 * side runs on the JVM, its natives bind to cn1sim.dll via the generated JNI
 * shims, and the port itself owns the Win32 window and message pump - the
 * exact same design as a translated desktop build, minus the translation.
 *
 * <p>Launch (no special flags - the pump runs on whichever thread calls it):</p>
 * <pre>
 * java -Dcn1.sim.native.path=C:\path\to\cn1sim.dll ^
 *      com.codename1.impl.windows.sim.CN1WinSimulator com.mycompany.MyApp
 * </pre>
 */
public class CN1WinSimulator {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: CN1WinSimulator <main-class>");
            System.exit(1);
        }
        String path = System.getProperty("cn1.sim.native.path");
        if (path == null || !new File(path).exists()) {
            System.err.println("cn1.sim.native.path must point at cn1sim.dll");
            System.exit(1);
        }
        System.load(new File(path).getAbsolutePath());

        ImplementationFactory.setInstance(new ImplementationFactory() {
            public Object createImplementation() {
                return new WindowsImplementation();
            }
        });

        final String mainClass = args[0];

        // Display.init runs HERE so the HWND is created on this thread -
        // Win32 delivers window messages to the creating thread, and this
        // same thread pumps them below (exactly like a translated build)
        Display.init(null);

        Thread appThread = new Thread("CN1Sim-App") {
            public void run() {
                try {
                    final Object app = Class.forName(mainClass).getDeclaredConstructor().newInstance();
                    invokeOptional(app, "init", new Class[]{Object.class}, new Object[]{null});
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            try {
                                invokeOptional(app, "start", new Class[0], new Object[0]);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                System.exit(1);
                            }
                        }
                    });
                } catch (Throwable t) {
                    t.printStackTrace();
                    System.exit(1);
                }
            }
        };
        appThread.setDaemon(true);
        appThread.start();

        WindowsImplementation.runMainEventLoop();
    }

    private static void invokeOptional(Object app, String name, Class[] sig, Object[] args) throws Exception {
        try {
            Method m = app.getClass().getMethod(name, sig);
            m.invoke(app, args);
        } catch (NoSuchMethodException ignored) {
        }
    }
}
