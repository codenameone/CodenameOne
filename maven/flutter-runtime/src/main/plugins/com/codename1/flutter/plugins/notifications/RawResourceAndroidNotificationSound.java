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
package com.codename1.flutter.plugins.notifications;

/**
 * {@code RawResourceAndroidNotificationSound('name')}: an Android {@code res/raw}
 * sound. Codename One's LocalNotification plays a file bundled in the app's default
 * package whose name starts with {@code notification_sound}, so {@code 'bells'} maps to
 * {@code /notification_sound_bells.mp3} (a name that already has the prefix keeps it, and
 * a name with an extension keeps that extension). The file must be bundled under that
 * name; Android's raw resource folder is not read.
 */
public class RawResourceAndroidNotificationSound {
    private final String sound;

    public RawResourceAndroidNotificationSound(String sound) {
        this.sound = sound;
    }

    /** The resource name as the app wrote it. */
    public String sound() {
        return sound;
    }

    /** The path handed to {@code LocalNotification.setAlertSound}. */
    public String alertSoundPath() {
        if (sound == null || sound.length() == 0) {
            return null;
        }
        String name = sound.startsWith("/") ? sound.substring(1) : sound;
        if (!name.startsWith("notification_sound")) {
            name = "notification_sound_" + name;
        }
        if (name.indexOf('.') < 0) {
            name = name + ".mp3";
        }
        return "/" + name;
    }
}
