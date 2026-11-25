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
package com.codenameone.devruntime;

import com.codename1.io.NetworkEvent;
import com.codename1.system.Lifecycle;

/**
 * The device runtime: an app whose whole purpose is to run other people's apps.
 *
 * <p>Install it once. From then on a Codename One project pushed from a desktop
 * runs here, interpreted, with no build and no reinstall. It is a debugging
 * proxy for arbitrary applications, so it deliberately contains no application
 * of its own -- one screen showing where it is dialling, what is loaded, and
 * which computers may push to it.</p>
 *
 * <p>The dialling direction is out, not in. A phone on a real network cannot
 * accept an inbound connection, and over USB the device's loopback is what
 * {@code adb reverse} maps. So the device asks the desktop, every couple of
 * seconds, whether it has anything to run.</p>
 *
 * @author Shai Almog
 */
public class DeviceRuntimeApp extends Lifecycle {
    /** The port the desktop tooling listens on. */
    private static final int PORT = 18234;

    @Override
    public void init(Object context) {
        super.init(context);
        // Before anything is pushed: a provider's implementation is chosen the
        // first time getInstance() is called, and a pushed program calling it
        // must find the mock rather than the real provider's "unsupported".
        com.codename1.social.DeviceRuntimeSocialMocks.install();
        try {
            DeviceRuntimeService svc = DeviceRuntimeService.getInstance();
            boolean started = svc.startDialer(PORT);
            System.out.println("CN1SS:DEVRUNTIME started=" + started + " " + svc.getStatus());
        } catch (Throwable t) {
            System.out.println("CN1SS:DEVRUNTIME:EXCEPTION "
                    + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    @Override
    public void start() {
        // Always the runtime screen. A pushed program replaces it while it runs
        // and this comes back when the app is resumed, which is what you want
        // from a debugging proxy: somewhere to see where it is dialling and what
        // it last loaded.
        DeviceRuntimeForm.showIt();
    }

    /**
     * Report a network failure; do not try to phone home about it.
     *
     * <p>Lifecycle's default handler calls {@code Log.sendLogAsync()}, which
     * makes another blocking request on the event thread -- and when the
     * network is what is broken, that request fails too and re-enters this
     * handler, leaving the event thread parked in nested {@code invokeAndBlock}
     * calls. A pushed program pointed at a wrong URL hits it immediately. The
     * modal dialog goes for the same reason: nobody is holding this device.</p>
     */
    @Override
    protected void handleNetworkError(NetworkEvent err) {
        err.consume();
        String url = err.getConnectionRequest() == null
                ? "(no request)" : err.getConnectionRequest().getUrl();
        Object cause = err.getError() != null
                ? err.getError() : ("http " + err.getResponseCode());
        System.out.println("CN1SS:DEVRUNTIME network error url=" + url + " error=" + cause);
    }
}
