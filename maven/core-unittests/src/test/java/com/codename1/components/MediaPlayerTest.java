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
package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.media.Media;

import static org.junit.jupiter.api.Assertions.*;

class MediaPlayerTest extends UITestBase {

    @FormTest
    void testDefaultConstructor() {
        MediaPlayer player = new MediaPlayer();
        assertNotNull(player);
    }

    @FormTest
    void testConstructorWithMedia() {
        Media mockMedia = new MockMedia();
        MediaPlayer player = new MediaPlayer(mockMedia);
        assertNotNull(player);
    }

    @FormTest
    void testAutoPlayGetterAndSetter() {
        MediaPlayer player = new MediaPlayer();
        player.setAutoplay(true);
        assertTrue(player.isAutoplay());

        player.setAutoplay(false);
        assertFalse(player.isAutoplay());
    }

    @FormTest
    void testLoopGetterAndSetter() {
        MediaPlayer player = new MediaPlayer();
        player.setLoop(true);
        assertTrue(player.isLoop());

        player.setLoop(false);
        assertFalse(player.isLoop());
    }

    @FormTest
    void testDataSourceGetterAndSetter() {
        MediaPlayer player = new MediaPlayer();
        player.setDataSource("http://example.com/video.mp4");
        assertEquals("http://example.com/video.mp4", player.getDataSource());
    }

    @FormTest
    void testHideNativeVideoControls() {
        MediaPlayer player = new MediaPlayer();
        assertFalse(player.isHideNativeVideoControls());

        player.setHideNativeVideoControls(true);
        assertTrue(player.isHideNativeVideoControls());
    }

    @FormTest
    void theProgressTimerBindsToTheWindowThePlayerLivesIn() throws Exception {
        com.codename1.testing.TestWindowManager wm =
                implementation.setMultiWindowSupported(true);
        assertNotNull(wm);
        com.codename1.ui.Window w = new com.codename1.ui.Window("player",
                new com.codename1.ui.layouts.BorderLayout());
        w.setWindowSize(400, 300);
        MediaPlayer player = new MediaPlayer(new MockMedia());
        w.add(com.codename1.ui.layouts.BorderLayout.CENTER, player);
        w.show();
        flushSerialCalls();

        // checkProgressSlider() handed getComponentForm() to UITimer, which dereferences
        // what it is bound to -- so starting playback threw on the event dispatch thread
        // after the media had already begun.
        java.lang.reflect.Method check =
                MediaPlayer.class.getDeclaredMethod("checkProgressSlider");
        check.setAccessible(true);
        check.invoke(player);

        java.lang.reflect.Field updater =
                MediaPlayer.class.getDeclaredField("progressUpdater");
        updater.setAccessible(true);
        Object timer = updater.get(player);
        assertNotNull(timer, "playback should have created a progress timer");

        java.lang.reflect.Field bound =
                com.codename1.ui.util.UITimer.class.getDeclaredField("bound");
        bound.setAccessible(true);
        assertSame(w, bound.get(timer),
                "the progress timer must be bound to the window the player lives in");

        w.dispose();
    }

    private static class MockMedia implements Media {
        @Override
        public void play() {}

        @Override
        public void pause() {}

        @Override
        public void prepare() {}

        @Override
        public void cleanup() {}

        @Override
        public int getTime() { return 0; }

        @Override
        public void setTime(int time) {}

        @Override
        public int getDuration() { return 0; }

        @Override
        public void setVolume(int vol) {}

        @Override
        public int getVolume() { return 0; }

        @Override
        public boolean isPlaying() { return false; }

        @Override
        public com.codename1.ui.Component getVideoComponent() { return null; }

        @Override
        public boolean isVideo() { return false; }

        @Override
        public boolean isFullScreen() { return false; }

        @Override
        public void setFullScreen(boolean fullScreen) {}

        @Override
        public void setNativePlayerMode(boolean nativePlayer) {}

        @Override
        public boolean isNativePlayerMode() { return false; }

        @Override
        public void setVariable(String key, Object value) {}

        @Override
        public Object getVariable(String key) { return null; }
    }
}
