package com.codename1.gaming;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Image;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Unit tests for {@link SpriteSheet}: the grid geometry and cached frame slicing.
class SpriteSheetTest extends UITestBase {

    @Test
    void gridGeometry() {
        Image sheet = Image.createImage(128, 96, 0xff223344);
        SpriteSheet ss = new SpriteSheet(sheet, 32, 32);
        assertEquals(4, ss.getColumns());
        assertEquals(3, ss.getRows());
        assertEquals(12, ss.getFrameCount());
        assertEquals(32, ss.getFrameWidth());
        assertEquals(32, ss.getFrameHeight());
    }

    @Test
    void framesSlicedToFrameSize() {
        Image sheet = Image.createImage(128, 96, 0xff223344);
        SpriteSheet ss = new SpriteSheet(sheet, 32, 32);
        Image f = ss.getFrame(0);
        assertNotNull(f);
        assertEquals(32, f.getWidth());
        assertEquals(32, f.getHeight());
    }

    @Test
    void framesAreCached() {
        Image sheet = Image.createImage(64, 64, 0xff223344);
        SpriteSheet ss = new SpriteSheet(sheet, 32, 32);
        Image first = ss.getFrame(2);
        Image again = ss.getFrame(2);
        assertSame(first, again);   // slicing is cached, not repeated
    }

    @Test
    void getFrameByColumnRowMatchesIndex() {
        Image sheet = Image.createImage(96, 64, 0xff223344);   // 3 cols x 2 rows
        SpriteSheet ss = new SpriteSheet(sheet, 32, 32);
        // index = row * columns + col
        assertSame(ss.getFrame(1), ss.getFrame(1, 0));
        assertSame(ss.getFrame(4), ss.getFrame(1, 1));
    }

    @Test
    void outOfRangeFrameThrows() {
        Image sheet = Image.createImage(64, 32, 0xff223344);   // 2 frames
        SpriteSheet ss = new SpriteSheet(sheet, 32, 32);
        assertThrows(IndexOutOfBoundsException.class, () -> ss.getFrame(2));
        assertThrows(IndexOutOfBoundsException.class, () -> ss.getFrame(-1));
    }
}
