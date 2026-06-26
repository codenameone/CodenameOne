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
package com.codename1.impl.javase.simulator.proxy;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.impl.CodenameOneImplementationDecorator;
import com.codename1.impl.javase.StubLocationManager;
import com.codename1.location.LocationManager;

/**
 * Decorator routing location queries to the simulator's location-simulation
 * tool ({@link StubLocationManager}, driven by the Location Simulation dialog)
 * regardless of which backend implementation is active.
 *
 * <p>The delegate is still consulted first so backend-specific behavior such
 * as the simulated Android location permission prompt keeps working - when the
 * port denies access (returns null) the proxy honors that.</p>
 */
public class SimulatorLocationProxy extends CodenameOneImplementationDecorator {
    public SimulatorLocationProxy(CodenameOneImplementation delegate) {
        super(delegate);
    }

    @Override
    public LocationManager getLocationManager() {
        LocationManager fromPort = delegate.getLocationManager();
        if (fromPort == null) {
            // the backend denied access (e.g. simulated permission prompt)
            return null;
        }
        return StubLocationManager.getLocationManager();
    }
}
