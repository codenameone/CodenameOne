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
package android.content.pm;

/// The foreground service types `Service.startForeground` takes. They decide
/// nothing here: the Codename One foreground service is the same whatever the
/// type.
public class ServiceInfo {

    public static final int FOREGROUND_SERVICE_TYPE_NONE = 0;
    public static final int FOREGROUND_SERVICE_TYPE_DATA_SYNC = 1;
    public static final int FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK = 2;
    public static final int FOREGROUND_SERVICE_TYPE_PHONE_CALL = 4;
    public static final int FOREGROUND_SERVICE_TYPE_LOCATION = 8;
    public static final int FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE = 16;
    public static final int FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION = 32;
    public static final int FOREGROUND_SERVICE_TYPE_CAMERA = 64;
    public static final int FOREGROUND_SERVICE_TYPE_MICROPHONE = 128;
    public static final int FOREGROUND_SERVICE_TYPE_HEALTH = 256;
    public static final int FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING = 512;
    public static final int FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED = 1024;
    public static final int FOREGROUND_SERVICE_TYPE_SHORT_SERVICE = 2048;
    public static final int FOREGROUND_SERVICE_TYPE_SPECIAL_USE = 0x40000000;
    public static final int FOREGROUND_SERVICE_TYPE_MANIFEST = -1;

    public String name;
    public String packageName;
}
