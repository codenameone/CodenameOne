/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codenameone.fidelity;

import com.codename1.system.Lifecycle;

/**
 * Entry point for the native-theme fidelity test app. It is not an interactive
 * app: on launch it runs the whole fidelity suite (render every component as the
 * CN1 widget, and -- in golden-regen mode -- as the native widget too), ships the
 * screenshots to the host over the CN1SS WebSocket, then prints
 * CN1SS:SUITE:FINISHED and exits.
 */
public class FidelityApp extends Lifecycle {
    @Override
    public void runApp() {
        // Run off the EDT so the suite can block on screenshot ACKs without
        // freezing the UI thread (mirrors the hellocodenameone runner).
        Thread runner = new Thread(new Runnable() {
            public void run() {
                new FidelityDeviceRunner().runSuite();
            }
        }, "CN1SS-Fidelity-Runner");
        runner.start();
    }
}
