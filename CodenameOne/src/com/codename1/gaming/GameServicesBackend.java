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

/// The platform side of `GameServices`: Steam, and later Game Center, Google
/// Play Games or Xbox Live. A port installs one with
/// `GameServices#setBackend(GameServicesBackend)`; applications never use this
/// class. Every method has a do-nothing default, so a backend overrides only
/// what its platform has.
///
/// A backend reports the platform's own interface opening over the game with
/// `GameServices#firePlatformUIChanged(boolean)`.
public abstract class GameServicesBackend {
    /// A short name for the platform, such as `"steam"`.
    public abstract String getName();

    /// True once the platform accepted the connection and a player is signed in.
    public abstract boolean isAvailable();

    /// See `GameServices#getPlayerId()`.
    public String getPlayerId() {
        return "";
    }

    /// See `GameServices#getPlayerName()`.
    public String getPlayerName() {
        return "";
    }

    /// See `GameServices#unlockAchievement(String)`.
    public boolean setAchievement(String id, boolean unlocked) {
        return false;
    }

    /// See `GameServices#isAchievementUnlocked(String)`.
    public boolean isAchievementUnlocked(String id) {
        return false;
    }

    /// See `GameServices#getStatInt(String, int)`.
    public int getStatInt(String id, int def) {
        return def;
    }

    /// See `GameServices#setStat(String, int)`.
    public boolean setStatInt(String id, int value) {
        return false;
    }

    /// See `GameServices#getStatFloat(String, float)`.
    public float getStatFloat(String id, float def) {
        return def;
    }

    /// See `GameServices#setStat(String, float)`.
    public boolean setStatFloat(String id, float value) {
        return false;
    }

    /// See `GameServices#commit()`.
    public boolean commit() {
        return false;
    }

    /// See `GameServices#isCloudSaveAvailable()`.
    public boolean isCloudSaveAvailable() {
        return false;
    }

    /// See `GameServices#writeSave(String, byte[])`.
    public boolean writeSave(String name, byte[] data) {
        return false;
    }

    /// See `GameServices#readSave(String)`.
    public byte[] readSave(String name) {
        return null;
    }

    /// See `GameServices#hasSave(String)`.
    public boolean hasSave(String name) {
        return false;
    }

    /// See `GameServices#deleteSave(String)`.
    public boolean deleteSave(String name) {
        return false;
    }

    /// See `GameServices#setPresence(String, String)`; a null key clears all.
    public boolean setPresence(String key, String value) {
        return false;
    }

    /// See `GameServices#ownsContent(String)`.
    public boolean ownsContent(String id) {
        return false;
    }

    /// See `GameServices#openPlatformUI(String)`.
    public boolean openPlatformUI(String target) {
        return false;
    }

    /// See `GameServices#isControllerFirst()`.
    public boolean isControllerFirst() {
        return false;
    }

    /// See `GameServices#getLanguage()`.
    public String getLanguage() {
        return "";
    }
}
