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
package com.codename1.gaming;

import java.util.ArrayList;

/// The gaming platform the application runs under: achievements, stats, cloud
/// saves, the signed in player and their presence.
///
/// One API for every platform. The first backend is Steam (the `steam` build
/// target); others plug in behind the same calls. Everything here is safe to
/// call everywhere: with no platform, or one that has not connected,
/// `#isAvailable()` is false and every call does nothing and answers its
/// default, so a game needs no platform checks around it.
///
/// ```java
/// GameServices.unlockAchievement("FIRST_WIN");
/// GameServices.setStat("wins", GameServices.getStatInt("wins", 0) + 1);
/// GameServices.commit();
/// ```
///
/// Identifiers are the ones configured with the platform (on Steam, the API
/// names entered in the Steamworks partner site).
///
/// **Pausing.** Register a `GameServicesListener` to hear when the platform's
/// own interface (the Steam overlay) opens over the game; pause while it is up.
///
/// Call from the EDT.
public final class GameServices {
    private static GameServicesBackend backend;
    private static final ArrayList LISTENERS = new ArrayList();

    private GameServices() {
    }

    /// Installs the platform backend. Called by ports.
    public static void setBackend(GameServicesBackend b) {
        backend = b;
    }

    /// The platform name (`"steam"`), or an empty string when there is none.
    public static String getPlatformName() {
        return backend == null ? "" : backend.getName();
    }

    /// True when a gaming platform is connected and a player signed in.
    public static boolean isAvailable() {
        return backend != null && backend.isAvailable();
    }

    private static GameServicesBackend live() {
        return backend != null && backend.isAvailable() ? backend : null;
    }

    /// The player's stable platform id (a Steam id, for instance), or an empty
    /// string.
    public static String getPlayerId() {
        GameServicesBackend b = live();
        return b == null ? "" : b.getPlayerId();
    }

    /// The player's display name, or an empty string.
    public static String getPlayerName() {
        GameServicesBackend b = live();
        return b == null ? "" : b.getPlayerName();
    }

    /// Unlocks an achievement. Platforms show their own notification. Persisted
    /// by `#commit()`.
    public static boolean unlockAchievement(String id) {
        GameServicesBackend b = live();
        return b != null && id != null && b.setAchievement(id, true);
    }

    /// Locks an achievement again, for testing; platforms may refuse it outside
    /// development builds.
    public static boolean resetAchievement(String id) {
        GameServicesBackend b = live();
        return b != null && id != null && b.setAchievement(id, false);
    }

    /// Whether an achievement is unlocked.
    public static boolean isAchievementUnlocked(String id) {
        GameServicesBackend b = live();
        return b != null && id != null && b.isAchievementUnlocked(id);
    }

    /// An integer stat, or `def` when unknown.
    public static int getStatInt(String id, int def) {
        GameServicesBackend b = live();
        return b == null || id == null ? def : b.getStatInt(id, def);
    }

    /// Sets an integer stat. Persisted by `#commit()`.
    public static boolean setStat(String id, int value) {
        GameServicesBackend b = live();
        return b != null && id != null && b.setStatInt(id, value);
    }

    /// A float stat, or `def` when unknown.
    public static float getStatFloat(String id, float def) {
        GameServicesBackend b = live();
        return b == null || id == null ? def : b.getStatFloat(id, def);
    }

    /// Sets a float stat. Persisted by `#commit()`.
    public static boolean setStat(String id, float value) {
        GameServicesBackend b = live();
        return b != null && id != null && b.setStatFloat(id, value);
    }

    /// Sends achievement and stat changes to the platform. Call it at natural
    /// points (end of a level), not every frame: platforms rate limit it.
    public static boolean commit() {
        GameServicesBackend b = live();
        return b != null && b.commit();
    }

    /// Whether saves are synced through the platform for this player.
    public static boolean isCloudSaveAvailable() {
        GameServicesBackend b = live();
        return b != null && b.isCloudSaveAvailable();
    }

    /// Writes a cloud save, replacing one with the same name.
    public static boolean writeSave(String name, byte[] data) {
        GameServicesBackend b = live();
        return b != null && name != null && data != null && b.writeSave(name, data);
    }

    /// Reads a cloud save, or null when there is none.
    public static byte[] readSave(String name) {
        GameServicesBackend b = live();
        return b == null || name == null ? null : b.readSave(name);
    }

    /// Whether a cloud save exists.
    public static boolean hasSave(String name) {
        GameServicesBackend b = live();
        return b != null && name != null && b.hasSave(name);
    }

    /// Deletes a cloud save.
    public static boolean deleteSave(String name) {
        GameServicesBackend b = live();
        return b != null && name != null && b.deleteSave(name);
    }

    /// Sets what friends see the player doing. On Steam `key` is a rich
    /// presence key (`"status"` is shown as is).
    public static boolean setPresence(String key, String value) {
        GameServicesBackend b = live();
        return b != null && key != null && b.setPresence(key, value);
    }

    /// Clears every presence value.
    public static boolean clearPresence() {
        GameServicesBackend b = live();
        return b != null && b.setPresence(null, null);
    }

    /// Whether the player owns an add-on (on Steam, a DLC app id).
    public static boolean ownsContent(String id) {
        GameServicesBackend b = live();
        return b != null && id != null && b.ownsContent(id);
    }

    /// Opens the platform's own interface over the game. `target` names a page:
    /// on Steam one of the overlay dialogs (`"friends"`, `"achievements"`,
    /// `"stats"`, `"community"`, `"settings"`) or an `https://` URL. Answers
    /// false when the platform has no such interface.
    public static boolean openPlatformUI(String target) {
        GameServicesBackend b = live();
        return b != null && target != null && b.openPlatformUI(target);
    }

    /// True when the player is on a controller-first interface (a Steam Deck,
    /// Steam's Big Picture / Gaming Mode): size touch targets and text for it and
    /// expect no mouse.
    public static boolean isControllerFirst() {
        GameServicesBackend b = live();
        return b != null && b.isControllerFirst();
    }

    /// The language the player chose for the game on the platform, as the
    /// platform names it (Steam: `"english"`, `"german"`...), or an empty string.
    public static String getLanguage() {
        GameServicesBackend b = live();
        return b == null ? "" : b.getLanguage();
    }

    /// Registers a listener.
    public static void addListener(GameServicesListener l) {
        if (l != null && !LISTENERS.contains(l)) {
            LISTENERS.add(l);
        }
    }

    /// Unregisters a listener.
    public static void removeListener(GameServicesListener l) {
        LISTENERS.remove(l);
    }

    /// Tells listeners the platform's interface opened or closed. Called by
    /// backends, on the EDT.
    public static void firePlatformUIChanged(boolean shown) {
        Object[] ls = LISTENERS.toArray();
        for (Object l : ls) {
            ((GameServicesListener) l).platformUIChanged(shown);
        }
    }
}
