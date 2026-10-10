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

import java.lang.reflect.Method;

/// What a forked test starts to run remapped classes that need a newer JVM
/// than the one running the tests: installs the headless implementation,
/// calls the static method named by the two arguments on Codename One's
/// event dispatch thread, and prints `RESULT:` and what it answered.
///
/// It reaches everything by name, so the class path of the fork decides
/// what it runs: the remapped classes, the headless implementation and the
/// framework. It ends the process itself, since the event dispatch thread
/// keeps a virtual machine alive after `main` returns.
public final class RemappedProbeMain {
    private RemappedProbeMain() {
    }

    public static void main(final String[] args) {
        int result = 0;
        try {
            Class.forName("com.codename1.compat.testing.HeadlessImplementation").getMethod("install").invoke(null);
            Class<?> displayClass = Class.forName("com.codename1.ui.Display");
            Object display = displayClass.getMethod("getInstance").invoke(null);
            final Object[] answer = new Object[1];
            final Throwable[] failure = new Throwable[1];
            Method wait = displayClass.getMethod("callSeriallyAndWait", Runnable.class);
            wait.invoke(display, new Runnable() {
                @Override
                public void run() {
                    try {
                        answer[0] = Class.forName(args[0]).getMethod(args[1]).invoke(null);
                    } catch (Throwable t) {
                        failure[0] = t;
                    }
                }
            });
            if (failure[0] != null) {
                Throwable t = failure[0].getCause() == null ? failure[0] : failure[0].getCause();
                t.printStackTrace(System.out);
                result = 1;
            } else {
                System.out.println("RESULT:" + answer[0]);
            }
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            result = 1;
        }
        System.out.flush();
        Runtime.getRuntime().halt(result);
    }
}
