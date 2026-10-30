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
package com.codenameone.developerguide.snippets.generated;

import com.codename1.hardware.*;
import com.codename1.io.Log;

class EmbeddedLinuxJava001Snippet {

    void wireBoard() {
        // tag::embedded-linux-java-001[]
        Hardware hw = Hardware.getInstance();
        try {
            if (hw.isSupported(Hardware.FEATURE_GPIO)) {
                GpioPin led = hw.openGpioOutput("gpiochip0", 17, false);
                led.setHigh(true);
                GpioPin button = hw.openGpioInput("gpiochip0", 27, GpioPin.BIAS_PULL_UP, GpioPin.EDGE_FALLING);
                button.setListener((pin, rising, nanos) -> {
                    try {
                        led.setHigh(!led.isHigh());
                    } catch (HardwareException e) {
                        Log.e(e);
                    }
                });
            }
            if (hw.isSupported(Hardware.FEATURE_I2C)) {
                I2cDevice sensor = hw.openI2c(1, 0x48);
                Log.p("Sensor reads " + sensor.readRegister(0));
            }
        } catch (HardwareException e) {
            Log.e(e);
        }
        // end::embedded-linux-java-001[]
    }
}
