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
package com.demo;

import com.codename1.backend.Backend;
import com.codename1.backend.HttpServer;
import com.codename1.hardware.Hardware;
import com.codename1.hardware.HardwareException;

/// The headless hardware target: a started server installs the kernel
/// implementation, so `Hardware.getInstance()` answers for the board it runs
/// on. On Linux it reports support and lists what the kernel exposes; built
/// for another host every feature is there but every open fails.
public final class HwCheck {
    private HwCheck() {
    }

    public static void main(String[] args) throws Exception {
        Backend backend = Backend.builder().quiet().host("127.0.0.1").port(0)
                .handler(new HttpServer.Handler() {
                    @Override
                    public HttpServer.Response handle(HttpServer.Request request) {
                        return null;
                    }
                }).start();
        Hardware hw = Hardware.getInstance();
        System.out.println("HWCHECK supported=" + hw.isSupported(Hardware.FEATURE_GPIO)
                + " gpio=" + hw.listGpioChips().length + " i2c=" + hw.listI2cBuses().length
                + " serial=" + hw.listSerialPorts().length + " can=" + hw.listCanInterfaces().length);
        String open;
        try {
            hw.openGpioOutput("gpiochip0", 17, false).close();
            open = "opened";
        } catch (HardwareException e) {
            open = "errno " + e.getErrno();
        }
        backend.stop();
        System.out.println("HWCHECK gpio " + open);
        System.out.println(hw.isSupported() ? "HWCHECK OK" : "HWCHECK FAILED");
    }
}
