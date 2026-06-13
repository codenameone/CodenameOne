package com.codename1.gaming;

import com.codename1.gpu.Camera;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Unit tests for {@link GameCamera}: 2D/3D mode switching, eye/target/up bookkeeping
/// and applying the configuration onto a {@link Camera}.
class GameCameraTest {

    @Test
    void defaultsToOrtho2D() {
        GameCamera c = new GameCamera();
        assertEquals(GameCamera.MODE_ORTHO_2D, c.getMode());
    }

    @Test
    void perspectiveModeAndFov() {
        GameCamera c = new GameCamera();
        assertSame(c, c.setPerspective(60f, 0.1f, 200f));
        assertEquals(GameCamera.MODE_PERSPECTIVE, c.getMode());
        assertEquals(60f, c.getFov(), 0.001);
        assertSame(c, c.setOrthographic2D());
        assertEquals(GameCamera.MODE_ORTHO_2D, c.getMode());
    }

    @Test
    void positionTargetUpAreChainableAndStored() {
        GameCamera c = new GameCamera();
        assertSame(c, c.setPosition(1f, 2f, 3f));
        assertSame(c, c.setTarget(4f, 5f, 6f));
        assertSame(c, c.setUp(0f, 1f, 0f));
        assertEquals(1f, c.getEyeX(), 0.001);
        assertEquals(2f, c.getEyeY(), 0.001);
        assertEquals(3f, c.getEyeZ(), 0.001);
        assertEquals(4f, c.getTargetX(), 0.001);
        assertEquals(5f, c.getTargetY(), 0.001);
        assertEquals(6f, c.getTargetZ(), 0.001);
    }

    @Test
    void applyPerspectiveCopiesEyeToCamera() {
        GameCamera c = new GameCamera();
        c.setPerspective(50f, 0.5f, 100f).setPosition(7f, 8f, 9f).setTarget(0f, 0f, 0f);
        Camera cam = new Camera();
        c.apply(cam, 800, 600);
        assertEquals(7f, cam.getEyeX(), 0.001);
        assertEquals(8f, cam.getEyeY(), 0.001);
        assertEquals(9f, cam.getEyeZ(), 0.001);
    }

    @Test
    void applyOrthoPlacesCameraStraightOn() {
        GameCamera c = new GameCamera();   // ortho 2D
        Camera cam = new Camera();
        c.apply(cam, 480, 800);
        // the 2D pixel-space camera looks straight down -z from (0,0,1)
        assertEquals(0f, cam.getEyeX(), 0.001);
        assertEquals(0f, cam.getEyeY(), 0.001);
        assertEquals(1f, cam.getEyeZ(), 0.001);
    }
}
