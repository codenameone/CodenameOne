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
package com.codename1.impl.windows;

import com.codename1.location.Location;
import com.codename1.location.LocationListener;
import com.codename1.location.LocationManager;
import java.io.IOException;

/**
 * Location services backed by the WinRT {@code Geolocator}. A continuous
 * listener is served by a lightweight polling thread (the native bridge resolves
 * one fix per call). Reports {@code OUT_OF_SERVICE} honestly when Windows
 * location is disabled or no provider answers, rather than fabricating a fix.
 */
class WindowsLocationManager extends LocationManager {
    private Thread poller;
    private volatile boolean listening;

    @Override
    public Location getCurrentLocation() throws IOException {
        double[] out = new double[6];
        if (!WindowsNative.locationGetCurrent(out)) {
            throw new IOException("Location is unavailable (disabled or no provider)");
        }
        return toLocation(out);
    }

    @Override
    public Location getLastKnownLocation() {
        double[] out = new double[6];
        if (!WindowsNative.locationGetCurrent(out)) {
            return null;
        }
        return toLocation(out);
    }

    private static Location toLocation(double[] o) {
        Location l = new Location();
        l.setLatitude(o[0]);
        l.setLongitude(o[1]);
        l.setAccuracy((float) o[2]);
        l.setAltitude(o[3]);
        l.setDirection((float) o[4]);
        l.setVelocity((float) o[5]);
        l.setTimeStamp(System.currentTimeMillis());
        l.setStatus(LocationManager.AVAILABLE);
        return l;
    }

    @Override
    protected void bindListener() {
        listening = true;
        setStatus(AVAILABLE);
        poller = new Thread(new Runnable() {
            @Override
            public void run() {
                while (listening) {
                    try {
                        Location l = getCurrentLocation();
                        LocationListener cb = getListener();
                        if (cb != null && l != null) {
                            cb.locationUpdated(l);
                        }
                    } catch (Throwable t) {
                        setStatus(TEMPORARILY_UNAVAILABLE);
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }, "cn1-windows-location");
        poller.setDaemon(true);
        poller.start();
    }

    @Override
    protected void clearListener() {
        listening = false;
        if (poller != null) {
            poller.interrupt();
            poller = null;
        }
    }
}
