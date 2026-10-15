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
public class SystemMembersApp {
    private static final StringBuilder OUT = new StringBuilder();
    private static Thread seenThread;
    private static String seenMessage;
    private static String seenDefault;

    private static void check(String name, boolean ok) {
        OUT.append(name).append(ok ? ":ok;" : ":FAIL;");
    }

    private static void systemMembers() {
        check("lineSeparator", "\n".equals(System.lineSeparator()));
        check("lineSeparatorProperty", "\n".equals(System.getProperty("line.separator")));
        check("fileSeparator", "/".equals(System.getProperty("file.separator")));
        check("pathSeparator", ":".equals(System.getProperty("path.separator")));
        check("unsetIsNull", System.getProperty("cn1.members.absent") == null);
        check("unsetDefault", "d".equals(System.getProperty("cn1.members.absent", "d")));
        check("firstSet", System.setProperty("cn1.members.key", "a") == null);
        check("secondSet", "a".equals(System.setProperty("cn1.members.key", "b")));
        check("read", "b".equals(System.getProperty("cn1.members.key")));
        check("clear", "b".equals(System.clearProperty("cn1.members.key")));
        check("cleared", System.getProperty("cn1.members.key") == null);
        check("getenv", System.getenv() != null);
        boolean npe = false;
        try {
            System.getProperty(null);
        } catch (NullPointerException e) {
            npe = true;
        }
        check("nullKey", npe);
        boolean iae = false;
        try {
            System.getProperty("");
        } catch (IllegalArgumentException e) {
            iae = true;
        }
        check("emptyKey", iae);
    }

    private static void threadMembers() throws Exception {
        Thread a = new Thread();
        Thread b = new Thread();
        check("idStable", a.getId() == a.getId());
        check("idDistinct", a.getId() != b.getId());
        a.setName("worker");
        check("setName", "worker".equals(a.getName()));
        boolean npe = false;
        try {
            a.setName(null);
        } catch (NullPointerException e) {
            npe = true;
        }
        check("setNameNull", npe);

        Thread failing = new Thread(new Runnable() {
            public void run() {
                throw new IllegalStateException("own");
            }
        });
        final Thread[] holder = {failing};
        Thread.UncaughtExceptionHandler own = new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                seenThread = t;
                seenMessage = e.getMessage();
            }
        };
        failing.setUncaughtExceptionHandler(own);
        check("ownHandlerSet", failing.getUncaughtExceptionHandler() == own);
        failing.start();
        failing.join();
        check("ownHandlerThread", seenThread == holder[0]);
        check("ownHandlerMessage", "own".equals(seenMessage));

        Thread.UncaughtExceptionHandler fallback = new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                seenDefault = e.getMessage();
            }
        };
        Thread.setDefaultUncaughtExceptionHandler(fallback);
        check("defaultSet", Thread.getDefaultUncaughtExceptionHandler() == fallback);
        Thread plain = new Thread(new Runnable() {
            public void run() {
                throw new IllegalStateException("default");
            }
        });
        plain.start();
        plain.join(10000, 0);
        check("defaultHandler", "default".equals(seenDefault));
        Thread.setDefaultUncaughtExceptionHandler(null);
        check("defaultCleared", Thread.getDefaultUncaughtExceptionHandler() == null);
        Thread.sleep(1, 0);
        check("sleepNanos", true);
    }

    private static void runtimeMembers() {
        Runtime rt = Runtime.getRuntime();
        check("processors", rt.availableProcessors() >= 1);
        check("maxMemory", rt.maxMemory() > 0);
        boolean npe = false;
        try {
            rt.addShutdownHook(null);
        } catch (NullPointerException e) {
            npe = true;
        }
        check("hookNull", npe);
        Thread hook = new Thread();
        rt.addShutdownHook(hook);
        boolean twice = false;
        try {
            rt.addShutdownHook(hook);
        } catch (IllegalArgumentException e) {
            twice = true;
        }
        check("hookTwice", twice);
        check("hookRemoved", rt.removeShutdownHook(hook));
        check("hookRemovedAgain", !rt.removeShutdownHook(hook));
        rt.addShutdownHook(new Thread(new Runnable() {
            public void run() {
                OUT.append("hookRan;");
                System.out.println("RESULT=" + OUT);
            }
        }));
    }

    public static void main(String[] args) throws Exception {
        systemMembers();
        threadMembers();
        runtimeMembers();
        // The RESULT line is printed by the shutdown hook, so a VM that does
        // not run hooks on exit prints none.
        System.exit(0);
    }
}
