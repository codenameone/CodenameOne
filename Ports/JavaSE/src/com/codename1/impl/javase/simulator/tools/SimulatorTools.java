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
package com.codename1.impl.javase.simulator.tools;

import com.codename1.impl.javase.LocationSimulation;
import com.codename1.impl.javase.NetworkMonitor;
import com.codename1.impl.javase.PerformanceMonitor;
import com.codename1.impl.javase.PushSimulator;

/**
 * Static registry for the simulator's tool windows (network monitor,
 * performance monitor, location simulation, push simulator).
 *
 * <p>Tool state lives here, outside the implementation instance, because
 * Display.init creates a fresh implementation (and decorator chain) on every
 * hot reload while tool windows survive across reloads. The tool proxies in
 * {@code com.codename1.impl.javase.simulator.proxy} look tools up through this
 * registry on each intercepted call, and the simulator chrome (menus, app
 * frame) creates and disposes the tools through it.</p>
 */
public class SimulatorTools {
    private static NetworkMonitor networkMonitor;
    private static PerformanceMonitor performanceMonitor;
    private static LocationSimulation locationSimulation;
    private static PushSimulator pushSimulator;
    private static boolean monitorsBlocked;
    private static volatile boolean slowConnectionMode;
    private static volatile boolean disconnectedMode;

    private SimulatorTools() {
    }

    /**
     * Prevents monitor tool windows from opening automatically during init.
     * Used by embedded/headless contexts (e.g. unit tests, CN panel embedding)
     * where preference-driven auto-open would pop unwanted windows.
     */
    public static void blockMonitors() {
        monitorsBlocked = true;
    }

    /**
     * @return true when monitor auto-open has been suppressed via blockMonitors()
     */
    public static boolean isMonitorsBlocked() {
        return monitorsBlocked;
    }

    /**
     * @return the network monitor, or null when it is not open
     */
    public static NetworkMonitor getNetworkMonitor() {
        return networkMonitor;
    }

    /**
     * Installs the network monitor instance created by the simulator chrome,
     * or null to detach it.
     *
     * @param monitor the network monitor instance or null
     */
    public static void setNetworkMonitor(NetworkMonitor monitor) {
        networkMonitor = monitor;
    }

    /**
     * Disposes the network monitor window (when open) and detaches it.
     */
    public static void disposeNetworkMonitor() {
        if (networkMonitor != null) {
            networkMonitor.dispose();
            networkMonitor = null;
        }
    }

    /**
     * @return the performance monitor, or null when it is not open
     */
    public static PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }

    /**
     * Installs the performance monitor instance created by the simulator
     * chrome, or null to detach it.
     *
     * @param monitor the performance monitor instance or null
     */
    public static void setPerformanceMonitor(PerformanceMonitor monitor) {
        performanceMonitor = monitor;
    }

    /**
     * Disposes the performance monitor window (when open) and detaches it.
     */
    public static void disposePerformanceMonitor() {
        if (performanceMonitor != null) {
            performanceMonitor.dispose();
            performanceMonitor = null;
        }
    }

    /**
     * @return the location simulation dialog, or null when it was never opened
     */
    public static LocationSimulation getLocationSimulation() {
        return locationSimulation;
    }

    /**
     * @return the location simulation dialog, creating it if necessary
     */
    public static LocationSimulation getOrCreateLocationSimulation() {
        if (locationSimulation == null) {
            locationSimulation = new LocationSimulation();
        }
        return locationSimulation;
    }

    /**
     * @return the push simulator dialog, or null when it was never opened
     */
    public static PushSimulator getPushSimulator() {
        return pushSimulator;
    }

    /**
     * @return the push simulator dialog, creating it if necessary
     */
    public static PushSimulator getOrCreatePushSimulator() {
        if (pushSimulator == null) {
            pushSimulator = new PushSimulator();
        }
        return pushSimulator;
    }

    /**
     * Toggles slow-connection simulation, applied to connection streams by the
     * NetworkConditionSimulator decorator. Driven by the simulator's
     * Tools/Network menu.
     *
     * @param slow true to simulate a slow connection
     */
    public static void setSlowConnectionMode(boolean slow) {
        slowConnectionMode = slow;
    }

    /**
     * @return true when slow-connection simulation is active
     */
    public static boolean isSlowConnectionMode() {
        return slowConnectionMode;
    }

    /**
     * Toggles disconnected-network simulation, applied by the
     * NetworkConditionSimulator decorator. Driven by the simulator's
     * Tools/Network menu.
     *
     * @param disconnected true to simulate a dropped network
     */
    public static void setDisconnectedMode(boolean disconnected) {
        disconnectedMode = disconnected;
    }

    /**
     * @return true when disconnected-network simulation is active
     */
    public static boolean isDisconnectedMode() {
        return disconnectedMode;
    }
}
