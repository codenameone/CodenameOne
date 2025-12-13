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
