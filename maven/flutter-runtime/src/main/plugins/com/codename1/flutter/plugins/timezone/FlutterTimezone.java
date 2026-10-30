/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.plugins.timezone;

import java.util.TimeZone;

import dart.async.Future;
import dart.core.DartList;

/**
 * flutter_timezone's {@code FlutterTimezone}, over {@code java.util.TimeZone}. A device
 * class library may know only the default zone, so the available list always holds the
 * local one; a zone with no identifier answers {@code UTC}.
 */
public final class FlutterTimezone {

    private FlutterTimezone() {
    }

    static String localId() {
        TimeZone tz = TimeZone.getDefault();
        String id = tz == null ? null : tz.getID();
        return id == null || id.length() == 0 ? "UTC" : id;
    }

    public static Future<String> getLocalTimezone() {
        return Future.value(localId());
    }

    public static Future<DartList<String>> getAvailableTimezones() {
        DartList<String> out = new DartList<String>();
        String local = localId();
        boolean hasLocal = false;
        String[] ids = TimeZone.getAvailableIDs();
        if (ids != null) {
            for (String id : ids) {
                if (id != null && id.length() > 0) {
                    out.add(id);
                    hasLocal |= id.equals(local);
                }
            }
        }
        if (!hasLocal) {
            out.add(local);
        }
        return Future.value(out);
    }
}
