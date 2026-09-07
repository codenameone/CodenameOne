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
package com.codename1.impl.javase;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * getDevicePixelRatio must describe the device being simulated, not the machine
 * simulating it.
 *
 * <p>The simulator loads a skin to stand in for a phone, and the host display's
 * backing scale is then the wrong number entirely: a 2x machine showing a 3x
 * phone skin would report 2, and anything laying out in the platform's logical
 * units comes out two thirds of its size. The contract already defines the
 * "not reported" answer, which sends the caller to the density bucket -- that
 * describes the simulated device, and is what this call resolved to before the
 * platform reported a scale at all.</p>
 *
 * <p>With no skin the process really is a desktop application on this display,
 * and the backing scale is exactly what was asked for.</p>
 *
 * <p>Exercises the decision directly rather than through a constructed port:
 * JavaSEPort's constructor initialises a look and feel that needs a native
 * library absent on some machines, which is why several tests in this module
 * cannot run everywhere. The decision is the part that was wrong.</p>
 */
public class JavaSEPortDevicePixelRatioTest {

    @Test
    public void aSkinnedSimulatorDoesNotReportTheHostScale() {
        assertEquals(0f, JavaSEPort.devicePixelRatioFor(false, 2.0, 0f), 0.0001f,
                "a skin stands in for another device, so the host's scale is not its scale");
    }

    @Test
    public void aDesktopApplicationReportsTheHostScale() {
        assertEquals(2f, JavaSEPort.devicePixelRatioFor(true, 2.0, 0f), 0.0001f,
                "with no skin this is a desktop app on this display, which is what the API asks");
    }

    @Test
    public void anUnknownHostScaleFallsBackRatherThanReportingZeroPointZero() {
        assertEquals(0f, JavaSEPort.devicePixelRatioFor(true, 0.0, 0f), 0.0001f,
                "a host that reports no scale is 'not reported', not a ratio of zero");
    }
}
