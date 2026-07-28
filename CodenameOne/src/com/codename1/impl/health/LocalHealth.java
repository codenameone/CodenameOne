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
package com.codename1.impl.health;

import com.codename1.health.Health;
import com.codename1.health.HealthAvailability;
import com.codename1.health.HealthStore;

/// The health entry point for ports with no platform health provider: the
/// desktop ports, the JavaScript port, and the simulator's default state.
///
/// Reports [HealthAvailability#LOCAL_ONLY] rather than pretending to be a
/// platform store, so an app can tell the difference between "this data is
/// what other apps and devices recorded" and "this data is only what I
/// wrote myself".
///
/// Workouts and Bluetooth sensors come from the shared base class
/// unchanged: recorded workouts already work everywhere, and the sensor
/// layer rides on `com.codename1.bluetooth.le`, which these ports do have.
public class LocalHealth extends Health {

    private final LocalHealthStore store;

    /// Creates a local health entry point backed by a store that survives
    /// a restart.
    ///
    /// Durability is what [HealthAvailability#LOCAL_ONLY] promises on the
    /// ports that use this constructor -- the data is only ever this app's
    /// own, which is not the same as being gone next launch. The simulator
    /// passes its own scripted store to the other constructor precisely so
    /// it does *not* get this.
    public LocalHealth() {
        this(new StoredHealthStore());
    }

    /// Creates a local health entry point backed by `store`, so a port can
    /// supply a persisting or scriptable subclass.
    public LocalHealth(LocalHealthStore store) {
        this.store = store == null ? new LocalHealthStore() : store;
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public HealthAvailability getAvailability() {
        return HealthAvailability.LOCAL_ONLY;
    }

    @Override
    public HealthStore getStore() {
        return store;
    }
}
