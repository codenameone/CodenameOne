package com.codename1.ui.animations;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Image;
import org.junit.jupiter.api.Assertions;

public class AnimationObjectTest extends UITestBase {

    @FormTest
    public void testAnimationObject() {
        Image img = Image.createImage(20, 20, 0xFFFF0000);
        AnimationObject anim = AnimationObject.createAnimationImage(img, 10, 10);

        anim.defineMotionX(AnimationObject.MOTION_TYPE_LINEAR, 0, 1000, 10, 100);
        anim.defineMotionY(AnimationObject.MOTION_TYPE_LINEAR, 0, 1000, 10, 100);
        anim.defineOpacity(AnimationObject.MOTION_TYPE_LINEAR, 0, 1000, 255, 0);

        anim.setTime(0);
        Assertions.assertEquals(10, anim.getX());
        Assertions.assertEquals(10, anim.getY());
        Assertions.assertEquals(255, anim.getOpacity());

        anim.setTime(500);
        Assertions.assertTrue(anim.getX() > 10);
        Assertions.assertTrue(anim.getY() > 10);
        Assertions.assertTrue(anim.getOpacity() < 255);

        anim.setTime(1000);
        Assertions.assertEquals(100, anim.getX());
        Assertions.assertEquals(100, anim.getY());
        Assertions.assertEquals(0, anim.getOpacity());

        AnimationObject copy = anim.copy();
        copy.setTime(0);
        Assertions.assertEquals(10, copy.getX());
    }

    @FormTest
    public void testFrames() {
        Image img = Image.createImage(100, 20, 0xFF00FF00); // 5 frames of 20x20
        AnimationObject anim = AnimationObject.createAnimationImage(img, 0, 0);
        anim.defineFrames(20, 20, 100); // 100ms per frame

        anim.setTime(0);
        Image frame0 = anim.getImage();
        Assertions.assertNotNull(frame0);
        Assertions.assertEquals(20, frame0.getWidth());

        anim.setTime(150);
        Image frame1 = anim.getImage();
        Assertions.assertNotNull(frame1);
        Assertions.assertNotSame(frame0, frame1);
    }
}
