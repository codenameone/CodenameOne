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
package com.codename1.sensors;

import java.util.HashSet;
import java.util.Set;

/**
 * Deterministic {@link MotionSensorManager} for unit tests. Sensors are
 * "supported" per the flags handed to the constructor, native start/stop
 * calls are counted, and tests push readings synchronously through
 * {@link #fire(int, float, float, float, long)} (delivered via
 * {@code Display.callSerially}, so drain with {@code flushSerialCalls()}).
 */
public class FakeMotionSensorManager extends MotionSensorManager {

    private final Set<Integer> supported = new HashSet<Integer>();
    public final Set<Integer> started = new HashSet<Integer>();
    public int startCount;
    public int stopCount;

    public FakeMotionSensorManager(int... supportedTypes) {
        for (int t : supportedTypes) {
            supported.add(t);
        }
    }

    @Override
    protected boolean isNativeSensorSupported(int type) {
        return supported.contains(type);
    }

    @Override
    protected void startNativeSensor(int type) {
        started.add(type);
        startCount++;
    }

    @Override
    protected void stopNativeSensor(int type) {
        started.remove(Integer.valueOf(type));
        stopCount++;
    }

    @Override
    protected boolean readNativeSensor(int type, float[] out) {
        // The background poll loop never produces readings; tests push
        // events explicitly through fire().
        return false;
    }

    /**
     * Pushes a reading into the sensor of the given type, exactly as the
     * platform poll loop would.
     */
    public void fire(int type, float x, float y, float z, long timestampMillis) {
        getSensor(type).dispatch(x, y, z, timestampMillis);
    }
}
