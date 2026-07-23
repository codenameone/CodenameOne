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
package com.codename1.bluetooth;

import com.codename1.bluetooth.le.BluetoothLE;
import com.codename1.bluetooth.le.ScanResult;

/**
 * Scripted {@link BluetoothLE} whose platform scan is entirely test-driven:
 * {@code startPlatformScan}/{@code stopPlatformScan} only record state and
 * the test injects advertisement sightings via
 * {@link #injectScanResult(ScanResult)} (which routes through the core
 * {@code fireScanResult} demultiplexer).
 */
public class FakeBluetoothLE extends BluetoothLE {

    private boolean scanSupported = true;
    private boolean platformScanActive;
    private int startCount;
    private int stopCount;
    private RuntimeException startFailure;

    public FakeBluetoothLE setScanSupported(boolean scanSupported) {
        this.scanSupported = scanSupported;
        return this;
    }

    /**
     * Makes the next {@code startPlatformScan} throw, exercising the
     * start-failure path of {@code startScan}.
     */
    public FakeBluetoothLE failNextStart(RuntimeException failure) {
        this.startFailure = failure;
        return this;
    }

    /**
     * {@code true} while the single underlying platform scan is running.
     */
    public boolean isPlatformScanActive() {
        return platformScanActive;
    }

    /**
     * How many times the platform scan was started.
     */
    public int getStartCount() {
        return startCount;
    }

    /**
     * How many times the platform scan was stopped.
     */
    public int getStopCount() {
        return stopCount;
    }

    /**
     * Injects one advertisement sighting into the core demultiplexer, as a
     * port would from its platform scan callback.
     */
    public void injectScanResult(ScanResult result) {
        fireScanResult(result);
    }

    /**
     * Simulates the OS aborting the platform scan.
     */
    public void failScan(BluetoothException reason) {
        platformScanActive = false;
        fireScanFailed(reason);
    }

    @Override
    protected boolean isScanSupported() {
        return scanSupported;
    }

    @Override
    protected void startPlatformScan() {
        if (startFailure != null) {
            RuntimeException ex = startFailure;
            startFailure = null;
            throw ex;
        }
        platformScanActive = true;
        startCount++;
    }

    @Override
    protected void stopPlatformScan() {
        platformScanActive = false;
        stopCount++;
    }
}
