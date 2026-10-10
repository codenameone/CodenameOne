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
package com.codename1.maven;

/// What a forked [RemapDifferential] run starts. It is not part of the fixture
/// and so is never remapped: installing the headless implementation and ending
/// the process are both calls the remap would have redirected, and Codename
/// One's event thread keeps a virtual machine alive after `main` returns.
public final class RemapDifferentialMain {
    private RemapDifferentialMain() {
    }

    public static void main(String[] args) {
        int result = 0;
        try {
            if (args.length > 0 && "device".equals(args[0])) {
                Class.forName("com.codename1.compat.testing.HeadlessImplementation")
                        .getMethod("install").invoke(null);
            }
            // The relocation ships a desktop application's main under
            // another name (ClassRelocator.DESKTOP_MAIN), so the remapped
            // arm finds it there and the JDK arm under its own.
            java.lang.reflect.Method entry = null;
            for (java.lang.reflect.Method m : Class.forName("q.D").getMethods()) {
                if (m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == String[].class
                        && ("main".equals(m.getName()) || "cn1DesktopMain".equals(m.getName()))) {
                    entry = m;
                }
            }
            if (entry == null) {
                throw new NoSuchMethodException("q.D has no main(String[])");
            }
            entry.invoke(null, (Object) new String[0]);
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            result = 1;
        }
        System.out.flush();
        Runtime.getRuntime().halt(result);
    }
}
