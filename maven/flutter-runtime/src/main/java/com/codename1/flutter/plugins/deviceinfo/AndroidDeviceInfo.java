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
package com.codename1.flutter.plugins.deviceinfo;

/** {@code AndroidDeviceInfo}, with {@code version} flattened to its own object. */
public class AndroidDeviceInfo extends BaseDeviceInfo {
    private final AndroidBuildVersion version = new AndroidBuildVersion();

    AndroidDeviceInfo() {
        data.put("model", model());
        data.put("manufacturer", manufacturer());
        data.put("isPhysicalDevice", Boolean.valueOf(isPhysicalDevice()));
    }

    public String model() {
        return DeviceInfoPlugin.prop("Platform", "");
    }

    public String brand() {
        return manufacturer();
    }

    public String manufacturer() {
        return DeviceInfoPlugin.prop("Manufacturer", "");
    }

    public String device() {
        return model();
    }

    public String id() {
        return DeviceInfoPlugin.prop("UDID", "");
    }

    public boolean isPhysicalDevice() {
        return DeviceInfoPlugin.physical();
    }

    public AndroidBuildVersion version() {
        return version;
    }
}
