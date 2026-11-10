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
package com.codename1.impl.linux;

import com.codename1.gaming.GameServices;
import com.codename1.gaming.GameServicesBackend;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.TextArea;

/// Steam as the `GameServices` backend. The natives are in `cn1_linux_steam.c`,
/// which loads `libsteam_api.so` from beside the executable at run time: an
/// application not started by Steam, or built without the library, is simply
/// "not available" and every call is a no-op.
///
/// Callbacks (the overlay opening and closing, the floating keyboard closing)
/// are pumped on a thread of their own at about 30 Hz and delivered on the EDT.
final class LinuxGameServices extends GameServicesBackend {
    // Mirrors EV_* in cn1_linux_steam.c.
    private static final int EV_OVERLAY_SHOWN = 1;
    private static final int EV_OVERLAY_HIDDEN = 2;
    private static final int EV_KEYBOARD_DISMISSED = 3;
    /// The field Steam's floating keyboard was opened for. EDT only.
    private static Component keyboardTarget;

    private static final int DECK = 1;
    private static final int BIG_PICTURE = 2;

    private boolean tried;
    private boolean available;

    /// Installs the backend, first letting Steam relaunch an application that
    /// was started outside it (the process then exits).
    static void install() {
        if (LinuxNative.steamRestartIfNecessary(0)) {
            System.exit(0);
        }
        GameServices.setBackend(new LinuxGameServices());
    }

    public String getName() {
        return "steam";
    }

    public boolean isAvailable() {
        if (!tried) {
            tried = true;
            available = LinuxNative.steamInit();
            if (available) {
                startPump();
            }
        }
        return available;
    }

    private static void startPump() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                for (;;) {
                    final int ev = LinuxNative.steamPump();
                    if (ev == EV_OVERLAY_SHOWN || ev == EV_OVERLAY_HIDDEN) {
                        Display.getInstance().callSerially(new Runnable() {
                            public void run() {
                                GameServices.firePlatformUIChanged(ev == EV_OVERLAY_SHOWN);
                            }
                        });
                        continue;
                    }
                    if (ev == EV_KEYBOARD_DISMISSED) {
                        // Closing the floating keyboard is the player saying "done":
                        // end the edit, or the field stays editing with no way to type.
                        Display.getInstance().callSerially(new Runnable() {
                            public void run() {
                                Component c = keyboardTarget;
                                keyboardTarget = null;
                                // stopEditing is a no-op unless c is still editing.
                                if (c != null) {
                                    Display.getInstance().stopEditing(c);
                                }
                            }
                        });
                        continue;
                    }
                    try {
                        Thread.sleep(33);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }, "Steam callbacks");
        t.setDaemon(true);
        t.start();
    }

    public String getPlayerId() {
        long id = LinuxNative.steamUserId();
        return id == 0 ? "" : String.valueOf(id);
    }

    public String getPlayerName() {
        return LinuxNative.steamPersonaName();
    }

    public boolean setAchievement(String id, boolean unlocked) {
        return LinuxNative.steamAchievement(id, unlocked);
    }

    public boolean isAchievementUnlocked(String id) {
        return LinuxNative.steamIsAchieved(id);
    }

    public int getStatInt(String id, int def) {
        return LinuxNative.steamGetStatInt(id, def);
    }

    public boolean setStatInt(String id, int value) {
        return LinuxNative.steamSetStatInt(id, value);
    }

    public float getStatFloat(String id, float def) {
        return LinuxNative.steamGetStatFloat(id, def);
    }

    public boolean setStatFloat(String id, float value) {
        return LinuxNative.steamSetStatFloat(id, value);
    }

    public boolean commit() {
        return LinuxNative.steamStoreStats();
    }

    public boolean isCloudSaveAvailable() {
        return LinuxNative.steamCloudEnabled();
    }

    public boolean writeSave(String name, byte[] data) {
        return LinuxNative.steamCloudWrite(name, data);
    }

    public byte[] readSave(String name) {
        return LinuxNative.steamCloudRead(name);
    }

    public boolean hasSave(String name) {
        return LinuxNative.steamCloudExists(name);
    }

    public boolean deleteSave(String name) {
        return LinuxNative.steamCloudDelete(name);
    }

    public boolean setPresence(String key, String value) {
        return LinuxNative.steamRichPresence(key, value);
    }

    /// Steam add-ons are DLC app ids.
    public boolean ownsContent(String id) {
        int appId = parseId(id);
        return appId > 0 && LinuxNative.steamDlcInstalled(appId);
    }

    public boolean openPlatformUI(String target) {
        boolean web = target.regionMatches(true, 0, "https://", 0, 8)
                || target.regionMatches(true, 0, "http://", 0, 7);
        LinuxNative.steamOverlay(target, web);
        return true;
    }

    public boolean isControllerFirst() {
        return (LinuxNative.steamDeviceFlags() & (DECK | BIG_PICTURE)) != 0;
    }

    public String getLanguage() {
        return LinuxNative.steamLanguage();
    }

    /// Called as a text field starts editing: on a controller-first interface
    /// there is no keyboard, so Steam's floating one is shown over the field. It
    /// types into the focused editor as ordinary key events.
    static void onEditStarted(Component cmp, boolean singleLine, int constraint) {
        if (!GameServices.isAvailable() || !GameServices.isControllerFirst()) {
            return;
        }
        int mode;
        // The low 16 bits are the kind; the high ones are flags (PASSWORD...).
        int kind = constraint & 0xffff;
        if (kind == TextArea.NUMERIC || kind == TextArea.DECIMAL || kind == TextArea.PHONENUMBER) {
            mode = 3;
        } else if (kind == TextArea.EMAILADDR) {
            mode = 2;
        } else {
            mode = singleLine ? 0 : 1;
        }
        keyboardTarget = cmp;
        // Steam wants screen coordinates. Controller-first means Gaming Mode,
        // where gamescope shows the window fullscreen at the origin, and the
        // port draws in device pixels, so component coordinates are already
        // screen coordinates there. On a desktop the keyboard is never shown.
        LinuxNative.steamFloatingKeyboard(mode, cmp.getAbsoluteX(), cmp.getAbsoluteY(),
                cmp.getWidth(), cmp.getHeight());
    }

    private static int parseId(String id) {
        int v = 0;
        if (id == null || id.length() == 0 || id.length() > 9) {
            return -1;
        }
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            v = v * 10 + (c - '0');
        }
        return v;
    }
}
