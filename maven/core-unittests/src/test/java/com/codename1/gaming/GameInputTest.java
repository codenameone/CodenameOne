package com.codename1.gaming;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Display;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Unit tests for {@link GameInput}: level vs edge key state, pointer state and the
/// analog axes fed by the joystick.
class GameInputTest extends UITestBase {

    @Test
    void keyLevelState() {
        GameInput in = new GameInput();
        assertFalse(in.isKeyDown(42));
        in.keyDown(42);
        assertTrue(in.isKeyDown(42));
        in.keyUp(42);
        assertFalse(in.isKeyDown(42));
    }

    @Test
    void keyPressedEdgeClearsAfterFrame() {
        GameInput in = new GameInput();
        in.keyDown(7);
        assertTrue(in.wasKeyPressed(7));
        assertFalse(in.wasKeyReleased(7));
        in.clearFrameEdges();
        assertFalse(in.wasKeyPressed(7));   // edge cleared
        assertTrue(in.isKeyDown(7));         // but still held
    }

    @Test
    void keyReleasedEdge() {
        GameInput in = new GameInput();
        in.keyDown(9);
        in.clearFrameEdges();
        in.keyUp(9);
        assertTrue(in.wasKeyReleased(9));
        in.clearFrameEdges();
        assertFalse(in.wasKeyReleased(9));
    }

    @Test
    void pointerState() {
        GameInput in = new GameInput();
        assertFalse(in.isPointerDown());
        in.pointer(30, 40, true, true, false);
        assertTrue(in.isPointerDown());
        assertTrue(in.wasPointerPressed());
        assertEquals(30, in.getPointerX());
        assertEquals(40, in.getPointerY());
        in.clearFrameEdges();
        assertFalse(in.wasPointerPressed());
        in.pointer(30, 40, false, false, true);
        assertFalse(in.isPointerDown());
        assertTrue(in.wasPointerReleased());
    }

    @Test
    void axesFromJoystick() {
        GameInput in = new GameInput();
        assertEquals(0f, in.getAxisX(), 0.001);
        assertEquals(0f, in.getAxisY(), 0.001);
        in.setAxis(-0.5f, 0.75f);
        assertEquals(-0.5f, in.getAxisX(), 0.001);
        assertEquals(0.75f, in.getAxisY(), 0.001);
    }

    @Test
    void gameKeyMapsThroughDisplay() {
        GameInput in = new GameInput();
        int code = Display.getInstance().getKeyCode(Display.GAME_RIGHT);
        in.keyDown(code);
        assertTrue(in.isGameKeyDown(Display.GAME_RIGHT));
        in.keyUp(code);
        assertFalse(in.isGameKeyDown(Display.GAME_RIGHT));
    }
}
