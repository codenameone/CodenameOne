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

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// {@link GameServices} answers defaults with no backend or a disconnected one,
/// and forwards to a connected backend.
class GameServicesTest {

    private static class FakeBackend extends GameServicesBackend {
        boolean available = true;
        final Map<String, Object> state = new HashMap<String, Object>();
        int commits;

        public String getName() {
            return "fake";
        }

        public boolean isAvailable() {
            return available;
        }

        public boolean setAchievement(String id, boolean unlocked) {
            state.put("ach:" + id, Boolean.valueOf(unlocked));
            return true;
        }

        public boolean isAchievementUnlocked(String id) {
            return Boolean.TRUE.equals(state.get("ach:" + id));
        }

        public int getStatInt(String id, int def) {
            Object v = state.get("stat:" + id);
            return v instanceof Integer ? ((Integer) v).intValue() : def;
        }

        public boolean setStatInt(String id, int value) {
            state.put("stat:" + id, Integer.valueOf(value));
            return true;
        }

        public boolean commit() {
            commits++;
            return true;
        }

        public boolean writeSave(String name, byte[] data) {
            state.put("save:" + name, data);
            return true;
        }

        public byte[] readSave(String name) {
            return (byte[]) state.get("save:" + name);
        }
    }

    @AfterEach
    void clear() {
        GameServices.setBackend(null);
    }

    @Test
    void noBackendAnswersDefaults() {
        assertFalse(GameServices.isAvailable());
        assertEquals("", GameServices.getPlatformName());
        assertFalse(GameServices.unlockAchievement("A"));
        assertEquals(7, GameServices.getStatInt("x", 7));
        assertNull(GameServices.readSave("s"));
        assertFalse(GameServices.commit());
        assertFalse(GameServices.isControllerFirst());
    }

    @Test
    void disconnectedBackendIsNotCalled() {
        FakeBackend b = new FakeBackend();
        b.available = false;
        GameServices.setBackend(b);
        assertFalse(GameServices.unlockAchievement("A"));
        assertTrue(b.state.isEmpty());
        assertEquals("fake", GameServices.getPlatformName());
    }

    @Test
    void connectedBackendForwards() {
        FakeBackend b = new FakeBackend();
        GameServices.setBackend(b);
        assertTrue(GameServices.isAvailable());
        assertTrue(GameServices.unlockAchievement("WIN"));
        assertTrue(GameServices.isAchievementUnlocked("WIN"));
        assertTrue(GameServices.resetAchievement("WIN"));
        assertFalse(GameServices.isAchievementUnlocked("WIN"));
        GameServices.setStat("wins", GameServices.getStatInt("wins", 0) + 1);
        assertEquals(1, GameServices.getStatInt("wins", 0));
        assertTrue(GameServices.commit());
        assertEquals(1, b.commits);
        byte[] data = {1, 2, 3};
        assertTrue(GameServices.writeSave("slot1", data));
        assertArrayEquals(data, GameServices.readSave("slot1"));
        assertFalse(GameServices.writeSave(null, data), "null names never reach the backend");
    }

    @Test
    void listenersHearThePlatformUI() {
        final boolean[] got = new boolean[2];
        GameServicesListener l = new GameServicesListener() {
            public void platformUIChanged(boolean shown) {
                got[shown ? 0 : 1] = true;
            }
        };
        GameServices.addListener(l);
        try {
            GameServices.firePlatformUIChanged(true);
            GameServices.firePlatformUIChanged(false);
            assertTrue(got[0] && got[1]);
        } finally {
            GameServices.removeListener(l);
        }
    }
}
