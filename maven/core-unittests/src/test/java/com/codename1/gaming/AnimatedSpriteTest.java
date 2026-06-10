package com.codename1.gaming;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Image;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Unit tests for {@link AnimatedSprite}: frame advancing, looping vs one-shot
/// playback and the playback controls.
class AnimatedSpriteTest extends UITestBase {

    private static Image[] frames(int n) {
        Image[] f = new Image[n];
        for (int i = 0; i < n; i++) {
            f[i] = Image.createImage(8, 8, 0xff000000 | i);
        }
        return f;
    }

    @Test
    void constructorDefaults() {
        Image[] f = frames(3);
        AnimatedSprite a = new AnimatedSprite(f, 0.1);
        assertEquals(3, a.getFrameCount());
        assertEquals(0, a.getCurrentFrame());
        assertTrue(a.isPlaying());      // plays by default
        assertTrue(a.isLooping());      // loops by default
        assertEquals(0.1, a.getFrameDuration(), 0.001);
        assertSame(f[0], a.getImage());
    }

    @Test
    void advancesOneFramePerDuration() {
        Image[] f = frames(3);
        AnimatedSprite a = new AnimatedSprite(f, 0.1);
        a.onUpdate(0.1);
        assertEquals(1, a.getCurrentFrame());
        assertSame(f[1], a.getImage());
        a.onUpdate(0.1);
        assertEquals(2, a.getCurrentFrame());
    }

    @Test
    void loopsBackToStart() {
        AnimatedSprite a = new AnimatedSprite(frames(3), 0.1);
        a.onUpdate(0.1);   // 1
        a.onUpdate(0.1);   // 2
        a.onUpdate(0.1);   // wraps to 0
        assertEquals(0, a.getCurrentFrame());
        assertTrue(a.isPlaying());
    }

    @Test
    void oneShotStopsOnLastFrame() {
        AnimatedSprite a = new AnimatedSprite(frames(3), 0.1);
        a.setLooping(false);
        assertFalse(a.isLooping());
        a.onUpdate(1.0);   // far past the end
        assertEquals(2, a.getCurrentFrame());   // clamps to last
        assertFalse(a.isPlaying());             // and stops
    }

    @Test
    void pauseFreezesAdvance() {
        AnimatedSprite a = new AnimatedSprite(frames(3), 0.1);
        a.pause();
        assertFalse(a.isPlaying());
        a.onUpdate(1.0);
        assertEquals(0, a.getCurrentFrame());   // no advance while paused
        a.play();
        assertTrue(a.isPlaying());
    }

    @Test
    void stopResets() {
        Image[] f = frames(3);
        AnimatedSprite a = new AnimatedSprite(f, 0.1);
        a.onUpdate(0.1);
        a.onUpdate(0.1);
        a.stop();
        assertFalse(a.isPlaying());
        assertEquals(0, a.getCurrentFrame());
        assertSame(f[0], a.getImage());
    }

    @Test
    void setCurrentFrameAndDuration() {
        Image[] f = frames(4);
        AnimatedSprite a = new AnimatedSprite(f, 0.1);
        a.setCurrentFrame(2);
        assertEquals(2, a.getCurrentFrame());
        assertSame(f[2], a.getImage());
        a.setFrameDuration(0.25);
        assertEquals(0.25, a.getFrameDuration(), 0.001);
    }

    @Test
    void builtFromSpriteSheet() {
        Image sheet = Image.createImage(64, 16, 0xffabcdef);   // 4 cols x 1 row of 16x16
        SpriteSheet ss = new SpriteSheet(sheet, 16, 16);
        AnimatedSprite a = new AnimatedSprite(ss, new int[]{0, 1, 2, 3}, 0.05);
        assertEquals(4, a.getFrameCount());
        assertNotNull(a.getImage());
    }
}
