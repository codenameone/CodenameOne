package com.codename1.gaming;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Rectangle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Unit tests for {@link Sprite}: transform properties, render size, bounding box and
/// broad-phase intersection, plus the {@code PhysicsLinkable} bridge.
class SpriteTest extends UITestBase {

    private static Image img(int w, int h) {
        return Image.createImage(w, h, 0xff112233);
    }

    @Test
    void defaults() {
        Sprite s = new Sprite();
        assertNull(s.getImage());
        assertEquals(0, s.getX(), 0.001);
        assertEquals(0, s.getY(), 0.001);
        assertEquals(0f, s.getRotation(), 0.001);
        assertEquals(1f, s.getScaleX(), 0.001);
        assertEquals(1f, s.getScaleY(), 0.001);
        assertEquals(255, s.getAlpha());
        assertTrue(s.isVisible());
        assertEquals(0, s.getZOrder());
        assertEquals(0.5, s.getAnchorX(), 0.001);
        assertEquals(0.5, s.getAnchorY(), 0.001);
    }

    @Test
    void transformProperties() {
        Sprite s = new Sprite(img(10, 10));
        s.setPosition(50, 60);
        assertEquals(50, s.getX(), 0.001);
        assertEquals(60, s.getY(), 0.001);
        s.setX(70);
        s.setY(80);
        assertEquals(70, s.getX(), 0.001);
        assertEquals(80, s.getY(), 0.001);
        s.setPosition(1, 2, 3);
        assertEquals(3, s.getZ(), 0.001);
        s.setRotation(45f);
        assertEquals(45f, s.getRotation(), 0.001);
        s.setScale(2f);
        assertEquals(2f, s.getScaleX(), 0.001);
        assertEquals(2f, s.getScaleY(), 0.001);
        s.setScale(-1f, 2f);
        assertEquals(-1f, s.getScaleX(), 0.001);
        s.setAlpha(128);
        assertEquals(128, s.getAlpha());
        s.setColor(0xff00ff00);
        assertEquals(0xff00ff00, s.getColor());
        s.setVisible(false);
        assertFalse(s.isVisible());
        s.setZOrder(5);
        assertEquals(5, s.getZOrder());
        Object tag = new Object();
        s.setUserData(tag);
        assertSame(tag, s.getUserData());
    }

    @Test
    void renderSizeFromImageOrExplicit() {
        Sprite s = new Sprite(img(32, 48));
        assertEquals(32f, s.getRenderWidth(), 0.001);
        assertEquals(48f, s.getRenderHeight(), 0.001);
        s.setSize(100, 200);
        assertEquals(100f, s.getRenderWidth(), 0.001);
        assertEquals(200f, s.getRenderHeight(), 0.001);
        assertEquals(0f, new Sprite().getRenderWidth(), 0.001);   // no image, no size
    }

    @Test
    void boundsCenterOnAnchor() {
        Sprite s = new Sprite();
        s.setSize(40, 40);
        s.setPosition(100, 100);   // default anchor 0.5,0.5 -> centered box
        Rectangle b = s.getBounds();
        assertEquals(80, b.getX());
        assertEquals(80, b.getY());
        assertEquals(40, b.getWidth());
        assertEquals(40, b.getHeight());
    }

    @Test
    void boundsRespectAnchorAndScale() {
        Sprite s = new Sprite();
        s.setSize(40, 40);
        s.setAnchor(0, 0);          // top-left anchor
        s.setScale(2f);             // 80x80
        s.setPosition(10, 10);
        Rectangle b = s.getBounds();
        assertEquals(10, b.getX());
        assertEquals(10, b.getY());
        assertEquals(80, b.getWidth());
        assertEquals(80, b.getHeight());
    }

    @Test
    void intersection() {
        Sprite a = new Sprite();
        a.setSize(40, 40);
        a.setPosition(100, 100);
        Sprite b = new Sprite();
        b.setSize(40, 40);
        b.setPosition(110, 110);    // overlaps a
        Sprite c = new Sprite();
        c.setSize(40, 40);
        c.setPosition(300, 300);    // far away
        assertTrue(a.intersects(b));
        assertTrue(b.intersects(a));
        assertFalse(a.intersects(c));
    }

    @Test
    void physicsLinkableBridge() {
        Sprite s = new Sprite();
        s.setPhysicsPosition(33f, 44f);
        assertEquals(33, s.getX(), 0.001);
        assertEquals(44, s.getY(), 0.001);
        s.setPhysicsRotation((float) (Math.PI / 2));   // 90 degrees
        assertEquals(90f, s.getRotation(), 0.01);
    }

    @Test
    void setImageSwaps() {
        Sprite s = new Sprite(img(10, 10));
        Image other = img(20, 20);
        s.setImage(other);
        assertSame(other, s.getImage());
        assertEquals(20f, s.getRenderWidth(), 0.001);
    }
}
