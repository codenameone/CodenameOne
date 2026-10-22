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

import com.codename1.ui.Display;

import dart.async.Future;

/**
 * device_info_plus's {@code DeviceInfoPlugin}, from Codename One's device properties.
 * Each platform's info object carries the fields apps commonly read -- model, OS
 * version, whether the device is physical -- and its {@code data} map holds them all.
 */
public class DeviceInfoPlugin {

    public DeviceInfoPlugin() {
    }

    static String prop(String key, String def) {
        return Display.isInitialized() ? Display.getInstance().getProperty(key, def) : def;
    }

    static String platform() {
        return Display.isInitialized() ? Display.getInstance().getPlatformName() : "se";
    }

    static boolean physical() {
        return Display.isInitialized() && !Display.getInstance().isSimulator();
    }

    public Future<AndroidDeviceInfo> androidInfo() {
        return Future.value(new AndroidDeviceInfo());
    }

    public Future<IosDeviceInfo> iosInfo() {
        return Future.value(new IosDeviceInfo());
    }

    /** The info of whichever platform this is. */
    public Future<BaseDeviceInfo> deviceInfo() {
        String p = platform();
        if ("ios".equals(p)) {
            return Future.value((BaseDeviceInfo) new IosDeviceInfo());
        }
        return Future.value((BaseDeviceInfo) new AndroidDeviceInfo());
    }
}
