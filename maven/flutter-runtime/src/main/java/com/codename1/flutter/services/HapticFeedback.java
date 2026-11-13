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
package com.codename1.flutter.services;

import com.codename1.ui.CN;
import dart.async.Future;

/// Flutter's `HapticFeedback`, as short device vibrations. Every call is a no-op
/// without a display or on a device that cannot vibrate.
public final class HapticFeedback {

    private HapticFeedback() {
    }

    private static Future<Object> buzz(int ms) {
        try {
            if (com.codename1.ui.Display.isInitialized()) {
                CN.vibrate(ms);
            }
        } catch (Throwable ignored) {
            // vibration unsupported here
        }
        return Future.value(null);
    }

    public static Future<Object> vibrate() {
        return buzz(200);
    }

    public static Future<Object> lightImpact() {
        return buzz(10);
    }

    public static Future<Object> mediumImpact() {
        return buzz(20);
    }

    public static Future<Object> heavyImpact() {
        return buzz(35);
    }

    public static Future<Object> selectionClick() {
        return buzz(5);
    }
}
