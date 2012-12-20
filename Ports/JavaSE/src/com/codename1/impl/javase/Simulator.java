/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.impl.javase;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.StringTokenizer;

/**
 * A simple class that can invoke a lifecycle object to allow it to run a
 * Codename One application. Classes are loaded with a classloader so the UI
 * skin can be updated and the lifecycle objects reloaded.
 *
 * @author Shai Almog
 */
public class Simulator {

    /**
     * Accepts the classname to launch
     */
    public static void main(final String[] argv) throws Exception {
        String skin = System.getProperty("dskin");
        if (skin == null) {
            System.setProperty("dskin", "/iphone3gs.skin");
        }
        StringTokenizer t = new StringTokenizer(System.getProperty("java.class.path"), File.pathSeparator);
        System.setProperty("MainClass", argv[0]);
        File[] files = new File[t.countTokens()];
        for (int iter = 0; iter < files.length; iter++) {
            files[iter] = new File(t.nextToken());
        }
        loadFXRuntime();
        ClassLoader ldr = new ClassPathLoader(files);
        Class c = Class.forName("com.codename1.impl.javase.Executor", true, ldr);
        Method m = c.getDeclaredMethod("main", String[].class);
        m.invoke(null, new Object[]{argv});

        new Thread() {
            public void run() {
                while (true) {
                    try {
                        sleep(500);
                    } catch (InterruptedException ex) {
                    }
                    String r = System.getProperty("reload.simulator");
                    if (r != null && r.equals("true")) {
                        System.setProperty("reload.simulator", "");
                        try {
                            main(argv);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        return;
                    }
                }
            }
        }.start();
    }

    static void loadFXRuntime() {
        String javahome = System.getProperty("java.home");
        String fx = javahome + "/lib/jfxrt.jar";
        File f = new File(fx);
        if (f.exists()) {
            URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Class<?> sysclass = URLClassLoader.class;
            try {
                Method method = sysclass.getDeclaredMethod("addURL", new Class[]{URL.class});
                method.setAccessible(true);
                method.invoke(sysloader, new Object[]{f.toURI().toURL()});
            } catch (Throwable t) {
                t.printStackTrace();
            }//end try catch                        

        }


    }
}

