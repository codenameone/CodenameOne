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
package com.codenameone.developerguide.snippets;

import com.codename1.gaming.GameServices;
import com.codename1.gaming.Gamepad;
import com.codename1.gaming.Gamepads;

/** Compiled source snippets for the Steam guide chapter. */
public class SteamSnippets {
    private float stickX;

    /** Reads the first controller once per frame. */
    public void readPad() {
        // tag::steam-java-001[]
        Gamepad pad = Gamepads.getGamepad(0);
        if (pad != null && pad.isConnected()) {
            float x = pad.getAxis(Gamepad.AXIS_LEFT_X);   // -1..1, dead zone applied
            if (pad.isButtonDown(Gamepad.BUTTON_A)) {
                jump();
            }
        }
        // end::steam-java-001[]
    }

    /** Achievements, stats and a cloud save after a won match. */
    public void recordWin(byte[] saveBytes) {
        // tag::steam-java-002[]
        if (GameServices.isAvailable()) {             // true on Steam
            GameServices.unlockAchievement("FIRST_WIN");
            GameServices.setStat("wins", GameServices.getStatInt("wins", 0) + 1);
            GameServices.commit();
            GameServices.writeSave("slot1", saveBytes);
        }
        // end::steam-java-002[]
    }

    private void jump() {
        stickX = 0;
    }
}
