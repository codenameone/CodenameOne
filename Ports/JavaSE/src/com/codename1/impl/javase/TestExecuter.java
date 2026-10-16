/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.javase;

import com.codename1.io.NetworkManager;
import com.codename1.testing.TestReporting;
import com.codename1.testing.UnitTest;
import com.codename1.ui.Accessor;
import com.codename1.ui.Display;
import com.codename1.util.EasyThreadAccessor;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Executes a single test
 */
public class TestExecuter {
    private static boolean failed;

    /**
     * Ends the threads of the Codename One this class was loaded with, once
     * its test is over.
     *
     * Clean mode gives every test a class loader of its own, and with it a
     * Codename One of its own: a Display, a port with its skin images and
     * screen buffers, the application and every form it built. That is
     * garbage when the test ends only if nothing still running was loaded by
     * it. The network threads, the threads behind invokeAndBlock and every
     * EasyThread (the map's tile worker, the image loader) wait for work for
     * as long as the process lives, and a waiting thread keeps its class, the
     * class keeps its loader and the loader keeps all of it. Every test's
     * world stayed in memory this way until the last one finished, and a
     * suite of a dozen screens grew past two gigabytes.
     *
     * Called by the runner, reflectively like {@link #runTest}, and only in
     * clean mode: a runner that goes on to use this same Codename One for the
     * next test still needs these threads.
     */
    public static void retire() {
        try {
            NetworkManager.getInstance().shutdown();
            Accessor.retireThreadPool();
            EasyThreadAccessor.killAll();
        } catch (Throwable t) {
            // The test has been judged already; failing to tidy up after it
            // costs memory and is said, but it is not a failed test.
            t.printStackTrace();
        }
    }
    public static boolean runTest(final String mainClass, final String testClass, boolean quietMode) {
        try {
            if(quietMode) {
                Display.init(new java.awt.Container());
            } else {
                //Simulator.loadFXRuntime();
                System.setProperty("dskin", "/iphone3gs.skin");
                Display.init(null);
            }
            final Class mainCls = Class.forName(mainClass);
            Display.getInstance().callSeriallyAndWait(new Runnable() {
                public void run() {
                    try {
                        Object main = mainCls.newInstance();
                        main.getClass().getMethod("init", Object.class).invoke(main, (Object)null);
                        main.getClass().getMethod("start").invoke(main);
                    } catch(Exception err) {
                        failed = true;
                        TestReporting.getInstance().logException(err);
                    }
                }
            });
            try {
                final UnitTest test = (UnitTest)Class.forName(testClass).newInstance();
                final int timeout = test.getTimeoutMillis();
                if(test.shouldExecuteOnEDT()) {
                    Display.getInstance().callSeriallyAndWait(new Runnable() {
                        public void run() {
                            try {
                                TestReporting.getInstance().startingTestCase(test.getClass().getName());
                                test.prepare();
                                TestReporting.getInstance().logMessage("Test prepared for execution on EDT");
                                failed = !test.runTest();
                                test.cleanup();
                            } catch(Exception err) {
                                failed = true;
                                TestReporting.getInstance().logException(err);
                            } finally {
                                TestReporting.getInstance().finishedTestCase(test.getClass().getName(), !failed);
                            }
                        }
                    }, timeout);
                } else {
                    Timer timeoutKiller = new Timer();
                    final Thread currentThread = Thread.currentThread();
                    TimerTask timeoutTask = new TimerTask() {
                        public void run() {
                            TestReporting.getInstance().logMessage("Test timeout occured: " + timeout + " milliseconds");
                            failed = true;
                            currentThread.stop();
                        }
                    };
                    try {
                        timeoutKiller.schedule(timeoutTask, timeout);
                        TestReporting.getInstance().startingTestCase(test.getClass().getName());
                        test.prepare();
                        TestReporting.getInstance().logMessage("Test prepared for execution off the EDT");
                        failed = !test.runTest();
                        test.cleanup();
                    } catch(Exception err) {
                        failed = true;
                        TestReporting.getInstance().logException(err);
                    } finally {
                        TestReporting.getInstance().finishedTestCase(test.getClass().getName(), !failed);
                        timeoutTask.cancel();
                        // The timer and not only its task: a cancelled task stays in
                        // the queue until the time it was set for, and until then the
                        // timer's thread holds this class and all that was loaded with it.
                        timeoutKiller.cancel();
                    }
                }
            } catch(Exception err) {
                failed = true;
                TestReporting.getInstance().logException(err);
            }
            Display.deinitialize();
            for(java.awt.Frame f : java.awt.Frame.getFrames()) {
                if(f != null && f.isShowing()) {
                    f.dispose();
                }
            }
            return !failed;
        } catch(Throwable t) {
            t.printStackTrace();
        }
        return false;
    }
}
