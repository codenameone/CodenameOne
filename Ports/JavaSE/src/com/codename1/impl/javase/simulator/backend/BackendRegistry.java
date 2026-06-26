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
package com.codename1.impl.javase.simulator.backend;

import com.codename1.impl.javase.simulator.backend.swing.SwingSimulatorBackend;
import com.codename1.impl.javase.simulator.spi.SimulatorBackend;

import java.util.ServiceLoader;

/**
 * Selects the active simulator backend. The cn1.simulator.backend system
 * property names the backend id ("swing" is the built-in default); other
 * backends - e.g. the native iOS/Windows rendering backends - register
 * themselves through the standard ServiceLoader mechanism
 * (META-INF/services/com.codename1.impl.javase.simulator.spi.SimulatorBackend).
 */
public class BackendRegistry {
    private static SimulatorBackend active;

    private BackendRegistry() {
    }

    /**
     * @return the active backend, resolving it on first call
     */
    public static synchronized SimulatorBackend getActive() {
        if (active == null) {
            active = select(System.getProperty("cn1.simulator.backend", "swing"));
        }
        return active;
    }

    /**
     * Overrides the active backend. Intended for tests and embedding
     * scenarios; normal selection goes through the system property.
     *
     * @param backend the backend to activate, or null to re-resolve on next use
     */
    public static synchronized void setActive(SimulatorBackend backend) {
        active = backend;
    }

    private static SimulatorBackend select(String id) {
        if (!"swing".equalsIgnoreCase(id)) {
            try {
                for (SimulatorBackend backend : ServiceLoader.load(SimulatorBackend.class)) {
                    if (id.equalsIgnoreCase(backend.getId())) {
                        return backend;
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
            System.err.println("cn1.simulator.backend=" + id
                    + " did not match any registered SimulatorBackend; falling back to swing");
        }
        return new SwingSimulatorBackend();
    }
}
